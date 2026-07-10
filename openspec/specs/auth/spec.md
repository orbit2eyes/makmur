# Domain: auth

## Purpose

Provide authentication (verify who the user is) and authorization (control what each user can do) for Makmur v1. The auth layer gates all feature access behind a JWT-based login, enforces role-based scope at the handler level, supports user management by managers/admins, and provides a QR-code-based first-admin onboarding flow for greenfield deployments.
## Requirements
### Requirement: REQ-auth-001 - Login with username and password returns JWT

- Description: A user with valid credentials SHALL authenticate via `POST /api/auth/login`. The server SHALL look up the user by username, verify the bcrypt hash, and return a JWT containing `sub` (user ID), `role` (`admin`, `manager`, or `staff`), `iat` (issued-at), and `exp` (expiration). The response SHALL also include the user object (id, username, role, active). Invalid credentials SHALL return `401 Unauthorized`. Deactivated accounts SHALL return `403 Forbidden` with an `account_disabled` message.
- Priority: P0
- Rationale: All protected features depend on successful authentication. No user can access any feature without a valid token.
- Acceptance criteria: AC-15, AC-24, AC-25, AC-28

#### Scenario: S-auth-001 - Login success

**Given** a registered user with username "jane" and a valid password
**When** they submit the login form with correct credentials via `POST /api/auth/login`
**Then** the server returns HTTP 200 with a JWT token and user object `{ id, username, role, active }`, the client stores the token in `sessionStorage`, and redirects to the role-appropriate home view

#### Scenario: S-auth-002 - Login failure — invalid credentials

**Given** a registered user with username "jane"
**When** they submit the login form with an incorrect password
**Then** the server returns HTTP 401 with `{ error: "invalid_credentials", message: "Invalid username or password" }` and the client shows a generic error message (no indication of whether the username or password was wrong, to prevent user enumeration)

### Requirement: REQ-auth-002 - Protected endpoints reject unauthenticated requests

- Description: Every API endpoint except `POST /api/auth/login`, `GET /api/health`, and the setup endpoints (`GET /api/setup/token`, `GET /api/setup/qr`, `POST /api/setup/register`) MUST reject requests that lack a valid `Authorization: Bearer <token>` header. Missing tokens SHALL return `401 Unauthorized`. Expired or malformed tokens SHALL also return `401 Unauthorized`.
- Priority: P0
- Rationale: Ensures no data is accessible without valid authentication.
- Acceptance criteria: AC-16, AC-17

#### Scenario: S-auth-020 - Unauthenticated access to protected endpoint

**Given** no JWT is stored in `sessionStorage`
**When** the user navigates to any protected route or the client makes an API request without an `Authorization` header
**Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Authentication required" }`, and the client redirects to the login page

#### Scenario: S-auth-022 - Missing token returns 401

**Given** no JWT is present in the `Authorization` header
**When** the client makes a request to a protected endpoint
**Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Authentication required" }`

#### Scenario: S-auth-023 - Malformed token returns 401

**Given** a JWT that is not a valid signed token (corrupted signature, invalid base64, wrong format)
**When** the client makes a request with this malformed token in the `Authorization` header
**Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Invalid token" }`

### Requirement: REQ-auth-003 - Scope-based authorization enforced at handler level

- Description: Each endpoint SHALL check the caller's role inside the handler method, not at the routing layer. The same endpoint URL SHALL serve all roles. If the caller's role does not match the required scope for that operation, the handler SHALL return `403 Forbidden`. The scope matrix:
  - **Products** (list, search, lookup, create, stock update): `staff` and `admin` allowed; `manager` blocked.
  - **Users** (list, create, deactivate, reactivate, reset-password): `manager` and `admin` allowed; `staff` blocked.
  - **Health**: all roles allowed.
- Priority: P0
- Rationale: PRD AC-21 explicitly requires handler-level scoping. Prevents role escalation through route manipulation.
- Acceptance criteria: AC-18, AC-19, AC-20, AC-21

#### Scenario: S-auth-004 - Staff accessing product endpoints

