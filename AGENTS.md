# Trippings — AI Agent Instructions

This repository contains **Trippings** (formerly TripFit), an Android trip-planning app built in Java 11.

For detailed developer instructions and copy-paste prompt templates tailored for ChatGPT, see [CHATGPT.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/CHATGPT.md).
For the full prototype unification roadmap, see [Combine-Prototypes-Plan.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/docs/Combine-Prototypes-Plan.md) and [Prototype-Merge-Map.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/docs/Prototype-Merge-Map.md).

---

## Technical Stack & Constraints

1. **Language:** **Java 11** strictly (`app/src/main/java`). Do not introduce Kotlin source files to the app module.
2. **UI Framework:** Standard Android Views (AppCompat, Material 3 Components, ConstraintLayout, RecyclerView). **No Jetpack Compose.**
3. **Architecture:**
   * **Room SQLite DB (`TripFitDatabase` v1):** Trips, Itinerary items, and Places.
   * **`PrototypeStore`:** SharedPreferences + Gson for prototype state (costs, leg modes, companions, weather, journal).
   * **Base Activity:** All screens extend `com.itismob.s01.group01.tripfit.ScreenActivity` (navy toolbar shell).
4. **Design Rules:**
   * 16dp corner radius outlined cards (`item_row`).
   * Touch targets $\ge$ 48x48dp.
   * All user-facing strings in `res/values/strings.xml`.
   * Dual-mode colors defined in both `values/colors.xml` and `values-night/colors.xml`.
5. **Testing & Verification:**
   * Compile check: `./gradlew compileDebugJavaWithJavac`
   * Unit tests: `./gradlew testDebugUnitTest`
   * Lint check: `./gradlew lintDebug`
6. **Git Hygiene:**
   * One atomic commit per step with conventional commit message (`feat: ...`, `refactor: ...`, `docs: ...`).
   * Do not add AI co-author or attribution tags.
