package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.domain.entity.AdminAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceSecurityTest {
    @Mock
    private AdminAuditEventRepository repository;

    @Mock
    private AdminAuthorizationService authorizationService;

    @Test
    void anonymousDenialKeepsActorUnknownAndNeverCopiesAuthorizationHeader() {
        AdminAuditService service = new AdminAuditService(repository, authorizationService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v2/admin/session");
        request.addHeader("Authorization", "Bearer do-not-store-this-token");
        request.addHeader("User-Agent", "security-test");
        request.addHeader("X-Request-ID", "request-1");
        when(repository.save(any(AdminAuditEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAuditEventEntity event = service.recordSecurityDenial(
            request, null, "ANONYMOUS", "authentication_required"
        );

        assertNull(event.getAdminUserId());
        assertEquals("ANONYMOUS", event.getRoleCode());
        assertEquals("admin.access.denied", event.getAction());
        assertEquals("ADMIN_ENDPOINT", event.getResourceType());
        assertEquals("/v2/admin/session", event.getResourceId());
        assertEquals("DENIED", event.getResult());
        assertEquals("request-1", event.getRequestId());
        verify(repository).save(event);
    }

    @Test
    void nonAdminDenialRetainsAuthenticatedSubjectForInvestigation() {
        AdminAuditService service = new AdminAuditService(repository, authorizationService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v2/admin/overview");
        when(repository.save(any(AdminAuditEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAuditEventEntity event = service.recordSecurityDenial(
            request, 42L, "NON_ADMIN", "permission_denied"
        );

        assertEquals(42L, event.getAdminUserId());
        assertEquals("NON_ADMIN", event.getRoleCode());
        assertEquals("permission_denied", event.getReason());
        verify(repository).save(event);
    }
}
