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

The backend now runs with SQL persistence by default (`POVARUP_PERSISTENCE=sql`) and stores data at `backend/data/povarup.sqlite` unless `POVARUP_DB_PATH` is set.

Run backend in SQL mode explicitly:

```bash
cd backend
POVARUP_PERSISTENCE=sql POVARUP_DB_PATH=./data/povarup.sqlite npm run dev
```

For local memory-only fallback (non-persistent):

```bash
cd backend
POVARUP_PERSISTENCE=memory npm run dev
```

The backend test suite validates lifecycle invariants, auth-protected mutating routes, idempotent escrow release, DTO response shape behavior, and SQL-backed persistence across server restarts.

Schema ownership note: runtime application tables are migration-owned (`backend/migrations/*.sql`). The runtime bootstrap seeds data only and does not define domain tables.

SQLite runtime stance: this project currently uses Node's built-in `node:sqlite` API (experimental in Node 22) for local and CI persistence verification. Treat this as acceptable for current development/test usage; if production hardening requires non-experimental DB APIs, switch to a stable driver before production rollout.


## MVP auth + flows demo accounts

Use login endpoint `POST /api/auth/login` with seeded credentials:

- worker.demo / workerpass
- business.demo / businesspass
- admin.demo / adminpass

Optional onboarding endpoint: `POST /api/auth/onboard` with invite code `WORKER-DEMO-2026` or `BUSINESS-DEMO-2026`.
