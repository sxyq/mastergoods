package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.admin.AdminOrganizationDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeQuery;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminStoreQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminUserQueryRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only organization queries for API-ADM-03 and API-ADM-04. */
@Service
public class AdminOrganizationService {
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final AdminAuthorizationService authorizationService;
    private final AdminUserQueryRepository userQueryRepository;
    private final AdminStoreQueryRepository storeQueryRepository;

    public AdminOrganizationService(
        AdminAuthorizationService authorizationService,
        AdminUserQueryRepository userQueryRepository,
        AdminStoreQueryRepository storeQueryRepository
    ) {
        this.authorizationService = authorizationService;
        this.userQueryRepository = userQueryRepository;
        this.storeQueryRepository = storeQueryRepository;
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminOrganizationDtos.UserSummary> listUsers(
        AdminPrincipal principal,
        String keyword,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal,
            AdminPermission.USER_READ,
            requestedOwnerUserId,
            requestedStoreId
        );
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        Pageable pageable = PaginationUtils.pageable(page, size);
        Page<UserEntity> result = userQueryRepository.findUsers(
            queryScope.allOwners(),
            queryScope.ownerUserIds(),
            queryScope.storeIds(),
            queryScope.allStores(),
            normalizeKeyword(keyword),
            pageable
        );
        List<AdminOrganizationDtos.UserSummary> items = result.getContent().stream()
            .map(this::toUserSummary)
            .toList();
        return pageResponse(result, items, scope, "COMPLETE");
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminOrganizationDtos.StoreSummary> listStores(
        AdminPrincipal principal,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal,
            AdminPermission.STORE_READ,
            requestedOwnerUserId,
            requestedStoreId
        );
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        Page<AdminStoreQueryRepository.StoreProjection> result = storeQueryRepository.findStores(
            queryScope.allOwners(),
            queryScope.ownerUserIds(),
            queryScope.storeIds(),
            queryScope.allStores(),
            PaginationUtils.pageable(page, size)
        );
        List<AdminOrganizationDtos.StoreSummary> items = result.getContent().stream()
            .map(this::toStoreSummary)
            .toList();
        return pageResponse(result, items, scope, "COMPLETE");
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("keyword is too long");
        }
        return normalized;
    }

    private AdminOrganizationDtos.UserSummary toUserSummary(UserEntity user) {
        return new AdminOrganizationDtos.UserSummary(
            id(user.getId()),
            maskPhone(user.getPhone()),
            user.getNickname(),
            status(user.getStatus()),
            instant(user.getCreatedAt()),
            instant(user.getUpdatedAt())
        );
    }

    private AdminOrganizationDtos.StoreSummary toStoreSummary(AdminStoreQueryRepository.StoreProjection store) {
        return new AdminOrganizationDtos.StoreSummary(
            id(store.getStoreId()),
            id(store.getOwnerUserId()),
            store.getName(),
            status(store.getStatus()),
            Math.toIntExact(store.getMemberCount() == null ? 0L : store.getMemberCount()),
            instant(store.getCreatedAt()),
            instant(store.getUpdatedAt())
        );
    }

    private <T> AdminPageDtos.PageResponse<T> pageResponse(
        Page<?> page,
        List<T> items,
        AdminDataScope scope,
        String scopeCompleteness
    ) {
        return new AdminPageDtos.PageResponse<>(
            items,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            Instant.now(),
            AdminScopeDtos.Scope.from(scope),
            scopeCompleteness
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String value = phone.trim();
        if (value.length() <= 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }

    private String status(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "ACTIVE" : "DISABLED";
    }

    private String id(Long value) {
        return value == null ? null : value.toString();
    }

    private Instant instant(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }
}
