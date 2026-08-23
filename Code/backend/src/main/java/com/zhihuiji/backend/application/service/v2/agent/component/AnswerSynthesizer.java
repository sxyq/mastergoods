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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent 答案合成组件。
 *
 * <p>承担原 {@code V2AgentAiService} 中的最终答案合成职责：
 * <ul>
 *   <li>非流式终答：{@link #buildFinalAnswer} —— 基于 {@link ResponsePayload} 与历史对话，
 *       将真实工具 facts 交给 {@link LongCatAnthropicClient} 生成最终回答</li>
 *   <li>流式终答：{@link #buildFinalAnswerForStream} —— 通过 SSE 推送经过事实校验的模型结果</li>
 *   <li>事实校验：模型只能引用本轮工具 facts，无法通过校验时不生成替代性答案</li>
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
    private static final Logger log = LoggerFactory.getLogger(AnswerSynthesizer.class);
    private static final Pattern NUMBER_TOKEN = Pattern.compile(
        "-?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?"
    );
    private static final Pattern UNSUPPORTED_WRITE_CLAIM = Pattern.compile(
        "(?:已|已经|成功|我已|我已经|好的[，, ]*)\\s*"
            + "[^。！？\\n]{0,24}?"
            + "(?:生成|创建|新增|保存|登记|下单)(?!了?(?:表格|图表|报表|清单|数据表|趋势图|统计图|折线图|柱状图|饼图|可视化|统计卡))"
            + "|(?:已|已经|成功|我已|我已经)\\s*(?:为您|帮您)?\\s*"
            + "(?:完成|执行|办理|成功)\\s*(?:付款|转账|入库|退货|记账)"
            + "|(?:付款|转账|入库|退货|记账)\\s*(?:已|已经)?\\s*(?:成功|完成)"
    );
    private static final Pattern READ_ONLY_CONTEXT = Pattern.compile(
        "查询|查询结果|查询到|信息|明细|记录|流程|链路|关联|情况|详情|列表|统计|查看|返回|结果"
            + "|到货|货品|状态|待付款|已付|正常|全部|没有|无|未"
    );

    private final LongCatAnthropicClient longCatAnthropicClient;
    private final SseStreamEmitter sseStreamEmitter;
    private final RunAuditService runAuditService;
    private final AgentMessageRepository agentMessageRepository;
    private final ObjectMapper objectMapper;
    private final ToolPlanner toolPlanner;

    public AnswerSynthesizer(
        LongCatAnthropicClient longCatAnthropicClient,
        SseStreamEmitter sseStreamEmitter,
        RunAuditService runAuditService,
        AgentMessageRepository agentMessageRepository,
        ObjectMapper objectMapper,
        ToolPlanner toolPlanner
    ) {
        this.longCatAnthropicClient = longCatAnthropicClient;
        this.sseStreamEmitter = sseStreamEmitter;
        this.runAuditService = runAuditService;
        this.agentMessageRepository = agentMessageRepository;
        this.objectMapper = objectMapper;
        this.toolPlanner = toolPlanner;
    }

    public FinalAnswer buildFinalAnswer(String userMessage, ResponsePayload payload,
                                         List<AgentMessageEntity> history, String conversationSummary) {
        if (!longCatAnthropicClient.isConfigured()) {
            return new FinalAnswer("", "llm_required", longCatAnthropicClient.configurationStatus(), false);
        }
        if (payload != null && payload.plan() != null
            && "model_tool_selection_failed".equals(payload.plan().source())) {
            return new FinalAnswer("", "llm_answer_unavailable", "model_tool_selection_failed", true);
        }
        // 优先尝试原生 function_call_output 续轮，让模型直接读取工具结果生成正文。
        // provider 不支持续轮时，重新向同一个模型提交原始工具结果。
        Optional<String> nativeContinuationAnswer = attemptNativeContinuation(userMessage, payload);
        if (nativeContinuationAnswer.isPresent()) {
            return new FinalAnswer(nativeContinuationAnswer.get(), "tool_query_llm_native", "native_continuation_completed", true);
        }
        String userPrompt = finalAnswerUserPrompt(userMessage, payload, history, conversationSummary);
        Optional<String> firstModelAnswer = longCatAnthropicClient.createJsonMessage(
            finalAnswerSystemPrompt(),
            userPrompt
        );
        Optional<String> modelAnswer = firstModelAnswer
            .flatMap(answer -> validateModelAnswer(answer, payload, userMessage));
        if (modelAnswer.isPresent()) {
            return new FinalAnswer(modelAnswer.get(), "tool_query_llm", "completed", true);
        }
        // 空响应也必须进入一次真实模型重试。此前只有首轮返回非空时才重试，
        // provider 的短暂空响应会直接变成用户可见的失败，且没有任何模板回答可用。
        Optional<String> retryAnswer = longCatAnthropicClient.createJsonMessage(
            finalAnswerRetrySystemPrompt(),
            userPrompt
        ).flatMap(answer -> validateModelAnswer(answer, payload, userMessage));
        if (retryAnswer.isPresent()) {
            return new FinalAnswer(retryAnswer.get(), "tool_query_llm", "validated_retry", true);
        }
        Optional<String> groundedFallback = firstModelAnswer.isEmpty()
            ? groundedFallbackAnswer(payload)
            : Optional.empty();
        if (groundedFallback.isPresent()) {
            return new FinalAnswer(groundedFallback.get(), "tool_query_grounded_fallback", "facts_fallback", false);
        }
        return new FinalAnswer("", "llm_answer_unavailable", "model_empty_or_ungrounded", true);
    }

    /**
     * Keep a completed read useful when a compatible provider returns two empty
     * answer attempts. The text is made only from the persisted tool summary
     * for a non-empty real query; it never invents business values or claims a
     * write succeeded.
     */
    private Optional<String> groundedFallbackAnswer(ResponsePayload payload) {
        if (payload == null || payload.toolResults() == null || payload.toolResults().isEmpty()) {
            return Optional.empty();
        }
        List<String> lines = new ArrayList<>();
        for (ToolExecutionResult result : payload.toolResults()) {
            if (result == null || !StringUtils.hasText(result.toolName())) {
                continue;
            }
            JsonNode audit = result.facts() == null ? null : result.facts().path("query_audit");
            if (audit == null || !audit.isObject() || audit.path("returned_count").asInt(0) <= 0) {
                continue;
            }
            String summary = StringUtils.hasText(result.summary()) ? result.summary().trim() : "工具已完成";
            lines.add(summary);
        }
        return lines.isEmpty()
            ? Optional.empty()
            : Optional.of("查询已完成，以下为工具返回的真实结果：\n" + String.join("\n", lines));
    }

    /**
     * 尝试原生 function_call_output 续轮：将服务端执行的真实工具结果以 provider 要求的格式回传给模型。
     *
     * <p>仅当 plan 来自原生 Function Calling（nativeResponseId + nativeToolCallBlocks 可用）时尝试。
     * provider 不支持该续轮格式时（如 gpt-5.6-luna 返回 HTTP 400），方法返回 empty，
     * 由 {@link #buildFinalAnswer} 重新请求同一个模型的自然语言回答。
     *
     * <p>不得伪造续轮成功：provider 返回 empty 或模型回答未通过 fact guard 时返回 empty。
     *
     * @param userMessage 原始用户问题
     * @param payload     包含工具执行结果和原生调用块的响应负载
     * @return 模型基于真实 facts 生成的最终回答 Optional；provider 不支持时为 empty
     */
    private Optional<String> attemptNativeContinuation(String userMessage, ResponsePayload payload) {
        String responseId = payload.nativeResponseId();
        List<AgentTypes.NativeToolCallBlock> nativeBlocks = nativeToolCallTranscript(payload);
        if (!StringUtils.hasText(responseId)
            && !longCatAnthropicClient.supportsToolResultContinuation()) {
            return Optional.empty();
        }
        if (nativeBlocks == null || nativeBlocks.isEmpty()) {
            return Optional.empty();
        }
        // 构建 function_call 项（模型上一轮返回的工具调用）
        List<LongCatAnthropicClient.FunctionCallItem> functionCalls = new ArrayList<>();
        for (AgentTypes.NativeToolCallBlock block : nativeBlocks) {
            if (block == null || block.toolCallId() == null || block.toolName() == null) {
                continue;
            }
            functionCalls.add(new LongCatAnthropicClient.FunctionCallItem(
                block.toolCallId(),
                block.toolName(),
                block.arguments() != null ? block.arguments() : "{}"
            ));
        }
        if (functionCalls.isEmpty()) {
            return Optional.empty();
        }
        // 构建 function_call_output 项（服务端执行的真实结果，按 call_id 关联）。
        // 旧数据没有 call_id 时才按同名工具的出现顺序兼容匹配。
        List<LongCatAnthropicClient.FunctionCallOutputItem> toolOutputs = new ArrayList<>();
        for (AgentTypes.NativeToolCallBlock block : nativeBlocks) {
            String callId = block.toolCallId();
            String toolName = block.toolName();
            Optional<ToolExecutionResult> matchingResult = matchingToolResult(
                payload.toolResults(), toolName, callId
            );
            String outputJson;
            if (matchingResult.isPresent() && matchingResult.get().facts() != null) {
                try {
                    outputJson = objectMapper.writeValueAsString(matchingResult.get().facts());
                } catch (Exception ignored) {
                    outputJson = "{}";
                }
            } else if (matchingFailure(payload.toolFailures(), toolName, callId).isPresent()) {
                AgentTypes.ToolFailureResult failure = matchingFailure(payload.toolFailures(), toolName, callId).get();
                outputJson = toToolFailureOutput(failure);
            } else {
                // Every native call must have a corresponding output. Keep the
                // output explicit instead of silently pretending that no result
                // was produced.
                outputJson = "{}";
            }
            toolOutputs.add(new LongCatAnthropicClient.FunctionCallOutputItem(callId, outputJson));
        }
        // 构建工具定义列表（保持与首轮一致）
        List<LongCatAnthropicClient.ToolDefinition> tools = toolPlanner.buildNativeToolDefinitions(
            nativeBlocks.stream()
                .map(AgentTypes.NativeToolCallBlock::toolName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList()
        );
        if (tools.isEmpty()) {
            return Optional.empty();
        }
        String systemPrompt = finalAnswerSystemPrompt();
        Optional<LongCatAnthropicClient.ToolUseResponse> continuationResponse =
            longCatAnthropicClient.continueWithToolOutputs(
                responseId,
                systemPrompt,
                userMessage,
                functionCalls,
                toolOutputs,
                tools
            );
        if (continuationResponse.isEmpty()) {
            // provider 不支持续轮或返回空，交给主回答路径重新请求模型。
            return Optional.empty();
        }
        // A continuation that still asks for tools belongs to the ReAct planner,
        // not the final-answer path. The planner will execute the next calls and
        // the final answer will be attempted again with the accumulated facts.
        if (continuationResponse.get().hasToolUses()) {
            return Optional.empty();
        }
        String modelAnswer = continuationResponse.get().text();
        if (modelAnswer == null || modelAnswer.isBlank()) {
            return Optional.empty();
        }
        // 只校验模型正文中的事实，不替模型补写或改写正文。
        return validateModelAnswer(modelAnswer, payload, userMessage);
    }

    private Optional<ToolExecutionResult> matchingToolResult(
        List<ToolExecutionResult> toolResults,
        String toolName,
        String toolCallId
    ) {
        if (toolResults == null || !StringUtils.hasText(toolName)) {
            return Optional.empty();
        }
        if (StringUtils.hasText(toolCallId)) {
            for (ToolExecutionResult result : toolResults) {
                if (result != null
                    && toolName.equals(result.toolName())
                    && toolCallId.equals(result.toolCallId())) {
                    return Optional.of(result);
                }
            }
        }
        for (ToolExecutionResult result : toolResults) {
            if (result == null || !toolName.equals(result.toolName())) {
                continue;
            }
            if (!StringUtils.hasText(toolCallId) && !StringUtils.hasText(result.toolCallId())) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    private Optional<AgentTypes.ToolFailureResult> matchingFailure(
        List<AgentTypes.ToolFailureResult> failures,
        String toolName,
        String toolCallId
    ) {
        if (failures == null || !StringUtils.hasText(toolName)) {
            return Optional.empty();
        }
        return failures.stream()
            .filter(failure -> failure != null && toolName.equals(failure.toolName()))
            .filter(failure -> !StringUtils.hasText(toolCallId)
                || toolCallId.equals(failure.toolCallId()))
            .findFirst();
    }

    private String toToolFailureOutput(AgentTypes.ToolFailureResult failure) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "error", failure.safeMessage() == null ? "工具执行失败" : failure.safeMessage(),
                "tool_name", failure.toolName() == null ? "unknown" : failure.toolName()
            ));
        } catch (Exception ignored) {
            return "{\"error\":\"工具执行失败\"}";
        }
    }

    /**
     * Rebuilds the complete native tool-call transcript across ReAct rounds.
     * Keeping only the last round loses the assistant/tool message pairing that
     * Chat Completions providers use to decide whether they can answer.
     */
    private List<AgentTypes.NativeToolCallBlock> nativeToolCallTranscript(ResponsePayload payload) {
        List<AgentTypes.NativeToolCallBlock> declared = payload.nativeToolCallBlocks();
        if (declared == null || declared.isEmpty()) {
            return List.of();
        }
        Map<String, AgentTypes.NativeToolCallBlock> byCallId = new LinkedHashMap<>();
        for (AgentTypes.NativeToolCallBlock block : declared) {
            if (block != null && StringUtils.hasText(block.toolCallId())) {
                byCallId.putIfAbsent(block.toolCallId(), block);
            }
        }
        if (payload.toolResults() != null) {
            for (ToolExecutionResult result : payload.toolResults()) {
                if (result != null && StringUtils.hasText(result.toolCallId())
                    && StringUtils.hasText(result.toolName())) {
                    byCallId.putIfAbsent(result.toolCallId(), new AgentTypes.NativeToolCallBlock(
                        result.toolCallId(), result.toolName(), "{}"
                    ));
                }
            }
        }
        if (payload.toolFailures() != null) {
            for (AgentTypes.ToolFailureResult failure : payload.toolFailures()) {
                if (failure != null && StringUtils.hasText(failure.toolCallId())
                    && StringUtils.hasText(failure.toolName())) {
                    byCallId.putIfAbsent(failure.toolCallId(), new AgentTypes.NativeToolCallBlock(
                        failure.toolCallId(), failure.toolName(), "{}"
                    ));
                }
            }
        }
        return new ArrayList<>(byCallId.values());
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
        if (!longCatAnthropicClient.isConfigured() || !longCatAnthropicClient.supportsStreaming()) {
            return new FinalAnswer(
                "",
                "llm_required",
                longCatAnthropicClient.streamingUnavailableStatus(),
                false
            );
        }
        if (payload != null && payload.plan() != null
            && "model_tool_selection_failed".equals(payload.plan().source())) {
            return new FinalAnswer("", "llm_answer_unavailable", "model_tool_selection_failed", true);
        }

        // Streaming and non-streaming answers share the same native continuation.
        // Only the fact-guarded final text is exposed as SSE.
        Optional<String> nativeContinuationAnswer = attemptNativeContinuation(userMessage, payload);
        if (nativeContinuationAnswer.isPresent()) {
            String answer = nativeContinuationAnswer.get();
            runAuditService.ensureRunActive(runId);
            emitModelAnswerDeltas(
                emitter,
                runId,
                answer,
                "model_native_continuation",
                onFirstModelDelta
            );
            return new FinalAnswer(
                answer,
                "tool_query_llm_native",
                "native_continuation_completed",
                true
            );
        }

        String systemPrompt = finalAnswerSystemPrompt();
        String userPrompt = finalAnswerUserPrompt(userMessage, payload, history, conversationSummary);
        StringBuilder providerAnswer = new StringBuilder();
        Optional<String> streamedAnswer = longCatAnthropicClient.streamTextMessage(
            systemPrompt,
            userPrompt,
            runId,
            delta -> {
                runAuditService.ensureRunActive(runId);
                providerAnswer.append(delta);
            }
        );
        runAuditService.ensureRunActive(runId);
        String candidate = streamedAnswer.orElse(providerAnswer.toString());
        Optional<String> renderedStreamedAnswer = validateModelAnswer(candidate, payload, userMessage);
        if (renderedStreamedAnswer.isPresent()) {
            String answer = renderedStreamedAnswer.get();
            // Do not expose provider deltas until the complete response passes the fact guard.
            emitModelAnswerDeltas(emitter, runId, answer, "model_stream", onFirstModelDelta);
            return new FinalAnswer(answer, "tool_query_llm_streamed", "completed", true);
        }

        runAuditService.ensureRunActive(runId);
        Optional<String> retry = longCatAnthropicClient.createJsonMessage(finalAnswerRetrySystemPrompt(), userPrompt)
            .flatMap(answer -> validateModelAnswer(answer, payload, userMessage));
        runAuditService.ensureRunActive(runId);
        if (retry.isPresent()) {
            String answer = retry.get();
            emitModelAnswerDeltas(emitter, runId, answer, "non_stream_retry", onFirstModelDelta);
            return new FinalAnswer(answer, "tool_query_llm", "non_stream_retry", true);
        }

        Optional<String> groundedFallback = streamedAnswer.isEmpty()
            ? groundedFallbackAnswer(payload)
            : Optional.empty();
        if (groundedFallback.isPresent()) {
            String answer = groundedFallback.get();
            emitModelAnswerDeltas(emitter, runId, answer, "facts_fallback", onFirstModelDelta);
            return new FinalAnswer(answer, "tool_query_grounded_fallback", "facts_fallback", false);
        }

        return new FinalAnswer("", "llm_answer_unavailable", "model_empty_or_ungrounded", true);
    }

    public String finalAnswerSystemPrompt() {
        return """
            你是智慧记的正式回答生成器。请直接返回面向用户的自然语言正文，不要返回 JSON 信封、固定模板、占位符、内部字段或工具调用说明。
            本轮工具结果是业务事实的唯一数据来源。只能根据这些结果回答；不得编造、估算、补全、重算或推测任何业务数据。
            所有业务数字必须原样出现在本轮工具结果或工具摘要中；不要计算差额、比例、合计或新增数字。需要分点时使用项目符号，不要用 1.、2. 这类数字编号。
            只输出普通段落或项目符号，不要输出 JSON、代码块、HTML 或 Markdown 表格。
            可以按用户问题组织简短解释、结论和建议，但建议不能伪装成工具查询结果；没有数据时要如实说明没有数据。
            如果本轮已有独立的表格或图表结果块，正文只解释结果，不复制或重新设计表格、图表和数据块。
            """;
    }

    private String finalAnswerRetrySystemPrompt() {
        return finalAnswerSystemPrompt()
            + "\n上一次正式回答为空或未通过格式/事实校验。请重新读取本轮真实工具结果后回答。"
            + "只写普通段落或项目符号，不要输出 JSON、代码块、Markdown 表格；"
            + "所有数字逐字复制工具结果，不要换算日期或新增编号。"
            + "当前问题是查询类问题时，不要声称已创建、保存、付款、转账、入库、退货或完成任何写操作。";
    }

    public String finalAnswerUserPrompt(
        String userMessage,
        ResponsePayload payload,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        String structuredBlockInstruction = payload != null
                && payload.blocks() != null
                && !payload.blocks().isEmpty()
            ? "本轮已经有独立的结构化结果块负责渲染。正式回答只写简短说明，不要复制表格、图表、KPI、排行或任何 HTML/Markdown 数据块。\n"
            : "本轮没有独立结构化结果块，仍只返回文字回答。\n";
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "会话摘要：" + conversationSummary + "\n"
            : "";
        return ToolPlanner.formatHistoryContext(history)
            + summaryContext
            + "用户问题：" + userMessage + "\n"
            + "本轮工具真实结果 JSON：" + serializeToolResults(payload.toolResults()) + "\n"
            + "失败工具 JSON：" + serializeToolFailures(payload.toolFailures()) + "\n"
            + structuredBlockInstruction
            + "请直接生成自然语言正式回答。";
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

    private Optional<String> validateModelAnswer(String rawAnswer, ResponsePayload payload, String userMessage) {
        if (!StringUtils.hasText(rawAnswer) || rawAnswer.length() > 8_000) {
            log.warn("Agent final answer rejected: reason=empty_or_too_long length={}", rawAnswer == null ? 0 : rawAnswer.length());
            return Optional.empty();
        }
        Map<String, String> factSlots = factSlotsFor(payload, userMessage);
        String answer = rawAnswer.trim();
        if (answer.length() > 4_000 || answer.contains("{{") || answer.contains("}}")) {
            log.warn("Agent final answer rejected: reason=invalid_length_or_placeholder length={}", answer.length());
            return Optional.empty();
        }
        // A formal answer is plain model text. JSON envelopes and placeholder
        // contracts are deliberately rejected instead of being rendered by the server.
        if (answer.startsWith("{") || answer.startsWith("[")) {
            log.warn("Agent final answer rejected: reason=json_envelope");
            return Optional.empty();
        }
        // 结构化结果块已经由工具返回并单独渲染。模型偶尔仍会把 Markdown
        // 表格或代码围栏复制到正文里，移除重复的展示标记后保留模型正文。
        if (containsStructuredMarkup(answer)) {
            answer = stripStructuredMarkup(answer);
            if (!StringUtils.hasText(answer) || containsStructuredMarkup(answer)) {
                log.warn("Agent final answer rejected: reason=structured_markup_only");
                return Optional.empty();
            }
        }
        if (!factSlots.isEmpty() && containsUntrustedNumericValue(answer, factSlots)) {
            log.warn("Agent final answer rejected: reason=untrusted_numeric_value");
            return Optional.empty();
        }
        if (!hasSuccessfulDraft(payload)
            && !hasSuccessfulVisualization(payload)
            && !hasSuccessfulPosterPrompt(payload)
            && containsUnsupportedWriteClaim(answer)) {
            log.warn("Agent final answer rejected: reason=unsupported_write_claim");
            return Optional.empty();
        }
        return Optional.of(answer);
    }

    private boolean containsUnsupportedWriteClaim(String answer) {
        Matcher matcher = UNSUPPORTED_WRITE_CLAIM.matcher(answer);
        while (matcher.find()) {
            int sentenceStart = lastSentenceBoundary(answer, matcher.start()) + 1;
            int sentenceEnd = nextSentenceBoundary(answer, matcher.end());
            String sentence = answer.substring(sentenceStart, sentenceEnd);
            // Read-only answers commonly mention 入库/退货 as the type of a
            // returned record. Only reject a claim when its sentence has no
            // query/report context that changes the meaning from a write.
            if (!READ_ONLY_CONTEXT.matcher(sentence).find()) {
                return true;
            }
        }
        return false;
    }

    private int lastSentenceBoundary(String text, int before) {
        int boundary = -1;
        for (int index = 0; index < before; index++) {
            char current = text.charAt(index);
            if (current == '。' || current == '！' || current == '？' || current == '\n') {
                boundary = index;
            }
        }
        return boundary;
    }

    private int nextSentenceBoundary(String text, int after) {
        for (int index = after; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '。' || current == '！' || current == '？' || current == '\n') {
                return index + 1;
            }
        }
        return text.length();
    }

    private String stripStructuredMarkup(String text) {
        String[] lines = text.split("\\R", -1);
        List<String> visibleLines = new ArrayList<>();
        for (int index = 0; index < lines.length;) {
            String line = lines[index];
            String trimmed = line.trim();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                String fence = trimmed.substring(0, 3);
                index++;
                while (index < lines.length && !lines[index].trim().startsWith(fence)) {
                    index++;
                }
                if (index < lines.length) {
                    index++;
                }
                continue;
            }
            if (index + 1 < lines.length
                && isMarkdownTableRow(line)
                && isMarkdownTableSeparator(lines[index + 1])) {
                index += 2;
                while (index < lines.length && isMarkdownTableRow(lines[index])) {
                    index++;
                }
                continue;
            }
            String normalized = trimmed.toLowerCase(java.util.Locale.ROOT);
            if (normalized.startsWith("<table") || normalized.startsWith("<svg")) {
                String closingTag = normalized.startsWith("<table") ? "</table>" : "</svg>";
                index++;
                while (index < lines.length
                    && !lines[index].toLowerCase(java.util.Locale.ROOT).contains(closingTag)) {
                    index++;
                }
                if (index < lines.length) {
                    index++;
                }
                continue;
            }
            visibleLines.add(line);
            index++;
        }
        return String.join("\n", visibleLines).trim();
    }

    private boolean isMarkdownTableRow(String line) {
        return StringUtils.hasText(line) && line.indexOf('|') >= 0;
    }

    private boolean isMarkdownTableSeparator(String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        String normalized = line.trim();
        if (normalized.startsWith("|")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("|")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String[] cells = normalized.split("\\|");
        if (cells.length == 0) {
            return false;
        }
        for (String cell : cells) {
            if (!cell.trim().matches(":?-{3,}:?")) {
                return false;
            }
        }
        return true;
    }

    private boolean containsStructuredMarkup(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("<table")
            || normalized.contains("<thead")
            || normalized.contains("<tbody")
            || normalized.contains("<svg")
            || normalized.contains("```")
            || normalized.contains("|---")
            || normalized.contains("| ---");
    }

    private boolean containsUntrustedNumericValue(String answer, Map<String, String> factSlots) {
        Set<String> trustedNumbers = new LinkedHashSet<>();
        for (String value : factSlots.values()) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            boolean dateLike = value.matches(".*\\b\\d{4}-\\d{1,2}(?:-\\d{1,2})?\\b.*");
            Matcher valueMatcher = NUMBER_TOKEN.matcher(value);
            while (valueMatcher.find()) {
                String normalized = normalizeNumericToken(valueMatcher.group());
                trustedNumbers.add(normalized);
                // NUMBER_TOKEN treats the separator in YYYY-MM-DD as a
                // negative sign. Keep the positive date component trusted so
                // a model may say "7月" after the tool returned "2025-07".
                if (dateLike && normalized.startsWith("-")) {
                    trustedNumbers.add(normalized.substring(1));
                }
            }
        }
        Matcher answerMatcher = NUMBER_TOKEN.matcher(answer);
        while (answerMatcher.find()) {
            if (isStructuralListNumber(answer, answerMatcher.start(), answerMatcher.end())) {
                continue;
            }
            String token = normalizeNumericToken(answerMatcher.group());
            if (!trustedNumbers.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeNumericToken(String token) {
        try {
            return new java.math.BigDecimal(token.replace(",", "")).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ignored) {
            return token;
        }
    }

    private boolean isStructuralListNumber(String text, int start, int end) {
        int lineStart = text.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        if (!text.substring(lineStart, start).trim().isEmpty() || end >= text.length()) {
            return false;
        }
        char suffix = text.charAt(end);
        return (suffix == '.' || suffix == ')' || suffix == '、')
            && end + 1 < text.length()
            && Character.isWhitespace(text.charAt(end + 1));
    }

    private Map<String, String> factSlotsFor(ResponsePayload payload, String userMessage) {
        Map<String, String> factSlots = new LinkedHashMap<>();
        if (payload == null || payload.toolResults() == null) {
            if (StringUtils.hasText(userMessage)) {
                factSlots.put("user_request", userMessage);
            }
            return factSlots;
        }
        int toolIndex = 1;
        for (ToolExecutionResult result : payload.toolResults()) {
            if (result != null && result.facts() != null && !result.facts().isNull()
                && !"result_visualization".equals(result.toolName())) {
                collectFactSlots(
                    factSlots,
                    "fact_" + toolIndex + "_" + normalizeFactSlotPart(result.toolName()),
                    result.facts()
                );
            }
            if (result != null && StringUtils.hasText(result.summary())) {
                factSlots.put("summary_" + toolIndex, result.summary());
            }
            toolIndex++;
        }
        if (payload.blocks() != null && !payload.blocks().isEmpty()) {
            collectFactSlots(factSlots, "visible_blocks", objectMapper.valueToTree(payload.blocks()));
        }
        if (StringUtils.hasText(userMessage)) {
            // Numbers supplied by the user describe a requested window or
            // quantity; they are not business facts invented by the model.
            factSlots.put("user_request", userMessage);
        }
        return factSlots;
    }

    private boolean hasSuccessfulDraft(ResponsePayload payload) {
        if (payload == null || payload.toolResults() == null) {
            return false;
        }
        return payload.toolResults().stream().anyMatch(result ->
            result != null
                && result.facts() != null
                && result.facts().path("draft_id").asLong(0L) > 0L
                && (StringUtils.hasText(result.facts().path("draft_type").asText())
                    || (result.toolName() != null && result.toolName().startsWith("create_")))
        );
    }

    /**
     * generate_poster_prompt 是 READ_ONLY 文本产物工具：它只生成提示词文本，
     * 不写入任何正式业务表。模型回答"已生成海报提示词"是真实事实，不应被
     * unsupported_write_claim 当作"伪造正式写入"拒绝。
     */
    private boolean hasSuccessfulPosterPrompt(ResponsePayload payload) {
        if (payload == null || payload.toolResults() == null) {
            return false;
        }
        return payload.toolResults().stream().anyMatch(result ->
            result != null
                && "generate_poster_prompt".equals(result.toolName())
                && result.facts() != null
                && result.facts().isObject()
        );
    }

    private boolean hasSuccessfulVisualization(ResponsePayload payload) {
        if (payload == null || payload.toolResults() == null) {
            return false;
        }
        return payload.toolResults().stream().anyMatch(result ->
            result != null
                && "result_visualization".equals(result.toolName())
                && result.facts() != null
                && result.facts().path("visualization_enabled").asBoolean(false)
        );
    }

    private void collectFactSlots(Map<String, String> factSlots, String path, JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return;
        }
        if (value.isObject()) {
            value.fields().forEachRemaining(entry -> {
                if (!"query_audit".equals(entry.getKey())) {
                    collectFactSlots(factSlots, path + "_" + normalizeFactSlotPart(entry.getKey()), entry.getValue());
                }
            });
            return;
        }
        if (value.isArray()) {
            for (int index = 0; index < value.size(); index++) {
                collectFactSlots(factSlots, path + "_" + index, value.get(index));
            }
            return;
        }
        String text = value.asText();
        if (StringUtils.hasText(text)) {
            factSlots.put(path, text);
        }
    }

    private String normalizeFactSlotPart(String value) {
        String normalized = StringUtils.hasText(value)
            ? value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
            : "value";
        return normalized.replaceAll("^_+|_+$", "");
    }

    public void emitModelAnswerDeltas(
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

}
