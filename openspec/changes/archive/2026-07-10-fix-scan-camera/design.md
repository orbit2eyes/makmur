# Design: Reliable Camera Detection for Barcode Scanning

- **Change**: fix-scan-camera
- **Status**: Approved for implementation
- **Date**: 2026-07-09
- **Domain**: scan
- **Files affected**: `client/src/components/Viewfinder.tsx` (primary), `client/src/App.tsx` (review only)
- **Dependencies**: None (no new npm packages)

---

## 1. Architecture Overview

The scan view is a single-page state within the React SPA. `App.tsx` manages view state (`view === 'scan'`), which conditionally renders the `Viewfinder` component. No router — it is a `switch` on a state enum.

### Viewfinder lifecycle

```
┌────────────────────────────────────────────────────────────────┐
│                        App.tsx (view === 'scan')              │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                   Viewfinder.tsx                        │  │
│  │                                                         │  │
│  │  ┌──────────────┐   ┌──────────────┐   ┌────────────┐  │  │
│  │  │  getUserMedia │──>│  init()      │──>│  Decoder   │  │  │
│  │  │  stream       │   │  setup video │   │  loop      │  │  │
│  │  └──────────────┘   └──────────────┘   └─────┬──────┘  │  │
│  │                                               │          │  │
│  │  ┌────────────────────────────────────────────┘          │  │
│  │  │                                                       │  │
│  │  ▼                                                       │  │
│  │  ┌──────────────┐   ┌──────────────┐   ┌────────────┐  │  │
│  │  │  onDecode()  │──>│  scannedRef  │──>│  onScan()  │  │  │
│  │  │  rawValue    │   │  = true      │   │  callback  │  │  │
│  │  └──────────────┘   └──────┬───────┘   └─────┬──────┘  │  │
│  │                             │                  │          │  │
│  │  ┌──────────────────────────┘                  │          │  │
│  │  │  reset scan state                          │          │  │
│  │  ▼                                             ▼          │  │
│  │  ┌──────────────┐                    ┌─────────────────┐  │  │
│  │  │ scannedRef   │                    │ App.handleScan()│  │  │
│  │  │ = false      │                    │ fetch product   │  │  │
│  │  └──────────────┘                    │ navigate        │  │  │
│  │                                      └─────────────────┘  │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
│  On unmount: cancelled=true, tracks.stop(), zxing reader      │
│  cancels, animation frame cancelled                           │
└───────────────────────────────────────────────────────────────┘
```

### Decode pipeline

```
Viewfinder.init()
    │
    ▼
Check permissions API (feature-detect)
    │
    ▼
getUserMedia({ facingMode: 'environment' })
    ├── Success ──────────────────────────────────────────┐
    │                                                     │
    │   ┌─────────────────────────────────────────────────┴─┐
    │   │  Wait video.readyState >= 2 (with 10s timeout)   │
    │   │  ├── Timeout → show timeout message + manual entry│
    │   │  └── Ready ──────────────────────────────────┐    │
    │   │                                              ▼    │
    │   │   ┌──────────────────────┐    ┌──────────────────┐ │
    │   │   │  BarcodeDetector     │    │  @zxing/library  │ │
    │   │   │  (native, Chrome)    │    │  (polyfill)      │ │
    │   │   │                      │    │                  │ │
    │   │   │  rAF + canvas        │    │  decodeFromVideo │ │
    │   │   │  detect() every 500ms│    │  Element()       │ │
    │   │   └────────┬─────────────┘    └────────┬─────────┘ │
    │   │            │                            │           │
    │   │            ▼                            ▼           │
    │   │   onDecode(rawValue)                             │
    │   │       │                                           │
    │   │       ├── scannedRef = true                       │
    │   │       ├── setDetected(true)                       │
    │   │       └── onScan(barcode) → App.tsx               │
    │   │           ├── fetchProduct -> detail/create view  │
    │   │           └── (after display) scannedRef = false  │
    │   │                                                   │
    │   └── Detector loop continues for next barcode        │
    │                                                       │
    ├── OverconstrainedError ──► retry { video: true }     │
    │       ├── Success ─────────→ same pipeline as above   │
    │       └── Error ───────────→ show error message       │
    │                                                       │
    ├── NotAllowedError ──► show permission denied message  │
    ├── NotFoundError ────► show no camera found message    │
    ├── NotReadableError ─► show camera in use message      │
    ├── SecurityError ────► show HTTPS required message     │
    └── Unknown ──────────► show generic error message      │
                            │                               │
                            ▼                               │
                     Manual entry always visible           │
```

