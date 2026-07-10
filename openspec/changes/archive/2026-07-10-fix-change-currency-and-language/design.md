# Design — Fix Currency and Language

Localize Makmur inventory webapp for Indonesian users. All UI text, error messages, and price formatting to Indonesian standard. No schema changes, no locale switcher, no multi-language support.

## 1. Architecture Overview

Two-pronged approach:

- **Client**: All user-facing strings in React components translated to Indonesian. Price formatting via `Intl.NumberFormat('id-ID')`.
- **Server**: Error response `message` fields translated in `GlobalExceptionHandler` and controller error logic.
- No new dependencies. No schema changes.
- `price` column stays as REAL — server returns raw number, client formats.

## 2. Component Tree — Translation Map

| Component | Current (English) | Change |
|-----------|-------------------|--------|
| Login.tsx | "Username", "Password", "Log In", error messages | Translate all labels, placeholders, errors |
| Sidebar.tsx | "Dashboard", "Products", "Scan", "Users", "Logout" | Translate nav items |
| ProductList.tsx | "No products found", loading text | Translate empty state |
| SearchBar.tsx | "Search products..." placeholder | Translate placeholder |
| ProductCard.tsx | "Stock:", "Price:", "Update Stock", "Scan Another" | Translate labels and buttons |
| ProductForm.tsx | "Product Name", "Price", "Initial Stock", "Save", "Cancel" | Translate labels, validation errors |
| StockControls.tsx | +1, -1 accessibility labels, success flash text | Translate |
| Viewfinder.tsx | All 5 camera error messages | Translate to Indonesian |
| SetupPage.tsx | QR instructions, form labels, validation | Translate |
| UserList.tsx | "Users", "Deactivate", "Reactivate", "Create User" | Translate |
| CreateUserForm.tsx | "Username", "Password", role selector, submit | Translate |
| Dashboard.tsx | Welcome text, role-based messages | Translate |

## 3. Server Changes

| File | Current (English) | Change |
|------|-------------------|--------|
| GlobalExceptionHandler.java | "Access denied" | `"Akses ditolak"` |
| AuthController.java | Error messages (login failed, token expired, etc.) | Translate to Indonesian |
| ProductController.java | "Product with barcode X not found", validation errors | Translate to Indonesian |
| UserController.java | "Username X already exists", scope errors | Translate to Indonesian |
| SetupController.java | "Admin account created", setup messages | Translate to Indonesian |

## 4. Price Formatting Design

```typescript
// Use Intl.NumberFormat for all price display
const formatPrice = (price: number): string => {
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(price)
}
```

- `Intl.NumberFormat('id-ID')` handles: `Rp` prefix, `.` thousands separator, no decimal for whole amounts
- For input: accept raw number, validate as positive number, store as number
- No server-side formatting — server returns raw number, client formats
- No schema change — `price` stays as REAL

## 5. Key Design Decisions

| Decision | Options | Chosen | Rationale |
|----------|---------|--------|-----------|
| Client vs server formatting | Format on server or client | Client (`Intl.NumberFormat`) | Server returns raw number, consistent API, no locale coupling |
| String approach | React i18n library vs simple find-and-replace | Find-and-replace, no library | YAGNI — Indonesian-only, no locale switcher needed in v1 |
| Error message translation | Properties file vs inline | Inline in Java code (constructor/response builder) | Simple change, no resource bundle overhead for ~10 messages |
| Price input format | Accept formatted input (Rp12.500) vs raw number | Raw number input, formatted display | Simpler validation, avoids parsing complications |

## 6. Verification Hooks

| AC | Method |
|----|--------|
| AC1 (UI in Indonesian) | Manual — walk through every view, verify no English strings visible |
| AC2 (Rp format) | Automated — unit test on `formatPrice()` with various magnitudes |
| AC3 (error messages in Indonesian) | Manual — trigger each error type, verify Indonesian `message` |
| AC4 (camera errors in Indonesian) | Manual — trigger each camera error, verify Indonesian text |
| AC5 (setup page in Indonesian) | Manual — navigate setup flow, verify Indonesian |
| AC6 (flash messages) | Manual — update stock, verify flash message in Indonesian |
| AC7 (price input) | Manual — enter price, verify save and display |
| AC8 (no regression) | Automated — `mvn test` 61/61 |

## 7. Risks and Mitigations

| Risk | Mitigation |
|------|-----------|
| Missing a string — English leaks through | Audit by component checklist after translation |
| Price format edge case (billions) | `Intl.NumberFormat` handles all magnitudes; test with 1.000.000.000 |
| Inconsistent translation style | Keep a glossary of 10 key terms and use consistently |
| Server error message encoding | Ensure UTF-8 throughout; Spring Boot defaults to UTF-8 |
