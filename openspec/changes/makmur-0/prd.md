# Product Requirements Document — Makmur v1 (makmur-0)

## 1. Problem

Retail and warehouse staff currently rely on manual stock tracking — pen-and-paper counts, spreadsheets, or legacy terminals. These methods are slow, error-prone, and create friction at every touchpoint:

- **Counting stock** requires looking up product names or codes manually, then writing down counts.
- **Receiving new shipments** means keying in product details by hand before items can be shelved.
- **Finding a product** involves scanning shelves or flipping through paper lists.
- **Price checks** for customers force staff to walk to a terminal or call a back-office colleague.

The result is wasted labour, stale inventory data, and slower service. Staff need a tool that meets them where they work — on the shop floor or warehouse aisle — and removes the data-entry overhead from every scan.

## 2. Goals

Makmur v1 aims to put a barcode scanner in every staff member's pocket — accessible from any device with a camera and a browser on the local network.

1. **Scan to identify** — Point the camera at any EAN-13 barcode and immediately see the product it belongs to.
2. **Instant product creation** — When a scanned barcode is unknown, capture the product name, price, and initial stock in under 30 seconds — right there, no second device.
3. **Stock lookup** — View current stock count for any product the instant it is scanned.
4. **Stock updates** — Adjust stock numbers up or down as goods arrive or leave the shelf, with the change reflected immediately.
5. **Product catalogue** — Browse and search the full inventory from any device on the network.
6. **Zero setup per device** — Open a URL on a phone, tablet, or laptop. No installs, no accounts, no login.

The guiding principle: **every common inventory action should be reachable in two taps or fewer from a scan**.

## 3. Non-goals

The following are explicitly deferred from v1:

- **Offline mode** — Requires network connectivity to the local server. No offline queue or sync.
- **Receipt / invoice generation** — No transaction history, purchase orders, or sales reports.
- **Supplier management** — No supplier profiles, reorder thresholds, or purchase-order workflows.
- **Barcode printing** — No label generation or printer integration. Products are assumed to already carry printed barcodes.
- **Non-EAN-13 barcodes** — UPC-A, Code 128, QR codes, and other formats are not supported.
- **Mobile-native apps** — No iOS or Android app. The target is a responsive web application.
- **Data export / import** — No CSV or API bulk operations in v1.

## 4. Users / Personas

### Shop-floor staff (primary)

A retail associate, cashier, or stock clerk working on the sales floor. They scan products for price checks, quick-stock queries, and flagging low inventory. They need speed and simplicity — tap scan, see result, move on. They are not technical and will not tolerate a learning curve.

### Warehouse / back-of-house staff (primary)

A stock receiver or inventory specialist handling incoming shipments, cycle counts, and shelf replenishment. They scan dozens of items in a session and need to create products on the fly. They value keyboard-like efficiency: continuous scan mode, quick numeric stock entry, and instant confirmation.

### Store manager (secondary)

Oversees inventory accuracy. They browse the full catalogue, search for items, and verify stock levels. Less frequent scanner use, more list-based interaction. They benefit most from the search and browse features.

## 5. User Journeys

### Journey A: Scan barcode — product found (detail view)

1. Staff member opens Makmur on a phone or tablet.
2. The app requests camera access. Staff grants it.
3. A live camera viewfinder appears on screen, centred on the barcode area.
4. Staff points the camera at a product's barcode. The viewfinder highlights the detected barcode area.
5. The barcode decodes within 1-2 seconds. A success sound or vibration confirms.
6. The viewfinder transitions into a **product detail card** showing:
   - Product name
   - EAN-13 barcode number
   - Current stock count
   - Price
7. Below the card, action buttons are available: **Update Stock**, **Scan Another**.
8. Staff taps **Scan Another**. The viewfinder reappears for the next scan.

**Variation — continuous scan mode:**
Steps 6-8 collapse: after showing the card briefly, the app returns to the viewfinder automatically, allowing rapid sequential scanning without tapping between each item.

### Journey B: Scan barcode — product not found (creation flow)

1. Staff scans a barcode (same as Journey A steps 1-4).
2. After decoding, the app displays: **"Product not found. Add it to the catalogue?"** along with the detected barcode number.
3. Staff taps **Add Product**.
4. A creation form appears with pre-filled barcode number and empty fields for:
   - Product name (required)
   - Price (required, numeric)
   - Initial stock count (required, numeric, default 0)
