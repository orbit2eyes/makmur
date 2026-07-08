# Domain: stock

## Related Specifications
This stock specification interacts with and depends on the following domain specifications:

- **Auth Spec** (`specs/auth/spec.md`): All stock endpoints are protected by authentication and authorization. The access control rules for stock operations (who can view vs modify stock) are defined in the auth spec's REQ-auth-003 and REQ-auth-004 requirements.

- **Product Spec** (`specs/product/spec.md`): Stock is a property of the Product entity. The stock specification should be read in conjunction with the product spec's data model and requirements, particularly REQ-product-001 which includes initial stock as part of product creation.

- **Scan Spec** (`specs/scan/spec.md`): Barcode scanning functionality often leads to stock viewing or updates. The scan spec's REQ-scan-005 and REQ-scan-006 describe how successful scans route to product detail views where stock information is displayed and can be modified.

- **Product Requirements Document** (`prd.md`): This stock specification implements the stock management requirements outlined in PRD Section 5 (Stock), particularly addressing acceptance criteria AC-04 through AC-11.

- **Design Document** (`design.md`): The data model decisions and API design patterns described in this spec align with and implement the stock-related architecture outlined in the design document.

## Requirements

### ADDED

- **REQ-stock-001**: View stock count on product detail
  - Description: The product detail card displays the current stock count as a prominent numeric value. The stock count is read from the database and displayed alongside the product name, barcode, and price.
  - Rationale: Stock visibility is the primary reason staff scan products — they need to know how many units are on hand at a glance.
  - Acceptance criteria: AC-04 (product detail includes stock), AC-08

- **REQ-stock-002**: Update stock — absolute value entry
  - Description: Staff can update the stock count to an absolute value by entering a number into a numeric input field. The input validates that the value is a non-negative integer. On confirm, the new count is persisted to the database immediately and the product detail card refreshes with the updated count.
  - Rationale: Absolute value entry is the most precise way to set stock — useful for cycle counts, receiving shipments, or correcting errors.
  - Acceptance criteria: AC-08, AC-09

- **REQ-stock-003**: Quick increment/decrement buttons (+1/-1)
  - Description: As an alternative to absolute value entry, staff can tap +1 or -1 buttons to increment or decrement the stock count by one. The -1 button is disabled if current stock is 0. Both buttons persist the change immediately and refresh the displayed count.
  - Rationale: Fast adjustments for common actions — removing one item sold or adding one item received. Reduces friction compared to opening a numeric keyboard for every single-unit change.
  - Acceptance criteria: AC-08, AC-09

- **REQ-stock-004**: Non-negative stock constraint
  - Description: Stock count is constrained to non-negative integers at both the input level and the database level. Any attempt to set a negative value is rejected immediately at the input (the -1 button is disabled at 0; entering a negative number in the absolute value field shows a validation error). The database schema enforces `stock >= 0` as an additional safety net.
  - Rationale: Negative stock is physically meaningless and would corrupt inventory data. Double enforcement (input + DB) prevents data integrity issues from any code path.
  - Acceptance criteria: AC-09

- **REQ-stock-005**: Stock persistence across page reloads
  - Description: After a stock update, the new count is persisted to the server database. If the staff member refreshes the page or reopens the app later, the persisted stock count is displayed. Two devices on the same network see the same stock data simultaneously; changes on one device are visible on the other after a page refresh.
  - Rationale: Stock data must survive page reloads and be consistent across devices. The database is the single source of truth.
  - Acceptance criteria: AC-08, AC-14

- **REQ-stock-006**: Success feedback on stock update
  - Description: After a stock update is confirmed, a brief visual success indicator appears (e.g., a green checkmark, a brief flash, or a colour change on the stock count). The indicator disappears automatically after 1-2 seconds.
  - Rationale: Staff need immediate, unambiguous confirmation that their change was recorded. Without feedback, they may tap again unnecessarily or doubt the system.
  - Acceptance criteria: AC-08 (implied — change is reflected instantly)

## Data Model

No separate table for stock. Stock is stored as the `stock` field on the `product` record.

See `specs/product/spec.md` for the full Product data model.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `stock` (on Product) | integer | NOT NULL, >= 0 | Current stock count |

## Scenarios

- **S-stock-001**: Update stock — absolute value
  - **Given** the product detail card is displayed with current stock of 10
  - **When** the staff member taps "Update Stock", enters "15" in the numeric input, and taps Confirm
  - **Then** the stock count updates to 15, the card refreshes, and a success indicator appears briefly

- **S-stock-002**: Quick increment (+1)
  - **Given** the product detail card is displayed with current stock of 10
  - **When** the staff member taps the +1 button
  - **Then** the stock count updates to 11, and the card refreshes immediately

- **S-stock-003**: Quick decrement (-1)
  - **Given** the product detail card is displayed with current stock of 5
  - **When** the staff member taps the -1 button
  - **Then** the stock count updates to 4, and the card refreshes immediately

- **S-stock-004**: Reject negative stock from quick decrement
  - **Given** the product detail card is displayed with current stock of 0
  - **When** the staff member views the stock controls
  - **Then** the -1 button is disabled, and the stock count remains 0

- **S-stock-005**: Reject negative stock from absolute value entry
  - **Given** the stock update input is open with current stock of 3
  - **When** the staff member enters "-5" and taps Confirm
  - **Then** a validation error is shown, and the stock count remains unchanged at 3

- **S-stock-006**: Stock persists across page reload
  - **Given** the stock count has been updated to 20 on one device
  - **When** the staff member refreshes the page on the same or another device
  - **Then** the product detail card displays stock count of 20