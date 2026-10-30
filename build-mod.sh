#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
if [[ ! -x "$JAVA_BIN" ]]; then JAVA_BIN="$(command -v java || true)"; fi
GRADLE_BIN="$(command -v gradle || true)"
if [[ -z "$JAVA_BIN" || ! -x "$JAVA_BIN" || -z "$GRADLE_BIN" ]]; then
  echo "Need JDK 25 and a compatible Gradle installation." >&2
  exit 2
fi
VERSION="$($JAVA_BIN -version 2>&1 | head -n 1)"
if ! grep -Eq 'version "25\.' <<<"$VERSION"; then
  echo "Minecraft 26.1.2 requires Java 25. Detected: $VERSION" >&2
  exit 2
fi
cd "$ROOT"
"$GRADLE_BIN" --no-daemon clean build
ls -lh "$ROOT/build/libs/aegis-client-2.0.0.jar"
