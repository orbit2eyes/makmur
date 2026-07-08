# Business Logic — Makmur v1

This document explains the **why** behind Makmur's design decisions. It is not a technical architecture document. It captures the reasoning, trade-offs, and constraints that shaped the application.

---

## Core Concept

### Why barcode scanning as primary input

Retail and warehouse staff perform dozens to hundreds of product lookups per shift. Every lookup has the same pattern: find the product identifier, look up the record, verify or act on the data. The fastest way to get a product identifier into a system is to read it optically. Typing a barcode string by hand takes ~5-10 seconds and is error-prone. Scanning takes under a second with zero typing errors.

Makmur treats scanning as the default interaction mode because it matches how staff already work: they pick up a product, glance at the barcode, and want the information immediately. Typing is the fallback for when scanning fails (damaged labels, poor lighting, unsupported barcode format). The UI reflects this priority — the viewfinder is the first thing staff see when they open the app on a camera-equipped device.

We chose not to add keyboard shortcuts, voice input, or NFC as alternative input methods. Keyboard shortcuts require a physical keyboard (not available on phones). Voice input is unreliable in noisy retail environments. NFC is not universally available on staff devices. Scanning hits the sweet spot of speed, accuracy, and universal device support.

### Why webapp vs. native app

A native iOS or Android app would give us better camera access, offline support, and push notifications. We chose a webapp anyway, for three reasons:

1. **Zero install.** Staff open a URL on any device — personal phone, shared tablet, store laptop — and the app works. No App Store approval, no update cycle, no "I deleted the app" support calls. This is the single biggest adoption barrier remover for a retail tool used by non-technical staff.

2. **Any device, any OS.** Staff devices are heterogeneous: Android phones, iPhones, iPads, Chromebooks, old Windows laptops. A native app would require building and maintaining two codebases (or using a cross-platform framework like Flutter). A responsive webapp with a camera API works on all of them from one codebase.

3. **Server-side data is the point.** Makmur's core value is that every device sees the same products and stock counts. A webapp naturally routes all data through a central server. A native app with the same architecture would still need network access to the server — the "native" part only changes the UI layer. If we needed offline scanning with sync, a native app would win. For v1, the server is always reachable on the local network, so a webapp is the right starting point.

The trade-off is that camera access on the web is less reliable than native: Safari's `getUserMedia` support has had quirks, and the `BarcodeDetector` API is only available in Chromium browsers. The zxing-js polyfill covers the gap. If camera reliability proves insufficient in practice, we can revisit a native wrapper (e.g., wrapping the webapp in a lightweight WebView shell with native camera bridge) without changing the app logic.

### Why local network (single store)

Makmur is designed for a single retail store or warehouse. The server runs on a machine in the back office, connected to the same local network as the staff devices. We deliberately chose not to deploy to the cloud or require internet access, for three reasons:

1. **Latency.** Local network round-trips are <5ms. Cloud round-trips (even to a nearby region) are 20-50ms. For a workflow where staff scan dozens of items per minute, every millisecond adds up. More importantly, the stock update flow (scan, tap +/-1, see confirmation) involves two network hops per action. Local latency makes this feel instant.

2. **Reliability.** Retail Wi-Fi is generally reliable. Internet connectivity is not — especially in back-of-house areas, stock rooms, and basements where inventory work happens. A cloud-dependent app would fail precisely where staff need it most. A local network app works as long as the server and the device are on the same LAN.

3. **Cost and complexity.** No cloud hosting, no domain, no SSL certificate management (for local-only), no database-as-a-service subscription. The store buys one machine (even a Raspberry Pi 4 is sufficient) and the app runs until the hardware fails. Support, deployment, and maintenance are all local — the store manager can restart the server without calling anyone.

We'll reconsider cloud deployment if (a) the app needs to span multiple stores, (b) staff need access from off-premises devices (e.g., remote inventory auditing), or (c) the store wants centralized backup and monitoring. Until then, local network is the simpler, faster, and more reliable choice.

