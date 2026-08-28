package com.zhihuiji.backend.infrastructure.security.admin;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves an administrator only when a trusted authentication component has
 * already placed an {@link AdminPrincipal} in the security context.
 *
 * <p>The current token filter places a regular user ID in the context, so that
 * authentication is deliberately rejected here until an administrator role
 * source is implemented. Request parameters never participate in resolution.</p>
 */
@Component
public class AdminPrincipalResolver {
    public AdminPrincipal requireCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken
            || !(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
            throw new AccessDeniedException("administrator authentication required");
        }
        return principal;
    }
}
