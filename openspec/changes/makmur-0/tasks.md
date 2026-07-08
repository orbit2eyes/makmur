# Tasks — Makmur v1 (makmur-0)

Greenfield project. React+Vite frontend, Node.js/Express backend, SQLite (better-sqlite3). No source code exists yet. All 3 domains (product, scan, stock) are ADDED.

---

## Phase 1: Project scaffold

> **SUPERSEDED** — original Node.js plan replaced by Java/Spring Boot implementation. See auth phase tasks (T-auth-*) and design.md for actual architecture.

Initialize the project structure, server skeleton, database schema, and API client. After this phase, the server starts and responds to a basic health check.

**Dependencies:** None (phase is the foundation).

**Verification:** Run `npm run dev` (or equivalent). Server binds to `localhost:<port>`. `GET /api/products` returns `[]`.

| # | Task | Files | Evidence | Req / AC |
|---|------|-------|----------|----------|
| T-1-01 | Initialize Vite + React frontend project | `client/package.json`, `client/vite.config.js`, `client/index.html`, `client/src/main.jsx`, `client/src/App.jsx` | `npm run dev` launches Vite dev server. Browser shows a blank page (or minimal shell) without errors in the console. | AC-01 |
| T-1-02 | Initialize Node.js Express server project with package entry | `server/package.json`, `server/index.js` | `node server/index.js` starts Express, logs "Makmur server listening on port X", accepts HTTP connections. | AC-01 |
| T-1-03 | Create SQLite database module — schema, indexes, WAL mode | `server/db.js` | Module runs `CREATE TABLE IF NOT EXISTS products (...)` and `PRAGMA journal_mode = WAL`. Importing the module creates the database file at the expected path. Schema matches design doc: fields `id`, `barcode` (UNIQUE), `name`, `price`, `stock` (>=0), `created_at`. Both indexes exist (`idx_products_barcode`, `idx_products_name`). | REQ-product-001, REQ-product-003, REQ-product-004, REQ-stock-004 |
| T-1-04 | Add `GET /api/products` route returning empty array | `server/index.js`, `server/routes/products.js` | `curl http://localhost:<port>/api/products` returns `[]` with status 200. | REQ-product-004, AC-11 |
| T-1-05 | Create API client module for frontend | `client/src/api.js` | Module exports `fetchProducts()`, `fetchProduct(barcode)`, `createProduct(data)`. Functions are defined and callable without runtime error (will fail on network until server routes are implemented — acceptable at this stage). | — |

---

## Phase 2: Product CRUD (backend)

> **SUPERSEDED** — original Node.js plan replaced by Java/Spring Boot implementation. See auth phase tasks (T-auth-*) and design.md for actual architecture.

Full product CRUD on the server: create, list, lookup by barcode, search by name.

**Dependencies:** Phase 1 (server scaffold, db module, routes file).

**Verification:** All endpoints are testable via curl/Postman. Create a product, verify it appears in list, look it up by barcode, search by partial name, attempt duplicate creation and observe 409.

| # | Task | Files | Evidence | Req / AC |
|---|------|-------|----------|----------|
| T-2-01 | Implement input validation middleware for product fields | `server/middleware/validate.js` | Export `validateProduct(req, res, next)` that checks: `name` non-empty string, `barcode` string length 13 (EAN-13), `price` positive number, `stock` non-negative integer. Calls `next()` on valid, or returns 422 JSON with `fields` map on invalid. Unit-testable in isolation. | REQ-product-001, AC-06 |
| T-2-02 | Implement `POST /api/products` — create product with duplicate barcode guard | `server/routes/products.js` | `POST /api/products` with valid body returns 201 with created product JSON. Duplicate barcode returns 409 with `error: "duplicate_barcode"` and `existing_product`. Missing/invalid fields return 422 with validation error map. | REQ-product-001, REQ-product-002, AC-06, AC-07 |
| T-2-03 | Implement `GET /api/products/:barcode` — lookup by barcode | `server/routes/products.js` | `GET /api/products/5901234567890` returns 200 with product JSON if found, 404 with `error: "not_found"` if not found. Response includes all fields: `id`, `barcode`, `name`, `price`, `stock`, `created_at`. | REQ-product-003, AC-04, AC-05 |
| T-2-04 | Implement `GET /api/products/search?q=...` — partial name search | `server/routes/products.js` | `GET /api/products/search?q=mil` returns array of products whose `name` contains "mil" (case-insensitive). Returns empty array if no matches. Requires minimum 2 characters in `q` param, else 422. Sorted alphabetically by name. | REQ-product-005, AC-10 |

---

## Phase 3: Product UI (frontend)

> **SUPERSEDED** — original Node.js plan replaced by Java/Spring Boot implementation. See auth phase tasks (T-auth-*) and design.md for actual architecture.

React components for browsing, searching, viewing, and creating products.

**Dependencies:** Phase 1 (frontend scaffold, API client), Phase 2 (backend CRUD endpoints).

**Verification:** Open browser. Product list renders with seeded data. Search filters in real-time. Tapping a product opens detail. Create a new product via the form — appears in list immediately.

