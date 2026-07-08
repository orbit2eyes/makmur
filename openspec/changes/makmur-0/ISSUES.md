# Issues — Makmur v1 (makmur-0)

> Sync target: GitHub Issues (repo: orbit2eyes/makmur)
> Auto-synced bidirectionally: GitHub Issues <-> ISSUES.md


## Open Issues

### #4: I-004: No refresh token mechanism

- **Labels**: enhancement, priority-p2, auth
- **Created**: 2026-07-08T13:20:45Z
- **Updated**: 2026-07-08T14:19:35Z

**Priority**: P2 | **Severity**: low | **Domain**: auth

Single 24h JWT token with no refresh flow. Staff mid-scanning when token expires will lose their action and be redirected to login. Not critical for v1.

**Resolution**: Deferred. Add refresh token in v2 if mid-shift expirations become a problem.

---

## Closed / Resolved Issues

### #18: No CHANGELOG.md for release tracking

- **Status**: resolved
- **Closed**: 2026-07-08T13:59:53Z
- **Labels**: enhancement, priority-p3
- **Created**: 2026-07-08T13:34:09Z

**Priority**: P3 | **Source**: documentation-review.md (G9, P7)

No release history document. Useful once the project has tagged releases.

**Fix**: Create CHANGELOG.md in Keep a Changelog format with [Unreleased] section listing auth feature and known gaps.

---

### #17: No LICENSE file (README says MIT)

- **Status**: resolved
- **Closed**: 2026-07-08T13:59:50Z
- **Labels**: priority-p3, docs
- **Created**: 2026-07-08T13:34:07Z

**Priority**: P3 | **Source**: documentation-review.md (G8, P8)

README.md footer says "MIT" but no LICENSE file exists at project root. Legal gap.

**Fix**: Create LICENSE file matching MIT license text.

---

### #16: No root-level AGENTS.md for AI assistants

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:43Z
- **Labels**: priority-p2, docs
- **Created**: 2026-07-08T13:34:06Z

**Priority**: P2 | **Source**: documentation-review.md (G7, P5)

AGENTS.md exists only inside openspec/changes/makmur-0/ (SDD change directory). No root-level AGENTS.md for AI assistants landing on the project root. Creates context gap for first interaction.

**Fix**: Create root AGENTS.md with project overview, tech stack, key conventions, and pointers to detailed docs in openspec/.

---

### #15: business-logic.md auth section reads as present-tense (auth is done)

- **Status**: resolved
- **Closed**: 2026-07-08T14:18:16Z
- **Labels**: priority-p3, docs
- **Created**: 2026-07-08T13:34:04Z

**Priority**: P3 | **Source**: documentation-review.md (G4, P9)

Section "Why only admin needs auth initially" describes auth as pending/upcoming feature. Auth is fully implemented. The section is now historical.

**Fix**: Mark as "Historical context — auth was added after v1" or update to reflect current state.

---

### #14: current-state-context.md test status outdated

- **Status**: resolved
- **Closed**: 2026-07-08T13:59:47Z
- **Labels**: priority-p3, docs
- **Created**: 2026-07-08T13:34:02Z

**Priority**: P3 | **Source**: documentation-review.md (G3, P6)

current-state-context.md says T-auth-22 "NOT STARTED" and PRD criteria "PARTIAL". verify-report.md confirms 61/61 tests pass across all domains.

**Fix**: Update test status section to reflect current state.

---

### #13: AGENTS.md code map has typos and missing entries

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:40Z
- **Labels**: priority-p2, docs
- **Created**: 2026-07-08T13:34:00Z

**Priority**: P2 | **Source**: documentation-review.md (G5, P5)

AGENTS.md inside openspec/changes/makmur-0/ has copy-paste errors in file tree and missing entries:

- "user-stories.md" listed as "This file" in two places
- Missing client components: Dashboard, ProtectedRoute, ScanResult, BarcodeDecoder
- Missing server files: SchemaMigration, SetupTokenStore, GlobalExceptionHandler, ForbiddenException, DataSourceConfig
- Missing artifacts: apply-progress, analysis, ISSUES, documentation-review

**Fix**: Clean up file tree, add all listed components, remove copy-paste errors.

---

### #12: tasks.md Phase 1-6 reference Node.js/Express (should be Java/Spring Boot)

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:38Z
- **Labels**: priority-p1, docs
- **Created**: 2026-07-08T13:33:57Z

**Priority**: P1 | **Source**: documentation-review.md (G2, P2)

Original 6 task phases reference Node.js files (package.json, index.js, routes/products.js). Actual backend is Java/Spring Boot. Auth phase tasks (T-auth-*) are correct. Anyone following Phase 1-6 literally would build a Node.js backend that conflicts with existing Java code.

**Fix**: Mark Phase 1-6 as COMPLETED/SUPERSEDED or rewrite references to Java paths (server/src/main/java/...).

---

### #11: README.md significantly outdated — wrong creds, auth claim, file tree

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:36Z
- **Labels**: priority-p1, docs
- **Created**: 2026-07-08T13:33:42Z

**Priority**: P1 | **Source**: documentation-review.md (G1, P1)

Root README.md has multiple critical errors:

