package com.zhihuiji.backend.application.service.v2.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.admin.AdminAgentRuntimeConfigService;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentRunState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ToolExecutorAdminConfigContractTest {
    @Test
    void disabledToolIsRejectedBeforeToolExecution() {
        AgentTool tool = new AgentTool() {
            @Override public String name() { return "sale_order_lookup"; }
            @Override public String displayName() { return "销售单查询"; }
            @Override public String description() { return "查询销售单"; }
            @Override public ToolType type() { return ToolType.READ_ONLY; }
            @Override public ToolResult execute(ToolContext context, JsonNode params) {
                throw new AssertionError("disabled tool must not execute");
            }
        };
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        CurrentOwnerService ownerService = Mockito.mock(CurrentOwnerService.class);
        when(ownerService.requireCurrentOwnerUserId()).thenReturn(101L);
        when(ownerService.requireCurrentUserId()).thenReturn(900L);
        when(ownerService.findCurrentStoreId()).thenReturn(Optional.of(501L));
        AdminAgentRuntimeConfigService runtimeConfig = Mockito.mock(AdminAgentRuntimeConfigService.class);
        when(runtimeConfig.isToolEnabled(101L, 501L, "sale_order_lookup")).thenReturn(false);

        ToolExecutor executor = new ToolExecutor(registry, ownerService, runtimeConfig);
        ToolExecutor.ExecutionOutcome outcome = executor.execute(
            new AgentRunState("run-1", 601L, 101L, 501L, 1),
            "sale_order_lookup", new ObjectMapper().createObjectNode(), null,
            601L, "run-1", null, new ObjectMapper()
        );

        assertFalse(outcome.executed());
        assertEquals(ToolExecutor.TOOL_OUT_OF_SCOPE, outcome.decision().reasonCode());
    }
}
