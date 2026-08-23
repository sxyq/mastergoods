package com.zhihuiji.backend.application.service.v2.agent.context;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Token 估算器（plan 6.3）。
 *
 * <p>不调用 Provider 真实 tokenizer：在请求构建路径上无法承担同步 tokenize 的延迟
 * 与失败成本。估算采用保守的字符/字节比例加消息固定开销，确保估算误差倾向于
 * 低估可用预算（更早触发压缩），而不是高估导致请求超限。
 *
 * <p>估算失败或不可解析的消息标记为降级；调用方在降级时应提高安全余量。
 */
@Component
public class TokenEstimator {
    /**
     * 中文字符占比高的请求按 chars/3 估算（保守上限）；
     * 英文为主的请求通常 chars/4 更接近真实，这里取偏保守值。
     */
    public static final double CHARS_PER_TOKEN_FALLBACK = 3.0;

    /**
     * 每条消息在 Provider wire 协议中的固定开销（role 标签、分隔符等）。
     */
    public static final int PER_MESSAGE_OVERHEAD = 4;

    /**
     * 极端长字符串切分阈值，避免单次估算占用过多 CPU。
     */
    private static final int ESTIMATION_CHAR_CAP = 64_000;

    /**
     * 估算一段文本的 token 数。
     *
     * <p>估算失败时使用更保守的字符/字节估算，并返回非零正值，确保预算计算不
     * 因为 null/0 导致跳过压缩。空文本返回 0。
     */
    public int estimate(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        int length = text.length();
        int boundedLength = Math.min(length, ESTIMATION_CHAR_CAP);
        // 中文字符占 1 token 以上，英文约 0.25 token；chars/3 是偏保守的混合估算。
        int estimate = (int) Math.ceil(boundedLength / CHARS_PER_TOKEN_FALLBACK);
        if (estimate <= 0) {
            // 估算降级：至少返回 1，确保非空文本计入预算。
            return 1;
        }
        return estimate;
    }

    /**
     * 估算一段 JSON 序列化后的消息负载 token 数。
     */
    public int estimateJson(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return 0;
        }
        String text;
        try {
            text = node.toString();
        } catch (Exception ex) {
            // 估算失败：按节点 sizeHint 粗略估算。
            return PER_MESSAGE_OVERHEAD;
        }
        return estimate(text) + PER_MESSAGE_OVERHEAD;
    }

    /**
     * 批量估算 OpenAI/Anthropic 风格消息列表的 token 数。
     *
     * <p>每条消息增加 {@link #PER_MESSAGE_OVERHEAD} 固定开销。估算失败的消息按
     * 保守字符长度估算，确保不会因为单条失败就漏算整体预算。
     */
    public int estimateMessages(Iterable<? extends JsonNode> messages) {
        if (messages == null) {
            return 0;
        }
        int total = 0;
        for (JsonNode message : messages) {
            if (message == null || message.isMissingNode()) {
                continue;
            }
            total += estimateJson(message);
        }
        return total;
    }

    /**
     * 估算一段已经格式化的历史文本的 token 数（含每条消息固定开销）。
     *
     * <p>用于把 ToolPlanner.formatHistoryContext 这种已经成型的字符串换算成预算。
     */
    public int estimateHistoryText(String formattedHistory) {
        if (!StringUtils.hasText(formattedHistory)) {
            return 0;
        }
        // 历史文本已经包含 role + content，固定开销按行数粗略估算。
        int lineCount = 0;
        for (int i = 0; i < formattedHistory.length(); i++) {
            if (formattedHistory.charAt(i) == '\n') {
                lineCount++;
            }
        }
        return estimate(formattedHistory) + lineCount * PER_MESSAGE_OVERHEAD;
    }
}
