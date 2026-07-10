# Domain: product

## Purpose

The product domain manages the inventory catalogue — creating, looking up, listing, and searching products. Prices are stored as numeric values and displayed in Indonesian Rupiah (IDR) format with `Rp` prefix and `.` as the thousands separator.

## MODIFIED Requirements

### Requirement: REQ-product-001 - Create product with name, barcode, and price in IDR

- Description: Staff SHALL create a new product by providing a product name (required, non-empty), EAN-13 barcode (required, unique, 13-digit), and price (required, positive number in Rupiah). An initial stock count MAY be set (required, non-negative integer, default 0). Price input SHALL accept Indonesian number format. After save, the price SHALL be displayed with `Rp` prefix and `.` as thousands separator (e.g. `Rp12.500`).
- Priority: P0
- Rationale: Indonesian staff transact in Rupiah. Displaying prices in IDR removes mental currency conversion on every scan.
- Change: Original accepted price as a generic positive number with no currency format. New behavior specifies IDR format for input and display.
- Acceptance criteria: AC1, AC2, AC7

#### Scenario: S-product-007 - Price shown in IDR format

**Given** a product with price 12500 exists in the catalogue
**When** the staff member views the product detail or list
**Then** the price is displayed as `Rp12.500`
**And** the decimal-free format uses `.` as the thousands separator

### Requirement: REQ-product-002 - Duplicate barcode guard at creation

- Description: If the barcode already exists in the database when creating a product, the app SHALL redirect to the existing product detail instead of creating a duplicate. A message in Indonesian SHALL inform the staff that the product already exists.
- Priority: P0
- Rationale: Prevents accidental duplicate product entries. The message must be in Indonesian for staff comprehension.
- Change: Original message was in English. New behavior uses Indonesian text for all user-facing messages.
- Acceptance criteria: AC3

#### Scenario: S-product-008 - Duplicate barcode message in Indonesian

**Given** a product with barcode "5901234567890" already exists
**When** a staff member attempts to create a product with the same barcode
**Then** the app shows `Produk sudah ada` or equivalent Indonesian message
**And** redirects to the existing product detail

### Requirement: REQ-product-005 - Product search by partial name match

- Description: A search bar SHALL filter the product list in real-time as the staff types, matching any product whose name contains the search string (case-insensitive, substring match). Results SHALL appear within 500 ms for a catalogue of 1000 products. A clear button SHALL restore the full list. All search UI labels, placeholders, and result messages SHALL be in Indonesian.
- Priority: P0
- Rationale: Indonesian staff must understand every UI element. Search labels and prompts in English cause confusion.
- Change: Original UI text was English. New behavior uses Indonesian for all search-related UI strings.
- Acceptance criteria: AC1

#### Scenario: S-product-009 - Search UI in Indonesian

**Given** the product list view is open
**When** the staff member sees the search bar
**Then** the placeholder text is in Indonesian (e.g. `Cari produk...`)
**And** the clear button label is in Indonesian

## Data Model

No change. The `price` field remains `REAL NOT NULL CHECK(price >= 0.01)`. Currency is a display concern, not a storage concern.

## Related Specifications

- **Auth Spec** (`specs/auth/spec.md`): Auth domain error messages must also be translated to Indonesian. See auth delta spec for REQ-auth-014 changes.
- **Scan Spec** (`specs/scan/spec.md`): Scan domain camera error messages must also be translated to Indonesian.
- **Stock Spec** (`specs/stock/spec.md`): Stock domain UI text must be translated to Indonesian. No requirement changes.
