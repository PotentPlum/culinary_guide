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

| ID | Lesson | Objectives | Prereqs | h | Moves |
|---|---|---|---|---|---|
| SAF-1.M1.L1 | Hands | [S] wash hands [S1] | — | 2 | SA-HYG→1 |
| SAF-1.M1.L2 | Boards | [S] separate boards | — | 2 | SA-HYG→2 |

- `SAF-1.M1.X1` *drill, recipe* — Audit.

**Assessment:** checklist.

### KNF-1 — Knives (6 h)

**Prerequisites:** SAF-1

#### KNF-1.M1 — Cuts (6 h)

| ID | Lesson | Objectives | Prereqs | h | Moves |
|---|---|---|---|---|---|
| KNF-1.M1.L1 | Grip | [S] grip | SAF-1.M1.L2 | 3 | TS-KNF→1 |
| KNF-1.M1.L2 | Dice | [S] dice | — | 3 | TS-KNF→2 |

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
        report, _, _ = vc.check(self.root)
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
        self.assertIn("| `TS-KNF` | 2 / 2 | · | · | · | · |", overview)
        self.assertIn("| S1 | 2 | 50 % | 50 % | 0 % | 70 / 30 / 0 % |", overview)


if __name__ == "__main__":
    unittest.main()
