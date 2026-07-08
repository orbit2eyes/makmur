---
slug: makmur-0
summary: Initial launch of Makmur — a barcode scanning inventory webapp for retail and warehouse staff
domains: [product, scan, stock]
status: planning
---

# Makmur v1 — Initial Launch

## Summary

Makmur is a greenfield inventory webapp that puts a barcode scanner in every staff member's pocket. Staff point their phone camera at any EAN-13 barcode to identify the product, view stock, update counts, or create new products on the fly. No installs, no accounts — any device with a modern browser and a camera on the local network works.

## Affected Domains

All three domains are **ADDED** (greenfield — no existing OpenSpec specs to modify):

| Domain | Scope |
|--------|-------|
| **product** | Product catalogue — CRUD, barcode lookup, list, search |
| **scan** | Barcode scanning pipeline — camera access, EAN-13 decode, result routing |
| **stock** | Stock management — view, update, non-negative constraint |

## Approach

Single-page webapp served from a local network server. Server holds the product catalogue in SQLite. Client is a responsive HTML/CSS/JS frontend that uses `navigator.mediaDevices.getUserMedia` for camera access and the `BarcodeDetector` API (or a polyfill) for EAN-13 decoding. Two devices on the same network see the same data; writes on one device are visible on the other after a page refresh.

The guiding principle: every common inventory action is reachable in two taps or fewer from a scan.

## References

- PRD: `openspec/changes/makmur-0/prd.md`
- Product spec: `openspec/changes/makmur-0/specs/product/spec.md`
- Scan spec: `openspec/changes/makmur-0/specs/scan/spec.md`
- Stock spec: `openspec/changes/makmur-0/specs/stock/spec.md`

---

# Authorization & Access Control

## Summary

Makmur v1 was designed as a single-user local-network app with no access control. The Authorization & Access Control feature adds role-based authentication and authorization — JWT login, bcrypt password hashing, three scoped roles (admin, manager, staff), and a QR-code first-admin onboarding flow — while keeping the same API endpoints serving all roles with handler-level scope filtering.

## Problem

Without auth, Makmur cannot be deployed in real retail environments with multiple staff members:

- **No audit trail** — Every stock change is anonymous. Mistakes and disputes are untraceable.
- **Unrestricted access** — Anyone on the local network can create, edit, or delete any data. No way to restrict sensitive operations (e.g., staff management) to trusted users.
- **No separation of duties** — A cashier scanning price checks should not be able to modify staff accounts. Today every user can do everything.
- **No onboarding guard** — First deployment has no initial user setup. Someone must seed the first admin account out of band.

## Solution Overview

### Authentication — JWT + bcrypt

- `POST /api/auth/login` accepts `{ username, password }`, verifies bcrypt hash, returns JWT with `sub` (user ID), `role`, `iat`, `exp`.
- All subsequent requests carry `Authorization: Bearer <token>`.
- JWT signing secret from environment variable (`JWT_SECRET`), never in code.
- Token expiry: 24 hours (single long-lived token; no refresh-token mechanism in v1).

### Authorization — handler-level scope filtering

- **Same endpoints serve all roles.** No role-gated routes. The handler inspects the JWT role and filters data or rejects with `403`.
- **staff** scope: products only (scan, stock, search, browse). `403` on any user-management endpoint.
- **manager** scope: staff management only (create, deactivate, password reset). `403` on any product endpoint.
- **admin** scope: full access — products + user management (create managers, create/deactivate staff, reset passwords).
- Scope enforcement is **server-side only**. The frontend uses the role from the JWT payload to show/hide nav items, but the server is the authority.

### Onboarding — QR code first-admin setup

- Server generates a one-time setup token at startup when `users` table is empty.
- First visit shows a QR code encoding a setup URL with the token.
- Scanning the QR code opens a setup page: create username + password for the first admin.
- After submission, the token is invalidated. Subsequent visits redirect to login.

## Affected Domains

| Domain | Scope |
|--------|-------|
| **auth** (new) | Authentication — login endpoint, JWT generation/verification, password hashing, token middleware |
| **authorization** (new) | Role-based scope enforcement — handler-level role checks, `403` responses, role-aware data filtering |
| **onboarding** (new) | First-admin setup — QR code generation, one-time token, setup page flow |
| **product** (modified) | Product endpoints gain JWT auth middleware and scope checks. Staff can access; managers are blocked. |
| **stock** (modified) | Stock update endpoints gain JWT auth middleware and scope checks. Staff can access; managers are blocked. |
| **scan** (modified) | Scan/lookup endpoint gains JWT auth middleware. Staff can access; managers are blocked. |

