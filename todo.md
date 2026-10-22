# Mise Android — overhaul todo

Actionable breakdown of [`spec.md`](spec.md). Each task names the spec section that defines "done".
Work top to bottom inside a phase; phases ship in order. Tick a box only when its acceptance check passes.

Legend: **[owner]** = needs you (account, device or decision), everything else is code work.

---

## Before anything else: secure the old data **[owner]**

The web app keeps running from its last Vercel deployment until you turn it off, but this repo no longer contains its code.

- [ ] **Export Supabase data** with the SQL query in spec §13.3 (Supabase dashboard → SQL editor). Save the result as `mise-legacy-supabase.json` somewhere safe, outside the repo.
- [ ] **Export browser data**: open the deployed site in the browser you used, open the devtools console, and run the `localStorage` snippet in spec §13.3. Save the result as `mise-legacy-local.json`.
- [ ] Check both files open and look complete (count the recipes).
- [ ] Review the assumptions A1–A8 in spec §0 and change any you disagree with **before** P0. A5 (applicationId) is hard to change later.
- [ ] Once the Android app has imported the data (P1 › Legacy import): disconnect the Vercel project, then pause or delete the Supabase project.
      Merging the PR that removes `web/` makes new Vercel builds fail; the existing deployment keeps serving until you disconnect it.

---

## P0 — Foundation

### Project setup
- [ ] Scaffold the Android project at the repo root: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper, `:app` module (spec §4.1, §4.3).
- [ ] Set `minSdk 30`, `targetSdk`/`compileSdk 36`, applicationId `nl.potentplum.mise`, debug suffix `.debug` (spec §0 A4/A5, §17).
- [ ] Wire `data/` into source sets: assets, res and test resources (spec §4.3). Acceptance: `strings.xml` from `data/res` resolves via `R.string.*`.
- [ ] Add Hilt, Compose BOM, Material 3, Navigation Compose, Room + KSP, DataStore, kotlinx.serialization, OkHttp, Jsoup, Coil 3, WorkManager, Timber.
- [ ] Lint config: `HardcodedText` = error; add ktlint (or detekt). Acceptance: a hardcoded `Text("Hallo")` fails `./gradlew lint`.
- [ ] Create the package skeleton from spec §4.2 with a placeholder file per package.
- [x] Update `.gitignore` for Android and signing material, and `.gitattributes` for binary artifacts (done in the overhaul PR).

### CI
- [ ] `.github/workflows/ci.yml`: Python `tools/validate_data.py` → `./gradlew lint ktlintCheck testDebugUnitTest assembleDebug` → upload the debug APK artifact (spec §16, §17).
- [ ] Replace the Claude Code SessionStart hook: install the Android SDK command-line tools if missing, accept licences, warm the Gradle cache (so cloud sessions can build). Add `Bash(./gradlew *)` usage notes to `CLAUDE.md` once the hook works.

### Design system (spec §5)
- [ ] Bundle the fonts Instrument Serif (regular + italic), Instrument Sans (400/500/600) and JetBrains Mono (400/500) under `res/font`, plus their OFL licence texts.
- [ ] `MiseTheme`: colour scheme from the token table, `MiseColors` for the status colours, typography, shapes. Dynamic colour off.
- [ ] Shared components: `HeroHeader`, `Eyebrow`, `VegToken`, `StatusDot`, `PeakBar`, `FilterPills`, `FacetChips`, `DifficultyBadge`, `EmptyState`, `ErrorState`.
- [ ] Compose `@Preview`s for every component (light theme).

### App shell (spec §6.1)
- [ ] `MainActivity` (edge-to-edge) + `NavHost` with type-safe routes, a bottom bar (Home · Seizoen · Recepten · Quests · Meer), a back stack per tab and predictive back (`enableOnBackInvokedCallback`).
- [ ] `Clock`/`MonthProvider` abstraction (Europe/Amsterdam), injectable for tests.