**Given** a user with role `staff` who holds a valid JWT
**When** they request `GET /api/products`, `GET /api/products/search`, `GET /api/products/:barcode`, `POST /api/products`, or `PATCH /api/products/:barcode/stock`
**Then** the server returns HTTP 200 with the requested data (same as pre-auth v1 behaviour)

#### Scenario: S-auth-005 - Staff denied from user management

**Given** a user with role `staff` who holds a valid JWT
**When** they request `GET /api/users`, `POST /api/users`, or any `PATCH /api/users/:id/*` endpoint
**Then** the server returns HTTP 403 with `{ error: "forbidden", message: "Access denied" }`

#### Scenario: S-auth-008 - Manager denied from product endpoints

**Given** a user with role `manager` who holds a valid JWT
**When** they request any product endpoint (`GET /api/products`, `GET /api/products/search`, `GET /api/products/:barcode`, `POST /api/products`, `PATCH /api/products/:barcode/stock`)
**Then** the server returns HTTP 403 with `{ error: "forbidden", message: "Access denied" }`

#### Scenario: S-auth-011 - Admin accessing all endpoints

**Given** a user with role `admin` who holds a valid JWT
**When** they request any product endpoint or any user-management endpoint
**Then** the server returns HTTP 200 and processes the request. Admin sees all users (including managers and other admins) when listing. Admin can create both `staff` and `manager` accounts.

### Requirement: REQ-auth-004 - Role-scoped user management

- Description: User-management endpoints (`GET /api/users`, `POST /api/users`, `PATCH /api/users/:id/deactivate`, `PATCH /api/users/:id/reactivate`, `PATCH /api/users/:id/reset-password`) SHALL be accessible only to `manager` and `admin` roles. `staff` users SHALL receive `403` on all user-management endpoints. Within these endpoints:
  - **Manager** can list, create, deactivate, reactivate, and reset passwords for `staff` accounts only. Creating a user SHALL always hard-code `role='staff'` server-side regardless of any client-supplied role field. Deactivation/reactivation SHALL be restricted to `staff`-role targets; attempting to deactivate a manager or admin SHALL return `403`.
  - **Admin** can list all users, create both `staff` and `manager` accounts, deactivate/reactivate any user, and reset passwords for any user. Admin SHALL NOT create another `admin` account through the API (admin creation is limited to the QR onboarding flow).
- Priority: P0
- Rationale: Prevents role escalation and ensures separation of duties between staff, manager, and admin.
- Acceptance criteria: AC-18, AC-19, AC-20

#### Scenario: S-auth-007 - Manager accessing user management

**Given** a user with role `manager` who holds a valid JWT
**When** they request `GET /api/users`, `POST /api/users`, `PATCH /api/users/:id/deactivate`, or `PATCH /api/users/:id/reset-password`
**Then** the server returns HTTP 200 and processes the request. The user list is filtered to show only `staff`-role accounts. Creating a user always creates a `staff` account regardless of any client-supplied role value.

#### Scenario: S-auth-010 - Manager blocked from deactivating another manager

**Given** a user with role `manager` who holds a valid JWT
**When** they attempt to deactivate another `manager`-role user via `PATCH /api/users/:id/deactivate`
**Then** the server returns HTTP 403 with `{ error: "forbidden", message: "You can only deactivate staff accounts" }`

### Requirement: REQ-auth-005 - QR-code-based admin onboarding

- Description: When the `users` table has no `admin` user, the server SHALL generate a one-time setup token (UUID v4) stored in-memory. The client SHALL detect no admin exists and display a setup page with a QR code. The QR code SHALL encode a setup URL with the token as a query parameter. Scanning the QR SHALL open the setup form where the admin creates the first account (username, password, confirm password). After successful creation, the token SHALL be invalidated. Subsequent access to setup endpoints SHALL return `403 Forbidden`. The token SHALL persist until used or expires (60-minute TTL from server start). A server restart SHALL generate a new token.
- Priority: P0
- Rationale: Greenfield deployments need a secure way to bootstrap the first admin account without out-of-band credential seeding.
- Acceptance criteria: AC-22, AC-23