---

## 2. Component Changes

| File | Change Type | What Changes |
|------|-------------|--------------|
| `client/src/components/Viewfinder.tsx` | MODIFIED | Camera fallback chain (OverconstrainedError retry), 5 distinct error messages, video readyState timeout (10s), zxing import `.catch()`, permissions pre-check via `navigator.permissions.query`, scan state reset after result display |
| `client/src/App.tsx` | NO CHANGE | No view state changes needed. The `handleScan` callback already fetches product and navigates. Scan state reset happens inside Viewfinder, not in App. App's auto-return timer (`autoReturn`) is orthogonal — it returns from detail to scan view, but does not affect the decoder lock. |

### Why App.tsx is not modified

The continuous scan fix (Fix 3) resets `scannedRef.current` inside Viewfinder after the `onScan` callback completes. The callback (`handleScan` in App) already navigates away to `detail` or `create` view, which unmounts Viewfinder. When the user navigates back to scan (via "Scan Another" button or auto-return timer), Viewfinder remounts fresh with `scannedRef.current = false` (default ref value). No App.tsx changes needed.

However, if the team wants seamless scanning *without* leaving the scan view (i.e., result overlay on top of the viewfinder), then App.tsx would need to support a sub-state within the scan view. This is not in scope for v1 (see Decision: scanRef reset).

---

## 3. Error Handling Design

### Camera initialization error decision tree

```
Viewfinder.init()
    │
    ├── 1. Permissions API check (feature-detected)
    │       ├── denied ──► show NotAllowedError message (skip getUserMedia)
    │       └── prompt/grant/unsupported ──► continue
    │
    ├── 2. getUserMedia({ facingMode: 'environment' })
    │       ├── Success ──► start decoder pipeline
    │       ├── OverconstrainedError ──► 2a. retry { video: true }
    │       │       ├── Success ──► start decoder pipeline
    │       │       ├── NotAllowedError ──► show "Camera permission denied..."
    │       │       ├── NotFoundError ────► show "No camera found..."
    │       │       ├── NotReadableError ─► show "Camera in use..."
    │       │       ├── SecurityError ────► show "HTTPS required..."
    │       │       └── Unknown ─────────► show generic message
    │       ├── NotAllowedError ──► show "Camera permission denied..."
    │       ├── NotFoundError ────► show "No camera found..."
    │       ├── NotReadableError ─► show "Camera in use..."
    │       ├── SecurityError ────► show "HTTPS required..."
    │       └── Unknown ─────────► show generic message
    │
    └── (error messages shown alongside ManualEntry component)

Error message map (constant object):

    ERRORS = {
      NotAllowedError: "Camera permission denied. Enable camera access in
                        browser settings, or use manual entry below.",
      NotFoundError:   "No camera found on this device. Use manual entry
                        below to enter the barcode.",
      NotReadableError:"Camera is in use by another application or browser
                        tab. Close the other tab and try again.",
      SecurityError:   "Camera requires a secure connection (HTTPS). Contact
                        your IT team to enable HTTPS on this site.",
      TIMEOUT:         "Camera stream is taking too long to start. Try
                        reloading the page or use manual entry below.",
      UNKNOWN:         "Camera unavailable. Use manual entry below.",
    }
```

