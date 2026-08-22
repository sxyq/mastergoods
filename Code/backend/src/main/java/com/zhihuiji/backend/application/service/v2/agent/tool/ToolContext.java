package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 工具执行上下文。
 *
 * <p>封装工具执行所需的环境信息：当前 owner、会话、运行 ID、SSE 推送器等。
 * 由 {@code V2AgentAiService} 在调用工具时构造，传递给 {@link AgentTool#execute}。
 *
 * <p>注意：
 * <ul>
 *   <li>{@code ownerUserId} 必须非空，所有工具查询必须按此过滤</li>
 *   <li>{@code emitter} 可能为 {@code null}（非流式调用），工具不应假设其存在</li>
 *   <li>{@code conversationId} 可能为 {@code null}（单轮无会话场景）</li>
 * </ul>
 *
 * @param ownerUserId    归属用户 ID（多租户隔离必需）
 * @param userId         当前认证用户 ID
 * @param storeId        当前认证用户的有效门店 ID，可为空（兼容无门店的 owner）
 * @param conversationId 会话 ID，无会话时为 null
 * @param runId          Agent 运行 ID，用于审计与 SSE 事件关联
 * @param emitter        SSE 推送器，非流式调用时为 null
 * @param objectMapper   JSON 序列化器
 */
public record ToolContext(
    Long ownerUserId,
    Long userId,
    Long storeId,
    Long conversationId,
    String runId,
    SseEmitter emitter,
    ObjectMapper objectMapper
) {

    /** Backward-compatible constructor for isolated tool tests and legacy callers. */
    public ToolContext(
        Long ownerUserId,
        Long userId,
        Long conversationId,
        String runId,
        SseEmitter emitter,
        ObjectMapper objectMapper
    ) {
        this(ownerUserId, userId, null, conversationId, runId, emitter, objectMapper);
    }

    /**
     * 判断当前是否流式调用（emitter 非空）。
     *
     * @return 流式调用返回 true
     */
    public boolean isStreaming() {
        return emitter != null;
    }
}
