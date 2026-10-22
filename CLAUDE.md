# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## State of the repo
Mise is being rebuilt as a **native Android app (Kotlin + Jetpack Compose, local-only data)**. The old Next.js/Supabase
web app was removed; its last version is commit `cf3ec39` (`git show cf3ec39:web/<path>` to read old code).

- `spec.md`: the full specification. It is the source of truth for behaviour; when code and spec disagree, fix one deliberately.
- `todo.md`: the phased task list (P0 → P3). Tick boxes only when the acceptance check passes.
- `data/`: curated content, test fixtures and JSON Schemas. Read `data/README.md` before touching it.
- `tools/validate_data.py`: stdlib-only validator for everything in `data/`.

The Android project (`app/`, Gradle files) does not exist yet. Scaffolding it is P0 in `todo.md`.

## Commands
```bash
python3 tools/validate_data.py                   # validate data/ (also runs automatically after edits under data/)
python3 tools/validate_data.py --write-fixtures  # regenerate seasonality fixture after intended peak/shoulder edits
```
Once scaffolded: `./gradlew lint ktlintCheck testDebugUnitTest assembleDebug` (see spec §16–17).

## Hard rules
- **Dutch UI only.** Every user-visible string goes in `data/res/values/strings.xml` (wired in as an Android res dir).
  Never hardcode UI text in Kotlin; lint `HardcodedText` is an error. Code, identifiers and comments are in English.
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
