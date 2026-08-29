package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminAuditDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-ADM-12 administrator audit query. */
@RestController
@RequestMapping("/v2/admin/audit")
public class AdminAuditController {
    private final AdminAuditService auditService;
    private final AdminPrincipalResolver principalResolver;

    public AdminAuditController(AdminAuditService auditService, AdminPrincipalResolver principalResolver) {
        this.auditService = auditService;
        this.principalResolver = principalResolver;
    }

    @GetMapping("/events")
    public ApiResponse<AdminPageDtos.PageResponse<AdminAuditDtos.Event>> events(
        @RequestParam(value = "eventId", required = false) String eventId,
        @RequestParam(value = "action", required = false) String action,
        @RequestParam(value = "resourceType", required = false) String resourceType,
        @RequestParam(value = "result", required = false) String result,
        @RequestParam(value = "from", required = false) Instant from,
        @RequestParam(value = "to", required = false) Instant to,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(auditService.list(
            principalResolver.requireCurrent(), eventId, action, resourceType, result, from, to,
            ownerUserId, storeId, page, size
        ));
    }
}
