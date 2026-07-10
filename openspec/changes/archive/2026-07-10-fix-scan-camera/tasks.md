---
phase: fix-scan-camera
description: Fix camera detection bugs in scan view
domains: [scan]
total-tasks: 7
effort-estimate: XS (7 tasks, 1 file)
---

# Tasks — fix-scan-camera

## Context

All changes in one file: `client/src/components/Viewfinder.tsx`. No server changes, no new dependencies.

6 fixes needed:
1. Camera fallback chain — try `environment` first, catch OverconstrainedError, retry `{ video: true }`
2. 7 distinct error messages per error type (currently 2 generic messages)
3. Reset scan state after decode (safe-guard in Viewfinder)
4. zxing dynamic import `.catch()` handler (currently unhandled rejection)
5. Video readyState timeout (10s polling loop timeout)
6. Permissions pre-check via `navigator.permissions.query()` before getUserMedia
7. Clean up camera stream on errors and unmount

---

### T-camera-01: Add camera fallback chain

- **Description**: Wrap getUserMedia call in try-catch. On OverconstrainedError, retry with `{ video: true }` (no facingMode). On second failure, fall through to error handling.
- **Files affected**:
  - `client/src/components/Viewfinder.tsx`
- **Dependencies**: None
- **Complexity**: XS
- **Acceptance criteria**:
  1. OverconstrainedError triggers `{ video: true }` retry
  2. Second call succeeds when camera is available
  3. No behavioral change on devices where `environment` works
- **Evidence**: Open scan view on iPad. Front camera appears without error. Open on phone with rear camera — no change.

### T-camera-02: Replace 2 generic error messages with 7 distinct messages

- **Description**: Map all 5 error types (NotAllowedError, NotFoundError, NotReadableError, SecurityError, OverconstrainedError) plus timeout and general-unknown to distinct user-facing strings with actionable guidance. OverconstrainedError maps to a silent internal fallback (no user message). Add a constant map for the 6 user-visible messages.
- **Files affected**:
  - `client/src/components/Viewfinder.tsx`
- **Dependencies**: T-camera-01 (error structure changes)
- **Complexity**: S
- **Acceptance criteria**:
  1. Each of the 6 shown error types produces a different message
  2. Each message mentions the specific error and an actionable fix
  3. OverconstrainedError produces no user message (handled internally by fallback)
- **Evidence**: Stub each error type in browser console, verify distinct message renders in DOM.

### T-camera-03: Add zxing dynamic import .catch() handler

- **Description**: Add `.catch()` to `await import('@zxing/library')`. On failure, render manual entry form instead of crashing. Set a state flag so the view shows manual entry UI.
- **Files affected**:
  - `client/src/components/Viewfinder.tsx`
- **Dependencies**: None
- **Complexity**: XS
- **Acceptance criteria**:
  1. Blocking the zxing script produces no unhandled promise rejection
  2. Manual entry form replaces viewfinder on import failure
- **Evidence**: Chrome DevTools -> Network tab -> Block `@zxing/library`. Open scan view. Verify manual entry form visible and no console error.

### T-camera-04: Add video readyState timeout (10s)

- **Description**: Add a timeout to the polling loop that waits for `video.readyState >= 2`. If 10 seconds pass without reaching HAVE_CURRENT_DATA, stop polling and show timeout message. Define timeout as a tunable constant at module scope.
- **Files affected**:
  - `client/src/components/Viewfinder.tsx`
- **Dependencies**: T-camera-02 (error display mechanism)
- **Complexity**: S
- **Acceptance criteria**:
  1. After 10s with readyState stuck at 0, timeout message appears
  2. Message is distinct from all other error messages
  3. Timeout constant is defined as a named module-level const
- **Evidence**: Stub `video.readyState = 0` via browser console. Wait 10s. Verify timeout message appears on screen.

### T-camera-05: Add permissions pre-check

- **Description**: Before calling getUserMedia, check `navigator.permissions.query({ name: 'camera' })`. If state is `'denied'`, show NotAllowedError message without making getUserMedia call. Feature-detect the Permissions API — skip silently if unsupported.
- **Files affected**:
  - `client/src/components/Viewfinder.tsx`
- **Dependencies**: T-camera-02 (error message display)
- **Complexity**: XS
- **Acceptance criteria**:
  1. When camera permission is denied, no getUserMedia call is made
  2. Error message appears faster than without pre-check
  3. On Safari (no Permissions API), behavior is unchanged
- **Evidence**: Revoke camera permission in browser settings. Open scan view. Verify no getUserMedia call in Network tab. Verify error message appears.

### T-camera-06: Reset scan state after decode

- **Description**: After a successful barcode decode, reset the scanning state internally so Viewfinder is ready for the next scan. This guards against edge cases where App.tsx unmount-recycle doesn't trigger.
- **Files affected**:
  - `client/src/components/Viewfinder.tsx`
- **Dependencies**: None
- **Complexity**: XS
- **Acceptance criteria**:
  1. After decode completes, Viewfinder can decode another barcode without navigating away
  2. No infinite re-scan of the same barcode (deduplication by code value)
- **Evidence**: Scan a barcode. Before the auto-nav fires, scan a second barcode. Verify the second decode fires.

### T-camera-07: Clean up camera stream on errors and timeout

- **Description**: Ensure `track.stop()` is called on the media stream in all exit paths: error, timeout, and component unmount. Add a cleanup function that stops all tracks.
- **Files affected**:
  - `client/src/components/Viewfinder.tsx`
- **Dependencies**: T-camera-01, T-camera-04
- **Complexity**: XS
- **Acceptance criteria**:
  1. Camera LED turns off within 1 second of error
  2. Camera LED turns off within 1 second of timeout
  3. Camera LED turns off when navigating away from scan view
- **Evidence**: Open scan view on phone with camera LED. Trigger an error. Verify LED turns off. Repeat for timeout. Repeat for navigation.

---

## Verification

- Run `npx tsc --noEmit` — no new TS errors (5 pre-existing errors in Viewfinder.tsx:81 and Login.test.tsx are unrelated and acceptable)
- Run `cd server && JWT_SECRET=test-only-min-32-bytes-long-test-key mvn test` — 61/61 tests pass (no backend regression)

## Notes

- All changes in one file: `client/src/components/Viewfinder.tsx`
- No npm dependencies to add or change
- No server changes
- Do not claim completion in this file — tasks describe what must be done