| # | Task | Files | Evidence | Req / AC |
|---|------|-------|----------|----------|
| T-3-01 | Implement `App.jsx` view state management | `client/src/App.jsx` | Component manages `currentView` state key (`"home"`, `"scan"`, `"detail"`, `"create"`). Switches between sub-components based on view state. Provides `navigate(view, data)` prop to children. Renders without error. | — |
| T-3-02 | Implement `ProductList` component — scrollable product catalogue | `client/src/components/ProductList.jsx` | Fetches products from `GET /api/products` on mount. Renders each product as a row (name, barcode, stock, price). Scrollable container. Tapping a row navigates to detail view. Shows loading state while fetching. Shows empty state when catalogue is empty. | REQ-product-004, AC-11 |
| T-3-03 | Implement `SearchBar` component — debounced search input | `client/src/components/SearchBar.jsx` | Renders a text input. Emits search query after 200ms debounce. Calls `GET /api/products/search?q=...` and passes results up. Clear button (X icon or text) resets to full product list. Styled consistently with the app. | REQ-product-005, AC-10 |
| T-3-04 | Implement `ProductCard` component — detail display | `client/src/components/ProductCard.jsx` | Receives product object as prop. Displays product name, barcode, price, and stock count. Renders action buttons: "Update Stock", "Scan Another". Fetches fresh data from `GET /api/products/:barcode` on mount. | REQ-product-003, REQ-stock-001, AC-04 |
| T-3-05 | Implement `ProductForm` component — creation form with validation | `client/src/components/ProductForm.jsx` | Renders form fields: barcode (pre-filled, read-only), name (required, text input), price (required, number input), stock (required, number input, default 0). Client-side validation before submit: name non-empty, price > 0, stock >= 0. On valid submit, calls `POST /api/products`. On 409, navigates to existing product detail. On success, navigates to product detail. "Cancel" button returns to viewfinder or home. Shows field-level error messages. | REQ-product-001, REQ-product-002, AC-05, AC-06 |

---

## Phase 4: Stock management

> **SUPERSEDED** — original Node.js plan replaced by Java/Spring Boot implementation. See auth phase tasks (T-auth-*) and design.md for actual architecture.

Stock update endpoint and UI controls.

**Dependencies:** Phase 2 (product routes), Phase 3 (ProductCard component).

**Verification:** Open product detail. Tap +1 — stock increments. Tap -1 — stock decrements (disabled at 0). Tap "Update Stock" — enter absolute value — stock updates. Refresh page — stock persists.

| # | Task | Files | Evidence | Req / AC |
|---|------|-------|----------|----------|
| T-4-01 | Implement `PATCH /api/products/:barcode/stock` — absolute and delta stock update | `server/routes/stock.js` | `PATCH /api/products/:barcode/stock` accepts JSON body with `value` (absolute) or `delta` (+1/-1). `value` and `delta` are mutually exclusive — `value` takes precedence if both provided. Returns 200 with `{ barcode, stock, previous_stock }`. Negative result (delta that drives stock below 0) returns 422. Missing/empty body returns 422. Non-existent barcode returns 404. | REQ-stock-002, REQ-stock-003, REQ-stock-004, AC-08, AC-09 |
| T-4-02 | Implement `StockControls` component | `client/src/components/StockControls.jsx` | Renders: +1 button, -1 button (disabled when stock is 0), "Update Stock" button that opens absolute value numeric input. On +1 tap: calls `PATCH` with `{ delta: 1 }`, refreshes displayed stock. On -1 tap: calls `PATCH` with `{ delta: -1 }`, refreshes. On absolute value confirm: validates non-negative integer, calls `PATCH` with `{ value }`. Shows validation error inline for negative input. | REQ-stock-002, REQ-stock-003, REQ-stock-004, AC-08, AC-09 |
| T-4-03 | Implement success feedback indicator on stock update | `client/src/components/StockControls.jsx` (or shared util) | After any successful stock update, a brief visual indicator appears (green flash on stock count, checkmark icon, or brief pulse). Disappears automatically after 1-2 seconds. No indicator on error responses. | REQ-stock-006, AC-08 |

---

## Phase 5: Barcode scanning

> **SUPERSEDED** — original Node.js plan replaced by Java/Spring Boot implementation. See auth phase tasks (T-auth-*) and design.md for actual architecture.

Camera viewfinder, barcode decoding (native API + zxing-js fallback), manual entry fallback, and scan result routing.

**Dependencies:** Phase 1 (frontend scaffold, API client), Phase 3 (ProductCard, ProductForm).

**Verification:** Open app on a device with a camera. Viewfinder appears. Camera permission is requested. Denying permission shows error + manual entry. Scanning a known EAN-13 barcode shows product detail. Scanning an unknown barcode shows creation prompt with pre-filled barcode.

