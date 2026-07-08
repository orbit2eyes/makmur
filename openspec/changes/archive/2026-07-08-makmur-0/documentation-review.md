# Documentation Review: makmur-0

Generated: 2026-07-09
Previous review: 2026-07-08 (stale — several flagged gaps resolved since)

---

## Docs Inventory

### Project Root

| File | Purpose | Current Status |
|------|---------|----------------|
| `README.md` | Human onboarding — tech stack, install, project structure, API endpoints, usage | **Updated** — auth flow, JWT, roles, QR onboarding, login, API table complete. No stale default-cred references. |
| `AGENTS.md` | AI assistant operational guidance — architecture, conventions, pitfalls | **Updated** — comprehensive. Component lists, code map, conventions, common pitfalls. |
| `CHANGELOG.md` | Release history — Keep a Changelog format | **Present** — [Unreleased] section with all auth-phase changes, known gaps documented. |
| `LICENSE` | MIT license file | **Present** — 24-line MIT license. |

### SDD Artifacts (openspec/changes/makmur-0/)

| File | Purpose | Current Status |
|------|---------|----------------|
| `prd.md` | Full PRD — v1 scope (Sections 1-8) + auth section (Section 9) | Current — journeys A-J, 30 acceptance criteria, risks, open questions |
| `proposal.md` | Change proposal — summary, affected domains, approach, trade-offs | Current — includes both v1 launch and auth feature proposals |
| `design.md` | Architecture, API contracts, data model, component tree, auth design | Current — ASCII diagrams, full API request/response shapes, data flows, key decisions |
| `tasks.md` | Phased implementation tasks | **Partially stale** — Auth phase tasks (T-auth-01 to T-auth-25) correct. Phase 1-6 reference Node.js/Express stack (superseded by Java/Spring Boot). |
| `specs/auth/spec.md` | Auth domain — 14 requirements, data model, 24 scenarios | Current |
| `specs/product/spec.md` | Product domain — 5 requirements, 6 scenarios | Current |
| `specs/scan/spec.md` | Scan domain — 8 requirements, 6 scenarios | Current |
| `specs/stock/spec.md` | Stock domain — 6 requirements, 6 scenarios | Current |
| `testing.md` | Auth testing procedure — unit, integration, manual | Current — comprehensive test matrix, manual test procedures, Postman/Newman integration, performance tests |
| `verify-report.md` | Auth verification results — 61/61 tests, AC coverage | Current — documents AC-16 gap (returns 403 not 401), 5 pre-existing TS errors, all T-auth tasks completed |
| `current-state-context.md` | Code state snapshot at auth phase start | **Stale** — claims tests "NOT STARTED" (T-auth-22) and implementation "in progress." Auth is complete with 61/61 tests passing. |
| `current-state-impact.md` | Implementation gaps, deviations, edge cases | Current — documents 6 implementation gaps (G1-G6), 4 task deviations, 8 edge cases |
| `business-logic.md` | Design rationale — why decisions were made | **Stale (minor)** — "Why only admin needs auth initially" section reads as if auth is still pending. Has a historical note marker but could be clearer. |
| `user-stories.md` | UX stories by role and priority — 50 stories across domains | Current — auth (10), scanning (9), products (5), stock (7), user mgmt (12), feedback (2), role/nav (5) |
| `ISSUES.md` | Issue tracker | Not reviewed |
| `analysis.md` | Analysis artifact | Not reviewed |
| `apply-progress.md` | Implementation progress tracker | Not reviewed |
| `documentation-review.md` | THIS FILE — documentation coverage audit | Updated 2026-07-09 |
| `AGENTS.md` (inside change dir) | AI operational guidance copy inside change artifacts | Current — comprehensive. Serves as reference material loaded by agent tooling. |

---

## Covered Areas (well-documented)

### Spec layer — strong

