# Routing Feasibility MVP

Phase 4a adds warnings-only travel feasibility checks to backend day-plan responses. Phase 4b adds a narrow flexible-task candidate filter that avoids known impossible travel placements without changing fixed tasks.

## What It Does

- Snapshots task locations onto day-plan items as `addressIdSnapshot` and `addressTextSnapshot`.
- Computes transitions between consecutive relevant day-plan items when `DayPlanResponse` is built.
- Adds non-persisted transition data to day-plan responses.
- Displays compact travel notices in the Schedule page Today timeline.
- Rejects flexible-task candidate placements when surrounding known locations cannot be reached in time.

## What It Does Not Do Yet

- No fixed-task placement changes.
- No moving fixed commitments because of travel time.
- No Google Maps, live traffic, route optimization, or routing-service integration.
- No travel notifications, Kafka, push, mobile routing, or turn-by-turn navigation.

Full route optimization belongs to a later phase.

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
- Phase 4b uses the existing greedy scheduling approach. Travel filtering can reject known impossible flexible placements, but it does not perform global route optimization. An earlier accepted flexible task may still influence what later tasks can fit. Full route optimization is out of scope.

## Phase 4b: Active Flexible-Task Avoidance

Phase 4b uses the same simple travel estimate from Phase 4a to avoid placing flexible tasks into candidate slots that are known to be travel-impossible.

Fixed tasks remain immovable anchors. Missing-location cases remain warning-only. The scheduler only rejects a flexible candidate slot when both locations are known and the available gap is shorter than the estimated travel time.

If every otherwise valid flexible placement is rejected for known travel infeasibility, the task is reported with `TRAVEL_TIME_CONFLICT`. Fixed-fixed conflicts remain visible as Phase 4a warnings and are not repaired by the scheduler.

This is not route optimization. It does not use Google Maps, routing-service, live traffic, multi-stop planning, or route clustering.