### Content layer (spec §7.1, §11.1)
- [ ] Kotlin models + serializers for `vegetables`, `techniques`, `facets`, `disciplines`, `archetypes`, matching `data/schemas/*` exactly.
- [ ] `ContentRepository`: loads once off the main thread, exposes id maps, fails loudly on an unknown `schemaVersion` in debug builds.
- [ ] Unit test: every asset deserialises with `ignoreUnknownKeys = false`, and the counts match the validator (66 / 74 / 4 / 4 / 5).
- [ ] `Seasonality.statusFor` + ordering. Acceptance: all 792 cells of `fixtures/seasonality/status.json` pass.

### Database (spec §7.2, §7.3)
- [ ] Room entities, DAOs and the DB v1 for every table in §7.2, with schema export enabled (`app/schemas/` committed).
- [ ] `MigrationTestHelper` set up (a trivial v1 test now; real migrations later).
- [ ] DataStore `SettingsRepository` (spec §7.3).

### Security and Claude spike (spec §8.1, §9)
- [ ] **Spike:** call `GET /v1/models` and a minimal structured-output `messages` request with the Anthropic Java SDK on a real device with an R8 release build. Record the outcome in `spec.md` §8.1 and choose the SDK or the OkHttp client.
- [ ] `KeystoreCipher` (AES-256-GCM, StrongBox with TEE fallback, `setUnlockedDeviceRequired`) + `SecretStore` (a separate DataStore file).
- [ ] `data_extraction_rules.xml` + `full_backup_content.xml` exclude `secrets.preferences_pb`. Acceptance: `adb shell bmgr backupnow` output does not contain the file.
- [ ] Handle invalidated or missing keys gracefully: the status becomes "opnieuw invoeren", with no crash (test on Robolectric or instrumented).
- [ ] Scrub `sk-ant-…` from exception messages; Timber no-op in release; no HTTP logging in release.

---

## P1 — Parity with the web app

### Strings
- [ ] Rename user-facing "Uitdaging" → "Quest(s)" in `strings.xml` (`challenge_*` keys → `quest_*`; nav label "Quests"; history title "Eerdere quests"; result buttons). Validator must stay green.
- [ ] Prune or reword web-only copy: `recipes_photo_dropzone`, `recipes_error_network` ("server" → "internet"), the "binnenkort" detail tags/notes, `controls_grid/list_aria_label`, `recipes_cooking_mode_unsupported`.
- [ ] Add every new string needed below to `data/res/values/strings.xml` as you go (Dutch only, snake_case keys, run the validator).

### Home, Seizoen, Kalender, Groente-detail
- [ ] Home (spec §6.2): hero, stats, spotlight (4), soon (3), ending (3), daily rotating seasonal tip.
- [ ] Seizoen (spec §6.3): status filter pills with counts, diacritic-insensitive search (`nl`, `nameEn`, family), sorting, Nu/Kalender toggle.
- [ ] Kalender (spec §6.4): year grid, family grouping and A–Z, current-month highlight, TalkBack row descriptions.
- [ ] Groente-detail (spec §6.5): all sections, substitution chips that link to other vegetables, related techniques, curated links, source link.

### Technieken (spec §6.6)
- [ ] Category list with subcategory grouping, level filter, search.
- [ ] Technique detail: body, tips, cookware, related vegetables, related techniques, parent and children.

### Recepten
- [ ] `RecipeRepository` (full graph read and write in one transaction, search, facet filters, sorting, cooked count via `attempt`).
- [ ] `JsonLdRecipeParser` + `WprmExtractor` port (spec §10). Acceptance: all 104 cases in `fixtures/recipe-ingestion/golden.json` pass.
- [ ] `RecipeFetcher` (OkHttp, https upgrade, 5 redirects, 5 MB cap, charset, timeouts, Dutch error mapping) with MockWebServer tests.
- [ ] Section → group mapping (default names → untitled groups) + an image download/downscale to `filesDir/images`.
- [ ] Library screen (spec §6.7): cards, search, facet chips, sort, empty states, FAB sheet.
- [ ] Recipe detail (spec §6.8): groups, ingredient check-off, servings scaling with the Dutch decimal comma (spec §11.2), Metrisch/US toggle, nutrition, tags, taxonomy links, notes, favourite.
- [ ] `IngredientScaler` unit tests (fractions, ranges, decimals, integer preservation, comma output).
- [ ] Import by pasted link (spec §6.9) with preview-in-edit-mode and duplicate detection.
- [ ] Manual entry ("Zelf invoeren").
- [ ] Delete with undo; "Delen" as text.

