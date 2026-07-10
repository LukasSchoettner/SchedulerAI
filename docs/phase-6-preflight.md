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

4. Start an HTTPS tunnel to the API gateway port, normally `8080`. See [Start The Ngrok Tunnel](#start-the-ngrok-tunnel).

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

## Start The Ngrok Tunnel

Start the backend stack first. The API gateway must be reachable locally at:

```text
http://localhost:8080
```

Then start ngrok against port `8080`:

```powershell
ngrok http 8080
```

Expected ngrok output should look like:

```text
Forwarding https://your-ngrok-url.ngrok-free.dev -> http://localhost:8080
```

The HTTPS forwarding URL is the value for `VITE_API_URL`.

Set `VITE_API_URL` before building the frontend because Vite embeds it at build time:

```powershell
$env:VITE_API_URL="https://your-ngrok-url.ngrok-free.dev"
pnpm --dir web run build
```

Do not use `http://localhost:8080` as `VITE_API_URL` for the deployed Hetzner frontend. A phone opening `https://imagine-him-happy.de` cannot reach your computer's `localhost`.

Do not tunnel PostgreSQL ports. Do not tunnel individual service ports. Tunnel only the API gateway port `8080`.

## Quick Command Flow

Terminal 1:

```powershell
$env:APP_CORS_ALLOWED_ORIGINS="https://imagine-him-happy.de,http://localhost:5173"
docker compose up --build
```

Terminal 2:

```powershell
ngrok http 8080
```

Terminal 3:

```powershell
$env:VITE_API_URL="https://your-ngrok-url.ngrok-free.dev"
pnpm --dir web run build
```

Then upload the contents of `web/dist/` to Hetzner.

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

## Troubleshooting

| Problem | Cause | Fix |
| --- | --- | --- |
| ngrok forwards to `http://localhost:80`. | ngrok was started without the correct port or with the wrong port. | Stop ngrok with `Ctrl+C` and restart it with `ngrok http 8080`. |
| Deployed frontend cannot call backend. | `VITE_API_URL` was built with the wrong URL, ngrok is stopped, backend stack is not running, or CORS does not include `https://imagine-him-happy.de`. | Restart backend, restart ngrok on port `8080`, rebuild frontend with the HTTPS ngrok URL, redeploy `web/dist/`, and verify `APP_CORS_ALLOWED_ORIGINS`. |
| CORS error in browser console. | `APP_CORS_ALLOWED_ORIGINS` does not include the exact frontend origin. | Set `APP_CORS_ALLOWED_ORIGINS=https://imagine-him-happy.de,http://localhost:5173` and restart the backend stack. |
| Login request goes to `https://imagine-him-happy.de` instead of the ngrok URL. | `VITE_API_URL` was not set before build, or the old frontend build is still deployed/cached. | Set `VITE_API_URL` to the ngrok HTTPS forwarding URL, rebuild, redeploy `web/dist/`, and hard-refresh or clear PWA/browser cache if needed. |
| `docker compose` fails with invalid interpolation format for `APP_CORS_ALLOWED_ORIGINS`. | Docker Compose uses `${VAR:-default}`, not Spring's `${VAR:default}` syntax. | `docker-compose.yml` must use `APP_CORS_ALLOWED_ORIGINS: "${APP_CORS_ALLOWED_ORIGINS:-http://localhost:5173}"`, while `application.yml` keeps `${APP_CORS_ALLOWED_ORIGINS:http://localhost:5173}`. |
| Login succeeds, but dashboard/user data does not load. Firefox Network shows `CORS Missing Allow Origin` for `onboarding-status`, `notifications/unread`, `notifications/due`, `preferences`, `zones/active`, or `tasks`. | Gateway CORS may not allow `Authorization` or required headers, CORS may not allow `OPTIONS`/`GET` correctly, the gateway container may not have been recreated after changing `APP_CORS_ALLOWED_ORIGINS`, the frontend may have been built with the wrong `VITE_API_URL`, ngrok free may return an HTML warning/interstitial instead of JSON, or the browser origin may not exactly match `APP_CORS_ALLOWED_ORIGINS`. | Start backend with `APP_CORS_ALLOWED_ORIGINS=https://imagine-him-happy.de,http://localhost:5173`, recreate the `api-gateway` container, confirm `docker compose exec api-gateway printenv APP_CORS_ALLOWED_ORIGINS`, confirm ngrok forwards to `localhost:8080`, confirm the frontend was built with `VITE_API_URL` set to the ngrok HTTPS URL, ensure gateway CORS explicitly allows `Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`, `ngrok-skip-browser-warning`, and `OPTIONS`, and rebuild/redeploy if needed. |

## Known Limitations

- This is not production deployment.
- The backend is temporarily exposed through an HTTPS tunnel.
- PostgreSQL remains local/private.
- The frontend build contains the tunnel URL configured at build time.
- If the tunnel URL changes, rebuild and redeploy the frontend.
- No backend VPS setup is implemented in this phase.
- No real credentials or secrets should be committed.
