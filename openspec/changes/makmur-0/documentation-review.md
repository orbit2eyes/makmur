# Documentation Review: makmur-0

Generated: 2026-07-08
Status: Feature implementation complete (auth phase done, 61/61 tests passing)

---

## Docs Inventory

### SDD Artifacts (openspec/changes/makmur-0/)

| File | Purpose | Status |
|------|---------|--------|
| `prd.md` | Full PRD — v1 scope + auth section (Section 9) | Up-to-date |
| `proposal.md` | Change proposal — summary, affected domains, approach | Up-to-date |
| `design.md` | Architecture, API contracts, data model, component tree, auth design | Up-to-date |
| `tasks.md` | Phased tasks — Phase 1-6 + Auth phase (T-auth-01 to T-auth-25) | **Stale** — Phase 1-6 reference Node.js/Express, actual server is Java/Spring Boot |
| `specs/auth/spec.md` | Auth domain spec — requirements, data model, scenarios | Up-to-date |
| `specs/product/spec.md` | Product domain spec | Up-to-date |
| `specs/scan/spec.md` | Scan domain spec | Up-to-date |
| `specs/stock/spec.md` | Stock domain spec | Up-to-date |
| `user-stories.md` | UX stories by role and priority | Up-to-date |
| `business-logic.md` | Design rationale — why decisions were made | **Stale** — Section "Why only admin needs auth initially" describes pre-auth state as current. Auth is implemented. |
| `testing.md` | Auth testing procedure — unit, integration, manual | Up-to-date |
| `verify-report.md` | Auth verification results — 61/61 tests, AC coverage, findings | Up-to-date (documents known gaps) |
| `current-state-context.md` | Code state snapshot at auth phase start | **Stale** — Says tests NOT STARTED (T-auth-22). Verify-report confirms 61/61 pass. |
| `current-state-impact.md` | Implementation gaps, deviations, edge cases | Up-to-date (comprehensive gap log) |
| `AGENTS.md` | AI assistant operational guidance | **Slightly stale** — code map missing several files, file tree inaccuracies |
| `analysis.md` | Analysis artifact | Not reviewed |
| `apply-progress.md` | Implementation progress tracker | Not reviewed |
| `ISSUES.md` | Issue tracker | Not reviewed |

### Project Root

| File | Purpose | Status |
|------|---------|--------|
| `README.md` | Human onboarding — tech stack, install, usage | **Stale** — References default admin creds (removed), says "No authentication", missing auth components in file tree, says "Production-ready v1 complete" (auth was after v1) |

### Missing

| File | Status |
|------|--------|
| `AGENTS.md` (project root) | Missing |
| `LICENSE` | Missing |
| `CHANGELOG.md` | Missing |

---

## Covered Areas

### What's documented well

**PRD (prd.md)** — Complete. Product requirements, user journeys (A-E), acceptance criteria (AC-01 to AC-14), risks, open questions, plus full auth section (Section 9) with separate journeys (F-J), acceptance criteria (AC-15 to AC-30), error format spec, security properties.

**Design (design.md)** — Thorough. ASCII architecture diagram, component tree, full API contracts with request/response shapes for every endpoint, SQLite schema, data flow diagrams (scan, stock update, search), key design decisions with rationale. Auth design section adds auth architecture diagram, module map, endpoint contracts, schema changes, flow diagrams, scope enforcement pattern.

**Specs (specs/)** — All 4 domains have clean spec docs with requirements, data models, and scenarios. Consistent format.

**Testing (testing.md)** — Comprehensive. Unit test inventory, integration test matrix (every endpoint x every role), manual test procedures (7 categories), error format compliance tests, AC coverage table.

**Verify report (verify-report.md)** — Complete. 61/61 test results, AC coverage (14/15 covered, 1 gap documented), task completion status, findings, warnings.

**User stories (user-stories.md)** — 42 stories across all domains with priority levels. Role-specific. Good acceptance criteria per story.

**Business logic (business-logic.md)** — Exceptional rationale doc. Covers: why barcode scanning as primary input, why webapp vs native, why local network, why 3 roles, why scope-based vs route-based, why manager can't see products, why JWT vs sessions, why QR setup, why absolute+delta stock, why CHECK at DB level, why page refresh for sync, why standardized errors, why 401/403 separation, why WAL mode, why state-based routing, why no delete.

**Impact analysis (current-state-impact.md)** — Detailed. Code paths per request flow, data model, test coverage, 6 implementation gaps (G1-G6), 4 task deviations (D1-D4), 6 observations (O1-O6), 8 edge case constraints (E1-E8), 7 unknowns (U1-U7), dependency graph.

