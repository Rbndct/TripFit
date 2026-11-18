# TripFit

Android trip-planning app (itinerary building, route optimization, calendar sync, budget tracking). ITISMOB (Mobile Development) term project, DLSU CCS-IT. Full scope, team, and backlog: `docs/TripFit-Proposal-and-Backlog.md`.

## Stack

- **Language:** Java (despite `.kts` Gradle build files — the app module itself has no Kotlin sources, see `app/src/main/java`)
- **UI:** Classic View system — AppCompat, Material Components, ConstraintLayout, RecyclerView (no Jetpack Compose)
- **Build:** Gradle (Kotlin DSL scripts), AGP, `compileSdk`/`targetSdk` 37, `minSdk` 24
- **Package:** `com.itismob.s01.group01.tripfit`
- **Testing:** JUnit4 (unit), Espresso + AndroidX Test (instrumented)

## Commands

```bash
./gradlew assembleDebug        # build debug APK
./gradlew test                 # unit tests (app/src/test)
./gradlew connectedAndroidTest # instrumented tests (app/src/androidTest) — needs emulator/device
./gradlew lint                 # Android lint
```

## Rules

Follows `~/.claude/rules/ecc/common/*` (coding style, testing, security, git workflow). Because source is Java, not Kotlin, `~/.claude/rules/ecc/java/*` applies for language-specific overrides (not the `kotlin/` set) — e.g. compile-check with `./gradlew compileDebugJavaWithJavac` instead of Kotlin-specific tooling.
