# Current-State-Context — Makmur v1 (makmur-0)

## Repository Surface

**Path**: `/drive2/Proyekan/makmur`  
**Git**: `main` branch  
**Status**: Implementation in progress — auth phase partially complete

```
makmur/
├── client/                    # React + TypeScript + Vite
│   ├── src/
│   │   ├── api.ts             # REST client + 401 interceptor
│   │   ├── App.tsx            # View state routing, role-based redirect
│   │   ├── types.ts           # Product, User, API types
│   │   ├── index.css
│   │   ├── main.tsx
│   │   ├── context/
│   │   │   └── AuthContext.tsx
│   │   └── components/
│   │       ├── Dashboard.tsx
│   │       ├── Login.tsx
│   │       ├── ManualEntry.tsx
│   │       ├── ProductCard.tsx
│   │       ├── ProductForm.tsx
│   │       ├── ProductList.tsx
│   │       ├── ProtectedRoute.tsx
│   │       ├── SearchBar.tsx
│   │       ├── SetupPage.tsx
│   │       ├── Sidebar.tsx
│   │       ├── StockControls.tsx
│   │       ├── UserList.tsx
│   │       └── Viewfinder.tsx
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.js
├── server/                    # Java 17 + Spring Boot 3.x
│   ├── src/main/java/com/makmur/
│   │   ├── Application.java
│   │   ├── config/
│   │   │   ├── DataSourceConfig.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtUtil.java          # Reads JWT_SECRET, embeds userId+role
│   │   │   ├── SchemaMigration.java
│   │   │   ├── SecurityConfig.java   # /api/setup/**, /api/auth/**, /api/health public
│   │   │   ├── SetupTokenStore.java
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java   # login + register (role-enforced)
│   │   │   ├── HealthController.java
│   │   │   ├── ProductController.java # Scope-checked: staff/admin OK, manager 403
│   │   │   ├── SetupController.java  # QR onboarding flow
│   │   │   └── UserController.java    # User mgmt scoped by role
│   │   ├── entity/
│   │   │   ├── Product.java
│   │   │   └── User.java              # passwordHash, active, role fields
│   │   ├── exception/
│   │   │   ├── ForbiddenException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/
│   │   │   ├── ProductRepository.java
│   │   │   └── UserRepository.java
│   │   └── service/
│   │       └── AuthService.java       # requireRole(), getCurrentRole()
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── schema.sql                 # products + users tables
│   ├── data/                          # SQLite DB created at runtime
│   └── pom.xml
└── openspec/changes/makmur-0/
    ├── prd.md                         # Full PRD with auth section
    ├── proposal.md
    ├── design.md
    ├── tasks.md                       # Phase 1-6 + Auth phase tasks
    ├── specs/
    │   ├── auth/spec.md              # Auth domain spec
    │   ├── product/spec.md
    │   ├── scan/spec.md
    │   └── stock/spec.md
    ├── current-state-context.md      # THIS FILE
    └── current-state-impact.md
```

---

## Documentation Coverage

| Doc | Status |
|-----|--------|
| `README.md` | Present — tech stack, quick start, project structure, API endpoints |
| `AGENTS.md` | Not present |
| `PRD.md` | Complete — v1 scope + auth section (Section 9) |
| `proposal.md` | Complete — feature summary, affected domains |
| `design.md` | Complete — architecture, module map, API contracts, auth design |
| `tasks.md` | Complete — Phase 1-6 + Auth phase (T-auth-01 to T-auth-25) |
| `specs/auth/spec.md` | Complete |
| `specs/product/spec.md` | Complete |
| `specs/scan/spec.md` | Complete |
| `specs/stock/spec.md` | Complete |

---

## Module Boundaries

### Server (Java/Spring Boot)

| Component | Responsibility |
|-----------|---------------|
| `JwtUtil` | JWT generation/validation, reads `JWT_SECRET` env var, embeds `userId` + `role` claims |
| `JwtAuthenticationFilter` | Extracts JWT, validates, checks `active` flag, sets SecurityContext |
| `SecurityConfig` | Permits `/api/auth/login`, `/api/health`, `/api/setup/**`; auth all others |
| `AuthController` | `POST /api/auth/login` (active check), `POST /api/auth/register` (role-enforced) |
| `ProductController` | CRUD + stock — handler-level scope check (`requireRole("admin", "staff")`) |
| `UserController` | `GET /api/users`, `POST`, `PATCH /deactivate`, `PATCH /reset-password` — scoped |
| `SetupController` | QR onboarding: `/api/setup/status`, `/token`, `/register` |
| `AuthService` | `requireRole()` throws 403, `getCurrentRole()` returns caller role |
| `ProductRepository` | JDBC repo — `findAllByOrderByNameAsc`, `findByBarcode`, `searchByName` |
| `UserRepository` | `findByUsername`, `findAll`, `findAllByRole`, `updateActiveStatus`, `updatePassword` |
| `User` entity | Fields: `id`, `username`, `passwordHash`, `role`, `active`, `createdAt` |
| `Product` entity | Fields: `id`, `barcode`, `name`, `price`, `stock`, `createdAt` |
| `schema.sql` | products table + users table (`password_hash`, `active`, role CHECK) |

