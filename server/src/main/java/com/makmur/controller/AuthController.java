package com.makmur.controller;

import com.makmur.config.JwtUtil;
import com.makmur.entity.User;
import com.makmur.exception.ForbiddenException;
import com.makmur.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("message", "Nama pengguna dan kata sandi wajib diisi");
            return ResponseEntity.status(422).body(err);
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "invalid_credentials");
            err.put("message", "Nama pengguna atau kata sandi salah");
            return ResponseEntity.status(401).body(err);
        }

        if (!user.isActive()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "account_disabled");
            err.put("message", "Akun dinonaktifkan. Hubungi atasan Anda.");
            return ResponseEntity.status(403).body(err);
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("user", userInfo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String requestedRole = body.getOrDefault("role", "staff");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("message", "Nama pengguna dan kata sandi wajib diisi");
            return ResponseEntity.status(422).body(err);
        }

        // Determine the caller's role from SecurityContext
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String callerRole = null;
        if (auth != null && auth.isAuthenticated()) {
            callerRole = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .findFirst().orElse(null);
        }

        if (callerRole == null) {
            throw new ForbiddenException("Akses ditolak");
        }

        // Enforce role assignment rules server-side
        String newRole;
        switch (callerRole) {
            case "manager":
                newRole = "staff";
                break;
            case "admin":
                if (!"staff".equals(requestedRole) && !"manager".equals(requestedRole)) {
                    newRole = "staff";
                } else {
                    newRole = requestedRole;
                }
                break;
            default:
                throw new ForbiddenException("Akses ditolak");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "duplicate_username");
            err.put("message", "Nama pengguna " + username + " sudah ada");
            return ResponseEntity.status(409).body(err);
        }

        User user = new User(username, passwordEncoder.encode(password), newRole);
        User saved = userRepository.save(user);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", saved.getId());
        userInfo.put("username", saved.getUsername());
        userInfo.put("role", saved.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(userInfo);
    }
}