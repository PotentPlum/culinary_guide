#!/usr/bin/env python3
"""Convert the old Mise web-app exports into a normal Mise backup zip (spec §13.3, mapping in spec Appendix A).

Usage:
  python3 tools/legacy_to_backup.py mise-legacy-supabase.json [mise-legacy-local.json] -o Mise-backup-legacy.zip

Restore the zip in the app (Instellingen › Back-up terugzetten), then run "Ingrediënten aanvullen": ingredients are
written as text only. Stdlib only. The output is checked against data/schemas/backup.schema.json before the zip is
written, and the report lists everything that was skipped or guessed. Exit code 0 = zip written, 1 = nothing written.

The output is deterministic: ids that the legacy data doesn't have are derived from the legacy ids (uuid5), so
converting the same exports twice gives the same backup and a second restore with Samenvoegen adds nothing.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
import unicodedata
import uuid
import zipfile
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

from schema_check import check_backup_references, check_file

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "data"
APP_VERSION = "legacy-converter"
ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]{1,64}$")
NAMESPACE = uuid.uuid5(uuid.NAMESPACE_URL, "https://github.com/PotentPlum/culinary_guide/tools/legacy_to_backup")
PHOTO_URL_PREFIX = "mise-photo:"
# Section names the web app filled in itself; a group with one of these is shown untitled.
DEFAULT_SECTION_NAMES = {"ingredients", "instructions", "ingrediënten", "ingredienten", "bereiding"}
FACET_TAG_KEYS = {"cuisine": "CUISINE", "dishType": "DISH_TYPE", "mainIngredient": "MAIN_INGREDIENT"}
DIFFICULTIES = {
    "beginner": "BEGINNER",
    "intermediate": "INTERMEDIATE",
    "advanced": "ADVANCED",
    "easy": "BEGINNER",
    "medium": "INTERMEDIATE",
    "hard": "ADVANCED",
}
TIME_LIMITS = {"<15": "UNDER_15", "<30": "UNDER_30", "<60": "UNDER_60", ">60": "OVER_60"}
QUEST_UNITS = {"gram": "g", "ml": "ml", "liter": "l", "stuks": "stuks"}
UNTITLED_RECIPE = "Recept zonder titel"


# ── Content the mapping needs (the same assets the app ships) ───────────────


@dataclass
class Content:
    vegetables: list[dict]
    technique_ids: set[str]
    facet_options: dict[str, set[str]]

    @staticmethod
    def load(data: Path = DATA) -> Content:
        def asset(name: str) -> dict:
            return json.loads((data / "assets" / name).read_text(encoding="utf-8"))

        return Content(
            vegetables=asset("vegetables.json")["vegetables"],
            # Only real skills become technique tags; the cookware guides have no discipline (spec Appendix A).
            technique_ids={t["id"] for t in asset("techniques.json")["techniques"] if t.get("discipline")},
            facet_options={f["key"]: {o["value"] for o in f["options"]} for f in asset("facets.json")["facets"]},
        )


# ── Seasonal matching: a port of SeasonalMatcher.kt and Seasonality (spec §11.1, §11.4) ──────────


def fold(text: str) -> str:
    """TextMatch.fold: diacritics removed, lowercased, trimmed ("Suikermaïs" → "suikermais")."""
    decomposed = unicodedata.normalize("NFD", text)
    return "".join(c for c in decomposed if not unicodedata.category(c).startswith("M")).lower().strip()


DOUBLE_VOWEL = re.compile(r"(aa|ee|oo|uu)([^aeiou])$")


def forms(name: str) -> set[str]:
    """SeasonalMatcher.forms: "tomaat" → tomaat, tomaten; "radijs" → radijzen; "sjalot" → sjalotten."""
    result = {name, name + "s", name + "en", name + "'s"}
    m = DOUBLE_VOWEL.search(name)
    if m:
        result.add(name[: m.start()] + m.group(1)[0] + m.group(2) + "en")
    if name:
        last = name[-1]
        if last not in "aeiou":
            result.add(name + last + "en")
        if last == "s":
            result.add(name[:-1] + "zen")
        if last == "f":
            result.add(name[:-1] + "ven")
    for suffix in ("en", "s", "'s"):
        if name.endswith(suffix) and len(name) - len(suffix) >= 3:
            result.add(name[: -len(suffix)])
    return result


# A letter is a word character that is neither a digit nor "_" (Kotlin: \p{L}).
LETTER = r"[^\W\d_]"


class SeasonalMatcher:
    def __init__(self, vegetables: list[dict]):
        self.vegetables = {v["id"]: v for v in vegetables}
        self.patterns: list[tuple[str, re.Pattern[str]]] = []
        for v in vegetables:
            names = {f for part in v["nl"].split(" / ") for f in forms(fold(part))}
            alternatives = "|".join(re.escape(n) for n in sorted(names, key=len, reverse=True))
            self.patterns.append((v["id"], re.compile(f"(?<!{LETTER})({alternatives})(?!{LETTER})")))

    def vegetable_ids(self, texts: list[str]) -> list[str]:
        folded = [fold(t) for t in texts if t]
        return sorted(vid for vid, pattern in self.patterns if any(pattern.search(t) for t in folded))

    def peak_ids(self, veg_ids: list[str], month: int) -> list[str]:
        return [vid for vid in veg_ids if month in self.vegetables[vid].get("peak", [])]


def local_month(millis: int) -> int:
    """The month on the phone's calendar; the owner cooks in the Netherlands."""
    try:
        from zoneinfo import ZoneInfo

        zone = ZoneInfo("Europe/Amsterdam")
    except Exception:  # noqa: BLE001 - no tz database (e.g. Windows without tzdata): UTC is off by at most hours
        zone = timezone.utc
    return datetime.fromtimestamp(millis / 1000, zone).month


