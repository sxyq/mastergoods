package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.dto.admin.AdminConfigDtos;
import com.zhihuiji.backend.api.dto.admin.AdminSystemDtos;
import com.zhihuiji.backend.api.common.AdminConflictException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.domain.entity.AdminAgentConfigEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentConfigRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminExportJobRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import com.zhihuiji.backend.api.dto.admin.AdminConfigDtos.UpdateRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** System health and versioned Agent configuration; provider secrets never leave this service. */
@Service
public class AdminSystemService {
    private final AdminAuthorizationService authorizationService;
    private final AgentLlmProperties llmProperties;
    private final LongCatAnthropicClient llmClient;
    private final AdminAgentConfigRepository configRepository;
    private final AdminAuditEventRepository auditRepository;
    private final AdminExportJobRepository exportRepository;
    private final AdminAuditService auditService;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;

    @Autowired
    public AdminSystemService(
        AdminAuthorizationService authorizationService,
        AgentLlmProperties llmProperties,
        LongCatAnthropicClient llmClient,
        AdminAgentConfigRepository configRepository,
        AdminAuditEventRepository auditRepository,
        AdminExportJobRepository exportRepository,
        AdminAuditService auditService,
        ToolRegistry toolRegistry,
        ObjectMapper objectMapper,
        DataSource dataSource
    ) {
        this.authorizationService = authorizationService;
        this.llmProperties = llmProperties;
        this.llmClient = llmClient;
        this.configRepository = configRepository;
        this.auditRepository = auditRepository;
        this.exportRepository = exportRepository;
        this.auditService = auditService;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
    }

    /** Compatibility constructor for focused metadata tests. */
    public AdminSystemService(
        AdminAuthorizationService authorizationService,
        AgentLlmProperties llmProperties,
        LongCatAnthropicClient llmClient
    ) {
        this(authorizationService, llmProperties, llmClient, null, null, null, null, null, new ObjectMapper(), null);
    }

    public AdminConfigDtos.ConfigResponse config(AdminPrincipal principal) {
        return config(principal, null, null);
    }

    @Transactional
    public AdminConfigDtos.ConfigResponse config(AdminPrincipal principal, Long requestedOwnerUserId, Long requestedStoreId) {
        if (requestedOwnerUserId == null && requestedStoreId == null) {
            authorizationService.requirePermission(principal, AdminPermission.AGENT_CONFIG_READ);
        } else {
            authorizationService.authorize(principal, AdminPermission.AGENT_CONFIG_READ, requestedOwnerUserId, requestedStoreId);
        }
        if (configRepository != null) {
            AdminAgentConfigEntity persisted = configRepository
                .findFirstByScopeOwnerUserIdAndScopeStoreId(requestedOwnerUserId, requestedStoreId).orElse(null);
            if (persisted != null) {
                if (auditService != null) auditService.recordRead(principal, "admin.agent.config.read", "CONFIG",
                    persisted.getId() == null ? null : persisted.getId().toString(), requestedOwnerUserId, requestedStoreId, "config");
                return toConfig(persisted);
            }
        }
        var response = new AdminConfigDtos.ConfigResponse(
            llmProperties.getModel(), llmProperties.isEnabled(), registeredToolNames(), 0L, llmClient.configurationStatus(), null, null
        );
        if (auditService != null) auditService.recordRead(principal, "admin.agent.config.read", "CONFIG", null,
            requestedOwnerUserId, requestedStoreId, "runtime");
        return response;
    }

