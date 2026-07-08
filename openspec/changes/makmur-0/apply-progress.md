# Apply Progress — makmur-0

## Phase 2 — Server Onboarding (T-auth-12, T-auth-13)

### Completed

| Task | Status | Files Changed |
|------|--------|---------------|
| T-auth-12: Create SetupController — onboarding flow | ✅ Done | `SetupTokenStore.java` (ADD), `SetupController.java` (ADD) |
| T-auth-13: SecurityConfig /api/setup/** | ✅ Already done (merged into T-auth-08) | `SecurityConfig.java` (no change needed) |

### Implementation details

**SetupTokenStore.java** (`server/.../config/SetupTokenStore.java`):
- `@Component` — Spring auto-discovers, generates UUID token via `@PostConstruct`
- Token stored in `ConcurrentHashMap<String, Instant>` with 60-minute TTL from generation
- `getToken()` — returns token string if not expired (lazy-expiry cleanup)
- `isValid(token)` — checks existence and expiry
- `invalidate(token)` — removes token (one-time use)
- `generateToken()` — clears old tokens, creates new UUID + expiry

**SetupController.java** (`server/.../controller/SetupController.java`):
- `GET /api/setup/status` — returns `{ needsSetup: boolean }` based on admin user count
- `GET /api/setup/token` — returns `{ token, expires_at }` if no admin exists, 403 if already set up. Auto-regenerates expired tokens.
- `POST /api/setup/register` — validates token + input (username, password ≥ 8 chars), creates admin user via `UserRepository.save()`, invalidates token. Race-condition guard: checks no admin exists after token validation.
- QR generation deferred to frontend (zxing library client-side)

### Deviation from design
- No `GET /api/setup/qr` endpoint — user confirmed frontend handles QR rendering via zxing on client side, server just serves token value
- Added `GET /api/setup/status` endpoint for frontend to check if setup is needed

---

## Phase 4 — Frontend Auth Layer (T-auth-14, T-auth-15, T-auth-16, T-auth-21)

### Completed

| Task | Status | Files Changed |
|------|--------|---------------|
| T-auth-14: AuthContext — expiry detection, role-aware user, logout redirect | ✅ Done | `AuthContext.tsx` (modify) |
| T-auth-15: api.ts 401 interceptor | ✅ Done | `api.ts` (modify) |
| T-auth-16: Login — deactivated-account error | ✅ Done | `Login.tsx` (modify) |
| T-auth-21: API types + user management functions | ✅ Done | `types.ts` (modify), `api.ts` (modify) |
| Extra: ProtectedRoute component | ✅ Done | `ProtectedRoute.tsx` (ADD), `App.tsx` (modify) |

### Implementation details

**AuthContext.tsx**:
- Added `isTokenExpired()` — decodes JWT payload (base64), checks `exp * 1000 < Date.now()`
- Initial state: if stored token exists but expired, auto-clears and sets token to null
- Mount effect: re-checks expiry (handles tokens that expired while page was open)
- Error passthrough: `login()` now attaches `error.code` from server response body for error-type discrimination
- `logout()`: clears sessionStorage + state, redirects to `/login`

**api.ts**:
- `apiFetch()` now checks `res.status === 401` after fetch, calls `redirectLogin()` (clears token, redirects to `/login`)
- Non-401 errors pass through unchanged
- Added setup functions: `getSetupStatus()`, `getSetupToken()`, `setupRegister()` — use plain `fetch` (no auth needed)
- Added user management functions: `fetchUsers()`, `createUser()`, `deactivateUser()`, `reactivateUser()`, `resetUserPassword()` — all use auth-protected `apiFetch`

**Login.tsx**:
- Checks `err.code === 'account_disabled'` for specific message: "Account is deactivated. Contact your manager."
- Falls through to generic error message display for all other errors

**ProtectedRoute.tsx**:
- Reads token from `sessionStorage` directly
- If no token: returns null (App.tsx gate handles redirect to login)
- If token expired: clears auth, redirects to `/login`
- Otherwise renders children

**App.tsx**:
- Wraps `MainApp` in `<ProtectedRoute>` when token exists
- `MainApp` initial view defaults to `'products'` for staff, `'dashboard'` for others
- LoginGate component checks setup status on mount when no token exists

### Verification evidence
- Server: `mvn compile -q` — clean build
- Frontend: `npx tsc --noEmit` — no new type errors (only 5 pre-existing in Viewfinder.tsx and test config)

### Next steps for verify
- Manual test: POST /api/auth/login with deactivated user → 403 account_disabled
- Manual test: GET /api/setup/status with empty users table → needsSetup: true
- Manual test: POST /api/setup/register with valid token → 201, admin created, token invalidated
- Manual test: expired JWT → 401 → redirect to /login
