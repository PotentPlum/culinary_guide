#!/bin/bash
# Claude Code cloud sessions: make `./gradlew lint ktlintCheck testDebugUnitTest assembleDebug` work.
# Installs the Android SDK command-line tools if missing, accepts licences, routes Maven Central through Google's
# mirror (the shared egress IP gets HTTP 429 from repo.maven.apache.org) and warms the Gradle cache. Idempotent.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

SDK="${ANDROID_HOME:-/opt/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-16111833_latest.zip"
PACKAGES=("platform-tools" "platforms;android-37.0" "build-tools;36.0.0")
PROJECT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

log() { echo "[session-start] $*" >&2; }

# ── Android SDK ──────────────────────────────────────────────────────────────
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  log "installing Android command-line tools into $SDK"
  tmp="$(mktemp -d)"
  curl -sSfL -o "$tmp/clt.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
  unzip -q "$tmp/clt.zip" -d "$tmp"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
missing=()
for pkg in "${PACKAGES[@]}"; do
  [ -d "$SDK/${pkg//;//}" ] || missing+=("$pkg")
done
if [ ${#missing[@]} -gt 0 ]; then
  log "installing ${missing[*]}"
  "$SDKMANAGER" --sdk_root="$SDK" "${missing[@]}" >/dev/null
fi

echo "sdk.dir=$SDK" > "$PROJECT/local.properties"
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export ANDROID_HOME=\"$SDK\"" >> "$CLAUDE_ENV_FILE"
fi

# ── Maven Central mirror (user-level, never committed) ───────────────────────
mkdir -p "$HOME/.gradle/init.d"
cat > "$HOME/.gradle/init.d/maven-central-mirror.gradle.kts" <<'KTS'
// Cloud sessions: Maven Central rate-limits (HTTP 429) the shared egress IP; use Google's mirror first.
val mirror = "https://maven-central.storage-download.googleapis.com/maven2/"
beforeSettings {
    pluginManagement.repositories { maven(mirror) }
    dependencyResolutionManagement.repositories { maven(mirror) }
}
// Robolectric downloads its android-all jars itself.
allprojects {
    tasks.withType<Test>().configureEach {
        systemProperty("robolectric.dependency.repo.url", mirror)
    }
}
KTS

# ── Warm the Gradle cache (dependencies, compiled plugins) ───────────────────
cd "$PROJECT"
log "warming the Gradle cache"
if ! ANDROID_HOME="$SDK" ./gradlew --no-daemon -q assembleDebug compileDebugUnitTestKotlin :lint-checks:jar >/dev/null 2>&1; then
  log "warm-up build failed; run ./gradlew assembleDebug to see why"
fi
log "done"
