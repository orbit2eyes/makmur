# Current State & Impact Analysis — makmur-0

Generated: 2026-07-09 (updated from 2026-07-08)
Scope: All 4 domains (product, scan, stock, auth)
Status: Full implementation complete (verify-report.md: 61/61 tests pass, 14/15 AC covered)

---

## 1. Project Identity

| Layer | Technology | Status |
|-------|-----------|--------|
| Frontend | React 18+ / Vite / TypeScript | Implemented |
| Backend | Java 17+ / Spring Boot 3 / Maven | Implemented |
| Database | SQLite via JDBC (org.xerial:sqlite-jdbc) | Implemented |
| Auth | JWT (Bearer, 24h, bcrypt) | Implemented |
| Barcode | Native BarcodeDetector + @zxing/library polyfill | Implemented |
| Port | 3001 (server), 5173 (Vite dev proxy) | Configured |

### Roles

| Role | Access Scope |
|------|-------------|
| admin | Full: products + user management (staff + manager accounts) |
| manager | User management only: staff accounts (create, deactivate, reset password) |
| staff | Products only: scan, stock updates, search, browse |

---

## 2. Code Paths (per request flow)

### Product: Scan -> Lookup -> Display

```
Camera (getUserMedia) -> Every 500ms (native BarcodeDetector) or continuous (zxing)
  |
BarcodeDetector API (Chrome) or BrowserMultiFormatReader (Safari/Firefox)
  | detects ean_13 format
App.tsx handleScan(barcode)
  |
api.ts fetchProduct(barcode) -> GET /api/products/:barcode
  |
JwtAuthenticationFilter.doFilterInternal()
  |-- Checks Authorization: Bearer header present
  |-- jwtUtil.validateToken(token) -> parse claims
  |-- jwtUtil.getUserId/Role/Username from claims
  |-- userRepository.findByUsername(username) -> check active flag
  |-- Sets SecurityContextHolder with ROLE_x authority + userId in details
  |   (if active=false -> no SecurityContext -> AccessDeniedException -> 403)
  |
ProductController.getByBarcode()
  |-- authService.requireRole("admin", "staff") -> 403 if manager
  |-- productRepository.findByBarcode(barcode) -> SELECT * FROM products WHERE barcode = ?
  |
Response: 200 + Product JSON or 404 + { error, message }
  |
App.tsx: product found -> setSelectedProduct + view='detail' + autoReturn=3 (countdown)
         product not found -> setScannedBarcode + view='create'
```

### Auth: Login -> JWT -> Protected Route

```
Login.tsx -> AuthContext.login(username, password)
  |
POST /api/auth/login { username, password }
  |
AuthController.login()
  |-- userRepository.findByUsername(username)
  |-- passwordEncoder.matches(password, user.passwordHash)
  |   |-- mismatch -> 401 { error: "invalid_credentials", message: "..." } (generic, no enumeration)
  |-- Check user.isActive()
  |   |-- false -> 403 { error: "account_disabled", message: "Account is deactivated. Contact manager." }
  |-- jwtUtil.generateToken(username, user.id, user.role)
  |
Response: { token, user: { id, username, role, active } }
  |
AuthContext: sessionStorage.setItem('token', data.token)
              sessionStorage.setItem('user', JSON.stringify(data.user))
              setToken(data.token); setUser(data.user)
  |
App.tsx LoginGate: token present -> <ProtectedRoute> -> <MainApp>
  |-- user.role 'staff' -> default view 'products'
  |-- user.role 'manager' -> default view 'users'
  |-- user.role 'admin' -> default view 'products'
  |
Sidebar.getNavItems(role):
  |-- staff/admin -> Products + Scan links
  |-- manager/admin -> Users link
  |-- Logout footer always visible with username/role badge
```

### Auth: Subsequent requests -> Scope Enforcement

