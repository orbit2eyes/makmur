# Issues — Makmur

> Repository: orbit2eyes/makmur
> Consolidated from all archived changes: makmur-0, fix-scan-camera, fix-change-currency-and-language, fix-input-barcode
> Auto-synced: issues from archive changes are listed below.

---

## Open Issues

### #22: Camera permission pre-check removed — optimization lost

- **Labels**: enhancement, priority-p2, scan
- **Created**: 2026-07-09
- **Domain**: scan
- **Source**: fix-scan-camera

Permissions pre-check (`navigator.permissions.query({ name: 'camera' })`) was added but removed before merge because it read the browser's cached 'denied' state from previous HTTP attempts, blocking `getUserMedia` before the browser could show a fresh permission prompt.

**Acceptance**: Re-add permissions pre-check with proper origin-scoped detection. `getUserMedia` should still be called if permission state is 'prompt' (not yet asked) or 'granted'. Only block on confirmed 'denied' for the current origin (scheme + host + port).

---

### #23: Self-signed cert warning on phone

- **Labels**: enhancement, priority-p3, infra
- **Created**: 2026-07-09
- **Domain**: infra
- **Source**: fix-scan-camera

Production server uses a self-signed certificate (generated via `keytool`). Users get a browser security warning on first visit: "Your connection is not private." Must tap "Advanced" → "Proceed to site" before accessing the app.

**Acceptance**: Replace with Let's Encrypt certificate or configure mDNS hostname that resolves on local network.

---

### #24: TypeScript compilation errors (5 pre-existing)

- **Labels**: bug, priority-p3, frontend
- **Created**: 2026-07-09
- **Domain**: frontend
- **Source**: fix-scan-camera

`npx tsc --noEmit` reports 5 errors across 2 files. All predate both makmur-0 and fix-scan-camera. Not blocking — app builds and runs via Vite which tolerates them.

**Affected files**:
- `client/src/components/Viewfinder.tsx:81` — `decodeFromVideoElement` callback signature mismatch (type cast needed)
- `client/src/test/components/Login.test.tsx:3` — missing `@testing-library/user-event` dependency
- `client/src/test/components/Login.test.tsx:18` — `beforeEach` not found (missing `vitest` import)

---

### #25: Vite dev server over HTTP — camera blocked on phone

- **Labels**: enhancement, priority-p3, frontend
- **Created**: 2026-07-09
- **Domain**: frontend
- **Source**: fix-scan-camera

`npm run dev` starts Vite dev server on HTTP (port 5173). Camera `getUserMedia` works on `localhost` (considered secure) but fails on any other device accessing via `http://<ip>:5173`. The production server (`npm start`) uses HTTPS/port 3001 and works from phone after accepting the cert warning.

**Acceptance**: Configure Vite dev server with the same self-signed cert for HTTPS, or document that phone testing requires `npm start` (port 3001).

---

### #26: Continuous in-place scanning not implemented

- **Labels**: enhancement, priority-p3, scan
- **Created**: 2026-07-09
- **Domain**: scan
- **Source**: fix-scan-camera

After a barcode is decoded, `handleScan()` in `App.tsx` navigates to detail/create view, which unmounts `Viewfinder`. Re-mounting reinitializes the camera. True continuous scanning (decoder stays active without camera restart) is not implemented. Acceptable for v1 — each scan cycle is <1s camera reinit.

**Acceptance**: Add configurable continuous scan mode where the decoder remains active after decode, allowing sequential scans without camera restart.

---

### #4: No refresh token mechanism

- **Labels**: enhancement, priority-p2, auth
- **Created**: 2026-07-08
- **Domain**: auth
- **Source**: makmur-0

Single 24h JWT token with no refresh flow. Staff mid-scanning when token expires will lose their action and be redirected to login. Not critical for v1.

**Resolution**: Deferred. Add refresh token in v2 if mid-shift expirations become a problem.

---

## Closed / Resolved Issues

