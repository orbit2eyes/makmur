package com.makmur.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigration {

    private final JdbcTemplate jdbc;

    public SchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void migrate() {
        // Check if old schema (has 'password' column) is in use
        boolean needsMigration;
        try {
            jdbc.queryForObject("SELECT password FROM users LIMIT 1", String.class);
            needsMigration = true;
        } catch (Exception e) {
            needsMigration = false;
        }

        if (needsMigration) {
            jdbc.execute("""
                CREATE TABLE users_new (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    role          TEXT    NOT NULL DEFAULT 'staff' CHECK(role IN ('admin', 'manager', 'staff')),
                    active        INTEGER NOT NULL DEFAULT 1,
                    created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                )
            """);
            jdbc.update("""
                INSERT INTO users_new (id, username, password_hash, role, active, created_at)
                SELECT id, username, password, role, 1, created_at FROM users
            """);
            jdbc.execute("DROP TABLE users");
            jdbc.execute("ALTER TABLE users_new RENAME TO users");
            System.out.println("Schema migration: users table updated (password→password_hash, added active column, extended role CHECK)");
        }
    }
}