5. Staff fills in the fields and taps **Save**.
6. The product is saved. The app displays the new product detail card (same as Journey A step 6).
7. Staff can now proceed to update stock or scan the next product.

**Error case (Journey B.1):** Staff starts entering a product name, then realises the barcode is damaged or wrong. A **Cancel** button discards the unsaved draft and returns to the viewfinder.

### Journey C: Browse product list

1. Staff opens Makmur. The home screen shows a **product list** view and a **Scan** button in a prominent position.
2. The product list displays all products in the catalogue, one per row:
   - Product name
   - Barcode number (truncated or full)
   - Current stock count
   - Price
3. The list is sorted alphabetically by product name by default.
4. Staff scrolls through the list. The list loads quickly — no pagination delays for catalogues up to several thousand products.
5. Tapping any row opens the **product detail card** (same as Journey A step 6).

### Journey D: Update stock count

1. Staff reaches the product detail card (via scan, search, or browse).
2. Staff taps **Update Stock**.
3. A simple numeric input appears with:
   - Current stock displayed for reference.
   - A field to enter the **new count** (absolute value, not a delta).
   - Alternative: **+1** and **-1** quick buttons for fast adjustments.
4. Staff enters the new count and taps **Confirm**.
5. The stock count updates instantly. The card refreshes with the new number.
6. A brief success indicator appears (green checkmark or flash).

### Journey E: Search products

1. Staff is on the product list view or home screen.
2. A search bar at the top of the list accepts text input.
3. Staff types a product name (partial match works — e.g., typing "mil" finds "Fresh Milk" and "Milk Chocolate").
4. The list filters in real-time as the staff types, narrowing to matching products.
5. Staff taps a result row to open the product detail card.
6. A **Clear** button resets the search and restores the full product list.

## 6. Acceptance Criteria

All criteria must pass before v1 is considered complete.

| ID | Criterion | Type |
|----|-----------|------|
| AC-01 | Any device on the local network can open the app in a modern browser and see the home screen. No login or account setup required. | Functional |
| AC-02 | The app requests camera permission once on first access. Denying permission shows a clear error message with instructions to enable it in browser settings. | Functional |
| AC-03 | Pointing the camera at a valid EAN-13 barcode decodes the number and displays the result within 3 seconds. | Performance |
| AC-04 | If the decoded barcode matches an existing product, the app shows the product detail card (name, barcode, stock, price) within 1 second of decoding. | Functional |
| AC-05 | If the decoded barcode matches no existing product, the app shows a creation prompt with the barcode pre-filled. | Functional |
| AC-06 | Product creation requires name, price, and initial stock. All fields validate: name is non-empty, price is a positive number, stock count is a non-negative integer. | Functional |
| AC-07 | Saved products appear immediately in the product list and are searchable. | Functional |
| AC-08 | Updating stock changes the displayed count instantly and persists across page reloads. | Functional |
| AC-09 | Stock count never goes below 0. Any attempt to set negative stock is rejected at input level. | Functional |
| AC-10 | Searching by partial product name returns matching results in under 500 ms for a catalogue of 1000 products. | Performance |
| AC-11 | The product list renders and is scrollable without noticeable lag for up to 5000 products. | Performance |
| AC-12 | The app is usable on a smartphone screen (viewport width 375px) and a desktop screen (viewport width 1280px). | Responsive |
| AC-13 | Consecutive scans (scan, view result, scan again) complete in under 5 seconds round-trip. | Performance |
| AC-14 | Two devices on the same network can see the same product data simultaneously. Adding or updating on one device is visible on the other after a page refresh. | Functional |

## 7. Risks

### Camera API support (medium)

Not all browsers on all devices support `getUserMedia` with sufficient resolution for barcode scanning. Older mobile browsers, some tablet browsers, and desktop Safari may have inconsistent behaviour. The app must detect camera API availability at startup and show a meaningful fallback message if unavailable.

### EAN-13 decoding accuracy (medium)

Barcode detection relies on the device camera quality, lighting conditions, and the barcode's physical condition (faded, wrinkled, partially obscured). Ambient light, motion blur, and reflective surfaces (shrink wrap, glossy packaging) can cause false negatives or misreads. The app should handle the common case well (well-lit, undamaged barcode) and degrade gracefully — requesting a second try rather than silently failing or showing wrong data.

