# Testing Procedure — Authorization & Access Control (makmur-0)

## 1. Unit Tests (Existing)

### ProductControllerTest

**File:** `server/src/test/java/com/makmur/controller/ProductControllerTest.java`

A Spring Boot integration test using `TestRestTemplate` on a random port. Covers:

| Test | Method | What it verifies |
|------|--------|------------------|
| `a_health_returnsOk` | `GET /api/health` | Health endpoint returns `{ status: "ok" }` without auth |
| `b_login_validCredentials_returnsToken` | `POST /api/auth/login` | Valid credentials return JWT + user object |
| `c_login_invalidCredentials_returns401` | `POST /api/auth/login` | Wrong password returns 401 with `invalid_credentials` |
| `d_createProduct_returns201` | `POST /api/products` | Valid product creation returns 201 with product data |
| `e_createProduct_duplicateBarcode_returns409` | `POST /api/products` | Duplicate barcode returns 409 |
| `f_createProduct_invalidFields_returns422` | `POST /api/products` | Invalid fields return 422 with field-level errors |
| `g_listProducts_returnsList` | `GET /api/products` | Returns sorted list of all products |
| `h_getProduct_returnsProduct` | `GET /api/products/:barcode` | Lookup by barcode returns product |
| `i_getProduct_unknown_returns404` | `GET /api/products/:barcode` | Unknown barcode returns 404 |
| `j_search_returnsFilteredResults` | `GET /api/products/search` | Search by partial name returns filtered results |
| `k_search_tooShort_returns422` | `GET /api/products/search` | Query < 2 chars returns 422 |
| `l_updateStock_value_updatesAbsolute` | `PATCH /api/products/:barcode/stock` | Absolute value update sets stock |
| `m_updateStock_delta_updatesRelative` | `PATCH /api/products/:barcode/stock` | Delta update increments stock |
| `n_updateStock_negativeResult_returns422` | `PATCH /api/products/:barcode/stock` | Delta that drives stock below 0 returns 422 |

### Tests to Add for Auth (this document covers these)

#### AuthServiceTest

| Test | What it verifies |
|------|------------------|
| `requireRole_allowsCorrectRole` | Admin passes `requireRole("admin", "staff")` |
| `requireRole_blocksWrongRole` | Manager fails `requireRole("admin", "staff")` → `ForbiddenException` |
| `requireRole_noAuth_blocks` | Unauthenticated request fails `requireRole("admin")` → `ForbiddenException` |
| `getCurrentRole_returnsRole` | Returns the correct authority string |
| `getCurrentUserId_returnsUserId` | Returns the userId from auth details |

#### AuthControllerTest

| Test | What it verifies |
|------|------------------|
| `login_validCredentials_returns200` | Username + correct password → 200 + JWT |
| `login_invalidPassword_returns401` | Correct username + wrong password → 401 |
| `login_unknownUser_returns401` | Nonexistent username → 401 (same generic error) |
| `login_deactivatedUser_returns403` | Deactivated account → 403 with `account_disabled` |
| `login_blankFields_returns422` | Missing username/password → 422 |
| `register_asManager_createsStaff` | Manager creates user → role forced to `staff` |
| `register_asAdmin_createsManager` | Admin creates user with role `manager` → 201 |
| `register_asAdmin_createsStaff` | Admin creates user with role `staff` → 201 |
| `register_duplicateUsername_returns409` | Existing username → 409 |

#### UserControllerTest

| Test | What it verifies |
|------|------------------|
| `listUsers_asAdmin_seesAll` | Admin sees all users including managers |
| `listUsers_asManager_seesOnlyStaff` | Manager sees only staff users |
| `listUsers_asStaff_returns403` | Staff blocked from user listing |
| `createUser_asManager_createsStaff` | Manager creates user → role forced to `staff` |
| `createUser_asAdmin_createsManager` | Admin creates manager account |
| `createUser_duplicateUsername_returns409` | Duplicate username → 409 |
| `deactivateUser_asManager_onStaff_returns200` | Manager deactivates staff → 200 |
| `deactivateUser_asManager_onManager_returns403` | Manager blocked from deactivating manager |
| `reactivateUser_returns200` | Reactivation succeeds |
| `resetPassword_returns200` | Password reset succeeds |

