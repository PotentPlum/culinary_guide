# Mise — een kooksalmanak

A personal Dutch cooking almanac for Android, built around one daily question: **"Wat kook ik vandaag?"** It ranks your
own recipes (imported from websites, cookbook photos or typed in) by which Dutch-grown vegetables are at their best right
now, offers a one-tap AI "Quest" (a focused cooking exercise generated with Claude), and keeps a cooking journal that
shapes the next suggestions. A technique library supports every step.

All data stays on the phone. The Claude API key you enter is stored encrypted with the Android Keystore.

**Status:** being rebuilt as a native Kotlin/Jetpack Compose app. The P0 foundation (project, design system, app shell,
content layer, database, settings, key storage, CI) is in place; the features follow in P1. The previous Next.js +
Supabase web app was retired (last version: commit `e76778c`).

| Document | Contents |
|---|---|
| [`spec.md`](spec.md) | Full specification |
| [`todo.md`](todo.md) | Phased task list |
| [`data/README.md`](data/README.md) | Curated content (66 vegetables, 74 techniques, badges, Dutch UI strings), test fixtures, schemas |

```bash
python3 tools/validate_data.py                                         # validate all content and fixtures
./gradlew lint ktlintCheck testDebugUnitTest :lint-checks:test assembleDebug   # build and test (JDK 17+, Android SDK)
```

Seasonal data reference: [velt.nu](https://www.velt.nu).

## Releasing

Pushing a tag `v*` runs [`release.yml`](.github/workflows/release.yml): the CI checks, a signed and shrunk release APK,
and a GitHub Release with `mise-<tag>.apk` and its SHA-256 checksum. A tag with a hyphen (`v0.1.0-alpha1`) becomes a
pre-release. `versionName` is the tag without the `v`; `versionCode` is `major·10000 + minor·100 + patch`.

One-time setup, on your own machine (the keystore never goes in the repo, spec §17):

```bash
keytool -genkeypair -v -keystore mise-release.jks -alias mise -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 mise-release.jks > mise-release.jks.b64      # macOS: base64 -i mise-release.jks -o mise-release.jks.b64
```

Back up `mise-release.jks` and its passwords offline: without them, installed copies can never be updated. Then add four
repository secrets (Settings → Secrets and variables → Actions): `MISE_KEYSTORE_BASE64` (the `.b64` file's contents),
`MISE_KEYSTORE_PASSWORD`, `MISE_KEY_ALIAS` (`mise`) and `MISE_KEY_PASSWORD`, and delete the `.b64` file.

To release: `git tag v0.1.0 && git push origin v0.1.0`. Push the tag; don't create the release in the GitHub UI first,
or the workflow's own `gh release create` will find it already there.
