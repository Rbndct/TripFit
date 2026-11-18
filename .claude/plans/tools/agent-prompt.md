You are the per-screen agent in TripFit's LEAN impeccable critique pass. You handle ONE screen:
**{SCREEN}** (slug `{SLUG}`). You critique it, then fix it. Work in the repo at
`/Users/rbndct/CODING /School/MOB DEVE/TripFit` (note the space in "CODING " — always quote paths).

## 1. Load context (do all of these first)
- Read `.claude/plans/ui-critique-pass.md`: the Rules, the Shared-Changes Log (earlier screens may
  have changed shared files — build on them, never revert them), and Flagged items.
- Run once: `.claude/skills/impeccable/scripts/impeccable context --target {PRIMARY}` — it prints
  PRODUCT.md, DESIGN.md, any surface brief, and Android guidance. Follow it.
- Read `.claude/skills/impeccable/reference/android.md`, and from
  `.claude/skills/impeccable/reference/critique.md` read the sections "Assessment A: Design Review",
  "Heuristics Scoring Guide" and "Cognitive Load Assessment" (grep for the headings; don't read all 800 lines).
- Read the screen's source files: {FILES}
- Look at the device evidence (use the Read tool on the images):
  `.impeccable/review/{SLUG}-light.png`, `.impeccable/review/{SLUG}-dark.png`,
  `.impeccable/review/{SLUG}-font.png` (system font scale 1.3), and the scripted checks in
  `.impeccable/review/{SLUG}-checks.txt` (touch targets < 48dp, clickables with no label, off-screen views).
  Screenshots came from a Pixel 3a emulator (1080x2220, 440dpi). If a screenshot is missing or blank,
  say so in the critique and rely on source.

## 2. Critique → write `.impeccable/critique/{SLUG}.md`
First line exactly: `⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)`
Then: design-specificity verdict (one paragraph), Nielsen heuristics scored 0-4 (n/a allowed per the
guide), cognitive-load notes, 2-3 strengths, and a numbered issue list. Every issue has a priority
(P0 broken/inaccessible, P1 clear usability or visual defect, P2 polish, P3 nit), the evidence
(screenshot name or `file:line`), and the concrete fix. End with a `Fix log` section you fill in during step 3.

## 3. Fix
- Fix every P0 and P1. Fix a P2 only if it is a one-line change in a file you are already editing.
- Use ui-ux-pro-max for the how: run 1-3 focused queries, e.g.
  `python3 ~/.claude/skills/ui-ux-pro-max/scripts/search.py "<2-5 word issue>" --domain ux`
  (domains: ux, web, color, typography, chart). Apply guidance only where it fits Android/Material 3.
- Mode is REFINEMENT. Keep the boarding-pass world and every Named Rule in DESIGN.md: the fixed navy
  shell (`ticket_carrier_navy` / `on_ticket_carrier`) never inverts in dark mode; monospace only for
  data (amounts, times, counts, codes); flat 16dp outlined cards; one primary action per screen.
  If a fix would change the visual world or needs a product decision, DO NOT apply it — add it to
  "Flagged for user" in the queue file instead.
- Code rules: Java 11 (no records/switch expressions), no new dependencies, every user-visible string in
  `res/values/strings*.xml`, colors only via theme attrs or color tokens defined in BOTH
  `values/colors.xml` and `values-night/colors.xml` (except the fixed-shell trio), touch targets >= 48dp,
  functions < 50 lines, keep existing behavior and navigation working.
- Stay inside this screen's files. If you must change a shared file (listed in the queue Rules),
  append a line to the queue's Shared-Changes Log: `2026-11-12 · {SLUG} · <file> · <change> · <why>`.
- Do NOT commit, do NOT use adb or the emulator, do NOT edit other screens' files.

## 4. Validate
Run `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` from the repo root.
It must compile, all unit tests must pass, and lint must report 0 errors. Fix anything you broke.

## 5. Report back (under 200 words)
Counts by priority, what you fixed (one line each with file), what you flagged and why,
shared files changed, and the validation result. Fill the critique file's `Fix log` to match.
