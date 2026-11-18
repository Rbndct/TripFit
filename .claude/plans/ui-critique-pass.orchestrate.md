# Plan-Orchestrate Result

**Plan**: `.claude/plans/ui-critique-pass.md`
**Lang**: `java`
**ECC mode**: `plugin`
**Steps**: 18 (step 0, the capture harness, is already done)
**Scope**: all

## Prerequisite (run in a shell, not through orchestrate)

Open the agent terminal in the TripFit folder with the model you want: `claude --model sonnet` or `claude --model opus`.

Before steps 2 and 4, re-capture the two screens whose screenshots came out blank:

```bash
(nohup ~/Library/Android/sdk/emulator/emulator -avd Pixel_3a_API_34_extension_level_7_arm64-v8a -no-window -no-audio -no-boot-anim -no-snapshot -memory 1536 -cores 2 -gpu swiftshader_indirect >/dev/null 2>&1 &)
~/Library/Android/sdk/platform-tools/adb wait-for-device
./gradlew assembleDebug -q && ~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 .claude/plans/tools/capture.py home itinerary
python3 .claude/plans/tools/native_checks.py 440 .impeccable/review/home.xml .impeccable/review/itinerary.xml
~/Library/Android/sdk/platform-tools/adb emu kill
```

## Steps overview

| # | Title | Tags | Chain |
|---|---|---|---|
| 1 | Login | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 2 | Home | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 3 | New Trip | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 4 | Itinerary | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 5 | Add Destination | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 6 | Map Picker | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 7 | Stop Detail | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 8 | Optimize | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 9 | Discover | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 10 | Suggestions | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 11 | Budget | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 12 | Budget Setup | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 13 | Calendar Sync | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 14 | Trip Summary | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 15 | Group Trip | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 16 | Journal | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 17 | Profile | impl, review | `ecc:tdd-guide,ecc:java-reviewer` |
| 18 | Final regression sweep + DESIGN.md update | review, docs | `ecc:java-reviewer,ecc:code-reviewer,ecc:doc-updater` |

---

## Step 1 — Login

**Intent**: Critique the Login screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-1] Critique then fix the Login screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Login, SLUG=login, PRIMARY=app/src/main/res/layout/activity_login.xml, FILES=Key files of queue row 1; then mark row 1 done and commit \"fix(ui): login critique pass\"; Acceptance: .impeccable/critique/login.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 2 — Home

**Intent**: Critique the Home screen from its screenshots, native checks and source, then apply the P0/P1 fixes. Re-capture this screen first (see Prerequisite); its current screenshots are blank.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-2] Critique then fix the Home screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Home, SLUG=home, PRIMARY=app/src/main/res/layout/activity_home.xml, FILES=Key files of queue row 2; then mark row 2 done and commit \"fix(ui): home critique pass\"; Acceptance: .impeccable/critique/home.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 3 — New Trip

**Intent**: Critique the New Trip screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-3] Critique then fix the New Trip screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=New Trip, SLUG=new-trip, PRIMARY=app/src/main/res/layout/activity_new_trip.xml, FILES=Key files of queue row 3; then mark row 3 done and commit \"fix(ui): new-trip critique pass\"; Acceptance: .impeccable/critique/new-trip.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 4 — Itinerary

**Intent**: Critique the Itinerary screen from its screenshots, native checks and source, then apply the P0/P1 fixes. Re-capture this screen first (see Prerequisite); its current screenshots are blank.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-4] Critique then fix the Itinerary screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Itinerary, SLUG=itinerary, PRIMARY=app/src/main/res/layout/activity_main.xml, FILES=Key files of queue row 4; then mark row 4 done and commit \"fix(ui): itinerary critique pass\"; Acceptance: .impeccable/critique/itinerary.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 5 — Add Destination

**Intent**: Critique the Add Destination screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-5] Critique then fix the Add Destination screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Add Destination, SLUG=add-destination, PRIMARY=app/src/main/res/layout/activity_add_destination.xml, FILES=Key files of queue row 5; then mark row 5 done and commit \"fix(ui): add-destination critique pass\"; Acceptance: .impeccable/critique/add-destination.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 6 — Map Picker