---

## Gaps Found

### G1: README.md is significantly stale (HIGH priority)

Root-level onboarding doc has not been updated after auth phase:

- **"Default Credentials" section** still lists `admin` / `admin123`. Default admin seed was removed in auth phase (T-auth-23). Admin creation now flows through QR onboarding only. This section is actively misleading.
- **"No authentication is required"** in Network Access section. Auth is now mandatory — login page is the default redirect.
- **Project structure** missing auth-related files: `AuthContext.tsx`, `Login.tsx`, `SetupPage.tsx`, `Sidebar.tsx`, `UserList.tsx`, `CreateUserForm.tsx`, `ProtectedRoute.tsx`, `Dashboard.tsx`, `ScanResult.tsx`, `CreatePrompt.tsx`, `BarcodeDecoder.tsx`. Server side missing: `JwtUtil.java`, `JwtAuthenticationFilter.java`, `SecurityConfig.java`, `AuthController.java`, `UserController.java`, `SetupController.java`, `User.java`, `UserRepository.java`, `AuthService.java`, `SchemaMigration.java`, `SetupTokenStore.java`, `GlobalExceptionHandler.java`, `ForbiddenException.java`, `DataSourceConfig.java`.
- **API Endpoints table** missing auth endpoints: `POST /api/auth/login`, `GET /api/setup/*`, `POST /api/setup/register`, `GET /api/users`, user management PATCH endpoints.
- **"Usage" section** mentions scanning and stock but no mention of login, roles, or user management.
- **"Status" line** says "Production-ready — v1 complete." Auth phase was completed after this was written.
- **"License" section** says MIT but no LICENSE file exists.

### G2: tasks.md has stale technology stack for Phase 1-6 (HIGH priority)

Phase 1-6 task descriptions reference Node.js/Express backend:

- Phase 1: `server/package.json`, `server/index.js`, `server/db.js`, `server/routes/products.js`
- Phase 2: `server/middleware/validate.js`, `server/routes/products.js`, `server/routes/stock.js`
- Phase 3-6: Same Node.js file references throughout

Auth phase tasks (T-auth-01 to T-auth-25) correctly reference Java/Spring Boot files.

**Impact**: Anyone following the task list literally would build a Node.js backend that conflicts with the existing Java codebase. The original phases were written before the Java/Spring Boot decision was made (see design.md §6 "Spring Boot over Express") and were never updated.

### G3: current-state-context.md stale on test status (MEDIUM)

Section "Remaining Work" table lists:
- T-auth-22: Auth integration tests — "NOT STARTED"
- All PRD acceptance criteria — "PARTIAL"

verify-report.md confirms 61/61 tests pass (14 original + 47 new). Context doc was written before testing completed.

### G4: business-logic.md section is now historical (LOW)

Section "Why only admin needs auth initially (pre-auth v1)" describes:
- "The original v1 (before the auth feature was added) had no authentication"
- "Now that the core is stable, auth is being added as a proper feature"

This accurately describes the project history but reads as if auth is still pending. Auth is now implemented. The section should be marked as historical context, not current state.

### G5: AGENTS.md code map slightly outdated (LOW)

Inside `openspec/changes/makmur-0/AGENTS.md`:

- **Code map** lists `user-stories.md` as "This file" in two places (copy-paste error from template).
- Missing files from client components: `Dashboard.tsx`, `ProtectedRoute.tsx`, `ScanResult.tsx`, `BarcodeDecoder.tsx` (mentioned in prose but not in code map tree).
- Missing server files: `SchemaMigration.java`, `SetupTokenStore.java`, `GlobalExceptionHandler.java`, `ForbiddenException.java`, `DataSourceConfig.java`.
- Missing artifacts from directory listing: `apply-progress.md`, `analysis.md`, `ISSUES.md`, `documentation-review.md`.

### G6: Implementation gaps documented but not resolved (MEDIUM)

From current-state-impact.md (G1-G6) — these are doc-vs-code deviations that affect doc accuracy:

| Gap | What doc says | What code does |
|-----|---------------|----------------|
| G1: AC-16 returns 403 not 401 | PRD/design: 401 Unauthorized | 403 Forbidden (no AuthenticationEntryPoint) |
| G2: QR code uses external API | Design: server serves `GET /api/setup/qr` PNG | Client uses `api.qrserver.com` external service |
| G3: Password min length mismatch | Auth spec: 8 chars | Client enforces 6, server enforces 8 |
| G4: Reset password UI missing | T-auth-20: reset password button in UserList | API endpoint exists, no frontend trigger |
| G6: zxing-js eager import | Design: lazy import for non-Chrome browsers | Module-level `import` loads on all browsers |

