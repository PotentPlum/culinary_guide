#!/usr/bin/env python3
"""Validate everything under data/: JSON Schema conformance, cross-references, strings.xml.

Stdlib only, so it runs anywhere (CI, Claude Code hook, a fresh laptop) without setup.
The schemas in data/schemas/ stick to the JSON Schema subset in tools/schema_check.py; the
Android unit tests load the same assets, so this script is the fast pre-build gate.

Usage: python3 tools/validate_data.py [--write-fixtures]
  --write-fixtures  regenerate fixtures/seasonality/status.json from vegetables.json and
                    fixtures/legacy/expected-backup.json from the legacy exports next to it
                    (do this deliberately, after editing peak/shoulder months or the converter)
Exit code 0 = all good, 1 = errors (printed one per line).
"""

from __future__ import annotations

import json
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

import export_curriculum
import legacy_to_backup
from schema_check import check_backup_references
from schema_check import validate as validate_schema

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "data"

errors: list[str] = []
# The legacy fixture's fixed conversion time, so expected-backup.json is stable.
LEGACY_EXPORTED_AT = 1793620800000


def err(where: str, msg: str) -> None:
    errors.append(f"{where}: {msg}")


def load(rel: str, schema: str | None = None) -> dict | list:
    path = DATA / rel
    try:
        doc = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as e:
        err(rel, f"cannot load: {e}")
        return {}
    if schema:
        root = json.loads((DATA / "schemas" / schema).read_text(encoding="utf-8"))
        validate_schema(doc, root, root, rel, errors)
    return doc


