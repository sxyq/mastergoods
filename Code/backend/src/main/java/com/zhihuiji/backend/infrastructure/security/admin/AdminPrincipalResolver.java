package com.zhihuiji.backend.infrastructure.security.admin;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhihuiji.backend.domain.entity.AdminScopeGrantEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAccountRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeGrantRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import java.util.HashSet;

/**
 * Resolves an administrator only when a trusted authentication component has
 * already placed an {@link AdminPrincipal} in the security context.
 *
 * <p>The token filter places a trusted session user ID in the context. This
 * resolver maps that ID through the server-owned administrator account and
 * scope tables; request parameters never participate in role resolution.</p>
 */
@Component
public class AdminPrincipalResolver {
    private final AdminAccountRepository accountRepository;
    private final AdminScopeGrantRepository scopeGrantRepository;
    private final StoreRepository storeRepository;

    /** Retained for focused unit tests that place an AdminPrincipal directly. */
    public AdminPrincipalResolver() {
        this.accountRepository = null;
        this.scopeGrantRepository = null;
        this.storeRepository = null;
    }

    @Autowired
    public AdminPrincipalResolver(
        AdminAccountRepository accountRepository,
        AdminScopeGrantRepository scopeGrantRepository,
        StoreRepository storeRepository
    ) {
        this.accountRepository = accountRepository;
        this.scopeGrantRepository = scopeGrantRepository;
        this.storeRepository = storeRepository;
    }

    public AdminPrincipal requireCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken
            || authentication.getPrincipal() == null) {
            throw new AdminAuthenticationRequiredException();
        }
        if (authentication.getPrincipal() instanceof AdminPrincipal principal) {
            return principal;
        }
        Object subject = authentication.getPrincipal();
        if (subject instanceof Number number) return resolveUserId(number.longValue());
        if (subject instanceof String text) {
            try { return resolveUserId(Long.parseLong(text)); }
            catch (NumberFormatException ignored) { }
        }
        throw new AccessDeniedException("administrator account required");
    }

    /** Resolves the database-backed administrator account from the trusted user ID. */
    public AdminPrincipal resolveUserId(Long userId) {
        if (accountRepository == null || scopeGrantRepository == null || userId == null || userId <= 0) {
            throw new AccessDeniedException("administrator authentication required");
        }
        var account = accountRepository.findByUserIdAndStatus(userId, 1)
            .orElseThrow(() -> new AccessDeniedException("administrator authentication required"));
        AdminPrincipal.AdminRole role;
        try {
            role = AdminPrincipal.AdminRole.valueOf(account.getRoleCode());
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("administrator role is invalid");
        }
        if (role == AdminPrincipal.AdminRole.SUPER_ADMIN) {
            return AdminPrincipal.forRole(userId, role,
                AdminDataScope.allOwners(true, AdminDataScope.ContentMode.AUTHORIZED));
        }
        var ownerIds = new HashSet<Long>();
        var storeIds = new HashSet<Long>();
        for (AdminScopeGrantEntity grant : scopeGrantRepository.findAllByAdminAccountIdAndStatusOrderByIdAsc(account.getId(), 1)) {
            if (grant.getOwnerUserId() != null) ownerIds.add(grant.getOwnerUserId());
            if (grant.getStoreId() != null) {
                storeIds.add(grant.getStoreId());
                if (storeRepository != null) {
                    storeRepository.findById(grant.getStoreId())
                        .map(store -> store.getOwnerUserId())
                        .ifPresent(ownerIds::add);
                }
            }
        }
        return AdminPrincipal.forRole(userId, role,
            new AdminDataScope(false, ownerIds, storeIds, false, AdminDataScope.ContentMode.REDACTED));
    }
}