### #29: Change UI language & currency to Indonesian (fix-change-currency-and-language)

- **Status**: resolved
- **Closed**: 2026-07-10
- **Labels**: enhancement, priority-p1, frontend
- **Created**: 2026-07-10
- **Source**: fix-change-currency-and-language

All UI text changed to Indonesian. Prices display as `Rp` with `.` thousands separator. Error messages translated on both frontend and backend. No new dependencies or schema changes.

**Fix**: Translated all user-facing text in React components, server error messages, and price format to Indonesian Rupiah standard.

---

### #28: Action-choice dialog after barcode scan (fix-input-barcode)

- **Status**: resolved
- **Closed**: 2026-07-10
- **Labels**: enhancement, priority-p1, scan
- **Created**: 2026-07-10
- **Source**: fix-input-barcode

After scanning a barcode, app jumped directly to detail or create view with no confirmation dialog. Staff had no choice of action before navigating.

**Fix**: Added ScanResultDialog component with two modes — found product (3 buttons: Tambah 1 stok / Lihat detail / Pindai lagi) and unknown barcode (2 buttons: Tambah produk baru / Pindai lagi). Removed auto-dismiss timer.

---

### #27: Build artifacts tracked in git

- **Status**: resolved
- **Closed**: 2026-07-09
- **Labels**: bug, priority-p1, infra
- **Created**: 2026-07-09
- **Source**: fix-scan-camera

`client/dist/`, `server/target/`, `server/data/`, `server/src/main/resources/keystore.p12`, and `server/data/*.db-shm/.db-wal` were tracked in git. Bloated repo size with generated files.

**Fix**: Updated `.gitignore` and ran `git rm --cached` on all build artifact paths.

---

### #18: No CHANGELOG.md for release tracking

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: enhancement, priority-p3
- **Created**: 2026-07-08
- **Source**: makmur-0

No release history document. Useful once the project has tagged releases.

**Fix**: Create CHANGELOG.md in Keep a Changelog format with [Unreleased] section listing auth feature and known gaps.

---

### #17: No LICENSE file (README says MIT)

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: priority-p3, docs
- **Created**: 2026-07-08
- **Source**: makmur-0

README.md footer says "MIT" but no LICENSE file exists at project root. Legal gap.

**Fix**: Create LICENSE file matching MIT license text.

---

### #16: No root-level AGENTS.md for AI assistants

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: priority-p2, docs
- **Created**: 2026-07-08
- **Source**: makmur-0

AGENTS.md exists only inside openspec/changes/makmur-0/ (SDD change directory). No root-level AGENTS.md for AI assistants landing on the project root.

**Fix**: Create root AGENTS.md with project overview, tech stack, key conventions, and pointers to detailed docs.

---

### #15: business-logic.md auth section reads as present-tense (auth is done)

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: priority-p3, docs
- **Created**: 2026-07-08
- **Source**: makmur-0

Section "Why only admin needs auth initially" describes auth as pending/upcoming feature. Auth is fully implemented.

**Fix**: Mark as "Historical context — auth was added after v1" or update to reflect current state.

---

### #14: current-state-context.md test status outdated

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: priority-p3, docs
- **Created**: 2026-07-08
- **Source**: makmur-0

current-state-context.md says T-auth-22 "NOT STARTED" and PRD criteria "PARTIAL". verify-report.md confirms 61/61 tests pass.

**Fix**: Update test status section to reflect current state.

---

### #13: AGENTS.md code map has typos and missing entries

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: priority-p2, docs
- **Created**: 2026-07-08
- **Source**: makmur-0

AGENTS.md inside openspec/changes/makmur-0/ has copy-paste errors in file tree and missing entries.

**Fix**: Clean up file tree, add all listed components, remove copy-paste errors.

---

### #12: tasks.md Phase 1-6 reference Node.js/Express (should be Java/Spring Boot)

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: priority-p1, docs
- **Created**: 2026-07-08
- **Source**: makmur-0

