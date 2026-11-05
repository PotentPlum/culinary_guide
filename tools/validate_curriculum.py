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

Phase 4 (curriculum/04-competencies/*.md, 04-assessment.md) adds:
- One entry per competency, in the file for its domain, with every required field and success criteria for each level
  from 2 up to the core target (no more); each level names its evidence, at least one source objective or partly
  verifiable (Phase 1 §10.2).
- Milestone and capstone rubrics: criteria from the fixed list, weights summing to 100, food safety as a gate, every
  competency a component re-tests scored by a row for that component (and nothing else), and objective, partly
  verifiable and self-assessed evidence in every rubric.

Phase 5 (curriculum/05-course-development.md, 05-courses/<stage>/<course>.md) adds:
- Every course of a developed stage has a file; each file has the fixed sections, an outline for every lesson (headed by
  the outline's title, italics aside, with a numbered session plan matching the outline's practical sessions) and a
  spec for every exercise, and nothing else.
- Exercise tags (`TS-KNF→3`): competencies the module's lessons move, at level 2 up to the level the module reaches.
- Module checks: knowledge-check items within the blueprint (assessment §4.1), every [K] lesson tested, a safety item
  wherever the module moves `SA-*`; evidence rows for every competency the module moves to level 2 or more, at the
  highest level it reaches, against a Phase 4 success row and an exercise tagged at that level.
- Every course outcome from level 2 is practised in an exercise; equipment stays within the stage's tier; every
  developed stage target is evidenced by an exercise at that level.
- Every exercise ID named in a course file is an exercise of the outline, with a spec once its stage is developed;
  every module and lesson ID named is in the outline, and a named lesson has its outline once its stage is developed.

The second reader's worksheet (curriculum/06-safety-review.md), once it exists, holds every safety item of the course
files exactly once, under its course's heading and word for word (so a later edit to an item shows as drift), each
mapped to the rows of assessment §9 it rests on (by the Topic up to its first bracket) or to a course rule
(`— <lesson ID in backticks>`); a verdict is empty, `agrees` or `disagrees`, and a disagreement has a note.

The generated blocks (between `<!-- BEGIN generated:NAME -->` and `<!-- END generated:NAME -->`) in
02-curriculum-outline.md, 04-competency-framework.md, 04-assessment.md, 04-competencies/*.md,
05-course-development.md, 05-courses/*/*.md and 06-safety-review.md must match what this script produces; `--write` regenerates them.

Usage:
    python3 tools/validate_curriculum.py          # check
    python3 tools/validate_curriculum.py --write  # regenerate the generated blocks, then check
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
#: Competency domains (Phase 1 §5.1), in order; each has a file 04-competencies/<code>.md.
DOMAINS = ("CK", "IN", "TS", "SC", "FL", "KM", "SA", "RD", "JD", "CR")
#: Required fields of a competency entry (Phase 4), in order.
ENTRY_FIELDS = (
    "Elements",
    "Knowledge",
    "Practical ability and judgment",
    "Independence",
    "Assessment",
    "Success criteria",
    "Not yet",
)
#: Evidence methods and their category (Phase 1 §10.2): objective, partly verifiable, self-assessed.
EVIDENCE = {
    "quiz": "O",
    "measure": "O",
    "calibration": "O",
    "log": "O",
    "photo": "P",
    "video": "P",
    "written": "P",
    "form": "S",
    "checklist": "S",
    "tasters": "S",
}
#: Rubric criteria: the ten from the brief, plus knowledge, planning and reflection.
CRITERIA = (
    "technical execution",
    "accuracy",
    "timing",
    "consistency",
    "texture",
    "flavour",
    "presentation",
    "organisation",
    "food safety",
    "independent decisions",
    "knowledge",
    "planning",
    "reflection",
)

COMP = r"[A-Z]{2}-[A-Z]{3,4}"
COURSE = r"(?:[A-Z]{3}-\d|MS-S\d|CAP-5)"
COURSE_RE = re.compile(rf"^### ({COURSE}) — (.+) \((\d+(?:\.\d+)?) h\)$")
MODULE_RE = re.compile(rf"^#### ({COURSE}\.M\d+) — (.+) \((\d+(?:\.\d+)?) h\)$")
LESSON_RE = re.compile(
    rf"^\| ({COURSE}\.M\d+\.L\d+) \| (.+?) \| (.+?) \| (.+?) \| (\d+(?:\.\d+)?) \| (\d+) \| (.+?) \| (.+?) \|$"
)
ELAPSED_RE = re.compile(r"^(—|\d+(?:\.\d+)? (?:h|d|wk))$")
SECTION_RE = re.compile(rf"^\| ({COURSE}\.SEC\d+) \| (.+?) \| M(\d+)–M(\d+) \| (\d+(?:\.\d+)?) \|$")
EXERCISE_RE = re.compile(rf"^- `({COURSE}\.M\d+\.X\d+)` \*([a-z]+), ([a-z]+)\* — (.+)$")
PREREQ_RE = re.compile(r"^\*\*Prerequisites:\*\* (.+)$")
MOVE_RE = re.compile(rf"^({COMP})→([1-5])$")
GENERATED_RE = re.compile(r"(<!-- BEGIN generated:([a-z0-9-]+) -->\n)(.*?)(<!-- END generated:\2 -->)", re.S)
OUTCOME_RE = re.compile(r"^- \[([KSJM])\] (.+?)(?: `([^`]+)`)?$")
ENTRY_RE = re.compile(rf"^### ({COMP}) — (.+)$")
FIELD_RE = re.compile(r"^\*\*([A-Za-z ]+):\*\*")
SUCCESS_RE = re.compile(r"^\| ([1-5]) \| (.+?) \| (.+?) \|$")
RUBRIC_RE = re.compile(r"^\| ((?:MS-S\d|CAP-5)\.R\d+) \| (.+?) \| (.+?) \| (.+?) \| (.+?) \| (.+?) \| (\d+|gate) \|$")


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
    objectives: str = ""


@dataclass
class Module:
    id: str
    title: str
    hours: float
    lessons: list[Lesson] = field(default_factory=list)
    exercises: list[tuple[str, str, str]] = field(default_factory=list)
    has_assessment: bool = False
    scope: str = ""
    objectives: str = ""
    assessment: str = ""
    #: Exercise descriptions from the outline, by exercise ID.
    descriptions: dict[str, str] = field(default_factory=dict)


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
    purpose: str = ""
    equipment: str = ""
    #: Outcomes: (dimension tag, text, moves).
    outcomes: list[tuple[str, str, dict[str, int]]] = field(default_factory=list)
    completion: str = ""

    @property
    def is_milestone(self) -> bool:
        return self.id.startswith(("MS-", "CAP-"))


@dataclass
class Phase1:
    targets: dict[str, list[int | None]]
    budgets: dict[str, tuple[str, float]]  # course id -> (stage, hours)
    names: dict[str, str] = field(default_factory=dict)  # competency id -> Phase 1 description


@dataclass
class Entry:
    """A Phase 4 competency entry."""

    id: str
    title: str
    file: str
    fields: list[str] = field(default_factory=list)
    success: dict[int, list[str]] = field(default_factory=dict)  # level -> evidence methods


@dataclass
class RubricRow:
    id: str
    criterion: str
    components: list[str]
    competencies: list[str]
    evidence: list[str]
    weight: int | None  # None: a gate (pass/fail, non-compensable)


@dataclass
class Phase4:
    entries: dict[str, Entry] = field(default_factory=dict)
    domain_files: dict[str, Path] = field(default_factory=dict)
    rubric: dict[str, list[RubricRow]] = field(default_factory=dict)  # milestone id -> rows


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
    names: dict[str, str] = {}
    row = re.compile(rf"^\| ({COMP}) \| ([^|]+)\|" + r"\s*([–\d])\s*\|" * 5 + r"\s*(\d)\s*\|$", re.M)
    for m in row.finditer(text):
        levels = [None if v == "–" else int(v) for v in m.groups()[2:7]]
        targets[m.group(1)] = levels
        names[m.group(1)] = m.group(2).strip()
    budgets: dict[str, tuple[str, float]] = {}
    for section in re.split(r"^### 7\.\d ", text, flags=re.M)[1:]:
        stage = section[:2]
        for m in re.finditer(rf"^\| ({COURSE}) \| [^|]+\| [^|]+\| (\d+(?:\.\d+)?) \|", section, re.M):
            budgets[m.group(1)] = (stage, number(m.group(2)))
    if not targets:
        report.error("01-vision-architecture.md: no competency table found")
    if not budgets:
        report.error("01-vision-architecture.md: no course tables found in §7")
    return Phase1(targets, budgets, names)


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
    for number_, line in joined_lines(path.read_text(encoding="utf-8")):
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
                    m.group(3).strip(),
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
            module.descriptions[m.group(1)] = m.group(4).strip()
        elif m := PREREQ_RE.match(line):
            if course is not None and module is None:
                course.prereqs = split_ids(m.group(1))
        elif line.startswith("**Assessment:**") and module is not None:
            module.has_assessment = True
            module.assessment = line.removeprefix("**Assessment:**").strip()
        elif course is not None and module is None and line.startswith("**Purpose:**"):
            course.purpose = line.removeprefix("**Purpose:**").strip()
        elif course is not None and module is None and line.startswith("**Equipment:**"):
            course.equipment = line.removeprefix("**Equipment:**").strip()
        elif course is not None and module is None and line.startswith("**Competencies on completion:**"):
            course.completion = line.removeprefix("**Competencies on completion:**").strip()
        elif course is not None and module is None and (m := OUTCOME_RE.match(line)):
            course.outcomes.append((m.group(1), m.group(2).strip(), parse_moves(m.group(3) or "", where, report)))
        elif module is not None and line.startswith("**Scope:**"):
            module.scope = line.removeprefix("**Scope:**").strip()
        elif module is not None and line.startswith("**Objectives:**"):
            module.objectives = line.removeprefix("**Objectives:**").strip()
        elif line.startswith("| ") and module is not None and re.match(rf"^\| {COURSE}\.M\d+\.L", line):
            report.error(f"{where}: malformed lesson row")
    return courses


#: Outline lines whose text may wrap onto the following lines.
WRAPPING = ("**Purpose:**", "**Equipment:**", "**Scope:**", "**Objectives:**", "**Assessment:**",
            "**Competencies on completion:**", "- [", "- `")


def joined_lines(text: str) -> list[tuple[int, str]]:
    """The lines of an outline file, with wrapped field and list lines joined (numbered by their first line)."""
    result: list[tuple[int, str]] = []
    for number_, line in enumerate(text.splitlines(), 1):
        if (
            result
            and line.strip()
            and not line.startswith(("**", "- ", "|", "#", "<!--", "---"))
            and result[-1][1].startswith(WRAPPING)
            and result[-1][1].strip()
        ):
            result[-1] = (result[-1][0], result[-1][1].rstrip() + " " + line.strip())
        else:
            result.append((number_, line))
    return result


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


def check(root: Path) -> tuple[Report, dict[str, list[Course]], Phase1, Phase4, Phase5]:
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
    phase4 = check_phase4(root, stages, phase1, report)
    phase5 = check_phase5(root, stages, phase1, phase4, report)
    check_safety_review(root, stages, phase5, report)
    return report, stages, phase1, phase4, phase5


def parse_entries(root: Path, report: Report) -> Phase4:
    """Parse the competency entries in 04-competencies/<domain>.md."""
    phase4 = Phase4()
    folder = root / "04-competencies"
    for domain in DOMAINS:
        path = folder / f"{domain}.md"
        if path.exists():
            phase4.domain_files[domain] = path
    for domain, path in phase4.domain_files.items():
        entry: Entry | None = None
        section = ""
        generated = False
        for number_, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            where = f"04-competencies/{path.name}:{number_}"
            if line.startswith("<!-- BEGIN generated:"):
                generated = True
                continue
            if line.startswith("<!-- END generated:"):
                generated = False
                continue
            if generated:
                continue
            if m := ENTRY_RE.match(line):
                ident = m.group(1)
                if ident in phase4.entries:
                    report.error(f"{where}: duplicate entry {ident} (also in {phase4.entries[ident].file})")
                if not ident.startswith(domain + "-"):
                    report.error(f"{where}: {ident} belongs in 04-competencies/{ident[:2]}.md")
                entry = Entry(ident, m.group(2), f"04-competencies/{path.name}")
                phase4.entries[ident] = entry
                section = ""
            elif line.startswith("### ") or line.startswith("## "):
                entry = None
            elif entry is not None and (m := FIELD_RE.match(line)):
                section = m.group(1)
                if section not in ENTRY_FIELDS:
                    report.error(f"{where}: unknown field **{section}:** (expected one of {', '.join(ENTRY_FIELDS)})")
                elif section in entry.fields:
                    report.error(f"{where}: {entry.id} has **{section}:** twice")
                else:
                    entry.fields.append(section)
            elif entry is not None and section == "Success criteria" and (m := SUCCESS_RE.match(line)):
                level = int(m.group(1))
                if level in entry.success:
                    report.error(f"{where}: {entry.id} has two success rows for level {level}")
                entry.success[level] = parse_evidence(m.group(3), where, report)
    return phase4


def parse_evidence(cell: str, where: str, report: Report) -> list[str]:
    methods = [part.strip() for part in cell.split(",") if part.strip()]
    for method in methods:
        if method not in EVIDENCE:
            report.error(f"{where}: unknown evidence method {method!r} (expected one of {', '.join(EVIDENCE)})")
    return methods


def parse_rubrics(path: Path, phase4: Phase4, report: Report) -> None:
    if not path.exists():
        return
    for number_, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        where = f"{path.name}:{number_}"
        if m := RUBRIC_RE.match(line):
            ident, criterion, _, components, competencies, evidence, weight = m.groups()
            row = RubricRow(
                ident,
                criterion.strip().lower(),
                split_ids(components),
                split_ids(competencies),
                parse_evidence(evidence, where, report),
                None if weight == "gate" else int(weight),
            )
            phase4.rubric.setdefault(ident.split(".")[0], []).append(row)
        elif re.match(r"^\| (?:MS-S\d|CAP-5)\.R", line):
            report.error(f"{where}: malformed rubric row")


def check_phase4(root: Path, stages: dict[str, list[Course]], phase1: Phase1, report: Report) -> Phase4:
    phase4 = parse_entries(root, report)
    parse_rubrics(root / "04-assessment.md", phase4, report)

    # Competency entries.
    for ident, entry in phase4.entries.items():
        if ident not in phase1.targets:
            report.error(f"{entry.file}: {ident} is not a Phase 1 competency")
            continue
        for name in ENTRY_FIELDS:
            if name not in entry.fields:
                report.error(f"{ident}: missing **{name}:**")
        if entry.fields != [f for f in ENTRY_FIELDS if f in entry.fields]:
            report.error(f"{ident}: fields out of order (expected {', '.join(ENTRY_FIELDS)})")
        target = phase1.targets[ident][-1] or 0
        for level in range(2, target + 1):
            if level not in entry.success:
                report.error(f"{ident}: no success criteria for level {level}")
        for level, methods in sorted(entry.success.items()):
            if not 2 <= level <= target:
                report.error(f"{ident}: success criteria for level {level}, outside 2–{target} (the core target)")
            if methods and not any(EVIDENCE.get(m) in ("O", "P") for m in methods):
                report.error(f"{ident}: level {level} rests on self-assessed evidence only")
    if phase4.domain_files:
        missing = sorted(set(phase1.targets) - set(phase4.entries))
        complete = len(phase4.domain_files) == len(DOMAINS)
        for ident in missing:
            if complete or ident[:2] in phase4.domain_files:
                report.error(f"{ident}: no entry in 04-competencies/{ident[:2]}.md")
        if not complete:
            absent = [d for d in DOMAINS if d not in phase4.domain_files]
            report.warn(f"04-competencies: no file yet for {', '.join(absent)}")

    # Rubrics.
    milestones = {c.id: c for cs in stages.values() for c in cs if c.is_milestone}
    for milestone, rows in phase4.rubric.items():
        course = milestones.get(milestone)
        if course is None:
            report.error(f"04-assessment.md: rubric for {milestone}, which is not in the outline")
            continue
        components = {l.id: l for m in course.modules for l in m.lessons}
        scored: set[tuple[str, str]] = set()
        categories: set[str] = set()
        for row in rows:
            if row.criterion not in CRITERIA:
                report.error(f"{row.id}: criterion {row.criterion!r} is not one of {', '.join(CRITERIA)}")
            if (row.criterion == "food safety") != (row.weight is None):
                report.error(f"{row.id}: food safety is a gate (weight 'gate'), and only food safety is")
            categories |= {EVIDENCE[m] for m in row.evidence if m in EVIDENCE}
            for component in row.components:
                if component not in components:
                    report.error(f"{row.id}: {component} is not a component of {milestone}")
            for comp in row.competencies:
                tested = [c for c in row.components if comp in getattr(components.get(c), "moves", {})]
                if not tested:
                    report.error(f"{row.id}: {comp} is not re-tested by {', '.join(row.components)} in the outline")
                scored |= {(c, comp) for c in tested}
        if not any(row.weight is None for row in rows):
            report.error(f"{milestone}: the rubric has no food-safety gate")
        total = sum(row.weight or 0 for row in rows)
        if total != 100:
            report.error(f"{milestone}: rubric weights add up to {total}, not 100")
        for lesson in components.values():
            for comp in lesson.moves:
                if (lesson.id, comp) not in scored:
                    report.error(f"{milestone}: {lesson.id} re-tests {comp}, but no rubric row scores it")
        for category, name in (("O", "objective"), ("P", "partly verifiable"), ("S", "self-assessed")):
            if category not in categories:
                report.error(f"{milestone}: the rubric has no {name} evidence")
    if phase4.rubric and len(stages) == len(STAGES):
        for milestone in sorted(set(milestones) - set(phase4.rubric)):
            report.error(f"{milestone}: no rubric in 04-assessment.md")
    return phase4


def anchor(heading: str) -> str:
    """GitHub's anchor for a heading."""
    return re.sub(r"[^\w\- ]", "", heading.lower()).replace(" ", "-")


def group_by_course(items: list[tuple[str, int | None]]) -> str:
    """`KNF-1` M1.L1→1, M2.L3→2; `WRK-1` M3.X1"""
    groups: dict[str, list[str]] = {}
    for ident, level in items:
        course, rest = ident.split(".", 1)
        groups.setdefault(course, []).append(rest if level is None else f"{rest}→{level}")
    return "; ".join(f"`{course}` {', '.join(parts)}" for course, parts in groups.items()) or "—"


def trace(stages: dict[str, list[Course]], phase1: Phase1) -> dict[str, dict[str, dict[str, object]]]:
    """Per competency and stage: lessons (with level), sessions, exercises and re-tests."""
    result: dict[str, dict[str, dict[str, object]]] = {
        comp: {s: {"lessons": [], "sessions": 0, "exercises": [], "retests": []} for s in STAGES} for comp in phase1.targets
    }
    for stage, courses in stages.items():
        for course in courses:
            for module in course.modules:
                touched: set[str] = set()
                for lesson in module.lessons:
                    for comp, level in lesson.moves.items():
                        if comp not in result:
                            continue
                        cell = result[comp][stage]
                        if course.is_milestone:
                            cell["retests"].append((lesson.id, level))
                        else:
                            cell["lessons"].append((lesson.id, level))
                            cell["sessions"] += lesson.practice
                            touched.add(comp)
                for comp in touched:
                    result[comp][stage]["exercises"].extend((e[0], None) for e in module.exercises)
    return result


def phase4_tables(stages: dict[str, list[Course]], phase1: Phase1, phase4: Phase4) -> dict[Path | str, dict[str, str]]:
    """The generated blocks of the Phase 4 files, by file (a domain code for the competency files)."""
    data = trace(stages, phase1)
    steps = {stage: best for stage, best, _ in progress(stages, phase1)}
    tables: dict[Path | str, dict[str, str]] = {domain: {} for domain in phase4.domain_files}
    for ident, entry in phase4.entries.items():
        if ident not in phase1.targets or ident[:2] not in tables:
            continue
        levels = phase1.targets[ident]
        lines = [
            f"*Phase 1:* {phase1.names[ident]}",
            "",
            "*Targets:* "
            + " · ".join(f"{s} {'–' if l is None else l}" for s, l in zip(STAGES, levels))
            + f" · core **{levels[-1]}**",
            "",
            "| Stage | Target | Taught to | Taught in (lesson→level) | Sessions | Exercises | Re-tested in |",
            "|---|---|---|---|---|---|---|",
        ]
        cumulative = 0
        for index, stage in enumerate(STAGES):
            cell = data[ident][stage]
            cumulative += cell["sessions"]
            reached = steps.get(stage, {}).get(ident, 0) if stage in stages else None
            lines.append(
                f"| {stage} | {'–' if levels[index] is None else levels[index]} | "
                f"{'·' if reached is None else reached or '–'} | {group_by_course(cell['lessons'])} | "
                f"{cell['sessions']} ({cumulative}) | {group_by_course(cell['exercises'])} | {group_by_course(cell['retests'])} |"
            )
        tables[ident[:2]][f"map-{ident.lower()}"] = "\n".join(lines)

    rows = [
        "| Competency | Target | Courses | Modules | Lessons | Exercises | Practical sessions | Re-tests |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for ident, levels in phase1.targets.items():
        cells = [data[ident][s] for s in STAGES]
        lessons = [l for c in cells for l, _ in c["lessons"]]
        retests = [l for c in cells for l, _ in c["retests"]]
        entry = phase4.entries.get(ident)
        link = (
            f"[`{ident}`](04-competencies/{ident[:2]}.md#{anchor(f'{ident} — {entry.title}')})" if entry else f"`{ident}`"
        )
        rows.append(
            f"| {link} | {levels[-1]} | {len({l.split('.')[0] for l in lessons})} | "
            f"{len({l.rsplit('.', 1)[0] for l in lessons})} | {len(lessons)} | "
            f"{len({e for c in cells for e, _ in c['exercises']})} | {sum(c['sessions'] for c in cells)} | {len(retests)} |"
        )
    traceability = "\n".join(rows)

    prog = [
        "| Stage | Competencies with a target | Raised in this stage | Of which safety (`SA-*`) | At level 5 | Milestone |",
        "|---|---|---|---|---|---|",
    ]
    for index, stage in enumerate(STAGES):
        with_target = [c for c, l in phase1.targets.items() if l[index] is not None]
        raised = [c for c in with_target if index == 0 or (phase1.targets[c][index - 1] or 0) < phase1.targets[c][index]]
        milestone = next((c for c in stages.get(stage, []) if c.is_milestone), None)
        prog.append(
            f"| {stage} | {len(with_target)} | {len(raised)} | {sum(1 for c in raised if c.startswith('SA-'))} | "
            f"{sum(1 for c in with_target if phase1.targets[c][index] == 5)} | "
            f"{f'`{milestone.id}` ({fmt(milestone.hours)} h)' if milestone else '—'} |"
        )
    progression = "\n".join(prog)

    order = ["MS-S1", "MS-S2", "MS-S3", "MS-S4", "CAP-5"]
    crit = ["| Criterion | " + " | ".join(f"`{m}`" for m in order) + " |", "|---|" + "---|" * len(order)]
    for criterion in CRITERIA:
        cells = []
        for milestone in order:
            rows_ = [r for r in phase4.rubric.get(milestone, []) if r.criterion == criterion]
            if not rows_:
                cells.append("—")
            elif all(r.weight is None for r in rows_):
                cells.append("gate")
            else:
                cells.append(str(sum(r.weight or 0 for r in rows_)))
        crit.append(f"| {criterion} | " + " | ".join(cells) + " |")
    crit.append(
        "| **rows** | "
        + " | ".join(f"**{len(phase4.rubric.get(m, []))}**" for m in order)
        + " |"
    )
    mix = [
        "| Assessment | Rows | Gates | Weight on rows with objective evidence | … partly verifiable at best | … self-assessed only |",
        "|---|---|---|---|---|---|",
    ]
    for milestone in order:
        rows_ = phase4.rubric.get(milestone, [])
        best = {"O": 0, "P": 0, "S": 0}
        for r in rows_:
            cats = {EVIDENCE.get(m) for m in r.evidence}
            best["O" if "O" in cats else "P" if "P" in cats else "S"] += r.weight or 0
        mix.append(
            f"| `{milestone}` | {len(rows_)} | {sum(1 for r in rows_ if r.weight is None)} | "
            f"{best['O']} | {best['P']} | {best['S']} |"
        )
    return {
        **tables,
        "04-competency-framework.md": {"traceability": traceability, "progression": progression},
        "04-assessment.md": {"criteria": "\n".join(crit), "evidence-mix": "\n".join(mix)},
    }


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


def sync_generated(path: Path, tables: dict[str, str], write: bool, report: Report, optional: bool = False) -> None:
    if not path.exists():
        if not optional:
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


# ---------------------------------------------------------------------------------------------------------------------
# Phase 5: course files (curriculum/05-courses/<stage>/<course>.md)
# ---------------------------------------------------------------------------------------------------------------------

#: The H2 sections of a course file, in order.
COURSE_SECTIONS = (
    "Introduction",
    "Learning outcomes",
    "Module structure and hours",
    "Modules",
    "Assessment criteria",
    "Materials and equipment",
    "Validation",
)
LESSON_FIELDS = ("Concepts", "Sessions", "Cues", "Faults")
EXERCISE_FIELDS = ("Evidences", "Aim", "Conditions", "Inputs", "Procedure", "Evidence", "Time")
ITEM_TYPES = ("multiple choice", "scenario", "calculation", "image identification", "ordering")
QUIZ_SIZE = (8, 20)
#: The highest equipment tier a course in each stage may require (Phase 1 §9.4).
STAGE_TIER = {"S1": 0, "S2": 1, "S3": 2, "S4": 3, "S5": 3}

H1_RE = re.compile(rf"^# ({COURSE}) — (.+)$")
DOC_MODULE_RE = re.compile(rf"^### ({COURSE}\.M\d+) — (.+)$")
DOC_LESSON_RE = re.compile(rf"^#### ({COURSE}\.M\d+\.L\d+) — (.+)$")
DOC_EXERCISE_RE = re.compile(rf"^#### ({COURSE}\.M\d+\.X\d+) — (.+)$")
EXERCISE_REF_RE = re.compile(rf"\b({COURSE}\.M\d+\.X\d+)\b")
#: Module and lesson IDs (`SAU-2.M2`, `FLV-3.M2.L1`), but not the module part of an exercise or item ID.
UNIT_REF_RE = re.compile(rf"\b({COURSE}\.M\d+(?:\.L\d+)?)(?!\d|\.[A-Z]\d)")
ITEM_RE = re.compile(rf"^\| ({COURSE}\.M\d+\.Q\d+) \| (.+?) \| (.+?) \| (yes|—) \| (.+?) \| (.+?) \| (.+?) \|$")
EVROW_RE = re.compile(rf"^\| ({COMP}) \| ([1-5]) \| (.+?) \| (.+?) \|$")
EQUIP_RE = re.compile(r"^\| (?!Item \|)(.+?) \| ([0-3]|—) \| (.+?) \| (.+?) \|$")
BUDGET_RE = re.compile(r"^\*\*Indicative ingredient budget:\*\* about €(\d+)")
DEVELOPED_RE = re.compile(r"^\*\*Stages developed:\*\* (.+)$", re.M)
NUMBERED_RE = re.compile(r"^\d+\. ")


@dataclass
class ExerciseDoc:
    id: str
    tags: dict[str, int] = field(default_factory=dict)
    fields: list[str] = field(default_factory=list)
    evidence: list[str] = field(default_factory=list)


@dataclass
class Item:
    id: str
    type: str
    lesson: str
    safety: bool
    answer: str
    question: str = ""
    why: str = ""


@dataclass
class CourseDoc:
    id: str
    path: Path
    sections: list[str] = field(default_factory=list)
    modules: dict[str, str] = field(default_factory=dict)  # module id -> title
    lessons: dict[str, list[str]] = field(default_factory=dict)  # lesson id -> fields
    lesson_titles: dict[str, str] = field(default_factory=dict)  # lesson id -> heading title
    sessions: dict[str, int] = field(default_factory=dict)  # lesson id -> numbered session items
    exercises: dict[str, ExerciseDoc] = field(default_factory=dict)
    checks: set[str] = field(default_factory=set)  # module ids with a "Module check" heading
    items: dict[str, list[Item]] = field(default_factory=dict)  # module id -> items
    evidence: dict[str, list[tuple[str, int, list[str], list[str]]]] = field(default_factory=dict)
    equipment: list[tuple[str, str]] = field(default_factory=list)  # (item, tier)
    budget: int | None = None


@dataclass
class Phase5:
    developed: list[str] = field(default_factory=list)
    docs: dict[str, CourseDoc] = field(default_factory=dict)
    overview: Path | None = None
    # The second reader's worksheet (06-safety-review.md): item id -> (stage, verdict); empty when there is no file.
    review: dict[str, tuple[str, str]] = field(default_factory=dict)


def split_methods(cell: str, where: str, report: Report) -> list[str]:
    """`photo, measure — text` or `photo, measure:` (a list follows): the evidence methods first."""
    head = cell.split(" — ", 1)[0].strip().removesuffix(":")
    return parse_evidence(head, where, report)


def parse_course_doc(path: Path, root: Path, report: Report) -> CourseDoc | None:
    lines = path.read_text(encoding="utf-8").splitlines()
    rel = path.relative_to(root)
    first = next((l for l in lines if l.strip()), "")
    m = H1_RE.match(first)
    if not m:
        report.error(f"{rel}: the first line must be '# <course ID> — <title>'")
        return None
    doc = CourseDoc(m.group(1), path)
    if path.stem != doc.id:
        report.error(f"{rel}: holds {doc.id}; the file must be named {doc.id}.md")
    module = lesson = exercise = None
    section = field_ = ""
    in_check = False
    generated = False
    for number_, line in enumerate(lines, 1):
        where = f"{rel}:{number_}"
        if line.startswith("<!-- BEGIN generated:"):
            generated = True
            continue
        if line.startswith("<!-- END generated:"):
            generated = False
            continue
        if generated:
            continue
        if line.startswith("## "):
            section = line[3:].strip()
            doc.sections.append(section)
            module = lesson = exercise = None
            in_check = False
            continue
        if m := DOC_MODULE_RE.match(line):
            module, lesson, exercise, in_check = m.group(1), None, None, False
            if module in doc.modules:
                report.error(f"{where}: {module} appears twice")
            doc.modules[module] = m.group(2).strip()
            continue
        if m := DOC_LESSON_RE.match(line):
            lesson, exercise, in_check, field_ = m.group(1), None, False, ""
            if module is None or not lesson.startswith(module + "."):
                report.error(f"{where}: lesson {lesson} is not under its module heading")
            if lesson in doc.lessons:
                report.error(f"{where}: {lesson} has two outlines")
            doc.lessons[lesson] = []
            doc.lesson_titles[lesson] = m.group(2).strip()
            doc.sessions[lesson] = 0
            continue
        if m := DOC_EXERCISE_RE.match(line):
            exercise, lesson, in_check, field_ = m.group(1), None, False, ""
            if module is None or not exercise.startswith(module + "."):
                report.error(f"{where}: exercise {exercise} is not under its module heading")
            if exercise in doc.exercises:
                report.error(f"{where}: {exercise} has two specs")
            doc.exercises[exercise] = ExerciseDoc(exercise)
            continue
        if line.strip() == "#### Module check":
            lesson = exercise = None
            in_check = True
            if module is None:
                report.error(f"{where}: a module check outside a module")
            else:
                doc.checks.add(module)
                doc.items.setdefault(module, [])
                doc.evidence.setdefault(module, [])
            continue
        if line.startswith("#### ") or line.startswith("### "):
            lesson = exercise = None
            in_check = False
            continue
        fm = FIELD_RE.match(line)
        if lesson is not None:
            if fm:
                field_ = fm.group(1)
                if field_ not in LESSON_FIELDS:
                    report.error(f"{where}: unknown lesson field **{field_}:** (expected {', '.join(LESSON_FIELDS)})")
                elif field_ in doc.lessons[lesson]:
                    report.error(f"{where}: {lesson} has **{field_}:** twice")
                else:
                    doc.lessons[lesson].append(field_)
            elif field_ == "Sessions" and NUMBERED_RE.match(line):
                doc.sessions[lesson] += 1
        elif exercise is not None and fm:
            name = fm.group(1)
            spec = doc.exercises[exercise]
            if name not in EXERCISE_FIELDS:
                report.error(f"{where}: unknown exercise field **{name}:** (expected {', '.join(EXERCISE_FIELDS)})")
            elif name in spec.fields:
                report.error(f"{where}: {exercise} has **{name}:** twice")
            else:
                spec.fields.append(name)
            value = line[fm.end():].strip()
            if name == "Evidences":
                spec.tags = parse_moves(value, where, report)
            elif name == "Evidence":
                spec.evidence = split_methods(value, where, report)
        elif in_check and module is not None:
            if m := ITEM_RE.match(line):
                ident, kind, tested, safety, question, answer, why = (g.strip() for g in m.groups())
                if not ident.startswith(module + "."):
                    report.error(f"{where}: item {ident} is not in {module}'s check")
                if kind not in ITEM_TYPES:
                    report.error(f"{where}: item type {kind!r} is not one of {', '.join(ITEM_TYPES)}")
                doc.items[module].append(Item(ident, kind, tested.strip("`"), safety == "yes", answer, question, why))
            elif re.match(rf"^\| {COURSE}\.M\d+\.Q", line):
                report.error(f"{where}: malformed knowledge-check item")
            elif (m := EVROW_RE.match(line)) and m.group(1) != "Competency":
                exercises = [e.strip("` ") for e in m.group(3).split(",")]
                doc.evidence[module].append(
                    (m.group(1), int(m.group(2)), exercises, split_methods(m.group(4), where, report))
                )
        elif section == "Materials and equipment":
            if (m := EQUIP_RE.match(line)) and not set(m.group(1)) <= set("-| "):
                doc.equipment.append((m.group(1).strip().strip("*"), m.group(2)))
            elif m := BUDGET_RE.match(line):
                doc.budget = int(m.group(1))
    return doc


def blueprint(module: Module) -> tuple[int, int, int, int]:
    """[K] and [J] objectives in the module's lessons, and the item range the blueprint allows (assessment §4.1)."""
    k = sum(l.objectives.count("[K]") for l in module.lessons)
    j = sum(l.objectives.count("[J]") for l in module.lessons)
    if k + j == 0:
        return k, j, 0, 0
    low, high = QUIZ_SIZE

    def clamp(n: int) -> int:
        return min(max(n, low), high)

    return k, j, clamp(2 * k), clamp(2 * k + j)


def module_levels(module: Module) -> dict[str, int]:
    best: dict[str, int] = {}
    for lesson in module.lessons:
        for comp, level in lesson.moves.items():
            best[comp] = max(best.get(comp, 0), level)
    return best


def quiz_only(phase4: Phase4, comp: str, level: int) -> bool:
    """The competency's success row at this level rests on the quiz alone, so no exercise is needed."""
    entry = phase4.entries.get(comp)
    return entry is not None and set(entry.success.get(level, [])) == {"quiz"}


def check_phase5(root: Path, stages: dict[str, list[Course]], phase1: Phase1, phase4: Phase4, report: Report) -> Phase5:
    phase5 = Phase5()
    overview = root / "05-course-development.md"
    if overview.exists():
        phase5.overview = overview
        m = DEVELOPED_RE.search(overview.read_text(encoding="utf-8"))
        if m is None:
            report.error("05-course-development.md: no **Stages developed:** line")
        else:
            phase5.developed = [s.strip() for s in m.group(1).split(",") if s.strip() in STAGES]
    courses = {c.id: c for cs in stages.values() for c in cs}
    folder = root / "05-courses"
    for path in sorted(folder.glob("*/*.md")) if folder.exists() else []:
        doc = parse_course_doc(path, root, report)
        if doc is None:
            continue
        course = courses.get(doc.id)
        if course is None or course.is_milestone:
            report.error(f"{path.relative_to(root)}: {doc.id} is not a course in the outline")
            continue
        if path.parent.name != course.stage:
            report.error(f"{path.relative_to(root)}: {doc.id} is in {course.stage}; the file belongs in 05-courses/{course.stage}/")
        if doc.id in phase5.docs:
            report.error(f"{path.relative_to(root)}: a second file for {doc.id}")
        phase5.docs[doc.id] = doc
        check_course_doc(doc, course, phase4, report)
    for stage in phase5.developed:
        for course in stages.get(stage, []):
            if not course.is_milestone and course.id not in phase5.docs:
                report.error(f"{course.id}: {stage} is developed, but 05-courses/{stage}/{course.id}.md is missing")

    # Exercise, module and lesson IDs named anywhere in a course file (evidence rows, specs, notes) must exist in the
    # outline, and exercises and lessons must be written once their stage is developed, so references can't drift.
    outline_exercises = {e[0]: c for c in courses.values() for m in c.modules for e in m.exercises}
    outline_units = {m.id: c for c in courses.values() for m in c.modules}
    outline_units.update({lesson.id: c for c in courses.values() for m in c.modules for lesson in m.lessons})
    for doc in phase5.docs.values():
        text = doc.path.read_text(encoding="utf-8")
        for ident in sorted(set(EXERCISE_REF_RE.findall(text))):
            owner = outline_exercises.get(ident)
            if owner is None:
                report.error(f"{doc.id}: names {ident}, which is not an exercise in the outline")
            elif owner.stage in phase5.developed and owner.id in phase5.docs and ident not in phase5.docs[owner.id].exercises:
                report.error(f"{doc.id}: names {ident}, which has no spec in {owner.id}")
        for ident in sorted(set(UNIT_REF_RE.findall(text))):
            owner = outline_units.get(ident)
            if owner is None:
                kind = "lesson" if ".L" in ident else "module"
                report.error(f"{doc.id}: names {ident}, which is not a {kind} in the outline")
            elif ".L" in ident and owner.stage in phase5.developed and owner.id in phase5.docs and ident not in phase5.docs[owner.id].lessons:
                report.error(f"{doc.id}: names {ident}, which has no lesson outline in {owner.id}")

    # Reachability: in every developed stage, each competency's stage target is evidenced by an exercise at that level.
    tagged = reach(stages, phase1, phase5)
    for index, stage in enumerate(STAGES):
        if stage not in phase5.developed:
            continue
        for comp, levels in phase1.targets.items():
            target = levels[index]
            if (
                target
                and target >= 2
                and not quiz_only(phase4, comp, target)
                and not any(tagged[comp][s].get(target) for s in STAGES[: index + 1])
            ):
                report.error(f"reachability: no exercise up to {stage} evidences {comp} at its {stage} target {target}")
    return phase5


SAFETY_REVIEW = "06-safety-review.md"
REVIEW_COURSE_RE = re.compile(rf"^### ({COURSE}) — (.+)$")
REVIEW_ROW_RE = re.compile(rf"^\| ({COURSE}\.M\d+\.Q\d+) \| (.+?) \| (.+?) \| (.+?) \| (.+?) \| (.*?) \| (.*?) \|$")
COURSE_RULE_RE = re.compile(rf"^— `({COURSE}\.M\d+\.L\d+)`$")
VERDICTS = ("", "agrees", "disagrees")


def safety_topics(root: Path) -> set[str]:
    """The short names of the §9 rows of 04-assessment.md: each Topic up to its first bracket."""
    path = root / "04-assessment.md"
    text = path.read_text(encoding="utf-8") if path.exists() else ""
    m = re.search(r"^## 9\..*?$(.*?)(?=^## |\Z)", text, re.M | re.S)
    topics = set()
    for line in (m.group(1) if m else "").splitlines():
        if line.startswith("| ") and not line.startswith("| Topic |"):
            topics.add(line.split(" | ")[0][2:].split(" (")[0].strip())
    return topics


def check_safety_review(root: Path, stages: dict[str, list[Course]], phase5: Phase5, report: Report) -> None:
    """The second reader's worksheet holds every safety item once, word for word, mapped to the §9 row it rests on."""
    path = root / SAFETY_REVIEW
    if not path.exists():
        return
    courses = {c.id: c for cs in stages.values() for c in cs}
    lessons = {l.id for c in courses.values() for m in c.modules for l in m.lessons}
    items = {
        item.id: (doc.id, item)
        for doc in phase5.docs.values()
        for module_items in doc.items.values()
        for item in module_items
        if item.safety
    }
    topics = safety_topics(root)
    course = None
    for number_, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        where = f"{SAFETY_REVIEW}:{number_}"
        if m := REVIEW_COURSE_RE.match(line):
            course = m.group(1)
            continue
        if not (m := REVIEW_ROW_RE.match(line)):
            if re.match(rf"^\| {COURSE}\.M\d+\.Q", line):
                report.error(f"{where}: malformed worksheet row")
            continue
        ident, question, answer, why, rests_on, verdict, note = (g.strip() for g in m.groups())
        if ident in phase5.review:
            report.error(f"{where}: {ident} appears twice")
            continue
        if ident not in items:
            report.error(f"{where}: {ident} is not a safety item of a course file")
            continue
        owner, item = items[ident]
        if course != owner:
            report.error(f"{where}: {ident} belongs under the heading of {owner}")
        for name, mine, theirs in (("question", question, item.question), ("answer", answer, item.answer), ("why", why, item.why)):
            if mine != theirs:
                report.error(f"{where}: {ident}'s {name} differs from {owner}.md; copy it again and clear the verdict")
        if c := COURSE_RULE_RE.match(rests_on):
            if c.group(1) not in lessons:
                report.error(f"{where}: {ident} rests on {c.group(1)}, which is not a lesson in the outline")
        else:
            for topic in rests_on.split("; "):
                if topic not in topics:
                    report.error(f"{where}: {ident} names §9 row {topic!r}, which is not a row of assessment §9")
        if verdict not in VERDICTS:
            report.error(f"{where}: verdict {verdict!r} is not one of agrees, disagrees (or empty)")
        elif verdict == "disagrees" and not note:
            report.error(f"{where}: {ident} disagrees without a note")
        phase5.review[ident] = (courses[owner].stage, verdict)
    for ident in sorted(set(items) - set(phase5.review)):
        report.error(f"{SAFETY_REVIEW}: safety item {ident} ({items[ident][0]}) is missing")


def review_summary(phase5: Phase5) -> str:
    """The generated log summary of the worksheet: items per stage by verdict."""
    rows = ["| Stage | Safety items | Agrees | Disagrees | Not yet read |", "|---|---|---|---|---|"]
    totals = [0, 0, 0, 0]
    for stage in STAGES:
        verdicts = [v for s, v in phase5.review.values() if s == stage]
        counts = [len(verdicts), verdicts.count("agrees"), verdicts.count("disagrees"), verdicts.count("")]
        totals = [a + b for a, b in zip(totals, counts)]
        rows.append(f"| {stage} | " + " | ".join(str(n) for n in counts) + " |")
    rows.append("| **Total** | " + " | ".join(f"**{n}**" for n in totals) + " |")
    return "\n".join(rows)


def plain_title(title: str) -> str:
    """A heading title without its italics, so `*Mise en place*` and `Mise en place` compare equal."""
    return re.sub(r"[*_]", "", title).strip()


def check_course_doc(doc: CourseDoc, course: Course, phase4: Phase4, report: Report) -> None:
    name = str(doc.path.name)
    if doc.sections != list(COURSE_SECTIONS):
        report.error(f"{course.id}: sections must be {', '.join(COURSE_SECTIONS)} (found {', '.join(doc.sections)})")
    modules = {m.id: m for m in course.modules}
    for ident in doc.modules:
        if ident not in modules:
            report.error(f"{name}: {ident} is not a module of {course.id}")
    for module in course.modules:
        if module.id not in doc.modules:
            report.error(f"{course.id}: no section for {module.id}")
        elif doc.modules[module.id] != module.title:
            report.error(f"{module.id}: titled {doc.modules[module.id]!r}, the outline says {module.title!r}")
        levels = module_levels(module)
        for lesson in module.lessons:
            fields = doc.lessons.get(lesson.id)
            if fields is None:
                report.error(f"{lesson.id}: no lesson outline")
                continue
            for f in LESSON_FIELDS:
                if f not in fields:
                    report.error(f"{lesson.id}: missing **{f}:**")
            if plain_title(doc.lesson_titles[lesson.id]) != plain_title(lesson.title):
                report.error(f"{lesson.id}: titled {doc.lesson_titles[lesson.id]!r}, the outline says {lesson.title!r}")
            if doc.sessions[lesson.id] != lesson.practice:
                report.error(
                    f"{lesson.id}: {doc.sessions[lesson.id]} numbered session(s) under **Sessions:**, "
                    f"the outline has {lesson.practice}"
                )
        outline_exercises = {e[0] for e in module.exercises}
        for ident, _, _ in module.exercises:
            spec = doc.exercises.get(ident)
            if spec is None:
                report.error(f"{ident}: no exercise spec")
                continue
            for f in EXERCISE_FIELDS:
                if f not in spec.fields:
                    report.error(f"{ident}: missing **{f}:**")
            for comp, level in spec.tags.items():
                if comp not in levels:
                    report.error(f"{ident}: {comp} is not moved by any lesson of {module.id}")
                elif level > levels[comp]:
                    report.error(f"{ident}: {comp}→{level} is above the level {module.id} reaches ({levels[comp]})")
                elif level < 2:
                    report.error(f"{ident}: {comp}→{level}: level 1 is shown by the quiz, not an exercise")
        # Knowledge check.
        if module.id not in doc.checks:
            report.error(f"{module.id}: no '#### Module check'")
            continue
        items = doc.items.get(module.id, [])
        k, j, low, high = blueprint(module)
        optional = k == 0  # [J] objectives only: a quiz only where they can be put as scenarios
        if not low <= len(items) <= high and not (optional and not items):
            expected = f"{low}" if low == high else f"{low}–{high}"
            expected = f"0 or {expected}" if optional and high else expected
            report.error(f"{module.id}: {len(items)} knowledge-check item(s); the blueprint asks for {expected} ({k} [K], {j} [J])")
        lessons = {l.id: l for l in module.lessons}
        for item in items:
            if item.lesson not in lessons:
                report.error(f"{item.id}: tests {item.lesson}, which is not a lesson of {module.id}")
            if not item.answer or item.answer == "—":
                report.error(f"{item.id}: no answer")
        for lesson in module.lessons:
            if "[K]" in lesson.objectives and not any(i.lesson == lesson.id for i in items):
                report.error(f"{module.id}: no knowledge-check item tests {lesson.id}'s [K] objective")
        if items and any(c.startswith("SA-") for c in levels) and not any(i.safety for i in items):
            report.error(f"{module.id}: moves a safety competency, but no item is marked safety")
        # Evidence check: each competency the module moves to level 2 or more, at the highest level it reaches.
        rows = doc.evidence.get(module.id, [])
        covered = set()
        for comp, level, exercises, _ in rows:
            covered.add(comp)
            if comp not in levels:
                report.error(f"{module.id}: evidence row for {comp}, which the module doesn't move")
                continue
            if level < 2:
                report.error(f"{module.id}: evidence row {comp} {level}: level 1 is shown by the quiz")
            elif level != levels[comp]:
                report.error(f"{module.id}: evidence row {comp} {level}; the module reaches {levels[comp]}")
            entry = phase4.entries.get(comp)
            if entry is not None and level not in entry.success:
                report.error(f"{module.id}: {comp} has no success row for level {level}")
            if exercises == ["knowledge check"]:
                if entry is not None and set(entry.success.get(level, [])) != {"quiz"}:
                    report.error(f"{module.id}: {comp} {level} needs more than the quiz (its success row), so name an exercise")
                if not any(comp in lessons[i.lesson].moves for i in items if i.lesson in lessons):
                    report.error(f"{module.id}: {comp} rests on the knowledge check, but no item tests a lesson that moves it")
                continue
            for ident in exercises:
                if ident not in outline_exercises:
                    report.error(f"{module.id}: evidence row {comp} names {ident}, not an exercise of this module")
                elif doc.exercises.get(ident) and doc.exercises[ident].tags.get(comp) != level:
                    report.error(f"{module.id}: {ident} is not tagged {comp}→{level}")
        for comp, level in levels.items():
            if level >= 2 and comp not in covered:
                report.error(f"{module.id}: no evidence row for {comp} (level {level})")
    for ident in doc.exercises:
        if not any(ident == e[0] for m in course.modules for e in m.exercises):
            report.error(f"{name}: {ident} is not an exercise in the outline")
    for ident in doc.lessons:
        if not any(ident == l.id for m in course.modules for l in m.lessons):
            report.error(f"{name}: {ident} is not a lesson in the outline")
    # Equipment and budget.
    for item, tier in doc.equipment:
        if tier != "—" and int(tier) > STAGE_TIER[course.stage]:
            report.error(f"{course.id}: {item} is tier {tier}, above what {course.stage} may require ({STAGE_TIER[course.stage]})")
    if not doc.equipment:
        report.error(f"{course.id}: no equipment table under Materials and equipment")
    if doc.budget is None:
        report.error(f"{course.id}: no '**Indicative ingredient budget:** about €…' line")
    for outcome in course.outcomes:
        for comp, level in outcome[2].items():
            if level >= 2 and not quiz_only(phase4, comp, level) and not any(s.tags.get(comp) for s in doc.exercises.values()):
                report.error(f"{course.id}: outcome {comp}→{level} is practised in no exercise")


def reach(
    stages: dict[str, list[Course]], phase1: Phase1, phase5: Phase5
) -> dict[str, dict[str, dict[int, list[str]]]]:
    """Per competency and stage: level -> the exercises tagged with it."""
    result: dict[str, dict[str, dict[int, list[str]]]] = {comp: {s: {} for s in STAGES} for comp in phase1.targets}
    for stage, courses in stages.items():
        for course in courses:
            doc = phase5.docs.get(course.id)
            if doc is None:
                continue
            for spec in doc.exercises.values():
                for comp, level in spec.tags.items():
                    if comp in result:
                        result[comp][stage].setdefault(level, []).append(spec.id)
    return result


def block(ident: str) -> str:
    return ident.lower().replace(".", "-")


def weeks(hours: float) -> str:
    return " / ".join(f"{hours / w:.1f}" for w in WEEKLY)


def phase5_tables(
    stages: dict[str, list[Course]], phase1: Phase1, phase4: Phase4, phase5: Phase5
) -> dict[Path, dict[str, str]]:
    courses = {c.id: c for cs in stages.values() for c in cs}
    required_by: dict[str, list[str]] = {}
    for c in courses.values():
        for p in c.prereqs:
            required_by.setdefault(p, []).append(c.id)
    retests: dict[str, list[tuple[str, int]]] = {}
    for c in courses.values():
        if c.is_milestone:
            for m in c.modules:
                for l in m.lessons:
                    for comp, level in l.moves.items():
                        retests.setdefault(comp, []).append((l.id, level))
    result: dict[Path, dict[str, str]] = {}
    for ident, doc in phase5.docs.items():
        course = courses[ident]
        tables: dict[str, str] = {}
        lessons = [l for m in course.modules for l in m.lessons]
        practice = sum(l.practice for l in lessons)
        waits = [f"`{l.id}` {l.elapsed}" for l in lessons if l.elapsed != "—"]
        tables["header"] = "\n".join(
            [
                f"**Stage:** {course.stage} · **Hours:** {fmt(course.hours)} h active, {practice} practical sessions · "
                f"**Modules:** {len(course.modules)} · **Lessons:** {len(lessons)}",
                "",
                f"**Prerequisites:** {', '.join(f'`{p}`' for p in course.prereqs) or '—'} · "
                f"**Required by:** {', '.join(f'`{p}`' for p in required_by.get(ident, [])) or '—'}",
                "",
                f"**Purpose (outline):** {course.purpose}",
            ]
        )
        rows = ["| Outcome | Moves | Taught in | Practised in | Checked in |", "|---|---|---|---|---|"]
        for tag, text, moves in course.outcomes:
            for comp, level in moves.items() or [("", 0)]:
                taught = [(l.id, l.moves[comp]) for l in lessons if comp in l.moves]
                practised = [(s.id, s.tags[comp]) for s in doc.exercises.values() if comp in s.tags]
                checked = [
                    m.id.split(".", 1)[1]
                    for m in course.modules
                    if comp in module_levels(m)
                    and (any(r[0] == comp for r in doc.evidence.get(m.id, [])) or doc.items.get(m.id))
                ]
                again = [
                    f"`{l}`→{lv}" for l, lv in retests.get(comp, []) if l.split(".")[0] in milestones_after(course, stages)[:1]
                ]
                rows.append(
                    f"| [{tag}] {text} | {f'`{comp}→{level}`' if comp else '—'} | {group_by_course(taught)} | "
                    f"{group_by_course(practised)} | "
                    f"{('checks ' + ', '.join(checked)) if checked else '—'}{'; ' + ', '.join(again) if again else ''} |"
                )
        tables["outcomes"] = "\n".join(rows)
        mod = [
            "| Module | h | Lessons | Practical sessions | Exercises | Knowledge check | Evidence rows |",
            "|---|---|---|---|---|---|---|",
        ]
        for m in course.modules:
            mod.append(
                f"| `{m.id}` {m.title} | {fmt(m.hours)} | {len(m.lessons)} | {sum(l.practice for l in m.lessons)} | "
                f"{len(m.exercises)} | {len(doc.items.get(m.id, []))} items "
                f"({sum(1 for i in doc.items.get(m.id, []) if i.safety)} safety) | {len(doc.evidence.get(m.id, []))} |"
            )
        mod.append(
            f"| **Total** | **{fmt(course.hours)}** | **{len(lessons)}** | **{practice}** | "
            f"**{sum(len(m.exercises) for m in course.modules)}** | "
            f"**{sum(len(v) for v in doc.items.values())} items** | **{sum(len(v) for v in doc.evidence.values())}** |"
        )
        mod += [
            "",
            f"At 5 / 6.5 / 8 hours a week the course takes about **{weeks(course.hours)} weeks** of active time."
            + (f" Calendar time beyond that: {'; '.join(waits)}." if waits else ""),
        ]
        tables["modules"] = "\n".join(mod)
        for m in course.modules:
            levels = module_levels(m)
            tables[f"module-{block(m.id)}"] = "\n".join(
                [
                    f"*{fmt(m.hours)} h · {len(m.lessons)} lessons · {sum(l.practice for l in m.lessons)} practical sessions · "
                    f"reaches {', '.join(f'{c} {lv}' for c, lv in levels.items())}*",
                    "",
                    f"*Scope:* {m.scope}",
                    "",
                    f"*Objectives:* {m.objectives}",
                    "",
                    f"*Check (outline):* {m.assessment}",
                ]
            )
            for l in m.lessons:
                tables[f"lesson-{block(l.id)}"] = "\n".join(
                    [
                        f"*{fmt(l.hours)} h · {l.practice} practical session{'s' if l.practice != 1 else ''}"
                        + (f" · elapsed {l.elapsed}" if l.elapsed != "—" else "")
                        + (f" · builds on {', '.join(f'`{p}`' for p in l.prereqs)}" if l.prereqs else "")
                        + f" · moves {', '.join(f'{c}→{lv}' for c, lv in l.moves.items()) or '—'}*",
                        "",
                        f"*Objectives:* {l.objectives}",
                    ]
                )
            for e_id, kind, support in m.exercises:
                tables[f"exercise-{block(e_id)}"] = f"*{kind}, {support}* — {m.descriptions.get(e_id, '')}"
            k, j, low, high = blueprint(m)
            size = "no quiz (no [K] or [J] objective)" if high == 0 else (
                f"{low} items" if low == high else f"{low}–{high} items"
            )
            if k == 0 and high:
                size = f"no quiz, or {size} if the [J] objectives are put as scenarios"
            tables[f"check-{block(m.id)}"] = (
                f"*Blueprint (assessment §4.1): {k} [K] and {j} [J] objective(s) in the lessons → {size}. "
                "Pass: 80 % and every safety item correct.*"
            )
        ass = ["| Module | Knowledge check | Safety items | Evidence rows (competency → level) |", "|---|---|---|---|"]
        for m in course.modules:
            items = doc.items.get(m.id, [])
            ass.append(
                f"| `{m.id}` | {len(items)} items | {sum(1 for i in items if i.safety)} | "
                f"{', '.join(f'{c}→{lv}' for c, lv, _, _ in doc.evidence.get(m.id, [])) or '—'} |"
            )
        moved = {c for l in lessons for c in l.moves}
        later = [
            f"`{l}` ({c}→{lv})"
            for c in sorted(moved)
            for l, lv in retests.get(c, [])
            if l.split(".")[0] in milestones_after(course, stages)[:1]
        ]
        ass += ["", f"**Re-tested at the next milestone:** {', '.join(later) or '—'}"]
        tables["assessment"] = "\n".join(ass)
        val = [
            f"- Lesson outlines: {sum(1 for l in lessons if l.id in doc.lessons)} of {len(lessons)}; session plans match the "
            "outline's practical sessions.",
            f"- Exercise specs: {len(doc.exercises)} of {sum(len(m.exercises) for m in course.modules)}; "
            f"{sum(len(s.tags) for s in doc.exercises.values())} competency tags, none above its module's level.",
            f"- Knowledge checks: {sum(len(v) for v in doc.items.values())} items within the blueprint; every [K] lesson "
            "tested; a safety item wherever a module moves `SA-*`.",
            f"- Evidence checks: {sum(len(v) for v in doc.evidence.values())} rows, each at the module's highest level "
            "and against a Phase 4 success row.",
            f"- Outcomes: {len(course.outcomes)}, each practised in an exercise (level 2 and up) and checked.",
            f"- Equipment within tier {STAGE_TIER[course.stage]}; indicative ingredient budget about €{doc.budget}.",
        ]
        tables["validation"] = "\n".join(val)
        result[doc.path] = tables

    if phase5.overview is not None:
        status = [
            "| Stage | Course | Lessons outlined | Exercises specified | Knowledge-check items | Safety items | Evidence rows | Budget |",
            "|---|---|---|---|---|---|---|---|",
        ]
        for stage, cs in stages.items():
            for c in cs:
                if c.is_milestone:
                    continue
                doc = phase5.docs.get(c.id)
                lessons = [l for m in c.modules for l in m.lessons]
                if doc is None:
                    status.append(f"| {stage} | `{c.id}` | 0 / {len(lessons)} | — | — | — | — | — |")
                    continue
                link = f"[`{c.id}`](05-courses/{stage}/{c.id}.md)"
                status.append(
                    f"| {stage} | {link} | {sum(1 for l in lessons if l.id in doc.lessons)} / {len(lessons)} | "
                    f"{len(doc.exercises)} / {sum(len(m.exercises) for m in c.modules)} | "
                    f"{sum(len(v) for v in doc.items.values())} | "
                    f"{sum(1 for v in doc.items.values() for i in v if i.safety)} | "
                    f"{sum(len(v) for v in doc.evidence.values())} | €{doc.budget} |"
                )
        tagged = reach(stages, phase1, phase5)
        written = {courses[d].stage for d in phase5.docs}
        shown = [s for s in STAGES if s in phase5.developed or s in written]
        rch = [
            "| Competency | "
            + " | ".join(s if s in phase5.developed else f"{s} (in progress)" for s in shown)
            + " | Exercises at L2 · L3 · L4 · L5 |",
            "|---|" + "---|" * (len(shown) + 1),
        ]
        for comp, levels in phase1.targets.items():
            if not any(levels[STAGES.index(s)] for s in shown):
                continue
            cells = []
            for s in shown:
                target = levels[STAGES.index(s)]
                ok = (
                    target is None
                    or target < 2
                    or any(tagged[comp][x].get(target) for x in STAGES[: STAGES.index(s) + 1])
                )
                quiz = target is not None and not ok and quiz_only(phase4, comp, target)
                cells.append("–" if target is None else f"{target}{' (quiz)' if quiz else '' if ok else ' ✗'}")
            counts = " · ".join(str(sum(len(tagged[comp][s].get(lv, [])) for s in STAGES)) for lv in (2, 3, 4, 5))
            rch.append(f"| `{comp}` | " + " | ".join(cells) + f" | {counts} |")
        equip: dict[str, list] = {}  # lower-case name -> [name, tier, courses]
        for ident, doc in phase5.docs.items():
            for item, tier in doc.equipment:
                row = equip.setdefault(item.lower(), [item, "—", []])
                if row[1] == "—":
                    row[1] = tier
                if ident not in row[2]:
                    row[2].append(ident)
        eq = ["| Item | Tier | Courses |", "|---|---|---|"]
        for key in sorted(equip, key=lambda k: (equip[k][1], k)):
            name, tier, used = equip[key]
            eq.append(f"| {name} | {tier} | {', '.join(f'`{u}`' for u in used)} |")
        result[phase5.overview] = {"status": "\n".join(status), "reachability": "\n".join(rch), "equipment": "\n".join(eq)}
    return result


def milestones_after(course: Course, stages: dict[str, list[Course]]) -> list[str]:
    """The milestones (and the capstone) from the course's own stage on."""
    index = STAGES.index(course.stage)
    return [c.id for s in STAGES[index:] for c in stages.get(s, []) if c.is_milestone]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--write", action="store_true", help="regenerate the generated blocks")
    parser.add_argument("--root", type=Path, default=ROOT, help=argparse.SUPPRESS)
    args = parser.parse_args(argv)
    report, stages, phase1, phase4, phase5 = check(args.root)
    sync_generated(args.root / "02-curriculum-outline.md", generated_tables(stages, phase1), args.write, report)
    for target, tables in phase4_tables(stages, phase1, phase4).items():
        path = phase4.domain_files[target] if target in phase4.domain_files else args.root / str(target)
        sync_generated(path, tables, args.write, report, optional=True)
    for path, tables in phase5_tables(stages, phase1, phase4, phase5).items():
        sync_generated(path, tables, args.write, report)
    if (args.root / SAFETY_REVIEW).exists():
        sync_generated(args.root / SAFETY_REVIEW, {"summary": review_summary(phase5)}, args.write, report)
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
    print(
        f"Curriculum OK — {len(stages)} stage(s), {len(courses)} courses, {modules} modules, {lessons} lessons, "
        f"{fmt(hours)} h; {len(phase4.entries)} competency entries, {len(phase4.rubric)} rubrics; "
        f"{len(phase5.docs)} course file(s), {sum(len(d.exercises) for d in phase5.docs.values())} exercise specs, "
        f"{sum(len(v) for d in phase5.docs.values() for v in d.items.values())} knowledge-check items"
        + (f" (developed: {', '.join(phase5.developed)})" if phase5.developed else "")
        + (
            f"; safety review: {sum(1 for _, v in phase5.review.values() if v)} of {len(phase5.review)} items logged"
            if phase5.review
            else ""
        )
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
