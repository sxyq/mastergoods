package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminOrganizationDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.application.service.admin.AdminOrganizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-ADM-03 and API-ADM-04 read-only organization endpoints. */
@RestController
@RequestMapping("/v2/admin")
public class AdminOrganizationController {
    private final AdminOrganizationService organizationService;
    private final AdminPrincipalResolver principalResolver;

    public AdminOrganizationController(
        AdminOrganizationService organizationService,
        AdminPrincipalResolver principalResolver
    ) {
        this.organizationService = organizationService;
        this.principalResolver = principalResolver;
    }

    @GetMapping("/users")
    public ApiResponse<AdminPageDtos.PageResponse<AdminOrganizationDtos.UserSummary>> users(
        @RequestParam(value = "query", required = false) String query,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(
            organizationService.listUsers(
                principalResolver.requireCurrent(), query, ownerUserId, storeId, page, size
            )
        );
    }

    @GetMapping("/stores")
    public ApiResponse<AdminPageDtos.PageResponse<AdminOrganizationDtos.StoreSummary>> stores(
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(
            organizationService.listStores(
                principalResolver.requireCurrent(), ownerUserId, storeId, page, size
            )
        );
    }
}