---

## Role Design

### Why 3 roles (admin, manager, staff)

A retail store has a natural hierarchy: the owner or senior manager who owns the system, shift supervisors who manage the team, and floor staff who do the day-to-day work. Three roles map directly to this structure.

- **Admin** owns the system. They deploy it, configure it, and have final authority over all data. They create manager accounts. They are the escalation point for account issues.
- **Manager** owns the team. They create staff accounts, deactivate departed staff, reset forgotten passwords. They do not touch product data — that's not their job.
- **Staff** owns the floor. They scan, stock, search, and browse. They do not manage users — that's not their job.

This three-tier design draws a clear line between who does what. A cashier scanning price checks should never be able to deactivate another employee's account. A shift supervisor should never accidentally change a stock count while managing the roster. The role separation makes these mistakes impossible by design.

We considered a two-role design (admin + staff) but rejected it because it forces the store owner to do all user management themselves. In practice, shift supervisors handle staffing — they hire, schedule, and manage the team. Forcing them to ask the owner every time a new cashier joins is friction that the role design should eliminate.

We considered a four-role design (adding a "viewer" or "auditor" role) but deferred it. YAGNI for v1. If a read-only role is needed later, it can be added without breaking the existing three.

### Why scope-based (handler-level) vs. route-based

The PRD explicitly requires (AC-21) that the same endpoint URL serves all roles. This means `GET /api/products` is the same URL whether the caller is staff, manager, or admin. The handler checks the role internally and decides whether to return data or 403.

Why not just route different roles to different URLs? Because that would force the frontend to know which URL to call based on the user's role, duplicating the access control logic on both sides. It would also create an API surface where role is part of the URL, which is semantically odd — the resource is the same (`products`), only the authorization differs.

The handler-level approach means:
- One endpoint, one implementation, regardless of who calls it.
- The frontend calls the same URL for all users. If a staff user tries to list users, they get 403 — the same URL, just a different response.
- Adding or changing roles in the future doesn't require URL restructuring. A new role is just a new entry in the scope matrix inside each handler.

The alternative — route-level gating with `SecurityConfig.requestMatchers().hasRole()` — would split the API surface into `/api/staff/products` and `/api/admin/products`, which is awkward, or would require Spring Security's method-level `@PreAuthorize` annotations, which work at the handler level anyway. The `AuthService.requireRole()` utility is explicit, visible in the handler code, and doesn't require understanding AOP proxies or annotation processing.

### Why manager can't see products

This is the most questioned design decision, so it deserves a clear explanation.

The manager role exists to **manage people, not products**. A shift supervisor's job is to schedule staff, approve time off, handle customer complaints, and ensure the team is working. They do not count stock, receive shipments, or check prices. Those tasks belong to floor staff.

Giving a manager access to products would create ambiguity: is the manager doing the staff's job, or is the manager checking up on the staff? Either interpretation erodes the accountability that role-based access is meant to create. If a manager adjusts a stock count, it's unclear whether the count reflects a real inventory change or a supervisory override. With role separation, every stock adjustment is attributable to a staff member. If the manager notices an incorrect count, they ask the staff member to correct it — the correction flows through the right person.

There is a practical argument that "managers sometimes help on the floor during busy periods." This is true. But rather than giving managers product access as a convenience, the right solution is to give them a staff-level account for their own scanning use, separate from their manager account. This preserves the audit trail: the person scanning is logged as staff, not as manager making a supervisory override.

We'll reconsider if managers report significant workflow friction from the product access restriction. The data from that feedback will tell us whether the design principle (separation of duties) is worth the convenience cost.

---

## Auth Decisions

### Why JWT vs. sessions

JWT was chosen over server-side sessions for three reasons:

