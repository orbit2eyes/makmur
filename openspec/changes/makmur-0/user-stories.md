# User Stories — Makmur v1

UX/UI-focused user stories organized by domain. Three roles: **admin**, **manager**, **staff**.

## Role Scopes & Responsibilities

Before the user stories, here's a clear definition of each role's scope and responsibilities:

### Admin Role
- **Full system access**: Can access all product and user management features
- **User management**: Can create, deactivate, and reset passwords for both staff and manager accounts
- **Role management**: Can create custom roles and define permissions (beyond default admin/manager/staff)
- **Cannot create additional admins**: Admin accounts can only be created through the QR onboarding process
- **Primary responsibilities**: System setup, overseeing all operations, managing managers and staff, configuring system roles/permissions

### Manager Role
- **Default access**: Limited to user management only (cannot access product features)
- **Customizable**: Permissions can be adjusted by admins to fit specific job requirements
- **Staff management**: Can create, deactivate, reactivate, and reset passwords for staff accounts (by default)
- **Cannot access manager or admin accounts**: Can only manage staff-role users (by default)
- **Primary responsibilities**: Overseeing staff, managing user accounts, reviewing daily stock reports

### Staff Role
- **Default access**: Limited to product features only (cannot access user management features)
- **Customizable**: Permissions can be adjusted by admins to fit specific job requirements
- **Product operations**: Can scan barcodes, view product details, update stock, search and browse catalogue (by default)
- **Sales recording**: Can record sales transactions (as cashier) for end-of-day reporting (by default)
- **Primary responsibilities**: Daily product management, stock updates, customer service, sales tracking

Priority levels: **P0** = must-have for v1, **P1** = important but not blocking, **P2** = nice-to-have.

---

## Auth & Onboarding

### P0

**As a** first-time admin, **I want** to see a QR code setup page when I open the app for the first time, **so that** I can create the initial admin account without needing pre-seeded credentials.

> **Acceptance:** First browser visit with empty users table shows a QR code image and instructions text. No login form appears. The QR code encodes a setup URL with a one-time token.

**As a** first-time admin, **I want** to scan the QR code with my phone's camera, **so that** I can navigate to the setup form on my phone.

> **Acceptance:** Scanning the QR code opens a URL on the phone's browser that shows a setup form with username, password, and confirm-password fields. The token from the QR is passed as a URL query parameter.

**As a** first-time admin, **I want** to fill in a username and password on the setup form, **so that** my admin account is created securely.

> **Acceptance:** Submit POSTs to `/api/setup/register`. On success, redirects to login with a success message. Password must be at least 8 characters. Confirm password must match. Shows inline validation errors.

**As any** user with valid credentials, **I want** to log in by entering my username and password, **so that** I can access the app.

> **Acceptance:** Login form with username field, password field, and Log In button. Send POST to `/api/auth/login`. On success, stores JWT in sessionStorage and redirects to role-appropriate home view.

**As any** user, **I want** to see a clear error message if I enter the wrong credentials, **so that** I know my login attempt failed.

> **Acceptance:** Invalid credentials show "Invalid username or password" — identical message whether username or password was wrong. No user enumeration.

**As a** deactivated staff member, **I want** to see a distinct "account disabled" error when I try to log in, **so that** I know to contact my manager.

> **Acceptance:** Login page shows "Account is deactivated. Contact your manager." for `account_disabled` errors from the API. This message is visually distinct from the generic invalid-credentials error.

### P1

**As a** logged-in user, **I want** to see a logout button at all times in the sidebar, **so that** I can end my session when I'm done.

> **Acceptance:** Sidebar footer shows username, role badge, and a Logout button. Clicking logout clears JWT from sessionStorage and redirects to the login page.

**As any** user, **I want** to be automatically redirected to the login page if my session expires, **so that** I don't see broken error pages.

> **Acceptance:** If any API call returns 401 (expired or invalid token), the client clears the token and redirects to login. The client also proactively checks the JWT `exp` claim before making API calls.

**As any** user, **I want** my session to survive a page refresh, **so that** I don't have to log in again just because I reloaded.

> **Acceptance:** JWT stored in sessionStorage persists through page refreshes. The app initializes by checking for an existing token and verifying its `exp` claim before rendering the login page.

### P2

**As an** admin** flow, **I want** to receive feedback if the setup token is invalid or expired, **so that** I know to contact a server operator.

> **Acceptance:** Setup form shows error message "Setup token is invalid or expired" with instructions to restart the server or contact the operator.

---

