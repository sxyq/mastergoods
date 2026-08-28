package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentRunState;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 统一工具执行门：所有 Agent 工具调用在进入业务 Repository 前必须通过四层检查。
 *
 * <ol>
 *   <li>范围门：工具已注册、在当前任务允许范围内、依赖工具已完成、去重。</li>
 *   <li>参数门：required、类型、数值边界、数组数量、枚举与非法字段的结构化校验，
 *       错误返回字段路径与稳定错误码；参数错误不得进入业务 Repository。</li>
 *   <li>权限与上下文门：requiredPermission 基于当前真实认证主体、成员关系与
 *       store 上下文计算，不信任模型参数或客户端自报权限；owner/store 条件
 *       贯穿到 Repository。</li>
 *   <li>完成门：requiresConfirmation 的 CREATE_ONLY 工具只生成草稿，
 *       不直接写正式业务表。</li>
 * </ol>
 *
 * <p>失败、权限拒绝、Schema 错误不自动重试；需要修正参数时必须产生新的候选
 * 并重新经过全部执行门。
 */
@Component
public class ToolExecutor {

    /** 稳定错误码。 */
    public static final String TOOL_NOT_REGISTERED = "TOOL_NOT_REGISTERED";
    public static final String TOOL_OUT_OF_SCOPE = "TOOL_OUT_OF_SCOPE";
    public static final String TOOL_DEPENDENCY_MISSING = "TOOL_DEPENDENCY_MISSING";
    public static final String TOOL_ARGUMENTS_INVALID = "TOOL_ARGUMENTS_INVALID";
    public static final String TOOL_PERMISSION_DENIED = "TOOL_PERMISSION_DENIED";
    public static final String TOOL_CONTEXT_INVALID = "TOOL_CONTEXT_INVALID";

    private final ToolRegistry toolRegistry;
    private final CurrentOwnerService currentOwnerService;

    public ToolExecutor(ToolRegistry toolRegistry, CurrentOwnerService currentOwnerService) {
        this.toolRegistry = toolRegistry;
        this.currentOwnerService = currentOwnerService;
    }

    /** 执行门判定结果：不允许执行时携带稳定码与安全描述。 */
    public record GateDecision(
        boolean allowed,
        String reasonCode,
        String safeMessage,
        List<ToolArgumentsValidator.Violation> violations
    ) {
        static GateDecision allow() {
            return new GateDecision(true, null, null, List.of());
        }

        static GateDecision deny(String reasonCode, String safeMessage) {
            return new GateDecision(false, reasonCode, safeMessage, List.of());
        }

        static GateDecision denyArguments(String safeMessage, List<ToolArgumentsValidator.Violation> violations) {
            return new GateDecision(false, TOOL_ARGUMENTS_INVALID, safeMessage, violations);
        }
    }

    /** 统一执行结果：执行门结论 + 工具结果（未执行时为空）。 */
    public record ExecutionOutcome(GateDecision decision, ToolResult result) {
        public boolean executed() {
            return decision != null && decision.allowed();
        }

        public boolean failed() {
            return result != null && !result.success();
        }
    }

