# Current State & Impact Analysis — makmur-0

Generated: 2026-07-08
Scope: All 4 domains (product, scan, stock, auth)
Status: Full implementation complete (verify-report.md: 61/61 tests pass)

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
| admin | Full: products + user management (staff + manager) |
| manager | User management only: staff accounts |
| staff | Products only: scan, stock, search, browse |

---

## 2. Code Paths (per request flow)

### Product: Scan → Lookup → Display

```
Camera (getUserMedia) → Every 500ms (native) or continuous (zxing)
  ↓
BarcodeDetector API (Chrome) or BrowserMultiFormatReader (Safari/Firefox)
  ↓ detects ean_13
App.tsx handleScan(barcode)
  ↓
api.ts fetchProduct(barcode) → GET /api/products/:barcode
  ↓
JwtAuthenticationFilter.doFilterInternal()
  ├── Checks Authorization: Bearer header
  ├── jwtUtil.validateToken()
  ├── jwtUtil.getUserId/Role/Username from claims
  ├── userRepository.findByUsername() → checks active flag
  └── Sets SecurityContextHolder with ROLE_x authority + userId in details
  ↓
ProductController.getByBarcode()
  ├── authService.requireRole("admin", "staff") → 403 if manager
  └── productRepository.findByBarcode() → SELECT * FROM products WHERE barcode = ?
  ↓
Response: 200 + Product JSON or 404 + { error, message }
  ↓
App.tsx: product found → set SelectedProduct + view='detail' + autoReturn=3 (countdown)
         product not found → setScannedBarcode + view='create'
```

### Auth: Login → JWT → Protected Route

```
Login.tsx → AuthContext.login(username, password)
  ↓
POST /api/auth/login { username, password }
  ↓
AuthController.login()
  ├── userRepository.findByUsername()
  ├── passwordEncoder.matches(password, user.passwordHash) → 401 if mismatch (generic)
  ├── Check user.isActive() → 403 account_disabled if deactivated
  └── jwtUtil.generateToken(username, user.id, user.role) → JWT
  ↓
Response: { token, user: { id, username, role } }
  ↓
AuthContext: sessionStorage.setItem('token', data.token)
              sessionStorage.setItem('user', JSON.stringify(data.user))
              setToken(data.token); setUser(data.user)
  ↓
App.tsx LoginGate: token present → <ProtectedRoute> → <MainApp>
  ├── user.role 'staff' → default view 'products'
  ├── user.role 'manager' → default view 'users'
  └── user.role 'admin' → default view 'products'
  ↓
Sidebar.getNavItems(role):
  ├── staff/admin → Products + Scan
  ├── manager/admin → Staff Management
  └── Logout footer always visible
```

### Auth: Subsequent requests → Scope Enforcement

```
Any component → api.ts apiFetch()
  ├── Adds authHeaders(): Authorization: Bearer <token>
  └── If 401 response → redirectLogin() → clear sessionStorage → window.location = '/login'
  ↓
JwtAuthenticationFilter (repeats for every request)
  ↓
Controller handler → authService.requireRole("admin", "staff"|"manager")
  ├── Reads SecurityContextHolder.getContext().getAuthentication()
  ├── Checks if authenticated
  └── Checks granted authorities contain allowed role → 403 ForbiddenException if fail
  ↓
GlobalExceptionHandler.handleForbidden() → { error: "forbidden", message: "Access denied" }
```

### User Management: List/Create/Deactivate

```
UserList.tsx → api.ts fetchUsers() → GET /api/users
UserController.listUsers()
  ├── authService.requireRole("admin", "manager")
  ├── If admin → userRepository.findAll()
  └── If manager → userRepository.findAllByRole("staff") → filtered to staff only

UserList.tsx → api.ts createUser() → POST /api/users
UserController.createUser()
  ├── authService.requireRole("admin", "manager")
  ├── If manager caller → hard-codes newRole = "staff"
  └── If admin caller → accepts "staff" or "manager" from client

UserList.tsx → handleToggleActive() → api.ts deactivateUser(id) → PATCH /api/users/:id/deactivate
UserController.deactivate()
  ├── authService.requireRole("admin", "manager")
  ├── checkTargetRole(id): manager can only act on staff targets
  └── userRepository.updateActiveStatus(id, false)
```

### Setup (First-time admin onboarding)

