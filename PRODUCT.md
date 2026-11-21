# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Independent travelers and small groups planning multi-stop trips — city tours, road trips, multi-day itineraries — who want an optimized, budget-aware plan. This includes travelers who have not yet decided what to visit and rely on Discover Mode to generate a starting itinerary from just a destination city.

## Product Purpose

Trippings folds itinerary building, route optimization, calendar sync, and budget tracking into one app, so travelers stop juggling separate tools (maps, a spreadsheet, a currency converter, blog posts, their calendar) for each slice of trip planning. Success means a user can go from "where do I want to go" to a finalized, budgeted, calendar-synced plan inside one app.

## Positioning

No existing app combines route optimization, calendar sync, budget-aware recommendations, currency conversion, and destination discovery in one place: Google Trips is discontinued, TripIt only organizes bookings already made, and Wanderlog/Google My Maps list places without sequencing them efficiently or accounting for budget. Trippings' mechanism is the pipeline itself — stops feed route optimization, which feeds the day-by-day builder, which feeds calendar sync and the budget tracker — rather than any single feature in isolation.

## Operating Context

- Term project for ITISMOB: Mobile Development, De La Salle University, College of Computer Studies, Department of Information Technology, under Prof. Joel Andrew B. Cruz Jr., MSIT. Proposal submitted 2026-09-26.
- Team: Cris Jemuel T. Dela Cruz, Raphael Benedict C. Maagma, Kevin Mathew V. Mariposa, Julia Kirsten G. Palomo.
- Five-sprint plan, 105 story points: Sprint 1 Foundations, Sprint 2 Calendar & Budget, Sprint 3 Optimization (heaviest, 26 pts), Sprint 4 Discover Mode, Sprint 5 Enhancements.
- Current build state: UI scaffolding only (Login, Home, manual Add Destination screens) with no persistence, auth backend, or API integration wired up yet — consistent with early Sprint 1.
- Full product proposal and backlog: `docs/TripFit-Proposal-and-Backlog.md`.

## Capabilities and Constraints

**Scope commitment:** full Trippings (all 19 backlog items) is committed for this term. The proposal's trimmed fallback, Trippings Lite (called TripFit Lite in the proposal; manual itinerary + calendar sync + budget tracking on 2-3 APIs, dropping route optimization and the recommendation engine), stays as a guardrail only if the term runs short — not the current plan.

In-scope capabilities, by epic:

- **User Management:** Google Sign-In (OAuth), reusing the same connection for Calendar access — no separate account system.
- **Itinerary Planning:** add destination by search or map pin; route optimization via OpenRouteService Matrix API; day-by-day itinerary builder with manual reordering; booked-activity lock-in; smart buffer time; per-leg transport mode (walk / cycle / drive / transit).
- **Discover Mode:** suggest famous and hidden-gem spots from a city name alone (Foursquare Places + Wikivoyage), feeding into the route pipeline.
- **Calendar Integration:** one-click push of the finalized itinerary to Google Calendar as timed events with location.
- **Budget Management:** overall or per-category budget setup; running spend-vs-budget tracker; currency conversion (Frankfurter API); budget-aware food/activity suggestions.
- **Trip Enhancements:** weather-aware rescheduling (OpenWeather); trip summary export (PDF / shareable); group trip mode (shared trips, voting, cost-splitting); photo/journal log.

Technical constraints:

- Client-only architecture orchestrating third-party APIs (OpenRouteService, Google Directions, Foursquare Places, Google Calendar, Frankfurter, Wikivoyage, OpenWeather) — no in-house backend beyond route-ordering logic.
- Minimum data model: `Trips` (user, name, date range, home_currency, budget) · `ItineraryItems` (trip_id, name, date, time, location, is_booked) · `Expenses` (trip_id, itinerary_item_id, amount, currency, converted_amount, category).
- Java (not Kotlin) despite `.kts` Gradle build scripts; classic View system — AppCompat, Material Components, ConstraintLayout, RecyclerView — no Jetpack Compose; minSdk 24, targetSdk/compileSdk 37.

## Evidence on Hand

Full proposal and 19-item backlog with sprint plan: `docs/TripFit-Proposal-and-Backlog.md`. No testimonials, case studies, press, or real user/trip data exist yet — none should be fabricated.

## Product Principles

- One pipeline, not five apps: destination input, route optimization, calendar sync, and budget tracking read as one continuous flow, never stitched-together tools.
- Undecided travelers are a first-class case: Discover Mode exists so a user with just a city name gets a usable starting itinerary, not only users who already have a full stop list.
- Booked reality wins: fixed-time, already-booked activities anchor the schedule; everything else is built around them, never the reverse.
- Budget awareness is continuous, not a setup step: spend-vs-budget and currency conversion stay visible as the plan evolves, not buried in a settings screen.

## Accessibility & Inclusion

Standard Android accessibility baseline (content descriptions, touch target sizing, color contrast) — no formal named standard or course-rubric requirement has been set beyond that.