### Client (React/TypeScript)

| Component | Responsibility |
|-----------|---------------|
| `AuthContext` | Stores token+user in sessionStorage, provides login/logout/user |
| `api.ts` | REST client with `authHeaders()`, 401 interceptor, user management calls |
| `App.tsx` | View routing (`dashboard|products|scan|detail|create|users`), role-based redirect |
| `Sidebar` | Role-based nav items (staff → products/scan; manager → users; admin → both) |
| `Login` | Form with error display, distinguishes `account_disabled` |
| `SetupPage` | QR mode + form mode |
| `UserList` | User table, create/deactivate/reactivate/reset-password actions |
| `ProductList`, `ProductCard`, `StockControls`, `Viewfinder`, etc. | Unchanged from v1 |

---

## Likely Impacted Areas

### Auth Implementation (In Progress)

- **Server auth foundation** — DONE (T-auth-01 to T-auth-06, T-auth-11)
- **Authorization layer** — DONE (T-auth-05, T-auth-07, T-auth-09, T-auth-10)
- **Onboarding flow** — DONE (T-auth-12, T-auth-13)
- **Frontend auth** — DONE (T-auth-14 to T-auth-21, T-auth-23 to T-auth-25)
- **Tests** — COMPLETED (T-auth-22)

### Remaining Work

| Task | Status | Impact |
|------|--------|--------|
| T-auth-22: Auth integration tests | COMPLETED | 61/61 tests passing |
| T-auth-24: CORS verification | VERIFY | Setup endpoints accessibility |
| All PRD acceptance criteria | 14/15 covered (AC-16 gap documented) | AC-16 returns 403 not 401 — need AuthenticationEntryPoint fix |

### Scope Enforcement Verified

- `ProductController` — `authService.requireRole("admin", "staff")` on all endpoints
- `UserController` — `authService.requireRole("admin", "manager")` + scoped user list
- `manager` → products = **403** (verified in code)
- `staff` → users = **403** (verified in code)
- `active=false` login → **403 account_disabled** (verified in AuthController)

---

## Schema State

```sql
-- products table: UNCHANGED
CREATE TABLE products (
    id INTEGER PRIMARY KEY,
    barcode TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    price REAL CHECK(price >= 0.01),
    stock INTEGER CHECK(stock >= 0),
    created_at TEXT
);

-- users table: MIGRATED
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT CHECK(role IN ('admin','manager','staff')) DEFAULT 'staff',
    active INTEGER DEFAULT 1,
    created_at TEXT
);
```

---

## Key Decisions Made

| Decision | Value | Ref |
|----------|-------|-----|
| JWT secret | From `JWT_SECRET` env var (required, >=32 bytes) | T-auth-01 |
| Token expiry | 24 hours (86,400,000ms) | T-auth-01 |
| Scope enforcement | Handler-level (not route-level) | T-auth-05, AC-21 |
| User creation | Manager → hard-coded `staff`; admin can set `staff`/`manager` | T-auth-07 |
| Deactivation | Soft (`active` flag), not hard delete | T-auth-11 |
| QR token | In-memory with TTL (60 min), invalidated after first admin | T-auth-12 |
| Default admin seed | REMOVED — QR onboarding only | T-auth-23 |

---

## Unknowns / Narrow Rerun Needed

1. **JWT_SECRET env var** — Must be set for server to start. Not seeded in dev.
2. **Frontend test coverage** — No test files found in `client/src/test/`.
3. **Backend test structure** — `server/src/test/java/com/makmur/` exists but contents unknown (need to list).
4. **Database migration** — Schema migration runs at startup? Verify `SchemaMigration.java`.
5. **BarcodeDetector fallback** — Client has `Viewfinder.tsx` but no explicit zxing-js lazy-load logic visible in quick scan.

---

## Status Summary

- **Phase 1-6 (v1 core)**: Likely complete (client has all components)
- **Auth Phase**: Complete — server, frontend, and 61/61 tests passing
- **Verification needed**: Verify CORS (T-auth-24), fix AC-16 (AuthenticationEntryPoint for 401), address gaps (reset password UI, password min-length alignment)