### Decode failure paths

```
startNative()
    │
    ├── BarcodeDetector constructor succeeds
    │       └── detect() loop (rAF, 500ms throttle)
    │           ├── detect() resolves with barcode ──► onDecode()
    │           └── detect() fails ──► silent retry on next frame
    │
    └── BarcodeDetector unsupported or fails
            └── fall through to startZxing()

startZxing()
    │
    ├── await import('@zxing/library') succeeds
    │       └── decodeFromVideoElement() callback
    │           ├── result present ──► onDecode()
    │           └── error only ──► silent retry
    │
    └── await import('@zxing/library') fails (.catch())
            └── show manual entry form (no viewfinder)

video readyState wait (shared between both paths)
    │
    ├── readyState >= 2 within TIMEOUT_MS (10s)
    │       └── start detection
    │
    └── readyState < 2 after TIMEOUT_MS
            └── show timeout error message + manual entry
```

### State machine

```
IDLE ──> INIT ──> REQUESTING_PERMISSION ──> STREAMING ──> DECODING
                  │                              │            │
                  │                              │            ├── DECODED ──> IDLE (reset)
                  │                              │            └── FAIL ──> RETRY
                  │                              │
                  │                              └── ERROR ──> ERROR_SHOWN
                  │                                            │
                  └── ERROR ──> ERROR_SHOWN                      + ManualEntry
                                + ManualEntry

States implemented via refs/state:
  - IDLE:          initial render
  - INIT:          useEffect running, permissions check
  - REQUESTING:    getUserMedia in flight
  - STREAMING:     stream active, video element displaying
  - DECODING:      readyState >= 2, detector running
  - DECODED:       scannedRef = true, result displayed
  - ERROR_SHOWN:   cameraError state set, ManualEntry rendered
  - RETRY:         OverconstrainedError → retry with { video: true }
```

---

## 4. Data Flow (Scan Lifecycle)

### Complete lifecycle, mount to unmount

```
1. MOUNT
   Viewfinder mounts (rendered by App.tsx when view === 'scan')
   useEffect fires (once, deps: [onScan, onError])

2. INIT (async)
   ┌─────────────────────────────────────────────────────────┐
   │  permissionsApiAvailable()                               │
   │    ├── true: navigator.permissions.query({name:'camera'})│
   │    │   └── denied → setCameraError(NOT_ALLOWED), return  │
   │    └── false: skip (Safari, old browsers)                │
   │                                                          │
   │  try {                                                   │
   │    stream = await getUserMedia({ facingMode:'environment'})│
   │  } catch (OverconstrainedError) {                        │
   │    stream = await getUserMedia({ video: true })          │
   │  }                                                       │
   │                                                          │
   │  if (cancelled) { stream.getTracks().stop(); return }    │
   │  streamRef.current = stream                              │
   │  videoRef.current.srcObject = stream                     │
   └─────────────────────────────────────────────────────────┘

3. DECODE SETUP
   ┌─────────────────────────────────────────────────────────┐
   │  if (native BarcodeDetector available)                   │
   │    startNative()                                         │
   │  else                                                    │
   │    startZxing()                                          │
   │                                                          │
   │  Both paths:                                             │
   │    wait = setTimeout(TIMEOUT_MS) {                        │
   │      if (readyState < 2) → timeout error                 │
   │    }                                                     │
   │    poll/setInterval until readyState >= 2                 │
   │      → clearTimeout, start detection loop                │
   └─────────────────────────────────────────────────────────┘

4. DECODE LOOP
   ┌─────────────────────────────────────────────────────────┐
   │  startNative():                                          │
   │    rAF → canvas.drawImage(video) → detector.detect()    │
   │      → throttle: 500ms between detections               │
   │                                                          │
   │  startZxing():                                           │
   │    reader.decodeFromVideoElement(video, callback)        │
   │      → fires on every frame (library throttles)          │
   │                                                          │
   │  on detection:                                           │
   │    scannedRef.current = true                             │
   │    setDetected(true)                                     │
   │    onScan(rawValue)                                      │
   └─────────────────────────────────────────────────────────┘

5. CALLBACK (App.tsx)
   ┌─────────────────────────────────────────────────────────┐
   │  handleScan(barcode):                                    │
   │    product = await fetchProduct(barcode)                 │
   │    if (product) {                                        │
   │      setSelectedProduct(product)                         │
   │      setView('detail')          // unmounts Viewfinder  │
   │      setAutoReturn(2)           // 3s auto-return timer │
   │    } else {                                              │
   │      setScannedBarcode(barcode)                          │
   │      setView('create')          // unmounts Viewfinder  │
   │    }                                                     │
   └─────────────────────────────────────────────────────────┘

6. UNMOUNT
   ┌─────────────────────────────────────────────────────────┐
   │  useEffect cleanup:                                      │
   │    cancelled = true           // stops all async ops    │
   │    cancelAnimationFrame(raf)                            │
   │    if (zxingReader) reader.reset()                      │
   │    if (streamRef.current)                                │
   │      streamRef.current.getTracks().forEach(t =>         │
   │        t.stop())                // camera LED off       │
   └─────────────────────────────────────────────────────────┘

7. RE-MOUNT (returning to scan)
   ┌─────────────────────────────────────────────────────────┐
   │  "Scan Another" button → setView('scan')                │
   │  or auto-return timer → setView('scan')                  │
   │  Viewfinder remounts → fresh state (scannedRef=false)   │
   │  Entire lifecycle repeats from step 1                   │
   └─────────────────────────────────────────────────────────┘
```