#### SetupControllerTest

| Test | What it verifies |
|------|------------------|
| `setupToken_whenNoAdmin_returns200` | Empty users table → token returned |
| `setupToken_whenAdminExists_returns403` | Admin exists → 403 |
| `setupRegister_validToken_createsAdmin` | Valid token + username/password → 201 + admin created |
| `setupRegister_invalidToken_returns403` | Bad token → 403 |
| `setupRegister_afterSetup_tokenInvalidated` | After successful setup, setup token returns 403 |
| `setupRegister_shortPassword_returns422` | Password < 8 chars → 422 |

#### ProductControllerTest — Add scope enforcement

| Test | What it verifies |
|------|------------------|
| `scope_staffOnProduct_returns200` | Staff role → products OK |
| `scope_managerOnProduct_returns403` | Manager role → products blocked |
| `scope_noJwt_returns401` | Missing token → 401 |

---

## 2. Integration Tests

### API Endpoint Test Matrix

Every endpoint should be tested with every relevant role to confirm scope enforcement.

| Endpoint | Admin | Manager | Staff | No Auth |
|----------|-------|---------|-------|---------|
| `GET /api/health` | 200 | 200 | 200 | 200 |
| `POST /api/auth/login` | n/a | n/a | n/a | 200/401 |
| `GET /api/products` | 200 | 403 | 200 | 401 |
| `GET /api/products/search?q=...` | 200 | 403 | 200 | 401 |
| `GET /api/products/:barcode` | 200 | 403 | 200 | 401 |
| `POST /api/products` | 200 | 403 | 200 | 401 |
| `PATCH /api/products/:barcode/stock` | 200 | 403 | 200 | 401 |
| `GET /api/users` | 200 (all) | 200 (staff only) | 403 | 401 |
| `POST /api/users` | 200 | 200 (staff only) | 403 | 401 |
| `PATCH /api/users/:id/deactivate` | 200 | 200 (staff only) | 403 | 401 |
| `PATCH /api/users/:id/reactivate` | 200 | 200 (staff only) | 403 | 401 |
| `PATCH /api/users/:id/reset-password` | 200 | 200 (staff only) | 403 | 401 |
| `GET /api/setup/token` | 403 | — | — | 200/403 |
| `POST /api/setup/register` | 403 | — | — | 201/403 |

### Token Expiry Test

1. Set `JWT_SECRET` and start server
2. Login as any user → receive JWT
3. Decode JWT body (base64), verify `exp` - `iat` ≤ 86400 seconds (24h)
4. Wait for token to expire (or manually verify via unit test calling `validateToken()` on an expired token)
5. Make API request with expired token → expect 401

---

## 3. Manual Testing Procedure

### Prerequisites

- Server running: `cd server && JWT_SECRET=<32+ char secret> mvn spring-boot:run`
- Client running: `cd client && npm run dev`
- A device with a camera (for QR setup flow)
- Two browser windows/sessions (for multi-user testing)

### 3.1 Setup Flow (First-time QR)

#### Test: First visit shows setup page

1. **Ensure** the `users` table is empty (delete database file or use a fresh instance)
2. **Open** `http://localhost:5173` in a browser
3. **Observe** the setup page is displayed (not the login page)
4. **Observe** a QR code image is rendered
5. **Observe** instructions: "Scan this QR code with your phone to set up the admin account"
6. **Expected:** No login form appears. QR code + instructions visible.

#### Test: QR code contains valid token

1. While on the setup page, check the DevTools network tab for `GET /api/setup/token`
2. **Observe** response contains `{ token: "<uuid>", expires_at: "<ISO timestamp>" }`
3. Keep the token value — it's needed for the next test if done via curl

#### Test: Admin registration via setup form

