# Current-State-Context — Makmur v1 (makmur-0)

Generated: 2026-07-09 (previous: 2026-07-08 — stale, now updated)

---

## Repository Surface

**Path**: `/drive2/Proyekan/makmur`
**Git**: `main` branch (af2db0a)
**Status**: Auth phase complete — 61/61 tests passing. 14/15 AC covered (AC-16 gap documented).

```
makmur/
├── client/                    # React + TypeScript + Vite
│   ├── src/
│   │   ├── api.ts             # REST client + 401 interceptor
│   │   ├── App.tsx            # View state routing, role-based redirect
│   │   ├── types.ts           # Product, User, API types
│   │   ├── index.css          # App styles (incl. auth UI, role badges)
│   │   ├── main.tsx           # React entry point
│   │   ├── context/
│   │   │   └── AuthContext.tsx # Auth state, login/logout, JWT expiry
│   │   └── components/
│   │       ├── BarcodeDecoder.tsx  # Barcode detection (native + zxing polyfill)
│   │       ├── CreatePrompt.tsx    # Scan-not-found creation prompt
│   │       ├── CreateUserForm.tsx  # Create user modal/form (manager/admin)
│   │       ├── Dashboard.tsx       # Home dashboard view
│   │       ├── Login.tsx           # Login form with error handling
│   │       ├── ManualEntry.tsx     # Manual barcode text input
│   │       ├── ProductCard.tsx     # Product detail display
│   │       ├── ProductForm.tsx     # Product creation form
│   │       ├── ProductList.tsx     # Scrollable product catalogue
│   │       ├── ProtectedRoute.tsx  # Route guard — redirects to login if unauthenticated
│   │       ├── ScanResult.tsx      # Scan result routing (found / not found)
│   │       ├── SearchBar.tsx       # Debounced search input
│   │       ├── SetupPage.tsx       # QR display + setup form for onboarding
│   │       ├── Sidebar.tsx         # Role-based navigation sidebar
│   │       ├── StockControls.tsx   # Stock update UI (+1/-1, absolute)
│   │       ├── UserList.tsx        # User management table (manager/admin)
│   │       └── Viewfinder.tsx      # Camera viewfinder
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.js
├── server/                    # Java 17 + Spring Boot 3.x + Maven
│   ├── src/main/java/com/makmur/
│   │   ├── Application.java         # Spring Boot entry point (port 3001)
│   │   ├── config/
│   │   │   ├── DataSourceConfig.java    # Creates data/ directory
│   │   │   ├── JwtAuthenticationFilter.java # Per-request JWT auth filter
│   │   │   ├── JwtUtil.java             # JWT generation/validation (JWT_SECRET from env)
│   │   │   ├── SchemaMigration.java     # Schema migration (password→password_hash)
│   │   │   ├── SecurityConfig.java      # Route security config (permitAll vs authenticated)
│   │   │   ├── SetupTokenStore.java     # In-memory one-time setup token store
│   │   │   └── WebConfig.java           # CORS + static file serving
│   │   ├── controller/
│   │   │   ├── AuthController.java      # POST /api/auth/login (active check, bcrypt)
│   │   │   ├── HealthController.java    # GET /api/health
│   │   │   ├── ProductController.java   # Product CRUD + stock (scope: staff/admin)
│   │   │   ├── SetupController.java     # QR onboarding (/api/setup/*)
│   │   │   └── UserController.java      # User management (/api/users/*)
│   │   ├── entity/
│   │   │   ├── Product.java             # id, barcode, name, price, stock, created_at
│   │   │   └── User.java                # username, passwordHash, role, active
│   │   ├── exception/
│   │   │   ├── ForbiddenException.java  # 403 exception
│   │   │   └── GlobalExceptionHandler.java  # Global error handler (standardized JSON)
│   │   ├── repository/
│   │   │   ├── ProductRepository.java   # JDBC-based product data access
│   │   │   └── UserRepository.java      # JDBC-based user data access
│   │   └── service/
│   │       └── AuthService.java         # Scope enforcement (requireRole)
│   ├── src/main/resources/
│   │   ├── application.properties       # SQLite config, port, Jackson naming
│   │   └── schema.sql                   # DB schema (products + users tables)
│   ├── src/test/java/com/makmur/
│   │   ├── controller/
│   │   │   ├── AuthControllerTest.java  # 10 tests: login, register, role enforcement
│   │   │   ├── ProductControllerTest.java # 17 tests: CRUD + stock + scope
│   │   │   ├── SetupControllerTest.java # 9 tests: token, register, invalidation
│   │   │   └── UserControllerTest.java  # 15 tests: list, create, deactivate, scope
│   │   └── service/
│   │       └── AuthServiceTest.java     # 10 tests: requireRole valid/invalid/no-auth
│   ├── server/data/                     # SQLite DB created at runtime (gitignored)
│   └── pom.xml
├── AGENTS.md                    # AI assistant guidance — architecture, conventions, pitfalls
├── CHANGELOG.md                 # Release history — Keep a Changelog format
├── LICENSE                      # MIT license
├── README.md                    # Human onboarding — tech stack, install, usage
└── openspec/changes/makmur-0/   # SDD artifacts (see Docs Coverage below)
```

