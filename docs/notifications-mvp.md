# Notifications MVP

Phase 3 adds database-backed in-app notifications. Notifications are stored in `scheduling-service` because day-plan generation, confirmation, follow-up actions, and the scheduling database already live there.

## What It Does

- Stores customer-owned notifications with read, unread, dismissed, and expired states.
- Shows unread notifications in the frontend notification center.
- Lets users mark notifications as read, mark all as read, or dismiss them.
- Creates day-plan confirmation notifications when a generated plan needs review.
- Creates upcoming-task notifications when a day plan is confirmed.
- Creates follow-up notifications for flexible scheduled items.
- Creates one unscheduled-task summary notification when tasks cannot be placed.
- Creates a simple plan-changed notification on regeneration.

## What It Does Not Do Yet

- No Kafka or Hazelcast.
- No WebSockets or SSE.
- No email, mobile push, native app, or PWA service worker.
- No routing or travel-time notifications.
- No full audit/event history.
- No complex background orchestration.

## Types And Statuses

MVP notification types:

- `DAY_PLAN_CONFIRMATION_NEEDED`
- `TASK_STARTING_SOON`
- `FOLLOW_UP_DUE`
- `UNSCHEDULED_TASKS`
- `PLAN_CHANGED`
- `REMINDER_DATE_REACHED`

Statuses:

- `UNREAD`
- `READ`
- `DISMISSED`
- `EXPIRED`

`FOLLOW_UP_DUE` is the real MVP follow-up notification. `FLEXIBLE_TASK_ENDED` is intentionally not created in Phase 3.

## Duplicate Prevention

Duplicate prevention is handled in service code instead of relying only on SQL equality. This matters because related ids may be null.

The notification service treats two notification identities as equal when all of these match, including both-null values:

- `customerId`
- `type`
- `relatedTaskId`
- `relatedDayPlanId`
- `relatedDayPlanItemId`

Only duplicate `UNREAD` notifications are suppressed.

## Day-Plan Lifecycle

When a day plan is generated and still needs review, the backend creates one `DAY_PLAN_CONFIRMATION_NEEDED` notification. It does not also create a separate `DAY_PLAN_READY` notification.

When a day plan is confirmed:

- `TASK_STARTING_SOON` is created for upcoming active items.
- The default offset is 10 minutes before the scheduled start.
- If that reminder time is already in the past but the task has not started, the notification is created as immediately due.
- If the task already started or ended, the start notification is skipped.
- `FOLLOW_UP_DUE` is created for active flexible items with `dueAt` equal to the scheduled end.

When a flexible item is completed or rescheduled, its follow-up state is persisted and matching unread follow-up notifications are dismissed.

When a plan is regenerated, the MVP always creates `PLAN_CHANGED` and dismisses future item notifications for replaced or inactive items. It does not perform complex plan-diff analysis.

## Frontend Delivery

Notifications are delivered by polling:

- Load unread notifications when the app layout mounts.
- Load due notifications when Home or Schedule opens.
- Poll every 60 seconds while the app is open.

This is intentionally simpler than push delivery and works without WebSockets, SSE, or mobile infrastructure.
