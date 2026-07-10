---
slug: fix-scan-camera
summary: Reliable camera detection for barcode scanning — graceful fallbacks, distinct error messages, continuous scanning, and network failure survival
domains: [scan]
status: planning
---

## Summary

The barcode scan view fails silently on devices without a rear camera (iPads, laptops) and locks up after one successful scan, forcing staff to reload or switch devices. This change adds camera fallback from `facingMode: 'environment'` to unrestricted `{ video: true }`, replaces 2 generic error messages with 5 distinct actionable messages, fixes the single-scan lock in `scannedRef.current`, adds a `.catch()` handler to the zxing polyfill import, and sets a 10s timeout on the video readyState polling loop.

## Why

Camera detection is the core input path of the app. When it fails on first try — no rear camera, permission denied, camera in use, insecure origin, or network failure for the polyfill — staff cannot scan. Each failure mode needs a distinct diagnostic path. Without these fixes, users on iPads, laptops, or behind restrictive networks are blocked from using the app entirely.

## Problem

Staff open the scan view and the camera does not start. On an iPad it silently fails — no rear camera, no scan. On a laptop the same thing. When the camera is in use by another tab, the error reads "camera unavailable" with no hint why. After a successful scan the view locks — the next product cannot be scanned without navigating away and back. If the network is slow and the barcode polyfill fails to load, the entire view crashes rather than showing manual entry. The scan feature is the core of the app; when it does not work on the first try, trust erodes fast.

## Solution Overview

1. **Camera initialization with OverconstrainedError fallback** — try `{ video: { facingMode: 'environment' } }`, catch `OverconstrainedError`, retry with `{ video: true }` (any camera).
2. **Distinct error messages per failure type** — replace 2 generic messages with 5 distinct messages for `NotAllowedError`, `NotFoundError`, `NotReadableError`, `SecurityError`, and `OverconstrainedError`, each with actionable guidance.
3. **Continuous scan support** — reset `scannedRef.current` after result display so the next barcode can be decoded without navigation.
4. **Network resilience for zxing polyfill** — add `.catch()` handler to the dynamic import; on failure show manual entry form instead of crashing.
5. **Video readyState timeout** — add 10s timeout to the polling loop that waits for `video.readyState >= 2`, with a user-facing timeout message.

## What Changes

1. `Viewfinder.tsx` — camera init now catches `OverconstrainedError` and retries with `{ video: true }`. 5 distinct error messages in a constant map. zxing dynamic import wrapped in try-catch with manual entry fallback. 10s timeout on video readyState polling. All tracks properly stopped on all exit paths.
2. No server changes, no new dependencies.

## Affected Domains

| Domain | Impact | Description |
|--------|--------|-------------|
| scan | MODIFIED | Camera init, error handling, scan state reset, polyfill resilience, readyState timeout |

## Trade-offs Considered

- **Silent vs prompted camera fallback**: Front camera as silent fallback (mirrored feed is usable for barcode scanning). A prompt adds friction with no clear benefit for the current user base. If users report confusion with the mirrored feed, add a one-time notice.
- **Timeout duration (10s)**: Chosen as a balance between patience (slow devices) and perceived responsiveness. Tunable via a constant if reports come in.
- **Permission pre-check before getUserMedia**: `navigator.permissions.query({ name: 'camera' })` is called where available (feature-detected). Safari does not support it, so it is a silent no-op. Avoids unnecessary permission prompts when permission is already denied.
- **No device enumeration UI**: YAGNI. Phone + tablet form factors work with auto-fallback. Add camera picker if warehouse staff use devices with built-in + USB cameras.

## Proposed Implementation Order (Phases)

| Phase | Scope | Files Affected |
|-------|-------|----------------|
| 1 | Camera fallback (OverconstrainedError → `{ video: true }`) + video readyState timeout | `client/src/components/Viewfinder.tsx` |
| 2 | Distinct error messages (5 types, actionable guidance) | `Viewfinder.tsx` |
| 3 | Permission pre-check before getUserMedia | `Viewfinder.tsx` |
| 4 | Continuous scan (fix `scannedRef.current` reset) | `client/src/components/ScanResult.tsx` or decoder component |
| 5 | zxing polyfill `.catch()` handler with manual entry fallback | `client/src/components/BarcodeDecoder.tsx` |
