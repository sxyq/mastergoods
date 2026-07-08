package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.FinalAnswer;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ResponsePayload;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolFailureResult;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 答案合成组件。
 *
 * <p>承担原 {@code V2AgentAiService} 中的最终答案合成职责：
 * <ul>
 *   <li>非流式终答：{@link #buildFinalAnswer} —— 基于 {@link ResponsePayload} 与历史对话，
 *       规则化合成初稿后，由 {@link LongCatAnthropicClient} 精炼输出</li>
 *   <li>流式终答：{@link #buildFinalAnswerForStream} —— 通过 SSE 推送模型 delta，
 *       支持 LLM 不可用时的规则摘要降级</li>
 *   <li>规则化合成：{@link #synthesizeAnswer} —— 基于工具结果的字段化文本拼装，
 *       按工具名分派到对应的 fact* 取值逻辑</li>
 *   <li>查询边界/失败通知：{@link #appendQueryBoundaryNotice}、{@link #appendFailureNotice}、
 *       {@link #withRuleSummaryNotice} —— 在答案尾部追加可解释性提示</li>
 *   <li>历史加载：{@link #loadRecentHistory} —— 按会话读取最近 N 条消息（时间正序）</li>
 * </ul>
 *
 * <p>原属 {@code V2AgentAiService} 内联逻辑，因 God Class 拆分提取为独立组件。
 * 依赖 {@link LongCatAnthropicClient}（模型调用）、{@link SseStreamEmitter}（SSE 推送）、
 * {@link RunAuditService}（运行活跃检测）、{@link AgentMessageRepository}（历史读取）、
 * {@link ObjectMapper}（工具结果序列化）。
 *
 * <p>历史格式化复用 {@link ToolPlanner#formatHistoryContext}，避免与工具规划组件重复实现。
 */
@Component
public class AnswerSynthesizer {
    private static final String RULE_SUMMARY_NOTICE =
        "说明：以下为当前账号真实数据查询后的规则摘要；正式回答仅基于本轮查询返回的真实数据拼装，当前未使用模型生成或改写，以避免引入未查询或无关信息。";
    private static final String DETERMINISTIC_LLM_STATUS = "skipped_for_truthfulness";

    private final LongCatAnthropicClient longCatAnthropicClient;
    private final SseStreamEmitter sseStreamEmitter;
    private final RunAuditService runAuditService;
    private final AgentMessageRepository agentMessageRepository;
    private final ObjectMapper objectMapper;

    public AnswerSynthesizer(
        LongCatAnthropicClient longCatAnthropicClient,
        SseStreamEmitter sseStreamEmitter,
        RunAuditService runAuditService,
        AgentMessageRepository agentMessageRepository,
        ObjectMapper objectMapper
    ) {
        this.longCatAnthropicClient = longCatAnthropicClient;
        this.sseStreamEmitter = sseStreamEmitter;
        this.runAuditService = runAuditService;
        this.agentMessageRepository = agentMessageRepository;
        this.objectMapper = objectMapper;
    }

    public FinalAnswer buildFinalAnswer(String userMessage, ResponsePayload payload,
                                         List<AgentMessageEntity> history, String conversationSummary) {
        if (payload.toolFailures() != null && !payload.toolFailures().isEmpty()
            && (payload.toolResults() == null || payload.toolResults().isEmpty())) {
            return new FinalAnswer(appendFailureNotice(payload.answer(), payload.toolFailures()), "tool_query_failed", "not_requested", false);
        }
        if (payload.toolResults() == null || payload.toolResults().isEmpty()) {
            return new FinalAnswer(payload.answer(), "unsupported_intent", "not_requested", false);
        }
        List<ToolExecutionResult> effectiveToolResults = collapseToolResultsForPresentation(payload.toolResults());
        String synthesized = synthesizeAnswer(userMessage, effectiveToolResults, payload.answer());
        String synthesizedWithFailures = appendQueryBoundaryNotice(
            appendFailureNotice(synthesized, payload.toolFailures()),
            effectiveToolResults
        );
        return new FinalAnswer(
            withRuleSummaryNotice(synthesizedWithFailures),
            "tool_query_rule_summary",
            deterministicLlmStatus(),
            false
        );
    }

    public FinalAnswer buildFinalAnswerForStream(
        String userMessage,
        ResponsePayload payload,
        SseEmitter emitter,
        String runId,
        Runnable onFirstModelDelta,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (payload.toolFailures() != null && !payload.toolFailures().isEmpty()
            && (payload.toolResults() == null || payload.toolResults().isEmpty())) {
            String failedAnswer = appendFailureNotice(payload.answer(), payload.toolFailures());
            emitDeterministicAnswerDeltas(emitter, runId, failedAnswer, "rule_summary", onFirstModelDelta);
            return new FinalAnswer(failedAnswer, "tool_query_failed", "not_requested", false);
        }
        if (payload.toolResults() == null || payload.toolResults().isEmpty()) {
            emitDeterministicAnswerDeltas(emitter, runId, payload.answer(), "rule_summary", onFirstModelDelta);
            return new FinalAnswer(payload.answer(), "unsupported_intent", "not_requested", false);
        }
        List<ToolExecutionResult> effectiveToolResults = collapseToolResultsForPresentation(payload.toolResults());
        String synthesized = synthesizeAnswer(userMessage, effectiveToolResults, payload.answer());
        String synthesizedWithFailures = appendQueryBoundaryNotice(
            appendFailureNotice(synthesized, payload.toolFailures()),
            effectiveToolResults
        );
        String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
        emitDeterministicAnswerDeltas(emitter, runId, ruleSummaryAnswer, "rule_summary", onFirstModelDelta);
        return new FinalAnswer(
            ruleSummaryAnswer,
            "tool_query_rule_summary",
            deterministicLlmStatus(),
            false
        );
    }

    public String synthesizeAnswer(String userMessage, List<ToolExecutionResult> toolResults, String fallbackAnswer) {
        if (toolResults == null || toolResults.isEmpty()) {
            return fallbackAnswer;
        }
        List<String> findings = new ArrayList<>();
        Set<String> actions = new LinkedHashSet<>();

        for (ToolExecutionResult toolResult : toolResults) {
            switch (toolResult.toolName()) {
                case "inventory_low_stock_lookup" -> {
                    Integer count = factInt(toolResult, "low_stock_count");
                    String countText = factText(toolResult, "low_stock_count", "低库存商品数量");
                    findings.add(count != null && count == 0
                        ? "库存侧暂时没有发现低于安全库存的商品。"
                        : "库存侧共发现 " + countText + " 个低库存商品，需要优先补货。");
                    if (count != null && count > 0) {
                        actions.add("优先处理前 3 个低库存商品，避免影响接单和销售。");
                    }
                }
                case "product_catalog_lookup" -> {
                    String count = factText(toolResult, "product_count", "商品数量");
                    String stockTotal = factText(toolResult, "stock_total", "库存总计");
                    String lowStockCount = factText(toolResult, "low_stock_count", "低库存商品数量");
                    findings.add("商品侧商品总数 " + count + " 个，库存总计 " + stockTotal + "，低库存商品 " + lowStockCount + " 个。");
                }
                case "inventory_panorama_lookup" -> {
                    String productName = factText(toolResult, "product_name", "商品名称");
                    String currentStock = factText(toolResult, "current_stock", "当前库存");
                    String safeStock = factText(toolResult, "safe_stock", "安全库存");
                    String recentSalesQuantity = factText(toolResult, "recent_sales_quantity", "近30天销量");
                    String turnoverDays = factText(toolResult, "turnover_days", "周转天数");
                    String suggestedRestock = factText(toolResult, "suggested_restock", "建议补货量");
                    findings.add("商品「" + productName + "」当前库存 " + currentStock
                        + "，安全库存 " + safeStock
                        + "，近30天销量 " + recentSalesQuantity
                        + "，周转天数 " + turnoverDays
                        + "，建议补货量 " + suggestedRestock + "。");
                    actions.add("优先复核「" + productName + "」的安全库存和补货节奏。");
                }
                case "purchase_tracking_lookup" -> {
                    String orderNo = factText(toolResult, "order_no", "采购单号");
                    String supplierName = factText(toolResult, "supplier_name", "供应商");
                    String totalAmount = factText(toolResult, "total_amount", "采购总额");
                    String receivedAmount = factText(toolResult, "received_amount", "已到货");
                    String outstandingAmount = factText(toolResult, "outstanding_amount", "待付款");
                    String receiptCount = factText(toolResult, "receipt_count", "入库单数");
                    String returnCount = factText(toolResult, "return_count", "退货单数");
                    findings.add("采购单「" + orderNo + "」供应商为 " + supplierName
                        + "，采购总额 " + totalAmount
                        + "，已到货 " + receivedAmount
                        + "，待付款 " + outstandingAmount
                        + "；关联入库 " + receiptCount + " 条，退货 " + returnCount + " 条。");
                    actions.add("继续核对「" + orderNo + "」的到货、退货和付款闭环。");
                }
                case "account_health_lookup" -> {
                    String accountCount = factText(toolResult, "account_count", "账户总数");
                    String totalBalance = factText(toolResult, "total_balance", "账户总余额");
                    String ratio = factText(toolResult, "income_expense_ratio", "收支比");
                    String lowBalanceCount = factText(toolResult, "low_balance_count", "低余额账户数");
                    String transferCount = factText(toolResult, "transfer_count", "近期转账数");
                    String defaultAccountName = factText(toolResult, "default_account_name", "默认账户");
                    findings.add("账户侧共 " + accountCount + " 个账户，账户总余额 " + totalBalance
                        + "，收支比 " + ratio
                        + "，低余额账户 " + lowBalanceCount + " 个，近期转账 " + transferCount + " 条。");
                    actions.add("优先复核默认账户「" + defaultAccountName + "」及低余额账户的资金调度。");
                }
                case "customer_receivable_lookup" -> {
                    Integer count = factInt(toolResult, "customer_count");
                    String countText = factText(toolResult, "customer_count", "客户数量");
                    String total = factText(toolResult, "total_receivable", "应收总额");
                    findings.add(count != null && count == 0
                        ? "客户侧没有明显应收欠款压力。"
                        : "客户侧欠款客户总数 " + countText + " 个，应收总额 " + total + "。");
                    if (count != null && count > 0) {
                        actions.add("先跟进欠款最高的 2 到 3 位客户，缩短回款周期。");
                    }
                }
                case "customer_profile_lookup" -> {
                    String customerName = factText(toolResult, "customer_name", "客户名称");
                    String totalSalesAmount = factText(toolResult, "total_sales_amount", "累计销售额");
                    String balance = factText(toolResult, "balance", "当前欠款");
                    String paymentHabit = factText(toolResult, "payment_habit", "付款习惯");
                    String collectionSuggestion = factText(toolResult, "collection_suggestion", "催收建议");
                    findings.add("客户「" + customerName + "」累计销售额 " + totalSalesAmount
                        + "，当前欠款 " + balance + "，付款习惯偏" + paymentHabit + "。");
                    actions.add(collectionSuggestion);
                }
                case "supplier_payable_lookup" -> {
                    Integer count = factInt(toolResult, "supplier_count");
                    String countText = factText(toolResult, "supplier_count", "供应商数量");
                    String total = factText(toolResult, "total_payable", "应付总额");
                    findings.add(count != null && count == 0
                        ? "供应商侧暂时没有突出的应付压力。"
                        : "供应商侧应付供应商总数 " + countText + " 个，应付总额 " + total + "。");
                    if (count != null && count > 0) {
                        actions.add("结合回款节奏安排供应商付款，避免现金流过度前置。");
                    }
                }
                case "sales_overview_lookup" -> {
                    String salesAmount = factText(toolResult, "sales_amount", "销售额");
                    String paidAmount = factText(toolResult, "paid_amount", "回款金额");
                    String salesCount = factText(toolResult, "sales_count", "销售笔数");
                    findings.add("近 7 天销售 " + salesCount + " 笔，销售额 " + salesAmount + "，回款 " + paidAmount + "。");
                }
                case "sale_order_lookup" -> {
                    String count = factText(toolResult, "order_count", "销售单数量");
                    String unpaidCount = factText(toolResult, "unpaid_count", "未收清数量");
                    String total = factText(toolResult, "recent_total_amount", "销售额");
                    String firstOrderNo = factArrayItemText(toolResult, "recent_orders", 0, "order_no");
                    String firstCustomerName = factArrayItemText(toolResult, "recent_orders", 0, "customer_name");
                    String leadOrder = StringUtils.hasText(firstOrderNo)
                        ? "，例如订单 " + firstOrderNo
                            + (StringUtils.hasText(firstCustomerName) ? "（" + firstCustomerName + "）" : "")
                        : "";
                    findings.add("销售单侧最近查询 " + count + " 条，查询销售额 " + total + "，未收清 " + unpaidCount + " 条" + leadOrder + "。");
                }
                case "purchase_order_lookup" -> {
                    String count = factText(toolResult, "order_count", "采购单数量");
                    String total = factText(toolResult, "recent_total_amount", "采购额");
                    findings.add("采购单侧最近查询 " + count + " 条，采购额 " + total + "。");
                }
                case "pay_order_lookup" -> {
                    String count = factText(toolResult, "pay_order_count", "付款单数量");
                    String pendingCount = factText(toolResult, "pending_count", "待付款数量");
                    String total = factText(toolResult, "recent_total_amount", "付款额");
                    findings.add("付款单侧最近查询 " + count + " 条，付款额 " + total + "，待付款 " + pendingCount + " 条。");
                }
                case "finance_record_lookup" -> {
                    String income = factText(toolResult, "recent_income", "收入");
                    String expense = factText(toolResult, "recent_expense", "支出");
                    String count = factText(toolResult, "record_count", "资金流水数量");
                    findings.add("资金流水侧最近查询 " + count + " 条，收入 " + income + "，支出 " + expense + "。");
                }
                case "result_visualization" -> {
                    // 展示决策工具不代表业务事实，不参与正式回答的数据总结。
                }
                default -> findings.add(toolResult.summary());
            }
        }

        if (findings.isEmpty()) {
            return fallbackAnswer;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("针对“").append(userMessage.trim()).append("”，我已基于当前登录账号下的真实经营数据完成查询。");
        builder.append("\n");
        for (int i = 0; i < findings.size(); i++) {
            builder.append(i + 1).append(". ").append(findings.get(i));
            if (i < findings.size() - 1) {
                builder.append("\n");
            }
        }

        List<String> dedupedActions = new ArrayList<>(Math.min(actions.size(), 3));
        for (String action : actions) {
            dedupedActions.add(action);
            if (dedupedActions.size() >= 3) {
                break;
            }
        }
        if (!dedupedActions.isEmpty()) {
            builder.append("\n建议：");
            for (int i = 0; i < dedupedActions.size(); i++) {
                builder.append("\n").append(i + 1).append(". ").append(dedupedActions.get(i));
            }
        }
        return builder.toString();
    }

    public String finalAnswerSystemPrompt(String conversationSummary) {
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "\n会话摘要：" + conversationSummary + "\n"
            : "";
        return """
            你是智慧记的 agentic AI 助手。你不能编造数据，只能基于服务端白名单工具返回的事实回答。
            回答要求：
            1. 先直接回答用户问题。
            2. 明确引用本轮查询到的关键数据。
            3. 给出 1-3 条可执行建议。
            4. 如果有工具查询失败，必须明确说明哪些查询失败，且不要用模拟数据替代。
            5. 不要输出 Markdown 表格；不要声称查询了未列出的数据。
            6. 结合历史对话上下文理解用户指代（如"他""刚才那个"等）。
            """ + summaryContext;
    }

    public String finalAnswerUserPrompt(String userMessage, ResponsePayload payload,
                                         String synthesizedWithFailures, List<AgentMessageEntity> history) {
        String historyContext = ToolPlanner.formatHistoryContext(history);
        return historyContext
            + "用户问题：" + userMessage + "\n"
            + "已执行工具结果 JSON：" + serializeToolResults(payload.toolResults()) + "\n"
            + "失败工具 JSON：" + serializeToolFailures(payload.toolFailures()) + "\n"
            + "基于事实的初稿：" + synthesizedWithFailures;
    }

    public List<AgentMessageEntity> loadRecentHistory(Long ownerUserId, Long conversationId, int limit) {
        if (conversationId == null || limit <= 0) {
            return List.of();
        }
        List<AgentMessageEntity> desc = agentMessageRepository
            .findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
                ownerUserId, conversationId, PageRequest.of(0, limit)
            );
        return desc.reversed();
    }

    public String serializeToolResults(List<ToolExecutionResult> toolResults) {
        try {
            return objectMapper.writeValueAsString(toolResults);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public String serializeToolFailures(List<ToolFailureResult> toolFailures) {
        try {
            return objectMapper.writeValueAsString(toolFailures == null ? List.of() : toolFailures);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public String appendFailureNotice(String answer, List<ToolFailureResult> toolFailures) {
        if (toolFailures == null || toolFailures.isEmpty()) {
            return answer;
        }
        String notice = formatFailureNotice(toolFailures);
        if (StringUtils.hasText(answer) && answer.contains(notice)) {
            return answer;
        }
        return (StringUtils.hasText(answer) ? answer.trim() + "\n" : "") + notice;
    }

    public String appendQueryBoundaryNotice(String answer, List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return answer;
        }
        String normalized = StringUtils.hasText(answer) ? answer.trim() : "";
        if (normalized.contains("查询边界：")) {
            return normalized;
        }
        Set<String> notices = new LinkedHashSet<>();
        for (ToolExecutionResult result : toolResults) {
            for (String notice : queryBoundaryNotices(result)) {
                notices.add(notice);
            }
        }
        if (notices.isEmpty()) {
            return normalized;
        }
        return normalized + "\n查询边界：" + String.join("；", notices) + "。";
    }

    public String withRuleSummaryNotice(String answer) {
        String normalized = StringUtils.hasText(answer) ? answer.trim() : "";
        if (normalized.startsWith(RULE_SUMMARY_NOTICE)) {
            return normalized;
        }
        return RULE_SUMMARY_NOTICE + "\n\n" + normalized;
    }

    public String deterministicLlmStatus() {
        return longCatAnthropicClient.isConfigured()
            ? DETERMINISTIC_LLM_STATUS
            : longCatAnthropicClient.configurationStatus();
    }

    public void emitDeterministicAnswerDeltas(
        SseEmitter emitter,
        String runId,
        String answer,
        String deltaSource,
        Runnable onFirstVisibleDelta
    ) {
        if (emitter == null || runId == null || !StringUtils.hasText(answer)) {
            return;
        }
        boolean emittedVisibleDelta = false;
        for (String chunk : chunkAnswerForVisibleStream(answer)) {
            sseStreamEmitter.emitAnswerDeltaUnchecked(emitter, runId, chunk, deltaSource);
            if (!emittedVisibleDelta) {
                emittedVisibleDelta = true;
                onFirstVisibleDelta.run();
            }
        }
    }

    public List<String> chunkAnswerForVisibleStream(String answer) {
        if (!StringUtils.hasText(answer)) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < answer.length(); index++) {
            char ch = answer.charAt(index);
            current.append(ch);
            boolean hardBreak = ch == '\n' && current.length() > 0;
            boolean naturalBreak = "。！？；;!?".indexOf(ch) >= 0;
            boolean softLimitReached = current.length() >= 48 && Character.isWhitespace(ch);
            boolean maxLimitReached = current.length() >= 72;
            if (hardBreak || naturalBreak || softLimitReached || maxLimitReached) {
                addChunk(chunks, current);
            }
        }
        addChunk(chunks, current);
        return chunks;
    }

    public String emitServerNoticeTailIfNeeded(
        SseEmitter emitter,
        String runId,
        String streamedAnswer,
        String finalAnswer
    ) {
        if (!StringUtils.hasText(streamedAnswer) || !StringUtils.hasText(finalAnswer)) {
            return finalAnswer;
        }
        String visibleAnswer = streamedAnswer.trim();
        String normalizedFinalAnswer = finalAnswer.trim();
        if (normalizedFinalAnswer.length() <= visibleAnswer.length() || !normalizedFinalAnswer.startsWith(visibleAnswer)) {
            return finalAnswer;
        }
        String serverNoticeTail = normalizedFinalAnswer.substring(visibleAnswer.length());
        if (StringUtils.hasText(serverNoticeTail)) {
            sseStreamEmitter.emitAnswerDeltaUnchecked(emitter, runId, serverNoticeTail, "server_notice");
        }
        return finalAnswer;
    }

    public FinalAnswer streamFallbackFinalAnswer(
        SseEmitter emitter,
        String runId,
        StringBuilder streamedAnswer,
        String synthesizedWithFailures,
        ResponsePayload payload,
        Runnable onFirstVisibleDelta
    ) {
        if (streamedAnswer != null && StringUtils.hasText(streamedAnswer.toString())) {
            String partialAnswer = appendFailureNotice(streamedAnswer.toString().trim(), payload.toolFailures());
            return new FinalAnswer(partialAnswer, "tool_query_llm_stream_interrupted", "stream_interrupted", true);
        }
        String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
        emitDeterministicAnswerDeltas(emitter, runId, ruleSummaryAnswer, "rule_summary", onFirstVisibleDelta);
        return new FinalAnswer(ruleSummaryAnswer, "tool_query_rule_summary", "stream_failed_or_empty", true);
    }

    public String factText(ToolExecutionResult toolResult, String fieldName, String displayName) {
        if (toolResult == null || toolResult.facts() == null || !toolResult.facts().has(fieldName) || toolResult.facts().path(fieldName).isNull()) {
            return "后端未返回" + displayName;
        }
        String value = toolResult.facts().path(fieldName).asText();
        return StringUtils.hasText(value) ? value : "后端未返回" + displayName;
    }

    public Integer factInt(ToolExecutionResult toolResult, String fieldName) {
        if (toolResult == null || toolResult.facts() == null || !toolResult.facts().has(fieldName) || !toolResult.facts().path(fieldName).canConvertToInt()) {
            return null;
        }
        return Math.max(0, toolResult.facts().path(fieldName).asInt());
    }

    public String factArrayItemText(ToolExecutionResult toolResult, String arrayFieldName, int index, String childFieldName) {
        if (toolResult == null || toolResult.facts() == null) {
            return null;
        }
        JsonNode arrayNode = toolResult.facts().path(arrayFieldName);
        if (!arrayNode.isArray() || index < 0 || index >= arrayNode.size()) {
            return null;
        }
        JsonNode childNode = arrayNode.path(index).path(childFieldName);
        if (childNode.isMissingNode() || childNode.isNull()) {
            return null;
        }
        String value = childNode.asText();
        return StringUtils.hasText(value) ? value : null;
    }

    public List<ToolExecutionResult> collapseToolResultsForPresentation(List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return List.of();
        }
        Map<String, ToolExecutionResult> collapsed = new LinkedHashMap<>();
        for (ToolExecutionResult result : toolResults) {
            if (result == null) {
                continue;
            }
            collapsed.remove(result.toolName());
            collapsed.put(result.toolName(), result);
        }
        return new ArrayList<>(collapsed.values());
    }

    private void addChunk(List<String> chunks, StringBuilder current) {
        String chunk = current.toString();
        current.setLength(0);
        if (!StringUtils.hasText(chunk)) {
            return;
        }
        chunks.add(chunk);
    }

    private String formatFailureNotice(List<ToolFailureResult> toolFailures) {
        StringBuilder builder = new StringBuilder("部分查询失败：");
        for (int i = 0; i < toolFailures.size(); i++) {
            ToolFailureResult failure = toolFailures.get(i);
            if (i > 0) {
                builder.append("；");
            }
            builder.append(failure.toolName()).append("（").append(failure.safeMessage()).append("）");
        }
        builder.append("。失败部分未使用模拟数据替代。");
        return builder.toString();
    }

    private List<String> queryBoundaryNotices(ToolExecutionResult result) {
        if (result == null) {
            return List.of();
        }
        if ("result_visualization".equals(result.toolName())) {
            return List.of();
        }
        List<String> notices = new ArrayList<>();
        Map<String, Object> audit = result.queryAudit();
        Integer limit = asInteger(audit.get("limit"));
        Integer returnedCount = asInteger(audit.get("returned_count"));
        Integer totalCount = asInteger(audit.get("total_count"));
        Boolean truncated = asBoolean(audit.get("is_truncated"));
        String label = toolDisplayName(result.toolName());
        if (Boolean.TRUE.equals(truncated)) {
            String totalText = totalCount == null ? "" : " / 已知 " + totalCount + " 条";
            notices.add(label + "仅返回前 " + safeInt(returnedCount) + totalText + " 条，不能视为全量结论");
        } else if (limit != null && returnedCount != null) {
            notices.add(label + "最多查询 " + limit + " 条，实际返回 " + returnedCount + " 条");
        }
        long windowDays = result.facts() == null ? 0L : result.facts().path("window_days").asLong(0L);
        if (windowDays > 0L) {
            notices.add(label + "窗口为近 " + windowDays + " 天");
        }
        return notices;
    }

    private String toolDisplayName(String toolName) {
        return switch (safeText(toolName, "")) {
            case "inventory_low_stock_lookup" -> "低库存查询";
            case "product_catalog_lookup" -> "商品查询";
            case "customer_receivable_lookup" -> "客户应收查询";
            case "supplier_payable_lookup" -> "供应商应付查询";
            case "sales_overview_lookup" -> "经营概览查询";
            case "sale_order_lookup" -> "销售单查询";
            case "purchase_order_lookup" -> "采购单查询";
            case "pay_order_lookup" -> "付款单查询";
            case "finance_record_lookup" -> "资金流水查询";
            default -> safeText(toolName, "工具查询");
        };
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return null;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
