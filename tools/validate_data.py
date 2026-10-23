#!/usr/bin/env python3
"""Validate everything under data/: JSON Schema conformance, cross-references, strings.xml.

Stdlib only, so it runs anywhere (CI, Claude Code hook, a fresh laptop) without setup.
The schemas in data/schemas/ stick to the JSON Schema subset implemented below; the
Android unit tests load the same assets, so this script is the fast pre-build gate.

Usage: python3 tools/validate_data.py [--write-fixtures]
  --write-fixtures  regenerate fixtures/seasonality/status.json from vegetables.json
                    (do this deliberately after editing peak/shoulder months)
Exit code 0 = all good, 1 = errors (printed one per line).
"""

from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "data"

errors: list[str] = []


def err(where: str, msg: str) -> None:
    errors.append(f"{where}: {msg}")


# ── Minimal JSON Schema validator (subset used by data/schemas) ──────────────

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


def validate(value: object, schema: dict, root: dict, path: str) -> None:
    if "$ref" in schema:
        ref = schema["$ref"]
        assert ref.startswith("#/$defs/"), ref
        validate(value, root["$defs"][ref.split("/")[-1]], root, path)
        return
    if "oneOf" in schema:
        before = len(errors)
        passing = 0
        for option in schema["oneOf"]:
            mark = len(errors)
            validate(value, option, root, path)
            passing += len(errors) == mark
        del errors[before:]
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
                validate(item, schema["items"], root, f"{path}[{i}]")
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
                validate(sub, props[key], root, f"{path}.{key}")
            elif schema.get("additionalProperties") is False:
                err(path, f"unexpected key {key!r}")


def load(rel: str, schema: str | None = None) -> dict | list:
    path = DATA / rel
    try:
        doc = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as e:
        err(rel, f"cannot load: {e}")
        return {}
    if schema:
        root = json.loads((DATA / "schemas" / schema).read_text(encoding="utf-8"))
        validate(doc, root, root, rel)
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
for t in techs:
    where = f"techniques[{t.get('id')}]"
    if t.get("parentId") is not None and (t["parentId"] not in tech_ids or t["parentId"] == t.get("id")):
        err(where, f"parentId {t['parentId']!r} does not resolve")
    for rid in t.get("relatedTechniqueIds", []):
        if rid not in tech_ids or rid == t.get("id"):
            err(where, f"relatedTechniqueIds {rid!r} does not resolve")
    for vid in t.get("relatedVegIds", []):
        if vid not in veg_ids:
            err(where, f"relatedVegIds {vid!r} does not resolve")
    if t.get("discipline") is None:
        if t.get("category") != "gereedschap":
            err(where, "only cookware guides (category gereedschap) may have no discipline")
    elif t["discipline"] not in disc_ids:
        err(where, f"discipline {t['discipline']!r} not in disciplines.json")

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
    f"{len(archs)} archetypes, {len(units)} units, {len(names) if tree is not None else 0} string resources, "
    f"{len(golden.get('cases', []))} golden cases"
)
