package com.makmur.service;

import com.makmur.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AuthService {

    /**
     * Checks that the current SecurityContext user has one of the allowed roles.
     * @throws ForbiddenException if the caller's role is not in the allowed set
     * @return the Authentication object for callers that need userId/role context
     */
    public Authentication requireRole(String... allowedRoles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Access denied");
        }

        List<String> allowed = Arrays.asList(allowedRoles);
        for (GrantedAuthority authority : auth.getAuthorities()) {
            // authorities are stored as "ROLE_ADMIN", "ROLE_STAFF", etc.
            String role = authority.getAuthority().replace("ROLE_", "").toLowerCase();
            if (allowed.contains(role)) {
                return auth;
            }
        }

        throw new ForbiddenException("Access denied");
    }

    /**
     * Returns the current user's role from SecurityContext.
     */
    public String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .findFirst().orElse(null);
    }

    /**
     * Returns the current user's userId from SecurityContext details.
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof Long)) return null;
        return (Long) auth.getDetails();
    }
}