## Scanning

### P0

**As a** staff member, **I want** to point my phone camera at a product barcode and see the live viewfinder, **so that** I can scan products hands-free.

> **Acceptance:** App requests camera permission via `getUserMedia` on first access. Once granted, a live camera preview fills the screen. A barcode detection overlay highlights the detected barcode area.

**As a** staff member, **I want** the app to automatically decode an EAN-13 barcode when I point the camera at it, **so that** I don't have to press any buttons to initiate a scan.

> **Acceptance:** Within 3 seconds of a valid EAN-13 barcode entering the camera frame under normal lighting, the barcode is decoded. A brief visual flash or haptic-like indicator confirms the decode.

**As a** staff member, **I want** to see the product detail card immediately after scanning a known barcode, **so that** I can check stock and price.

> **Acceptance:** After decoding, product detail card (name, barcode, stock, price) appears within 1 second. Viewfinder transitions smoothly to the card.

**As a** staff member, **I want** to see a "Product not found" prompt if I scan an unknown barcode, **so that** I can decide whether to add it to the catalogue.

> **Acceptance:** Unknown barcode shows "Product not found — Add it to the catalogue?" with the detected barcode number. Two buttons: "Add Product" and "Cancel" (returns to viewfinder).

**As a** staff member, **I want** a text input field to manually type a barcode, **so that** I can look up a product even if the camera is unavailable or I deny permission.

> **Acceptance:** Manual entry text input is visible alongside the viewfinder (or as a fallback when camera is denied). Validates 13-digit EAN-13 format before submit. Found product shows detail card; not-found shows creation prompt.

**As a** staff member, **I want** to scan multiple products in quick succession without tapping between each scan, **so that** I can work through a shipment efficiently.

> **Acceptance:** Continuous scan mode: after showing the result for 2 seconds, the viewfinder reactivates automatically. The camera stream stays active between scans — no reinitialization. "Scan Another" button also available for immediate return.

### P1

**As a** staff member, **I want** to see a clear error message if I deny camera permission, **so that** I know how to enable it.

> **Acceptance:** If `getUserMedia` fails with `NotAllowedError`, shows "Camera access denied. Enable camera in your browser settings and refresh." Manual entry field is shown alongside.

**As a** staff member, **I want** the app to keep trying to decode if the first attempt fails (poor lighting, blurry label), **so that** I don't have to restart the scan flow.

> **Acceptance:** Non-blocking retry message appears after ~10 seconds without a successful decode. Viewfinder stays active. Manual entry option is always available.

### P2

**As a** staff member in a warehouse, **I want** the option to disable auto-return to viewfinder, **so that** I can study the product card without it disappearing.

> **Acceptance:** A toggle or onboarding hint explains the auto-return behavior. When auto-return is disabled, the product card stays until the user taps "Scan Another" or navigates away.

---

## Products

### P0

**As a** staff member, **I want** to see a scrollable list of all products in the catalogue, **so that** I can browse what's available.

> **Acceptance:** Product list view shows all products sorted alphabetically by name. Each row shows product name, barcode, stock count, and price. List scrolls smoothly for up to 5000 products. Loading state shown while fetching. Empty state when catalogue is empty.

**As a** staff member, **I want** to search for a product by typing part of its name, **so that** I can find it quickly without scanning.

> **Acceptance:** Search bar at top of product list. Filters results in real-time with 200ms debounce. Matches case-insensitive substring of product name. Results appear within 500ms for 1000 products. Clear button (X) resets to full list.

**As a** staff member, **I want** to tap a product row to see its full details, **so that** I can view stock, price, and take action.

> **Acceptance:** Tapping a product row navigates to the product detail card showing name, barcode, stock, and price. Fetches fresh data from the server on mount.

### P1

**As a** staff member, **I want** to create a new product by providing its name, price, and initial stock, **so that** I can add it to the catalogue.

> **Acceptance:** Product creation form with barcode (pre-filled if from scan), name (required), price (required, positive number), stock (required, non-negative integer). Client-side validation before submit. On success, navigates to product detail card. Cancel button returns to previous view.

**As a** staff member, **I want** to be redirected to the existing product if I try to create a product with a barcode that already exists, **so that** I don't accidentally create duplicates.

> **Acceptance:** If POST /api/products returns 409 (duplicate barcode), the app navigates to the existing product's detail card with a message "This product already exists."

---

## Stock

### P0

**As a** staff member, **I want** to see the current stock count prominently on the product detail card, **so that** I know how many units are on hand at a glance.

