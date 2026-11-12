---
name: TripFit
description: Trip-planning Android app whose Home and Itinerary screens read as boarding passes — one hero ticket on Home, a per-day tab strip on Itinerary, both riding the same fixed navy shell.
colors:
  ticket-carrier-navy: "#13213B"
  on-ticket-carrier: "#FFFFFF"
  on-ticket-carrier-translucent: "#66FFFFFF"
  md-primary: "#13213B"
  md-on-primary: "#FFFFFF"
  md-primary-container: "#D7DEEA"
  md-on-primary-container: "#13213B"
  md-secondary: "#8A5B00"
  md-on-secondary: "#FFFFFF"
  md-secondary-container: "#FFE8B2"
  md-on-secondary-container: "#2B1B00"
  md-background: "#FBFBF9"
  md-on-background: "#15181D"
  md-surface: "#FBFBF9"
  md-on-surface: "#15181D"
  md-surface-variant: "#E7E9ED"
  md-on-surface-variant: "#4A4E57"
  md-outline: "#8B93A6"
  ticket-barcode-bar: "#8B93A6"
typography:
  title:
    fontFamily: "Roboto (Material textAppearanceTitleLarge / TitleMedium, system default)"
    fontSize: "22sp / 16sp"
    fontWeight: 500
  body:
    fontFamily: "Roboto (Material textAppearanceBodySmall)"
    fontSize: "12sp"
    fontWeight: 400
  data-mono:
    fontFamily: "monospace (system face, android:fontFamily=\"monospace\")"
    fontSize: "11sp-12sp (Material textAppearanceLabelLarge / LabelSmall)"
    fontWeight: 400
    letterSpacing: "normal"
rounded:
  card: "16dp"
  chip: "20dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
components:
  trip-card-hero:
    backgroundColor: "{colors.md-surface}"
    textColor: "{colors.on-ticket-carrier}"
    rounded: "{rounded.card}"
    padding: "16dp"
  trip-card-stub:
    backgroundColor: "{colors.md-surface}"
    textColor: "md-on-surface (theme role, not a frontmatter primitive)"
    rounded: "{rounded.card}"
    padding: "16dp"
  badge-draft:
    backgroundColor: "colorSurfaceVariant (theme role)"
    textColor: "colorOnSurfaceVariant (theme role)"
    rounded: "{rounded.chip}"
  badge-planned:
    backgroundColor: "colorSecondaryContainer (theme role)"
    textColor: "colorOnSurfaceVariant (theme role)"
    rounded: "{rounded.chip}"
  day-tab-selected:
    backgroundColor: "{colors.on-ticket-carrier}"
    textColor: "{colors.ticket-carrier-navy}"
    rounded: "{rounded.chip}"
    padding: "16dp 8dp"
  day-tab-unselected:
    backgroundColor: "transparent"
    textColor: "{colors.on-ticket-carrier}"
    rounded: "{rounded.chip}"
    padding: "16dp 8dp"
---

# Design System: TripFit

## Overview

**Creative North Star: "Boarding Pass / Ticket Stub"**

TripFit's Home screen reads its trip list as a small stack of travel tickets: one trip — the one the pipeline (destination → route → calendar → budget) is currently moving — gets the full boarding-pass treatment; every other trip is a plain stub. The Itinerary screen (per-trip day-by-day builder) is the second surface built in this world: it inherits the fixed navy shell and tabular-monospace rule as-is and adds one structural move of its own — a horizontal day-tab strip, read as stepping between legs of the same boarding pass rather than paging a generic tab control. Both screens stay inside the platform's standard navigation, layout, and controls: the world contributes type, palette, density, and a signature move per surface, never a substitute for RecyclerView lists, AppBarLayout, or Material components.

The palette is a fixed navy carrier band (`#13213B`) holding white text, paired with an off-white paper surface — a deliberate, non-theme-reactive shell color chosen once and held constant across light and dark, the same way a printed ticket's ink color doesn't invert at night. Everything else in either screen (surfaces, outlines, secondary/amber accents) is ordinary Material role tokens that DayNight inverts normally.

Count and status data — the ticket's "business" fields — are set in monospace, read as a punch-card ledger. Titles, subtitles, and prose stay on the existing Material/Roboto type scale; monospace never leaks into a generic "technical" costume elsewhere on either screen. On Itinerary, this extends naturally to the day-tab numbers, which are data in the same sense Home's day/stop counts are.