1. **Stateless** — The server does not store session data. Every request is self-contained with the JWT carrying the user's identity and role. This makes horizontal scaling trivial (no session store to share across server instances). For a local-network app on a single machine, scaling isn't a concern, but the simplicity gain is still real: no session table, no cookie config, no session expiry management.

2. **Simplicity** — A JWT is a signed token that the client stores in `sessionStorage`. The server validates the signature on every request using a shared secret. No session lookup, no session affinity, no serialization/deserialization of session objects. The entire auth flow is one login endpoint, one filter, one utility class.

3. **Role in the token** — Embedding the role as a JWT claim means the server does not need to query the database on every request to determine the caller's role. This avoids a DB round-trip on every API call. The only DB check the filter performs is a lightweight `active`-flag lookup (by primary key, fast).

The downsides of JWT are well-known: token revocation requires a blacklist (not implemented in v1), and a leaked token grants access until expiry. For a local-network app where the threat model assumes trusted network and devices, these are acceptable risks. If internet-facing deployment is needed in the future, a refresh-token mechanism or token blacklist should be added.

### Why QR setup for first admin

Every deployment needs an initial admin account. The conventional approach is:

- **Default credentials in code** — e.g., `admin/admin123`. This is the most common pattern and the most dangerous. If the deployer forgets to change the password, the system is wide open. The default password is often guessable and shared across all deployments.
- **CLI seed command** — e.g., `java -jar app.jar --seed-admin`. This requires terminal access, which the store manager may not have or may be uncomfortable with.
- **Environment variable** — e.g., `ADMIN_PASSWORD=...`. This works but leaks the password into the process list, shell history, and log files.

The QR onboarding flow solves all three problems:

1. **No default credentials.** The first admin creates their own password. The system has zero pre-seeded credentials.
2. **No terminal access required.** The store manager opens the app in a browser, sees a QR code, scans it with their phone, and creates the account. The entire flow happens in a browser.
3. **Physical presence required.** The QR code is displayed on the server machine's browser. Someone must be physically present to scan it. This prevents remote attackers from creating an admin account.

The one-time token prevents replay: once the admin account is created, the token is invalidated. Even if someone captured the QR code image, they cannot use it to create a second admin account. The 60-minute TTL adds a second layer: if the admin doesn't complete setup within an hour, they need to restart the server (which generates a fresh token). This is a one-time inconvenience during initial deployment.

### Why only admin needs auth initially (pre-auth v1)

> **Historical note (July 2026):** Auth is now fully implemented. Admin login via QR onboarding, manager/staff roles with scope enforcement. See openspec/changes/makmur-0/specs/auth/spec.md for current state.

The original v1 (before the auth feature was added) had no authentication at all. Every device on the network could do everything. This was a deliberate choice for the initial scope: get the scanning and stock-tracking working first, then add auth.

Why not build auth from the start? Because auth adds friction at every layer:
- The login page is an extra step before every session.
- Token management adds complexity to the frontend (`api.ts` interceptor, AuthContext, expiry detection).
- Role-based navigation and scope enforcement add complexity to every handler.
- User management (create, deactivate, password reset) is a whole new feature domain.

For a proof-of-concept or pilot deployment in a single store with a small, trusted team, the auth layer is overhead that slows development and complicates every change. By building the core product features first (scan, stock, products), we validated the concept before investing in auth. The auth feature was always planned as the next phase — not an afterthought, but a deliberate second step.

Now that the core is stable, auth is being added as a proper feature with its own PRD, spec, tasks, and tests.

---

## Stock Design

### Why absolute + delta input (both modes serve different workflows)

Stock updates serve two distinct workflows that look similar on paper but have different requirements:

1. **Quick adjustments** — A staff member sells one unit and wants to decrement stock by 1. Or they receive one unit and want to increment by 1. The action is "adjust by one." Tapping +1 or -1 is the fastest path: one tap, immediate result, move on.

