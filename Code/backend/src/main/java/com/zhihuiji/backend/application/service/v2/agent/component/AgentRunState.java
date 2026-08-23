package com.zhihuiji.backend.application.service.v2.agent.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单次 Agent 运行的循环状态对象。
 *
 * <p>只存在于单次运行中（不入库、不进会话摘要）。负责跟踪轮次、工具调用计数、
 * 已完成工具、未完成目标工具、原生 tool transcript 与最终终态，使“模型返回了文本”
 * 与“业务任务已经完成”分开判断。
 *
 * <p>transcript 保存每轮 assistant tool call（call_id、工具名、原始参数字符串）与
 * 对应的 tool result/失败，call_id 一一配对；原始参数解析失败时以
 * {@code TOOL_ARGUMENTS_INVALID} 结构化失败记录，不得用空对象掩盖。
 */
public final class AgentRunState {

    /** 轮次硬上限：复杂度预算不得超过该值。 */
    public static final int HARD_ITERATION_CAP = 6;

    private final String runId;
    private final Long conversationId;
    private final Long ownerUserId;
    private final Long storeId;

    private int iteration;
    private int iterationBudget;
    private int toolCallCount;
    private final Set<String> completedToolNames = new LinkedHashSet<>();
    private final Set<String> attemptedToolNames = new LinkedHashSet<>();
    private final Set<String> requiredTargetTools = new LinkedHashSet<>();
    private final List<TranscriptRound> nativeTranscript = new ArrayList<>();
    private AgentTerminalStatus terminalStatus;

    public AgentRunState(String runId, Long conversationId, Long ownerUserId, Long storeId, int iterationBudget) {
        this.runId = runId;
        this.conversationId = conversationId;
        this.ownerUserId = ownerUserId;
        this.storeId = storeId;
        this.iterationBudget = Math.max(1, Math.min(HARD_ITERATION_CAP, iterationBudget));
        this.iteration = 1;
    }

    public String runId() {
        return runId;
    }

    public Long conversationId() {
        return conversationId;
    }

    public Long ownerUserId() {
        return ownerUserId;
    }

    public Long storeId() {
        return storeId;
    }

    public int iteration() {
        return iteration;
    }

    public int iterationBudget() {
        return iterationBudget;
    }

    /** 预算内是否还能再发起一轮模型规划。 */
    public boolean hasIterationLeft() {
        return iteration < iterationBudget;
    }

    /** 进入下一轮前推进轮次；超出预算返回 false。 */
    public boolean advanceIteration() {
        if (iteration >= iterationBudget) {
            return false;
        }
        iteration += 1;
        return true;
    }

    public int toolCallCount() {
        return toolCallCount;
    }

    /** 记录一次工具调用（含跳过/失败），受运行内工具预算约束。 */
    public boolean recordToolCall() {
        toolCallCount += 1;
        return true;
    }

    public void markCompleted(String toolName) {
        if (toolName != null) {
            completedToolNames.add(toolName);
            attemptedToolNames.add(toolName);
        }
    }

    public void markAttempted(String toolName) {
        if (toolName != null) {
            attemptedToolNames.add(toolName);
        }
    }

    public boolean isCompleted(String toolName) {
        return completedToolNames.contains(toolName);
    }

    public Set<String> completedToolNames() {
        return Collections.unmodifiableSet(completedToolNames);
    }

    public Set<String> attemptedToolNames() {
        return Collections.unmodifiableSet(attemptedToolNames);
    }

    /** 登记完成任务所需的目标工具（例如 CREATE_ONLY 草稿工具）。 */
    public void requireTargetTools(Set<String> toolNames) {
        if (toolNames != null) {
            requiredTargetTools.addAll(toolNames);
        }
    }

    public Set<String> requiredTargetTools() {
        return Collections.unmodifiableSet(requiredTargetTools);
    }

    /** 尚未完成的目标工具。 */
    public List<String> missingTargetTools() {
        List<String> missing = new ArrayList<>();
        for (String tool : requiredTargetTools) {
            if (!completedToolNames.contains(tool)) {
                missing.add(tool);
            }
        }
        return missing;
    }

    /** 所有登记的目标工具是否都已成功完成。 */
    public boolean allTargetToolsCompleted() {
        return !requiredTargetTools.isEmpty() && missingTargetTools().isEmpty();
    }

