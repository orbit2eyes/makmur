# Makmur v1 (makmur-0) — Design Document

## Related Specifications
This design document implements and is informed by the following domain specifications:

- **Auth Spec** (`specs/auth/spec.md`): The authentication and authorization mechanisms described in this design document (JWT-based auth, role-based access control, session management) directly implement the requirements outlined in the auth spec, particularly REQ-auth-001 through REQ-auth-014.

- **Product Spec** (`specs/product/spec.md`): The product-related API endpoints, data models, and UI components described in this design fulfill the product management requirements outlined in the product spec, including REQ-product-001 through REQ-product-005 and the associated scenarios.

- **Stock Spec** (`specs/stock/spec.md`): The stock update mechanisms, validation rules, and UI components for stock management described in this design implement the stock requirements from the stock spec, including REQ-stock-001 through REQ-stock-006.

- **Scan Spec** (`specs/scan/spec.md`): The barcode scanning pipeline, camera handling, and scan result routing described in this design fulfill the scanning requirements from the scan spec, including REQ-scan-001 through REQ-scan-008.

- **Product Requirements Document** (`prd.md`): This design document implements the technical approach to fulfill all functional and non-functional requirements outlined in the PRD, particularly the architecture, API design, database choices, and technology selections that enable the user journeys and acceptance criteria described in the PRD.

- **User Stories** (`user-stories.md`): The implementation details in this design support the user stories across all roles (admin, manager, staff), particularly enabling the core workflows for scanning, stock management, product creation, and user management described in the user stories.

## 1. Architecture Overview

### System Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          Browser Client (React SPA)                      │
│                                                                          │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────────────────┐  │
│  │ Camera Stream │───>│ Barcode Detector │───>│ Product Detail Card  │  │
│  │ (getUserMedia)│    │ (BarcodeDetector │    │ (name, stock, price) │  │
│  └──────────────┘    │  API / zxing-js) │    └───────────────────────┘  │
│                      └──────────────────┘               │               │
│                               │                          │               │
│                               │ 13-digit EAN-13          │ product data  │
│                               ▼                          ▼               │
│                      ┌──────────────────┐    ┌───────────────────────┐  │
│                      │  Barcode Lookup  │───>│ Product List / Search │  │
│                      │  (GET /products/ │    │ (GET /products/search)│  │
│                      │   :barcode)      │    └───────────────────────┘  │
│                      └──────────────────┘               │               │
│                              │                           │               │
│                              │ HTTP (fetch)              │ HTTP (fetch)  │
│                              ▼                           ▼               │
│                      ┌──────────────────────────────────────────────┐   │
│                      │         REST Client (fetch API)              │   │
│                      │  POST /products  PATCH /products/:barcode   │   │
│                      │  /stock                                     │   │
│                      └──────────────────────────────────────────────┘   │
└──────────────────────────────────┬───────────────────────────────────────┘
                                   │ HTTP (JSON) — local network
                                   ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     Java / Spring Boot Server                            │