1. Lists default admin/admin123 credentials that don't exist (removed in auth phase)
2. Says "No authentication is required" — auth is now mandatory
3. Missing all auth files from project structure tree
4. Missing auth endpoints from API table
5. No mention of login flow, roles, or user management
6. "Status" line says "Production-ready — v1 complete" (auth was added after)

**Fix**: Rewrite README to reflect current state. Remove fake creds, update auth claim, add auth files to tree, add login/setup endpoints to API table.

---

### #10: No Cache-Control: no-cache on index.html

- **Status**: resolved
- **Closed**: 2026-07-08T13:59:45Z
- **Labels**: bug, frontend, priority-p3
- **Created**: 2026-07-08T13:33:28Z

**Priority**: P3 | **Source**: current-state-impact.md (G5)

No Cache-Control header on index.html. Browser caches old app version after deploy.

**Fix**: Add Cache-Control: no-cache to Vite config or server WebConfig.

---

### #9: No server-side QR endpoint — uses external API

- **Status**: resolved
- **Closed**: 2026-07-08T13:59:43Z
- **Labels**: enhancement, auth, priority-p3
- **Created**: 2026-07-08T13:33:26Z

**Priority**: P3 | **Source**: current-state-impact.md (G2)

SetupPage renders QR using api.qrserver.com (external HTTP service). No offline coverage if network restricts external. Design says server should serve QR.

**Fix**: Add GET /api/setup/qr server endpoint or document external API as accepted design.

---

### #8: zxing-js polyfill eagerly imported (not lazy-loaded)

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:33Z
- **Labels**: enhancement, priority-p2, frontend
- **Created**: 2026-07-08T13:33:02Z

**Priority**: P2 | **Source**: current-state-impact.md (G6)

zxing-js (1.2MB) loads via module-level import on ALL browsers. Spec says lazy-load only for Safari/Firefox where native BarcodeDetector is unavailable. Chrome users pay unnecessary bundle cost.

**Fix**: Dynamic import zxing-js only when BarcodeDetector is not available.

---

### #7: Reset password button missing from UserList frontend

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:31Z
- **Labels**: bug, priority-p2, frontend
- **Created**: 2026-07-08T13:33:00Z

**Priority**: P2 | **Source**: current-state-impact.md (G4)

PATCH /api/users/:id/reset-password endpoint exists on server. UserList.tsx has no button or UI to trigger it. Manager/admin cannot reset staff passwords via UI.

**Fix**: Add "Reset Password" button per user row in UserList with confirmation dialog.

---

### #6: Password min length mismatch — client enforces 6, server enforces 8

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:29Z
- **Labels**: bug, priority-p2, auth
- **Created**: 2026-07-08T13:32:48Z

**Priority**: P2 | **Source**: current-state-impact.md (G3)

Client Login.tsx enforces 6-char minimum. Server AuthController enforces 8-char minimum. Users can create a 6-7 char password that passes client validation but fails on server.

**Fix**: Align client minimum length to 8 in Login.tsx and CreateUserForm.tsx.

---

### #5: I-005: Camera permission denied — no graceful fallback

- **Status**: resolved
- **Closed**: 2026-07-08T14:18:16Z
- **Labels**: bug, priority-p1, scan
- **Created**: 2026-07-08T13:20:47Z

**Priority**: P1 | **Severity**: medium | **Domain**: scan

If a user denies camera permission on first access, app shows browser-level error instead of a friendly message with manual barcode entry fallback.

**Acceptance**: Denying camera permission shows in-app message with instructions + manual barcode entry input.

---

### #3: I-003: No JWT_SECRET env var guard at production startup

- **Status**: resolved
- **Closed**: 2026-07-08T14:18:16Z
- **Labels**: bug, priority-p1, auth
- **Created**: 2026-07-08T13:20:43Z

**Priority**: P1 | **Severity**: medium | **Domain**: auth

JwtUtil reads JWT_SECRET from env and validates minimum 32 bytes at startup. If unset, server fails to start. No documentation or startup script for setting this in production.

**Acceptance**: Server startup script documents setting JWT_SECRET. Dev mode generates a default warning.

---

### #2: I-002: Pre-existing TypeScript errors in frontend

- **Status**: resolved
- **Closed**: 2026-07-08T14:18:16Z
- **Labels**: bug, priority-p2, frontend
- **Created**: 2026-07-08T13:20:41Z

**Priority**: P2 | **Severity**: minor | **Domain**: frontend

5 pre-existing TS errors in client/src/ (Viewfinder callback type mismatch, test file missing dependency). Not auth-related, predate this change.

**Affected files**: `client/src/components/Viewfinder.tsx`, test config files.

---

### #1: I-001: Unauthenticated requests return 403 instead of 401

- **Status**: resolved
- **Closed**: 2026-07-08T13:52:26Z
- **Labels**: bug, priority-p1, auth
- **Created**: 2026-07-08T13:20:29Z

**Priority**: P1 | **Severity**: minor | **Domain**: auth

Spring Security default returns `403 Forbidden` when no JWT is provided. Spec requires `401 Unauthorized`. Fix: add `AuthenticationEntryPoint` in `SecurityConfig`.

**Acceptance**: Unauthenticated request to any protected endpoint returns `{"error":"unauthorized","message":"..."}` with HTTP 401.

---

