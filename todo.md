# Mise Android — overhaul todo

Actionable breakdown of [`spec.md`](spec.md) (v3; the UI/UX review of Appendix D is folded into the tasks below). Each task names the spec section that defines "done".
Work top to bottom inside a phase; phases ship in order. Tick a box only when its acceptance check passes.

Legend: **[owner]** = needs you (account, device or decision), everything else is code work.

---

## Before anything else: secure the old data **[owner]**

The web app keeps running from its last Vercel deployment until you turn it off, but this repo no longer contains its code.

- [ ] **Export Supabase data** with the SQL query in spec §13.3 (Supabase dashboard → SQL editor). Save the result as `mise-legacy-supabase.json` somewhere safe, outside the repo.
- [ ] **Export browser data**: open the deployed site in the browser you used, open the devtools console, and run the `localStorage` snippet in spec §13.3. Save the result as `mise-legacy-local.json`.
- [ ] Check both files open and look complete (count the recipes).
- [ ] Review the assumptions A1–A8 in spec §0 and change any you disagree with **before** P0. A5 (applicationId) is hard to change later.
- [x] Stop Vercel builds for this repo: `vercel.json` sets `git.deploymentEnabled: false`, so pushes and PRs no longer deploy or post a failing
      "Vercel" status. The existing production deployment keeps serving, so the browser export above still works.
- [ ] Once the app has restored the converted data (P1 › Backup and legacy): delete the Vercel project (or disconnect its Git repository and
      uninstall the Vercel GitHub App, which also removes the "Vercel Preview Comments" check), then delete `vercel.json`, and pause or
      delete the Supabase project.

---

## P0 — Foundation

### Project setup
- [x] Scaffold the Android project at the repo root: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper, `:app` module (spec §4.1, §4.3).
- [x] Set `minSdk 30`, `targetSdk`/`compileSdk 36`, applicationId `nl.potentplum.mise`, debug suffix `.debug` (spec §0 A4/A5, §17). compileSdk is 37: current AndroidX and OkHttp releases require it (spec §0 A4).
- [x] Wire `data/` into source sets: assets, res and test resources (spec §4.3). Acceptance: `strings.xml` from `data/res` resolves via `R.string.*`.
- [x] Add Hilt, Compose BOM, Material 3, Navigation Compose, Room + KSP, DataStore, kotlinx.serialization, OkHttp, Jsoup, Coil 3, WorkManager, Timber.
- [x] Lint config: `HardcodedText` = error; add ktlint (or detekt). Acceptance: a hardcoded `Text("Hallo")` fails `./gradlew lint`. Android's `HardcodedText` only covers XML, so `:lint-checks` adds `ComposeHardcodedText`.
- [x] Create the package skeleton from spec §4.2.
- [x] Update `.gitignore` for Android and signing material, and `.gitattributes` for binary artifacts (done in the overhaul PR).

### CI
- [x] `.github/workflows/ci.yml`: Python `tools/validate_data.py` → `./gradlew lint ktlintCheck testDebugUnitTest assembleDebug` → upload the debug APK artifact (spec §16, §17).
- [x] Add a Claude Code SessionStart hook that installs the Android SDK command-line tools if missing, accepts licences and warms the Gradle cache, so cloud sessions can build. Document it in `CLAUDE.md`.

### Design system (spec §5)
- [x] Bundle the fonts Instrument Serif (regular + italic), Instrument Sans (400/500/600) and JetBrains Mono (400/500) under `res/font`, plus their OFL licence texts.
- [x] `MiseTheme`: colour scheme from the token table, `MiseColors` for the status colours (shoulder = primary outline + soft fill), typography, shapes. Dynamic colour off.
      Acceptance: a unit test checks every text/indicator pair in the §5 table against its contrast floor (4.5:1 text, 3:1 graphics).