    /**
     * 范围门：检查工具注册、允许范围与依赖完成情况。
     *
     * <p>依赖未完成时，若目标工具的必填参数已经齐备（模型直接给出了真实 ID），
     * 允许执行；否则返回 DEPENDENCY_MISSING，让下一轮先补依赖查询。这避免在
     * 模型已提供合法参数时仍误拦截，同时保留“参数缺失时必须先查依赖”的约束。
     *
     * @param runState      当前运行状态（提供已完成工具集合）
     * @param toolName      工具名
     * @param allowedTools  本轮允许的工具集合（null 表示不限制，仍需注册）
     * @param params        模型给出的参数（用于判断必填是否齐备）
     * @return 判定结果
     */
    public GateDecision checkScope(AgentRunState runState, String toolName, Set<String> allowedTools, JsonNode params) {
        Optional<AgentTool> tool = toolRegistry.getTool(toolName);
        if (tool.isEmpty()) {
            return GateDecision.deny(TOOL_NOT_REGISTERED, "工具未注册：" + toolName);
        }
        if (allowedTools != null && !allowedTools.contains(toolName)) {
            return GateDecision.deny(TOOL_OUT_OF_SCOPE, "工具不在当前任务的允许范围内：" + toolName);
        }
        List<String> dependencies = tool.get().dependsOn();
        if (dependencies != null && !dependencies.isEmpty() && runState != null) {
            boolean missingDependency = false;
            for (String dependency : dependencies) {
                if (!runState.isCompleted(dependency)) {
                    missingDependency = true;
                    break;
                }
            }
            if (missingDependency) {
                // 依赖未完成时，仅当必填参数已齐备才放行（模型直接给出真实 ID 的场景）。
                if (!hasRequiredFields(tool.get(), params)) {
                    return GateDecision.deny(TOOL_DEPENDENCY_MISSING,
                        "依赖工具未完成且必填参数缺失：" + toolName
                            + " 需要先完成真实查询并补齐参数");
                }
            }
        }
        return GateDecision.allow();
    }

    private boolean hasRequiredFields(AgentTool tool, JsonNode params) {
        JsonNode required = tool.parameterSchema() == null ? null : tool.parameterSchema().get("required");
        if (required == null || !required.isArray() || required.isEmpty()) {
            return true;
        }
        if (params == null || !params.isObject()) {
            return false;
        }
        for (JsonNode field : required) {
            if (!field.isTextual()) {
                continue;
            }
            JsonNode value = params.get(field.asText());
            if (value == null || value.isNull()
                || (value.isTextual() && value.asText().isBlank())) {
                return false;
            }
        }
        return true;
    }

    /** 旧签名兼容：不传参数时按依赖完成情况判定（不检查参数齐备）。 */
    public GateDecision checkScope(AgentRunState runState, String toolName, Set<String> allowedTools) {
        return checkScope(runState, toolName, allowedTools, null);
    }

    /**
     * 参数门：结构化 Schema 校验，返回全部违反项。
     *
     * @param toolName 工具名
     * @param params   模型给出的参数
     * @return 判定结果（违反项见 {@link GateDecision#violations()}）
     */
    public GateDecision checkArguments(String toolName, JsonNode params) {
        Optional<AgentTool> tool = toolRegistry.getTool(toolName);
        if (tool.isEmpty()) {
            return GateDecision.deny(TOOL_NOT_REGISTERED, "工具未注册：" + toolName);
        }
        JsonNode schema = tool.get().parameterSchema();
        if (schema == null || !schema.isObject()) {
            return GateDecision.allow();
        }
        List<ToolArgumentsValidator.Violation> violations = ToolArgumentsValidator.validate(schema, params);
        if (!violations.isEmpty()) {
            return GateDecision.denyArguments(
                "工具参数不符合声明的参数约束（" + violations.size() + " 处）",
                violations
            );
        }
        return GateDecision.allow();
    }

