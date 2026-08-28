package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;

/**
 * Calculates and checks server-owned visibility for administrator requests.
 */
public interface AdminScopeService {
    AdminDataScope resolve(AdminPrincipal principal, Long requestedOwnerUserId, Long requestedStoreId);
}