### Props flow

```
App.tsx                             Viewfinder.tsx
──────                              ──────────────
handleScan: (barcode) => { ... }    onScan={handleScan}
                                    onError={(err) => console.error(err)}
                                    │
                                    │ onScan(barcode)
                                    ▼
                                    App.handleScan(barcode)
                                      → fetchProduct(barcode)
                                      → setView('detail' or 'create')
                                    ▼
                                    re-render with new view
                                    → Viewfinder unmounts
                                    → Cleanup: tracks.stop(), cancelled=true
```

---

## 5. Key Design Decisions

| Decision | Options | Chosen | Rationale |
|----------|---------|--------|-----------|
| Camera fallback UX | (a) Silent fallback to `{ video: true }` | **Silent** | Front camera mirrored feed works for barcode scanning (EAN-13 has large enough features). A prompt after OverconstrainedError adds friction with no benefit. |
| | (b) Show prompt: "Rear camera not found. Use front camera?" | | |
| Timeout duration | (a) 5s, (b) 10s, (c) 15s | **10s** | 5s risks false positives on slow-starting devices (older Android phones, tablets waking from sleep). 15s is too long for perceived responsiveness. 10s is a conservative middle ground. Tunable constant. |
| Error message storage | (a) Inline template strings in catch blocks | **Constant map** (named `ERRORS` or `CAMERA_ERRORS`) | Single source of truth. Easy to audit all messages, test in isolation, and localize (one import swap). Inline strings scattered across catch blocks risk drift and missed updates. |
| | (b) Static object/Map in module scope | | |
| scanRef reset trigger | (a) Reset when App returns to scan view (on mount) | **Reset after onScan callback** (within Viewfinder, as part of the same effect cycle) | Actually — on mount reset is simpler: every time Viewfinder mounts, `scannedRef` starts as `false` (it is a ref, not state). Since navigating to detail/create unmounts Viewfinder, re-mounting on return already gives a fresh ref. The real bug is that the decoder does not check `scannedRef` after returning from the callback — but `cancelled=true` on unmount handles that. **Decision**: No change needed. The mount-cycle already resets scan state. The lock is a non-issue because Viewfinder unmounts on navigation. However, Fix 3 (per PRD) wants scanning without leaving the scan view. That requires a reset *without* unmount — meaning a `scannedRef.current = false` call after the `onScan` handler finishes, while the component stays mounted. This is only needed if we implement in-place result overlay. For v1 (navigate away pattern), no code change needed; for future in-pane results, add the reset. |
| | (b) Reset after onScan completes (in-place) | | |
| Permission pre-check | (a) Mandatory (must have Permissions API) | **Optional (feature-detected)** | Safari does not support `navigator.permissions.query({ name: 'camera' })`. Making it mandatory would break on iOS. Feature-detection: `'permissions' in navigator && typeof navigator.permissions.query === 'function'` then try/catch the call. |
| | (b) Optional (silent skip if unsupported) | | |
| zxing .catch() handling | (a) Show manual entry form | **Show manual entry form** | The simplest fallback that preserves functionality. No retry mechanism needed — if the import failed once on this page load, retrying is unlikely to succeed (network condition is the same). User can retry by reloading. |
| | (b) Retry import after delay | | |
| Manual entry visibility | (a) Always rendered (separate from error state) | **Always rendered below Viewfinder** | Currently `ManualEntry` is rendered only inside the error block in Viewfinder. For continuous scan, the user needs access to manual typing even when the camera is working (damaged barcode, low light). Move `ManualEntry` to the parent `renderScan()` in App.tsx, outside Viewfinder. |
| | (b) Only in error states | | |

