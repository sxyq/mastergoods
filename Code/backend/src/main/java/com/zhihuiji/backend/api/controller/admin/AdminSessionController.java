package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminSessionDtos;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

/** Server-derived administrator identity and scope contract (API-ADM-01). */
@RestController
@RequestMapping("/v2/admin")
public class AdminSessionController {
    private final AdminPrincipalResolver principalResolver;
    private final AdminAuditService auditService;

    public AdminSessionController(AdminPrincipalResolver principalResolver) {
        this(principalResolver, null);
    }

    @Autowired
    public AdminSessionController(AdminPrincipalResolver principalResolver, AdminAuditService auditService) {
        this.principalResolver = principalResolver;
        this.auditService = auditService;
    }

    @GetMapping("/session")
    public ApiResponse<AdminSessionDtos.SessionResponse> session() {
        AdminPrincipal principal = principalResolver.requireCurrent();
        var response = new AdminSessionDtos.SessionResponse(
            principal.userId().toString(), principal.role().name(),
            principal.permissions().stream().map(AdminPermission::code).collect(Collectors.toUnmodifiableSet()),
            principal.scope().ownerUserIds().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet()),
            principal.scope().storeIds().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet()),
            principal.can(AdminPermission.AGENT_CONTENT_READ),
            principal.scope().allOwners() || principal.scope().storeIds().isEmpty() ? "COMPLETE" : "PARTIAL"
        );
        if (auditService != null) auditService.recordRead(principal, "admin.session.read", "SESSION", null, null, null, "session");
        return ApiResponse.success(response);
    }
}