# ── Small value helpers ──────────────────────────────────────────────────────


def text(value: object) -> str | None:
    """A trimmed non-empty string, or None."""
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        value = format(value, "g")
    if not isinstance(value, str):
        return None
    value = " ".join(value.split())
    return value or None


def strings(value: object) -> list[str]:
    """A string or a list of strings (RecipeOutput.cuisine/category) as a clean list."""
    items = value if isinstance(value, list) else [value]
    return [s for s in (text(v) for v in items) if s]


def integer(value: object, minimum: int = 0, maximum: int | None = None) -> int | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, str):
        try:
            value = float(value.strip())
        except ValueError:
            return None
    if isinstance(value, float) and value.is_integer():
        value = int(value)
    if not isinstance(value, int) or value < minimum or (maximum is not None and value > maximum):
        return None
    return value


def number(value: object) -> float | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str):
        try:
            return float(value.strip())
        except ValueError:
            return None
    return None


def present(value: object) -> bool:
    """A legacy column counts as set when it holds something: Postgres defaults ('', '{}', []) mean "not set"."""
    if value is None:
        return False
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, (list, dict)):
        return bool(value) and not (isinstance(value, dict) and all(v is None for v in value.values()))
    return True


def millis(value: object) -> int | None:
    """An ISO timestamp from Postgres (json_agg of timestamptz) or epoch millis from localStorage."""
    if isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        return int(value)
    if not isinstance(value, str) or not value.strip():
        return None
    raw = value.strip().replace(" ", "T", 1)
    raw = re.sub(r"Z$", "+00:00", raw)
    raw = re.sub(r"(\.\d{6})\d+", r"\1", raw)  # Python accepts at most microseconds
    raw = re.sub(r"([+-]\d{2})$", r"\1:00", raw)  # Postgres writes "+00" for UTC
    try:
        parsed = datetime.fromisoformat(raw)
    except ValueError:
        return None
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return int(parsed.timestamp() * 1000)


def derived_id(*parts: str) -> str:
    return str(uuid.uuid5(NAMESPACE, "/".join(parts)))


# ── The conversion ───────────────────────────────────────────────────────────


@dataclass
class Report:
    lines: list[str] = field(default_factory=list)

    def note(self, where: str, message: str) -> None:
        self.lines.append(f"{where}: {message}")


