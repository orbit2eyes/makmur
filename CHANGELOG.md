# Changelog

All notable changes to the Makmur project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **JWT authentication** — login endpoint (`POST /api/auth/login`) issues 24h Bearer JWTs with `sub`, `role`, `iat`, `exp` claims. `JwtUtil` reads `JWT_SECRET` env var (min 32 bytes, required at startup). Active-flag check on every authenticated request via `JwtAuthenticationFilter`.
- **Role-based access control** — three roles (`admin`, `manager`, `staff`) enforced at handler level via `AuthService.requireRole()`. Admin gets full access, manager manages staff users only, staff accesses products/scan only. No role-based routing — same endpoints serve all roles.
- **QR onboarding flow** — first-time admin setup via `/api/setup/status`, `/api/setup/token`, `/api/setup/register`. Single-use token with 60-minute TTL, bcrypt password hashing, token invalidation after successful registration. Default admin seed removed — QR-only onboarding.
- **User management** — `GET /api/users` (role-scoped list: admin sees all, manager sees staff only), `POST /api/users` (scope-enforced creation), `PATCH /api/users/:id/deactivate`, `PATCH /api/users/:id/reactivate`, `PATCH /api/users/:id/reset-password`. Manager-created users hard-coded to `staff` role.
- **Scope enforcement on existing endpoints** — `ProductController` requires `admin` or `staff` role (manager gets 403). `UserController` requires `admin` or `manager` role (staff gets 403).
- **Standardized error responses** — all errors return `{ error: "<snake_case_code>", message: "<human readable>" }` via `GlobalExceptionHandler`. ForbiddenException returns 403, validation errors return 422 with field-level details.
- **Bcrypt password storage** — `BCryptPasswordEncoder` (strength 10) for all password hashing. `$2a$` prefix verified in tests. Schema migration handles `password` → `password_hash` column rename for existing databases.
- **Frontend auth integration** — `AuthContext` stores JWT + user in `sessionStorage`, decodes `exp` claim for proactive expiry detection. `api.ts` wraps `fetch` with `Authorization: Bearer <token>` and 401 redirect. Role-based sidebar navigation. Setup page with QR display + form modes. Login form with `account_disabled` error handling.
- **Comprehensive test suite** — 61 tests (14 original + 47 new) across `AuthServiceTest`, `AuthControllerTest`, `UserControllerTest`, `SetupControllerTest`, and scope tests in `ProductControllerTest`. Covers login (valid/invalid/deactivated), registration (role enforcement, duplicate), user management (list scoping, create role-hardcode, deactivate scoping), setup flow (token gen, register, invalidation), and scope enforcement for all three roles.

### Changed

- **User schema** — added `password_hash`, `active` flag, extended `role CHECK` to include `'manager'`. Schema migration recreates table on old databases.

### Fixed

- **AC-16 gap documented** — unauthenticated requests return 403 instead of 401. Root cause: missing `AuthenticationEntryPoint` in `SecurityConfig`. Security intent (blocking unauthenticated access) is achieved; fix requires small config change. 14/15 acceptance criteria covered.
- **Password min-length mismatch documented** — client enforces 6 chars, server enforces 8 chars. Surface-level UX gap flagged.
- **Reset password UI documented** — API endpoint exists (`PATCH /api/users/:id/reset-password`) but no frontend trigger in `UserList` component.
