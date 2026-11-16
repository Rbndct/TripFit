---
version: 1
slug: "itinerary"
primary_target: "itinerary"
related_targets: []
---

## Scope & mode

Itinerary / day-by-day builder screen (MainActivity) — where a traveler reviews and adds stops across trip days. Mode: Operate.

## Audience, job, action, proof, constraints

Independent travelers reviewing or building a multi-day itinerary; job is "see this day's stops in order, know which are locked in, add more." Primary action: add a destination (FAB); secondary: jump between days. Constraint: Material 3 components, standard touch targets, and the app's real navigation stay as-is — the world supplies only type/palette/density/signature move, never layout or controls (per the Android Operate mode rules).

## Direction contract

THESIS: The day-by-day screen reads as a set of boarding legs, not one long printed itinerary — each day is its own leg you step into, refusing the single endless-scroll list every itinerary app defaults to.

OWN-WORLD: Inherits Home's fixed ticket system from DESIGN.md as-is — fixed navy shell (`ticket_carrier_navy` #13213B / `on_ticket_carrier` #FFFFFF, non-theme-reactive), tabular monospace for every count/status/time field, Material components themed through it, no new palette. This surface's own structural move: a horizontal pill tab strip (Day 1 / Day 2 / Day 3…) directly under the header, day numbers in tabular monospace, the active pill filled with the fixed navy.

STORY: The visitor opens the itinerary and sees the day tabs immediately; the first day is selected by default; tapping a tab swaps the list below to just that day's numbered stop cards, booked stops keep their existing lock badge unchanged; the FAB adds a destination to the active day.

FIRST VIEWPORT: header (trip name + subtitle, navy per Home) at top; directly below, the day tab strip (one pill per day, tabular monospace numbers, active pill filled navy, others outlined); below that, the selected day's stop list using the existing numbered-card treatment; FAB bottom-right.

FORM: Day Tabs — dealt lead (kicker THE ROLL) from the surface-scope round over Day Rail and the Plain Flowing List baseline, seed key b496050d.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Memorable moment

The day tab strip itself — tapping between days should feel like flipping to a different leg of the same boarding pass, not paging through a generic tab control.

## Unresolved decisions

- Manual stop reordering (backlog item #5) is out of scope for this structural pass — not being added here, only the existing read-only day/stop display is being restructured.
- Exact pill sizing/spacing resolved during build within Home's existing dimens scale.