**Key Characteristics:**
- One fixed shell color (navy carrier band + its white text, plus its translucent stroke variant) that never inverts with DayNight; all other color is ordinary theme-reactive Material roles.
- Monospace reserved strictly for time/count/status data fields — including day-tab numbers — never for titles, labels, or body prose.
- Exactly one hero card carries Home's ticket motif (navy band, perforation, barcode); every other trip is a plain stub card with no ticket ornament.
- Status vocabulary is DRAFT / PLANNED, driven by actual stop count (0 vs 1+), not the brief's literal "confirmed" — there is no booking-confirmation field in the data model.
- Flat Material cards (16dp corner radius) with a 1dp outline stroke; elevation is used only to separate the hero card (2dp) from stub cards (0dp). Itinerary's day-tab pills follow a parallel but distinct fill/stroke rule because they sit directly on the navy band itself (see Named Rule below).

## Colors

A fixed navy/white carrier shell sits on top of an otherwise ordinary off-white-paper Material palette that fully inverts for dark mode.

### Primary
- **Ticket Carrier Navy** (`#13213B`, `ticket_carrier_navy` / `md_primary`): the hero card's top band, both screens' header band, and the app's Material primary role. As `ticket_carrier_navy` + `on_ticket_carrier` (`#FFFFFF`) it is fixed in both themes — defined only in `values/colors.xml`, deliberately absent from `values-night/colors.xml` — because it is the direction's one committed shell color, not a role that should invert. Itinerary's header uses these same fixed tokens from the start (not the theme-reactive `?attr/colorPrimary`/`?attr/colorOnPrimary`), carrying forward the fix Home needed. As `md_primary`/`colorPrimary` it is a separate, ordinary theme-reactive Material role (inverts to `#AEB9D1` in dark) governing standard themed components (e.g. the FAB).
- **On Ticket Carrier Translucent** (`#66FFFFFF`, `on_ticket_carrier_translucent`): a 40%-alpha variant of the fixed white on-color, used only as a stroke on Itinerary's unselected day-tab pills. Lives only in `values/colors.xml`, same as its parent token — it is a derivative of the fixed shell color, not a new theme-reactive role, so it does not get a `values-night` counterpart either.

