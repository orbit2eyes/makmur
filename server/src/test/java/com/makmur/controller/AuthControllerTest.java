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
class AuthControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String adminToken;
    private String managerToken;
    private String staffToken;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port;

        // Clean tables
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM products");

        // Seed standard users
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "admin", passwordEncoder.encode("admin123"), "admin", 1);
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "manager1", passwordEncoder.encode("pass1234"), "manager", 1);
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "staff1", passwordEncoder.encode("pass1234"), "staff", 1);
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "deactivated1", passwordEncoder.encode("pass1234"), "staff", 0);

        // Get tokens
        adminToken = login("admin", "admin123");
        managerToken = login("manager1", "pass1234");
        staffToken = login("staff1", "pass1234");
    }

    private String login(String username, String password) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        ResponseEntity<Map> resp = rest.postForEntity(baseUrl + "/api/auth/login", body, Map.class);
        if (resp.getStatusCode() == HttpStatus.OK) {
            return (String) resp.getBody().get("token");
        }
        return null;
    }

    // ── Login tests ─────────────────────────────────────────────────────

    @Test
    void a_login_validCredentials_returns200() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "admin");
        body.put("password", "admin123");

        ResponseEntity<Map> resp = rest.postForEntity(baseUrl + "/api/auth/login", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("token");
        assertThat(resp.getBody()).containsKey("user");

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) resp.getBody().get("user");
        assertThat(user).containsEntry("username", "admin");
        assertThat(user).containsEntry("role", "admin");
        assertThat(user).containsKey("id");
    }

    @Test
    void b_login_invalidPassword_returns401() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "admin");
        body.put("password", "wrongpassword");

        ResponseEntity<Map> resp = rest.postForEntity(baseUrl + "/api/auth/login", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsEntry("error", "invalid_credentials");
        assertThat(resp.getBody()).containsEntry("message", "Invalid username or password");
    }

    @Test
    void c_login_unknownUser_returns401() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "nonexistent");
        body.put("password", "anything");

        ResponseEntity<Map> resp = rest.postForEntity(baseUrl + "/api/auth/login", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsEntry("error", "invalid_credentials");
        // Same message as invalid password — no user enumeration
        assertThat(resp.getBody()).containsEntry("message", "Invalid username or password");
    }

    @Test
    void d_login_deactivatedUser_returns403() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "deactivated1");
        body.put("password", "pass1234");

        ResponseEntity<Map> resp = rest.postForEntity(baseUrl + "/api/auth/login", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("error", "account_disabled");
        assertThat(resp.getBody()).containsEntry("message", "Account is deactivated. Contact your manager.");
    }

    @Test
    void e_login_blankFields_returns422() {
        // Empty body
        ResponseEntity<Map> resp = rest.postForEntity(
                baseUrl + "/api/auth/login", Map.of(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).containsEntry("error", "validation_error");
    }

    // ── Register tests ──────────────────────────────────────────────────

    @Test
    void f_register_asManager_createsStaff() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "newstaff");
        body.put("password", "staffpass1");
        body.put("role", "manager"); // Should be ignored — manager can only create staff

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/auth/register",
                HttpMethod.POST,
                authRequest(body, managerToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("username", "newstaff");
        assertThat(resp.getBody()).containsEntry("role", "staff"); // forced to staff
    }

    @Test
    void g_register_asAdmin_createsManager() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "newmanager");
        body.put("password", "mgrpass12");
        body.put("role", "manager");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/auth/register",
                HttpMethod.POST,
                authRequest(body, adminToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("username", "newmanager");
        assertThat(resp.getBody()).containsEntry("role", "manager");
    }

    @Test
    void h_register_asAdmin_createsStaff() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "newstaff2");
        body.put("password", "staffpass2");
        body.put("role", "staff");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/auth/register",
                HttpMethod.POST,
                authRequest(body, adminToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("username", "newstaff2");
        assertThat(resp.getBody()).containsEntry("role", "staff");
    }

    @Test
    void i_register_duplicateUsername_returns409() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "staff1"); // already exists
        body.put("password", "staffpass1");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/auth/register",
                HttpMethod.POST,
                authRequest(body, adminToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).containsEntry("error", "duplicate_username");
    }

    @Test
    void j_register_noAuth_returns403() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", "someuser");
        body.put("password", "somepass");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/auth/register",
                HttpMethod.POST,
                authRequest(body, null), // no auth
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private HttpEntity<?> authRequest(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }
}