> **Acceptance:** Stock count is displayed as a large, prominent number on the product detail card alongside the product name, barcode, and price.

**As a** staff member, **I want** to tap +1 or -1 buttons to quickly adjust stock, **so that** I can update counts for single-unit changes without opening a keyboard.

> **Acceptance:** +1 button and -1 button on the product detail card. Tapping either immediately sends a PATCH request and refreshes the displayed stock. The -1 button is disabled when stock is 0. A success indicator (green flash) appears briefly after the update.

**As a** staff member, **I want** to enter an exact stock count for cycle counts or receiving shipments, **so that** I can set the stock to the correct value.

> **Acceptance:** "Update Stock" button opens a numeric input showing the current stock for reference. Staff enters a non-negative integer and confirms. Stock updates immediately and refreshes. Invalid input (negative numbers) shows a validation error.

### P1

**As a** staff member (cashier), **I want** to record sales transactions by scanning products and specifying quantity sold, **so that** my sales can be tracked for the daily stock reduction report.

> **Acceptance:** Staff can scan a product barcode or search for product, enter quantity sold (positive integer), and submit. System records transaction with timestamp, staff ID, product ID, and quantity. Stock is reduced accordingly. Transactions are included in the daily stock reduction report.

**As a** staff member, **I want** to see a brief success indicator after any stock update, **so that** I'm confident the change was saved.

> **Acceptance:** After a successful stock update, the stock count briefly flashes green or shows a checkmark indicator. The indicator disappears after 1-2 seconds. No indicator appears on error responses.

**As a** manager, **I want** stock updates made on one device to be visible on another device after a page refresh, **so that** all staff see consistent inventory data.

> **Acceptance:** Stock data is stored server-side. Refreshing the page on any device fetches the latest data from the database. No real-time sync (WebSocket) in v1 — page refresh is the sync mechanism.

**As a** manager, **I want** to receive a daily stock reduction report from staff (cashiers) after business closing, **so that** I can update stock levels for the next business day based on actual sales transactions.

> **Acceptance:** Staff can generate a daily stock report showing reductions from starting stock. Manager reviews the report and applies stock adjustments. System records adjustments with timestamp and staff ID for audit trail.

**As a** manager, **I want** to apply bulk stock adjustments based on the daily report, **so that** I can update stock numbers after reviewing transactions from the day.

> **Acceptance:** From the stock report view, manager can select items and apply stock adjustments. Adjustments update the official stock count in the system. Manager receives confirmation of applied changes.

---

## User Management

### P0

**As a** manager, **I want** to create new staff accounts with a username and temporary password, **so that** new hires can log in and start using the app.

> **Acceptance:** Create user form with username and password fields. No role selector — manager always creates staff accounts. Username must be unique. Shows error on duplicate. On success, the new user appears in the user list.

**As a** manager, **I want** to deactivate a staff account, **so that** a departing employee can no longer access the app.

> **Acceptance:** Deactivate button on each staff user row. Confirmation dialog before deactivation. Deactivated users cannot log in (403 error). The deactivated user's row shows an "inactive" status badge.

**As a** manager, **I want** to reactivate a previously deactivated staff account, **so that** I can restore access if needed.

> **Acceptance:** Reactivate button on deactivated user rows. Clicking it sets the `active` flag back to 1. The user can log in again.

**As a** manager, **I want** to reset a staff member's password, **so that** they can log in if they forget their credentials.

> **Acceptance:** Reset password button opens a prompt for a new password. On submit, sends PATCH to update the password hash. Shows confirmation. Minimum 8-character validation.

### P1

**As a** manager, **I want** to see a list of all staff accounts with their status, **so that** I can manage the team.

> **Acceptance:** User list table with columns: Username, Role (badge), Status (active/deactivated badge). Manager sees only staff-role accounts. Admin sees all accounts including other managers.

**As an** admin, **I want** to create manager accounts, **so that** I can delegate user management to supervisors.

> **Acceptance:** Admin sees a role selector in the create user form with options: staff and manager. Admin creates manager accounts via the same form. Manager role badge shows as blue, staff as gray, admin as purple.

**As an** admin, **I want** to deactivate or reset passwords for any user including managers, **so that** I have full control over access.

> **Acceptance:** Admin's user list shows all users. Admin can deactivate/reactivate/reset-password for any user regardless of role. Admin cannot create another admin via the API (admin creation is limited to QR onboarding).

**As an** admin, **I want** to create custom roles with specific permissions, **so that** I can match system roles to our actual job descriptions and responsibilities.