- [x] `styledStringResource()`: renders `<i>`/`<b>` from string resources as `AnnotatedString` (italic → Instrument Serif italic). Acceptance: `today_hero_title` renders "vandaag" in italic and TalkBack reads one sentence.
- [x] Shared components: `HeroHeader`, `Eyebrow`, `VegToken` (initials in ink or white by contrast, always beside the name), `StatusDot`, `SeasonBadge`, `PeakBar` (status colours, never the vegetable accent), `FilterPills`, `FacetChips`, `DifficultyBadge`, `EmptyState`, `ErrorState`.
- [x] Review components (spec §5, Appendix D): `SuggestionCard` (reason + time + Niet vandaag / Dit kook ik vanavond), `TonightCard`, `SeasonTally`, `ScoreScale` (5 steps → 2/4/6/8/10), `TimerChip` (idle / running / done), `RecipeActionBar`.
- [x] Compose `@Preview`s for every component (light theme).

### App shell (spec §6.1)
- [x] `MainActivity` (edge-to-edge) + `NavHost` with type-safe routes, a bottom bar (Vandaag · Seizoen · Recepten · Boodschappen · Dagboek; Boodschappen shows the unticked count), a back stack per tab and predictive back. Feature screens are placeholders until P1.
- [x] Top-bar overflow menu: Technieken, Instellingen, Over Mise.
- [x] `Clock`/`MonthProvider` abstraction (Europe/Amsterdam), injectable for tests. (`MiseClock`)

### Content layer (spec §7.1, §11.1)
- [x] Kotlin models + serializers for `vegetables`, `techniques` (nullable `discipline`), `facets`, `disciplines`, `archetypes` (typed `rule`), matching `data/schemas/*` exactly.
- [x] `ContentRepository`: loads once off the main thread, exposes id maps, fails loudly on an unknown `schemaVersion` in debug builds.
- [x] Unit test: every asset deserialises with `ignoreUnknownKeys = false`, and the counts match the validator (66 vegetables / 74 techniques / 4 facets / 3 disciplines / 7 badges).
- [x] `Seasonality.statusFor`. Acceptance: all 792 cells of `fixtures/seasonality/status.json` pass.
- [x] `SeasonPresentation`: Nu verkrijgbaar / Komt eraan, the Laatste kans and Kort seizoen badges, the ranked order (spec §11.1), with unit tests including September (43 vegetables at peak).

### Database (spec §7.2, §7.3)
- [x] Room entities, DAOs and the DB v1 for every table in §7.2 (structured `ingredient`, the single `recipe_tag`, `attempt` with `vegIds`/`peakVegIds`/`photoPath`), with schema export enabled (`app/schemas/` committed).
- [x] `MigrationTestHelper` set up (a trivial v1 test now; real migrations later).
- [x] DataStore `SettingsRepository` (spec §7.3), including `showQuestCard` and the day/session state (`tonight`, `todayDismissed`, `cookSession`) with their expiry rules; unit tests for expiry across midnight and the 24-hour cook-session limit.

### Security and Claude spike (spec §8.1, §9)
- [ ] **[owner]** **Spike:** call `GET /v1/models` and a minimal structured-output `messages` request with the Anthropic Java SDK on a real device with an R8 release build. Record the outcome in `spec.md` §8.1 and choose the SDK or the OkHttp client.
- [x] `KeystoreCipher` (AES-256-GCM, StrongBox with TEE fallback, `setUnlockedDeviceRequired`) + `SecretStore` (a separate DataStore file). `KeystoreCipherTest` (instrumented) still has to run on a device.
- [ ] `data_extraction_rules.xml` + `full_backup_content.xml` exclude `secrets.preferences_pb`. Acceptance: `adb shell bmgr backupnow` output does not contain the file.
      Rules written and unit-tested (`BackupExclusionTest`); **[owner]** the `bmgr` check needs a phone.
- [x] Handle invalidated or missing keys gracefully: the status becomes "opnieuw invoeren", with no crash (test on Robolectric or instrumented).
- [x] Scrub `sk-ant-…` from exception messages; Timber no-op in release; no HTTP logging in release.

---

## P1 — Core loop