### SQLite concurrency under browser-based clients (low)

If the database is server-side, simultaneous write requests from multiple devices could, in theory, cause write contention. For v1 scale (single store, handful of staff), this is unlikely to be a practical problem. Worst case: one staff member's stock update blocks briefly while another's completes. Monitoring for this in production is sufficient — no preemptive locking layer needed.

### Browser compatibility across devices (low)

Staff devices are a heterogeneous mix: personal phones, shared tablets, older laptops, various OS versions. The app must work on Chrome, Safari, Firefox, and Samsung Internet — the last two release cycles at minimum. CSS features, JavaScript API availability (`BarcodeDetector`, `navigator.mediaDevices`), and form-factor responsiveness are the main vectors for breakage.

### Accidental duplicate product creation (medium)

If staff scan a product that exists but fail to recognise it in the result, they might re-enter it as a new product, creating duplicates with different row IDs but the same barcode. The app should guard against this at the point of creation — if the barcode already exists in the database, redirect to the existing product instead of allowing a duplicate.

## 8. Open Questions

The following questions remain open and could affect scope or design decisions in v1. They should be resolved before or during the design phase.

| # | Question | Impact |
|---|----------|--------|
| Q1 | Should the app support continuous scan mode (auto-return to viewfinder after each scan) as the default, or require a manual tap per scan? | Affects core UX flow and first-impression design. |
| Q2 | Should stock updates use absolute value entry ("set stock to 42") or delta entry ("add 5, subtract 3"), or both? | Affects the stock update form design and edge-case handling (e.g., negative deltas). |
| Q3 | What is the expected maximum catalogue size for v1? (100 products? 10,000? 100,000?) | Affects pagination strategy, search indexing, and database schema decisions. |
| Q4 | Should the app display a product photo alongside the name and details? If yes, how are photos captured or imported? | Adds significant scope: image capture, storage, and rendering pipeline. Deferrable to v2. |
| Q5 | Should the product list support filtering by low-stock threshold (e.g., "show items where stock < 5")? | Adds query complexity to the list view. Useful for restocking workflows, but deferrable. |
| Q6 | How should the app handle barcodes from damaged or partially obscured labels — retry prompt, manual barcode entry fallback, or skip? | Affects error handling UX and first-time-user frustration tolerance. |
| Q7 | Should a "delete product" function exist in v1? Removing products from the catalogue carries risk of accidental deletion. | Affects CRUD completeness vs. safety trade-off. |

---

## 9. Authorization & Access Control Feature

### 9.1 Problem

Makmur v1 was designed as a single-user local-network app with no access control — every device on the network had full read and write access. As the app moves toward deployment in real retail environments with multiple staff members, the absence of authentication and authorization creates several problems:

- **No audit trail** — Any stock change or product creation cannot be attributed to a specific person. Mistakes and disputes are untraceable.
- **Unrestricted access** — Anyone on the local network can create, edit, or delete products. There is no way to restrict sensitive operations (e.g., staff management) to trusted users.
- **No separation of duties** — A cashier scanning products for price checks should not be able to modify staff accounts or manage other users. In v1, every user can do everything.
- **No onboarding guard** — The first deployment has no initial user setup flow. Someone must seed the first admin account out of band.

The authorization feature adds role-based access control with scoped permissions, a secure login flow, and an onboarding mechanism for initial setup.

### 9.2 Goals

1. **Authenticate users** — Staff log in with username and password. The server issues a JWT token that authorises subsequent requests.
2. **Scope-based authorization** — API endpoints check the caller's role and restrict data/operations accordingly. The same endpoint serves all roles; scope filtering happens inside the handler, not at the routing layer.
3. **Three roles with distinct scopes**:
   - **admin**: Full access to all data. Can manage managers.
   - **manager**: Access to staff data only (create, read, update staff users). Cannot access products.
   - **staff**: Access to products only (scan, stock updates, search, browse). Cannot access user management.
4. **Initial admin onboarding** — QR-code-based setup for the first admin user. No pre-seeded credentials.
5. **Secure credential storage** — Passwords hashed with a strong algorithm (bcrypt). No plain-text storage.
6. **Token-based session** — JWT expiry and refresh mechanism to limit exposure from leaked tokens.