### Claude features (spec §8)
- [ ] `ClaudeClient` (the implementation chosen in P0), retry policy, error mapping, refusal / `max_tokens` handling, refusal fallback header for Sonnet/Opus.
- [ ] `Pricing` + `claude_usage` recording; unit tests for cost maths.
- [ ] Runtime schema builders (quest, recipe-from-photos/page, tagging) from the content assets, and post-parse validation (spec §8.6). Unit tests with recorded responses.
- [ ] Rewrite the three prompts from `data/reference/prompts/` as Kotlin templates without JSON-format instructions (spec §8.3).
- [ ] Photo import (spec §6.9): photo picker + camera, 1–5 photos, reorder/remove, downscale to ≤ 1568 px, call, preview in edit mode, keep the first photo as the image.
- [ ] Background tagging after a URL import (`taggingState`), plus a "Tags genereren" action.

### Quests (spec §6.11)
- [ ] Quest form: ingredient modes (Zelf kiezen / Seizoensgroente / Verras me), soort quest, difficulty, technique, cuisine, time, simple, quantity + unit.
- [ ] Generation → `quest` + hidden recipe stored in one transaction; result screen with all actions.
- [ ] Quest history tab with filters (open/completed, type, difficulty), reopen, retry with the same parameters, delete.
- [ ] Offline / no-key disabled state with explanation and a link to Instellingen.

### Evaluation and profile (spec §6.12, §6.13, §12)
- [ ] Evaluation sheet (scores, notes, date) → `attempt` with a taxonomy snapshot; "Overslaan" logs without scores.
- [ ] `ProfileAggregator` port. Acceptance: the cases ported from `web/lib/profileAggregation.test.ts@cf3ec39` pass (`git show cf3ec39:web/lib/profileAggregation.test.ts`).
- [ ] `ArchetypeEvaluator` (data-driven from `archetypes.json`) + progress text; edge-case tests (no scores, exact thresholds).
- [ ] Profile screen: stats, radar chart (Canvas), archetype badges, cuisines, ingredient cloud, techniques, neglected, history ledger.

### Settings and key (spec §6.15, §9)
- [ ] Key entry screen: password field without IME learning, `FLAG_SECURE`, live check via `GET /v1/models`, masked display, offer to clear the clipboard, delete key.
- [ ] Model choice per feature (Snel & goedkoop / Gebalanceerd / Beste kwaliteit); usage this month.
- [ ] Optional biometric lock (re-wrap the key with an auth-bound Keystore key; BiometricPrompt at most every 5 minutes).
- [ ] Over screen: version, data sources, OSS licences (including fonts), privacy statement and the threat-model note.

### Backup and legacy import (spec §13)
- [ ] Export to zip (`backup.json` + images) via SAF/share. The key is never included (unit test asserts this).
- [ ] Restore: validate, summary, Samenvoegen / Vervangen, in one transaction. Round-trip test: export → wipe → restore gives an identical DB.
- [ ] **Legacy import** (spec Appendix A): parse `mise-legacy-supabase` + `mise-legacy-local`, dedupe, keep UUIDs, link attempts, show a report. Write synthetic fixture files covering every branch (JSON-in-`instructions`, column fallback, photo prefix, legacy difficulty values, unresolved `source_id`).
- [ ] **[owner]** Import your real exported files on your phone and spot-check 5 recipes, 3 quests and the profile numbers.

