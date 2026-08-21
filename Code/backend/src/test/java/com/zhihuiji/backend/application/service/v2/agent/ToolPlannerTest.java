package com.zhihuiji.backend.application.service.v2.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentToolPlan;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.application.service.v2.agent.component.ToolPlanner;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToolPlannerTest {

    @Mock
    private LongCatAnthropicClient longCatAnthropicClient;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private AgentTool supplierStatementTool;

    @Test
    void supplierStatementUsesModelAutoChoiceInsteadOfServerNamedChoice() {
        when(supplierStatementTool.name()).thenReturn("supplier_statement_lookup");
        when(supplierStatementTool.description()).thenReturn("查询供应商对账事实");
        when(supplierStatementTool.type()).thenReturn(AgentTool.ToolType.READ_ONLY);
        when(supplierStatementTool.parameterSchema()).thenReturn(new ObjectMapper().createObjectNode()
            .putObject("properties"));
        when(toolRegistry.getTool("supplier_statement_lookup")).thenReturn(Optional.of(supplierStatementTool));
        when(toolRegistry.listTools()).thenReturn(List.of(supplierStatementTool));
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-supplier-statement",
                    "supplier_statement_lookup",
                    new ObjectMapper().createObjectNode()
                )),
                "模型选择供应商对账工具"
            )));

        ToolPlanner planner = new ToolPlanner(longCatAnthropicClient, toolRegistry, new ObjectMapper());
        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我和供应商对一下账",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertTrue(plan.get().tools().contains("supplier_statement_lookup"));
        assertTrue("model_tool_call".equals(plan.get().selectionOrigin()));
        verify(longCatAnthropicClient).createMessageWithTools(anyString(), anyString(), any());
        verify(longCatAnthropicClient, never())
            .createMessageWithTools(anyString(), anyString(), any(), anyString());
    }

    @Test
    void preservesDistinctSameToolCallsButAppliesPerCallPlanLimit() {
        when(supplierStatementTool.name()).thenReturn("supplier_statement_lookup");
        when(supplierStatementTool.description()).thenReturn("查询供应商对账事实");
        when(supplierStatementTool.type()).thenReturn(AgentTool.ToolType.READ_ONLY);
        when(supplierStatementTool.parameterSchema()).thenReturn(new ObjectMapper().createObjectNode()
            .putObject("properties"));
        when(toolRegistry.getTool("supplier_statement_lookup")).thenReturn(Optional.of(supplierStatementTool));
        when(toolRegistry.listTools()).thenReturn(List.of(supplierStatementTool));

        ObjectMapper mapper = new ObjectMapper();
        List<LongCatAnthropicClient.ToolUseBlock> calls = java.util.stream.IntStream.range(0, 8)
            .mapToObj(index -> new LongCatAnthropicClient.ToolUseBlock(
                "call-" + index,
                "supplier_statement_lookup",
                mapper.createObjectNode().put("sequence", index)
            ))
            .toList();
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(calls, null)));

        ToolPlanner planner = new ToolPlanner(longCatAnthropicClient, toolRegistry, mapper);
        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我和供应商对一下账",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(1, plan.get().tools().size());
        assertEquals(6, plan.get().nativeToolCallBlocks().size());
        assertEquals("call-0", plan.get().nativeToolCallBlocks().get(0).toolCallId());
        assertEquals("call-5", plan.get().nativeToolCallBlocks().get(5).toolCallId());
    }

    @Test
    void singleTopicContinuationStopsWithoutExplicitVisualizationRequest() {
        AgentTool cashflowTool = tool("cashflow_summary_lookup", "汇总现金流");
        when(toolRegistry.getTool("cashflow_summary_lookup")).thenReturn(Optional.of(cashflowTool));
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        ObjectMapper mapper = new ObjectMapper();

        ToolPlanner planner = new ToolPlanner(longCatAnthropicClient, toolRegistry, mapper);
        ObjectNode facts = mapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);
        Optional<AgentToolPlan> next = planner.planNextIteration(
            "最近现金流怎么样？收入、支出和净现金流帮我算一下。",
            new AgentToolPlan(List.of("cashflow_summary_lookup"), "已查询"),
            List.of(new ToolExecutionResult(
                "cashflow_summary_lookup", "已返回现金流", facts, false, "call-1", 1
            )),
            List.of(),
            2
        );

        assertTrue(next.isEmpty());
        verify(longCatAnthropicClient, never()).createMessageWithTools(anyString(), anyString(), any());
    }

    @Test
    void retriesOnlyThePendingPurchaseDraftAfterDependencyFacts() {
        AgentTool supplierDirectory = tool("supplier_directory_lookup", "查询真实供应商");
        AgentTool productCatalog = tool("product_catalog_lookup", "查询真实商品");
        AgentTool purchaseDraft = tool("create_purchase_order", "生成采购草稿", AgentTool.ToolType.CREATE_ONLY);
        when(toolRegistry.getTool("supplier_directory_lookup")).thenReturn(Optional.of(supplierDirectory));
        when(toolRegistry.getTool("product_catalog_lookup")).thenReturn(Optional.of(productCatalog));
        when(toolRegistry.getTool("create_purchase_order")).thenReturn(Optional.of(purchaseDraft));
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);

        ObjectMapper mapper = new ObjectMapper();
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(), "我先确认一下真实数据。"
            )));
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any(), anyString()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-create-purchase",
                    "create_purchase_order",
                    mapper.createObjectNode()
                        .put("supplier_id", 11L)
                        .put("product_id", 22L)
                        .put("quantity", 1)
                        .put("unit_price", 1.23)
                )),
                null
            )));

        ObjectNode supplierFacts = mapper.createObjectNode();
        supplierFacts.putObject("query_audit").put("returned_count", 1);
        ObjectNode productFacts = mapper.createObjectNode();
        productFacts.putObject("query_audit").put("returned_count", 1);
        ToolPlanner planner = new ToolPlanner(longCatAnthropicClient, toolRegistry, mapper);

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "向现有供应商买一个真实商品，数量 1、单价 1.23，先做采购草稿让我看看。",
            new AgentToolPlan(
                List.of("supplier_directory_lookup", "product_catalog_lookup"),
                "已查真实依赖"
            ),
            List.of(
                new ToolExecutionResult("supplier_directory_lookup", "找到 1 个供应商", supplierFacts, false),
                new ToolExecutionResult("product_catalog_lookup", "找到 1 个商品", productFacts, false)
            ),
            List.of(),
            2
        );

        assertTrue(next.isPresent());
        assertEquals(List.of("create_purchase_order"), next.get().tools());
        assertEquals("native_tool_use_write_target_retry", next.get().source());
        verify(longCatAnthropicClient).createMessageWithTools(anyString(), anyString(), any(), eq("auto"));
    }

    private AgentTool tool(String name, String description) {
        return tool(name, description, AgentTool.ToolType.READ_ONLY);
    }

    private AgentTool tool(String name, String description, AgentTool.ToolType type) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String displayName() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public ToolType type() {
                return type;
            }

            @Override
            public com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult execute(
                com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext ctx,
                com.fasterxml.jackson.databind.JsonNode params
            ) {
                return com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult.empty(description);
            }
        };
    }
}
