package com.makmur.controller;

import com.makmur.entity.User;
import com.makmur.exception.ForbiddenException;
import com.makmur.repository.UserRepository;
import com.makmur.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<?> listUsers() {
        authService.requireRole("admin", "manager");

        String callerRole = authService.getCurrentRole();
        List<User> users;
        if ("admin".equals(callerRole)) {
            users = userRepository.findAll();
        } else {
            // Manager sees only staff
            users = userRepository.findAllByRole("staff");
        }

        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("role", u.getRole());
            m.put("active", u.isActive());
            m.put("created_at", u.getCreatedAt());
            return m;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        authService.requireRole("admin", "manager");

        String username = body.get("username");
        String password = body.get("password");
        String requestedRole = body.getOrDefault("role", "staff");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("message", "Username and password are required");
            return ResponseEntity.status(422).body(err);
        }

        String callerRole = authService.getCurrentRole();
        String newRole;
        if ("manager".equals(callerRole)) {
            newRole = "staff";
        } else {
            // admin can create staff or manager
            if (!"staff".equals(requestedRole) && !"manager".equals(requestedRole)) {
                newRole = "staff";
            } else {
                newRole = requestedRole;
            }
        }

        if (userRepository.findByUsername(username).isPresent()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "duplicate_username");
            err.put("message", "Username " + username + " already exists");
            return ResponseEntity.status(409).body(err);
        }

        User user = new User(username, passwordEncoder.encode(password), newRole);
        User saved = userRepository.save(user);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", saved.getId());
        userInfo.put("username", saved.getUsername());
        userInfo.put("role", saved.getRole());
        userInfo.put("active", saved.isActive());
        userInfo.put("created_at", saved.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(userInfo);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        authService.requireRole("admin", "manager");
        checkTargetRole(id);
        userRepository.updateActiveStatus(id, false);
        return ResponseEntity.ok(Map.of("id", id, "active", false));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {
        authService.requireRole("admin", "manager");
        checkTargetRole(id);
        userRepository.updateActiveStatus(id, true);
        return ResponseEntity.ok(Map.of("id", id, "active", true));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        authService.requireRole("admin", "manager");

        String newPassword = body.get("new_password");
        if (newPassword == null || newPassword.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("message", "new_password is required");
            return ResponseEntity.status(422).body(err);
        }

        checkTargetRole(id);
        userRepository.updatePassword(id, passwordEncoder.encode(newPassword));
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    /**
     * For manager callers: ensure the target user is a staff member.
     * Admin can act on any user (including managers).
     */
    private void checkTargetRole(Long targetId) {
        String callerRole = authService.getCurrentRole();
        if ("admin".equals(callerRole)) return;

        // Manager can only act on staff users
        User target = userRepository.findByUsername(
            userRepository.findAll().stream()
                .filter(u -> u.getId().equals(targetId))
                .findFirst().map(User::getUsername)
                .orElse(null)
        ).orElse(null);

        if (target == null) {
            throw new ForbiddenException("User not found");
        }
        if (!"staff".equals(target.getRole())) {
            throw new ForbiddenException("You can only manage staff accounts");
        }
    }
}
