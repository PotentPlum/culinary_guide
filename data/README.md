# data/ — content and fixtures for the Mise Android app

Everything here is hand-curated content that you can't easily download again, rescued from the retired
Next.js app (last web commit: `e76778c`). It is stored in neutral formats (JSON, Android XML,
Markdown) so the Kotlin app reads it directly. Nothing in this folder is generated at build time.

Run the validator after **any** edit (a Claude Code hook does this automatically):

```bash
python3 tools/validate_data.py
```

## Layout and how each part is integrated

| Path | What | Integrated as |
|---|---|---|
| `assets/vegetables.json` | 66 Dutch locally grown vegetables: Dutch name, family, colours, peak/shoulder months, blurb, storage, prep, technique tip, pairings, substitutions, curated recipe links | APK asset → `ContentRepository` (spec §7.1) |
| `assets/techniques.json` | 74 cooking techniques in Dutch: body text, tips, cookware, level, heat, **discipline** (null for the 3 cookware guides), related techniques, related vegetables (by id) | APK asset → `ContentRepository` |
| `assets/facets.json` | Recipe tag vocabulary: cuisine (with colours), dish type, main ingredient, prep time | APK asset; also the enum source for Claude output schemas (spec §8) |
| `assets/disciplines.json` | The 3 skill disciplines (Dagboek bars) with Dutch labels, in display order | APK asset |
| `assets/units.json` | 28 measurement units: Dutch singular/plural, parser aliases, dimension (MASS / VOLUME / COUNT / OTHER) and factor to g / ml / stuks | APK asset → local ingredient parser, scaling, shopping list (spec §11.2) |
| `assets/archetypes.json` | 7 Dagboek badges as typed rules (`techniques`, `distinctPeakVegetables`, `peakMonths`) | APK asset → `BadgeEvaluator` (spec §12) |
| `res/values/strings.xml` | Dutch UI strings, plurals and string arrays (`months_short` / `months_long` / `months_initial`, `score_steps`) | Android resource dir (`res.srcDir`) |
| `fixtures/recipe-ingestion/golden.json` | 104 recorded input→output cases for the recipe parser, from the 94 passing TS tests | JVM unit-test resource; the Kotlin port must reproduce all of them |
| `fixtures/seasonality/status.json` | Expected `peak/shoulder/soon/ending/off` for every vegetable × month (792 cells) | JVM unit-test resource |
| `schemas/*.schema.json` | JSON Schemas for every asset and fixture file | Used by `tools/validate_data.py`; Kotlin tests may reuse them |
| `reference/velt-vegetables.json` | Original scraped velt.nu source data (77 vegetables, Dutch text, harvest months) | **Not shipped.** Third-party text for research and adding vegetables; do not copy its prose into the app |
| `reference/prompts/*.md` | The three Claude prompts from the web app, word for word | **Not shipped.** Source material for the rewritten prompts (spec §8) |

Gradle wiring (from spec §4.3):

```kotlin
android.sourceSets {
    getByName("main") {
        assets.srcDir(rootProject.file("data/assets"))
        res.srcDir(rootProject.file("data/res"))
    }
    getByName("test") { resources.srcDir(rootProject.file("data/fixtures")) }
}
```

## Changes from the web sources (all deliberate)

- **camelCase keys and `schemaVersion: 1`** at the root of every file, so the app can refuse files it doesn't understand.
- **Vegetables:** `name` → `nameEn` (an internal English alias for search, never shown); `nutrients` →
  `nutrientsEn` (it *is* English, so translate before ever showing it); `url` → `sourceUrl`;
  `recipes` → `curatedRecipes` with `time_mins` → `timeMinutes`; `shoulder` is always present (may be `[]`).
- **Techniques:** `relatedVeg` (English names, fragile) → `relatedVegIds` (vegetable ids).
  `discipline` is folded in from `disciplineMap.ts`, so one file is the source of truth.
- **Disciplines (spec v2 review):** "material" was dropped. It was fed only by three cookware pages (koekenpan, braadpan,
  steelpan), which are reference guides, not skills, so they now have `discipline: null`. `messenonderhoud` moved to
  `mechanical`.
- **Archetypes → badges:** the TypeScript `check` functions became typed rules. The `techniques` rule keeps the web logic: count
  attempts with ≥1 matching technique; if `minAvgScore` is set, average their non-null execution scores; no scores means
  not earned. Spec v2 review changes: *Stille Fermenteur* no longer counts `marineren`; thresholds were lowered so badges
  are reachable in weeks (emulsie 10→6, messen 10→8); two seasonal badges were added (*De Seizoenskok*: 15 distinct vegetables
  cooked at peak; *Het Hele Jaar Rond*: a peak vegetable cooked in all 12 months).
- **strings.xml:** keys are the `UI.*` paths in snake_case (`recipes.saveBtn` → `recipes_save_btn`).
  Web-only groups were dropped (`auth`, `footer`, `futurePeek`, `home.modules`, `nav.soon`, and later
  `profile_discipline_material`), and the archetype names live in `archetypes.json` instead. The two template functions became format strings (`home_spotlight_more` →
  `%1$d`, `home_tip_link` → `%1$s`). P1 renamed "Uitdaging" → "Quest" (`challenge_*` → `quest_*`) and "Profiel" → "Dagboek" (`profile_*` → `journal_*`), and
  pruned web-only copy (dropped views, US/metric toggle, nutrition labels, "binnenkort" notes).
- **strings.xml, UI/UX review (spec Appendix D):** one season vocabulary (Op z'n best / Verkrijgbaar / Komt eraan; the "aflopend"
  strings are gone), one difficulty scale (`level_*` replaces three key sets), headlines as single strings with `<i>` emphasis instead
  of `*_em` / `*_end` fragments, "Zonder cijfers vastleggen", and new strings for Vanavond, cook sessions, Dagboek segments and first run.
- **Vegetable colours:** `radish`, `wild-garlic` and `watercress` fills were lightened slightly so ink initials reach 4.5:1.

## Content rules (unchanged from the web app)

- **Dutch locally grown vegetables only.** Seasonality means Dutch field/greenhouse harvest windows (velt.nu),
  not supermarket imports. No imported year-round produce (avocado, citrus, mango), no fruit, berries or nuts.
- Every user-visible string is Dutch. `nameEn` / `nutrientsEn` are never shown as they are.
- Every vegetable `color` must reach 4.5:1 with ink (`#1E1A14`) or white, because it is the background for the `VegToken` initials
  and the detail header (the validator checks this). `accent` is decorative only and has no contrast requirement (spec §5).
- `strings.xml` allows only `<i>` and `<b>` markup, for emphasis inside one string; never split a sentence across keys.
- `family` must be one of: Wortel, Blad, Brassica, Allium, Peulvrucht, Stengel, Pompoen, Knolgewas,
  Vruchtgroente, Paddenstoel, Kruid.

## Known content gaps (tracked in todo.md)

- `nutrientsEn` is English for 58 vegetables; 8 have none.
- Only 3 vegetables have `curatedRecipes`; 7 have no `sourceUrl`.
- 31 of 74 techniques have no `relatedVegIds`, and only 5 are about preparing vegetables.
- `backup.schema.json` (spec §13.1) is still to be created (todo P1).
- Every in-scope vegetable in `reference/velt-vegetables.json` is already covered: its "Kropsla" is our `butterhead-lettuce`
  (Botersla), and that entry could take `https://velt.nu/groente/kropsla` as its `sourceUrl`. The other 18 velt entries are fruit and nuts,
  which are out of scope.