| # | Task | Files | Evidence | Req / AC |
|---|------|-------|----------|----------|
| T-5-01 | Implement `Viewfinder` component — camera stream | `client/src/components/Viewfinder.jsx` | Uses `navigator.mediaDevices.getUserMedia` to request camera access. Renders a `<video>` element with the live camera stream. Detects camera API availability on mount; if unavailable (`getUserMedia` not supported), renders fallback message. Emits stream-ready event/callback. Handles cleanup (stops tracks) on unmount. | REQ-scan-001, AC-02 |
| T-5-02 | Implement camera permission denied handling | `client/src/components/Viewfinder.jsx` | When `getUserMedia` fails with `NotAllowedError`, shows error message: "Camera access denied. Enable camera in your browser settings and refresh." Renders `ManualEntry` component alongside the error message as a fallback path. | REQ-scan-002, AC-02 |
| T-5-03 | Implement `BarcodeDecoder` component — EAN-13 decoding with fallback | `client/src/components/BarcodeDecoder.jsx` | Detects `'BarcodeDetector' in window`. If native: creates `BarcodeDetector({ formats: ['ean_13'] })`, runs detection on frames from the camera stream. If not supported: lazy-loads zxing-js, decodes frames via zxing `BrowserMultiFormatReader`. Emits `onDecode(barcodeString)` when a valid EAN-13 is decoded. Emits `onError(error)` on continuous decode failure. Highlights detected barcode area in the viewfinder. Runs detection at a reasonable interval (e.g., every 500ms, not every frame). | REQ-scan-003, REQ-scan-004, AC-03 |
| T-5-04 | Implement `ManualEntry` component — text input fallback | `client/src/components/ManualEntry.jsx` | Renders a text input for typing a barcode number. Submit calls `GET /api/products/:barcode`. Found product navigates to detail card. Not found navigates to creation prompt with barcode pre-filled. Input validates 13-digit EAN-13 format before submit. | REQ-scan-002, REQ-scan-004 |
| T-5-05 | Implement scan result routing — product found | `client/src/App.jsx` (routing logic) client `src/components/ScanResult.jsx` | When `BarcodeDecoder` emits a barcode, call `GET /api/products/:barcode`. If 200 (product exists): transition view to product detail card with the product data. Transition happens within 1 second of decode. Display duration 2 seconds (auto-return timer starts). | REQ-scan-005, AC-04, AC-13 |
| T-5-06 | Implement scan result routing — product not found (creation prompt) | `client/src/App.jsx` (routing logic), `client/src/components/CreatePrompt.jsx` | When `GET /api/products/:barcode` returns 404, show creation prompt: "Product not found. Add it to the catalogue?" with the detected barcode number. "Add Product" button navigates to `ProductForm` with barcode pre-filled. "Cancel" button returns to viewfinder. | REQ-scan-006, AC-05 |
| T-5-07 | Implement continuous scan mode and "Scan Another" action | `client/src/App.jsx`, `client/src/components/ProductCard.jsx` | After scan result display, auto-return to viewfinder after 2 seconds (default). "Scan Another" button on product card immediately returns to viewfinder. Camera stream stays active across scans — no reinitialisation. Previous scan result is replaced by the new result. Unlimited scan sequence supported. A settings toggle (or onboarding hint) explains the auto-return behaviour. | REQ-scan-007, REQ-scan-008, AC-13 |

---

## Phase 6: Integration and polish

> **SUPERSEDED** — original Node.js plan replaced by Java/Spring Boot implementation. See auth phase tasks (T-auth-*) and design.md for actual architecture.

Serve the production build from Express, enable cross-device access, handle error states, ensure responsive layout, and update README.

**Dependencies:** All previous phases (functionally complete app).

**Verification:** Run production build. Open `http://<local-ip>:<port>` from another device on the same network — app loads and works. Test on 375px viewport (mobile) and 1280px (desktop). Deny camera, observe fallback. Trigger decode failure, observe retry prompt.

| # | Task | Files | Evidence | Req / AC |
|---|------|-------|----------|----------|
| T-6-01 | Configure Vite build and static file serving from Express | `server/index.js`, `client/vite.config.js` | `npm run build` produces production build in `client/dist/`. Express serves `client/dist/` as static files. `GET /` returns `index.html`. `Cache-Control: no-cache` set on `index.html` to force fresh load. | AC-01 |
| T-6-02 | Bind server to local network IP for cross-device access | `server/index.js` | Server binds to `[IP_ADDRESS]` (or detects local IP) so devices on the same network can connect via `http://<ip>:<port>`. Server logs the accessible URL on startup. | AC-01, AC-14 |
| T-6-03 | Implement error state: camera denied / unavailable | `client/src/components/Viewfinder.jsx`, `client/src/App.jsx` | When `getUserMedia` fails with `NotAllowedError` or `NotFoundError`, show a distinct error UI with explanation and instructions. `ManualEntry` component is always reachable as an alternative. | REQ-scan-002, AC-02 |
| T-6-04 | Implement error state: decode failure retry | `client/src/components/BarcodeDecoder.jsx`, `client/src/App.jsx` | After a configurable timeout (e.g., 10 seconds with no successful decode), show a non-blocking retry hint. Viewfinder remains active. Manual entry option is always visible. | REQ-scan-004 |
| T-6-05 | Implement responsive layout (375px mobile, 1280px desktop) | `client/src/App.jsx`, `client/src/index.css` | CSS uses responsive breakpoints. All pages render without horizontal overflow at 375px width. Buttons and inputs are touch-target-sized (min 44px). At 1280px, content is centred/constrained with max-width container. No elements overlap or clip at either width. | AC-12 |
| T-6-06 | Update README with build, run, and usage instructions | `README.md` | README includes: project description, prerequisites (Node.js version), install steps (`npm install` in both `client/` and `server/`), run commands (dev and production), usage overview (scan, browse, search, update stock), and network access instructions. | AC-01 |

---

## Phase A — Authorization & Access Control

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1600–2200 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (server auth foundation + schema) → PR 2 (authorization + onboarding) → PR 3 (frontend auth + role views) |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

```text
Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High
```

### Phase A notes