def contrast(a: str, b: str) -> float:
    """WCAG 2 contrast ratio between two #RRGGBB colours."""

    def luminance(hex_colour: str) -> float:
        channels = [int(hex_colour[i : i + 2], 16) / 255 for i in (1, 3, 5)]
        r, g, b = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
        return 0.2126 * r + 0.7152 * g + 0.0722 * b

    hi, lo = sorted((luminance(a), luminance(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


INK, WHITE = "#1E1A14", "#FFFFFF"


def unique(where: str, values: list[str]) -> set[str]:
    seen: set[str] = set()
    for v in values:
        if v in seen:
            err(where, f"duplicate {v!r}")
        seen.add(v)
    return seen


# ── Assets shipped in the app ────────────────────────────────────────────────

veg_doc = load("assets/vegetables.json", "vegetables.schema.json")
tech_doc = load("assets/techniques.json", "techniques.schema.json")
facet_doc = load("assets/facets.json", "facets.schema.json")
disc_doc = load("assets/disciplines.json", "disciplines.schema.json")
arch_doc = load("assets/archetypes.json", "archetypes.schema.json")
unit_doc = load("assets/units.json", "units.schema.json")
dept_doc = load("assets/departments.json", "departments.schema.json")

vegs = veg_doc.get("vegetables", [])
veg_ids = unique("vegetables.id", [v.get("id", "") for v in vegs])
unique("vegetables.nl", [v.get("nl", "") for v in vegs])
for v in vegs:
    overlap = set(v.get("peak", [])) & set(v.get("shoulder", []))
    if overlap:
        err(f"vegetables[{v.get('id')}]", f"months {sorted(overlap)} are both peak and shoulder")
    # VegToken initials are drawn in ink or white, whichever reads better (spec §5): one must reach WCAG AA.
    fill = v.get("color", "")
    if re.fullmatch(r"#[0-9A-Fa-f]{6}", fill) and max(contrast(fill, INK), contrast(fill, WHITE)) < 4.5:
        err(f"vegetables[{v.get('id')}]", f"color {fill} reaches < 4.5:1 with both ink and white initials")

disc_ids = unique("disciplines.id", [d.get("id", "") for d in disc_doc.get("disciplines", [])])

techs = tech_doc.get("techniques", [])
tech_ids = unique("techniques.id", [t.get("id", "") for t in techs])
tech_children: dict[str, set[str]] = {}
for t in techs:
    if t.get("parentId"):
        tech_children.setdefault(t["parentId"], set()).add(t.get("id", ""))
for t in techs:
    where = f"techniques[{t.get('id')}]"
    if t.get("parentId") is not None and (t["parentId"] not in tech_ids or t["parentId"] == t.get("id")):
        err(where, f"parentId {t['parentId']!r} does not resolve")
    # The detail page lists the parent and the variants in their own sections; "Gerelateerd" must not repeat them.
    family = tech_children.get(t.get("id", ""), set()) | ({t["parentId"]} if t.get("parentId") else set())
    for rid in t.get("relatedTechniqueIds", []):
        if rid not in tech_ids or rid == t.get("id"):
            err(where, f"relatedTechniqueIds {rid!r} does not resolve")
        elif rid in family:
            err(where, f"relatedTechniqueIds {rid!r} is its parent or a variant, already shown on the page")
    for vid in t.get("relatedVegIds", []):
        if vid not in veg_ids:
            err(where, f"relatedVegIds {vid!r} does not resolve")
    if t.get("discipline") is None:
        if t.get("category") != "gereedschap":
            err(where, "only cookware guides (category gereedschap) may have no discipline")
    elif t["discipline"] not in disc_ids:
        err(where, f"discipline {t['discipline']!r} not in disciplines.json")

# Everything shown is Dutch (CLAUDE.md, spec §15.4). Words that are only English, never Dutch: "pan" and "oven" are both.
ENGLISH_WORDS = {
    "the", "and", "with", "from", "into", "until", "your", "you", "this", "that", "slow", "cook", "cooking", "cooked",
    "fry", "fried", "frying", "boil", "boiling", "roast", "roasted", "roasting", "bake", "baking", "baked", "steam",
    "steamed", "steaming", "sauce", "heat", "chopped", "sliced", "stir", "braise", "braised", "serve", "fresh",
}


def english_words(text: str) -> list[str]:
    return sorted({w for w in re.findall(r"[A-Za-z]+", text) if w.lower() in ENGLISH_WORDS})


def check_dutch(where: str, item: dict, fields: list[str]) -> None:
    for field in fields:
        value = item.get(field)
        text = " ".join(map(str, value)) if isinstance(value, list) else str(value or "")
        found = english_words(text)
        if found:
            err(where, f"{field} has English words {found}: {text[:60]!r}")


for t in techs:
    check_dutch(f"techniques[{t.get('id')}]", t, ["nl", "subtitle", "body", "tips"])
for v in vegs:
    check_dutch(f"vegetables[{v.get('id')}]", v, ["nl", "blurb", "storage", "prep", "technique", "pairings", "substitutions"])

facets = facet_doc.get("facets", [])
unique("facets.key", [f.get("key", "") for f in facets])
for f in facets:
    unique(f"facets[{f.get('key')}].options.value", [o.get("value", "") for o in f.get("options", [])])

archs = arch_doc.get("archetypes", [])
unique("archetypes.id", [a.get("id", "") for a in archs])
for a in archs:
    rule = a.get("rule", {})
    where = f"archetypes[{a.get('id')}]"
    if "discipline" in rule and rule["discipline"] not in disc_ids:
        err(where, f"discipline {rule['discipline']!r} does not resolve")
    for tid in rule.get("techniqueIds", []):
        if tid not in tech_ids:
            err(where, f"techniqueId {tid!r} does not resolve")
    if rule.get("type") == "peakMonths" and rule.get("min", 0) > 12:
        err(where, "peakMonths min cannot exceed 12")
    if rule.get("type") == "distinctPeakVegetables" and rule.get("min", 0) > len(veg_ids):
        err(where, f"distinctPeakVegetables min exceeds the {len(veg_ids)} vegetables in the dataset")

# ── strings.xml ──────────────────────────────────────────────────────────────

STRINGS = "res/values/strings.xml"
FORMAT_SPEC = re.compile(r"%(\d+\$)?[sd]")
try:
    tree = ET.parse(DATA / STRINGS)
except (OSError, ET.ParseError) as e:
    err(STRINGS, f"cannot parse: {e}")
    tree = None
if tree is not None:
    names: list[str] = []
    for el in tree.getroot():
        name = el.get("name", "")
        names.append(name)
        where = f"{STRINGS}[{name}]"
        if not re.fullmatch(r"[a-z][a-z0-9_]*", name):
            err(where, "name must be snake_case")
        items = [el] if el.tag == "string" else el.findall("item")
        texts = ["".join(i.itertext()) for i in items]
        if el.tag not in ("string", "string-array", "plurals"):
            err(where, f"unexpected element <{el.tag}>")
        if el.tag == "plurals":
            quantities = [i.get("quantity") for i in items]
            if "other" not in quantities or not set(quantities) <= {"zero", "one", "two", "few", "many", "other"}:
                err(where, f"plurals need an 'other' item and valid quantities (got {quantities})")
        # Emphasis is inline markup inside one string (spec §5), never a sentence split across keys.
        for child in (c for i in items for c in i.iter() if c is not i):
            if child.tag not in ("i", "b"):
                err(where, f"unexpected markup <{child.tag}> (only <i> and <b>)")
        for text in texts:
            if not text.strip():
                err(where, "empty text")
            if re.search(r"(?<!\\)['\"]", text):
                err(where, "unescaped quote (Android requires \\' and \\\")")
            stripped = FORMAT_SPEC.sub("", text).replace("%%", "")
            if "%" in stripped:
                err(where, "bare % (use %% or a positional format specifier)")
            specs = FORMAT_SPEC.findall(text)
            if len(specs) > 1 and not all(specs):
                err(where, "multiple format args must be positional (%1$s, %2$d)")
    unique(STRINGS, names)
    months = [el for el in tree.getroot() if el.get("name") in ("months_short", "months_long", "months_initial")]
    for el in months:
        if len(el.findall("item")) != 12:
            err(f"{STRINGS}[{el.get('name')}]", "needs exactly 12 items")

# ── Test fixtures and reference data ────────────────────────────────────────


units = unit_doc.get("units", [])
unique("units.id", [u.get("id", "") for u in units])
# Every alias maps to exactly one unit, or the local ingredient parser would be ambiguous (spec §11.2).
unique("units.aliases", [a for u in units for a in u.get("aliases", [])])
base_units = {"MASS": "g", "VOLUME": "ml", "COUNT": "stuks"}
for u in units:
    where = f"units[{u.get('id')}]"
    dimension, factor = u.get("dimension"), u.get("factor")
    if dimension == "OTHER":
        if factor is not None:
            err(where, "OTHER units are never converted, so factor must be null")
    elif not isinstance(factor, (int, float)) or isinstance(factor, bool) or factor <= 0:
        err(where, "factor must be a number > 0")
    elif u.get("id") == base_units.get(dimension) and factor != 1:
        err(where, f"the {dimension} base unit must have factor 1")
for dimension, base in base_units.items():
    if not any(u.get("id") == base and u.get("dimension") == dimension for u in units):
        err("units", f"missing base unit {base!r} for {dimension}")


def plain_words(text: str) -> list[str]:
    """Lowercase words without diacritics, as the app's DepartmentClassifier splits them."""
    folded = unicodedata.normalize("NFD", text.lower())
    return re.findall(r"[a-z]+", "".join(c for c in folded if not unicodedata.combining(c)))


# Shop departments (spec §6.14, §11.7): a fixed set in a fixed order, with string resources and unambiguous keywords.
DEPARTMENT_IDS = ["groente", "zuivel-eieren", "vlees-vis", "brood", "voorraad", "overig"]
departments = dept_doc.get("departments", [])
if [d.get("id") for d in departments] != DEPARTMENT_IDS:
    err("departments", f"ids must be exactly {DEPARTMENT_IDS}, in that order (the app shows them in this order)")
# The app counts each vegetable as groente by its name (the last word of each " / " part), so those need no keyword.
veg_words = {plain_words(part)[-1] for v in vegs for part in v.get("nl", "").split(" / ") if plain_words(part)}
seen_keywords: dict[str, str] = {}
for d in departments:
    did, keywords = d.get("id", ""), d.get("keywords", [])
    where = f"departments[{did}]"
    if did == "overig" and keywords:
        err(where, "overig is the fallback and has no keywords")
    if did != "overig" and not keywords:
        err(where, "needs keywords")
    for k in keywords:
        if k in seen_keywords:
            err(where, f"keyword {k!r} is also in {seen_keywords[k]}")
        seen_keywords[k] = did
        if k in veg_words:
            err(where, f"keyword {k!r} is a vegetable name, which is always groente")
    if tree is not None and f"shopping_department_{did.replace('-', '_')}" not in names:
        err(where, f"missing string resource shopping_department_{did.replace('-', '_')}")


def status_for(veg: dict, month: int) -> str:
    """Reference implementation of the seasonality rule (spec.md §11.1)."""
    peak, shoulder = veg["peak"], veg["shoulder"]
    if month in peak:
        return "peak"
    if month in shoulder:
        return "shoulder"
    if any(1 <= (m - month) % 12 <= 2 for m in peak):
        return "soon"
    if (month - 2) % 12 + 1 in peak:
        return "ending"
    return "off"


SEASON = "fixtures/seasonality/status.json"
expected = {v["id"]: [status_for(v, m) for m in range(1, 13)] for v in vegs if "peak" in v}
if "--write-fixtures" in sys.argv:
    doc = json.loads((DATA / SEASON).read_text(encoding="utf-8"))
    doc["statuses"] = expected
    (DATA / SEASON).write_text(json.dumps(doc, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
season = load(SEASON)
if season.get("statuses") != expected:
    err(SEASON, "out of sync with vegetables.json (rerun with --write-fixtures if the change is intended)")

golden = load("fixtures/recipe-ingestion/golden.json", "golden.schema.json")
for i, case in enumerate(golden.get("cases", [])):
    if ("result" in case) == ("error" in case):
        err(f"golden.cases[{i}]", "needs exactly one of result / error")
    if "error" in case and case["error"].get("name") != "RecipeIngestionError":
        err(f"golden.cases[{i}]", "only RecipeIngestionError may be expected")

# Backup format (spec §13.1): the sample must match backup.schema.json and reference only what it contains.
BACKUP = "fixtures/backup/sample.json"
errors.extend(check_backup_references(load(BACKUP, "backup.schema.json"), veg_ids, BACKUP))

# Legacy converter (spec §13.3): the synthetic exports must convert to exactly the expected backup, which is valid.
LEGACY = "fixtures/legacy"
legacy_converted = legacy_to_backup.Converter(legacy_to_backup.Content.load(DATA), exported_at=LEGACY_EXPORTED_AT).convert(
    load(f"{LEGACY}/supabase.json"), load(f"{LEGACY}/local.json")
)
if "--write-fixtures" in sys.argv:
    legacy_text = json.dumps(legacy_converted, ensure_ascii=False, indent=1) + "\n"
    (DATA / LEGACY / "expected-backup.json").write_text(legacy_text, encoding="utf-8")
legacy_expected = load(f"{LEGACY}/expected-backup.json", "backup.schema.json")
errors.extend(check_backup_references(legacy_expected, veg_ids, f"{LEGACY}/expected-backup.json"))
if legacy_converted != legacy_expected:
    err(f"{LEGACY}/expected-backup.json", "differs from what tools/legacy_to_backup.py makes of the fixtures (rerun with --write-fixtures if intended)")

# ── Curriculum assets (spec §7.1; generated from curriculum/ by tools/export_curriculum.py) ─────────────────────

CURRICULUM = "assets/curriculum"
try:
    curriculum_files = export_curriculum.export()
except ValueError as e:
    curriculum_files = {}
    err(CURRICULUM, str(e))
for rel in export_curriculum.stale(curriculum_files) if curriculum_files else []:
    err(f"{CURRICULUM}/{rel}", "out of date with curriculum/ (run tools/export_curriculum.py --write)")
curriculum_index = load(f"{CURRICULUM}/index.json", "curriculum-index.schema.json")
curriculum_courses = curriculum_index.get("courses", [])
course_ids = unique("curriculum.courses.id", [c.get("id", "") for c in curriculum_courses])
competency_ids = unique("curriculum.competencies.id", [c.get("id", "") for c in curriculum_index.get("competencies", [])])
staged = [cid for s in curriculum_index.get("stages", []) for cid in s.get("courseIds", [])]
if sorted(staged) != sorted(course_ids):
    err(f"{CURRICULUM}/index.json", "every course must be listed in exactly one stage")
curriculum_units = 0
for c in curriculum_courses:
    where = f"{CURRICULUM}/courses/{c.get('id')}.json"
    for pid in c.get("prerequisites", []):
        if pid not in course_ids:
            err(f"curriculum.courses[{c.get('id')}]", f"prerequisite {pid!r} does not resolve")
    if not c.get("hasContent"):
        continue
    doc = load(where, "curriculum-course.schema.json")
    outline = {m["id"]: m for m in c.get("modules", [])}
    if [m.get("id") for m in doc.get("modules", [])] != list(outline):
        err(where, "modules differ from the index")
        continue
    for m in doc.get("modules", []):
        lesson_ids = [l["id"] for l in m.get("lessons", [])]
        if lesson_ids != [l["id"] for l in outline[m["id"]]["lessons"]]:
            err(where, f"{m['id']}: lessons differ from the index")
        if [x["id"] for x in m.get("exercises", [])] != [x["id"] for x in outline[m["id"]]["exercises"]]:
            err(where, f"{m['id']}: exercises differ from the index")
        curriculum_units += len(lesson_ids) + len(m.get("exercises", []))
        for unit in m.get("lessons", []) + m.get("exercises", []):
            for tag in unit.get("competencies", []):
                if tag["id"] not in competency_ids:
                    err(where, f"{unit['id']}: competency {tag['id']!r} does not resolve")
        for item in m.get("items", []):
            if item["lessonId"] not in lesson_ids:
                err(where, f"{item['id']}: lesson {item['lessonId']!r} is not in its module")
            keys = [o["key"] for o in item["options"]]
            if (item["type"] == "MULTIPLE_CHOICE") != bool(keys) or (keys and item["answer"] not in keys):
                err(where, f"{item['id']}: options and answer do not fit its type")

velt = load("reference/velt-vegetables.json")
if not isinstance(velt, list) or not all(isinstance(v, dict) and v.get("name") and v.get("url") for v in velt):
    err("reference/velt-vegetables.json", "expected a list of objects with name + url")

for prompt in ("quest-generation.md", "recipe-from-photos.md", "recipe-taxonomy.md"):
    if not (DATA / "reference" / "prompts" / prompt).is_file():
        err("reference/prompts", f"missing {prompt}")

# ── Report ───────────────────────────────────────────────────────────────────

if errors:
    print("\n".join(errors))
    print(f"\n{len(errors)} error(s)")
    sys.exit(1)
print(
    f"data OK — {len(vegs)} vegetables, {len(techs)} techniques, {len(facets)} facets, "
    f"{len(archs)} archetypes, {len(units)} units, {len(departments)} departments, {len(names) if tree is not None else 0} string resources, "
    f"{len(golden.get('cases', []))} golden cases, {len(course_ids)} curriculum courses ({curriculum_units} lessons and exercises)"
)
