package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.domain.entity.StoreMembershipEntity;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentOwnerServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private StoreMembershipRepository storeMembershipRepository;

    private CurrentOwnerService currentOwnerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        currentOwnerService = new CurrentOwnerService(userRepository, storeMembershipRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requirePermissionsAllowsOwnerWithoutMembershipRow() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(9L, "n/a", List.of())
        );
        when(storeMembershipRepository.findByUserId(9L)).thenReturn(Optional.empty());

        assertEquals(9L, currentOwnerService.requireCurrentOwnerUserId());
        assertDoesNotThrow(() -> currentOwnerService.requirePermissions("users:manage", "database:manage"));
    }

    @Test
    void requirePermissionsRejectsMemberWithoutUsersManagePermission() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(11L, "n/a", List.of())
        );
        StoreMembershipEntity membership = new StoreMembershipEntity();
        membership.setOwnerUserId(9L);
        membership.setUserId(11L);
        membership.setRoleCode("SALES");
        membership.setStatus(1);
        when(storeMembershipRepository.findByUserId(11L)).thenReturn(Optional.of(membership));

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> currentOwnerService.requirePermissions("users:manage")
        );

        assertEquals("当前账号缺少权限: users:manage", exception.getMessage());
        assertEquals(9L, currentOwnerService.requireCurrentOwnerUserId());
    }
}
