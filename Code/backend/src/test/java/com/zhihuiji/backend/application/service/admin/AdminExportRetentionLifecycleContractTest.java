package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.common.AdminConflictException;
import com.zhihuiji.backend.api.dto.admin.AdminExportDtos;
import com.zhihuiji.backend.api.dto.admin.AdminRetentionDtos;
import com.zhihuiji.backend.domain.entity.AdminExportJobEntity;
import com.zhihuiji.backend.domain.entity.AdminRetentionPolicyEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminExportJobRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminRetentionPolicyRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminExportRetentionLifecycleContractTest {
    @Mock private AdminAuthorizationService authorizationService;
    @Mock private AdminExportJobRepository exportRepository;
    @Mock private AdminAuditEventRepository auditRepository;
    @Mock private AdminRetentionPolicyRepository retentionRepository;
    @Mock private AdminAuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        AdminDataScope scope = new AdminDataScope(false, Set.of(101L), Set.of(501L), false,
            AdminDataScope.ContentMode.REDACTED);
        principal = AdminPrincipal.forRole(900L, AdminPrincipal.AdminRole.SUPER_ADMIN, scope);
        lenient().when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(scope);
    }

    @Test
    void secondDownloadIsRevalidatedAndAuditedAsRedownload() {
        AdminExportJobEntity job = readyJob("export-redownload", System.currentTimeMillis() + 60_000L);
        job.setContentCsv("eventId\nabc\n");
        when(exportRepository.findByExportId("export-redownload")).thenReturn(Optional.of(job));
        AdminExportService service = exportService();

        service.download(principal, "export-redownload");
        service.download(principal, "export-redownload");

        assertEquals(2, job.getDownloadCount());
        verify(auditService).recordRead(principal, "admin.export.download", "EXPORT", "export-redownload",
            null, null, "redownloaded");
    }

    @Test
    void nonReadyDownloadIsRejectedAndAudited() {
        AdminExportJobEntity job = readyJob("export-pending", System.currentTimeMillis() + 60_000L);
        job.setStatus("PENDING");
        when(exportRepository.findByExportId("export-pending")).thenReturn(Optional.of(job));

        assertThrows(IllegalArgumentException.class, () -> exportService().download(principal, "export-pending"));

        verify(auditService).record(principal, "admin.export.download", "EXPORT", "export-pending", null, null,
            "REJECTED", null, "export status=PENDING", null, null);
        verify(exportRepository, never()).save(any(AdminExportJobEntity.class));
    }

    @Test
    void cleanupExpiresAndClearsPayloadsWithoutDeletingAuditState() {
        when(exportRepository.expireAndClearExpired(any(Long.class))).thenReturn(3);

        assertEquals(3, exportService().cleanupExpired());

        verify(exportRepository).expireAndClearExpired(any(Long.class));
    }

    @Test
    void retentionIdempotencyKeyIsRejectedEvenAfterCurrentPolicyChanged() {
        AdminRetentionPolicyEntity historical = retentionPolicy("old-key", "different-hash", 4L);
        when(retentionRepository.findByIdempotencyKey("old-key")).thenReturn(Optional.of(historical));
        when(auditService.payloadHash(any())).thenReturn("new-hash");

        assertThrows(AdminConflictException.class, () -> retentionService().update(principal,
            retentionRequest(4L, "old-key", true)));

        verify(retentionRepository, never()).findById(1L);
        verify(retentionRepository, never()).saveAndFlush(any(AdminRetentionPolicyEntity.class));
    }

    @Test
    void expiredJobDtoDoesNotExposeDownloadUrlAndReadMarksPayloadCleared() {
        AdminExportJobEntity job = readyJob("export-expired-read", System.currentTimeMillis() - 1L);
        job.setContentCsv("sensitive-placeholder");
        when(exportRepository.findByExportId("export-expired-read")).thenReturn(Optional.of(job));

        AdminExportDtos.Job response = exportService().get(principal, "export-expired-read");

        assertEquals("EXPIRED", response.status());
        assertNull(response.downloadUrl());
        assertNull(job.getContentCsv());
    }

    private AdminExportService exportService() {
        return new AdminExportService(authorizationService, exportRepository, auditRepository, auditService, objectMapper);
    }

    private AdminRetentionService retentionService() {
        return new AdminRetentionService(authorizationService, retentionRepository, auditService);
    }

    private AdminExportJobEntity readyJob(String exportId, long expiresAt) {
        AdminExportJobEntity job = new AdminExportJobEntity();
        job.setExportId(exportId);
        job.setAdminUserId(principal.userId());
        job.setStatus("READY");
        job.setExpiresAt(expiresAt);
        job.setDownloadCount(0);
        return job;
    }

    private AdminRetentionPolicyEntity retentionPolicy(String idempotencyKey, String hash, long version) {
        AdminRetentionPolicyEntity policy = new AdminRetentionPolicyEntity();
        policy.setId(1L);
        policy.setVersion(version);
        policy.setAuditDays(365);
        policy.setMessageDays(30);
        policy.setToolResultDays(30);
        policy.setMetricsDays(365);
        policy.setContentMode("REDACTED");
        policy.setIdempotencyKey(idempotencyKey);
        policy.setIdempotencyPayloadHash(hash);
        return policy;
    }

    private AdminRetentionDtos.UpdateRequest retentionRequest(long version, String key, boolean confirmed) {
        return new AdminRetentionDtos.UpdateRequest(365, 30, 30, 365, "REDACTED", version, key,
            "retention reason", confirmed);
    }
}