### 9.3 Non-goals

- **SSO / OAuth / social login** — No external identity providers. All authentication is local.
- **Fine-grained per-object permissions** — No per-product or per-user access control lists. Roles define fixed scopes.
- **Multi-tenant / organisation separation** — Single-tenant app. All users belong to the same store.
- **Password reset workflow** — No email-based or SMS-based password reset for v1. Admin can reset staff passwords manually.
- **Session revocation / token blacklist** — Tokens are valid until expiry. No server-side token blacklist in v1.
- **Login rate limiting** — Deferred; relies on application-level monitoring for brute-force detection.
- **Audit log** — No persistent audit log of user actions in v1. Attribution exists via the authenticated user on each request but is not stored as a historical record.

### 9.4 Users / Personas

#### Admin (primary for setup, secondary for ongoing)

The store owner or senior manager responsible for deploying Makmur. They set up the initial admin account by scanning the QR code on the first launch, then create manager accounts. Admin has unrestricted access to all product and user data. Admin is the escalation point for account issues.

#### Manager (primary for staff operations)

A shift supervisor or department lead who manages the staff roster. The manager creates, updates, and deactivates staff user accounts. The manager does NOT access product data — no scanning, no stock updates, no catalogue browsing. Their interface is limited to staff management.

#### Staff (primary for daily operations)

A retail associate, cashier, or stock clerk who uses Makmur for its original product-management features: scanning barcodes, checking stock, updating counts, searching the catalogue. Staff cannot see or manage any user accounts. Their experience is identical to v1's single-user mode, but gated behind authentication.

### 9.5 User Journeys

#### Journey F: First-time setup — QR admin onboarding

1. Staff opens Makmur on a browser for the first time.
2. The app detects that no admin user exists in the `users` table.
3. The home screen displays a **Setup** page with a QR code.
4. The QR code encodes a one-time setup token (generated at server startup, stored in a server-side file or environment variable).
5. The admin (store owner) scans the QR code with their phone.
6. The phone navigates to the setup URL encoded in the QR code.
7. The setup page prompts the admin to create the initial admin account:
   - Username (required)
   - Password (required, minimum length enforced)
   - Confirm password
8. Admin fills in the fields and submits.
9. The server creates the first `admin` user, invalidates the setup token, and redirects to the login page.
10. The admin logs in with the new credentials and can now create manager accounts.

**Error case (F.1):** If someone tries to access the setup URL after the admin account has been created, the server returns a `403 Forbidden` or redirects to login. The setup token is single-use.

**Error case (F.2):** If the QR code is lost or expired before setup completes, the server operator must restart the server or manually clear the `admin` seed flag to regenerate the token.

#### Journey G: Login

1. Any user opens Makmur. They are redirected to the **Login** page.
2. The login page displays a simple form: username field, password field, **Log In** button.
3. User enters their credentials and taps **Log In**.
4. The client sends `POST /api/auth/login` with `{ username, password }`.
5. The server looks up the user by username, verifies the password hash, and generates a JWT containing:
   - `sub`: user ID
   - `role`: the user's role (`admin`, `manager`, `staff`)
   - `iat`: issued-at timestamp
   - `exp`: expiration timestamp (e.g., 24 hours)
6. The server responds with `{ token, user: { id, username, role } }`.
7. The client stores the JWT (e.g., `localStorage` or `sessionStorage`) and redirects to the appropriate home view based on role:
   - **staff**: product catalogue / scan view (same as v1)
   - **manager**: staff management view
   - **admin**: unified view with access to both products and user management
8. On subsequent requests, the client includes the JWT in the `Authorization: Bearer <token>` header.

**Error case (G.1):** Invalid credentials return `401 Unauthorized` with a generic message (no indication of whether the username or password was wrong) to prevent user enumeration.

**Error case (G.2):** An expired or malformed JWT on a protected endpoint returns `401 Unauthorized`. The client redirects to the login page.

**Error case (G.3):** A valid JWT for a user whose account has been deactivated returns `403 Forbidden` with a message indicating the account is disabled.

#### Journey H: Scope-based access — staff role