> **Acceptance:** Admin can access a role management interface to create new role names and select/deselect specific permissions (menu items, features) for each role. Custom roles can be saved and assigned to users.

**As an** admin, **I want** to assign multiple roles to a single user, **so that** I can accommodate users who perform multiple job functions.

> **Acceptance:** When editing a user, admin can select multiple roles from a list of available roles. The user inherits permissions from all assigned roles. The system prevents conflicting permission assignments.

**As an** admin, **I want** to view a permission audit trail showing who has access to what features, **so that** I can verify compliance and troubleshoot access issues.

> **Acceptance:** Admin can access a permission report showing each user, their assigned roles, and the specific permissions granted. The report can be filtered by user, role, or permission.

### P2

**As a** manager, **I want** to see a confirmation dialog before deactivating a user, **so that** I don't accidentally remove access.

> **Acceptance:** Deactivation triggers a modal: "Are you sure you want to deactivate [username]? They will not be able to log in." Confirm and Cancel buttons.

**As a** manager, **I want** the create-user form to show validation feedback immediately, **so that** I know if the username is taken or the password is too short.

> **Acceptance:** Inline validation: duplicate username shows "Username already exists" on the field. Short password shows "Password must be at least 8 characters." Button stays disabled until form is valid.

---

## Feedback & Analytics

### P1

**As a** manager, **I want** to collect customer feedback on their experience with the webapp, **so that** I can understand reasons for low transaction periods and improve the system.

> **Acceptance:** Staff can input customer feedback (e.g., via a simple form) after assisting a customer or observing low engagement. Feedback includes rating (1-5 stars), optional comments, and category (e.g., usability, product availability, staff help). Manager can view a feedback dashboard showing trends over time, filter by date/category, and export data for analysis.

**As a** staff member (cashier), **I want** to quickly assess and record customer sentiment during or after transactions (e.g., satisfied, neutral, frustrated) based on verbal and non-verbal cues, **so that** management can correlate sentiment with transaction volume and identify service issues proactively.

> **Acceptance:** Cashiers can select a sentiment indicator (e.g., emoji or short label) with minimal taps after or during a customer interaction. Sentiment data is stored with timestamp, staff ID, and optional transaction context. Managers can view sentiment trends alongside transaction volume and explicit feedback in the analytics dashboard.

---

## Role & Navigation

### P0

**As a** staff member, **I want** the sidebar to show links only for Dashboard, Products, and Scan, **so that** I see only the tools I need.

> **Acceptance:** Staff sidebar: Dashboard, Products, Scan. No Users link. Username/role badge and logout button in footer.

**As a** manager, **I want** the sidebar to show links only for Dashboard and Users, **so that** I focus on staff management.

> **Acceptance:** Manager sidebar: Dashboard, Users. No Products or Scan links. Username/role badge and logout button in footer.

**As an** admin, **I want** the sidebar to show links for Dashboard, Products, Scan, and Users, **so that** I can access all features.

> **Acceptance:** Admin sidebar: Dashboard, Products, Scan, Users. Full navigation. Username/role badge and logout button in footer.

### P1

**As any** user, **I want** to see my current username and role displayed in the sidebar, **so that** I can confirm which account I'm logged into.

> **Acceptance:** Sidebar footer shows the logged-in user's username and role as a colored badge (admin=purple, manager=blue, staff=gray).

**As a** staff member, **I want** to see a clear "access denied" message if I try to navigate to a page I don't have permission for, **so that** I understand why it's blocked.

> **Acceptance:** If a staff member navigates to /users or any user-management page, the client shows "Access denied — you don't have permission to view this page" and a link back to the products view. The server also returns 403 on the underlying API call.

**As a** manager, **I want** to see an "access denied" message if I try to navigate to product or scan views, **so that** I understand the limitation.

> **Acceptance:** Manager navigating to Products or Scan views sees an access-denied message with a link back to the user management view.

---

### Summary

| Domain | P0 | P1 | P2 | Total |
|--------|----|----|----|-------|
| Auth & Onboarding | 6 | 3 | 1 | 10 |
| Scanning | 6 | 2 | 1 | 9 |
| Products | 3 | 2 | 0 | 5 |
| Stock | 3 | 4 | 0 | 7 |
| User Management | 5 | 5 | 2 | 12 |
| Feedback & Analytics | 0 | 2 | 0 | 2 |
| Role & Navigation | 3 | 2 | 0 | 5 |
| **Total** | **26** | **20** | **4** | **50** |