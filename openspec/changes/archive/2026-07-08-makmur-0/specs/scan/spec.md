# Domain: scan

## Related Specifications
This scan specification interacts with and depends on the following domain specifications:

- **Auth Spec** (`specs/auth/spec.md`): All scan-related endpoints that access product data are protected by authentication and authorization. The access control rules for product access (which scanning relies on) are defined in the auth spec's REQ-auth-003 and REQ-auth-004 requirements.

- **Product Spec** (`specs/product/spec.md`): Scanning functionality exists to look up or create products. The scan spec's requirements directly implement the product lookup (REQ-product-003) and creation (REQ-product-001) requirements, particularly the barcode-based product lookup and duplicate prevention logic.

- **Stock Spec** (`specs/stock/spec.md`): While scanning itself doesn't modify stock, the typical workflow after scanning involves viewing or updating stock information. The stock specification defines how stock levels are viewed and updated, which is the next step after a successful scan in most use cases.

- **Product Requirements Document** (`prd.md`): This scan specification implements the barcode scanning requirements outlined in PRD Section 5 (Scanning), particularly addressing acceptance criteria AC-02 through AC-05 and AC-13.

- **Design Document** (`design.md`): The technical decisions regarding camera access, barcode decoding libraries, and UI flow described in this spec align with and implement the scanning architecture outlined in the design document.

## Requirements

### ADDED

- **REQ-scan-001**: Camera access via getUserMedia
  - Description: The app requests camera permission using `navigator.mediaDevices.getUserMedia` on first access. A live camera viewfinder is displayed once permission is granted. If the camera API is unavailable (`getUserMedia` not supported), a fallback message is shown explaining the requirement.
  - Rationale: Camera access is the primary input method for the entire app — without it, the scanning pipeline cannot function.
  - Acceptance criteria: AC-02

- **REQ-scan-002**: Camera permission denied error handling
  - Description: If the user denies camera permission, a clear error message is shown instructing the user to enable camera access in their browser settings. A manual barcode entry field is provided as a fallback so staff can still look up products by typing the barcode number.
  - Rationale: Staff may accidentally deny permission on first access. The app must not become unusable — a manual fallback keeps the workflow moving.
  - Acceptance criteria: AC-02

- **REQ-scan-003**: EAN-13 barcode decoding
  - Description: The app detects and decodes EAN-13 barcodes from the live camera feed. A detected barcode is highlighted in the viewfinder. Decoding completes within 3 seconds of the barcode coming into view under normal lighting conditions (well-lit, undamaged label, minimal motion blur).
  - Rationale: Fast, reliable decoding is the core UX promise. Staff should not wait more than a few seconds for a scan to resolve.
  - Acceptance criteria: AC-03

- **REQ-scan-004**: Decode failure handling
  - Description: If the camera cannot decode a barcode after a reasonable attempt (poor lighting, damaged label, motion blur), the app shows a non-blocking retry prompt. The viewfinder remains active so the staff can try again without restarting the flow. A manual barcode entry option is available as an alternative.
  - Rationale: Real-world barcodes are not always pristine. The app should degrade gracefully rather than silently failing or freezing.
  - Acceptance criteria: AC-03 (implied — decode succeeds within 3s for valid barcodes)

- **REQ-scan-005**: Scan result routing — product found
  - Description: When a barcode is successfully decoded and matches an existing product, the app transitions from the viewfinder to the product detail card showing name, barcode, stock, and price within 1 second of decoding.
  - Rationale: The most common scan outcome — fast transition from scan to actionable product data.
  - Acceptance criteria: AC-04, AC-13

- **REQ-scan-006**: Scan result routing — product not found
  - Description: When a barcode is successfully decoded but no matching product exists in the catalogue, the app displays a creation prompt: "Product not found. Add it to the catalogue?" along with the detected barcode number. Tapping "Add Product" opens a creation form with the barcode pre-filled.
  - Rationale: New or unknown products are a regular occurrence in retail — receiving shipments, new SKUs. The creation flow must be seamless from the scan.
  - Acceptance criteria: AC-05

- **REQ-scan-007**: Continuous scan support
  - Description: After a successful scan and result display, the app provides a "Scan Another" button that returns to the viewfinder. Optionally, an auto-return mode (continuous scan) is supported where the viewfinder reactivates automatically after a brief result display.
  - Rationale: Warehouse staff scanning dozens of items per session benefit from rapid sequential scanning without tapping between items.
  - Acceptance criteria: AC-13

- **REQ-scan-008**: Multiple sequential scans
  - Description: The app supports an unlimited sequence of scan → result → scan cycles. Each scan is independent; previous results are replaced by the new scan result. The camera stream remains active across scans without re-requesting permission.
  - Rationale: A typical shift involves many scans. Re-requesting camera access or reinitialising the camera on every scan would be unacceptable.
  - Acceptance criteria: AC-13

## Data Model

No additional data model. The scan domain reads from the `product` data model and does not persist scan-specific records in v1.

## Scenarios

- **S-scan-001**: Successful scan — product found
  - **Given** the camera viewfinder is active and a product with barcode "1234567890123" exists in the catalogue
  - **When** the staff member points the camera at a valid EAN-13 barcode "1234567890123"
  - **Then** the barcode is decoded within 3 seconds, and the product detail card (name, barcode, stock, price) is displayed within 1 second of decoding

- **S-scan-002**: Successful scan — unknown barcode
  - **Given** the camera viewfinder is active and no product with the scanned barcode exists
  - **When** the staff member points the camera at a valid EAN-13 barcode not in the catalogue
  - **Then** the barcode is decoded and a creation prompt is shown with the barcode pre-filled

- **S-scan-003**: Camera permission denied
  - **Given** the app has requested camera access
  - **When** the staff member denies the permission request
  - **Then** an error message is displayed explaining how to enable camera access in browser settings, and a manual barcode entry field is shown as a fallback

- **S-scan-004**: Camera API unavailable
  - **Given** the device or browser does not support `navigator.mediaDevices.getUserMedia`
  - **When** the app attempts to initialise the camera
  - **Then** a fallback message is displayed explaining the camera requirement, and a manual barcode entry field is provided

- **S-scan-005**: Decode failure — retry
  - **Given** the camera viewfinder is active
  - **When** the camera cannot decode a barcode (poor lighting, motion blur, damaged label)
  - **Then** a non-blocking retry prompt is shown, the viewfinder remains active, and a manual barcode entry option is available

- **S-scan-006**: Multiple sequential scans
  - **Given** the staff member has completed a scan and viewed the result
  - **When** they tap "Scan Another" (or auto-return is enabled)
  - **Then** the viewfinder reactivates, the camera stream remains active, and the next scan is ready without re-requesting permission or reinitialising the camera