### Content and strings
- [x] Create `data/assets/units.json` + `data/schemas/units.schema.json` (id, Dutch singular/plural, aliases, dimension, factor; spec §11.2), and extend `tools/validate_data.py` (unique ids and aliases, factors > 0).
- [x] Rename user-facing "Uitdaging" → "Quest(s)" in `strings.xml` (`challenge_*` keys → `quest_*`; history title "Eerdere quests"; result buttons).
- [x] Rename "Profiel" → "Dagboek" strings; remove the strings for dropped views (radar, ingredient cloud, highest-rated cuisines, US/metric toggle, nutrition labels).
- [x] Season vocabulary in `strings.xml` (spec §5): Op z'n best / Verkrijgbaar / Komt eraan, filter `filter_available`, badges "Laatste kans" and "Kort seizoen"; the "aflopend" strings removed (UI/UX review).
- [x] One difficulty scale: `level_basis` / `level_gemiddeld` / `level_gevorderd` / `level_auto` replace the `challenge_difficulty_*`, `detail_difficulty_*` and `techniques_level_*` keys (UI/UX review).
- [x] Headlines as single strings with `<i>` markup (`hero_title`, `calendar_title`, `home_hero_title`, `today_hero_title`); evaluation copy ("Zonder cijfers vastleggen", `score_steps`); `months_initial`; strings for Vanavond, Niet vandaag, cook session, Dagboek segments and first run (UI/UX review).
- [x] Prune web-only copy: `recipes_photo_dropzone`, `recipes_error_network` ("server" → "internet"), the "binnenkort" detail tags/notes, `controls_grid/list_aria_label`, `recipes_cooking_mode_unsupported`.
- [ ] Add every new string needed below to `data/res/values/strings.xml` as you go (Dutch only, snake_case keys, run the validator).

### Seizoen and vegetables
- [x] Seizoen Nu view (spec §6.3): filter pills with counts, badges, ranked order, diacritic-insensitive search, Nu/Kalender toggle.
- [x] Kalender (spec §6.4): year grid where all 12 months always fit (single-letter headers, ellipsized names, no horizontal scroll), family grouping with sticky headers and A–Z, current-month highlight, TalkBack row descriptions. Acceptance: no horizontal scroll on a 360 dp wide screen at 200% font scale.
- [ ] Groente-detail (spec §6.5): all sections, substitution chips that link to other vegetables, related techniques, curated links, source link, "Quest met {nl}".
      Done except "In jouw recepten" (needs `SeasonalMatcher` and the library) and importing a curated link.

### Technieken (spec §6.6)
- [x] Category list with subcategory grouping, level filter, search; cookware guides marked as reference.
- [x] Technique detail: body, tips, cookware, related vegetables, related techniques, parent and children, "Oefen in een quest".

### Recepten
- [x] `JsonLdRecipeParser` + `WprmExtractor` port (spec §10). Acceptance: all 104 cases in `fixtures/recipe-ingestion/golden.json` pass.
- [x] `RecipeFetcher` (OkHttp, https upgrade, 5 redirects, 5 MB cap, charset, timeouts, Dutch error mapping) with MockWebServer tests.
- [x] Mapping to Room: default section names → untitled groups, metric text wins, Schema.org extras → `sourceMeta`, image download/downscale.
- [x] `IngredientParser` (local, spec §11.2): table-driven tests for fractions, `1/2`, comma decimals, ranges, unit aliases, notes after a comma, pack sizes ("1 blik à 400 g tomaten" → note), unparseable lines.
- [x] `IngredientScaler` + Dutch formatting (spec §11.2): rounding rules per dimension, g→kg / ml→l switch, singular/plural labels, the text fallback for unparsed lines.
- [x] `RecipeRepository`: full graph read and write in one transaction, search, quick filters + "Meer filters", 3 sort orders, cooked count via `attempt`, derived prep-time bucket.
- [x] Library screen (spec §6.7): cards with cooked count and in-season chips, quick filter chips, the "Meer filters" sheet, empty states, the FAB sheet.
- [x] Recipe detail (spec §6.8): cook-first section order, sticky `RecipeActionBar` (Kookmodus / "Verder bij stap n" + Gekookt), favourite in the top bar, ⋮ with Dit kook ik vanavond / Bewerken / Delen / Verwijderen, structured ingredient rendering + check-off shared with the cook session, servings scaling, "Technieken in dit recept", extra fields, tags, notes, attempts with photos. No metadata display.
- [x] **In-place edit mode** (spec §6.8): groups (add/rename/delete/drag-reorder), ingredient lines (re-parsed on save; MANUAL parses kept), steps, extra fields, tags, image, notes; unsaved-changes guard.
- [x] Import by pasted link (spec §6.9) with a read-view preview (Bewaren / Aanpassen; unparsed lines marked; edit mode only when title, ingredients or steps are missing) and duplicate detection; manual entry ("Zelf invoeren") opens in edit mode.
- [x] Share target `ACTION_SEND text/plain` → URL import (extract the first URL from the shared text; tested with real Chrome and Instagram share text). Moved from P2: sharing is the main way in (spec §3, §6.9).
      Unit-tested with Chrome- and Instagram-style share text; **[owner]** check real shares on the phone.
