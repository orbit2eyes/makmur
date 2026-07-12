# PRD: Action-Choice Dialog After Barcode Scan

- **Change**: fix-input-barcode
- **Status**: Proposed
- **Date**: 2026-07-10

---

## Problem

After scanning a barcode, staff see no action-choice dialog. The app makes an assumption about what they want and jumps there immediately, bypassing confirmation or alternatives.

**Found product path:** Staff scan a known barcode. The app jumps directly to ProductCard detail view with a 2-second auto-return timer. If staff wanted to quickly add stock, they must discover the StockControls buttons (increment/decrement) and operate them inside a 2-second window before being auto-returned to the scan view. If they miss the timer, they re-scan the same barcode and repeat the cycle. There is no "add 1 to stock in one tap" shortcut.

**Unknown product path:** Staff scan an unknown barcode. The app jumps directly to ProductForm creation form. Staff may have mis-scanned (wrong barcode number) or picked up wrong packaging, but the form blocks without letting them confirm the barcode first. The only way back is Cancel, which returns to scan — the barcode is lost.

Both paths lack a confirmation dialog with clear choices. Staff are dumped into a UI they may not want, and the auto-return timer adds time pressure that makes mistakes more likely.

## Goals

1. Show a dialog after every successful scan with explicit action choices.
2. Found-product dialog: show product name, offer "Tambah 1 stok", "Lihat detail", "Pindai lagi".
3. Unknown-product dialog: show barcode number, offer "Tambah produk baru", "Pindai lagi".
4. Remove auto-return timer from the found-product path — no time pressure.
5. Frontend-only change. No server modifications.

## Non-goals

- Changes to scan pipeline (Viewfinder, BarcodeDecoder remain unchanged).
- Changes to ProductForm creation form.
- Changes to ProductCard detail card.
- Changes to StockControls component.
- Backend changes of any kind.
- Bulk stock increment (e.g. "add 5") — single-tap increment only.
- Toast/failure feedback for stock increment (rely on existing error handling).

## Users

| Persona | Device | Scenario |
|---------|--------|----------|
| Shop-floor staff | Phone (rear camera) | Scans incoming delivery, needs to add 1 to stock per item quickly |
| Warehouse staff | Rugged Android tablet | Scans pallet barcodes, sometimes needs to view details, sometimes scan next |
| Manager on floor | Phone or tablet | Scans items to check stock, then returns to scanning |

All personas share one need: after scanning, see the product identity confirmed and pick the next action in one tap, without being rushed.

## User Journeys

**J1 — Scan known product:** Staff scans a known barcode. A dialog appears: "Produk ditemukan: [product name]" with three vertically-stacked buttons: "Tambah 1 stok" (primary), "Lihat detail", "Pindai lagi". No timer. Staff chooses an action by tapping once.

**J2 — Scan unknown product:** Staff scans an unknown barcode. A dialog appears: "Barcode tidak dikenal: [barcode number]" with two vertically-stacked buttons: "Tambah produk baru" (primary), "Pindai lagi". Staff chooses an action by tapping once.

**J3 — Tap "Tambah 1 stok":** Staff taps "Tambah 1 stok" on the found-product dialog. The app calls the stock increment API (+1). On success, dialog closes and the view returns to the scan viewfinder for the next scan. On failure, the existing API error is shown.

**J4 — Tap "Lihat detail":** Staff taps "Lihat detail" on the found-product dialog. Dialog closes. The app navigates to ProductCard detail view with full StockControls. No auto-dismiss timer runs. Staff can adjust stock, browse details, or tap a manual "Kembali ke pindai" button when ready.

## Acceptance Criteria

| ID | Criterion | Verification |
|----|-----------|-------------|
| AC1 | No auto-dismiss timer on any dialog — user must tap a button | Scan a known product; wait 10 seconds. Dialog remains visible. No auto-return. |
| AC2 | Found-product dialog shows product name and exactly 3 action buttons | Scan a known product. Verify text "Produk ditemukan: [name]" and buttons "Tambah 1 stok", "Lihat detail", "Pindai lagi". |
| AC3 | "Tambah 1 stok" calls stock increment API (+1), closes dialog, returns to scan viewfinder | Tap "Tambah 1 stok". Verify stock incremented by 1 via detail view. Verify viewfinder is active for next scan. |
| AC4 | "Lihat detail" navigates to ProductCard without auto-dismiss | Scan known product, tap "Lihat detail". Verify ProductCard + StockControls visible. Verify no auto-return timer runs. |
| AC5 | Unknown-product dialog shows barcode number and exactly 2 action buttons | Scan an unknown barcode. Verify "Barcode tidak dikenal: [barcode]" and buttons "Tambah produk baru", "Pindai lagi". |
| AC6 | "Tambah produk baru" opens ProductForm with barcode pre-filled | Scan unknown barcode, tap "Tambah produk baru". Verify ProductForm loaded with the scanned barcode in its barcode field. |
| AC7 | "Pindai lagi" returns to scan viewfinder immediately | On either dialog, tap "Pindai lagi". Verify viewfinder is active and ready for next scan. |
| AC8 | All 61 backend tests still pass | Run `mvn test` in `server/` — 61/61 green. |
| AC9 | No new npm dependencies | `npm ls` diff shows zero new packages. |

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Dialog blocks the scan loop — user must dismiss before next scan | High | Low | All buttons are action-choices, not just "OK" dismiss. Fast path: "Tambah 1 stok" is one tap back to scan. |
| 3 buttons on mobile cramped | Medium | Medium | Use full-width buttons, vertically stacked. Minimum tap target 44px. |
| Staff habit: muscle-memory expects auto-return flow | Medium | Low | Remove timer entirely. The dialog is a clearer, calmer UX after one scan. Staff adapt quickly. |
| Stock increment failure not surfaced clearly | Low | Medium | Rely on existing API error display in the dialog/wrapper layer. No new error UI added. |

## Open Questions

**Q1: Should "Tambah 1 stok" show a brief success indicator (checkmark animation) before returning to scan?**

Current plan: no feedback — silent success, return to scan. A brief indicator adds complexity. If staff report confusion ("did it add?"), add a brief banner or toast.

**Q2: Should there be a "Tambah 5" or custom-quantity variant of the stock increment button?**

Not in v1 (non-goal). If warehouse staff report needing larger increments, add a quantity picker in a follow-up. For typical shop-floor use (scan box, add 1, scan next box), "Tambah 1" covers the majority of actions.

**Q3: Should the dialog persist scan results (barcode) in view so staff can verify before confirming?** The barcode number is shown on the unknown-product dialog. The found-product dialog shows the product name, which is higher-value than the raw barcode number for verification. If needed, add barcode subtitle to found-product dialog.