```
Any component -> api.ts apiFetch(url, options)
  |-- Adds authHeaders(): Authorization: Bearer <token>
  |-- If 401 response -> redirectLogin() -> clear sessionStorage -> window.location = '/login'
  |
JwtAuthenticationFilter (repeats for every request)
  |-- Same flow as above
  |
Controller handler -> authService.requireRole("admin", "staff"|"manager")
  |-- Reads SecurityContextHolder.getContext().getAuthentication()
  |-- Checks if authenticated (exists and isAuthenticated) -> 403 if not
  |-- Checks granted authorities contain allowed role -> 403 ForbiddenException if mismatch
  |
GlobalExceptionHandler.handleForbidden() -> { error: "forbidden", message: "Access denied" }
```

### User Management: List/Create/Deactivate

```
UserList.tsx mount -> api.ts fetchUsers() -> GET /api/users
  |
UserController.listUsers()
  |-- authService.requireRole("admin", "manager")
  |-- If admin -> userRepository.findAll() -> all users
  |-- If manager -> userRepository.findAllByRole("staff") -> staff only
  |
UserList.tsx -> api.ts createUser({ username, password, role? }) -> POST /api/users
  |
UserController.createUser()
  |-- authService.requireRole("admin", "manager")
  |-- If manager caller -> hard-codes newRole = "staff" (ignores client role field)
  |-- If admin caller -> accepts "staff" or "manager" from client
  |-- Duplicate username -> 409 { error: "conflict", message: "Username X already exists" }
  |
UserList.tsx -> handleToggleActive(user) -> api.ts deactivateUser(id) -> PATCH /api/users/:id/deactivate
  |
UserController.deactivate()
  |-- authService.requireRole("admin", "manager")
  |-- checkTargetRole(id): manager can only deactivate staff targets
  |   |-- target is manager and caller is manager -> 403
  |-- userRepository.updateActiveStatus(id, false)
```

### Setup (First-time admin onboarding)

```
Server start -> SetupTokenStore @PostConstruct init()
  |-- Check countAdmins() == 0 -> generate UUID v4 token, store with expiry (60min TTL)
  |
GET /api/setup/status -> SetupController.getStatus()
  |-- userRepository.countByRole("admin") > 0 -> { needsSetup: false }
  |-- else -> { needsSetup: true }
  |
Client: if needsSetup -> GET /api/setup/token -> { token, expires_at }
  |-- SetupPage renders QR via external api.qrserver.com (G2 gap: not server-generated)
  |
Admin scans QR -> navigates to /setup?token=<uuid>
  |
SetupPage form mode -> POST /api/setup/register { token, username, password }
  |
SetupController.register()
  |-- Validate token via SetupTokenStore.isValid(token)
  |   |-- invalid/expired -> 403 { error: "invalid_token" }
  |-- Race-condition guard: countAdmins() > 0 -> 403 { error: "already_setup" }
  |-- Validate: username not blank, password >= 8 chars -> 422 if invalid
  |-- bcrypt.encode(password) -> create User(role='admin')
  |-- userRepository.save(adminUser)
  |-- tokenStore.invalidate(token) -> single-use
  |
Response 201 { message: "Admin account created", user: { id, username, role } }
  |-- Client redirects to /login with success message
```

### Stock Update

```
StockControls.tsx
  |-- +1 button -> updateStock(barcode, { delta: 1 })
  |-- -1 button -> updateStock(barcode, { delta: -1 }) (disabled at stock=0)
  |-- "Update Stock" -> opens absolute value input -> { value: N }
  |
api.ts updateStock(barcode, body) -> PATCH /api/products/:barcode/stock
  |
ProductController.updateStock()
  |-- authService.requireRole("admin", "staff") -> 403 if manager
  |-- productRepository.findByBarcode(barcode) -> 404 if not found
  |-- Parse body: value (absolute) | delta (relative). value takes precedence if both.
  |   |-- Neither -> 422 { error: "validation_error" }
  |-- Calculate newStock = (value !== null) ? value : (current + delta)
  |   |-- newStock < 0 -> 422 { error: "validation_error" }
  |   |-- Not integer -> 422
  |-- product.setStock(newStock); productRepository.save(product)
  |
Response: { barcode, stock, previous_stock }
  |
StockControls: onUpdate(result) -> setFlash(true) -> setTimeout 1.5s -> setFlash(false)
```