The existing codebase already has **basic auth boilerplate** from the original v1 scope:
- `JwtUtil` — generates/validates JWT but uses hardcoded secret, no `role`/`userId` in claims
- `JwtAuthenticationFilter` — extracts token, loads user from DB every request
- `SecurityConfig` — permits `/api/auth/**` and `/api/health`, requires auth for everything else
- `AuthController` — login + register endpoints
- `User` entity — has `password` field (no `active`, no `passwordHash`)
- `UserRepository` — `findByUsername`, `save`, `count`
- `AuthContext` (frontend) — `login()`, `logout()`, stores token + user in `sessionStorage`
- `Login` component (frontend) — form with validation, error display
- `App.tsx` — gates all views behind token presence
- `Sidebar` — shows user info + logout but nav items are hardcoded (not role-based)

The auth tasks **modify** these existing files and **add** new ones for scope enforcement, user management, onboarding flow, and role-aware UI.

---

### T-auth-01: Refactor JwtUtil — read secret from env, embed role + userId in claims

- **Description**: Modify `JwtUtil` to read `JWT_SECRET` from environment variable. Fail at startup if unset or shorter than 32 bytes. Add `userId` and `role` as claims in `generateToken()`. Add `getUserIdFromToken()` and `getRoleFromToken()` methods. Keep existing `getUsernameFromToken()` (alias for `sub`).
- **Files affected**:
  - `server/src/main/java/com/makmur/config/JwtUtil.java` (modify)
- **Dependencies**: None (phase foundation)
- **Complexity**: S
- **Acceptance criteria**:
  - Server fails to start with `IllegalStateException` if `JWT_SECRET` env var is unset
  - Server fails to start if `JWT_SECRET` is shorter than 32 bytes
  - Generated JWT contains `sub`, `role`, `userId`, `iat`, `exp` claims
  - `getUserIdFromToken()` returns the userId claim
  - `getRoleFromToken()` returns the role claim
  - Token expiry ≤ 86,400,000ms (24h) from issued-at
  - Existing `validateToken()` and `getUsernameFromToken()` still work
  - REQ-auth-007, AC-25

---

### T-auth-02: Add active-flag check to JwtAuthenticationFilter

- **Description**: Modify `JwtAuthenticationFilter` to check the user's `active` flag after extracting the JWT. If `active == 0`, do not set SecurityContext (effectively denying the request). Load user data from DB for the `active` check but derive role from JWT claims (not DB) to avoid query-per-request for role info.
- **Files affected**:
  - `server/src/main/java/com/makmur/config/JwtAuthenticationFilter.java` (modify)
- **Dependencies**: T-auth-01 (JwtUtil claims), T-auth-06 (User entity with active field)
- **Complexity**: S
- **Acceptance criteria**:
  - Filter extracts `userId`, `role`, `username` from JWT claims
  - Filter checks `active` flag from DB before setting SecurityContext
  - Deactivated user with valid JWT gets `403` on first API call after deactivation
  - Active user flows through as before
  - REQ-auth-013, AC-28

---

### T-auth-03: Refactor User entity — rename password→passwordHash, add active field

- **Description**: Rename the `password` field to `passwordHash` in the `User` entity. Add `active` boolean field (default `true`). Update getters/setters accordingly.
- **Files affected**:
  - `server/src/main/java/com/makmur/entity/User.java` (modify)
- **Dependencies**: None (entity change)
- **Complexity**: S
- **Acceptance criteria**:
  - `User` entity has `passwordHash` field (not `password`)
  - `User` entity has `active` field (`boolean`, default `true`)
  - Existing constructors updated to reflect new field name
  - Compiles without errors

---

### T-auth-04: Extend UserRepository — scoped findAll, updateActive, updatePassword, findByRole

- **Description**: Add methods to `UserRepository`: `findAll()` returns all users, `findAllByRole(String role)` returns users filtered by role, `updateActiveStatus(Long id, boolean active)` sets the `active` flag, `updatePassword(Long id, String passwordHash)` updates the password hash.
- **Files affected**:
  - `server/src/main/java/com/makmur/repository/UserRepository.java` (modify)
- **Dependencies**: T-auth-03 (User entity with active + passwordHash)
- **Complexity**: M
- **Acceptance criteria**:
  - `findAll()` returns all user rows
  - `findAllByRole("staff")` returns only staff rows
  - `updateActiveStatus(3, false)` sets `active = 0` for user id 3
  - `updatePassword(3, "$2a$...")` updates password_hash for user id 3
  - `save()` uses `password_hash` column name (not `password`)
  - All methods use column name `password_hash`, not `password`

---

### T-auth-05: Create AuthService — scope enforcement utility

- **Description**: Add `AuthService` with a `requireRole(String... roles)` method. It reads the current `Authentication` from `SecurityContextHolder`, extracts the GrantedAuthority set, and throws `AccessDeniedException` (→ 403) if the caller's role doesn't match any permitted role. Returns the Authentication object on success for callers needing user context.
- **Files affected**:
  - `server/src/main/java/com/makmur/service/AuthService.java` (add)
- **Dependencies**: T-auth-01 (JwtUtil), T-auth-02 (filter sets authorities)
- **Complexity**: S
- **Acceptance criteria**:
  - `authService.requireRole("admin", "staff")` succeeds for admin and staff, throws `AccessDeniedException` for manager
  - `authService.requireRole("admin")` succeeds for admin, throws for manager and staff
  - `AccessDeniedException` results in HTTP 403 response with `{ "error": "forbidden", "message": "Access denied" }`
  - Returns Authentication on success for callers needing userId/role
  - No DB queries performed by the utility
  - REQ-auth-003, AC-21