---

## Documentation Coverage

| Doc | Path | Status |
|-----|------|--------|
| PRD | `openspec/changes/makmur-0/prd.md` | Current — v1 scope + auth section (Section 9) |
| Proposal | `openspec/changes/makmur-0/proposal.md` | Current — feature summary, affected domains |
| Design | `openspec/changes/makmur-0/design.md` | Current — architecture, API contracts, auth design |
| Tasks | `openspec/changes/makmur-0/tasks.md` | **Partially stale** — Phase 1-6 reference Node.js/Express (superseded by Java). Auth tasks (T-auth-*) correct. |
| Auth spec | `openspec/changes/makmur-0/specs/auth/spec.md` | Current — 14 requirements, 24 scenarios |
| Product spec | `openspec/changes/makmur-0/specs/product/spec.md` | Current — 5 requirements, 6 scenarios |
| Scan spec | `openspec/changes/makmur-0/specs/scan/spec.md` | Current — 8 requirements, 6 scenarios |
| Stock spec | `openspec/changes/makmur-0/specs/stock/spec.md` | Current — 6 requirements, 6 scenarios |
| Testing | `openspec/changes/makmur-0/testing.md` | Current — unit, integration, manual test procedures |
| Verify report | `openspec/changes/makmur-0/verify-report.md` | Current — 61/61 results, AC coverage, findings |
| Impact analysis | `openspec/changes/makmur-0/current-state-impact.md` | Current — code paths, gaps, constraints, unknowns |
| Business logic | `openspec/changes/makmur-0/business-logic.md` | Current — design rationale, historical note for pre-auth |
| User stories | `openspec/changes/makmur-0/user-stories.md` | Current — 50 stories across domains |
| Documentation review | `openspec/changes/makmur-0/documentation-review.md` | Current — updated 2026-07-09 |
| README.md | project root | **Updated** — auth flow, JWT, roles, QR onboarding |
| AGENTS.md | project root | **Updated** — comprehensive architecture, conventions, pitfalls |
| CHANGELOG.md | project root | **Present** — [Unreleased] section with auth changes |
| LICENSE | project root | **Present** — MIT |

### Known stale docs

1. **tasks.md Phase 1-6** — References `server/package.json`, `server/index.js`, `server/db.js`, etc. (Node.js/Express). Actual server is Java/Spring Boot. Auth phase tasks (T-auth-*) correctly reference Java files. Phase 1-6 should be marked `SUPERSEDED`.
2. **current-state-context.md (this file)** — Now updated with current auth-complete state. Tests were "NOT STARTED" → now 61/61 passing.

---

## Module Boundaries

### Server (Java/Spring Boot) — 18 files

| Component | Files | Responsibility |
|-----------|-------|---------------|
| Entry | `Application.java` | Spring Boot entry, schema migration runner, port 3001 |
| Config | `JwtUtil.java`, `JwtAuthFilter.java`, `SecurityConfig.java`, `WebConfig.java`, `DataSourceConfig.java` | JWT lifecycle, auth filter, route security, CORS, data dir |
| Schema | `SchemaMigration.java`, `schema.sql` | Table creation, migration (password→password_hash) |
| Onboarding | `SetupController.java`, `SetupTokenStore.java` | QR token generation, admin registration |
| Auth | `AuthController.java`, `AuthService.java` | Login (bcrypt verify + JWT issue), scope enforcement |
| Products | `ProductController.java`, `ProductRepository.java`, `Product.java` | CRUD + stock + search (scope: staff/admin) |
| Users | `UserController.java`, `UserRepository.java`, `User.java` | CRUD + deactivate + reset-password (scope: manager/admin) |
| Errors | `GlobalExceptionHandler.java`, `ForbiddenException.java` | Standardized JSON error responses |

### Client (React/TypeScript) — 20 files

| Component | Files | Responsibility |
|-----------|-------|---------------|
| App shell | `App.tsx`, `main.tsx`, `types.ts`, `index.css` | View state routing (6 views), entry point, types, styles |
| Auth | `AuthContext.tsx`, `Login.tsx`, `SetupPage.tsx`, `ProtectedRoute.tsx` | JWT storage, login form, QR onboarding, route guard |
| Nav | `Sidebar.tsx` | Role-based nav items (staff→products/scan, manager→users, admin→both) |
| Products | `ProductList.tsx`, `ProductCard.tsx`, `ProductForm.tsx`, `SearchBar.tsx` | List, detail, creation, search |
| Stock | `StockControls.tsx` | +1/-1, absolute value input |
| Scan | `Viewfinder.tsx`, `BarcodeDecoder.tsx`, `ManualEntry.tsx`, `ScanResult.tsx`, `CreatePrompt.tsx` | Camera, decode, fallback, routing |
| Users | `UserList.tsx`, `CreateUserForm.tsx` | User table, create/deactivate/reactivate |
| API | `api.ts` | fetch wrapper, auth headers, 401 interception |