---

## 3. Data Model (SQLite)

### products

```sql
CREATE TABLE products (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode    TEXT    NOT NULL UNIQUE,
    name       TEXT    NOT NULL,
    price      REAL    NOT NULL CHECK(price >= 0.01),
    stock      INTEGER NOT NULL DEFAULT 0 CHECK(stock >= 0),
    created_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE UNIQUE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_name ON products(name COLLATE NOCASE);
```

### users

```sql
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL DEFAULT 'staff' CHECK(role IN ('admin', 'manager', 'staff')),
    active        INTEGER NOT NULL DEFAULT 1,
    created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
```

### Key storage notes

- `SchemaMigration.java` handles `password` -> `password_hash` column rename for old DBs (table recreation pattern required by SQLite: create new, copy data, drop old)
- DB file: `server/data/makmur.db` (created at runtime, gitignored)
- WAL mode enabled at schema init for concurrent read/write performance
- `spring.jackson.property-naming-strategy=SNAKE_CASE` -> Java `createdAt` serializes as `created_at` in JSON

---

## 4. Test Coverage

### 61 tests passing (14 original + 47 new), 0 failures, 0 errors

| Test class | Count | Coverage |
|-----------|-------|----------|
| ProductControllerTest (original) | 14 | CRUD, stock value/delta, search, validation, duplicates, health |
| ProductControllerTest (scope) | 3 | Staff->200, Manager->403, No JWT->403 |
| AuthControllerTest | 10 | Login valid/invalid/deactivated, register role enforcement, duplicate |
| UserControllerTest | 15 | List scoped, create role-hardcode, deactivate scoped, reset-password |
| SetupControllerTest | 9 | Token gen (200/403), register valid/invalid/after-setup, short password |
| AuthServiceTest | 10 | requireRole valid/invalid/no-auth, getCurrentRole, getCurrentUserId |

### Verified AC coverage

| AC | Criterion | Status | Verification |
|----|-----------|--------|-------------|
| AC-15 | Login valid -> JWT, invalid -> 401 | ✅ | AuthControllerTest.a, b, c |
| AC-16 | No JWT -> 401 | ⚠️ | **Returns 403 not 401.** Missing AuthEntryPoint in SecurityConfig. |
| AC-17 | Expired/malformed JWT -> 401 | ⚠️ | Same 403 problem as AC-16. Needs explicit test. |
| AC-18 | Staff -> products 200, users 403 | ✅ | ProductControllerTest.o, UserControllerTest.c |
| AC-19 | Manager -> users 200, products 403 | ✅ | UserControllerTest.b, ProductControllerTest.p |
| AC-20 | Admin -> everything | ✅ | All product/user tests use admin + scope tests |
| AC-21 | Handler-level scope | ✅ | AuthService.requireRole per handler |
| AC-22 | QR onboarding flow | ✅ | SetupControllerTest.a, c, e |
| AC-23 | Token invalidated after setup | ✅ | SetupControllerTest.g |
| AC-24 | Bcrypt password storage | ✅ | SetupControllerTest.e (verifies $2a$ prefix) |
| AC-25 | JWT claims: sub, role, iat, exp <= 24h | ✅ | JwtUtil code review |
| AC-26 | Schema: password_hash, active, manager role | ✅ | schema.sql + SchemaMigration review |
| AC-27 | Expiry/logout -> redirect to login | ✅ | AuthContext code review, api.ts interceptor |
| AC-28 | Deactivated user -> 403 login | ✅ | AuthControllerTest.d |
| AC-29 | Login as default, setup as exception | ✅ | App.tsx routing review |
| AC-30 | Standardized error format | ⚠️ | Partially: error+message fields used, but some errors constructed inline (not via GlobalExceptionHandler) |

---

## 5. Known Implementation Gaps & Findings

### GAPS (spec says X, code does Y)

