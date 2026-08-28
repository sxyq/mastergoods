package com.zhihuiji.backend.application.service.v2.agent.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentToolPlan;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.NativeToolCallBlock;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class ToolPlannerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void imageGenerationIntentOffersImageGenerateAsTheNativeToolChoice() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode().put("prompt", "生成商品主图");
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-image", "image_generate", params
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("image_generate", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我生成一张商品图片，先给我确认。", List.of(), null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("image_generate"), plan.get().tools());
        assertEquals("image_generate", plan.get().nativeToolCallBlocks().get(0).toolName());
    }

    @Test
    void modelAutoChoiceUsesSalesReturnToolAfterRealOrderItemsAreAvailable() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode();
        params.put("sale_order_id", 42L);
        params.put("reason", "全量工具测试");
        params.putArray("items").addObject()
            .put("product_id", 7L)
            .put("product_name", "测试商品")
            .put("quantity", 1)
            .put("price", 1.23);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(new LongCatAnthropicClient.ToolUseBlock(
                "call-sales-return", "create_sales_return", params
            )),
            null
        )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sale_order_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_full_chain_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_sales_return", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 2);
        facts.putArray("recent_orders").addObject()
            .putArray("items").addObject()
            .put("product_id", 7L)
            .put("product_name", "测试商品")
            .put("quantity", 2)
            .put("price", 1.23);
        ToolExecutionResult lookup = new ToolExecutionResult(
            "sale_order_lookup", "最近销售单 1 条", facts, false
        );

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "把一张销售单退 1 件，原因写全量工具测试，先做草稿让我确认。",
            List.of(lookup),
            2
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("create_sales_return"), plan.get().tools());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    void singleTopicMultipleReadCallsAreClarifiedAndOnlyOneIsReturned() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock("call-health", "account_health_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock("call-balance", "account_balance_lookup", objectMapper.createObjectNode())
                ),
                null
            )));
        when(client.createMessageWithTools(anyString(), anyString(), anyList(), eq("auto")))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-health-clarified", "account_health_lookup", objectMapper.createObjectNode()
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("account_health_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("account_balance_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("account_transfer_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我看一下资金账户状态。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("account_health_lookup"), plan.get().tools());
        assertEquals("native_tool_use_clarified", plan.get().source());
        assertTrue(plan.get().rationale().contains("account_balance_lookup"));
        assertTrue(plan.get().rationale().contains("account_health_lookup"));
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList(), eq("auto"));
    }

    @Test
    void unresolvedMultipleReadCallsBecomeAuditableSelectionFailureWithoutExecutionPlan() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock("call-health", "account_health_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock("call-balance", "account_balance_lookup", objectMapper.createObjectNode())
                ),
                null
            )));
        when(client.createMessageWithTools(anyString(), anyString(), anyList(), eq("auto")))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock("call-health-clarified", "account_health_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock("call-balance-clarified", "account_balance_lookup", objectMapper.createObjectNode())
                ),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("account_health_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("account_balance_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("account_transfer_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我看一下资金账户状态。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of(), plan.get().tools());
        assertEquals("model_tool_selection_failed", plan.get().source());
        assertEquals(4, plan.get().nativeToolCallBlocks().size());
        assertTrue(plan.get().rationale().contains("澄清轮仍选择多个"));
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList(), eq("auto"));
    }

    @Test
    void explicitMultiSourceRequestKeepsMultipleModelCallsWithoutClarification() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock("call-sales", "sales_overview_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock("call-cash", "cashflow_summary_lookup", objectMapper.createObjectNode())
                ),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("cashflow_summary_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "把最近销售和现金流放一起看看。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("sales_overview_lookup", "cashflow_summary_lookup"), plan.get().tools());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), eq("auto"));
    }

    @Test
    void focusedNativeSelectionDoesNotRepeatTheSameCandidateRequest() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode();
        params.put("sale_order_id", 42L);
        params.put("reason", "全量工具测试");
        params.putArray("items").addObject()
            .put("product_id", 7L)
            .put("product_name", "测试商品")
            .put("quantity", 1)
            .put("price", 1.23);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-sales-return-focused", "create_sales_return", params
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sale_order_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_sales_return", AgentTool.ToolType.CREATE_ONLY),
            new TestTool("create_purchase_return", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithLlm(
            "把一张销售单退 1 件，原因写全量工具测试，先做草稿让我确认。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("create_sales_return"), plan.get().tools());
        assertEquals("native_tool_use", plan.get().source());
        ArgumentCaptor<List> toolDefinitions = ArgumentCaptor.forClass(List.class);
        verify(client, org.mockito.Mockito.times(1))
            .createMessageWithTools(anyString(), anyString(), toolDefinitions.capture());
        List<?> focusedTools = toolDefinitions.getValue();
        assertEquals(
            Set.of("sale_order_lookup", "create_sales_return"),
            focusedTools.stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void llmPlanningUsesHighConfidenceProductSupplierCandidateScope() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product-supplier", "product_supplier_relation_lookup", objectMapper.createObjectNode()
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_supplier_relation_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithLlm(
            "这些商品分别是从哪家供应商进的？最近采购价是多少？",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("product_supplier_relation_lookup"), plan.get().tools());
        ArgumentCaptor<List> toolDefinitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), toolDefinitions.capture());
        assertEquals(
            Set.of("product_supplier_relation_lookup"),
            toolDefinitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void modelAutoChoiceUsesVisualizationToolAfterRealFactsWhenUserRequestsAChart() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode();
        params.put("mode", "chart");
        params.put("reason", "用户明确要求趋势图");
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(new LongCatAnthropicClient.ToolUseBlock(
                "call-visualization", "result_visualization", params
            )),
            null
        )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 2);
        facts.put("sales_count", 2);
        ToolExecutionResult lookup = new ToolExecutionResult(
            "sales_overview_lookup", "近7天销售 2 笔", facts, false
        );

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "最近销售给我画一张趋势图。",
            List.of(lookup),
            2
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("result_visualization"), plan.get().tools());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    void continuationKeepsModelAutoChoiceForVisualizationCandidate() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.supportsToolResultContinuation()).thenReturn(true);
        when(client.continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        )).thenReturn(Optional.empty());
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.empty());

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 2);
        AgentToolPlan previousPlan = new AgentToolPlan(
            List.of("sales_overview_lookup"),
            "首轮查询",
            "native_tool_use",
            Map.of(),
            "response-1",
            List.of(new NativeToolCallBlock("call-1", "sales_overview_lookup", "{}"))
        );

        planner.planNextIteration(
            "最近销售给我画一张趋势图。",
            previousPlan,
            List.of(new ToolExecutionResult("sales_overview_lookup", "近7天销售 2 笔", facts, false)),
            List.of(),
            2
        );

        verify(client).continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        );
        verify(client, never()).continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList(), anyString()
        );
    }

    @Test
    void modelCannotChooseVisualizationWithoutExplicitDisplayRequest() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode().put("mode", "chart");
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-visualization-auto", "result_visualization", params
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 3);

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "最近销售情况怎么样？",
            List.of(new ToolExecutionResult("sales_overview_lookup", "销售 3 笔", facts, false)),
            2
        );

        assertTrue(plan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
    }

    @Test
    void purchaseChainMayContinueWithRelatedQueriesButNotVisualizationByDefault() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-order", "purchase_order_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-receipt", "purchase_receipt_lookup", objectMapper.createObjectNode())
                ),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("purchase_tracking_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("purchase_order_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("purchase_receipt_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 0);

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "帮我把采购单、入库和退货的关联过程串起来看看。",
            List.of(new ToolExecutionResult("purchase_tracking_lookup", "未匹配到采购单", facts, false)),
            2
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("purchase_order_lookup", "purchase_receipt_lookup"), plan.get().tools());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertTrue(definitions.getValue().stream()
            .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
            .noneMatch("result_visualization"::equals));
    }

    @Test
    void modelAutoChoiceUsesAccountTransferAfterDirectionAndRealAccountsAreAvailable() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode()
            .put("from_account_id", 1L)
            .put("to_account_id", 2L)
            .put("amount", 1.23D);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-account-transfer", "create_account_transfer", params
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("account_balance_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_account_transfer", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "把第一个账户的钱转到第二个账户，金额 1.23 元，先做草稿。",
            List.of(new ToolExecutionResult("account_balance_lookup", "资金账户 2 个", facts, false)),
            2
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("create_account_transfer"), plan.get().tools());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    void modelAutoChoiceUsesProductDraftAfterProductFactsAreAvailable() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode()
            .put("name", "全量工具测试商品")
            .put("code", "EVAL-ONLY-20260802");
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-create-product", "create_product", params
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_product", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "帮我加个商品，名称全量工具测试商品，编码 EVAL-ONLY-20260802，先生成草稿。",
            List.of(new ToolExecutionResult("product_catalog_lookup", "商品总数 3 个", facts, false)),
            2
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("create_product"), plan.get().tools());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    void nativeContinuationUsesRealToolOutputForDependentPosterTool() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.supportsToolResultContinuation()).thenReturn(true);
        ObjectNode params = objectMapper.createObjectNode().put("product_id", 7L);
        when(client.continueWithToolOutputs(
            any(), anyString(), anyString(), anyList(), anyList(), anyList()
        )).thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(new LongCatAnthropicClient.ToolUseBlock(
                "call-poster", "generate_poster_prompt", params
            )),
            null
        )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("generate_poster_prompt", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);
        facts.putArray("top_products").addObject().put("product_id", 7L);
        ToolExecutionResult lookup = new ToolExecutionResult(
            "product_catalog_lookup", "商品总数 1 个", facts, false, "call-products", 1
        );
        AgentToolPlan previousPlan = new AgentToolPlan(
            List.of("product_catalog_lookup"),
            "先查询商品",
            "native_tool_use",
            Map.of(),
            null,
            List.of(new NativeToolCallBlock("call-products", "product_catalog_lookup", "{}"))
        );

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "拿商品信息帮我写个海报提示词，先不要生成图片。",
            previousPlan,
            List.of(lookup),
            List.of(),
            2
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("generate_poster_prompt"), plan.get().tools());
        assertEquals("native_tool_use_react_continuation", plan.get().source());
        verify(client).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
    }

    @Test
    void modelAutoChoiceUsesSupplierStatementForExplicitSupplierReconciliation() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-supplier-statement", "supplier_statement_lookup", objectMapper.createObjectNode()
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("supplier_statement_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("supplier_directory_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我和供应商对一下账，余额、采购和退货都算进去。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("supplier_statement_lookup"), plan.get().tools());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    void simpleManagementSummaryAdvertisesOnlyRelevantToolAndStopsAfterItReturnsFacts() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("report_query", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("cashflow_summary_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        assertEquals(
            List.of("report_query"),
            planner.candidateToolNamesForMessage("帮我看看这个月销售怎么样，给我一个经营汇总。")
        );

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);
        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "帮我看看这个月销售怎么样，给我一个经营汇总。",
            new AgentToolPlan(List.of("report_query"), "查询报表", "native_tool_use", Map.of()),
            List.of(new ToolExecutionResult("report_query", "销售汇总 销售额 ¥37.00", facts, false)),
            List.of(),
            2
        );

        assertTrue(nextPlan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void initialNativePlanningLetsTheModelChooseAmongPermittedTools() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-sales", "sales_overview_lookup", objectMapper.createObjectNode()
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("cashflow_summary_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("payment_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "最近店里的销售怎么样？",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("sales_overview_lookup"), plan.get().tools());
        ArgumentCaptor<List> toolDefinitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), toolDefinitions.capture());
        assertEquals(
            Set.of("cashflow_summary_lookup", "payment_lookup", "sales_overview_lookup"),
            toolDefinitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void initialNativePlanningExposesAllPermittedToolsToTheModel() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-account-health", "account_health_lookup", objectMapper.createObjectNode()
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("account_health_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("anomaly_alert_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("account_balance_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我看下资金账户最近状态，有没有什么异常？",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("account_health_lookup"), plan.get().tools());
        ArgumentCaptor<List> toolDefinitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), toolDefinitions.capture());
        assertEquals(
            Set.of("account_health_lookup", "anomaly_alert_lookup", "account_balance_lookup"),
            toolDefinitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void initialNativePlanningExposesOnlyRelevantToolsForDraftRequest() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-create-product", "create_product", objectMapper.createObjectNode()
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("product_category_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_product", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我加个商品，名称全量工具测试商品，编码 EVAL-ONLY-20260802，先生成草稿。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("create_product"), plan.get().tools());
        ArgumentCaptor<List> toolDefinitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), toolDefinitions.capture());
        assertEquals(
            Set.of("create_product"),
            toolDefinitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void chartRequestAdvertisesAllPermittedReadToolsBeforeVisualizationDecision() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-trend", "sales_trend_lookup", objectMapper.createObjectNode()
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_trend_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("payment_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("cashflow_summary_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "最近一周销售和回款给我画一张趋势图。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("sales_trend_lookup"), plan.get().tools());
        ArgumentCaptor<List> toolDefinitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), toolDefinitions.capture());
        assertEquals(
            Set.of(
                "sales_trend_lookup",
                "payment_lookup"
            ),
            toolDefinitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void trendReceivableContinuationExposesOnlyTheMissingSource() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.empty());

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_trend_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("payment_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 8);
        AgentToolPlan previousPlan = new AgentToolPlan(
            List.of("sales_trend_lookup"),
            "首轮查询趋势",
            "native_tool_use",
            Map.of(),
            "response-trend",
            List.of(new NativeToolCallBlock("call-trend", "sales_trend_lookup", "{}"))
        );

        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "最近一周销售和回款给我画一张趋势图。",
            previousPlan,
            List.of(new ToolExecutionResult(
                "sales_trend_lookup", "销售趋势 8 桶", facts, false, "call-trend", 1
            )),
            List.of(),
            2
        );

        assertTrue(nextPlan.isEmpty());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("payment_lookup"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void continuationDoesNotExpandAPlainCashChangeQuestionIntoAnUnrelatedScan() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.empty());

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("cash_change_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("payment_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("finance_record_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("account_balance_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("account_transfer_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 2);

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "最近的钱都怎么进出的？把资金变动列一下。",
            new AgentToolPlan(List.of("cash_change_lookup"), "查询资金变动", "native_tool_use", Map.of()),
            List.of(new ToolExecutionResult("cash_change_lookup", "资金变动 2 条", facts, false)),
            List.of(),
            2
        );

        assertTrue(plan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
    }

    @Test
    void stopsPlanningAfterTheBoundedContinuationBudget() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolPlanner planner = new ToolPlanner(
            client,
            new ToolRegistry(List.of(new TestTool("cash_change_lookup", AgentTool.ToolType.READ_ONLY))),
            objectMapper
        );

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "最近的钱都怎么进出的？",
            new AgentToolPlan(List.of("cash_change_lookup"), "查询资金变动", "native_tool_use", Map.of()),
            List.of(),
            List.of(),
            3
        );

        assertTrue(plan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void supplierPayableDoesNotReopenTheWholeRegistryAfterTheTargetQueryReturnsFacts() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("supplier_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("purchase_order_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("supplier_statement_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("receivable_payable_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        String message = "我还欠哪些供应商钱？金额和采购情况一起看看。";
        assertEquals(List.of("supplier_payable_lookup"), planner.candidateToolNamesForMessage(message));

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 0);
        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            message,
            new AgentToolPlan(List.of("supplier_payable_lookup"), "查询供应商欠款", "native_tool_use", Map.of()),
            List.of(new ToolExecutionResult("supplier_payable_lookup", "应付供应商总数 0 个", facts, false)),
            List.of(),
            2
        );

        assertTrue(nextPlan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void singleReadResultStopsBeforeProviderCanAddUnrelatedTools() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("cashflow_summary_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("cash_change_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("payment_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);

        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "最近现金流怎么样？",
            new AgentToolPlan(List.of("cashflow_summary_lookup"), "查询现金流", "native_tool_use", Map.of()),
            List.of(new ToolExecutionResult("cashflow_summary_lookup", "净现金流 ¥37.00", facts, false)),
            List.of(),
            2
        );

        assertTrue(nextPlan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void ambiguousAccountHealthQuestionDoesNotExpandIntoAnomalyScan() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("account_health_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("anomaly_alert_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);

        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "帮我看下资金账户最近状态，有没有什么异常？",
            new AgentToolPlan(List.of("account_health_lookup"), "查询账户状态", "native_tool_use", Map.of()),
            List.of(new ToolExecutionResult(
                "account_health_lookup", "资金账户状态已返回", facts, false
            )),
            List.of(),
            2
        );

        assertTrue(nextPlan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void textOnlyToolContinuationStopsWithoutStartingAnotherSelectionRound() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.supportsToolResultContinuation()).thenReturn(true);
        when(client.continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(), "真实商品结果已经足够回答", "response-terminal"
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("generate_poster_prompt", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);
        ToolExecutionResult lookup = new ToolExecutionResult(
            "product_catalog_lookup", "商品总数 1 个", facts, false, "call-products", 1
        );
        AgentToolPlan previousPlan = new AgentToolPlan(
            List.of("product_catalog_lookup"),
            "先查询商品",
            "native_tool_use",
            Map.of(),
            "response-products",
            List.of(new NativeToolCallBlock("call-products", "product_catalog_lookup", "{}"))
        );

        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "拿商品信息帮我写个海报提示词",
            previousPlan,
            List.of(lookup),
            List.of(),
            2
        );

        assertTrue(nextPlan.isPresent());
        assertTrue(nextPlan.get().tools().isEmpty());
        assertEquals("model_terminal_after_tool_results", nextPlan.get().source());
        assertEquals("真实商品结果已经足够回答", nextPlan.get().terminalAnswer());
        verify(client).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
    }

    @Test
    void unknownQuestionMayUseTheInitialRegistryButCannotExpandAfterACompletedLookup() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("store_info_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("cashflow_summary_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);

        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "帮我随便看一下店里的情况。",
            new AgentToolPlan(List.of("store_info_lookup"), "查询门店", "native_tool_use", Map.of()),
            List.of(new ToolExecutionResult("store_info_lookup", "门店信息已返回", facts, false)),
            List.of(),
            2
        );

        assertTrue(nextPlan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void multiDomainQuestionKeepsModelChoiceWithinRelevantToolScope() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("cashflow_summary_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        assertEquals(
            Set.of("sales_overview_lookup", "cashflow_summary_lookup", "result_visualization"),
            Set.copyOf(planner.candidateToolNamesForMessage("最近一周销售和现金流放在一起看下，合适的话用图展示。"))
        );
    }

    @Test
    void trendAndCollectionQuestionDoesNotOfferRedundantSalesOverview() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_trend_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("payment_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_overview_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        assertEquals(
            Set.of("sales_trend_lookup", "payment_lookup", "result_visualization"),
            Set.copyOf(planner.candidateToolNamesForMessage("最近一周销售和回款给我画一张趋势图。"))
        );
    }

    @Test
    void highSignalCompoundQuestionsUseDedicatedReadTools() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("anomaly_alert_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_profile_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_receivable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_category_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_price_level_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sale_order_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("sales_full_chain_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("receivable_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("supplier_payable_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        assertEquals(
            List.of("anomaly_alert_lookup"),
            planner.candidateToolNamesForMessage("最近一周销售下滑、缺货、客户欠款帮我扫一遍")
        );
        assertEquals(
            List.of("product_category_lookup"),
            planner.candidateToolNamesForMessage("商品分类现在怎么分的？")
        );
        assertEquals(
            List.of("product_catalog_lookup", "product_category_lookup"),
            planner.candidateToolNamesForMessage("把现在的商品、库存、价格和分类一起给我看下。")
        );
        assertEquals(
            List.of("product_price_level_lookup"),
            planner.candidateToolNamesForMessage("商品价格等级有哪些？")
        );
        assertEquals(
            List.of("sale_order_lookup"),
            planner.candidateToolNamesForMessage("最近卖出去的单子，客户和收款也带上")
        );
        assertEquals(
            List.of("sales_full_chain_lookup"),
            planner.candidateToolNamesForMessage("销售单、收款和退货的关联记录串起来")
        );
        assertEquals(
            List.of("receivable_payable_lookup"),
            planner.candidateToolNamesForMessage("客户欠款和供应商应付款一起算一下")
        );
        assertEquals(
            List.of("customer_receivable_lookup"),
            planner.candidateToolNamesForMessage("帮我看看客户欠款")
        );
        assertEquals(
            List.of("supplier_payable_lookup"),
            planner.candidateToolNamesForMessage("帮我看看供应商应付款")
        );
        assertEquals(
            List.of("customer_profile_lookup"),
            planner.candidateToolNamesForMessage("帮我看看客户整体情况，余额、下单、收款和退货都说说。")
        );
    }

    @Test
    void combinedReceivablePayableContinuationDoesNotReopenSplitTools() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.empty());
        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("receivable_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_receivable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("supplier_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        )), objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 2);

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "客户欠款和供应商应付款一起算一下，重点对象用表格列出来",
            new AgentToolPlan(
                List.of("receivable_payable_lookup"),
                "查询综合应收应付",
                "native_tool_use",
                Map.of()
            ),
            List.of(new ToolExecutionResult(
                "receivable_payable_lookup", "应收应付 2 条", facts, false
            )),
            List.of(),
            2
        );

        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("result_visualization"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
        assertTrue(next.isEmpty());
    }

    @Test
    void simpleProductDraftKeepsModelChoiceInsideTheWriteIntentScope() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("product_category_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_product", AgentTool.ToolType.CREATE_ONLY),
            new TestTool("create_customer", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        assertEquals(
            List.of("create_product"),
            planner.candidateToolNamesForMessage(
                "帮我加个商品，名称全量工具测试商品，编码 EVAL-ONLY-20260802，先生成草稿。"
            )
        );
    }

    @Test
    void inventoryAndRestockQuestionKeepsBothReadToolsAvailableToModel() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        assertEquals(
            Set.of("inventory_panorama_lookup", "smart_restock_lookup", "result_visualization"),
            Set.copyOf(planner.candidateToolNamesForMessage(
                "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。"
            ))
        );
    }

    @Test
    void inventoryRestockContinuationDoesNotExposeLowStockOrSnapshotTools() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.supportsToolResultContinuation()).thenReturn(true);
        when(client.continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        )).thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(new LongCatAnthropicClient.ToolUseBlock(
                "call-panorama", "inventory_panorama_lookup", objectMapper.createObjectNode()
            )),
            null,
            "response-panorama"
        )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_low_stock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_snapshot_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 3);

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。",
            new AgentToolPlan(
                List.of("smart_restock_lookup"),
                "先查补货建议",
                "native_tool_use",
                Map.of(),
                "response-restock",
                List.of(new NativeToolCallBlock("call-restock", "smart_restock_lookup", "{}"))
            ),
            List.of(new ToolExecutionResult(
                "smart_restock_lookup", "补货建议已返回", facts, false
            )),
            List.of(),
            2
        );

        assertTrue(next.isPresent());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), definitions.capture()
        );
        assertEquals(
            Set.of("inventory_panorama_lookup"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void initialInventoryAndRestockPlanningOnlyAdvertisesExplicitlyRelevantTools() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-restock", "smart_restock_lookup", objectMapper.createObjectNode()
                )),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_low_stock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_snapshot_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "库存现状和哪些要补？用合适的方式展示。", List.of(), null
        );

        assertTrue(plan.isPresent());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("inventory_panorama_lookup", "smart_restock_lookup"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void initialCustomerDraftPlanningDoesNotAdvertiseOptionalGroupLookup() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-customer", "create_customer", objectMapper.createObjectNode()
                )),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("create_customer", AgentTool.ToolType.CREATE_ONLY),
            new TestTool("partner_group_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_directory_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我加一个客户，名字叫全量工具测试客户，电话 13900000001，先把要保存的内容给我确认。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("create_customer"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void initialReceivablePayablePlanningUsesTheCombinedCoverageTool() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable-payable",
                    "receivable_payable_lookup",
                    objectMapper.createObjectNode()
                )),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("receivable_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_receivable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("supplier_payable_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "客户欠款和供应商应付款一起算一下，重点对象用表格列出来。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("receivable_payable_lookup"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void combinedReceivablePayableResultDoesNotReopenNarrowTools() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("receivable_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_receivable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("supplier_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 0);

        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "客户欠款和供应商应付款一起算一下，重点对象用表格列出来。",
            new AgentToolPlan(
                List.of("receivable_payable_lookup"),
                "查询应收应付",
                "native_tool_use",
                Map.of()
            ),
            List.of(new ToolExecutionResult(
                "receivable_payable_lookup", "应收 ¥0.00，应付 ¥0.00", facts, false
            )),
            List.of(),
            2
        );

        assertTrue(nextPlan.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void completedInventoryRestockStateAccumulatesAcrossVisualizationContinuation() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_low_stock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_snapshot_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        )), objectMapper);
        ObjectNode visualizationFacts = objectMapper.createObjectNode()
            .put("visualization_enabled", true)
            .put("mode", "table");

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。",
            new AgentToolPlan(
                List.of(
                    "inventory_panorama_lookup",
                    "smart_restock_lookup",
                    "result_visualization"
                ),
                "库存和补货已完成并展示",
                "native_tool_use_react",
                Map.of()
            ),
            List.of(new ToolExecutionResult(
                "result_visualization", "展示完成", visualizationFacts, false
            )),
            List.of(),
            4
        );

        assertTrue(next.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void completedReceivablePayableStateAccumulatesAcrossVisualizationContinuation() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("receivable_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_receivable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("supplier_payable_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        )), objectMapper);
        ObjectNode visualizationFacts = objectMapper.createObjectNode()
            .put("visualization_enabled", true)
            .put("mode", "table");

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "客户欠款和供应商应付款一起算一下，重点对象用表格列出来。",
            new AgentToolPlan(
                List.of("receivable_payable_lookup", "result_visualization"),
                "综合应收应付已完成并展示",
                "native_tool_use_react",
                Map.of()
            ),
            List.of(new ToolExecutionResult(
                "result_visualization", "展示完成", visualizationFacts, false
            )),
            List.of(),
            3
        );

        assertTrue(next.isEmpty());
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList());
        verify(client, never()).continueWithToolOutputs(any(), anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    void failedSaleDraftTargetRemainsAvailableAfterDependencyFacts() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-sale-retry",
                    "create_sale_order",
                    objectMapper.createObjectNode()
                        .put("customer_id", 1L)
                        .put("customer_name", "测试客户A")
                        .putArray("items")
                        .addObject()
                        .put("product_id", 1L)
                        .put("product_name", "测试商品A")
                        .put("quantity", 1)
                        .put("price", 1.23)
                )),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("customer_directory_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_sale_order", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        ObjectNode customerFacts = objectMapper.createObjectNode();
        customerFacts.putObject("query_audit").put("returned_count", 1);
        ObjectNode productFacts = objectMapper.createObjectNode();
        productFacts.putObject("query_audit").put("returned_count", 1);

        Optional<AgentToolPlan> nextPlan = planner.planNextIteration(
            "给客户“测试客户A”开一单，商品用“测试商品A”，数量 1、单价 1.23，先生成销售草稿。",
            new AgentToolPlan(List.of("create_sale_order"), "销售草稿", "native_tool_use", Map.of()),
            List.of(
                new ToolExecutionResult("customer_directory_lookup", "真实客户 1 个", customerFacts, false),
                new ToolExecutionResult("product_catalog_lookup", "返回 1 个商品", productFacts, false)
            ),
            List.of(new AgentTypes.ToolFailureResult(
                "create_sale_order", "客户或商品不属于当前账号"
            )),
            3
        );

        assertTrue(nextPlan.isPresent());
        assertEquals(List.of("create_sale_order"), nextPlan.get().tools());
        verify(client).createMessageWithTools(anyString(), anyString(), anyList());
    }

    @Test
    void inventoryVisualizationIsNotAdvertisedBeforeRestockFactsAreReady() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.supportsToolResultContinuation()).thenReturn(true);
        when(client.continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        ))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-restock", "smart_restock_lookup", objectMapper.createObjectNode()
                )),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 3);

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。",
            new AgentToolPlan(
                List.of("inventory_panorama_lookup"),
                "库存全景",
                "native_tool_use",
                Map.of(),
                "response-inventory",
                List.of(new NativeToolCallBlock(
                    "call-panorama", "inventory_panorama_lookup", "{}"
                ))
            ),
            List.of(new ToolExecutionResult("inventory_panorama_lookup", "库存全景", facts, false)),
            List.of(),
            2
        );

        assertTrue(next.isPresent());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), definitions.capture()
        );
        assertEquals(
            Set.of("smart_restock_lookup"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void ordinaryInventoryPanoramaDoesNotBecomeAnInventoryAndRestockMultiSourceRequest() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-panorama", "inventory_panorama_lookup", objectMapper.createObjectNode()
                )),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("inventory_low_stock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "库存全貌、安全库存、销量、周转和补货建议一起看看。", List.of(), null
        );

        assertTrue(plan.isPresent());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("inventory_panorama_lookup"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void emptyCreateCustomerArgumentsRecoverOnlyExplicitNameAndPhone() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-customer-empty",
                    "create_customer",
                    objectMapper.createObjectNode()
                )),
                null
            )));
        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("create_customer", AgentTool.ToolType.CREATE_ONLY),
            new TestTool("partner_group_lookup", AgentTool.ToolType.READ_ONLY)
        )), objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我加一个客户，名字叫全量工具测试客户，电话 13900000001，先把要保存的内容给我确认。",
            List.of(),
            null,
            List.of("create_customer")
        );

        assertTrue(plan.isPresent());
        assertEquals("全量工具测试客户", plan.get().toolParams().get("create_customer").path("name").asText());
        assertEquals("13900000001", plan.get().toolParams().get("create_customer").path("phone").asText());
        assertTrue(plan.get().nativeToolCallBlocks().get(0).arguments().contains("全量工具测试客户"));
        assertTrue(plan.get().nativeToolCallBlocks().get(0).arguments().contains("13900000001"));
        assertTrue(plan.get().rationale().contains("仅从用户原话明确恢复 create_customer 的 name/phone"));
    }

    @Test
    void partialCreateCustomerArgumentsRecoverOnlyMissingExplicitFields() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        ObjectNode input = objectMapper.createObjectNode()
            .put("name", "")
            .put("phone", "13800000000")
            .put("group_id", 99L);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-customer-partial", "create_customer", input
                )),
                null
            )));
        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("create_customer", AgentTool.ToolType.CREATE_ONLY)
        )), objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我加一个客户，名字叫全量工具测试客户，电话 13900000001，先把要保存的内容给我确认。",
            List.of(),
            null,
            List.of("create_customer")
        );

        assertTrue(plan.isPresent());
        JsonNode params = plan.get().toolParams().get("create_customer");
        assertEquals("全量工具测试客户", params.path("name").asText());
        assertEquals("13800000000", params.path("phone").asText());
        assertEquals(99L, params.path("group_id").asLong());
    }

    @Test
    void jsonPlanningPathRecoversOnlyExplicitMissingCustomerFields() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.empty());
        when(client.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("{\"tools\":[{\"name\":\"create_customer\","
                + "\"params\":{\"name\":\"\",\"phone\":\"13800000000\",\"group_id\":99}}],"
                + "\"rationale\":\"模型选择客户草稿\"}"));
        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("create_customer", AgentTool.ToolType.CREATE_ONLY)
        )), objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithLlm(
            "帮我加一个客户，名字叫全量工具测试客户，电话 13900000001，先把要保存的内容给我确认。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        JsonNode params = plan.get().toolParams().get("create_customer");
        assertEquals("全量工具测试客户", params.path("name").asText());
        assertEquals("13800000000", params.path("phone").asText());
        assertEquals(99L, params.path("group_id").asLong());
        assertTrue(plan.get().rationale().contains("仅从用户原话明确恢复 create_customer 的 name/phone"));
    }

    @Test
    void inventoryAndRestockContinuationExcludesCompletedSources() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-panorama-repeat",
                    "inventory_panorama_lookup",
                    objectMapper.createObjectNode()
                )),
                null
            )));
        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        )), objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 2);

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。",
            new AgentToolPlan(
                List.of("inventory_panorama_lookup", "smart_restock_lookup"),
                "库存和补货",
                "native_tool_use",
                Map.of(),
                "response-inventory",
                List.of()
            ),
            List.of(
                new ToolExecutionResult("inventory_panorama_lookup", "库存全景", facts, false),
                new ToolExecutionResult("smart_restock_lookup", "补货建议", facts, false)
            ),
            List.of(),
            2
        );

        assertTrue(next.isEmpty());
    }

    @Test
    void simpleCustomerDraftDoesNotExposeOptionalGroupLookupToInitialModelChoice() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-customer", "create_customer", objectMapper.createObjectNode()
                )),
                null
            )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("create_customer", AgentTool.ToolType.CREATE_ONLY),
            new TestTool("partner_group_lookup", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "帮我加一个客户，名字叫全量工具测试客户，电话 13900000001，先把要保存的内容给我确认。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("create_customer"), plan.get().tools());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("create_customer"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void inventoryAndRestockContinuationRequiresTheRemainingExplicitDataSource() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.supportsToolResultContinuation()).thenReturn(true);
        when(client.continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        )).thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(new LongCatAnthropicClient.ToolUseBlock(
                "call-restock", "smart_restock_lookup", objectMapper.createObjectNode()
            )),
            null,
            "response-restock"
        )));
        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("inventory_panorama_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("smart_restock_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 3);

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。",
            new AgentToolPlan(
                List.of("inventory_panorama_lookup"),
                "先查库存全景",
                "native_tool_use",
                Map.of(),
                "response-panorama",
                List.of(new NativeToolCallBlock("call-panorama", "inventory_panorama_lookup", "{}"))
            ),
            List.of(new ToolExecutionResult("inventory_panorama_lookup", "库存全景 3 个商品", facts, false)),
            List.of(),
            2
        );

        assertTrue(next.isPresent());
        assertEquals(List.of("smart_restock_lookup"), next.get().tools());
        verify(client).continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        );
    }

    @Test
    void explicitChartRequestUsesNamedVisualizationToolAfterRequiredFactsArePresent() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.supportsToolResultContinuation()).thenReturn(true);
        when(client.continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        )).thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(new LongCatAnthropicClient.ToolUseBlock(
                "call-chart", "result_visualization", objectMapper.createObjectNode().put("mode", "chart")
            )),
            null
        )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("sales_trend_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("payment_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);
        ObjectNode salesFacts = objectMapper.createObjectNode();
        salesFacts.putObject("query_audit").put("returned_count", 7);
        ObjectNode paymentFacts = objectMapper.createObjectNode();
        paymentFacts.putObject("query_audit").put("returned_count", 3);
        AgentToolPlan previousPlan = new AgentToolPlan(
            List.of("sales_trend_lookup", "payment_lookup"),
            "先查销售趋势和回款",
            "native_tool_use",
            Map.of(),
            "response-sales-payment",
            List.of(
                new NativeToolCallBlock("call-sales", "sales_trend_lookup", "{}"),
                new NativeToolCallBlock("call-payment", "payment_lookup", "{}")
            )
        );

        Optional<AgentToolPlan> plan = planner.planNextIteration(
            "最近一周销售和回款给我画一张趋势图。",
            previousPlan,
            List.of(
                new ToolExecutionResult("sales_trend_lookup", "销售趋势 7 桶", salesFacts, false, "call-sales", 7),
                new ToolExecutionResult("payment_lookup", "回款 3 条", paymentFacts, false, "call-payment", 3)
            ),
            List.of(),
            2
        );

        assertTrue(plan.isPresent());
        assertEquals(List.of("result_visualization"), plan.get().tools());
        verify(client).continueWithToolOutputs(
            anyString(), anyString(), anyString(), anyList(), anyList(), anyList()
        );
    }

    @Test
    void writeRequestExposesAllRegisteredToolsAndLeavesSelectionToTheModel() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("customer_profile_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_product", AgentTool.ToolType.CREATE_ONLY),
            new TestTool("create_customer", AgentTool.ToolType.CREATE_ONLY),
            new TestTool("result_visualization", AgentTool.ToolType.READ_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        assertEquals(
            Set.of(
                "product_catalog_lookup",
                "customer_profile_lookup",
                "create_product",
                "create_customer",
                "result_visualization"
            ),
            Set.copyOf(planner.candidateToolNamesForMessage("帮我处理一下商品和客户信息，先做草稿。"))
        );
    }

    @Test
    void purchaseDependencyReadsAreNotCollapsedIntoSingleToolClarification() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-supplier", "supplier_directory_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-product", "product_catalog_lookup", objectMapper.createObjectNode())
                ),
                null
            )));

        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("supplier_directory_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_purchase_order", AgentTool.ToolType.CREATE_ONLY)
        )), objectMapper);

        Optional<AgentToolPlan> plan = planner.planToolsWithNativeFunctionCalling(
            "向供应商“测试供应商A”买商品“测试商品A”，数量 1、单价 1.23，先做采购草稿让我看看。",
            List.of(),
            null
        );

        assertTrue(plan.isPresent());
        assertEquals(
            List.of("supplier_directory_lookup", "product_catalog_lookup"),
            plan.get().tools()
        );
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), eq("auto"));
    }

    @Test
    void purchaseDependencyFactsExposeOnlyTheCreateTargetOnContinuation() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-purchase-create",
                    "create_purchase_order",
                    objectMapper.createObjectNode()
                        .put("supplier_id", 1L)
                        .put("supplier_name", "测试供应商A")
                        .putArray("items")
                        .addObject()
                        .put("product_id", 1L)
                        .put("product_name", "测试商品A")
                        .put("quantity", 1)
                        .put("price", 1.23)
                )),
                null
            )));

        ToolPlanner planner = new ToolPlanner(client, new ToolRegistry(List.of(
            new TestTool("supplier_directory_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_purchase_order", AgentTool.ToolType.CREATE_ONLY)
        )), objectMapper);
        ObjectNode supplierFacts = objectMapper.createObjectNode();
        supplierFacts.putObject("query_audit").put("returned_count", 1);
        ObjectNode productFacts = objectMapper.createObjectNode();
        productFacts.putObject("query_audit").put("returned_count", 1);

        Optional<AgentToolPlan> next = planner.planNextIteration(
            "向供应商“测试供应商A”买商品“测试商品A”，数量 1、单价 1.23，先做采购草稿让我看看。",
            new AgentToolPlan(
                List.of("supplier_directory_lookup", "product_catalog_lookup"),
                "查询供应商和商品",
                "native_tool_use",
                Map.of()
            ),
            List.of(
                new ToolExecutionResult("supplier_directory_lookup", "供应商 1 条", supplierFacts, false),
                new ToolExecutionResult("product_catalog_lookup", "商品 1 条", productFacts, false)
            ),
            List.of(),
            2
        );

        assertTrue(next.isPresent());
        assertEquals(List.of("create_purchase_order"), next.get().tools());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("create_purchase_order"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void invalidWriteCallGetsOneTargetOnlyAutoRetryAfterDependencyFacts() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-purchase-retry",
                    "create_purchase_order",
                    objectMapper.createObjectNode()
                        .put("supplier_id", 1L)
                        .put("supplier_name", "测试供应商A")
                        .putArray("items")
                        .addObject()
                        .put("product_id", 1L)
                        .put("product_name", "测试商品A")
                        .put("quantity", 2)
                        .put("price", 10)
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("supplier_directory_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("product_catalog_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_purchase_order", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 1);
        Optional<AgentToolPlan> next = planner.planNextIteration(
            "向现有供应商买点货，先做采购单给我确认。",
            new AgentToolPlan(List.of("create_purchase_order"), "先生成采购单", "native_tool_use", Map.of()),
            List.of(new ToolExecutionResult("product_catalog_lookup", "商品 1 条", facts, false)),
            List.of(new com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolFailureResult(
                "create_purchase_order", "必填参数缺失，已等待真实查询结果后重试"
            )),
            2
        );

        assertTrue(next.isPresent());
        assertEquals(List.of("create_purchase_order"), next.get().tools());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        verify(client).createMessageWithTools(anyString(), anyString(), definitions.capture());
        assertEquals(
            Set.of("create_purchase_order"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void supplierFactsKeepPaymentDraftOnModelAutoChoiceAndExposeResolvedParameters() {
        LongCatAnthropicClient client = mock(LongCatAnthropicClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createMessageWithTools(anyString(), anyString(), anyList()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-pay-order",
                    "create_pay_order",
                    objectMapper.createObjectNode()
                        .put("supplier_id", 31L)
                        .put("supplier_name", "测试供应商A")
                        .put("amount", 1.23D)
                        .put("remark", "全量工具测试")
                )),
                null
            )));

        ToolRegistry registry = new ToolRegistry(List.of(
            new TestTool("supplier_directory_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("purchase_order_lookup", AgentTool.ToolType.READ_ONLY),
            new TestTool("create_pay_order", AgentTool.ToolType.CREATE_ONLY)
        ));
        ToolPlanner planner = new ToolPlanner(client, registry, objectMapper);

        ObjectNode supplierFacts = objectMapper.createObjectNode();
        supplierFacts.putObject("query_audit").put("returned_count", 1);
        supplierFacts.putArray("suppliers").addObject()
            .put("supplier_id", 31L)
            .put("name", "测试供应商A")
            .put("phone", "13700030001");

        AgentToolPlan previousPlan = new AgentToolPlan(
            List.of("supplier_directory_lookup"),
            "先查供应商",
            "native_tool_use",
            Map.of(),
            null,
            List.of(new NativeToolCallBlock("call-supplier", "supplier_directory_lookup", "{}"))
        );
        Optional<AgentToolPlan> next = planner.planNextIteration(
            "给供应商记一笔 1.23 元付款，备注全量工具测试，先别直接付款，做成草稿。",
            previousPlan,
            List.of(new ToolExecutionResult(
                "supplier_directory_lookup",
                "真实供应商 1 个",
                supplierFacts,
                false,
                "call-supplier",
                1
            )),
            List.of(),
            2
        );

        assertTrue(next.isPresent());
        assertEquals(List.of("create_pay_order"), next.get().tools());
        ArgumentCaptor<List> definitions = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(client).createMessageWithTools(systemPrompt.capture(), userPrompt.capture(), definitions.capture());
        assertEquals(
            Set.of("create_pay_order"),
            definitions.getValue().stream()
                .map(value -> ((LongCatAnthropicClient.ToolDefinition) value).name())
                .collect(java.util.stream.Collectors.toSet())
        );
        assertTrue(userPrompt.getValue().contains("supplier_name 和 amount"));
        assertTrue(userPrompt.getValue().contains("测试供应商A"));
        assertTrue(userPrompt.getValue().contains("supplier_id"));
        verify(client, never()).createMessageWithTools(anyString(), anyString(), anyList(), eq("create_pay_order"));
    }

    private record TestTool(String name, ToolType type) implements AgentTool {
        @Override
        public String displayName() {
            return name;
        }

        @Override
        public String description() {
            return name;
        }

        @Override
        public JsonNode parameterSchema() {
            ObjectNode schema = new ObjectMapper().createObjectNode();
            schema.put("type", "object");
            schema.putObject("properties");
            return schema;
        }

        @Override
        public ToolResult execute(ToolContext ctx, JsonNode params) {
            return ToolResult.empty(name);
        }
    }
}
