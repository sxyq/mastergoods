package com.zhihuiji.backend.infrastructure.security.admin;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminPrincipalResolverTest {
    private final AdminPrincipalResolver resolver = new AdminPrincipalResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ordinaryAuthenticatedUserIsNotAnAdministrator() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(9L, null, Set.of())
        );

        assertThrows(AccessDeniedException.class, resolver::requireCurrent);
    }

    @Test
    void resolverReturnsOnlyTrustedAdminPrincipalFromSecurityContext() {
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.empty()
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Set.of())
        );

        assertSame(principal, resolver.requireCurrent());
    }
}