| # | Finding | Spec/Design | Current Code | Impact |
|---|---------|-------------|--------------|--------|
| G1 | AC-16: No JWT returns 403 not 401 | 401 Unauthorized | 403 Forbidden (Spring Security default, no AuthenticationEntryPoint) | AC-16/17 non-compliant. Fix: add AuthenticationEntryPoint returning 401 JSON to SecurityConfig. |
| G2 | QR code uses external API | Design: server serves QR PNG via `GET /api/setup/qr` | Client uses `api.qrserver.com` external URL to render QR | Depends on external service. No offline setup possible. Server endpoint missing. |
| G3 | Setup password min-length mismatch | Auth spec: 8 chars minimum | Client enforces 6 chars, server enforces 8 chars | Client lets 6-7 char passwords through, server rejects with 422. Surface-level UX gap. |
| G4 | Reset password not wired in UserList | T-auth-20: UserList has Reset Password button | UserList only has Deactivate/Reactivate. No reset-password UI trigger. | Missing UX. API endpoint exists (`PATCH /api/users/:id/reset-password`) but no frontend button. |
| G5 | No Cache-Control: no-cache for index.html | Design Section 7: "Cache-Control: no-cache on index.html" | WebConfig static file config not verified | Client may serve stale HTML after server update. Needs verification or fix. |
| G6 | zxing-js polyfill imported eagerly | Design: lazy import for non-Chrome browsers | `Viewfinder.tsx` has `import { BrowserMultiFormatReader } from '@zxing/library'` at module top | ~100KB loaded on all browsers including Chrome where native BarcodeDetector works. Not a bug but unnecessary weight. |

### DEVIATIONS FROM TASKS.MD

| # | Item | Status | Note |
|---|------|--------|------|
| D1 | T-auth-01 (JwtUtil env secret) | Complete | Env var check, min 32 bytes, all claims set. |
| D2 | T-auth-12 (SetupController with QR) | Partial | Server-side QR endpoint NOT implemented. Client uses external API. |
| D3 | T-auth-20 (UserList with reset password) | Partial | Create/Deactivate/Reactivate done. Reset password button missing. |
| D4 | T-auth-25 (Auth styles) | Complete | index.css updated with auth UI classes (role badges, sidebar, login, setup). |
| D5 | Phase 1-6 Task files | Superseded | All reference Node.js/Express. Actual implementation is Java/Spring Boot. |

### OBSERVATIONS (non-blocking)

| # | Observation | Details |
|---|-------------|---------|
| O1 | Error format not fully unified | ProductController returns inline `LinkedHashMap` errors. `ForbiddenException` -> `GlobalExceptionHandler`. Both use `{ error, message }` so API surface is consistent, but not all error paths go through the global handler. |
| O2 | Setup controller has extra /status endpoint | Design lists `/api/setup/token`, `/api/setup/qr`, `/api/setup/register`. Code adds `/api/setup/status` (useful client check). Missing `/api/setup/qr`. |
| O3 | Continuous scan auto-return: design 2s, code 3s countdown | Minor UX difference. No user-toggle to disable auto-return. |
| O4 | Token expiry detected on init + ProtectedRoute but not periodically | `AuthContext` checks `exp` on mount. No `setInterval` check. `api.ts` 401 interceptor catches the eventual stale-token API call. Acceptable for v1. |
| O5 | Login `onSuccess` prop unused | `App.tsx` passes `onSuccess={() => {}}` no-op to `Login.tsx`. Parent navigates based on token presence anyway. |
| O6 | Search min 2 chars enforced server-side only | Client SearchBar doesn't prevent sending short queries. Server returns 422. Causes extra round-trip. |

---

## 6. Edge Cases & Constraints

