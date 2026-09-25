// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentRunState;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ToolExecutorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CurrentOwnerService currentOwnerService;
    private CountingTool tool;
    private ToolExecutor executor;

    @BeforeEach
    void setUp() {
        currentOwnerService = mock(CurrentOwnerService.class);
        tool = new CountingTool(schema());
        executor = new ToolExecutor(new ToolRegistry(List.of(tool)), currentOwnerService);
    }

    @Test
    void rejectsUnregisteredToolWithoutCallingPermissionOrTool() {
        ToolExecutor.ExecutionOutcome outcome = execute("missing_tool", objectMapper.createObjectNode(), null);

        assertDenied(outcome, ToolExecutor.TOOL_NOT_REGISTERED);
        assertEquals(0, tool.executionCount);
    }

    @Test
    void rejectsToolOutsideAllowedRangeBeforeExecution() {
        ToolExecutor.ExecutionOutcome outcome = execute(tool.name(), validParams(), Set.of("another_tool"));

        assertDenied(outcome, ToolExecutor.TOOL_OUT_OF_SCOPE);
        assertEquals(0, tool.executionCount);
    }

    @Test
    void rejectsInvalidArgumentsBeforePermissionAndBusinessTool() {
        ObjectNode invalidParams = objectMapper.createObjectNode()
            .put("amount", 0)
            .put("unexpected", true);

        ToolExecutor.ExecutionOutcome outcome = execute(tool.name(), invalidParams, null);

        assertDenied(outcome, ToolExecutor.TOOL_ARGUMENTS_INVALID);
        assertFalse(outcome.decision().violations().isEmpty());
        assertEquals(0, tool.executionCount);
    }

    @Test
    void rejectsBooleanStatusBeforeBusinessToolExecution() {
        ObjectNode invalidParams = objectMapper.createObjectNode()
            .put("amount", 1)
            .put("status", true);

        ToolExecutor.ExecutionOutcome outcome = execute(tool.name(), invalidParams, null);

        assertDenied(outcome, ToolExecutor.TOOL_ARGUMENTS_INVALID);
        assertEquals(0, tool.executionCount);
    }

    @Test
    void acceptsNullSentinelForOptionalSchemaField() {
        ObjectNode params = validParams().put("status", "null");

        ToolExecutor.GateDecision decision = executor.checkArguments(tool.name(), params);

        assertTrue(decision.allowed());
    }

    @Test
    void normalizesIntegralTextBeforeBusinessToolExecution() {
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(42L);
        when(currentOwnerService.requireCurrentUserId()).thenReturn(100L);
        when(currentOwnerService.findCurrentStoreId()).thenReturn(Optional.of(7L));

        ObjectNode params = validParams().put("status", "1");
        ToolExecutor.ExecutionOutcome outcome = execute(
            tool.name(), params, null, new AgentRunState("run-1", 10L, 42L, 7L, 1));

        assertTrue(outcome.executed());
        assertEquals(1, tool.lastParams.path("status").intValue());
        assertTrue(tool.lastParams.path("status").isIntegralNumber());
    }

    @Test
    void rejectsNonIntegralTextForIntegerSchema() {
        ObjectNode params = validParams().put("status", "1.0");

        ToolExecutor.ExecutionOutcome outcome = execute(tool.name(), params, null);

        assertDenied(outcome, ToolExecutor.TOOL_ARGUMENTS_INVALID);
        assertEquals(0, tool.executionCount);
    }

    @Test
    void rejectsMissingPermissionBeforeResolvingOwnerContext() {
        doThrow(new AccessDeniedException("denied"))
            .when(currentOwnerService).requirePermissions("agent:view");

        ToolExecutor.ExecutionOutcome outcome = execute(tool.name(), validParams(), null);

        assertDenied(outcome, ToolExecutor.TOOL_PERMISSION_DENIED);
        assertEquals(0, tool.executionCount);
        verify(currentOwnerService).requirePermissions("agent:view");
    }

    @Test
    void rejectsRunStateOwnerDifferentFromCurrentOwnerBeforeToolExecution() {
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(99L);

        ToolExecutor.ExecutionOutcome outcome = execute(
            tool.name(), validParams(), null, new AgentRunState("run-1", 10L, 42L, 7L, 1));

        assertDenied(outcome, ToolExecutor.TOOL_CONTEXT_INVALID);
        assertEquals(0, tool.executionCount);
        verify(currentOwnerService).requireCurrentOwnerUserId();
    }

    @Test
    void executesValidToolAfterAllGatesPass() {
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(42L);
        when(currentOwnerService.requireCurrentUserId()).thenReturn(100L);
        when(currentOwnerService.findCurrentStoreId()).thenReturn(Optional.of(7L));
        ToolResult expected = ToolResult.empty("executed");
        tool.result = expected;

        ToolExecutor.ExecutionOutcome outcome = execute(
            tool.name(), validParams(), null, new AgentRunState("run-1", 10L, 42L, 7L, 1));

        assertTrue(outcome.executed());
        assertSame(expected, outcome.result());
        assertEquals(1, tool.executionCount);
        assertEquals(42L, tool.context.ownerUserId());
        assertEquals(100L, tool.context.userId());
        assertEquals(7L, tool.context.storeId());
        verify(currentOwnerService).requirePermissions("agent:view");
    }

    private ToolExecutor.ExecutionOutcome execute(String toolName, JsonNode params, Set<String> allowedTools) {
        return execute(toolName, params, allowedTools, new AgentRunState("run-1", 10L, 42L, 7L, 1));
    }

    private ToolExecutor.ExecutionOutcome execute(
        String toolName, JsonNode params, Set<String> allowedTools, AgentRunState runState
    ) {
        return executor.execute(
            runState,
            toolName,
            params,
            allowedTools,
            10L,
            "run-1",
            null,
            objectMapper
        );
    }

    private ObjectNode validParams() {
        return objectMapper.createObjectNode().put("amount", 1);
    }

    private ObjectNode schema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("amount").put("type", "integer").put("minimum", 1);
        schema.with("properties").putObject("status").put("type", "integer").putArray("enum").add(0).add(1);
        schema.putArray("required").add("amount");
        schema.put("additionalProperties", false);
        return schema;
    }

    private static void assertDenied(ToolExecutor.ExecutionOutcome outcome, String reasonCode) {
        assertFalse(outcome.executed());
        assertEquals(reasonCode, outcome.decision().reasonCode());
        assertTrue(outcome.result() == null);
    }

    private static final class CountingTool implements AgentTool {
        private final JsonNode schema;
        private int executionCount;
        private ToolContext context;
        private JsonNode lastParams;
        private ToolResult result = ToolResult.empty("default");

        private CountingTool(JsonNode schema) {
            this.schema = schema;
        }

        @Override
        public String name() {
            return "counting_tool";
        }

        @Override
        public String displayName() {
            return "Counting tool";
        }

        @Override
        public String description() {
            return "Test-only tool";
        }

        @Override
        public ToolType type() {
            return ToolType.READ_ONLY;
        }

        @Override
        public JsonNode parameterSchema() {
            return schema;
        }

        @Override
        public ToolResult execute(ToolContext context, JsonNode params) {
            executionCount++;
            this.context = context;
            this.lastParams = params;
            return result;
        }
    }
}
