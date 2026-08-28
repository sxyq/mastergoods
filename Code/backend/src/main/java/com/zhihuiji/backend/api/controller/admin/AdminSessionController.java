package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminSessionDtos;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Server-derived administrator identity and scope contract (API-ADM-01). */
@RestController
@RequestMapping("/v2/admin")
public class AdminSessionController {
    private final AdminPrincipalResolver principalResolver;

    public AdminSessionController(AdminPrincipalResolver principalResolver) {
        this.principalResolver = principalResolver;
    }

    @GetMapping("/session")
    public ApiResponse<AdminSessionDtos.SessionResponse> session() {
        AdminPrincipal principal = principalResolver.requireCurrent();
        return ApiResponse.success(new AdminSessionDtos.SessionResponse(
            principal.userId().toString(), principal.role().name(),
            principal.permissions().stream().map(AdminPermission::code).collect(Collectors.toUnmodifiableSet()),
            principal.scope().ownerUserIds().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet()),
            principal.scope().storeIds().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet()),
            principal.can(AdminPermission.AGENT_CONTENT_READ),
            principal.scope().allOwners() || principal.scope().storeIds().isEmpty() ? "COMPLETE" : "PARTIAL"
        ));
    }
}
