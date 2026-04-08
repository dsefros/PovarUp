# PovarUp

## PR review safety for binary files

This repo uses text-only PR review tooling in some workflows.
To prevent failures like `Binary files are not supported`, binary payloads are excluded from review artifacts and `gradle-wrapper.jar` is not tracked in git.

- Generate review artifacts with:

```bash
scripts/dev/prepare_pr_diff.sh
```

- If `origin/main` is unavailable locally:

```bash
BASE_REF=main scripts/dev/prepare_pr_diff.sh
```

## Android wrapper policy

`android/gradle/wrapper/gradle-wrapper.jar` is intentionally untracked to keep PR diffs text-only.
Regenerate it locally when needed:

```bash
android/scripts/regenerate_wrapper.sh
```

## Backend verification quickstart

```bash
cd backend
npm ci
npm test
```

The backend test suite validates lifecycle invariants, auth-protected mutating routes, idempotent escrow release, and DTO response shape behavior.