### Refined decision: Manual entry visibility

Current code renders `ManualEntry` only inside Viewfinder's error state block. The `renderScan()` in App.tsx already has a separate `<ManualEntry>` outside Viewfinder, so it appears both in the error state and below the viewfinder. No change needed for v1.

```tsx
// App.tsx renderScan() — already correct
const renderScan = () => (
  <div className="scan-screen">
    <Viewfinder onScan={handleScan} />
    <ManualEntry onLookup={(p) => navigate('detail', { product: p })} />
    <button className="btn btn-back" onClick={() => navigate('products')}>Cancel</button>
  </div>
)
```

The Viewfinder error state *also* renders ManualEntry inside its own div. This is redundant but harmless — delete the inner ManualEntry when refactoring Viewfinder error handling.

---

## 6. Verification Hooks

| AC | Method | How to Run |
|----|--------|------------|
| AC1 (fallback) | Manual | Open app on iPad or device with front camera only. Verify viewfinder starts with front camera (no error). |
| AC2 (error messages) | Manual | Open browser dev tools. Before opening scan view, run: `navigator.mediaDevices.getUserMedia = () => Promise.reject(new DOMException('msg', 'NotAllowedError'))`. Verify message text. Repeat for NotFoundError, NotReadableError, SecurityError. |
| AC3 (continuous scan) | Manual | Open scan view on device with camera. Scan barcode #1 → verify detail view shown → tap "Scan Another" → scan barcode #2 → verify detail view shows product #2. |
| AC4 (zxing .catch()) | Manual | Block `@zxing/library` CDN in dev tools network tab → open scan view → verify manual entry form appears with no console errors. |
| AC5 (timeout) | Manual | Before mounting Viewfinder, run: `HTMLVideoElement.prototype.__defineGetter__('readyState', () => 0)`. Open scan view → wait 10s → verify timeout message. |
| AC6 (manual entry always visible) | Visual inspection | Verify `ManualEntry` renders in all states: camera streaming, camera error, timeout. |
| AC7 (permissions pre-check) | Manual | Deny camera permission in browser site settings → open scan view → verify console shows no `getUserMedia` call (check Network/Media tab). |
| AC8 (no regressions) | Automated | `cd server && mvn test` — all 61 tests must pass. |
| AC9 (no new deps) | Automated | `npm ls --depth=0` — package list unchanged from `git diff HEAD -- package.json`. |
| AC10 (track.stop()) | Manual | Open scan view → observe camera LED → navigate away (click Cancel) → verify camera LED turns off. Open scan view → deny permission → verify no LED. |

