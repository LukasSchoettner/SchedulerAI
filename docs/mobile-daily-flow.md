# Mobile Daily Flow MVP

Phase 5a makes the existing web app easier to use on a phone during a real day. It keeps the current routes, backend APIs, and full task creation flow intact.

## Daily Flow

- `/home` remains the authenticated Today/Home route.
- `/` remains the public landing page.
- `/schedule` remains the detailed schedule workspace.
- `/tasks` remains the full task creation and editing flow.

On mobile, the bottom navigation exposes Today, Schedule, Quick Add, Tasks, and Notifications. The center `+` opens Quick Add Task.

## Quick Add Task

Quick Add is for fast capture when a task appears during the day.

- opened from the mobile `+`
- requires only a title
- supports compact optional fields: category, estimated duration, priority, due date, location text, and `Schedule today`
- creates a normal flexible task through the existing `POST /tasks` endpoint
- does not force a deadline by default
- does not force `latestEndDateTime` by default
- does not force `earliestStartDateTime` by default

Default quick-add behavior:

- `type = FLEXIBLE`
- `status = PENDING`
- normal priority
- existing default category
- fixed-estimate flexible task fields only where the current API expects them

## Full Add Task

The full Add Task flow still lives on `/tasks`.

Use the full flow for:

- fixed appointments
- recurring tasks
- project-style tasks
- advanced scheduling fields
- reminders and detailed location handling
- editing existing tasks

Quick Add includes `More options`, which opens `/tasks`. If carrying the typed Quick Add title into the full editor is not implemented, the user can still continue in the existing full task wizard.

## Schedule Today

`Schedule today` is explicit and defaults off.

When off:

- Quick Add saves the task normally.
- It does not set `earliestStartDateTime`.
- It does not set `dueDate`.
- It does not set `latestEndDateTime`.
- It does not regenerate the current day plan.

When on:

- Quick Add may set `earliestStartDateTime` to now, using existing accepted task fields.
- `Save and regenerate today` becomes the intended action.
- Regeneration uses only the existing safe day-plan regeneration callback when available.

No new backend scheduler endpoint is added in Phase 5a.

## Notifications And Follow-Ups

Notifications remain in-app and polling-based. The mobile nav uses the existing notification center and unread count.

The follow-up prompt is optimized for small screens with the actions:

- Yes, done
- No, reschedule
- Partly done
- Dismiss

Follow-up behavior continues to use the existing day-plan item completion and reschedule APIs.

## Known Limitations

- Phase 5a is still a web app experience, not a native mobile app.
- Push notifications are not included.
- Offline sync is not included.
- PWA installability and service worker support are postponed to Phase 5b.
- Quick Add does not include recurrence, dependencies, project setup, attachments, voice input, OCR, AI parsing, calendar import, or bulk editing.
- Save-and-regenerate depends on an existing page-level regeneration callback. If that callback is unavailable, the UI tells the user to regenerate the day plan manually.