    /** 是否已成功完成至少一个 CREATE_ONLY 目标工具（进入待确认）。 */
    public boolean anyTargetToolCompleted() {
        for (String tool : requiredTargetTools) {
            if (completedToolNames.contains(tool)) {
                return true;
            }
        }
        return false;
    }

    public AgentTerminalStatus terminalStatus() {
        return terminalStatus;
    }

    public void terminalStatus(AgentTerminalStatus status) {
        this.terminalStatus = status;
    }

    /**
     * 追加一轮原生 transcript：assistant tool call 与 tool result 按轮配对。
     *
     * @param round 本轮记录（由 {@link TranscriptRound#of} 构建）
     */
    public void appendTranscriptRound(TranscriptRound round) {
        if (round != null) {
            nativeTranscript.add(round);
        }
    }

    public List<TranscriptRound> nativeTranscript() {
        return Collections.unmodifiableList(nativeTranscript);
    }

    /** 单轮原生 transcript：assistant 发起的调用与对应的执行结果。 */
    public static final class TranscriptRound {
        private final int round;
        private final List<AssistantToolCall> assistantToolCalls;
        private final List<ToolMessage> toolMessages;

        private TranscriptRound(int round, List<AssistantToolCall> assistantToolCalls, List<ToolMessage> toolMessages) {
            this.round = round;
            this.assistantToolCalls = List.copyOf(assistantToolCalls);
            this.toolMessages = List.copyOf(toolMessages);
        }

        public static TranscriptRound of(
            int round,
            List<AssistantToolCall> assistantToolCalls,
            List<ToolMessage> toolMessages
        ) {
            return new TranscriptRound(
                round,
                assistantToolCalls == null ? List.of() : assistantToolCalls,
                toolMessages == null ? List.of() : toolMessages
            );
        }

        public int round() {
            return round;
        }

        public List<AssistantToolCall> assistantToolCalls() {
            return assistantToolCalls;
        }

        public List<ToolMessage> toolMessages() {
            return toolMessages;
        }

        /** 每个 assistant tool call 是否都有配对结果（完成/失败/取消）。 */
        public boolean fullyPaired() {
            Set<String> callIds = new LinkedHashSet<>();
            for (AssistantToolCall call : assistantToolCalls) {
                if (call.callId() != null) {
                    callIds.add(call.callId());
                }
            }
            Set<String> resultIds = new LinkedHashSet<>();
            for (ToolMessage message : toolMessages) {
                if (message.callId() != null) {
                    resultIds.add(message.callId());
                }
            }
            for (String callId : callIds) {
                if (!resultIds.contains(callId)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** assistant 消息中的 tool_calls[] 项：call_id、工具名、未改写的原始参数。 */
    public record AssistantToolCall(String callId, String toolName, String rawArguments) {}

    /** role=tool / function_call_output 消息：与 call_id 配对的结果或结构化失败。 */
    public record ToolMessage(String callId, String toolName, String status, String output) {}

    /** 以稳定 JSON 形式导出 transcript 摘要（审计/调试用，输出经过截断）。 */
    public Map<String, Object> transcriptSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("run_id", runId);
        summary.put("iteration", iteration);
        summary.put("iteration_budget", iterationBudget);
        summary.put("tool_call_count", toolCallCount);
        summary.put("completed_tools", List.copyOf(completedToolNames));
        summary.put("missing_target_tools", missingTargetTools());
        List<Map<String, Object>> rounds = new ArrayList<>(nativeTranscript.size());
        for (TranscriptRound round : nativeTranscript) {
            Map<String, Object> roundMap = new LinkedHashMap<>();
            roundMap.put("round", round.round());
            roundMap.put("fully_paired", round.fullyPaired());
            List<Map<String, Object>> calls = new ArrayList<>();
            for (AssistantToolCall call : round.assistantToolCalls()) {
                Map<String, Object> callMap = new LinkedHashMap<>();
                callMap.put("call_id", call.callId());
                callMap.put("tool_name", call.toolName());
                callMap.put("raw_arguments_length", call.rawArguments() == null ? 0 : call.rawArguments().length());
                calls.add(callMap);
            }
            roundMap.put("assistant_tool_calls", calls);
            List<Map<String, Object>> results = new ArrayList<>();
            for (ToolMessage message : round.toolMessages()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("call_id", message.callId());
                result.put("tool_name", message.toolName());
                result.put("status", message.status());
                results.add(result);
            }
            roundMap.put("tool_messages", results);
            rounds.add(roundMap);
        }
        summary.put("rounds", rounds);
        return summary;
    }
}