- [x] Share target `ACTION_SEND(_MULTIPLE) image/*` → photo import (moved from P2).
      Shared URIs are copied into the import folder at once (UI-tested with file URIs); **[owner]** check a real gallery share.
- [x] `SeasonalMatcher` (spec §11.4) with tests for plurals, " / " names, and structured versus unparsed items; "Seizoen nu" filter.
- [x] Delete with undo; "Delen" as text.

### Claude (spec §8)
- [x] `ClaudeClient` (the implementation chosen in P0), retry policy, error mapping, refusal / `max_tokens` handling, refusal fallback header for Sonnet/Opus, Kwaliteit → model mapping.
      `OkHttpClaudeClient` ships (spec §8.1 status); the SDK spike stays an **[owner]** item in P0.
- [x] `Pricing` + `claude_usage` recording; unit tests for cost maths.
- [x] Runtime schema builders (quest, photo/page recipe, enrichment) from the content assets, with the shared structured ingredient item and the merged tags. Post-parse validation (spec §8.6). Unit tests with recorded responses.
- [x] Rewrite the prompts from `data/reference/prompts/` as Kotlin templates without JSON-format instructions; add the quest focus discipline and the "niet herhalen" list (spec §8.3). **No checkpoints.**
- [x] **Enrichment** worker (Haiku, spec §6.9/§8.4): structured ingredients + tags in one call per recipe, `enrichState`, never overwrites MANUAL parses, failure notification on the `background` channel.
      `EnrichmentWorker` (unique expedited work per recipe, needs network, 3 retries for network/429/5xx); queued after a URL import
      or a save with new or changed lines when a key is stored. A reply for lines edited in the meantime is discarded.
- [x] Photo import (spec §6.9): photo picker + camera, 1–5 photos, reorder/remove, downscale to ≤ 1568 px, call, preview in edit mode, keep the first photo as the image.
      Previews in read view like a link import (edit mode only when parts are missing, spec §6.9); camera via `TakePicture` +
      `FileProvider` (no camera permission). UI-tested with recorded replies; **[owner]** try the camera on the phone.

### Quests (spec §6.11, §11.6)
- [x] `QuestDefaults` (pure): vegetable choice (peak, not cooked in 60 days, with fallbacks), focus discipline, adaptive difficulty, last-10 history. Unit tests with fixed dates.
      Today's one-tap quest uses one candidate (stable per day, the next one for "Andere quest") as Seizoensgroente, so the card can
      show it while generating; "Verras me" in Verfijn sends the whole candidate list.
- [x] Quest van vandaag: one-tap generation; the "what was chosen" line, a progress indicator and Annuleren show while generating (cancel records nothing); one daily quest (regenerate replaces it; the old one stays in history).
- [x] Verfijn form (collapsed): ingredient modes, soort quest, difficulty (Automatisch · Basis · Gemiddeld · Gevorderd), technique (skills only), cuisine, time, simple, quantity + unit.
      Its own screen (from the card, Dagboek › Quests, groente- and techniekdetail), so there is nothing to collapse.
