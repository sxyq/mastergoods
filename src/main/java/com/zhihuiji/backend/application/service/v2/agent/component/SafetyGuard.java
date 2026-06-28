package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Agent 安全审查组件。
 *
 * <p>负责对用户输入进行规则与 LLM 双层安全审查：
 * <ul>
 *   <li>硬编码关键词快速拦截越权与破坏性指令</li>
 *   <li>写入意图检测 + 10 分钟内 20 条的频率限制</li>
 *   <li>敏感关键词触发 LLM 语义审查增强层</li>
 * </ul>
 *
 * <p>原属 {@code V2AgentAiService} 内联逻辑，因边界清晰提取为独立组件。
 */
@Component
public class SafetyGuard {
    private final LongCatAnthropicClient longCatAnthropicClient;
    private final ObjectMapper objectMapper;
    private final CurrentOwnerService currentOwnerService;
    private final Map<Long, List<Long>> writeFrequencyTracker = new ConcurrentHashMap<>();

    public SafetyGuard(
        LongCatAnthropicClient longCatAnthropicClient,
        ObjectMapper objectMapper,
        CurrentOwnerService currentOwnerService
    ) {
        this.longCatAnthropicClient = longCatAnthropicClient;
        this.objectMapper = objectMapper;
        this.currentOwnerService = currentOwnerService;
    }

    public SafetyDecision evaluateSafety(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        // 快速通道：硬编码关键词命中即拦截
        if ((normalized.contains("别人的") || normalized.contains("其他账号") || normalized.contains("越权"))
            && containsAny(normalized, "数据", "订单", "客户", "库存")) {
            return new SafetyDecision(false, "请求疑似越权访问其他账号的数据");
        }
        if ((normalized.contains("drop table") || normalized.contains("delete from") || normalized.contains("truncate"))
            || (normalized.contains("清空") && normalized.contains("数据库"))
            || (normalized.contains("删除") && normalized.contains("所有数据"))) {
            return new SafetyDecision(false, "请求包含高风险破坏性数据库指令");
        }
        // 写入专用安全层：检测写入意图并校验
        SafetyDecision writeDecision = evaluateWriteSafety(message, normalized);
        if (!writeDecision.passed()) {
            return writeDecision;
        }
        // 敏感关键词快速通道
        if (containsAny(normalized, "sql", "select *", "绕过", "token", "密码", "管理员")) {
            Optional<SafetyDecision> modelDecision = modelSafetyReview(message);
            if (modelDecision.isPresent() && !modelDecision.get().passed()) {
                return modelDecision.get();
            }
            return new SafetyDecision(false, "请求包含敏感查询或权限绕过风险");
        }
        // LLM 语义审查增强层
        Optional<SafetyDecision> modelDecision = modelSafetyReview(message);
        if (modelDecision.isPresent() && !modelDecision.get().passed()) {
            return modelDecision.get();
        }
        return new SafetyDecision(true, null);
    }

    public SafetyDecision evaluateWriteSafety(String message, String normalized) {
        // 破坏性指令检测
        if (containsAny(normalized, "删除", "清空", "drop", "delete", "truncate", "销毁", "抹掉")) {
            return new SafetyDecision(false, "请求包含破坏性操作，不支持删除/清空");
        }
        // 检测写入意图
        boolean hasWriteIntent = containsAny(normalized, "创建", "新建", "开一张", "记一笔", "录入",
            "建一个", "加一个", "添加", "生成", "create", "add", "new");
        if (!hasWriteIntent) {
            return new SafetyDecision(true, null);
        }
        // 金额合理性校验：提取数字，超过 100,000 元触发警告但不拦截
        // 频率限制：10 分钟内最多 20 条写入
        long now = System.currentTimeMillis();
        long windowStart = now - 10 * 60 * 1000L;
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<Long> timestamps = writeFrequencyTracker.computeIfAbsent(
            ownerUserId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (timestamps) {
            timestamps.removeIf(ts -> ts < windowStart);
            if (timestamps.size() >= 20) {
                return new SafetyDecision(false, "10 分钟内写入操作超过 20 条限制，请稍后再试");
            }
            timestamps.add(now);
        }
        return new SafetyDecision(true, null);
    }

    public Optional<SafetyDecision> modelSafetyReview(String message) {
        if (!longCatAnthropicClient.isConfigured()) {
            // 未配置 LLM 时不再跳过，返回保守放行（规则层已拦截高风险请求）
            return Optional.of(new SafetyDecision(true, "LLM 未配置，仅规则审查"));
        }
        String systemPrompt = """
            你是智慧记 AI 助手的安全审查器。只判断用户请求是否允许。
            允许：查询当前登录账号自己的经营数据、库存、客户欠款、销售概览；创建当前账号下的业务单据（销售单、采购单、客户等）。
            拦截：访问其他账号/其他租户数据、要求绕过权限、导出敏感凭据、直接执行 SQL、破坏性数据库操作、删除操作。
            只输出 JSON：{"allowed":true,"reason":"..."}。
            """;
        return longCatAnthropicClient.createJsonMessage(systemPrompt, "用户请求：" + message)
            .flatMap(raw -> {
                try {
                    JsonNode root = objectMapper.readTree(extractJsonObject(raw));
                    boolean allowed = root.path("allowed").asBoolean(false);
                    String reason = root.path("reason").asText(allowed ? null : "模型安全审查未通过");
                    return Optional.of(new SafetyDecision(allowed, allowed ? null : reason));
                } catch (Exception ignored) {
                    return Optional.empty();
                }
            });
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String extractJsonObject(String rawText) {
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
}
