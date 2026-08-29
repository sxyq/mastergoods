package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.admin.AdminOrganizationDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.domain.entity.StoreMembershipEntity;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationServiceTest {
    @Mock
    private AdminAuthorizationService authorizationService;
    @Mock
    private AdminUserQueryRepository userQueryRepository;
    @Mock
    private AdminStoreQueryRepository storeQueryRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreMembershipRepository membershipRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdminAuditService auditService;

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
        assertEquals("PARTIAL", response.scopeCompleteness());
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

    @Test
    void listMembersUsesDatabasePaginationInsteadOfLoadingAllMemberships() {
        AdminOrganizationService service = new AdminOrganizationService(
            authorizationService, userQueryRepository, storeQueryRepository, userRepository, storeRepository,
            membershipRepository, sessionRepository, passwordEncoder, auditService
        );
        when(authorizationService.authorize(principal, AdminPermission.STORE_READ, null, 501L))
            .thenReturn(principal.scope());
        StoreEntity store = mock(StoreEntity.class);
        when(store.getOwnerUserId()).thenReturn(101L);
        when(storeRepository.findById(501L)).thenReturn(java.util.Optional.of(store));

        StoreMembershipEntity membership = new StoreMembershipEntity();
        membership.setOwnerUserId(101L);
        membership.setStoreId(501L);
        membership.setUserId(701L);
        membership.setRoleCode("STAFF");
        membership.setTitle("店员");
        membership.setStatus(1);
        membership.setCreatedAt(1000L);
        membership.setUpdatedAt(2000L);
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(701L);
        when(user.getNickname()).thenReturn("店员");
        when(user.getPhone()).thenReturn("13800138000");
        when(user.getUpdatedAt()).thenReturn(2000L);
        when(membershipRepository.findByOwnerUserIdAndStoreIdOrderByCreatedAtAsc(
            eq(101L), eq(501L), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(membership), Pageable.ofSize(1), 101L));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));

        AdminPageDtos.PageResponse<AdminOrganizationDtos.MemberSummary> response = service.listMembers(
            principal, 501L, null, 0, 1
        );

        assertEquals(101L, response.total());
        assertEquals(1, response.items().size());
        assertEquals("701", response.items().get(0).userId());
        verify(membershipRepository).findByOwnerUserIdAndStoreIdOrderByCreatedAtAsc(eq(101L), eq(501L), any(Pageable.class));
        verify(membershipRepository, never()).findByOwnerUserIdAndStoreIdOrderByCreatedAtAsc(eq(101L), eq(501L));
    }
}