- [x] Generation → `quest` + hidden recipe in one transaction; result screen with technique chips and the actions Start koken / Quest voltooid / Andere quest (+ Bewaar recept in ⋮).
      Start koken and Quest voltooid open the cook-mode and evaluation routes, which are still placeholders (Cooking section).
- [x] Completion from any entry point: an attempt on the quest's recipe sets `questId` and COMPLETED (Quest voltooid, Klaar met koken, cook-mode exit prompt). Unit test for each path.
  _Done: `AttemptRepository.log` looks the quest up by recipe, so every path that logs an attempt completes it. Tested via Quest voltooid (`QuestFlowsTest`) and Klaar met koken (`CookFlowsTest`); the exit prompt opens the same sheet._
- [x] Quest history as **Dagboek › Quests** (spec §6.13) with filters (open/completed, type, difficulty), reopen, retry with the same parameters, delete, Nieuwe quest.
      Delete offers Ongedaan maken. Tijdlijn and Inzichten are still placeholders (Dagboek section).
- [ ] Offline / no-key state on the Vandaag card: explanation, Sleutel toevoegen, Verbergen (`showQuestCard`, restored when a key is added or in Instellingen).
      Card states done; restoring `showQuestCard` comes with the key screen and Instellingen.

### Vandaag (spec §6.2, §11.5)
- [x] `TodaySuggester` (pure): season score, recency, favourite bonus, exclusions (incl. `todayDismissed` and the Vanavond pin), the ≤ 30 min filter, tie-breaks, reason lines, stable per day. Unit tests with fixed dates and a seeded library.
  _Done: `domain/TodaySuggester.kt` (`rank` + `pick`) and `CuratedFallback`; "≤ 30 days → × 0.5" halves season + favourite. `TodaySuggesterTest`._
- [x] Vandaag screen (spec §6.2): hero, Vanavond card (Kookmodus / Op boodschappenlijst / Toch niet), 3 suggestions with total time, Niet vandaag (refills the slot) and Dit kook ik vanavond, the ≤ 30 min chip, curated-link fallback for small libraries, the import-led empty state for an empty library, the Quest van vandaag card, the "Nu op z'n best" shortlist (8). No seasonal ring here.
  _Done: `TodayViewModel` + `TodayScreen`; Kookmodus opens the `CookRoute` placeholder until Cooking lands. "Alle quests" opens Dagboek › Quests. `TodayFlowsTest`._
- [ ] Performance: ranking for 1,000 synthetic recipes off the main thread, cached per day, Vandaag visible in < 1.5 s cold start.
  _Partly: ranking runs on `Dispatchers.Default`, only when the library or the day changes (1,000 recipes rank in < 250 ms in a unit test). Cold start < 1.5 s still needs a device._

### Cooking (spec §6.10, §6.12)
- [x] Cook mode (spec §6.10): step pager with progress bar, ingredients sheet (portrait) / two panes (landscape) with scaled amounts, keep screen on, technique info buttons, "Klaar met koken" on the last page, "Heb je dit gekookt?" on exit **only after the last step was reached**.
  _Done: `feature/cook/CookScreen` + `CookViewModel`; technique buttons come from `TechniqueMentions` (imperative stems). The bottom bar is hidden in cook mode. `CookFlowsTest`._
- [x] Cook session (spec §7.3): persisted step, servings, ingredient ticks (shared with recipe detail) and timers; "Verder bij stap n" on the recipe; cleared on an attempt or after 24 hours. Acceptance: leave at step 3, kill the process, reopen → step 3.
  _Done: leaving at step 3 and reopening resumes at step 3 through a fresh ViewModel reading the DataStore session (`CookFlowsTest`); a real process kill is still worth a check on the phone._
- [x] `DurationDetector` (spec §11.3) + timer chips; unit tests with Dutch and English steps, ranges, and false-positive guards (°C, g).
  _Done: `domain/DurationDetector.kt`, `DurationDetectorTest`._