2. **Cycle counts and receiving** — A staff member counts all units of a product on the shelf. The result is an absolute count: "there are 42 units here." They don't care about the previous count (which might be wrong from accumulated errors). They want to set the stock to exactly 42. Absolute value entry is the right tool: enter 42, confirm, done.

Both modes coexist because they serve different staff personas. The delta buttons (+1/-1) serve the shop-floor staff doing frequent small adjustments. The absolute value input serves the back-of-house staff doing systematic counts. Neither mode alone covers both workflows well: delta-only would be painful for cycle counts ("I counted 42 units, so I need to tap +1 32 times if the old count was 10"), and absolute-only would be tedious for single-unit adjustments ("I sold one, I need to look up the current count, subtract 1 in my head, and type the result").

The server accepts both modes in the same endpoint: `{ value: 42 }` sets absolute, `{ delta: 1 }` adjusts relative. They are mutually exclusive in a single request — if both are provided, `value` takes precedence. This avoids ambiguity and keeps the endpoint simple.

### Why CHECK(stock >= 0) at DB level

Stock should never be negative. This is a business invariant, not just a validation concern. We enforce it at three levels:

1. **UI** — The -1 button is disabled when stock is 0. The absolute value input rejects negative numbers.
2. **Controller** — The PATCH endpoint validates that the result would be non-negative before applying the update.
3. **Database** — `CREATE TABLE ... CHECK(stock >= 0)`.

The database-level constraint is the safety net. If a bug in the controller skips validation, or if someone modifies the database directly (bypassing the API), the CHECK constraint prevents data corruption. Double enforcement at the input and database levels is not redundancy — it's defense in depth. Input validation can be buggy. SQL constraints cannot.

We considered removing the database constraint to simplify schema migrations, but decided against it. The stock non-negative invariant is fundamental to inventory accuracy. A schema migration that temporarily removes this constraint is inviting data corruption. The cost of adding it back later (migrating rows where stock went negative) is much higher than the cost of maintaining it from the start.

### Why page refresh for cross-device sync

Two devices on the same network should see the same stock data. The simplest way to achieve this in v1 is: writes go to the server (single source of truth), and reads get the latest data from the server. If device A updates stock, device B must reload the page (or re-fetch the product) to see the change.

We chose not to implement real-time sync (WebSocket, Server-Sent Events, or polling) because:

- **WebSocket** adds a persistent connection, reconnection logic, and state management on both client and server. For a v1 app with 2-5 concurrent staff, this is disproportionate complexity.
- **Polling** (client fetches stock on a timer) adds network traffic and battery drain for minimal benefit — staff rarely look at the same product simultaneously.
- **Shared state via local storage** (BroadcastChannel API) only works on the same browser — no cross-device benefit.

Page refresh is the v1 sync mechanism. It works, it's simple, and it's what staff already expect: "I updated stock on the tablet, but the phone still shows the old number — let me refresh." If the same-product-same-time scenario becomes a pain point (e.g., two staff members doing adjacent cycle counts), we can add a lightweight polling mechanism — e.g., re-fetch the product data every 60 seconds when a detail card is open. For v1, YAGNI applies.

---

## Error Handling

### Why standardized JSON errors

Every API error response looks the same:

```json
{ "error": "not_found", "message": "Product with barcode 123 not found" }
```

Two fields: `error` (machine-readable snake_case code) and `message` (human-readable description). No stack traces. No extra fields. No variation between endpoints.

This consistency means the frontend can write one error handler that works for every API call. The error code determines what to show to the user and whether to retry. The message is safe to display directly (no stack traces, no SQL, no server paths).

The alternative — per-endpoint error formats — would force every frontend component to understand the specific error shape of the endpoint it calls. This is fragile, hard to test, and leads to "error not caught" bugs where an unexpected format crashes the UI.

### Why 401/403 separation

401 (Unauthorized) and 403 (Forbidden) serve different purposes:

