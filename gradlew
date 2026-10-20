#!/usr/bin/env sh
set -eu
GRADLE_VERSION=8.13
BASE="${HOME}/.gradle/gymmanagement-bootstrap"
DIST="$BASE/gradle-$GRADLE_VERSION"
if [ -x "$DIST/bin/gradle" ]; then
  exec "$DIST/bin/gradle" "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
mkdir -p "$BASE"
ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"
if [ ! -f "$ZIP" ]; then
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ZIP"
  else
    echo "Install Gradle or curl/wget first." >&2
    exit 1
  fi
fi
unzip -oq "$ZIP" -d "$BASE"
exec "$DIST/bin/gradle" "$@"