- [x] `TimerChip` strip in cook mode: remaining time computed from the recorded start ("Stap 4 · nog ~9 min" → "klaar"); tapping a running chip opens `AlarmClock.ACTION_SHOW_TIMERS`.
  _Done: chips count down every 15 s from the recorded start._
- [ ] `ClockTimerLauncher`: `ACTION_SET_TIMER` with length, message and skip-UI; `SET_ALARM` permission; a `<queries>` entry; a snackbar; hide chips + a one-time hint when unsupported. **[owner]** Test on your phone's Clock app.
  _Code done (`platform/ClockTimerLauncher.kt`, manifest permission and `<queries>`, snackbar, hidden chips + one-time hint, faked in tests). Open until the owner checks it with the phone's Clock app._
- [x] Evaluation sheet (spec §6.12): two `ScoreScale`s (Mislukt · Matig · Oké · Goed · Top → 2/4/6/8/10; legacy values shown as the nearest step), notes, **photo**, date → `attempt` with snapshots of techniques, cuisines, `vegIds` and `peakVegIds`; "Zonder cijfers vastleggen" logs without scores; dismissing logs nothing; clears the cook session and Vanavond pin.
  _Done: `EvaluationSheet` hosted by recipe detail, cook mode and the quest screen (the `EvaluationRoute` placeholder is gone); photos are staged in the import folder and moved to `files/journal/`._

### Dagboek (spec §6.13, §12)
- [ ] `JournalAggregator` port (3 disciplines). Acceptance: the applicable cases from `git show e76778c:web/lib/profileAggregation.test.ts` pass, plus the seasonal metric (season boundaries, Dec/Jan wrap) and "nog te proberen" (PEAK this month, not cooked this season).
- [ ] `BadgeEvaluator` for all three rule types + progress text; tests at and around each threshold.
- [ ] Dagboek screen (spec §6.13): segments Tijdlijn · Quests · Inzichten. Tijdlijn with photos, filters and entry editing; Inzichten with header stats, `SeasonTally` (count without denominator, cooked tokens, 3 "nog te proberen" in §11.1 order), discipline bars, top techniques, neglected, top cuisines, badges.

### Settings and key (spec §6.15, §9)
- [ ] Key entry screen: password field without IME learning, `FLAG_SECURE`, live check via `GET /v1/models`, masked display, offer to clear the clipboard, delete key.
- [ ] Kwaliteit setting (Snel / Gebalanceerd / Beste), usage this month, the Anthropic Console spend-limit tip, "Quest-kaart tonen op Vandaag".
- [ ] "Ingrediënten verrijken" for all unparsed recipes, with an estimated cost confirmation.
- [ ] Over screen: version, data sources, OSS licences (including fonts), privacy statement and the threat-model note.

### Backup and legacy (spec §13)
- [ ] `data/schemas/backup.schema.json` for the backup format (spec §13.1), shared by the app and the converter.
- [ ] Export to zip (`backup.json` + images + journal photos) via SAF/share. The key is never included (unit test asserts this).
- [ ] Restore: validate, summary, Samenvoegen / Vervangen, in one transaction, then offer enrichment. Round-trip test: export → wipe → restore gives an identical DB.
- [ ] `tools/legacy_to_backup.py` (stdlib, spec §13.3 + Appendix A): converts both legacy exports into a backup zip and prints a report. Include synthetic legacy fixtures covering every branch (JSON-in-`instructions`, column fallback, photo prefix, localStorage merge by `remoteId`, legacy difficulty values, unresolved `source_id`, `vegIds`/`peakVegIds` computation). Its output must validate against `backup.schema.json`.
- [ ] **[owner]** Convert your real exports, restore them on your phone, run enrichment, and spot-check 5 recipes, 3 quests and the Dagboek numbers.