---

### T-auth-06: Refactor AuthController — active-flag check, manager-safe register

- **Description**: Modify `AuthController.login()` to check `user.isActive()` after password verification. If deactivated, return 403 with `account_disabled`. Modify `AuthController.register()` to accept a calling-user context (or gate behind auth) — the controller will be superseded by `UserController` for manager/admin user creation, but keep register for testability. Ensure role field from client is never trusted for manager-created users.
- **Files affected**:
  - `server/src/main/java/com/makmur/controller/AuthController.java` (modify)
- **Dependencies**: T-auth-03 (User entity active field), T-auth-04 (UserRepository updates)
- **Complexity**: M
- **Acceptance criteria**:
  - Login with valid creds + active user → 200 with JWT
  - Login with valid creds + deactivated user → 403 with `account_disabled`
  - Login with invalid creds → 401 with `invalid_credentials` (generic, no user enumeration)
  - Missing/blank username or password → 422
  - REQ-auth-001, REQ-auth-006, AC-15, AC-24, AC-28

---

### T-auth-07: Create UserController — user management endpoints

- **Description**: Add `UserController` with endpoints: `GET /api/users` (list, scoped by role — manager sees only staff, admin sees all), `POST /api/users` (create user — manager hard-codes staff role, admin can set staff/manager), `PATCH /api/users/:id/deactivate` (set active=0), `PATCH /api/users/:id/reactivate` (set active=1), `PATCH /api/users/:id/reset-password` (set new password hash). All endpoints call `authService.requireRole("admin", "manager")`. Manager endpoints enforce target-role checks (can only act on staff users).
- **Files affected**:
  - `server/src/main/java/com/makmur/controller/UserController.java` (add)
- **Dependencies**: T-auth-04 (UserRepository), T-auth-05 (AuthService), T-auth-06 (AuthController pattern)
- **Complexity**: L
- **Acceptance criteria**:
  - `GET /api/users` as admin → 200 with all users (including managers)
  - `GET /api/users` as manager → 200 with only staff users
  - `GET /api/users` as staff → 403
  - `POST /api/users` as manager → creates user with role='staff' (ignores client role field)
  - `POST /api/users` as admin → creates user with role='staff' or 'manager'
  - `PATCH /api/users/:id/deactivate` as manager on staff → 200
  - `PATCH /api/users/:id/deactivate` as manager on manager → 403
  - `PATCH /api/users/:id/reset-password` → 200, password hash updated
  - Duplicate username → 409
  - REQ-auth-003, REQ-auth-004, AC-18, AC-19, AC-20, AC-21

---

### T-auth-08: Update SecurityConfig — add setup endpoints to permitAll, add /api/users to secure

- **Description**: Modify `SecurityConfig` to add `/api/setup/**` to `permitAll()` (alongside `/api/auth/**` and `/api/health`). Ensure `/api/users/**` requires authentication. No role-based routing here — all scope is handler-level. Remove the hardcoded permit-all on `/api/auth/register` if it needs auth (design says user creation is manager/admin-only).
- **Files affected**:
  - `server/src/main/java/com/makmur/config/SecurityConfig.java` (modify)
- **Dependencies**: T-auth-01 (JwtUtil), T-auth-02 (JwtAuthFilter)
- **Complexity**: S
- **Acceptance criteria**:
  - `/api/setup/token`, `/api/setup/qr`, `/api/setup/register` are accessible without auth
  - `/api/users/**` requires valid JWT
  - `/api/auth/login`, `/api/health` remain public
  - All other endpoints require valid JWT (unchanged)
  - No role-based route exclusions exist (scope is handler-level)
  - REQ-auth-002, AC-16

---

### T-auth-09: Apply scope checks to ProductController endpoints

- **Description**: Add `authService.requireRole("admin", "staff")` at the top of each handler method in `ProductController` (listAll, search, getByBarcode, create, updateStock). Manager role gets 403 on all product endpoints.
- **Files affected**:
  - `server/src/main/java/com/makmur/controller/ProductController.java` (modify)
- **Dependencies**: T-auth-05 (AuthService), T-auth-02 (JwtAuthFilter sets context)
- **Complexity**: S
- **Acceptance criteria**:
  - Staff and admin can access all product endpoints (200)
  - Manager gets 403 on all product endpoints (list, search, lookup, create, stock update)
  - Unauthenticated requests still get 401 (from filter)
  - REQ-auth-003, AC-18, AC-19, AC-20, AC-21

---

### T-auth-10: Apply scope checks to HealthController (public, all roles)

- **Description**: `HealthController` remains public — no scope check needed. Verify it still works for all roles and unauthenticated requests.
- **Files affected**:
  - `server/src/main/java/com/makmur/controller/HealthController.java` (verify — no change needed)
- **Dependencies**: None
- **Complexity**: XS
- **Acceptance criteria**:
  - `GET /api/health` returns 200 for all roles and unauthenticated requests
  - No scope check on this endpoint

---

### T-auth-11: Schema migration — rename password→password_hash, add active, extend role CHECK