All 4 domain specs (auth, product, scan, stock) have a consistent format: requirements with priority/rationale/AC, data model tables, scenarios covering success + error paths. Each spec cross-references related specs, PRD, and design doc. This is the strongest documentation layer in the project.

**Auth spec** is the most detailed: 14 requirements (P0-P1), 24 scenarios covering every auth path (login valid/invalid/deactivated, scope enforcement for all roles, QR flow, token expiry, logout). Error format table with HTTP status → error code mapping.

### Design layer — thorough

Design doc covers: ASCII architecture diagrams, full API contracts with every request/response shape, SQLite schema with field-level notes, component tree, data flow diagrams (scan, stock, search). Auth design section adds separate architecture diagram, module map with 30+ files, endpoint contracts, schema migration, flow diagrams. Key design decisions with rationale for each.

### Verification layer — honest

Verify report documents 61/61 test results, maps every AC to verification evidence, flags known gaps (AC-16 returns 403 not 401) with root cause and fix. Testing.md covers unit test inventory, integration matrix, manual procedures (7 categories, 20+ tests), error format compliance, performance/scenarios. This layer does not conceal imperfections.

### Business logic layer — exceptional

Business-logic.md captures the reasoning behind every significant decision: why scanning over keyboard, why webapp over native, why local network over cloud, why 3 roles, why handler-level scope, why manager can't see products, why JWT over sessions, why QR setup, why absolute+delta stock, why DB-level CHECK, why page refresh for sync. Each decision explains alternatives considered and why they were rejected. This is the most valuable doc for an incoming developer.

---

## Gaps (unresolved)

### G1: tasks.md Phase 1-6 reference Node.js/Express (HIGH)

Phase 1-6 task descriptions (T-1-01 through T-6-06) list:
- `client/package.json`, `server/package.json`, `server/index.js`, `server/routes/products.js`, `server/db.js`
- `server/middleware/validate.js`

These files do not exist. The actual server is Java/Spring Boot (`pom.xml`, `Application.java`, `ProductRepository.java`, etc.). Auth phase tasks correctly reference Java files.

**Impact**: Anyone following the task list sequentially would build a Node.js backend that conflicts with the existing Java codebase. The original phases were written before the Java decision (design.md Section 6) and were never updated.

**Recommendation**: Mark Phase 1-6 tasks as `SUPERSEDED` with a note pointing to the Java implementation. Auth phase tasks (already correctly Java-referenced) cover the remaining work.

### G2: current-state-context.md test status stale (MEDIUM)

Doc says under "Remaining Work":
- T-auth-22: Auth integration tests — "NOT STARTED"
- All PRD acceptance criteria — "PARTIAL"

Verify report confirms 61/61 tests pass and 14/15 AC covered. Context doc was written before testing completed and was never updated.

**Recommendation**: Update "Remaining Work" table to reflect current state (tests completed, AC-16 gap documented).

### G3: AC-16 implementation returns 403 not 401 (MEDIUM)

From verify-report and current-state-impact.md (G1):
- **Spec says**: Missing JWT returns 401 Unauthorized
- **Code does**: Returns 403 Forbidden

Root cause: no `AuthenticationEntryPoint` configured in `SecurityConfig`. Spring Security's `ExceptionTranslationFilter` delegates to default handler which returns 403 instead of 401.

**Impact**: Affects AC-16 and AC-17 compliance. Security intent (blocking unauthenticated access) is achieved, but HTTP status code contradicts spec.

**Recommendation**: Add `AuthenticationEntryPoint` returning 401 in `SecurityConfig.java`. Fix documented in verify-report.md with code snippet.

### G4: QR code uses external API, not server endpoint (LOW)

Current-state-impact.md (G2):
- **Design says**: Server serves `GET /api/setup/qr` returning `image/png`
- **Code does**: Client uses `api.qrserver.com` external service to generate QR

**Recommendation**: Either implement `GET /api/setup/qr` server endpoint (adding zxing dependency to server) or update design.md to document external API as intentional. The external API works but introduces a network dependency during setup.

