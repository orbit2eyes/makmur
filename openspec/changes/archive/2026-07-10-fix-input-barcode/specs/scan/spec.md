# Domain: scan

## Purpose

The scan domain covers the barcode scanning pipeline — camera access, barcode decoding, and result routing. After a barcode is decoded, the result routing step SHALL present an action-choice dialog so the user can decide what to do next, rather than auto-navigating to a single view.

## MODIFIED Requirements

### Requirement: REQ-scan-005 - Scan result routing with action-choice dialog — product found

- Description: When a barcode is successfully decoded and matches an existing product, the app SHALL show a dialog instead of navigating directly to any view. The dialog SHALL display the product name and three action buttons: "Tambah 1 stok" (primary), "Lihat detail", and "Pindai lagi". No auto-return timer SHALL run on the dialog — the user MUST tap a button to proceed. If the user taps "Tambah 1 stok", the app SHALL call the stock increment API (+1) and return to the scan viewfinder. If the user taps "Lihat detail", the app SHALL navigate to the ProductCard detail view with full StockControls and no auto-dismiss. If the user taps "Pindai lagi", the app SHALL return to the scan viewfinder immediately.
- Priority: P0
- Rationale: Previously the app jumped directly to the detail card with a 2-second auto-return timer, making stock actions hard to discover and adding time pressure. A dialog gives the user a clear choice without rushing.
- Change: Original behavior displayed the ProductCard detail view with a 2-second auto-return timer and no intermediate dialog. New behavior shows an action-choice dialog with no timer.
- Acceptance criteria: AC1, AC2, AC3, AC4, AC7

#### Scenario: S-scan-012 - Found product shows action dialog

**Given** a product with barcode "5901234567890" exists in the catalogue
**When** the staff member scans that barcode
**Then** a dialog appears showing "Produk ditemukan: [product name]"
**And** three buttons are visible: "Tambah 1 stok", "Lihat detail", "Pindai lagi"
**And** no auto-return timer is active

### Requirement: REQ-scan-006 - Scan result routing with action-choice dialog — product not found

- Description: When a barcode is successfully decoded but no matching product exists, the app SHALL show a dialog instead of navigating directly to the creation form. The dialog SHALL display the barcode number and two action buttons: "Tambah produk baru" (primary) and "Pindai lagi". If the user taps "Tambah produk baru", the app SHALL navigate to ProductForm with the barcode pre-filled. If the user taps "Pindai lagi", the app SHALL return to the scan viewfinder immediately.
- Priority: P0
- Rationale: Previously the app jumped straight to the creation form with no confirmation. Staff who accidentally scanned the wrong barcode had to Cancel and re-scan. A confirmation dialog reduces accidental product creation.
- Change: Original behavior navigated directly to ProductForm with no intermediate dialog. New behavior shows a confirmation dialog first.
- Acceptance criteria: AC5, AC6, AC7

#### Scenario: S-scan-013 - Unknown barcode shows action dialog

**Given** no product with barcode "5901234567890" exists
**When** the staff member scans that barcode
**Then** a dialog appears showing "Barcode tidak dikenal: 5901234567890"
**And** two buttons are visible: "Tambah produk baru", "Pindai lagi"

## Data Model

No change. The scan domain does not persist data.

## Related Specifications

- **Product Spec** (`specs/product/spec.md`): The scan dialog navigates to ProductCard (detail) and ProductForm (create) depending on user choice. These components are unchanged.
- **Stock Spec** (`specs/stock/spec.md`): The "Tambah 1 stok" button calls the stock increment API. This flow is unchanged.