**Intent**: Critique the Map Picker screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-6] Critique then fix the Map Picker screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Map Picker, SLUG=map-picker, PRIMARY=app/src/main/res/layout/activity_map_picker.xml, FILES=Key files of queue row 6; then mark row 6 done and commit \"fix(ui): map-picker critique pass\"; Acceptance: .impeccable/critique/map-picker.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 7 — Stop Detail

**Intent**: Critique the Stop Detail screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-7] Critique then fix the Stop Detail screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Stop Detail, SLUG=stop-detail, PRIMARY=app/src/main/res/layout/activity_stop_detail.xml, FILES=Key files of queue row 7; then mark row 7 done and commit \"fix(ui): stop-detail critique pass\"; Acceptance: .impeccable/critique/stop-detail.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 8 — Optimize

**Intent**: Critique the Optimize screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-8] Critique then fix the Optimize screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Optimize, SLUG=optimize, PRIMARY=app/src/main/res/layout/activity_optimize.xml, FILES=Key files of queue row 8; then mark row 8 done and commit \"fix(ui): optimize critique pass\"; Acceptance: .impeccable/critique/optimize.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 9 — Discover

**Intent**: Critique the Discover screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-9] Critique then fix the Discover screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Discover, SLUG=discover, PRIMARY=app/src/main/res/layout/activity_discover.xml, FILES=Key files of queue row 9; then mark row 9 done and commit \"fix(ui): discover critique pass\"; Acceptance: .impeccable/critique/discover.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 10 — Suggestions

**Intent**: Critique the Suggestions screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-10] Critique then fix the Suggestions screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Suggestions, SLUG=suggestions, PRIMARY=app/src/main/res/layout/activity_suggestions.xml, FILES=Key files of queue row 10; then mark row 10 done and commit \"fix(ui): suggestions critique pass\"; Acceptance: .impeccable/critique/suggestions.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 11 — Budget

**Intent**: Critique the Budget screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-11] Critique then fix the Budget screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Budget, SLUG=budget, PRIMARY=app/src/main/res/layout/activity_budget.xml, FILES=Key files of queue row 11; then mark row 11 done and commit \"fix(ui): budget critique pass\"; Acceptance: .impeccable/critique/budget.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 12 — Budget Setup

**Intent**: Critique the Budget Setup screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-12] Critique then fix the Budget Setup screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Budget Setup, SLUG=budget-setup, PRIMARY=app/src/main/res/layout/activity_budget_setup.xml, FILES=Key files of queue row 12; then mark row 12 done and commit \"fix(ui): budget-setup critique pass\"; Acceptance: .impeccable/critique/budget-setup.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 13 — Calendar Sync

**Intent**: Critique the Calendar Sync screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-13] Critique then fix the Calendar Sync screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Calendar Sync, SLUG=calendar-sync, PRIMARY=app/src/main/res/layout/activity_calendar_sync.xml, FILES=Key files of queue row 13; then mark row 13 done and commit \"fix(ui): calendar-sync critique pass\"; Acceptance: .impeccable/critique/calendar-sync.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 14 — Trip Summary

**Intent**: Critique the Trip Summary screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-14] Critique then fix the Trip Summary screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Trip Summary, SLUG=summary, PRIMARY=app/src/main/res/layout/activity_summary.xml, FILES=Key files of queue row 14; then mark row 14 done and commit \"fix(ui): summary critique pass\"; Acceptance: .impeccable/critique/summary.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 15 — Group Trip

**Intent**: Critique the Group Trip screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-15] Critique then fix the Group Trip screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Group Trip, SLUG=group, PRIMARY=app/src/main/res/layout/activity_group.xml, FILES=Key files of queue row 15; then mark row 15 done and commit \"fix(ui): group critique pass\"; Acceptance: .impeccable/critique/group.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 16 — Journal

