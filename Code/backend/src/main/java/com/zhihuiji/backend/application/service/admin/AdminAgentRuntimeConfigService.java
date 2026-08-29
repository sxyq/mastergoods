package com.zhihuiji.backend.application.service.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.domain.entity.AdminAgentConfigEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentConfigRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the effective administrator-owned Agent tool policy for a runtime request. */
@Service
public class AdminAgentRuntimeConfigService {
    private final AdminAgentConfigRepository repository;
    private final ObjectMapper objectMapper;

    public AdminAgentRuntimeConfigService(AdminAgentConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Scope precedence is exact owner/store, owner default, store default, then global.
     * A missing configuration means the existing runtime defaults remain in effect.
     */
    @Transactional(readOnly = true)
    public Optional<RuntimeConfig> resolve(Long ownerUserId, Long storeId) {
        if (ownerUserId == null) {
            return Optional.empty();
        }
        AdminAgentConfigEntity entity = repository.findByScopeOwnerUserIdAndScopeStoreId(ownerUserId, storeId)
            .orElseGet(() -> repository.findByScopeOwnerUserIdAndScopeStoreId(ownerUserId, null)
                .orElseGet(() -> repository.findByScopeOwnerUserIdAndScopeStoreId(null, storeId)
                    .orElseGet(() -> repository.findByScopeOwnerUserIdAndScopeStoreId(null, null).orElse(null))));
        return entity == null ? Optional.empty() : Optional.of(toRuntimeConfig(entity));
    }

    /** Returns false when a persisted policy explicitly disables the tool. */
    public boolean isToolEnabled(Long ownerUserId, Long storeId, String toolName) {
        return resolve(ownerUserId, storeId)
            .map(config -> config.agentEnabled() && config.enabledTools().contains(toolName))
            .orElse(true);
    }

    private RuntimeConfig toRuntimeConfig(AdminAgentConfigEntity entity) {
        return new RuntimeConfig(Boolean.TRUE.equals(entity.getAgentEnabled()), parseTools(entity.getEnabledToolsJson()));
    }

    private Set<String> parseTools(String json) {
        try {
            List<String> values = objectMapper.readValue(json == null ? "[]" : json, new TypeReference<List<String>>() {});
            LinkedHashSet<String> result = new LinkedHashSet<>();
            if (values != null) {
                for (String value : values) {
                    if (value != null && !value.isBlank()) {
                        result.add(value.trim());
                    }
                }
            }
            return Set.copyOf(result);
        } catch (Exception ignored) {
            // A malformed persisted policy must fail closed for tool execution.
            return Set.of();
        }
    }

    public record RuntimeConfig(boolean agentEnabled, Set<String> enabledTools) {
        public RuntimeConfig {
            enabledTools = Set.copyOf(enabledTools == null ? Set.of() : enabledTools);
        }
    }
}
