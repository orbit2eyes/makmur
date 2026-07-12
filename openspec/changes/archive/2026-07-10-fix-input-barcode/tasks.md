---
phase: fix-input-barcode
description: Add action-choice dialog after barcode scan
domains: [scan]
total-tasks: 3
effort-estimate: S
---

## Context

This change adds an action-choice dialog after every barcode scan. Currently App.tsx's handleScan() jumps directly to detail view (with 2s auto-return) or creation form. This change adds a new ScanResultDialog component and a new 'scan-result' view state.

## Scope

- New component: `ScanResultDialog.tsx`
- Modified: `App.tsx` (add view state, modify handleScan, add 4 handlers)
- No server changes, no new deps, no changes to Viewfinder/ProductCard/ProductForm/StockControls

## Tasks

### T-dialog-01: Create ScanResultDialog component

- **Description**: New component with two modes ('found' / 'not-found'). Found mode shows product name + 3 buttons (Tambah 1 stok, Lihat detail, Pindai lagi). Not-found mode shows barcode + 2 buttons (Tambah produk baru, Pindai lagi). Styled consistently with existing components using index.css classes.
- **Files affected**: `client/src/components/ScanResultDialog.tsx` (NEW)
- **Dependencies**: None
- **Complexity**: S
- **Acceptance criteria**:
  1. Dialog renders correct title per mode
  2. 3 buttons rendered in found mode
  3. 2 buttons rendered in not-found mode
  4. All buttons are vertically stacked, full-width, min 44px tap target
  5. Props callbacks fire on tap
- **Evidence**: Render in isolation (temporarily mount in App.tsx), verify DOM has correct button count and text.

### T-dialog-02: Modify App.tsx handleScan and view state

- **Description**: Add 'scan-result' to View type union. In handleScan, store scanned data and setView('scan-result') instead of 'detail' or 'create'. Remove setAutoReturn(2) call. Add 4 handler functions (handleAddStock, handleViewDetail, handleAddProduct, handleRescan). Render ScanResultDialog when view === 'scan-result'.
- **Files affected**: `client/src/App.tsx`
- **Dependencies**: T-dialog-01 (ScanResultDialog must exist)
- **Complexity**: M
- **Acceptance criteria**:
  1. Scan known barcode -> dialog appears (not detail card)
  2. Scan unknown barcode -> dialog appears (not creation form)
  3. No auto-return timer fires after scan
  4. handleAddStock: PATCH /api/products/:barcode/stock { delta: 1 } -> on success setView('scan')
  5. handleViewDetail: setView('detail') with selected product
  6. handleAddProduct: setScannedBarcode(barcode), setView('create')
  7. handleRescan: setView('scan')
- **Evidence**: Manual walkthrough: scan known product -> dialog with 3 buttons. Tap each button -> correct view. Scan unknown barcode -> dialog with 2 buttons. Tap each -> correct view.

### T-dialog-03: Verify no regressions

- **Description**: TypeScript compiles clean, backend tests pass, no new npm dependencies, all existing components unchanged.
- **Files affected**: All affected files (client/src/components/ScanResultDialog.tsx, client/src/App.tsx)
- **Dependencies**: T-dialog-01, T-dialog-02
- **Complexity**: S
- **Acceptance criteria**:
  1. `npx tsc --noEmit` -> clean
  2. `mvn test` -> 61/61 passing
  3. `npm ls` -> no new packages
  4. All files outside scope unchanged
- **Evidence**: Run all three checks.

## Notes

- No npm dependencies to add
- No server changes
- After all tasks: npx tsc --noEmit clean, mvn test 61/61