# Phase 6 Preflight Testing

Phase 6 is a short real-life testing setup. The frontend/PWA is deployed to Hetzner webhosting at:

```text
https://imagine-him-happy.de
```

The backend stays on your computer and is exposed temporarily through an HTTPS tunnel to the API gateway. PostgreSQL stays local/private and must not be exposed.

## Important URLs And Environment Variables

Frontend production origin:

```text
https://imagine-him-happy.de
```

CORS origins must be normalized origins without a trailing slash. Use:

```text
https://imagine-him-happy.de
```

Do not use:

```text
https://imagine-him-happy.de/
```

If the browser actually uses the `www` domain, add that exact origin too:

```text
https://www.imagine-him-happy.de
```

Frontend API variable:

```text
VITE_API_URL=https://your-temporary-tunnel-url.example
```

`VITE_API_URL` points to the backend/API gateway, not to `https://imagine-him-happy.de`. For Phase 6 it should usually be the HTTPS tunnel URL for the local API gateway.

Vite embeds `VITE_API_URL` at build time, so set it before running:

```powershell
pnpm --dir web run build
```

API gateway CORS variable:

```text
APP_CORS_ALLOWED_ORIGINS=https://imagine-him-happy.de,http://localhost:5173
```

## Preferred Backend Startup

Use Docker Compose for the backend stack during Phase 6 preflight:

```powershell
docker compose up --build
```

Docker Compose is preferred because the API gateway routes use Docker service names such as `task-service`, `customer-service`, `routing-service`, and `scheduling-service`. Those names resolve correctly inside Docker Compose.

If you run services directly from the host, the gateway route URIs need a local profile that points to localhost service ports instead. Direct host execution is not the default Phase 6 path unless that local profile exists.

## Step-By-Step Preflight

1. Start local PostgreSQL through Docker Compose.
2. Start the backend stack, preferably with Docker Compose.
3. Set CORS for the API gateway:

```powershell
$env:APP_CORS_ALLOWED_ORIGINS="https://imagine-him-happy.de,http://localhost:5173"
docker compose up --build
```

4. Start an HTTPS tunnel to the API gateway port, normally `8080`.

Example only:

```text
https://your-temporary-tunnel-url.example -> http://localhost:8080
```

5. Set the frontend API URL to the HTTPS tunnel URL before building:

```powershell
$env:VITE_API_URL="https://your-temporary-tunnel-url.example"
pnpm --dir web run build
```

6. Upload the contents of `web/dist/` to the Hetzner webhosting public directory.
7. Verify route refreshes in the deployed frontend:

```text
https://imagine-him-happy.de/
https://imagine-him-happy.de/home
https://imagine-him-happy.de/tasks
https://imagine-him-happy.de/schedule
https://imagine-him-happy.de/settings
```

8. Test login through `https://imagine-him-happy.de`.
9. Open the site from Android Chrome.
10. Install the PWA from Android Chrome.
11. Run a fake real-day test:
    - create a dedicated test user;
    - add non-sensitive test tasks;
    - review Home;
    - confirm a day plan;
    - use Quick Add;
    - skip one task;
    - reserve one `Free time` block;
    - mark one task completed;
    - check notifications and travel warnings if present.
12. Stop the HTTPS tunnel when testing is finished.

## Hetzner Static Frontend Deployment

Build the frontend:

```powershell
pnpm --dir web run build
```

Build output:

```text
web/dist/
```

Upload the contents of `web/dist/` to the Hetzner webhosting public directory.

Hetzner webhosting is suitable for the built static React/PWA frontend only. Backend APIs still need to run elsewhere during Phase 6, for example on your local computer behind an HTTPS tunnel.

HTTPS is required for Android PWA installability outside localhost.

## Security Checklist

- Use a dedicated test user.
- Use non-sensitive test data first.
- PostgreSQL stays private/local.
- Do not tunnel or port-forward PostgreSQL ports.
- Expose only the API gateway port.
- Use an HTTPS tunnel only.
- Restrict CORS to `https://imagine-him-happy.de` plus the localhost development origin.
- Do not allow wildcard CORS with credentials.
- Use a strong `JWT_SECRET`.
- Disable or protect the H2 console if present.
- Disable or protect actuator/debug endpoints if present.
- Disable or protect Swagger/OpenAPI endpoints if present.
- Stop the tunnel when finished.
- Do not commit secrets, credentials, or real tunnel URLs.

## Verification Commands

Run before upload:

```powershell
pnpm --dir web run test
pnpm --dir web run build
mvn test
```

After the frontend build, confirm the Hetzner SPA fallback was copied:

```powershell
Test-Path web/dist/.htaccess
```

Expected result:

```text
True
```

## Known Limitations

- This is not production deployment.
- The backend is temporarily exposed through an HTTPS tunnel.
- PostgreSQL remains local/private.
- The frontend build contains the tunnel URL configured at build time.
- If the tunnel URL changes, rebuild and redeploy the frontend.
- No backend VPS setup is implemented in this phase.
- No real credentials or secrets should be committed.
