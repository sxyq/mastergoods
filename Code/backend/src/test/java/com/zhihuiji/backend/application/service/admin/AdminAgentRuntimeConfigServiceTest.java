package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.domain.entity.AdminAgentConfigEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAgentRuntimeConfigServiceTest {
    @Mock private AdminAgentConfigRepository repository;

    @Test
    void resolvesExactOwnerStorePolicyBeforeBroaderDefaults() {
        AdminAgentConfigEntity exact = config(true, "[\"inventory_ledger_lookup\"]");
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(101L, 501L)).thenReturn(Optional.of(exact));

        AdminAgentRuntimeConfigService service = new AdminAgentRuntimeConfigService(repository, new ObjectMapper());

        assertTrue(service.isToolEnabled(101L, 501L, "inventory_ledger_lookup"));
        assertFalse(service.isToolEnabled(101L, 501L, "sale_order_lookup"));
        verify(repository, never()).findByScopeOwnerUserIdAndScopeStoreId(101L, null);
    }

    @Test
    void fallsBackToGlobalPolicyAndFailsClosedOnMalformedToolJson() {
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(101L, 501L)).thenReturn(Optional.empty());
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(101L, null)).thenReturn(Optional.empty());
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(null, 501L)).thenReturn(Optional.empty());
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(null, null))
            .thenReturn(Optional.of(config(true, "not-json")));

        AdminAgentRuntimeConfigService service = new AdminAgentRuntimeConfigService(repository, new ObjectMapper());

        assertFalse(service.isToolEnabled(101L, 501L, "sale_order_lookup"));
    }

    @Test
    void missingPolicyPreservesExistingRuntimeDefaults() {
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(101L, 501L)).thenReturn(Optional.empty());
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(101L, null)).thenReturn(Optional.empty());
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(null, 501L)).thenReturn(Optional.empty());
        when(repository.findByScopeOwnerUserIdAndScopeStoreId(null, null)).thenReturn(Optional.empty());

        AdminAgentRuntimeConfigService service = new AdminAgentRuntimeConfigService(repository, new ObjectMapper());

        assertTrue(service.isToolEnabled(101L, 501L, "sale_order_lookup"));
    }

    private AdminAgentConfigEntity config(boolean enabled, String tools) {
        AdminAgentConfigEntity entity = new AdminAgentConfigEntity();
        entity.setAgentEnabled(enabled);
        entity.setEnabledToolsJson(tools);
        return entity;
    }
}
