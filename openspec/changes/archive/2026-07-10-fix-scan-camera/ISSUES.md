# Issues — Makmur (fix-scan-camera)

> Sync target: GitHub Issues (repo: orbit2eyes/makmur)
> Active change: fix-scan-camera (camera detection fix)

---

## Open Issues

### #22: I-022: Camera permission pre-check removed — optimization lost

- **Labels**: enhancement, priority-p2, scan
- **Created**: 2026-07-09
- **Domain**: scan

Permissions pre-check (`navigator.permissions.query({ name: 'camera' })`) was added in T-camera-05 but removed before merge because it read the browser's cached 'denied' state from previous HTTP attempts, blocking `getUserMedia` before the browser could show a fresh permission prompt.

**Acceptance**: Re-add permissions pre-check with proper origin-scoped detection. `getUserMedia` should still be called if permission state is 'prompt' (not yet asked) or 'granted'. Only block on confirmed 'denied' for the current origin (scheme + host + port).

---

### #23: I-023: Self-signed cert warning on phone

- **Labels**: enhancement, priority-p3, infra
- **Created**: 2026-07-09
- **Domain**: infra

Production server uses a self-signed certificate (generated via `keytool`). Users get a browser security warning on first visit: "Your connection is not private." Must tap "Advanced" → "Proceed to site" before accessing the app.

**Acceptance**: Replace with Let's Encrypt certificate or configure mDNS hostname that resolves on local network.

---

### #24: I-024: 5 pre-existing TypeScript compilation errors

- **Labels**: bug, priority-p3, frontend
- **Created**: 2026-07-09
- **Domain**: frontend

`npx tsc --noEmit` reports 5 errors across 2 files. All predate both makmur-0 and fix-scan-camera. Not blocking — app builds and runs via Vite which tolerates them.

**Affected files**:
- `client/src/components/Viewfinder.tsx:81` — `decodeFromVideoElement` callback signature mismatch (type cast needed)
- `client/src/test/components/Login.test.tsx:3` — missing `@testing-library/user-event` dependency
- `client/src/test/components/Login.test.tsx:18` — `beforeEach` not found (missing `vitest` import)

---

### #25: I-025: Vite dev server over HTTP — camera blocked on phone

- **Labels**: enhancement, priority-p3, frontend
- **Created**: 2026-07-09
- **Domain**: frontend

`npm run dev` starts Vite dev server on HTTP (port 5173). Camera `getUserMedia` works on `localhost` (considered secure) but fails on any other device accessing via `http://<ip>:5173`. The production server (`npm start`) uses HTTPS/port 3001 and works from phone after accepting the cert warning.

**Acceptance**: Configure Vite dev server with the same self-signed cert for HTTPS, or document that phone testing requires `npm start` (port 3001).

---

### #26: I-026: Continuous in-place scanning not implemented

- **Labels**: enhancement, priority-p3, scan
- **Created**: 2026-07-09
- **Domain**: scan

After a barcode is decoded, `handleScan()` in `App.tsx` navigates to detail/create view, which unmounts `Viewfinder`. Re-mounting reinitializes the camera. True continuous scanning (decoder stays active without camera restart) is not implemented. Acceptable for v1 — each scan cycle is <1s camera reinit.

**Acceptance**: Add configurable continuous scan mode where the decoder remains active after decode, allowing sequential scans without camera restart.

---

## Closed / Resolved Issues

### #27: Build artifacts tracked in git

- **Status**: resolved
- **Closed**: 2026-07-09
- **Labels**: bug, priority-p1, infra
- **Created**: 2026-07-09

`client/dist/`, `server/target/`, `server/data/`, `server/src/main/resources/keystore.p12`, and `server/data/*.db-shm/.db-wal` were tracked in git. Bloated repo size with generated files.

**Fix**: Updated `.gitignore` and ran `git rm --cached` on all build artifact paths.
