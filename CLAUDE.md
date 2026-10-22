# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## State of the repo
Mise is being rebuilt as a **native Android app (Kotlin + Jetpack Compose, local-only data)**. The old Next.js/Supabase
web app was removed; its last version is commit `cf3ec39` (`git show cf3ec39:web/<path>` to read old code).

- `spec.md`: the full specification. It is the source of truth for behaviour; when code and spec disagree, fix one deliberately.
- `todo.md`: the phased task list (P0 → P3). Tick boxes only when the acceptance check passes.
- `data/`: curated content, test fixtures and JSON Schemas. Read `data/README.md` before touching it.
- `tools/validate_data.py`: stdlib-only validator for everything in `data/`.
- `app/`: the Android app (single `:app` module, packages as in spec §4.2). `lint-checks/`: the custom lint rule
  `ComposeHardcodedText` (Android's `HardcodedText` only covers XML layouts). P0 is done except the owner/device items;
  feature screens are placeholders until P1.

## Commands
```bash
python3 tools/validate_data.py                   # validate data/ (also runs automatically after edits under data/)
python3 tools/validate_data.py --write-fixtures  # regenerate seasonality fixture after intended peak/shoulder edits
./gradlew lint ktlintCheck testDebugUnitTest :lint-checks:test assembleDebug   # the CI gate (spec §16–17)
./gradlew ktlintFormat                           # fix formatting (ktlint_official style)
./gradlew testDebugUnitTest --tests '*SeasonalityTest*'   # one test class
```
Versions are pinned in `gradle/libs.versions.toml`. compileSdk is 37 (current AndroidX/OkHttp require it), targetSdk 36.
Room schemas are exported to `app/schemas/` (commit them); unit tests use Robolectric for anything Android.

**Cloud sessions:** `.claude/hooks/session-start.sh` (SessionStart, remote only) installs the Android SDK into
`/opt/android-sdk` if missing (platform 37.0, build-tools 36.0.0), writes `local.properties`, adds a user-level Gradle
init script that routes Maven Central (and Robolectric's downloads) through Google's mirror because the shared egress IP
gets HTTP 429, and warms the Gradle cache. Nothing environment-specific is committed to the build files.

## Hard rules
- **Dutch UI only.** Every user-visible string goes in `data/res/values/strings.xml` (wired in as an Android res dir).
  Never hardcode UI text in Kotlin; lint `HardcodedText` and `ComposeHardcodedText` are errors (`@Preview` sample data
  is exempt). Code, identifiers and comments are in English.
- **Content scope:** Dutch locally grown vegetables only (velt.nu harvest windows). No imported year-round produce,
  fruit, berries or nuts. `nameEn` and `nutrientsEn` are never displayed.
- **API key:** the Claude key lives only in the Keystore-encrypted `SecretStore` (spec §9). Never log it, never put it in
  settings, backups, exports or test fixtures, and never commit a real key.
- **Claude API calls:** structured outputs (`output_config.format`). Never send `temperature`/`top_p`/`top_k`.
  Build enums from the content assets (spec §8).
- **Quests have no checkpoints or success criteria.** The owner rejected them (spec §0 D5); evaluation is scores + notes + photo.
- **Room:** never use destructive migrations. Every schema change gets a numbered migration and a migration test.
- **Data edits:** keep `data/schemas/*` and the Kotlin models in sync, and run the validator.
  The recipe parser must keep passing all cases in `data/fixtures/recipe-ingestion/golden.json`.

## Design system
"Mise — een kooksalmanak": editorial cookbook look. Cream `#F4EFE6`, ink `#1E1A14`, leaf green `#3F5E2A` (primary),
terracotta `#B95128` (accent). Instrument Serif (headlines, italic emphasis), Instrument Sans (body), JetBrains Mono
(meta labels). Full token table in spec §5. No dynamic colour.
- Contrast: text ≥ 4.5:1, meaningful graphics ≥ 3:1. `PeakBar`/calendar use the status colours, never a vegetable's `accent`;
  terracotta only for fills and large text.
- Italic emphasis is `<i>` inside one string resource, rendered with `styledStringResource()`; never split a sentence across keys.
- One vocabulary (spec §5): Op z'n best / Verkrijgbaar / Komt eraan, badges Laatste kans / Kort seizoen,
  difficulty Basis / Gemiddeld / Gevorderd, scores Mislukt · Matig · Oké · Goed · Top (stored 2/4/6/8/10).
