# Domain: auth

## Purpose

Provide authentication (verify who the user is) and authorization (control what each user can do) for Makmur v1. The auth layer gates all feature access behind a JWT-based login, enforces role-based scope at the handler level, supports user management by managers/admins, and provides a QR-code-based first-admin onboarding flow for greenfield deployments.

## Requirements

### ADDED

- **REQ-auth-001**: Login with username and password returns JWT
  - Description: A user with valid credentials can authenticate via `POST /api/auth/login`. The server looks up the user by username, verifies the bcrypt hash, and returns a JWT containing `sub` (user ID), `role` (`admin`, `manager`, or `staff`), `iat` (issued-at), and `exp` (expiration). The response also includes the user object (id, username, role, active). Invalid credentials return `401 Unauthorized`. Deactivated accounts return `403 Forbidden` with an `account_disabled` message.
  - Priority: P0
  - Rationale: All protected features depend on successful authentication. No user can access any feature without a valid token.
  - Acceptance criteria: AC-15, AC-24, AC-25, AC-28

- **REQ-auth-002**: Protected endpoints reject unauthenticated requests
  - Description: Every API endpoint except `POST /api/auth/login`, `GET /api/health`, and the setup endpoints (`GET /api/setup/token`, `GET /api/setup/qr`, `POST /api/setup/register`) MUST reject requests that lack a valid `Authorization: Bearer <token>` header. Missing tokens return `401 Unauthorized`. Expired or malformed tokens also return `401 Unauthorized`.
  - Priority: P0
  - Rationale: Ensures no data is accessible without valid authentication.
  - Acceptance criteria: AC-16, AC-17

- **REQ-auth-003**: Scope-based authorization enforced at handler level
  - Description: Each endpoint checks the caller's role inside the handler method, not at the routing layer. The same endpoint URL serves all roles. If the caller's role does not match the required scope for that operation, the handler returns `403 Forbidden`. The scope matrix:
    - **Products** (list, search, lookup, create, stock update): `staff` and `admin` allowed; `manager` blocked.
    - **Users** (list, create, deactivate, reactivate, reset-password): `manager` and `admin` allowed; `staff` blocked.
    - **Health**: all roles allowed.
  - Priority: P0
  - Rationale: PRD AC-21 explicitly requires handler-level scoping. Prevents role escalation through route manipulation.
  - Acceptance criteria: AC-18, AC-19, AC-20, AC-21

- **REQ-auth-004**: Role-scoped user management
  - Description: User-management endpoints (`GET /api/users`, `POST /api/users`, `PATCH /api/users/:id/deactivate`, `PATCH /api/users/:id/reactivate`, `PATCH /api/users/:id/reset-password`) are accessible only to `manager` and `admin` roles. `staff` users receive `403` on all user-management endpoints. Within these endpoints:
    - **Manager** can list, create, deactivate, reactivate, and reset passwords for `staff` accounts only. Creating a user always hard-codes `role='staff'` server-side regardless of any client-supplied role field. Deactivation/reactivation is restricted to `staff`-role targets; attempting to deactivate a manager or admin returns `403`.
    - **Admin** can list all users, create both `staff` and `manager` accounts, deactivate/reactivate any user, and reset passwords for any user. Admin cannot create another `admin` account through the API (admin creation is limited to the QR onboarding flow).
  - Priority: P0
  - Rationale: Prevents role escalation and ensures separation of duties between staff, manager, and admin.
  - Acceptance criteria: AC-18, AC-19, AC-20

- **REQ-auth-005**: QR-code-based admin onboarding
  - Description: When the `users` table has no `admin` user, the server generates a one-time setup token (UUID v4) stored in-memory. The client detects no admin exists and displays a setup page with a QR code. The QR code encodes a setup URL with the token as a query parameter. Scanning the QR opens the setup form where the admin creates the first account (username, password, confirm password). After successful creation, the token is invalidated. Subsequent access to setup endpoints returns `403 Forbidden`. The token persists until used or expires (60-minute TTL from server start). A server restart generates a new token.
  - Priority: P0
  - Rationale: Greenfield deployments need a secure way to bootstrap the first admin account without out-of-band credential seeding.
  - Acceptance criteria: AC-22, AC-23

- **REQ-auth-006**: Passwords stored as bcrypt hashes
  - Description: All passwords stored in the `users` table MUST be bcrypt-encoded strings. The `password_hash` column stores only bcrypt hashes (prefixed `$2a$` or `$2b$`). Spring Security's `BCryptPasswordEncoder` (strength factor 10) is used for hashing and verification. Plain-text passwords are never stored, logged, or returned in API responses.
  - Priority: P0
  - Rationale: Critical security requirement — leaked database file must not expose plain-text credentials.
  - Acceptance criteria: AC-24