1. Navigate to `http://localhost:5173/?token=<token-from-above>` (mimics scanning QR)
2. **Observe** the setup form: username field, password field, confirm password field
3. **Enter:**
   - Username: `admin`
   - Password: `Admin@1234`
   - Confirm password: `Admin@1234`
4. **Click** submit
5. **Expected:** Redirect to login page with success message "Admin account created!"

#### Test: Setup token invalidated after use

1. After successful setup, try to access `http://localhost:5173/?token=<old-token>`
2. **Expected:** Shows error "Setup token is invalid or expired"
3. Navigate to the home page without token
4. **Expected:** Shows login page (not setup page)

### 3.2 Login Test

#### Test: Valid login redirects to role-appropriate home

1. Open `http://localhost:5173` on a fresh browser
2. **Observe** login page (username field, password field, login button)
3. **Enter** credentials: `admin` / `Admin@1234`
4. **Click** Log In
5. **Expected:** Redirected to dashboard. Admin sees: Dashboard, Products, Scan, Users in sidebar.

#### Test: Invalid password shows generic error

1. On login page, enter `admin` / `wrongpassword`
2. **Click** Log In
3. **Expected:** Error message: "Invalid username or password"
4. **Verify** the error does NOT say whether username or password was wrong (no user enumeration)

#### Test: Nonexistent user shows same generic error

1. On login page, enter `nonexistentuser` / `anything`
2. **Click** Log In
3. **Expected:** Same error message: "Invalid username or password" (identical to wrong password)

#### Test: Blank fields

1. **Click** Log In with empty username and password
2. **Expected:** Validation error or 422 — in-page error message

#### Test: Deactivated account shows distinct error

1. Create a staff user (requires admin/manager)
2. Deactivate that user via the user management UI
3. Log out
4. Log in with the deactivated user's credentials
5. **Expected:** Error message: "Account is deactivated. Contact your manager."

### 3.3 Role Scope Tests

#### Test: Staff → Products only

**Prerequisites:** Create a staff user with the admin account.

1. Logout from admin
2. Log in with staff credentials
3. **Expected:** Sidebar shows: Dashboard, Products, Scan — NO Users link
4. Navigate to `http://localhost:5173/products`
5. **Expected:** Product list renders (if products exist)
6. Try navigating to `http://localhost:5173/users` directly
7. **Expected:** Access denied message or redirect to products; API returns 403

#### Test: Manager → Staff management only

**Prerequisites:** Create a manager account with the admin account.

1. Logout from staff
2. Log in with manager credentials
3. **Expected:** Sidebar shows: Dashboard, Users — NO Products, NO Scan links
4. Navigate to `http://localhost:5173/users`
5. **Expected:** User list renders (staff users only)
6. Navigate to `http://localhost:5173/products`
7. **Expected:** Access denied message; API returns 403

#### Test: Manager can create staff users

1. As manager, go to user management
2. Create a new user: `staff1` / `Staff1234`
3. **Expected:** User appears in the list with role "staff"
4. **Verify** the role selector is NOT available (manager cannot choose role)

#### Test: Manager blocked from deactivating other managers

1. As manager, try to deactivate the admin user or another manager
2. **Expected:** Error message and operation fails; API returns 403

#### Test: Admin → Everything

1. Logout from manager
2. Log in with admin (`admin` / `Admin@1234`)
3. **Expected:** Sidebar shows: Dashboard, Products, Scan, Users
4. Products page works (200)
5. Users page works — shows ALL users including managers
6. User management: create staff, create manager — both succeed

### 3.4 Token Expiry Handling

#### Test: Token expiry detected client-side

1. Log in as any user
2. Open DevTools → Application → Session Storage
3. Locate the token
4. Decode the token's payload (base64-decode the second segment)
5. Note the `exp` timestamp
6. Use JavaScript in console to simulate expiry:
   ```js
   const token segment)
5. Note the `exp` timestamp
6. Use JavaScript in console to simulate expiry:
   ```js
   const token = JSON.parse(atob(localStorage.getItem('token').split('.')[1]));
   // Wait for exp to pass, or modify the stored token exp
   ```
7. Refresh the page
8. **Expected:** Client detects expired token, clears it, redirects to login

#### Test: 401 interception redirects to login

1. Log in as any user
2. **Manually** corrupt the token in sessionStorage (change one character)
3. Navigate to products page
4. **Expected:** API call fails with 401, client redirects to login page

### 3.5 Logout and Re-login

#### Test: Logout clears session

1. Log in as any user
2. Click the Logout button (sidebar footer)
3. **Expected:** Session storage cleared, redirected to login page
4. Try navigating directly to `http://localhost:5173/products`
5. **Expected:** Redirected back to login page (no token)

