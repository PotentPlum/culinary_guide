# Mise — een kooksalmanak · Android app specification

Status: **draft v3** (2026-10-22). v2 applied the feature review (Appendix C); v3 applies the UI/UX review (Appendix D). Supersedes the Next.js/Supabase web app
(last web commit `cf3ec39`). Actionable breakdown: [`todo.md`](todo.md). Content & fixtures: [`data/README.md`](data/README.md).

---

## 0. Decisions and assumptions

Decided by the owner:

| # | Decision |
|---|---|
| D1 | Fully native Android app in **Kotlin + Jetpack Compose**. No web version, no iOS. |
| D2 | **Local-only data** on the phone. No backend, no accounts, no sync. |
| D3 | The **Claude API key** is entered in the app and stored encrypted with the Android Keystore (§9). |
| D4 | Start from a fresh project; carry over only the curated content and test fixtures in `data/`. |
| D5 | Apply the feature review (Appendix C), **except quest checkpoints**: quests do not get success criteria to tick off. Evaluation stays free scores + notes (§6.12). |
| D6 | Apply the UI/UX review (Appendix D) in full: one season and difficulty vocabulary, Boodschappen as a tab with quest history in Dagboek, "Vanavond" pinning, a 5-step score scale, cook-mode resume, and the contrast rules in §5. |

Assumptions; each one can be changed by editing this table:

| # | Assumption | Why |
|---|---|---|
| A1 | Distribution as a **signed APK via GitHub Releases** (sideload). The Play Store is optional, later (§17). | Single user; avoids Play listing and policy overhead. |
| A2 | Existing Supabase + browser data is converted once by a **script** into a normal Mise backup, which the app restores (§13.3). | No one-off migration code in the app. |
| A3 | Delivery is **phased**: P0 foundation → P1 core loop → P2 depth and polish → P3 later (see `todo.md`). | Get a usable app early. |
| A4 | `minSdk 30` (Android 11), `targetSdk 36`, `compileSdk 37` (the current AndroidX and OkHttp releases require it; P0 scaffold). | Simplifies the Keystore APIs; covers practically all current phones. |
| A5 | applicationId **`nl.potentplum.mise`**. Kotlin root package is the same. | Permanent once installed; change before the first release if desired. |
| A6 | One Gradle **`:app` module** with strict package boundaries (§4.2). | Least ceremony for a personal app. |
| A7 | Light theme in P1; dark theme in P2. | The brand palette is designed light-first. |
| A8 | One **Kwaliteit** setting for generation (quests, photo and page import): Snel = `claude-haiku-5-5`, **Gebalanceerd = `claude-sonnet-5-5` (default)**, Beste = `claude-opus-5-5`. Background enrichment always uses `claude-haiku-5-5` (§8.2). | One choice instead of one per feature. |

---

## 1. Product overview

Mise is a personal Dutch cooking almanac built around one daily question: **"Wat kook ik vandaag?"**
Four parts feed that question and each other:

1. **Seizoen**: which Dutch-grown vegetables are at their best right now (seasonal list, calendar, vegetable details).
2. **Recepten**: your recipe library, imported from websites, cookbook photos or typed in, with structured ingredients.
3. **Quests**: AI-generated cooking exercises, one tap for today's quest on Vandaag, shaped by the season and your journal.
4. **Dagboek**: what you cooked, how it went, your quests, and how seasonally and broadly you cook.

The loop: the season ranks recipes and quests (Vandaag) → you pick tonight's dish and shop for it (Boodschappen) → you cook
(cook mode) → you log it (Dagboek) → your journal shapes the next suggestions and quests. The **technique library** supports every step.

All UI text is **Dutch**. Code, identifiers, comments and filenames are English.

## 2. Goals and non-goals

**Goals**
- Opening the app answers "wat kook ik vandaag?" in one screen, offline, without Claude.
- Data entry is fast on a phone: share a link or photos into the app, use the camera, edit in place.
- Ingredients are structured (amount / unit / name), so scaling, shopping lists and seasonal matching are reliable.
- The phone is the only home of the data, so backup/restore must be robust (§13).
- The Claude API key cannot leak through backups, logs, screenshots or exports (§9).
- The editorial "Mise" look carries over (§5).

**Non-goals**
- Multi-user, sharing, cloud sync, accounts, login.
- Imported or year-round produce, fruit, berries, nuts (content scope, `data/README.md`).
- Nutrition tracking, meal planning, and showing a website's own ratings or metadata.
- Quest checkpoints or success criteria (D5).
- Custom in-app timers, farmer markets, foraging, biometric lock: deferred to P3 (§18).

## 3. Usage context

- One user, on one Android phone, often in the kitchen: wet hands, phone propped up, the screen must stay on.
- Typically 3–5 cooked meals logged per week, and a library growing to a few hundred recipes.
- Recipe sources: Dutch and English cooking websites (most have Schema.org JSON-LD), Instagram/Chrome share
  sheets, and printed cookbooks (photographed, e.g. the "Tobias" cookbook with sauce/marinade sub-recipes and wine pairings).

---

## 4. Platform and architecture

### 4.1 Tech stack

Pin versions in `gradle/libs.versions.toml`, using the latest stable release at scaffold time.

| Concern | Choice |
|---|---|
| Language / build | Kotlin 2.x (K2), Gradle Kotlin DSL, version catalog, KSP |
| UI | Jetpack Compose, Material 3 (custom theme; dynamic colour **off**) |
| Navigation | Navigation Compose with type-safe `@Serializable` routes; predictive back enabled |
| State | `ViewModel` + `StateFlow` (unidirectional data flow), `collectAsStateWithLifecycle` |
| DI | Hilt |
| Database | Room (KSP) with exported schemas and migration tests |
| Preferences | Jetpack DataStore (Preferences) |
| JSON | kotlinx.serialization |
| HTTP (recipe pages) | OkHttp |
| HTML parsing | Jsoup |
| Images | Coil 3 (disk cache); photo picker + `TakePicture` contract (no CAMERA permission) |
| Claude | Official Anthropic Java SDK `com.anthropic:anthropic-java` behind a `ClaudeClient` interface; raw OkHttp fallback if the SDK misbehaves on Android (§8.1) |
| Background | WorkManager (enrichment, backups, seasonal notification) |
| Security | Android Keystore (AES-256-GCM) |
| Logging | Timber (no-op tree in release) |
| Testing | JUnit + kotlinx-coroutines-test + Turbine; Robolectric where needed; Compose UI tests; Room `MigrationTestHelper` |
| Static analysis | Android Lint (`HardcodedText` = error), ktlint or detekt |

### 4.2 Architecture and packages

Layers: **ui** (Compose screens + ViewModels) → **domain** (pure Kotlin rules: seasonality, suggestions, scaling,
journal aggregation, badges, duration detection, shopping aggregation) → **data** (Room, DataStore, assets, network, Claude,
secrets). The domain layer has no Android imports and is unit-tested on the JVM.

```
nl.potentplum.mise
├─ MiseApp, MainActivity
├─ core/        designsystem (theme, typography, colour tokens, shared components), util (dates, Dutch number formatting)
├─ content/     asset models + ContentRepository (vegetables, techniques, facets, disciplines, archetypes, units)
├─ domain/      Seasonality, SeasonPresentation, TodaySuggester, QuestDefaults, IngredientParser (local), IngredientScaler,
│               ShoppingAggregator, DurationDetector, SeasonalMatcher, JournalAggregator, BadgeEvaluator
├─ data/        db/ (Room DB, entities, DAOs, migrations), repo/ (Recipe, Quest, Attempt, Shopping, Usage), settings/, backup/
├─ security/    SecretStore, KeystoreCipher
├─ claude/      ClaudeClient, request builders, output schemas, prompts, pricing, error mapping
├─ ingest/      RecipeFetcher, JsonLdRecipeParser (port), WprmExtractor, SharedContentParser, ImageProcessor
├─ feature/     today, season, vegetable, techniques, recipes, cook, quests, journal, shopping, settings, onboarding
└─ platform/    notification channels, workers, ClockTimerLauncher
```

Rules: features depend on `domain`/`data` interfaces, never on each other. Navigation between features goes only through routes.

### 4.3 Repository layout

```
/                       settings.gradle.kts, build.gradle.kts, gradle/, gradlew, spec.md, todo.md, README.md, CLAUDE.md
/app                    Android application module
/data                   curated content, fixtures, schemas (see data/README.md)
/tools/validate_data.py stdlib-only data validator (runs in CI and as a Claude Code hook)
/tools/legacy_to_backup.py  one-off converter for the old web-app exports (§13.3)
/lint-checks            custom Android Lint rules (ComposeHardcodedText, §15.4); build tooling, not shipped
/.github/workflows      CI + release
```

`app/build.gradle.kts` reads content straight from `/data`, so there is no copy step:

```kotlin
android.sourceSets {
    getByName("main") { assets.srcDir(rootProject.file("data/assets")); res.srcDir(rootProject.file("data/res")) }
    getByName("test") { resources.srcDir(rootProject.file("data/fixtures")) }
}
```

---

## 5. Design system

The "Mise — een kooksalmanak" look is editorial and cookbook-like: cream paper, ink text, a leaf-green primary, a terracotta accent, and
serif headlines with italic emphasis words.

**Colour tokens** (from the web CSS; contrast measured on cream `#F4EFE6`):

| Token | Value | M3 role | Contrast on cream |
|---|---|---|---|
| primary | `#3F5E2A` | `primary` | 6.4:1 |
| primary-soft | `#7A9C5C` | `primaryContainer` (with `onPrimaryContainer` = ink, 5.6:1) | 2.7:1: fills only, never alone |
| accent (terracotta) | `#B95128` | `tertiary` (`onTertiary` white, 4.9:1) | 4.3:1: large text and fills only |
| surface (cream) | `#F4EFE6` | `background`, `surface` | |
| surface-card | `#FBF8F1` | `surfaceContainerLow` (cards) | |
| surface-warm | `#EAE2D0` | `surfaceContainerHigh` | |
| ink | `#1E1A14` | `onBackground`, `onSurface` | |
| ink-soft | `#5A5347` | `onSurfaceVariant` | 6.6:1 |
| line | ink @ 10% | `outlineVariant`; ink @ 22% = `outline` | |
| status peak | `#3F5E2A` | custom `MiseColors.peak` | 6.4:1 |
| status shoulder | outline `#3F5E2A` on fill primary-soft | custom `MiseColors.shoulder` | (outline carries the contrast) |
| status soon | fill `#C98B2A` / fg `#8A5C12` | custom ("Komt eraan") | fill 2.5:1, fg 5.1:1 |
| status last chance | fill `#99514A` / fg `#6E3A35` | custom (badge "Laatste kans") | fill 5.1:1, fg 7.9:1 |

