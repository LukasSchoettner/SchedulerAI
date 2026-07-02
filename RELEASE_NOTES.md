# Release Notes

## Release: Hand-In Build

Date: 2026-07-02

## Highlights

- Full local Docker Compose startup for frontend, gateway, backend services, and databases.
- React frontend for account access, tasks, saved locations, customer settings, zone definitions, and schedule generation.
- JWT-based authentication with password hashing for new registrations and password updates.
- Registration rejects duplicate customer names/emails, and login tolerates older duplicate local records without crashing.
- Task management supports create, edit, complete, and delete flows.
- Scheduling supports fixed, flexible, and project tasks.
- Zone definitions can be created, edited, deleted, and scoped to the authenticated customer.
- Address creation respects membership limits.
- Backend unit tests were added for core product rules.

## Verified Commands

The following checks have passed during release preparation:

```powershell
mvn test
```

```powershell
cd web
pnpm run build
```

```powershell
docker compose build
docker compose up -d
```

Live checks:

- Web app returned HTTP 200 at `http://localhost:5173`
- Protected scheduling endpoint returned HTTP 401 without a token, as expected
- Fresh registration and login returned authentication tokens through the API gateway
- Docker Compose reported all services running

## Service URLs

- Web app: `http://localhost:5173`
- API gateway: `http://localhost:8080`
- Task service: `http://localhost:8081`
- Customer service: `http://localhost:8082`
- Notification service: `http://localhost:8083`
- Routing service: `http://localhost:8084`
- Scheduling service: `http://localhost:8085`

## Test Coverage Added

- Authentication accepts correct passwords and rejects wrong/inactive users.
- Customer registration hashes passwords and profile updates do not self-upgrade membership.
- Zone activation and definition updates enforce customer ownership.
- Address limits enforce Basic and Plus membership boundaries.
- Project task scheduling places tasks into available slots and respects due dates.

## Known Risks

- Existing Docker volumes may contain older local accounts. Fresh registration is recommended for evaluation if old login data is stale.
- Notification workflows are scaffolded but not a complete user-facing notification product.
- Routing is deterministic and local; it does not call a live routing provider.
- There is no CI pipeline file in the repository; verification is local command based.