### Boodschappen (spec §6.14, §11.7; a bottom-bar tab, so P1)
- [ ] `ShoppingAggregator` (pure): group by name, sum within a dimension via base units, best display unit, never merge across dimensions or unnamed items. Table-driven tests.
- [ ] Boodschappen tab: add from recipe and the Vanavond card (scaled, all or selected), combined lines with source-recipe chips, manual items, check off (ticked items sink), clear with undo, share as text, empty state.

### First run (spec §6.16)
- [ ] Welcome → optional key → optional "Back-up terugzetten" → "Je eerste recept" (Link plakken · Foto's maken · Later; skipped when a restore brought recipes) → done (`onboardingDone`).

### P1 release
- [ ] Release signing (keystore outside the repo, **[owner]** back it up offline), R8 keep rules, `release.yml` producing a signed APK on tag `v*`.
  _Partly: signing from env/Actions secrets and `release.yml` (signed APK + checksum on a `v*` tag, verified locally with a throwaway key; owner steps in README). Open: the owner's keystore and secrets, a first tagged release, and checking the R8 build on the phone._
- [ ] Manual checklist from spec §16 on a real phone.
- [ ] **[owner]** Install `v1.0.0` and use it for a week before P2.

---

## P2 — Depth and polish

### Phone-native import
- [ ] Clipboard URL prefill on the "Link plakken" sheet.
- [ ] Page text fallback with Claude when no JSON-LD is found (spec §8.5), with a cost estimate confirmation.

### Seasonal and links
- [ ] Monthly seasonal notification (spec §14): opt-in, posts once per month, lists vegetables that newly reached PEAK.
- [ ] Technique aliases: add optional `aliases` to `techniques.json` + schema + validator, then make technique names in step text tappable (spec §6.8).
- [ ] "Zo vaak geoefend" on technique detail; "Jouw dagboek" line on vegetable detail.

### Polish
- [ ] Automatic backup to a chosen folder (WorkManager, keep 5, failure notification).
- [ ] Dark theme derived from the brand palette (spec §0 A7).
- [ ] Accessibility pass: TalkBack walkthrough of every screen, 200% font scale, contrast check of the status colours.
- [ ] Performance pass: library search with 1,000 synthetic recipes (add FTS if needed).

---

## Content (any phase)

- [ ] **Vegetable techniques:** add techniques for the vegetable side of the app, e.g. snel inleggen (quick pickle), knolgewassen roosteren, bladgroenten stoven, groenten grillen, puree en crème, rauwkost snijden (schaven/linten), groenten confijten. Each needs `discipline`, `relatedVegIds` and Dutch body + tips. Check against the existing `groenten` (5) and `bewaren` entries first: `fermenteren`, `inmaken`, `groenten-roasten` and `groenten-smoren` already exist and may only need extending.
- [ ] Translate `nutrientsEn` to Dutch (`nutrients`) for the 58 vegetables before ever showing it; fill in the 8 missing.
- [ ] Add `sourceUrl` for the 7 vegetables without one (e.g. `butterhead-lettuce` → `https://velt.nu/groente/kropsla`).
- [ ] Add `curatedRecipes` (3–5 each) for more vegetables, starting with the short-season and peak vegetables of the current season (they feed the Vandaag fallback).
- [ ] Add `relatedVegIds` to the 31 techniques without any.
- [ ] Review `strings.xml` copy for the app context (mobile wording: "tik" instead of "klik").
- [x] Vegetable fills reach 4.5:1 with ink or white initials (radish, wild garlic and watercress lightened); `tools/validate_data.py` enforces it (UI/UX review).

---

## P3 — Later (research first)

- [ ] In-app kitchen timers, only if the Clock-app handoff proves annoying (spec §18).
- [ ] Biometric lock on Claude use (spec §9, §18).
- [ ] Markten in de buurt / boerenmarkten: find a reliable data source with schedules, then decide the scope.
- [ ] Wildplukken / plukkaart: a separate dataset with safety rules (look-alikes, protected areas).
- [ ] Vervangers & combinaties explorer.
- [ ] Home-screen widget "Wat kook ik vandaag?".
- [ ] Optional Play Store release (AAB, privacy policy page, data-safety form).
