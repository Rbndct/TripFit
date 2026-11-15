⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Optimize route: critique

Evidence: `optimize-light.png`, `optimize-dark.png` render. `optimize-font.png` is BLANK (launcher icon, launch timing), so font-scale 1.3 was judged from source only. `optimize-light.png` predates the shared navy status-bar fix (clock/icons look dark on navy); the dark capture shows light icons. `optimize-checks.txt`: no native-check issues.

## Design-specificity verdict
The screen is on-world but generic: navy shell, 16dp outlined cards, monospace distances. It is a plain summary + rows list with no ticket cue, which is acceptable for an Operate-mode confirmation step in Refinement mode. Its weakness is copy and logic: it said "Your route is already efficient" above a primary "Confirm new order" button and rows labelled "New order" whose before/after distances were identical, so the screen contradicted itself.

## Nielsen heuristics (0-4)
1. Visibility of system status: 3 (loading state with text; no busy state on apply)
2. Match with real world: 3
3. User control and freedom: 3 (Keep current order is clear)
4. Consistency and standards: 3
5. Error prevention: 1 (could confirm a no-op, or a nearest-neighbour order that is longer than the original)
6. Recognition over recall: 3
7. Flexibility and efficiency: n/a
8. Aesthetic and minimalist design: 3
9. Error recovery: 2 (failure toast, but buttons stay live and later failures repeat)
10. Help and documentation: 3 (locked-stop note)

## Cognitive load
Low. One summary, one card per day, two actions. Load spikes in the row: the monospace "13.3 km → 13.3 km" trailing column took about 40% of the card width and squeezed the order list into a narrow, 3-line column (it would be worse at font 1.3). The locked-stops note also shows in the empty state, where it is irrelevant.

## Strengths
- Pinned action bar outside the scroll area keeps the single primary action reachable.
- Dark theme is clean; hierarchy (summary > day cards > note) is clear.
- Booked-stop rule is stated in plain language.

## Issues
1. P1 Contradiction: summary says "already efficient" but "Confirm new order" is enabled and rows say "New order" with identical distances (optimize-light.png; `OptimizeActivity.java` bindSummary enabled Apply whenever rows existed). Fix: queue only days whose order really improves; disable Apply when nothing is queued; unchanged days read "Already the shortest order".
2. P1 Wrong result: nearest-neighbour can lengthen a day, yet it was offered and applied (savings were only clamped for display). Fix: a day keeps its current order unless the new one is shorter than before.
3. P1 No busy state on apply: repeated taps queue duplicate writes; on failure the buttons stay live and every failure toasts. Fix: disable both buttons while writing, re-enable once on failure.
4. P1 Result not visible: success finishes the screen silently. Fix: a platform Toast ("Route order updated") that outlives the activity (no change to the Itinerary contract).
5. P1 Cramped row: trailing before/after pair crushes the order text (optimize-light.png). Fix: trailing shows only the new distance; the "Was x km" line moves into the subtitle.
6. P2 Buttons are 40dp tall (M3 default); set `minHeight` 48dp on both (`activity_optimize.xml`).
7. P2 Summary is not exposed as a heading to TalkBack; set accessibility heading in code.
8. P2 Locked note shown in the empty state; hide it when there is nothing to optimize.
9. P2 "Saves about 0 min" is possible when a reorder saves under 30 s; added a "saves a little travel time" summary.
10. P3 "→" glyphs in the order are read as "right arrow" by TalkBack; left as is.
11. P3 Rotation: state is recomputed on recreate (900 ms spinner again); acceptable, the delayed callback is still cancelled on destroy.
12. P3 Font 1.3 capture is blank; re-check in the final sweep. Itinerary does not show a result message after Apply (Toast is the stopgap).

## Fix log
- 1, 2, 5 (`OptimizeActivity.java`, `strings_prototype.xml`): describeDay only queues days that improve by more than 0.05 km; summary and Apply state follow `pendingOrders`; trailing = new distance, subtitle = order plus "Was x km" or "Already the shortest order".
- 3, 4 (`OptimizeActivity.java`): `applying` flag disables both buttons, re-enables once on failure; success shows a Toast, then finishes. Delayed-callback cancel on destroy kept.
- 6, 8 (`activity_optimize.xml`): buttons minHeight 48dp; locked note given an id and hidden in the empty state.
- 7, 9 (`OptimizeActivity.java`, `strings_prototype.xml`): summary is an accessibility heading; `optimize_summary_small` added.
- Not fixed: 10, 11, 12 (P3).