### Secondary
- **Booked Amber** (`#8A5B00` light / `#FFB95C` dark, `md_secondary`): accents tied to booked/planned state (e.g. the PLANNED badge's container). Ordinary Material role, inverts normally.

### Neutral
- **Paper** (`#FBFBF9` light / `#121419` dark, `md_background` / `md_surface`): the base screen and card background.
- **Ink** (`#15181D` light / `#E4E6EA` dark, `md_on_surface`): primary text on paper.
- **Surface Variant** (`#E7E9ED` light / `#2B2E35` dark, `md_surface_variant`): the DRAFT badge's fill.
- **Outline** (`#8B93A6` light / `#6A7180` dark, `md_outline`): card stroke and the perforation divider's dot color.
- **Barcode Bar** (`#8B93A6` light / `#6A7180` dark, `ticket_barcode_bar`): the hero card's barcode strip. Theme-swapped like an ordinary neutral accent — unlike the carrier navy, it is not the signature shell color and correctly inverts.

### Named Rules
**The Fixed Shell Rule.** The carrier navy (`ticket_carrier_navy`, `#13213B`) and its on-color (`on_ticket_carrier`, `#FFFFFF`), plus the translucent on-color derivative (`on_ticket_carrier_translucent`, `#66FFFFFF`), are the color set in this system that does not invert with DayNight. They exist only in `values/colors.xml`. Every other color token, including `md_primary` (which happens to share navy's hex in light mode), is a normal theme role and must be defined in both `values/colors.xml` and `values-night/colors.xml`.

**The On-Navy Contrast Rule.** When a component is placed directly on the fixed navy ground itself (not on paper), the active/selected state fills with `on_ticket_carrier` (white fill, navy text) and the inactive/unselected state outlines with `on_ticket_carrier_translucent` (stroke only, no fill) — never invert to a navy fill when the component already sits on navy. This is why Itinerary's day-tab active pill is a white-filled pill with navy text rather than a navy-filled pill: a navy-on-navy fill would have no contrast. Both states still resolve from only the two fixed-shell tokens; no third color was introduced to solve this.

## Typography

**Display/Title Font:** Roboto (Material `textAppearanceTitleLarge` / `textAppearanceTitleMedium`, system default)
**Body Font:** Roboto (Material `textAppearanceBodySmall`)
**Label/Mono Font:** monospace (system face via `android:fontFamily="monospace"`) — no bundled font files; per Operate-mode guidance this surface is well served by the system stack.

**Character:** Ordinary Material prose carries the narrative (trip name, subtitle); a tabular monospace face is reserved for anything that is counted or stated as a status, giving those fields a ledger/manifest feel without turning the whole screen into a "technical" pastiche.

### Hierarchy
- **Title** (500 weight, 22sp hero / 16sp stub, Material `textAppearanceTitleLarge`/`TitleMedium`): trip name on Home; trip name in Itinerary's header.
- **Body** (400 weight, 12sp, Material `textAppearanceBodySmall`): itinerary subtitle line on both screens.
- **Label (mono)** (400 weight, 11-12sp, `android:fontFamily="monospace"` over Material `textAppearanceLabelLarge`/`LabelSmall`): hero band's day/stop counts, DRAFT/PLANNED status badges (hero and stub), stub cards' day/stop count chips, and Itinerary's day-tab numbers. This is the only place monospace appears.

### Named Rules
**The Mono-Means-Data Rule.** `android:fontFamily="monospace"` is applied only to time/count/status fields (status badges, day/stop counts, day-tab numbers). It is never applied to a title, a label, or body prose — monospace is a data marker, not a generic "technical" decoration. Itinerary's day-tab numbers qualify under this rule the same way Home's day/stop counts do: a day number is data, not a label.

## Layout

Home is a standard RecyclerView list of Material cards, laid out and scrolled the ordinary platform way; the ticket world supplies no navigation model or page organization of its own. Cards are full-width minus `space_md` (16dp) horizontal margin, stacked with `space_sm` (8dp) top margin between items, bottom list padding of 96dp to clear the FAB. Internal card padding is `space_md` (16dp); the status/subtitle row and chip row use `space_sm` (8dp) between elements. Exactly one item in the list renders as `item_trip_card_hero.xml`; every other item renders as the plain `item_trip_card_stub.xml`.

Itinerary keeps the same AppBarLayout + RecyclerView + FAB skeleton as Home, with one addition inside the AppBarLayout: a `HorizontalScrollView` wrapping a `LinearLayout` (`day_tab_container`) directly below the title/subtitle block, one pill per distinct day (sorted), horizontally scrollable if the day count exceeds viewport width. Selecting a day filters the stop list below to that day only; day grouping, which previously lived as an inline list header row, now lives entirely in the tab strip — the list itself is a single flat row type with no header view. The existing numbered-stop-card treatment (circle index badge, title, duration meta, amber "Booked" lock badge) is unchanged and predates this world; it is not part of the ticket motif.

## Elevation & Depth

Depth is nearly flat: Material cards use a 1dp `colorOutline` stroke as the primary separation device, not shadow. The one exception is the hero card, which carries `cardElevation="2dp"` to lift it slightly off the stub cards below it (`cardElevation="0dp"`); this is a hierarchy signal (hero vs. everything else), not a general elevation system. Itinerary's day-tab strip carries no elevation of its own — it sits flush inside the navy AppBarLayout band, distinguished by fill/stroke (see Shapes and Components), not by lift.

### Named Rules
**The One Elevation Rule.** Only the hero card elevates (2dp). Stub cards are flat (0dp) and rely on their 1dp outline stroke alone. Do not add elevation to a stub card to make it feel more important — that is what the hero slot is for.

## Shapes

Cards use a 16dp corner radius (`card_corner`) on all four corners, except the hero card's ticket band, which is clipped to top corners only (`bg_ticket_band.xml`: `topLeftRadius`/`topRightRadius` = 16dp, bottom corners 0dp) so the band reads as an integrated header inside the card's rounded shell rather than a separate rectangle. Status and count chips use a 20dp corner radius (`chip_corner_radius`), i.e. fully pill-shaped at their height — the same radius token is reused for Itinerary's day-tab pills, keeping the two screens' "pill" vocabulary identical. Card borders are a uniform 1dp `colorOutline` stroke; the day-tab's unselected state uses a 1dp stroke in `on_ticket_carrier_translucent` instead, since it sits on navy rather than paper.

## Components

### Cards / Containers
- **Corner Style:** 16dp radius (`card_corner`), all corners on stub cards; top-only on the hero's ticket band.
- **Background:** `colorSurface` (paper), themed.
- **Shadow Strategy:** none; see Elevation & Depth. Hero card elevation 2dp, stub 0dp.
- **Border:** 1dp `colorOutline` stroke on every card.
- **Internal Padding:** `space_md` (16dp).

### Chips (status badges / count chips)
- **Style:** DRAFT badge — `colorSurfaceVariant` fill, `colorOnSurfaceVariant` text, monospace, 20dp pill radius. PLANNED badge — `colorSecondaryContainer` fill, same text/radius/font. Day/stop count chips (stub cards) — `bg_chip_count` fill, `colorOnPrimaryContainer` text, monospace, same pill radius.
- **State:** status is derived, not user-toggled — DRAFT when a trip has 0 stops, PLANNED when it has 1+. This is the honest substitute for the surface brief's literal "confirmed," since the data model has no booking-confirmation field.

### Trip Card — Hero (signature component)
The one ticket-stub-styled card in the list, built from `item_trip_card_hero.xml` + `TripAdapter.HeroViewHolder`:
- A navy carrier band (`bg_ticket_band`, `ticket_carrier_navy` fill, top-rounded) holding the trip name and a monospace day/stop count line, both in `on_ticket_carrier` white.
- `PerforationDividerView`: a custom `View` drawing a row of small punch-hole dots in `colorOutline` directly below the band — the literal "tear here" line of a ticket stub.
- A status badge (DRAFT/PLANNED, monospace) plus the itinerary subtitle, in ordinary Material roles.
- `BarcodeStripView`: a custom `View` drawing a deterministic bar pattern in `ticket_barcode_bar`, seeded from `trip.getTripId().hashCode()` so each trip's "barcode" is stable across renders.

### Trip Card — Stub
The plain secondary-tier card for every other trip, built from `item_trip_card_stub.xml`: title, subtitle, status badge, day/stop count chips — no navy band, no perforation, no barcode. It is visually a standard Material card with no ticket ornament at all.

### Day Tab Strip (signature component, Itinerary)
Itinerary's structural addition, built from a `HorizontalScrollView` + `LinearLayout` (`day_tab_container`) inside the header's `AppBarLayout`, populated at runtime from `item_day_tab.xml` pills:
- Each pill is a single `TextView` with `android:fontFamily="monospace"` on the day number, 48dp minimum touch target, `chip_corner_radius` (20dp) shape background.
- **Selected state** (`bg_day_tab_selected.xml`): solid `on_ticket_carrier` white fill, navy text — reads as the one "stamped" leg among the row.
- **Unselected state** (`bg_day_tab_unselected.xml`): transparent fill, 1dp `on_ticket_carrier_translucent` stroke only — visible against the navy ground without competing with the selected pill.
- Governed by the On-Navy Contrast Rule (see Colors): because the strip lives physically inside the fixed-navy band rather than on paper, fill/stroke assignment inverts relative to a paper-ground chip — this was reviewed and confirmed correct, not a drift from the system.
- Selecting a tab (`MainActivity`'s `selectedDay` state) filters the stop list to that day; day grouping no longer uses an inline list header (`item_day_header.xml` removed), `ItineraryRow`/`ItineraryAdapter` simplified to a single flat row type.

## Do's and Don'ts

### Do:
- **Do** keep the carrier navy (`#13213B`), its white on-color, and the on-color's translucent stroke derivative (`#66FFFFFF`) fixed across light and dark — all three live only in `values/colors.xml`, never in `values-night/colors.xml`.
- **Do** restrict `android:fontFamily="monospace"` to count/status/data fields (day/stop counts, DRAFT/PLANNED badges, day-tab numbers) — titles, subtitles, and body text stay on the Material/Roboto scale.
- **Do** render the ticket motif (navy band, `PerforationDividerView`, `BarcodeStripView`) on exactly one hero card per list; every other trip renders as the plain stub layout with zero ticket ornament.
- **Do** derive status from real data (stop count 0 vs 1+ → DRAFT vs PLANNED) rather than a field the data model doesn't have.
- **Do** apply the On-Navy Contrast Rule whenever a new component sits directly on the fixed navy ground: active/selected fills with `on_ticket_carrier`, inactive/unselected outlines with `on_ticket_carrier_translucent` — never the reverse.

### Don't:
- **Don't** apply the ticket band, perforation divider, or barcode strip to more than one card in the list — repeating the signature motif as general decoration is the one rule this world cannot survive breaking.
- **Don't** let `md_primary`/`colorPrimary` and the fixed `ticket_carrier_navy`/`on_ticket_carrier` pair drift into being treated as the same token — one inverts with DayNight, the other must never be defined in `values-night/colors.xml`.
- **Don't** fill a selected pill (or any selected state) with navy when the component already sits on a navy ground — there is no contrast; use the white fill / translucent-stroke pairing instead.
- **Don't** extend this documented system to Add Destination, New Trip, or Map Picker — those screens have no surface brief or built direction yet; this DESIGN.md covers Home and Itinerary only.
- **Don't** treat the unverified tablet/expanded-width layout as settled — only phone-viewport (Pixel 6 / API 34) builds exist for both Home and Itinerary; no tablet AVD or `cmdline-tools`/`avdmanager` is available on this dev machine, so neither Home's nav-rail-at-expanded-width behavior nor Itinerary's day-tab wrap/scroll/rail behavior at expanded width is confirmed.