Original 6 task phases reference Node.js files (package.json, index.js, routes/products.js). Actual backend is Java/Spring Boot.

**Fix**: Mark Phase 1-6 as COMPLETED/SUPERSEDED or rewrite references to Java paths.

---

### #11: README.md significantly outdated

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: priority-p1, docs
- **Created**: 2026-07-08
- **Source**: makmur-0

Root README.md has multiple critical errors: fake creds, wrong auth claim, missing auth files from tree, no login/setup endpoints.

**Fix**: Rewrite README to reflect current state.

---

### #10: No Cache-Control: no-cache on index.html

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: bug, frontend, priority-p3
- **Created**: 2026-07-08
- **Source**: makmur-0

No Cache-Control header on index.html. Browser caches old app version after deploy.

**Fix**: Add Cache-Control: no-cache to Vite config or server WebConfig.

---

### #9: No server-side QR endpoint — uses external API

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: enhancement, auth, priority-p3
- **Created**: 2026-07-08
- **Source**: makmur-0

SetupPage renders QR using api.qrserver.com (external HTTP service). No offline coverage.

**Fix**: Add GET /api/setup/qr server endpoint or document external API as accepted design.

---

### #8: zxing-js polyfill eagerly imported (not lazy-loaded)

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: enhancement, priority-p2, frontend
- **Created**: 2026-07-08
- **Source**: makmur-0

zxing-js (1.2MB) loads via module-level import on ALL browsers. Spec says lazy-load only for Safari/Firefox.

**Fix**: Dynamic import zxing-js only when BarcodeDetector is not available.

---

### #7: Reset password button missing from UserList frontend

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: bug, priority-p2, frontend
- **Created**: 2026-07-08
- **Source**: makmur-0

PATCH /api/users/:id/reset-password endpoint exists on server. UserList.tsx has no button or UI to trigger it.

**Fix**: Add "Reset Password" button per user row in UserList with confirmation dialog.

---

### #6: Password min length mismatch — client enforces 6, server enforces 8

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: bug, priority-p2, auth
- **Created**: 2026-07-08
- **Source**: makmur-0

Client Login.tsx enforces 6-char minimum. Server AuthController enforces 8-char minimum.

**Fix**: Align client minimum length to 8 in Login.tsx and CreateUserForm.tsx.

---

### #5: Camera permission denied — no graceful fallback

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: bug, priority-p1, scan
- **Created**: 2026-07-08
- **Source**: makmur-0

If a user denies camera permission on first access, app shows browser-level error instead of a friendly message.

**Acceptance**: Denying camera permission shows in-app message with instructions + manual barcode entry input.

---

### #3: No JWT_SECRET env var guard at production startup

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: bug, priority-p1, auth
- **Created**: 2026-07-08
- **Source**: makmur-0

JwtUtil reads JWT_SECRET from env and validates minimum 32 bytes at startup. If unset, server fails to start.

**Acceptance**: Server startup script documents setting JWT_SECRET. Dev mode generates a default warning.

---

### #2: Pre-existing TypeScript errors in frontend

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: bug, priority-p2, frontend
- **Created**: 2026-07-08
- **Source**: makmur-0

5 pre-existing TS errors in client/src/ (Viewfinder callback type mismatch, test file missing dependency).

**Affected files**: `client/src/components/Viewfinder.tsx`, test config files.

---

### #1: Unauthenticated requests return 403 instead of 401

- **Status**: resolved
- **Closed**: 2026-07-08
- **Labels**: bug, priority-p1, auth
- **Created**: 2026-07-08
- **Source**: makmur-0

Spring Security default returns `403 Forbidden` when no JWT is provided. Spec requires `401 Unauthorized`.

**Fix**: Add `AuthenticationEntryPoint` in `SecurityConfig`.

**Acceptance**: Unauthenticated request to any protected endpoint returns `{"error":"unauthorized","message":"..."}` with HTTP 401.

---