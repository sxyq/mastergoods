package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Agent 工具统一接口。
 *
 * <p>实现类声明工具名、展示名、描述、类型、所需权限、参数 Schema 与执行逻辑。
 * 注册到 {@link ToolRegistry} 后由 Spring 自动扫描，新增工具 0 核心代码改动。
 *
 * <p>设计原则：
 * <ul>
 *   <li>READ_ONLY 工具可被 LLM 规划器直接调用</li>
 *   <li>CREATE_ONLY 工具强制走草稿确认流程，不直接执行写入</li>
 *   <li>所有工具必须遵守 owner 隔离，仅查询/创建当前 ownerUserId 的数据</li>
 * </ul>
 */
public interface AgentTool {

    /**
     * 工具唯一标识，如 {@code sale_order_lookup}。
     *
     * @return 工具名
     */
    String name();

    /**
     * 中文展示名，用于审计日志与前端展示。
     *
     * @return 展示名
     */
    String displayName();

    /**
     * 给 LLM 规划器的工具描述，说明工具能力与适用场景。
     *
     * @return 工具描述
     */
    String description();

    /**
     * 工具类型：只读查询或仅创建。
     *
     * @return 工具类型
     */
    ToolType type();

    /**
     * 调用此工具所需的权限标识，空字符串表示无需额外权限。
     *
     * @return 权限标识
     */
    default String requiredPermission() {
        return type() == ToolType.CREATE_ONLY ? "agent:write" : "agent:view";
    }

    /**
     * 工具参数的 JSON Schema，描述可接受的参数名、类型与约束。
     *
     * <p>LLM 规划器根据此 Schema 从用户自然语言中提取参数值。
     * 返回空节点表示工具不接受参数。
     *
     * @return 参数 JSON Schema
     */
    default JsonNode parameterSchema() {
        return null;
    }

    /**
     * 执行此工具前必须已成功完成的依赖工具名（真实注册名）。
     *
     * <p>依赖由元数据表达，服务端在统一执行门校验：目标工具只有在所有必需依赖
     * 完成、参数由依赖结果构建后才进入执行范围。默认无依赖。
     *
     * @return 依赖工具名列表
     */
    default List<String> dependsOn() {
        return List.of();
    }

    /**
     * 工具在任务完成策略中的角色。
     *
     * @return 完成角色
     */
    default CompletionRole completionRole() {
        return type() == ToolType.CREATE_ONLY ? CompletionRole.TARGET_ACTION : CompletionRole.DEPENDENCY_QUERY;
    }

    /**
     * 工具结果是否需要用户二次确认后才进入正式业务写入。
     *
     * @return CREATE_ONLY 工具恒为 true
     */
    default boolean requiresConfirmation() {
        return type() == ToolType.CREATE_ONLY;
    }

    /**
     * 执行工具。
     *
     * @param ctx    工具执行上下文，包含 ownerUserId、runId、emitter 等
     * @param params LLM 提取的参数，空节点表示无参数
     * @return 工具执行结果
     */
    ToolResult execute(ToolContext ctx, JsonNode params);

    /** 工具类型枚举。 */
    enum ToolType {
        /** 只读查询工具，可被 LLM 直接调用。 */
        READ_ONLY,
        /** 仅创建工具，强制走草稿确认流程。 */
        CREATE_ONLY
    }

    /** 任务完成策略中的工具角色。 */
    enum CompletionRole {
        /** 依赖查询：为创建类工具提供真实实体 ID 与事实。 */
        DEPENDENCY_QUERY,
        /** 目标动作：任务完成所需的目标工具（例如生成草稿）。 */
        TARGET_ACTION,
        /** 展示决策：依赖真实数据结果，不查询数据库。 */
        PRESENTATION
    }
}