- **Description**: Update `schema.sql` with new users table schema: rename `password` to `password_hash`, add `active INTEGER NOT NULL DEFAULT 1`, extend `role` CHECK to include `'manager'`. Add migration logic in `Application.java` or a dedicated migration runner: detect old schema, create new table, copy data, drop old table. Remove the `CommandLineRunner` default admin seed (superseded by QR onboarding).
- **Files affected**:
  - `server/src/main/resources/schema.sql` (modify)
  - `server/src/main/java/com/makmur/Application.java` (modify — remove default admin seed)
  - (optional) `server/src/main/java/com/makmur/config/SchemaMigration.java` (add)
- **Dependencies**: T-auth-03 (User entity), T-auth-04 (UserRepository uses password_hash)
- **Complexity**: M
- **Acceptance criteria**:
  - `users` table has columns: `id`, `username`, `password_hash`, `role`, `active`, `created_at`
  - `role` CHECK constraint allows `'admin'`, `'manager'`, `'staff'`
  - `role` CHECK rejects invalid values (e.g., `'superadmin'`)
  - `active` defaults to `1` (active)
  - Existing data from `password` column is copied to `password_hash` during migration
  - Default admin seed is removed — admin creation flows through QR onboarding only
  - Products table is unchanged
  - REQ-auth-008, AC-26

---

### T-auth-12: Create SetupController — QR onboarding flow

- **Description**: Add `SetupController` with three endpoints: `GET /api/setup/token` returns one-time UUID token (only when no admin exists, 403 otherwise), `GET /api/setup/qr` returns a QR code PNG (encoding setup URL + token), `POST /api/setup/register` accepts token + username + password, creates admin user with bcrypt-hashed password, invalidates token. Token stored in-memory with 60-minute TTL. Add a QR code generation dependency (`com.google.zxing:core` + `javase`) or embed a minimal QR generator.
- **Files affected**:
  - `server/src/main/java/com/makmur/controller/SetupController.java` (add)
  - `server/pom.xml` (add zxing dependency)
  - `server/src/main/java/com/makmur/config/SetupTokenStore.java` (add — in-memory token store)
- **Dependencies**: T-auth-04 (UserRepository), T-auth-11 (schema migration for admin check)
- **Complexity**: L
- **Acceptance criteria**:
  - `GET /api/setup/token` with no admin → 200 with `{ token, expires_at }`
  - `GET /api/setup/token` with existing admin → 403 `already_setup`
  - `GET /api/setup/qr` with no admin → 200 `image/png`
  - `POST /api/setup/register` with valid token → 201, admin created, token invalidated
  - `POST /api/setup/register` with invalid/expired token → 403 `invalid_token`
  - `POST /api/setup/register` after admin already exists → 403 even with valid token (race-condition guard)
  - Token TTL: 60 minutes from server start
  - Server restart generates a new token
  - REQ-auth-005, AC-22, AC-23

---

### T-auth-13: Update SecurityConfig — add /api/setup/** to permitAll

- **Description**: Merge into T-auth-08 or add separately. Ensures setup endpoints are accessible without auth.
- **Files affected**: Same as T-auth-08 (merged)
- **Dependencies**: T-auth-12 (SetupController exists)
- **Complexity**: XS (merged with T-auth-08)
- **Acceptance criteria**: Setup endpoints work without authentication

---

### T-auth-14: Refactor AuthContext — add JWT expiry detection, role-aware user type, 401 interception

- **Description**: Modify `AuthContext` to decode the JWT payload client-side (base64-decode the body) and extract `exp` and `role`. Add a check on init: if `exp` is in the past, clear token and user. Add `fetchWithAuth` wrapper that redirects to `/login` on 401 responses. Ensure `user.role` is typed and available for role-based rendering.
- **Files affected**:
  - `client/src/context/AuthContext.tsx` (modify)
  - `client/src/types.ts` (modify — ensure User has typed `role: 'admin' | 'manager' | 'staff'`)
- **Dependencies**: None (can be done in parallel with server work)
- **Complexity**: M
- **Acceptance criteria**:
  - On init: if stored JWT `exp` is past, clear token → user sees login page
  - Proactive expiry check before API calls avoids unnecessary 401 round-trips
  - `fetchWithAuth` returns the response normally on 2xx/4xx/5xx
  - 401 responses from any API call trigger token clear + redirect to `/login`
  - No automatic retry on 401
  - `user.role` is available and typed for role-based rendering
  - REQ-auth-009, REQ-auth-010, AC-27, AC-29

---

### T-auth-15: Add 401 interceptor to api.ts

- **Description**: Modify `apiFetch` in `api.ts` to call a provided `onUnauthorized` callback (or redirect directly) when any API response is 401. This ensures that expired-token API calls trigger the login redirect from any component, not just AuthContext-aware code.
- **Files affected**:
  - `client/src/api.ts` (modify)
- **Dependencies**: T-auth-14 (AuthContext provides the callback or redirect mechanism)
- **Complexity**: S
- **Acceptance criteria**:
  - Any API call returning 401 clears token from sessionStorage
  - After 401, user is redirected to `/login`
  - Non-401 errors pass through unchanged to the caller
  - REQ-auth-010, AC-27

---

### T-auth-16: Update Login component — deactivated-account error display

- **Description**: Modify `Login` component to distinguish `account_disabled` errors from generic `invalid_credentials`. Show distinct message for deactivated accounts: "Account is deactivated. Contact your manager." Keep generic "Invalid username or password" for wrong credentials.
- **Files affected**:
  - `client/src/components/Login.tsx` (modify)
