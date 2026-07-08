package com.makmur.controller;

import com.makmur.config.SetupTokenStore;
import com.makmur.entity.User;
import com.makmur.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final SetupTokenStore tokenStore;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupController(SetupTokenStore tokenStore,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean needsSetup = countAdmins() == 0;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("needsSetup", needsSetup);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/token")
    public ResponseEntity<?> getToken() {
        if (countAdmins() > 0) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "already_setup");
            err.put("message", "Admin account already exists");
            return ResponseEntity.status(403).body(err);
        }

        String token = tokenStore.getToken();
        // If token expired or missing, generate a new one
        if (token == null) {
            tokenStore.generateToken();
            token = tokenStore.getToken();
        }

        Instant expiresAt = tokenStore.getExpiresAt();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("expires_at", expiresAt != null ? expiresAt.toString() : null);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String username = body.get("username");
        String password = body.get("password");

        // Validate input
        if (token == null || token.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("message", "Setup token is required");
            return ResponseEntity.status(422).body(err);
        }

        if (username == null || username.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("message", "Username is required");
            return ResponseEntity.status(422).body(err);
        }

        if (password == null || password.length() < 8) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("message", "Password must be at least 8 characters");
            return ResponseEntity.status(422).body(err);
        }

        // Validate token
        if (!tokenStore.isValid(token)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "invalid_token");
            err.put("message", "Setup token is invalid or expired");
            return ResponseEntity.status(403).body(err);
        }

        // Race-condition guard: check no admin exists yet
        if (countAdmins() > 0) {
            tokenStore.invalidate(token);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "already_setup");
            err.put("message", "Admin account already exists");
            return ResponseEntity.status(403).body(err);
        }

        // Check for duplicate username
        if (userRepository.findByUsername(username).isPresent()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "duplicate_username");
            err.put("message", "Username " + username + " already exists");
            return ResponseEntity.status(409).body(err);
        }

        // Create admin user
        User admin = new User(username, passwordEncoder.encode(password), "admin");
        User saved = userRepository.save(admin);

        // Invalidate token
        tokenStore.invalidate(token);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", saved.getId());
        userInfo.put("username", saved.getUsername());
        userInfo.put("role", saved.getRole());
        userInfo.put("active", saved.isActive());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Admin account created");
        response.put("user", userInfo);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private long countAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> "admin".equals(u.getRole()))
                .count();
    }
}
