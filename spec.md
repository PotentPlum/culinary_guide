# Mise — een kooksalmanak · Android app specification

Status: **draft v1** (2026-10-22). Supersedes the Next.js/Supabase web app (last web commit `cf3ec39`).
Actionable breakdown: [`todo.md`](todo.md). Content & fixtures: [`data/README.md`](data/README.md).

---

## 0. Decisions and assumptions

Decided by the owner:

| # | Decision |
|---|---|
| D1 | Fully native Android app in **Kotlin + Jetpack Compose**. No web version, no iOS. |
| D2 | **Local-only data** on the phone. No backend, no accounts, no sync. |
| D3 | The **Claude API key** is entered in the app and stored encrypted with the Android Keystore (§9). |
| D4 | Start from a fresh project; carry over only the curated content and test fixtures in `data/`. |

Assumptions; each one can be changed by editing this table:

| # | Assumption | Why |
|---|---|---|
| A1 | Distribution as a **signed APK via GitHub Releases** (sideload). The Play Store is optional, later (§17). | Single user; avoids Play listing and policy overhead. |
| A2 | Existing Supabase + browser data is migrated once through a **legacy import** (§13.3). | Keeps the recipes and history already built up. |
| A3 | Delivery is **phased**: P0 foundation → P1 parity with the web app → P2 new features → P3 later (see `todo.md`). | Get a usable app early. |
| A4 | `minSdk 30` (Android 11), `targetSdk`/`compileSdk 36`. | Simplifies the Keystore and auth-bound key APIs; covers practically all current phones. |
| A5 | applicationId **`nl.potentplum.mise`**. Kotlin root package is the same. | Permanent once installed; change before the first release if desired. |
| A6 | One Gradle **`:app` module** with strict package boundaries (§4.2). Split into modules only if build times hurt. | Least ceremony for a personal app. |
| A7 | Light theme in P1; dark theme in P2. | The brand palette is designed light-first. |
| A8 | Claude model defaults: quests + photo import → `claude-sonnet-5-5`; tagging → `claude-haiku-5-5`; `claude-opus-5-5` selectable (§8.2). | Same tiers the web app used, on the current model generation. |

---

## 1. Product overview

Mise is a personal Dutch cooking almanac. It answers four questions:

1. **What's in season right now in the Netherlands?** (seasonal vegetables, calendar, vegetable details)
2. **How do I cook it well?** (technique library)
3. **What do I cook?** (my recipe library, imported from websites, cookbook photos or typed in)
4. **How do I get better?** (AI "Quests": focused cooking exercises, plus a profile that tracks progress)

All UI text is **Dutch**. Code, identifiers, comments and filenames are English.

## 2. Goals and non-goals

**Goals**
- Works fully offline, except for features that need the internet: Claude calls and fetching recipe URLs.
- Data entry is fast on a phone: share a link or photos into the app, use the camera, edit in place.
- The phone is the only home of the data, so backup/restore must be robust (§13).
- The Claude API key cannot leak through backups, logs, screenshots or exports (§9).
- The editorial "Mise" look carries over (§5).

**Non-goals**
- Multi-user, sharing, cloud sync, accounts, login.
- Imported or year-round produce, fruit, berries, nuts (content scope, `data/README.md`).
- Nutrition tracking or meal planning (beyond the shopping list).
- Farmer markets and foraging. Deferred to P3 (§18).

## 3. Usage context

- One user, on one Android phone, often in the kitchen: wet hands, phone propped up, the screen must stay on.
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
| Background | WorkManager (backups, seasonal notification); AlarmManager (kitchen timers) |
| Security | Android Keystore (AES-256-GCM), BiometricPrompt |
| Logging | Timber (no-op tree in release) |
| Testing | JUnit 5 (or 4) + kotlinx-coroutines-test + Turbine; Robolectric where needed; Compose UI tests; Room `MigrationTestHelper` |
| Static analysis | Android Lint (`HardcodedText` = error), ktlint or detekt |

### 4.2 Architecture and packages

Layers: **ui** (Compose screens + ViewModels) → **domain** (pure Kotlin rules: seasonality, scaling, profile
aggregation, archetypes, duration detection) → **data** (Room, DataStore, assets, network, Claude, secrets).
The domain layer has no Android imports and is unit-tested on the JVM.

```
nl.potentplum.mise
├─ MiseApp, MainActivity
├─ core/        designsystem (theme, typography, colour tokens, shared components), util (dates, Dutch number formatting)
├─ content/     asset models + ContentRepository (vegetables, techniques, facets, disciplines, archetypes)
├─ domain/      Seasonality, IngredientScaler, DurationDetector, ProfileAggregator, ArchetypeEvaluator, SeasonalMatcher
├─ data/        db/ (Room DB, entities, DAOs, migrations), repo/ (Recipe, Quest, Attempt, Shopping, Usage), settings/, backup/, legacy/
├─ security/    SecretStore, KeystoreCipher, BiometricGate
├─ claude/      ClaudeClient, request builders, output schemas, prompts, pricing, error mapping
├─ ingest/      RecipeFetcher, JsonLdRecipeParser (port), WprmExtractor, SharedContentParser, ImageProcessor
├─ feature/     home, season, calendar, vegetable, techniques, recipes, cook, quests, profile, shopping, settings, more, onboarding
└─ platform/    notification channels, TimerAlarmReceiver, workers
```

Rules: features depend on `domain`/`data` interfaces, never on each other. Navigation between features goes only through routes.

### 4.3 Repository layout

