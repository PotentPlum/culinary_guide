"""Tests for tools/legacy_to_backup.py (spec §13.3, Appendix A). Run: python3 -m unittest discover -s tools

The synthetic exports in data/fixtures/legacy/ cover every mapping branch; tools/validate_data.py checks that they
convert to exactly expected-backup.json, and BackupManagerTest restores that file in the app. These tests pin the
individual rules, so a change to the fixture can't silently drop one.
"""

from __future__ import annotations

import io
import json
import tempfile
import unittest
import zipfile
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

import legacy_to_backup as legacy

FIXTURES = legacy.DATA / "fixtures" / "legacy"
CONTENT = legacy.Content.load()
EXPORTED_AT = 1793620800000

R_JSON = "0b6f6a52-1c1e-4b55-9d61-1f0c3f1a0001"
R_COLUMNS = "0b6f6a52-1c1e-4b55-9d61-1f0c3f1a0002"
R_PHOTO = "0b6f6a52-1c1e-4b55-9d61-1f0c3f1a0003"
R_LEGACY = "0b6f6a52-1c1e-4b55-9d61-1f0c3f1a0004"
Q_DONE = "7c1d2e3f-0000-4000-8000-00000000c001"
Q_OPEN = "7c1d2e3f-0000-4000-8000-00000000c002"


def fixture(name: str) -> dict:
    return json.loads((FIXTURES / name).read_text(encoding="utf-8"))


def convert(supabase: dict | None = None, local: dict | None = None) -> tuple[dict, list[str]]:
    converter = legacy.Converter(CONTENT, EXPORTED_AT)
    backup = converter.convert(supabase if supabase is not None else fixture("supabase.json"), local)
    return backup, converter.report.lines


