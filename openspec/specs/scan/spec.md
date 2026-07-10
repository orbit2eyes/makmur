# Domain: scan

## Purpose

The scan domain is the core input method for the Makmur retail inventory webapp. It captures barcodes via the device camera using `navigator.mediaDevices.getUserMedia`, decodes them using the native `BarcodeDetector` API with a `@zxing/library` polyfill for Safari/Firefox, and routes the decoded barcode to product lookup or creation. This specification covers the camera lifecycle, error handling, barcode decoding, and result routing for inventory staff across phone, tablet, and laptop devices.

## Related Specifications

This scan specification interacts with and depends on the following domain specifications:

- **Auth Spec** (`specs/auth/spec.md`): All scan-related endpoints that access product data are protected by authentication and authorization. The access control rules for product access (which scanning relies on) are defined in the auth spec's REQ-auth-003 and REQ-auth-004 requirements.
- **Product Spec** (`specs/product/spec.md`): Scanning functionality exists to look up or create products. The scan spec's requirements directly implement the product lookup (REQ-product-003) and creation (REQ-product-001) requirements, particularly the barcode-based product lookup and duplicate prevention logic.
- **Stock Spec** (`specs/stock/spec.md`): While scanning itself does not modify stock, the typical workflow after scanning involves viewing or updating stock information. The stock specification defines how stock levels are viewed and updated.
- **Product Requirements Document** (`prd.md`): This scan specification implements the barcode scanning requirements outlined in PRD Section 5 (Scanning), particularly addressing acceptance criteria AC-02 through AC-05 and AC-13.
- **Design Document** (`design.md`): The technical decisions regarding camera access, barcode decoding libraries, and UI flow described in this spec align with and implement the scanning architecture outlined in the design document.
## Requirements
### Requirement: REQ-scan-001 - Camera initialization with facingMode fallback

- Description: The app SHALL request camera permission using `navigator.mediaDevices.getUserMedia` on first access. It SHALL first attempt `{ video: { facingMode: 'environment' } }` to prefer the rear camera. If `OverconstrainedError` is thrown (no rear camera available), it SHALL fall back silently to `{ video: true }` (any available camera). A live camera viewfinder SHALL be displayed once permission is granted. If the camera API is unavailable (`getUserMedia` not supported), a fallback message SHALL be shown explaining the requirement.
- Priority: P0
- Rationale: iPads and laptops lack a rear camera. Without the fallback, the scan view fails silently on these devices, which is the most common support complaint.
- Change: Original behavior hardcoded `{ video: { facingMode: 'environment' } }` with no fallback. New behavior catches `OverconstrainedError` and retries with `{ video: true }`.
- Acceptance criteria: AC1

#### Scenario: S-scan-007 - Staff on device with front camera only (iPad)

**Given** the device has only a front-facing camera (no rear camera)
**When** the staff member opens the scan view
**Then** `getUserMedia` with `facingMode: 'environment'` throws `OverconstrainedError`
**And** the app falls back to `{ video: true }` — the front camera stream starts
**And** the viewfinder shows the (mirrored) front camera feed, and scanning works

### Requirement: REQ-scan-002 - Camera error handling with distinct messages

- Description: If the camera stream cannot be obtained, the app MUST check the error type and show a distinct, actionable error message:

  | Error Type | User-Facing Message |
  |------------|-------------------|
  | `NotAllowedError` | "Camera permission denied. Enable camera access in browser settings, or use manual entry below." |
  | `NotFoundError` | "No camera found on this device. Use manual entry below to enter the barcode." |
  | `NotReadableError` | "Camera is in use by another application or browser tab. Close the other tab and try again." |
  | `SecurityError` | "Camera requires a secure connection (HTTPS). Contact your IT team to enable HTTPS on this site." |
  | `OverconstrainedError` | (handled internally — triggers `{ video: true }` fallback, not shown to user) |

  A manual barcode entry field SHALL be shown alongside every error state so staff can continue working even if the camera fails entirely.
