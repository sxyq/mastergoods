package com.zhihuiji.backend.application.service.v2.agent.component;

import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * Agent 轮次预算策略。
 *
 * <p>按任务复杂度（目标创建工具、依赖查询数量、规划工具数量）计算轮次预算，
 * 取代此前对所有任务固定的 MAX_AGENT_ITERATIONS=3。预算受
 * {@link AgentRunState#HARD_ITERATION_CAP} 硬上限约束。
 *
 * <p>初始策略（与优化计划一致）：
 * <ul>
 *   <li>纯文本问题：1 轮</li>
 *   <li>单个只读查询：2 轮</li>
 *   <li>多个只读查询：3 轮</li>
 *   <li>查询后生成草稿：4 轮</li>
 *   <li>多依赖创建任务：5 轮</li>
 * </ul>
 */
public final class AgentIterationPolicy {

    private AgentIterationPolicy() {
    }

    /**
     * 依据用户请求与首轮规划计算本次运行的轮次预算。
     *
     * @param message      用户请求
     * @param initialPlan  首轮规划（可为 null）
     * @param toolRegistry 工具注册表（用于读取依赖元数据）
     * @return 至少 1、至多 {@link AgentRunState#HARD_ITERATION_CAP} 的轮次预算
     */
    public static int budgetFor(String message, AgentTypes.AgentToolPlan initialPlan, ToolRegistry toolRegistry) {
        String writeTarget = AgentPromptCatalog.targetWriteTool(message);
        int plannedToolCount = plannedToolCount(initialPlan);
        boolean hasWriteTarget = StringUtils.hasText(writeTarget);

        if (!hasWriteTarget && plannedToolCount == 0) {
            // 纯文本问题：一次模型回答即可。
            return 1;
        }
        if (hasWriteTarget) {
            int dependencyCount = dependencyCountOf(writeTarget, toolRegistry);
            // 查询、补参数、生成草稿、回答；多依赖任务多一轮。
            return Math.min(AgentRunState.HARD_ITERATION_CAP, dependencyCount >= 2 ? 5 : 4);
        }
        if (plannedToolCount >= 2) {
            return 3;
        }
        return 2;
    }

    private static int plannedToolCount(AgentTypes.AgentToolPlan plan) {
        if (plan == null || plan.tools() == null) {
            return 0;
        }
        return (int) plan.tools().stream().filter(StringUtils::hasText).count();
    }

    /** 读取目标工具的依赖数量（dependsOn 元数据；未声明时按 1 个依赖估算）。 */
    private static int dependencyCountOf(String writeTarget, ToolRegistry toolRegistry) {
        if (toolRegistry == null) {
            return 1;
        }
        return toolRegistry.getTool(writeTarget)
            .map(AgentTool::dependsOn)
            .map(List::size)
            .filter(count -> count > 0)
            .orElse(1);
    }
}