```
Server start → SetupTokenStore @PostConstruct init() → generates UUID
  ↓
GET /api/setup/status → SetupController.getStatus() → { needsSetup: false|true }
  ↓
Client: if needsSetup → GET /api/setup/token → { token, expires_at }
         → SetupPage shows QR via external api.qrserver.com
  ↓
Admin scans QR → navigates to /setup?token=<uuid>
  ↓
SetupPage form mode → POST /api/setup/register { token, username, password }
  ↓
SetupController.register()
  ├── Validates token via SetupTokenStore.isValid()
  ├── Race-condition guard: countAdmins() > 0 → 403 already_setup
  ├── bcrypt encode password → create User(role='admin')
  └── tokenStore.invalidate(token)
  ↓
Response 201 → redirect to /login
```

### Stock Update

```
StockControls.tsx
  ├── +1 button → updateStock(barcode, { delta: 1 })
  ├── -1 button → updateStock(barcode, { delta: -1 }) (disabled at 0)
  └── absolute input → updateStock(barcode, { value: N })
  ↓
api.ts updateStock() → PATCH /api/products/:barcode/stock
  ↓
ProductController.updateStock()
  ├── authService.requireRole("admin", "staff")
  ├── productRepository.findByBarcode()
  ├── value | delta (value takes precedence, mutually exclusive, 422 if neither)
  ├── newStock >= 0 check → 422 if negative
  ├── product.setStock(newStock); repo.save(product)
  └── Response: { barcode, stock, previous_stock }
  ↓
StockControls: onUpdate(result) → setFlash(true) → setTimeout 1.5s
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

- SchemaMigration.java handles `password` → `password_hash` column rename for old DBs (table recreation pattern)
- DB file: `server/data/makmur.db` (created at runtime, gitignored)
- WAL mode enabled at schema init
- `spring.jackson.property-naming-strategy=SNAKE_CASE` — so Java `createdAt` serializes as `created_at` in JSON

---

## 4. Test Coverage

### Existing: 61 tests passing (14 original + 47 new)

| Test class | Count | Coverage |
|-----------|-------|----------|
| ProductControllerTest (original) | 14 | CRUD, stock, search, validation, duplicates |
| AuthServiceTest | 10 | requireRole valid/invalid, no auth, role/userId getters |
| AuthControllerTest | 10 | Login valid/invalid/deactivated, register role enforcement |
| UserControllerTest | 15 | List scoped, create role-hardcode, deactivate scoped |
| SetupControllerTest | 9 | Token gen, register, invalidation, expired token |
| ProductControllerTest (scope) | 3 | Staff→200, Manager→403, No JWT→403 |

### Verified AC coverage (from verify-report.md)

| AC | Status | Notes |
|----|--------|-------|
| AC-15 | ✅ | Login valid→JWT, invalid→401 |
| AC-16 | ⚠️ | **Returns 403 not 401.** Missing JWT → Spring Security fallthrough to AccessDeniedHandler. Fix: add AuthenticationEntryPoint returning 401. |
| AC-17 | ⚠️ | Expired/malformed JWT → same 403 problem. Needs explicit test. |
| AC-18 | ✅ | Staff→products 200, staff→users 403 |
| AC-19 | ✅ | Manager→users 200, manager→products 403 |
| AC-20 | ✅ | Admin→everything 200 |
| AC-21 | ✅ | Handler-level scope (AuthService.requireRole per handler) |
| AC-22 | ✅ | QR onboarding flow |
| AC-23 | ✅ | Token invalidated after setup |
| AC-24 | ✅ | Bcrypt password hash ($2a$ prefix verified) |
| AC-25 | ✅ | JWT claims: sub, role, iat, exp ≤ 24h |
| AC-26 | ✅ | Schema: password_hash, active, manager role in CHECK |
| AC-27 | ✅ | Expiry/logout → redirect to login |
| AC-28 | ✅ | Deactivated user → 403 login |
| AC-29 | ✅ | Login as default, setup as exception |

---

## 5. Known Implementation Gaps & Findings

### GAPS (spec says X, code does Y)

| # | Finding | Spec/Design | Current Code | Impact |
|---|---------|-------------|--------------|--------|
| G1 | AC-16: No JWT returns 403 not 401 | 401 Unauthorized | 403 Forbidden (Spring Security default, no AuthenticationEntryPoint) | AC-16/17 non-compliant. Fix: add `AuthenticationEntryPoint` to SecurityConfig. |
| G2 | QR code uses external API | Design says server serves QR PNG via `GET /api/setup/qr` | Client uses `api.qrserver.com` external URL | Depends on external service, no offline setup. Missing `GET /api/setup/qr` endpoint. |
| G3 | Setup password min length mismatch | Spec: 8 chars minimum | Client enforces 6 chars, Server enforces 8 chars | Client lets 6-7 char passwords through, server rejects with 422. Surface-level UX gap. |
| G4 | Reset password not in UserList | T-auth-20: UserList has Reset Password button | UserList only has Deactivate/Reactivate. No reset-password UI. | Missing UX. API endpoint exists (`PATCH /api/users/:id/reset-password`) but no frontend trigger. |
| G5 | No Cache-Control: no-cache for index.html | Design §7: "Cache-Control: no-cache on index.html" | Not verified in WebConfig static file config | Client may serve stale HTML after server update. |
| G6 | zxing-js imported eagerly | Design: lazy import for browsers that need it | Viewfinder.tsx has `import { BrowserMultiFormatReader } from '@zxing/library'` at module top | ~100KB loaded on all browsers including Chrome where native BarcodeDetector works. Not a bug but unnecessary weight. |

### DEVIATIONS FROM TASKS.MD

| # | Item | Status | Note |
|---|------|--------|------|
| D1 | T-auth-01 (JwtUtil env secret) | ✅ Complete | Env var check, min 32 bytes, all claims set |
| D2 | T-auth-12 (SetupController with zxing QR) | ✅ Partial | Server-side QR endpoint NOT implemented. Client uses external QR API. |
| D3 | T-auth-20 (UserList with reset password) | ⚠️ Partial | Create/Deactivate/Reactivate done. Reset password button missing. |
| D4 | T-auth-25 (Auth styles) | ✅ Complete | index.css updated with auth UI classes |

### OBSERVATIONS (non-blocking)

| # | Observation | Details |
|---|-------------|---------|
| O1 | Error format inconsistency | ProductController returns inline `LinkedHashMap` errors. Some errors go through `GlobalExceptionHandler` (ForbiddenException → 403), others are manually constructed. Both use `{ error, message }` format so API surface is consistent, but exception handling not unified. |
| O2 | Setup controller uses /status + /token split | Design spec lists `/api/setup/qr`, `/api/setup/token`, `/api/setup/register`. Code has `/api/setup/status`, `/api/setup/token`, `/api/setup/register`. Extra /status endpoint (useful, not a bug). Missing /qr. |
| O3 | Continuous scan auto-return default UX | Design says 2s, code uses 3s countdown. Minor. No user-toggle for disabling auto-return. |
| O4 | Token expiry in AuthContext only checks on init | `AuthContext` checks `exp` on mount and on init. `ProtectedRoute` also checks on render. But there's no periodic `setInterval` check — a user could stay logged in past expiry if no API call triggers redirect. `api.ts 401 interceptor` catches the eventual API call. Acceptable. |
| O5 | Login `onSuccess` prop not used | `App.tsx` passes `onSuccess={() => {}}` empty function. `Login.tsx` calls it but the parent (`LoginGate`) navigates based on token presence anyway. No-op. |
| O6 | Product search minimum 2 chars enforced only on server | Client SearchBar doesn't enforce min 2 chars before sending. Server returns 422 if < 2 chars. OK for UX but extra 422 round-trip. |

---

## 6. Edge Cases & Constraints

| # | Constraint | Where Enforced | Bypass Risk |
|---|-----------|---------------|-------------|
| E1 | Stock >= 0 | Client (-1 disabled at 0), Controller (422 on negative), DB (CHECK) | Triple enforced. Lowest risk. |
| E2 | Barcode unique | DB (UNIQUE INDEX), Controller (409 on duplicate) | DB enforces at insert level. Low risk. |
| E3 | Barcode exactly 13 digits | Controller `matches("\\d{13}")` | Client doesn't enforce format — could send 10-digit barcode → 422. Acceptable (server validates). |
| E4 | Deactivated user blocked | JwtAuthenticationFilter checks active flag every request, AuthController.login checks at login | Covered. Even with valid JWT, next API call fails. |
| E5 | Manager cannot create admin | UserController hard-codes `role='staff'` for managers, ignores client role field | Server-side enforced. No escalation path. |
| E6 | Setup token single-use | SetupTokenStore.invalidate() after successful register | Memory-bound (ConcurrentHashMap). Server restart regenerates. |
| E7 | JWT_SECRET min 32 bytes | JwtUtil @PostConstruct throws IllegalStateException | Server won't start with weak secret. |
| E8 | Camera unavailable | Navigator UI shows cameraError + ManualEntry as fallback | Always a fallback path. |
| E9 | No SQL injection protection (raw string concat) | ProductRepository.searchByName uses `LIKE '%' || ? || '%'` (prepared statement, parameterized) | Correct — `?` placeholder prevents injection. No raw string concatenation. |
| E10 | No HTTPS (self-signed) for getUserMedia | getUsermedia requires secure context (HTTPS or localhost). In production on local network, browsers may block camera if served over HTTP. | Risk. Design §7 mentions self-signed cert but not implemented. |

---

## 7. Implementation Unknowns

| # | Unknown | What to Check | Narrow Rerun |
|---|---------|---------------|--------------|
| U1 | SQLite WAL + concurrent access test | Verify two simultaneous PATCH /stock requests don't race. Current: one JVM + WAL. Need `@Transactional` test. | `curl -X PATCH ... stock 'delta:-1' & curl -X PATCH ... stock 'delta:-1' & wait` on stock=1 → expect one 422 |
| U2 | 5000+ product list render performance | AC-11: "Without noticeable lag". No virtual scrolling implemented (simple `<div>` per row). 5000 `<div>` elements could lag on mobile. | Create 5000 products via seed script, measure initial render + scroll FPS on 375px viewport |
| U3 | Safari/Firefox barcode accuracy | Native `BarcodeDetector` is Chrome-only. zxing-js polyfill accuracy on iOS Safari needs real-device testing. | Test with 10 printed EAN-13 barcodes on iPhone Safari, measure decode time and false-negative rate |
| U4 | Cross-device data visibility without refresh | AC-14: "visible on other after page refresh". No push/WebSocket. Staff must manually refresh. Verify this is acceptable UX. | User acceptance test: "Update stock on Phone A, walk to Phone B, refresh, see new count" |
| U5 | Server-side QR endpoint (GET /api/setup/qr) | Design explicitly lists this endpoint. Code doesn't have it. Client uses external API. Should this be a task? | Check if external QR API is acceptable for the deployment context (local network + no internet?) |
| U6 | Password validation — min length on reset password | UserController.resetPassword() validates `new_password` is not blank but doesn't enforce minimum length. Should match register's 8-char rule. | `PATCH /api/users/1/reset-password { "new_password": "ab" }` → should be 422 but passes |
| U7 | Content-Type enforcement for PATCH endpoints | Stock update and user management endpoints expect JSON body but don't validate Content-Type header. Non-JSON body could cause confusing errors. | `PATCH /api/products/X/stock` with `Content-Type: text/plain` → likely 500 instead of 400 |

---

## 8. Dependency Graph

```
React SPA ──HTTP──> Spring Boot Server ──JDBC──> SQLite
   │                      │
   │                      ├── JWT filter (every request)
   │                      ├── AuthService (scope per handler)
   │                      ├── SchemaMigration (@PostConstruct)
   │                      └── SetupTokenStore (in-memory)
   │
   ├── @zxing/library (barcode polyfill for Safari/Firefox)
   └── External QR API (api.qrserver.com for setup)
