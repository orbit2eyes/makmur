# PRD: Reliable Camera Detection for Barcode Scanning

- **Change**: fix-scan-camera
- **Status**: Proposed
- **Date**: 2026-07-09

---

## Problem

Staff open the scan view to check stock and the camera just does not start. On an iPad it silently fails — no rear camera, no scan. On a laptop used for desktop setup the same thing. When the camera is already in use by another app or tab, the error reads "camera unavailable" with no hint why. After a successful scan the view locks — the next product cannot be scanned without navigating away and back. If the network is slow and the barcode library fails to load, the entire view crashes rather than showing the manual entry form. Staff waste time reloading, switching devices, or finding someone else's phone. The scan feature is the core of the app; when it does not work on the first try, trust erodes fast.

## Goals

1. **Fall back gracefully** — scan view starts on any device with any camera (front or rear) without crashing.
2. **Diagnose camera errors** — every distinct failure mode shows a distinct message with actionable guidance.
3. **Enable continuous scanning** — after one barcode is decoded, the user can scan the next product without leaving the view.
4. **Survive network failures** — if the barcode polyfill fails to load, the app shows manual entry instead of crashing.
5. **Timeout gracefully** — if the video stream is stuck (slow device, stalled readyState), the user sees a clear timeout message rather than an infinite spinner.
6. **Respect permissions** — check camera permission state before requesting the stream, where the browser supports it.

## Non-goals

- Camera switching UI (allow user to pick which camera to use mid-scan).
- Torch or flash control.
- Device enumeration as a user-visible feature.
- Any server-side changes (backend is stable, 61/61 tests passing).
- New npm dependencies.

## Users / Personas

| Persona | Device | Scenario |
|---------|--------|----------|
| Shop-floor staff | Phone (rear camera) | Scans incoming stock, checks shelf prices |
| Warehouse staff | Rugged Android tablet | Scans pallet barcodes in rapid succession |
| Manager/admin | iPad or laptop | Sets up the app on devices shared by the team |
| Casual user | Unknown device, unknown browser | Opens the app, tries scan for the first time |

All personas share one need: the camera works on the first try with no config, no reload, no workaround.

## User Journeys

### J1 — Staff on iPad (no rear camera)

Staff opens the scan view on an iPad. The app detects no environment-facing camera, falls back to the front camera. The viewfinder shows the front-facing feed. Staff holds barcode up to the screen — scan works. Staff scans the next product. No error, no reload.

### J2 — Staff opens app on http:// (not https)

Staff opens the internal deployment URL over plain HTTP. Camera permission request fails with SecurityError. The view shows a clear message: "Camera requires a secure connection (HTTPS). Contact your IT team to enable HTTPS on this site." A manual entry button remains visible.

### J3 — Staff with camera in use by another tab

Staff opens the scan view while another tab is using the camera (video call, another scan session). The app catches NotReadableError and shows "Camera is in use by another application or browser tab. Close the other tab and try again." Manual entry is one tap away.

### J4 — Staff scans multiple products

Staff scans a barcode. The decoded barcode appears on screen and triggers a lookup. The view resets automatically. Staff scans the next barcode without touching the screen. Repeat for 30 products in under 2 minutes.

### J5 — Staff on slow network

Staff opens the scan view on a 3G connection. The zxing polyfill script takes 15+ seconds to load. A timeout fires at 10 seconds. The view shows a friendly message asking staff to enter the barcode manually. The manual keypad is presented immediately.

## Acceptance Criteria

| ID | Criterion | Verification |
|----|-----------|-------------|
| AC1 | OverconstrainedError (no rear camera) triggers fallback to `{ video: true }` instead of showing error | Start app on device with front camera only — viewfinder shows front camera stream |
| AC2 | All 5 error types (NotAllowedError, NotFoundError, NotReadableError, SecurityError, OverconstrainedError) show distinct messages with actionable guidance | Stub each error type in dev tools — verify message text differs per type |
| AC3 | After a barcode is decoded and displayed, scan state resets so the next barcode can be scanned without navigation | Scan 2 different barcodes in succession — both decode and trigger lookup |
| AC4 | zxing dynamic import has a `.catch()` handler; on failure, view falls back to manual entry without crashing | Block the zxing CDN script in dev tools — verify manual entry form appears |
| AC5 | Polling loop for `video.readyState >= 2` times out after 10 seconds and shows an error message | Stub `video.readyState` to stay at 0 — verify timeout message after 10s |
| AC6 | Manual entry button or form is always reachable, even when camera stream is active or has failed | Verify manual entry link is visible in all 5 error states and while camera is streaming |
| AC7 | `navigator.permissions.query({ name: 'camera' })` is called before `getUserMedia` where available; if permission is denied, show message without requesting stream | Block camera permission at browser level — verify no `getUserMedia` call is made |
| AC8 | All 61 backend tests still pass after the changes | Run `mvn test` in `server/` — 61/61 green |
| AC9 | No new npm dependencies added | `npm ls` diff shows zero new packages |
| AC10 | Camera stream is properly stopped (`track.stop()`) on unmount, timeout, and error | Unmount the component while streaming — verify camera LED turns off |

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Fix breaks camera on devices where it currently works | Low | High | Run manual regression on 2+ phone models before merging |
| Safari vs Chrome differences in `navigator.permissions` support | Medium | Medium | Wrap permission check in feature detection; silent no-op on Safari |
| Safari non-standard error types for camera failures | Medium | Medium | Map known Safari error strings alongside standard `error.name` |
| HTTPS requirement not clearly communicated to IT teams | Medium | Low | Error message includes actionable instruction for ops team |
| Timeout of 10s too short on very old devices | Low | Low | Timeout value is a tunable constant; adjust if reports come in |

## Open Questions

**Q1: Should front-facing camera be used as secondary fallback or prompt user to switch?**

Current plan: silent fallback to front camera (mirrored feed is still usable for barcode scanning). A prompt adds friction. If users report confusion with the mirrored feed, add a one-time notice.

**Q2: Should we add device enumeration UI to let user pick camera?**

Not in v1 (listed in non-goals). If warehouse staff share devices with built-in + USB cameras, revisit. For the current user base (phone + tablet), auto-fallback covers all cases.
