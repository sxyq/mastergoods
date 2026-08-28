package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminConfigDtos;
import com.zhihuiji.backend.api.dto.admin.AdminSystemDtos;
import com.zhihuiji.backend.application.service.admin.AdminSystemService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only API-ADM-11 and API-ADM-13 metadata endpoints. */
@RestController
@RequestMapping("/v2/admin")
public class AdminSystemController {
    private final AdminSystemService systemService;
    private final AdminPrincipalResolver principalResolver;

    public AdminSystemController(AdminSystemService systemService, AdminPrincipalResolver principalResolver) {
        this.systemService = systemService;
        this.principalResolver = principalResolver;
    }

    @GetMapping("/agent/config")
    public ApiResponse<AdminConfigDtos.ConfigResponse> config() {
        return ApiResponse.success(systemService.config(principalResolver.requireCurrent()));
    }

    @PatchMapping("/agent/config")
    public ApiResponse<AdminConfigDtos.ConfigResponse> updateConfig(
        @Valid @RequestBody AdminConfigDtos.UpdateRequest request
    ) {
        return ApiResponse.success(systemService.updateConfig(principalResolver.requireCurrent(), request));
    }

    @GetMapping("/system/health")
    public ApiResponse<AdminSystemDtos.HealthResponse> health() {
        return ApiResponse.success(systemService.health(principalResolver.requireCurrent()));
    }
}
