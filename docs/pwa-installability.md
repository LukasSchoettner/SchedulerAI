# Android PWA Installability

Phase 5b targets Android Chrome and Chromium-based mobile browsers. The goal is to make the existing web app installable and app-like on Android without changing backend deployment or adding offline editing.

iOS Safari install behavior is out of scope for this phase. The app may still load as a normal website in iOS browsers, but iOS-specific install behavior, Apple meta tags, and Safari instructions are not supported here.

## Manifest And Icons

The frontend provides `web/public/manifest.webmanifest`.

Important values:

- `name`: `SchedulerAI`
- `short_name`: `Scheduler`
- `start_url`: `/home`
- `scope`: `/`
- `display`: `standalone`
- `orientation`: `portrait`
- `theme_color`: `#1565c0`
- `background_color`: `#ffffff`

Android icons live in `web/public/icons/`:

- `icon-192.png`
- `icon-512.png`
- `maskable-icon-512.png`

`web/index.html` only uses generic PWA metadata:

- manifest link
- theme-color

No Apple mobile web app meta tags are added in Phase 5b.

## Service Worker Caching

The service worker is registered from the frontend bootstrap only in production builds. This avoids development-cache confusion while still allowing Android Chrome to detect the PWA after deployment.

The service worker uses a versioned app-shell cache. On activation, old cache versions are deleted.

Cached:

- static JS/CSS build assets
- manifest
- icons
- frontend shell assets
- an app-shell fallback for navigation requests

Not cached:

- `/tasks`
- `/day-plans`
- `/scheduling`
- `/notifications`
- `/customers`
- `/routing`
- `/api`
- `/auth`

Authenticated API data is intentionally fetched live. The service worker should help the UI shell load after a successful first visit, but it must not present cached task, day-plan, notification, customer, routing, or scheduler data as fresh.

Offline task editing, offline day-plan editing, API caching, background sync, and conflict handling are future work.

## Install Prompt

The install prompt is Android/Chromium-focused.

Behavior:

- listens for `beforeinstallprompt`
- stores the deferred prompt
- shows a small prompt only after the event fires
- `Install` calls `prompt()`
- hides after install, dismiss, or unsupported state
- persists dismissal in `localStorage` with `scheduler.installPrompt.dismissed`

Unsupported browsers show no install UI. The prompt is mounted inside the authenticated app shell, not the public landing page.

## Offline And Reconnect Banner

The app shows a small status banner when the browser goes offline. It explains that the app shell may still load, but live scheduler data needs a connection.

When the browser reconnects, the app briefly shows a back-online message.

## Hetzner Static Frontend Deployment

Existing Hetzner Level 9 webhosting can be used for the built static React/PWA frontend only.

Build:

```bash
pnpm --dir web run build
```

Output:

```text
web/dist/
```

Deploy by uploading the contents of `web/dist/` to the Hetzner webhosting public directory.

Important:

- Hetzner webhosting is suitable for the built static React/PWA frontend.
- Backend APIs still need to run elsewhere, for example on a VPS or a local backend during testing.
- HTTPS is required for Android PWA installability outside localhost.
- Configure the frontend API base URL correctly for the deployed environment.
- Backend deployment is not implemented in Phase 5b.

## Manual Android Verification

1. Build and serve the frontend.
2. Open the app in Android Chrome or an Android Chromium-based browser.
3. Verify the manifest is detected.
4. Verify the service worker registers.
5. Verify the install prompt appears when browser criteria are met.
6. Install to the Android home screen.
7. Launch the installed app.
8. Confirm it starts at `/home`.
9. Confirm standalone display mode.
10. Toggle offline after first load.
11. Confirm the app shell still loads.
12. Confirm API data is not falsely shown as freshly updated.
13. Reconnect and confirm Quick Add, Today, Schedule, Notifications, and Tasks work online.

Desktop Chrome DevTools checks are useful, but Android Chrome behavior is the target.

## Known Limitations

- Native Android is not implemented.
- Push notifications are not implemented.
- Background sync is not implemented.
- Offline task editing is not implemented.
- Offline day-plan editing is not implemented.
- Calendar sync, maps, route navigation, and location tracking are not implemented.
- Backend deployment is not part of Phase 5b.