#### Scenario: S-auth-013 - QR onboarding flow — first visit

**Given** the server has just started and the `users` table is empty
**When** a browser visits the app for the first time
**Then** the app displays the setup page showing a QR code (from `GET /api/setup/qr`) and instructions: "Scan this QR code with your phone to set up the admin account"

#### Scenario: S-auth-014 - QR onboarding flow — setup completion

**Given** a valid setup token exists and no admin user exists
**When** the admin scans the QR code, navigates to the setup URL with the token, fills in username and password, and submits `POST /api/setup/register`
**Then** the server creates the first `admin` user, invalidates the setup token, returns HTTP 201, and the client redirects to the login page with a success message

#### Scenario: S-auth-015 - QR onboarding — token invalidated after use

**Given** the first admin account has been created
**When** someone accesses `GET /api/setup/token`
**Then** the server returns HTTP 403 with `{ error: "already_setup", message: "Admin account already exists" }`

### Requirement: REQ-auth-006 - Passwords stored as bcrypt hashes

- Description: All passwords stored in the `users` table MUST be bcrypt-encoded strings. The `password_hash` column SHALL store only bcrypt hashes (prefixed `$2a$` or `$2b$`). Spring Security's `BCryptPasswordEncoder` (strength factor 10) SHALL be used for hashing and verification. Plain-text passwords SHALL NOT be stored, logged, or returned in API responses.
- Priority: P0
- Rationale: Critical security requirement — leaked database file must not expose plain-text credentials.
- Acceptance criteria: AC-24

#### Scenario: S-auth-025 - Password stored as bcrypt hash

**Given** a new user is created with password "mypassword"
**When** the password is stored in the `users` table
**Then** the `password_hash` column contains a string starting with `$2a$` or `$2b$`
**And** the original plain-text password is not stored anywhere

### Requirement: REQ-auth-007 - JWT structure and expiry

