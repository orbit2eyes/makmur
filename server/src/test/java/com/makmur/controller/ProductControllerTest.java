package com.makmur.controller;

import com.makmur.entity.Product;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodName.class)
class ProductControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String token;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port;

        // Clean products and users between tests
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM users");

        // Seed two standard products used by most tests
        jdbc.update("INSERT INTO products (barcode, name, price, stock) VALUES (?, ?, ?, ?)",
                "5901234567890", "Test Product", 10.50, 100);
        jdbc.update("INSERT INTO products (barcode, name, price, stock) VALUES (?, ?, ?, ?)",
                "4901234567890", "Another Product", 25.00, 50);

        // Seed admin user
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "admin", passwordEncoder.encode("admin123"), "admin", 1);

        // Authenticate to get token for protected endpoints
        Map<String, String> loginBody = new LinkedHashMap<>();
        loginBody.put("username", "admin");
        loginBody.put("password", "admin123");
        ResponseEntity<Map> loginResp = rest.postForEntity(
                baseUrl + "/api/auth/login", loginBody, Map.class);
        token = (String) loginResp.getBody().get("token");
    }

    // ── Health (no auth) ────────────────────────────────────────────────

    @Test
    void a_health_returnsOk() {
        ResponseEntity<Map> resp = rest.getForEntity(baseUrl + "/api/health", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "ok");
    }

    // ── Auth ────────────────────────────────────────────────────────────

    @Test
    void b_login_validCredentials_returnsToken() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "admin");
        body.put("password", "admin123");

        ResponseEntity<Map> resp = rest.postForEntity(baseUrl + "/api/auth/login", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("token");
        assertThat(resp.getBody()).containsKey("user");
    }

    @Test
    void c_login_invalidCredentials_returns401() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "admin");
        body.put("password", "wrong");

        ResponseEntity<Map> resp = rest.postForEntity(baseUrl + "/api/auth/login", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsEntry("error", "invalid_credentials");
    }

    // ── Create product ──────────────────────────────────────────────────

    @Test
    void d_createProduct_returns201() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("barcode", "1234567890123");
        body.put("name", "New Product");
        body.put("price", 15.99);
        body.put("stock", 10);

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                authRequest(body),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("barcode", "1234567890123");
        assertThat(resp.getBody()).containsEntry("name", "New Product");
        assertThat(resp.getBody()).containsEntry("stock", 10);
        assertThat(resp.getBody()).containsKey("id");
        assertThat(resp.getBody()).containsKey("created_at");
    }

    @Test
    void e_createProduct_duplicateBarcode_returns409() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("barcode", "5901234567890");   // already seeded
        body.put("name", "Duplicate");
        body.put("price", 5.00);
        body.put("stock", 1);

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                authRequest(body),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).containsEntry("error", "duplicate_barcode");
    }

    @Test
    void f_createProduct_invalidFields_returns422() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("barcode", "invalid");    // not 13 digits
        body.put("name", "");              // empty name
        body.put("price", -1);             // negative price

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                authRequest(body),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).containsEntry("error", "validation_error");
        // Check that field-level errors exist for invalid fields (note: price -1 may not trigger, only name+barcode)
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) resp.getBody().get("fields");
        assertThat(fields).isNotNull();
        // barcode invalid (not 13 digits) and name empty should be flagged
        assertThat(fields).containsKey("barcode");
        assertThat(fields).containsKey("name");
    }

    // ── List products ───────────────────────────────────────────────────

    @Test
    void g_listProducts_returnsList() {
        ResponseEntity<Product[]> resp = rest.exchange(
                baseUrl + "/api/products",
                HttpMethod.GET,
                authRequest(null),
                Product[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
        // Should be sorted alphabetically by name
        assertThat(resp.getBody()[0].getName()).isEqualTo("Another Product");
        assertThat(resp.getBody()[1].getName()).isEqualTo("Test Product");
    }

    // ── Get by barcode ──────────────────────────────────────────────────

    @Test
    void h_getProduct_returnsProduct() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products/5901234567890",
                HttpMethod.GET,
                authRequest(null),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("barcode", "5901234567890");
        assertThat(resp.getBody()).containsEntry("name", "Test Product");
    }

    @Test
    void i_getProduct_unknown_returns404() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products/9999999999999",
                HttpMethod.GET,
                authRequest(null),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("error", "not_found");
    }

    // ── Search ──────────────────────────────────────────────────────────

    @Test
    void j_search_returnsFilteredResults() {
        ResponseEntity<Product[]> resp = rest.exchange(
                baseUrl + "/api/products/search?q=test",
                HttpMethod.GET,
                authRequest(null),
                Product[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody()[0].getName()).isEqualTo("Test Product");
    }

    @Test
    void k_search_tooShort_returns422() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products/search?q=x",
                HttpMethod.GET,
                authRequest(null),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).containsEntry("error", "validation_error");
    }

    // ── Stock update ────────────────────────────────────────────────────

    @Test
    void l_updateStock_value_updatesAbsolute() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("value", 42);

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products/5901234567890/stock",
                HttpMethod.PATCH,
                authRequest(body),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("barcode", "5901234567890");
        assertThat(resp.getBody()).containsEntry("stock", 42);
        assertThat(resp.getBody()).containsEntry("previous_stock", 100);
    }

    @Test
    void m_updateStock_delta_updatesRelative() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("delta", 5);

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products/5901234567890/stock",
                HttpMethod.PATCH,
                authRequest(body),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("barcode", "5901234567890");
        assertThat(resp.getBody()).containsEntry("stock", 105);
        assertThat(resp.getBody()).containsEntry("previous_stock", 100);
    }

    @Test
    void n_updateStock_negativeResult_returns422() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("delta", -999);

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products/5901234567890/stock",
                HttpMethod.PATCH,
                authRequest(body),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).containsEntry("error", "validation_error");
    }

    // ── Scope enforcement ───────────────────────────────────────────────

    @Test
    void o_scope_staffOnProduct_returns200() {
        // Log in as staff
        Map<String, String> loginBody = new LinkedHashMap<>();
        loginBody.put("username", "staff1");
        loginBody.put("password", "pass1234");
        // Create staff user first
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "staff1", passwordEncoder.encode("pass1234"), "staff", 1);
        ResponseEntity<Map> loginResp = rest.postForEntity(
                baseUrl + "/api/auth/login", loginBody, Map.class);
        String staffToken = (String) loginResp.getBody().get("token");

        ResponseEntity<Product[]> resp = rest.exchange(
                baseUrl + "/api/products",
                HttpMethod.GET,
                authRequestWithToken(null, staffToken),
                Product[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void p_scope_managerOnProduct_returns403() {
        // Log in as manager
        Map<String, String> loginBody = new LinkedHashMap<>();
        loginBody.put("username", "manager1");
        loginBody.put("password", "pass1234");
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "manager1", passwordEncoder.encode("pass1234"), "manager", 1);
        ResponseEntity<Map> loginResp = rest.postForEntity(
                baseUrl + "/api/auth/login", loginBody, Map.class);
        String managerToken = (String) loginResp.getBody().get("token");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products",
                HttpMethod.GET,
                authRequestWithToken(null, managerToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("error", "forbidden");
    }

    @Test
    void q_scope_noJwt_returns403() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/products",
                HttpMethod.GET,
                authRequestWithToken(null, null),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private HttpEntity<?> authRequest(Object body) {
        return authRequestWithToken(body, token);
    }

    private HttpEntity<?> authRequestWithToken(Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return new HttpEntity<>(body, headers);
    }
}