class Converter:
    def __init__(self, content: Content, exported_at: int):
        self.content = content
        self.exported_at = exported_at
        self.matcher = SeasonalMatcher(content.vegetables)
        self.report = Report()

    # Ids ────────────────────────────────────────────────────────────────────

    def legacy_id(self, value: object, kind: str, fallback: str) -> str:
        """The legacy uuid when it is a valid backup id, else one derived from [fallback]."""
        raw = text(value)
        if raw and ID_PATTERN.match(raw):
            return raw
        new = derived_id(kind, raw or fallback)
        self.report.note(f"{kind} {raw or fallback}", f"no usable id, using {new}")
        return new

    # Tags ───────────────────────────────────────────────────────────────────

    def facet_tags(self, tags: object, where: str) -> list[dict]:
        """RecipeTags {cuisine?, dishType?, mainIngredient?, prepTime?} → recipe tags; prepTime is derived, so dropped."""
        if not isinstance(tags, dict):
            return []
        result = []
        for key, tag_key in FACET_TAG_KEYS.items():
            for value in strings(tags.get(key)):
                if value in self.content.facet_options.get(key, set()):
                    result.append({"key": tag_key, "value": value})
                else:
                    self.report.note(where, f"dropped unknown {key} tag {value!r}")
        return result

    def technique_tags(self, ids: object, where: str) -> list[dict]:
        result = []
        for value in strings(ids):
            if value in self.content.technique_ids:
                result.append({"key": "TECHNIQUE", "value": value})
            else:
                self.report.note(where, f"dropped technique {value!r} (not in techniques.json or not a skill)")
        return result

    def cuisine_tags(self, values: object, where: str, quiet: bool = False) -> list[dict]:
        result = []
        for value in strings(values):
            if value in self.content.facet_options.get("cuisine", set()):
                result.append({"key": "CUISINE", "value": value})
            elif not quiet:
                self.report.note(where, f"dropped unknown cuisine {value!r}")
        return result

    @staticmethod
    def merge_tags(*groups: list[dict]) -> list[dict]:
        seen: set[tuple[str, str]] = set()
        result = []
        for group in groups:
            for tag in group:
                pair = (tag["key"], tag["value"])
                if pair not in seen:
                    seen.add(pair)
                    result.append(tag)
        return result

    # Recipes ────────────────────────────────────────────────────────────────

    @staticmethod
    def recipe_output(instructions: object) -> dict | None:
        """The JSON RecipeOutput the web app stored in `instructions`, or None when it holds something else."""
        if isinstance(instructions, dict):
            return instructions
        if not isinstance(instructions, str):
            return None
        try:
            parsed = json.loads(instructions)
        except ValueError:
            return None
        return parsed if isinstance(parsed, dict) else None

    @staticmethod
    def section_name(value: object) -> str | None:
        name = text(value)
        if name:
            name = name.rstrip(":").strip()
        return None if not name or fold(name) in {fold(n) for n in DEFAULT_SECTION_NAMES} else name

    def ingredient_groups(self, recipe_id: str, sections: list[tuple[str | None, list[str]]]) -> list[dict]:
        groups = []
        for g, (name, lines) in enumerate((n, ls) for n, ls in sections if ls):
            group_id = derived_id(recipe_id, "ingredients", str(g))
            groups.append(
                {
                    "id": group_id,
                    "name": name,
                    "items": [
                        {"id": derived_id(group_id, str(i)), "text": line, "parseSource": "NONE"} for i, line in enumerate(lines)
                    ],
                }
            )
        return groups

    @staticmethod
    def step_groups(recipe_id: str, sections: list[tuple[str | None, list[str]]]) -> list[dict]:
        groups = []
        for g, (name, steps) in enumerate((n, s) for n, s in sections if s):
            group_id = derived_id(recipe_id, "steps", str(g))
            groups.append(
                {"id": group_id, "name": name, "steps": [{"id": derived_id(group_id, str(i)), "text": s} for i, s in enumerate(steps)]}
            )
        return groups

    def output_ingredients(self, output: dict) -> list[tuple[str | None, list[str]]]:
        sections = []
        for section in output.get("ingredientSections") or []:
            if not isinstance(section, dict):
                continue
            lines = []
            for item in section.get("ingredients") or []:
                if isinstance(item, dict):
                    # The metric text is what the web app showed when the page had both.
                    line = text(item.get("textMetric")) or text(item.get("text"))
                else:
                    line = text(item)
                if line:
                    lines.append(line)
            sections.append((self.section_name(section.get("sectionName")), lines))
        return sections

    def output_steps(self, output: dict) -> list[tuple[str | None, list[str]]]:
        sections = []
        for section in output.get("instructionSections") or []:
            if not isinstance(section, dict):
                continue
            raw = [s for s in section.get("steps") or [] if isinstance(s, (dict, str))]
            ordered = sorted(
                enumerate(raw),
                key=lambda pair: (integer(pair[1].get("stepNumber")) if isinstance(pair[1], dict) else None) or pair[0] + 1,
            )
            steps = [text(s.get("text")) if isinstance(s, dict) else text(s) for _, s in ordered]
            sections.append((self.section_name(section.get("sectionName")), [s for s in steps if s]))
        return sections

    @staticmethod
    def source_meta(fields: dict) -> dict | None:
        rating = fields.get("aggregateRating") if isinstance(fields.get("aggregateRating"), dict) else {}
        nutrition = fields.get("nutrition") if isinstance(fields.get("nutrition"), dict) else {}
        nutrition = {k: text(nutrition.get(k)) for k in ("calories", "protein", "carbs", "fat") if text(nutrition.get(k))}
        meta = {
            "ratingValue": number(rating.get("ratingValue")),
            "ratingCount": integer(rating.get("ratingCount")),
            "datePublished": text(fields.get("datePublished")),
            "keywords": strings(fields.get("keywords")),
            "diet": strings(fields.get("suitableForDiet")),
            "nutrition": nutrition or None,
            "video": text(fields.get("video")),
        }
        meta = {k: v for k, v in meta.items() if v not in (None, [])}
        return meta or None

    def recipe(self, row: dict, local: dict | None) -> dict:
        """A Supabase `recipes` row (or a localStorage-only entry when [row] is empty) → backup recipe."""
        local = local or {}
        local_output = local.get("recipe") if isinstance(local.get("recipe"), dict) else {}
        url = text(row.get("source_url")) or text(local.get("url")) or ""
        recipe_id = self.legacy_id(row.get("id"), "recipe", url or json.dumps(local_output, sort_keys=True)) if row else derived_id(
            "local", url or text(local_output.get("title")) or str(local.get("savedAt"))
        )
        where = f"recipe {recipe_id}"
        output = self.recipe_output(row.get("instructions")) if row else local_output
        if row and output is None:
            if present(row.get("instructions")):
                self.report.note(where, "instructions are not JSON: built from the columns, steps from the text")
            output = {}
        if not output and local_output:
            output = local_output

        # Column values override the JSON when they are set (the web app's fetchRemoteSaved).
        columns = {
            "description": "description",
            "prepTimeMinutes": "prep_time",
            "cookTimeMinutes": "cook_time",
            "totalTimeMinutes": "total_time",
            "servings": "servings",
            "cuisine": "cuisine",
            "category": "category",
            "keywords": "keywords",
            "image": "image_url",
            "author": "author",
            "suitableForDiet": "suitable_for_diet",
            "datePublished": "date_published",
            "aggregateRating": "aggregate_rating",
            "video": "video_url",
            "nutrition": "nutrition",
        }
        fields = {key: row.get(column) if present(row.get(column)) else output.get(key) for key, column in columns.items()}
        # A recipe that is both in Supabase and in localStorage keeps the Supabase data and fills the gaps from local.
        for key in columns:
            if not present(fields[key]) and present(local_output.get(key)):
                fields[key] = local_output.get(key)

        title = text(output.get("title")) or text(row.get("name")) or text(local_output.get("title"))
        if not title:
            title = UNTITLED_RECIPE
            self.report.note(where, f"no title, named {title!r}")

        ingredients = self.output_ingredients(output) or self.output_ingredients(local_output)
        if not any(lines for _, lines in ingredients):
            lines = [line for line in (text(i) for i in row.get("ingredients") or []) if line]
            ingredients = [(None, lines)]
        steps = self.output_steps(output) or self.output_steps(local_output)
        if not any(s for _, s in steps) and row and self.recipe_output(row.get("instructions")) is None:
            plain = row.get("instructions") if isinstance(row.get("instructions"), str) else ""
            steps = [(None, [s for s in (text(line) for line in plain.splitlines()) if s])]
        if not any(s for _, s in steps):
            self.report.note(where, "no steps")

        if url.startswith(PHOTO_URL_PREFIX):
            source_type, source_url = "PHOTO", None
        elif re.match(r"^https?://", url, re.IGNORECASE):
            source_type, source_url = "URL", url
        else:
            source_type, source_url = "LEGACY", None
            self.report.note(where, f"no web address ({url!r}): imported as LEGACY" if url else "no web address: imported as LEGACY")
        source_label = text(row.get("source")) or text(local.get("source")) or ""
        if not source_label and source_url:
            source_label = re.sub(r"^www\.", "", re.sub(r"^https?://([^/:?#]+).*$", r"\1", source_url, flags=re.IGNORECASE))

        image_url = text(fields["image"])
        if image_url and not re.match(r"^https?://", image_url, re.IGNORECASE):
            self.report.note(where, "dropped an image that is not a web address")
            image_url = None

        created = millis(row.get("created_at")) or millis(local.get("savedAt"))
        if created is None:
            created = self.exported_at
            self.report.note(where, "no creation date, using the conversion time")
        updated = max(millis(row.get("updated_at")) or created, created)

        # Tags: the Supabase row's, else the local entry's; then taxonomy and Schema.org values when a key is missing.
        tags = self.facet_tags(row.get("tags") if present(row.get("tags")) else local.get("tags"), where)
        taxonomy = local.get("taxonomy") if isinstance(local.get("taxonomy"), dict) else {}
        tags += self.technique_tags(taxonomy.get("techniques"), where)
        if not any(t["key"] == "CUISINE" for t in tags):
            tags += self.cuisine_tags(taxonomy.get("cuisines"), where)
        if not any(t["key"] == "CUISINE" for t in tags):
            tags += self.cuisine_tags(fields["cuisine"], where, quiet=True)
        if not any(t["key"] == "DISH_TYPE" for t in tags):
            dish = [v for v in strings(fields["category"]) if v in self.content.facet_options.get("dishType", set())]
            tags += [{"key": "DISH_TYPE", "value": v} for v in dish]

        recipe = {
            "id": recipe_id,
            "title": title,
            "description": text(fields["description"]) or "",
            "sourceType": source_type,
            "sourceUrl": source_url,
            "sourceLabel": source_label,
            "author": text(fields["author"]),
            "imageUrl": image_url,
            "servings": integer(fields["servings"], 1, 99),
            "prepMinutes": integer(fields["prepTimeMinutes"], 1),
            "cookMinutes": integer(fields["cookTimeMinutes"], 1),
            "totalMinutes": integer(fields["totalTimeMinutes"], 1),
            "inLibrary": True,
            "enrichState": "NONE",
            "sourceMeta": self.source_meta(fields),
            "createdAt": created,
            "updatedAt": updated,
            "ingredientGroups": self.ingredient_groups(recipe_id, ingredients),
            "stepGroups": self.step_groups(recipe_id, steps),
            "tags": self.merge_tags(tags),
        }
        return {k: v for k, v in recipe.items() if v is not None}

    # Quests ─────────────────────────────────────────────────────────────────

    def quest(self, row: dict) -> tuple[dict, dict]:
        """A Supabase `challenges` row → (quest, its hidden QUEST recipe)."""
        quest_id = self.legacy_id(row.get("id"), "quest", json.dumps(row, sort_keys=True, default=str))
        where = f"quest {quest_id}"
        recipe_id = derived_id(quest_id, "recipe")
        legacy = row.get("recipe") if isinstance(row.get("recipe"), dict) else {}
        if isinstance(row.get("recipe"), str):
            legacy = self.recipe_output(row["recipe"]) or {}
        challenge = text(row.get("challenge_text")) or ""
        if not legacy:
            self.report.note(where, "no recipe: the quest gets an empty one")

        difficulty = DIFFICULTIES.get((text(row.get("difficulty")) or "").lower())
        if difficulty is None:
            difficulty = "INTERMEDIATE"
            self.report.note(where, f"difficulty {row.get('difficulty')!r} unknown, using INTERMEDIATE")
        output_type = text(row.get("output_type")) or ""
        params = row.get("params") if isinstance(row.get("params"), dict) else {}

        lines = []
        for item in legacy.get("ingredients") or []:
            if isinstance(item, dict):
                parts = [text(item.get("amount")), text(item.get("unit")), text(item.get("name"))]
                line = " ".join(p for p in parts if p)
                if line:
                    lines.append((line, (text(item.get("name")) or "").lower() or None))
            elif text(item):
                lines.append((text(item), None))
        steps = []
        raw_steps = [s for s in legacy.get("steps") or [] if isinstance(s, (dict, str))]
        for position, step in sorted(
            enumerate(raw_steps), key=lambda p: (integer(p[1].get("step")) if isinstance(p[1], dict) else None) or p[0] + 1
        ):
            step_text = text(step.get("instruction")) if isinstance(step, dict) else text(step)
            if step_text:
                steps.append(step_text)

        groups = self.ingredient_groups(recipe_id, [(None, [line for line, _ in lines])])
        for group in groups:
            for item, (_, name) in zip(group["items"], lines):
                if name:
                    item["name"] = name

        tags = self.facet_tags(legacy.get("tags"), where)
        tags += self.technique_tags(row.get("techniques"), where)
        tags += self.cuisine_tags(row.get("cuisines"), where)
        if output_type in self.content.facet_options.get("dishType", set()):
            tags.append({"key": "DISH_TYPE", "value": output_type})
        created = millis(row.get("created_at"))
        if created is None:
            created = self.exported_at
            self.report.note(where, "no creation date, using the conversion time")
        time_estimate = integer(row.get("time_estimate")) or 0

        title = text(legacy.get("title")) or challenge[:80].rstrip() or text(row.get("ingredient")) or UNTITLED_RECIPE
        recipe = {
            "id": recipe_id,
            "title": title,
            "sourceType": "QUEST",
            "servings": integer(legacy.get("servings"), 1, 99),
            "totalMinutes": time_estimate or None,
            "inLibrary": False,
            "enrichState": "NONE",
            "createdAt": created,
            "updatedAt": created,
            "ingredientGroups": groups,
            "stepGroups": self.step_groups(recipe_id, [(None, steps)]),
            "tags": self.merge_tags(tags),
        }

        technique = text(params.get("technique"))
        if technique and technique not in self.content.technique_ids:
            self.report.note(where, f"dropped requested technique {technique!r}")
            technique = None
        unit = text(params.get("unit"))
        quest = {
            "id": quest_id,
            "createdAt": created,
            "isDaily": False,
            "status": "OPEN",
            "ingredientMode": "CUSTOM",
            "ingredient": text(row.get("ingredient")) or "",
            "quantity": number(params.get("quantity")),
            "unit": QUEST_UNITS.get(unit, unit) if unit else None,
            "questType": "SAUS" if output_type == "Saus/Basis" else "MAALTIJD",
            "requestedTechniqueId": technique,
            "requestedCuisine": text(params.get("cuisine")),
            "timeLimit": TIME_LIMITS.get(text(params.get("timeLimit")) or ""),
            "simpleAndSmall": params.get("simpleAndSmall") is True,
            "text": challenge,
            "skill": text(row.get("skill")) or "",
            "difficulty": difficulty,
            "timeEstimateMinutes": time_estimate,
            "why": text(row.get("why")) or "",
            "dishType": output_type,
            "tips": strings(legacy.get("tips")),
            "recipeId": recipe_id,
        }
        return {k: v for k, v in quest.items() if v is not None}, {k: v for k, v in recipe.items() if v is not None}

    # Attempts ───────────────────────────────────────────────────────────────

    def attempt(self, row: dict, recipes: dict[str, dict], quests: dict[str, dict]) -> dict | None:
        attempt_id = self.legacy_id(row.get("id"), "attempt", json.dumps(row, sort_keys=True, default=str))
        where = f"attempt {attempt_id}"
        cooked_at = millis(row.get("attempted_at"))
        if cooked_at is None:
            self.report.note(where, "skipped: no date")
            return None
        source = text(row.get("source_type"))
        source_id = text(row.get("source_id"))
        recipe_id = quest_id = None
        if source == "challenge":
            source_type = "QUEST"
            if source_id in quests:
                quest_id = source_id
                recipe_id = quests[source_id]["recipeId"]
        elif source == "recipe":
            source_type = "RECIPE"
            if source_id in recipes:
                recipe_id = source_id
        else:
            self.report.note(where, f"skipped: unknown source_type {source!r}")
            return None
        if source_id and recipe_id is None:
            self.report.note(where, f"{source} {source_id} not found: kept without a link")

        # §11.4 on the attempt's ingredient names, plus the linked recipe's lines (what the app itself snapshots).
        texts = strings(row.get("ingredients"))
        if recipe_id:
            texts += [i.get("name") or i["text"] for g in recipes[recipe_id].get("ingredientGroups", []) for i in g["items"]]
        veg_ids = self.matcher.vegetable_ids(texts)

        score, taste = integer(row.get("score"), 1, 10), integer(row.get("taste_score"), 1, 10)
        for label, raw, value in (("score", row.get("score"), score), ("taste_score", row.get("taste_score"), taste)):
            if raw is not None and value is None:
                self.report.note(where, f"dropped {label} {raw!r} (not 1–10)")
        attempt = {
            "id": attempt_id,
            "sourceType": source_type,
            "recipeId": recipe_id,
            "questId": quest_id,
            "cookedAt": cooked_at,
            "score": score,
            "tasteScore": taste,
            # Trimmed but not collapsed: the owner's line breaks stay.
            "notes": row["notes"].strip() or None if isinstance(row.get("notes"), str) else None,
            "techniques": strings(row.get("techniques")),
            "cuisines": strings(row.get("cuisines")),
            "vegIds": veg_ids,
            "peakVegIds": self.matcher.peak_ids(veg_ids, local_month(cooked_at)),
        }
        return {k: v for k, v in attempt.items() if v is not None}

    # Everything ─────────────────────────────────────────────────────────────

    def convert(self, supabase: dict, local: dict | None = None) -> dict:
        rows = [r for r in supabase.get("recipes") or [] if isinstance(r, dict)]
        entries = [e for e in (local or {}).get("localRecipes") or [] if isinstance(e, dict)]
        row_ids = {text(r.get("id")) for r in rows}
        row_urls = {text(r.get("source_url")): text(r.get("id")) for r in rows if text(r.get("source_url"))}

        # localStorage entries merge into their Supabase row by remoteId (or, without one, by URL as the web app did).
        local_by_row: dict[str, dict] = {}
        local_only: list[dict] = []
        for entry in entries:
            remote = text(entry.get("remoteId"))
            target = remote if remote in row_ids else row_urls.get(text(entry.get("url")))
            if target:
                local_by_row.setdefault(target, entry)
            else:
                local_only.append(entry)

        recipes: dict[str, dict] = {}
        for row in rows:
            recipe = self.recipe(row, local_by_row.get(text(row.get("id"))))
            if recipe["id"] in recipes:
                self.report.note(f"recipe {recipe['id']}", "skipped: duplicate id")
                continue
            recipes[recipe["id"]] = recipe
        for entry in local_only:
            recipe = self.recipe({}, entry)
            if recipe["id"] in recipes:
                self.report.note(f"recipe {recipe['id']}", "skipped: the same local recipe twice")
                continue
            self.report.note(f"recipe {recipe['id']}", f"only in localStorage: added ({recipe['title']})")
            recipes[recipe["id"]] = recipe

        quests: dict[str, dict] = {}
        for row in (r for r in supabase.get("challenges") or [] if isinstance(r, dict)):
            quest, recipe = self.quest(row)
            if quest["id"] in quests:
                self.report.note(f"quest {quest['id']}", "skipped: duplicate id")
                continue
            quests[quest["id"]] = quest
            recipes[recipe["id"]] = recipe

        attempts: dict[str, dict] = {}
        for row in (r for r in supabase.get("user_attempts") or [] if isinstance(r, dict)):
            attempt = self.attempt(row, recipes, quests)
            if attempt and attempt["id"] not in attempts:
                attempts[attempt["id"]] = attempt

        # A quest that was cooked at least once is COMPLETED, at its first attempt.
        for attempt in attempts.values():
            quest = quests.get(attempt.get("questId") or "")
            if quest:
                quest["status"] = "COMPLETED"
                quest["completedAt"] = min(quest.get("completedAt", attempt["cookedAt"]), attempt["cookedAt"])

        return {
            "format": "mise-backup",
            "schemaVersion": 1,
            "exportedAt": self.exported_at,
            "appVersion": APP_VERSION,
            "recipes": list(recipes.values()),
            "quests": list(quests.values()),
            "attempts": sorted(attempts.values(), key=lambda a: (a["cookedAt"], a["id"])),
            "shoppingItems": [],
        }