- **REQ-auth-007**: JWT structure and expiry
  - Description: The JWT MUST contain the following claims: `sub` (user ID as string), `role` (the user's role string), `iat` (issued-at Unix timestamp), `exp` (expiration Unix timestamp). The difference between `exp` and `iat` MUST NOT exceed 86,400 seconds (24 hours). The signing secret is read from the `JWT_SECRET` environment variable; the server fails to start if this variable is unset or shorter than 32 bytes (256 bits).
  - Priority: P0
  - Rationale: Limits token exposure window. Environment-variable-based secret prevents hardcoded fallbacks.
  - Acceptance criteria: AC-25

- **REQ-auth-008**: Schema migration — users table
  - Description: The `users` table schema is updated:
    - `password` column renamed to `password_hash` (semantic rename — makes hash storage explicit).
    - `active` column added (INTEGER NOT NULL DEFAULT 1; `0` = deactivated, `1` = active).
    - `role` CHECK constraint extended to include `'manager'` in addition to `'admin'` and `'staff'`.
    - The `products` table is unchanged.
  - Priority: P1
  - Rationale: The schema change is required for auth to work. Rename makes the storage format explicit. The `active` column enables soft deactivation without data loss.
  - Acceptance criteria: AC-26

- **REQ-auth-009**: Frontend login page and session management
  - Description: The app displays a login page (`/login`) for unauthenticated users. The login form has username and password fields, a submit button, and error display. On successful login, the JWT is stored in `sessionStorage` and the user is redirected to their role-appropriate home view:
    - `staff` → product catalogue / scan view
    - `manager` → user management view
    - `admin` → dashboard with navigation to both products and users
  - The login page is the default redirect for any unauthenticated request. The setup page (QR flow) is the only exception, and only when no admin exists.
  - Priority: P0
  - Rationale: All access is gated behind authentication. No feature is available without logging in.
  - Acceptance criteria: AC-27, AC-29

- **REQ-auth-010**: Frontend JWT expiry detection and 401 interception
  - Description: The client-side API layer (`api.ts`) MUST detect expired JWT tokens by checking the `exp` claim before making requests. If the token is expired, the client clears the token from `sessionStorage` and redirects to the login page. Additionally, any API response with status `401` triggers the same redirect. The client does not automatically retry on 401.
  - Priority: P1
  - Rationale: Prevents failed API calls from surfacing to the user as opaque errors. Proactive expiry detection avoids unnecessary server round-trips.
  - Acceptance criteria: AC-27

- **REQ-auth-011**: Frontend role-based navigation and view gating
  - Description: The frontend sidebar/navigation renders links based on the logged-in user's role, derived from the JWT payload:
    - `staff`: Dashboard, Products, Scan. No link to user management.
    - `manager`: Dashboard, Users (staff management only). No link to products or scan.
    - `admin`: Dashboard, Products, Scan, Users.
  - The role is used for UI navigation hints only. The server remains the authority for access control — client-side role checks are never an access control boundary.
  - Priority: P1
  - Rationale: Staff should not see UI elements for features they cannot use. Manager should not see product UI.
  - Acceptance criteria: AC-18, AC-19, AC-20 (indirect — server is the authority, but UI should align)

- **REQ-auth-012**: Logout button
  - Description: A logout button is visible at all times in the sidebar footer alongside the current username and role badge. Clicking logout clears the JWT from `sessionStorage` and redirects to the login page. No server-side token invalidation is needed (tokens remain valid until expiry — v1 has no token blacklist).
  - Priority: P1
  - Rationale: Staff on shared devices need a clear way to end their session.
  - Acceptance criteria: AC-27

- **REQ-auth-013**: Deactivated account handling
  - Description: A user whose `active` flag is `0` cannot log in. The login endpoint returns `403 Forbidden` with error code `account_disabled` and message "Account is deactivated. Contact your manager." The frontend login page displays this distinct error message (separate from generic `invalid_credentials`). Deactivated users with still-valid JWTs (logged in before deactivation) are caught by the JWT filter's `active`-flag check and receive `403` on their next API request.
  - Priority: P0
  - Rationale: Manager/admin must be able to revoke access immediately without waiting for token expiry.
  - Acceptance criteria: AC-28

- **REQ-auth-014**: Standardized API error responses
  - Description: All API endpoints return errors in a consistent JSON format. Every error response contains exactly two fields: `error` (machine-readable snake_case code) and `message` (human-readable description). Error responses never include stack traces, internal server paths, or SQL details.

    | HTTP Status | error_code | When | Notes |
    |-------------|-----------|------|-------|
    | 400 | bad_request | Malformed request body, missing required fields, validation failure | |
    | 401 | unauthorized | No JWT provided, JWT expired, JWT malformed | Applied by JwtAuthenticationFilter |
    | 403 | forbidden | Authenticated but role lacks scope, deactivated account, setup already completed | Scope enforcement + deactivated check |
    | 404 | not_found | User, product, or resource not found | |
    | 409 | conflict | Duplicate username on create | |
    | 422 | unprocessable_entity | Password too short, invalid role value, business rule violation | |
    | 500 | internal_error | Unexpected server error | Never expose stack traces |

    Unauthenticated errors (`401`) use a generic message regardless of whether the username exists, the password is wrong, or the token is malformed — preventing user enumeration. Deactivated account errors (`403`) use `error: "account_disabled"` so the frontend can display a specific message, but no account existence info is revealed before authentication. Internal server errors (`500`) must never include stack traces, file paths, or SQL detail in the response body.
  - Priority: P0
  - Rationale: Consistent error format is required for reliable frontend error handling and prevents information leakage through stack traces or user enumeration.
  - Acceptance criteria: AC-30

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

## Scenarios

- **S-auth-001**: Login success
  - **Given** a registered user with username "jane" and a valid password
  - **When** they submit the login form with correct credentials via `POST /api/auth/login`
  - **Then** the server returns HTTP 200 with a JWT token and user object `{ id, username, role, active }`, the client stores the token in `sessionStorage`, and redirects to the role-appropriate home view

- **S-auth-002**: Login failure — invalid credentials
  - **Given** a registered user with username "jane"
  - **When** they submit the login form with an incorrect password
  - **Then** the server returns HTTP 401 with `{ error: "invalid_credentials", message: "Invalid username or password" }` and the client shows a generic error message (no indication of whether the username or password was wrong, to prevent user enumeration)

- **S-auth-003**: Login failure — deactivated account
  - **Given** a user whose `active` flag is `0` (deactivated by manager/admin)
  - **When** they attempt to log in with correct credentials
  - **Then** the server returns HTTP 403 with `{ error: "account_disabled", message: "Account is deactivated. Contact your manager." }` and the client shows a distinct "account disabled" message with the contact-your-manager guidance

- **S-auth-004**: Staff accessing product endpoints
  - **Given** a user with role `staff` who holds a valid JWT
  - **When** they request `GET /api/products`, `GET /api/products/search`, `GET /api/products/:barcode`, `POST /api/products`, or `PATCH /api/products/:barcode/stock`
  - **Then** the server returns HTTP 200 with the requested data (same as pre-auth v1 behaviour)

- **S-auth-005**: Staff denied from user management
  - **Given** a user with role `staff` who holds a valid JWT
  - **When** they request `GET /api/users`, `POST /api/users`, or any `PATCH /api/users/:id/*` endpoint
  - **Then** the server returns HTTP 403 with `{ error: "forbidden", message: "Access denied" }`

- **S-auth-006**: Staff sees only product-related UI
  - **Given** a user with role `staff` who has logged in
  - **When** the app renders the sidebar/navigation
  - **Then** the user sees navigation links for Dashboard, Products, and Scan only. No link to Users or user management appears.

- **S-auth-007**: Manager accessing user management
  - **Given** a user with role `manager` who holds a valid JWT
  - **When** they request `GET /api/users`, `POST /api/users`, `PATCH /api/users/:id/deactivate`, or `PATCH /api/users/:id/reset-password`
  - **Then** the server returns HTTP 200 and processes the request. The user list is filtered to show only `staff`-role accounts. Creating a user always creates a `staff` account regardless of any client-supplied role value.

- **S-auth-008**: Manager denied from product endpoints
  - **Given** a user with role `manager` who holds a valid JWT
  - **When** they request any product endpoint (`GET /api/products`, `GET /api/products/search`, `GET /api/products/:barcode`, `POST /api/products`, `PATCH /api/products/:barcode/stock`)
  - **Then** the server returns HTTP 403 with `{ error: "forbidden", message: "Access denied" }`

- **S-auth-009**: Manager sees only user management UI
  - **Given** a user with role `manager` who has logged in
  - **When** the app renders the sidebar/navigation
  - **Then** the user sees navigation links for Dashboard and Users only. No links to Products or Scan appear.

- **S-auth-010**: Manager blocked from deactivating another manager
  - **Given** a user with role `manager` who holds a valid JWT
  - **When** they attempt to deactivate another `manager`-role user via `PATCH /api/users/:id/deactivate`
  - **Then** the server returns HTTP 403 with `{ error: "forbidden", message: "You can only deactivate staff accounts" }`

- **S-auth-011**: Admin accessing all endpoints
  - **Given** a user with role `admin` who holds a valid JWT
  - **When** they request any product endpoint or any user-management endpoint
  - **Then** the server returns HTTP 200 and processes the request. Admin sees all users (including managers and other admins) when listing. Admin can create both `staff` and `manager` accounts.

- **S-auth-012**: Admin sees comprehensive navigation
  - **Given** a user with role `admin` who has logged in
  - **When** the app renders the sidebar/navigation
  - **Then** the user sees navigation links for Dashboard, Products, Scan, and Users

- **S-auth-013**: QR onboarding flow — first visit
  - **Given** the server has just started and the `users` table is empty
  - **When** a browser visits the app for the first time
  - **Then** the app displays the setup page showing a QR code (from `GET /api/setup/qr`) and instructions: "Scan this QR code with your phone to set up the admin account"

- **S-auth-014**: QR onboarding flow — setup completion
  - **Given** a valid setup token exists and no admin user exists
  - **When** the admin scans the QR code, navigates to the setup URL with the token, fills in username and password, and submits `POST /api/setup/register`
  - **Then** the server creates the first `admin` user, invalidates the setup token, returns HTTP 201, and the client redirects to the login page with a success message

- **S-auth-015**: QR onboarding — token invalidated after use
  - **Given** the first admin account has been created
  - **When** someone accesses `GET /api/setup/token`
  - **Then** the server returns HTTP 403 with `{ error: "already_setup", message: "Admin account already exists" }`

- **S-auth-016**: Token expiry — mid-session
  - **Given** a user has been logged in for more than 24 hours (JWT expired)
  - **When** the client makes any API request
  - **Then** the server returns HTTP 401. The client clears the token from `sessionStorage` and redirects to the login page.

- **S-auth-017**: Token expiry — proactive detection
  - **Given** a user has a JWT whose `exp` claim is in the past
  - **When** the client initialises or before making an API request
  - **Then** the client detects the expiry, clears the token from `sessionStorage`, and redirects to the login page without making the API call

- **S-auth-018**: Deactivated user with still-valid JWT
  - **Given** a user with an active session (valid, non-expired JWT)
  - **When** a manager deactivates their account via `PATCH /api/users/:id/deactivate`
  - **Then** on the user's next API request, the JWT filter checks the `active` flag, finds it `0`, and returns HTTP 403. The client clears the token and redirects to the login page.

- **S-auth-019**: Logout
  - **Given** a logged-in user
  - **When** they click the Logout button
  - **Then** the client clears the JWT from `sessionStorage` and redirects to the login page. No server-side token invalidation occurs.

- **S-auth-020**: Unauthenticated access to protected endpoint
  - **Given** no JWT is stored in `sessionStorage`
  - **When** the user navigates to any protected route or the client makes an API request without an `Authorization` header
  - **Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Authentication required" }`, and the client redirects to the login page

- **S-auth-021**: Expired token returns 401
  - **Given** a user whose JWT has expired (the `exp` claim is in the past)
  - **When** the client makes any API request with the expired token
  - **Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Token has expired" }`, and the client clears the token and redirects to the login page

- **S-auth-022**: Missing token returns 401
  - **Given** no JWT is present in the `Authorization` header
  - **When** the client makes a request to a protected endpoint
  - **Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Authentication required" }`

- **S-auth-023**: Malformed token returns 401
  - **Given** a JWT that is not a valid signed token (corrupted signature, invalid base64, wrong format)
  - **When** the client makes a request with this malformed token in the `Authorization` header
  - **Then** the server returns HTTP 401 with `{ error: "unauthorized", message: "Invalid token" }`

- **S-auth-024**: Deactivated account returns 403 with account_disabled
  - **Given** a user whose `active` flag is `0`
  - **When** they attempt to log in with correct credentials
  - **Then** the server returns HTTP 403 with `{ error: "account_disabled", message: "Account is deactivated. Contact your manager." }`
  - **And** the client shows a distinct "account disabled" error message with contact guidance

## Related Specifications Cross-Cons and other Document.: FianDate:  - The auth specification relates to:
The specauthentication it**: (specs/product/spec.md) - The auth specifications REQ-auth-003 and REQ-auth-004, which enforce role-based access control, directly implement the access controls needed for the product management operations described in the product spec.
- **Stock specification and authorization** (specs/stock/spec.md) - The auth specification defines who can view vs. modify stock through REQ-auth-003 (product/stock endpoint access) and REQ-auth-004 (detailed role permissions), which enforces the stock operation requirements in the stock spec.
- **Scan specification** (specs/scan/spec.md) - Scanning functionality depends on authenticated access to product data. The scan features rely on product lookup (GET /api/products/:barcode) and creation (POST /api/products) endpoints, whose access controls are governed by the auth spec's REQ-auth-003 and REQ-auth-004 requirements.
- **Product Requirements Document** (prd.md) - This auth specification implements the security and authorization requirements from PRD Section 9 (Authorization & Access Control Feature), covering all acceptance criteria from AC-15 through AC-30.
- **Design Document** (design.md) - The architectural decisions in this spec (JWT-based authentication, handler-level role-based access control, API security patterns) align with and implement the security architecture outlined in the design document.

---
