package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminRetentionDtos;
import com.zhihuiji.backend.application.service.admin.AdminRetentionService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API-ADM-15 retention policy endpoints. */
@RestController
@RequestMapping("/v2/admin/retention")
public class AdminRetentionController {
    private final AdminRetentionService service;
    private final AdminPrincipalResolver principalResolver;

    public AdminRetentionController(AdminRetentionService service, AdminPrincipalResolver principalResolver) {
        this.service = service;
        this.principalResolver = principalResolver;
    }

    @GetMapping
    public ApiResponse<AdminRetentionDtos.Policy> get() {
        return ApiResponse.success(service.get(principalResolver.requireCurrent()));
    }

    @PatchMapping
    public ApiResponse<AdminRetentionDtos.Policy> update(@Valid @RequestBody AdminRetentionDtos.UpdateRequest request) {
        return ApiResponse.success(service.update(principalResolver.requireCurrent(), request));
    }
}