#### Test: Re-login after logout

1. After logout, enter valid credentials
2. **Expected:** Successful login, redirected to role-appropriate home
3. Session storage shows new token

### 3.6 Concurrent / Multi-device Tests

#### Test: Two users, different roles, same server

1. Log in as admin on Browser A
2. Log in as staff on Browser B (use incognito or different browser)
3. On Browser A: access products OK, users OK
4. On Browser B: access products OK, users blocked
5. **Verify** both sessions share the same database (stock changes from Browser B appear on Browser A)

### 3.7 Edge Cases

#### Test: Server restart preserves DB

1. Create products and users
2. Restart the server
3. Login with existing credentials → OK
4. **Observe** data is still present

#### Test: Setup flow after DB wipe

1. Stop the server
2. Delete the database file
3. Restart the server
4. **Observe** setup flow is active again (fresh QR token generated)

---

## 5. Postman/Newman API Testing

Postman provides a powerful platform for creating, testing, and documenting APIs. Integrating Postman/Newman into our testing strategy allows for executable API tests that can be run in CI/CD pipelines and shared across teams.

### 5.1 Postman Collection Structure
We will organize our API tests into the following collections:

1. **Authentication Collection**
   - Login/logout flows
   - Token validation and refresh
   - Password reset flows
   - Setup/onboarding flow

2. **Product Management Collection**
   - Product CRUD operations
   - Product search and filtering
   - Stock update operations (absolute and delta)
   - Product listing and pagination

3. **User Management Collection** (Admin/Manager only)
   - User creation, retrieval, update, deletion
   - Role assignment and modification
   - User activation/deactivation
   - Password reset for users

4. **Validation and Error Handling Collection**
   - Input validation tests (boundary values, invalid formats)
   - Error response format verification
   - Security testing (authentication bypass attempts, injection tests)

5. **Workflow and Scenario Tests**
   - End-to-end user journeys (scan → product view → stock update)
   - Role-based access control verification
   - Concurrent operation simulations

### 5.2 Environment Configuration
We will maintain separate environments for different deployment stages:

| Environment | Purpose | Variables |
|-------------|---------|-----------|
| `local` | Local development server | `base_url`: http://localhost:3001 |
| `staging` | Staging/pre-production | `base_url`: https://staging-murmur.example.com |
| `production` | Production environment | `base_url`: https://murmur.example.com |

Each environment will include:
- Authentication tokens (as variables to be set during test execution)
- Test user credentials for each role (admin, manager, staff)
- Test data identifiers (product barcodes, user IDs)
- Configuration flags for test-specific behavior

### 5.3 Test Script Examples
Postman tests use JavaScript in the "Tests" tab of each request or folder:

```javascript
// Example: Validate successful login response
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response contains token and user info", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("token");
    pm.expect(jsonData).to.have.property("user");
    pm.expect(jsonData.user).to.have.all.keys("id", "username", "role", "active");
});

pm.test("JWT contains expected claims", function () {
    var token = pm.response.json().token;
    var payload = JSON.parse(atob(token.split('.')[1]));
    pm.expect(payload).to.have.property("sub"); // user ID
    pm.expect(payload).to.have.property("role");
    pm.expect(payload).to.have.property("iat");
    pm.expect(payload).to.have.property("exp");
});

// Example: Verify error response format
pm.test("Error response has correct format", function () {
    if (pm.response.code >= 400) {
        var jsonData = pm.response.json();
        pm.expect(jsonData).to.have.all.keys("error", "message");
        pm.expect(jsonData.error).to.be.a("string");
        pm.expect(jsonData.message).to.be.a("string").that.is.not.empty;
    }
});

// Example: Validate stock update response
pm.test("Stock update returns correct data", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("barcode");
    pm.expect(jsonData).to.have.property("stock");
    pm.expect(jsonData).to.have.property("previous_stock");
    pm.expect(jsonData.stock).to.be.a("number");
    pm.expect(jsonData.previous_stock).to.be.a("number");
});
```

