package com.makmur.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SetupTokenStore {

    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        generateToken();
    }

    public synchronized void generateToken() {
        tokens.clear();
        tokens.put(UUID.randomUUID().toString(), Instant.now().plus(60, ChronoUnit.MINUTES));
    }

    public String getToken() {
        String token = tokens.keySet().stream().findFirst().orElse(null);
        if (token == null) return null;
        Instant expires = tokens.get(token);
        if (expires != null && Instant.now().isAfter(expires)) {
            tokens.remove(token);
            return null;
        }
        return token;
    }

    public Instant getExpiresAt() {
        return tokens.values().stream().findFirst().orElse(null);
    }

    public boolean isValid(String token) {
        Instant expires = tokens.get(token);
        if (expires == null) return false;
        if (Instant.now().isAfter(expires)) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public void invalidate(String token) {
        tokens.remove(token);
    }
}
