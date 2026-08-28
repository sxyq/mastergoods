package com.zhihuiji.backend.infrastructure.security.admin;

import com.zhihuiji.backend.application.service.admin.AdminScopeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Server-side action and data-scope checks for administrator services. */
@Service
public class AdminAuthorizationService {
    private final AdminScopeService adminScopeService;

    public AdminAuthorizationService(AdminScopeService adminScopeService) {
        this.adminScopeService = adminScopeService;
    }

    public AdminDataScope authorize(
        AdminPrincipal principal,
        AdminPermission permission,
        Long requestedOwnerUserId,
        Long requestedStoreId
    ) {
        requirePermission(principal, permission);
        return adminScopeService.resolve(principal, requestedOwnerUserId, requestedStoreId);
    }

    public void requirePermission(AdminPrincipal principal, AdminPermission permission) {
        if (principal == null || permission == null || !principal.can(permission)) {
            throw new AccessDeniedException("administrator permission denied");
        }
    }
}
