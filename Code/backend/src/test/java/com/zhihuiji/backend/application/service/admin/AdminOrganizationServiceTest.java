package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.admin.AdminOrganizationDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminStoreQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminUserQueryRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationServiceTest {
    @Mock
    private AdminAuthorizationService authorizationService;
    @Mock
    private AdminUserQueryRepository userQueryRepository;
    @Mock
    private AdminStoreQueryRepository storeQueryRepository;

    private AdminOrganizationService organizationService;
    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        organizationService = new AdminOrganizationService(
            authorizationService, userQueryRepository, storeQueryRepository
        );
        principal = AdminPrincipal.forRole(
            900L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.owners(Set.of(101L), false, AdminDataScope.ContentMode.REDACTED)
        );
    }

    @Test
    void listUsersUsesAuthorizedScopeAndMasksPhone() {
        when(authorizationService.authorize(
            principal, AdminPermission.USER_READ, 101L, null
        )).thenReturn(principal.scope());
        UserEntity user = new UserEntity();
        user.setPhone("13800138000");
        user.setNickname("用户");
        user.setStatus(1);
        user.setCreatedAt(1000L);
        user.setUpdatedAt(2000L);
        when(userQueryRepository.findUsers(
            any(Boolean.class), any(), any(), any(Boolean.class), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(user), Pageable.ofSize(50), 1));

        AdminPageDtos.PageResponse<AdminOrganizationDtos.UserSummary> response = organizationService.listUsers(
            principal, " 用户 ", 101L, null, 0, 50
        );

        assertEquals(1, response.total());
        assertEquals("*******8000", response.items().get(0).phoneMasked());
        assertEquals("ACTIVE", response.items().get(0).status());
        assertEquals("COMPLETE", response.scopeCompleteness());
        verify(authorizationService).authorize(principal, AdminPermission.USER_READ, 101L, null);
    }

    @Test
    void listStoresUsesDatabaseProjectionAndReturnsMemberCount() {
        when(authorizationService.authorize(
            principal, AdminPermission.STORE_READ, 101L, null
        )).thenReturn(principal.scope());
        AdminStoreQueryRepository.StoreProjection projection = org.mockito.Mockito.mock(
            AdminStoreQueryRepository.StoreProjection.class
        );
        when(projection.getStoreId()).thenReturn(501L);
        when(projection.getOwnerUserId()).thenReturn(101L);
        when(projection.getName()).thenReturn("门店");
        when(projection.getStatus()).thenReturn(1);
        when(projection.getMemberCount()).thenReturn(3L);
        when(projection.getCreatedAt()).thenReturn(1000L);
        when(projection.getUpdatedAt()).thenReturn(2000L);
        when(storeQueryRepository.findStores(
            any(Boolean.class), any(), any(), any(Boolean.class), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(projection), Pageable.ofSize(50), 1));

        AdminPageDtos.PageResponse<AdminOrganizationDtos.StoreSummary> response = organizationService.listStores(
            principal, 101L, null, 0, 50
        );

        assertEquals(1, response.total());
        assertEquals("501", response.items().get(0).storeId());
        assertEquals(3, response.items().get(0).memberCount());
        verify(authorizationService).authorize(principal, AdminPermission.STORE_READ, 101L, null);
    }
}
