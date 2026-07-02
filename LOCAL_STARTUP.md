# Local Startup

## Prerequisites

- Docker Desktop must be running.
- Java 21 and Maven are useful for local backend builds.
- Node is only required if you want to run the frontend outside Docker.

## One-command Docker startup

From the project root:

```powershell
docker compose up --build
```

Then open:

- Web app: http://localhost:5173
- API gateway: http://localhost:8080

Main service ports:

- Task service: http://localhost:8081
- Customer service: http://localhost:8082
- Notification service: http://localhost:8083
- Routing service: http://localhost:8084
- Scheduling service: http://localhost:8085

PostgreSQL ports:

- Task DB: localhost:5433
- Notification DB: localhost:5434
- Customer DB: localhost:5435
- Routing DB: localhost:5436

## If Docker cannot connect

Start Docker Desktop first, then run:

```powershell
docker compose config
docker compose up --build
```

## If container name conflicts appear

Older versions of this project used fixed container names. The current Compose file lets Docker manage project-scoped names automatically. If an old container still blocks startup, stop the current stack and remove only the old conflicting container, for example:

```powershell
docker compose down
docker rm -f api-gateway postgres-routing
docker compose up -d
```

## If old database volumes cause startup issues

This removes local container data for this project:

```powershell
docker compose down -v
docker compose up --build
```

## Backend-only local build

```powershell
mvn package -DskipTests
```

## Frontend-only local build

If `npm` is installed:

```powershell
cd web
npm ci
npm run build
```

In this Codex environment, `pnpm` with the bundled Node runtime was used instead because global `npm` was not available.
