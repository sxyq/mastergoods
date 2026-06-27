package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import java.util.List;

/**
 * Agent 工具执行结果。
 *
 * <p>由 {@link AgentTool#execute} 返回，包含工具执行后的答案、结构化结果块、
 * 事实数据、摘要与失败信息。{@code V2AgentAiService} 消费此结果构建最终响应。
 *
 * <p>字段说明：
 * <ul>
 *   <li>{@code success} — 执行是否成功</li>
 *   <li>{@code answer} — 工具产生的自然语言答案片段，将拼入最终回复</li>
 *   <li>{@code blocks} — 结构化结果块（表格/图表/风险卡片等），推送给前端</li>
 *   <li>{@code toolFacts} — 工具事实数据 JSON，传给 LLM 合成最终答案</li>
 *   <li>{@code toolSummary} — 工具执行摘要，用于审计与运行轨迹展示</li>
 *   <li>{@code errorMessage} — 失败时的安全错误消息（不含敏感信息）</li>
 *   <li>{@code insufficient} — 结果是否不充分，触发 Agent 循环重新规划</li>
 * </ul>
 *
 * @param success       是否成功
 * @param answer        自然语言答案片段
 * @param blocks        结构化结果块
 * @param toolFacts     事实数据 JSON
 * @param toolSummary   执行摘要
 * @param errorMessage  失败错误消息
 * @param insufficient  结果是否不充分（触发 Agent 循环）
 */
public record ToolResult(
    boolean success,
    String answer,
    List<V2AgentDtos.ResultBlockDto> blocks,
    JsonNode toolFacts,
    String toolSummary,
    String errorMessage,
    boolean insufficient
) {

    private static final List<V2AgentDtos.ResultBlockDto> EMPTY_BLOCKS = List.of();

    /**
     * 构造成功结果。
     *
     * @param answer      自然语言答案
     * @param blocks      结构化结果块
     * @param toolFacts   事实数据
     * @param toolSummary 执行摘要
     * @return 成功结果
     */
    public static ToolResult success(String answer, List<V2AgentDtos.ResultBlockDto> blocks,
                                     JsonNode toolFacts, String toolSummary) {
        return new ToolResult(true, answer, blocks, toolFacts, toolSummary, null, false);
    }

    /**
     * 构造成功但不充分的结果（触发 Agent 循环重新规划）。
     *
     * @param answer      自然语言答案
     * @param blocks      结构化结果块
     * @param toolFacts   事实数据
     * @param toolSummary 执行摘要
     * @return 不充分的成功结果
     */
    public static ToolResult insufficient(String answer, List<V2AgentDtos.ResultBlockDto> blocks,
                                          JsonNode toolFacts, String toolSummary) {
        return new ToolResult(true, answer, blocks, toolFacts, toolSummary, null, true);
    }

    /**
     * 构造失败结果。
     *
     * @param toolName    工具名
     * @param errorMessage 安全错误消息
     * @return 失败结果
     */
    public static ToolResult failure(String toolName, String errorMessage) {
        return new ToolResult(false, "", EMPTY_BLOCKS, NullNode.getInstance(),
            toolName + " 执行失败", errorMessage, false);
    }

    /**
     * 构造空结果（工具未命中任何数据）。
     *
     * @param toolSummary 执行摘要
     * @return 空结果
     */
    public static ToolResult empty(String toolSummary) {
        return new ToolResult(true, "", EMPTY_BLOCKS, NullNode.getInstance(), toolSummary, null, false);
    }
}
