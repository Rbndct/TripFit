# TripFit — Project Proposal & Product Backlog

_As of 2026-10-13 · Raphael Benedict C. Maagma_

> Consolidated from the ITISMOB project proposal (PDF) and the TripFit product backlog (CSV).
> Live doc: https://claude.ai/code/artifact/225683f2-d4ca-4531-9964-9af450f24218

## Project Overview

TripFit is a smart trip-planning app that puts itinerary building, route optimization, calendar sync, and budget tracking in one tool, so travelers stop juggling separate apps for each.

It is the term project for **ITISMOB: Mobile Development** at De La Salle University (College of Computer Studies, Department of Information Technology), under Prof. Joel Andrew B. Cruz Jr., MSIT. Proposal submitted September 26, 2026.

**Team:** Cris Jemuel T. Dela Cruz, Raphael Benedict C. Maagma, Kevin Mathew V. Mariposa, Julia Kirsten G. Palomo.

The proposal weighs two scopes: the full **TripFit** (route + budget optimization with destination discovery) and a trimmed **TripFit Lite** (manual itinerary + calendar + budget on 2-3 APIs). The backlog and sprint plan below track the full scope.

## Problem & Motivation

Existing travel apps each solve only a slice of trip planning, so no single tool covers it end to end:

- Google Trips is discontinued.
- TripIt only organizes bookings you have already made.
- Wanderlog and Google My Maps let you list places but do not sequence them efficiently or account for budget.

No app combines route optimization, calendar sync, budget-aware food and activity recommendations, currency conversion, and destination discovery in one place. Travelers end up juggling Google Maps, a spreadsheet, a currency converter, blog posts for "things to do," and their calendar separately. TripFit's goal is to fold all of that into one mobile app.

## Objectives & Scope

Deliver one mobile app that takes a traveler from "where do I want to go" to a finalized, budgeted, calendar-synced plan.

**Objectives**

1. Sequence multi-stop trips in the most time-efficient order.
2. Build a day-by-day schedule around already-booked activities.
3. Push the finalized itinerary to the user's Google Calendar.
4. Track spend against budget with automatic currency conversion.
5. Help undecided users discover places via famous and hidden-gem suggestions.

**In scope:** the 19 backlog functions below, grouped into User Management, Itinerary Planning, Discover Mode, Calendar Integration, Budget Management, and Trip Enhancements.

**Scope guardrail:** if the full build proves too large for one term, the proposal defines a fallback **TripFit Lite** that drops route optimization and the recommendation engine, keeping a manual itinerary builder, calendar sync, and budget tracking on just 2-3 APIs. A minimum viable stack of ~5 APIs covers the core proposal end to end.

## Target Users

Independent travelers and small groups planning multi-stop trips — city tours, road trips, and multi-day itineraries — who want an optimized, budget-aware plan.

This explicitly includes travelers who have not yet decided what to visit, who lean on Discover Mode to generate a starting itinerary from just a destination city.

## Key Features

**Core**

- **Destination input & route optimization** — add stops by search or map pin; auto-calculate travel time between them and order them to minimize total travel.
- **Discover Mode** — from just a city name, suggest a ready-made itinerary split into famous/must-see spots and underrated/hidden gems, which flow into the route pipeline.
- **Day-by-day itinerary builder** — split the optimized route across trip days, with manual reordering and booked-activity lock-in.
- **Calendar sync** — one-click push of the finalized itinerary to Google Calendar as timed events with location.
- **Budget-aware recommendations** — set an overall or per-category budget; get nearby restaurant and activity suggestions filtered by price, with a running spend-vs-budget tracker.
- **Currency conversion** — auto-convert foreign prices to the user's home currency.

**Enhancements**

- **Set duration per stop** and **smart buffer time** for realistic scheduling.
- **Per-leg transport mode** (walk / cycle / drive / transit) chosen independently per leg.
- **Weather-aware rescheduling** that flags outdoor stops on bad-forecast days.
- **Trip summary export** (PDF / shareable), **group trip mode** (shared trips, voting, cost-splitting), and a **photo / journal log**.

## Technical Approach & Stack

TripFit is an API-driven client that orchestrates several third-party services; no heavy backend algorithm is built in-house beyond the route-ordering logic.

