"""The JSON Schema subset used by data/schemas, as a stdlib-only checker.

Shared by tools/validate_data.py and tools/legacy_to_backup.py, so the converter checks its output against
backup.schema.json with exactly the rules the data gate uses. Supported keywords: $ref (to #/$defs), oneOf, enum, type,
minLength, pattern, minimum, maximum, minItems, uniqueItems, items, required, minProperties, maxProperties, properties,
additionalProperties: false.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

TYPES = {
    "object": dict,
    "array": list,
    "string": str,
    "integer": int,
    "number": (int, float),
    "boolean": bool,
    "null": type(None),
}


def type_ok(value: object, name: str) -> bool:
    if name in ("integer", "number") and isinstance(value, bool):
        return False
    return isinstance(value, TYPES[name])


def validate(value: object, schema: dict, root: dict, path: str, errors: list[str]) -> None:
    """Appends one "path: message" line to [errors] per violation."""

    def err(where: str, msg: str) -> None:
        errors.append(f"{where}: {msg}")

    if "$ref" in schema:
        ref = schema["$ref"]
        assert ref.startswith("#/$defs/"), ref
        validate(value, root["$defs"][ref.split("/")[-1]], root, path, errors)
        return
    if "oneOf" in schema:
        passing = 0
        for option in schema["oneOf"]:
            trial: list[str] = []
            validate(value, option, root, path, trial)
            passing += not trial
        if passing != 1:
            err(path, f"must match exactly one oneOf option (matched {passing})")
        return
    if "enum" in schema and value not in schema["enum"]:
        err(path, f"{value!r} not in {schema['enum']}")
        return
    if "type" in schema:
        types = schema["type"] if isinstance(schema["type"], list) else [schema["type"]]
        if not any(type_ok(value, t) for t in types):
            err(path, f"expected {'/'.join(types)}, got {type(value).__name__}")
            return
    if isinstance(value, str):
        if len(value) < schema.get("minLength", 0):
            err(path, "string too short")
        if "pattern" in schema and not re.search(schema["pattern"], value):
            err(path, f"{value!r} does not match {schema['pattern']}")
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        if "minimum" in schema and value < schema["minimum"]:
            err(path, f"{value} < minimum {schema['minimum']}")
        if "maximum" in schema and value > schema["maximum"]:
            err(path, f"{value} > maximum {schema['maximum']}")
    if isinstance(value, list):
        if len(value) < schema.get("minItems", 0):
            err(path, f"needs at least {schema['minItems']} items")
        if schema.get("uniqueItems"):
            seen = [json.dumps(v, sort_keys=True) for v in value]
            if len(seen) != len(set(seen)):
                err(path, "items are not unique")
        if "items" in schema:
            for i, item in enumerate(value):
                validate(item, schema["items"], root, f"{path}[{i}]", errors)
    if isinstance(value, dict):
        for key in schema.get("required", []):
            if key not in value:
                err(path, f"missing required key {key!r}")
        if "minProperties" in schema and len(value) < schema["minProperties"]:
            err(path, f"needs at least {schema['minProperties']} keys")
        if "maxProperties" in schema and len(value) > schema["maxProperties"]:
            err(path, f"allows at most {schema['maxProperties']} keys")
        props = schema.get("properties", {})
        for key, sub in value.items():
            if key in props:
                validate(sub, props[key], root, f"{path}.{key}", errors)
            elif schema.get("additionalProperties") is False:
                err(path, f"unexpected key {key!r}")


def check_file(value: object, schema_path: Path, where: str) -> list[str]:
    """Validates [value] against the schema at [schema_path]; returns the violations."""
    root = json.loads(schema_path.read_text(encoding="utf-8"))
    errors: list[str] = []
    validate(value, root, root, where, errors)
    return errors


def check_backup_references(backup: dict, veg_ids: set[str], where: str) -> list[str]:
    """What backup.schema.json can't express (spec §13.1): unique ids, resolvable references, the zip entry names."""
    errors: list[str] = []

    def err(at: str, msg: str) -> None:
        errors.append(f"{at}: {msg}")

    def unique(at: str, values: list[str]) -> None:
        seen: set[str] = set()
        for v in values:
            if v in seen:
                err(at, f"duplicate {v!r}")
            seen.add(v)

    recipes = {r["id"] for r in backup.get("recipes", [])}
    quests = {q["id"] for q in backup.get("quests", [])}
    for kind in ("recipes", "quests", "attempts", "shoppingItems"):
        unique(f"{where}.{kind}", [x["id"] for x in backup.get(kind, [])])
    # Groups, ingredients and steps are rows with their own primary key, so their ids are unique across recipes.
    for groups, children in (("ingredientGroups", "items"), ("stepGroups", "steps")):
        rows = [g for r in backup.get("recipes", []) for g in r.get(groups, [])]
        unique(f"{where}.recipes[*].{groups}", [g["id"] for g in rows])
        unique(f"{where}.recipes[*].{groups}[*].{children}", [c["id"] for g in rows for c in g.get(children, [])])
    for q in backup.get("quests", []):
        if q["recipeId"] not in recipes:
            err(f"{where}.quests.{q['id']}", f"unknown recipeId {q['recipeId']!r}")
    for a in backup.get("attempts", []):
        at = f"{where}.attempts.{a['id']}"
        if a.get("recipeId") and a["recipeId"] not in recipes:
            err(at, f"unknown recipeId {a['recipeId']!r}")
        if a.get("questId") and a["questId"] not in quests:
            err(at, f"unknown questId {a['questId']!r}")
        for vid in a.get("vegIds", []) + a.get("peakVegIds", []):
            if vid not in veg_ids:
                err(at, f"unknown vegetable {vid!r}")
        if not set(a.get("peakVegIds", [])) <= set(a.get("vegIds", [])):
            err(at, "peakVegIds must be a subset of vegIds")
        if a.get("photo") and a["photo"] != f"journal/{a['id']}.jpg":
            err(at, "photo must be journal/{id}.jpg")
    for r in backup.get("recipes", []):
        if r.get("image") and r["image"] != f"images/{r['id']}.jpg":
            err(f"{where}.recipes.{r['id']}", "image must be images/{id}.jpg")
    return errors
