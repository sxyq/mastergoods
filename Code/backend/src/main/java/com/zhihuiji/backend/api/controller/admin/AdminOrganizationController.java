package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminOrganizationDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.application.service.admin.AdminOrganizationService;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

/** API-ADM-03 and API-ADM-04 organization read and mutation endpoints. */
@RestController
@RequestMapping("/v2/admin")
public class AdminOrganizationController {
    private final AdminOrganizationService organizationService;
    private final AdminPrincipalResolver principalResolver;
    private final AdminAuditService auditService;

    @Autowired
    public AdminOrganizationController(
        AdminOrganizationService organizationService,
        AdminPrincipalResolver principalResolver,
        AdminAuditService auditService
    ) {
        this.organizationService = organizationService;
        this.principalResolver = principalResolver;
        this.auditService = auditService;
    }

    public AdminOrganizationController(AdminOrganizationService organizationService, AdminPrincipalResolver principalResolver) {
        this(organizationService, principalResolver, null);
    }

    @GetMapping("/users")
    public ApiResponse<AdminPageDtos.PageResponse<AdminOrganizationDtos.UserSummary>> users(
        @RequestParam(value = "query", required = false) String query,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        var principal = principalResolver.requireCurrent();
        var response = organizationService.listUsers(principal, query, ownerUserId, storeId, page, size);
        if (auditService != null) auditService.recordRead(principal, "admin.users.read", "USER", null, ownerUserId, storeId, "list");
        return ApiResponse.success(response);
    }

    @GetMapping("/stores")
    public ApiResponse<AdminPageDtos.PageResponse<AdminOrganizationDtos.StoreSummary>> stores(
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        var principal = principalResolver.requireCurrent();
        var response = organizationService.listStores(principal, ownerUserId, storeId, page, size);
        if (auditService != null) auditService.recordRead(principal, "admin.stores.read", "STORE", null, ownerUserId, storeId, "list");
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<AdminOrganizationDtos.UserSummary> user(
        @PathVariable Long userId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        var principal = principalResolver.requireCurrent();
        var response = organizationService.getUser(principal, userId, ownerUserId, storeId);
        if (auditService != null) auditService.recordRead(principal, "admin.user.read", "USER", userId.toString(), ownerUserId, storeId, "detail");
        return ApiResponse.success(response);
    }

    @PatchMapping("/users/{userId}")
    public ApiResponse<AdminOrganizationDtos.UserSummary> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody AdminOrganizationDtos.UserPatchRequest request
    ) {
        return ApiResponse.success(organizationService.updateUser(principalResolver.requireCurrent(), userId, request));
    }

    @GetMapping("/stores/{storeId}")
    public ApiResponse<AdminOrganizationDtos.StoreSummary> store(
        @PathVariable Long storeId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId
    ) {
        var principal = principalResolver.requireCurrent();
        var response = organizationService.getStore(principal, storeId, ownerUserId);
        if (auditService != null) auditService.recordRead(principal, "admin.store.read", "STORE", storeId.toString(), ownerUserId, storeId, "detail");
        return ApiResponse.success(response);
    }

    @PatchMapping("/stores/{storeId}")
    public ApiResponse<AdminOrganizationDtos.StoreSummary> updateStore(
        @PathVariable Long storeId,
        @Valid @RequestBody AdminOrganizationDtos.StorePatchRequest request
    ) {
        return ApiResponse.success(organizationService.updateStore(principalResolver.requireCurrent(), storeId, request));
    }

    @GetMapping("/stores/{storeId}/members")
    public ApiResponse<AdminPageDtos.PageResponse<AdminOrganizationDtos.MemberSummary>> members(
        @PathVariable Long storeId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        var principal = principalResolver.requireCurrent();
        var response = organizationService.listMembers(principal, storeId, ownerUserId, page, size);
        if (auditService != null) auditService.recordRead(principal, "admin.store.members.read", "STORE_MEMBER", storeId.toString(), ownerUserId, storeId, "list");
        return ApiResponse.success(response);
    }

    @PatchMapping("/stores/{storeId}/members/{userId}")
    public ApiResponse<AdminOrganizationDtos.MemberSummary> updateMember(
        @PathVariable Long storeId,
        @PathVariable Long userId,
        @Valid @RequestBody AdminOrganizationDtos.MemberPatchRequest request
    ) {
        return ApiResponse.success(organizationService.updateMember(principalResolver.requireCurrent(), storeId, userId, request));
    }
}
