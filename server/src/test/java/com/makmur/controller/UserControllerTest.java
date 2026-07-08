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
class UserControllerTest {

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

        // Seed users: admin, manager, staff
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "admin", passwordEncoder.encode("admin123"), "admin", 1);
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "manager1", passwordEncoder.encode("pass1234"), "manager", 1);
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "staff1", passwordEncoder.encode("pass1234"), "staff", 1);
        jdbc.update("INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                "staff2", passwordEncoder.encode("pass1234"), "staff", 1);

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

    // ── List users ──────────────────────────────────────────────────────

    @Test
    void a_listUsers_asAdmin_returnsAll() {
        ResponseEntity<Map[]> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                authRequest(null, adminToken),
                Map[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // Should see admin, manager1, staff1, staff2 = 4 users
        assertThat(resp.getBody()).hasSize(4);

        Set<String> usernames = new java.util.HashSet<>();
        for (Map u : resp.getBody()) {
            usernames.add((String) u.get("username"));
        }
        assertThat(usernames).contains("admin", "manager1", "staff1", "staff2");
    }

    @Test
    void b_listUsers_asManager_returnsOnlyStaff() {
        ResponseEntity<Map[]> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                authRequest(null, managerToken),
                Map[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // Manager should see only staff users
        Set<String> roles = new java.util.HashSet<>();
        Set<String> usernames = new java.util.HashSet<>();
        for (Map u : resp.getBody()) {
            roles.add((String) u.get("role"));
            usernames.add((String) u.get("username"));
        }
        assertThat(roles).containsOnly("staff");
        assertThat(usernames).doesNotContain("admin", "manager1");
        assertThat(usernames).contains("staff1", "staff2");
    }

    @Test
    void c_listUsers_asStaff_returns403() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                authRequest(null, staffToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("error", "forbidden");
    }

    @Test
    void d_listUsers_noAuth_returns401() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                authRequest(null, null),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Create users ────────────────────────────────────────────────────

    @Test
    void e_createUser_asManager_createsStaff() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "newstaff3");
        body.put("password", "pass1234");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                authRequest(body, managerToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("username", "newstaff3");
        assertThat(resp.getBody()).containsEntry("role", "staff");
    }

    @Test
    void f_createUser_asManager_roleForcedToStaff() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "newstaff4");
        body.put("password", "pass1234");
        body.put("role", "manager"); // manager should not be able to create manager

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                authRequest(body, managerToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Role should be forced to staff
        assertThat(resp.getBody()).containsEntry("role", "staff");
    }

    @Test
    void g_createUser_asAdmin_createsManager() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "newmanager2");
        body.put("password", "pass1234");
        body.put("role", "manager");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                authRequest(body, adminToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("username", "newmanager2");
        assertThat(resp.getBody()).containsEntry("role", "manager");
    }

    @Test
    void h_createUser_asStaff_returns403() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "cantcreate");
        body.put("password", "pass1234");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                authRequest(body, staffToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void i_createUser_duplicateUsername_returns409() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "staff1"); // already exists
        body.put("password", "pass1234");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                authRequest(body, adminToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).containsEntry("error", "duplicate_username");
    }

    // ── Deactivate / Reactivate ─────────────────────────────────────────

    @Test
    void j_deactivate_asManager_onStaff_returns200() {
        // staff1 has id=3 (after setup order)
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users/" + getUserId("staff1") + "/deactivate",
                HttpMethod.PATCH,
                authRequest(null, managerToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("active", false);

        // Verify deactivation persisted
        Integer active = jdbc.queryForObject(
                "SELECT active FROM users WHERE username = ?", Integer.class, "staff1");
        assertThat(active).isZero();
    }

    @Test
    void k_deactivate_asManager_onManager_returns403() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users/" + getUserId("manager1") + "/deactivate",
                HttpMethod.PATCH,
                authRequest(null, managerToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void l_reactivate_asManager_returns200() {
        // First deactivate
        jdbc.update("UPDATE users SET active = 0 WHERE username = ?", "staff1");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users/" + getUserId("staff1") + "/reactivate",
                HttpMethod.PATCH,
                authRequest(null, managerToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("active", true);
    }

    @Test
    void m_deactivate_asStaff_returns403() {
        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users/" + getUserId("staff2") + "/deactivate",
                HttpMethod.PATCH,
                authRequest(null, staffToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Reset password ──────────────────────────────────────────────────

    @Test
    void n_resetPassword_returns200() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("new_password", "newpass123");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users/" + getUserId("staff1") + "/reset-password",
                HttpMethod.PATCH,
                authRequest(body, adminToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("message", "Password updated");

        // Verify login with new password works
        Map<String, String> loginBody = new LinkedHashMap<>();
        loginBody.put("username", "staff1");
        loginBody.put("password", "newpass123");
        ResponseEntity<Map> loginResp = rest.postForEntity(
                baseUrl + "/api/auth/login", loginBody, Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void o_resetPassword_asStaff_returns403() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("new_password", "newpass123");

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl + "/api/users/" + getUserId("staff2") + "/reset-password",
                HttpMethod.PATCH,
                authRequest(body, staffToken),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private Long getUserId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private HttpEntity<?> authRequest(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }
}