- Priority: P0
- Rationale: The original 2 generic messages ("camera unavailable", "permission denied") gave staff no way to diagnose or fix the issue. Actionable guidance reduces support tickets and downtime.
- Change: Original had a single catch-all for camera errors plus one for permission denial. New behavior maps 4 distinct error types to 4 distinct messages; `OverconstrainedError` is handled internally (silent fallback).
- Acceptance criteria: AC2

#### Scenario: S-scan-008 - Staff opens scan on HTTP (not HTTPS)

**Given** the app is served over HTTP
**When** the staff member opens the scan view
**Then** `getUserMedia` throws `SecurityError`
**And** the view shows "Camera requires a secure connection (HTTPS). Contact your IT team to enable HTTPS on this site."
**And** a manual entry field is available for barcode input

### Requirement: REQ-scan-003 - EAN-13 barcode decoding

- Description: The app SHALL detect and decode EAN-13 barcodes from the live camera feed. A detected barcode MUST be highlighted in the viewfinder. Decoding SHALL complete within 3 seconds of the barcode coming into view under normal lighting conditions (well-lit, undamaged label, minimal motion blur).
- Rationale: Fast, reliable decoding is the core UX promise. Staff should not wait more than a few seconds for a scan to resolve.
- Acceptance criteria: AC-03

#### Scenario: S-scan-004 - Camera API unavailable

**Given** the device or browser does not support `navigator.mediaDevices.getUserMedia`
**When** the app attempts to initialise the camera
**Then** a fallback message is displayed explaining the camera requirement, and a manual barcode entry field is provided

### Requirement: REQ-scan-004 - Decode failure handling with network resilience

- Description: The `@zxing/library` dynamic import SHALL include a `.catch()` handler. If the polyfill script fails to load (slow network, CDN down, script blocked), the app SHALL show a manual entry form instead of crashing. When the camera stream fails to decode a barcode after a reasonable attempt (poor lighting, damaged label, motion blur), a non-blocking retry prompt MUST be shown with the viewfinder remaining active. Manual barcode entry SHALL always be available as an alternative.
- Priority: P0
- Rationale: If the polyfill fails to load, the current app crashes entirely with an unhandled promise rejection. Staff on slow networks or behind restrictive proxies are blocked from using the scan feature.
- Change: Original had no `.catch()` on the dynamic import. New behavior catches the failure and renders the manual entry form.
- Acceptance criteria: AC4, AC5

#### Scenario: S-scan-010 - Staff on slow network, polyfill fails to load

**Given** the network is slow or the CDN is unreachable
**When** the `@zxing/library` dynamic import fails
**Then** the `.catch()` handler fires
**And** a manual entry form is shown in place of the viewfinder
**And** no crash or blank page occurs

### Requirement: REQ-scan-005 - Scan result routing — product found

- Description: When a barcode is successfully decoded and matches an existing product, the app SHALL transition from the viewfinder to the product detail card showing name, barcode, stock, and price within 1 second of decoding.
- Rationale: The most common scan outcome — fast transition from scan to actionable product data.
- Acceptance criteria: AC-04, AC-13

#### Scenario: S-scan-005 - Decode failure — retry

**Given** the camera viewfinder is active
**When** the camera cannot decode a barcode (poor lighting, motion blur, damaged label)
**Then** a non-blocking retry prompt is shown, the viewfinder remains active, and a manual barcode entry option is available

### Requirement: REQ-scan-006 - Scan result routing — product not found

- Description: When a barcode is successfully decoded but no matching product exists in the catalogue, the app SHALL display a creation prompt: "Product not found. Add it to the catalogue?" along with the detected barcode number. Tapping "Add Product" SHALL open a creation form with the barcode pre-filled.
- Rationale: New or unknown products are a regular occurrence in retail — receiving shipments, new SKUs. The creation flow must be seamless from the scan.
- Acceptance criteria: AC-05

#### Scenario: S-scan-002 - Successful scan — unknown barcode

**Given** the camera viewfinder is active and no product with the scanned barcode exists
**When** the staff member points the camera at a valid EAN-13 barcode not in the catalogue
**Then** the barcode is decoded and a creation prompt is shown with the barcode pre-filled

