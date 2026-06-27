package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Agent 工具注册表。
 *
 * <p>Spring 启动时自动扫描所有 {@link AgentTool} Bean 并注册到 {@link ConcurrentHashMap}。
 * 后续工具扩展只需实现 {@code AgentTool} + {@code @Component}，0 核心代码改动。
 *
 * <p>提供工具查询、权限过滤与统一执行入口。{@code V2AgentAiService} 通过此注册表
 * 动态获取可用工具列表（替代硬编码 switch），{@code ToolRegistry} 优先匹配注册工具，
 * 未注册时由 {@code V2AgentAiService} 的旧 switch 兜底（渐进式迁移）。
 *
 * <p>安全约束：
 * <ul>
 *   <li>{@link AgentTool.ToolType#CREATE_ONLY} 工具不直接执行写入，由调用方走草稿确认流程</li>
 *   <li>所有工具执行结果由调用方按 owner 隔离审计</li>
 * </ul>
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    /**
     * 构造注册表，Spring 自动注入所有 {@link AgentTool} Bean。
     *
     * @param registeredTools 已注册的工具列表（可为空）
     */
    public ToolRegistry(List<AgentTool> registeredTools) {
        if (registeredTools != null) {
            for (AgentTool tool : registeredTools) {
                AgentTool previous = tools.put(tool.name(), tool);
                if (previous != null) {
                    throw new IllegalStateException(
                        "AgentTool 名称冲突：" + tool.name()
                            + " 已被 " + previous.getClass().getName() + " 注册");
                }
            }
        }
    }

    /**
     * 按名称获取工具。
     *
     * @param name 工具名
     * @return 工具 Optional
     */
    public Optional<AgentTool> getTool(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 判断工具是否已注册。
     *
     * @param name 工具名
     * @return 已注册返回 true
     */
    public boolean isRegistered(String name) {
        return name != null && tools.containsKey(name);
    }

    /**
     * 获取全部已注册工具。
     *
     * @return 不可变工具列表
     */
    public List<AgentTool> listTools() {
        return List.copyOf(tools.values());
    }

    /**
     * 获取全部只读查询工具（可被 LLM 规划器直接调用）。
     *
     * @return 只读工具列表
     */
    public List<AgentTool> listReadOnlyTools() {
        List<AgentTool> readonly = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            if (tool.type() == AgentTool.ToolType.READ_ONLY) {
                readonly.add(tool);
            }
        }
        return Collections.unmodifiableList(readonly);
    }

    /**
     * 获取全部仅创建工具（强制走草稿确认流程）。
     *
     * @return 创建工具列表
     */
    public List<AgentTool> listCreateTools() {
        List<AgentTool> createOnly = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            if (tool.type() == AgentTool.ToolType.CREATE_ONLY) {
                createOnly.add(tool);
            }
        }
        return Collections.unmodifiableList(createOnly);
    }

    /**
     * 执行已注册工具。
     *
     * <p>调用方负责在执行前进行安全审查与（对 CREATE_ONLY 工具的）草稿确认流程。
     * 此方法仅负责查找工具并调用其 {@link AgentTool#execute}。
     *
     * @param name   工具名
     * @param ctx    执行上下文
     * @param params LLM 提取的参数，可为 null
     * @return 工具执行结果 Optional（工具未注册时返回 empty）
     */
    public Optional<ToolResult> executeTool(String name, ToolContext ctx, JsonNode params) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            return Optional.empty();
        }
        JsonNode safeParams = params == null || params.isNull() ? ctx.objectMapper().createObjectNode() : params;
        return Optional.of(tool.execute(ctx, safeParams));
    }

    /**
     * 生成给 LLM 规划器的工具描述清单。
     *
     * <p>每行格式：{@code - {工具名}：{描述}}，用于拼入规划器 system prompt。
     * 同时列出只读查询工具和仅创建工具；仅创建工具描述追加草稿确认提示。
     *
     * @return 工具描述文本
     */
    public String buildToolCatalogForLlm() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool tool : tools.values()) {
            if (tool.type() == AgentTool.ToolType.READ_ONLY) {
                sb.append("- ").append(tool.name()).append("：").append(tool.description()).append('\n');
            } else if (tool.type() == AgentTool.ToolType.CREATE_ONLY) {
                sb.append("- ").append(tool.name()).append("：").append(tool.description())
                    .append("（生成草稿，需用户确认后执行）").append('\n');
            }
        }
        return sb.toString();
    }
}
