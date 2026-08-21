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
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agent 安全审查组件。
 *
 * <p>对用户输入进行三层安全审查：
 * <ol>
 *   <li>规则硬拦截层：明确破坏性数据库指令、明确越权访问其他账号数据、敏感关键词快速拦截</li>
 *   <li>写入意图检测层：否定写入语义识别（保留 "不要创建/Do not create" 等否定模式不被误判为写入）
 *       + 10 分钟内 20 条写入频率限制</li>
 *   <li>LLM 语义审查层：对高风险/敏感/模糊/疑似越权/提示词注入请求调用模型语义审查；
 *       普通当前账号业务查询跳过此层，避免安全模型误判覆盖真实数据问答；
 *       LLM 不可用时走可审计降级（记日志 + 放行，因规则层已过滤明确高风险）</li>
 * </ol>
 *
 * <p>原属 {@code V2AgentAiService} 内联逻辑，因边界清晰提取为独立组件。
 */
@Component
public class SafetyGuard {
    private static final Logger log = LoggerFactory.getLogger(SafetyGuard.class);
    private static final Pattern NEGATED_WRITE_INTENT = Pattern.compile(
        "\\b(?:do\\s+not|don't|dont|not)\\s+(?:create|add|new)\\b"
            + "|(?:不要|不需要|无需|别|不|未|禁止)\\s*(?:创建|新建|开一张|记一笔|录入|建一个|加一个|添加|生成)"
    );
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
        return evaluateSafety(currentOwnerService.requireCurrentOwnerUserId(), message);
    }

    public SafetyDecision evaluateSafety(Long ownerUserId, String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        // Layer 1: 规则硬拦截 — 明确破坏性数据库指令直接拒绝（无需 LLM）
        if (containsAny(normalized, "drop table", "delete from", "truncate")
            || (containsAny(normalized, "清空") && containsAny(normalized, "数据库"))
            || (containsAny(normalized, "删除") && containsAny(normalized, "所有数据"))) {
            return new SafetyDecision(false, "请求包含高风险破坏性数据库指令");
        }
        // Layer 1b: 规则硬拦截 — 明确越权访问其他账号数据直接拒绝
        if ((containsAny(normalized, "别人的", "其他账号", "越权"))
            && containsAny(normalized, "数据", "订单", "客户", "库存")) {
            return new SafetyDecision(false, "请求疑似越权访问其他账号的数据");
        }
        // Layer 2: 写入安全层（保留否定写入语义，避免 "不要创建" 被误判）
        SafetyDecision writeDecision = evaluateWriteSafety(ownerUserId, message, normalized);
        if (!writeDecision.passed()) {
            return writeDecision;
        }
        // Layer 3: LLM 语义审查层 — 对高风险/敏感/模糊/疑似越权/提示词注入请求调用模型审查
        if (requiresSemanticReview(normalized)) {
            Optional<SafetyDecision> llmDecision = modelSafetyReview(message);
            if (llmDecision.isPresent()) {
                return llmDecision.get();
            }
            // LLM 不可用时走可审计降级：规则层已过滤明确高风险请求，记日志后放行
            log.info("SafetyGuard LLM 不可用，敏感请求走规则降级放行：message_prefix={}",
                message.substring(0, Math.min(message.length(), 40)));
            return new SafetyDecision(true, "LLM 未配置，规则层已过滤高风险请求，放行");
        }
        // Layer 4: 当前账号的普通业务查询直接放行，避免安全模型误判覆盖真实数据问答
        return new SafetyDecision(true, null);
    }

    /**
     * 判断请求是否需要进入 LLM 语义审查层。
     *
     * <p>命中以下任一模式则进入语义审查：
     * <ul>
     *   <li>敏感关键词：sql, select 星号, 绕过, token, 密码, 管理员, admin, root</li>
     *   <li>敏感导出：导出, export, 批量下载全部数据</li>
     *   <li>间接越权：扮演, 假装, 作为, impersonate, act as, pretend</li>
     *   <li>提示词注入：忽略, 无视, 忘记, ignore previous, forget, system prompt</li>
     *   <li>模糊越权：他人, 别的账号, 其他用户, 其他租户, 全部账号</li>
     * </ul>
     *
     * @param normalized 小写化后的用户消息
     * @return true 表示需要 LLM 语义审查
     */
    boolean requiresSemanticReview(String normalized) {
        // 敏感关键词（不直接拦截，交由 LLM 判断上下文）
        if (containsAny(normalized, "sql", "select *", "绕过", "token", "密码", "管理员", "admin", "root")) {
            return true;
        }
        // 敏感导出（大量数据导出需 LLM 判断合法性）
        if (containsAny(normalized, "导出", "export", "下载全部", "下载所有", "批量导出")) {
            return true;
        }
        // 间接越权 / 身份扮演
        if (containsAny(normalized, "扮演", "假装", "作为", "impersonate", "act as", "pretend")) {
            return true;
        }
        // 提示词注入
        if (containsAny(normalized, "忽略", "无视", "忘记", "reset", "ignore previous", "forget", "system prompt", "系统提示")) {
            return true;
        }
        // 模糊越权（未命中 Layer 1b 硬拦截的间接越权提示）
        if (containsAny(normalized, "他人", "别的账号", "其他用户", "其他租户", "全部账号", "所有账号")) {
            return true;
        }
        return false;
    }

    public SafetyDecision evaluateWriteSafety(String message, String normalized) {
        return evaluateWriteSafety(currentOwnerService.requireCurrentOwnerUserId(), message, normalized);
    }

    public SafetyDecision evaluateWriteSafety(Long ownerUserId, String message, String normalized) {
        // 破坏性指令检测
        if (containsAny(normalized, "删除", "清空", "drop", "delete", "truncate", "销毁", "抹掉")) {
            return new SafetyDecision(false, "请求包含破坏性操作，不支持删除/清空");
        }
        // 检测写入意图
        String writeIntentText = NEGATED_WRITE_INTENT.matcher(normalized).replaceAll("");
        boolean hasWriteIntent = containsAny(writeIntentText, "创建", "新建", "开一张", "记一笔", "录入",
            "建一个", "加一个", "添加", "生成", "create", "add", "new");
        if (!hasWriteIntent) {
            return new SafetyDecision(true, null);
        }
        // 金额合理性校验：提取数字，超过 100,000 元触发警告但不拦截
        // 频率限制：10 分钟内最多 20 条写入
        long now = System.currentTimeMillis();
        long windowStart = now - 10 * 60 * 1000L;
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
                    return Optional.of(new SafetyDecision(false, "模型安全审查结果无法解析，默认拒绝"));
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