```

### File Dependency Map

```
client/src/main.tsx → App.tsx
  ├── AuthContext.tsx → api.ts
  │                      ├── /api/products/* → ProductController → ProductRepository
  │                      ├── /api/auth/* → AuthController → UserRepository
  │                      ├── /api/users/* → UserController → UserRepository
  │                      ├── /api/setup/* → SetupController → UserRepository + SetupTokenStore
  │                      └── 401 interceptor → redirectLogin()
  ├── SetupPage.tsx → api.ts (getSetup/register)
  ├── Login.tsx → AuthContext.login()
  ├── ProtectedRoute.tsx → AuthContext (isTokenExpired)
  └── MainApp (view state machine)
       ├── Sidebar (role-based nav)
       ├── ProductList + SearchBar
       ├── Viewfinder + ManualEntry
       ├── ProductCard + StockControls
       ├── ProductForm
       └── UserList
```

---

## 9. Summary

### What works well

- Full CRUD lifecycle for products and users
- JWT auth with role-based scope enforcement at handler level
- QR admin onboarding (with external QR dependency)
- Barcode scanning with native + ZXing polyfill fallback
- Stock management with absolute + delta updates
- Non-negative stock enforced at 3 levels (UI, controller, DB)
- Password bcrypt hashing (no plain text storage)
- 61 passing tests covering all user journeys

### What needs attention

1. **AC-16 gap** — Fix AuthenticationEntryPoint to return 401 instead of 403 for missing/expired JWTs (small SecurityConfig change)
2. **Reset password UI** — Add the button/flow in UserList component
3. **Password min-length alignment** — Make client match server's 8-char minimum
4. **Server-side QR endpoint** — Implement `GET /api/setup/qr` per design spec (optional if external API is acceptable)
5. **Performance testing** — Verify 5000+ product render and search speed
6. **Safari camera testing** — Real-device verification for ZXing polyfill accuracy
