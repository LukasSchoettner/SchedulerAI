# Scheduler Hand-In Guide

## Project Summary

Scheduler is a Dockerized task scheduling application with a React frontend and Spring Boot microservices. Users can register, log in, manage tasks and saved locations, configure scheduling preferences/zones, generate a calendar schedule, and review a morning briefing before acting on the plan.

## What Is Included

- React web app served at `http://localhost:5173`
- API gateway at `http://localhost:8080`
- Spring Boot services for customers, tasks, routing, scheduling, and notifications
- PostgreSQL databases for customer, task, routing, scheduling day-plan, and notification data
- Docker Compose startup for the whole stack
- Unit tests for authentication, customer updates, zone ownership, address limits, generalized zone scheduling, pauses, and project scheduling

## Quick Start

Prerequisite: Docker Desktop must be running.

From the project root:

```powershell
docker compose up --build
```

Open:

```text
http://localhost:5173
```

Create a new account through the Register form, then use the app from the Home page.

## Recommended Demo Flow

1. Register a new customer.
2. Open `Tasks` and create a flexible task.
3. Optionally add a location through the task form or the `Locations` page.
4. Open `Scheduler Setup` and configure category ranking, normal planning time, zones, and pause duration.
5. Open `Schedule`; today's plan loads automatically or is generated if none exists yet.
6. Review the morning briefing, confirm the day plan, skip a flexible item if needed, and use `Keep this time free` to reserve a slot as a fixed `Free time` task.
7. Use the action menu on a planned item to mark it completed.

## Verification Commands

Backend tests:

```powershell
mvn test
```

Backend package build:

```powershell
mvn package -DskipTests
```

Frontend build:

```powershell
cd web
pnpm run build
```

Full Docker build/start:

```powershell
docker compose up --build
```

## Login Notes

Registration creates a working account immediately. Existing local accounts from older builds may have stale data in Docker volumes. If login behaves strangely during evaluation, create a fresh account or reset local Docker data:

```powershell
docker compose down -v
docker compose up --build
```

## Main Implemented Features

- Register/login with JWT authentication
- Password hashing for new and updated passwords
- Task create, edit, complete, and delete
- Fixed, flexible, and project task support
- Saved locations with membership-based address limits
- Scheduler onboarding for category priorities, normal planning time, zones, and pauses
- Generalized zone configuration with primary category, secondary categories, strict/preferred behavior, and priority override
- Schedule generation with fixed tasks first and flexible tasks guided by zones, priorities, deadlines, and pauses
- Week/day calendar view with category filters
- Backend-persisted `DayPlan` records for generated and confirmed daily plans
- Morning briefing panel and chronological today's-plan view with fixed commitments, flexible suggestions, skipped/delayed task hints, tight spots, and `Free time` reservation
- Backend-backed briefing actions for confirm, skip today, mark completed, regenerate, and keep time free
- Plan signatures that detect when a regenerated plan differs from the confirmed plan
- Dockerized local deployment

## Known Limitations

- Notification service is present and tasks can store reminder dates, but there is no completed user-facing notification delivery flow yet.
- Routing uses an internal distance estimate, not a live external maps API.
- The scheduler is heuristic-based, not a full constraint optimizer.
- Move-to-another-day, shorten-duration, and replace-task actions are visible but disabled until occurrence/allocation persistence exists.
- Per-occurrence recurrence edits are future work.
- There is no admin UI for changing membership tiers.
- Test coverage focuses on critical backend rules; there are no browser automation tests yet.

## Evening Check Options

The evening check should ask the user which planned tasks were actually finished if they were not marked completed during the day.

Possible implementation paths:

- Next-login review: show a review panel for the latest confirmed plan when the user returns in the evening or the next day.
- Notification-driven review: create an evening notification that opens the review screen.
- Backend day-plan review: use persisted confirmed plans, compare scheduled blocks with task completion status, and ask only about unresolved items.
- Lightweight MVP review: show unresolved `PLANNED`/`KEPT` items from the backend day plan in an evening checklist.

Best next step: build the evening review screen on top of persisted `DayPlan` records so the behavior works reliably across browsers and devices.
