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

### Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Staff | (create via admin) | — |

The admin account is created automatically on first server startup.

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
│   │   ├── api.ts             # REST client — fetch calls to /api/*
│   │   ├── App.tsx            # Top-level component — view state management
│   │   ├── main.tsx           # React entry point
│   │   ├── types.ts           # TypeScript interfaces (Product, API errors, etc.)
│   │   ├── index.css          # App styles
│   │   └── components/
│   │       ├── ManualEntry.tsx    # Manual barcode text input
│   │       ├── ProductCard.tsx    # Product detail display
│   │       ├── ProductForm.tsx    # Product creation form
│   │       ├── ProductList.tsx    # Scrollable product catalogue
│   │       ├── SearchBar.tsx      # Debounced search input
│   │       ├── StockControls.tsx  # Stock update UI (+1/-1, absolute)
│   │       └── Viewfinder.tsx     # Camera viewfinder
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.js
├── server/                    # Java / Spring Boot backend
│   ├── src/main/java/com/makmur/
│   │   ├── Application.java             # Spring Boot entry point (port 3001)
│   │   ├── config/
│   │   │   ├── DataSourceConfig.java    # Creates data/ directory
│   │   │   └── WebConfig.java           # CORS + static file serving
│   │   ├── controller/
│   │   │   ├── HealthController.java    # GET /api/health
│   │   │   └── ProductController.java   # Product CRUD + stock update endpoints
│   │   ├── entity/
│   │   │   └── Product.java             # Product entity (id, barcode, name, price, stock, created_at)
│   │   └── repository/
│   │       └── ProductRepository.java   # JDBC-based data access
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
| `GET` | `/api/health` | Health check — `{"status":"ok"}` |
| `GET` | `/api/products` | List all products, sorted alphabetically |
| `GET` | `/api/products/search?q=<query>` | Search by partial name (case-insensitive, min 2 chars) |
| `GET` | `/api/products/:barcode` | Lookup product by EAN-13 barcode |
| `POST` | `/api/products` | Create product (barcode, name, price, stock) |
| `PATCH` | `/api/products/:barcode/stock` | Update stock (value=absolute or delta=change) |

## Usage

1. **Scan** — Point your camera at an EAN-13 barcode. The app decodes it and looks up the product.
2. **Browse** — Scroll through the full product catalogue from the home screen.
3. **Search** — Type in the search bar to filter products by name.
4. **Create** — If a scanned barcode is not in the catalogue, add it with name, price, and initial stock.
5. **Update Stock** — Use +1/-1 buttons for quick adjustments, or enter an absolute value.

## Network Access

The server binds to all network interfaces. Other devices on the same local network can access the app at `http://<server-ip>:3001`. No authentication is required (local network only).

## Status

**Production-ready** — v1 complete.

## License

MIT