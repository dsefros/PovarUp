# Android review-safe wrapper policy

To keep PRs compatible with text-only review tooling, this repository does **not** track `gradle-wrapper.jar`.

## Backend URL configuration
Android reads backend URL from `BuildConfig.BACKEND_BASE_URL`.

Default value:
- `http://10.0.2.2:4000/api`

Override for local runs/tests:

```bash
cd android
./gradlew testDebugUnitTest -PbackendBaseUrl=http://localhost:4000/api
```

## Minimal session-aware flow
The Android client now uses backend sessions via `POST /api/auth/session` with:

```json
{"userId":"u_worker_1","role":"worker"}
```

For local seeded backend testing, practical defaults are:
- worker: `u_worker_1`
- business: `u_biz_1`

The app stores `{token,userId,role}` in `SharedPreferences` (`povarup_session`) and automatically sends `Authorization: Bearer <token>` for API-backed shifts calls.

## Android SDK setup
Gradle Android tasks require an installed Android SDK and `local.properties` with `sdk.dir=...`.

## Local wrapper regeneration
If you need wrapper binaries locally:

```bash
android/scripts/regenerate_wrapper.sh
```

This regenerates wrapper files using local Gradle and writes the JAR under `android/gradle/wrapper/` (ignored by git).

## Build
After regenerating wrapper locally (and with SDK configured):

```bash
cd android
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