- **401** means "I don't know who you are." The JWT is missing, expired, or malformed. The solution is to log in again.
- **403** means "I know who you are, but you can't do this." The JWT is valid, but the user's role doesn't have scope for this operation. The solution is different: the user needs a role upgrade, not a new login.

The frontend reacts differently to each:
- **401** → clear token, redirect to login.
- **403** → show a message ("You don't have permission to do this") without clearing the token. The user may still navigate to other views.

We also use 403 for deactivated accounts, with a distinct error code `account_disabled`. This allows the login page to show a different message ("Contact your manager") than the generic "Invalid credentials" message — without revealing whether the account exists before authentication.

The separation prevents confusing error flows. If a manager gets 403 on a product endpoint, the app should not log them out — they can still manage users. If they get 401 (e.g., token expired), the app should log them out and redirect to login. These are different problems requiring different user actions.

---

## Constraints & Tradeoffs

### Why WAL mode for SQLite

WAL (Write-Ahead Logging) mode allows concurrent reads while a write is in progress. In the default rollback journal mode, a write locks the database, blocking all readers until the write completes. For most SQLite use cases this is fine — writes are fast (<1ms). But Makmur has a specific workflow where it matters:

Stock check: Staff member A opens a product detail card (read) while staff member B updates that same product's stock (write). In rollback journal mode, A's read would wait for B's write to complete. In WAL mode, A's read proceeds immediately because WAL readers can read the old committed state while a new write is in progress on the WAL file.

For a single-store deployment with 2-5 concurrent staff, rollback journal mode would probably work fine in practice — the write contention window is tiny. But WAL mode is a simple `PRAGMA` statement that eliminates the risk entirely with zero ongoing cost. There's no reason not to enable it.

### Why state-based routing, not React Router

The app uses a `currentView` state variable in `App.tsx` to switch between views: home, scan, detail, create, users, setup. No React Router. No URL-based routing.

Why not React Router? Because the app has only 6 views, and none of them need deep linking or browser back-button integration in v1. Staff don't bookmark product detail pages. They don't share URLs to specific products. They open the app, scan, look, and move on. Browser history for scanned products would be confusing at best.

React Router would add ~15KB to the bundle, a dependency to manage, and an indirection layer between the view state and the component tree. For a v1 app where the view transition logic is straightforward (scan → detail, tap back → scan, tap product → detail), a simple switch statement in one component is easier to read, debug, and modify.

If v2 needs URL-based sharing, browser history navigation, or deep links to specific products, React Router can be added then. The component structure already supports it: each view is a separate component that can be wrapped in a route. The migration cost is minimal because the views are already isolated.

### Why no delete — only deactivate

There is no endpoint to delete a user or a product in v1. Users can be deactivated (active flag set to 0). Products cannot be removed from the catalogue at all through the API — that requires a database-level operation.

Why no delete? Because deletion is irreversible and destructive. Deactivation preserves data integrity:

- A deactivated user cannot log in (same as deleted) but their historical actions (future feature) remain attributable.
- Deactivation can be undone. Accidental deactivation is a one-click fix. Accidental deletion requires a database restore.
- Deactivation avoids cascade problems. If future features relate user IDs to activity records, deleting a user would either orphan those records or require expensive cascade logic.

Products are not deleteable because removing a product from the catalogue would break the audit trail. If a product was scanned 300 times and then deleted, those scan records (if we add them in the future) would reference a nonexistent product. The catalogue becomes the permanent record of everything that has ever been in the inventory. Products that are no longer carried can be marked inactive rather than deleted.

The trade-off is that the product list may accumulate entries over time. For a single store with a finite product catalogue, this is acceptable. If the list grows unwieldy, we can add a visibility filter ("show active only") without needing a delete feature.

We'll reconsider delete functionality if (a) staff regularly create test products that need cleaning up, or (b) the catalogue accumulates enough inactive products to degrade search performance. Until then, deactivation is the safer default.