│                                                                          │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────────────────┐  │
│  │  Static Files │    │   REST Controller│    │   ProductRepository  │  │
│  │  (Vite build) │    │  /api/products   │───>│ (CRUD via Spring     │  │
│  └──────────────┘    │  /api/health      │    │  Data JDBC)          │  │
│                      └────────┬─────────┘    └──────────┬────────────┘  │
│                               │                          │               │
│                               │                          ▼               │
│                               │                ┌────────────────────┐   │
│                               │                │ SQLite via JDBC    │   │
│                               └────────────────> (org.xerial:       │   │
│                                                │  sqlite-jdbc)      │   │
│                                                └────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘
```

### Module Responsibilities

| Module / File | Role |
|---------------|------|
| `client/src/main.tsx` | React entry point — mounts the app, initialises routing |
| `client/src/App.tsx` | Top-level component — manages view state (scan, list, detail, create) |
| `client/src/components/Viewfinder.tsx` | Camera viewfinder — manages `getUserMedia` stream, renders live preview |
| `client/src/components/BarcodeDecoder.tsx` | Barcode detection — wraps `BarcodeDetector` API or zxing-js fallback, emits decoded string |
| `client/src/components/ProductCard.tsx` | Product detail display — name, barcode, stock, price, action buttons |
| `client/src/components/StockControls.tsx` | Stock update UI — absolute value input, +1/-1 buttons, validation |
| `client/src/components/ProductForm.tsx` | Product creation form — name, price, stock fields with validation |
| `client/src/components/ProductList.tsx` | Scrollable product catalogue — renders row per product |
| `client/src/components/SearchBar.tsx` | Debounced search input — emits query string for filtering |
| `client/src/components/ManualEntry.tsx` | Manual barcode text input — fallback when camera is unavailable/denied |
| `client/src/api.ts` | REST client — wraps fetch calls to all `/api/*` endpoints |
| `server/src/main/java/com/makmur/Application.java` | Spring Boot entry point — port 3001, CORS, static file serving |
| `server/src/main/java/com/makmur/entity/Product.java` | Product entity — JPA-style mapping for SQLite |
| `server/src/main/java/com/makmur/repository/ProductRepository.java` | Spring Data JDBC repository — barcode lookup, search, sorted list |
| `server/src/main/java/com/makmur/controller/ProductController.java` | Product REST controller — CRUD + search + stock update |
| `server/src/main/java/com/makmur/controller/HealthController.java` | Health check — `GET /api/health` → `{status: "ok"}` |
| `server/src/main/java/com/makmur/config/WebConfig.java` | CORS config + static resource handler |
| `server/src/main/resources/schema.sql` | SQLite schema — products table, indexes, WAL mode |

---

## 2. Component Tree (React)

```
<App>
 ├── <NavigationBar />          — Scan button, logo/title
 ├── <HomeScreen>
 │    ├── <ProductList>
 │    │    ├── <SearchBar />
 │    │    └── <ProductRow />    — repeated per product
 │    └── <ScanButton />        — FAB / prominent scan trigger
 │
 ├── <ScanScreen>
 │    ├── <Viewfinder />
 │    │    ├── Camera preview (<video>)
 │    │    ├── Barcode overlay highlight
 │    │    └── <ManualEntry />  — fallback text input
 │    └── <ScanResult>          — conditionally rendered after decode
 │         ├── <ProductCard>
 │         │    ├── Name, barcode, stock, price
 │         │    ├── <StockControls>
 │         │    │    ├── Absolute value input
 │         │    │    ├── +1 button
 │         │    │    └── -1 button
 │         │    └── Action buttons (Scan Another)
 │         └── <CreatePrompt>   — shown when barcode unknown
 │              └── <ProductForm>
 │                   ├── Name field
 │                   ├── Price field
 │                   └── Stock field
 │
 └── <ProductDetailScreen>
      └── <ProductCard>
           ├── Name, barcode, stock, price
           └── <StockControls>
```

**Navigation pattern:** Single-page with state-based view switching (no router library needed for v1). App state holds a `currentView` key: `"home"`, `"scan"`, `"detail"`, or `"create"`. The view transition is driven by scan results, button taps, and confirmations.

---

## 3. API Design

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/health` | Health check — returns `{status: "ok"}` |
| `GET` | `/api/products` | List all products, sorted alphabetically by name |
| `GET` | `/api/products/search?q=<query>` | Search products by partial name match (case-insensitive substring) |
| `GET` | `/api/products/:barcode` | Lookup a single product by EAN-13 barcode |
| `POST` | `/api/products` | Create a new product |
| `PATCH` | `/api/products/:barcode/stock` | Update stock count (absolute or delta) |

### Controller Structure (Java)

```
ProductController.java
├── @GetMapping("/api/products")              → listAll()
├── @GetMapping("/api/products/search")       → search(@RequestParam q)
├── @GetMapping("/api/products/{barcode}")    → getByBarcode(@PathVariable barcode)
├── @PostMapping("/api/products")             → create(@RequestBody)
└── @PatchMapping("/api/products/{barcode}/stock") → updateStock(@PathVariable, @RequestBody)

HealthController.java
└── @GetMapping("/api/health")                → health()
```

### Request / Response Shapes

**GET /api/health**

```
Response 200:
{
  "status": "ok"
}
```

**GET /api/products**

```
Response 200:
[
  {
    "id": 1,
    "barcode": "5901234567890",
    "name": "Fresh Milk 1L",
    "price": 12.50,
    "stock": 42,
    "created_at": "2026-07-01T10:00:00.000Z"
  },
  ...
]
```

**GET /api/products/search?q=mil**

```
Response 200:
[
  {
    "id": 1,
    "barcode": "5901234567890",
    "name": "Fresh Milk 1L",
    "price": 12.50,
    "stock": 42,
    "created_at": "2026-07-01T10:00:00.000Z"
  },
  {
    "id": 2,
    "barcode": "5901234567891",
    "name": "Milk Chocolate Bar",
    "price": 8.00,
    "stock": 120,
    "created_at": "2026-07-01T10:05:00.000Z"
  }
]

Response 200 (empty):
[]

Note: q query param is required. Minimum 2 characters to avoid broad matches.
```

**GET /api/products/5901234567890**

```
Response 200:
{
  "id": 1,
  "barcode": "5901234567890",
  "name": "Fresh Milk 1L",
  "price": 12.50,
  "stock": 42,
  "created_at": "2026-07-01T10:00:00.000Z"
}

Response 404:
{
  "error": "not_found",
  "message": "Product with barcode 5901234567890 not found"
}
```

**POST /api/products**

```
Request body:
{
  "barcode": "5901234567890",
  "name": "Fresh Milk 1L",
  "price": 12.50,
  "stock": 0
}

Response 201:
{
  "id": 1,
  "barcode": "5901234567890",
  "name": "Fresh Milk 1L",
  "price": 12.50,
  "stock": 0,
  "created_at": "2026-07-01T10:00:00.000Z"
}

Response 409 (duplicate barcode):
{
  "error": "duplicate_barcode",
  "message": "Product with barcode 5901234567890 already exists",
  "existing_product": { ... }
}

Response 422 (validation error):
{
  "error": "validation_error",
  "fields": {
    "name": "Product name is required",
    "price": "Price must be a positive number"
  }
}
```

**PATCH /api/products/5901234567890/stock**

```
Request body (absolute):
{
  "value": 15
}

Request body (delta):
{
  "delta": 1
}

Request body (delta decrement):
{
  "delta": -1
}

Response 200:
{
  "barcode": "5901234567890",
  "stock": 15,
  "previous_stock": 10
}

Response 200 (delta):
{
  "barcode": "5901234567890",
  "stock": 11,
  "previous_stock": 10
}

Response 422:
{
  "error": "validation_error",
  "fields": {
    "stock": "Stock cannot be negative"
  }
}

Note: `value` and `delta` are mutually exclusive. If both provided, `value`
takes precedence. If neither, 422. `delta` may be positive or negative;
negative delta that would drive stock below 0 returns 422.
```

---

## 4. Database Schema

```sql
-- Enable WAL mode for better concurrent read performance
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS products (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode    TEXT    NOT NULL UNIQUE,
    name       TEXT    NOT NULL,
    price      REAL    NOT NULL CHECK(price >= 0.01),
    stock      INTEGER NOT NULL DEFAULT 0 CHECK(stock >= 0),
    created_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

-- Index for fast barcode lookups (GET /api/products/:barcode)
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode);

-- Index for case-insensitive name search (LIKE / COLLATE NOCASE)
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name COLLATE NOCASE);
```

**Schema notes:**

- `barcode` is stored as `TEXT` (not `INTEGER`) because EAN-13 can have leading zeros and is semantically an identifier, not a number.
- `price` is `REAL` (floating point). For a single-currency retail app at v1 scale, this is sufficient. If currency precision becomes critical (e.g., tax calculations), migrate to integer cents.
- `CHECK(stock >= 0)` is a database-level safety net — duplicates the input validation on the client and server.
- `created_at` uses ISO 8601 text format. SQLite has no native datetime type; text is the most portable and debuggable format for v1.
- WAL mode is enabled at startup to allow concurrent reads while a write is in progress — important when multiple staff devices are querying during a stock update.

---

## 5. Data Flow

### Scan Flow

```
Camera stream ──> Frame capture (every N ms)
                    │
                    ▼
              BarcodeDetector API / zxing-js
                    │
                    ├── No barcode detected ──> Continue frame capture
                    │
                    └── EAN-13 decoded (e.g. "5901234567890")
                              │
                              ▼
                    GET /api/products/5901234567890
                              │
                    ┌─────────┴──────────┐
                    ▼                    ▼
               200 OK                404 Not Found
              (product exists)      (unknown barcode)
                    │                    │
                    ▼                    ▼
            Show ProductCard       Show CreatePrompt
            (name, stock, price)   ("Add to catalogue?")
                    │                    │
                    ▼                    ▼
            Staff action:           Staff taps "Add Product":
            • Update Stock          ProductForm with barcode
            • Scan Another          pre-filled. On save:
              → return to             POST /api/products → 201
                viewfinder            → redirect to ProductCard
                                    • Cancel → return to viewfinder
```

**Key properties:**
- Camera stream stays active across consecutive scans — no reinitialisation.
- Scan result replaces previous result; there is no scan history in v1.
- A brief success indicator (vibration or visual flash) fires on decode before transitioning to the result view.

### Stock Update Flow

```
Staff on ProductCard
       │
       ├── Taps +1 ──> PATCH /api/products/:barcode/stock { delta: 1 }
       │                   │
       │                   ▼ 200 OK { stock: 11 }
       │              Update local state → UI refreshes → flash indicator
       │
       ├── Taps -1 ──> PATCH /api/products/:barcode/stock { delta: -1 }
       │                   │
       │                   ├── stock > 0 ──> 200 OK
       │                   │                 UI refreshes
       │                   └── stock = 0 ──> Button disabled at UI level
       │                                     (server also rejects with 422)
       │
       └── Taps "Update Stock" ──> Opens numeric input
                │
                ├── Enters "15", taps Confirm
                │       │
                │       ▼
                │   Validate: 15 >= 0 and integer
                │       │
                │       ▼
                │   PATCH /api/products/:barcode/stock { value: 15 }
                │       │
                │       ▼ 200 OK
                │   UI refreshes → flash indicator → dismisses input
                │
                └── Enters "-5", taps Confirm
                        │
                        ▼
                    Validation error: "Stock cannot be negative"
                    Input stays open for correction
```

**Key properties:**
- Every stock write round-trips to the server — no optimistic UI updates. For v1 latency on local network (<10ms), the trade-off favours correctness over perceived speed.
- `previous_stock` in the response allows the client to animate a transition if desired.

### Search Flow

```
Staff focuses search bar on product list view
       │
       ▼
Types characters: "mil"
       │
       ▼
Debounce ~200ms after last keystroke
       │
       ▼
GET /api/products/search?q=mil
       │
       ▼
Server query: SELECT * FROM products
              WHERE name LIKE '%mil%'
              COLLATE NOCASE
              ORDER BY name ASC
       │
       ▼
Response 200: [matching products]
       │
       ▼
ProductList re-renders with filtered results
       │
       ▼
Staff taps Clear / backspace query to empty → GET /api/products (full list)
```

**Key properties:**
- `LIKE '%query%'` with `COLLATE NOCASE` is substring case-insensitive matching — simple and sufficient for v1 catalogues up to 5000 products.
- Debounce prevents a request per keystroke. 200ms is short enough to feel instantaneous but aggressive enough to cut excessive requests.
- Search is server-side, not client-side filter. The SQLite index ensures sub-500ms response time for 1000-product catalogues (AC-10).

---

## 6. Key Design Decisions

### SQLite over PostgreSQL

**Decision:** Use server-side SQLite (via JDBC / org.xerial:sqlite-jdbc) instead of PostgreSQL.

**Rationale:**
- Zero configuration — no daemon, no user creation, no port management. The database is a single file on disk. Start the Spring Boot server and the database is ready.
- Single-process model — SQLite via JDBC runs embedded in the JVM, which is simpler to reason about than connection pooling.
- Storage fits the scale — a single retail store's product catalogue (hundreds to low thousands of products) is comfortably within SQLite's sweet spot.
- Backup is a file copy — no pg_dump, no replication setup.
- PostgreSQL would be justified if we needed concurrent write-heavy workloads, row-level security, or multi-region replication. None of these apply to v1.

**Trade-off:** No concurrent writers. All write operations are serialised through the single JVM process. For a handful of staff on a local network making occasional stock updates, this is orders of magnitude below SQLite's practical ceiling (tens of thousands of writes per second in WAL mode).

### Spring Boot over Express

**Decision:** Use Spring Boot 3.x (Java) instead of Node.js/Express.

**Rationale:**
- Type safety — Java's static typing catches entire classes of bugs at compile time (wrong field names, type mismatches, null pointer paths) that would be runtime errors in JavaScript.
- JDBC + SQLite — the `org.xerial:sqlite-jdbc` driver is mature, well-tested, and requires zero configuration. Spring Data JDBC provides a clean repository abstraction over raw SQL.
- Deployment simplicity — a single fat JAR with embedded Tomcat runs the whole server. No Node.js runtime to manage, no npm audit, no transitive dependency surprises.
- Performance — Spring Boot with embedded Tomcat handles concurrent requests efficiently with a thread-per-request model. For this app's scale (handful of staff, local network), the performance characteristics are indistinguishable from Express, but the type safety and tooling (IDE, debugging, profiling) are significantly better.
- Ecosystem — Spring Boot's validation, error handling, and testing infrastructure (`@WebMvcTest`, `@DataJdbcTest`) are production-grade and well-documented.

**Trade-off:** Higher initial setup cost compared to Express (Maven/Gradle, Java compilation, more boilerplate). For v1, the extra upfront cost is justified by long-term maintainability and fewer runtime surprises.

### Server-side SQLite over Client-side WASM (sql.js)

**Decision:** Run SQLite on the Java server, not in the browser via WASM.

**Rationale:**
- Multi-device data sharing is the primary requirement — three staff phones must see the same stock counts. A client-side WASM database would require a sync protocol (or each device having a private copy), reintroducing the server complexity we avoided.
- Server-side access control (even if minimal in v1) is simpler — the server mediates all reads and writes. A WASM database shared over the network would need a custom file-locking mechanism.

**Trade-off:** Requires network connectivity. Staff devices must reach the server. A client-side-only solution would work offline but cannot share data. For v1, shared inventory data is the core feature — the trade-off is correct.

### REST over GraphQL

**Decision:** Use REST endpoints rather than GraphQL.

**Rationale:**
- The data model is small (one entity: Product). There is no graph of related objects to traverse.
- Number of endpoints is low (5). The learning cost and dependency weight of Apollo/Relay far exceed the benefit.
- REST is debuggable with curl, works over plain HTTP, and has zero client-side library requirements.
- If the API grows in future versions (purchase orders, suppliers, transactions), GraphQL can be added alongside REST or as a replacement. YAGNI for v1.

**Trade-off:** Over-fetching on the product list endpoint (returns all fields when the client may only need name and stock). At this scale, the extra bytes per response are negligible.

### Continuous Scan as Default UX

**Decision:** Continuous scan mode is the default — after showing the result for 2 seconds, the viewfinder reactivates automatically. A toggle in settings can disable auto-return.

**Rationale:**
- Warehouse staff scanning dozens or hundreds of items in a session should not tap between each scan. Every extra tap per scan is friction multiplied by scan count.
- 2-second result display is long enough to read the stock count but short enough to keep the pace.
- The "Scan Another" button is always visible for immediate return — the auto-return is a convenience, not a lock-in.

**Trade-off:** Staff who scan one item at a time and need to study the result may find auto-return disruptive. The manual toggle and the persistent "Scan Another" button mitigate this. First-time users get an onboarding hint: "Auto-return is on — tap a result to pause scanning."

### BarcodeDetector API vs zxing-js Polyfill

**Decision:** Use the built-in `BarcodeDetector` API with a zxing-js polyfill as fallback for browsers that do not support it (Safari, Firefox).

**Rationale:**
- Chrome and Chromium-based browsers (most common on Android devices and desktops) ship `BarcodeDetector` with native hardware-accelerated barcode decoding. Using it means zero dependency weight on those devices.
- Safari and Firefox do not support `BarcodeDetector` (as of mid-2026). zxing-js is a pure JS decoder that works in all modern browsers. It is slower and less accurate but functional.
- The detection path is: `if ('BarcodeDetector' in window) → native, else → zxing-js`. The client checks once at startup and uses the chosen path for the session.
- zxing-js adds ~100KB gzipped — loaded only on browsers that need it (lazy import).

**Trade-off:** Decoding accuracy and speed vary between native and polyfill. On Safari iPhones (a common staff device), zxing-js may be ~2x slower than Chrome native decoding. Still within the 3-second acceptance criterion for well-lit, undamaged barcodes. If accuracy on Safari proves insufficient, a future version may add a native camera capture → server-side decode path using a compiled ZBar binary.

---

## 7. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Browser camera API support** — `getUserMedia` unavailable in older browsers or restricted (HTTP vs HTTPS) | Medium | High — scanning pipeline cannot start | Detect API availability at startup. Show clear fallback with manual barcode entry field. Run the server with HTTPS (self-signed cert for local network) to satisfy `getUserMedia` secure-context requirement. |
| **Barcode decode accuracy** — poor lighting, damaged labels, motion blur cause false negatives | Medium | Medium — staff re-scan or enter manually | Non-blocking retry prompt — viewfinder stays active, no restart. Manual barcode entry as permanent fallback below the viewfinder. The 3-second timeout per scan attempt prevents infinite loops. |
| **SQLite write contention** — multiple devices updating stock simultaneously | Low | Low — brief blocking during writes | WAL mode allows concurrent reads during writes. One JVM process serialises writes anyway — contention window is <1ms. No mitigation needed for v1. Monitor in production; add a write queue only if contention appears. |
| **Duplicate barcode creation** — staff re-creates an existing product | Medium | Medium — data duplication, confusion | Server-side guard: `POST /api/products` checks barcode uniqueness before insert. Returns 409 with existing product data on conflict. Client-side check: after scan, the already-existing barcode route (GET → 200) never reaches the creation flow (GET → 404). Only truly unknown barcodes enter creation. |
| **Accidental negative stock from concurrent -1 taps** — two staff tap -1 at the same time on stock of 1 | Low | Low — stock goes to -1 temporarily | Server validates `stock >= 0` before update and returns 422 if violated. The second -1 tap receives an error. The UI shows the updated stock (0) after the first write. The second staff member sees the error and the correct current value. |
| **Client-server version mismatch** — browser caches old frontend after server updates | Low | Medium — API contract breakage | Serve the Vite build as static files from the same server. The API version is implicitly tied to the frontend build. Add a `Cache-Control: no-cache` header on `index.html` to force fresh load on each page open. |

---

## 8. Verification Hooks

### Product Domain

| Criterion | Verification Method |
|-----------|-------------------|
| AC-06: Product creation validates name (non-empty), price (positive number), stock (non-negative integer) | Automated — unit test on `POST /api/products` with invalid payloads returns 422. Automated — client-side form validation test (React Testing Library). |
| AC-07: Saved products appear immediately in product list and are searchable | Automated — integration test: `POST /api/products` then `GET /api/products` returns the new product. Manual — visual confirmation that list re-renders. |
| AC-10: Partial name search returns results within 500ms for 1000 products | Automated — insert 1000 test products, time 10 search queries, assert p50 < 500ms. |
| AC-11: Product list renders without lag for 5000 products | Automated — render 5000 rows in virtualised list, measure initial render and scroll frame rate (Playwright). |
| AC-12: Usable on 375px and 1280px viewports | Automated — Playwright viewport test: assert all key elements visible and not overlapping at both widths. |

### Scan Domain

| Criterion | Verification Method |
|-----------|-------------------|
| AC-02: Camera permission request on first access; denial shows error + manual entry | Manual — open app on a phone, deny camera prompt, verify error message and manual entry field appear. |
| AC-03: EAN-13 barcode decoded within 3 seconds | Automated — Playwright test with synthetic camera feed (pre-recorded EAN-13 image) measures decode-to-result time. Manual — real-world test with printed barcodes and various phones. |
| AC-04: Known barcode shows product detail within 1 second of decode | Automated — integration test: seed product, mock barcode detection, assert product card renders within 1s. |
| AC-05: Unknown barcode shows creation prompt with pre-filled barcode | Automated — integration test: mock detection of unknown barcode, assert creation prompt renders with correct barcode. |
| AC-13: Consecutive scan round-trip under 5 seconds | Automated — Playwright test measures 3 consecutive scan cycles (mock decode → result → auto-return → decode). Manual — real-device timing with stopwatch. |

### Stock Domain

| Criterion | Verification Method |
|-----------|-------------------|
| AC-08: Stock update persists across page reload | Automated — integration test: `PATCH /api/products/:barcode/stock` then `GET /api/products/:barcode` returns updated stock. Manual — reload browser and verify displayed stock. |
| AC-09: Stock never goes below 0; -1 button disabled at 0 | Automated — unit test on PATCH endpoint with `delta: -1` at stock 0 returns 422. Automated — React test verifies -1 button disabled when stock is 0. |
| AC-14: Two devices see same data; change on one visible on other after refresh | Manual — open app on two phones, update stock on one, refresh the other, verify same value. |

### Network / System

| Criterion | Verification Method |
|-----------|-------------------|
| AC-01: Any device on local network can open the app without login | Manual — open `http://<server-ip>:<port>` on a second device, verify home screen loads without auth prompt. |

---

## Open Questions Carried Forward

The following PRD open questions inform the design above and are considered settled for v1:

| # | Question | Design Answer |
|---|----------|--------------|
| Q1 | Continuous scan default? | Yes — auto-return to viewfinder after 2s result display. Toggle to disable. |
| Q2 | Absolute or delta stock updates? | Both — `PATCH /stock` accepts `value` (absolute) or `delta` (+1/-1). Mutually exclusive in one request. |
| Q3 | Max catalogue size? | No hard limit in schema. Virtualised rendering and index-backed search handle 5000+ products. Tested up to 5000 for AC-11. |
| Q4-7 | Photos, low-stock filter, damaged barcodes, delete product? | Deferred — not in v1 scope. See non-goals in PRD. |

---

# 9. Authorization & Access Control Design

## 9.1 Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                           Browser Client (React SPA)                             │
│                                                                                  │
│  ┌─────────────────┐   ┌──────────────────┐    ┌────────────────────────────┐   │
│  │  Login Page      │   │  AuthContext      │    │  ProtectedRoute wrappers  │   │
│  │  /setup?token=X  │──>│  JWT in           │───>│  (role-based view gate)   │   │
│  │  QR Setup Page   │   │  sessionStorage   │    └────────────────────────────┘   │
│  └─────────────────┘   └──────────────────┘                 │                   │
│                                                                │                   │
│  ┌─────────────────────────────────────────────────────────┐   │                   │
│  │  api.ts interceptor — Authorization: Bearer <token>    │<──┘                   │
│  └─────────────────────────────────────────────────────────┘                       │
│                                                                 │                   │
│  ┌──────────────────┐    ┌──────────────────┐    ┌────────────────────────────┐   │
│  │  Staff View       │    │  Manager View     │    │  Admin View                │   │
│  │  (product/scan)   │    │  (user mgmt only) │    │  (products + user mgmt)    │   │
│  └──────────────────┘    └──────────────────┘    └────────────────────────────┘   │
└──────────────────────────────────┬────────────────────────────────────────────────┘
                                   │ HTTP (JSON) — Authorization: Bearer <JWT>
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      Java / Spring Boot Server                                    │
│                                                                                  │
│  ┌──────────────┐    ┌───────────────────────────────┐    ┌──────────────────┐   │
│  │ JwtAuthFilter│───>│     SecurityContextHolder      │    │  ProductController│   │
│  │ (OncePerReq) │    │ set auth with role authorities │───>│  StockController  │   │
│  └──────────────┘    └───────────────────────────────┘    │  ScanController   │   │
│         │                                                   └────────┬─────────┘   │
│         │  ┌──────────────┐                                       │               │
│         └──│  JwtUtil     │                                       │               │
│            │  (env secret) │                                       │               │
│            └──────────────┘                                       │               │
│                                                                    │               │
│  ┌──────────────┐    ┌──────────────────┐    ┌────────────────────┐               │
│  │ AuthController│   │  UserController   │    │  SetupController   │               │
│  │ /api/auth/    │   │  /api/users       │    │  /api/setup/       │               │
│  │  - login      │   │  - list (mgmt)    │    │  - token (QR gen)  │               │
│  │  - register   │   │  - create (mgmt)  │    │  - register (admin)│               │
│  └──────┬────────┘   │  - deactivate     │    └────────────────────┘               │
│         │            │  - reset password │                                        │
│         ▼            └────────┬──────────┘                                        │
│  ┌─────────────────────────────────────────────────────────────────┐              │
│  │  UserRepository / ProductRepository — SQLite via Spring Data JDBC│              │
│  └─────────────────────────────────────────────────────────────────┘              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### Module Map — New & Modified

| Module / File | Status | Role |
|---------------|--------|------|
| `server/.../config/JwtUtil.java` | **MODIFY** | Read `JWT_SECRET` env var, embed `role` + `userId` in claims. Currently hardcodes secret and omits role. |
| `server/.../config/JwtAuthenticationFilter.java` | **MODIFY** | Add `active`-flag check against DB; pass `userId` into principal details. |
| `server/.../config/SecurityConfig.java` | **MODIFY** | Add method-level role guards (`@PreAuthorize` or manual) for user-management endpoints; keep `/api/auth/**` and `/api/health` public. |
| `server/.../controller/AuthController.java` | **MODIFY** | Add `active`-flag check during login; remove client-controlled `role` field in register (hard-code server-side). |
| `server/.../controller/UserController.java` | **ADD** | GET `/api/users` (list, scoped by role), POST (create, hard-coded role), PATCH deactivate/reactivate, PATCH reset-password. |
| `server/.../controller/SetupController.java` | **ADD** | QR onboarding flow: GET `/api/setup/token` → one-time UUID, GET `/api/setup/qr` → QR image, POST `/api/setup/register` → create first admin. |
| `server/.../entity/User.java` | **MODIFY** | Rename field `password` → `passwordHash`; add `active` field (boolean). |
| `server/.../repository/UserRepository.java` | **MODIFY** | Add `findAll()`, `updateActiveStatus()`, `updatePassword()`, scoped queries. |
| `server/.../controller/ProductController.java` | **MODIFY** | Add handler-level scope check — reject requests from `manager` role with 403. |
| `server/.../Application.java` | **MODIFY** | Remove default admin seed (superseded by QR onboarding). Add startup check: if no users exist, print setup instructions to log. |
| `server/.../resources/schema.sql` | **MODIFY** | `password` → `password_hash`, add `active` column, extend `role` CHECK to include `'manager'`. |
| `client/src/context/AuthContext.tsx` | **MODIFY** | Add JWT expiry detection, role-aware `user` type. |
| `client/src/components/Login.tsx` | **MODIFY** | Add deactivated-account error display; cleanup styling. |
| `client/src/components/Sidebar.tsx` | **MODIFY** | Role-based nav items (staff → products/scan; manager → users; admin → both). |
| `client/src/components/SetupPage.tsx` | **ADD** | QR display when no admin exists; setup form when token in URL. |
| `client/src/components/UserList.tsx` | **ADD** | Manager-admin view: user table, create/deactivate/reset buttons. |
| `client/src/App.tsx` | **MODIFY** | Role-based routing: staff → products; manager → user mgmt; admin → dashboard. |
| `client/src/api.ts` | **MODIFY** | Add 401 interceptor → auto-redirect to login; add user-management API calls. |

---

## 9.2 API Surface — Auth Endpoints

### New / Modified Endpoints

| Method | Path | Auth Required | Roles | Description |
|--------|------|---------------|-------|-------------|
| `POST` | `/api/auth/login` | No | All | Authenticate, return JWT + user payload |
| `POST` | `/api/auth/register` | Yes | admin, manager | Create user (server hard-codes role; see §9.7) |
| `GET` | `/api/setup/token` | No | — | Return one-time setup token if no admin exists |
| `GET` | `/api/setup/qr` | No | — | Serve QR image encoding setup URL + token |
| `POST` | `/api/setup/register` | No | — | Create first admin; invalidate setup token |
| `GET` | `/api/users` | Yes | manager, admin | List users (scoped: manager sees only staff; admin sees all) |
| `PATCH` | `/api/users/:id/deactivate` | Yes | manager, admin | Set `active = false` (soft deactivation) |
| `PATCH` | `/api/users/:id/reactivate` | Yes | manager, admin | Set `active = true` |
| `PATCH` | `/api/users/:id/reset-password` | Yes | manager, admin | Set new password (manager can only reset staff) |

### Request / Response Shapes

**POST /api/auth/login**

```
Request:
{
  "username": "jane",
  "password": "secret123"
}

Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "jane",
    "role": "staff",
    "active": true
  }
}

Response 401:
{
  "error": "invalid_credentials",
  "message": "Invalid username or password"
}

Response 403 (deactivated):
{
  "error": "account_disabled",
  "message": "Account is deactivated. Contact your manager."
}

Response 422:
{
  "error": "validation_error",
  "message": "Username and password are required"
}
```

**POST /api/auth/register (user creation by manager/admin)**

```
Request:
{
  "username": "newstaff",
  "password": "tempPass123",
  "role": "staff"   ← ignored if caller is manager; admin can pass "staff" or "manager"
}

Response 201:
{
  "id": 3,
  "username": "newstaff",
  "role": "staff",
  "active": true,
  "created_at": "2026-07-08T12:00:00.000Z"
}

Response 409:
{
  "error": "duplicate_username",
  "message": "Username newstaff already exists"
}
```

**GET /api/setup/token**

```
Response 200 (no admin):
{
  "token": "a1b2c3d4-e5f6-...",
  "expires_at": "2026-07-08T13:00:00.000Z"
}

Response 403 (admin already exists):
{
  "error": "already_setup",
  "message": "Admin account already exists"
}
```

**GET /api/setup/qr**
- Returns `image/png` — QR encoding `http://<server>:<port>/setup?token=<uuid>`
- Only available when no admin user exists (same guard as GET /api/setup/token)

**POST /api/setup/register**

```
Request:
{
  "token": "a1b2c3d4-e5f6-...",
  "username": "admin",
  "password": "strongAdminPass123"
}

Response 201:
{
  "message": "Admin account created",
  "user": { "id": 1, "username": "admin", "role": "admin", "active": true }
}

Response 403:
{
  "error": "invalid_token",
  "message": "Setup token is invalid or expired"
}
```

**GET /api/users**

```
Response 200 (admin sees all):
[
  { "id": 1, "username": "admin", "role": "admin", "active": true, "created_at": "..." },
  { "id": 2, "username": "shiftlead", "role": "manager", "active": true, "created_at": "..." },
  { "id": 3, "username": "jane", "role": "staff", "active": false, "created_at": "..." }
]

Response 200 (manager sees only staff):
[
  { "id": 3, "username": "jane", "role": "staff", "active": false, "created_at": "..." },
  { "id": 4, "username": "john", "role": "staff", "active": true, "created_at": "..." }
]
```

**PATCH /api/users/:id/deactivate**

```
Request: (empty body)

Response 200:
{
  "id": 3,
  "username": "jane",
  "active": false
}

Response 403 (manager trying to deactivate another manager):
{
  "error": "forbidden",
  "message": "You can only deactivate staff accounts"
}
```

**PATCH /api/users/:id/reset-password**

```
Request:
{
  "new_password": "newTempPass456"
}

Response 200:
{
  "message": "Password updated"
}
```

---

## 9.3 Database Schema Changes

```sql
-- Modified users table — rename password column, add active, extend role CHECK

-- SQLite does not support DROP CHECK or ALTER COLUMN directly.
-- Migration strategy: recreate the table with the new schema,
-- copy existing data, then drop old table.

CREATE TABLE IF NOT EXISTS users_new (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL DEFAULT 'staff' CHECK(role IN ('admin', 'manager', 'staff')),
    active        INTEGER NOT NULL DEFAULT 1,
    created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

-- Migrate data if old users table exists with different schema
INSERT INTO users_new (id, username, password_hash, role, active, created_at)
SELECT id, username, password, role, 1, created_at FROM users;

-- After data migration, drop old table and rename new one
-- (Executed as a single migration step at startup or via migration tooling)
```

### Schema notes

- **`password_hash`** (renamed from `password`) — makes the storage format explicit. All writes to this column must be bcrypt-encoded strings, never plain text.
- **`active` (INTEGER NOT NULL DEFAULT 1)** — soft deactivation flag. `0` = disabled (cannot log in), `1` = active. Deactivation is the only supported action for ending access; there is no hard-delete endpoint in v1. This preserves referential integrity if future features relate user IDs to action records.
- **`role` CHECK extended** — now includes `'manager'` alongside `'admin'` and `'staff'`. The CHECK constraint is a database-level safety net; the application enforces role assignment logic server-side.
- **`products` table — unchanged.** No foreign key to users. No audit columns in v1 (deferred — see non-goals).

---

## 9.4 Data Flows

### Login → JWT → Filter → Handler Scope Check

```
                   ┌───────────────┐
                   │  Login Page   │
                   │  POST /auth/  │
                   │  login        │
                   └───────┬───────┘
                           │ { username, password }
                           ▼
                   ┌───────────────────┐
                   │  AuthController   │
                   │  .login()         │
                   └───────┬───────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        UserRepository  PasswordEncoder  JwtUtil
        .findByUsername  .matches()     .generateToken(
         (lookup user)   (bcrypt)        username, userId, role)
              │            │            │
              └────────────┴────────────┘
                           │
                           ▼
              Response: { token, user: { id, username, role } }
                           │
                           ▼
              Client stores JWT + user in sessionStorage
              AuthContext state updated
              Client navigates to role-appropriate home view
                            ···
              ─── On subsequent requests ───
                           │
              Client sends Authorization: Bearer <JWT>
                           │
                           ▼
              ┌───────────────────────┐
              │  JwtAuthenticationFilter │
              │  .doFilterInternal()   │
              │                        │
              │  1. Extract JWT from   │
              │     Authorization header│
              │  2. jwtUtil.validate() │
              │  3. Extract claims      │
              │     (sub, role, userId) │
              │  4. UserRepository      │
              │     .findByUsername()   │
              │  5. Check active flag   │
              │  6. Set SecurityContext │
              │     with role authority │
              └──────────┬────────────┘
                         │
                         ▼
              ┌───────────────────────┐
              │   ProductController   │
              │   (or any handler)    │
              │                        │
              │  Handler-level scope   │
              │  check:                │
              │                        │
              │  if (role == "manager")│
              │    return 403          │
              │                        │
              │  else → execute        │
              │  (staff / admin)       │
              └────────────────────────┘
```

### QR Onboarding Flow

```
Server starts — users table empty
            │
            ▼
  SetupController initializes:
  - Generate UUID v4 → one-time setup token
  - Store token in-memory (ConcurrentHashMap with TTL)
            │
            ▼
  GET /api/setup/qr  ─── QR image (PNG)
  GET /api/setup/token ── JSON { token, expires_at }
            │
            ▼
  First browser visit → detects no users in DB
  → Shows SetupPage with QR code (rendered via qrcode.js or server-served PNG)
            │
            ▼
  Admin scans QR with phone
  → Navigates to http://<server>:3001/setup?token=<uuid>
            │
            ▼
  SetupPage detects token in URL query param
  → Shows setup form: username + password + confirm
            │
            ▼
  POST /api/setup/register { token, username, password }
            │
            ▼
  SetupController:
  1. Validate token matches stored token
  2. Validate token not expired (TTL check)
  3. Validate no admin user exists yet (race-condition guard)
  4. Create user with role='admin' (bcrypt-hashed password)
  5. Invalidate token (set null in map)
  6. Return 201 + user info
            │
            ▼
  Client clears setup state, redirects to login page
  Token no longer valid — GET /api/setup/token returns 403
```

---

## 9.5 Scope Enforcement Pattern

### Handler-level scope (not route-level)

The PRD requires (AC-21) that scope checks happen **inside** the endpoint handler, not at the routing layer. This means:

- **One endpoint URL, one implementation** per operation. No controller method is duplicated for different roles.
- **The handler checks `SecurityContextHolder`** for the authenticated user's role.
- **If the caller's role does not match** the required scope, the handler returns `403 Forbidden` with `{ error: "forbidden", message: "..." }`.

### Scope Matrix

| Operation | staff | manager | admin |
|-----------|-------|---------|-------|
| GET /api/products (list) | ✅ | ❌ 403 | ✅ |
| GET /api/products/search | ✅ | ❌ 403 | ✅ |
| GET /api/products/:barcode | ✅ | ❌ 403 | ✅ |
| POST /api/products | ✅ | ❌ 403 | ✅ |
| PATCH /api/products/:barcode/stock | ✅ | ❌ 403 | ✅ |
| GET /api/users | ❌ 403 | ✅ (staff only) | ✅ (all) |
| POST /api/users (create) | ❌ 403 | ✅ (role=staff) | ✅ (any role) |
| PATCH /api/users/:id/deactivate | ❌ 403 | ✅ (staff only) | ✅ (any) |
| PATCH /api/users/:id/reset-password | ❌ 403 | ✅ (staff only) | ✅ (any) |
| GET /api/health | ✅ | ✅ | ✅ |

### Implementation approach

Two patterns are available; use **#1 (helper utility)** as the primary:

1. **`AuthService.requireRole(String... roles)` utility** — a static helper that reads the current `Authentication` from `SecurityContextHolder`, extracts the GrantedAuthority set, and throws `AccessDeniedException` (→ 403) if the caller's role doesn't match any of the permitted roles. Called at the top of each handler method.

   ```java
   // Inside ProductController.listAll():
   @GetMapping
   public List<Product> listAll() {
       authService.requireRole("admin", "staff");  // manager → 403
       return repo.findAllByOrderByNameAsc();
   }
   ```

2. **`@RequireRole` annotation** (optional) — a custom method-level annotation processed by an AOP interceptor. Defers the role check to annotation processing. More declarative but adds AOP complexity. Use the utility approach for v1; if handlers grow beyond 10+ methods, consider the annotation.

Neither pattern uses route-level guards — the same endpoint URL serves all roles, and the handler decides access.

### Scope enforcement on existing product/stock/scan endpoints

Each existing handler method in `ProductController` gains a single line at the top:

```java
authService.requireRole("admin", "staff");  // blocks manager
```

The `HealthController` endpoint is excluded from scope checks (public health check, already permitted in `SecurityConfig`).

### Scope enforcement on new user-management endpoints

`UserController` (new) uses `authService.requireRole("admin", "manager")` at the class level via a constructor or handler-interceptor pattern, plus additional internal role checks:

- **List users** — if caller is `manager`, filter results to `role='staff'` only. Admin sees all rows.
- **Create user** — if caller is `manager`, hard-code `role='staff'` server-side, ignoring any `role` value from the request body.
- **Deactivate/reactivate** — if caller is `manager`, verify the target user has `role='staff'`; reject if target is manager or admin.
- **Reset password** — same restriction: manager can only reset staff passwords.

---

## 9.6 Frontend Changes

### Component Tree (Auth-Augmented)

```
<App>
 ├── [No token] → <Login />                                ← redrawn: gate all views
 ├── [Token but no admin exists] → <SetupPage />            ← new: QR or setup form
 │    ├── <QRCodeDisplay />                                 ← new: server QRs or qrcode.js
 │    └── <SetupForm />                                     ← new: create first admin
 └── [Token + admin exists] → <MainApp />
      ├── <Sidebar />                                       ← modified: role-based nav
      │    ├── Dashboard (all roles)
      │    ├── Products (admin, staff)
      │    ├── Scan (admin, staff)
      │    └── Users (admin, manager)                       ← new: user management link
      │    └── User badge + logout
      │
      ├── [view=dashboard] → <Dashboard />
      ├── [view=products]   → product list/search           ← unchanged v1
      ├── [view=scan]       → scan pipeline                  ← unchanged v1
      ├── [view=detail]     → product card + stock           ← unchanged v1
      ├── [view=create]     → product form                   ← unchanged v1
      └── [view=users]      → <UserManagement />             ← new: manager/admin view
           ├── <UserList />                                  ← new: table of users
           │    ├── Username, role badge, active status
           │    ├── Deactivate/reactivate toggle
           │    └── Reset password button
           └── <CreateUserForm />                            ← new: add user form
                ├── Username field
                ├── Password field
                ├── [admin only] Role selector
                └── Create button
```

### AuthContext modifications

- Add JWT expiry check on initialization — if `sessionStorage` token's `exp` claim is in the past, treat as no token (redirect to login).
- Add `fetchWithAuth` wrapper — wraps `fetch` with automatic redirect to `/login` on 401 responses.
- `user` object already carries `role` — no structural change needed.

### Login page modifications

- Add separate display for `account_disabled` error (distinct from generic `invalid_credentials`).
- Move deactivated-account messaging: "Contact your manager" instead of "Invalid username or password".

### Sidebar modifications

- **staff role**: nav items = Dashboard, Products, Scan.
- **manager role**: nav items = Dashboard, Users (staff management only).
- **admin role**: nav items = Dashboard, Products, Scan, Users.
- Nav items list is filtered at render time based on `user.role`.

### SetupPage (new)

- Detected via `GET /api/setup/token` response on first load (before login).
- **Mode 1 — QR display**: When no admin exists, show the QR code (server-rendered PNG or qrcode.js-generated) and instructions: "Scan this QR code with your phone to set up the admin account."
- **Mode 2 — Setup form**: When `?token=<uuid>` is present in URL, show setup form with username, password, confirm password fields.
- On successful setup, redirect to login page with success message.

### UserManagement / UserList (new)

- Table with columns: Username, Role (badge), Status (active/deactivated badge), Actions.
- **Create user**: Modal or inline form; admin sees role selector; manager does not (hard-codes 'staff').
- **Deactivate**: Confirmation dialog → PATCH deactivate → refresh list.
- **Reactivate**: Same, but PATCH reactivate.
- **Reset password**: Prompt for new password → PATCH reset-password → show confirmation.
- Only accessible to `manager` and `admin` roles.

### api.ts modifications

- Add API functions for user management: `fetchUsers()`, `createUser()`, `deactivateUser()`, `reactivateUser()`, `resetPassword()`.
- Add 401 interceptor: if any API call returns 401, clear `sessionStorage` token and redirect to `/login`.

---

## 9.7 Key Design Decisions

### JWT Secret from Environment Variable

**Decision:** `JwtUtil` reads the signing secret from `JWT_SECRET` environment variable. The server fails to start if `JWT_SECRET` is unset or shorter than 256 bits.

**Rationale:** The current codebase has a hardcoded fallback secret — `"makmur-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm"` — which is unacceptable for production. A leaked secret allows token forgery at any role level. The environment variable ensures the secret is configurable per-deployment and never checked into version control.

**Implementation:** `System.getenv("JWT_SECRET")` in a static initializer. If null, throw `IllegalStateException` in a `@PostConstruct` method. Minimum length: 32 bytes (256 bits) for HMAC-SHA256. If longer than 32 bytes, truncate or use the full key depending on JJWT's key-size handling.

### Handler-level vs. Route-level Scope

**Decision:** Handler-level scope checks inside the method body (via `AuthService.requireRole()`).

**Rationale:** AC-21 explicitly requires it. The alternative — route-level guards (`SecurityConfig.requestMatchers(...).hasRole(...)`) — would require splitting endpoints that serve multiple roles into separate route patterns, which violates the AC-21 requirement. The handler-level approach keeps one implementation per endpoint regardless of role, with a single-line scope gate at the top of the method body.

**Trade-off:** The scope logic is spread across handler methods rather than centralised in a config file. This is acceptable because (a) there are only ~10 handlers in v1, and (b) each handler's scope is immediately visible when reading its code — no need to cross-reference route config to understand who can call it.

### Token Storage (sessionStorage)

**Decision:** Store JWT in `sessionStorage` (not `localStorage`, not cookies).

**Rationale:** `sessionStorage` ties the token to the browser tab — closing the tab or browser window ends the session. This is appropriate for a retail staff app on shared devices where a staff member walks away from a logged-in device. `localStorage` would persist the token indefinitely, including across browser restarts. Cookies would expose the token to CSRF and would require a different backend architecture (server-signed cookies, CSRF tokens).

**Trade-off:** `sessionStorage` is cleared on tab close but not on page refresh (same-origin navigations retain it). This matches the expected behaviour: refreshing the page should not require re-login, but walking away from the device eventually will.

### Password Hash Algorithm (bcrypt)

**Decision:** Use bcrypt via Spring Security's `BCryptPasswordEncoder` (strength factor 10).

**Rationale:** bcrypt is already a dependency (Spring Security ships `BCryptPasswordEncoder`). It is well-tested, resists GPU-based brute-force, and has no native-library dependency on the JVM. Argon2 and scrypt would require additional dependencies (e.g., Bouncy Castle or a native libsodium binding) — unjustified for a local-network app on trusted hardware. The strength factor 10 (~100ms per hash on modern hardware) is sufficient given the app's threat model (local network, not internet-facing).

**Trade-off:** Slower hashing means login takes ~100ms longer — imperceptible for a staff app. If hardware is very constrained (e.g., Raspberry Pi Zero), the strength factor can be lowered to 8. The cost factor is configurable via a property or env var.

### JWT Claims: Include role vs. DB Lookup Every Request

**Decision:** Embed `role` and `userId` as JWT claims. Do NOT look up the user from the DB on every authenticated request (except for `active`-flag verification).

**Rationale:** The current `JwtAuthenticationFilter` already calls `userRepository.findByUsername()` on every request to populate the SecurityContext. In the modified design, the JWT will contain `sub` (username), `role`, and `userId` claims. The filter extracts these from the token directly for the SecurityContext — no DB query needed for role attribution. The only DB query the filter performs is a lightweight `active`-flag check (fast — indexed by primary key). This avoids an unnecessary DB round-trip on every API call while still catching deactivations.

**Trade-off:** Role changes only take effect after token expiry (24h max). If a manager is promoted to admin, they must re-login to see the new scope. This is acceptable — re-login is a natural consequence of a role change.

### QR Setup Token: Persist Until Used (with TTL)

**Decision:** The one-time setup token persists in server memory (ConcurrentHashMap) until the first admin is created or until a TTL expires (e.g., 60 minutes from server start).

**Rationale:** The PRD proposes a TTL of 60 minutes. This balances setup convenience (admin has an hour to scan the QR) against security (a leaked QR code is eventually invalid). The token is stored in-memory (not persisted to disk) so a server restart generates a fresh token. The setup endpoint (`POST /api/setup/register`) requires the exact UUID — no enumeration possible.

**Trade-off:** If the admin does not complete setup within 60 minutes, they must restart the server (or the server operator must restart it). This is a one-time inconvenience during initial deployment. An alternative — persist the token to DB — would survive restart but also persist a leaked token. In-memory is the safer choice.

### AC-01 Merger: Auth Required Breaks Original AC-01

**Decision:** AC-01 ("Open the app without login") is **superseded** by auth. The original AC-01 was valid for the pre-auth v1 scope. With auth introduced, unauthenticated access is no longer possible — the login page is the new landing page for all devices.

**Migration note:** Existing v1 deployments (no auth) will need manual data migration or a one-time script to update the `users` table schema and create an admin account. The QR onboarding flow handles greenfield deployments. For brownfield upgrades, the server can detect the old `password` column at startup and run the migration automatically, then prompt the operator to create an admin account via a CLI command or a one-shot endpoint.

---

## 9.8 Verification Hooks — Auth Domain

### Authentication & Login

| Criterion | Verification Method |
|-----------|-------------------|
| AC-15: Valid credentials return JWT; invalid return 401 | Automated — integration test: POST /api/auth/login with valid creds → 200 + token; invalid creds → 401. |
| AC-16: No JWT on protected endpoints returns 401 | Automated — GET /api/products without Authorization header → 401. |
| AC-17: Expired/malformed JWT returns 401 | Automated — set Authorization with expired token → 401; with tampered token → 401. |
| AC-24: Passwords stored as bcrypt hashes | Automated — unit test: UserRepository.save() verifies stored value starts with `$2a$` or `$2b$`. |
| AC-25: JWT contains sub, role, iat, exp ≤ 24h | Automated — unit test: decode JWT, assert claims present, assert exp - iat ≤ 86,400,000ms. |
| AC-28: Deactivated account cannot log in | Automated — integration test: deactivate user via PATCH /api/users/:id/deactivate, then POST /api/auth/login → 403 with `account_disabled`. |
| AC-27: Expired/logged-out user redirected to login | Automated — integration test: clear token on client → next API call → 401. Manual — verify redirect in browser. |
| AC-29: Login page is default redirect; setup page only when no admin exists | Automated — GET / on server → serves index.html (React handles routing). Manual — visit app without token → see login page; visit with setup token → see setup form. |

### Authorization — Role Scopes

| Criterion | Verification Method |
|-----------|-------------------|
| AC-18: Staff can access products but gets 403 on user mgmt | Automated — integration test: as staff role, GET /api/products → 200; GET /api/users → 403. |
| AC-19: Manager can access user mgmt but gets 403 on products | Automated — integration test: as manager role, GET /api/users → 200; GET /api/products → 403. |
| AC-20: Admin can access all endpoints | Automated — integration test: as admin role, GET /api/products → 200; GET /api/users → 200. |
| AC-21: Scope checks inside handler, not routing layer | Architecture review — verify each handler body calls `authService.requireRole()`. No role-based route exclusion in SecurityConfig. |

### Onboarding

| Criterion | Verification Method |
|-----------|-------------------|
| AC-22: QR flow when no admin exists | Automated — integration test: truncate users table, GET /api/setup/token → 200 with token; GET /api/setup/qr → 200 with image/png. Manual — open app with empty DB → see QR page. |
| AC-23: Setup token invalidated after first admin created | Automated — integration test: POST /api/setup/register → 201; GET /api/setup/token → 403. |

### Schema

| Criterion | Verification Method |
|-----------|-------------------|
| AC-26: Schema migration: password_hash, active, role CHECK includes 'manager' | Automated — database test: verify `users` table columns; verify INSERT with role='manager' succeeds; verify INSERT with role='superadmin' fails. |

---

## 9.9 Risks and Mitigations — Auth Domain

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **JWT secret hardcoded in codebase** | High (current state) | Critical — token forgery | Enforce `JWT_SECRET` env var at startup. Server fails to start if unset. CI pipeline scans for hardcoded secrets. Remove fallback constant. |
| **Role escalation via client-controlled role field** | Medium | High — manager creates admin accounts | Server hard-codes role for manager-created users. Only the admin endpoint accepts role from client, and only `'manager'` or `'staff'` are valid values. Server rejects `'admin'` in creation requests. |
| **QR setup token intercepted before admin scans** | Low-Medium | Medium — attacker creates own admin account | Token is valid only on local network. Setup page only served when no admin exists. Token invalidated after first use. TTL (60 min) limits exposure window. |
| **Direct SQLite database access** | Medium | Critical — bypasses all auth | DB file stored outside web root. File permissions restrict to application process only. No direct DB access from API — all data operations go through controllers with auth. |
| **Token expiry mid-scan disrupts workflow** | Medium | Low-Medium — staff re-login | 24h token covers a full shift. If expiry is disruptive, add refresh-token mechanism in v2. The login flow is fast (~100ms) — re-login is a minor inconvenience. |
| **Manager creates staff accounts offline / bypass** | Low | Medium — unmanaged staff accounts | All user creation flows through logged API endpoints. No offline user creation. Audit manifests as server logs. |
| **Race condition: two simultaneous POST /api/setup/register** | Low | Medium — two admin accounts created | Token invalidation happens BEFORE user creation (or wrapped in synchronized block). Since setup is a one-time operation on a single JVM, a synchronized guard is sufficient. |

---

## 9.10 Open Questions — Auth

| # | Question | Design Answer |
|---|----------|--------------|
| Q8 | Single JWT vs. refresh-token mechanism? | Single 24h JWT. Refresh tokens add complexity (endpoint, storage, client refresh logic) that is unjustified for a local-network app with short-lived tokens. Deferred to v2 if mid-session expiry becomes a pain point. |
| Q9 | Gate all views behind login or allow public scan? | Gate all views. The PRD explicitly requires login for all access. A public scan-only mode would require separate public vs. authenticated endpoints, adding complexity. |
| Q10 | QR setup token TTL? | 60 minutes from server start. Tokens stored in-memory. Server restart = new token. |
| Q11 | Soft deactivation vs. hard delete? | Soft deactivation (`active` column). Preserves referential integrity. Deletion is a DB-level operation only. |
| Q12 | Upgrade path for existing v1 deployments? | Server detects old `password` column at startup → runs migration automatically (copy data to new schema). Operator creates admin via CLI or one-shot endpoint. |
| Q13 | Remember last username on login page? | No, for v1. Shared devices mean the last username is more likely to confuse than help. |
| Q14 | Logout button visible at all times? | Yes — always visible in the sidebar footer alongside the username/role badge. Staff need a clear way to end their session, especially on shared devices. |