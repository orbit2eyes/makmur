# Domain: product

## Related Specifications
This product specification interacts with and depends on the following domain specifications:

- **Auth Spec** (`specs/auth/spec.md`): All product endpoints are protected by authentication and authorization. The access control rules for product operations (who can view vs modify products) are defined in the auth spec's REQ-auth-003 and REQ-auth-004 requirements.

- **Stock Spec** (`specs/stock/spec.md`): Stock is a key attribute of the Product entity. The stock specification defines how stock levels are viewed and updated, which directly relates to product management operations described in this spec.

- **Scan Spec** (`specs/scan/spec.md`): Barcode scanning functionality often leads to product viewing or creation. The scan spec's REQ-scan-005 and REQ-scan-006 describe how successful scans route to product detail views or creation forms, implementing the product lookup and creation requirements described in this spec.

- **Product Requirements Document** (`prd.md`): This product specification implements the product management requirements outlined in PRD Sections 5 (Product Catalogue) and 6 (Acceptance Criteria AC-04 through AC-11), particularly addressing the core product lifecycle features.

- **Design Document** (`design.md`): The data model decisions and API design patterns described in this spec align with and implement the product-related architecture outlined in the design document.

## Requirements

### ADDED

- **REQ-product-001**: Create product with name, barcode, and price
  - Description: Staff can create a new product by providing a product name (required, non-empty), EAN-13 barcode (required, unique, 13-digit), and price (required, positive number). An initial stock count may be set (required, non-negative integer, default 0).
  - Rationale: Foundation of the catalogue — products must exist in the database before they can be scanned, searched, or stocked.
  - Acceptance criteria: AC-06, AC-07

- **REQ-product-002**: Duplicate barcode guard at creation
  - Description: If the barcode already exists in the database when creating a product, the app redirects to the existing product detail instead of creating a duplicate. A message informs the staff that the product already exists.
  - Rationale: Prevents accidental duplicate product entries when staff scan an existing product but do not recognise it.
  - Acceptance criteria: AC-05 (implied — creation prompt only shows for truly unknown barcodes)

- **REQ-product-003**: Product lookup by barcode
  - Description: Given a 13-digit EAN-13 barcode string, the system returns the matching product (name, barcode, stock, price) within 1 second if it exists, or a not-found indicator if it does not.
  - Rationale: Core lookup path used by the scanning pipeline — every successful scan resolves through this lookup.
  - Acceptance criteria: AC-04, AC-05

- **REQ-product-004**: Product list with alphabetical sort and scroll
  - Description: The app displays all products in the catalogue, one per row (name, barcode, stock, price), sorted alphabetically by product name. The list scrolls smoothly without pagination lag for up to 5000 products.
  - Rationale: Staff need to browse the full catalogue from the home screen. No pagination delays for realistic catalogue sizes.
  - Acceptance criteria: AC-11

- **REQ-product-005**: Product search by partial name match
  - Description: A search bar filters the product list in real-time as the staff types, matching any product whose name contains the search string (case-insensitive, substring match). Results appear within 500 ms for a catalogue of 1000 products. A clear button restores the full list.
  - Rationale: Staff need to find products quickly without scanning — e.g., price checks for customers or verifying stock during shelf walks.
  - Acceptance criteria: AC-10

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

## Scenarios

- **S-product-001**: Create product
  - **Given** the staff member has scanned an unknown barcode or tapped a "New Product" button
  - **When** they fill in the product name, price, and initial stock, then tap Save
  - **Then** the product is stored in the catalogue, and the product detail card is displayed

- **S-product-002**: Duplicate barcode rejected
  - **Given** a barcode already exists in the catalogue
  - **When** a staff member attempts to create a new product with that same barcode
  - **Then** the app redirects to the existing product detail with a message that the product already exists

- **S-product-003**: Find product by barcode
  - **Given** a product with barcode "1234567890123" exists in the catalogue
  - **When** the system looks up the barcode string
  - **Then** it returns the product record (name, price, stock) within 1 second

- **S-product-004**: Barcode not found
  - **Given** no product with barcode "9876543210987" exists
  - **When** the system looks up the barcode string
  - **Then** it returns a not-found indicator

- **S-product-005**: List all products
  - **Given** the catalogue contains products
  - **When** the staff member opens the product list view
  - **Then** all products are displayed sorted alphabetically by name, with name, barcode, stock, and price per row

- **S-product-006**: Search by partial name
  - **Given** the product list view is open
  - **When** the staff member types "mil" in the search bar
  - **Then** the list filters to show only products whose name contains "mil" (e.g., "Fresh Milk", "Milk Chocolate"), and results appear within 500 ms for 1000 products