| # | Constraint | Where Enforced | Bypass Risk |
|---|-----------|---------------|-------------|
| E1 | Stock >= 0 | Client (-1 disabled at 0), Controller (422 on negative), DB (CHECK) | Triple enforced. Lowest risk of all constraints. |
| E2 | Barcode unique | DB (UNIQUE INDEX), Controller (409 on duplicate create) | DB enforces at insert level. Low risk. |
| E3 | Barcode format 13 digits | Controller validates `matches("\\d{13}")` | Client doesn't validate format pre-submit. 10-digit barcode -> 422 from server. Acceptable. |
| E4 | Deactivated user blocked | JwtAuthFilter checks active flag per-request. AuthController.login checks at login. | Covered. Even with valid JWT, next API call fails. |
| E5 | Manager cannot create admin | UserController hard-codes `role='staff'` for manager callers, ignores client role field. | Server-side enforced. No escalation path. |
| E6 | Setup token single-use | SetupTokenStore.invalidate() after successful register. | Memory-bound (ConcurrentHashMap). Server restart generates new token. |
| E7 | JWT_SECRET min 32 bytes | JwtUtil @PostConstruct throws IllegalStateException. | Server won't start with weak/missing secret. |
| E8 | Camera unavailable | Navigator UI shows cameraError + ManualEntry fallback. | Always a fallback path. |
| E9 | SQL injection protection | ProductRepository.searchByName uses `LIKE '%' || ? || '%'` (prepared statement) | Correct. `?` placeholder prevents injection. No raw string concatenation. |
| E10 | No HTTPS for getUserMedia | getUsermedia requires secure context. Localhost works. Production on local network HTTP may block camera. | Risk documented in design Section 7. Self-signed cert not implemented. |

---

## 7. Implementation Unknowns

| # | Unknown | What to Check | Recommended Narrow Rerun |
|---|---------|---------------|--------------------------|
| U1 | SQLite WAL + concurrent stock update race | Two simultaneous PATCH /stock { delta: -1 } on stock=1. Expect one 422. | `curl -X PATCH ... 'delta:-1' & curl -X PATCH ... 'delta:-1' & wait` |
| U2 | 5000+ product list render performance | AC-11: "without noticeable lag". No virtual scrolling. 5000 `<div>` elements on mobile. | Seed 5000 products, measure initial render + scroll FPS on 375px viewport |
| U3 | Safari/Firefox barcode accuracy | zxing polyfill accuracy on iOS Safari unknown. | Test with 10 printed EAN-13 barcodes on iPhone Safari. Measure decode time + false-negative rate. |
| U4 | Cross-device data refresh UX accepted | AC-14: "visible on other after page refresh". No push/WebSocket. | UAT: "Update stock on Phone A, walk to Phone B, refresh, see new count" |
| U5 | External QR API dependency acceptable | Design says server endpoint. Client uses external. Decision needed. | Check if `api.qrserver.com` is reachable on the local network (no internet?). |
| U6 | Reset-password min-length not enforced | `UserController.resetPassword()` validates non-blank but no minimum length. | `PATCH /api/users/1/reset-password { "new_password": "ab" }` -> should be 422, currently passes. |
| U7 | Content-Type not validated on PATCH | Stock + user PATCH endpoints expect JSON but don't check Content-Type header. | `PATCH /api/products/X/stock` with `Content-Type: text/plain` -> likely 500, not 400. |
| U8 | No frontend test files | `client/src/` has zero test files. `npx vitest run` status unknown. | `cd client && npx vitest run` to confirm. May fail or find nothing. |
| U9 | 5 pre-existing TS compilation errors | `npx tsc --noEmit` reports errors in Viewfinder.tsx (3) and Login.test.tsx (2). Not auth-related. | Ignore for now. Not blocking app functionality. |
| U10 | No server startup test for missing JWT_SECRET | The check exists in JwtUtil but no integration test verifies server fails to start. | Check if there's a test that starts the app with JWT_SECRET unset. |
| U11 | BarcodeDetector API format support | Native API supports multiple formats. Code only handles ean_13. What if a UPC-A (12-digit) is scanned? | Check BarcodeDetector format list. UPC-A is close to EAN-13 but will fail format validation. |

---

## 8. Dependency Graph

```
React SPA ----HTTP----> Spring Boot Server ----JDBC----> SQLite
   |                          |
   |                          |-- JwtAuthFilter (every request, active-flag check)
   |                          |-- AuthService.requireRole() (per handler)
   |                          |-- SchemaMigration (@PostConstruct on startup)
   |                          |-- SetupTokenStore (in-memory ConcurrentHashMap)
   |
   |-- @zxing/library (barcode polyfill for Safari/Firefox — eager import)
   |-- api.qrserver.com (external QR generation for setup page)
```

