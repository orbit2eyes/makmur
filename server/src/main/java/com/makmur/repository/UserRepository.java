package com.makmur.repository;

import com.makmur.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<User> MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setActive(rs.getInt("active") == 1);
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    };

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByUsername(String username) {
        List<User> results = jdbc.query(
            "SELECT * FROM users WHERE username = ?", MAPPER, username);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public User save(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }
        return findByUsername(user.getUsername()).orElse(user);
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM users ORDER BY username ASC", MAPPER);
    }

    public List<User> findAllByRole(String role) {
        return jdbc.query("SELECT * FROM users WHERE role = ? ORDER BY username ASC", MAPPER, role);
    }

    public void updateActiveStatus(Long id, boolean active) {
        jdbc.update("UPDATE users SET active = ? WHERE id = ?", active ? 1 : 0, id);
    }

    public void updatePassword(Long id, String passwordHash) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, id);
    }
}