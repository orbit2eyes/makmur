# Makmur

Barcode scanning webapp for retail inventory management. Scan EAN-13 barcodes on products, look up or create product records, and manage stock levels — all from a web browser camera.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React + Vite + TypeScript |
| Backend | Java 17+ (Spring Boot 3.x) |
| Database | SQLite (server-side via JDBC / org.xerial:sqlite-jdbc) |
| Build | Maven (server), npm (client) |
| Barcode format | EAN-13 |
| Auth | JWT multi-user (admin/staff roles) |

## Prerequisites

- Java 17+ (JDK)
- Maven 3.x
- Node.js 18+
- npm or yarn
- `JWT_SECRET` environment variable (min 32 bytes) — required at production startup. See server/ENV.md.

## Quick Start

```bash
# clone
git clone <repo-url> && cd makmur

# install server dependencies and build
cd server
mvn package -DskipTests

# install client dependencies and build
cd ../client
npm install
npm run build

# start server (serves API + static client files)
cd ../server
java -jar target/makmur-server-1.0.0.jar
```

Open `http://localhost:3001` in a browser. You will be redirected to the login page.

> **Note:** There is no default admin account. On first launch, the server serves a QR-based onboarding flow. Scan the QR code displayed in the browser to create the initial admin account.

### Development mode

```bash
# terminal 1 — start backend
cd server && mvn spring-boot:run

# terminal 2 — start frontend dev server (proxies API to backend)
cd client && npm run dev
```

Open `http://localhost:5173` in development mode (Vite dev server with API proxy to port 3001).

## Project Structure