**Colour rules** (WCAG 2.1 AA):
- Text and small labels: ≥ 4.5:1. Status text, status dots and `SeasonBadge` text use the `*-fg` variants (or primary for peak).
- Graphical indicators that carry meaning (status dots, `PeakBar` and calendar cells, progress bars, focus rings): ≥ 3:1.
  The soft fills (`primary-soft`, the soon fill) never carry meaning on their own: a shoulder cell or dot is a primary outline
  with a primary-soft fill, so it stays readable as "half" without colour.
- Terracotta is for fills (buttons, the active filter pill) and text ≥ 18 sp; never for small text or inline links (use primary).
- **Vegetable colours:** each vegetable's `color` is the `VegToken` fill and the detail header background; its initials and header text use ink
  or white, whichever contrasts more, and that must reach 4.5:1 (`tools/validate_data.py` enforces it). The vegetable's `accent` is
  decorative only (header rule, card edge); many accents are below 3:1, so **`PeakBar` and the calendar always use the status colours**,
  never the vegetable's accent.
- Colour is never the only signal: every status has a label, every shoulder cell a shape.

**Typography** (bundled fonts, SIL OFL, licence files included in the app's licences screen):
- *Instrument Serif* (regular + italic) → display/headline only (≥ 22 sp). Emphasis words are set in italic, e.g. "Wat kook ik *vandaag*?".
  The emphasis is **inline markup in one string resource** (`<i>vandaag</i>`), rendered by a `styledStringResource()` helper that maps
  `StyleSpan(ITALIC)` to the serif italic. Never split a sentence across string keys: that breaks TalkBack phrasing and
  wrapping at large font scales.
- *Instrument Sans* (400/500/600) → title, body, label.
- *JetBrains Mono* (400/500) → meta labels only: eyebrows, month abbreviations, counts (≥ 11 sp). Uppercase with letter spacing.
  The uppercase is applied in the composable; the string resource stays in sentence case so TalkBack reads words, not letters.

**Shape:** radius 8 / 12 / 14 / 20 dp; pills fully rounded. Cards have a hairline outline (line colour) and minimal
elevation. Touch targets are at least 48 dp.

**Components:** `VegToken` (coloured circle with initials; always shown next to the vegetable's name, never as its only label),
`StatusDot`, `SeasonBadge` (Laatste kans / Kort seizoen), `PeakBar` (12 cells: peak filled, shoulder outlined + soft fill),
`FilterPills`, `FacetChips`, `RecipeCard`, `SuggestionCard` (recipe + reason line + total time + "Niet vandaag" / "Dit kook ik vanavond"),
`TonightCard`, `TechniqueCard`, `DifficultyBadge`, `Eyebrow` (mono label), `HeroHeader` (styled headline), `DisciplineBars`,
`SeasonTally` (count of vegetables cooked at peak + "nog te proberen" tokens), `ScoreScale` (5 labelled steps, §6.12),
`TimerChip` (idle / running with remaining time / done), `RecipeActionBar` (sticky bottom actions), `EmptyState`, `ErrorState`.

**Vocabulary** (one word per concept, everywhere in the UI, notifications and content):

| Concept | Label |
|---|---|
| Status PEAK / SHOULDER / SOON | **Op z'n best** / **Verkrijgbaar** / **Komt eraan** |
| Status ENDING and OFF | **Buiten seizoen** (ENDING is only visible as the grid shape in the calendar) |
| Badges | **Laatste kans**, **Kort seizoen** |
| Difficulty (quests, techniques, curated recipes) | **Basis** / **Gemiddeld** / **Gevorderd** (+ **Automatisch** in the quest form) |
| Score steps (§6.12) | **Mislukt** / **Matig** / **Oké** / **Goed** / **Top** |

Internal enums keep their names (`BEGINNER` / `INTERMEDIATE` / `ADVANCED`, `easy` / `medium` / `hard`); only the labels are unified.

---

## 6. Navigation and screens

### 6.1 App shell

- Single activity, edge-to-edge, Material 3 `NavigationBar` with 5 destinations:
  **Vandaag · Seizoen · Recepten · Boodschappen · Dagboek**. The Boodschappen icon shows a badge with the number of unticked items.
- Quests have no tab: they start from the **Quest van vandaag** card on Vandaag (and "Quest met …" / "Oefen in een quest" on vegetable
  and technique detail), and their history lives in **Dagboek › Quests** (§6.13). Completed quests are journal entries anyway.
- Each top app bar has an overflow menu (⋮) with **Technieken, Instellingen, Over Mise**.
  Techniques are mostly reached through links (§6.6).
- System back and predictive back work everywhere, and each tab keeps its own back stack.
- Share targets (§6.9) open the import flow on top of the current stack.
- The current month is always `LocalDate.now(ZoneId.of("Europe/Amsterdam")).monthValue`.

### 6.2 Vandaag (home)

Replaces the web app's Home. Its seasonal lists are folded into one shortlist, so Home no longer duplicates Seizoen.
Vandaag answers "wat kook ik vandaag?" and gets you from the answer to cooking. Top to bottom:

1. Hero: eyebrow "NEDERLAND · {Maand} {jaar}", title "Wat kook ik *vandaag*?" (`today_hero_title`).
2. **Vanavond** (only when a recipe is pinned for today): `TonightCard` with the recipe, total time and three actions:
   **Kookmodus**, **Op boodschappenlijst** (scaled, §6.14) and **Toch niet** (unpins). The pin expires at the end of the day and is
   cleared when an attempt is logged for that recipe. While a recipe is pinned, the suggestions below are collapsed under "Andere ideeën".
3. **Suggesties**: 3 recipes from the library, ranked by §11.5. Each `SuggestionCard` shows the total time and a reason line, e.g.
   "Pompoen en boerenkool zijn op hun best · 6 weken niet gekookt", plus two actions:
   - **Dit kook ik vanavond** pins it (item 2).
   - **Niet vandaag** removes it for the rest of the day; the next-ranked recipe slides into its slot.
   A single chip **≤ 30 min** above the cards restricts the suggestions to recipes with `totalMinutes ≤ 30` (recipes without a time are
   hidden while it's on). The chip state is not persisted.
   When the library has fewer than 3 eligible recipes, the remaining slots show curated links of peak vegetables (`curatedRecipes`).
   With an **empty library**, this section becomes an `EmptyState` that leads with import: "Begin met je eerste recept",
   with the buttons Link plakken · Foto's maken, and the tip to share a recipe from the browser; the curated links follow below it.
4. **Quest van vandaag**: one button that generates a quest with smart defaults (§11.6). If today's quest already exists,
   the card shows it instead (open or completed). "Verfijn" opens the full quest form (§6.11). "Alle quests" opens Dagboek › Quests.
   Without a Claude key, the card explains what is needed with **Sleutel toevoegen** (→ Instellingen) and **Verbergen**. Hidden, the
   card stays away until a key is added or it is turned back on in Instellingen (`showQuestCard`).
5. **Nu op z'n best**: a horizontal shortlist of at most 8 peak vegetables, ordered by §11.1, with `SeasonBadge`s.
   "Alle groenten →" goes to Seizoen.

The seasonal tally lives on Dagboek only (§6.13); Vandaag stays about today. Works offline.

### 6.3 Seizoen

- **Nu** view (default): filter pills with counts: **Verkrijgbaar** (PEAK + SHOULDER, default) / **Op z'n best** (PEAK) /
  **Komt eraan** (SOON). ENDING vegetables are not available, so they don't get a filter.
  The calendar still shows when the peak just ended.
- Rows: `VegToken` next to `nl`, family, status dot + label (§5 vocabulary), `SeasonBadge`s, mini `PeakBar`. Order per §11.1.
- Search matches `nl`, `nameEn` (a hidden alias) and `family`; case- and diacritic-insensitive.
- A segmented toggle switches between **Nu** and **Kalender** (§6.4).

### 6.4 Kalender

- A year grid: each row is a vegetable, with 12 month columns (peak filled, shoulder outlined + soft fill, §5). The current month column is highlighted.
- **The 12 months always fit the screen width**, so the year reads at a glance without horizontal scrolling: month headers are
  single letters (`months_initial`: J F M A M J J A S O N D), the name column takes about a third of the width and ellipsizes long
  names (the full name is in the row's TalkBack description and on the detail screen). Rows are at least 48 dp high.
- The grid can be grouped by family (default, with sticky family headers) or listed A–Z.
- Tapping a row opens the vegetable detail. TalkBack reads each row as "{nl}: op z'n best {maanden}; verkrijgbaar {maanden}", with
  month names in full.

### 6.5 Groente-detail

- Header on the vegetable's `color` (text in ink or white, §5): `nl`, family, current status, `SeasonBadge`s, and a 12-month
  `PeakBar` in the status colours with a legend (Op z'n best / Verkrijgbaar).
- Sections: **Bewaren** (`storage`), **Bereiden** (`prep`), **Bereidingstip** (`technique`),
  **Combineert goed met** (`pairings` chips), **Als je het niet hebt** (`substitutions` chips; a chip whose text
  matches another vegetable's `nl` opens that vegetable).
- **In jouw recepten**: library recipes that use this vegetable (§11.4), with cooked counts.
- **Technieken**: techniques whose `relatedVegIds` contain this vegetable.
- **Ideeën van het web**: `curatedRecipes` (open in the browser, or import).
- **Jouw dagboek**: "Laatst gekookt: {datum}", or "Nog niet gekookt dit seizoen".
- Action: **"Quest met {nl}"**, which opens the quest form with this vegetable pre-selected.
- `sourceUrl` is shown as "Bron: velt.nu" and opens in the browser.

### 6.6 Technieken

- Reached from the overflow menu and, mainly, through links: the recipe detail and cook mode ("Technieken in dit recept"),
  quest results, vegetable detail, and the Dagboek.
- Categories, in this order, labelled from `techniques_category_*`: Snijdtechnieken, Garingsmethoden, Smaakgebouw,
  Eiwitten, Sauzen & Fonds, Groentenvoorbereiding, Gereedschap, Conservering. Within a category, techniques are grouped by `subcategory`.
- Filter by level (Basis / Gemiddeld / Gevorderd); search on `nl` + `subtitle`.
- Detail: subtitle, `body` paragraphs, **Tips**, **Benodigde kookgerei**, **Aanbevolen groenten** (chips → vegetable detail),
  **Verwante technieken**, "Bouwt voort op" (`parentId`), "Vervolgtechnieken" (children), and "Zo vaak geoefend: {n}×"
  from the journal. Action: **"Oefen in een quest"** pre-fills the quest technique.
- Cookware guides (`discipline = null`) are read-only reference pages. They are never tagged on recipes and never sent to Claude as techniques.

### 6.7 Recepten — bibliotheek

- Shows recipes with `inLibrary = true`. Sort: **Laatst toegevoegd** (default) / **A–Z** / **Laatst gekookt**.
- Search across title, ingredient names and source.
- Quick filter chips: **Seizoen nu** (§11.4) · **Favorieten** · **Nooit gekookt** · dish type. **Meer filters** opens a
  sheet with cuisine, main ingredient, technique and time (prep time derived from `totalMinutes`, §8.4).
  The empty state with filters active offers "Wis zoekopdracht en filters".
- Card: image (or a cuisine-coloured placeholder), title, source, total time, cooked count ("3× gekookt"),
  in-season vegetable chips.
- FAB **"Recept toevoegen"** opens a sheet: Link plakken · Foto's maken · Foto's kiezen · Zelf invoeren.
- Empty library: "Nog geen recepten" plus a CTA.

### 6.8 Recept — detail and in-place editing

**Detail view**
- Image, title, source (tap → original URL), author (when known), description, times (voorbereiding / bereiden / totaal),
  and a servings stepper (1–99) that scales structured ingredients (§11.2).
- **Ingredient groups** with headers ("Voor de saus", "Voor de marinade"). Items are rendered from structured fields
  ("150 g boter, in blokjes"); unparsed items show their text. Items can be ticked off; the ticks are the same state as in
  cook mode (the cook session, §6.10).
  "Op boodschappenlijst" adds all ingredients or a selection, scaled.
- **Technieken in dit recept**: chips for the recipe's technique tags (→ technique detail).
- **Step groups** with headers. Steps show timer chips (§11.3), and technique names in the text are tappable (alias match, §6.6).
- **Extra velden**: free label/value pairs, for example "Wijnpairing: Chablis", "Bron pagina: 112", "Opmerking".
- Tags (cuisine, dish type, main ingredient) and personal notes, the **cooked count and last cooked date**,
  and the attempts for this recipe with photos.
- Imported website metadata (rating, date published, keywords, diet labels, nutrition, video) is stored in `sourceMeta`
  but **not displayed**.
- **Layout of actions:** a sticky `RecipeActionBar` at the bottom with **Kookmodus** (primary; "Verder bij stap {n}" when a cook
  session for this recipe is open, §6.10) and **Gekookt** (→ evaluation §6.12). The top app bar holds the favourite toggle and ⋮ with
  **Dit kook ik vanavond**, Bewerken, Delen (as text) and Verwijderen (with undo). "Op boodschappenlijst" sits with the ingredients.
- **Order of sections** (cook-first): image and title, times and servings, ingredients, steps, Technieken in dit recept, then the
  reference material (extra fields, tags, notes, cooked count, attempts).

**Edit mode (in place)**
- "Bewerken" switches the same screen to editable fields: title, description, servings, times, source label/URL, groups (add,
  rename, delete, reorder by drag), ingredient lines, steps, extra fields, tags, notes, image (replace or remove).
- Ingredient lines are edited as text. On save, changed lines are re-parsed: locally at once, then by Claude enrichment in the
  background when a key is set (§8.4). An ingredient whose structured fields were corrected by hand keeps them (`parseSource = MANUAL`).
- Save / Annuleren. Leaving with unsaved changes asks for confirmation. `updatedAt` is set on save.

### 6.9 Recept importeren

| Entry | Flow |
|---|---|
| Paste a link | URL field (prefilled from the clipboard when it holds a URL) → fetch + parse (§10) → preview → Bewaren |
| Share a link | `ACTION_SEND text/plain`: take the first http(s) URL from the shared text → same flow |
| Camera / gallery | 1–5 photos (photo picker, `TakePicture`, `ACTION_SEND(_MULTIPLE) image/*`) → reorder or remove → "Lees recept" → Claude vision (§8.3) → preview → Bewaren |
| No JSON-LD found | Offer "**Lees de pagina met Claude**" (§8.5) or "Zelf invoeren" |
| Zelf invoeren | An empty recipe in edit mode |

- **Preview:** the detail screen in **read view** with a bottom bar **Bewaren** (primary) and **Aanpassen** (switches to edit mode).
  A clean import is one tap to save. Lines the local parser could not structure are marked subtly ("Niet herkend; wordt aangevuld als
  er een sleutel is"). The preview opens **directly in edit mode** only for "Zelf invoeren" and when the import has no title, no
  ingredients or no steps; the missing parts are highlighted. A source label is suggested (domain, or a cookbook title detected by Claude).
- **Sharing is the main way in** (§3), so the `ACTION_SEND` share targets for links and images ship in P1, not later.
- **Enrichment:** after a URL import or a manual save, the local ingredient parser runs immediately. When a key is set,
  a background job then calls Claude once per recipe for structured ingredients **and** tags (§8.4). The recipe shows
  "wordt verrijkt…" and a failure never blocks the save. Photo and page imports already return structured
  ingredients and tags, so they skip this step.
- Duplicate detection: the same canonical URL already in the library → "Dit recept heb je al", with the option to open it or import a copy.
- Images are downloaded and stored locally (§7.4), so the library works offline.

### 6.10 Kookmodus

- A full-screen, step-by-step pager: one step per page, large type (scales with the system font), group name as an eyebrow,
  progress "Stap 3 van 9" with a thin progress bar. Swipe or tap the large previous/next buttons.
- **Landscape** (phone propped up sideways): two panes, the step on the left and the ingredients list on the right. In portrait the
  ingredients panel is one tap away (bottom sheet). Both show the scaled amounts.
- The screen stays on (`FLAG_KEEP_SCREEN_ON`) while cook mode is visible.
- **Cook session:** one open session at a time (§7.3): recipe, current step, servings, ingredient ticks and started timers. It is
  saved on every change, so leaving and coming back (or process death) resumes at the same step: the recipe's Kookmodus button reads
  "Verder bij stap {n}". It is cleared when an attempt is logged for the recipe, or after 24 hours.
- **Timers use the phone's Clock app:** tapping a timer chip (§11.3) fires `AlarmClock.ACTION_SET_TIMER` with
  `EXTRA_LENGTH`, `EXTRA_MESSAGE` = "Mise · Stap 4" and `EXTRA_SKIP_UI = true`, so the timer starts without leaving cook mode.
  A snackbar confirms "Timer van 12 min gestart". If no app handles the intent, chips are hidden and a one-time hint explains why.
  In-app timers are P3 (§18).
- **Running timers stay visible:** Mise records each start time in the cook session and shows a `TimerChip` strip at the top of cook
  mode ("Stap 4 · nog ~9 min", then "Stap 4 · klaar"), computed locally; it is an estimate, the Clock app rings. Tapping a running
  chip opens the Clock app's timer list (`AlarmClock.ACTION_SHOW_TIMERS`). No extra permissions.
- **Technique help:** a step whose text names a technique shows an info button that opens that technique as a bottom sheet.
- **Finishing:** the last page has a large **Klaar met koken** button → evaluation (§6.12). Leaving cook mode **after reaching the last
  step** asks "Heb je dit gekookt?" (Vastleggen / Nu niet). Leaving earlier never asks: the session is kept for resuming.

### 6.11 Quests

A quest is a focused cooking exercise generated by Claude (§8.3). There are **no checkpoints** (D5). Quests start from the
Quest van vandaag card on Vandaag; their history is **Dagboek › Quests** (§6.13).

**Quest van vandaag (one tap)**
- From the Vandaag card (or Dagboek › Quests). Uses the defaults from §11.6 (in-season vegetable, neglected discipline, adapted
  difficulty, no repeats) and generates right away.
- **While generating** (this can take tens of seconds), the card already shows what was chosen ("Seizoensgroente: pastinaak ·
  Focus: snijtechnieken · Gemiddeld"), a progress indicator and **Annuleren**; cancelling returns the card to its idle state
  and records no quest. A "Verfijn" link is always there.
- At most one "quest van vandaag" per day. Regenerating replaces it (the old one stays in history).

**Verfijn (full form, collapsed by default)**
- **Ingredient**, one of three modes:
  - *Zelf kiezen*: free text (max 200 characters), with an optional quantity and unit (for using up leftovers).
  - *Seizoensgroente*: a dropdown of in-season vegetables (§11.1 order).
  - *Verras me* (default): Claude chooses from the in-season list and returns its id.
- **Soort quest** (chips): Maaltijd · Lunch · Bijgerecht · Saus · Bouillon / fond · Component · Laat Claude kiezen.
  "Component" means a building block such as a pickle, crumble, flavoured oil or dressing.
- **Moeilijkheid**: Automatisch (§11.6) · Basis · Gemiddeld · Gevorderd.
- Optional: **Techniek** (searchable, skills only), **Keuken** (facet values), **Beschikbare tijd** (<15 / <30 / <60 / >60),
  **Klein & eenvoudig** (pantry ingredients only).
- "Geef quest". The button is disabled with an explanation when offline or when no key is set.

**Result**
- Quest text, skill, difficulty badge, time estimate, "Waarom deze quest?", result type (dish type), the chosen vegetable (if any),
  technique chips (→ technique detail) and a full recipe (ingredient groups, steps, tips).
- Actions: **Start koken** (cook mode on the quest's recipe; primary), **Quest voltooid** (→ evaluation), **Andere quest**
  (same parameters), and in ⋮ **Bewaar recept in bibliotheek** (sets `inLibrary = true`).
- **One way to complete:** any attempt logged for the quest's recipe, whether from Quest voltooid, "Klaar met koken" in cook mode, or
  the cook-mode exit prompt, links the attempt to the quest (`questId`) and sets the quest to COMPLETED.

### 6.12 Evaluatie ("Gekookt")

- A bottom sheet, reached from every place that ends a cook: **Gekookt** on recipe detail, **Klaar met koken** and the exit prompt in
  cook mode, and **Quest voltooid**.
- **Uitvoering** and **Smaak** (both optional) are each a `ScoreScale`: one row of 5 labelled steps, one tap each, at least 48 dp:
  **Mislukt · Matig · Oké · Goed · Top**, stored as **2 · 4 · 6 · 8 · 10** in the 1–10 fields (§7.2). Tapping the selected step clears it.
  The thresholds in §11.6 (≥ 8 up, ≤ 5 down) and the badges' `minAvgScore` keep working on the stored values. Older attempts with
  other values (restored web data) show the nearest step label (1–2 Mislukt, 3–4 Matig, 5–6 Oké, 7–8 Goed, 9–10 Top) and keep
  their stored value until a step is tapped.
- **Notities**, **Foto** (optional; camera or gallery, stored locally), and the date (default today; editable).
- **Vastleggen** logs an attempt (§7.2) with snapshots of techniques, cuisines and the vegetables used (§11.4).
  **Zonder cijfers vastleggen** logs it without scores. Dismissing the sheet logs nothing.
- The recipe's cooked count and last cooked date update right away; the cook session and a Vanavond pin for the recipe are cleared.

### 6.13 Dagboek (replaces "Profiel")

A cooking journal first, statistics second. Three segments at the top: **Tijdlijn** (default) · **Quests** · **Inzichten**.

**Tijdlijn**
- Attempts grouped by month (newest first), each with photo, title, scores (step labels), notes, and recipe or quest.
  Filter: alles / recepten / quests. Tapping an entry edits it (scores, notes, photo, date) or opens the recipe.

**Quests** (the quest history)
- All quests, newest first: filter on Open / Voltooid, plus type and difficulty. Each item can be reopened, retried with the same
  parameters, or deleted. A **Nieuwe quest** button opens the Verfijn form (§6.11).

**Inzichten**
- Header stats: attempts this month, total attempts, quests completed.
- **Dit seizoen**: a `SeasonTally`: "{x} groenten op hun best gekookt dit seizoen" (no denominator, §12), the cooked vegetables as
  tokens, and **Nog te proberen**: the first 3 vegetables that are PEAK this month and not yet cooked this season, in §11.1 order
  (so Laatste kans comes first), each linking to vegetable detail, plus "Alle →".
- **Technieken**: `DisciplineBars` for the 3 disciplines (count + average execution score), meest geoefende technieken
  (top 10), and **verwaarloosd** (disciplines not practised in the last 30 days). Quest van vandaag uses this list.
- **Keukens**: meest gekookte keukens (top 5).
- **Insignes**: badges from `archetypes.json`, earned or locked, with progress text ("Nog 3 groenten op hun best").
- Dropped from the web profile: radar chart, ingredient cloud, "hoogst gewaardeerde keukens", "hoogst beoordeelde technieken".

### 6.14 Boodschappen (tab)

- A bottom-bar tab (§6.1), because it is used in the shop with one hand. Items arrive from recipe detail ("Op boodschappenlijst")
  and the Vanavond card.
- Items from several recipes are **combined** by ingredient name and compatible unit (§11.7): "3 uien" from two recipes
  is one line, with the source recipes as small chips. Unparsed lines stay separate.
- Sections: combined items (A–Z) and "Los" (manual items). Tick off, swipe to delete, add manual items.
  Ticked items move to the bottom of their section.
- Clear ticked items, or clear everything (with undo). "Delen" shares the list as plain text.
- Empty state: "Je lijst is leeg" with a hint to add ingredients from a recipe.

### 6.15 Instellingen

- **Claude**: key status (none / valid / invalid), add/replace/delete key (§9), "Sleutel testen", **Kwaliteit**
  (Snel / Gebalanceerd / Beste, A8), usage this month (tokens + estimated €), a tip to set a monthly spend limit
  in the Anthropic Console, and **Quest-kaart tonen op Vandaag** (`showQuestCard`, §6.2).
- **Gegevens**: export a backup, restore a backup (this also restores converted web-app data, §13.3), automatic
  backup (folder + frequency, last run), "Ingrediënten verrijken" for all unparsed recipes (shows an estimated cost first),
  delete all data (typed confirmation "VERWIJDER").
- **Meldingen**: seasonal notification on/off.
- **Over**: version, data sources (velt.nu), open-source licences (including the fonts), privacy statement (§15.1).

### 6.16 First run

1. Welcome (brand + one sentence).
2. Optional: "Claude-sleutel toevoegen" (explains which features need it, what it costs, how it is stored) or "Later".
3. Optional: "Back-up terugzetten", for an earlier backup or the converted web-app data (§13.3). Skipped automatically after a restore.
4. **Je eerste recept** (skipped when a restore brought recipes): Link plakken · Foto's maken · Later, with the tip that sharing a recipe
   from Chrome or Instagram to Mise imports it directly. Choosing an option ends onboarding in that import flow; "Later" lands on
   Vandaag, whose empty-library state repeats the same choice (§6.2).
5. Notification permission is asked **only** when the seasonal notification is turned on.

---

## 7. Data

### 7.1 Static content (APK assets)

`ContentRepository` loads `vegetables.json`, `techniques.json`, `facets.json`, `disciplines.json`, `archetypes.json`
(and `units.json`, §11.2) once, on a background dispatcher, with kotlinx.serialization (`ignoreUnknownKeys = false` in tests
and `true` in release). Files whose `schemaVersion` is unsupported fail loudly in debug builds. Models mirror
`data/schemas/*.schema.json` exactly. Content is read-only at runtime. Lookups by id are O(1) maps.

### 7.2 Local database (Room)

IDs are UUID strings; timestamps are epoch millis (UTC). Lists that are only ever read whole are stored as JSON text through TypeConverters.

| Entity | Fields |
|---|---|
| `recipe` | `id`, `title`, `description`, `sourceType` (URL / PHOTO / PAGE / MANUAL / QUEST / LEGACY), `sourceUrl?`, `sourceLabel`, `author?`, `imagePath?`, `imageUrl?`, `servings?`, `prepMinutes?`, `cookMinutes?`, `totalMinutes?`, `notes?`, `isFavorite`, `inLibrary`, `enrichState` (NONE / PENDING / DONE / FAILED), `sourceMeta` (json: rating, datePublished, keywords, diet, nutrition, video; stored, not shown), `createdAt`, `updatedAt` |
| `ingredient_group` | `id`, `recipeId` (FK, cascade), `position`, `name?` (null = untitled default group) |
| `ingredient` | `id`, `groupId` (FK, cascade), `position`, `text` (as written), `quantity?`, `quantityMax?` (ranges), `unit?` (id from `units.json`), `name?` (Dutch, lowercase, singular), `note?` ("in blokjes"), `parseSource` (NONE / LOCAL / CLAUDE / MANUAL) |
| `step_group` | `id`, `recipeId`, `position`, `name?` |
| `step` | `id`, `groupId`, `position`, `text` |
| `recipe_extra_field` | `id`, `recipeId`, `position`, `label`, `value` |
| `recipe_tag` | `recipeId`, `key` (cuisine / dishType / mainIngredient / technique), `value` (PK all three) |
| `quest` | `id`, `createdAt`, `isDaily`, `status` (OPEN / COMPLETED), `completedAt?`, `ingredientMode` (CUSTOM / SEASONAL / SURPRISE), `ingredient`, `vegId?`, `quantity?`, `unit?`, `questType`, `requestedDifficulty?`, `focusDiscipline?`, `requestedTechniqueId?`, `requestedCuisine?`, `timeLimit?`, `simpleAndSmall`, `text`, `skill`, `difficulty`, `timeEstimateMinutes`, `why`, `dishType`, `tips` (json), `recipeId` (FK → `recipe` with `sourceType = QUEST`) |
| `attempt` | `id`, `sourceType` (RECIPE / QUEST), `recipeId?`, `questId?`, `cookedAt`, `score?` (1–10; the UI writes 2/4/6/8/10, §6.12), `tasteScore?` (1–10, same), `notes?`, `photoPath?`, `techniques` (json snapshot), `cuisines` (json snapshot), `vegIds` (json snapshot, §11.4), `peakVegIds` (json: subset at PEAK on `cookedAt`) |
| `shopping_item` | `id`, `text`, `quantity?`, `unit?`, `name?`, `recipeId?`, `checked`, `position`, `createdAt` |
| `claude_usage` | `id`, `at`, `feature` (QUEST / PHOTO_IMPORT / PAGE_IMPORT / ENRICH / KEY_CHECK), `model`, `inputTokens`, `outputTokens`, `cacheReadTokens`, `costMicroUsd` |

- Indexes on all FKs, plus `recipe(inLibrary, updatedAt)`, `attempt(cookedAt)`, `attempt(recipeId)`, `quest(status, createdAt)`,
  `ingredient(name)`.
- Derived values (never stored): cooked count, last cooked date, prep-time bucket (§8.4).
- Attempts keep **snapshots**, so the journal doesn't change when a recipe is edited later.
- Deleting a recipe keeps its attempts (`recipeId` set to null) so journal history survives. Deleting a quest deletes its hidden recipe.
- Schema changes go through numbered Room migrations with a `MigrationTestHelper` test. **Never** use destructive migration.

### 7.3 Settings (DataStore `settings.preferences_pb`)

`quality` (FAST / BALANCED / BEST), `seasonalNotificationsEnabled`, `autoBackupTreeUri?`, `autoBackupFrequency`
(OFF / DAILY / WEEKLY), `lastBackupAt?`, `lastSeasonalNotifiedMonth?`, `onboardingDone`, `librarySort`,
`clockTimerHintShown`, `showQuestCard` (default true). The API key is **not** stored here (§9).

Day and session state (JSON strings in the same file; never backed up, §13.1):
- `tonight?`: `{ date, recipeId }`, the Vanavond pin (§6.2). Ignored when `date` is not today.
- `todayDismissed?`: `{ date, recipeIds[] }`, suggestions dismissed with "Niet vandaag". Ignored when `date` is not today.
- `cookSession?`: `{ recipeId, stepIndex, servings, checkedIngredientIds[], timers[{ stepIndex, lengthSeconds, startedAt }], updatedAt }`
  (§6.10). Ignored and cleared when `updatedAt` is more than 24 hours old or the recipe no longer exists.

### 7.4 Files

- Recipe images: `filesDir/images/{recipeId}.jpg`; attempt photos: `filesDir/journal/{attemptId}.jpg`. Both are downscaled to at most
  1600 px on the long edge, JPEG quality 85, EXIF stripped.
- Import photos sent to Claude are temporary (`cacheDir/import/`) and deleted after a successful import. The user can keep
  the first one as the recipe image.

---

## 8. Claude integration

### 8.1 Client

- The `ClaudeClient` interface (suspend functions) has two implementations: `AnthropicSdkClaudeClient` (the official Java SDK, preferred)
  and `OkHttpClaudeClient` (raw `POST https://api.anthropic.com/v1/messages`, `anthropic-version: 2026-10-21`).
  A P0 spike decides which one ships. The SDK must work on API 30+ with R8 enabled.
  **Status (P1):** the spike needs a real device and has not run, so P1 ships `OkHttpClaudeClient`: it reuses the app's
  OkHttp client, adds no dependency or R8 rules, and sends exactly the body this section describes (the SDK's builder for
  `fallbacks: "default"` is not documented). Swapping in the SDK later only touches the Hilt binding in `ClaudeModule`.
- The key is read from `SecretStore` per call (§9). It never appears in logs, crash reports or exceptions shown to the user.
- **Structured outputs** for every generation call: `output_config.format` = JSON Schema (§8.6). No regex extraction of JSON.
- **Do not send** `temperature`, `top_p` or `top_k`: since 2026-10-21 the API rejects them with a 400. The legacy quest prompt
  used `temperature: 0.7`; drop it.
- Thinking: omit the parameter (adaptive by default on current models). `output_config.effort` is set per feature (§8.2).
- `max_tokens`: 16000 (non-streaming). Read timeout: 180 s. A cancellable progress UI is shown while waiting.
- **Response checks, in order:** `stop_reason == "refusal"` → Dutch message "Claude wilde dit verzoek niet uitvoeren"
  (show `stop_details.explanation` when present); `max_tokens` → "Antwoord was te lang en is afgebroken"; otherwise parse
  the first `text` block as JSON and validate it (§8.6).
- **Refusal fallback:** on Sonnet 5.5 / Opus 5.5 requests to the Claude API, send `fallbacks: "default"` with beta header
  `server-side-fallback-2026-07-01`. Haiku 5.5 has no server-side fallback.
- **Retries:** network errors, 408, 429 (honour `retry-after`), 500/502/503/529 → at most 2 retries with exponential
  backoff and jitter. **Never retry** 400 / 401 / 403 / 404 / 413. A 401 marks the key invalid and links to Instellingen.
- **Usage tracking:** after each call, store a `claude_usage` row from `usage` × the price table (`claude/Pricing.kt`, with a
  "prijzen per" date; per MTok input/output: Haiku 5.5 $0.10/$0.50, Sonnet 5.5 $2/$10, Opus 5.5 $4/$20).

### 8.2 Features, models and effort

| Feature | Model | Effort | Input |
|---|---|---|---|
| Quest generation | Kwaliteit setting (default Sonnet 5.5) | medium | parameters or §11.6 defaults, in-season list, recent quest history, vocab |
| Recipe from photos | Kwaliteit setting | medium | 1–5 JPEGs (≤ 1568 px long edge, ≤ 5 MB each) + instructions |
| Page text fallback | Kwaliteit setting | medium | visible page text (≤ 60k chars) |
| Enrichment (ingredients + tags) | always `claude-haiku-5-5` | low | title, ingredient lines, steps |
| Key check | — | — | `GET /v1/models` (no tokens used) |

### 8.3 Prompts

Prompts are Kotlin string templates in `claude/prompts/`. They are written in Dutch, built from `data/reference/prompts/` but
**without** JSON-format instructions, because the schema enforces the format. They keep the old rules:
- **Quest:** coaching tone; a concrete, finished dish; respect every given parameter strictly; a usable recipe with no vague
  steps; "Klein & eenvoudig" means few, pantry ingredients; *Verras me* picks from the provided in-season list and
  returns its id; the soort quest decides the output (a Saus quest yields a sauce, not a meal). New: a **focus discipline**
  ("oefen vooral snijtechnieken") and a **"niet herhalen" list** of the last 10 quest skills + titles.
  No success criteria or checkpoints (D5).
- **Photos:** combine multiple photos into one recipe; **convert all measures to metric** (oz→g, cups→ml, °F→°C);
  keep printed section headers as group names ("Voor de saus"); capture extra printed fields (wine pairing, page number,
  notes) as `extraFields`; return the cookbook title if visible.
- **Enrichment:** split each ingredient line into quantity / range / unit / name / note using the unit list; convert
  non-metric units to metric; Dutch singular lowercase names; only tag what the recipe clearly supports.

The system prompt and the vocabulary block come first and stay byte-stable, so prompt caching applies when the prefix is
long enough; per-request content comes last.

### 8.4 One tag system

The web app had two overlapping systems: facet "tags" and a "taxonomy" that repeated cuisine. They are merged into one
`recipe_tag` table with four keys:

| Key | Values | Source |
|---|---|---|
| `cuisine` | `facets.json` cuisine options | Claude |
| `dishType` | `facets.json` dish types (exactly 1) | Claude |
| `mainIngredient` | `facets.json` main ingredients | Claude |
| `technique` | technique ids with a non-null discipline | Claude |

The prep-time facet is **derived** from `totalMinutes` (<15 / <30 / <60 / >60) and never asked from Claude. The ingredient list
for journal and matching comes from structured ingredient names, not from a separate taxonomy.

### 8.5 Page text fallback

When a page has no JSON-LD recipe, the app extracts the readable text with Jsoup (drops script/style/nav/footer and trims to
60k characters), shows "Dit kost ongeveer € X" (estimated from the character count), and on confirmation sends it with the photo-import
schema. The URL is kept as `sourceUrl`.

### 8.6 Output schemas and validation

- Schemas are built **at runtime** from the content assets, so enums always match: technique ids (skills only), facet values,
  unit ids, and vegetable ids from the in-season list.
- Keep schemas within the structured-output subset: every object has `additionalProperties: false` and all properties
  `required`; optional values are `["type","null"]`; use enums; no numeric bounds. Bounds such as 1–5 photos, servings 1–99 and
  quantities > 0 are checked client-side.
- **Structured ingredient item** (shared): `{ text, quantity|null, quantityMax|null, unit|null (unit enum), name|null, note|null }`.
- **Quest:** `questText`, `skill`, `difficulty` (BEGINNER / INTERMEDIATE / ADVANCED), `timeEstimateMinutes`, `why`,
  `dishType`, `chosenIngredient`, `chosenVegId|null`, `tags { cuisine[], mainIngredient[], techniques[] }`,
  `recipe { title, servings, ingredientGroups[{ name|null, items[ingredient item] }], steps[{ text }], tips[] }`.
- **Photo / page recipe:** `title`, `description`, `sourceLabel|null`, `servings|null`, `prepMinutes|null`,
  `cookMinutes|null`, `totalMinutes|null`, `ingredientGroups[{ name|null, items[ingredient item] }]`,
  `stepGroups[{ name|null, steps[string] }]`, `extraFields[{ label, value }]`, `tags { cuisine[], dishType, mainIngredient[], techniques[] }`.
- **Enrichment:** `ingredients[{ index, ...ingredient item without text }]` (index = position in the request) +
  `tags { cuisine[], dishType, mainIngredient[], techniques[] }`.
- After parsing: unknown enum values are dropped (not fatal), strings are trimmed, empty groups removed, a missing or invalid
  ingredient parse falls back to the local parse, and `MANUAL` parses are never overwritten.

---

## 9. API key storage and security

**Storage**
- On first save, a Keystore key is generated (alias `mise_claude_key_wrap`): AES-256, GCM, NoPadding,
  `setRandomizedEncryptionRequired(true)`, `setUnlockedDeviceRequired(true)`. The app tries `setIsStrongBoxBacked(true)` first
  and falls back to TEE-backed when StrongBox is unavailable.
- The API key is encrypted with this key; `IV + ciphertext` (Base64) is stored in a **separate** DataStore file
  `secrets.preferences_pb`. The plaintext key never touches disk.
- **Backup exclusion:** `data_extraction_rules.xml` (cloud-backup **and** device-transfer) and `full_backup_content.xml`
  (API 30) exclude `datastore/secrets.preferences_pb`. Mise backups (§13) **never** contain the key. After a restore
  onto a new phone, the app asks for the key again.
- `KeyPermanentlyInvalidatedException`, `AEADBadTagException`, or a missing alias → wipe the stored ciphertext, set the
  status to "Sleutel opnieuw invoeren", and never crash.
- A biometric lock on Claude use is deferred to P3 (§18). The design above can add it later by re-wrapping with an auth-bound key.

**Entry UX**
- A password-type field with no autocorrect, suggestions or personalised IME learning, and a show/hide toggle.
- Soft format check (`sk-ant-` prefix). Then a **live check** with `GET /v1/models`: 200 → save; 401 → "Ongeldige sleutel";
  network error → offer "Toch opslaan".
- After a successful paste and save, offer "Klembord wissen" (`ClipboardManager.clearPrimaryClip()`).
- The key screen sets `FLAG_SECURE` (no screenshots or recents thumbnail). The key is displayed only as `sk-ant-…` + last 4 characters.
- "Sleutel verwijderen" wipes the ciphertext and the Keystore alias.

**Runtime**
- The key is decrypted just before a request and passed to the client; it is not kept in a long-lived field or ViewModel.
  A JVM `String` can't be zeroed; this limitation is accepted and documented.
- Release builds have **no** HTTP logging. Debug logging redacts `x-api-key`. Crash/exception messages are scrubbed of
  anything matching `sk-ant-[A-Za-z0-9_-]+`.
- TLS uses system trust only (no user CAs, cleartext traffic disabled). No certificate pinning, because it's brittle.
- **Threat model note (shown in Over):** this protects against backups, other apps, casual device access and logs. It does not
  protect against a rooted or compromised phone. Mitigation: a dedicated key with a monthly spend limit.

---

## 10. Recipe ingestion (URL)

**Fetch** (`RecipeFetcher`, OkHttp)
- Only http/https. `http://` is first upgraded to `https://`; cleartext is never sent. Up to 5 redirects.
- Browser-like headers (desktop Chrome UA, `Accept-Language: nl-NL,nl;q=0.9,en;q=0.8`). Connect/read timeout 20 s.
- Body capped at **5 MB** while streaming. Charset comes from the headers or `<meta charset>`.
- Errors map to the Dutch strings (`recipes_error_*`): unreachable, HTTP {code}, too large, no recipe found.

**Parse** (`JsonLdRecipeParser`): a faithful port of `web/lib/recipeIngestion.ts@cf3ec39`.
- Collect every `<script type="application/ld+json">` (single or double quotes); skip malformed blocks silently.
- Find the Recipe node by deep traversal: the root object, arrays, `@graph`, `mainEntity`, `mainEntityOfPage`, and `@type` as a string or array.
- Ingredients: split into sections at "header" lines (ending in `:`, or ALL-CAPS without digits, length > 1).
  The parser's default section names are `"Ingredients"` / `"Instructions"` (WPRM: `"Ingrediënten"`). Keep them
  byte-identical so the goldens pass; the mapping step turns default names into untitled groups.
- Instructions: plain strings, `HowToStep`, `HowToSection` (one section each), and a mix (loose steps join the
  preceding section; steps before the first section go to the default section). Steps are numbered from 1 per section.
- Text cleaning: strip HTML (block closers and `<br>` → space), decode entities, normalise curly quotes, ring-above → °,
  collapse whitespace.
- Durations: ISO 8601 (`P1DT30M`, `PT1H15M`); seconds round up to the next minute; zero or invalid → null.
- Nutrition, image, author, rating, video, keywords, diet, cuisine/category (string or array) are parsed for `sourceMeta`.
- **WPRM override:** when a `wprm_recipes` script exists, take the ingredients from it (US text + `textMetric` when different, notes included).
- **Acceptance:** all 104 cases in `data/fixtures/recipe-ingestion/golden.json` pass. Each case is compared by JSON
  equality of the result (or the expected `RecipeIngestionError` code).
- **Mapping to Room:** sections → groups. A section named `Ingredients`, `Instructions`, `Ingrediënten` or `Bereiding`
  becomes an untitled group (`name = null`); other names are kept as headers ("Voor de saus"). Ingredient `text` =
  `textMetric` when present (metric wins; there is no unit toggle), else `text`. Then the local parser runs (§11.2).

---

## 11. Domain rules

### 11.1 Seasonality and how it is presented

```
statusFor(veg, month):
  if month in peak            → PEAK
  if month in shoulder        → SHOULDER
  if any p in peak with (p - month) mod 12 in {1, 2} → SOON
  if previousMonth(month) in peak                    → ENDING
  else                        → OFF
```

Acceptance: matches `data/fixtures/seasonality/status.json` for all 66 × 12 cells. The rule is unchanged from the web app;
only the presentation changes:

- Labels follow the §5 vocabulary: PEAK **Op z'n best**, SHOULDER **Verkrijgbaar**, SOON **Komt eraan**, ENDING and OFF
  **Buiten seizoen**. The default filter **Verkrijgbaar** = PEAK or SHOULDER. ENDING is visible only as the grid shape in the calendar.
- **Laatste kans** badge: PEAK now and neither PEAK nor SHOULDER next month.
- **Kort seizoen** badge: `|peak ∪ shoulder| ≤ 3` months (e.g. white asparagus).
- **Order** (Nu op z'n best shortlist, Seizoen lists, dropdowns, Nog te proberen): status (PEAK, SHOULDER, SOON) → Laatste kans first →
  Kort seizoen first → fewer available months first → `nl` (Dutch collation). This keeps the list meaningful in
  September, when 43 vegetables are at peak at once.

### 11.2 Structured ingredients, units and scaling

- **Units** come from a content asset `data/assets/units.json` (todo P1): id, Dutch singular/plural labels, aliases for the local
  parser (gram, gr, g; eetlepel, el, eetl.; …), a dimension (MASS / VOLUME / COUNT / OTHER), and a factor to the base unit
  (g, ml, stuks). `el` = 15 ml and `tl` = 5 ml are VOLUME. `snuf`, `teen`, `bos`, `blik`, `takje`, `handvol` and `naar smaak` are OTHER (no conversion).
- **Local parser** (`IngredientParser`, offline, deterministic): leading quantity (integer, decimal with `,` or `.`, unicode
  fraction, `1/2`, range `2-3` / `2 à 3`), optional unit alias, then name, with the `note` after the first comma. Unparseable
  lines are kept as text (`parseSource = NONE`). Claude enrichment replaces LOCAL and NONE parses, never MANUAL ones.
- **Scaling:** `ratio = targetServings / baseServings` (stepper hidden when the base is unknown). Parsed items: quantity (and
  quantityMax) × ratio, then sensible rounding: COUNT → nearest ½ (minimum ½); g/ml < 50 → whole numbers, ≥ 50 → nearest 5;
  ≥ 1000 g/ml switches to kg/l with 1 decimal; el/tl → nearest ½. Unparsed items fall back to the web app's text scaling
  (multiply every number). A pack size like "à 400 g" is part of the `note`, so it is never scaled.
- **Formatting:** Dutch decimal comma (`1,5 el`), unit label singular or plural by quantity, `quantity–quantityMax` for ranges.

### 11.3 Duration detection (timer chips)

Dutch and English patterns in step text: `(\d+)(?:\s*[-–]\s*(\d+))?\s*(seconden|sec|minuten|minuut|min|uur|u|hours?|minutes?)`.
For a range, use the upper bound. Ignore matches that look like temperatures or quantities (followed by `°`, `g`, `ml`).
Chips launch the Clock app timer (§6.10).

### 11.4 Seasonal matching

A recipe (or attempt) "uses" vegetable V when any name in `V.nl.split(" / ")` (lowercased, diacritics folded) matches a structured
ingredient `name` (exact or with a Dutch plural `-s` / `-en`), or, for unparsed items, appears as a whole word in the ingredient text.
"Seizoen nu" = uses at least one vegetable whose status is PEAK or SHOULDER. At evaluation time, the matched ids are
snapshotted into `attempt.vegIds`, and those at PEAK on `cookedAt` into `peakVegIds`.

### 11.5 "Wat kook ik vandaag?" ranking

For every library recipe:

```
season   = Σ over used vegetables: PEAK 1.0 (+0.5 if Laatste kans or Kort seizoen), SHOULDER 0.5
recency  = cooked ≤ 7 days ago → excluded; ≤ 30 days → × 0.5; never cooked → + 0.3; else days since last cooked
           capped at 90, scaled to + 0…0.6
score    = season + recency + (favourite ? 0.4 : 0)
```

- Exclude recipes with `inLibrary = false`. Ties are broken by fewest times cooked, then title.
- The reason line names the top contributors: up to 2 peak vegetables, "{n} weken niet gekookt", "Nog nooit gekookt", "Favoriet".
- The ranking is deterministic for a given day (stable across app restarts). Vandaag shows the first 3 recipes that are not in
  `todayDismissed` and not the Vanavond pin; with the **≤ 30 min** chip on, only recipes with `totalMinutes ≤ 30` qualify.
  Dismissing a card takes the next qualifying recipe. When none are left, the empty slots fall back to curated links (§6.2).
- The `SuggestionCard` shows `totalMinutes` (or nothing when unknown) next to the reason line.
- Pure function, unit-tested with fixed dates.

### 11.6 Quest van vandaag defaults

- **Ingredient**: *Verras me* restricted to PEAK vegetables not cooked (`attempt.vegIds`) in the last 60 days. Use all PEAK if
  that leaves fewer than 3, and all in-season vegetables if PEAK is empty.
- **Focus discipline**: the most neglected discipline (§12). If none is neglected, the one with the fewest attempts in 30 days.
- **Difficulty**: from the last 5 completed quests with an execution score. Average ≥ 8 → one level up from the most recent
  difficulty; ≤ 5 → one level down; otherwise the same. No history → Basis (`BEGINNER`).
- **Soort quest**: Laat Claude kiezen. Other parameters are empty.
- **History**: the last 10 quests (skill + title) are sent as the "niet herhalen" list.

### 11.7 Shopping list aggregation

- Items with a `name` are grouped by `name`. Within a group, quantities in the same dimension are summed after converting to
  the base unit (g, ml, stuks) and shown in the best display unit (§11.2). Items in different dimensions or OTHER units stay
  as separate lines under the same name ("2 teen knoflook" + "1 tl knoflookpoeder" do not merge).
- Items without a `name` are never merged.
- Scaling is applied before items are added to the list.

---

## 12. Journal aggregation and badges

Input: all attempts. "Matches discipline D" means at least one technique id in the attempt's snapshot has `discipline == D`
(from `techniques.json`; cookware guides have no discipline and never match).

- **Disciplines** (order from `disciplines.json`: mechanical, thermal, flavor): `count` = matching attempts;
  `avgScore` = mean of their non-null execution scores, else none.
- **Top techniques:** count per technique id, desc, top 10.
- **Neglected disciplines:** disciplines with no matching attempt, or whose most recent matching attempt is more than 30 days ago.
- **Top cuisines:** count per cuisine (attempt snapshot), desc, top 5.
- **Dit seizoen:** seasons are lente (Mar–May), zomer (Jun–Aug), herfst (Sep–Nov) and winter (Dec–Feb). `x` = distinct vegetables
  in `peakVegIds` of attempts cooked in the current season. It is shown **without a denominator**: the number of vegetables that peak
  in a season (21 in winter up to 53 in autumn) made "2 van 53" read as failure. **Nog te proberen** = vegetables that are PEAK in the
  current month and not in `x`, in §11.1 order.
- **Badges** (`archetypes.json`, typed rules):
  - `techniques`: matching attempts (by `discipline` or `techniqueIds`) ≥ `minAttempts`, and, if `minAvgScore` is set, the mean of their
    non-null execution scores ≥ `minAvgScore`. No scores means not earned.
  - `distinctPeakVegetables`: distinct ids across all `peakVegIds` ≥ `min`.
  - `peakMonths`: distinct calendar months (1–12) of attempts with non-empty `peakVegIds` ≥ `min`.
  - Progress text shows what's missing ("Nog 4 groenten op hun best").
- Acceptance: port the applicable cases from `web/lib/profileAggregation.test.ts@cf3ec39` (adapted to 3 disciplines), plus tests
  for the seasonal metric (season boundaries, December/January wrap) and every badge rule at and around its threshold.

---

## 13. Backup, restore and legacy data

### 13.1 Export

- `Mise-backup-YYYYMMDD-HHmm.zip` via the system file picker (SAF `CreateDocument`) or the share sheet.
- Contents: `backup.json` + `images/{recipeId}.jpg` + `journal/{attemptId}.jpg`. `backup.json` holds
  `{ format: "mise-backup", schemaVersion: 1, exportedAt, appVersion, recipes[] (the full graph with groups, items, extra fields, tags),
  quests[], attempts[], shoppingItems[], settings (non-secret subset, without the day and session state of §7.3) }`.
- **Never** includes the API key or Keystore material. `claude_usage` is left out.
- The format is documented in `data/schemas/backup.schema.json` (todo P1), so the legacy converter and the app share one contract.

### 13.2 Restore and automatic backup

- Restore: pick a file → validate format and version → show a summary ("42 recepten, 17 quests, 88 pogingen") → choose
  **Samenvoegen** (by id; newer `updatedAt` wins) or **Vervangen** (wipe first, with typed confirmation) → run inside one DB transaction.
- After a restore, recipes with unparsed ingredients are offered "Ingrediënten verrijken" (§6.15).
- Automatic backup: the user picks a folder (`OpenDocumentTree`, persisted permission). WorkManager writes daily or weekly
  (only when charging or with battery not low) and keeps the newest 5 files. A failure raises a low-priority notification.
- Android Auto Backup may include the DB and images. The secrets file is excluded (§9).

### 13.3 Legacy data (old web app)

There is no legacy code in the app. A one-off script converts the old exports into a normal backup:

1. Export **before** shutting down Supabase/Vercel:
   - **Supabase** (SQL editor, runs as owner so RLS doesn't apply):
     ```sql
     select json_build_object(
       'format', 'mise-legacy-supabase',
       'recipes',       (select coalesce(json_agg(r), '[]') from recipes r),
       'challenges',    (select coalesce(json_agg(c), '[]') from challenges c),
       'user_attempts', (select coalesce(json_agg(a), '[]') from user_attempts a)
     );
     ```
   - **Browser localStorage** (devtools console on the deployed site; this catches recipes whose Supabase save failed):
     ```js
     copy(JSON.stringify({ format: "mise-legacy-local", localRecipes: JSON.parse(localStorage.getItem("mise-saved-recipes") ?? "[]") }))
     ```
2. Convert: `python3 tools/legacy_to_backup.py mise-legacy-supabase.json [mise-legacy-local.json] -o Mise-backup-legacy.zip`
   (stdlib only, mapping in Appendix A, output validated against `backup.schema.json`, prints a report of anything skipped).
3. Restore the zip in the app (§13.2), then run "Ingrediënten verrijken".

---

## 14. Notifications, background work and permissions

| Channel | Importance | Use |
|---|---|---|
| `season` | DEFAULT | Monthly "Nu op z'n best: asperges, rabarber, …" (vegetables that became PEAK this month) |
| `background` | LOW | Automatic backup or enrichment failed |

- Seasonal notification: a daily WorkManager job posts once per month (on the first run on or after the 1st, ~09:00), only when enabled.
- Enrichment runs as expedited/unique WorkManager work per recipe and needs a network connection.
- Permissions: `INTERNET`, `POST_NOTIFICATIONS` (requested in context), `com.android.alarm.permission.SET_ALARM` (normal
  permission, for the Clock timer intent). The manifest needs a `<queries>` entry for `android.intent.action.SET_TIMER`.
  **No** camera, storage, location, exact-alarm or boot permissions.

---

## 15. Non-functional requirements

### 15.1 Privacy
- No analytics, no crash reporting service, no ads. Network traffic goes only to: recipe URLs the user enters or shares,
  `api.anthropic.com`, and image URLs of imported recipes.
- What goes to Claude: the selected photos, the recipe or page text being imported or enriched, quest parameters, the in-season
  vegetable list, and the titles and skills of the last 10 quests. Nothing else. The app states this in Over and on first key entry.

### 15.2 Performance
- Cold start to Vandaag in under 1.5 s on a mid-range phone, including the suggestion ranking for 1,000 recipes (computed off the
  main thread, cached per day). Asset parsing stays off the main thread; lists are lazy; library search responds in under 100 ms
  (Room FTS4 on title and ingredient names if needed).

### 15.3 Accessibility
- TalkBack labels in Dutch for every icon button, chart and status (month names in full, not abbreviated).
- Supports font scale up to 200% without clipping; 48 dp targets; colour is never the only status signal (dot + label,
  outlined shoulder cells).
- Contrast follows the colour rules in §5: text ≥ 4.5:1 (status text uses the `*-fg` variants), meaningful graphics ≥ 3:1,
  vegetable fills checked by the data validator.
- Headlines with italic emphasis are single string resources (§5), so TalkBack reads them as one sentence.
- Every way to finish an action is a visible button; swipes (cook-mode pager, swipe to delete) always have a button or undo next to them.

### 15.4 Language
- Every user-visible string lives in `res/values/strings.xml` (Dutch is the default and only locale). Lint `HardcodedText`
  and a unit test fail the build on hardcoded UI text. Plurals use `<plurals>`. Number and date formatting uses the `nl-NL` locale.
- Content prose comes from the assets (already Dutch). `nameEn` and `nutrientsEn` are never displayed.

### 15.5 Robustness
- Every Claude or network operation is cancellable, shows progress, and returns to a stable state on failure with a retry button.
- The database is never left half-written: multi-table writes run in one transaction.
- Process death restores in-progress forms (SavedStateHandle).

---

## 16. Testing and quality

| Level | What |
|---|---|
| Data | `python3 tools/validate_data.py` (schemas, cross-references, strings.xml incl. markup and plurals, vegetable colour contrast, seasonality drift) in CI and as a Claude Code hook |
| Unit (JVM) | Asset deserialisation of every file (`ignoreUnknownKeys = false`); seasonality goldens + presentation order and badges; parser goldens; local ingredient parser (table-driven: fractions, ranges, comma decimals, aliases, notes, pack sizes); scaling + rounding + Dutch formatting; shopping aggregation; duration detection; seasonal matcher; suggestion ranking (fixed dates); quest defaults; journal aggregation; badges; Claude schema builders + response mapping (recorded JSON responses, no live calls); backup round-trip |
| Tools | `tools/legacy_to_backup.py` tested with synthetic legacy fixtures covering every branch of Appendix A; its output validates against `backup.schema.json` and restores in an instrumented test |
| Integration | Room DAOs + `MigrationTestHelper`; `SecretStore` on Robolectric/instrumented (encrypt → decrypt, invalidation path) |
| UI (Compose) | Vandaag with seeded data (Niet vandaag refill, ≤ 30 min chip, Vanavond pin), import from URL (fake fetcher; clean import saves in one tap), edit-in-place save/cancel, quest van vandaag (fake client, cancel while generating), key entry states, cook mode (timer chip intent, resume at step after leaving, exit prompt only after the last step), evaluation score steps |
| Manual checklist | Share from Chrome/Instagram, camera import, cook mode in landscape, TalkBack and 200% font scale on Vandaag, Kalender and cook mode, Clock timer from cook mode on 2 phones, restore on a fresh install, legacy restore with real data |

CI must stay green on every push: validate data → `./gradlew lint ktlintCheck testDebugUnitTest :lint-checks:test assembleDebug`.

## 17. Build, CI and release

- Build types: `debug` (applicationIdSuffix `.debug`, so both can be installed side by side) and `release` (R8 + resource shrinking,
  with keep rules for kotlinx.serialization, Room and the Anthropic SDK).
- `versionCode` / `versionName` come from the git tag (`v1.2.3`).
- Signing: a release keystore kept **outside** the repo and backed up offline (losing it means installed apps can't be updated).
  CI signs from GitHub Actions secrets.
- Workflows: `ci.yml` (PRs and pushes: validate + lint + tests + debug APK artifact) and `release.yml` (tag `v*`: signed APK +
  checksum attached to the GitHub Release).
- Play Store (optional, later): AAB, a privacy policy page, a data-safety form (no data collected; the key is user-provided).

## 18. Out of scope and later (P3)

- **In-app kitchen timers** (several named countdowns, notifications, exact alarms): only if the Clock-app handoff (§6.10) proves annoying.
- **Biometric lock** on Claude use (§9).
- **Markten in de buurt / boerenmarkten**: needs a data source for Dutch farmer markets with schedules. Research first.
- **Wildplukken / plukkaart**: a separate foraging dataset with strict safety rules (look-alikes, protected areas).
- **Vervangers & combinaties explorer** (web "binnenkort").
- Home-screen widget ("Wat kook ik vandaag?"), Wear OS.

---

## Appendix A — legacy data formats and mapping (for `tools/legacy_to_backup.py`)

**Supabase `recipes` row** → backup recipe (`sourceType` = URL, or PHOTO when `source_url` starts with `mise-photo:`; LEGACY when unknown)
- `instructions` holds a JSON `RecipeOutput` (below). When it parses, it is the primary source. Otherwise build the recipe from the
  columns: `name`, `ingredients[]` (one untitled group), `description`, `prep_time`, `cook_time`, `total_time`, `servings`,
  `cuisine[]`, `category[]`, `keywords[]`, `image_url`, `author`, `suitable_for_diet[]`, `date_published`,
  `aggregate_rating{ratingValue,ratingCount}`, `video_url`, `nutrition{calories,protein,carbs,fat}`.
- Column values override the JSON when they are non-null (the same precedence as the web app's `fetchRemoteSaved`).
- `source` → `sourceLabel`; `tags` (`{cuisine?,dishType?,mainIngredient?,prepTime?: string[]}`) → `recipe_tag` (cuisine,
  dishType, mainIngredient; prepTime is dropped because it is derived); `created_at` → `createdAt`.
- Ingredients are exported as text only (`parseSource = NONE`); the app enriches them after restore.
  `textMetric` wins over `text` when present. Rating, dates, keywords, diet, nutrition and video go into `sourceMeta`.

**`RecipeOutput`** (web type): `title`, `description`, `prepTimeMinutes`, `cookTimeMinutes`, `totalTimeMinutes`,
`servings?`, `cuisine` (string or string[]), `category` (string or string[]), `keywords[]`, `image`, `author`,
`suitableForDiet[]`, `datePublished`, `aggregateRating`, `video`, `nutrition`,
`ingredientSections[{ sectionName, ingredients[{ text, textMetric? }] }]`,
`instructionSections[{ sectionName, steps[{ stepNumber, text }] }]`, `unitSystems?{ hasMetric }`.

**localStorage `mise-saved-recipes`** entries: `{ url, source, recipe: RecipeOutput, savedAt (ms), remoteId?, tags?, taxonomy?{techniques,cuisines,ingredients} }`.
Entries with a `remoteId` that matches a Supabase row are merged into that row; the rest become new recipes. `taxonomy.techniques`
→ `technique` tags (only ids that exist with a non-null discipline); `taxonomy.cuisines` → `cuisine` tags when missing;
`taxonomy.ingredients` is dropped.

**Supabase `challenges` row** → backup quest + hidden recipe (`sourceType = QUEST`)
- `ingredient`, `challenge_text` → `text`, `skill`, `difficulty` (Beginner / Intermediate / Advanced, or legacy easy / medium / hard →
  BEGINNER / INTERMEDIATE / ADVANCED), `time_estimate`, `why`, `output_type` → `dishType`, `params`
  (`{technique?, cuisine?, timeLimit?, simpleAndSmall?, quantity?, unit?}`), `created_at`; `isDaily = false`.
- `recipe` JSON `{ title, servings, ingredients[{amount,unit,name}], steps[{step,instruction}], tips[], tags? }` →
  one untitled ingredient group (text = "amount unit name", trimmed; `name` = name lowercased; `parseSource = NONE`), steps, tips.
  `techniques` → `technique` tags; `cuisines` → `cuisine` tags.
- `ingredientMode = CUSTOM`, `questType` = derived from `output_type` (Saus/Basis → SAUS, else MAALTIJD).
- `status` = COMPLETED if any `user_attempts.source_id` references it, else OPEN.

**Supabase `user_attempts` row** → backup attempt: `source_type` (challenge → QUEST, recipe → RECIPE), `source_id` → `questId`/`recipeId`
(dropped if unresolved), `attempted_at` → `cookedAt`, `score`, `taste_score`, `notes`, `techniques[]`, `cuisines[]`.
`vegIds`/`peakVegIds` are computed by the script with the §11.4 name match against `ingredients[]` and §11.1 for the month of `cookedAt`.

## Appendix B — traceability of the old `ToDo` file

| Old ToDo item (Dutch) | Where |
|---|---|
| Markten in de buurt / FarmerMarkets toevoegen | §18 (P3) |
| Wildplukken | §18 (P3) |
| Recept-structuur: "Voor saus" / "Voor marinade", flexibele velden (Wijn pairing; Tobias-kookboek) | §6.8, §7.2 (`ingredient_group`, `step_group`, `recipe_extra_field`), §8.3 photo prompt |
| Aantal keren gekookt | §6.7, §6.8, §7.2 (derived from `attempt`) |
| Edit mode in recept zelf | §6.8 |
| Overzicht oude challenges | §6.11 History |
| Moeilijkheidsgraad als keuze | §6.11 Verfijn (plus automatic, §11.6) |
| Challenge als "Saus maken" / "Bouillon maken" / "Component maken" | §6.11 Soort quest |
| Tickbox seizoensingrediënt (leeg = Claude kiest, of dropdown) | §6.11 ingredient modes, §11.6 |
| Soort eten: bijgerecht, saus, bouillon, component, maaltijd, lunch | §6.11 Soort quest |
| Challenges hernoemen naar "Quests" | §6.11, todo.md (strings) |

## Appendix C — feature review outcome (v2)

| Review finding | Change | Where |
|---|---|---|
| No daily reason to open the app | "Wat kook ik vandaag?" ranking on a new Vandaag tab | §6.2, §11.5 |
| Home, Seizoen and Kalender repeat one dataset | Home's seasonal sections folded into one ranked shortlist; Profiel tab replaced by Dagboek | §6.1–6.3 |
| "Op hoogtepunt" meaningless in autumn; "Aflopend" nearly empty; "Alles in seizoen" counted unavailable vegetables | Nu verkrijgbaar / Komt eraan, Laatste kans and Kort seizoen badges, ranked order | §6.3, §11.1 |
| Free-text ingredients break scaling, shopping lists and matching | Structured ingredients (local parser + Haiku enrichment), unit table, smart rounding, aggregation | §7.2, §8.4, §11.2, §11.7 |
| Two overlapping tag systems | One `recipe_tag` table; prep time derived | §8.4 |
| Schema.org metadata clutter and a US/metric toggle | Stored in `sourceMeta`, not shown; metric always wins | §6.8, §10 |
| Quest form has too many inputs; quests ignore the journal and history | One-tap Quest van vandaag with season, focus discipline, adaptive difficulty and a no-repeat list; full form under "Verfijn" | §6.11, §11.6 |
| Quest checkpoints | **Rejected by the owner** (D5); evaluation stays scores + notes, now with an optional photo | §0, §6.12 |
| Profile built on noisy data; "material" axis always empty; arbitrary badges | Dagboek (timeline + photos, seasonal metric, 3 discipline bars); cookware has no discipline; badges fixed (fermenteur no longer counts marinating), thresholds lowered, 2 seasonal badges added | §6.13, §12, `data/assets/*` |
| Techniques are rarely browsed | Linked from recipes, steps, cook mode, quests and the journal; moved to the overflow menu | §6.6, §6.8, §6.10 |
| Too few vegetable techniques | Content task | todo.md |
| Custom timers are complex | Clock app handoff; custom timers P3 | §6.10, §18 |
| Legacy import code lives forever in the app | One-off converter script → normal restore | §13.3, Appendix A |
| Too much configuration | One Kwaliteit setting; biometric lock P3; 3 sort orders | §0 A8, §6.7, §6.15 |

## Appendix D — UI/UX review outcome (v3)

The spec was reviewed for usability before any UI was built (2026-10-22). The owner accepted all findings (D6).

| Finding | Change | Where |
|---|---|---|
| Vandaag ranks recipes but nothing connects choosing to cooking; no cooking time on the cards | Total time on `SuggestionCard`, a ≤ 30 min chip, "Dit kook ik vanavond" → Vanavond card with Kookmodus and Op boodschappenlijst | §6.2, §11.5 |
| "Andere suggesties" replaces all three cards at once | Per-card "Niet vandaag"; the next-ranked recipe slides in | §6.2, §11.5 |
| Day one is empty: onboarding never leads to a first recipe; share import was P2 although sharing is the main way in | First-run step "Je eerste recept", an import-led empty state on Vandaag, share targets moved to P1 | §6.2, §6.9, §6.16 |
| "{x} van {y} piekgroenten" reads as failure (y = 53 in autumn) and is shown twice | `SeasonTally` without a denominator plus 3 "nog te proberen" tokens, on Dagboek only | §6.13, §12 |
| Eight overlapping season words (Op hoogtepunt, Beschikbaar, Nu verkrijgbaar, Binnenkort, Aflopend, …) | Three statuses (Op z'n best / Verkrijgbaar / Komt eraan) + two badges; ENDING shows as Buiten seizoen | §5, §6.3, §11.1, `strings.xml` |
| Three difficulty vocabularies (Beginner/Expert, Makkelijk/Moeilijk, Basis/Gevorderd) | One scale: Basis / Gemiddeld / Gevorderd (+ Automatisch) | §5, §6.11, `strings.xml` |
| "Overslaan" in the evaluation sheet still saves an attempt | Renamed "Zonder cijfers vastleggen"; dismissing logs nothing | §6.12 |
| Soft fills and 14 vegetable accents below 3:1 used for meaning; 3 vegetable fills below 4.5:1 for initials; terracotta 4.3:1 | Colour rules: status colours for `PeakBar`/calendar, outlined shoulder cells, terracotta only large; fills fixed and checked by the validator | §5, §15.3, `vegetables.json`, `tools/validate_data.py` |
| Headlines split across string keys for the italic word | One string with `<i>` markup and a `styledStringResource()` helper; the validator allows only `<i>`/`<b>` | §5, `strings.xml` |
| Initials-only `VegToken`s collide among 66 mostly green vegetables | The token is always paired with the name | §5, §6.3 |
| Year grid scrolls horizontally on a phone, losing the "year at a glance" | 12 columns always fit; single-letter months; ellipsized names | §6.4 |
| Cook mode asks "Heb je dit gekookt?" on every exit and forgets where you were | Prompt only after the last step; "Klaar met koken" button; persisted cook session with "Verder bij stap n" | §6.10, §7.3 |
| Clock-app timers run out of sight | Local `TimerChip` strip with remaining time; tap opens the Clock app's timers | §6.10 |
| Phone propped up sideways | Two-pane landscape cook mode; ingredient ticks shared with recipe detail | §6.8, §6.10 |
| Import preview starts in edit mode even for a clean import | Read-view preview with Bewaren; edit mode only for manual entry or missing parts | §6.9 |
| Recipe detail: long scroll, five equal actions | Sticky Kookmodus / Gekookt bar, favourite in the top bar, the rest in ⋮, cook-first section order | §6.8 |
| Two ways to complete a quest | Any attempt on the quest's recipe completes it | §6.11 |
| Quest generation waits silently; a dead quest card every day without a key | Chosen defaults + cancel while generating; "Verbergen" for the no-key card | §6.2, §6.11, §6.15 |
| Two 1–10 sliders are fiddly with wet hands | 5 labelled steps stored as 2/4/6/8/10, so thresholds and badges keep working | §6.12, §7.2 |
| Dagboek mixes the journal with statistics in one scroll; the Quests tab duplicates Vandaag while the shopping list is two taps deep | Dagboek segments Tijdlijn · Quests · Inzichten; Boodschappen replaces Quests in the bottom bar | §6.1, §6.13, §6.14 |