class FixtureConversion(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.backup, cls.report = convert(fixture("supabase.json"), fixture("local.json"))
        cls.recipes = {r["id"]: r for r in cls.backup["recipes"]}
        cls.quests = {q["id"]: q for q in cls.backup["quests"]}
        cls.attempts = {a["id"][-2:]: a for a in cls.backup["attempts"]}

    def items(self, recipe_id: str) -> list[dict]:
        return [i for g in self.recipes[recipe_id]["ingredientGroups"] for i in g["items"]]

    def tags(self, recipe_id: str) -> set[tuple[str, str]]:
        return {(t["key"], t["value"]) for t in self.recipes[recipe_id].get("tags", [])}

    def test_output_is_a_valid_backup(self):
        self.assertEqual([], legacy.check(self.backup, CONTENT))

    def test_json_in_instructions_is_the_primary_source(self):
        recipe = self.recipes[R_JSON]
        self.assertEqual("Pompoensoep met salie", recipe["title"])
        self.assertEqual((15, 30), (recipe["prepMinutes"], recipe["cookMinutes"]))
        # Default section names ("Ingredients", "Instructions") become untitled; a trailing colon goes.
        self.assertEqual([None, "Topping"], [g["name"] for g in recipe["ingredientGroups"]])
        self.assertEqual(["Snijd de pompoen in blokjes.", "Kook 20 minuten en pureer."], [s["text"] for s in recipe["stepGroups"][0]["steps"]])

    def test_set_columns_override_the_json_and_empty_ones_do_not(self):
        recipe = self.recipes[R_JSON]
        self.assertEqual("Romige soep van pompoen.", recipe["description"])
        self.assertEqual(4, recipe["servings"])
        self.assertEqual("Example", recipe["author"])  # the column is null, so the JSON's author stays
        self.assertEqual(["soep", "herfst"], recipe["sourceMeta"]["keywords"])  # the column is '{}'

    def test_text_metric_wins_and_ingredients_are_text_only(self):
        items = self.items(R_JSON)
        self.assertEqual(["1 kg pompoen", "2 uien, gesnipperd", "een handje salie"], [i["text"] for i in items])
        self.assertTrue(all(i["parseSource"] == "NONE" and "name" not in i for i in items))
        self.assertEqual("NONE", self.recipes[R_JSON]["enrichState"])

    def test_metadata_goes_into_source_meta(self):
        meta = self.recipes[R_JSON]["sourceMeta"]
        self.assertEqual((4.5, 12, "2025-10-01"), (meta["ratingValue"], meta["ratingCount"], meta["datePublished"]))
        self.assertEqual({"calories": "210 kcal"}, meta["nutrition"])
        self.assertEqual(["VegetarianDiet"], meta["diet"])
        self.assertEqual("https://video.example.com/1", self.recipes[R_COLUMNS]["sourceMeta"]["video"])

    def test_tags_map_and_prep_time_and_unknown_values_are_dropped(self):
        self.assertEqual({("CUISINE", "Nederlands"), ("DISH_TYPE", "Soep"), ("TECHNIQUE", "sauteren")}, self.tags(R_JSON))
        self.assertIn(f"recipe {R_JSON}: dropped unknown mainIngredient tag 'Knollen'", self.report)

    def test_column_fallback_when_instructions_are_not_json(self):
        recipe = self.recipes[R_COLUMNS]
        self.assertEqual("Tomatensalade", recipe["title"])
        self.assertEqual(["4 tomaten", "1 rode ui", "olijfolie"], [i["text"] for i in self.items(R_COLUMNS)])
        self.assertEqual(["Snijd de tomaten.", "Meng met de ui en olie."], [s["text"] for s in recipe["stepGroups"][0]["steps"]])
        self.assertEqual((2, 10, 10), (recipe["servings"], recipe["prepMinutes"], recipe["totalMinutes"]))
        self.assertNotIn("cookMinutes", recipe)  # 0 means unknown
        self.assertEqual({("CUISINE", "Italiaans"), ("DISH_TYPE", "Salade")}, self.tags(R_COLUMNS))
        self.assertNotIn("imageUrl", recipe)  # a data: URL is not kept

    def test_source_types(self):
        self.assertEqual(("URL", "https://www.example.nl/pompoensoep", "example.nl"), self.source(R_JSON))
        self.assertEqual(("URL", "http://salades.example.com/tomaat?id=3", "salades.example.com"), self.source(R_COLUMNS))
        self.assertEqual(("PHOTO", None, "Tobias"), self.source(R_PHOTO))
        self.assertEqual(("LEGACY", None, "Oma"), self.source(R_LEGACY))

    def source(self, recipe_id: str) -> tuple:
        r = self.recipes[recipe_id]
        return r["sourceType"], r.get("sourceUrl"), r["sourceLabel"]

    def test_postgres_timestamps(self):
        self.assertEqual(1779358272123, self.recipes[R_JSON]["createdAt"])  # 2026-05-21T10:11:12.123456+00:00
        self.assertEqual(1779436800000, self.recipes[R_JSON]["updatedAt"])
        self.assertEqual(1779526800000, self.recipes[R_COLUMNS]["createdAt"])  # "2026-05-23 09:00:00+00"

    def test_local_storage_merges_by_remote_id_and_url_and_adds_the_rest(self):
        self.assertEqual(5, sum(r["inLibrary"] for r in self.backup["recipes"]))
        # By remoteId: the taxonomy brings technique tags (only real skills) and the cuisine.
        self.assertEqual({("TECHNIQUE", "koken"), ("CUISINE", "Nederlands")}, self.tags(R_LEGACY))
        self.assertIn(f"recipe {R_LEGACY}: dropped technique 'koekenpan' (not in techniques.json or not a skill)", self.report)
        # By URL (no remoteId): the Supabase row keeps its id.
        self.assertIn(("TECHNIQUE", "sauteren"), self.tags(R_JSON))
        local_only = [r for r in self.backup["recipes"] if r["inLibrary"] and r["id"] not in (R_JSON, R_COLUMNS, R_PHOTO, R_LEGACY)]
        self.assertEqual(["Geroosterde spruitjes"], [r["title"] for r in local_only])
        self.assertEqual(1779900000000, local_only[0]["createdAt"])  # savedAt
        self.assertEqual({("DISH_TYPE", "Bijgerecht"), ("MAIN_INGREDIENT", "Groenten")}, self.tags(local_only[0]["id"]))

    def test_challenge_becomes_quest_with_a_hidden_recipe(self):
        quest = self.quests[Q_DONE]
        recipe = self.recipes[quest["recipeId"]]
        self.assertEqual(("QUEST", False), (recipe["sourceType"], recipe["inLibrary"]))
        self.assertEqual(
            [("2 Prei", "prei"), ("100 g koude boter", "koude boter"), ("zout", "zout")],
            [(i["text"], i["name"]) for i in self.items(recipe["id"])],
        )
        self.assertEqual(["Reduceer de wijn met sjalot.", "Monteer met de boter."], [s["text"] for s in recipe["stepGroups"][0]["steps"]])
        self.assertEqual(["Houd de saus lauw."], quest["tips"])
        self.assertEqual({("CUISINE", "Frans"), ("DISH_TYPE", "Saus/Basis"), ("TECHNIQUE", "emulgeren")}, self.tags(recipe["id"]))
        expected = {
            "ingredientMode": "CUSTOM",
            "isDaily": False,
            "questType": "SAUS",
            "difficulty": "INTERMEDIATE",
            "requestedTechniqueId": "emulgeren",
            "requestedCuisine": "Frans",
            "timeLimit": "UNDER_60",
            "simpleAndSmall": True,
            "quantity": 500.0,
            "unit": "g",
            "skill": "emulgeren",
            "timeEstimateMinutes": 40,
            "dishType": "Saus/Basis",
        }
        self.assertEqual(expected, {k: quest[k] for k in expected})

    def test_legacy_difficulty_and_missing_fields(self):
        quest = self.quests[Q_OPEN]
        self.assertEqual(("ADVANCED", "MAALTIJD", "OPEN"), (quest["difficulty"], quest["questType"], quest["status"]))
        self.assertEqual(("", "", 0), (quest["skill"], quest["why"], quest["timeEstimateMinutes"]))
        self.assertEqual([], self.recipes[quest["recipeId"]]["ingredientGroups"])

    def test_quest_status_follows_attempts(self):
        quest = self.quests[Q_DONE]
        self.assertEqual("COMPLETED", quest["status"])
        self.assertEqual(self.attempts["a2"]["cookedAt"], quest["completedAt"])

    def test_attempts_link_and_unresolved_ids_are_dropped(self):
        a1, a2, a3 = self.attempts["a1"], self.attempts["a2"], self.attempts["a3"]
        self.assertEqual(("RECIPE", R_JSON), (a1["sourceType"], a1["recipeId"]))
        self.assertEqual(("QUEST", Q_DONE, self.quests[Q_DONE]["recipeId"]), (a2["sourceType"], a2["questId"], a2["recipeId"]))
        self.assertNotIn("recipeId", a3)
        self.assertEqual("Recept was verwijderd.", a3["notes"])

    def test_scores_notes_and_tags_are_kept(self):
        a1 = self.attempts["a1"]
        self.assertEqual((7, 9), (a1["score"], a1["tasteScore"]))  # legacy 1–10 scores stay; the app shows the nearest step
        self.assertEqual("Meer salie.\nVolgende keer gember.", a1["notes"])
        self.assertEqual((["blancheren"], ["Nederlands"]), (a1["techniques"], a1["cuisines"]))
        self.assertNotIn("notes", self.attempts["a2"])  # blank
        self.assertNotIn("score", self.attempts["a4"])  # 0 is not a score

    def test_veg_ids_and_peak_veg_ids(self):
        # October: pompoen from the recipe and prei from the attempt's ingredients[], both at peak.
        self.assertEqual((["leek", "pumpkin"], ["leek", "pumpkin"]), (self.attempts["a1"]["vegIds"], self.attempts["a1"]["peakVegIds"]))
        # June: prei is only Verkrijgbaar.
        self.assertEqual((["leek"], []), (self.attempts["a2"]["vegIds"], self.attempts["a2"]["peakVegIds"]))
        self.assertEqual(["asparagus-green"], self.attempts["a3"]["peakVegIds"])

    def test_conversion_is_deterministic(self):
        again, _ = convert(fixture("supabase.json"), fixture("local.json"))
        self.assertEqual(self.backup, again)


class Rules(unittest.TestCase):
    def test_forms_match_the_kotlin_matcher(self):
        self.assertLessEqual({"tomaat", "tomaten"}, legacy.forms("tomaat"))
        self.assertIn("radijzen", legacy.forms("radijs"))
        self.assertIn("sjalotten", legacy.forms("sjalot"))
        self.assertIn("wortel", legacy.forms("wortelen"))

    def test_matching_is_whole_word_and_diacritic_insensitive(self):
        matcher = legacy.SeasonalMatcher(CONTENT.vegetables)
        self.assertEqual(["corn"], matcher.vegetable_ids(["2 kolven suikermais"]))
        self.assertEqual([], matcher.vegetable_ids(["preisoep"]))  # part of a word
        self.assertEqual(["leek"], matcher.vegetable_ids(["3 preien"]))

    def test_millis(self):
        self.assertEqual(0, legacy.millis("1970-01-01T00:00:00Z"))
        self.assertEqual(3_600_000, legacy.millis("1970-01-01 02:00:00+01"))
        self.assertEqual(1, legacy.millis("1970-01-01T00:00:00.001234567+00:00"))
        self.assertEqual(1780000000000, legacy.millis(1780000000000))
        self.assertIsNone(legacy.millis("gisteren"))

    def test_a_local_export_alone_is_enough(self):
        backup, _ = convert({"format": "mise-legacy-supabase"}, fixture("local.json"))
        self.assertEqual(3, len(backup["recipes"]))
        self.assertEqual([], legacy.check(backup, CONTENT))

    def test_unusable_ids_get_a_stable_derived_id(self):
        row = {"id": "not a uuid!", "name": "Soep", "instructions": "", "source_url": "https://a.example/b", "created_at": 1}
        first, report = convert({"recipes": [row]})
        second, _ = convert({"recipes": [row]})
        self.assertEqual(first["recipes"][0]["id"], second["recipes"][0]["id"])
        self.assertRegex(first["recipes"][0]["id"], legacy.ID_PATTERN)
        self.assertTrue(any("no usable id" in line for line in report))

    def test_unknown_difficulty_and_attempt_source(self):
        backup, report = convert(
            {
                "challenges": [{"id": "c", "ingredient": "prei", "challenge_text": "x", "difficulty": "pittig", "created_at": 1}],
                "user_attempts": [{"id": "a", "source_type": "dinner", "attempted_at": 1}],
            }
        )
        self.assertEqual("INTERMEDIATE", backup["quests"][0]["difficulty"])
        self.assertEqual([], backup["attempts"])
        self.assertIn("attempt a: skipped: unknown source_type 'dinner'", report)


class CommandLine(unittest.TestCase):
    def run_main(self, *args: str) -> tuple[int, str]:
        out = io.StringIO()
        with redirect_stdout(out), redirect_stderr(io.StringIO()):
            code = legacy.main(list(args))
        return code, out.getvalue()

    def test_writes_a_zip_with_only_backup_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "Mise-backup-legacy.zip"
            code, out = self.run_main(str(FIXTURES / "supabase.json"), str(FIXTURES / "local.json"), "-o", str(target))
            self.assertEqual(0, code)
            self.assertIn("5 recipes, 2 quests (1 completed), 4 attempts", out)
            with zipfile.ZipFile(target) as zip_file:
                self.assertEqual(["backup.json"], zip_file.namelist())
                backup = json.loads(zip_file.read("backup.json"))
            self.assertEqual([], legacy.check(backup, CONTENT))

    def test_accepts_the_sql_editor_wrapping(self):
        with tempfile.TemporaryDirectory() as tmp:
            wrapped = Path(tmp) / "supabase.json"
            wrapped.write_text(json.dumps([{"json_build_object": fixture("supabase.json")}]), encoding="utf-8")
            code, _ = self.run_main(str(wrapped), "-o", str(Path(tmp) / "out.zip"))
            self.assertEqual(0, code)

    def test_refuses_the_wrong_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "out.zip"
            code, _ = self.run_main(str(FIXTURES / "local.json"), "-o", str(target))
            self.assertEqual(1, code)
            self.assertFalse(target.exists())


if __name__ == "__main__":
    unittest.main()
