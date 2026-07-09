# Routing Feasibility MVP

Phase 4a adds warnings-only travel feasibility checks to backend day-plan responses. It helps users see impossible or tight transitions between scheduled items without changing schedule generation.

## What It Does

- Snapshots task locations onto day-plan items as `addressIdSnapshot` and `addressTextSnapshot`.
- Computes transitions between consecutive relevant day-plan items when `DayPlanResponse` is built.
- Adds non-persisted transition data to day-plan responses.
- Displays compact travel notices in the Schedule page Today timeline.

## What It Does Not Do Yet

- No scheduler placement changes.
- No moving or unscheduling tasks because of travel time.
- No Google Maps, live traffic, route optimization, or routing-service integration.
- No travel notifications, Kafka, push, mobile routing, or turn-by-turn navigation.

Active travel-aware scheduling belongs to Phase 4b.

## Location Resolution

The MVP uses day-plan item snapshots:

1. `addressIdSnapshot`
2. `addressTextSnapshot`
3. unknown location

Free-time blocks are ignored unless they have an explicit location snapshot. A user-reserved `Free time` block does not necessarily represent a physical place, so it should not create noisy missing-location warnings by default.

## Travel Estimate Rules

- Same non-null address id: `0` minutes.
- Same normalized non-blank address text: `0` minutes.
- Missing location on either side: unknown travel time.
- Different known locations: default `30` minutes.

Address text is normalized by trimming, lowercasing, and collapsing repeated whitespace.

## Warning Order

For each transition, the backend first computes `availableMinutes`.

1. Negative gaps are `INSUFFICIENT_TRAVEL_TIME` with an overlap message.
2. Missing locations are `MISSING_LOCATION`.
3. Same locations are `SAME_LOCATION`.
4. Different known locations use the default 30-minute estimate.
5. Too-short gaps are `INSUFFICIENT_TRAVEL_TIME`.
6. Gaps with 5 minutes or less remaining buffer are `TIGHT_TRAVEL_TIME`.
7. Comfortable gaps are `FEASIBLE`.

This ordering ensures same-location checks do not hide overlapping schedule items.

## Response Shape

`DayPlanResponse.transitions` is always present and defaults to an empty list when fewer than two relevant items exist. Transition results are computed on response creation and are not persisted.

## Schema Handling

The scheduling service currently uses Hibernate `ddl-auto: update` in local configuration, so the new day-plan item snapshot fields rely on the existing local schema update behavior. No new migration framework is introduced in Phase 4a.

## Known Limitations

- Default travel time is intentionally coarse.
- `UNKNOWN_TRAVEL_TIME` is reserved for future estimators; the MVP usually reports missing location, same location, or default-estimated travel.
- Existing `tightSpotSummary` remains time-gap based and separate from travel feasibility.
- The frontend shows warnings only; it does not offer routing actions.

## Phase 4b Boundary

Phase 4b can use this transition data as a foundation for active travel-aware scheduling, such as avoiding impossible transitions, moving flexible tasks, integrating a routing provider, or clustering errands. None of that belongs to Phase 4a.
