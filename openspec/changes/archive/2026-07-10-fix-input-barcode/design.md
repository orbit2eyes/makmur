# Design: Scan Result Action Choice Dialog

Frontend-only UX fix. After scanning a barcode, app currently jumps directly to ProductCard (2s auto-dismiss) or ProductForm. Both skip user choice. This adds an action-choice dialog after every scan.

---

## 1. Architecture

Add view state `'scan-result'` and component `ScanResultDialog.tsx`. Flow:

```
Barcode decoded → handleScan(barcode) → fetchProduct(barcode)
  ├── product found → setScannedProduct(product) + setView('scan-result') + setMode('found')
  └── not found → setScannedBarcode(barcode) + setView('scan-result') + setMode('not-found')

ScanResultDialog renders based on mode:
  ├── 'found' → "Produk ditemukan: [name]" + 3 buttons
  └── 'not-found' → "Barcode tidak dikenal: [barcode]" + 2 buttons
```

All dialog buttons exit the dialog — no dead-end dismiss.

## 2. Component: ScanResultDialog

**Props:**

| Prop | Type | Description |
|------|------|-------------|
| `mode` | `'found' \| 'not-found'` | Determines which buttons and copy to show |
| `product` | `Product?` | Product data (found mode) |
| `barcode` | `string` | Scanned barcode (not-found mode) |
| `onAddStock` | `() => void` | Increment stock by 1, return to scan |
| `onViewDetail` | `() => void` | Navigate to detail card |
| `onAddProduct` | `() => void` | Navigate to creation form |
| `onRescan` | `() => void` | Return to scan viewfinder |

**Found mode buttons (order):** Tambah 1 stok (primary), Lihat detail, Pindai lagi

**Not-found mode buttons:** Tambah produk baru (primary), Pindai lagi

## 3. App.tsx Changes

- Add `'scan-result'` to `View` type union
- In view switch, render `<ScanResultDialog>` for `'scan-result'`
- In `handleScan`: replace direct navigation with `setView('scan-result')` + store scanned data + set mode
- Remove `setAutoReturn(2)`
- Add handlers: `handleAddStock`, `handleViewDetail`, `handleAddProduct`, `handleRescan`

## 4. Stock Increment Flow

```
handleAddStock:
  PATCH /api/products/:barcode/stock { delta: 1 }
    → success: setView('scan')
    → error: show inline error message on dialog

Button disabled on tap, re-enabled on response (anti-double-tap).
```

## 5. Design Decisions

| Decision | Chosen | Rationale |
|----------|--------|-----------|
| New component vs inline in App.tsx | New ScanResultDialog component | Clean separation, testable in isolation |
| New view state vs modal overlay | New view state 'scan-result' | Consistent with existing pattern (state-based routing), no CSS z-index issues |
| 3 buttons: order | Tambah 1 stok (primary), Lihat detail, Pindai lagi | Most common action first, least common last |
| Auto-return timer on dialog | Removed entirely | No time pressure. User taps when ready. |
| Stock increment error handling | Show inline error on dialog | No new toast/notification component needed |

## 6. File Changes

| File | Change |
|------|--------|
| `client/src/App.tsx` | Add 'scan-result' view state, modify handleScan, add 4 handlers, render ScanResultDialog in view switch |
| `client/src/ScanResultDialog.tsx` (NEW) | Dialog component with two modes, action buttons, inline error state |

No other files changed. Viewfinder, BarcodeDecoder, ProductForm, ProductCard, StockControls all untouched.

## 7. Verification Hooks

| AC | Method |
|----|--------|
| AC1 (no auto-dismiss) | Manual: scan known product, wait 10s, dialog persists |
| AC2 (found dialog: 3 buttons) | Manual: scan known product, verify 3 buttons visible |
| AC3 (Tambah 1 stok -> API call) | Manual: tap +1, check stock increments, returns to scan |
| AC4 (Lihat detail -> no timer) | Manual: tap detail, verify no auto-return on detail card |
| AC5 (unknown dialog: 2 buttons) | Manual: scan unknown barcode, verify 2 buttons |
| AC6 (Tambah produk baru -> form) | Manual: tap add, verify ProductForm with barcode pre-filled |
| AC7 (Pindai lagi -> scan) | Manual: tap rescan, verify viewfinder active |
| AC8 (no regression) | Automated: `mvn test` 61/61 |
| AC9 (no new deps) | Automated: `npm ls` — no new entries |

## 8. Risks

| Risk | Mitigation |
|------|-----------|
| New view state conflicts with existing state machine | 'scan-result' is between 'scan' and 'detail'/'create' — no overlap |
| Stock increment API race condition (double-tap) | Disable button immediately on tap, re-enable on response |
| Dialog blocks scan loop | All buttons exit the dialog — no dismiss-only dead end |