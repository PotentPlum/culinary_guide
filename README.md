# Mise — een kooksalmanak

A personal Dutch cooking almanac for Android, built around one daily question: **"Wat kook ik vandaag?"** It ranks your
own recipes (imported from websites, cookbook photos or typed in) by which Dutch-grown vegetables are at their best right
now, offers a one-tap AI "Quest" (a focused cooking exercise generated with Claude), and keeps a cooking journal that
shapes the next suggestions. A technique library supports every step.

All data stays on the phone. The Claude API key you enter is stored encrypted with the Android Keystore.

**Status:** being rebuilt as a native Kotlin/Jetpack Compose app. The P0 foundation and the P1 core loop (Vandaag,
Seizoen, Recepten with import, quests, cook mode, Boodschappen, Dagboek, Instellingen with the key and backups, first run)
are in place; see [`todo.md`](todo.md) for what is left. The previous Next.js + Supabase web app was retired (last
version: commit `e76778c`).

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

**Test release without a keystore:** run the workflow by hand with **debug_key** ticked. It needs no secrets, signs with
the runner's throwaway debug key and publishes a pre-release titled "(debug key)". Locally the same build is
`MISE_SIGN_WITH_DEBUG_KEY=true ./gradlew assembleRelease`. Each debug key is different, so installing a later build (or
the first one signed with the real key) means uninstalling first, which deletes the app's data: make a backup in
Instellingen before you do.

To release, either run the workflow by hand (Actions → Release → Run workflow, pick the branch, enter `v0.1.0`), which
tags that branch's commit itself, or push a tag: `git tag v0.1.0 && git push origin v0.1.0`. Don't create the release in
the GitHub UI first, or the workflow's own `gh release create` will find it already there.
