# User Stories — Fix Scan Camera

UX/UI-focused user stories for improving camera detection reliability in barcode scanning.

---

## Scanning Camera Reliability

### P0

**As a** shop-floor staff member using an iPad, **I want** the scan view to automatically use the front-facing camera when rear camera is unavailable, **so that** I can still scan barcodes without switching devices.

> **Acceptance:** When `getUserMedia` is called with rear camera constraints and fails with OverconstrainedError, the app silently falls back to `{ video: true }` and shows a live front-camera feed. Staff can successfully scan barcodes using the front camera.

**As a** manager setting up the app on a desktop or laptop, **I want** to see a clear error message when the camera requires HTTPS, **so that** I understand why it won't work and can ask IT to enable SSL.

> **Acceptance:** When opening the app over HTTP (not HTTPS), camera permission request fails with SecurityError. The view shows "Camera requires a secure connection (HTTPS). Contact your IT team to enable HTTPS on this site." with a manual entry button visible.

**As a** staff member scanning products, **I want** to scan multiple products in quick succession without the view freezing after each scan, **so that** I can efficiently process shipments or inventory.

> **Acceptance:** After a barcode is decoded and the product lookup completes, the viewfinder automatically resets within 2 seconds. Staff can scan the next barcode without navigating away or manually restarting the camera.

**As a** staff member on an unreliable network, **I want** the scan view to gracefully degrade to manual entry if the barcode library fails to load, **so that** I can still look up products when the camera feature is unavailable.

> **Acceptance:** If the zxing polyfill fails to load (network error, blocked script), the app shows a manual barcode entry form instead of crashing or showing a spinner forever.

**As a** staff member whose camera is in use by another app, **I want** to see a clear error explaining why the camera won't start, **so that** I can close the conflicting app and try again.

> **Acceptance:** When camera is in use by another tab or application, the app catches NotReadableError and shows "Camera is in use by another application or browser tab. Close the other tab and try again." Manual entry remains available.

**As a** staff member who denies camera permission, **I want** to see an actionable message without the view crashing, **so that** I can easily enable permission in my browser settings.

> **Acceptance:** When camera permission is denied, NotAllowedError is caught. The view shows "Camera access denied. Enable camera in your browser settings and refresh." Manual entry input is visible alongside the message.

### P1

**As a** staff member with an older device, **I want** to know if the camera stream is stuck (not loading) rather than waiting indefinitely, **so that** I can try a different approach or device.

> **Acceptance:** If `video.readyState` stays below 2 for more than 10 seconds, a timeout message appears: "Camera stream is taking too long to start. Try closing other apps or using a different device." Manual entry remains accessible.

**As a** warehouse staff scanning dozens of items, **I want** the camera stream to be properly released when I leave the scan view, **so that** other apps can use the camera without conflicts.

> **Acceptance:** When navigating away from the scan view or on component unmount, the camera stream is properly stopped via `track.stop()`. The camera LED on the device turns off, confirming the stream is released.

**As a** staff member who already denied camera permission, **I want** the app to not repeatedly ask for permission on page refresh, **so that** I'm not annoyed by repeated prompts.

> **Acceptance:** Before calling `getUserMedia`, the app checks `navigator.permissions.query({ name: 'camera' })` where available. If permission is already denied, a friendly message is shown immediately without attempting to open the camera.

### P2

**As a** staff member unfamiliar with mirrored front-camera feeds, **I want** a brief orientation hint on first use, **so that** I understand how to position barcodes correctly.

> **Acceptance:** On first successful front-camera fallback, a subtle one-time overlay appears saying "Using front camera - hold barcode up to screen" that disappears after 3 seconds or on interaction. Preference stored in localStorage to avoid repeated hints.

---

### Summary

| Priority | Stories | Total |
|----------|---------|-------|
| P0 | 6 | 6 |
| P1 | 3 | 3 |
| P2 | 1 | 1 |
| **Total** | **10** |

---

## Related Specifications

- **Scan Spec**: `specs/scan/spec.md` — Barcode decoding requirements
- **PRD**: Parent PRD covers core scanning functionality
- **Design**: `design.md` — Camera component architecture and error handling flow