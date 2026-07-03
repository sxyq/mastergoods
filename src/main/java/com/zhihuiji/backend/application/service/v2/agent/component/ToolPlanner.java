package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentToolPlan;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Agent 工具规划组件。
 *
 * <p>负责根据用户问题、历史对话与会话摘要，规划本轮需要调用的工具集合与参数。
 * 规划路径优先级：
 * <ol>
 *   <li>原生 Function Calling（Anthropic Messages API 的 tool_use block）</li>
 *   <li>JSON 字符串解析路径（兼容 Chat Completions / Responses API）</li>
 *   <li>关键词兜底推断（{@link #inferToolPlan}）：包括创建意图推断与只读查询关键词匹配</li>
 * </ol>
 *
 * <p>原属 {@code V2AgentAiService} 内联逻辑，因 God Class 拆分提取为独立组件。
 * 依赖 {@link LongCatAnthropicClient}、{@link ToolRegistry}、{@link ObjectMapper} 三个组件。
 *
 * <p>纯函数 {@link #formatHistoryContext} 与 {@link #containsAny} 已改为 public static，
 * 供 {@code AnswerSynthesizer} 等其他组件复用。
 */
@Component
public class ToolPlanner {
    private static final int MAX_AGENT_ITERATIONS = 3;

    /** 内置只读工具白名单，用于注册表为空时的兜底鉴权。 */
    private static final Set<String> ALLOWED_READONLY_TOOLS = Set.of(
        "inventory_low_stock_lookup",
        "product_catalog_lookup",
        "customer_receivable_lookup",
        "supplier_payable_lookup",
        "purchase_order_lookup",
        "sale_order_lookup",
        "pay_order_lookup",
        "finance_record_lookup",
        "sales_overview_lookup",
        "sales_return_lookup",
        "purchase_receipt_lookup",
        "purchase_return_lookup",
        "inventory_ledger_lookup",
        "inventory_snapshot_lookup",
        "payment_lookup",
        "account_balance_lookup",
        "account_transfer_lookup",
        "cash_change_lookup",
        "product_category_lookup",
        "partner_group_lookup",
        "partner_contact_lookup",
        "sales_trend_lookup",
        "cashflow_summary_lookup",
        "inventory_adjustment_lookup",
        "report_query",
        "sales_full_chain_lookup",
        "purchase_tracking_lookup",
        "inventory_panorama_lookup",
        "customer_profile_lookup",
        "account_health_lookup",
        "receivable_payable_lookup",
        "supplier_statement_lookup",
        "product_supplier_relation_lookup",
        "product_price_level_lookup",
        "import_job_lookup",
        "sync_status_lookup",
        "smart_restock_lookup",
        "cross_analysis_lookup",
        "anomaly_alert_lookup",
        "data_export_tool",
        "store_info_lookup"
    );

    private final LongCatAnthropicClient longCatAnthropicClient;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ToolPlanner(
        LongCatAnthropicClient longCatAnthropicClient,
        ToolRegistry toolRegistry,
        ObjectMapper objectMapper
    ) {
        this.longCatAnthropicClient = longCatAnthropicClient;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public AgentToolPlan planTools(String message, List<AgentMessageEntity> history, String conversationSummary) {
        return planToolsWithLlm(message, history, conversationSummary)
            .orElseGet(() -> inferToolPlan(message, history, conversationSummary));
    }

    public Optional<AgentToolPlan> planToolsWithLlm(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (!longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        // 优先尝试原生 Function Calling（Anthropic Messages API 的 tool_use block）
        // 不支持或模型未返回 tool_use 时降级到 JSON 字符串解析路径
        Optional<AgentToolPlan> nativePlan = planToolsWithNativeFunctionCalling(message, history, conversationSummary);
        if (nativePlan.isPresent()) {
            return nativePlan;
        }
        AgentToolPlan hintedPlan = inferToolPlan(message, history, conversationSummary);
        if (!hintedPlan.tools().isEmpty()) {
            Optional<AgentToolPlan> narrowedNativePlan = planToolsWithNativeFunctionCalling(
                message,
                history,
                conversationSummary,
                hintedPlan.tools()
            );
            if (narrowedNativePlan.isPresent()) {
                return narrowedNativePlan;
            }
        }
        // 降级路径：prompt + JSON 解析（兼容 Chat Completions / Responses API 及不支持 tool_use 的模型）
        // 工具清单优先从注册表动态生成；注册表为空时降级为旧硬编码白名单（渐进式迁移兼容）
        String toolCatalog = toolRegistry.buildToolCatalogForLlm();
        String systemPrompt;
        if (toolCatalog.isBlank()) {
            systemPrompt = """
                你是智慧记的工具规划器。你只能选择白名单中的只读数据查询工具，不允许生成 SQL，不允许访问其他账号数据，不允许执行写操作。
                可选工具：
                - inventory_low_stock_lookup：查询当前账号低库存、补货、缺货相关数据
                - customer_receivable_lookup：查询当前账号客户欠款、应收、回款优先级
                - sales_overview_lookup：查询当前账号近7天销售、回款、经营概览
                - product_catalog_lookup：查询当前账号商品、库存、价格、类别相关数据
                - supplier_payable_lookup：查询当前账号供应商欠款、应付与采购相关数据
                - sale_order_lookup：查询当前账号销售单、客户订单与收款情况
                - purchase_order_lookup：查询当前账号采购单、供应商采购与到货情况
                - pay_order_lookup：查询当前账号付款单、付款状态与金额
                - finance_record_lookup：查询当前账号收入支出流水、分类与近期开支
                只输出 JSON，不要输出 Markdown。
                """;
        } else {
            systemPrompt = "你是智慧记的工具规划器。你可以选择以下只读查询工具和创建类工具。\n"
                + "只读工具直接返回查询结果；创建类工具会生成草稿，需用户确认后才执行写入，不会直接修改数据。\n"
                + "不允许生成 SQL，不允许访问其他账号数据。\n"
                + "可选工具：\n"
                + toolCatalog
                + "只输出 JSON，不要输出 Markdown。\n";
        }
        String historyContext = formatHistoryContext(history);
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "会话摘要：" + conversationSummary + "\n"
            : "";
        String userPrompt = historyContext
            + summaryContext
            + "用户问题：" + message + "\n"
            + "请输出形如 {\"tools\":[{\"name\":\"sale_order_lookup\",\"params\":{\"keyword\":\"张三\"}}],\"rationale\":\"...\"} 的 JSON。"
            + "tools 最多 3 个，必须来自可选工具。params 为该工具的查询参数，根据用户问题提取，无参数时省略 params 字段。";
        return longCatAnthropicClient.createJsonMessage(systemPrompt, userPrompt).flatMap(this::parseToolPlan);
    }

    /**
     * 原生 Function Calling 规划路径。
     *
     * <p>从 {@link ToolRegistry} 构建原生 {@code ToolDefinition} 列表，调用
     * {@link LongCatAnthropicClient#createMessageWithTools}；模型返回 {@code tool_use}
     * block 时直接转为 {@link AgentToolPlan}，无需正则提取 JSON。
     *
     * <p>仅 Anthropic Messages API 支持此路径；其他 wireApi 或模型未返回 tool_use 时返回 empty，
     * 由 {@link #planToolsWithLlm} 降级到 JSON 字符串解析。
     *
     * @param message 用户问题
     * @return 工具规划 Optional；无 tool_use 返回时为 empty
     */
    public Optional<AgentToolPlan> planToolsWithNativeFunctionCalling(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = buildNativeToolDefinitions();
        if (nativeTools.isEmpty()) {
            return Optional.empty();
        }
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "\n当前会话摘要：" + conversationSummary
            : "";
        String systemPrompt = "你是智慧记的工具规划器。根据用户问题选择最相关的只读查询工具或创建类工具。\n"
            + "只读工具直接返回查询结果；创建类工具生成草稿，需用户确认后才执行写入。\n"
            + "不允许生成 SQL，不允许访问其他账号数据。最多选择 3 个工具。"
            + summaryContext;
        String planningMessage = formatHistoryContext(history) + "用户问题：" + message;
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, planningMessage, nativeTools);
        return toNativeToolPlan(response);
    }

    public Optional<AgentToolPlan> planToolsWithNativeFunctionCalling(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary,
        List<String> candidateToolNames
    ) {
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = buildNativeToolDefinitions(candidateToolNames);
        if (nativeTools.isEmpty()) {
            return Optional.empty();
        }
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "\n当前会话摘要：" + conversationSummary
            : "";
        String systemPrompt = "你是智慧记的工具规划器。根据用户问题从给定候选工具中选择最相关的工具。\n"
            + "不要输出自然语言解释，优先直接返回原生工具调用。最多选择 3 个工具。"
            + summaryContext;
        String planningMessage = formatHistoryContext(history) + "用户问题：" + message;
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, planningMessage, nativeTools);
        return toNativeToolPlan(response);
    }

    private Optional<AgentToolPlan> toNativeToolPlan(Optional<LongCatAnthropicClient.ToolUseResponse> response) {
        if (response.isEmpty() || !response.get().hasToolUses()) {
            return Optional.empty();
        }
        List<String> tools = new ArrayList<>();
        Map<String, JsonNode> toolParams = new LinkedHashMap<>();
        for (LongCatAnthropicClient.ToolUseBlock toolUse : response.get().toolUses()) {
            String toolName = toolUse.name();
            if (!isAllowedTool(toolName) || tools.size() >= 3) {
                continue;
            }
            if (tools.contains(toolName)) {
                continue;
            }
            tools.add(toolName);
            JsonNode input = toolUse.input();
            if (input != null && input.isObject() && input.size() > 0) {
                toolParams.put(toolName, input);
            }
        }
        if (tools.isEmpty()) {
            return Optional.empty();
        }
        String rationale = response.get().text() != null && !response.get().text().isBlank()
            ? response.get().text()
            : "模型通过原生 Function Calling 选择工具";
        return Optional.of(new AgentToolPlan(tools, rationale, "native_tool_use", toolParams));
    }

    /**
     * 从 {@link ToolRegistry} 构建原生 Function Calling 的工具定义列表。
     *
     * <p>合并只读工具与创建类工具，将每个工具的 {@link AgentTool#parameterSchema()}（JsonNode）
     * 转为 {@code Map<String, Object>} 作为 {@code input_schema}。
     *
     * @return 工具定义列表；注册表为空时返回空列表
     */
    public List<LongCatAnthropicClient.ToolDefinition> buildNativeToolDefinitions() {
        List<AgentTool> allTools = new ArrayList<>();
        allTools.addAll(toolRegistry.listReadOnlyTools());
        allTools.addAll(toolRegistry.listCreateTools());
        if (allTools.isEmpty()) {
            return List.of();
        }
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = new ArrayList<>(allTools.size());
        for (AgentTool tool : allTools) {
            Map<String, Object> inputSchema = convertSchemaToMap(tool.parameterSchema());
            nativeTools.add(new LongCatAnthropicClient.ToolDefinition(
                tool.name(),
                tool.description(),
                inputSchema
            ));
        }
        return nativeTools;
    }

    public List<LongCatAnthropicClient.ToolDefinition> buildNativeToolDefinitions(List<String> candidateToolNames) {
        if (candidateToolNames == null || candidateToolNames.isEmpty()) {
            return buildNativeToolDefinitions();
        }
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = new ArrayList<>();
        for (String toolName : candidateToolNames) {
            toolRegistry.getTool(toolName).ifPresent(tool -> nativeTools.add(new LongCatAnthropicClient.ToolDefinition(
                tool.name(),
                tool.description(),
                convertSchemaToMap(tool.parameterSchema())
            )));
        }
        return nativeTools;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> convertSchemaToMap(JsonNode schema) {
        if (schema == null || schema.isNull() || !schema.isObject()) {
            return Map.of("type", "object", "properties", Map.of());
        }
        try {
            return objectMapper.convertValue(schema, Map.class);
        } catch (Exception ignored) {
            return Map.of("type", "object", "properties", Map.of());
        }
    }

    public Optional<AgentToolPlan> parseToolPlan(String rawText) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawText));
            Set<String> tools = new LinkedHashSet<>();
            Map<String, JsonNode> toolParams = new LinkedHashMap<>();
            JsonNode toolsNode = root.get("tools");
            if (toolsNode != null && toolsNode.isArray()) {
                for (JsonNode item : toolsNode) {
                    String tool;
                    JsonNode params = null;
                    if (item.isObject()) {
                        tool = item.path("name").asText("");
                        params = item.get("params");
                    } else {
                        tool = item.asText("");
                    }
                    if (isAllowedTool(tool)) {
                        tools.add(tool);
                        if (params != null && params.isObject()) {
                            toolParams.put(tool, params);
                        }
                    }
                    if (tools.size() >= 3) {
                        break;
                    }
                }
            }
            if (tools.isEmpty()) {
                return Optional.empty();
            }
            String rationale = root.path("rationale").asText("模型选择了当前问题所需的只读查询工具");
            return Optional.of(new AgentToolPlan(new ArrayList<>(tools), rationale, toolParams));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public String extractJsonObject(String rawText) {
        if (rawText == null) {
            return "{}";
        }
        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return rawText.substring(start, end + 1);
        }
        return rawText;
    }

    public AgentToolPlan inferToolPlan(String message, List<AgentMessageEntity> history, String conversationSummary) {
        String normalized = message.toLowerCase(Locale.ROOT);
        Optional<AgentToolPlan> createPlan = inferCreateToolPlan(message, normalized, history, conversationSummary);
        if (createPlan.isPresent()) {
            return createPlan.get();
        }
        Set<String> tools = new LinkedHashSet<>();
        if (containsAny(normalized, "智能补货", "补货建议", "restock", "reorder", "replenishment")) {
            tools.add("smart_restock_lookup");
        }
        if (containsAny(normalized, "库存", "补货", "低库存", "缺货", "inventory", "stock", "low stock", "replenish")) {
            tools.add("inventory_low_stock_lookup");
        }
        if (containsAny(normalized, "库存全景", "库存健康", "库存周转", "panorama", "turnover")) {
            tools.add("inventory_panorama_lookup");
        }
        if (containsAny(normalized, "商品", "sku", "品类", "目录", "售价", "进价", "product", "catalog", "price")) {
            tools.add("product_catalog_lookup");
        }
        if (containsAny(normalized, "商品供应商", "供应商关联", "供货关系", "supplier relation", "vendor relation")) {
            tools.add("product_supplier_relation_lookup");
        }
        if (containsAny(normalized, "价格等级", "价目", "price level", "pricing tier")) {
            tools.add("product_price_level_lookup");
        }
        if (containsAny(normalized, "欠款", "应收", "客户", "回款", "receivable", "customer", "collection")) {
            tools.add("customer_receivable_lookup");
        }
        if (containsAny(normalized, "客户画像", "客户分析", "客户档案", "profile", "customer insights")) {
            tools.add("customer_profile_lookup");
        }
        if (containsAny(normalized, "供应商", "应付", "采购", "到货", "收货", "supplier", "payable", "purchase", "procurement")) {
            tools.add("supplier_payable_lookup");
            tools.add("purchase_order_lookup");
        }
        if (containsAny(normalized, "供应商对账", "对账单", "statement", "supplier statement")) {
            tools.add("supplier_statement_lookup");
        }
        if (containsAny(normalized, "采购跟踪", "采购进度", "入库退货", "tracking", "receipt flow")) {
            tools.add("purchase_tracking_lookup");
        }
        if (containsAny(normalized, "销售单", "订单", "成交", "收款", "付款情况", "sale order", "sales order", "order", "deal")) {
            tools.add("sale_order_lookup");
        }
        if (containsAny(normalized, "销售全链路", "销售链路", "退货收款", "full chain", "return flow")) {
            tools.add("sales_full_chain_lookup");
        }
        if (containsAny(normalized, "付款单", "已付款", "待付款", "payment", "paid", "unpaid")) {
            tools.add("pay_order_lookup");
        }
        if (containsAny(normalized, "账户健康", "收支比", "账户概览", "account health", "cash ratio")) {
            tools.add("account_health_lookup");
        }
        if (containsAny(normalized, "流水", "收入", "支出", "财务", "费用", "开支", "finance", "cashflow", "income", "expense")) {
            tools.add("finance_record_lookup");
        }
        if (containsAny(normalized, "交叉分析", "多维分析", "综合分析", "cross analysis", "multi dimension")) {
            tools.add("cross_analysis_lookup");
        }
        if (containsAny(normalized, "异常预警", "异常", "风险预警", "anomaly", "alert")) {
            tools.add("anomaly_alert_lookup");
        }
        if (containsAny(normalized, "应收应付", "往来对账", "对账汇总", "reconciliation", "receivable payable")) {
            tools.add("receivable_payable_lookup");
        }
        if (containsAny(normalized, "经营", "概览", "销售", "最近", "7天", "七天", "business", "overview", "sales", "recent", "7 days")) {
            tools.add("sales_overview_lookup");
        }
        if (containsAny(normalized, "导出", "导出数据", "下载csv", "下载json", "export")) {
            tools.add("data_export_tool");
        }
        if (containsAny(normalized, "门店", "店铺信息", "store info", "shop info")) {
            tools.add("store_info_lookup");
        }
        if (containsAny(normalized, "导入任务", "导入状态", "import job", "import status")) {
            tools.add("import_job_lookup");
        }
        if (containsAny(normalized, "同步状态", "同步任务", "sync status", "sync job")) {
            tools.add("sync_status_lookup");
        }
        List<String> deduplicated = new ArrayList<>();
        for (String tool : tools) {
            if (isAllowedTool(tool)) {
                deduplicated.add(tool);
            }
            if (deduplicated.size() >= 6) {
                break;
            }
        }
        Map<String, JsonNode> toolParams = inferKeywordFallbackToolParams(message, deduplicated, history, conversationSummary);
        return new AgentToolPlan(deduplicated, "根据问题关键词兜底选择已接入工具", "keyword_fallback", toolParams);
    }

    public Map<String, JsonNode> inferKeywordFallbackToolParams(
        String message,
        List<String> tools,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (tools == null || tools.isEmpty()) {
            return Map.of();
        }
        Map<String, JsonNode> toolParams = new LinkedHashMap<>();
        for (String tool : tools) {
            JsonNode params = switch (tool) {
                case "customer_receivable_lookup", "customer_profile_lookup" ->
                    buildCustomerKeywordParams(message, history, conversationSummary);
                case "inventory_panorama_lookup" -> buildProductKeywordParams(message, history, conversationSummary);
                case "purchase_tracking_lookup" -> buildPurchaseKeywordParams(message, history, conversationSummary);
                case "account_health_lookup" -> buildAccountKeywordParams(message, history, conversationSummary);
                default -> null;
            };
            if (params != null && !params.isEmpty()) {
                toolParams.put(tool, params);
            }
        }
        return toolParams;
    }

    public Optional<AgentToolPlan> inferCreateToolPlan(
        String message,
        String normalized,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (!looksLikeCreateIntent(normalized)) {
            return Optional.empty();
        }
        if (containsAny(normalized, "客户") && isAllowedTool("create_customer")) {
            JsonNode params = buildCreateCustomerParams(message, history, conversationSummary);
            if (hasTextParam(params, "name")) {
                return Optional.of(createOnlyPlan("create_customer", params, "根据自然语言兜底生成客户草稿"));
            }
        }
        if (containsAny(normalized, "供应商") && isAllowedTool("create_supplier")) {
            JsonNode params = buildCreateSupplierParams(message, history, conversationSummary);
            if (hasTextParam(params, "name")) {
                return Optional.of(createOnlyPlan("create_supplier", params, "根据自然语言兜底生成供应商草稿"));
            }
        }
        if (containsAny(normalized, "商品", "产品", "sku") && isAllowedTool("create_product")) {
            JsonNode params = buildCreateProductParams(message, history, conversationSummary);
            if (hasTextParam(params, "name") && hasTextParam(params, "code")) {
                return Optional.of(createOnlyPlan("create_product", params, "根据自然语言兜底生成商品草稿"));
            }
        }
        if (containsAny(normalized, "付款单", "付款") && isAllowedTool("create_pay_order")) {
            JsonNode params = buildCreatePayOrderParams(message, history, conversationSummary);
            if (hasTextParam(params, "supplier_name") && hasNumericParam(params, "amount")) {
                return Optional.of(createOnlyPlan("create_pay_order", params, "根据自然语言兜底生成付款单草稿"));
            }
        }
        if (containsAny(normalized, "流水", "记账", "记一笔", "收入", "支出", "报销", "开支", "费用", "finance", "expense", "income")
            && isAllowedTool("create_finance_record")) {
            JsonNode params = buildCreateFinanceRecordParams(message, normalized, history, conversationSummary);
            if (hasNumericParam(params, "amount") && hasNumericParam(params, "type")) {
                return Optional.of(createOnlyPlan("create_finance_record", params, "根据自然语言兜底生成资金流水草稿"));
            }
        }
        if (containsAny(normalized, "采购单", "采购订单") && isAllowedTool("create_purchase_order")) {
            JsonNode params = buildCreatePurchaseOrderParams(message, history, conversationSummary);
            if (hasTextParam(params, "supplier_name")) {
                return Optional.of(createOnlyPlan("create_purchase_order", params, "根据自然语言兜底生成采购单草稿"));
            }
        }
        if (containsAny(normalized, "销售单", "销售订单", "开单", "下单") && isAllowedTool("create_sale_order")) {
            JsonNode params = buildCreateSaleOrderParams(message, history, conversationSummary);
            if (hasTextParam(params, "customer_name")) {
                return Optional.of(createOnlyPlan("create_sale_order", params, "根据自然语言兜底生成销售单草稿"));
            }
        }
        return Optional.empty();
    }

    public Optional<AgentToolPlan> planNextIteration(String message, List<ToolExecutionResult> toolResults, int iteration) {
        if (iteration > MAX_AGENT_ITERATIONS || !longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        String toolCatalog = toolRegistry.buildToolCatalogForLlm();
        if (toolCatalog.isBlank()) {
            return Optional.empty();
        }
        StringBuilder contextBuilder = new StringBuilder();
        for (ToolExecutionResult result : toolResults) {
            contextBuilder.append("- ").append(result.toolName()).append("：").append(result.summary()).append('\n');
        }
        String executedContext = contextBuilder.length() > 0
            ? "已执行工具及结果摘要：\n" + contextBuilder
            : "上一轮工具未返回有效结果。\n";
        String systemPrompt = "你是智慧记的工具规划器。根据用户问题与已查询结果，判断是否需要补充查询其他工具。\n"
            + "可选工具：\n"
            + toolCatalog
            + "若已收集到足够信息回答用户问题，输出 {\"tools\":[]}。\n"
            + "若需要补充查询，输出 {\"tools\":[{\"name\":\"...\",\"params\":{...}}],\"rationale\":\"...\"}，最多 2 个补充工具。\n"
            + "只输出 JSON，不要输出 Markdown。";
        String userPrompt = "用户问题：" + message + "\n"
            + executedContext
            + "请判断是否需要补充查询。";
        return longCatAnthropicClient.createJsonMessage(systemPrompt, userPrompt).flatMap(this::parseToolPlan);
    }

    public boolean isAllowedTool(String tool) {
        Optional<AgentTool> registered = toolRegistry.getTool(tool);
        if (registered.isPresent()) {
            return registered.get().type() == AgentTool.ToolType.READ_ONLY
                || registered.get().type() == AgentTool.ToolType.CREATE_ONLY;
        }
        // 注册表未命中时回退到内置只读工具白名单（兼容 ToolRegistry 为空的测试场景；
        // 生产环境 ToolRegistry 自动扫描 @Component 工具，不会走到此处）
        return ALLOWED_READONLY_TOOLS.contains(tool);
    }

    /**
     * 格式化历史对话上下文，供 LLM 规划器与 AnswerSynthesizer 复用。
     *
     * @param history 历史消息列表（按时间正序）
     * @return 格式化后的历史对话文本；无历史时返回空串
     */
    public static String formatHistoryContext(List<AgentMessageEntity> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("历史对话：\n");
        for (AgentMessageEntity msg : history) {
            String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "AI" : "用户";
            String content = msg.getContent();
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            sb.append(role).append("：").append(content).append('\n');
        }
        return sb.toString() + "\n";
    }

    /**
     * 关键词匹配纯函数：text 是否包含任意一个 keyword。
     *
     * @param text     待匹配文本
     * @param keywords 候选关键词
     * @return 命中任意一个返回 true
     */
    public static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private AgentToolPlan createOnlyPlan(String toolName, JsonNode params, String rationale) {
        return new AgentToolPlan(
            List.of(toolName),
            rationale,
            "keyword_fallback",
            Map.of(toolName, params)
        );
    }

    private JsonNode buildCreateCustomerParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "name", firstNonBlank(
            extractNamedValue(message, "(?:客户名称|客户名|姓名|名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)客户\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "客户\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            findRecentEntityHint(history, conversationSummary, "customer")
        ));
        putText(params, "phone", extractNamedValue(message, "(1\\d{10})"));
        putText(params, "remark", extractRemark(message));
        return params;
    }

    private JsonNode buildCreateSupplierParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "name", firstNonBlank(
            extractNamedValue(message, "(?:供应商名称|供应商名|名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)供应商\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "供应商\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putText(params, "phone", extractNamedValue(message, "(1\\d{10})"));
        putText(params, "remark", extractRemark(message));
        return params;
    }

    private JsonNode buildCreateProductParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "code", firstNonBlank(
            extractNamedValue(message, "(?:编码|商品编码|code|CODE|sku|SKU)\\s*[:：]?\\s*([A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)商品\\s*([A-Za-z0-9_-]{2,32})\\s+([\\p{IsHan}A-Za-z0-9_-]{2,32})", 1)
        ));
        putText(params, "name", firstNonBlank(
            extractNamedValue(message, "(?:商品名称|产品名称|名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)商品\\s*[A-Za-z0-9_-]{2,32}\\s+([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:商品|产品)\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            findRecentEntityHint(history, conversationSummary, "product")
        ));
        putDouble(params, "price", extractDecimal(message, "(?:售价|销售价|单价|价格)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"));
        putDouble(params, "cost", extractDecimal(message, "(?:成本|成本价|进价|采购价)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"));
        putDouble(params, "stock", extractDecimal(message, "(?:库存|期初库存|数量)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"));
        return params;
    }

    private JsonNode buildAccountKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        String keyword = firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:账户健康|账户概览|收支比)"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:账户健康|账户概览|收支比)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:账户健康|账户概览|收支比)"),
            extractNamedValue(message, "(?:账户|资金账户)\\s*[:：]\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "account")
        );
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            boolean genericOnly = normalizedKeyword.equals("账户")
                || normalizedKeyword.equals("资金账户")
                || normalizedKeyword.equals("账户健康")
                || normalizedKeyword.equals("账户概览")
                || normalizedKeyword.equals("收支比");
            if (!genericOnly
                && !containsAny(normalizedKeyword, "健康", "收支比", "概览")) {
                putText(params, "keyword", keyword);
            }
        }
        Integer windowDays = extractInteger(message, "(\\d{1,3})\\s*(?:天|日)");
        if (windowDays != null) {
            params.put("window_days", Math.max(1, Math.min(180, windowDays)));
        }
        return params;
    }

    private JsonNode buildCreateSaleOrderParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "customer_name", firstNonBlank(
            extractNamedValue(message, "(?:客户名称|客户名)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "帮\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})\\s*(?:开|下|做).{0,8}(?:销售单|销售订单|订单)"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})\\s*(?:开|下|做).{0,8}(?:销售单|销售订单|订单)"),
            extractNamedValue(message, "为\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})\\s*(?:开|下|做).{0,8}(?:销售单|销售订单|订单)"),
            findRecentEntityHint(history, conversationSummary, "customer")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCreatePurchaseOrderParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "supplier_name", firstNonBlank(
            extractNamedValue(message, "(?:供应商名称|供应商名)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "向\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:下|开|做).{0,8}(?:采购单|采购订单)"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:下|开|做).{0,8}(?:采购单|采购订单)"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCreatePayOrderParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "supplier_name", firstNonBlank(
            extractNamedValue(message, "(?:供应商名称|供应商名)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:付|付款|打款)"),
            extractNamedValue(message, "向\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:付|付款|打款)"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putDouble(params, "amount", firstNonNull(
            extractDecimal(message, "(?:金额|付款金额|支付金额)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"),
            extractDecimal(message, "([0-9]+(?:\\.[0-9]+)?)\\s*(?:元|块)")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCreateFinanceRecordParams(
        String message,
        String normalized,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        var params = objectMapper.createObjectNode();
        Integer type = inferFinanceRecordType(normalized);
        if (type != null) {
            params.put("type", type);
        }
        putDouble(params, "amount", firstNonNull(
            extractDecimal(message, "(?:金额|支出|收入|报销|费用|开支)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"),
            extractDecimal(message, "([0-9]+(?:\\.[0-9]+)?)\\s*(?:元|块)")
        ));
        putText(params, "category", firstNonBlank(
            extractNamedValue(message, "(?:分类|类目)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,32})"),
            extractNamedValue(message, "记一笔\\s*([\\p{IsHan}]{2,16})(?:收入|支出|费用|开支|报销)"),
            extractNamedValue(message, "([\\p{IsHan}]{2,16})(?:收入|支出|费用|开支|报销)")
        ));
        putText(params, "partner_name", firstNonBlank(
            extractNamedValue(message, "(?:往来方|对方|对象)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:报销|付款|打款|转账)"),
            extractNamedValue(message, "向\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:收款|收费|付款|打款|转账)"),
            findRecentEntityHint(history, conversationSummary, "customer"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCustomerKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        String keyword = firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:客户画像|客户分析|客户档案)"),
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的催收建议"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:客户画像|客户分析|客户档案|催收建议|应收|欠款)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:客户画像|客户分析|客户档案|催收建议|应收|欠款)"),
            extractNamedValue(message, "(?:客户画像|客户分析|客户档案)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:客户|客户名|客户名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "customer")
        );
        if (StringUtils.hasText(keyword) && !isInvalidContextCarryKeyword(keyword) && !isGenericCustomerKeyword(keyword)) {
            putText(params, "keyword", keyword);
        } else {
            String contextKeyword = findRecentEntityHint(history, conversationSummary, "customer");
            if (StringUtils.hasText(contextKeyword) && !isGenericCustomerKeyword(contextKeyword)) {
                putText(params, "keyword", contextKeyword);
            }
        }
        return params;
    }

    private boolean isGenericCustomerKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (containsAny(normalizedKeyword, "供应商", "应付", "采购")) {
            return true;
        }
        if (normalizedKeyword.endsWith("情况")) {
            String base = normalizedKeyword.substring(0, normalizedKeyword.length() - 2);
            if (base.equals("客户应收")
                || base.equals("客户欠款")
                || base.equals("客户回款")
                || base.equals("应收")
                || base.equals("欠款")
                || base.equals("回款")
                || base.equals("客户画像")
                || base.equals("客户分析")
                || base.equals("客户档案")
                || base.equals("催收建议")) {
                return true;
            }
        }
        return normalizedKeyword.equals("客户")
            || normalizedKeyword.equals("客户名")
            || normalizedKeyword.equals("客户名称")
            || normalizedKeyword.equals("客户画像")
            || normalizedKeyword.equals("客户分析")
            || normalizedKeyword.equals("客户档案")
            || normalizedKeyword.equals("催收建议")
            || normalizedKeyword.equals("应收")
            || normalizedKeyword.equals("欠款")
            || normalizedKeyword.equals("回款")
            || normalizedKeyword.equals("客户应收")
            || normalizedKeyword.equals("客户欠款")
            || normalizedKeyword.equals("客户回款")
            || normalizedKeyword.equals("应收情况")
            || normalizedKeyword.equals("欠款情况")
            || normalizedKeyword.equals("回款情况")
            || normalizedKeyword.equals("客户应收情况")
            || normalizedKeyword.equals("客户欠款情况")
            || normalizedKeyword.equals("客户回款情况");
    }

    private boolean isInvalidContextCarryKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return normalizedKeyword.equals("那个客户")
            || normalizedKeyword.equals("刚才那个客户")
            || normalizedKeyword.equals("这个客户")
            || normalizedKeyword.equals("的欠款")
            || normalizedKeyword.equals("的欠款呢")
            || normalizedKeyword.equals("欠款呢")
            || normalizedKeyword.startsWith("的")
            || normalizedKeyword.startsWith("那个")
            || normalizedKeyword.startsWith("这个");
    }

    private JsonNode buildProductKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "keyword", firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:库存全景|库存健康|库存周转)"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:库存全景|库存健康|库存周转)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:库存全景|库存健康|库存周转)"),
            extractNamedValue(message, "(?:库存全景|库存健康|库存周转)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:商品|货品|SKU|sku)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "product")
        ));
        return params;
    }

    private JsonNode buildPurchaseKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "keyword", firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:采购跟踪|采购进度|入库退货)"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:采购跟踪|采购进度|入库退货)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:采购跟踪|采购进度|入库退货)"),
            extractNamedValue(message, "(?:采购跟踪|采购进度|入库退货)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:采购单|采购订单|供应商)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        return params;
    }

    private Integer inferFinanceRecordType(String normalized) {
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (containsAny(normalized, "收入", "收款", "入账", "到账", "income")) {
            return 1;
        }
        if (containsAny(normalized, "支出", "费用", "开支", "报销", "付款", "expense")) {
            return 2;
        }
        return null;
    }

    private boolean looksLikeCreateIntent(String normalized) {
        return containsAny(normalized, "新建", "新增", "创建", "添加", "生成草稿", "建一个", "建个")
            || containsAny(normalized, "记一笔", "记账", "记个", "报销一笔")
            || (containsAny(normalized, "开单", "开一张", "开个", "下一张", "下一笔")
            && containsAny(normalized, "销售单", "销售订单", "采购单", "采购订单", "付款单", "付款"));
    }

    private boolean hasTextParam(JsonNode params, String fieldName) {
        return params != null && StringUtils.hasText(params.path(fieldName).asText(null));
    }

    private boolean hasNumericParam(JsonNode params, String fieldName) {
        return params != null && params.hasNonNull(fieldName) && params.path(fieldName).isNumber();
    }

    private void putText(JsonNode params, String fieldName, String value) {
        if (params instanceof ObjectNode objectNode && StringUtils.hasText(value)) {
            objectNode.put(fieldName, value.trim());
        }
    }

    private void putDouble(JsonNode params, String fieldName, Double value) {
        if (params instanceof ObjectNode objectNode && value != null) {
            objectNode.put(fieldName, value);
        }
    }

    private String findRecentEntityHint(List<AgentMessageEntity> history, String conversationSummary, String entityKind) {
        String fromSummary = findEntityHintInText(conversationSummary, entityKind);
        if (StringUtils.hasText(fromSummary)) {
            return fromSummary;
        }
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int index = history.size() - 1; index >= 0; index--) {
            AgentMessageEntity message = history.get(index);
            if (message == null) {
                continue;
            }
            String hint = findEntityHintInText(message.getContent(), entityKind);
            if (StringUtils.hasText(hint)) {
                return hint;
            }
        }
        return null;
    }

    private String findEntityHintInText(String text, String entityKind) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(entityKind)) {
            return null;
        }
        return switch (entityKind) {
            case "customer" -> firstNonBlank(
                extractNamedValue(text, "客户[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})(?:商贸|超市|门店|公司)")
            );
            case "supplier" -> firstNonBlank(
                extractNamedValue(text, "供应商[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})(?:供货|批发|贸易)")
            );
            case "product" -> firstNonBlank(
                extractNamedValue(text, "(?:商品|货品|SKU)[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})(?:库存全景|库存健康|库存周转)")
            );
            case "account" -> firstNonBlank(
                extractNamedValue(text, "(?:账户|资金账户)[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "默认账户[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?")
            );
            default -> null;
        };
    }

    private String extractNamedValue(String message, String regex) {
        return extractNamedValue(message, regex, 1);
    }

    private String extractNamedValue(String message, String regex, int group) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return normalizeExtractedText(matcher.group(group));
    }

    private Double extractDecimal(String message, String regex) {
        String text = extractNamedValue(message, regex);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer extractInteger(String message, String regex) {
        String text = extractNamedValue(message, regex);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractRemark(String message) {
        return firstNonBlank(
            extractNamedValue(message, "(?:备注|说明)\\s*[:：]?\\s*([^，。,；;]+)"),
            extractNamedValue(message, "(?:备注|说明)\\s+(.+)$")
        );
    }

    private String normalizeExtractedText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("^[\\s:：,，。;；]+|[\\s,，。;；]+$", "").trim();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... candidates) {
        for (T candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }
}
