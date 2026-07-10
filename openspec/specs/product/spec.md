# Domain: product

## Purpose

The product domain manages the inventory catalogue — creating, looking up, listing, and searching products. Prices are stored as numeric values and displayed in Indonesian Rupiah (IDR) format with `Rp` prefix and `.` as thousands separator.

## Related Specifications
This product specification interacts with and depends on the following domain specifications:

- **Auth Spec** (`specs/auth/spec.md`): All product endpoints are protected by authentication and authorization. The access control rules for product operations (who can view vs modify products) are defined in the auth spec's REQ-auth-003 and REQ-auth-004 requirements.

- **Stock Spec** (`specs/stock/spec.md`): Stock is a key attribute of the Product entity. The stock specification defines how stock levels are viewed and updated, which directly relates to product management operations described in this spec.

- **Scan Spec** (`specs/scan/spec.md`): Barcode scanning functionality often leads to product viewing or creation. The scan spec's REQ-scan-005 and REQ-scan-006 describe how successful scans route to product detail views or creation forms, implementing the product lookup and creation requirements described in this spec.

- **Product Requirements Document** (`prd.md`): This product specification implements the product management requirements outlined in PRD Sections 5 (Product Catalogue) and 6 (Acceptance Criteria AC-04 through AC-11), particularly addressing the core product lifecycle features.

- **Design Document** (`design.md`): The data model decisions and API design patterns described in this spec align with and implement the product-related architecture outlined in the design document.
## Requirements
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

### Requirement: REQ-product-003 - Product lookup by barcode

- Description: Given a 13-digit EAN-13 barcode string, the system SHALL return the matching product (name, barcode, stock, price) within 1 second if it exists, or a not-found indicator if it does not.
- Priority: P0
- Rationale: Core lookup path used by the scanning pipeline — every successful scan resolves through this lookup.
- Acceptance criteria: AC-04, AC-05

#### Scenario: S-product-003 - Find product by barcode

**Given** a product with barcode "1234567890123" exists in the catalogue
**When** the system looks up the barcode string
**Then** it returns the product record (name, price, stock) within 1 second

#### Scenario: S-product-004 - Barcode not found

**Given** no product with barcode "9876543210987" exists
**When** the system looks up the barcode string
**Then** it returns a not-found indicator

### Requirement: REQ-product-004 - Product list with alphabetical sort and scroll

- Description: The app SHALL display all products in the catalogue, one per row (name, barcode, stock, price), sorted alphabetically by product name. The list SHALL scroll smoothly without pagination lag for up to 5000 products.
- Priority: P1
- Rationale: Staff need to browse the full catalogue from the home screen. No pagination delays for realistic catalogue sizes.
- Acceptance criteria: AC-11

#### Scenario: S-product-005 - List all products

**Given** the catalogue contains products
**When** the staff member opens the product list view
**Then** all products are displayed sorted alphabetically by name, with name, barcode, stock, and price per row

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

### Product

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | integer | PRIMARY KEY, AUTOINCREMENT | Auto-generated unique identifier |
| `barcode` | text | UNIQUE, NOT NULL, length = 13 | EAN-13 barcode string |
| `name` | text | NOT NULL | Product display name |
| `price` | real | NOT NULL, >= 0.01 | Product price in the local currency |
| `stock` | integer | NOT NULL, >= 0 | Current stock count |
| `created_at` | text | NOT NULL, ISO 8601 | Timestamp of product creation |
