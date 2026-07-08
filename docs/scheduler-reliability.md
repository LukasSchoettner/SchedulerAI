# Scheduler Reliability Notes

Phase 2 makes scheduler behavior easier to trust before adding notifications, routing, mobile/PWA work, or real-life testing loops.

## Scheduling Profiles And Planning Windows

A Scheduling Profile is the active set of scheduling rules for a customer. Backend compatibility names still use `ZoneConfiguration` and `/customers/zones/**`.

A Planning Window is a time rule inside a Scheduling Profile. Backend compatibility names still use `ZoneDefinition`.

- Preferred: try the main focus and also-allowed categories first, then allow other suitable flexible tasks.
- Strict: allow only the main focus and also-allowed categories, unless urgent override is enabled.
- `ALLOW_ELSEWHERE`: target categories may use fallback/default scheduling time.
- `PREFER_INSIDE_WINDOW`: currently behaves like `ALLOW_ELSEWHERE` for eligibility; lower outside-window ranking is future work.
- `KEEP_INSIDE_WINDOW`: target categories are excluded from fallback/default scheduling time.

Fixed tasks remain anchors. Planning Windows guide flexible tasks; they do not make whole categories fixed.

## Category Importance And Effective Priority

Category importance is a scheduling preference used only at calculation time. It does not write inherited priority back into tasks.

Default importance:

- Work: 3
- Duty: 3
- Health: 3
- Social: 2
- Sport: 2
- Leisure: 2
- Education: 3

Phase 2 MVP priority rule:

- non-null positive `task.priority` is treated as manual priority;
- missing or zero task priority inherits category importance;
- custom categories fall back to Normal / 2.

`EffectivePriorityCalculator` applies deadline pressure and caps at 5. A Highest/Very Important category maps to base 4, not permanent 5. Due-today and overdue work may boost effective priority to 5.

Future improvement: add `prioritySource = USER | CATEGORY_DEFAULT | SYSTEM` if task priority semantics become more complex.

## Urgent Override

Strict Planning Windows use effective priority for urgent override checks. A task can enter an unrelated strict window only when urgent override is enabled and its effective priority reaches the configured threshold, currently priority 5.

## Unscheduled Reasons And Explanations

The scheduler now produces lightweight backend/test-visible unscheduled reports and explanations.

Reason codes:

- `NO_AVAILABLE_SLOT`
- `DURATION_TOO_LONG`
- `OUTSIDE_ALLOWED_WINDOW`
- `AFTER_LATEST_END`
- `BEFORE_EARLIEST_START`
- `CONFLICTS_WITH_FIXED_TASK`
- `UNKNOWN`

Explanations are intentionally short and are not a full audit trail. They are meant to make tests and debugging clearer.

### Known Phase 2 limitations

- `PREFER_INSIDE_WINDOW` currently behaves like `ALLOW_ELSEWHERE` for scheduling eligibility. It expresses intent, but does not yet apply stronger outside-window ranking.
- Legacy allowed/excluded-only `CategoryEvaluator` override logic still uses raw `task.priority`. Generalized Planning Windows use effective priority in `MasterScheduler`.
- Unscheduled reason inference is intentionally lightweight. Some cases, especially fixed-task conflicts, may appear as `NO_AVAILABLE_SLOT`, `DURATION_TOO_LONG`, or `OUTSIDE_ALLOWED_WINDOW` rather than a precise `CONFLICTS_WITH_FIXED_TASK`.

## Running Reliability Checks

```powershell
mvn test
```

```powershell
pnpm --dir web run test
pnpm --dir web run build
```

Seed scenarios for repeatable manual testing live in `docs/seed-scenarios/`.
