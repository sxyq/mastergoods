package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.common.AdminConflictException;
import com.zhihuiji.backend.api.dto.admin.AdminAuditDtos;
import com.zhihuiji.backend.api.dto.admin.AdminConfigDtos;
import com.zhihuiji.backend.api.dto.admin.AdminExportDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminRetentionDtos;
import com.zhihuiji.backend.api.dto.admin.AdminSystemDtos;
import com.zhihuiji.backend.domain.entity.AdminAgentConfigEntity;
import com.zhihuiji.backend.domain.entity.AdminAuditEventEntity;
import com.zhihuiji.backend.domain.entity.AdminExportJobEntity;
import com.zhihuiji.backend.domain.entity.AdminRetentionPolicyEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentConfigRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminExportJobRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminRetentionPolicyRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminI4ServiceContractTest {
    @Mock private AdminAuthorizationService authorizationService;
    @Mock private AdminAgentConfigRepository configRepository;
    @Mock private AdminAuditEventRepository auditRepository;
    @Mock private AdminExportJobRepository exportRepository;
    @Mock private AdminRetentionPolicyRepository retentionRepository;
    @Mock private AdminAuditService auditService;
    @Mock private ToolRegistry toolRegistry;
    @Mock private LongCatAnthropicClient llmClient;
    @Mock private DataSource dataSource;

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
    void auditListPassesServerResolvedOwnerAndStoreScopeToRepository() {
        when(auditRepository.findVisible(
            eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)),
            any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(25), 0));
        AdminAuditService service = new AdminAuditService(auditRepository, authorizationService);

        AdminPageDtos.PageResponse<AdminAuditDtos.Event> response = service.list(
            principal, null, null, null, null, null, 0, 25
        );

        assertEquals(0, response.total());
        verify(authorizationService).authorize(principal, AdminPermission.AUDIT_READ, null, null);
        verify(auditRepository).findVisible(
            eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)),
            any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void auditListPassesRequestedOwnerAndStoreToAuthorizationBeforeQuery() {
        when(auditRepository.findVisible(
            eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)),
            any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(25), 0));
        AdminAuditService service = new AdminAuditService(auditRepository, authorizationService);

        service.list(principal, null, null, null, null, null, 101L, 501L, 0, 25);

        verify(authorizationService).authorize(principal, AdminPermission.AUDIT_READ, 101L, 501L);
        verify(auditRepository).findVisible(
            eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)),
            any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void configUpdateRejectsUnregisteredToolAndInvalidModel() {
        when(toolRegistry.isRegistered("unknown.tool")).thenReturn(false);
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setModel("model-a");
        AdminSystemService service = systemService(properties);

        AdminConfigDtos.UpdateRequest toolRequest = new AdminConfigDtos.UpdateRequest(
            "model-a", true, List.of("unknown.tool"), 0L, "config-tool-1", "test", true, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(principal, toolRequest));

        AdminConfigDtos.UpdateRequest modelRequest = new AdminConfigDtos.UpdateRequest(
            "bad model", true, List.of(), 0L, "config-model-1", "test", true, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(principal, modelRequest));
        verify(configRepository, never()).saveAndFlush(any());
    }

    @Test
    void configUpdateEnforcesVersionAndReplaysMatchingIdempotency() {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setModel("model-a");
        AdminSystemService service = systemService(properties);
        AdminAgentConfigEntity current = config("config-key", "payload-hash", 2L);

        when(configRepository.findByIdempotencyKey("config-key")).thenReturn(Optional.of(current));
        AdminConfigDtos.UpdateRequest replay = new AdminConfigDtos.UpdateRequest(
            "model-a", true, List.of(), 0L, "config-key", "test", true, null, null
        );
        assertEquals(2L, service.updateConfig(principal, replay).version());
        verify(configRepository, never()).saveAndFlush(any());

        when(configRepository.findByIdempotencyKey("config-conflict")).thenReturn(Optional.empty());
        when(configRepository.findByScopeOwnerUserIdAndScopeStoreId(null, null)).thenReturn(Optional.of(current));
        AdminConfigDtos.UpdateRequest conflict = new AdminConfigDtos.UpdateRequest(
            "model-a", true, List.of(), 1L, "config-conflict", "test", true, null, null
        );
        assertThrows(AdminConflictException.class, () -> service.updateConfig(principal, conflict));
    }

    @Test
    void healthKeepsDependencyFailuresInComponentStateInsteadOfThrowing() throws Exception {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setModel("model-a");
        AdminSystemService service = systemService(properties);
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("database unavailable"));
        when(auditRepository.count()).thenThrow(new RuntimeException("audit unavailable"));
        when(exportRepository.countByStatus("PENDING")).thenThrow(new RuntimeException("export unavailable"));

        AdminSystemDtos.HealthResponse response = service.health(principal);

        assertEquals("DOWN", response.status());
        assertEquals("DOWN", response.components().stream()
            .filter(component -> component.serviceName().equals("database")).findFirst().orElseThrow().status());
        assertEquals("UNAVAILABLE", response.components().stream()
            .filter(component -> component.serviceName().equals("admin_audit")).findFirst().orElseThrow().status());
        assertEquals("UNAVAILABLE", response.components().stream()
            .filter(component -> component.serviceName().equals("export_queue")).findFirst().orElseThrow().status());
    }

    @Test
    void healthRemainsAvailableWhenHealthAuditWriteFails() {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setModel("model-a");
        AdminSystemService service = systemService(properties);
        doThrow(new RuntimeException("audit write unavailable")).when(auditService).recordRead(
            principal, "admin.system.health.read", "SYSTEM", null, null, null, "DOWN");

        AdminSystemDtos.HealthResponse response = service.health(principal);

        assertEquals("DOWN", response.status());
    }

    @Test
    void configReadAuthorizesRequestedScopeAndReadsMatchingRecord() {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setModel("runtime-model");
        AdminAgentConfigEntity scoped = config("scope-read", "payload-hash", 3L);
        scoped.setModelId("scoped-model");
        scoped.setAgentEnabled(true);
        scoped.setEnabledToolsJson("[]");
        when(authorizationService.authorize(principal, AdminPermission.AGENT_CONFIG_READ, 101L, 501L))
            .thenReturn(scope);
        when(configRepository.findByScopeOwnerUserIdAndScopeStoreId(101L, 501L)).thenReturn(Optional.of(scoped));

        AdminConfigDtos.ConfigResponse response = systemService(properties).config(principal, 101L, 501L);

        assertEquals("scoped-model", response.modelId());
        assertEquals(3L, response.version());
        verify(configRepository).findByScopeOwnerUserIdAndScopeStoreId(101L, 501L);
    }

    @Test
    void retentionUpdateRequiresConfirmationAndIncrementsVersion() {
        AdminRetentionService service = new AdminRetentionService(authorizationService, retentionRepository, auditService);
        AdminRetentionDtos.UpdateRequest unconfirmed = retentionRequest(0L, "retention-1", false);
        assertThrows(IllegalArgumentException.class, () -> service.update(principal, unconfirmed));

        when(retentionRepository.findById(1L)).thenReturn(Optional.empty());
        when(retentionRepository.saveAndFlush(any(AdminRetentionPolicyEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        AdminRetentionDtos.Policy updated = service.update(principal, retentionRequest(0L, "retention-2", true));
        assertEquals(1L, updated.version());
        verify(retentionRepository).saveAndFlush(any(AdminRetentionPolicyEntity.class));
    }

    @Test
    void retentionUpdateRejectsStaleVersion() {
        AdminRetentionService service = new AdminRetentionService(authorizationService, retentionRepository, auditService);
        AdminRetentionPolicyEntity policy = new AdminRetentionPolicyEntity();
        policy.setId(1L);
        policy.setVersion(3L);
        when(retentionRepository.findById(1L)).thenReturn(Optional.of(policy));

        assertThrows(AdminConflictException.class, () -> service.update(principal, retentionRequest(2L, "retention-3", true)));
        verify(retentionRepository, never()).saveAndFlush(any());
    }

    @Test
    void exportRejectsNonWhitelistedField() {
        AdminExportService service = exportService();
        AdminExportDtos.CreateRequest request = new AdminExportDtos.CreateRequest(
            "audit_events", List.of("reason"), null, null, null, null, "export reason", "export-1"
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(principal, request));
        verify(exportRepository, never()).saveAndFlush(any());
    }

    @Test
    void exportListPassesServerResolvedOwnerAndStoreScopeToRepository() {
        when(exportRepository.findVisible(
            eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(25), 0));
        AdminExportService service = exportService();

        AdminPageDtos.PageResponse<AdminExportDtos.Job> response = service.list(principal, 0, 25);

        assertEquals(0, response.total());
        verify(authorizationService).authorize(principal, AdminPermission.EXPORT, null, null);
        verify(exportRepository).findVisible(
            eq(900L), eq(false), eq(Set.of(101L)), eq(false), eq(Set.of(501L)), any(Pageable.class)
        );
    }

    @Test
    void expiredExportIsMarkedAndAuditedBeforeDownloadFails() {
        AdminExportService service = exportService();
        AdminExportJobEntity job = exportJob("export-expired", System.currentTimeMillis() - 1L);
        when(exportRepository.findByExportId("export-expired")).thenReturn(Optional.of(job));

        assertThrows(IllegalArgumentException.class, () -> service.download(principal, "export-expired"));
        assertEquals("EXPIRED", job.getStatus());
        verify(exportRepository).save(job);
        verify(auditService).record(principal, "admin.export.download", "EXPORT", "export-expired", null, null,
            "EXPIRED", null, "export expired", null, null);
    }

    @Test
    void successfulExportDownloadIncrementsCountAndRecordsRead() {
        AdminExportService service = exportService();
        AdminExportJobEntity job = exportJob("export-ready", System.currentTimeMillis() + 60_000L);
        job.setContentCsv("eventId\nabc\n");
        when(exportRepository.findByExportId("export-ready")).thenReturn(Optional.of(job));

        byte[] content = service.download(principal, "export-ready");

        assertEquals("eventId\nabc\n", new String(content));
        assertEquals(1, job.getDownloadCount());
        verify(exportRepository).save(job);
        verify(auditService).recordRead(principal, "admin.export.download", "EXPORT", "export-ready", null, null, "downloaded");
    }

    private AdminSystemService systemService(AgentLlmProperties properties) {
        lenient().when(llmClient.configurationStatus()).thenReturn("configured");
        return new AdminSystemService(authorizationService, properties, llmClient, configRepository,
            auditRepository, exportRepository, auditService, toolRegistry, objectMapper, dataSource);
    }

    private AdminExportService exportService() {
        return new AdminExportService(authorizationService, exportRepository, auditRepository, auditService, objectMapper);
    }

    private AdminAgentConfigEntity config(String key, String hash, long version) {
        AdminAgentConfigEntity entity = new AdminAgentConfigEntity();
        entity.setModelId("model-a");
        entity.setAgentEnabled(true);
        entity.setEnabledToolsJson("[]");
        entity.setVersion(version);
        entity.setIdempotencyKey(key);
        entity.setIdempotencyPayloadHash(hash);
        return entity;
    }

    private AdminRetentionDtos.UpdateRequest retentionRequest(long version, String key, boolean confirmed) {
        return new AdminRetentionDtos.UpdateRequest(365, 30, 30, 365, "REDACTED", version, key, "retention reason", confirmed);
    }

    private AdminExportJobEntity exportJob(String exportId, long expiresAt) {
        AdminExportJobEntity job = new AdminExportJobEntity();
        job.setExportId(exportId);
        job.setAdminUserId(principal.userId());
        job.setStatus("READY");
        job.setExpiresAt(expiresAt);
        job.setDownloadCount(0);
        job.setScopeOwnerUserId(null);
        job.setScopeStoreId(null);
        return job;
    }
}