- **Dependencies**: T-auth-14 (AuthContext login throws structured errors)
- **Complexity**: S
- **Acceptance criteria**:
  - `account_disabled` error shows distinct message with "Contact your manager" guidance
  - `invalid_credentials` shows generic "Invalid username or password"
  - Form disabled during submission (loading state)
  - Other error states show the error message from the response
  - REQ-auth-009, REQ-auth-013, AC-28, AC-29

---

### T-auth-17: Add SetupPage component — QR display and setup form

- **Description**: Add `SetupPage` component with two modes. Mode 1 — QR display: when no admin exists, fetch setup token from `GET /api/setup/token`, render QR code (via qrcode.js or <img> from `/api/setup/qr`) with instructions. Mode 2 — Setup form: when `?token=...` is in URL, show username + password + confirm password fields. On submit, POST to `/api/setup/register`. On success, redirect to login with success message.
- **Files affected**:
  - `client/src/components/SetupPage.tsx` (add)
  - `client/src/App.tsx` (modify — add setup view routing)
- **Dependencies**: T-auth-12 (SetupController), T-auth-14 (AuthContext)
- **Complexity**: M
- **Acceptance criteria**:
  - Mode 1: QR code image is rendered from `/api/setup/qr` endpoint
  - Mode 1: Instructions text is visible: "Scan this QR code with your phone to set up the admin account"
  - Mode 2: Setup form has username, password, confirm password fields
  - Mode 2: Password minimum length validation (8 chars) on client side
  - Mode 2: Passwords must match (confirm password check)
  - Successful setup redirects to login page with success message
  - Failed setup (invalid token) shows error message
  - REQ-auth-005, AC-22, AC-23

---

### T-auth-18: Add setup view routing in App.tsx

- **Description**: Modify `App.tsx` to check if setup is needed (no token, and `GET /api/setup/token` returns 200). If setup needed, show `SetupPage` in QR display mode. If `?token=` in URL, show `SetupPage` in form mode. Otherwise show `Login` page. After login, route to role-appropriate home view.
- **Files affected**:
  - `client/src/App.tsx` (modify)
- **Dependencies**: T-auth-17 (SetupPage component), T-auth-14 (AuthContext)
- **Complexity**: M
- **Acceptance criteria**:
  - First visit with empty users table → shows setup page (QR mode)
  - Visit with `?token=abc` → shows setup form mode
  - Visit with existing admin + no token → shows login page
  - After login, user is routed based on role:
    - staff → product catalogue / scan view
    - manager → user management view
    - admin → dashboard with nav to both
  - Logged-in user with role-appropriate default view
  - REQ-auth-009, REQ-auth-011, AC-29

---

### T-auth-19: Update Sidebar — role-based navigation items

- **Description**: Modify `Sidebar` to filter `navItems` based on `user.role`. Staff sees: Dashboard, Products, Scan. Manager sees: Dashboard, Users. Admin sees: Dashboard, Products, Scan, Users. Add a view type for `'users'` to the View type union. Keep user badge and logout button in footer for all roles.
- **Files affected**:
  - `client/src/components/Sidebar.tsx` (modify)
  - `client/src/App.tsx` (modify — add `'users'` to View type and render user management content)
- **Dependencies**: T-auth-14 (AuthContext provides user.role)
- **Complexity**: M
- **Acceptance criteria**:
  - Staff sidebar: Dashboard, Products, Scan (no Users)
  - Manager sidebar: Dashboard, Users (no Products, no Scan)
  - Admin sidebar: Dashboard, Products, Scan, Users
  - Logout button visible at all times alongside username/role badge
  - REQ-auth-011, AC-18, AC-19, AC-20

---

### T-auth-20: Create UserList component — manager/admin user management view

- **Description**: Add `UserList` component showing a table of users with columns: Username, Role (badge), Status (active/deactivated badge), Actions. Actions: Deactivate/Reactivate toggle button, Reset Password button. Add `CreateUserForm` modal or inline form with username, password, and (for admin) role selector. Add API functions to `api.ts` for user management calls.
- **Files affected**:
  - `client/src/components/UserList.tsx` (add)
  - `client/src/components/CreateUserForm.tsx` (add)
  - `client/src/api.ts` (modify — add fetchUsers, createUser, deactivateUser, reactivateUser, resetPassword)
  - `client/src/types.ts` (modify — add User type)
- **Dependencies**: T-auth-07 (UserController), T-auth-14 (AuthContext for role), T-auth-19 (Sidebar navigation)
- **Complexity**: L
- **Acceptance criteria**:
  - User table renders with username, role badge, active status
  - Admin sees all users; manager sees only staff users
  - Deactivate button sets user inactive (PATCH /deactivate), refresh list
  - Reactivate button sets user active (PATCH /reactivate)
  - Reset Password opens prompt, sends PATCH /reset-password
  - Create User form: admin sees role selector; manager does not
  - Confirmation dialog before deactivation
  - REQ-auth-004, AC-18, AC-19, AC-20

---

### T-auth-21: Add API types and functions for user management

- **Description**: Add TypeScript types for `User` (id, username, role, active, created_at) and API functions: `fetchUsers()`, `createUser(data)`, `deactivateUser(id)`, `reactivateUser(id)`, `resetPassword(id, newPassword)`. Wire the 401 interceptor (from T-auth-15) into these new functions.
- **Files affected**:
  - `client/src/types.ts` (modify — add User interface)
  - `client/src/api.ts` (modify — add user management functions)
