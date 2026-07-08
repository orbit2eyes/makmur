package com.makmur.service;

import com.makmur.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final AuthService authService = new AuthService();

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireRole_allowsCorrectRole() {
        setAuth("admin", "ROLE_ADMIN");

        Authentication result = authService.requireRole("admin", "staff");

        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isEqualTo("admin");
    }

    @Test
    void requireRole_allowsStaffOnProductScope() {
        setAuth("staff1", "ROLE_STAFF");

        Authentication result = authService.requireRole("admin", "staff");

        assertThat(result).isNotNull();
    }

    @Test
    void requireRole_blocksWrongRole() {
        setAuth("manager1", "ROLE_MANAGER");

        assertThatThrownBy(() -> authService.requireRole("admin", "staff"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied");
    }

    @Test
    void requireRole_blocksStaffOnAdminOnly() {
        setAuth("staff1", "ROLE_STAFF");

        assertThatThrownBy(() -> authService.requireRole("admin"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied");
    }

    @Test
    void requireRole_noAuth_blocks() {
        // No SecurityContext set

        assertThatThrownBy(() -> authService.requireRole("admin"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied");
    }

    @Test
    void requireRole_unauthenticated_blocks() {
        // Set an unauthenticated token
        UsernamePasswordAuthenticationToken unauthenticated =
                new UsernamePasswordAuthenticationToken("anon", null, List.of());
        unauthenticated.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        assertThatThrownBy(() -> authService.requireRole("admin"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied");
    }

    @Test
    void getCurrentRole_returnsCorrectRole() {
        setAuth("admin", "ROLE_ADMIN");

        assertThat(authService.getCurrentRole()).isEqualTo("admin");
    }

    @Test
    void getCurrentRole_noAuth_returnsNull() {
        assertThat(authService.getCurrentRole()).isNull();
    }

    @Test
    void getCurrentUserId_returnsUserId() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(42L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(authService.getCurrentUserId()).isEqualTo(42L);
    }

    @Test
    void getCurrentUserId_noAuth_returnsNull() {
        assertThat(authService.getCurrentUserId()).isNull();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void setAuth(String username, String role) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority(role)));
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
