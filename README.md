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

## Current MVP capabilities (P0)

- **Auth hardening**
  - Passwords are stored as salted `scrypt` hashes (no plaintext password storage).
  - `POST /api/auth/login` validates hashed passwords and returns `expiresAt`.
  - Sessions are expiry-checked on every authenticated route.
  - `POST /api/auth/logout` invalidates bearer sessions.
- **Worker flow (Android + API)**
  - List published shifts, inspect shift details, apply, view applications/assignments, then check in/out.
  - Worker-side accept action is removed from Android; assignments are treated as accepted when offered by business.
  - View active shift, completed history, and payout statuses.
  - UI includes loading/empty/error/retry states and disables invalid actions.
- **Business flow**
  - Android business surface supports create shifts, list owned shifts, inspect shift applications, issue offers, and release payout creation.
- **Operator/Admin flow**
  - Android admin/operator section supports viewing assignments/payouts/problem-case counts and progressing payout status.
  - API endpoints: `GET /api/admin/assignments`, `GET /api/admin/payouts`, `POST /api/admin/payouts/{payoutId}/status`, `GET /api/admin/problem-cases`, `GET /api/violation-flags`.
- **Communication honesty**
  - Contact reveal relay endpoint is removed (`410`) and chat is the single supported MVP communication path.

## Payout lifecycle

- `created`: payout record exists after business release.
- `pending`: operator has started manual payout processing.
- `paid`: payout completed.
- `failed`: payout failed and requires operator retry/handling.

## Lifecycle model

Backend status is canonical. Android reads `productStatus` directly and does pure UI mapping only.

- Shift lifecycle: `draft -> published -> closed -> cancelled`
- Application lifecycle: `applied -> accepted -> rejected -> withdrawn` (with guarded branch transitions)
- Assignment lifecycle: `assigned -> in_progress -> completed -> cancelled`
  - Cancellation is allowed only from `assigned` or `in_progress`.
- Payout lifecycle: `created -> pending -> paid -> failed`

Invalid transitions are rejected with `409 invalid_transition` and transition endpoints are explicit (publish/close/cancel shift, reject/withdraw application, cancel assignment, payout status progression). Cancelled assignments cannot create payouts.

## Run and test

```bash
cd backend
npm ci
npm test
```

Android unit tests:

```bash
cd android
./gradlew test
```

## Remaining limitations

- No external payment processor integration yet (manual payout lifecycle only).
- Operator tooling is API-first (no dedicated admin web UI yet).
- Session expiry is fixed-duration TTL; no refresh token flow in MVP.