def check(backup: dict, content: Content, data: Path = DATA) -> list[str]:
    """Schema and reference errors in [backup]; empty when the app will accept it."""
    errors = check_file(backup, data / "schemas" / "backup.schema.json", "backup.json")
    return errors + check_backup_references(backup, {v["id"] for v in content.vegetables}, "backup.json")


def read_export(path: Path, expected_format: str) -> dict:
    doc = json.loads(path.read_text(encoding="utf-8-sig"))
    # The Supabase SQL editor can wrap the result in a one-row, one-column table.
    if isinstance(doc, list) and len(doc) == 1 and isinstance(doc[0], dict) and len(doc[0]) == 1:
        doc = next(iter(doc[0].values()))
    if isinstance(doc, str):
        doc = json.loads(doc)
    if not isinstance(doc, dict) or doc.get("format") != expected_format:
        raise ValueError(f"{path}: not a {expected_format} export (format is {doc.get('format') if isinstance(doc, dict) else None!r})")
    return doc


def write_zip(backup: dict, path: Path) -> None:
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as zip_file:
        zip_file.writestr("backup.json", json.dumps(backup, ensure_ascii=False, indent=1))


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Convert the old Mise web-app exports into a Mise backup zip (spec §13.3).")
    parser.add_argument("supabase", type=Path, help="mise-legacy-supabase.json")
    parser.add_argument("local", type=Path, nargs="?", help="mise-legacy-local.json (optional)")
    parser.add_argument("-o", "--output", type=Path, required=True, help="the zip to write, e.g. Mise-backup-legacy.zip")
    args = parser.parse_args(argv)

    try:
        supabase = read_export(args.supabase, "mise-legacy-supabase")
        local = read_export(args.local, "mise-legacy-local") if args.local else None
    except (OSError, ValueError) as e:
        print(f"error: {e}", file=sys.stderr)
        return 1

    content = Content.load()
    converter = Converter(content, exported_at=int(time.time() * 1000))
    backup = converter.convert(supabase, local)
    errors = check(backup, content)
    if errors:
        print("error: the converted backup is invalid, nothing written:", file=sys.stderr)
        print("\n".join(errors), file=sys.stderr)
        return 1
    write_zip(backup, args.output)

    library = [r for r in backup["recipes"] if r["inLibrary"]]
    print(f"Wrote {args.output}")
    print(
        f"  {len(library)} recipes, {len(backup['quests'])} quests "
        f"({sum(q['status'] == 'COMPLETED' for q in backup['quests'])} completed), {len(backup['attempts'])} attempts"
    )
    print(f"  in: {len(supabase.get('recipes') or [])} Supabase recipes, {len((local or {}).get('localRecipes') or [])} local recipes, "
          f"{len(supabase.get('challenges') or [])} challenges, {len(supabase.get('user_attempts') or [])} attempts")
    if converter.report.lines:
        print(f"\nReport ({len(converter.report.lines)}):")
        print("\n".join(f"  {line}" for line in converter.report.lines))
    print("\nNext: restore the zip in the app (Instellingen › Back-up terugzetten), then run Ingrediënten aanvullen.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