### First run (spec §6.16)
- [ ] Welcome → optional key → optional legacy import → done (`onboardingDone`).

### P1 release
- [ ] Release signing (keystore outside the repo, **[owner]** back it up offline), R8 keep rules, `release.yml` producing a signed APK on tag `v*`.
- [ ] Manual checklist from spec §16 on a real phone.
- [ ] **[owner]** Install `v1.0.0` and use it for a week before P2.

---

## P2 — New features

### Recipe structure (old ToDo)
- [ ] In-place edit mode on the detail screen (spec §6.8): groups (add/rename/delete/drag-reorder), items, extra fields, tags, image, notes; unsaved-changes guard.
- [ ] Extra fields ("Wijnpairing", page number, …) shown on the detail screen and captured by the photo prompt.
- [ ] "Aantal keer gekookt" + last cooked date on cards and detail; sorts "laatst gekookt" / "vaakst gekookt".

### Phone-native import
- [ ] Share target `ACTION_SEND text/plain` → URL import (extract the first URL from the shared text; tested with real Chrome and Instagram share text).
- [ ] Share target `ACTION_SEND(_MULTIPLE) image/*` → photo import.
- [ ] Clipboard URL prefill on the "Link plakken" sheet.
- [ ] Page text fallback with Claude when no JSON-LD is found (spec §8.5), with a cost estimate confirmation.

### Cooking
- [ ] Cook mode (spec §6.10): step pager, ingredients sheet, keep screen on, "Heb je dit gekookt?" on exit.
- [ ] `DurationDetector` (spec §11.3) + timer chips; unit tests with Dutch and English steps, ranges, and false-positive guards (°C, g).
- [ ] Timers: several named countdowns, ongoing chronometer notification, AlarmManager completion (exact if permitted, else inexact + warning), re-arm after boot, a `timers` notification channel.
- [ ] Boodschappenlijst (spec §6.14): add from recipe (scaled, all or selected), manual items, check off, clear, share.

### Seasonal
- [ ] "Met seizoensgroenten nu" filter + in-season chips on recipe cards (`SeasonalMatcher`, spec §11.4) with tests for plurals and " / " names.
- [ ] Vegetable detail → "Recepten met deze groente" and "Start een quest met {nl}".
- [ ] Monthly seasonal notification (spec §14): opt-in, posts once per month, lists vegetables that newly reached PEAK.

### Polish
- [ ] Home extras: open quest card, recently cooked, profile summary.
- [ ] Automatic backup to a chosen folder (WorkManager, keep 5, failure notification).
- [ ] Dark theme derived from the brand palette (spec §0 A7).
- [ ] Accessibility pass: TalkBack walkthrough of every screen, 200% font scale, contrast check of the status colours.
- [ ] Performance pass: cold start < 1.5 s; library search with 1,000 synthetic recipes (add FTS if needed).

---

## Content (any phase)

- [ ] Translate `nutrientsEn` to Dutch (`nutrients`) for the 58 vegetables before ever showing it; fill in the 8 missing.
- [ ] Add `sourceUrl` for the 7 vegetables without one (e.g. `butterhead-lettuce` → `https://velt.nu/groente/kropsla`).
- [ ] Add `curatedRecipes` (3–5 each) for more vegetables, starting with the peak vegetables of the current season.
- [ ] Add `relatedVegIds` to the 31 techniques without any.
- [ ] Review `strings.xml` copy for the app context (mobile wording, no "klik", "tik" instead).

---

## P3 — Later (research first)

- [ ] Markten in de buurt / boerenmarkten: find a reliable data source with schedules, then decide the scope (spec §18).
- [ ] Wildplukken / plukkaart: a separate dataset with safety rules (look-alikes, protected areas).
- [ ] Vervangers & combinaties explorer.
- [ ] Home-screen widget "Nu op hoogtepunt".
- [ ] Optional Play Store release (AAB, privacy policy page, data-safety form).