## Schema Changes

One table modified (`users`), one column renamed, one constraint extended:

```sql
-- Current schema:
--   password   TEXT NOT NULL
--   role       TEXT NOT NULL DEFAULT 'staff' CHECK(role IN ('admin', 'staff'))

-- New schema:
--   password_hash   TEXT NOT NULL,
--   active          INTEGER NOT NULL DEFAULT 1,
--   role            TEXT NOT NULL DEFAULT 'staff' CHECK(role IN ('admin', 'manager', 'staff'))
```

- `password` → `password_hash` (semantic rename to make hash storage explicit)
- `active` column added (soft deactivation: `0` = disabled, `1` = active)
- `role` CHECK constraint extended to include `'manager'`
- `products` table — **unchanged**

## Trade-offs Considered

| Trade-off | Chosen | Rationale |
|-----------|--------|-----------|
| Single long-lived JWT vs. refresh-token pair | Single 24h JWT | Simpler to implement. Refresh tokens add a token-storage endpoint, a refresh endpoint, and client-side refresh logic. Defer to v2 if mid-session expiry becomes a pain point. |
| Handler-level vs. route-level scope checks | Handler-level (same endpoint, one implementation) | AC-21 explicitly requires handler-level scoping. Avoids route duplication and keeps the API surface uniform. |
| Server-enforced vs. client-enforced role gating | Server-enforced, client as UX hint | The server always validates the JWT and checks scopes. The frontend uses the role for nav visibility only — never as an access control boundary. |
| Soft deactivation vs. hard delete | Soft deactivation (`active` column) | Preserves referential integrity and allows reactivation. Deletion is a database-level operation reserved for admins. |
| QR setup token: time-limited vs. persist-until-used | Persist until first admin created | Simpler implementation. Time-limiting adds a clock dependency and a failure mode (token expired before admin scans it). Security risk is low because the setup page is only served on the local network. |
| bcrypt vs. scrypt vs. argon2 | bcrypt | Available in Java stdlib (Spring Security `BCryptPasswordEncoder`). Good-enough security for a local-network app. Argon2 would add a native-library dependency. |

## Risk Mitigations

- **JWT secret**: Must be set via `JWT_SECRET` env var. Server fails to start if unset. No default/fallback.
- **Role escalation**: Manager user-creation endpoint hard-codes `role='staff'` server-side. The `role` field from the client is ignored. Only the admin endpoint can set `role='manager'`.
- **Database bypass**: SQLite file stored outside web root with filesystem permissions restricted to the application process.
- **Credential management**: Minimum password length (8 chars). Force password change on first login (optional — can be deferred).

## Proposed Implementation Order

### Phase 1 — Server auth foundation

Files: `server/src/main/java/com/makmur/...`

1. Add JWT utility class (`JwtUtil.java`) — generate, verify, decode tokens. Reads secret from `JWT_SECRET` env var.
2. Add auth middleware/filter — intercept requests, extract JWT from `Authorization` header, populate request context with `userId` and `role`.
3. Add login endpoint (`POST /api/auth/login`) — look up user by username, verify bcrypt hash, return JWT + user payload.
4. Add `UserRepository` — JDBC queries for user lookup by username, user creation, user list (scoped).
5. Update schema: rename `password` → `password_hash`, add `active` column, extend `role` CHECK.
6. Add `BCryptPasswordEncoder` bean (Spring Security dependency or manual `BCrypt`).

### Phase 2 — Server authorization layer

Files: `server/src/main/java/com/makmur/...`

1. Add role-check annotation or utility (`@RequireRole`) — declarative scope gating on handler methods.
2. Apply scope checks to existing product/stock/scan handlers — reject if role is `manager`.
3. Add user-management endpoints (`GET /api/users`, `POST /api/users`, `PATCH /api/users/:id/deactivate`, `PATCH /api/users/:id/reset-password`) — admin creates managers, manager creates staff.
4. Scope-check these new endpoints — only `manager` and `admin` can access; `staff` gets `403`.
5. Add active-flag check in login flow — deactivated accounts return `403 Forbidden` with "account disabled" message.