**Intent**: Critique the Journal screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-16] Critique then fix the Journal screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Journal, SLUG=journal, PRIMARY=app/src/main/res/layout/activity_journal.xml, FILES=Key files of queue row 16; then mark row 16 done and commit \"fix(ui): journal critique pass\"; Acceptance: .impeccable/critique/journal.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 17 — Profile

**Intent**: Critique the Profile screen from its screenshots, native checks and source, then apply the P0/P1 fixes.
**Tags**: impl, review
**Chain rationale**: `tdd-guide` is the only agent in this chain that can edit files, so it writes the critique and applies the fixes; `java-reviewer` closes the loop. The extra reviewer from the `review` tag is dropped to keep the Lean one-pass-per-screen decision.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-17] Critique then fix the Profile screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Profile, SLUG=profile, PRIMARY=app/src/main/res/layout/activity_profile.xml, FILES=Key files of queue row 17; then mark row 17 done and commit \"fix(ui): profile critique pass\"; Acceptance: .impeccable/critique/profile.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
```

## Step 18 — Final regression sweep + DESIGN.md update

**Intent**: Confirm no screen regressed through shared components and document the settled patterns. Run the Prerequisite capture for all screens (`capture.py` with no arguments) before this step.
**Tags**: review, docs
**Chain rationale**: Read-only reviewers verify; `doc-updater` closes by updating DESIGN.md.

```bash
/ecc:orchestrate custom "ecc:java-reviewer,ecc:code-reviewer,ecc:doc-updater" "[Plan: .claude/plans/ui-critique-pass.md#step-18] Final regression sweep after all 17 screens: compare the fresh screenshots in .impeccable/review/ against each .impeccable/critique/<slug>.md, confirm the queue's Shared-Changes Log broke no earlier screen, then update DESIGN.md with the settled patterns; Acceptance: every queue row is done or flagged; gradle assembleDebug, unit tests and lint pass with 0 lint errors; DESIGN.md documents the shared toolbar, row card and status colors as shipped"
```

## Batch execution

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-1] Critique then fix the Login screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Login, SLUG=login, PRIMARY=app/src/main/res/layout/activity_login.xml, FILES=Key files of queue row 1; then mark row 1 done and commit \"fix(ui): login critique pass\"; Acceptance: .impeccable/critique/login.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-2] Critique then fix the Home screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Home, SLUG=home, PRIMARY=app/src/main/res/layout/activity_home.xml, FILES=Key files of queue row 2; then mark row 2 done and commit \"fix(ui): home critique pass\"; Acceptance: .impeccable/critique/home.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-3] Critique then fix the New Trip screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=New Trip, SLUG=new-trip, PRIMARY=app/src/main/res/layout/activity_new_trip.xml, FILES=Key files of queue row 3; then mark row 3 done and commit \"fix(ui): new-trip critique pass\"; Acceptance: .impeccable/critique/new-trip.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-4] Critique then fix the Itinerary screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Itinerary, SLUG=itinerary, PRIMARY=app/src/main/res/layout/activity_main.xml, FILES=Key files of queue row 4; then mark row 4 done and commit \"fix(ui): itinerary critique pass\"; Acceptance: .impeccable/critique/itinerary.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-5] Critique then fix the Add Destination screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Add Destination, SLUG=add-destination, PRIMARY=app/src/main/res/layout/activity_add_destination.xml, FILES=Key files of queue row 5; then mark row 5 done and commit \"fix(ui): add-destination critique pass\"; Acceptance: .impeccable/critique/add-destination.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-6] Critique then fix the Map Picker screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Map Picker, SLUG=map-picker, PRIMARY=app/src/main/res/layout/activity_map_picker.xml, FILES=Key files of queue row 6; then mark row 6 done and commit \"fix(ui): map-picker critique pass\"; Acceptance: .impeccable/critique/map-picker.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-7] Critique then fix the Stop Detail screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Stop Detail, SLUG=stop-detail, PRIMARY=app/src/main/res/layout/activity_stop_detail.xml, FILES=Key files of queue row 7; then mark row 7 done and commit \"fix(ui): stop-detail critique pass\"; Acceptance: .impeccable/critique/stop-detail.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-8] Critique then fix the Optimize screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Optimize, SLUG=optimize, PRIMARY=app/src/main/res/layout/activity_optimize.xml, FILES=Key files of queue row 8; then mark row 8 done and commit \"fix(ui): optimize critique pass\"; Acceptance: .impeccable/critique/optimize.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-9] Critique then fix the Discover screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Discover, SLUG=discover, PRIMARY=app/src/main/res/layout/activity_discover.xml, FILES=Key files of queue row 9; then mark row 9 done and commit \"fix(ui): discover critique pass\"; Acceptance: .impeccable/critique/discover.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-10] Critique then fix the Suggestions screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Suggestions, SLUG=suggestions, PRIMARY=app/src/main/res/layout/activity_suggestions.xml, FILES=Key files of queue row 10; then mark row 10 done and commit \"fix(ui): suggestions critique pass\"; Acceptance: .impeccable/critique/suggestions.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-11] Critique then fix the Budget screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Budget, SLUG=budget, PRIMARY=app/src/main/res/layout/activity_budget.xml, FILES=Key files of queue row 11; then mark row 11 done and commit \"fix(ui): budget critique pass\"; Acceptance: .impeccable/critique/budget.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-12] Critique then fix the Budget Setup screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Budget Setup, SLUG=budget-setup, PRIMARY=app/src/main/res/layout/activity_budget_setup.xml, FILES=Key files of queue row 12; then mark row 12 done and commit \"fix(ui): budget-setup critique pass\"; Acceptance: .impeccable/critique/budget-setup.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-13] Critique then fix the Calendar Sync screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Calendar Sync, SLUG=calendar-sync, PRIMARY=app/src/main/res/layout/activity_calendar_sync.xml, FILES=Key files of queue row 13; then mark row 13 done and commit \"fix(ui): calendar-sync critique pass\"; Acceptance: .impeccable/critique/calendar-sync.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-14] Critique then fix the Trip Summary screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Trip Summary, SLUG=summary, PRIMARY=app/src/main/res/layout/activity_summary.xml, FILES=Key files of queue row 14; then mark row 14 done and commit \"fix(ui): summary critique pass\"; Acceptance: .impeccable/critique/summary.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-15] Critique then fix the Group Trip screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Group Trip, SLUG=group, PRIMARY=app/src/main/res/layout/activity_group.xml, FILES=Key files of queue row 15; then mark row 15 done and commit \"fix(ui): group critique pass\"; Acceptance: .impeccable/critique/group.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-16] Critique then fix the Journal screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Journal, SLUG=journal, PRIMARY=app/src/main/res/layout/activity_journal.xml, FILES=Key files of queue row 16; then mark row 16 done and commit \"fix(ui): journal critique pass\"; Acceptance: .impeccable/critique/journal.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/ui-critique-pass.md#step-17] Critique then fix the Profile screen. Follow .claude/plans/tools/agent-prompt.md with SCREEN=Profile, SLUG=profile, PRIMARY=app/src/main/res/layout/activity_profile.xml, FILES=Key files of queue row 17; then mark row 17 done and commit \"fix(ui): profile critique pass\"; Acceptance: .impeccable/critique/profile.md has the DEGRADED banner and a filled Fix log; every P0/P1 fixed or flagged; gradle compile, tests and lint pass, 0 lint errors; Out of scope: other screens' files, adb/emulator use"
/ecc:orchestrate custom "ecc:java-reviewer,ecc:code-reviewer,ecc:doc-updater" "[Plan: .claude/plans/ui-critique-pass.md#step-18] Final regression sweep after all 17 screens: compare the fresh screenshots in .impeccable/review/ against each .impeccable/critique/<slug>.md, confirm the queue's Shared-Changes Log broke no earlier screen, then update DESIGN.md with the settled patterns; Acceptance: every queue row is done or flagged; gradle assembleDebug, unit tests and lint pass with 0 lint errors; DESIGN.md documents the shared toolbar, row card and status colors as shipped"
```