```
makmur/
├── client/                    # React + TypeScript + Vite frontend
│   ├── src/
│   │   ├── api.ts             # REST client — fetch calls to /api/*, 401 interceptor
│   │   ├── App.tsx            # Top-level component — view state management
│   │   ├── main.tsx           # React entry point
│   │   ├── types.ts           # TypeScript interfaces (Product, User, API errors)
│   │   ├── index.css          # App styles (incl. auth UI, role badges)
│   │   ├── context/
│   │   │   └── AuthContext.tsx     # Auth state, login/logout, JWT expiry detection
│   │   └── components/
│   │       ├── BarcodeDecoder.tsx  # Barcode detection (native API + zxing polyfill)
│   │       ├── CreatePrompt.tsx    # Scan-not-found creation prompt
│   │       ├── CreateUserForm.tsx  # Create user modal/form (manager/admin)
│   │       ├── Login.tsx           # Login form with error handling
│   │       ├── ManualEntry.tsx     # Manual barcode text input
│   │       ├── ProductCard.tsx     # Product detail display
│   │       ├── ProductForm.tsx     # Product creation form
│   │       ├── ProductList.tsx     # Scrollable product catalogue
│   │       ├── ProtectedRoute.tsx  # Route guard — redirects to login if unauthenticated
│   │       ├── ScanResult.tsx      # Scan result routing (found / not found)
│   │       ├── SearchBar.tsx       # Debounced search input
│   │       ├── SetupPage.tsx       # QR display + setup form for onboarding
│   │       ├── Sidebar.tsx         # Role-based navigation sidebar
│   │       ├── StockControls.tsx   # Stock update UI (+1/-1, absolute)
│   │       ├── UserList.tsx        # User management table (manager/admin)
│   │       └── Viewfinder.tsx      # Camera viewfinder
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.js
├── server/                    # Java / Spring Boot backend
│   ├── src/main/java/com/makmur/
│   │   ├── Application.java             # Spring Boot entry point (port 3001)
│   │   ├── config/
│   │   │   ├── DataSourceConfig.java    # Creates data/ directory
│   │   │   ├── JwtAuthenticationFilter.java  # Per-request JWT auth filter
│   │   │   ├── JwtUtil.java             # JWT generation/validation (JWT_SECRET from env)
│   │   │   ├── SchemaMigration.java     # Schema migration (password→password_hash, active flag)
│   │   │   ├── SecurityConfig.java      # Route security config (permitAll vs authenticated)
│   │   │   ├── SetupTokenStore.java     # In-memory one-time setup token store
│   │   │   └── WebConfig.java           # CORS + static file serving
│   │   ├── controller/
│   │   │   ├── AuthController.java      # POST /api/auth/login
│   │   │   ├── HealthController.java    # GET /api/health
│   │   │   ├── ProductController.java   # Product CRUD + stock update endpoints
│   │   │   ├── SetupController.java     # QR onboarding endpoints (/api/setup/*)
│   │   │   └── UserController.java      # User management endpoints (/api/users/*)
│   │   ├── entity/
│   │   │   ├── Product.java             # Product entity (id, barcode, name, price, stock, created_at)
│   │   │   └── User.java                # User entity (username, passwordHash, role, active)
│   │   ├── exception/
│   │   │   ├── ForbiddenException.java  # 403 Forbidden exception
│   │   │   └── GlobalExceptionHandler.java  # Global error handler (standardized error JSON)
│   │   ├── repository/
│   │   │   ├── ProductRepository.java   # JDBC-based product data access
│   │   │   └── UserRepository.java      # JDBC-based user data access
│   │   └── service/
│   │       └── AuthService.java         # Scope enforcement utility (requireRole)
│   ├── src/main/resources/
│   │   ├── application.properties       # SQLite config, port, Jackson naming
│   │   └── schema.sql                   # Database schema initialization
│   ├── pom.xml
│   └── data/                            # SQLite database file (created at runtime)
└── openspec/                    # OpenSpec SDD artifacts (design, specs, tasks)
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/health` | Health check — `{"status":"ok"}` (public) |
| `POST` | `/api/auth/login` | Login — returns JWT token on success (public) |
| `GET` | `/api/setup/token` | Get one-time setup token (public, only when no admin exists) |
| `GET` | `/api/setup/qr` | Get QR code PNG encoding setup URL (public, only when no admin exists) |
| `POST` | `/api/setup/register` | Register initial admin with setup token (public) |
| `GET` | `/api/products` | List all products, sorted alphabetically |
| `GET` | `/api/products/search?q=<query>` | Search by partial name (case-insensitive, min 2 chars) |
| `GET` | `/api/products/:barcode` | Lookup product by EAN-13 barcode |
| `POST` | `/api/products` | Create product (barcode, name, price, stock) |
| `PATCH` | `/api/products/:barcode/stock` | Update stock (value=absolute or delta=change) |
| `GET` | `/api/users` | List users (admin sees all, manager sees staff only) |
| `POST` | `/api/users` | Create user (admin sets role, manager creates staff only) |
| `PATCH` | `/api/users/:id/deactivate` | Deactivate user (set active=0) |
| `PATCH` | `/api/users/:id/reactivate` | Reactivate user (set active=1) |
| `PATCH` | `/api/users/:id/reset-password` | Reset user password |

## Usage

1. **Onboarding** — On first launch with no admin account, the app displays a setup QR code. Scan it or use the setup token to create the initial admin account.
2. **Login** — All users must authenticate. Login with username and password to receive a JWT (24h expiry).
3. **Role-based access** — Staff can scan, browse, search, create products, and update stock. Managers can manage users (create, deactivate, reset passwords for staff accounts). Admins can do everything.
4. **Scan** — Point your camera at an EAN-13 barcode. The app decodes it and looks up the product.
5. **Browse** — Scroll through the full product catalogue from the home screen.
6. **Search** — Type in the search bar to filter products by name.
7. **Create** — If a scanned barcode is not in the catalogue, add it with name, price, and initial stock.
8. **Update Stock** — Use +1/-1 buttons for quick adjustments, or enter an absolute value.

## Network Access

The server binds to all network interfaces. Other devices on the same local network can access the app at `http://<server-ip>:3001`. Authentication is required for all endpoints except `/api/health`, `/api/auth/login`, and `/api/setup/*`.

## Status

**Production-ready** — v1 complete with JWT multi-user auth (admin/manager/staff roles) and QR-based onboarding.

## License

MIT