---

## Likely Impacted Areas

### Current — what's fully built and stable

- **Products** — CRUD, search, stock update, v1 UX. 17 integration tests. No changes expected.
- **Scan** — Camera viewfinder, native BarcodeDetector, zxing polyfill, manual entry, result routing. 3-5 tests via manual procedures. Stable.
- **Users** — User management CRUD, role enforcement, scope filtering. 15 tests. Stable.
- **Auth** — Login, JWT, bcrypt, QR onboarding, filter. 10 + 9 tests. Stable.
- **Auth service** — Scope enforcement utility. 10 tests. Stable.

### Remaining known gaps (next work items)

| Gap | Type | Effort | Files |
|-----|------|--------|-------|
| AC-16: No JWT → 403 not 401 | Bug fix | XS | `SecurityConfig.java` — add `AuthenticationEntryPoint` |
| Reset password UI: button missing in UserList | Missing feature | S | `UserList.tsx` — add button + prompt |
| Password min-length: client 6 vs server 8 | UX fix | XS | `SetupPage.tsx`, `CreateUserForm.tsx` — change 6→8 |
| Server-side QR endpoint `GET /api/setup/qr` | Missing endpoint | S | `SetupController.java`, `pom.xml` (zxing dep) — optional |
| Lazy-load zxing-js polyfill | Optimisation | XS | `BarcodeDecoder.tsx` — dynamic import |
| Cache-Control: no-cache on index.html | Bug fix | XS | `WebConfig.java` |
| U6: Password min-length on reset | Bug fix | XS | `UserController.java` — add length check |
| U7: Content-Type enforcement | Bug fix | XS | `GlobalExceptionHandler.java` or controller |

### Unknowns needing investigation

| # | Unknown | To Check |
|---|---------|----------|
| U1 | SQLite WAL + concurrent stock updates race | `@Transactional` test with two simultaneous PATCH requests on stock=1 |
| U2 | 5000+ product list render performance | Seed 5000 products, measure render + scroll FPS on 375px viewport |
| U3 | Safari/Firefox zxing barcode accuracy | Real-device test: 10 printed EAN-13 barcodes on iPhone Safari |
| U4 | Cross-device data refresh UX acceptable | UAT: "Update stock on Phone A, refresh Phone B, see new count" |
| U5 | External QR API dependency acceptable | Decide: implement server endpoint or accept external dependency |
| U10 | No frontend test files | `client/src/` has no test files. `npx vitest run` likely fails or runs nothing |
| U11 | `npx tsc --noEmit` reports 5 pre-existing TS errors | Viewfinder.tsx (3), Login.test.tsx (2). Not auth-related, not blocking |

---

## Schema State

```sql
-- products: UNCHANGED from v1
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

-- users: MIGRATED (password → password_hash, added active, extended role)
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL DEFAULT 'staff' CHECK(role IN ('admin', 'manager', 'staff')),
    active        INTEGER NOT NULL DEFAULT 1,
    created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
```

- DB file: `server/data/makmur.db` (gitignored, created at runtime)
- WAL mode enabled at schema init
- `SchemaMigration.java` handles old-schema upgrade at startup (table recreation pattern)

---

## Key Architecture Decisions

| Decision | Value | Rationale |
|----------|-------|-----------|
| Stack | Java/Spring Boot (not Node.js) | Type safety, JDBC maturity, fat-JAR deployment. Changed from initial Node.js plan. |
| DB | SQLite (not PostgreSQL) | Zero-config, single file, embedded. Sufficient for single-store scale. |
| Auth | JWT (not sessions) | Stateless, role in claims avoids DB query per request. 24h expiry, no refresh token in v1. |
| Scope | Handler-level (not route-level) | Same URL serves all roles. `AuthService.requireRole()` per handler. |
| Navigation | State-based (not React Router) | Only 6 views, no deep-linking needed in v1. Simple switch statement in App.tsx. |
| Routing | No role-based routes | One URL per resource. Role checks inside handler. |
| Deletion | Soft deactivate only (no hard delete) | Preserves referential integrity for future audit features. |
| Onboarding | QR flow (no default admin seed) | No pre-seeded credentials. Token is single-use with 60min TTL. |

---

## Environment Variables

| Var | Required | Min | Default | Notes |
|-----|----------|-----|---------|-------|
| `JWT_SECRET` | Yes | 32 bytes | none | Server fails to start if unset |
| `SPRING_PROFILES_ACTIVE` | No | — | — | Set to `dev` for hot-reload |

- Server binds to all network interfaces (`0.0.0.0:3001`)
- No default admin seed — QR onboarding only
- CORS allows all origins in dev mode, same-origin in production