### G7: No project-root AGENTS.md (MEDIUM)

AGENTS.md exists only at `openspec/changes/makmur-0/AGENTS.md` (inside the change artifact directory). No root-level AGENTS.md for AI assistants who land on the repo. The guidance system reference material (`rpiv-guidance`) loaded the one from inside the change directory, which is an unusual location.

### G8: No LICENSE file (LOW)

README.md footer says "MIT" but no LICENSE file exists at project root.

### G9: No CHANGELOG.md (LOW)

No release history document. Useful once the project has tagged releases.

---

## Recommendations

Ordered by priority.

### P1 — Fix README.md

The root onboarding doc is the most visible surface. It must reflect current state:

- Remove "Default Credentials" table. Replace with: "First-time setup: open the app, scan the QR code to create the admin account."
- Remove "No authentication is required" from Network Access. Replace with: "All access requires login. See Auth section."
- Update project structure tree to include all auth-related files (both client and server).
- Add auth endpoints to API table: `POST /api/auth/login`, user management, setup endpoints.
- Update usage section to include login flow, role behavior.
- Update "Status" line to reflect auth completion.
- Add `JWT_SECRET` to prerequisites or environment section.

### P2 — Fix tasks.md Phase 1-6 to match Java/Spring Boot

Two options:

**Option A (recommended):** Since the feature is already implemented, mark Phase 1-6 tasks as `COMPLETED` (or `SUPERSEDED`) and reference the Java implementation in a note.

**Option B:** Rewrite Phase 1-6 task descriptions to reference Java files (`server/src/main/java/...`, `pom.xml`, `schema.sql`, etc.).

The auth phase tasks (T-auth-*) are correct — only the original 6 phases need fixing.

### P3 — Fix AC-16 implementation to match spec

Add `AuthenticationEntryPoint` to `SecurityConfig.java` so missing JWTs return 401 instead of 403. This brings the code into compliance with PRD Section 9.6 and AC-16. Already documented in verify-report.md as a known gap.

### P4 — Fix known implementation gaps from docs

- **G2**: Decide whether `GET /api/setup/qr` server endpoint is needed or external QR API is acceptable. If external is fine, update design.md. If not, implement the endpoint.
- **G3**: Align client password min-length to 8 (match server).
- **G4**: Add reset-password button to UserList component (API endpoint exists, just no UI trigger).

### P5 — Update AGENTS.md code map and fix copy-paste errors

- Fix "This file" labels in file tree (two incorrect references).
- Add missing client components: `Dashboard.tsx`, `ProtectedRoute.tsx`, `ScanResult.tsx`, `BarcodeDecoder.tsx`.
- Add missing server files: `SchemaMigration.java`, `SetupTokenStore.java`, `GlobalExceptionHandler.java`, `ForbiddenException.java`, `DataSourceConfig.java`.
- Add missing artifacts: `apply-progress.md`, `analysis.md`, `ISSUES.md`, `documentation-review.md`.

### P6 — Update current-state-context.md test status

Change T-auth-22 status from "NOT STARTED" to "COMPLETED — 61/61 tests passing". Update "Remaining Work" table references.

### P7 — Add CHANGELOG.md

Create root-level CHANGELOG.md following Keep a Changelog format. Start with [Unreleased] section listing the auth feature and known gaps.

### P8 — Add LICENSE file

Create root-level LICENSE file matching the MIT reference in README.md.

### P9 — Mark business-logic.md historical section

Add a note to the "Why only admin needs auth initially" section clarifying that auth is now implemented. Or move that section to an appendix as project history.

---

## Summary

| Area | Files | Verdict |
|------|-------|---------|
| Specs (PRD, design, domain specs) | 6 files | Current — accurately describe the system |
| Verification (testing.md, verify-report.md, impact.md) | 3 files | Current — document known gaps honestly |
| User-facing (README.md, AGENTS.md) | 2 files | Need updates — README is actively misleading |
| Tasks (tasks.md) | 1 file | Phase 1-6 stale (Node.js refs), auth phase current |
| Context (current-state-context.md) | 1 file | Test status stale |
| Rationale (business-logic.md) | 1 file | Auth section reads as current but auth is done |
| Missing | LICENSE, CHANGELOG.md, root AGENTS.md | Should be created |

**Critical fix needed: README.md** — it's the front door and directly contradicts reality (wrong credentials, wrong auth claim, incomplete file tree).

**Secondary fix needed: tasks.md Phase 1-6** — references nonexistent Node.js stack. Blocks anyone from following the task list.
