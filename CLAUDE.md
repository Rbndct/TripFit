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

## AI Guidelines & Shared Plans

- **ChatGPT Guide & Prompts:** See [CHATGPT.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/CHATGPT.md) for step-by-step copy-paste prompts for ChatGPT.
- **Universal Agent Instructions:** See [AGENTS.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/AGENTS.md).
- **Combine Prototypes Roadmap:** See [docs/Combine-Prototypes-Plan.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/docs/Combine-Prototypes-Plan.md) and [docs/Prototype-Merge-Map.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/docs/Prototype-Merge-Map.md).

## Rules

- **Language:** Strictly Java 11 for all `app/src/main/java` sources. Compile-check with `./gradlew compileDebugJavaWithJavac`.
- **UI & Layout:** Classic Views, Material Components 3, `ScreenActivity` base shell. No Jetpack Compose.
- **Design Guidelines:** Follow `DESIGN.md` (16dp outlined cards, 48dp touch targets, strings in `res/values/strings.xml`, colors in `values` and `values-night`).
- **Git Commits:** Conventional commit messages (`feat:`, `fix:`, `refactor:`, `docs:`), no AI attribution tags. Run `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug` before committing.
