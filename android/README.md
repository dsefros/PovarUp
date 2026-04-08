# Android review-safe wrapper policy

To keep PRs compatible with text-only review tooling, this repository does **not** track `gradle-wrapper.jar`.

## Local wrapper regeneration
If you need wrapper binaries locally:

```bash
android/scripts/regenerate_wrapper.sh
```

This regenerates wrapper files using local Gradle and writes the JAR under `android/gradle/wrapper/` (ignored by git).

## Build
After regenerating wrapper locally (or using local Gradle directly):

```bash
cd android
./gradlew tasks
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