### G5: Password min-length mismatch between client and server (LOW)

Current-state-impact.md (G3):
- **Client enforces**: 6 characters
- **Server enforces**: 8 characters

**Impact**: A user who meets client validation (7 chars) will get a server-side error. Surface-level UX gap.

**Recommendation**: Align client validation to 8 characters to match server.

### G6: Reset password has no frontend trigger (LOW)

Current-state-impact.md (G4):
- **API endpoint**: `PATCH /api/users/:id/reset-password` exists and works
- **Frontend**: No reset-password button in `UserList` component

**Recommendation**: Add a reset-password button/action to `UserList.tsx` that opens a password input prompt and calls the API.

### G7: zxing-js loads eagerly instead of lazy (LOW)

Current-state-impact.md (G6):
- **Design says**: Lazy-loaded only on non-Chrome browsers
- **Code does**: Module-level `import` loads on all browsers

**Impact**: Adds ~100KB to bundle on Chrome where native `BarcodeDetector` would suffice.

**Recommendation**: Use dynamic `import()` for zxing-js, loaded only when `'BarcodeDetector' in window` is false.

### G8: No .rpiv/guidance/ directory (LOW)

The project root has no `.rpiv/guidance/` shadow tree. AGENTS.md lives at project root, which is fine for most tooling, but the guidance system pattern (`.rpiv/guidance/architecture.md`) is not set up.

**Recommendation**: Optional — create if agent tooling requires it. Current AGENTS.md at root covers all guidance needs.

---

## Resolved Since Previous Review (2026-07-08)

The following gaps from the previous review are now resolved:

| Gap (Previous Review) | Status | Evidence |
|-----------------------|--------|----------|
| README.md stale (G1) | **RESOLVED** | README updated with auth flow, JWT, QR onboarding, roles, complete API table, no default-cred references |
| AGENTS.md missing from root (G7) | **RESOLVED** | `AGENTS.md` exists at project root with comprehensive architecture, conventions, pitfalls |
| CHANGELOG.md missing (G9) | **RESOLVED** | `CHANGELOG.md` exists with [Unreleased] section covering all auth changes |
| LICENSE file missing (G8) | **RESOLVED** | `LICENSE` file exists (MIT) |
| AGENTS.md code map errors (G5) | **RESOLVED** | `AGENTS.md` code map is accurate, no "This file" copy-paste errors |

---

## Recommendations

### Immediate (next working session)

| Priority | Action | Files |
|----------|--------|-------|
| P1 | Add `AuthenticationEntryPoint` returning 401 in SecurityConfig | `server/.../config/SecurityConfig.java` |
| P2 | Update tasks.md Phase 1-6: mark `SUPERSEDED`, point to Java stack | `tasks.md` |
| P3 | Update current-state-context.md test status | `current-state-context.md` |
| P4 | Align client password min-length to 8 | `client/.../SetupPage.tsx`, `CreateUserForm.tsx` |
| P5 | Add reset-password button to UserList | `client/.../UserList.tsx` |

### Short-term (next few sessions)

| Priority | Action | Files |
|----------|--------|-------|
| P6 | Either implement GET /api/setup/qr or update design.md | `design.md` or new server endpoint |
| P7 | Lazy-load zxing-js polyfill | `client/.../BarcodeDecoder.tsx` |

### Nice-to-have

| Priority | Action | Files |
|----------|--------|-------|
| P8 | Create .rpiv/guidance/architecture.md from AGENTS.md | New directory + file |
| P9 | Update business-logic.md historical section phrasing | `business-logic.md` |

---

## Documentation Health Summary

