package com.zhihuiji.backend.application.service.v2.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * TokenEstimator 单元测试（plan 6.3）。
 *
 * <p>覆盖：空文本、字符比例估算、JSON 估算、消息列表固定开销、
 * 历史文本行数开销与降级行为。
 */
class TokenEstimatorTest {

    private final TokenEstimator estimator = new TokenEstimator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emptyAndBlankTextEstimateZero() {
        assertEquals(0, estimator.estimate(null));
        assertEquals(0, estimator.estimate(""));
        assertEquals(0, estimator.estimate("   "));
    }

    @Test
    void textEstimatedByCeilingCharsPerToken() {
        // "abc" -> ceil(3/3)=1
        assertEquals(1, estimator.estimate("abc"));
        // 7 个字符 -> ceil(7/3)=3
        assertEquals(3, estimator.estimate("abcdefg"));
        // 中文混排同样按 chars/3 保守估算。
        String chinese = "你好智慧记";
        assertEquals((int) Math.ceil(chinese.length() / 3.0), estimator.estimate(chinese));
    }

    @Test
    void singleCharNeverEstimatesZero() {
        assertEquals(1, estimator.estimate("a"));
    }

    @Test
    void jsonEstimateAddsMessageOverhead() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", "user");
        node.put("content", "abc");
        int estimate = estimator.estimateJson(node);
        assertTrue(estimate >= 1 + TokenEstimator.PER_MESSAGE_OVERHEAD,
            "JSON 估算应包含文本与固定开销");
    }

    @Test
    void nullJsonEstimatesZero() {
        assertEquals(0, estimator.estimateJson(null));
        assertEquals(0, estimator.estimateJson(objectMapper.createObjectNode().nullNode()));
    }

    @Test
    void messageListAddsPerMessageOverhead() {
        ObjectNode first = objectMapper.createObjectNode();
        first.put("role", "user");
        first.put("content", "abc");
        ObjectNode second = objectMapper.createObjectNode();
        second.put("role", "assistant");
        second.put("content", "def");
        int total = estimator.estimateMessages(List.of(first, second));
        // 每条至少 1 + 固定开销。
        assertTrue(total >= 2 * (1 + TokenEstimator.PER_MESSAGE_OVERHEAD));
    }

    @Test
    void nullMessagesEstimatesZero() {
        assertEquals(0, estimator.estimateMessages(null));
        assertEquals(0, estimator.estimateMessages(List.of()));
    }

    @Test
    void historyTextIncludesLineOverhead() {
        String history = "user: 你好\nassistant: 你好\n";
        int estimate = estimator.estimateHistoryText(history);
        // 2 行文本 + 2 行固定开销。
        assertTrue(estimate >= estimator.estimate(history) + 2 * TokenEstimator.PER_MESSAGE_OVERHEAD);
    }

    @Test
    void emptyHistoryTextEstimatesZero() {
        assertEquals(0, estimator.estimateHistoryText(null));
        assertEquals(0, estimator.estimateHistoryText(""));
    }

    @Test
    void longTextIsCappedDuringEstimation() {
        // 超过估算字符上限时仍返回有限值，不因超长文本拖慢估算。
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            builder.append('a');
        }
        int estimate = estimator.estimate(builder.toString());
        assertTrue(estimate > 0 && estimate < 100_000,
            "超长文本估算应被截断到字符上限范围内");
    }
}