    @Transactional
    public AdminConfigDtos.ConfigResponse updateConfig(AdminPrincipal principal, UpdateRequest request) {
        AdminDataScope scope = authorizationService.authorize(
            principal, AdminPermission.AGENT_CONFIG_MANAGE, request == null ? null : request.ownerUserId(),
            request == null ? null : request.storeId()
        );
        validateUpdateRequest(request);
        String payload = canonicalConfigPayload(request);
        String payloadHash = auditService == null ? null : auditService.payloadHash(payload);
        if (configRepository == null) throw new IllegalStateException("configuration storage is unavailable");
        AdminAgentConfigEntity existingByKey = configRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existingByKey != null) {
            if (payloadHash != null && !payloadHash.equals(existingByKey.getIdempotencyPayloadHash())) {
                throw new AdminConflictException("idempotency key was already used with a different payload");
            }
            return toConfig(existingByKey);
        }
        Long scopeOwner = request.ownerUserId();
        Long scopeStore = request.storeId();
        if (scopeStore != null && !scope.storeIds().contains(scopeStore) && !scope.allOwners()) {
            throw new org.springframework.security.access.AccessDeniedException("administrator data scope denied");
        }
        AdminAgentConfigEntity entity = configRepository.findByScopeOwnerUserIdAndScopeStoreId(scopeOwner, scopeStore).orElse(null);
        long currentVersion = entity == null || entity.getVersion() == null ? 0L : entity.getVersion();
        if (request.expectedVersion() != currentVersion) {
            throw new AdminConflictException("configuration version conflict");
        }
        String model = request.modelId() == null || request.modelId().isBlank() ? llmProperties.getModel() : request.modelId().trim();
        validateModel(model);
        List<String> tools = request.enabledTools() == null
            ? (entity == null ? registeredToolNames() : parseTools(entity.getEnabledToolsJson()))
            : normalizeTools(request.enabledTools());
        boolean enabled = request.agentEnabled() == null ? llmProperties.isEnabled() : request.agentEnabled();
        long now = System.currentTimeMillis();
        if (entity == null) {
            entity = new AdminAgentConfigEntity();
            entity.setCreatedAt(now);
        }
        entity.setModelId(model);
        entity.setAgentEnabled(enabled);
        entity.setEnabledToolsJson(writeTools(tools));
        entity.setScopeOwnerUserId(scopeOwner);
        entity.setScopeStoreId(scopeStore);
        entity.setVersion(currentVersion + 1L);
        entity.setEffectiveState(scopeOwner == null && scopeStore == null ? "APPLIED" : "STORED");
        entity.setEffectiveAt(now);
        entity.setUpdatedBy(principal.userId());
        entity.setIdempotencyKey(request.idempotencyKey().trim());
        entity.setIdempotencyPayloadHash(payloadHash);
        entity.setUpdatedAt(now);
        AdminAgentConfigEntity saved = configRepository.saveAndFlush(entity);
        if (scopeOwner == null && scopeStore == null) {
            llmProperties.setModel(model);
            llmProperties.setEnabled(enabled);
        }
        if (auditService != null) {
            auditService.record(principal, "admin.agent.config.update", "CONFIG", saved.getId() == null ? null : saved.getId().toString(),
                scopeOwner, scopeStore, "SUCCESS", request.reason(), "version=" + saved.getVersion(), request.idempotencyKey(), payloadHash);
        }
        return toConfig(saved);
    }

    @Transactional
    public AdminSystemDtos.HealthResponse health(AdminPrincipal principal) {
        authorizationService.requirePermission(principal, AdminPermission.SYSTEM_READ);
        Instant checkedAt = Instant.now();
        java.util.ArrayList<AdminSystemDtos.Component> components = new java.util.ArrayList<>();
        java.util.ArrayList<AdminSystemDtos.ErrorSummary> errors = new java.util.ArrayList<>();
        boolean databaseUp = databaseHealthy();
        components.add(new AdminSystemDtos.Component("database", databaseUp ? "UP" : "DOWN", packageVersion(), checkedAt,
            databaseUp ? null : "database check failed"));
        String providerStatus = llmClient.configurationStatus();
        components.add(new AdminSystemDtos.Component("agent_llm", providerStatus, packageVersion(), checkedAt,
            "configuration status only"));
        boolean auditUp = false;
        long auditCount = 0L;
        long auditFailures = 0L;
        if (auditRepository != null) {
            try {
                auditCount = auditRepository.count();
                auditFailures = auditRepository.countByResult("FAILED");
                auditUp = true;
            } catch (RuntimeException ignored) {
                // Keep the health endpoint available when the audit store is unavailable.
            }
        }
        components.add(new AdminSystemDtos.Component("admin_audit", auditUp ? "UP" : "UNAVAILABLE", packageVersion(), checkedAt,
            auditUp ? "events=" + auditCount + ",failures=" + auditFailures : "audit repository unavailable", auditCount));
        boolean exportUp = false;
        long exportQueueDepth = 0L;
        if (exportRepository != null) {
            try {
                exportQueueDepth = exportRepository.countByStatus("PENDING") + exportRepository.countByStatus("RUNNING");
                exportUp = true;
            } catch (RuntimeException ignored) {
                // Keep the health endpoint available when the export store is unavailable.
            }
        }
        components.add(new AdminSystemDtos.Component("export_queue", exportUp ? "UP" : "UNAVAILABLE", packageVersion(), checkedAt,
            exportUp ? "pending_or_running=" + exportQueueDepth : "export repository unavailable", exportQueueDepth));
        if (!databaseUp) errors.add(new AdminSystemDtos.ErrorSummary("database", "DEPENDENCY", "database check failed", checkedAt));
        String status = databaseUp && !"not_configured".equals(providerStatus) ? "UP" : databaseUp ? "DEGRADED" : "DOWN";
        if (auditService != null) {
            try {
                auditService.recordRead(principal, "admin.system.health.read", "SYSTEM", null, null, null, status);
            } catch (RuntimeException ignored) {
                // Health remains useful when its own audit sink is unavailable.
            }
        }
        return new AdminSystemDtos.HealthResponse(status, packageVersion(), checkedAt, components, errors);
    }

    private AdminConfigDtos.ConfigResponse toConfig(AdminAgentConfigEntity entity) {
        return new AdminConfigDtos.ConfigResponse(entity.getModelId(), Boolean.TRUE.equals(entity.getAgentEnabled()),
            parseTools(entity.getEnabledToolsJson()), entity.getVersion() == null ? 0L : entity.getVersion(), entity.getEffectiveState(),
            entity.getEffectiveAt() == null ? null : Instant.ofEpochMilli(entity.getEffectiveAt()), id(entity.getUpdatedBy()));
    }

    private List<String> registeredToolNames() {
        return toolRegistry == null ? List.of() : toolRegistry.listTools().stream().map(tool -> tool.name()).toList();
    }

    private List<String> parseTools(String json) {
        try {
            List<String> values = objectMapper.readValue(json == null ? "[]" : json, new TypeReference<List<String>>() {});
            return normalizeTools(values);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> normalizeTools(List<String> values) {
        if (values == null) return List.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank() || value.length() > 128) throw new IllegalArgumentException("tool name is invalid");
            String tool = value.trim();
            if (toolRegistry != null && !toolRegistry.isRegistered(tool)) throw new IllegalArgumentException("tool is not registered: " + tool);
            normalized.add(tool);
        }
        if (normalized.size() > 200) throw new IllegalArgumentException("too many tools");
        return List.copyOf(normalized);
    }

    private String writeTools(List<String> values) {
        try { return objectMapper.writeValueAsString(values); }
        catch (Exception ex) { throw new IllegalStateException("configuration serialization failed", ex); }
    }

    private void validateModel(String model) {
        if (model == null || model.length() > 128 || !model.matches("[A-Za-z0-9._:/-]+")) {
            throw new IllegalArgumentException("modelId is invalid");
        }
        List<String> allowed = llmProperties.getAllowedModels();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(model)) {
            throw new IllegalArgumentException("modelId is not allowed");
        }
    }

    private void validateUpdateRequest(UpdateRequest request) {
        if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0
            || request.idempotencyKey() == null || request.idempotencyKey().isBlank()
            || request.idempotencyKey().trim().length() > 128 || request.reason() == null || request.reason().isBlank()
            || request.reason().trim().length() > 512 || !Boolean.TRUE.equals(request.confirmed())) {
            throw new IllegalArgumentException("configuration update requires confirmation, version, idempotencyKey and reason");
        }
    }

    private String canonicalConfigPayload(UpdateRequest request) {
        return String.valueOf(request.modelId()) + "|" + request.agentEnabled() + "|" + String.valueOf(request.enabledTools())
            + "|" + request.ownerUserId() + "|" + request.storeId();
    }

    private boolean databaseHealthy() {
        if (dataSource == null) return false;
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement(); var ignored = statement.executeQuery("SELECT 1")) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String packageVersion() {
        return AdminSystemService.class.getPackage().getImplementationVersion();
    }

    private String id(Long value) { return value == null ? null : value.toString(); }
}
