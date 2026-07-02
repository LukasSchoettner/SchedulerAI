# Scheduler Hand-In Guide

## Project Summary

Scheduler is a Dockerized task scheduling application with a React frontend and Spring Boot microservices. Users can register, log in, manage tasks and saved locations, configure scheduling zones, and generate a calendar schedule.

## What Is Included

- React web app served at `http://localhost:5173`
- API gateway at `http://localhost:8080`
- Spring Boot services for customers, tasks, routing, scheduling, and notifications
- PostgreSQL databases for customer, task, routing, and notification data
- Docker Compose startup for the whole stack
- Unit tests for authentication, customer updates, zone ownership, address limits, and project scheduling

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
4. Open `Customer`, create a zone configuration, then add or edit zone definitions.
5. Open `Schedule` and generate the schedule.
6. Click a scheduled calendar item to mark it completed.

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
- Zone configuration and zone definition management
- Schedule generation with fixed/flexible/project task placement
- Calendar view with completion action
- Dockerized local deployment

## Known Limitations

- Notification service is present but not fully productized as real-time user notifications.
- Routing uses an internal distance estimate, not a live external maps API.
- The scheduler is heuristic-based, not a full constraint optimizer.
- There is no admin UI for changing membership tiers.
- Test coverage focuses on critical backend rules; there are no browser automation tests yet.