### Phase 3 — Server onboarding flow

Files: `server/src/main/java/com/makmur/...`

1. Add startup check: if `users` table is empty, generate one-time setup token (UUID).
2. Expose `GET /api/setup/token` — returns the token (only when no admin exists).
3. Expose `POST /api/setup/register` — accepts token + username + password, creates first admin, invalidates token.
4. Generate QR code endpoint or serve QR image inline (`GET /api/setup/qr`) — encodes the setup URL with token as query param.

### Phase 4 — Frontend auth layer

Files: `client/src/...`

1. Wire up `AuthContext` — the existing boilerplate already has `login()`, `logout()`, `token` state, and `authHeaders()`. Ensure `login()` calls `POST /api/auth/login`, stores the JWT, and populates the user context.
2. Add JWT expiry detection — decode `exp` claim in `api.ts` interceptor, redirect to login page if expired.
3. Protect routes — wrap `<Route>` components with a `ProtectedRoute` that checks JWT presence and redirects to `/login` if missing.
4. Role-based navigation — after login, redirect based on role:
   - `staff` → product/scan view
   - `manager` → user-management view
   - `admin` → dashboard with nav to both products and users
5. Add logout button — clears JWT from storage, redirects to login.

### Phase 5 — Frontend role-aware views

Files: `client/src/components/...`

1. **Staff view** — unchanged from v1. Product catalogue, scan, stock update. The sidebar/nav hides user-management links.
2. **Manager view** — user list table (staff accounts only), create staff form, deactivate/reactivate buttons, password reset button. No product links visible.
3. **Admin view** — nav sidebar with links to both products and user management. User management shows all users with role badges. Can create managers and staff.
4. Login page — clean up existing `Login.tsx` boilerplate, add error display, disable form during submission.
5. Setup page — first-visit QR code display or setup form when the setup token is present in the URL.

### Phase 6 — Schema migration

1. Create migration script or DDL patch for existing deployments:
   - `ALTER TABLE users RENAME COLUMN password TO password_hash;`
   - `ALTER TABLE users ADD COLUMN active INTEGER NOT NULL DEFAULT 1;`
   - Drop old CHECK and re-create with `'manager'` included (SQLite requires table recreation for CHECK changes).
2. Seed script: if no admin exists after migration, prompt for admin creation via the onboarding flow.

## Success Criteria

All acceptance criteria from PRD Section 9.6 must pass:

| ID | Summary |
|----|---------|
| AC-15 | Login returns JWT for valid credentials, `401` for invalid |
| AC-16 | No JWT → `401` on protected endpoints |
| AC-17 | Expired/malformed JWT → `401` |
| AC-18 | Staff can access products but gets `403` on user mgmt |
| AC-19 | Manager can access user mgmt but gets `403` on products |
| AC-20 | Admin can access everything |
| AC-21 | Scope checks inside handler, not routing layer |
| AC-22 | QR flow when no admin exists |
| AC-23 | Setup token invalidated after first admin created |
| AC-24 | Passwords stored as bcrypt hashes |
| AC-25 | JWT has `sub`, `role`, `iat`, `exp`; expiry ≤ 24h |
| AC-26 | Schema migration: `password_hash`, `active`, `role` CHECK includes `'manager'` |
| AC-27 | Expired/logged-out user redirected to login |
| AC-28 | Deactivated account cannot log in (`403`) |
| AC-29 | Login page is default redirect; setup page only when no admin |

## Rollback Plan

- **Database**: Keep the original `password` column data during migration (copy to `password_hash` but don't drop `password` until the next release). This allows reverting the server binary to the pre-auth version.
- **Frontend**: The pre-auth frontend has no `Authorization` headers. If rolled back, the server falls back to no-JWT mode. Not clean — but workable: the auth middleware can be configured to permit unauthenticated access to product endpoints when a `DISABLE_AUTH=true` env var is set. This flag should be removed after the rollback window closes.
- **Token secret rotation**: If `JWT_SECRET` is compromised, regenerate it and restart the server. All existing tokens become invalid — all users must re-login. This is acceptable because tokens are short-lived (24h).
