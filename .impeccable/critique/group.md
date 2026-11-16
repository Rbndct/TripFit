⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Group Trip critique

Evidence: `group-light.png`, `group-dark.png`, `group-font.png` (all rendered; the light shot predates the shared status-bar fix, so its dark clock on navy is not judged). Only the invite card, travelers and the first one or two vote rows are visible; the split section was reviewed from source.

## Design specificity
The shell is on-brand (navy carrier bar, flat 16dp outlined cards, monospace invite code) but the body is a stack of identical outlined cards, so the screen reads as a settings list rather than a boarding pass. The one distinctive moment is the TRIP-7K2Q code in monospace. Within Refinement mode that is acceptable; a ticket-style invite card would change the visual world, so it is not attempted.

## Nielsen heuristics (0-4)
1. Visibility of system status: 2. Votes changed only the button label and count; no row-level "mine" state. Now a selected outline plus "including yours" text. (3 after fix)
2. Match with real world: 3. "Travelers", "Vote on destinations", "Cost split" are clear.
3. User control: 3. Vote toggles back; Even/Custom reversible.
4. Consistency: 3. Reuses shared rows. Total line was a monospace sentence (broke the data-only rule). Fixed.
5. Error prevention: 3. Nothing destructive.
6. Recognition over recall: 3. Split rows now name "You".
7. Flexibility: 2. Custom shares are fixed mock values (flagged).
8. Aesthetic/minimal: 3. Dense rows at font 1.3: "Senso-ji Temple 2 votes Vote" squeezed in one line (group-font.png). Fixed by moving the tally under the name.
9. Error recovery: n/a.
10. Help/docs: 2. No hint that the member list and tallies are demo data (prototype, accepted).
Accessibility sub-check: 2 before (identical "Vote" buttons, no headings, Copy ambiguous), 3 after.

## Cognitive load
Three sections, 5-ish items each; fine. The page is long (invite, 4 members, 5 votes, split, 4 rows) and the vote and split sections sit below the fold, but section headings chunk it well. Four equal-weight member cards push the actionable vote section down; they could be compact (flagged P3).

## Strengths
1. Invite card: code is large, monospace, with Copy (outlined) and Share (filled) giving one clear primary action.
2. Dark mode holds: navy shell fixed, outlines and tonal buttons remain legible.
3. Mock flows all work and survive rotation: votes live in the store, copy confirms with a toast, share uses the system chooser.

## Issues
1. P1 · Vote buttons all read "Vote" to TalkBack (`GroupActivity.java` renderVotes, `item_row.xml`). Fix: `withAccessibility("Vote for X" / "Voted for X, tap to remove")`; both contain the visible label.
2. P1 · Voted state is only a label swap on a pale tonal button (`group-light.png`); color-ish and easy to miss. Fix: `withSelected(voted)` primary outline plus "N votes, including yours" text.
3. P1 · "1 votes" grammar (`vote_count` string; only call site GroupActivity). Fix: plurals `vote_count`, `vote_count_yours` via `getQuantityString`.
4. P1 · Vote rows crowd at font 1.3: title, monospace "2 votes" and button share one line (`group-font.png`). Fix: tally moves to the subtitle (sans), trailing freed for the title.
5. P1 · Section headings are not marked for TalkBack (`activity_group.xml`). Fix: `accessibilityHeading="true"` on the three titles.
6. P1 · "Estimated total ₱15,000" is a sentence in monospace (`activity_group.xml` text_split_total). Fix: sans label plus monospace amount in a row; string `split_total` is now the label only.
7. P1 · Copy button's spoken name is just "Copy". Fix: `contentDescription` "Copy invite code" (contains visible label).
8. P1 · Split toggle may be restored to Custom after `onCreate` rendered Even, leaving rows out of sync after rotation. Fix: `onRestoreInstanceState` re-renders.
9. P2 · Split row for the owner shows "Traveler" with no "You" cue; Even mode rows have no subtitle. Fix: "You" / "You · 40% share" (`splitSubtitle`).
10. P2 · Even/Custom toggle buttons had no explicit 48dp height. Fix: `minHeight=48dp`.
11. P3 · SCRIPTED CHECK `TOUCH_TARGET 88x30dp` on Vote is a false positive: in `group.xml` the first Vote button is bounds [750,1830][992,1962] = 88x48dp (`item_row.xml` already sets `minHeight=48dp`); the 30dp node is the second row clipped by the viewport edge. Cause is local to the screenshot, not shared; no shared file changed.
12. P3 · Share invite can stack two chooser dialogs on a fast double tap. Not fixed.
13. P3 · Four equal member cards could be compact. Not fixed.

## Fix log
- Fixed 1, 2, 3, 4 in `GroupActivity.java` (`voteRow`), `strings_prototype.xml` (plurals `vote_count`, `vote_count_yours`, `vote_cd`, `vote_remove_cd`).
- Fixed 5, 6, 7, 10 in `activity_group.xml` (headings, total row, copy cd, toggle minHeight); strings `split_total` (label only), `cd_copy_code`.
- Fixed 8 in `GroupActivity.java` (`onRestoreInstanceState`); 9 via `splitSubtitle`, strings `split_you`, `split_you_share`.
- 11 verified false positive; 12, 13 left.
- Shared files changed: none (strings_prototype.xml additions only). Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0, 0 lint errors.
