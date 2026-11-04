"""Tests for tools/validate_curriculum.py, on small inline curricula (run: python3 -m unittest discover -s tools)."""

from __future__ import annotations

import contextlib
import io
import tempfile
import unittest
from pathlib import Path

import validate_curriculum as vc

PHASE1 = """# Phase 1

| ID | Competency | S1 | S2 | S3 | S4 | S5 | Target |
|---|---|---|---|---|---|---|---|
| TS-KNF | Knife skills | 2 | 3 | 3 | 3 | 3 | 3 |
| SA-HYG | Hygiene | 1 | 2 | 2 | 2 | 2 | 2 |

### 7.1 S1 — Foundations (10 h)

| ID | Course | Purpose | Hours | Prerequisites | Main competencies |
|---|---|---|---|---|---|
| SAF-1 | Safety | x | 4 | — | SA-HYG |
| KNF-1 | Knives | x | 6 | SAF-1 | TS-KNF |
| | **Total** | | **10** | | |
"""

SOURCES = "# Sources\n\n| # | Source |\n|---|---|\n| S1 | A book |\n"

S1_GOOD = """# S1

### SAF-1 — Safety (4 h)

**Prerequisites:** —

#### SAF-1.M1 — Hygiene (4 h)

| ID | Lesson | Objectives | Prereqs | h | Practice | Elapsed | Moves |
|---|---|---|---|---|---|---|---|
| SAF-1.M1.L1 | Hands | [S] wash hands [S1] | — | 2 | 1 | — | SA-HYG→1 |
| SAF-1.M1.L2 | Boards | [S] separate boards | — | 2 | 1 | 1 wk | SA-HYG→2 |

- `SAF-1.M1.X1` *drill, recipe* — Audit.

**Assessment:** checklist.

### KNF-1 — Knives (6 h)

**Prerequisites:** SAF-1

#### KNF-1.M1 — Cuts (6 h)

| ID | Lesson | Objectives | Prereqs | h | Practice | Elapsed | Moves |
|---|---|---|---|---|---|---|---|
| KNF-1.M1.L1 | Grip | [S] grip | SAF-1.M1.L2 | 3 | 2 | — | TS-KNF→1 |
| KNF-1.M1.L2 | Dice | [S] dice | — | 3 | 2 | — | TS-KNF→2 |

- `KNF-1.M1.X1` *framework, framework* — Dice.

**Assessment:** photos.
"""

OVERVIEW = """# Outline

<!-- BEGIN generated:summary -->
<!-- END generated:summary -->
<!-- BEGIN generated:coverage -->
<!-- END generated:coverage -->
<!-- BEGIN generated:support -->
<!-- END generated:support -->
<!-- BEGIN generated:load -->
<!-- END generated:load -->
"""


def entry(ident: str, title: str, levels: dict[int, str], fields: tuple[str, ...] = vc.ENTRY_FIELDS) -> str:
    """A Phase 4 competency entry with the given success rows (level -> evidence)."""
    lines = [f"### {ident} — {title}", ""]
    for name in fields:
        lines.append(f"**{name}:** x")
        if name == "Success criteria":
            lines += ["", "| Level | A successful demonstration | Evidence |", "|---|---|---|"]
            lines += [f"| {level} | Works | {evidence} |" for level, evidence in levels.items()]
            lines.append("")
    name = f"map-{ident.lower()}"
    lines += ["", f"<!-- BEGIN generated:{name} -->", f"<!-- END generated:{name} -->", ""]
    return "\n".join(lines)


PHASE1_MS = PHASE1.replace("| | **Total** |", "| MS-S1 | Milestone | x | 1 | KNF-1 | TS-KNF |\n| | **Total** |")

MILESTONE = (
    "\n### MS-S1 — Milestone (1 h)\n\n#### MS-S1.M1 — Gate (1 h)\n\n"
    "| ID | Lesson | Objectives | Prereqs | h | Practice | Elapsed | Moves |\n|---|---|---|---|---|---|---|---|\n"
    "| MS-S1.M1.L1 | Gate | Re-tests | — | 1 | 1 | — | TS-KNF→2, SA-HYG→1 |\n"
)

RUBRIC = """# Assessment

| ID | Criterion | Competent means | Components | Competencies | Evidence | Weight |
|---|---|---|---|---|---|---|
| MS-S1.R1 | Accuracy | Even dice | MS-S1.M1.L1 | TS-KNF | measure, photo | 100 |
| MS-S1.R2 | Food safety | Clean board | MS-S1.M1.L1 | SA-HYG | checklist | gate |
"""


class ValidateCurriculumTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root / "02-outline").mkdir()
        (self.root / "01-vision-architecture.md").write_text(PHASE1, encoding="utf-8")
        (self.root / "sources.md").write_text(SOURCES, encoding="utf-8")
        (self.root / "02-curriculum-outline.md").write_text(OVERVIEW, encoding="utf-8")

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def errors(self, s1: str) -> list[str]:
        (self.root / "02-outline" / "S1-foundations.md").write_text(s1, encoding="utf-8")
        report, _, _, _ = vc.check(self.root)
        return report.errors

    def run_main(self, *args: str) -> int:
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            return vc.main(["--root", str(self.root), *args])

    def test_a_consistent_outline_passes(self) -> None:
        self.assertEqual(self.errors(S1_GOOD), [])

    def test_lesson_hours_must_add_up_to_the_module(self) -> None:
        bad = S1_GOOD.replace("| SAF-1.M1.L2 | Boards | [S] separate boards | — | 2 |", "| SAF-1.M1.L2 | Boards | [S] separate boards | — | 1 |")
        self.assertIn("SAF-1.M1: lessons add up to 3 h, heading says 4 h", self.errors(bad))

    def test_drift_beyond_ten_percent_is_an_error(self) -> None:
        bad = (
            S1_GOOD.replace("### KNF-1 — Knives (6 h)", "### KNF-1 — Knives (8 h)")
            .replace("#### KNF-1.M1 — Cuts (6 h)", "#### KNF-1.M1 — Cuts (8 h)")
            .replace("| Dice | [S] dice | — | 3 |", "| Dice | [S] dice | — | 5 |")
        )
        self.assertIn("KNF-1: 8 h drifts more than 10 % from Phase 1 (6 h)", self.errors(bad))

    def test_unknown_and_later_prerequisites_are_errors(self) -> None:
        unknown = S1_GOOD.replace("| Grip | [S] grip | SAF-1.M1.L2 |", "| Grip | [S] grip | SAF-1.M1.L9 |")
        self.assertIn("KNF-1.M1.L1: unknown prerequisite SAF-1.M1.L9", self.errors(unknown))
        later = S1_GOOD.replace("| Hands | [S] wash hands [S1] | — |", "| Hands | [S] wash hands [S1] | KNF-1.M1.L2 |")
        self.assertTrue(any("prerequisite KNF-1.M1.L2" in e for e in self.errors(later)))

    def test_a_same_stage_prerequisite_needs_the_course_prerequisite(self) -> None:
        bad = S1_GOOD.replace("**Prerequisites:** SAF-1\n", "**Prerequisites:** —\n")
        self.assertIn("KNF-1.M1.L1: prerequisite SAF-1.M1.L2 is in SAF-1, which KNF-1 doesn't require", self.errors(bad))

    def test_coverage_below_the_stage_target_is_an_error(self) -> None:
        bad = S1_GOOD.replace("| TS-KNF→2 |", "| TS-KNF→1 |")
        self.assertIn("coverage: TS-KNF reaches 1 by the end of S1, target 2", self.errors(bad))

    def test_unknown_competency_and_bad_move_are_errors(self) -> None:
        errors = self.errors(S1_GOOD.replace("| TS-KNF→2 |", "| TS-KNIF→2, TS-KNF→2 |").replace("| SA-HYG→1 |", "| SA-HYG 1 |"))
        self.assertIn("KNF-1.M1.L2: unknown competency TS-KNIF", errors)
        self.assertTrue(any("bad competency move 'SA-HYG 1'" in e for e in errors))

    def test_modules_need_an_exercise_and_an_assessment(self) -> None:
        bad = S1_GOOD.replace("- `SAF-1.M1.X1` *drill, recipe* — Audit.\n\n**Assessment:** checklist.\n", "")
        errors = self.errors(bad)
        self.assertIn("SAF-1.M1: has no practical exercise", errors)
        self.assertIn("SAF-1.M1: has no **Assessment:** line", errors)

    def test_unknown_source_is_an_error(self) -> None:
        bad = S1_GOOD.replace("wash hands [S1]", "wash hands [S9]")
        self.assertIn("02-outline/S1-foundations.md: source [S9] is not in sources.md", self.errors(bad))

    def test_generated_tables_are_written_then_checked(self) -> None:
        (self.root / "02-outline" / "S1-foundations.md").write_text(S1_GOOD, encoding="utf-8")
        self.assertEqual(self.run_main(), 1)  # the tables are still empty
        self.assertEqual(self.run_main("--write"), 0)
        self.assertEqual(self.run_main(), 0)
        overview = (self.root / "02-curriculum-outline.md").read_text(encoding="utf-8")
        self.assertIn("| `TS-KNF` | 2 / 2 | · | · | · | · | 4 (level 2 needs 1) |", overview)
        self.assertIn("| S1 | 2 | 50 % | 50 % | 0 % | 70 / 30 / 0 % |", overview)
        self.assertIn("| S1 | 4 | 10 | 6 | 1 | 2 | 2 | 1 |", overview)

    def test_sections_must_cover_the_modules_and_add_up(self) -> None:
        def with_section(row: str) -> str:
            table = "| ID | Section | Modules | h |\n|---|---|---|---|\n" + row + "\n"
            return S1_GOOD.replace("**Prerequisites:** —\n", "**Prerequisites:** —\n\n" + table, 1)

        self.assertEqual(self.errors(with_section("| SAF-1.SEC1 | All | M1–M1 | 4 |")), [])
        self.assertIn("SAF-1.SEC1: modules add up to 4 h, section says 3 h", self.errors(with_section("| SAF-1.SEC1 | All | M1–M1 | 3 |")))
        self.assertIn(
            "SAF-1.SEC1: modules M2–M2 don't continue from the previous section",
            self.errors(with_section("| SAF-1.SEC1 | All | M2–M2 | 4 |")),
        )

    def test_a_level_needs_enough_practical_sessions(self) -> None:
        bad = S1_GOOD.replace("| Grip | [S] grip | SAF-1.M1.L2 | 3 | 2 |", "| Grip | [S] grip | SAF-1.M1.L2 | 3 | 0 |").replace(
            "| Dice | [S] dice | — | 3 | 2 |", "| Dice | [S] dice | — | 3 | 0 |"
        )
        self.assertIn(
            "practice: TS-KNF is taught to level 2 by the end of S1 with 0 practical session(s); level 2 needs 1",
            self.errors(bad),
        )

    def test_elapsed_time_and_session_hours_are_checked(self) -> None:
        errors = self.errors(S1_GOOD.replace("| 1 | 1 wk |", "| 1 | a week |").replace("| Dice | [S] dice | — | 3 | 2 |", "| Dice | [S] dice | — | 3 | 7 |"))
        self.assertTrue(any("bad elapsed time 'a week'" in e for e in errors))
        self.assertIn("KNF-1.M1.L2: 7 practical sessions don't fit in 3 h", errors)

    def test_every_competency_needs_a_milestone_retest_once_all_stages_exist(self) -> None:
        for stage in ("S2", "S3", "S4", "S5"):
            (self.root / "02-outline" / f"{stage}-x.md").write_text("# empty\n", encoding="utf-8")
        errors = self.errors(S1_GOOD)
        self.assertIn("assessment: TS-KNF is not re-tested in any milestone or the capstone", errors)
        retest = S1_GOOD + (
            "\n### MS-S1 — Milestone (1 h)\n\n#### MS-S1.M1 — Gate (1 h)\n\n"
            "| ID | Lesson | Objectives | Prereqs | h | Practice | Elapsed | Moves |\n|---|---|---|---|---|---|---|---|\n"
            "| MS-S1.M1.L1 | Gate | Re-tests | — | 1 | 1 | — | TS-KNF→2, SA-HYG→1 |\n"
        )
        self.assertFalse([e for e in self.errors(retest) if e.startswith("assessment:")])


    def write_entries(self, knf: str | None = None, hyg: str | None = None) -> None:
        folder = self.root / "04-competencies"
        folder.mkdir(exist_ok=True)
        knf = knf if knf is not None else entry("TS-KNF", "Knife skills", {2: "photo, checklist", 3: "measure, photo"})
        hyg = hyg if hyg is not None else entry("SA-HYG", "Hygiene", {2: "checklist, photo"})
        (folder / "TS.md").write_text("# TS\n\n" + knf, encoding="utf-8")
        (folder / "SA.md").write_text("# SA\n\n" + hyg, encoding="utf-8")

    def test_complete_competency_entries_pass(self) -> None:
        self.write_entries()
        self.assertEqual(self.errors(S1_GOOD), [])

    def test_entries_need_every_field_and_a_success_row_per_level(self) -> None:
        fields = tuple(f for f in vc.ENTRY_FIELDS if f != "Independence")
        self.write_entries(knf=entry("TS-KNF", "Knife skills", {2: "photo", 4: "photo"}, fields))
        errors = self.errors(S1_GOOD)
        self.assertIn("TS-KNF: missing **Independence:**", errors)
        self.assertIn("TS-KNF: no success criteria for level 3", errors)
        self.assertIn("TS-KNF: success criteria for level 4, outside 2–3 (the core target)", errors)
        swapped = ("Knowledge", "Elements") + vc.ENTRY_FIELDS[2:]
        self.write_entries(knf=entry("TS-KNF", "Knife skills", {2: "photo", 3: "photo"}, swapped))
        self.assertTrue(any(e.startswith("TS-KNF: fields out of order") for e in self.errors(S1_GOOD)))

    def test_a_level_cannot_rest_on_self_assessment_alone(self) -> None:
        self.write_entries(hyg=entry("SA-HYG", "Hygiene", {2: "checklist, tasters"}))
        self.assertIn("SA-HYG: level 2 rests on self-assessed evidence only", self.errors(S1_GOOD))
        self.write_entries(hyg=entry("SA-HYG", "Hygiene", {2: "checklist, guesswork"}))
        self.assertTrue(any("unknown evidence method 'guesswork'" in e for e in self.errors(S1_GOOD)))

    def test_entries_belong_in_their_domain_file_and_none_may_be_missing(self) -> None:
        self.write_entries(hyg=entry("SA-HYG", "Hygiene", {2: "photo"}) + entry("TS-MEP", "Mise", {2: "photo"}))
        errors = self.errors(S1_GOOD)
        self.assertTrue(any("TS-MEP belongs in 04-competencies/TS.md" in e for e in errors))
        self.assertIn("04-competencies/SA.md: TS-MEP is not a Phase 1 competency", errors)
        self.write_entries(knf="")
        self.assertIn("TS-KNF: no entry in 04-competencies/TS.md", self.errors(S1_GOOD))

    def test_mapping_blocks_are_written_then_checked(self) -> None:
        self.write_entries()
        (self.root / "01-vision-architecture.md").write_text(PHASE1_MS, encoding="utf-8")
        (self.root / "02-outline" / "S1-foundations.md").write_text(S1_GOOD + MILESTONE, encoding="utf-8")
        (self.root / "04-competency-framework.md").write_text(
            "<!-- BEGIN generated:traceability -->\n<!-- END generated:traceability -->\n"
            "<!-- BEGIN generated:progression -->\n<!-- END generated:progression -->\n",
            encoding="utf-8",
        )
        self.assertEqual(self.run_main("--write"), 0)
        self.assertEqual(self.run_main(), 0)
        ts = (self.root / "04-competencies" / "TS.md").read_text(encoding="utf-8")
        self.assertIn("*Targets:* S1 2 · S2 3 · S3 3 · S4 3 · S5 3 · core **3**", ts)
        self.assertIn("| S1 | 2 | 2 | `KNF-1` M1.L1→1, M1.L2→2 | 4 (4) | `KNF-1` M1.X1 | `MS-S1` M1.L1→2 |", ts)
        overview = (self.root / "04-competency-framework.md").read_text(encoding="utf-8")
        self.assertIn("| [`TS-KNF`](04-competencies/TS.md#ts-knf--knife-skills) | 3 | 1 | 1 | 2 | 1 | 4 | 1 |", overview)
        self.assertIn("| S1 | 2 | 2 | 1 | 0 | `MS-S1` (1 h) |", overview)
        (self.root / "04-competencies" / "TS.md").write_text(ts.replace("4 (4)", "5 (5)"), encoding="utf-8")
        self.assertEqual(self.run_main(), 1)

    def rubric_errors(self, rubric: str) -> list[str]:
        (self.root / "01-vision-architecture.md").write_text(PHASE1_MS, encoding="utf-8")
        (self.root / "04-assessment.md").write_text(rubric, encoding="utf-8")
        return self.errors(S1_GOOD + MILESTONE)

    def test_a_complete_rubric_passes(self) -> None:
        self.assertEqual(self.rubric_errors(RUBRIC), [])

    def test_rubric_weights_gates_and_retests_are_checked(self) -> None:
        self.assertIn("MS-S1: rubric weights add up to 90, not 100", self.rubric_errors(RUBRIC.replace("| 100 |", "| 90 |")))
        no_gate = self.rubric_errors(RUBRIC.replace("| gate |", "| 0 |"))
        self.assertIn("MS-S1.R2: food safety is a gate (weight 'gate'), and only food safety is", no_gate)
        self.assertIn("MS-S1: the rubric has no food-safety gate", no_gate)
        dropped = RUBRIC.replace("| MS-S1.R2 | Food safety | Clean board | MS-S1.M1.L1 | SA-HYG | checklist | gate |\n", "")
        errors = self.rubric_errors(dropped)
        self.assertIn("MS-S1: MS-S1.M1.L1 re-tests SA-HYG, but no rubric row scores it", errors)
        self.assertIn("MS-S1: the rubric has no self-assessed evidence", errors)
        self.assertIn(
            "MS-S1.R1: TS-MEP is not re-tested by MS-S1.M1.L1 in the outline",
            self.rubric_errors(RUBRIC.replace("| TS-KNF |", "| TS-KNF, TS-MEP |")),
        )
        self.assertTrue(any("criterion 'speed'" in e for e in self.rubric_errors(RUBRIC.replace("| Accuracy |", "| Speed |"))))


if __name__ == "__main__":
    unittest.main()
