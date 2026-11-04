#!/usr/bin/env python3
"""Validate the curriculum outline in curriculum/ (stdlib only).

Phase 1 (curriculum/01-vision-architecture.md) defines the competencies, their target level per stage, and the
courses with their hour budgets (as revised in Phase 3). Phase 2 (curriculum/02-outline/S*.md) breaks every course into
modules and lessons; each lesson row gives its active hours, its number of practical sessions and its elapsed time.
This script checks that the two agree:

- IDs are well formed, unique, and nested correctly (course > module > lesson / exercise).
- Lesson hours add up to the module, module hours to the course; each course stays within +/-10 % of its Phase 1
  budget.
- Every course in Phase 1 is outlined (once all stage files exist), and course prerequisites exist and come earlier.
- Lesson prerequisites exist and come earlier; one from another course of the same stage needs that course as a
  (transitive) course prerequisite.
- Every competency referenced exists; by the end of each stage, the lessons reach every Phase 1 target level.
  Milestone components re-test competencies and don't count towards coverage.
- Practice: every level the lessons claim is backed by enough practical sessions (Phase 3 §4.3): the Phase 1 §10.3
  demonstrations plus the sessions needed to learn the step.
- Optional course sections group consecutive modules, cover every module once, and add up.
- Every module has at least one practical exercise and an assessment line; every competency is re-tested in at least
  one milestone or the capstone.
- Relative links and [S#] source references resolve.

The generated tables in curriculum/02-curriculum-outline.md (between `<!-- BEGIN generated:NAME -->` and
`<!-- END generated:NAME -->`) must match what this script produces; `--write` regenerates them.

Usage:
    python3 tools/validate_curriculum.py          # check
    python3 tools/validate_curriculum.py --write  # regenerate the tables in 02-curriculum-outline.md, then check
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "curriculum"
STAGES = ("S1", "S2", "S3", "S4", "S5")
DRIFT = 0.10
LESSON_HOURS = (0.5, 6)
#: Weekly study time (Phase 1 A2) used for the load table: low, planned, high.
WEEKLY = (5, 6.5, 8)
ACTIVE_WEEKS = 46
#: Practical sessions needed, counted over all lessons that move a competency, before a level counts as taught
#: (Phase 3 §4.3): the §10.3 demonstrations (L3 2, L4 3, L5 5) plus the learning sessions before them.
PRACTICE_NEEDED = {0: 0, 1: 0, 2: 1, 3: 3, 4: 5, 5: 8}
#: Phase 1 §6.6 share of exercises per support level (recipe, framework, brief), in percent.
SUPPORT_TARGET = {
    "S1": (70, 30, 0),
    "S2": (40, 50, 10),
    "S3": (15, 50, 35),
    "S4": (5, 35, 60),
    "S5": (0, 10, 90),
}
EXERCISE_TYPES = ("drill", "comparison", "guided", "framework", "brief", "project")
SUPPORT_LEVELS = ("recipe", "framework", "brief")

COMP = r"[A-Z]{2}-[A-Z]{3,4}"
COURSE = r"(?:[A-Z]{3}-\d|MS-S\d|CAP-5)"
COURSE_RE = re.compile(rf"^### ({COURSE}) — (.+) \((\d+(?:\.\d+)?) h\)$")
MODULE_RE = re.compile(rf"^#### ({COURSE}\.M\d+) — (.+) \((\d+(?:\.\d+)?) h\)$")
LESSON_RE = re.compile(
    rf"^\| ({COURSE}\.M\d+\.L\d+) \| (.+?) \| (.+?) \| (.+?) \| (\d+(?:\.\d+)?) \| (\d+) \| (.+?) \| (.+?) \|$"
)
ELAPSED_RE = re.compile(r"^(—|\d+(?:\.\d+)? (?:h|d|wk))$")
SECTION_RE = re.compile(rf"^\| ({COURSE}\.SEC\d+) \| (.+?) \| M(\d+)–M(\d+) \| (\d+(?:\.\d+)?) \|$")
EXERCISE_RE = re.compile(rf"^- `({COURSE}\.M\d+\.X\d+)` \*([a-z]+), ([a-z]+)\* — .+$")
PREREQ_RE = re.compile(r"^\*\*Prerequisites:\*\* (.+)$")
MOVE_RE = re.compile(rf"^({COMP})→([1-5])$")
GENERATED_RE = re.compile(r"(<!-- BEGIN generated:([a-z-]+) -->\n)(.*?)(<!-- END generated:\2 -->)", re.S)


@dataclass
class Lesson:
    id: str
    title: str
    prereqs: list[str]
    hours: float
    practice: int
    elapsed: str
    moves: dict[str, int]
    order: int


@dataclass
class Module:
    id: str
    title: str
    hours: float
    lessons: list[Lesson] = field(default_factory=list)
    exercises: list[tuple[str, str, str]] = field(default_factory=list)
    has_assessment: bool = False


@dataclass
class Course:
    id: str
    title: str
    hours: float
    stage: str
    file: str
    order: int
    prereqs: list[str] = field(default_factory=list)
    modules: list[Module] = field(default_factory=list)
    #: Optional sections: (id, title, first module number, last module number, hours).
    sections: list[tuple[str, str, int, int, float]] = field(default_factory=list)

    @property
    def is_milestone(self) -> bool:
        return self.id.startswith(("MS-", "CAP-"))


@dataclass
class Phase1:
    targets: dict[str, list[int | None]]
    budgets: dict[str, tuple[str, float]]  # course id -> (stage, hours)


@dataclass
class Report:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    def error(self, message: str) -> None:
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)


def number(text: str) -> float:
    return float(text)


def fmt(hours: float) -> str:
    return f"{hours:g}"


def parse_phase1(text: str, report: Report) -> Phase1:
    targets: dict[str, list[int | None]] = {}
    row = re.compile(rf"^\| ({COMP}) \| [^|]+\|" + r"\s*([–\d])\s*\|" * 5 + r"\s*(\d)\s*\|$", re.M)
    for m in row.finditer(text):
        levels = [None if v == "–" else int(v) for v in m.groups()[1:6]]
        targets[m.group(1)] = levels
    budgets: dict[str, tuple[str, float]] = {}
    for section in re.split(r"^### 7\.\d ", text, flags=re.M)[1:]:
        stage = section[:2]
        for m in re.finditer(rf"^\| ({COURSE}) \| [^|]+\| [^|]+\| (\d+(?:\.\d+)?) \|", section, re.M):
            budgets[m.group(1)] = (stage, number(m.group(2)))
    if not targets:
        report.error("01-vision-architecture.md: no competency table found")
    if not budgets:
        report.error("01-vision-architecture.md: no course tables found in §7")
    return Phase1(targets, budgets)


def split_ids(cell: str) -> list[str]:
    cell = cell.strip()
    if cell in ("—", "-", ""):
        return []
    return [part.strip() for part in cell.split(",") if part.strip()]


def parse_moves(cell: str, where: str, report: Report) -> dict[str, int]:
    moves: dict[str, int] = {}
    for part in split_ids(cell):
        m = MOVE_RE.match(part)
        if not m:
            report.error(f"{where}: bad competency move {part!r} (expected e.g. TS-KNF→2)")
            continue
        moves[m.group(1)] = max(moves.get(m.group(1), 0), int(m.group(2)))
    return moves


def parse_stage(path: Path, stage: str, start_order: int, report: Report) -> list[Course]:
    courses: list[Course] = []
    course: Course | None = None
    module: Module | None = None
    order = start_order
    for number_, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        where = f"{path.name}:{number_}"
        if m := COURSE_RE.match(line):
            course = Course(m.group(1), m.group(2), number(m.group(3)), stage, path.name, len(courses))
            courses.append(course)
            module = None
        elif m := MODULE_RE.match(line):
            if course is None or not m.group(1).startswith(course.id + "."):
                report.error(f"{where}: module {m.group(1)} is not inside its course")
                continue
            module = Module(m.group(1), m.group(2), number(m.group(3)))
            course.modules.append(module)
        elif m := LESSON_RE.match(line):
            if module is None or not m.group(1).startswith(module.id + "."):
                report.error(f"{where}: lesson {m.group(1)} is not inside its module")
                continue
            order += 1
            elapsed = m.group(7).strip()
            if not ELAPSED_RE.match(elapsed):
                report.error(f"{where}: bad elapsed time {elapsed!r} (expected — or e.g. 5 h, 2 d, 3 wk)")
            module.lessons.append(
                Lesson(
                    m.group(1),
                    m.group(2),
                    split_ids(m.group(4)),
                    number(m.group(5)),
                    int(m.group(6)),
                    elapsed,
                    parse_moves(m.group(8), where, report),
                    order,
                )
            )
        elif m := SECTION_RE.match(line):
            if course is None or module is not None or not m.group(1).startswith(course.id + "."):
                report.error(f"{where}: section {m.group(1)} is not in its course header")
                continue
            course.sections.append((m.group(1), m.group(2), int(m.group(3)), int(m.group(4)), number(m.group(5))))
        elif m := EXERCISE_RE.match(line):
            if module is None or not m.group(1).startswith(module.id + "."):
                report.error(f"{where}: exercise {m.group(1)} is not inside its module")
                continue
            if m.group(2) not in EXERCISE_TYPES:
                report.error(f"{where}: exercise type {m.group(2)!r} is not one of {', '.join(EXERCISE_TYPES)}")
            if m.group(3) not in SUPPORT_LEVELS:
                report.error(f"{where}: support level {m.group(3)!r} is not one of {', '.join(SUPPORT_LEVELS)}")
            module.exercises.append((m.group(1), m.group(2), m.group(3)))
        elif m := PREREQ_RE.match(line):
            if course is not None and module is None:
                course.prereqs = split_ids(m.group(1))
        elif line.startswith("**Assessment:**") and module is not None:
            module.has_assessment = True
        elif line.startswith("| ") and module is not None and re.match(rf"^\| {COURSE}\.M\d+\.L", line):
            report.error(f"{where}: malformed lesson row")
    return courses


def check_sections(course: Course, report: Report) -> None:
    """Sections group consecutive modules of one course; together they cover every module exactly once."""
    if not course.sections:
        return
    numbers = [int(m.id.rsplit(".M", 1)[1]) for m in course.modules]
    expected = 0
    for ident, _, first, last, hours in course.sections:
        if first != (numbers[expected] if expected < len(numbers) else None) or last < first or last not in numbers:
            report.error(f"{ident}: modules M{first}–M{last} don't continue from the previous section")
            return
        members = [m for m in course.modules if first <= int(m.id.rsplit(".M", 1)[1]) <= last]
        expected = numbers.index(last) + 1
        total = sum(m.hours for m in members)
        if abs(total - hours) > 1e-6:
            report.error(f"{ident}: modules add up to {fmt(total)} h, section says {fmt(hours)} h")
    if expected != len(numbers):
        report.error(f"{course.id}: sections don't cover every module")


def stage_files(root: Path) -> dict[str, Path]:
    found: dict[str, Path] = {}
    for stage in STAGES:
        matches = sorted((root / "02-outline").glob(f"{stage}-*.md"))
        if len(matches) > 1:
            raise SystemExit(f"more than one outline file for {stage}: {[m.name for m in matches]}")
        if matches:
            found[stage] = matches[0]
    return found


def check(root: Path) -> tuple[Report, dict[str, list[Course]], Phase1]:
    report = Report()
    phase1 = parse_phase1((root / "01-vision-architecture.md").read_text(encoding="utf-8"), report)
    files = stage_files(root)
    stages: dict[str, list[Course]] = {}
    order = 0
    for stage in STAGES:
        if stage not in files:
            report.warn(f"{stage}: no outline file yet")
            continue
        stages[stage] = parse_stage(files[stage], stage, order, report)
        order += 10_000

    seen: dict[str, str] = {}
    course_stage: dict[str, str] = {}
    lessons: dict[str, tuple[Lesson, Course]] = {}
    for stage, courses in stages.items():
        for course in courses:
            for ident in [course.id] + [sec[0] for sec in course.sections] + [m.id for m in course.modules] + [
                x for m in course.modules for x in [l.id for l in m.lessons] + [e[0] for e in m.exercises]
            ]:
                if ident in seen:
                    report.error(f"{ident}: duplicate ID (also in {seen[ident]})")
                seen[ident] = course.file
            course_stage[course.id] = stage
            for module in course.modules:
                for lesson in module.lessons:
                    lessons[lesson.id] = (lesson, course)

    # Hours and Phase 1 budgets.
    for stage, courses in stages.items():
        for course in courses:
            if not course.modules:
                report.error(f"{course.id}: has no modules")
            module_sum = sum(m.hours for m in course.modules)
            if abs(module_sum - course.hours) > 1e-6:
                report.error(f"{course.id}: modules add up to {fmt(module_sum)} h, heading says {fmt(course.hours)} h")
            for module in course.modules:
                lesson_sum = sum(l.hours for l in module.lessons)
                if not module.lessons:
                    report.error(f"{module.id}: has no lessons")
                elif abs(lesson_sum - module.hours) > 1e-6:
                    report.error(f"{module.id}: lessons add up to {fmt(lesson_sum)} h, heading says {fmt(module.hours)} h")
                if not course.is_milestone:
                    if not module.exercises:
                        report.error(f"{module.id}: has no practical exercise")
                    if not module.has_assessment:
                        report.error(f"{module.id}: has no **Assessment:** line")
                for lesson in module.lessons:
                    if not LESSON_HOURS[0] <= lesson.hours <= LESSON_HOURS[1]:
                        report.warn(f"{lesson.id}: {fmt(lesson.hours)} h is outside the 0.5–6 h lesson range")
                    if lesson.practice and lesson.hours < 0.5 * lesson.practice:
                        report.error(f"{lesson.id}: {lesson.practice} practical sessions don't fit in {fmt(lesson.hours)} h")
            check_sections(course, report)
            budget = phase1.budgets.get(course.id)
            if budget is None:
                report.error(f"{course.id}: not in the Phase 1 course tables")
                continue
            if budget[0] != stage:
                report.error(f"{course.id}: Phase 1 puts it in {budget[0]}, the outline in {stage}")
            if abs(course.hours - budget[1]) > DRIFT * budget[1] + 1e-6:
                report.error(f"{course.id}: {fmt(course.hours)} h drifts more than 10 % from Phase 1 ({fmt(budget[1])} h)")
    if len(stages) == len(STAGES):
        outlined = set(course_stage)
        for missing in sorted(set(phase1.budgets) - outlined):
            report.error(f"{missing}: in Phase 1 but not outlined")

    # Course prerequisites.
    position = {c.id: (STAGES.index(s), c.order) for s, cs in stages.items() for c in cs}
    closure: dict[str, set[str]] = {}

    def requires(course_id: str) -> set[str]:
        if course_id in closure:
            return closure[course_id]
        closure[course_id] = set()
        course = next(c for cs in stages.values() for c in cs if c.id == course_id)
        result: set[str] = set()
        for prereq in course.prereqs:
            if prereq in position:
                result |= {prereq} | requires(prereq)
        closure[course_id] = result
        return result

    for stage, courses in stages.items():
        for course in courses:
            for prereq in course.prereqs:
                m = re.match(r"^MS-(S\d)$", prereq)
                if m:
                    if STAGES.index(m.group(1)) >= STAGES.index(stage) and not course.is_milestone:
                        report.error(f"{course.id}: prerequisite {prereq} is not from an earlier stage")
                    continue
                if prereq not in position:
                    if prereq in phase1.budgets and phase1.budgets[prereq][0] != stage:
                        continue  # an earlier/later stage whose file doesn't exist yet
                    report.error(f"{course.id}: unknown prerequisite {prereq}")
                elif position[prereq] >= position[course.id]:
                    report.error(f"{course.id}: prerequisite {prereq} comes later in the outline")

    # Lesson prerequisites.
    for stage, courses in stages.items():
        for course in courses:
            allowed = requires(course.id) | {course.id}
            for module in course.modules:
                for lesson in module.lessons:
                    for prereq in lesson.prereqs:
                        target_course = prereq.split(".")[0]
                        if prereq in lessons:
                            other, other_course = lessons[prereq]
                            if other.order >= lesson.order:
                                report.error(f"{lesson.id}: prerequisite {prereq} comes later")
                            elif course_stage[other_course.id] == stage and other_course.id not in allowed:
                                report.error(f"{lesson.id}: prerequisite {prereq} is in {other_course.id}, which {course.id} doesn't require")
                        elif re.fullmatch(COURSE, prereq) and prereq in position:
                            if position[prereq] >= position[course.id]:
                                report.error(f"{lesson.id}: prerequisite {prereq} comes later")
                        elif target_course in position or target_course in phase1.budgets:
                            report.error(f"{lesson.id}: unknown prerequisite {prereq}")
                        else:
                            report.error(f"{lesson.id}: prerequisite {prereq!r} is not a lesson or course ID")
                    for comp in lesson.moves:
                        if comp not in phase1.targets:
                            report.error(f"{lesson.id}: unknown competency {comp}")

    # Coverage and practice.
    for index, (stage, best, practice) in enumerate(progress(stages, phase1)):
        if stage not in stages:
            continue
        for comp, levels in phase1.targets.items():
            target = levels[index]
            if target is not None and best[comp] < target:
                report.error(f"coverage: {comp} reaches {best[comp]} by the end of {stage}, target {target}")
            needed = PRACTICE_NEEDED[best[comp]]
            if practice[comp] < needed:
                report.error(
                    f"practice: {comp} is taught to level {best[comp]} by the end of {stage} with {practice[comp]} "
                    f"practical session(s); level {best[comp]} needs {needed}"
                )

    # Every competency is re-tested at a milestone or the capstone, once all stages exist.
    if len(stages) == len(STAGES):
        retested = {
            comp
            for courses in stages.values()
            for course in courses
            if course.is_milestone
            for module in course.modules
            for lesson in module.lessons
            for comp in lesson.moves
        }
        for comp in sorted(set(phase1.targets) - retested):
            report.error(f"assessment: {comp} is not re-tested in any milestone or the capstone")

    # Sources and links.
    sources = set(re.findall(r"^\| (S\d+) \|", (root / "sources.md").read_text(encoding="utf-8"), re.M))
    for path in sorted(root.rglob("*.md")):
        text = path.read_text(encoding="utf-8")
        for cited in set(re.findall(r"\[(S\d+)\]", text)):
            if cited not in sources:
                report.error(f"{path.relative_to(root)}: source [{cited}] is not in sources.md")
        for link in re.findall(r"\]\(([^)#:\s]+)(?:#[^)]*)?\)", text):
            if not (path.parent / link).exists():
                report.error(f"{path.relative_to(root)}: broken link {link}")
    return report, stages, phase1


def progress(stages: dict[str, list[Course]], phase1: Phase1) -> list[tuple[str, dict[str, int], dict[str, int]]]:
    """Per stage, the highest level each competency is taught to and the practical sessions behind it, cumulatively.

    Milestone components re-test competencies; they count towards neither.
    """
    best = dict.fromkeys(phase1.targets, 0)
    practice = dict.fromkeys(phase1.targets, 0)
    result = []
    for stage in STAGES:
        for course in stages.get(stage, []):
            if course.is_milestone:
                continue
            for module in course.modules:
                for lesson in module.lessons:
                    for comp, level in lesson.moves.items():
                        if comp in best:
                            best[comp] = max(best[comp], level)
                            practice[comp] += lesson.practice
        result.append((stage, dict(best), dict(practice)))
    return result


def generated_tables(stages: dict[str, list[Course]], phase1: Phase1) -> dict[str, str]:
    lines = [
        "| Stage | Course | Title | Modules | Lessons | Practical sessions | Budget h | Outline h | Drift |",
        "|---|---|---|---|---|---|---|---|---|",
    ]
    totals = {"modules": 0, "lessons": 0, "practice": 0, "p1": 0.0, "p2": 0.0}
    for stage, courses in stages.items():
        for course in courses:
            lesson_count = sum(len(m.lessons) for m in course.modules)
            practice = sum(l.practice for m in course.modules for l in m.lessons)
            p1 = phase1.budgets.get(course.id, (stage, 0.0))[1]
            drift = course.hours - p1
            lines.append(
                f"| {stage} | `{course.id}` | {course.title} | {len(course.modules)} | {lesson_count} | {practice} | {fmt(p1)} | "
                f"{fmt(course.hours)} | {'±0' if drift == 0 else f'{drift:+g}'} |"
            )
            totals["modules"] += len(course.modules)
            totals["lessons"] += lesson_count
            totals["practice"] += practice
            totals["p1"] += p1
            totals["p2"] += course.hours
    lines.append(
        f"| | | **Total** | **{totals['modules']}** | **{totals['lessons']}** | **{totals['practice']}** | "
        f"**{fmt(totals['p1'])}** | **{fmt(totals['p2'])}** | |"
    )
    summary = "\n".join(lines)

    steps = progress(stages, phase1)
    reached = {comp: [best[comp] for _, best, _ in steps] for comp in phase1.targets}
    sessions = {comp: [practice[comp] for _, _, practice in steps] for comp in phase1.targets}
    cov = ["| Competency | " + " | ".join(STAGES) + " | Practical sessions |", "|---|" + "---|" * (len(STAGES) + 1)]
    for comp, levels in phase1.targets.items():
        cells = []
        for index, stage in enumerate(STAGES):
            target = levels[index]
            if stage not in stages:
                cells.append("·")
            elif target is None:
                cells.append(f"{reached[comp][index]} / –" if reached[comp][index] else "–")
            else:
                mark = "" if reached[comp][index] >= target else " ✗"
                cells.append(f"{reached[comp][index]} / {target}{mark}")
        last = max((i for i, s in enumerate(STAGES) if s in stages), default=0)
        cells.append(f"{sessions[comp][last]} (level {reached[comp][last]} needs {PRACTICE_NEEDED[reached[comp][last]]})")
        cov.append(f"| `{comp}` | " + " | ".join(cells) + " |")
    coverage = "\n".join(cov)

    sup = ["| Stage | Exercises | Recipe | Framework | Brief | Phase 1 §6.6 target |", "|---|---|---|---|---|---|"]
    for stage, courses in stages.items():
        counts = dict.fromkeys(SUPPORT_LEVELS, 0)
        for course in courses:
            for module in course.modules:
                for _, _, support in module.exercises:
                    counts[support] += 1
        total = sum(counts.values()) or 1
        pct = [round(100 * counts[s] / total) for s in SUPPORT_LEVELS]
        target = SUPPORT_TARGET[stage]
        sup.append(
            f"| {stage} | {sum(counts.values())} | {pct[0]} % | {pct[1]} % | {pct[2]} % | "
            f"{target[0]} / {target[1]} / {target[2]} % |"
        )
    support = "\n".join(sup)

    load = [
        "| Stage | Lessons | Active h | Practical sessions | Lessons with ≥ 1 d elapsed | "
        + " | ".join(f"Weeks at {fmt(w)} h" for w in WEEKLY)
        + " |",
        "|---|---|---|---|---|" + "---|" * len(WEEKLY),
    ]
    total_hours = 0.0
    for stage, courses in stages.items():
        all_lessons = [l for c in courses for m in c.modules for l in m.lessons]
        hours = sum(c.hours for c in courses)
        total_hours += hours
        long_waits = sum(1 for l in all_lessons if l.elapsed.endswith((" d", " wk")))
        load.append(
            f"| {stage} | {len(all_lessons)} | {fmt(hours)} | {sum(l.practice for l in all_lessons)} | {long_waits} | "
            + " | ".join(str(round(hours / w)) for w in WEEKLY)
            + " |"
        )
    years = " | ".join(f"**{round(total_hours / w)}** (≈ {total_hours / w / ACTIVE_WEEKS:.1f} yr)" for w in WEEKLY)
    load.append(f"| **Core** | | **{fmt(total_hours)}** | | | {years} |")
    return {"summary": summary, "coverage": coverage, "support": support, "load": "\n".join(load)}


def sync_generated(path: Path, tables: dict[str, str], write: bool, report: Report) -> None:
    if not path.exists():
        report.warn(f"{path.name}: missing, so the generated tables aren't checked")
        return
    text = path.read_text(encoding="utf-8")
    names = set()

    def replace(m: re.Match[str]) -> str:
        names.add(m.group(2))
        if m.group(2) not in tables:
            report.error(f"{path.name}: unknown generated block {m.group(2)!r}")
            return m.group(0)
        return m.group(1) + tables[m.group(2)] + "\n" + m.group(4)

    updated = GENERATED_RE.sub(replace, text)
    for missing in sorted(set(tables) - names):
        report.error(f"{path.name}: no generated block {missing!r}")
    if updated != text:
        if write:
            path.write_text(updated, encoding="utf-8")
        else:
            report.error(f"{path.name}: generated tables are out of date (run with --write)")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--write", action="store_true", help="regenerate the tables in 02-curriculum-outline.md")
    parser.add_argument("--root", type=Path, default=ROOT, help=argparse.SUPPRESS)
    args = parser.parse_args(argv)
    report, stages, phase1 = check(args.root)
    sync_generated(args.root / "02-curriculum-outline.md", generated_tables(stages, phase1), args.write, report)
    for warning in report.warnings:
        print(f"warning: {warning}")
    for error in report.errors:
        print(f"error: {error}", file=sys.stderr)
    if report.errors:
        print(f"Curriculum: {len(report.errors)} error(s)", file=sys.stderr)
        return 1
    courses = [c for cs in stages.values() for c in cs]
    modules = sum(len(c.modules) for c in courses)
    lessons = sum(len(m.lessons) for c in courses for m in c.modules)
    hours = sum(c.hours for c in courses)
    print(f"Curriculum OK — {len(stages)} stage(s), {len(courses)} courses, {modules} modules, {lessons} lessons, {fmt(hours)} h")
    return 0


if __name__ == "__main__":
    sys.exit(main())