1. Staff user logs in (Journey G).
2. The home screen shows the product catalogue / scan view (identical to v1).
3. Staff scans a barcode: the scan endpoint checks the JWT role, confirms `staff` scope, and returns product data.
4. Staff updates stock: the stock endpoint checks the JWT role, confirms `staff` scope, and processes the update.
5. Staff searches products: the search endpoint returns matching products.
6. Staff navigates to `/users` or any user-management URL. The server returns `403 Forbidden`. The client shows an access-denied message and a link back to the product view.

#### Journey I: Scope-based access — manager role

1. Manager logs in (Journey G).
2. The home screen shows the **staff management** view — a list of staff user accounts.
3. Manager can:
   - **Create staff account**: username + temporary password. The new account has `role='staff'`.
   - **Deactivate staff account**: sets an `active` flag to `false`. Deactivated users cannot log in.
   - **Reset staff password**: sets a new temporary password for a staff account.
4. Manager navigates to `/products` or any product endpoint. The server returns `403 Forbidden`. The client shows an access-denied message.
5. Manager cannot see or modify other managers or the admin account.

#### Journey J: Scope-based access — admin role

1. Admin logs in (Journey G).
2. The home screen shows a **dashboard** with navigation to both product management and user management.
3. Admin can:
   - Access all product features (scan, stock, search, browse) — identical scope to staff.
   - Manage **manager** accounts (create, deactivate, password reset).
   - Manage **staff** accounts (same capabilities as manager, but can also reassign staff to different managers).
   - **Note**: Admin cannot delete users — only deactivate. Deletion is a database-level operation.
5. Admin's JWT has the same structure as other roles but carries `role='admin'`. The scope filter grants access to all business data.

### 9.6 Standardized Error Response Format

All API endpoints return errors in a consistent JSON envelope. Every error response has exactly two fields:

```json
{
  "error": "<error_code>",
  "message": "<human-readable description>"
}
```

The `error` field is a machine-readable code (snake_case). The `message` field is a human-readable description suitable for display in the frontend. Error responses never contain stack traces, internal server paths, or implementation details.

#### Auth-related error codes

| HTTP Status | error_code | When | Notes |
|-------------|-----------|------|-------|
| 400 | bad_request | Malformed request body, missing required fields, validation failure | |
| 401 | unauthorized | No JWT provided, JWT expired, JWT malformed | Applied by JwtAuthenticationFilter |
| 403 | forbidden | Authenticated but role lacks scope, deactivated account, setup already completed | Scope enforcement + deactivated check |
| 404 | not_found | User, product, or resource not found | |
| 409 | conflict | Duplicate username on create | |
| 422 | unprocessable_entity | Password too short, invalid role value, business rule violation | |
| 500 | internal_error | Unexpected server error | Never expose stack traces |

#### Security properties

- Unauthenticated errors (`401`) use a generic message regardless of whether the username exists, the password is wrong, or the token is malformed. This prevents user enumeration.
- Deactivated account errors (`403`) use a distinct error code `account_disabled` in the `error` field so the frontend can show a specific message, but no information about the account's existence is revealed before authentication.
- Internal server errors (`500`) must never include stack traces, file paths, or SQL detail in the response body.

### 9.7 Acceptance Criteria