### Requirement: REQ-scan-007 - Continuous scan support

- Description: After a successful scan and result display, the scan state SHALL reset automatically so the next barcode can be decoded without navigating away. Previously `scannedRef.current` remained set after the first scan, locking the decoder. The camera stream SHALL remain active across scans without re-requesting permission. The app SHALL support an unlimited sequence of scan -> result -> scan cycles.
- Priority: P0
- Rationale: Warehouse staff scanning dozens of items per session need rapid sequential scanning. The current single-scan lock forces a page reload or navigation away and back.
- Change: Original set `scannedRef.current` on decode and never reset it. New behavior resets the scan state after the result is displayed, allowing the decoder to process the next barcode.
- Acceptance criteria: AC3

#### Scenario: S-scan-009 - Staff scans multiple products in succession

**Given** the camera viewfinder is active and a barcode has already been decoded and displayed
**When** the staff member scans a second barcode without navigating away
**Then** `scannedRef.current` is already reset
**And** the second barcode is decoded and its result is displayed
**And** the sequence repeats for any number of scans without interruption

### Requirement: REQ-scan-008 - Multiple sequential scans

- Description: The app SHALL support an unlimited sequence of scan to result to scan cycles. Each scan SHALL be independent; previous results MUST be replaced by the new scan result. The camera stream MUST remain active across scans without re-requesting permission.
- Rationale: A typical shift involves many scans. Re-requesting camera access or reinitialising the camera on every scan would be unacceptable.
- Acceptance criteria: AC-13

#### Scenario: S-scan-008 - Multiple sequential scans

**Given** the staff member has completed a scan and viewed the result
**When** they tap "Scan Another" (or auto-return is enabled)
**Then** the viewfinder reactivates, the camera stream remains active, and the next scan is ready without re-requesting permission or reinitialising the camera

### Requirement: REQ-scan-009 - Video readyState timeout

- Description: A 10-second timeout MUST be added to the polling loop that waits for `video.readyState >= 2` (video data available). If the readyState does not reach `HAVE_CURRENT_DATA` within 10 seconds, the polling MUST stop and a timeout message SHALL be shown: "Camera stream is taking too long to start. Try reloading the page or use manual entry below." The timeout value SHALL be defined as a tunable constant.
- Priority: P0
- Rationale: On slow devices or stalled streams, the view shows an infinite loading spinner with no feedback. The timeout prevents indefinite waiting and gives the user a clear next step.
- Acceptance criteria: AC5

#### Scenario: S-scan-011 - Slow device, video stream never ready

**Given** the device is slow and the video readyState never reaches 2
**When** the staff member opens the scan view
**Then** the polling loop runs for 10 seconds without `readyState >= 2` being met
**And** the timeout message "Camera stream is taking too long to start. Try reloading the page or use manual entry below." is shown
**And** the manual entry form is available

### Requirement: REQ-scan-010 - Camera permission pre-check

- Description: Before calling `getUserMedia`, the app SHOULD check `navigator.permissions.query({ name: 'camera' })` where the API is supported (feature-detected). If the permission state is `denied`, the app SHALL show the `NotAllowedError` message without making a `getUserMedia` call. If the permission API is unsupported (Safari, older browsers), the check MUST be skipped silently and `getUserMedia` SHALL be called directly.
- Priority: P1
- Rationale: When permission is already denied, calling `getUserMedia` triggers an unnecessary browser prompt or immediate rejection. Checking first avoids redundant browser UI and provides a faster error path.
- Acceptance criteria: AC7

#### Scenario: S-scan-012 - Permission already denied, getUserMedia not called

**Given** the browser has camera permission set to `denied` for this origin
**When** the staff member opens the scan view
**Then** `navigator.permissions.query({ name: 'camera' })` returns state `denied`
**And** the error message "Camera permission denied. Enable camera access in browser settings, or use manual entry below." is shown
**And** `getUserMedia` is never called

## Data Model

No additional data model. The scan domain reads from the `product` data model and does not persist scan-specific records in v1.