```
/                       settings.gradle.kts, build.gradle.kts, gradle/, gradlew, spec.md, todo.md, README.md, CLAUDE.md
/app                    Android application module
/data                   curated content, fixtures, schemas (see data/README.md)
/tools/validate_data.py stdlib-only data validator (runs in CI and as a Claude Code hook)
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

**Colour tokens** (from the web CSS):

| Token | Value | M3 role |
|---|---|---|
| primary | `#3F5E2A` | `primary` |
| primary-soft | `#7A9C5C` | `primaryContainer` (with `onPrimaryContainer` = ink) |
| accent (terracotta) | `#B95128` | `tertiary` |
| surface (cream) | `#F4EFE6` | `background`, `surface` |
| surface-card | `#FBF8F1` | `surfaceContainerLow` (cards) |
| surface-warm | `#EAE2D0` | `surfaceContainerHigh` |
| ink | `#1E1A14` | `onBackground`, `onSurface` |
| ink-soft | `#5A5347` | `onSurfaceVariant` |
| line | ink @ 10% | `outlineVariant`; ink @ 22% = `outline` |
| status peak | `#3F5E2A` | custom `MiseColors.peak` |
| status soon | `#C98B2A` / fg `#8A5C12` | custom |
| status ending | `#99514A` / fg `#6E3A35` | custom |
| status shoulder | primary-soft | custom |

Vegetables bring their own `color` (fill) and `accent` (bars, borders). Cuisine tags use the `facets.json` colours.