- Description: The JWT MUST contain the following claims: `sub` (user ID as string), `role` (the user's role string), `iat` (issued-at Unix timestamp), `exp` (expiration Unix timestamp). The difference between `exp` and `iat` MUST NOT exceed 86,400 seconds (24 hours). The signing secret SHALL be read from the `JWT_SECRET` environment variable; the server SHALL fail to start if this variable is unset or shorter than 32 bytes (256 bits).
- Priority: P0
- Rationale: Limits token exposure window. Environment-variable-based secret prevents hardcoded fallbacks.
- Acceptance criteria: AC-25

#### Scenario: S-auth-016 - Token expiry — mid-session

**Given** a user has been logged in for more than 24 hours (JWT expired)
**When** the client makes any API request
**Then** the server returns HTTP 401. The client clears the token from `sessionStorage` and redirects to the login page.

### Requirement: REQ-auth-008 - Schema migration — users table

- Description: The `users` table schema SHALL be updated:
  - `password` column SHALL be renamed to `password_hash` (semantic rename — makes hash storage explicit).
  - `active` column SHALL be added (INTEGER NOT NULL DEFAULT 1; `0` = deactivated, `1` = active).
  - `role` CHECK constraint SHALL be extended to include `'manager'` in addition to `'admin'` and `'staff'`.
  - The `products` table SHALL be unchanged.
- Priority: P1
- Rationale: The schema change is required for auth to work. Rename makes the storage format explicit. The `active` column enables soft deactivation without data loss.
- Acceptance criteria: AC-26

#### Scenario: S-auth-026 - Schema migration runs on startup

**Given** an existing database with the old `users` table schema (column name `password`, no `active`, limited role CHECK)
**When** the server starts
**Then** the migration creates the new schema with `password_hash`, `active`, and extended `role` CHECK
**And** existing data is preserved

### Requirement: REQ-auth-009 - Frontend login page and session management

- Description: The app SHALL display a login page (`/login`) for unauthenticated users. The login form SHALL have username and password fields, a submit button, and error display. On successful login, the JWT SHALL be stored in `sessionStorage` and the user SHALL be redirected to their role-appropriate home view:
  - `staff` SHALL go to product catalogue / scan view
  - `manager` SHALL go to user management view
  - `admin` SHALL go to dashboard with navigation to both products and users
  The login page SHALL be the default redirect for any unauthenticated request. The setup page (QR flow) SHALL be the only exception, and only when no admin exists.
- Priority: P0
- Rationale: All access is gated behind authentication. No feature is available without logging in.
- Acceptance criteria: AC-27, AC-29

#### Scenario: S-auth-027 - Login page shown for unauthenticated users

**Given** a user has no valid JWT
**When** they open the app
**Then** the login page is displayed with username and password fields
**And** the setup page is shown only when no admin exists

### Requirement: REQ-auth-010 - Frontend JWT expiry detection and 401 interception

- Description: The client-side API layer (`api.ts`) MUST detect expired JWT tokens by checking the `exp` claim before making requests. If the token is expired, the client SHALL clear the token from `sessionStorage` and redirect to the login page. Additionally, any API response with status `401` SHALL trigger the same redirect. The client SHALL NOT automatically retry on 401.
- Priority: P1
- Rationale: Prevents failed API calls from surfacing to the user as opaque errors. Proactive expiry detection avoids unnecessary server round-trips.
- Acceptance criteria: AC-27

#### Scenario: S-auth-017 - Token expiry — proactive detection

**Given** a user has a JWT whose `exp` claim is in the past
**When** the client initialises or before making an API request
**Then** the client detects the expiry, clears the token from `sessionStorage`, and redirects to the login page without making the API call

#### Scenario: S-auth-021 - Expired token returns 401

**Given** a user whose JWT has expired (the `exp` claim is in the past)
**When** the client makes any API request with the expired token
**Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Token has expired" }`, and the client clears the token and redirects to the login page

### Requirement: REQ-auth-011 - Frontend role-based navigation and view gating

- Description: The frontend sidebar/navigation SHALL render links based on the logged-in user's role, derived from the JWT payload:
  - `staff`: Dashboard, Products, Scan. No link to user management.
  - `manager`: Dashboard, Users (staff management only). No link to products or scan.
  - `admin`: Dashboard, Products, Scan, Users.
  The role SHALL be used for UI navigation hints only. The server SHALL remain the authority for access control — client-side role checks are never an access control boundary.
- Priority: P1
- Rationale: Staff should not see UI elements for features they cannot use. Manager should not see product UI.
- Acceptance criteria: AC-18, AC-19, AC-20 (indirect — server is the authority, but UI should align)

#### Scenario: S-auth-006 - Staff sees only product-related UI

**Given** a user with role `staff` who has logged in
**When** the app renders the sidebar/navigation
**Then** the user sees navigation links for Dashboard, Products, and Scan only. No link to Users or user management appears.

#### Scenario: S-auth-009 - Manager sees only user management UI

**Given** a user with role `manager` who has logged in
**When** the app renders the sidebar/navigation
**Then** the user sees navigation links for Dashboard and Users only. No links to Products or Scan appear.

#### Scenario: S-auth-012 - Admin sees comprehensive navigation

**Given** a user with role `admin` who has logged in
**When** the app renders the sidebar/navigation
**Then** the user sees navigation links for Dashboard, Products, Scan, and Users

### Requirement: REQ-auth-012 - Logout button

- Description: A logout button SHALL be visible at all times in the sidebar footer alongside the current username and role badge. Clicking logout SHALL clear the JWT from `sessionStorage` and redirect to the login page. No server-side token invalidation is needed (tokens remain valid until expiry — v1 has no token blacklist).
- Priority: P1
- Rationale: Staff on shared devices need a clear way to end their session.
- Acceptance criteria: AC-27

#### Scenario: S-auth-019 - Logout

**Given** a logged-in user
**When** they click the Logout button
**Then** the client clears the JWT from `sessionStorage` and redirects to the login page. No server-side token invalidation occurs.

### Requirement: REQ-auth-013 - Deactivated account handling

- Description: A user whose `active` flag is `0` SHALL NOT be able to log in. The login endpoint SHALL return `403 Forbidden` with error code `account_disabled` and message "Account is deactivated. Contact your manager." The frontend login page SHALL display this distinct error message (separate from generic `invalid_credentials`). Deactivated users with still-valid JWTs (logged in before deactivation) SHALL be caught by the JWT filter's `active`-flag check and receive `403` on their next API request.
- Priority: P0
- Rationale: Manager/admin must be able to revoke access immediately without waiting for token expiry.
- Acceptance criteria: AC-28

#### Scenario: S-auth-003 - Login failure — deactivated account

**Given** a user whose `active` flag is `0` (deactivated by manager/admin)
**When** they attempt to log in with correct credentials
**Then** the server returns HTTP 403 with `{ error: "account_disabled", message: "Account is deactivated. Contact your manager." }` and the client shows a distinct "account disabled" message with the contact-your-manager guidance

#### Scenario: S-auth-018 - Deactivated user with still-valid JWT

**Given** a user with an active session (valid, non-expired JWT)
**When** a manager deactivates their account via `PATCH /api/users/:id/deactivate`
**Then** on the user's next API request, the JWT filter checks the `active` flag, finds it `0`, and returns HTTP 403. The client clears the token and redirects to the login page.

#### Scenario: S-auth-024 - Deactivated account returns 403 with account_disabled

**Given** a user whose `active` flag is `0`
**When** they attempt to log in with correct credentials
**Then** the server returns HTTP 403 with `{ error: "account_disabled", message: "Account is deactivated. Contact your manager." }`
**And** the client shows a distinct "account disabled" error message with contact guidance

### Requirement: REQ-auth-014 - Standardized API error responses in Indonesian

- Description: All API endpoints SHALL return errors in a consistent JSON format with `error` (machine-readable snake_case code) and `message` (human-readable description in Indonesian) fields. The `error` code field SHALL remain in English (machine-readable). The `message` field SHALL use Indonesian translations. Error responses MUST never include stack traces, internal server paths, or SQL details.

  | HTTP Status | error_code | message (Indonesian) |
  |-------------|-----------|---------------------|
  | 400 | bad_request | `Permintaan tidak valid. Periksa kembali data yang dikirim.` |
  | 401 | unauthorized | `Sesi telah berakhir. Silakan login kembali.` |
  | 403 | forbidden | `Akses ditolak. Anda tidak memiliki izin yang cukup.` |
  | 403 | account_disabled | `Akun dinonaktifkan. Hubungi atasan Anda.` |
  | 404 | not_found | `Data tidak ditemukan.` |
  | 409 | conflict | `Data sudah ada. Gunakan nilai yang berbeda.` |
  | 422 | unprocessable_entity | `Data tidak valid. Periksa kembali input Anda.` |
  | 500 | internal_error | `Terjadi kesalahan. Coba lagi nanti.` |

- Priority: P0
- Rationale: Error messages are seen by staff when things go wrong. English messages cause confusion and support calls. Indonesian messages help staff understand and resolve issues independently.
- Change: Original error `message` fields were in English. New behavior uses Indonesian translations. The `error` code field remains English for machine processing.
- Acceptance criteria: AC3

#### Scenario: S-auth-025 - Error message in Indonesian

**Given** a staff member makes an API request that fails with 404
**When** the server returns the error response
**Then** the `message` field contains `Data tidak ditemukan.`
**And** the `error` field remains `not_found`

## Data Model

### User

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | integer | PRIMARY KEY, AUTOINCREMENT | Auto-generated unique identifier |
| `username` | text | UNIQUE, NOT NULL | Login username |
| `password_hash` | text | NOT NULL | Bcrypt hash of the password |
| `role` | text | NOT NULL, CHECK(role IN ('admin', 'manager', 'staff')), DEFAULT 'staff' | Role determining feature access scope |
| `active` | integer | NOT NULL, DEFAULT 1 | Soft deactivation flag: 1 = active, 0 = deactivated |
| `created_at` | text | NOT NULL, DEFAULT ISO 8601 | Timestamp of user account creation |
