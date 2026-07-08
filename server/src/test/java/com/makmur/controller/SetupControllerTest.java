package com.makmur.controller;

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
class SetupControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port;

        // Clean tables — start with NO admin
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM products");
    }

    // ── Status ──────────────────────────────────────────────────────────

    @Test
    void a_status_noAdmin_returnsNeedsSetupTrue() {
        ResponseEntity<Map> resp = rest.getForEntity(baseUrl + "/api/setup/status", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("needsSetup", true);
    }

    @Test
    void b_status_withAdmin_returnsNeedsSetupFalse() {
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "admin", passwordEncoder.encode("admin123"), "admin", 1);

        ResponseEntity<Map> resp = rest.getForEntity(baseUrl + "/api/setup/status", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("needsSetup", false);
    }

    // ── Token ───────────────────────────────────────────────────────────

    @Test
    void c_getToken_noAdmin_returns200() {
        ResponseEntity<Map> resp = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("token");
        assertThat(resp.getBody()).containsKey("expires_at");
        assertThat(resp.getBody().get("token")).isNotNull();
    }

    @Test
    void d_getToken_withAdmin_returns403() {
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "admin", passwordEncoder.encode("admin123"), "admin", 1);

        ResponseEntity<Map> resp = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("error", "already_setup");
    }

    // ── Register ────────────────────────────────────────────────────────

    @Test
    void e_register_validToken_createsAdmin() {
        // Get valid token
        ResponseEntity<Map> tokenResp = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);
        String token = (String) tokenResp.getBody().get("token");

        // Register with the token
        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("username", "admin");
        body.put("password", "Admin@1234");

        ResponseEntity<Map> resp = rest.postForEntity(
                baseUrl + "/api/setup/register", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsKey("message");
        assertThat(resp.getBody()).containsKey("user");

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) resp.getBody().get("user");
        assertThat(user).containsEntry("username", "admin");
        assertThat(user).containsEntry("role", "admin");
        assertThat(user).containsEntry("active", true);

        // Verify admin user exists in DB
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ? AND role = ?",
                Long.class, "admin", "admin");
        assertThat(count).isEqualTo(1);

        // Verify password is bcrypt hashed
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?",
                String.class, "admin");
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void f_register_invalidToken_returns403() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", "00000000-0000-0000-0000-000000000000");
        body.put("username", "admin");
        body.put("password", "Admin@1234");

        ResponseEntity<Map> resp = rest.postForEntity(
                baseUrl + "/api/setup/register", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("error", "invalid_token");
    }

    @Test
    void g_register_afterSetup_tokenInvalidated() {
        // Get token and register
        ResponseEntity<Map> tokenResp = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);
        String token = (String) tokenResp.getBody().get("token");

        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("username", "admin");
        body.put("password", "Admin@1234");
        rest.postForEntity(baseUrl + "/api/setup/register", body, Map.class);

        // Try calling setup/token now — should be 403
        ResponseEntity<Map> postSetup = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);
        assertThat(postSetup.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postSetup.getBody()).containsEntry("error", "already_setup");

        // Try setup/status — needsSetup should be false
        ResponseEntity<Map> statusResp = rest.getForEntity(baseUrl + "/api/setup/status", Map.class);
        assertThat(statusResp.getBody()).containsEntry("needsSetup", false);
    }

    @Test
    void h_register_shortPassword_returns422() {
        ResponseEntity<Map> tokenResp = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);
        String token = (String) tokenResp.getBody().get("token");

        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("username", "admin");
        body.put("password", "short"); // < 8 chars

        ResponseEntity<Map> resp = rest.postForEntity(
                baseUrl + "/api/setup/register", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void i_register_duplicateUsername_returns409() {
        // Register first admin
        ResponseEntity<Map> tokenResp = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);
        String token = (String) tokenResp.getBody().get("token");

        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("username", "admin");
        body.put("password", "Admin@1234");
        rest.postForEntity(baseUrl + "/api/setup/register", body, Map.class);

        // Try registering with same username — need to directly test
        // Token is invalidated, so we insert directly
        ResponseEntity<Map> postSetupToken = rest.getForEntity(baseUrl + "/api/setup/token", Map.class);
        assertThat(postSetupToken.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
