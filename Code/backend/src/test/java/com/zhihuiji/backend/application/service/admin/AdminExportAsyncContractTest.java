package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.admin.AdminExportDtos;
import com.zhihuiji.backend.domain.entity.AdminAuditEventEntity;
import com.zhihuiji.backend.domain.entity.AdminExportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminExportJobRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
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
class AdminExportAsyncContractTest {
    @Mock private AdminAuthorizationService authorizationService;
    @Mock private AdminExportJobRepository jobRepository;
    @Mock private AdminAuditEventRepository auditRepository;
    @Mock private AdminAuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AdminPrincipal principal;
    private AdminDataScope scope;

    @BeforeEach
    void setUp() {
        scope = new AdminDataScope(false, Set.of(101L), Set.of(501L), false, AdminDataScope.ContentMode.REDACTED);
        principal = AdminPrincipal.forRole(900L, AdminPrincipal.AdminRole.SUPER_ADMIN, scope);
        lenient().when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(scope);
        lenient().when(auditService.payloadHash(any())).thenReturn("payload-hash");
    }

    @Test
    void createOnlyQueuesJobAndStoresAuthorizationSnapshot() {
        when(jobRepository.findByAdminUserIdAndIdempotencyKey(900L, "export-1")).thenReturn(Optional.empty());
        when(jobRepository.saveAndFlush(any(AdminExportJobEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AdminExportDtos.Job response = service().create(principal, new AdminExportDtos.CreateRequest(
            "audit_events", List.of("eventId"), Instant.EPOCH, Instant.EPOCH.plusSeconds(60),
            null, null, "support review", "export-1"
        ));

        assertEquals("PENDING", response.status());
        verify(jobRepository).saveAndFlush(any(AdminExportJobEntity.class));
        verify(auditRepository, never()).findVisible(any(), any(boolean.class), any(), any(boolean.class), any(),
            any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void claimedJobUsesPersistedScopeAndBecomesReady() {
        AdminExportJobEntity job = new AdminExportJobEntity();
        job.setExportId("export-2");
        job.setAdminUserId(900L);
        job.setStatus("RUNNING");
        job.setFieldsJson("[\"eventId\"]");
        job.setScopeOwnerUserIdsJson("[101]");
        job.setScopeStoreIdsJson("[501]");
        job.setScopeAllOwners(false);
        job.setScopeAllStores(false);
        job.setRequestedFromAt(0L);
        job.setRequestedToAt(60_000L);
        job.setExpiresAt(System.currentTimeMillis() + 60_000L);
        when(jobRepository.findByExportId("export-2")).thenReturn(Optional.of(job));

        AdminAuditEventEntity row = new AdminAuditEventEntity();
        row.setEventId("event-1");
        when(auditRepository.findVisible(eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)),
            eq(null), eq(null), eq(null), eq(0L), eq(60_000L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(row)));

        service().processClaimed("export-2");

        assertEquals("READY", job.getStatus());
        assertEquals("eventId\nevent-1\n", job.getContentCsv());
        verify(jobRepository).save(job);
    }

    private AdminExportService service() {
        return new AdminExportService(authorizationService, jobRepository, auditRepository, auditService, objectMapper);
    }
}