| Layer | Files | Verdict |
|-------|-------|---------|
| PRD (product requirements) | `prd.md` | Current |
| Proposal (approach) | `proposal.md` | Current |
| Design (architecture, API, data model) | `design.md` | Current |
| Domain specs (auth, product, scan, stock) | 4 files in `specs/` | Current |
| Tasks (implementation plan) | `tasks.md` | **Partially stale** — Phase 1-6 reference wrong stack |
| Testing (procedures) | `testing.md` | Current |
| Verify (results, gaps) | `verify-report.md`, `current-state-impact.md` | Current |
| Context (state snapshots) | `current-state-context.md` | **Stale** — test status |
| Rationale (design decisions) | `business-logic.md` | Current (minor historical note aging) |
| Human onboarding | `README.md` | **Updated** — current |
| Agent guidance | `AGENTS.md` (root) | **Updated** — comprehensive |
| Release history | `CHANGELOG.md` | **Present** — has unreleased section |
| License | `LICENSE` | **Present** — MIT |

**18 of 21 documentation files are current.** The 3 stale files are:
1. `tasks.md` — Phase 1-6 stack reference (High impact, low effort to fix)
2. `current-state-context.md` — test status (Medium impact, low effort)
3. `business-logic.md` — minor historical phrasing (Low impact, trivial fix)

The documentation is in **good health overall.** The spec layer (PRD, design, 4 domain specs) is thorough and accurate. The verification layer documents known gaps honestly. The project-root docs (README, AGENTS, CHANGELOG, LICENSE) are all present and updated. The remaining stale items are in support/artifact docs that don't affect day-to-day development.

---

## AC Coverage vs Documentation

| AC | Documented In | Status |
|----|--------------|--------|
| AC-01 (browser access) | PRD, design | Documented |
| AC-02 (camera permission) | PRD, scan spec, design | Documented |
| AC-03 (decode within 3s) | PRD, scan spec, design | Documented |
| AC-04 (known barcode → detail) | PRD, scan spec, design | Documented |
| AC-05 (unknown barcode → create) | PRD, scan spec, design | Documented |
| AC-06 (product validation) | PRD, product spec, design | Documented |
| AC-07 (product persistence) | PRD, product spec, design | Documented |
| AC-08 (stock persists) | PRD, stock spec, design | Documented |
| AC-09 (stock >= 0) | PRD, stock spec, design | Documented |
| AC-10 (search < 500ms) | PRD, product spec, design | Documented |
| AC-11 (list renders 5000) | PRD, product spec, design | Documented |
| AC-12 (responsive 375/1280) | PRD, design | Documented |
| AC-13 (scan round-trip < 5s) | PRD, scan spec, design | Documented |
| AC-14 (cross-device data) | PRD, stock spec, design | Documented |
| AC-15 (login JWT) | PRD §9, auth spec, design | Documented |
| AC-16 (no JWT → 401) | PRD §9, auth spec, design | **Gap**: code returns 403 |
| AC-17 (expired JWT → 401) | PRD §9, auth spec, design | **Gap**: code returns 403 |
| AC-18 (staff scope) | PRD §9, auth spec, design | Documented |
| AC-19 (manager scope) | PRD §9, auth spec, design | Documented |
| AC-20 (admin scope) | PRD §9, auth spec, design | Documented |
| AC-21 (handler-level scope) | PRD §9, auth spec, design | Documented |
| AC-22 (QR onboarding) | PRD §9, auth spec, design | Documented |
| AC-23 (token invalidation) | PRD §9, auth spec, design | Documented |
| AC-24 (bcrypt storage) | PRD §9, auth spec, design | Documented |
| AC-25 (JWT claims) | PRD §9, auth spec, design | Documented |
| AC-26 (schema migration) | PRD §9, auth spec, design | Documented |
| AC-27 (expiry/logout redirect) | PRD §9, auth spec, design | Documented |
| AC-28 (deactivated → 403) | PRD §9, auth spec, design | Documented |
| AC-29 (login as default) | PRD §9, auth spec, design | Documented |
| AC-30 (standardized errors) | PRD §9, auth spec, design | Documented |

28/30 acceptance criteria fully documented in spec + design + code. AC-16 and AC-17 have a known implementation gap (403 instead of 401) that is documented in verify-report.md.
