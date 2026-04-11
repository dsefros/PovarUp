#!/usr/bin/env bash
set -euo pipefail

# Recreate Gradle wrapper jar/scripts locally when needed.
# The repository intentionally does not track gradle-wrapper.jar to keep PRs text-review safe.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_BUILD="$ROOT_DIR/.wrapper-bootstrap.gradle"

cat > "$TMP_BUILD" <<'GRADLE'
// Empty bootstrap build file for running wrapper task only.
GRADLE

gradle -p "$ROOT_DIR" -b "$TMP_BUILD" wrapper --gradle-version 8.14.3 --distribution-type bin --no-validate-url
rm -f "$TMP_BUILD"

echo "Wrapper regenerated under $ROOT_DIR/gradle/wrapper/."
