package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.admin.AdminAuditDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.domain.entity.AdminAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminAuditEventIdContractTest {
    @Mock private AdminAuditEventRepository repository;
    @Mock private AdminAuthorizationService authorizationService;

    private AdminAuditService service;
    private AdminPrincipal principal;
    private AdminDataScope scope;

    @BeforeEach
    void setUp() {
        scope = new AdminDataScope(false, Set.of(101L), Set.of(501L), false, AdminDataScope.ContentMode.REDACTED);
        principal = AdminPrincipal.forRole(900L, AdminPrincipal.AdminRole.AUDIT_OBSERVER, scope);
        service = new AdminAuditService(repository, authorizationService);
    }

    @Test
    void eventIdLookupUsesOwnerAndStoreScope() {
        when(authorizationService.authorize(principal, AdminPermission.AUDIT_READ, 101L, 501L)).thenReturn(scope);
        AdminAuditEventEntity event = event("event-1", 101L, 501L);
        when(repository.findVisibleByEventId(
            eq("event-1"), eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event), Pageable.ofSize(25), 1));

        AdminPageDtos.PageResponse<AdminAuditDtos.Event> response = service.list(
            principal, "event-1", null, null, null, null, null, 101L, 501L, 0, 25
        );

        assertEquals(1L, response.total());
        assertEquals("event-1", response.items().get(0).eventId());
        verify(repository).findVisibleByEventId(
            eq("event-1"), eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)), any(Pageable.class)
        );
    }

    @Test
    void eventIdLookupReturnsEmptyPageForInvisibleOrUnknownEvent() {
        when(authorizationService.authorize(principal, AdminPermission.AUDIT_READ, null, null)).thenReturn(scope);
        when(repository.findVisibleByEventId(
            eq("event-outside"), eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(25), 0));

        AdminPageDtos.PageResponse<AdminAuditDtos.Event> response = service.list(
            principal, "event-outside", null, null, null, null, null, null, null, 0, 25
        );

        assertEquals(0L, response.total());
        assertTrue(response.items().isEmpty());
    }

    private AdminAuditEventEntity event(String eventId, Long ownerUserId, Long storeId) {
        AdminAuditEventEntity event = new AdminAuditEventEntity();
        event.setEventId(eventId);
        event.setAdminUserId(900L);
        event.setRoleCode("AUDIT_OBSERVER");
        event.setAction("admin.users.read");
        event.setOwnerUserId(ownerUserId);
        event.setStoreId(storeId);
        event.setResult("SUCCESS");
        event.setOccurredAt(1_000L);
        return event;
    }
}