| Service / API | Purpose |
| --- | --- |
| OpenRouteService Matrix API | Travel time/distance between all destination pairs (powers route optimization) |
| OpenRouteService Directions API | Walking / cycling / driving routing per leg |
| Google Directions API | Public-transit routing for transit legs |
| Foursquare Places API | Destination search; nearby restaurants/activities with rating, price tier, reviews |
| Google Calendar API | Push itinerary as timed events (OAuth, user's own calendar) |
| Frankfurter (Currency Exchange API) | Convert prices to the user's home currency |
| Wikivoyage API | Community content for hidden-gem suggestions in Discover Mode |
| OpenWeather API | Per-day forecast to flag weather conflicts |

**Minimum viable stack:** route matrix + places + calendar + a currency API + Wikivoyage = ~5 APIs covering the core proposal.

**Data model (minimum):** `Trips` (user, name, date range, home_currency, budget) · `ItineraryItems` (trip_id, name, date, time, location, is_booked) · `Expenses` (trip_id, itinerary_item_id, amount, currency, converted_amount, category).

**Open question:** the proposal's description calls TripFit "web-based," but this is a mobile-development (ITISMOB) term project — the target platform (native Android, cross-platform, or mobile web) should be confirmed before Sprint 1.

## Product Backlog

19 backlog items across 6 epics. All are status **To Do**, owned by Kevin Mariposa. Points are drawn from the sprint plan below.

| # | Epic | Feature | Priority | Pts | What it does |
| --- | --- | --- | --- | --- | --- |
| 1 | User Management | Google Sign-In Authentication | High | setup | Sign in with Google OAuth; reuses the Calendar connection, no separate account |
| 2 | Itinerary Planning | Add Destination | High | 5 | Add a stop by search or map pin; stores coordinates for routing |
| 3 | Itinerary Planning | Route Optimization | High | 13 | Order stops to minimize total travel time via the Matrix API |
| 4 | Itinerary Planning | Set Duration Per Stop | Medium | 3 | Optional stay time per stop; defaults by place type |
| 5 | Itinerary Planning | Day-by-Day Itinerary Builder | High | 8 | Split the route across trip days with manual reordering |
| 6 | Itinerary Planning | Booked Activity Lock-In | Medium | 5 | Fix booked items at set times; schedule the rest around them |
| 7 | Itinerary Planning | Smart Buffer Time | Low | 5 | Auto-add realistic gaps between stops |
| 8 | Itinerary Planning | Per-Leg Transport Mode Selection | Medium | 5 | Choose walk / cycle / drive / transit per leg |
| 9 | Discover Mode | Discover Suggested Itinerary | Medium | 8 | Suggest famous and hidden-gem spots from a city name |
| 10 | Calendar Integration | Calendar Sync | High | 8 | One-click push of the itinerary to Google Calendar |
| 11 | Budget Management | Budget Setup | High | 3 | Set overall or daily budget, optionally by category |
| 12 | Budget Management | Running Budget Tracker | Medium | 5 | Real-time spend vs. remaining budget by category |
| 13 | Budget Management | Currency Conversion | Medium | 5 | Convert prices to home currency |
| 14 | Budget Management | Budget-Aware Food Suggestions | Medium | 5 | Nearby restaurants filtered by food budget |
| 15 | Budget Management | Budget-Aware Activity Suggestions | Low | 5 | Nearby activities filtered by activity budget |
| 16 | Trip Enhancements | Weather-Aware Rescheduling | Low | 5 | Flag outdoor stops on bad-weather days; suggest indoor swaps |
| 17 | Trip Enhancements | Trip Summary Export | Low | 3 | Export itinerary + budget as PDF / shareable summary |
| 18 | Trip Enhancements | Group Trip Mode | Low | 8 | Shared trips, destination voting, cost-splitting |
| 19 | Trip Enhancements | Photo / Journal Log | Low | 3 | Attach notes/photos per stop for a trip journal |

## Milestones & Timeline

Five sprints, 105 story points total. Each sprint delivers a usable slice, front-loading the itinerary core and deferring enhancements to the end.

| Sprint | Focus | Functions | Pts |
| --- | --- | --- | --- |
| 1 | Foundations | Database & Project Setup, Add Destination, Day-by-Day Builder, Booked Activity Lock-In | 21 |
| 2 | Calendar & Budget | Calendar Sync, Budget Setup, Running Budget Tracker, Currency Conversion | 21 |
| 3 | Optimization | Route Optimization, Set Duration Per Stop, Smart Buffer Time, Transport Mode Toggle | 26 |
| 4 | Discover Mode | Discover Suggested Itinerary, Budget-Aware Food Suggestions, Budget-Aware Activity Suggestions | 18 |
| 5 | Enhancements | Weather-Aware Rescheduling, Trip Summary Export, Group Trip Mode, Photo / Journal Log | 19 |

Sprint 3 is the heaviest (26 pts) because route optimization alone is 13. If the term runs short, Sprints 4-5 are the natural trim toward the TripFit Lite scope.