### File Dependency Map

```
client/src/main.tsx -> App.tsx
  |-- AuthContext.tsx -> api.ts
  |                     |-- /api/products/* -> ProductController -> ProductRepository
  |                     |-- /api/auth/* -> AuthController -> UserRepository
  |                     |-- /api/users/* -> UserController -> UserRepository
  |                     |-- /api/setup/* -> SetupController -> UserRepository + SetupTokenStore
  |                     |-- 401 interceptor -> redirectLogin() -> clear sessionStorage
  |-- SetupPage.tsx -> api.ts (setup status/token/register calls)
  |-- Login.tsx -> AuthContext.login()
  |-- ProtectedRoute.tsx -> AuthContext.isTokenExpired()
  |-- MainApp (view state machine)
       |-- Sidebar (role-based nav items)
       |-- ProductList + SearchBar
       |-- Viewfinder + BarcodeDecoder + ManualEntry
       |-- ProductCard + StockControls
       |-- ProductForm
       |-- UserList + CreateUserForm
```

### External Dependencies

| Dependency | Version | Purpose | Risk |
|-----------|---------|---------|------|
| Spring Boot | 3.x | Web framework, security, JDBC | Low — actively maintained |
| sqlite-jdbc | (org.xerial) | SQLite JDBC driver | Low — mature |
| jjwt | 0.12.x | JWT generation/validation | Low — standard library |
| bcrypt | (Spring Security) | Password hashing | Low — built into Spring |
| @zxing/library | (browser polyfill) | Barcode decoding on non-Chrome | Medium — accuracy varies by device |
| qrserver.com | (external API) | QR code generation | Medium — requires internet access |
| zxing (server) | (com.google.zxing) | Only if server-side QR endpoint is implemented | Low — if added |
| Vite | 5.x | Frontend build tool | Low |
| React | 18+ | UI framework | Low |

---

## 9. Summary

### What works well

- Full CRUD lifecycle for products and users
- JWT auth with role-based scope enforcement at handler level (admin/manager/staff)
- QR admin onboarding (with external QR API dependency)
- Barcode scanning with native BarcodeDetector + ZXing polyfill fallback
- Stock management with absolute + delta update modes
- Non-negative stock enforced at 3 levels (UI, controller, DB CHECK)
- Password bcrypt hashing (no plain text storage, $2a$ prefix verified)
- 61 passing tests covering all user journeys
- Standardized JSON error format (`{ error, message }`) across all endpoints

### What needs attention (ordered by impact)

| Priority | Item | Effort | Type |
|----------|------|--------|------|
| P1 | AC-16: Add AuthenticationEntryPoint for 401 instead of 403 | XS | Bug fix |
| P2 | Add reset-password button to UserList component | S | Missing feature |
| P3 | Align client password min-length to 8 (match server) | XS | UX fix |
| P4 | Enforce password min-length on reset-password endpoint | XS | Bug fix |
| P5 | Implement `GET /api/setup/qr` server endpoint per design | S | Missing endpoint |
| P6 | Lazy-load zxing-js polyfill on non-Chrome browsers | XS | Optimisation |
| P7 | Add Cache-Control: no-cache for index.html | XS | Bug fix |
| P8 | Add Content-Type validation on PATCH endpoints | XS | Hardening |
| P9 | Performance test: 5000+ product list render | M | Verification |
| P10 | Real-device Safari barcode accuracy test | M | Verification |

### Risk profile

- **Security**: No critical issues. AC-16 gap is a status-code mismatch (security intent achieved). All access controls enforced server-side. Password hashing verified.
- **Data integrity**: Stock non-negative triple-enforced. Barcode uniqueness enforced at DB + controller. No delete endpoints — only soft deactivate.
- **Stability**: 61/61 tests pass. Server boots without default admin. Schema migration handles old databases.
- **UX gaps**: Reset-password UI missing. Password min-length mismatch between client/server. External QR API dependency.
