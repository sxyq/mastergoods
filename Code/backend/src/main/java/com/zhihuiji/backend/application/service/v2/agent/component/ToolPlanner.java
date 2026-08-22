package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentToolPlan;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.NativeToolCallBlock;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolFailureResult;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
 * 所有工具选择和参数都必须来自 LLM；服务端只负责校验工具白名单、执行工具并回填真实 facts。
 * 规划路径优先级：
 * <ol>
 *   <li>原生 Function Calling（Anthropic Messages API 的 tool_use block）</li>
 *   <li>JSON 字符串解析路径（兼容 Chat Completions / Responses API）</li>
 * </ol>
 *
 * <p>原属 {@code V2AgentAiService} 内联逻辑，因 God Class 拆分提取为独立组件。
 * 依赖 {@link LongCatAnthropicClient}、{@link ToolRegistry}、{@link ObjectMapper} 三个组件。
 *
 * <p>历史上下文格式化由 {@link #formatHistoryContext} 统一提供，供终答模型复用。
 */
@Component
public class ToolPlanner {
    /**
     * Keep one initial model decision plus at most three bounded continuations.
     * A write request may need one initial call, two dependency lookups and a
     * final model-selected draft call before the answer can be produced.
     */
    /** Shared with the orchestration loop so the planner and executor stop at the same turn. */
    public static final int MAX_AGENT_ITERATIONS = 3;
    private static final int MAX_TOOLS_PER_PLAN = 6;
    private static final String RESULT_VISUALIZATION_TOOL = "result_visualization";
    private static final Pattern EXPLICIT_CUSTOMER_NAME = Pattern.compile(
        "(?:名字|名称|客户名)\\s*(?:叫|是|为)?\\s*[\\\"“「]?([^，,。；;\\n]+?)[\\\"”」]?\\s*(?=电话|手机号|手机|，|,|。|；|;|$)"
    );
    private static final Pattern EXPLICIT_PHONE = Pattern.compile(
        "(?:电话|手机号|手机)\\s*[:：]?\\s*(1\\d{10})(?!\\d)"
    );

    /**
     * A candidate scope narrows the tools advertised for a turn; it never
     * executes a tool or replaces the model's native tool choice.
     */
    private static final List<CandidateRule> READ_CANDIDATE_RULES = List.of(
        rule(List.of("资金账户"), List.of("还剩", "余额", "几个"), "account_balance_lookup"),
        rule(List.of("资金账户"), List.of("状态", "异常"), "account_health_lookup"),
        rule(List.of("账户"), List.of("健康", "收支比"), "account_health_lookup"),
        rule(List.of("账户"), List.of("转过", "转账", "转入", "转出"), "account_transfer_lookup"),
        rule(List.of(), List.of("异常", "下滑", "缺货", "客户欠款"), "anomaly_alert_lookup"),
        rule(List.of(), List.of("怎么进出", "进出的", "资金变动"), "cash_change_lookup"),
        rule(List.of(), List.of("现金流", "净现金流"), "cashflow_summary_lookup"),
        rule(List.of("销售", "采购", "库存"), List.of(), "cross_analysis_lookup"),
        rule(List.of("客户"), List.of("整体", "下单", "收款", "退货"), "customer_profile_lookup"),
        rule(List.of("客户"), List.of("欠我钱", "欠款", "优先收款", "还欠"), "customer_receivable_lookup"),
        rule(List.of(), List.of("导出", "字段"), "data_export_tool"),
        rule(List.of(), List.of("收入支出流水", "按类别", "收入支出"), "finance_record_lookup"),
        rule(List.of(), List.of("海报提示词", "海报文案", "海报"), "product_catalog_lookup", "generate_poster_prompt"),
        rule(List.of(), List.of("数据导入", "导入", "失败重试"), "import_job_lookup"),
        rule(List.of("库存"), List.of("调整", "盘盈盘亏"), "inventory_adjustment_lookup"),
        rule(List.of("库存"), List.of("出入库", "流水", "来源"), "inventory_ledger_lookup"),
        rule(List.of(), List.of("没货", "低库存", "补多少", "快没货"), "inventory_low_stock_lookup"),
        rule(List.of("库存"), List.of("全貌", "安全库存", "周转", "库存和补货"), "inventory_panorama_lookup"),
        rule(List.of("库存"), List.of("盘点", "快照"), "inventory_snapshot_lookup"),
        rule(List.of(), List.of("联系人"), "partner_contact_lookup"),
        rule(List.of(), List.of("分组"), "partner_group_lookup"),
        rule(List.of("供应商"), List.of("付了", "付款", "支付"), "pay_order_lookup"),
        rule(List.of("收款", "付款"), List.of("记录", "理一下", "最近"), "payment_lookup"),
        rule(List.of("回款"), List.of(), "payment_lookup"),
        rule(List.of("商品"), List.of("库存", "价格", "分类", "现在有哪些"), "product_catalog_lookup"),
        rule(List.of(), List.of("商品分类", "分类"), "product_category_lookup"),
        rule(List.of(), List.of("价格等级"), "product_price_level_lookup"),
        rule(List.of("商品"), List.of("采购价", "哪家供应商", "从哪家"), "product_supplier_relation_lookup"),
        rule(List.of("采购"), List.of("采购单", "到货", "最近的采购"), "purchase_order_lookup"),
        rule(List.of(), List.of("入库", "入了哪些采购货"), "purchase_receipt_lookup"),
        rule(List.of("退"), List.of("供应商", "采购货"), "purchase_return_lookup"),
        rule(List.of("采购", "入库", "退货"), List.of(), "purchase_tracking_lookup"),
        rule(List.of(), List.of("应收", "应付", "供应商应付款", "客户欠我的"), "receivable_payable_lookup"),
        rule(List.of(), List.of("经营汇总", "经营情况", "这个月销售怎么样", "经营报表"), "report_query"),
        rule(List.of("销售单"), List.of("关联", "收款", "退货"), "sales_full_chain_lookup"),
        rule(List.of(), List.of("卖出去", "销售单和收款", "客户和收款"), "sale_order_lookup"),
        rule(List.of("销售"), List.of("最近一周", "整体情况", "销售和回款"), "sales_overview_lookup"),
        rule(List.of(), List.of("销售退货", "退货明细"), "sales_return_lookup"),
        rule(List.of(), List.of("趋势", "按天", "每天卖"), "sales_trend_lookup"),
        rule(List.of(), List.of("补货", "补多少", "紧急程度", "建议数量"), "smart_restock_lookup"),
        rule(List.of(), List.of("门店", "成员数量"), "store_info_lookup"),
        rule(List.of("供应商"), List.of("欠", "应付", "采购情况"), "supplier_payable_lookup"),
        rule(List.of("供应商"), List.of("对账", "核对", "算进去"), "supplier_statement_lookup"),
        rule(List.of(), List.of("同步"), "sync_status_lookup")
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
        if (!longCatAnthropicClient.isConfigured()) {
            return new AgentToolPlan(List.of(), "", "llm_unavailable", Map.of());
        }
        return planToolsWithLlm(message, history, conversationSummary)
            .orElseGet(() -> new AgentToolPlan(List.of(), "", "llm_planning_failed", Map.of()));
    }

    public Optional<AgentToolPlan> planToolsWithLlm(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (!longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        // 先把已经明确的业务候选集交给模型选择。服务端只缩小工具白名单，实际工具
        // 仍由 provider 的 tool_choice=auto 决定，避免一次请求把全量 schema 和无关
        // 工具都发给模型导致慢响应。
        List<String> focusedCandidates = focusedCandidateToolNamesForMessage(message);
        if (focusedCandidates.isEmpty()) {
            focusedCandidates = highConfidenceReadCandidates(message);
        }
        List<String> initialCandidates = initialToolNamesForMessage(message);
        boolean hasFocusedCandidates = !focusedCandidates.isEmpty();
        List<String> firstCandidates = hasFocusedCandidates ? focusedCandidates : initialCandidates;
        Optional<AgentToolPlan> nativePlan = planToolsWithNativeFunctionCalling(
            message,
            history,
            conversationSummary,
            firstCandidates
        );
        if (nativePlan.isPresent()) {
            return nativePlan;
        }

        // 仅当首轮使用了语义候选集，且候选集确实不同于初始范围时，才回退到
        // 初始范围重试。对于 create_sale_order、create_customer 等写请求，首轮
        // 已经是同一组候选工具，禁止再次发送完全相同的模型请求。
        if (hasFocusedCandidates
            && !new LinkedHashSet<>(focusedCandidates).equals(new LinkedHashSet<>(initialCandidates))) {
            Optional<AgentToolPlan> broadNativePlan = planToolsWithNativeFunctionCalling(
                message,
                history,
                conversationSummary,
                initialCandidates
            );
            if (broadNativePlan.isPresent()) {
                return broadNativePlan;
            }
        }
        // 降级路径：prompt + JSON 解析（兼容 Chat Completions / Responses API 及不支持 tool_use 的模型）
        // 工具清单只从注册表动态生成；没有已注册工具时不调用模型规划。
        // 初始规划阶段只允许选择真实数据查询/草稿工具。
        // result_visualization 必须在查询结果返回后由下一轮模型规划主动选择。
        String toolCatalog = buildInitialToolCatalogForLlm(message);
        if (toolCatalog.isBlank()) {
            return Optional.empty();
        }
        String systemPrompt = AgentPromptCatalog.initialSystemPrompt(toolCatalog, message)
            + "本次使用兼容模式时只输出 JSON，不要输出 Markdown。\n";
        String historyContext = formatHistoryContext(history);
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "会话摘要：" + conversationSummary + "\n"
            : "";
        String userPrompt = historyContext
            + summaryContext
            + "用户问题：" + message + "\n"
            + "请输出形如 {\"tools\":[{\"name\":\"sale_order_lookup\",\"params\":{\"keyword\":\"张三\"}}],\"rationale\":\"...\"} 的 JSON。"
            + "tools 最多 6 个，必须来自可选工具。params 为该工具的查询参数，根据用户问题提取，无参数时省略 params 字段。"
            + "初始阶段不要调用 result_visualization；查询结果返回后，下一轮模型会根据真实事实决定是否调用它。";
        return longCatAnthropicClient.createJsonMessage(systemPrompt, userPrompt)
            .flatMap(raw -> parseToolPlan(raw, false, "model_json_plan", message));
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
        // The initial turn is still the model's decision point. A recognized
        // request gets a bounded semantic scope so the model cannot turn one
        // topic into a broad scan of neighboring tools. The model continues
        // to choose the actual call with the provider's auto tool choice.
        List<String> availableToolNames = initialToolNamesForMessage(message);
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = buildInitialNativeToolDefinitions(availableToolNames);
        if (nativeTools.isEmpty()) {
            return Optional.empty();
        }
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "\n当前会话摘要：" + conversationSummary
            : "";
        String systemPrompt = AgentPromptCatalog.initialSystemPrompt(
            AgentPromptCatalog.buildCatalog(
                availableToolNames.stream()
                    .map(toolRegistry::getTool)
                    .flatMap(Optional::stream)
                    .toList(),
                false
            ),
            message
        )
            + "最多选择 6 个工具。"
            + (isExplicitInventoryRestockMultiSourceRequest(message)
                && AgentPromptCatalog.requestsVisualization(message)
                ? "用户明确要求展示；首轮先查询真实库存和补货数据，真实结果返回后再由你自主决定是否调用 result_visualization。"
                : "")
            + summaryContext;
        String planningMessage = formatHistoryContext(history) + "用户问题：" + message;
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, planningMessage, nativeTools);
        return toNativeToolPlan(
            message,
            history,
            conversationSummary,
            response,
            false,
            new LinkedHashSet<>(availableToolNames),
            "native_tool_use",
            true
        );
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
            + "不要输出自然语言解释，优先直接返回原生工具调用。最多选择 6 个工具。"
            + "单主题只选一个工具；只有用户明确要求多个独立来源时才选择多个。聚合工具已经覆盖的字段不要再拆分调用。"
            + summaryContext;
        String planningMessage = formatHistoryContext(history) + "用户问题：" + message;
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, planningMessage, nativeTools);
        return toNativeToolPlan(
            message,
            history,
            conversationSummary,
            response,
            false,
            new LinkedHashSet<>(candidateToolNames),
            "native_tool_use",
            true
        );
    }

    private Optional<AgentToolPlan> toNativeToolPlan(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary,
        Optional<LongCatAnthropicClient.ToolUseResponse> response,
        boolean allowVisualization
    ) {
        return toNativeToolPlan(
            message,
            history,
            conversationSummary,
            response,
            allowVisualization,
            null,
            "native_tool_use"
        );
    }

    private Optional<AgentToolPlan> toNativeToolPlan(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary,
        Optional<LongCatAnthropicClient.ToolUseResponse> response,
        boolean allowVisualization,
        Set<String> candidateToolNames,
        String planSource
    ) {
        return toNativeToolPlan(
            message,
            history,
            conversationSummary,
            response,
            allowVisualization,
            candidateToolNames,
            planSource,
            true
        );
    }

    private Optional<AgentToolPlan> toNativeToolPlan(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary,
        Optional<LongCatAnthropicClient.ToolUseResponse> response,
        boolean allowVisualization,
        Set<String> candidateToolNames,
        String planSource,
        boolean allowClarification
    ) {
        if (response.isEmpty() || !response.get().hasToolUses()) {
            return Optional.empty();
        }
        List<String> tools = new ArrayList<>();
        Map<String, JsonNode> toolParams = new LinkedHashMap<>();
        List<NativeToolCallBlock> nativeBlocks = new ArrayList<>();
        boolean recoveredExplicitCustomerParameters = false;
        for (LongCatAnthropicClient.ToolUseBlock toolUse : response.get().toolUses()) {
            if (nativeBlocks.size() >= MAX_TOOLS_PER_PLAN) {
                break;
            }
            if (toolUse == null || !StringUtils.hasText(toolUse.name())) {
                continue;
            }
            String toolName = toolUse.name();
            if ((!allowVisualization && RESULT_VISUALIZATION_TOOL.equals(toolName))
                || (candidateToolNames != null && !candidateToolNames.contains(toolName))
                || !isAllowedTool(toolName)) {
                continue;
            }
            if (!tools.contains(toolName)) {
                tools.add(toolName);
            }
            JsonNode input = toolUse.input();
            JsonNode effectiveInput = recoverExplicitCustomerParameters(message, toolName, input);
            recoveredExplicitCustomerParameters |= effectiveInput != input;
            if (effectiveInput != null && effectiveInput.isObject() && !effectiveInput.isEmpty()
                && !toolParams.containsKey(toolName)) {
                toolParams.put(toolName, effectiveInput);
            }
            if (StringUtils.hasText(toolUse.id())) {
                String argumentsJson;
                try {
                    argumentsJson = effectiveInput != null
                        ? objectMapper.writeValueAsString(effectiveInput)
                        : "{}";
                } catch (Exception ignored) {
                    argumentsJson = "{}";
                }
                nativeBlocks.add(new NativeToolCallBlock(toolUse.id(), toolName, argumentsJson));
            }
        }
        if (tools.isEmpty()) {
            return Optional.empty();
        }
        String rationale = response.get().text() != null && !response.get().text().isBlank()
            ? response.get().text()
            : "模型通过原生 Function Calling 选择工具";
        if (recoveredExplicitCustomerParameters) {
            rationale += "；参数审计：仅从用户原话明确恢复 create_customer 的 name/phone";
        }
        AgentToolPlan originalPlan = new AgentToolPlan(tools, rationale, planSource, toolParams,
            response.get().responseId(), nativeBlocks);
        if (!allowClarification || !requiresSingleToolClarification(message, tools)) {
            return Optional.of(originalPlan);
        }
        return clarifySingleToolSelection(
            message,
            history,
            conversationSummary,
            originalPlan,
            candidateToolNames,
            allowVisualization
        );
    }

    /**
     * Recovers only values explicitly labelled in the user's request after
     * the model has selected create_customer. Existing valid model values are
     * retained; this is not a general natural-language router.
     */
    private JsonNode recoverExplicitCustomerParameters(String message, String toolName, JsonNode input) {
        if (!"create_customer".equals(toolName) || !StringUtils.hasText(message)) {
            return input;
        }
        Matcher nameMatcher = EXPLICIT_CUSTOMER_NAME.matcher(message);
        Matcher phoneMatcher = EXPLICIT_PHONE.matcher(message);
        boolean hasName = nameMatcher.find();
        boolean hasPhone = phoneMatcher.find();
        if (!hasName && !hasPhone) {
            return input;
        }
        ObjectNode recovered = input != null && input.isObject()
            ? ((ObjectNode) input).deepCopy()
            : objectMapper.createObjectNode();
        if ((!recovered.has("name") || !StringUtils.hasText(recovered.path("name").asText()))
            && hasName && nameMatcher.groupCount() >= 1 && StringUtils.hasText(nameMatcher.group(1))) {
            recovered.put("name", nameMatcher.group(1).trim());
        }
        if ((!recovered.has("phone") || !StringUtils.hasText(recovered.path("phone").asText()))
            && hasPhone && phoneMatcher.groupCount() >= 1 && StringUtils.hasText(phoneMatcher.group(1))) {
            recovered.put("phone", phoneMatcher.group(1).trim());
        }
        return recovered.equals(input) ? input : recovered;
    }

    /**
     * A single-topic read must not fan out merely because the provider returned
     * several neighboring tools. Explicit comparison/multi-source requests and
     * read-plus-write dependency chains remain model-controlled multi-tool flows.
     */
    private boolean requiresSingleToolClarification(String message, List<String> tools) {
        if (tools == null || tools.size() <= 1 || allowsMultipleModelTools(message, tools)) {
            return false;
        }
        return tools.stream().allMatch(this::isReadOnlyTool);
    }

    private boolean allowsMultipleModelTools(String message, List<String> tools) {
        if (AgentPromptCatalog.requestsMultipleSources(message)
            || AgentPromptCatalog.requestsVisualization(message)
            || isExplicitMultiSourceRequest(message)) {
            return true;
        }
        // Purchase and sale drafts are dependency flows: the model may need
        // both a partner lookup and a product lookup before it can create the
        // draft. Do not collapse those read-only dependencies into a single
        // clarification choice before the real facts are available.
        if (isWriteDependencyFlow(message)) {
            return true;
        }
        boolean hasReadOnly = tools.stream().anyMatch(this::isReadOnlyTool);
        boolean hasCreateOnly = tools.stream().anyMatch(toolName -> toolRegistry.getTool(toolName)
            .map(tool -> tool.type() == AgentTool.ToolType.CREATE_ONLY)
            .orElse(false));
        return hasReadOnly && hasCreateOnly;
    }

    private boolean isWriteDependencyFlow(String message) {
        String writeTarget = AgentPromptCatalog.targetWriteTool(message);
        return "create_purchase_order".equals(writeTarget)
            || "create_sale_order".equals(writeTarget);
    }

    private boolean isReadOnlyTool(String toolName) {
        return toolRegistry.getTool(toolName)
            .map(tool -> tool.type() == AgentTool.ToolType.READ_ONLY)
            .orElse(false);
    }

    private Optional<AgentToolPlan> clarifySingleToolSelection(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary,
        AgentToolPlan originalPlan,
        Set<String> candidateToolNames,
        boolean allowVisualization
    ) {
        LinkedHashSet<String> clarificationCandidates = new LinkedHashSet<>();
        if (candidateToolNames != null) {
            clarificationCandidates.addAll(candidateToolNames);
        }
        if (clarificationCandidates.isEmpty()) {
            clarificationCandidates.addAll(originalPlan.tools());
        }
        clarificationCandidates.removeIf(toolName -> !isAllowedTool(toolName)
            || (!allowVisualization && RESULT_VISUALIZATION_TOOL.equals(toolName)));
        List<LongCatAnthropicClient.ToolDefinition> definitions = buildNativeToolDefinitions(
            new ArrayList<>(clarificationCandidates)
        );
        if (definitions.isEmpty()) {
            return Optional.of(selectionFailurePlan(originalPlan, List.of(), "没有可用于澄清的合法工具"));
        }
        String originalChoices = String.join("、", originalPlan.tools());
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "\n会话摘要：" + conversationSummary
            : "";
        String systemPrompt = "你是智慧记的工具选择澄清器。模型上一轮为同一个用户主题选择了多个相邻的只读工具。"
            + "请重新理解用户原话，只通过原生 Function Calling 选择一个最直接、覆盖范围最完整的工具。"
            + "不要并行选择，不要输出自然语言解释；服务端不会替你猜测工具。"
            + summaryContext
            + "\n上一轮选择：" + originalChoices
            + "\n可选工具：\n"
            + AgentPromptCatalog.buildCatalog(
                clarificationCandidates.stream()
                    .map(toolRegistry::getTool)
                    .flatMap(Optional::stream)
                    .toList(),
                allowVisualization
            );
        String userPrompt = formatHistoryContext(history) + "用户原话：" + message
            + "\n请只返回一个最直接的工具调用。";
        Optional<LongCatAnthropicClient.ToolUseResponse> clarificationResponse =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, userPrompt, definitions, "auto");
        if (clarificationResponse.isEmpty() || !clarificationResponse.get().hasToolUses()) {
            return Optional.of(selectionFailurePlan(originalPlan, List.of(), "澄清轮没有返回工具调用"));
        }
        Optional<AgentToolPlan> clarifiedPlan = toNativeToolPlan(
            message,
            history,
            conversationSummary,
            clarificationResponse,
            allowVisualization,
            clarificationCandidates,
            "native_tool_use_clarified",
            false
        );
        if (clarifiedPlan.isEmpty()) {
            return Optional.of(selectionFailurePlan(originalPlan, clarificationResponse.get().toolUses(), "澄清轮没有返回合法工具"));
        }
        AgentToolPlan plan = clarifiedPlan.get();
        if (plan.tools().size() != 1) {
            return Optional.of(selectionFailurePlan(originalPlan, plan.nativeToolCallBlocks(),
                "澄清轮仍选择多个互斥只读工具：" + String.join("、", plan.tools())));
        }
        String clarifiedRationale = "原始模型选择：" + originalChoices
            + "；澄清后选择：" + plan.tools().get(0);
        return Optional.of(new AgentToolPlan(
            plan.tools(), clarifiedRationale, "native_tool_use_clarified", plan.toolParams(),
            plan.nativeResponseId(), plan.nativeToolCallBlocks()
        ));
    }

    private AgentToolPlan selectionFailurePlan(
        AgentToolPlan originalPlan,
        List<?> clarificationBlocks,
        String reason
    ) {
        List<NativeToolCallBlock> traceBlocks = new ArrayList<>(originalPlan.nativeToolCallBlocks());
        if (clarificationBlocks != null) {
            for (Object value : clarificationBlocks) {
                if (value instanceof LongCatAnthropicClient.ToolUseBlock block
                    && StringUtils.hasText(block.id()) && StringUtils.hasText(block.name())) {
                    String arguments;
                    try {
                        arguments = block.input() == null ? "{}" : objectMapper.writeValueAsString(block.input());
                    } catch (Exception ignored) {
                        arguments = "{}";
                    }
                    traceBlocks.add(new NativeToolCallBlock(block.id(), block.name(), arguments));
                } else if (value instanceof NativeToolCallBlock block) {
                    traceBlocks.add(block);
                }
            }
        }
        return new AgentToolPlan(
            List.of(),
            "模型工具选择失败：" + reason + "；原始选择：" + String.join("、", originalPlan.tools()),
            "model_tool_selection_failed",
            Map.of(),
            originalPlan.nativeResponseId(),
            traceBlocks
        );
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
                AgentPromptCatalog.modelDescription(tool),
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
                AgentPromptCatalog.modelDescription(tool),
                convertSchemaToMap(tool.parameterSchema())
            )));
        }
        return nativeTools;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> convertSchemaToMap(JsonNode schema) {
        try {
            ObjectNode normalized = schema != null && schema.isObject()
                ? ((ObjectNode) schema).deepCopy()
                : objectMapper.createObjectNode();
            normalizeStrictSchema(normalized);
            return objectMapper.convertValue(normalized, Map.class);
        } catch (Exception ignored) {
            return Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of(),
                "additionalProperties", false
            );
        }
    }

    /**
     * Normalizes an object schema for Responses strict function calling.
     * Optional business inputs stay optional by accepting JSON null, while the
     * provider-facing required list contains every declared property.
     */
    private void normalizeStrictSchema(ObjectNode schema) {
        if (!schema.has("type")) {
            schema.put("type", "object");
        }
        if (!schema.has("properties") || !schema.path("properties").isObject()) {
            schema.putObject("properties");
        }

        Set<String> originalRequired = new LinkedHashSet<>();
        JsonNode requiredNode = schema.get("required");
        if (requiredNode != null && requiredNode.isArray()) {
            requiredNode.forEach(item -> {
                if (item.isTextual()) {
                    originalRequired.add(item.textValue());
                }
            });
        }

        ObjectNode properties = (ObjectNode) schema.get("properties");
        ArrayNode required = schema.putArray("required");
        properties.fieldNames().forEachRemaining(name -> {
            JsonNode property = properties.get(name);
            if (property instanceof ObjectNode propertyObject) {
                normalizeNestedSchema(propertyObject);
                if (!originalRequired.contains(name)) {
                    allowNull(propertyObject);
                }
            }
            required.add(name);
        });
        schema.put("additionalProperties", false);
    }

    private void normalizeNestedSchema(ObjectNode schema) {
        if (schema.has("properties") || "object".equals(schema.path("type").asText())) {
            normalizeStrictSchema(schema);
        }
        JsonNode items = schema.get("items");
        if (items instanceof ObjectNode itemObject) {
            normalizeNestedSchema(itemObject);
        }
        for (String branch : List.of("oneOf", "anyOf", "allOf")) {
            JsonNode branchNode = schema.get(branch);
            if (branchNode != null && branchNode.isArray()) {
                branchNode.forEach(item -> {
                    if (item instanceof ObjectNode itemObject) {
                        normalizeNestedSchema(itemObject);
                    }
                });
            }
        }
    }

    private void allowNull(ObjectNode property) {
        JsonNode type = property.get("type");
        if (type == null || type.isNull()) {
            return;
        }
        if (type.isTextual()) {
            ArrayNode types = objectMapper.createArrayNode();
            types.add(type.textValue());
            types.add("null");
            property.set("type", types);
        } else if (type.isArray() && !type.toString().contains("\"null\"")) {
            ((ArrayNode) type).add("null");
        }
        JsonNode enumNode = property.get("enum");
        if (enumNode != null && enumNode.isArray()) {
            ArrayNode enumValues = (ArrayNode) enumNode;
            boolean hasNullEnum = false;
            for (JsonNode value : enumValues) {
                if (value.isNull()) {
                    hasNullEnum = true;
                    break;
                }
            }
            if (!hasNullEnum) {
                enumValues.addNull();
            }
        }
    }

    public Optional<AgentToolPlan> parseToolPlan(String rawText) {
        return parseToolPlan(rawText, true, "model_json_plan", null);
    }

    private Optional<AgentToolPlan> parseToolPlan(
        String rawText,
        boolean allowVisualization,
        String source,
        String userMessage
    ) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawText));
            Set<String> tools = new LinkedHashSet<>();
            Map<String, JsonNode> toolParams = new LinkedHashMap<>();
            boolean recoveredExplicitCustomerParameters = false;
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
                    if (allowVisualization || !RESULT_VISUALIZATION_TOOL.equals(tool)) {
                        if (isAllowedTool(tool)) {
                            tools.add(tool);
                            JsonNode effectiveParams = recoverExplicitCustomerParameters(
                                userMessage, tool, params
                            );
                            recoveredExplicitCustomerParameters |= effectiveParams != params;
                            if (effectiveParams != null && effectiveParams.isObject()) {
                                toolParams.put(tool, effectiveParams);
                            }
                        }
                    }
                    if (tools.size() >= MAX_TOOLS_PER_PLAN) {
                        break;
                    }
                }
            }
            if (tools.isEmpty() && (toolsNode == null || !toolsNode.isArray())) {
                return Optional.empty();
            }
            String rationale = root.path("rationale").asText("模型选择了当前问题所需的只读查询工具");
            if (recoveredExplicitCustomerParameters) {
                rationale += "；参数审计：仅从用户原话明确恢复 create_customer 的 name/phone";
            }
            return Optional.of(new AgentToolPlan(new ArrayList<>(tools), rationale, source, toolParams));
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

    public Optional<AgentToolPlan> planNextIteration(String message, List<ToolExecutionResult> toolResults, int iteration) {
        return planNextIteration(message, null, toolResults, List.of(), iteration);
    }

    public Optional<AgentToolPlan> planNextIteration(
        String message,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        int iteration
    ) {
        return planNextIteration(message, null, toolResults, toolFailures, iteration);
    }

    /**
     * Plans the next turn while preserving the previous native tool-call transcript.
     *
     * <p>When the provider supports the standard assistant tool-call/tool-result
     * protocol, use it before rebuilding a summarized prompt. This keeps the
     * model's call IDs and arguments intact and gives it a real opportunity to
     * choose a dependent tool from the facts just returned.
     */
    public Optional<AgentToolPlan> planNextIteration(
        String message,
        AgentToolPlan previousPlan,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        int iteration
    ) {
        if (iteration > MAX_AGENT_ITERATIONS || !longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        Set<String> attemptedToolNames = observedToolNames(previousPlan, toolResults, toolFailures);
        boolean hasRealDataQuery = hasNonEmptyRealDataQuery(toolResults);
        if (!shouldAllowContinuation(message, toolResults, toolFailures, attemptedToolNames)) {
            return Optional.empty();
        }
        Optional<String> pendingRequiredWriteTarget = pendingRequiredWriteTarget(
            message, toolResults, toolFailures, hasRealDataQuery
        );
        Optional<String> retryableWriteTarget = retryableWriteTarget(
            message, toolResults, toolFailures, hasRealDataQuery, pendingRequiredWriteTarget
        );
        // A continuation must stay inside the bounded semantic scope. After a
        // single successful query, the only optional next decision is whether
        // the model wants a visualization. Re-opening neighboring read tools
        // here turns a correct one-tool answer into an unrelated scan. Explicit
        // multi-source questions and dependency completion remain eligible for
        // their bounded follow-up tools.
        boolean writeIntent = AgentPromptCatalog.hasWriteIntent(message);
        boolean hasPosterFollowup = message != null && message.contains("海报");
        boolean explicitMultiSource = isExplicitMultiSourceRequest(message);
        boolean explicitVisualization = AgentPromptCatalog.requestsVisualization(message);
        boolean sourcesReadyForVisualization = visualizationSourcesReady(
            message, toolResults, explicitMultiSource, attemptedToolNames
        );
        List<String> remainingRequiredToolNames = remainingRequiredToolNames(message, attemptedToolNames);
        boolean hasRemainingRequiredTool = !remainingRequiredToolNames.isEmpty();
        List<String> preferredToolNames = hasRemainingRequiredTool
            ? remainingRequiredToolNames
            : hasRealDataQuery
            && !writeIntent
            && !hasPosterFollowup
            && explicitVisualization
            && sourcesReadyForVisualization
            ? List.of(RESULT_VISUALIZATION_TOOL)
            : boundedCandidateToolNamesForMessage(message);
        List<String> candidateToolNames = preferredToolNames
            .stream()
            .filter(toolName -> !attemptedToolNames.contains(toolName)
                // A dependency lookup can make a failed create call valid.
                // Keep that write target available for one bounded model retry
                // after real owner-scoped facts have been returned.
                || (toolName.equals(AgentPromptCatalog.targetWriteTool(message))
                    && retryableWriteTarget.filter(toolName::equals).isPresent()))
            // 展示工具只有在已有真实 facts 后才进入候选集；是否展示由模型决定。
            .filter(toolName -> !RESULT_VISUALIZATION_TOOL.equals(toolName)
                || (hasRealDataQuery && explicitVisualization && sourcesReadyForVisualization))
            .toList();
        candidateToolNames = restrictAggregateContinuationCandidates(message, candidateToolNames);
        if (hasRealDataQuery && !attemptedToolNames.contains(RESULT_VISUALIZATION_TOOL)
            && explicitVisualization && !writeIntent && !hasPosterFollowup && sourcesReadyForVisualization) {
            LinkedHashSet<String> visualizationCandidates = new LinkedHashSet<>(candidateToolNames);
            visualizationCandidates.add(RESULT_VISUALIZATION_TOOL);
            candidateToolNames = visualizationCandidates.stream().filter(this::isAllowedTool).toList();
        }
        if (pendingRequiredWriteTarget.isPresent()) {
            // Once real dependency facts exist, expose only the original write
            // target for one model-owned auto-choice retry. Otherwise the model
            // can spend the remaining iteration budget re-querying dependencies.
            candidateToolNames = List.of(pendingRequiredWriteTarget.get());
        }
        candidateToolNames = narrowResolvedPayOrderCandidates(
            message,
            toolResults,
            candidateToolNames
        );
        if (candidateToolNames.isEmpty()) {
            return Optional.empty();
        }
        List<String> allowedCandidateToolNames = candidateToolNames;

        Optional<AgentToolPlan> continuedPlan = planNextIterationWithToolResultContinuation(
            message,
            previousPlan,
            toolResults,
            toolFailures,
            iteration,
            allowedCandidateToolNames,
            hasRealDataQuery
        );
        if (continuedPlan.isPresent()) {
            return continuedPlan;
        }

        // Provider 不支持标准续轮时仍使用 auto。服务端只过滤已执行工具和
        // 当前权限范围，不根据关键词指定下一件工具。
        Optional<AgentToolPlan> nativePlan = planNextIterationWithNativeFunctionCalling(
            message,
            toolResults,
            iteration,
            allowedCandidateToolNames,
            hasRealDataQuery
        );
        if (nativePlan.isPresent()) {
            return nativePlan;
        }

        // Some providers return a valid terminal sentence after a dependency
        // lookup even though the requested draft is still pending. Give the
        // model one bounded retry with only that registered draft tool exposed.
        // The server still uses auto tool choice; it does not execute or
        // synthesize the write operation itself.
        Optional<AgentToolPlan> writeTargetRetry = planRequiredWriteTargetRetry(
            message,
            toolResults,
            toolFailures,
            iteration,
            allowedCandidateToolNames,
            hasRealDataQuery
        );
        if (writeTargetRetry.isPresent()) {
            return writeTargetRetry;
        }

        Optional<AgentToolPlan> singleCandidateRetry = planRequiredSingleCandidateRetry(
            message,
            toolResults,
            iteration,
            allowedCandidateToolNames,
            hasRealDataQuery
        );
        if (singleCandidateRetry.isPresent()) {
            return singleCandidateRetry;
        }

        String toolCatalog = buildToolCatalogForLlm(allowedCandidateToolNames);
        if (toolCatalog.isBlank()) {
            return Optional.empty();
        }
        StringBuilder contextBuilder = new StringBuilder();
        for (ToolExecutionResult result : toolResults) {
            contextBuilder.append("- ").append(result.toolName()).append("：").append(result.summary()).append('\n');
            String factsJson = compactFacts(result.facts());
            if (!factsJson.isBlank()) {
                contextBuilder.append("  真实工具结果 JSON：").append(factsJson).append('\n');
            }
        }
        String executedContext = contextBuilder.length() > 0
            ? "已执行工具及结果摘要：\n" + contextBuilder
            : "上一轮工具未返回有效结果。\n";
        String systemPrompt = AgentPromptCatalog.reactSystemPrompt(
            toolCatalog,
            AgentPromptCatalog.hasWriteIntent(message),
            iteration,
            message
        ) + "兼容模式只输出 JSON，不要输出 Markdown。";
        String userPrompt = "用户问题：" + message + "\n"
            + executedContext
            + "请决定下一步；如果用户想做一件事而真实结果已经足够，请继续调用对应工具完成它；如果已经足够回答，请不要调用工具。";
        return longCatAnthropicClient.createJsonMessage(systemPrompt, userPrompt)
            .flatMap(raw -> parseToolPlan(raw, hasRealDataQuery, "model_json_plan_react", message))
            .map(plan -> restrictPlanToCandidates(plan, allowedCandidateToolNames));
    }

    /**
     * ReAct 续轮只用于依赖补全、用户明确的多来源查询或展示决策。
     * 单个查询已经返回事实后，不能因为模型的泛化倾向再次扫描其它工具。
     */
    private boolean shouldAllowContinuation(
        String message,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        Set<String> observedToolNames
    ) {
        Set<String> observed = observedToolNames == null ? Set.of() : observedToolNames;
        if (toolResults == null || toolResults.isEmpty()) {
            return !observed.contains(RESULT_VISUALIZATION_TOOL);
        }
        if (toolResults.stream().anyMatch(result -> result != null && result.insufficient())) {
            return true;
        }
        if (observed.contains(RESULT_VISUALIZATION_TOOL)) {
            return false;
        }
        // A completed single-source lookup is terminal by default. Re-opening
        // the model after every successful read turns one question into an
        // unrelated data scan. Continuation is enabled only by an explicit
        // multi-source/display request or by a dependency/write flow below.
        String writeTarget = AgentPromptCatalog.targetWriteTool(message);
        if (writeTarget != null && !hasCompletedTool(toolResults, writeTarget)) {
            return true;
        }
        if (message != null
            && message.contains("海报")
            && !hasCompletedTool(toolResults, "generate_poster_prompt")) {
            return true;
        }
        // The combined receivable/payable query already covers both sides of
        // this question. Do not reopen the two narrower tools after it has
        // completed; only an explicit visualization may be considered when
        // the combined query returned real rows.
        if (isExplicitReceivablePayableMultiSourceRequest(message)
            && observed.contains("receivable_payable_lookup")) {
            return hasNonEmptyRealDataQuery(toolResults)
                && AgentPromptCatalog.requestsVisualization(message)
                && !observed.contains(RESULT_VISUALIZATION_TOOL);
        }
        if (isExplicitInventoryRestockMultiSourceRequest(message)
            && observed.contains("inventory_panorama_lookup")
            && observed.contains("smart_restock_lookup")) {
            return hasNonEmptyRealDataQuery(toolResults)
                && AgentPromptCatalog.requestsVisualization(message)
                && !observed.contains(RESULT_VISUALIZATION_TOOL);
        }
        if (AgentPromptCatalog.requestsVisualization(message)
            && !observed.contains(RESULT_VISUALIZATION_TOOL)) {
            return true;
        }
        if (AgentPromptCatalog.requestsMultipleSources(message)
            && distinctReadToolCount(observed) < 2) {
            return true;
        }
        return false;
    }

    private Set<String> observedToolNames(
        AgentToolPlan previousPlan,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures
    ) {
        LinkedHashSet<String> observed = new LinkedHashSet<>();
        if (toolResults != null) {
            toolResults.stream()
                .filter(result -> result != null && StringUtils.hasText(result.toolName()))
                .map(ToolExecutionResult::toolName)
                .forEach(observed::add);
        }
        if (toolFailures != null) {
            toolFailures.stream()
                .filter(failure -> failure != null && StringUtils.hasText(failure.toolName()))
                .map(ToolFailureResult::toolName)
                .forEach(observed::add);
        }
        if (previousPlan != null) {
            if (previousPlan.tools() != null) {
                previousPlan.tools().stream()
                    .filter(StringUtils::hasText)
                    .forEach(observed::add);
            }
            if (previousPlan.nativeToolCallBlocks() != null) {
                previousPlan.nativeToolCallBlocks().stream()
                    .filter(block -> block != null && StringUtils.hasText(block.toolName()))
                    .map(NativeToolCallBlock::toolName)
                    .forEach(observed::add);
            }
        }
        return observed;
    }

    private boolean hasCompletedTool(List<ToolExecutionResult> toolResults, String toolName) {
        if (toolResults == null || !StringUtils.hasText(toolName)) {
            return false;
        }
        return toolResults.stream().anyMatch(result ->
            result != null && toolName.equals(result.toolName())
        );
    }

    private boolean visualizationSourcesReady(
        String message,
        List<ToolExecutionResult> toolResults,
        boolean explicitMultiSource,
        Set<String> observedToolNames
    ) {
        if (!explicitMultiSource) {
            return true;
        }
        Set<String> observed = observedToolNames == null ? Set.of() : observedToolNames;
        if (isExplicitInventoryRestockMultiSourceRequest(message)) {
            return observed.contains("inventory_panorama_lookup")
                && observed.contains("smart_restock_lookup");
        }
        if (isExplicitReceivablePayableMultiSourceRequest(message)) {
            // One combined read already covers both requested sources. It is
            // sufficient for a visualization decision when it returned rows.
            return observed.contains("receivable_payable_lookup")
                && hasNonEmptyRealDataQuery(toolResults);
        }
        return distinctReadToolCount(observed) >= 2;
    }

    private List<String> restrictAggregateContinuationCandidates(
        String message,
        List<String> candidateToolNames
    ) {
        if (candidateToolNames == null || candidateToolNames.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = null;
        if (isExplicitInventoryRestockMultiSourceRequest(message)) {
            allowed = Set.of(
                "inventory_panorama_lookup",
                "smart_restock_lookup",
                RESULT_VISUALIZATION_TOOL
            );
        } else if (isExplicitReceivablePayableMultiSourceRequest(message)) {
            allowed = Set.of("receivable_payable_lookup", RESULT_VISUALIZATION_TOOL);
        }
        if (allowed == null) {
            return candidateToolNames;
        }
        return candidateToolNames.stream().filter(allowed::contains).toList();
    }

    private boolean isExplicitMultiSourceRequest(String message) {
        return AgentPromptCatalog.requestsMultipleSources(message)
            || (message != null && message.contains("趋势") && message.contains("回款"))
            // Device/contract tests and English-speaking users may express
            // the same explicit multi-source intent in English.
            || (message != null && (message.contains(" and ")
                || message.toLowerCase(java.util.Locale.ROOT).contains("compare")
                || message.toLowerCase(java.util.Locale.ROOT).contains("chart")));
    }

    private int distinctReadResultCount(List<ToolExecutionResult> toolResults) {
        if (toolResults == null) {
            return 0;
        }
        return (int) toolResults.stream()
            .filter(result -> result != null && toolRegistry.getTool(result.toolName())
                .map(tool -> tool.type() == AgentTool.ToolType.READ_ONLY)
                .orElse(false))
            .map(ToolExecutionResult::toolName)
            .distinct()
            .count();
    }

    private int distinctReadToolCount(Set<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return 0;
        }
        return (int) toolNames.stream()
            .filter(name -> toolRegistry.getTool(name)
                .map(tool -> tool.type() == AgentTool.ToolType.READ_ONLY)
                .orElse(false))
            .distinct()
            .count();
    }

    private Optional<AgentToolPlan> planNextIterationWithToolResultContinuation(
        String message,
        AgentToolPlan previousPlan,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        int iteration,
        List<String> candidateToolNames,
        boolean hasRealDataQuery
    ) {
        if (previousPlan == null
            || previousPlan.nativeToolCallBlocks() == null
            || previousPlan.nativeToolCallBlocks().isEmpty()
            || candidateToolNames == null
            || candidateToolNames.isEmpty()
            || !longCatAnthropicClient.supportsToolResultContinuation()) {
            return Optional.empty();
        }
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = buildNativeToolDefinitions(candidateToolNames);
        if (nativeTools.isEmpty()) {
            return Optional.empty();
        }

        List<LongCatAnthropicClient.FunctionCallItem> functionCalls = previousPlan.nativeToolCallBlocks().stream()
            .filter(block -> block != null && StringUtils.hasText(block.toolCallId()) && StringUtils.hasText(block.toolName()))
            .map(block -> new LongCatAnthropicClient.FunctionCallItem(
                block.toolCallId(),
                block.toolName(),
                block.arguments() == null ? "{}" : block.arguments()
            ))
            .toList();
        if (functionCalls.isEmpty()) {
            return Optional.empty();
        }

        List<LongCatAnthropicClient.FunctionCallOutputItem> outputs = new ArrayList<>();
        for (LongCatAnthropicClient.FunctionCallItem call : functionCalls) {
            String output = findToolOutputJson(call, toolResults, toolFailures);
            outputs.add(new LongCatAnthropicClient.FunctionCallOutputItem(call.id(), output));
        }
        String systemPrompt = AgentPromptCatalog.reactSystemPrompt(
            buildToolCatalogForLlm(candidateToolNames),
            AgentPromptCatalog.hasWriteIntent(message),
            iteration,
            message
        ) + "上一轮工具已经通过标准 tool result 返回真实事实。请只根据这些事实自主决定是否继续调用当前需要的工具；如果已经足够回答，不要调用工具。"
            + resolvedPayOrderParameterContext(message, toolResults);
        // The candidate list is a permission and context boundary, not a
        // hidden router. The model must still decide whether to call the
        // remaining tool, including result_visualization.
        // The candidate list narrows the model's context and permission boundary,
        // but the model must still decide whether to call the remaining tool.
        // Never force a business tool with tool_choice=required.
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.continueWithToolOutputs(
                previousPlan.nativeResponseId(),
                systemPrompt,
                message,
                functionCalls,
                outputs,
                nativeTools
            );
        if (response.isEmpty()) {
            return Optional.empty();
        }
        if (!response.get().hasToolUses()) {
            String writeTarget = AgentPromptCatalog.targetWriteTool(message);
            if (StringUtils.hasText(writeTarget)
                && candidateToolNames.contains(writeTarget)
                && !hasCompletedTool(toolResults, writeTarget)) {
                // Let the outer planner issue the bounded target-only retry
                // instead of treating an early model sentence as final.
                return Optional.empty();
            }
            String rationale = StringUtils.hasText(response.get().text())
                ? response.get().text()
                : "模型判断当前真实结果已足够回答";
            return Optional.of(new AgentToolPlan(
                List.of(),
                rationale,
                "model_terminal_after_tool_results",
                Map.of(),
                response.get().responseId(),
                List.of(),
                response.get().text()
            ));
        }
        return toNativeToolPlan(
            message,
            List.of(),
            null,
            response,
            hasRealDataQuery,
            new LinkedHashSet<>(candidateToolNames),
            "native_tool_use_react_continuation"
        );
    }

    private Optional<AgentToolPlan> planRequiredWriteTargetRetry(
        String message,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        int iteration,
        List<String> candidateToolNames,
        boolean hasRealDataQuery
    ) {
        String target = AgentPromptCatalog.targetWriteTool(message);
        if (!StringUtils.hasText(target)
            || candidateToolNames == null
            || !candidateToolNames.contains(target)
            || hasCompletedTool(toolResults, target)
            || !hasRealDataQuery) {
            return Optional.empty();
        }
        List<LongCatAnthropicClient.ToolDefinition> targetTools = buildNativeToolDefinitions(List.of(target));
        if (targetTools.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder facts = new StringBuilder();
        if (toolResults != null) {
            for (ToolExecutionResult result : toolResults) {
                if (result == null) {
                    continue;
                }
                facts.append("- ").append(result.toolName()).append("：").append(result.summary()).append('\n');
                String factsJson = compactFacts(result.facts());
                if (!factsJson.isBlank()) {
                    facts.append("  真实结果 JSON：").append(factsJson).append('\n');
                }
            }
        }
        if (toolFailures != null) {
            for (ToolFailureResult failure : toolFailures) {
                if (failure != null) {
                    facts.append("- ").append(failure.toolName()).append(" 未完成：")
                        .append(failure.safeMessage()).append('\n');
                }
            }
        }

        String systemPrompt = AgentPromptCatalog.reactSystemPrompt(
            buildToolCatalogForLlm(List.of(target)),
            true,
            iteration,
            message
        ) + "这是一次有限的依赖完成重试。前面已经查到真实依赖数据。"
            + "如果 create 工具的必填参数已经能够从用户原话和真实结果中确定，请通过 auto 原生工具调用生成草稿；"
            + "不要返回‘没有这个工具’或只写说明。若真实数据仍不足，返回空工具调用。";
        String userPrompt = "用户问题：" + message + "\n"
            + "已执行工具及真实结果：\n" + facts
            + "当前唯一候选工具：" + target + "。请自主判断是否调用。"
            + resolvedPayOrderParameterContext(message, toolResults);
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, userPrompt, targetTools, "auto");
        // Some compatible providers return a terminal sentence even when the
        // only remaining candidate is the user's already classified write
        // target. A second request with the same single registered tool and
        // required choice makes the provider emit a real function call; it
        // still cannot bypass schema, permission, owner, or draft checks.
        if (response.isEmpty() || !response.get().hasToolUses()) {
            response = longCatAnthropicClient.createMessageWithTools(
                systemPrompt + "请返回该唯一草稿工具的原生调用，不要返回终止文本。",
                userPrompt,
                targetTools,
                "required"
            );
        }
        return toNativeToolPlan(
            message,
            List.of(),
            null,
            response,
            false,
            Set.of(target),
            "native_tool_use_write_target_retry"
        );
    }

    /**
     * Recover a required follow-up when the model has already completed the
     * first real query and only one declared candidate remains. The candidate
     * is derived from the planner scope and still selected by the provider;
     * this method never names or executes a business tool from a keyword.
     */
    private Optional<AgentToolPlan> planRequiredSingleCandidateRetry(
        String message,
        List<ToolExecutionResult> toolResults,
        int iteration,
        List<String> candidateToolNames,
        boolean hasRealDataQuery
    ) {
        if (!hasRealDataQuery || candidateToolNames == null || candidateToolNames.size() != 1) {
            return Optional.empty();
        }
        String candidate = candidateToolNames.get(0);
        if (!isAllowedTool(candidate) || hasCompletedTool(toolResults, candidate)) {
            return Optional.empty();
        }
        List<LongCatAnthropicClient.ToolDefinition> definitions = buildNativeToolDefinitions(List.of(candidate));
        if (definitions.isEmpty()) {
            return Optional.empty();
        }
        String facts = toolResults == null
            ? "无"
            : toolResults.stream()
                .filter(result -> result != null)
                .map(result -> "- " + result.toolName() + "：" + result.summary())
                .collect(java.util.stream.Collectors.joining("\n"));
        String systemPrompt = AgentPromptCatalog.reactSystemPrompt(
            buildToolCatalogForLlm(List.of(candidate)),
            AgentPromptCatalog.hasWriteIntent(message),
            iteration,
            message
        ) + "上一轮已取得真实结果，当前只剩一个由规划器声明的后续工具候选。请通过原生 Function Calling 自主判断是否调用；不要返回终止文本。";
        String userPrompt = "用户问题：" + message + "\n已执行真实结果：\n" + facts;
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, userPrompt, definitions, "auto");
        if (response.isEmpty() || !response.get().hasToolUses()) {
            response = longCatAnthropicClient.createMessageWithTools(
                systemPrompt + "请返回唯一候选工具的原生调用。",
                userPrompt,
                definitions,
                "required"
            );
        }
        return toNativeToolPlan(
            message,
            List.of(),
            null,
            response,
            hasRealDataQuery,
            Set.of(candidate),
            "native_tool_use_single_candidate_retry",
            false
        );
    }

    private String findToolOutputJson(
        LongCatAnthropicClient.FunctionCallItem call,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures
    ) {
        ToolExecutionResult matchingResult = null;
        if (toolResults != null) {
            matchingResult = toolResults.stream()
                .filter(result -> result != null && call.name().equals(result.toolName()))
                .filter(result -> StringUtils.hasText(call.id())
                    ? call.id().equals(result.toolCallId())
                    : !StringUtils.hasText(result.toolCallId()))
                .findFirst()
                .orElse(null);
        }
        if (matchingResult != null && matchingResult.facts() != null) {
            try {
                return objectMapper.writeValueAsString(matchingResult.facts());
            } catch (Exception ignored) {
                return "{}";
            }
        }
        if (toolFailures != null) {
            ToolFailureResult matchingFailure = toolFailures.stream()
                .filter(failure -> failure != null && call.name().equals(failure.toolName()))
                .filter(failure -> StringUtils.hasText(call.id())
                    ? call.id().equals(failure.toolCallId())
                    : !StringUtils.hasText(failure.toolCallId()))
                .findFirst()
                .orElse(null);
            if (matchingFailure != null) {
                try {
                    return objectMapper.writeValueAsString(Map.of(
                        "error", true,
                        "tool_name", matchingFailure.toolName(),
                        "message", matchingFailure.safeMessage()
                    ));
                } catch (Exception ignored) {
                    return "{\"error\":true}";
                }
            }
        }
        return "{}";
    }

    /**
     * Returns all registered tools allowed for this turn.
     *
     * <p>The server does not route a write request to a named business tool.
     * The model receives the permitted tool set and the native tool call is
     * the actual selection event. Execution still enforces registration,
     * required parameters, ownership and draft-only writes.
     */
    public List<String> candidateToolNamesForMessage(String message) {
        List<String> bounded = boundedCandidateToolNamesForMessage(message);
        if (!bounded.isEmpty()) {
            return bounded;
        }
        boolean writeIntent = AgentPromptCatalog.hasWriteIntent(message);
        return toolRegistry.listTools().stream()
            .filter(tool -> writeIntent || tool.type() == AgentTool.ToolType.READ_ONLY)
            .map(AgentTool::name)
            .toList();
    }

    /**
     * Returns only a semantically bounded scope. This is intentionally separate
     * from the public initial-planning fallback: an unknown first question may
     * still be offered the full registry once, but a completed tool must never
     * reopen that registry during ReAct continuation.
     */
    private List<String> boundedCandidateToolNamesForMessage(String message) {
        boolean writeIntent = AgentPromptCatalog.hasWriteIntent(message);
        if (isSalesTrendAndReceivableRequest(message)) {
            LinkedHashSet<String> candidates = new LinkedHashSet<>(List.of(
                "sales_trend_lookup",
                "payment_lookup"
            ));
            if (AgentPromptCatalog.requestsVisualization(message)) {
                candidates.add(RESULT_VISUALIZATION_TOOL);
            }
            return candidates.stream()
                .filter(this::isAllowedTool)
                .toList();
        }
        List<String> focused = focusedCandidateToolNamesForMessage(message);
        if (!focused.isEmpty() && !isAmbiguousWriteRequest(message)) {
            return focused;
        }
        if (writeIntent) {
            return List.of();
        }
        List<String> highConfidence = highConfidenceReadCandidates(message);
        if (!highConfidence.isEmpty()) {
            return highConfidence;
        }
        List<CandidateRule> matchingRules = READ_CANDIDATE_RULES.stream()
            .filter(rule -> rule.matches(message))
            .toList();
        LinkedHashSet<String> semanticCandidates = new LinkedHashSet<>();
        boolean explicitMultiSource = AgentPromptCatalog.requestsMultipleSources(message)
            || isExplicitMultiSourceRequest(message);
        if (explicitMultiSource) {
            matchingRules.forEach(rule -> semanticCandidates.addAll(rule.toolNames()));
        } else {
            int maxSpecificity = matchingRules.stream()
                .mapToInt(CandidateRule::specificity)
                .max()
                .orElse(-1);
            matchingRules.stream()
                .filter(rule -> rule.specificity() == maxSpecificity)
                .forEach(rule -> semanticCandidates.addAll(rule.toolNames()));
        }
        // A trend request needs day buckets and collection facts. The overview
        // tool already summarizes both domains, but it cannot add a trend
        // series; keeping it in the same continuation scope causes redundant
        // sales queries after the model has already selected sales_trend.
        if (message != null && message.contains("趋势") && message.contains("回款")) {
            semanticCandidates.remove("sales_overview_lookup");
            semanticCandidates.add("sales_trend_lookup");
            semanticCandidates.add("payment_lookup");
        }
        if (AgentPromptCatalog.requestsVisualization(message) && !semanticCandidates.isEmpty()) {
            semanticCandidates.add(RESULT_VISUALIZATION_TOOL);
        }
        return semanticCandidates.stream().filter(this::isAllowedTool).toList();
    }

    /**
     * Keep high-signal compound questions on their dedicated tool set. The
     * model still chooses the actual call with auto tool choice; this only
     * prevents a broad generic tool from shadowing a more complete one.
     */
    private List<String> highConfidenceReadCandidates(String message) {
        if (!StringUtils.hasText(message)) {
            return List.of();
        }
        List<String> candidates;
        if (isExplicitInventoryRestockMultiSourceRequest(message)) {
            candidates = new ArrayList<>(List.of("inventory_panorama_lookup", "smart_restock_lookup"));
            if (AgentPromptCatalog.requestsVisualization(message)) {
                candidates.add(RESULT_VISUALIZATION_TOOL);
            }
        } else if (isInventoryPanoramaRequest(message)) {
            candidates = List.of("inventory_panorama_lookup");
        } else if (containsAll(message, "销售下滑", "缺货", "客户欠款")) {
            candidates = List.of("anomaly_alert_lookup");
        } else if (containsAll(message, "客户", "整体")
            && containsAny(message, "余额", "下单", "收款", "退货")) {
            candidates = List.of("customer_profile_lookup");
        } else if (containsAll(message, "商品", "分类") && !message.contains("价格等级")) {
            // A plain category question should stay on the category tool. When
            // the user also asks for product-level inventory or prices, keep
            // both relevant tools available so the model can choose one or
            // call both without the server executing either one implicitly.
            candidates = containsAny(message, "库存", "价格", "售价", "商品信息")
                ? List.of("product_catalog_lookup", "product_category_lookup")
                : List.of("product_category_lookup");
        } else if (message.contains("价格等级")) {
            candidates = List.of("product_price_level_lookup");
        } else if (containsAny(message, "采购价", "哪家供应商", "从哪家")) {
            candidates = List.of("product_supplier_relation_lookup");
        } else if (containsAll(message, "卖出去", "客户", "收款")) {
            candidates = List.of("sale_order_lookup");
        } else if (containsAll(message, "销售单", "收款", "退货")) {
            candidates = List.of("sales_full_chain_lookup");
        } else if (isExplicitReceivablePayableMultiSourceRequest(message)) {
            // Keep the combined lookup as the single source for both sides;
            // the narrower tools must not be reopened by a continuation.
            candidates = List.of("receivable_payable_lookup");
        } else if (containsAll(message, "客户欠款", "供应商应付款")) {
            candidates = List.of("customer_receivable_lookup", "supplier_payable_lookup");
        } else {
            return List.of();
        }
        return candidates.stream().filter(this::isAllowedTool).toList();
    }

    /**
     * Recognizes only explicit two-source wording. Merely mentioning both
     * inventory and replenishment can still describe the single panorama
     * report, which must remain on inventory_panorama_lookup.
     */
    private boolean isExplicitInventoryRestockMultiSourceRequest(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return message.contains("库存和补货")
            || message.contains("库存与补货")
            || message.contains("库存、补货")
            || (message.contains("库存现状")
                && containsAny(message, "哪些要补", "补货建议"))
            || (message.contains("库存情况")
                && containsAny(message, "哪些要补", "补货建议"));
    }

    private boolean isExplicitReceivablePayableMultiSourceRequest(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return (message.contains("客户欠款") && message.contains("供应商应付款"))
            || (message.contains("客户欠我的") && message.contains("我欠供应商"))
            || (message.contains("应收") && message.contains("应付"));
    }

    private boolean isInventoryPanoramaRequest(String message) {
        return StringUtils.hasText(message)
            && !isExplicitInventoryRestockMultiSourceRequest(message)
            && containsAny(message, "库存全貌", "库存全景", "库存现状", "安全库存");
    }

    private boolean containsAll(String message, String... fragments) {
        for (String fragment : fragments) {
            if (!message.contains(fragment)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAny(String message, String... fragments) {
        for (String fragment : fragments) {
            if (message.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAmbiguousWriteRequest(String message) {
        if (!AgentPromptCatalog.hasWriteIntent(message)) {
            return false;
        }
        String target = AgentPromptCatalog.targetWriteTool(message);
        if ("create_purchase_order".equals(target) || "create_sale_order".equals(target)) {
            // A purchase/sale request naturally names both a partner and a
            // product. That is a dependency flow, not an ambiguous request.
            return false;
        }
        boolean mentionsProduct = message != null && message.contains("商品");
        boolean mentionsCustomer = message != null && message.contains("客户");
        boolean mentionsSupplier = message != null && message.contains("供应商");
        return (mentionsProduct && mentionsCustomer)
            || (mentionsProduct && mentionsSupplier)
            || (mentionsCustomer && mentionsSupplier);
    }

    private static CandidateRule rule(List<String> allOf, List<String> anyOf, String... toolNames) {
        return new CandidateRule(allOf, anyOf, List.of(toolNames));
    }

    private record CandidateRule(List<String> allOf, List<String> anyOf, List<String> toolNames) {
        private boolean matches(String message) {
            if (!StringUtils.hasText(message)) {
                return false;
            }
            boolean allMatch = allOf == null || allOf.stream().allMatch(message::contains);
            boolean anyMatch = anyOf == null || anyOf.isEmpty() || anyOf.stream().anyMatch(message::contains);
            return allMatch && anyMatch;
        }

        private int specificity() {
            int allOfScore = allOf == null ? 0 : allOf.size() * 100;
            int anyOfScore = anyOf == null ? 0 : anyOf.size();
            return allOfScore + anyOfScore;
        }
    }

    private List<String> focusedCandidateToolNamesForMessage(String message) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String writeTarget = AgentPromptCatalog.targetWriteTool(message);
        if (writeTarget != null) {
            switch (writeTarget) {
                case "create_account_transfer" -> candidates.add("account_balance_lookup");
                case "create_inventory_adjustment", "create_inventory_count_draft" -> {
                    candidates.add("product_catalog_lookup");
                    candidates.add("inventory_snapshot_lookup");
                }
                case "create_pay_order" -> {
                    candidates.add("supplier_directory_lookup");
                    candidates.add("purchase_order_lookup");
                }
                case "create_purchase_order" -> {
                    candidates.add("supplier_directory_lookup");
                    candidates.add("product_catalog_lookup");
                }
                case "create_purchase_receipt", "create_purchase_return" -> candidates.add("purchase_order_lookup");
                case "create_sale_order" -> {
                    candidates.add("customer_directory_lookup");
                    candidates.add("product_catalog_lookup");
                }
                case "create_sales_return" -> candidates.add("sale_order_lookup");
                default -> {
                    // The target create tool itself remains the model's choice.
                }
            }
            if ("create_customer".equals(writeTarget) && message != null && message.contains("分组")) {
                candidates.add("partner_group_lookup");
            }
            candidates.add(writeTarget);
        } else {
            String readTarget = AgentPromptCatalog.targetReadTool(message);
            if (readTarget != null) {
                candidates.add(readTarget);
            }
        }
        return candidates.stream()
            .filter(this::isAllowedTool)
            .toList();
    }

    private String buildToolCatalogForLlm(List<String> candidateToolNames) {
        if (candidateToolNames == null || candidateToolNames.isEmpty()) {
            return "";
        }
        Set<String> candidates = new LinkedHashSet<>(candidateToolNames);
        StringBuilder catalog = new StringBuilder();
        List<AgentTool> candidateTools = toolRegistry.listTools().stream()
            .filter(tool -> candidates.contains(tool.name()))
            .toList();
        return AgentPromptCatalog.buildCatalog(candidateTools, true);
    }

    private AgentToolPlan restrictPlanToCandidates(AgentToolPlan plan, List<String> candidateToolNames) {
        if (plan == null || candidateToolNames == null || candidateToolNames.isEmpty()) {
            return plan;
        }
        Set<String> candidates = new LinkedHashSet<>(candidateToolNames);
        List<String> tools = plan.tools().stream().filter(candidates::contains).toList();
        Map<String, JsonNode> params = new LinkedHashMap<>();
        for (String tool : tools) {
            JsonNode value = plan.toolParams().get(tool);
            if (value != null) {
                params.put(tool, value);
            }
        }
        List<NativeToolCallBlock> nativeBlocks = plan.nativeToolCallBlocks() == null
            ? List.of()
            : plan.nativeToolCallBlocks().stream()
                .filter(block -> block != null && candidates.contains(block.toolName()))
                .toList();
        return new AgentToolPlan(
            tools,
            plan.rationale(),
            plan.source(),
            params,
            plan.nativeResponseId(),
            nativeBlocks
        );
    }

    private Optional<AgentToolPlan> planNextIterationWithNativeFunctionCalling(
        String message,
        List<ToolExecutionResult> toolResults,
        int iteration,
        List<String> candidateToolNames,
        boolean hasRealDataQuery
    ) {
        if (candidateToolNames == null || candidateToolNames.isEmpty()) {
            return Optional.empty();
        }
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = buildNativeToolDefinitions(candidateToolNames);
        if (nativeTools.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder contextBuilder = new StringBuilder();
        if (toolResults != null) {
            for (ToolExecutionResult result : toolResults) {
                if (result == null) {
                    continue;
                }
                contextBuilder.append("- ").append(result.toolName()).append("：").append(result.summary()).append('\n');
                String factsJson = compactFacts(result.facts());
                if (!factsJson.isBlank()) {
                    contextBuilder.append("  真实工具结果 JSON：").append(factsJson).append('\n');
                }
            }
        }
        String systemPrompt = AgentPromptCatalog.reactSystemPrompt(
            AgentPromptCatalog.buildCatalog(
                candidateToolNames.stream()
                    .map(toolRegistry::getTool)
                    .flatMap(Optional::stream)
                    .toList(),
                true
            ),
            AgentPromptCatalog.hasWriteIntent(message),
            iteration,
            message
        ) + "如果现有事实足够回答且不需要展示，返回空工具调用。";
        String userPrompt = "用户问题：" + message + "\n"
            + (contextBuilder.isEmpty() ? "上一轮没有有效结果。\n" : "已执行工具及真实结果：\n" + contextBuilder)
            + "真实查询结果可用：" + hasRealDataQuery + "。请自主决定是否继续调用工具；如果需要，直接返回一个或多个最相关的原生工具调用。"
            + resolvedPayOrderParameterContext(message, toolResults);
        // Keep native auto selection for every production planner path. The
        // server may constrain the visible candidates, but must not choose the
        // next business tool on the model's behalf.
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, userPrompt, nativeTools);
        return toNativeToolPlan(
            message,
            List.of(),
            null,
            response,
            hasRealDataQuery,
            new LinkedHashSet<>(candidateToolNames),
            "native_tool_use_react"
        );
    }

    private List<LongCatAnthropicClient.ToolDefinition> buildInitialNativeToolDefinitions() {
        return buildNativeToolDefinitions().stream()
            .filter(definition -> !RESULT_VISUALIZATION_TOOL.equals(definition.name()))
            .toList();
    }

    private List<LongCatAnthropicClient.ToolDefinition> buildInitialNativeToolDefinitions(
        List<String> candidateToolNames
    ) {
        return buildNativeToolDefinitions(candidateToolNames);
    }

    private String buildInitialToolCatalogForLlm(String message) {
        List<String> availableToolNames = initialToolNamesForMessage(message);
        List<AgentTool> tools = availableToolNames.stream()
            .map(toolRegistry::getTool)
            .flatMap(Optional::stream)
            .toList();
        return AgentPromptCatalog.buildCatalog(tools, false);
    }

    /**
     * Tools visible during the initial model decision.
     *
     * <p>The registry type is the permission boundary. The model receives all
     * read-only tools for a read request, and create tools are added only when
     * the request has write intent. Keyword rules are reserved for a bounded
     * retry after a provider fails to emit a call; they must not silently route
     * the first request to a server-selected business tool.
     */
    private List<String> initialToolNamesForMessage(String message) {
        if (isSalesTrendAndReceivableRequest(message)) {
            return List.of("sales_trend_lookup", "payment_lookup").stream()
                .filter(this::isAllowedTool)
                .toList();
        }
        if (isExplicitReceivablePayableMultiSourceRequest(message)) {
            return List.of("receivable_payable_lookup").stream()
                .filter(this::isAllowedTool)
                .toList();
        }
        if (isExplicitInventoryRestockMultiSourceRequest(message)) {
            return List.of(
                "inventory_panorama_lookup",
                "smart_restock_lookup"
            ).stream().filter(this::isAllowedTool).toList();
        }
        if (isInventoryPanoramaRequest(message)) {
            return List.of("inventory_panorama_lookup").stream()
                .filter(this::isAllowedTool)
                .toList();
        }
        boolean writeIntent = AgentPromptCatalog.hasWriteIntent(message);
        if (writeIntent) {
            List<String> focused = focusedCandidateToolNamesForMessage(message);
            if (!focused.isEmpty() && !isAmbiguousWriteRequest(message)) {
                return focused.stream()
                    .filter(toolName -> !RESULT_VISUALIZATION_TOOL.equals(toolName))
                    .filter(this::isAllowedTool)
                    .toList();
            }
        }
        return toolRegistry.listTools().stream()
            .filter(tool -> tool.type() == AgentTool.ToolType.READ_ONLY
                || (writeIntent && tool.type() == AgentTool.ToolType.CREATE_ONLY))
            .map(AgentTool::name)
            .filter(toolName -> !RESULT_VISUALIZATION_TOOL.equals(toolName))
            .toList();
    }

    private boolean isSalesTrendAndReceivableRequest(String message) {
        return StringUtils.hasText(message)
            && message.contains("趋势")
            && message.contains("回款");
    }

    private List<String> remainingRequiredToolNames(String message, Set<String> attemptedToolNames) {
        if (!isExplicitInventoryRestockMultiSourceRequest(message)) {
            return List.of();
        }
        Set<String> attempted = attemptedToolNames == null ? Set.of() : attemptedToolNames;
        return List.of("inventory_panorama_lookup", "smart_restock_lookup").stream()
            .filter(this::isAllowedTool)
            .filter(toolName -> !attempted.contains(toolName))
            .toList();
    }

    /**
     * A payment draft has no product or purchase-order dependency. Once the
     * supplier directory has returned a real row, keeping purchase-order
     * lookup in the next model context makes the provider treat the request as
     * an unfinished procurement flow. Narrow only this resolved dependency
     * case; the model still decides whether to call the remaining tool.
     */
    private List<String> narrowResolvedPayOrderCandidates(
        String message,
        List<ToolExecutionResult> toolResults,
        List<String> candidateToolNames
    ) {
        String target = AgentPromptCatalog.targetWriteTool(message);
        if (!"create_pay_order".equals(target)
            || candidateToolNames == null
            || !candidateToolNames.contains(target)
            || !hasNonEmptyRealDataQuery(toolResults, "supplier_directory_lookup")) {
            return candidateToolNames;
        }
        return List.of(target);
    }

    /**
     * Makes the dependency-to-parameter mapping explicit without inventing
     * values. The supplier name/id comes only from the real directory result;
     * amount and remark remain sourced from the user's original request.
     */
    private String resolvedPayOrderParameterContext(
        String message,
        List<ToolExecutionResult> toolResults
    ) {
        if (!"create_pay_order".equals(AgentPromptCatalog.targetWriteTool(message))
            || !hasNonEmptyRealDataQuery(toolResults, "supplier_directory_lookup")) {
            return "";
        }
        String supplierContext = toolResults.stream()
            .filter(result -> result != null && "supplier_directory_lookup".equals(result.toolName()))
            .map(ToolExecutionResult::facts)
            .filter(facts -> facts != null && facts.isObject())
            .map(facts -> compactSupplierCandidates(facts.path("suppliers")))
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse("供应商目录已返回真实记录，请从真实结果中选择供应商");
        return "\n付款草稿参数上下文：supplier_directory_lookup 已返回真实供应商。"
            + "create_pay_order 的必填参数是 supplier_id、supplier_name 和 amount；account_id 是可选参数。"
            + "supplier_name 必须使用下列真实目录记录中的 name，supplier_id 只能使用对应真实 ID；"
            + "amount 和 remark 只能从用户原话读取，不要猜测或使用供应商余额代替付款金额。"
            + "真实供应商候选：" + supplierContext + "。"
            + "请继续由模型以 auto Function Calling 自主判断是否调用 create_pay_order。\n";
    }

    private String compactSupplierCandidates(JsonNode suppliers) {
        if (suppliers == null || !suppliers.isArray() || suppliers.isEmpty()) {
            return "[]";
        }
        ArrayNode candidates = objectMapper.createArrayNode();
        for (JsonNode supplier : suppliers) {
            if (supplier == null || !supplier.isObject()) {
                continue;
            }
            ObjectNode candidate = objectMapper.createObjectNode();
            if (supplier.has("supplier_id")) {
                candidate.set("supplier_id", supplier.get("supplier_id"));
            }
            if (supplier.has("name")) {
                candidate.set("name", supplier.get("name"));
            }
            if (!candidate.isEmpty()) {
                candidates.add(candidate);
            }
        }
        return candidates.toString();
    }

    private boolean hasNonEmptyRealDataQuery(List<ToolExecutionResult> toolResults) {
        if (toolResults == null) {
            return false;
        }
        return toolResults.stream().anyMatch(result -> {
            if (result == null || RESULT_VISUALIZATION_TOOL.equals(result.toolName())
                || result.facts() == null || !result.facts().isObject()) {
                return false;
            }
            JsonNode audit = result.facts().path("query_audit");
            return toolRegistry.getTool(result.toolName())
                .map(tool -> tool.type() == AgentTool.ToolType.READ_ONLY)
                .orElse(false)
                && audit.isObject()
                && audit.path("returned_count").asInt(0) > 0;
        });
    }

    private boolean hasNonEmptyRealDataQuery(
        List<ToolExecutionResult> toolResults,
        String toolName
    ) {
        if (toolResults == null || !StringUtils.hasText(toolName)) {
            return false;
        }
        return toolResults.stream().anyMatch(result -> {
            if (result == null || !toolName.equals(result.toolName())
                || result.facts() == null || !result.facts().isObject()) {
                return false;
            }
            JsonNode audit = result.facts().path("query_audit");
            return toolRegistry.getTool(result.toolName())
                .map(tool -> tool.type() == AgentTool.ToolType.READ_ONLY)
                .orElse(false)
                && audit.isObject()
                && audit.path("returned_count").asInt(0) > 0;
        });
    }

    private Optional<String> pendingRequiredWriteTarget(
        String message,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        boolean hasRealDataQuery
    ) {
        String target = AgentPromptCatalog.targetWriteTool(message);
        if (!StringUtils.hasText(target) || !hasRealDataQuery || hasCompletedTool(toolResults, target)) {
            return Optional.empty();
        }
        long requiredParameterFailures = toolFailures == null ? 0L : toolFailures.stream()
            .filter(failure -> failure != null
                && target.equals(failure.toolName())
                && failure.safeMessage() != null
                && failure.safeMessage().contains("必填参数缺失"))
            .count();
        return requiredParameterFailures == 1L && isAllowedTool(target)
            ? Optional.of(target)
            : Optional.empty();
    }

    private Optional<String> retryableWriteTarget(
        String message,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        boolean hasRealDataQuery,
        Optional<String> pendingRequiredTarget
    ) {
        if (pendingRequiredTarget.isPresent()) {
            return pendingRequiredTarget;
        }
        String target = AgentPromptCatalog.targetWriteTool(message);
        if (!StringUtils.hasText(target) || !hasRealDataQuery || hasCompletedTool(toolResults, target)) {
            return Optional.empty();
        }
        boolean hasNonParameterFailure = toolFailures != null && toolFailures.stream()
            .anyMatch(failure -> failure != null
                && target.equals(failure.toolName())
                && (failure.safeMessage() == null || !failure.safeMessage().contains("必填参数缺失")));
        return hasNonParameterFailure && isAllowedTool(target)
            ? Optional.of(target)
            : Optional.empty();
    }

    private String compactFacts(JsonNode facts) {
        if (facts == null || facts.isNull() || facts.isMissingNode()) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(facts);
            return json.length() <= 6000 ? json : json.substring(0, 6000) + "…";
        } catch (Exception ignored) {
            return "";
        }
    }

    public boolean isAllowedTool(String tool) {
        Optional<AgentTool> registered = toolRegistry.getTool(tool);
        if (registered.isPresent()) {
            return registered.get().type() == AgentTool.ToolType.READ_ONLY
                || registered.get().type() == AgentTool.ToolType.CREATE_ONLY;
        }
        return false;
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

}
