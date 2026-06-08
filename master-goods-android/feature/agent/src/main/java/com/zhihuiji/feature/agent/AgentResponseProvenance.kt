package com.zhihuiji.feature.agent

internal const val DeltaSourceModelStream = "model_stream"
internal const val DeltaSourceRuleSummary = "rule_summary"

internal fun assistantHeaderStatusLabel(
    isStreaming: Boolean,
    hasServerAnswerDelta: Boolean,
    answerDeltaSource: String?,
    hasToolEvidence: Boolean,
    hasAuditTrace: Boolean,
): String = when {
    hasServerAnswerDelta -> answerDeltaSource.headerStatusLabel(isStreaming)
    isStreaming -> "正在等待服务端事件"
    hasAuditTrace || hasToolEvidence -> "服务端回复结果"
    else -> "助手回复"
}

internal fun assistantProvenanceLabel(
    hasCompletedTool: Boolean,
    hasToolEvidence: Boolean,
    answerDeltaSource: String?,
): String = when {
    hasCompletedTool -> "工具完成"
    hasToolEvidence -> "工具执行"
    answerDeltaSource == DeltaSourceModelStream -> "模型流"
    answerDeltaSource == DeltaSourceRuleSummary -> "服务端摘要"
    else -> "服务端文本"
}

internal fun String?.headerStatusLabel(isStreaming: Boolean = true): String =
    when (this) {
        DeltaSourceModelStream -> if (isStreaming) "模型正在流式生成" else "模型流式回复"
        DeltaSourceRuleSummary -> "数据查询 / 规则摘要模式"
        null -> if (isStreaming) "正在接收服务端回答" else "服务端回复结果"
        else -> if (isStreaming) "正在接收服务端回答" else "服务端回复结果"
    }

internal fun String?.inlineStreamingLabel(): String =
    when (this) {
        DeltaSourceModelStream -> "模型实时输出中"
        DeltaSourceRuleSummary -> "正在展示服务端规则摘要"
        null -> "正在接收服务端增量"
        else -> "正在接收服务端增量"
    }
