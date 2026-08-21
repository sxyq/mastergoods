package com.zhihuiji.backend.application.service.v2.agent.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentPromptCatalogTest {

    @org.junit.jupiter.api.Test
    void resolvesNaturalWriteTargetsForDraftFollowUp() {
        org.junit.jupiter.api.Assertions.assertEquals(
            "create_purchase_order",
            AgentPromptCatalog.targetWriteTool("向现有供应商买一个真实商品，先做采购草稿")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "create_purchase_receipt",
            AgentPromptCatalog.targetWriteTool("把采购单里的货做入库草稿")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "create_sales_return",
            AgentPromptCatalog.targetWriteTool("把一张销售单退 1 件")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "create_account_transfer",
            AgentPromptCatalog.targetWriteTool("把第一个账户的钱转到第二个账户，先做草稿")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "create_finance_record",
            AgentPromptCatalog.targetWriteTool("记一笔收入 1.23 元，分类写测试，先做成草稿")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "create_inventory_adjustment",
            AgentPromptCatalog.targetWriteTool("把第一个商品库存加 1 件，先做调整草稿")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "create_product",
            AgentPromptCatalog.targetWriteTool("帮我加个商品，先生成草稿")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "supplier_statement_lookup",
            AgentPromptCatalog.targetReadTool("帮我和供应商对一下账，余额、采购和退货都算进去")
        );
        org.junit.jupiter.api.Assertions.assertNull(
            AgentPromptCatalog.targetReadTool("我还欠哪些供应商钱？")
        );
        org.junit.jupiter.api.Assertions.assertNull(
            AgentPromptCatalog.targetWriteTool("最近库存都调整过什么？")
        );
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void catalogUsesNaturalUserLanguageAlongsideStableToolName() {
        AgentTool tool = new TestTool(
            "account_balance_lookup",
            "账户余额查询",
            "查询当前账号资金账户、余额与账户类型",
            AgentTool.ToolType.READ_ONLY,
            objectMapper.createObjectNode()
        );

        String catalog = AgentPromptCatalog.buildCatalog(List.of(tool), true);

        assertTrue(catalog.contains("account_balance_lookup"));
        assertTrue(catalog.contains("我现在有几个账户，里面还剩多少钱"));
        String prompt = AgentPromptCatalog.initialSystemPrompt(catalog);
        assertTrue(prompt.contains("默认只选一个最直接的工具"));
        assertTrue(prompt.contains("单主题首轮只返回一个最相关的原生工具调用"));
        assertTrue(prompt.contains("已有足够事实时停止调用"));
    }

    @Test
    void overlappingToolsDeclareTheirBoundariesToTheModel() {
        AgentTool tool = new TestTool(
            "account_health_lookup",
            "账户健康",
            "汇总资金账户状态",
            AgentTool.ToolType.READ_ONLY,
            objectMapper.createObjectNode()
        );

        String description = AgentPromptCatalog.modelDescription(tool);

        assertTrue(description.contains("账户健康问题优先只调用它"));
        assertTrue(description.contains("不要再补查账户余额"));
    }

    @Test
    void financeWriteGuidanceDoesNotTurnOptionalAccountIntoMandatoryLookup() {
        String guidance = AgentPromptCatalog.writeTargetGuidance(
            "记一笔收入 1.23 元，分类写全量工具测试，先做成草稿让我确认。"
        );

        assertTrue(guidance.contains("create_finance_record"));
        assertTrue(guidance.contains("account_id 是可选"));
        assertTrue(guidance.contains("不要先为了补账户 ID"));
    }

    @Test
    void customerDraftDoesNotLookupOptionalGroupWhenNameAndPhoneAreProvided() {
        String guidance = AgentPromptCatalog.writeTargetGuidance(
            "帮我加一个客户，名字叫全量工具测试客户，电话 13900000001，先把要保存的内容给我确认。"
        );

        assertTrue(guidance.contains("create_customer"));
        assertTrue(guidance.contains("name、phone 已足够"));
        assertTrue(guidance.contains("group_id 是可选字段"));
        assertTrue(guidance.contains("只有用户明确指定客户分组"));
    }

    @Test
    void writeIntentRecognizesOrdinaryUserWording() {
        assertTrue(AgentPromptCatalog.hasWriteIntent("帮我加一个商品，先别直接保存"));
        assertTrue(AgentPromptCatalog.hasWriteIntent("帮我新建客户李四，先生成草稿"));
        assertTrue(AgentPromptCatalog.hasWriteIntent("帮张三开一张销售单，先确认"));
        assertTrue(AgentPromptCatalog.hasWriteIntent("给供应商记一笔付款"));
        assertTrue(AgentPromptCatalog.hasWriteIntent("把一张销售单退 1 件，先做草稿"));
        assertTrue(AgentPromptCatalog.hasWriteIntent("帮我下单买一个商品，先做草稿"));
        assertFalse(AgentPromptCatalog.hasWriteIntent("最近入了哪些采购货？入库明细和状态给我看看。"));
        assertFalse(AgentPromptCatalog.hasWriteIntent("最近有哪些销售退货？退货明细和状态给我看看。"));
        assertFalse(AgentPromptCatalog.hasWriteIntent("最近采购了什么，货到了没有？"));
        assertFalse(AgentPromptCatalog.hasWriteIntent("最近收款和付款记录帮我理一下。"));
        assertFalse(AgentPromptCatalog.hasWriteIntent("最近销售和库存怎么样"));
        assertFalse(AgentPromptCatalog.hasWriteIntent("把销售单、收款和退货的关联记录串起来给我看。"));
        assertFalse(AgentPromptCatalog.hasWriteIntent("帮我看看客户整体情况，余额、下单、收款和退货都说说。"));
        assertNull(AgentPromptCatalog.targetWriteTool("把销售单、收款和退货的关联记录串起来给我看。"));
        assertNull(AgentPromptCatalog.targetWriteTool("最近入了哪些采购货？入库明细和状态给我看看。"));
        assertNull(AgentPromptCatalog.targetWriteTool("最近有哪些销售退货？退货明细和状态给我看看。"));
        assertEquals("create_customer", AgentPromptCatalog.targetWriteTool("帮我新建客户李四，先生成草稿"));
        assertEquals("create_sale_order", AgentPromptCatalog.targetWriteTool("帮张三开一张销售单，先确认"));
    }

    @Test
    void visualizationRequiresExplicitUserRequest() {
        assertTrue(AgentPromptCatalog.requestsVisualization("最近销售用表格列出来"));
        assertTrue(AgentPromptCatalog.requestsVisualization("库存和补货用合适的方式展示"));
        assertFalse(AgentPromptCatalog.requestsVisualization("最近销售和库存怎么样"));
        assertFalse(AgentPromptCatalog.requestsVisualization("帮我做个经营汇总"));
    }

    @Test
    void multipleSourceContinuationRequiresExplicitCombinedRequest() {
        assertTrue(AgentPromptCatalog.requestsMultipleSources("最近一周销售和现金流放一起看下"));
        assertTrue(AgentPromptCatalog.requestsMultipleSources("客户欠款和供应商应付款都算进去"));
        assertFalse(AgentPromptCatalog.requestsMultipleSources("最近现金流怎么样"));
        assertTrue(AgentPromptCatalog.initialSystemPrompt("- report_query：经营汇总")
            .contains("当前业务日期是"));
    }

    @Test
    void explicitVisualizationRequestRequiresVisualizationAfterRealFacts() {
        String prompt = AgentPromptCatalog.reactSystemPrompt(
            "- result_visualization：决定展示方式",
            false,
            2,
            "最近一周销售和回款给我画一张趋势图。"
        );

        assertTrue(prompt.contains("必须调用 result_visualization"));
        assertTrue(prompt.contains("不要在没有真实查询结果时调用"));
    }

    @Test
    void inventoryAndRestockRequestRequiresBothBusinessSourcesBeforeVisualization() {
        String prompt = AgentPromptCatalog.initialSystemPrompt(
            "- inventory_panorama_lookup：库存现状\n"
                + "- smart_restock_lookup：补货建议\n"
                + "- result_visualization：决定展示方式",
            "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。"
        );

        assertTrue(prompt.contains("必须分别调用 inventory_panorama_lookup 和 smart_restock_lookup"));
        assertTrue(prompt.contains("再由你自主判断是否需要调用 result_visualization"));
    }

    private record TestTool(
        String name,
        String displayName,
        String description,
        ToolType type,
        ObjectNode schema
    ) implements AgentTool {
        @Override
        public com.fasterxml.jackson.databind.JsonNode parameterSchema() {
            return schema;
        }

        @Override
        public com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult execute(
            com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext ctx,
            com.fasterxml.jackson.databind.JsonNode params
        ) {
            return com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult.empty("test");
        }
    }
}