    /**
     * 权限与上下文门：基于当前认证主体构建执行上下文。
     *
     * <p>requiredPermission 每次调用都从当前认证主体、成员关系和当前 store 上下文
     * 计算；模型参数中的 owner/store 只作为业务筛选输入，不能作为可信租户身份。
     *
     * @param tool           目标工具
     * @param ownerUserId    当前 owner
     * @param conversationId 会话 ID
     * @param runId          运行 ID
     * @param emitter        SSE emitter（透传给工具）
     * @return 执行上下文（权限不足时返回权限拒绝判定）
     */
    public Either executeContext(
        AgentTool tool,
        Long ownerUserId,
        Long conversationId,
        String runId,
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter,
        com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        try {
            currentOwnerService.requirePermissions(tool.requiredPermission());
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return Either.denied(new GateDecision(false, TOOL_PERMISSION_DENIED,
                "当前调用者没有执行该工具所需的权限：" + tool.name(), List.of()));
        }
        Long currentOwnerUserId;
        try {
            currentOwnerUserId = currentOwnerService.requireCurrentOwnerUserId();
        } catch (RuntimeException ex) {
            return Either.denied(new GateDecision(false, TOOL_CONTEXT_INVALID,
                "无法解析当前调用者上下文", List.of()));
        }
        if (ownerUserId == null || !ownerUserId.equals(currentOwnerUserId)) {
            return Either.denied(new GateDecision(false, TOOL_CONTEXT_INVALID,
                "工具执行上下文无效：owner 与当前调用者不一致", List.of()));
        }
        Long currentUserId;
        Long currentStoreId;
        try {
            currentUserId = currentOwnerService.requireCurrentUserId();
            currentStoreId = currentOwnerService.findCurrentStoreId().orElse(null);
        } catch (RuntimeException ex) {
            return Either.denied(new GateDecision(false, TOOL_CONTEXT_INVALID,
                "无法解析当前调用者上下文", List.of()));
        }
        return Either.context(new ToolContext(
            ownerUserId,
            currentUserId,
            currentStoreId,
            conversationId,
            runId,
            emitter,
            objectMapper
        ));
    }

    /** 权限门结果：上下文或拒绝。 */
    public static final class Either {
        private final ToolContext context;
        private final GateDecision denied;

        private Either(ToolContext context, GateDecision denied) {
            this.context = context;
            this.denied = denied;
        }

        static Either context(ToolContext context) {
            return new Either(context, null);
        }

        static Either denied(GateDecision decision) {
            return new Either(null, decision);
        }

        public boolean allowed() {
            return context != null;
        }

        public ToolContext context() {
            return context;
        }

        public GateDecision denial() {
            return denied;
        }
    }

    /**
     * 完整执行链：范围门 → 参数门 → 权限与上下文门 → 执行。
     *
     * <p>CREATE_ONLY 工具由其实现保证只写草稿表；本方法不提供任何绕过路径。
     *
     * @param runState     运行状态
     * @param toolName     工具名
     * @param params       参数（可为 null）
     * @param allowedTools 本轮允许集合（null 不限制）
     * @param conversationId 会话 ID
     * @param runId        运行 ID
     * @param emitter      SSE emitter
     * @param objectMapper JSON mapper
     * @return 执行结果（含执行门结论）
     */
    public ExecutionOutcome execute(
        AgentRunState runState,
        String toolName,
        JsonNode params,
        Set<String> allowedTools,
        Long conversationId,
        String runId,
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter,
        com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        GateDecision scope = checkScope(runState, toolName, allowedTools, params);
        if (!scope.allowed()) {
            return new ExecutionOutcome(scope, null);
        }
        AgentTool tool = toolRegistry.getTool(toolName).orElse(null);
        if (tool == null) {
            return new ExecutionOutcome(GateDecision.deny(TOOL_NOT_REGISTERED, "工具未注册：" + toolName), null);
        }
        GateDecision arguments = checkArguments(toolName, params);
        if (!arguments.allowed()) {
            return new ExecutionOutcome(arguments, null);
        }
        Either either = executeContext(tool, runState == null ? null : runState.ownerUserId(),
            conversationId, runId, emitter, objectMapper);
        if (!either.allowed()) {
            return new ExecutionOutcome(either.denial(), null);
        }
        JsonNode safeParams = params == null || params.isNull()
            ? objectMapper.createObjectNode()
            : params;
        try {
            return new ExecutionOutcome(GateDecision.allow(), tool.execute(either.context(), safeParams));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return new ExecutionOutcome(
                GateDecision.deny(TOOL_PERMISSION_DENIED, "权限不足：" + safeDeniedMessage(ex)),
                null
            );
        }
    }

    private static String safeDeniedMessage(Exception ex) {
        String message = ex.getMessage();
        return StringUtils.hasText(message) ? message : "访问被拒绝";
    }
}
