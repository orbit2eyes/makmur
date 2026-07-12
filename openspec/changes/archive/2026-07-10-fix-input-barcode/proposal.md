---
slug: fix-input-barcode
summary: Add action-choice dialog after barcode scan — choose to add stock, view details, or add new product
domains: [scan]
status: planning
---

## Why

After scanning a barcode, no dialog appears. If the product exists, staff get 2 seconds on the detail card before auto-return to scan — no time to act. If unknown, staff jump straight into a creation form with no confirmation. Both paths skip user choice and add friction.

## What Changes

1. Add a scan result dialog component (`ScanResultDialog.tsx`) that appears after every successful decode
2. Found product: dialog shows product name + 3 buttons — "Tambah 1 stok", "Lihat detail", "Pindai lagi"
3. Unknown barcode: dialog shows barcode number + 2 buttons — "Tambah produk baru", "Pindai lagi"
4. Remove auto-return timer from the found-product path entirely
5. Modify `App.tsx handleScan()` to render the dialog instead of jumping directly to detail/create views
6. No server changes, no new dependencies

## Summary

A frontend-only UX fix: after scanning, show a dialog with explicit action choices instead of auto-navigating to a view the user may not want. Both found and unknown paths get clear, one-tap actions. The auto-return timer is removed — no time pressure.