---

## 7. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Breaking camera on devices where it works | Low | High | Run manual regression on 2+ phone models (Android Chrome, iOS Safari) before merging. Keep current happy-path unchanged (getUserMedia with environment facingMode still ships first). |
| Safari vs Chrome permissions API | Medium | Medium | Wrap `navigator.permissions.query` in feature detection. Safari returns `undefined` or throws silently — catch and treat as unsupported. |
| Non-standard Safari error names | Medium | Medium | Safari may throw `DOMException` with different error names (e.g., `"NotSupportedError"` instead of `"SecurityError"`). Maintain a browser-specific error name map alongside the standard map. Test on iOS Safari explicitly. |
| 10s timeout too short for old devices | Low | Low | Define `TIMEOUT_MS = 10000` as exported constant. If reports come in, increase to 15000 with a single-line change. |
| Manual entry hidden in some error state | Low | Medium | The `Viewfinder` error block currently renders `ManualEntry` itself. The parent `renderScan()` also renders it. If the inner `ManualEntry` is removed during refactoring, ensure the outer one is always visible. |
| OverconstrainedError not thrown on all platforms | Medium | Low | Some browsers emit `NotFoundError` instead. Catch both: `if (name === 'OverconstrainedError' || name === 'NotFoundError') → retry with { video: true }`. |

---

## 8. Implementation Notes

### Order of changes in Viewfinder.tsx

Apply changes in this order to minimize merge conflicts and allow easy review:

1. **Extract error message map** — replace inline strings with `CAMERA_ERRORS` constant object at module top level.
2. **Add TIMEOUT_MS constant** — `const TIMEOUT_MS = 10_000` at module top level.
3. **Add permissions pre-check** — before `getUserMedia` call in `start()` function.
4. **Wrap getUserMedia with fallback** — catch `OverconstrainedError`/`NotFoundError` and retry with `{ video: true }`. All other errors fall through to the existing error handler.
5. **Map error names to messages** — in the catch block, replace the if/else that checks `NotAllowedError` with a switch over all 5 error types + unknown fallback.
6. **Add readyState timeout** — add `setTimeout` before the poll/interval loops. Clear on readyState >= 2.
7. **Add zxing .catch()** — attach `.catch(() => showManualEntry())` to the dynamic import.
8. **Remove inner ManualEntry** — delete the `ManualEntry` rendered inside Viewfinder's error block (redundant with parent).
9. **Add scannedRef reset** — only needed if implementing in-place scanning. Not needed for v1 (navigate-away pattern).

### Safari-specific error names to map

```javascript
const ERROR_NAMES = {
  NotAllowedError: ['NotAllowedError', 'PermissionDeniedError'],
  NotFoundError:   ['NotFoundError'],
  NotReadableError:['NotReadableError', 'AbortError'],
  SecurityError:   ['SecurityError', 'NotSupportedError'],
}
```

Match on `error.name` or `error.code` against these lists before falling back to the standard `error.name` lookup.

### Test stubs (for manual verification)

```javascript
// AC2: Stub each error type
// In browser console before opening scan view:
const orig = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices)
navigator.mediaDevices.getUserMedia = () =>
  Promise.reject(new DOMException('simulated', 'NotAllowedError'))

// AC5: Stub readyState
// After opening scan view but before stream starts:
Object.defineProperty(HTMLVideoElement.prototype, 'readyState', { get: () => 0 })

// AC7: Verify permissions pre-check
// Deny camera in site settings, then:
// Check that getUserMedia is never called:
const origGUM = navigator.mediaDevices.getUserMedia
navigator.mediaDevices.getUserMedia = () => {
  console.log('GUM CALLED') // should not appear
  return origGUM()
}
```