**Typography** (bundled fonts, SIL OFL, licence files included in the app's licences screen):
- *Instrument Serif* (regular + italic) → display/headline. Emphasis words are set in italic, e.g. "Wat is er nu *in het seizoen*".
- *Instrument Sans* (400/500/600) → title, body, label.
- *JetBrains Mono* (400/500) → meta labels: eyebrows, month abbreviations, counts, timers. Uppercase with letter spacing.

**Shape:** radius 8 / 12 / 14 / 20 dp; pills fully rounded. Cards have a hairline outline (line colour) and minimal
elevation. Touch targets are at least 48 dp.

**Components:** `VegToken` (coloured circle with initials), `StatusDot`, `PeakBar` (12 cells: peak filled, shoulder
half), `FilterPills`, `FacetChips`, `RecipeCard`, `TechniqueCard`, `DifficultyBadge`, `Eyebrow` (mono label),
`HeroHeader` (eyebrow + serif title with italic span + subtitle), `RadarChart` (Canvas), `EmptyState`, `ErrorState`.

---

## 6. Navigation and screens

### 6.1 App shell

- Single activity, edge-to-edge, Material 3 `NavigationBar` with 5 destinations:
  **Home · Seizoen · Recepten · Quests · Meer**. *Meer* lists Profiel, Technieken, Boodschappenlijst,
  Instellingen and Over. (Profiel is also reachable from a card on Home.)
- System back and predictive back work everywhere, and each tab keeps its own back stack.
- Share targets (§6.9) open the import flow on top of the current stack.
- The current month is always `LocalDate.now(ZoneId.of("Europe/Amsterdam")).monthValue`.

### 6.2 Home

- Hero: eyebrow "NEDERLAND · {Maand} {jaar}", title "Jouw persoonlijke *kookgids* voor het seizoen" and a subtitle.
- Stats row: counts for op hoogtepunt / binnenkort / aflopend.
- "Nu op hoogtepunt": up to 4 peak vegetables plus a link "Alle {n} groenten →" (to Seizoen with filter = peak).
- "Binnenkort" (up to 3 soon) and "Laatste kans" (up to 3 ending).
- **Seizoenstip:** one peak vegetable, rotated daily (`peak[dayOfYear % peak.size]`, peak list sorted by `nl`), shown as
  "{nl}: {blurb}" with a link "Meer over {nl} →". Falls back to `home_tip_fallback`.
- **New:** "Open quest" card (latest uncompleted quest), "Recent gekookt" (last 3 recipes with an attempt),
  and a profile summary card (earned archetypes, attempts this month).

### 6.3 Seizoen (in season now)

- Shows all vegetables with status ≠ `off` for the current month (§11.1).
- Filter pills with counts: Alles in seizoen / Op hoogtepunt / Beschikbaar / Binnenkort / Aflopend.
- Search matches `nl`, `nameEn` (a hidden alias) and `family`; case- and diacritic-insensitive.
- Sorted by status order (peak, shoulder, soon, ending), then `nl` (Dutch collation).
- List rows: `VegToken`, `nl`, family, status dot + label, mini `PeakBar`. Tapping a row opens the vegetable detail.
- A segmented toggle at the top switches between **Nu** and **Kalender** (§6.4).

### 6.4 Kalender

- A year grid: each row is a vegetable, with 12 month columns (peak filled, shoulder half-filled). The current month column is highlighted.
- The grid can be grouped by family (default) or listed A–Z. The name column stays fixed while the grid scrolls horizontally when it doesn't fit.
- Tapping a row opens the vegetable detail. TalkBack reads each row as "{nl}: hoogtepunt {maanden}; beschikbaar {maanden}".

### 6.5 Groente-detail

A full screen or a large bottom sheet with:
- Header in the vegetable's colours: `nl`, family, current status, and a 12-month `PeakBar` with a legend.
- Sections: **Seizoen**, **Bewaren** (`storage`), **Bereiden** (`prep`), **Bereidingstip** (`technique`),
  **Combineert goed met** (`pairings` chips), **Als je het niet hebt** (`substitutions` chips; a chip whose text
  matches another vegetable's `nl` opens that vegetable).
- **Technieken**: techniques whose `relatedVegIds` contain this vegetable.
- **Recepten**: curated links (`curatedRecipes`: open in the browser, or import), plus library recipes that mention
  this vegetable (§11.4).
- Action: **"Start een quest met {nl}"**, which pre-fills the quest form.
- `sourceUrl` is shown as "Bron: velt.nu" and opens in the browser.

### 6.6 Technieken

- Categories, in this order, labelled from `techniques_category_*`: Snijdtechnieken, Garingsmethoden, Smaakgebouw,
  Eiwitten, Sauzen & Fonds, Groentenvoorbereiding, Gereedschap, Conservering. Within a category, techniques are grouped by `subcategory`.
- Filter by level (Basis / Gemiddeld / Gevorderd); search on `nl` + `subtitle`.
- Cards show `nl`, `subtitle`, a level badge and a heat badge.
- Detail: subtitle, `body` paragraphs, **Tips**, **Benodigde kookgerei**, **Aanbevolen groenten** (chips → vegetable detail),
  **Verwante technieken**, "Bouwt voort op" (`parentId`) and "Vervolgtechnieken" (the techniques that list this one as parent).
  Action: **"Oefen in een quest"** pre-fills the quest technique.

### 6.7 Recepten — bibliotheek

- Shows recipes with `inLibrary = true`. Sort: nieuwste eerst (default) / oudste eerst / A–Z / laatst gekookt / vaakst gekookt.
- Search across title, description, ingredient texts and source.
- Facet filters (cuisine, dish type, main ingredient, prep time) as chip rows. The filters "**Met seizoensgroenten nu**" (§11.4) and
  "Favorieten" sit on top. The empty state with filters active offers "Wis zoekopdracht en filters".
- Card: image (or a cuisine-coloured placeholder), title, source, total time, cooked count ("3× gekookt"),
  in-season vegetable chips.
- FAB **"Recept toevoegen"** opens a sheet: Link plakken · Foto's maken · Foto's kiezen · Zelf invoeren.
- Empty library: "Nog geen recepten" plus a CTA.

### 6.8 Recept — detail and in-place editing

**Detail view**
- Image, title, source (tap → original URL), author, description, times (voorbereiding / bereiden / totaal),
  servings stepper (1–99) that scales the ingredients (§11.2), and a unit toggle Metrisch/US (only when any ingredient has `textMetric`;
  metric by default).
- **Ingredient groups** with headers ("Voor de saus", "Voor de marinade"). Each ingredient can be ticked off.
  "Op boodschappenlijst" adds all ingredients or a selection (scaled).
- **Step groups** with headers. Each step shows detected timer chips (§11.3).
- **Extra velden**: free label/value pairs, for example "Wijnpairing: Chablis", "Bron pagina: 112", "Opmerking".
- Tags (facet chips), taxonomy (techniques link to technique detail), nutrition chips, keywords, diet suitability, video link.
- Personal: favourite toggle, personal notes, **cooked count + last cooked date**, and the attempt history for this recipe.
- Actions: **Kookmodus**, **Ik heb dit gekookt** (→ evaluation §6.12), Bewerken, Delen (as text), Verwijderen (with undo).

**Edit mode (in place, ToDo item)**
- "Bewerken" switches the same screen to editable fields. Everything can be edited: title, description, servings, times,
  source label/URL, groups (add, rename, delete, reorder by drag), items in groups (add/edit/delete/reorder),
  extra fields, tags, notes, image (replace from camera or gallery, or remove).
- Save / Annuleren. Leaving with unsaved changes asks for confirmation. `updatedAt` is set on save.
- "Tags genereren met Claude" (re-)runs §8.4 for this recipe.

### 6.9 Recept importeren

| Entry | Flow |
|---|---|
| Paste a link | URL field (prefilled from the clipboard when it holds a URL) → fetch + parse (§10) → preview → Bewaren |
| Share a link | `ACTION_SEND text/plain`: take the first http(s) URL from the shared text → same flow |
| Camera / gallery | 1–5 photos (photo picker, `TakePicture`, `ACTION_SEND(_MULTIPLE) image/*`) → reorder or remove → "Lees recept" → Claude vision (§8.3) → preview → Bewaren |
| No JSON-LD found | Offer "**Lees de pagina met Claude**" (§8.5) or "Zelf invoeren" |
| Zelf invoeren | An empty recipe in edit mode |

- The preview is the detail screen in edit mode, so the user can fix anything before saving. A source label is suggested
  (domain, or a cookbook title detected by Claude).
- After saving a URL import, tagging (§8.4) runs in the background when a key is configured. The recipe shows "tags worden
  gegenereerd…" and a failure never blocks the save.
- Duplicate detection: the same canonical URL already in the library → "Dit recept heb je al", with the option to open it or import a copy.
- Images are downloaded and stored locally (§7.4), so the library works offline.

### 6.10 Kookmodus and timers

- A full-screen, step-by-step pager: one step per page, large type (scales with the system font), group name as an eyebrow,
  progress "Stap 3 van 9". Swipe or tap the large previous/next buttons.
- The ingredients panel is one tap away (bottom sheet), shows the scaled amounts and remembers ticks.
- The screen stays on (`FLAG_KEEP_SCREEN_ON`) while cook mode is visible.
- **Timers:** tap a detected timer chip, or "+ timer", to start a named countdown ("Stap 4 · 12 min").
  Several timers can run at once. Pause, +1 min and stop are available.
  - While timers run, an ongoing notification shows a live countdown (`setUsesChronometer` + `setChronometerCountDown`),
    so the app can be backgrounded.
  - Completion uses AlarmManager: `setExactAndAllowWhileIdle` when `canScheduleExactAlarms()`; otherwise the app explains
    and links to the "Wekkers en herinneringen" setting, and falls back to an inexact alarm with a visible warning.
  - When a timer finishes: a high-importance notification with sound and vibration ("Timer klaar: Stap 4"). Tapping it opens cook mode.
- Leaving cook mode asks "Heb je dit gekookt?" → evaluation (§6.12), or not now.

### 6.11 Quests (previously "Uitdaging")

Renamed throughout the UI to **Quests** (ToDo). A quest is a focused cooking exercise generated by Claude (§8.3).

**Form**
- **Ingredient**, one of three modes:
  - *Zelf kiezen*: free text (max 200 characters), with an optional quantity and unit (stuks, gram, ml, liter).
  - *Seizoensgroente*: a dropdown of in-season vegetables (peak first, then shoulder).
  - *Verras me* (default when the field is empty): Claude chooses one from the current in-season list and returns its id.
- **Soort quest** (chips): Maaltijd · Lunch · Bijgerecht · Saus · Bouillon / fond · Component · Laat Claude kiezen.
  "Component" means a building block such as a pickle, crumble, flavoured oil or dressing.
- **Moeilijkheid**: Laat Claude kiezen · Beginner · Gemiddeld · Gevorderd.
- Optional: **Techniek** (searchable list from `techniques.json`, with "Laat Claude kiezen"), **Keuken** (facet values),
  **Beschikbare tijd** (<15 / <30 / <60 / >60), **Klein & eenvoudig** (pantry ingredients only).
- "Geef quest". The button is disabled with an explanation when offline or when no key is set (with a link to Instellingen).

**Result**
- Quest text, skill, difficulty badge, time estimate, "Waarom deze quest?", result type (dish type),
  the chosen vegetable (if any), and a full recipe (ingredient groups, steps, tips).
- Actions: **Start koken** (cook mode on the quest's recipe), **Andere quest** (same parameters),
  **Bewaar recept in bibliotheek** (sets `inLibrary = true`), **Quest voltooid** (→ evaluation; status COMPLETED).

**History**
- A tab with all quests: filter on Open / Voltooid, plus type and difficulty. Each item can be reopened, retried with the same parameters, or deleted.

### 6.12 Evaluatie ("Ik heb dit gekookt")

- A bottom sheet: **Uitvoering (1–10)**, **Smaak (1–10)** (both optional), **Notities**, and the date (default today; editable).
- **Vastleggen** logs an attempt (§7.2) with a snapshot of techniques/cuisines/ingredients. **Overslaan** logs the attempt without scores.
- The recipe's cooked count and `lastCookedAt` update right away.

### 6.13 Profiel

- Header stats: total attempts, quests completed, recipes cooked, attempts this month.
- **Vaardigheidsprofiel**: a radar chart over the 4 disciplines (proficiency = average execution score, 0–10) with frequency per axis.
- **Jouw kookidentiteit**: archetype badges, earned or locked, with the condition text ("Nog 4 emulsies met gem. ≥ 7").
- **Keukens & ingrediënten**: meest gekookte keukens, hoogst gewaardeerde keukens (taste score) and an ingredient cloud.
- **Technieken**: meest geoefend, hoogst beoordeeld, and **verwaarloosd** (disciplines not practised in the last 30 days).
- **Geschiedenis**: a paged attempt ledger (newest first), with filters for recipe or quest.
- All numbers follow §12 exactly.

### 6.14 Boodschappenlijst

- Items grouped by recipe (plus "Los"). Tick off items, swipe to delete, add manual items.
- Clear ticked items, or clear everything (with undo). "Delen" shares the list as plain text.

### 6.15 Meer and Instellingen

- **Meer**: Profiel, Technieken, Boodschappenlijst, Instellingen, Over Mise.
- **Instellingen → Claude**: key status (none / valid / invalid / locked), add/replace/delete key (§9),
  "Sleutel testen", model per feature (§8.2), biometric lock toggle, usage this month (tokens + estimated € / $),
  and a tip to set a monthly spend limit in the Anthropic Console.
- **Instellingen → Gegevens**: export a backup, restore a backup, automatic backup (folder + frequency, last run),
  import old web-app data (§13.3), delete all data (typed confirmation "VERWIJDER").
- **Instellingen → Meldingen**: seasonal notification on/off; timer alarm permission status.
- **Over**: version, data sources (velt.nu), open-source licences (including the fonts), privacy statement (§15.1).

### 6.16 First run

1. Welcome (brand + one sentence).
2. Optional: "Claude-sleutel toevoegen" (explains which features need it, what it costs, how it is stored) or "Later".
3. Optional: "Gegevens van de oude web-app importeren" (§13.3).
4. Notification permission is asked **only** the first time a timer or seasonal notification is turned on.

---

## 7. Data

### 7.1 Static content (APK assets)

`ContentRepository` loads `vegetables.json`, `techniques.json`, `facets.json`, `disciplines.json` and `archetypes.json`
once, on a background dispatcher, with kotlinx.serialization (`ignoreUnknownKeys = false` in tests and `true` in release).
Files whose `schemaVersion` is unsupported fail loudly in debug builds. Models mirror `data/schemas/*.schema.json` exactly.
Content is read-only at runtime. Lookups by id are O(1) maps.

### 7.2 Local database (Room)

IDs are UUID strings; timestamps are epoch millis (UTC). Lists that are only ever read whole are stored as JSON text through TypeConverters.

| Entity | Fields |
|---|---|
| `recipe` | `id`, `title`, `description`, `sourceType` (URL / PHOTO / MANUAL / QUEST / LEGACY), `sourceUrl?`, `sourceLabel`, `author?`, `imagePath?`, `imageUrl?`, `servings?`, `prepMinutes?`, `cookMinutes?`, `totalMinutes?`, `hasMetricAlt`, `nutrition` (json: calories/protein/carbs/fat strings), `keywords` (json), `dietTags` (json), `datePublished?`, `videoUrl?`, `ratingValue?`, `ratingCount?`, `notes?`, `isFavorite`, `inLibrary`, `taggingState` (NONE / PENDING / DONE / FAILED), `createdAt`, `updatedAt` |
| `ingredient_group` | `id`, `recipeId` (FK, cascade), `position`, `name?` (null = untitled default group) |
| `ingredient` | `id`, `groupId` (FK, cascade), `position`, `text`, `textMetric?` |
| `step_group` | `id`, `recipeId`, `position`, `name?` |
| `step` | `id`, `groupId`, `position`, `text` |
| `recipe_extra_field` | `id`, `recipeId`, `position`, `label`, `value` |
| `recipe_tag` | `recipeId`, `facetKey`, `value` (PK all three) |
| `recipe_taxonomy` | `recipeId`, `kind` (TECHNIQUE / CUISINE / INGREDIENT), `value` (PK all three) |
| `quest` | `id`, `createdAt`, `status` (OPEN / COMPLETED), `completedAt?`, `ingredientMode` (CUSTOM / SEASONAL / SURPRISE), `ingredient`, `vegId?`, `quantity?`, `unit?`, `questType`, `requestedDifficulty?`, `requestedTechniqueId?`, `requestedCuisine?`, `timeLimit?`, `simpleAndSmall`, `text`, `skill`, `difficulty`, `timeEstimateMinutes`, `why`, `dishType`, `tips` (json), `recipeId` (FK → `recipe` with `sourceType = QUEST`), `model` |
| `attempt` | `id`, `sourceType` (RECIPE / QUEST), `recipeId?`, `questId?`, `cookedAt`, `score?` (1–10), `tasteScore?` (1–10), `notes?`, `techniques` (json snapshot), `cuisines` (json snapshot), `ingredients` (json snapshot) |
| `shopping_item` | `id`, `text`, `recipeId?`, `checked`, `position`, `createdAt` |
| `claude_usage` | `id`, `at`, `feature` (QUEST / PHOTO_IMPORT / TAGGING / PAGE_IMPORT / KEY_CHECK), `model`, `inputTokens`, `outputTokens`, `cacheReadTokens`, `costMicroUsd` |

- Indexes on all FKs, plus `recipe(inLibrary, updatedAt)`, `attempt(cookedAt)`, `attempt(recipeId)`, `quest(status, createdAt)`.
- Derived values (never stored): cooked count = `count(attempt where recipeId = ?)`, and `lastCookedAt`.
- Attempts keep **snapshots**, so the profile doesn't change when a recipe is edited later.
- Deleting a recipe keeps its attempts (`recipeId` set to null) so profile history survives. Deleting a quest deletes its hidden recipe.
- Schema changes go through numbered Room migrations with a `MigrationTestHelper` test. **Never** use destructive migration.

### 7.3 Settings (DataStore `settings.preferences_pb`)

`modelQuest`, `modelPhotoImport`, `modelTagging`, `biometricLockEnabled`, `seasonalNotificationsEnabled`,
`autoBackupTreeUri?`, `autoBackupFrequency` (OFF / DAILY / WEEKLY), `lastBackupAt?`, `lastSeasonalNotifiedMonth?`,
`preferredUnitSystem` (METRIC default), `onboardingDone`, `librarySort`. The API key is **not** stored here (§9).

### 7.4 Files

- Recipe images: `filesDir/images/{recipeId}.jpg`, downscaled to at most 1600 px on the long edge, JPEG quality 85, EXIF stripped.
- Import photos sent to Claude are temporary (`cacheDir/import/`) and deleted after a successful import. The user can keep
  the first one as the recipe image.

---

## 8. Claude integration

### 8.1 Client

- The `ClaudeClient` interface (suspend functions) has two implementations: `AnthropicSdkClaudeClient` (the official Java SDK, preferred)
  and `OkHttpClaudeClient` (raw `POST https://api.anthropic.com/v1/messages`, `anthropic-version: 2023-06-01`).
  A P0 spike decides which one ships. The SDK must work on API 30+ with R8 enabled.
- The key is read from `SecretStore` per call (§9). It never appears in logs, crash reports or exceptions shown to the user.
- **Structured outputs** for every generation call: `output_config.format` = JSON Schema (§8.6). No regex extraction of JSON.
- **Do not send** `temperature`, `top_p` or `top_k`: since 2026-10-21 the API rejects them with a 400. The legacy quest prompt
  used `temperature: 0.7`; drop it.
- Thinking: omit the parameter (adaptive by default on current models). `output_config.effort` is set per feature (§8.2).
- `max_tokens`: 16000 (non-streaming). Read timeout: 180 s. A cancellable progress UI is shown while waiting.
- **Response checks, in order:** `stop_reason == "refusal"` → Dutch message "Claude wilde dit verzoek niet uitvoeren"
  (show `stop_details.explanation` when present); `max_tokens` → "Antwoord was te lang en is afgebroken"; otherwise parse
  the first `text` block as JSON and validate it against the vocabulary (§8.6).
- **Refusal fallback:** on Sonnet 5.5 / Opus 5.5 requests to the Claude API, send `fallbacks: "default"` with beta header
  `server-side-fallback-2026-07-01` (the server re-runs a declined request on a fallback model). Haiku 5.5 has no
  server-side fallback.
- **Retries:** network errors, 408, 429 (honour `retry-after`), 500/502/503/529 → at most 2 retries with exponential
  backoff and jitter. **Never retry** 400 / 401 / 403 / 404 / 413. A 401 marks the key invalid and links to Instellingen.
- **Usage tracking:** after each call, store a `claude_usage` row from `usage` × the price table (`claude/Pricing.kt`, with a
  "prijzen per" date; per MTok input/output: Haiku 5.5 $0.10/$0.50, Sonnet 5.5 $2/$10, Opus 5.5 $4/$20).

### 8.2 Features, models and effort

| Feature | Default model | Effort | Input |
|---|---|---|---|
| Quest generation (§8.3) | `claude-sonnet-5-5` | medium | form parameters + in-season list + vocab |
| Recipe from photos (§8.3) | `claude-sonnet-5-5` | medium | 1–5 JPEGs (≤ 1568 px long edge, ≤ 5 MB each) + instructions |
| Tagging / taxonomy (§8.4) | `claude-haiku-5-5` | low | title, ingredients, steps |
| Page text fallback (§8.5) | `claude-sonnet-5-5` | medium | visible page text (≤ 60k chars) |
| Key check | — | — | `GET /v1/models` (no tokens used) |

In Instellingen, each feature offers **Snel & goedkoop** (Haiku 5.5) / **Gebalanceerd** (Sonnet 5.5) /
**Beste kwaliteit** (Opus 5.5).

### 8.3 Prompts

Prompts are Kotlin string templates in `claude/prompts/`. They are written in Dutch, built from `data/reference/prompts/` but
**without** JSON-format instructions, because the schema enforces the format. They keep the old rules:
- Quest: coaching tone; a concrete, finished dish; respect every given parameter strictly; a usable recipe with no vague
  steps; "Klein & eenvoudig" means few, pantry ingredients; *Verras me* picks a vegetable from the provided in-season list and
  returns its id; the soort quest decides the output (a Saus quest yields a sauce, not a meal).
- Photos: combine multiple photos into one recipe; **convert all measures to metric** (oz→g, cups→ml, °F→°C);
  keep printed section headers as group names ("Voor de saus"); capture extra printed fields (wine pairing, page number,
  notes) as `extraFields`; return the cookbook title if visible.
- Tagging: only tag what is clearly supported by the recipe; at most 8 main ingredients, Dutch, lowercase.

The system prompt and the vocabulary block come first and stay byte-stable, so prompt caching applies when the prefix is
long enough; per-request content comes last.

### 8.4 Tagging output

`{ tags: { cuisine[], dishType[1], mainIngredient[], prepTime[1] }, taxonomy: { techniques[], cuisines[], ingredients[] } }`.

### 8.5 Page text fallback

When a page has no JSON-LD recipe, the app extracts the readable text with Jsoup (drops script/style/nav/footer and trims to
60k characters), shows "Dit kost ongeveer € X" (estimated from the character count), and on confirmation sends it with the photo-import
schema. The URL is kept as `sourceUrl`.

### 8.6 Output schemas and validation

- Schemas are built **at runtime** from the content assets, so enums always match: technique ids from `techniques.json`,
  facet values from `facets.json`, vegetable ids from the in-season list.
- Keep schemas within the structured-output subset: every object has `additionalProperties: false` and all properties
  `required`; optional values are `["type","null"]`; use enums; no numeric bounds. Bounds such as 1–5 photos, servings 1–99 and
  max 8 ingredients are checked client-side.
- **Quest schema:** `questText`, `skill`, `difficulty` (BEGINNER / INTERMEDIATE / ADVANCED), `timeEstimateMinutes`, `why`,
  `dishType` (facet enum), `chosenIngredient`, `chosenVegId` (enum of in-season ids, or null), `tags` (§8.4),
  `taxonomy` (§8.4), `recipe { title, servings, ingredientGroups[{ name|null, items[{ amount, unit, name }] }],
  steps[{ text }], tips[] }`.
- **Photo / page recipe schema:** `title`, `description`, `sourceLabel|null`, `servings|null`, `prepMinutes|null`,
  `cookMinutes|null`, `totalMinutes|null`, `ingredientGroups[{ name|null, items[string] }]`,
  `stepGroups[{ name|null, steps[string] }]`, `extraFields[{ label, value }]`, `tags`, `taxonomy`.
- After parsing: unknown enum values are dropped (not fatal), strings are trimmed, empty groups removed, and taxonomy
  ingredients lowercased and capped at 8.

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
- **Biometric lock (optional):** when enabled, the wrapping key is re-created with
  `setUserAuthenticationParameters(300, AUTH_BIOMETRIC_STRONG or AUTH_DEVICE_CREDENTIAL)` and the stored key re-encrypted.
  Using Claude then needs a BiometricPrompt at most every 5 minutes.
- `KeyPermanentlyInvalidatedException`, `AEADBadTagException`, or a missing alias → wipe the stored ciphertext, set the
  status to "Sleutel opnieuw invoeren", and never crash.

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
  byte-identical so the goldens pass; the mapping step below turns default names into untitled groups.
- Instructions: plain strings, `HowToStep`, `HowToSection` (one section each), and a mix (loose steps join the
  preceding section; steps before the first section go to the default section). Steps are numbered from 1 per section.
- Text cleaning: strip HTML (block closers and `<br>` → space), decode entities, normalise curly quotes, ring-above → °,
  collapse whitespace.
- Durations: ISO 8601 (`P1DT30M`, `PT1H15M`); seconds round up to the next minute; zero or invalid → null.
- Nutrition, image (string / object / array), author (string / object / array), rating, video, keywords, diet, cuisine/category (string or array).
- **WPRM override:** when a `wprm_recipes` script exists, take the ingredients from it (US text + `textMetric` when different, notes included).
- **Acceptance:** all 104 cases in `data/fixtures/recipe-ingestion/golden.json` pass. Each case is compared by JSON
  equality of the result (or the expected `RecipeIngestionError` code).
- Mapping to Room: sections → groups. A section named `Ingredients`, `Instructions`, `Ingrediënten` or `Bereiding`
  becomes an untitled group (`name = null`); other names are kept as headers ("Voor de saus").

---

## 11. Domain rules

### 11.1 Seasonality

```
statusFor(veg, month):
  if month in peak            → PEAK
  if month in shoulder        → SHOULDER
  if any p in peak with (p - month) mod 12 in {1, 2} → SOON
  if previousMonth(month) in peak                    → ENDING
  else                        → OFF
```

Acceptance: matches `data/fixtures/seasonality/status.json` for all 66 × 12 cells.
Display order: PEAK, SHOULDER, SOON, ENDING, OFF.

### 11.2 Portion scaling and units

- `ratio = targetServings / baseServings` (no scaling when the base is unknown; the stepper is hidden then).
- Unicode fractions (½ ⅓ ⅔ ¼ ¾ ⅛ ⅜ ⅝ ⅞) → decimals; every number (`\d+([.,]\d+)?`) × ratio; integers stay integers,
  otherwise 1 decimal. **Output uses the Dutch decimal comma** (`1,5 el`). This intentionally differs from the web app, which printed `1.5`.
- Unit toggle: show `textMetric` when METRIC is selected and present, otherwise `text`.

### 11.3 Duration detection (timer chips)

Dutch and English patterns in step text: `(\d+)(?:\s*[-–]\s*(\d+))?\s*(seconden|sec|minuten|minuut|min|uur|u|hours?|minutes?)`.
For a range, use the upper bound. Ignore matches that look like temperatures or quantities (followed by `°`, `g`, `ml`).

### 11.4 Seasonal matching for recipes

A recipe "uses" vegetable V when any name in `V.nl.split(" / ")` (lowercased, diacritics folded) appears in the
recipe's taxonomy ingredients or its ingredient texts, matched as a whole word with an optional Dutch plural `-s` / `-en`.
"Met seizoensgroenten nu" = uses at least one vegetable whose status is PEAK or SHOULDER.

---

## 12. Profile aggregation and archetypes

Input: all attempts. "Matches discipline D" means at least one technique id in the attempt has `discipline == D` (from `techniques.json`).

- **Disciplines** (order from `disciplines.json`): `frequency` = number of matching attempts; `proficiency` = mean of their
  non-null `score`, else 0.
- **Top cuisines:** count per cuisine across attempts; `avgScore` = mean of non-null **taste** scores; sorted by count desc, top 10.
  "Hoogst gewaardeerd" = the same list restricted to cuisines with ≥ 1 taste score, sorted by avgScore desc.
- **Ingredient cloud:** count per ingredient, sorted desc (show the top 30).
- **Top techniques:** count per technique id; `avgScore` = mean of non-null **execution** scores; count desc, top 10.
- **Neglected disciplines:** disciplines with no matching attempt, or whose most recent matching attempt is more than 30 days ago.
- **Recent attempts:** newest 20 by `cookedAt`.
- **Archetypes** (`archetypes.json`): earned when matching attempts ≥ `minAttempts` and (if `minAvgScore`) the mean
  of their non-null execution scores ≥ `minAvgScore`. No scores means not earned. Progress text shows what's missing.
- Acceptance: port the cases from `web/lib/profileAggregation.test.ts@cf3ec39` plus archetype edge cases.

---

## 13. Backup, restore and legacy import

### 13.1 Export

- `Mise-backup-YYYYMMDD-HHmm.zip` via the system file picker (SAF `CreateDocument`) or the share sheet.
- Contents: `backup.json` + `images/{recipeId}.jpg`. `backup.json` holds `{ format: "mise-backup", schemaVersion: 1,
  exportedAt, appVersion, recipes[] (the full graph with groups, items, extra fields, tags, taxonomy), quests[], attempts[], shoppingItems[],
  settings (non-secret subset) }`.
- **Never** includes the API key, the Keystore material or `claude_usage` cost data. `claude_usage` is optional and off by default.

### 13.2 Restore and automatic backup

- Restore: pick a file → validate format and version → show a summary ("42 recepten, 17 quests, 88 pogingen") → choose
  **Samenvoegen** (by id; newer `updatedAt` wins) or **Vervangen** (wipe first, with typed confirmation) → run inside one DB transaction.
- Automatic backup: the user picks a folder (`OpenDocumentTree`, persisted permission). WorkManager writes daily or weekly
  (only when charging or with battery not low) and keeps the newest 5 files. A failure raises a low-priority notification.
- Android Auto Backup may include the DB and images. The secrets file is excluded (§9).

### 13.3 Legacy import (old web app)

One-time import of a JSON file the user exports **before** shutting down Supabase/Vercel:

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
- The import accepts one or both files. Mapping is in Appendix A. Recipes are deduplicated by `source_url` + title. IDs are kept
  where they are UUIDs, so attempts still link to their recipes and quests. A summary report lists anything skipped.

---

## 14. Notifications, background work and permissions

| Channel | Importance | Use |
|---|---|---|
| `timers` | HIGH (sound, vibration) | Finished timers; ongoing countdown (LOW, silent) |
| `season` | DEFAULT | Monthly "Nu op hoogtepunt: asperges, rabarber, …" (vegetables that became PEAK this month) |
| `backup` | LOW | Automatic backup failed |

- Seasonal notification: a daily WorkManager job posts once per month (on the first run on or after the 1st, ~09:00), only when enabled.
- Permissions: `INTERNET`, `POST_NOTIFICATIONS` (requested in context), `SCHEDULE_EXACT_ALARM` (user-granted, optional),
  `RECEIVE_BOOT_COMPLETED` (re-arm running timers and the seasonal work), `USE_BIOMETRIC`. **No** camera, storage or location
  permissions: the photo picker, `TakePicture` and SAF cover those needs.

---

## 15. Non-functional requirements

### 15.1 Privacy
- No analytics, no crash reporting service, no ads. Network traffic goes only to: recipe URLs the user enters or shares,
  `api.anthropic.com`, and image URLs of imported recipes.
- What goes to Claude: the selected photos, the recipe text or page text being tagged or imported, quest parameters and the in-season
  vegetable list. Nothing else. The app states this in Over and on first key entry.

### 15.2 Performance
- Cold start to Home in under 1.5 s on a mid-range phone; asset parsing stays off the main thread; lists are lazy; library search
  responds in under 100 ms for 1,000 recipes (Room FTS4 on title/description/ingredients if needed).

### 15.3 Accessibility
- TalkBack labels in Dutch for every icon button, chart and status (month names in full, not abbreviated).
- Supports font scale up to 200% without clipping; 48 dp targets; colour is never the only status signal (dot + label).
- Status colours meet WCAG AA contrast on cream for text use (the `*-fg` variants).

### 15.4 Language
- Every user-visible string lives in `res/values/strings.xml` (Dutch is the default and only locale). Lint `HardcodedText`
  and a unit test fail the build on hardcoded UI text. Plurals use `<plurals>`. Number and date formatting uses the `nl-NL` locale.
- Content prose comes from the assets (already Dutch). `nameEn` and `nutrientsEn` are never displayed.

### 15.5 Robustness
- Every Claude or network operation is cancellable, shows progress, and returns to a stable state on failure with a retry button.
- The database is never left half-written: multi-table writes run in one transaction.
- Process death restores in-progress forms (SavedStateHandle) and running timers (alarms are system-held).

---

## 16. Testing and quality

| Level | What |
|---|---|
| Data | `python3 tools/validate_data.py` (schemas, cross-references, strings.xml, seasonality drift) in CI and as a Claude Code hook |
| Unit (JVM) | Asset deserialisation of every file (`ignoreUnknownKeys = false`); seasonality goldens; parser goldens; scaling + Dutch formatting; duration detection; seasonal matcher; profile aggregation; archetypes; Claude schema builder + response mapping (recorded JSON responses, no live calls); legacy mapping (synthetic fixtures); backup round-trip |
| Integration | Room DAOs + `MigrationTestHelper`; `SecretStore` on Robolectric/instrumented (encrypt → decrypt, invalidation path) |
| UI (Compose) | Import from URL (fake fetcher), edit-in-place save/cancel, quest form → result (fake client), key entry states, cook mode timer chips |
| Manual checklist | Share from Chrome/Instagram, camera import, timers with the app backgrounded, restore on a fresh install, Doze behaviour |

CI must stay green on every push: validate data → `./gradlew lint ktlintCheck testDebugUnitTest assembleDebug`.

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

- **Markten in de buurt / boerenmarkten** (ToDo): needs a data source for Dutch farmer markets with schedules. Research first.
- **Wildplukken / plukkaart** (ToDo): a separate foraging dataset with strict safety rules (look-alikes, protected areas).
- **Vervangers & combinaties explorer** (web "binnenkort").
- Dark theme (if not done in P2), home-screen widget ("Nu op hoogtepunt"), Wear OS timers.

---

## Appendix A — legacy data formats and mapping

**Supabase `recipes` row** → `recipe` (`sourceType` = URL, or PHOTO when `source_url` starts with `mise-photo:`; LEGACY when unknown)
- `instructions` holds a JSON `RecipeOutput` (below). When it parses, it is the primary source. Otherwise build the recipe from the
  columns: `name`, `ingredients[]` (one untitled group), `description`, `prep_time`, `cook_time`, `total_time`, `servings`,
  `cuisine[]`, `category[]`, `keywords[]`, `image_url`, `author`, `suitable_for_diet[]`, `date_published`,
  `aggregate_rating{ratingValue,ratingCount}`, `video_url`, `nutrition{calories,protein,carbs,fat}`.
- Column values override the JSON when they are non-null (the same precedence as the web app's `fetchRemoteSaved`).
- `source` → `sourceLabel`; `tags` (`{cuisine?,dishType?,mainIngredient?,prepTime?: string[]}`) → `recipe_tag`; `created_at` → `createdAt`.

**`RecipeOutput`** (web type): `title`, `description`, `prepTimeMinutes`, `cookTimeMinutes`, `totalTimeMinutes`,
`servings?`, `cuisine` (string or string[]), `category` (string or string[]), `keywords[]`, `image`, `author`,
`suitableForDiet[]`, `datePublished`, `aggregateRating`, `video`, `nutrition`,
`ingredientSections[{ sectionName, ingredients[{ text, textMetric? }] }]`,
`instructionSections[{ sectionName, steps[{ stepNumber, text }] }]`, `unitSystems?{ hasMetric }`.

**localStorage `mise-saved-recipes`** entries: `{ url, source, recipe: RecipeOutput, savedAt (ms), remoteId?, tags?, taxonomy?{techniques,cuisines,ingredients} }`.
Entries with a `remoteId` that matches an imported Supabase row are merged into that row (filling in taxonomy); the rest are imported as new recipes.

**Supabase `challenges` row** → `quest` + hidden `recipe` (`sourceType = QUEST`)
- `ingredient`, `challenge_text` → `text`, `skill`, `difficulty` (Beginner / Intermediate / Advanced, or legacy easy / medium / hard →
  BEGINNER / INTERMEDIATE / ADVANCED), `time_estimate`, `why`, `output_type` → `dishType`, `params`
  (`{technique?, cuisine?, timeLimit?, simpleAndSmall?, quantity?, unit?}`), `created_at`.
- `recipe` JSON `{ title, servings, ingredients[{amount,unit,name}], steps[{step,instruction}], tips[], tags? }` →
  one untitled ingredient group (text = "amount unit name", trimmed), steps, tips. `techniques`/`cuisines`/`ingredients` → taxonomy.
- `ingredientMode = CUSTOM`, `questType` = derived from `output_type` (Saus/Basis → SAUS, else MAALTIJD).
- `status` = COMPLETED if any `user_attempts.source_id` references it, else OPEN.

**Supabase `user_attempts` row** → `attempt`: `source_type` (challenge → QUEST, recipe → RECIPE), `source_id` → `questId`/`recipeId`
(dropped if unresolved), `attempted_at` → `cookedAt`, `score`, `taste_score`, `notes`, `techniques[]`, `cuisines[]`, `ingredients[]`.

## Appendix B — traceability of the old `ToDo` file

| Old ToDo item (Dutch) | Where |
|---|---|
| Markten in de buurt / FarmerMarkets toevoegen | §18 (P3) |
| Wildplukken | §18 (P3) |
| Recept-structuur: "Voor saus" / "Voor marinade", flexibele velden (Wijn pairing; Tobias-kookboek) | §6.8, §7.2 (`ingredient_group`, `step_group`, `recipe_extra_field`), §8.3 photo prompt |
| Aantal keren gekookt | §6.7, §6.8, §7.2 (derived from `attempt`) |
| Edit mode in recept zelf | §6.8 |
| Overzicht oude challenges | §6.11 History |
| Moeilijkheidsgraad als keuze | §6.11 form |
| Challenge als "Saus maken" / "Bouillon maken" / "Component maken" | §6.11 Soort quest |
| Tickbox seizoensingrediënt (leeg = Claude kiest, of dropdown) | §6.11 ingredient modes |
| Soort eten: bijgerecht, saus, bouillon, component, maaltijd, lunch | §6.11 Soort quest |
| Challenges hernoemen naar "Quests" | §6.11, todo.md (strings) |
