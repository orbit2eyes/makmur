---
phase: fix-change-currency-and-language
description: Localize UI and error messages to Indonesian, change currency to IDR
domains: [product, auth]
total-tasks: 8
effort-estimate: M
---

# Tasks — Localization (UI + Currency + Error Messages)

This change localizes the Makmur inventory webapp for Indonesian users:
- All UI text translated from English to Indonesian
- Price format from USD ($) to IDR (Rp) with `.` thousands separator
- Server error `message` fields to Indonesian
- No schema changes, no new dependencies, no locale switcher

**Design Summary**

- **Client**: 12 components need string translation. Price formatting via `Intl.NumberFormat('id-ID')`. Server returns raw number, client formats.
- **Server**: 5 Java files need error message `message` field translations. Error codes remain English.

---

### T-l10n-01: Price formatting utility

- **Description**: Create a shared `formatPrice()` function using `Intl.NumberFormat('id-ID')` with Rp prefix, `.` separator, no decimals for whole amounts.
- **Files affected**: `client/src/utils.ts` (new) or `client/src/api.ts` (add export)
- **Dependencies**: None
- **Complexity**: XS
- **Acceptance criteria**:
  1. `formatPrice(12500)` returns `"Rp12.500"`
  2. `formatPrice(1500000)` returns `"Rp1.500.000"`
  3. `formatPrice(500)` returns `"Rp500"`
- **Evidence**: `node -e "console.log(new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',minimumFractionDigits:0}).format(12500))"` prints `Rp12.500`

### T-l10n-02: Translate product components (ProductList, ProductCard, ProductForm, SearchBar, StockControls)

- **Description**: Translate all user-facing strings in these 5 product-related components.
- **Files affected**: `ProductList.tsx`, `ProductCard.tsx`, `ProductForm.tsx`, `SearchBar.tsx`, `StockControls.tsx`
- **Dependencies**: T-l10n-01 (for price format in ProductCard, ProductForm, ProductList)
- **Complexity**: M
- **Acceptance criteria**:
  1. Product list empty state shows Indonesian text
  2. Search placeholder is "Cari produk..."
  3. Product card labels show "Stok:" and "Harga:"
  4. Form labels show "Nama Produk", "Harga", "Stok Awal"
  5. Stock increment/decrement button labels are Indonesian
- **Evidence**: Open each view in browser, verify no English strings visible.

### T-l10n-03: Translate auth components (Login, Sidebar, SetupPage)

- **Description**: Translate all strings in Login, Sidebar, and SetupPage components.
- **Files affected**: `Login.tsx`, `Sidebar.tsx`, `SetupPage.tsx`
- **Dependencies**: None
- **Complexity**: S
- **Acceptance criteria**:
  1. Login form labels are "Nama Pengguna" and "Kata Sandi"
  2. Login button text is "Masuk"
  3. Invalid credentials error message is Indonesian
  4. Deactivated account error message is Indonesian
  5. Sidebar nav items: "Dasbor", "Produk", "Pindai", "Pengguna", "Keluar"
  6. Setup page instructions and form labels are Indonesian
- **Evidence**: Open login page, verify Indonesian text. Log in, verify sidebar items.

### T-l10n-04: Translate user management components (UserList, CreateUserForm, Dashboard)

- **Description**: Translate all strings in UserList, CreateUserForm, and Dashboard components.
- **Files affected**: `UserList.tsx`, `CreateUserForm.tsx`, `Dashboard.tsx`
- **Dependencies**: None
- **Complexity**: S
- **Acceptance criteria**:
  1. User table column headers are Indonesian
  2. Deactivate/Reactivate button labels are Indonesian
  3. Create user form labels are Indonesian
  4. Dashboard welcome text is Indonesian
- **Evidence**: Navigate to user management as admin, verify Indonesian text.

### T-l10n-05: Translate scan components (Viewfinder camera error messages)

- **Description**: Translate all 5 camera error messages in the `CAMERA_ERRORS` constant map.
- **Files affected**: `Viewfinder.tsx`
- **Dependencies**: None
- **Complexity**: XS
- **Acceptance criteria**:
  1. Permission denied error message is Indonesian
  2. No camera found error message is Indonesian
  3. Camera in use error message is Indonesian
  4. HTTPS required error message is Indonesian
  5. Unknown error message is Indonesian
- **Evidence**: Trigger each error type in browser, verify Indonesian message text.

### T-l10n-06: Translate server error messages (GlobalExceptionHandler, AuthController)

- **Description**: Translate `message` fields in GlobalExceptionHandler and AuthController error responses.
- **Files affected**: `GlobalExceptionHandler.java`, `AuthController.java`
- **Dependencies**: None
- **Complexity**: S
- **Acceptance criteria**:
  1. 401 response returns `"message": "Sesi telah berakhir"`
  2. 403 forbidden response returns `"message": "Akses ditolak"`
  3. 403 account_disabled response returns `"message": "Akun dinonaktifkan. Hubungi atasan Anda."`
  4. 404 response returns `"message": "Data tidak ditemukan."`
- **Evidence**: `curl -v http://localhost:3001/api/products` without auth header — verify `message` field in JSON response body.

### T-l10n-07: Translate server error messages (ProductController, UserController, SetupController)

- **Description**: Translate `message` fields in ProductController, UserController, and SetupController error responses.
- **Files affected**: `ProductController.java`, `UserController.java`, `SetupController.java`
- **Dependencies**: T-l10n-06
- **Complexity**: S
- **Acceptance criteria**:
  1. Product not found returns `"message": "Produk tidak ditemukan"`
  2. Duplicate barcode returns `"message": "Produk dengan barcode ini sudah ada"`
  3. Duplicate username returns `"message": "Nama pengguna sudah ada"`
  4. Setup-related messages are Indonesian
- **Evidence**: Hit each error endpoint via curl, verify Indonesian `message` field.

### T-l10n-08: Verify no English strings remain

- **Description**: Walk through every view and API error path to confirm no English user-facing strings remain.
- **Files affected**: All files from T-l10n-01 through T-l10n-07
- **Dependencies**: All previous tasks (T-l10n-01 through T-l10n-07)
- **Complexity**: M
- **Acceptance criteria**:
  1. No English user-facing text in login page
  2. No English user-facing text in sidebar
  3. No English user-facing text in dashboard
  4. No English user-facing text in product list/detail/form
  5. No English user-facing text in scan view
  6. No English user-facing text in user management
  7. No English user-facing text in setup page
  8. No English `message` fields in API error responses
  9. `mvn test` passes (61/61)
  10. `npx tsc --noEmit` clean
- **Evidence**: Manual audit checklist completed. `mvn test` returns 61/61. `npx tsc --noEmit` exits 0 with no errors.

---

## Notes

- No new npm dependencies
- No schema changes
- No new Java dependencies
- After all tasks complete: `npx tsc --noEmit` clean, `mvn test` 61/61
- Keep error codes machine-readable (English), only translate `message` fields
- UTF-8 throughout (Spring Boot defaults to UTF-8)
