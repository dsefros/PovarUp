#!/usr/bin/env bash
set -euo pipefail

BASE_REF="${BASE_REF:-origin/main}"
OUT_DIR="${OUT_DIR:-artifacts}"

if [[ $# -gt 0 ]]; then
  BASE_REF="$1"
fi

if ! git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
  echo "ERROR: Base ref '$BASE_REF' not found." >&2
  echo "Hint: fetch remotes or pass a valid ref, e.g. BASE_REF=main scripts/dev/prepare_pr_diff.sh" >&2
  exit 2
fi

mkdir -p "$OUT_DIR"

EXCLUDES=(
  ':(exclude)*.jar'
  ':(exclude)*.png'
  ':(exclude)*.jpg'
  ':(exclude)*.jpeg'
  ':(exclude)*.gif'
  ':(exclude)*.webp'
  ':(exclude)*.pdf'
  ':(exclude)*.keystore'
  ':(exclude)*.jks'
  ':(exclude)*.p12'
  ':(exclude)*.ico'
  ':(exclude)*.so'
  ':(exclude)*.a'
  ':(exclude)*.o'
  ':(exclude)*.class'
  ':(exclude)*.dex'
  ':(exclude)*.apk'
  ':(exclude)*.aab'
  ':(exclude)*.bin'
  ':(exclude)*.zip'
)

range="$BASE_REF...HEAD"

git diff --name-only "$range" -- . "${EXCLUDES[@]}" > "$OUT_DIR/pr.files.txt"

if [[ ! -s "$OUT_DIR/pr.files.txt" ]]; then
  echo "ERROR: No text-reviewable changes remain after exclusions." >&2
  exit 3
fi

git diff --stat "$range" -- . "${EXCLUDES[@]}" > "$OUT_DIR/pr.stat.txt"
git diff "$range" -- . "${EXCLUDES[@]}" > "$OUT_DIR/pr.diff"

if grep -E '^(Binary files|GIT binary patch)' "$OUT_DIR/pr.diff" >/dev/null; then
  echo "ERROR: Binary sections still detected in $OUT_DIR/pr.diff" >&2
  exit 4
fi

{
  echo "base_ref=$BASE_REF"
  echo "head_ref=$(git rev-parse --abbrev-ref HEAD)"
  echo "head_sha=$(git rev-parse HEAD)"
  echo "generated_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "file_count=$(wc -l < "$OUT_DIR/pr.files.txt" | tr -d ' ')"
} > "$OUT_DIR/pr.meta.txt"

echo "Wrote:"
echo "  $OUT_DIR/pr.files.txt"
echo "  $OUT_DIR/pr.stat.txt"
echo "  $OUT_DIR/pr.diff"
echo "  $OUT_DIR/pr.meta.txt"