- **Dependencies**: T-auth-15 (api.ts 401 interceptor)
- **Complexity**: S
- **Acceptance criteria**:
  - `fetchUsers()` calls `GET /api/users` and returns typed User[]
  - `createUser({username, password, role?})` calls `POST /api/users`
  - `deactivateUser(3)` calls `PATCH /api/users/3/deactivate`
  - `reactivateUser(3)` calls `PATCH /api/users/3/reactivate`
  - `resetPassword(3, "newpass")` calls `PATCH /api/users/3/reset-password`
  - All functions include Authorization header and handle 401 redirect
  - REQ-auth-004

---

### T-auth-22: Add auth-domain tests — server-side

- **Description**: Add integration tests for all auth endpoints in `ProductControllerTest` style. Test login (valid, invalid, deactivated), scope enforcement (staff→products, manager→users, admin→all, staff→users 403, manager→products 403), setup flow (token, QR, register, token invalidation), user management CRUD.
- **Files affected**:
  - `server/src/test/java/com/makmur/controller/AuthControllerTest.java` (add)
  - `server/src/test/java/com/makmur/controller/UserControllerTest.java` (add)
  - `server/src/test/java/com/makmur/controller/SetupControllerTest.java` (add)
  - `server/src/test/java/com/makmur/controller/ProductControllerTest.java` (modify — add scope enforcement tests)
- **Dependencies**: T-auth-06 (AuthController), T-auth-07 (UserController), T-auth-09 (scope checks), T-auth-12 (SetupController)
- **Complexity**: M
- **Acceptance criteria**:
  - Login: valid creds → 200 + JWT; invalid creds → 401; deactivated → 403
  - JWT claims: sub, role, userId, iat, exp present; exp - iat ≤ 24h
  - Staff: products OK → 200; users → 403
  - Manager: users OK → 200; products → 403
  - Admin: products → 200; users → 200
  - No JWT on protected endpoint → 401
  - Expired/malformed JWT → 401
  - Setup: token → 200 (no admin) / 403 (admin exists); register → 201; token invalidated
  - User CRUD: create, deactivate, reactivate, reset-password all scoped correctly
  - Password stored as bcrypt hash (starts with `$2a$` or `$2b$`)
  - All AC-15 through AC-28 covered

---

### T-auth-23: Remove default admin seed from Application.java

- **Description**: Remove the `CommandLineRunner` that seeds the default admin user (admin/admin123) in `Application.java`. This is superseded by the QR onboarding flow. The server should detect an empty users table and serve the setup flow instead.
- **Files affected**:
  - `server/src/main/java/com/makmur/Application.java` (modify)
- **Dependencies**: T-auth-11 (schema migration), T-auth-12 (SetupController handles empty DB)
- **Complexity**: XS
- **Acceptance criteria**:
  - Server starts without creating default admin user
  - Empty users table → setup endpoints are accessible
  - Existing test's `BeforeEach` login still works (tests should create their own test users if needed)
  - REQ-auth-005

---

### T-auth-24: Update WebConfig or CORS if needed for setup endpoints

- **Description**: Verify that CORS configuration in `WebConfig` covers the new endpoints (`/api/setup/**`, `/api/users/**`). If using the wildcard `/api/**` pattern, no change needed. Verify in development.
- **Files affected**:
  - `server/src/main/java/com/makmur/config/WebConfig.java` (verify — likely no change)
- **Dependencies**: T-auth-12 (SetupController)
- **Complexity**: XS
- **Acceptance criteria**: Setup endpoints accessible from frontend in dev mode without CORS errors

---

### T-auth-25: Add auth styles to index.css

- **Description**: Add CSS classes for setup page (QR display, setup form), user management table, role badges, active/deactivated status indicators, and any other auth-specific UI elements referenced by new components.
- **Files affected**:
  - `client/src/index.css` (modify)
- **Dependencies**: T-auth-17 (SetupPage), T-auth-20 (UserList, CreateUserForm)
- **Complexity**: S
- **Acceptance criteria**:
  - Setup page renders correctly on mobile (375px) and desktop (1280px)
  - User table has consistent row styling with role badges
  - Active/deactivated status has distinct visual (green/red text or badge)
  - Role badges are styled (admin=purple, manager=blue, staff=gray)
  - No layout breakage on existing pages

---

## Verification summary (auth phase)

| Criterion | How verified |
|-----------|-------------|
| AC-15 | T-auth-22 integration test |
| AC-16 | T-auth-22 integration test |
| AC-17 | T-auth-22 integration test |
| AC-18 | T-auth-22 integration test (staff→products 200, staff→users 403) |
| AC-19 | T-auth-22 integration test (manager→users 200, manager→products 403) |
| AC-20 | T-auth-22 integration test (admin→products 200, admin→users 200) |
| AC-21 | Architecture review (T-auth-05, T-auth-09) |
| AC-22 | T-auth-22 integration test + manual (T-auth-17) |
| AC-23 | T-auth-22 integration test |
| AC-24 | T-auth-22 integration test (bcrypt prefix check) |
| AC-25 | T-auth-22 integration test (JWT decode + assertions) |
| AC-26 | T-auth-22 schema test |
| AC-27 | T-auth-14 (expiry detection) + T-auth-15 (401 interceptor) |
| AC-28 | T-auth-06 (login check) + T-auth-02 (filter check) |
| AC-29 | T-auth-18 (routing logic) |
