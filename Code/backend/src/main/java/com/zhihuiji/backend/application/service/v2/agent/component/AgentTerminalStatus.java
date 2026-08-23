package com.zhihuiji.backend.application.service.v2.agent.component;

/**
 * Agent 运行统一终态。
 *
 * <p>终态字段是 Agent 运行结果的唯一业务判断依据。HTTP 状态码只表示传输、认证、
 * 参数或服务层处理结果，不能用 HTTP 200 单独推导业务成功；存在文本回答或
 * llm_status=completed 也不能单独判定业务任务完成。
 *
 * <p>REST、SSE、审计与 Web/Android/iOS 客户端必须使用同一组大写终态值。
 */
public enum AgentTerminalStatus {

    /** 查询或目标动作满足完成策略，回答只陈述已验证结果。 */
    COMPLETED("completed"),

    /** CREATE_ONLY 工具已生成草稿，等待用户确认；不能声称已写入正式表。 */
    CONFIRMATION_PENDING("confirmation_pending"),

    /** 工具、Provider、上下文或系统错误导致运行失败。 */
    FAILED("failed"),

    /** 安全或权限策略拒绝，未执行业务工具。 */
    BLOCKED("blocked"),

    /** 用户或系统取消；不覆盖为普通失败。 */
    CANCELLED("cancelled"),

    /** 轮次或工具预算耗尽但完成策略未满足；非成功语义。 */
    EXHAUSTED("exhausted");

    private final String auditStatus;

    AgentTerminalStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    /** 审计表（agent_run_audits.status）使用的小写状态值。 */
    public String auditStatus() {
        return auditStatus;
    }

    /** SSE 终止事件名：每个 run 只出现一次终态事件。 */
    public String terminalEventName() {
        return switch (this) {
            case COMPLETED -> "run_completed";
            case CONFIRMATION_PENDING -> "run_completed";
            case FAILED -> "run_failed";
            case BLOCKED -> "run_blocked";
            case CANCELLED -> "run_cancelled";
            case EXHAUSTED -> "run_exhausted";
        };
    }

    public static AgentTerminalStatus fromAuditStatus(String status) {
        if (status == null) {
            return FAILED;
        }
        for (AgentTerminalStatus value : values()) {
            if (value.auditStatus.equalsIgnoreCase(status)) {
                return value;
            }
        }
        return FAILED;
    }
}