| ID | Criterion | Type |
|----|-----------|------|
| AC-15 | A user with valid credentials can log in via `POST /api/auth/login` and receive a JWT. Invalid credentials return `401`. | Functional |
| AC-16 | Protected endpoints reject requests with no JWT (`401 Unauthorized`). | Security |
| AC-17 | Protected endpoints reject requests with an expired or malformed JWT (`401 Unauthorized`). | Security |
| AC-18 | A `staff` user can access product endpoints (scan, stock, search, browse) but receives `403` on user-management endpoints. | Functional |
| AC-19 | A `manager` user can access staff-management endpoints but receives `403` on product endpoints. | Functional |
| AC-20 | An `admin` user can access all endpoints including product, user-management, and developer/internal tooling. | Functional |
| AC-21 | Scope checks are implemented inside the endpoint handler, not at the routing layer. The same endpoint URL serves all roles. | Architecture |
| AC-22 | The first-time setup flow generates a QR code when no admin user exists. Scanning the QR code opens a setup page to create the first admin account. | Functional |
| AC-23 | After the first admin account is created, the setup token is invalidated. Accessing the setup URL returns `403`. | Security |
| AC-24 | Passwords are stored as bcrypt hashes. No plain-text password is stored in the `users` table. | Security |
| AC-25 | The JWT contains `sub`, `role`, `iat`, and `exp` claims. Token expiration is no more than 24 hours. | Security |
| AC-26 | The existing `products` table schema is unchanged. The `users` table column `password` is renamed to `password_hash`. The `role` CHECK constraint includes `'manager'` in addition to `'admin'` and `'staff'`. | Architecture |
| AC-27 | A `staff` user who logs out or whose token expires is redirected to the login page on their next action. The product data is inaccessible without a valid token. | Functional |
| AC-28 | A deactivated user account (set inactive by manager/admin) cannot log in. Login returns `403 Forbidden` with a message indicating the account is disabled. | Functional |
| AC-29 | The login page is the default redirect for any unauthenticated request. The setup page (QR flow) is the only exception, and only when no admin exists. | Functional |
| AC-30 | All API error responses use the standardized JSON format with `error` and `message` fields. No error response exposes stack traces, server paths, or SQL details. | Security |

### 9.8 Risks

#### JWT secret management (high)

The JWT signing secret must be securely generated, stored, and kept out of version control. If the secret is leaked, anyone can forge tokens with any role. The secret should be set via environment variable (e.g., `JWT_SECRET`) or read from a file outside the repository. Default/fallback secrets in code are unacceptable.

#### QR code setup token security (medium)

The one-time setup token is generated at server startup and served via the QR code. If someone intercepts the QR code before the legitimate admin scans it, they could create their own admin account. Mitigations: the token is valid only on the local network, the setup page is only served when no admin exists, and the token is invalidated after first use. For additional security, the token could be time-limited (e.g., expires 1 hour after server start).

#### Token expiry vs. user experience (medium)

A 24-hour JWT expiry means users must log in once per day. If a staff member is mid-scan when the token expires, the next API call will fail and they will be redirected to login, losing their current action. Mitigations: use a shorter token lifetime with a refresh token mechanism, or set expiry to 8 hours (shift-length) to reduce mid-shift expirations.

#### Staff credential management (medium)

Managers create staff accounts with temporary passwords. If the temporary password is sent insecurely (verbally, written on paper), an eavesdropper could gain staff-level access. Staff may not change their temporary password promptly. Mitigation: force password change on first login, enforce minimum password length (8+ characters).

#### Direct database access bypass (high)

If the SQLite database file is accessible on the filesystem, anyone with server access can read or modify data directly, bypassing all authorization. Mitigation: the database file must be stored outside the web server's document root, with file-system permissions restricting read/write to the application process only. The API is the sole entry point for data access.

#### Role escalation via user-management API (medium)

If a manager endpoint does not properly restrict the `role` field when creating staff accounts, a manager could create an account with `role='admin'` or `role='manager'`. Mitigation: the user creation endpoint for managers must hard-code `role='staff'` server-side. Only the admin endpoint can set `role='manager'`. The frontend role selector, if any, is informational only — the server enforces the final role value.

### 9.9 Open Questions

| # | Question | Impact |
|---|----------|--------|
| Q8 | Should the JWT use a refresh-token mechanism (short-lived access token + long-lived refresh token), or a single long-lived token (24h)? | Affects UX (silent refresh vs. re-login) and implementation complexity. |
| Q9 | Should the staff home view require login before showing the scan interface, or show a public scan view and gate only write operations behind auth? | Affects first-time user experience and the login-wall design. |
| Q10 | Should the QR setup token be time-limited (e.g., expires 60 min after server start), or persist until the first admin is created? | Affects setup convenience vs. security window. |
| Q11 | Should the `users` table include an `active` column for account deactivation (soft disable), or should accounts be hard-deleted? | Affects data integrity and the deactivation journey design. |
| Q12 | How should the app handle the transition from v1 (no auth, no users) to v2 (auth required)? Will existing deployments need a migration script or manual setup? | Affects upgrade path for existing installations. |
| Q13 | Should the login page remember the last username for convenience, or require full re-entry every time? | Affects UX for staff who use shared devices. |
| Q14 | Should there be a **Log Out** button visible at all times, or only on certain pages? | Affects navigation design and session management UX. | 