#!/usr/bin/env python3
"""Export curriculum/ to the app's content assets (curriculum Phase 7, spec §7.1).

Reads the outline, the course files (knowledge checks), the lesson files (lesson and exercise text), the competency
entries, the milestone rubrics and the second reader's worksheet, and writes:

  data/assets/curriculum/index.json          stages, courses, modules, lesson and exercise titles, competencies
  data/assets/curriculum/courses/<ID>.json   one course: introduction, lessons, exercises, knowledge-check items

Lesson text is exported as blocks (paragraph, list, table), so the app needs no Markdown parser; only inline marks
(`**bold**`, `*italic*`, `` `code` ``) stay in the strings. The files are committed; `tools/validate_data.py` fails
when they are out of date.

Usage: python3 tools/export_curriculum.py [--write]
  --write  regenerate the files (otherwise only check that they are up to date)
Exit code 0 = up to date (or written), 1 = out of date or the curriculum has errors.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import validate_curriculum as vc

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "data" / "assets" / "curriculum"
SCHEMA_VERSION = 1

ITEM_TYPES = {
    "multiple choice": "MULTIPLE_CHOICE",
    "scenario": "SCENARIO",
    "calculation": "CALCULATION",
    "image identification": "IMAGE_IDENTIFICATION",
    "ordering": "ORDERING",
}

STAGE_H1_RE = re.compile(r"^# (S\d) — (.+?)(?: \(\*(.+?)\*\))?, [\d.]+ h$")
STAGE_QUESTION_RE = re.compile(r"^\*\*Stage question:\*\* (.+)$")
LIST_RE = re.compile(r"^(\s*)(- |\d+\. )(.*)$")
LINK_RE = re.compile(r"\[([^\]]+)\]\([^)]+\)")
OPTION_RE = re.compile(r"(?:^|\s)([A-F])\) ")
TABLE_RULE_RE = re.compile(r"^\|[-:| ]+\|$")


def inline(text: str) -> str:
    """Links become their text: the targets are repository paths, which mean nothing in the app."""
    return LINK_RE.sub(r"\1", text).strip()


def cells(line: str) -> list[str]:
    return [inline(c) for c in line.strip().strip("|").split("|")]


def blocks(lines: list[str]) -> list[dict]:
    """The Markdown subset of the lesson files as blocks.

    - `{"type": "paragraph", "text"}`: wrapped lines joined by a space;
    - `{"type": "list", "items": [{"level", "marker", "text"}]}`: `marker` is "" for a bullet and "1." for a number;
    - `{"type": "table", "header": [...], "rows": [[...]]}`.
    Generated blocks and HTML comments are left out.
    """
    out: list[dict] = []
    paragraph: list[str] = []
    items: list[dict] | None = None
    table: list[str] = []
    generated = False

    def flush() -> None:
        nonlocal paragraph, items, table
        if paragraph:
            out.append({"type": "paragraph", "text": inline(" ".join(paragraph))})
            paragraph = []
        if items:
            for item in items:
                item["text"] = inline(item["text"])
            out.append({"type": "list", "items": items})
        items = None
        if table:
            rows = [cells(row) for row in table if not TABLE_RULE_RE.match(row.strip())]
            out.append({"type": "table", "header": rows[0], "rows": rows[1:]})
            table = []

    for line in lines:
        if line.startswith("<!-- BEGIN generated:"):
            generated = True
            continue
        if line.startswith("<!-- END generated:"):
            generated = False
            continue
        if generated or line.startswith("<!--"):
            continue
        if not line.strip():
            flush()
            continue
        if line.lstrip().startswith("|"):
            if not table:
                flush()
            table.append(line)
            continue
        if m := LIST_RE.match(line):
            if items is None:
                flush()
                items = []
            indent = len(m.group(1))
            level = 0 if indent == 0 else (1 if indent <= 4 else 2)
            marker = "" if m.group(2) == "- " else m.group(2).strip()
            items.append({"level": level, "marker": marker, "text": m.group(3).strip()})
            continue
        if items is not None and line.startswith(" "):
            items[-1]["text"] += " " + line.strip()
            continue
        if items is not None or table:
            flush()
        paragraph.append(line.strip())
    flush()
    return out


def split_lesson_file(path: Path) -> tuple[list[str], dict[str, list[str]], dict[str, list[str]]]:
    """A lesson file as (introduction lines, module id -> its own lines, unit id -> lines)."""
    intro: list[str] = []
    modules: dict[str, list[str]] = {}
    units: dict[str, list[str]] = {}
    target = intro
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("# "):
            continue
        if m := vc.LESSON_DOC_MODULE_RE.match(line):
            target = modules.setdefault(m.group(1), [])
            continue
        if m := vc.LESSON_DOC_UNIT_RE.match(line):
            target = units.setdefault(m.group(1), [])
            continue
        target.append(line)
    return intro, modules, units


def without_status(intro: list[dict]) -> list[dict]:
    """The status line is shown by the app from `status`, so its paragraph is dropped from the introduction."""
    return [b for b in intro if not (b["type"] == "paragraph" and b["text"].startswith("**Status:**"))]


def item_json(item: vc.Item) -> dict:
    out = {
        "id": item.id,
        "type": ITEM_TYPES[item.type],
        "lessonId": item.lesson,
        "safety": item.safety,
        "question": inline(item.question),
        "options": [],
        "answer": inline(item.answer),
        "why": inline(item.why),
    }
    if item.type == "multiple choice":
        marks = list(OPTION_RE.finditer(item.question))
        if not marks or marks[0].group(1) != "A" or not re.fullmatch(r"[A-F]", item.answer.strip()):
            raise ValueError(f"{item.id}: a multiple-choice item needs options from A) and a one-letter answer")
        out["question"] = inline(item.question[: marks[0].start()])
        for mark, following in zip(marks, marks[1:] + [None]):
            end = following.start() if following else len(item.question)
            out["options"].append({"key": mark.group(1), "text": inline(item.question[mark.end() : end])})
        if out["answer"] not in [o["key"] for o in out["options"]]:
            raise ValueError(f"{item.id}: answer {out['answer']} is not one of its options")
    return out


def stage_meta(root: Path) -> dict[str, dict]:
    meta: dict[str, dict] = {}
    for stage, path in vc.stage_files(root).items():
        entry = {"id": stage, "title": stage, "titleNl": "", "question": ""}
        lines = vc.joined_lines(path.read_text(encoding="utf-8"))
        for _, line in lines:
            if m := STAGE_H1_RE.match(line):
                entry["title"], entry["titleNl"] = m.group(2), m.group(3) or ""
            elif (m := STAGE_QUESTION_RE.match(line)) and not entry["question"]:
                entry["question"] = m.group(1)
        meta[stage] = entry
    return meta


def export(root: Path = vc.ROOT) -> dict[str, str]:
    """Every output file as relative path -> text; raises ValueError when the curriculum itself has errors."""
    report, stages, _phase1, phase4, phase5 = vc.check(root)
    if report.errors:
        raise ValueError("the curriculum has errors; run tools/validate_curriculum.py first:\n" + "\n".join(report.errors[:10]))
    files: dict[str, str] = {}
    stage_info = stage_meta(root)
    index_courses: list[dict] = []
    for stage, courses in stages.items():
        for course in courses:
            doc = phase5.docs.get(course.id)
            lesson_doc = phase5.lesson_docs.get(course.id)
            kind = "CAPSTONE" if course.id.startswith("CAP-") else ("MILESTONE" if course.is_milestone else "COURSE")
            safety_ids = [i.id for items in (doc.items.values() if doc else []) for i in items if i.safety]
            entry = {
                "id": course.id,
                "stage": stage,
                "kind": kind,
                "title": course.title,
                "hours": course.hours,
                "purpose": inline(course.purpose),
                "prerequisites": course.prereqs,
                "status": lesson_doc.status.upper() if lesson_doc else None,
                "hasContent": lesson_doc is not None,
                "safetyItems": len(safety_ids),
                "safetyItemsRead": sum(1 for i in safety_ids if phase5.review.get(i, ("", ""))[1]),
                "modules": [],
                "rubric": [
                    {
                        "id": row.id,
                        "criterion": inline(row.criterion),
                        "competencies": row.competencies,
                        "evidence": row.evidence,
                        "weight": row.weight,
                    }
                    for row in phase4.rubric.get(course.id, [])
                ],
            }
            intro, module_lines, unit_lines = split_lesson_file(lesson_doc.path) if lesson_doc else ([], {}, {})
            course_modules: list[dict] = []
            for module in course.modules:
                items = doc.items.get(module.id, []) if doc else []
                entry["modules"].append(
                    {
                        "id": module.id,
                        "title": module.title,
                        "hours": module.hours,
                        "lessons": [
                            {
                                "id": lesson.id,
                                "title": lesson.title,
                                "hours": lesson.hours,
                                "practiceSessions": lesson.practice,
                                "objectives": inline(lesson.objectives),
                            }
                            for lesson in module.lessons
                        ],
                        "exercises": [
                            {
                                "id": ident,
                                "title": (doc.exercises[ident].title if doc and ident in doc.exercises else inline(module.descriptions.get(ident, ident))),
                                "kind": kind_,
                                "support": support.upper(),
                            }
                            for ident, kind_, support in module.exercises
                        ],
                        "checkItems": len(items),
                        "safetyCheckItems": sum(1 for i in items if i.safety),
                    }
                )
                if lesson_doc:
                    course_modules.append(
                        {
                            "id": module.id,
                            "title": module.title,
                            "note": blocks(module_lines.get(module.id, [])),
                            "lessons": [
                                {
                                    "id": lesson.id,
                                    "title": lesson.title,
                                    "prerequisites": lesson.prereqs,
                                    "competencies": [{"id": c, "level": level} for c, level in lesson.moves.items()],
                                    "blocks": blocks(unit_lines[lesson.id]),
                                }
                                for lesson in module.lessons
                            ],
                            "exercises": [
                                {
                                    "id": ident,
                                    "title": doc.exercises[ident].title,
                                    "description": inline(module.descriptions.get(ident, "")),
                                    "competencies": [{"id": c, "level": level} for c, level in doc.exercises[ident].tags.items()],
                                    "blocks": blocks(unit_lines[ident]),
                                }
                                for ident, _kind, _support in module.exercises
                            ],
                            "items": [item_json(i) for i in items],
                        }
                    )
            index_courses.append(entry)
            if lesson_doc:
                files[f"courses/{course.id}.json"] = dump(
                    {
                        "schemaVersion": SCHEMA_VERSION,
                        "id": course.id,
                        "introduction": without_status(blocks(intro)),
                        "modules": course_modules,
                    }
                )
    files["index.json"] = dump(
        {
            "schemaVersion": SCHEMA_VERSION,
            "stages": [
                {**stage_info[stage], "courseIds": [c.id for c in courses]} for stage, courses in stages.items()
            ],
            "courses": index_courses,
            "competencies": [{"id": e.id, "title": e.title} for e in phase4.entries.values()],
        }
    )
    return files


def dump(doc: dict) -> str:
    return json.dumps(doc, ensure_ascii=False, indent=1) + "\n"


def stale(files: dict[str, str], out: Path = OUT) -> list[str]:
    """Relative paths that differ from, are missing from, or are extra in [out]."""
    wrong = [rel for rel, text in files.items() if not (out / rel).is_file() or (out / rel).read_text(encoding="utf-8") != text]
    if out.is_dir():
        wrong += [str(p.relative_to(out)) for p in sorted(out.rglob("*.json")) if str(p.relative_to(out)) not in files]
    return wrong


def write(files: dict[str, str], out: Path = OUT) -> None:
    for rel in stale(files, out):
        path = out / rel
        if rel in files:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(files[rel], encoding="utf-8")
        else:
            path.unlink()


def main(argv: list[str] | None = None) -> int:
    args = sys.argv[1:] if argv is None else argv
    try:
        files = export()
    except ValueError as e:
        print(f"error: {e}", file=sys.stderr)
        return 1
    if "--write" in args:
        write(files)
    wrong = stale(files)
    if wrong:
        print(f"error: {len(wrong)} curriculum asset(s) out of date, e.g. {wrong[0]} (run tools/export_curriculum.py --write)", file=sys.stderr)
        return 1
    size = sum(len(text.encode("utf-8")) for text in files.values())
    print(f"curriculum assets OK — {len(files) - 1} course file(s), {size / 1_000_000:.1f} MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
