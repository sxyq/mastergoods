package com.zhihuiji.backend.application.service.v2.agent.component;

/**
 * 安全审查结论。
 *
 * <p>{@code passed} 为 true 表示请求通过安全检查；为 false 时 {@code reason} 给出拦截原因。
 *
 * @param passed 是否通过安全检查
 * @param reason 拦截原因，通过时为 null
 */
public record SafetyDecision(boolean passed, String reason) {}
