# PRD: Change UI Language & Currency to Indonesian

## Problem

Makmur was built with English UI and USD ($) as currency. Target users are Indonesian retail staff who think and transact in Rupiah. Price tags in the real world show `Rp` — the app shows `$`. Staff must mentally convert currencies on every scan. Error messages in English are opaque to non-English-speaking staff, causing confusion and support calls. Every screen adds friction that a retail worker should not have to deal with at 8 AM on the shop floor.

## Goals

- All user-facing text rendered in Indonesian.
- Prices display in IDR format: `Rp` prefix, `.` as thousands separator, no decimals for whole amounts.
- No English strings visible during normal operation.
- Currency stored as numeric value in DB — no schema change, only display format changes.

## Non-goals

- **Not** translating server-side error codes — they are machine-readable identifiers. Only the `message` field in error responses will be translated.
- **Not** adding multi-language support (locale switcher). v1 is Indonesian-only. Add a switcher when a second market appears.
- **Not** changing the database schema. `price` stays REAL. No currency column.
- **Not** adding exchange rate conversion. All prices are Rupiah.

## Users / Personas

| Persona | Role | Needs |
|---------|------|-------|
| Shop-floor staff | Frontline retail | Scan products, check stock, update counts. Must understand every label and error instantly. |
| Warehouse staff | Back-of-house | Stock intake, inventory checks. Same UI needs as shop-floor. |
| Manager / Admin | Store management | User accounts, setup flow, reporting. Indonesian text for all admin screens. |

All personas are Indonesian speakers. None are expected to read English.

## User Journeys

| ID | Journey | Description |
|----|---------|-------------|
| J1 | Open app | Staff sees Indonesian text on login page, scan page, product list, detail cards, and stock controls. |
| J2 | View price | Staff sees `Rp12.500` format — not `$12.50`. No decimals for whole amounts. |
| J3 | Create product | Staff enters price in Rupiah. After save, price displays as `Rp` format. |
| J4 | See error | Staff sees `Nama produk wajib diisi` instead of `Product name is required`. |

## Acceptance Criteria

| ID | Criteria |
|----|----------|
| AC1 | Login page, sidebar, product list, product card, product form, stock controls all show Indonesian text. |
| AC2 | Prices display with `Rp` prefix and `.` thousands separator, e.g. `Rp12.500`. |
| AC3 | Error response `message` fields from the API are translated to Indonesian. |
| AC4 | Scan view camera error messages are in Indonesian. |
| AC5 | Setup page and user management pages are in Indonesian. |
| AC6 | Success indicators and flash messages are in Indonesian. |
| AC7 | Price input fields accept values in Rupiah format. |
| AC8 | All 61 backend tests still pass. |

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Missing a string | English text leaks through some component | Audit every component after translation pass |
| Price formatting edge cases | Wrong format for thousands/millions/billions | Use `Intl.NumberFormat('id-ID')` for display — handles all magnitudes |
| Server error messages untranslated | English leaks through API errors | Add a server-side message translation map |

## Open Questions

| # | Question |
|---|----------|
| Q1 | Should we translate server-side error response `message` fields too, or keep them English? |
| Q2 | Should the product name field accept Indonesian-only input, or any Unicode? |
| Q3 | Should we add `<html lang="id">` to the HTML? |
| Q4 | Should price formatting live on the server (return formatted string) or client (return raw number, format in component)? |
