"""Tests for tools/export_curriculum.py: the Markdown-to-blocks conversion, quiz items and the drift check."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

import export_curriculum as ex
import validate_curriculum as vc


class BlocksTest(unittest.TestCase):
    def test_wrapped_paragraph_is_joined_and_links_lose_their_target(self) -> None:
        lines = ["**Why it matters:** an egg's age", "changes how it behaves ([`SAF-1`](../x.md)).", "", "Next."]
        self.assertEqual(
            ex.blocks(lines),
            [
                {"type": "paragraph", "text": "**Why it matters:** an egg's age changes how it behaves (`SAF-1`)."},
                {"type": "paragraph", "text": "Next."},
            ],
        )

    def test_list_follows_a_paragraph_without_a_blank_line(self) -> None:
        lines = ["*Safety.* Eggs:", "- keep them cool,", "  in their box;", "- wash your hands."]
        self.assertEqual(
            ex.blocks(lines),
            [
                {"type": "paragraph", "text": "*Safety.* Eggs:"},
                {
                    "type": "list",
                    "items": [
                        {"level": 0, "marker": "", "text": "keep them cool, in their box;"},
                        {"level": 0, "marker": "", "text": "wash your hands."},
                    ],
                },
            ],
        )

    def test_numbered_list_with_nested_bullets(self) -> None:
        lines = ["1. **Grade a box (45 min).** Test each.", "   - note how it lies;", "   - crack it.", "2. Rank them."]
        items = ex.blocks(lines)[0]["items"]
        self.assertEqual([(i["level"], i["marker"]) for i in items], [(0, "1."), (1, ""), (1, ""), (0, "2.")])
        self.assertEqual(items[0]["text"], "**Grade a box (45 min).** Test each.")

    def test_table_and_header_only_table(self) -> None:
        lines = ["| Fault | Cause | Fix |", "|---|---|---|", "| Shell in the egg | The rim | The counter |", "", "| Date | Cut |", "|---|---|"]
        self.assertEqual(
            ex.blocks(lines),
            [
                {"type": "table", "header": ["Fault", "Cause", "Fix"], "rows": [["Shell in the egg", "The rim", "The counter"]]},
                {"type": "table", "header": ["Date", "Cut"], "rows": []},
            ],
        )

    def test_generated_blocks_are_left_out(self) -> None:
        lines = ["<!-- BEGIN generated:lesson-x -->", "*1.5 h*", "<!-- END generated:lesson-x -->", "", "Text."]
        self.assertEqual(ex.blocks(lines), [{"type": "paragraph", "text": "Text."}])


class ItemTest(unittest.TestCase):
    def item(self, kind: str, question: str, answer: str) -> vc.Item:
        return vc.Item("EGG-1.M1.Q1", kind, "EGG-1.M1.L1", True, answer, question, "Because.")

    def test_multiple_choice_is_split_into_stem_and_options(self) -> None:
        out = ex.item_json(self.item("multiple choice", "An egg stands on end. Why? A) fresh B) older: its air cell grew C) cracked", "B"))
        self.assertEqual(out["question"], "An egg stands on end. Why?")
        self.assertEqual([o["key"] for o in out["options"]], ["A", "B", "C"])
        self.assertEqual(out["options"][1]["text"], "older: its air cell grew")
        self.assertEqual((out["type"], out["answer"], out["safety"]), ("MULTIPLE_CHOICE", "B", True))

    def test_scenario_keeps_its_text_and_has_no_options(self) -> None:
        out = ex.item_json(self.item("scenario", "The probe reads 69 °C. What now?", "Cook on to 75 °C."))
        self.assertEqual((out["options"], out["answer"]), ([], "Cook on to 75 °C."))

    def test_multiple_choice_with_an_answer_outside_its_options_is_refused(self) -> None:
        with self.assertRaises(ValueError):
            ex.item_json(self.item("multiple choice", "Which? A) one B) two", "D"))


class ExportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.files = ex.export()
        cls.index = json.loads(cls.files["index.json"])

    def test_every_developed_course_has_a_file_and_milestones_have_a_rubric(self) -> None:
        courses = self.index["courses"]
        with_content = [c["id"] for c in courses if c["hasContent"]]
        self.assertEqual(sorted(f"courses/{c}.json" for c in with_content), sorted(f for f in self.files if f != "index.json"))
        for course in courses:
            if course["kind"] == "COURSE":
                self.assertTrue(course["hasContent"], course["id"])
                self.assertEqual(course["rubric"], [])
            else:
                self.assertFalse(course["hasContent"], course["id"])
                self.assertTrue(course["rubric"], course["id"])

    def test_safety_counts_match_the_worksheet(self) -> None:
        self.assertEqual(sum(c["safetyItems"] for c in self.index["courses"]), 278)

    def test_committed_assets_are_up_to_date(self) -> None:
        self.assertEqual(ex.stale(self.files), [])

    def test_stale_reports_changed_missing_and_extra_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            ex.write({"index.json": "{}\n", "courses/A.json": "{}\n"}, out)
            self.assertEqual(ex.stale({"index.json": "{}\n", "courses/A.json": "{}\n"}, out), [])
            self.assertEqual(sorted(ex.stale({"index.json": "[]\n", "courses/B.json": "{}\n"}, out)), ["courses/A.json", "courses/B.json", "index.json"])
            ex.write({"index.json": "[]\n"}, out)
            self.assertEqual(sorted(p.name for p in out.rglob("*.json")), ["index.json"])


if __name__ == "__main__":
    unittest.main()