### 5.4 Newman Integration for CI/CD
Newman (Postman's CLI companion) enables running collections in automated environments:

**Basic Usage:**
```bash
newman run postman/collections/murmur-api-tests.postman_collection.json \
    -e postman/environments/local.postman_environment.json \
    --reporters cli,json \
    --reporter-json-export newman-reports/execution-$(date +%Y%m%d-%H%M%S).json
```

**CI/CD Integration Example (in Jenkins/GitLab CI):**
```yaml
test:
  script:
    - npm install -g newman  # Install Newman
    - newman run postman/collections/murmur-api-tests.postman_collection.json \
        -e postman/environments/${CI_ENVIRONMENT_NAME}.postman_environment.json \
        --reporters cli,junit \
        --reporter-junit-export newman-results.xml
  artifacts:
    reports:
      junit: newman-results.xml
```

### 5.5 Test Data Management in Postman
To ensure test isolation and prevent data pollution:

1. **Dynamic Test Data Generation**
   - Use pre-request scripts to generate unique test data:
     ```javascript
     // Generate unique email for test user
     var timestamp = new Date().getTime();
     var randomId = Math.floor(Math.random() * 10000);
     pm.environment.set("testUserEmail", "test." + timestamp + "." + randomId + "@example.com");
     pm.environment.set("testUserUsername", "testuser_" + timestamp + "_" + randomId);
     ```

2. **Data Cleanup in Test Scripts**
   - Chain requests to clean up after themselves:
     ```javascript
     // In the "Tests" tab of a user creation request:
     if (pm.response.code === 201) {
         var userId = pm.response.json().id;
         // Store ID for cleanup in environment or global variable
         pm.environment.set("userToCleanup", userId);
         
         // Add a cleanup request to run after this one
         postman.setNextRequest("Cleanup Test User");
     }
     ```
   - Create dedicated cleanup requests in collections

3. **Environment Scoping**
   - Use environment-specific prefixes for test data:
     ```
     Test User: [STAGING] test_user_12345
     Test Product: [TEST] Temp Product ABC
     ```

### 5.6 Benefits of Postman/Newman Testing
- **Executable Documentation**: Tests serve as live API documentation
- **Visual Debugging**: Rich interface for inspecting requests/responses
- **Team Collaboration**: Easy sharing of test collections across developers/QA
- **CI/CD Integration**: Automated testing in pipelines with clear pass/fail reporting
- **Environment Promotion: 
  - Postman collections can be promoted from dev → staging → production with environment variables
  - Ensures tests run in identical manner across environments

### 5.7 Maintenance and Best Practices
- **Version Control**: Store collections and environments in Git alongside code
- **Review Process**: Include test updates in pull request reviews for API changes
- **Naming Conventions**: Use clear, descriptive names for requests and folders
- **Documentation**: Add descriptions to requests and collections explaining test purpose
- **Regular Audits**: Periodically review tests for relevance and effectiveness

---

### 6.1 Load Testing Scenarios

| Test | Description | Success Criteria |
|------|-------------|------------------|
| Concurrent logins | 50 users logging in simultaneously | All succeed within 2s, no errors |
| Sustained product listing | 100 requests/min to /api/products for 10min | Avg response time < 500ms, 95th percentile < 1s |
| Bulk product creation | Create 1000 products via API | Completed within 30s, no failures |
| Stock update burst | 200 stock updates in 10 seconds | All processed correctly, avg latency < 100ms |
| Search under load | 50 concurrent search requests | Response times consistent with baseline |

### 4.2 Data Volume Testing

| Test | Description | Success Criteria |
|------|-------------|------------------|
| Large product catalog | 50,000 products in database | List/search operations < 2s |
| User base simulation | 5,000 registered users | Login/authentication < 1.5s |
| Transaction history | 100,000 stock transactions | Reporting queries complete in < 5s |
| Session handling | 100 concurrent active sessions | No memory leaks, stable performance |

### 4.3 Resource Utilization

| Test | Description | Success Criteria |
|------|-------------|------------------|
| Memory leak test | 1 hour of continuous operation | Memory increase < 50MB |
| CPU utilization | Simulated peak load (75% capacity) | CPU < 80% average |
| Database connection pool | 50 concurrent DB connections | No connection exhaustion errors |
| Disk I/O | Continuous logging/audit trails | Write latency < 10ms |

### 4.4 Stress Testing

| Test | Description | Success Criteria |
|------|-------------|------------------|
| Beyond capacity | 150% of expected peak load | Graceful degradation, no crashes |
| Recovery test | After overload, return to normal load | Full recovery within 30s |
| Network partition | Simulate intermittent connectivity | Automatic retry, data consistency |
| Long running | 24-hour continuous operation | No performance degradation, clean logs |

### 4.5 Monitoring and Observability

| Test | Description | Success Criteria |
|------|-------------|------------------|
| Metrics collection | Verify key metrics are exposed | Response time, error rate, throughput |
| Alert thresholds | Test alerting on degraded performance | Alerts trigger appropriately |
| Log analysis | Verify sufficient diagnostic info | Errors include context for debugging |
| Health checks | Endpoint returns correct status | /api/health reflects true state |

---

## 5. Error Response Format Tests

All error-related tests must verify that responses conform to the standardized error format defined in the PRD (Section 9.6) and auth spec (REQ-auth-014).

### 5.1 Format Compliance

Every test that expects an error response should verify:

1. The response body is valid JSON.
2. The response body contains exactly two top-level fields: `error` (string) and `message` (string).
3. The `error` field uses snake_case and matches the expected error code for the scenario.
4. The `message` field is a non-empty human-readable string.
5. No additional fields (`stack`, `exception`, `path`, `trace`, `sql`, `details`) are present in the response.

### 5.2 Error Coverage Matrix

| Test | Endpoint | Expected HTTP Status | Expected `error` | Notes |
|------|----------|---------------------|-------------------|-------|
| Missing JWT | Any protected endpoint | 401 | `unauthorized` | Request with no `Authorization` header |
| Expired JWT | Any protected endpoint | 401 | `unauthorized` | Token with `exp` in the past |
| Malformed JWT | Any protected endpoint | 401 | `unauthorized` | Corrupted token string |
| Invalid token signature | Any protected endpoint | 401 | `invalid_token` | Token has valid format but invalid signature |
| Token with wrong audience | Any protected endpoint | 401 | `invalid_token` | Token aud claim doesn't match |
| Token with wrong issuer | Any protected endpoint | 401 | `invalid_token` | Token iss claim doesn't match |
| Deactivated account login | `POST /api/auth/login` | 403 | `account_disabled` | Active flag is `0` |
| Invalid credentials | `POST /api/auth/login` | 401 | `invalid_credentials` | Wrong password or nonexistent user |
| Staff on user endpoint | `GET /api/users` | 403 | `forbidden` | Role lacks scope |
| Manager on product endpoint | `GET /api/products` | 403 | `forbidden` | Role lacks scope |
| Attempt to access non-existent product | `GET /api/products/nonexistent` | 404 | `resource_not_found` |  |
| Attempt to update non-existent product | `PATCH /api/products/nonexistent/stock` | 404 | `resource_not_found` |  |
| Duplicate username | `POST /api/users` | 409 | `conflict` | Username already exists |
| Password too short | `POST /api/setup/register` | 422 | `unprocessable_entity` | Password < 8 chars |
| Invalid role value | `POST /api/users` | 422 | `unprocessable_entity` | Non-admin setting role |
| Setup after admin exists | `GET /api/setup/token` | 403 | `already_setup` | Admin already created |

### 5.3 Security Assertions

For each error response, the test MUST also assert:

- **No user enumeration on 401**: The `message` for invalid credentials must be identical regardless of whether the username exists or the password is wrong. Recommended message: `"Invalid username or password"`.
- **No stack trace on 500**: If an internal server error is triggered intentionally, verify the response body contains `{ error: "internal_error", message: "An unexpected error occurred" }` and does NOT contain any Java class name, file path, line number, or SQL statement.
- **No account existence leak on 403**: The `message` for deactivated accounts must not reveal whether the account would have been valid if active. It must only indicate the account is disabled.

### 5.4 Error Message Clarity and Usability

Error messages should be designed to help users understand what went wrong and how to resolve it, without compromising security. Tests should verify:

| Test | Description | Success Criteria |
|------|-------------|------------------|
| Clear language | Error messages use plain, non-technical language | Messages avoid jargon, acronyms, and internal system terms unless absolutely necessary and explained |
| Actionable guidance | When appropriate, messages suggest next steps | For validation errors: indicate what needs fixing (e.g., "Password must be at least 8 characters") |
| Consistent tone | All error messages follow a consistent style | Similar structure, capitalization, and punctuation across all messages |
| Localization readiness | Messages are designed for easy translation | Avoid culture-specific references, idioms, or complex sentence structures |
| Length appropriateness | Messages are concise but complete | Generally 1-2 sentences; long enough to be helpful, short enough to be quickly understood |
| Specificity balance | Messages are specific enough to be useful but not so specific as to aid attackers | For auth errors: generic ("Invalid credentials"); for validation: specific ("Email must contain @ symbol") |

#### Specific Message Guidelines:

1. **Authentication Errors (401/403)**:
   - Must NOT reveal whether username exists or password is correct
   - Should guide user to check credentials or contact support
   - Example good message: "Invalid username or password. Please check your credentials and try again."

2. **Validation Errors (422)**:
   - Should clearly identify which field failed validation
   - Should specify what is expected (format, length, etc.)
   - Example good message: "Password must be at least 8 characters long and contain at least one number."

3. **Resource Errors (404)**:
   - Should indicate what resource was not found
   - Should suggest alternatives if applicable (create, search again, etc.)
   - Example good message: "Product with barcode '1234567890123' not found. Please verify the barcode or add a new product."

4. **Conflict Errors (409)**:
   - Should explain what conflict occurred
   - Should suggest how to resolve it
   - Example good message: "A user with username 'johnsmith' already exists. Please choose a different username."

5. **Rate Limit Errors (429)**:
   - Should indicate the limit and when to try again
   - Example good message: "Too many requests. Please wait 60 seconds before trying again."

### 5.5 Error Context and Debugging (for developers)

While user-facing messages should be secure and understandable, internal logging and development environments may need more detail. Tests should verify:

- **Development vs Production**: In development mode, additional debug information may be logged (but never returned in API responses)
- **Correlation IDs**: All errors should include a traceable ID in logs (not in response body) for debugging
- **Log sufficiency**: Error logs should contain enough information for developers to diagnose issues without exposing sensitive data
- **Sensitive data masking**: Logs must never contain passwords, tokens, or other sensitive information even in debug mode

---

## 7. Test Cleanup and Teardown Procedures

To ensure tests are repeatable and don't leave residual data that could affect subsequent tests or production data, the following cleanup procedures must be followed:

### 7.1 Data Cleanup After Tests
| Test Type | Cleanup Procedure | Responsibility |
|-----------|-------------------|----------------|
| User creation tests | Delete all test-created users (except admin) after test completion | Test script / teardown hook |
| Product creation tests | Delete all test-created products after test completion | Test script / teardown hook |
| Stock modification tests | Reset stock levels to known baseline values after test | Test verification step |
| Role/permission tests | Restore any modified role assignments or permissions | Test cleanup phase |
| Session/token tests | Clear any test-generated tokens from storage | Client-side cleanup |

### 7.2 Environment Reset
| Component | Reset Procedure | Frequency |
|-----------|-----------------|-----------|
| Database | Restore from known clean snapshot or truncate test tables | Before each test suite run |
| Server state | Restart server to clear in-memory state (setup tokens, caches) | Between test suites if stateful |
| Client storage | Clear localStorage, sessionStorage, cookies | Before each test scenario |
| Log files | Rotate or clear test logs to prevent disk space issues | Daily during test execution |

### 7.3 Automated Teardown Hooks
All automated tests should implement:

**Before Each Test:**
- Verify clean starting state (optional, for verification)
- Reserve/test-specific resource allocation (if needed)

**After Each Test:**
- Delete test-created entities (users, products, etc.)
- Reset modified configuration to baseline
- Clear temporary files or caches
- Validate no unexpected side effects remain

**After Test Suite:**
- Full environment reset to pristine state
- Archive test results and logs
- Notify team of test completion/status

### 7.4 Specific Cleanup Examples

**For User Tests:**
```javascript
// After test: delete test user
await api.delete(`/api/users/${testUserId}`);
// Verify deletion
const response = await api.get(`/api/users/${testUserId}`);
expect(response.status).toBe(404);
```

**For Product Tests:**
```javascript
// After test: delete test product
await api.delete(`/api/products/${testProductBarcode}`);
// Verify deletion
const response = await api.get(`/api/products/${testProductBarcode}`);
expect(response.status).toBe(404);
```

**For Stock Tests:**
```javascript
// After test: reset stock to known value
await api.patch(`/api/products/${testProductBarcode}/stock`, { value: 0 });
// Verify reset
const response = await api.get(`/api/products/${testProductBarcode}`);
expect(response.body.stock).toBe(0);
```

### 7.5 Safety Mechanisms
To prevent accidental production data loss during testing:

1. **Test Data Marking**: All test-created entities should have identifiable markers (e.g., username prefix "test_", product name containing "[TEST]")
2. **Confirmation Prompts**: Interactive cleanup scripts should require confirmation before bulk deletion
3. **Environment Checks**: Tests should verify they're running against a test/staging environment, never production
4. **Backup Before Bulk Operations**: For any cleanup affecting multiple records, backup should be taken first
5. **Audit Trail**: All cleanup actions should be logged for traceability

### 7.6 Validation of Cleanup
After cleanup procedures, validation steps should confirm:
- No test-specific data remains in the system
- System returns to expected baseline state
- No unintended side effects on production-like data
- Resource usage (memory, connections) returns to normal levels

---

| AC | Test Coverage | Verified By |
|----|---------------|-------------|
| AC-15 | Login valid/invalid credentials | `AuthControllerTest`, `ProductControllerTest.b/c` |
| AC-16 | No JWT → 401 | `ProductControllerTest` (via authRequest without token) |
| AC-17 | Expired/malformed JWT → 401 | Manual token manipulation, unit test |
| AC-18 | Staff → products 200, users 403 | `scope_staffOnProduct`, `listUsers_asStaff_returns403` |
| AC-19 | Manager → users 200, products 403 | `scope_managerOnProduct`, `listUsers_asManager_seesOnlyStaff` |
| AC-20 | Admin → everything | `listUsers_asAdmin_seesAll`, scope tests |
| AC-21 | Handler-level scope (not routing) | Architecture review — `requireRole()` called per handler |
| AC-22 | QR onboarding flow | `SetupControllerTest`, manual |
| AC-23 | Token invalidated after setup | `setupRegister_afterSetup after setup | `setupRegister_afterSetup_tokenInvalidated` |
| AC-24 | Bcrypt password storage | `SetupControllerTest` + code review |
| AC-25 | JWT claims: sub, role, iat, exp ≤ 24h | `JwtUtil` code review + test |
| AC-26 | Schema: password_hash, active, manager role | `schema.sql` review + `SchemaMigration` |
| AC-27 | Expiry/logout → redirect to login | Manual test, `AuthContext` code review |
| AC-28 | Deactivated user → 403 login | `login_deactivatedUser_returns403` |
| AC-29 | Login as default, setup as exception | `App.tsx` code review, manual |