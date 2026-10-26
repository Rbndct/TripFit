---
version: 1
slug: "home"
primary_target: "home"
related_targets: []
---

## Scope & mode

Home screen (HomeActivity) — the trip dashboard a traveler returns to. Mode: Operate.

## Audience, job, action, proof, constraints

Independent travelers / small groups with a trip in progress or being planned; job is "see where my trip stands" at a glance (next trip's day/stop progress, status, other trips) and jump into booking or adding a destination. Primary action: open the active trip, or FAB to add a destination. Constraint: Google Sign-In button keeps its official branding regardless of this world; Material 3 navigation/components/touch targets stay standard per the Android platform reference.

## Direction contract

THESIS: Home reads as a confirmed travel document — the trip you're actually taking right now is a locked ticket, not a card in a feed; refuses the soft-cornered, photo-card "upcoming trips" feed every travel app already ships.

OWN-WORLD: off-white paper ground (#fbfbf9), deep navy carrier band (#13213b) as the one committed shell color, white secondary surfaces; tabular monospace for every time/count/status field, a plain humanist sans for labels and prose; a dashed perforation divider and a barcode-pattern strip as the two recurring ticket motifs; Material 3 components (FAB, nav bar, chips, cards) themed navy-on-white rather than replaced.

STORY: the visitor opens Home and immediately reads their next trip as a confirmed boarding pass — day progress, stop count, and status in one ticket card; scrolling down shows other trips as plainer stub-style cards, draft vs. confirmed read by a badge, never by a different card family; the FAB books a new destination.

FIRST VIEWPORT: greeting + avatar at top; below it, the hero ticket card — navy band naming the trip and "Day X of Y", a perforated divider splitting stop-count from status, a barcode strip closing the card; "Your Trips" section header; one secondary trip stub card with a DRAFT badge; FAB bottom-right; bottom nav (Home / Trips / Budget / Profile) with navy active state.

FORM: Boarding Pass / Ticket Stub — picked from the safer-register round (card 1 of 3 presented there), seed key 91b9f777.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Memorable moment

The navy ticket band + perforated divider + barcode strip on the hero trip card — the one signature move, never repeated as decoration elsewhere on the screen.

## Unresolved decisions

- Exact navy/accent hex and type family selection happen during build, within this contract's palette intent.
- Dark theme and tablet/rail layout follow the Android platform reference (Material tonal surfaces, nav rail at expanded width) — not separately decided here.
