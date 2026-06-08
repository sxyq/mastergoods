package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ChatMessagePart

internal const val DeltaSourceModelStream = "model_stream"
internal const val DeltaSourceRuleSummary = "rule_summary"
internal const val DeltaSourceServerNotice = "server_notice"

internal fun assistantHeaderStatusLabel(
    isStreaming: Boolean,
    hasServerAnswerDelta: Boolean,
    answerDeltaSource: String?,
    hasToolEvidence: Boolean,
    hasAuditTrace: Boolean,
    mode: String? = null,
    llmStatus: String? = null,
): String = when {
    isStreaming -> "正在分析并生成回答"
    isRuleSummaryMode(mode = mode, llmStatus = llmStatus) -> "数据查询 / 规则摘要模式"
    hasServerAnswerDelta -> answerDeltaSource.headerStatusLabel(isStreaming)
    hasAuditTrace || hasToolEvidence -> "已基于真实查询回答"
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
        answerDeltaSource == DeltaSourceRuleSummary -> "规则摘要"
        answerDeltaSource == DeltaSourceServerNotice -> "查询说明"
        else -> "AI 文本"
    }

internal fun String?.headerStatusLabel(isStreaming: Boolean = true): String =
    when (this) {
        DeltaSourceModelStream -> if (isStreaming) "模型正在流式生成" else "模型流式回复"
        DeltaSourceRuleSummary -> "数据查询 / 规则摘要模式"
        DeltaSourceServerNotice -> "正在补充查询说明"
        null -> if (isStreaming) "正在生成回答" else "已基于真实查询回答"
        else -> if (isStreaming) "正在生成回答" else "已基于真实查询回答"
    }

internal fun String?.inlineStreamingLabel(): String =
    when (this) {
        DeltaSourceModelStream -> "模型实时输出中"
        DeltaSourceRuleSummary -> "正在展示规则摘要"
        DeltaSourceServerNotice -> "正在补充查询边界说明"
        null -> "正在生成回答"
        else -> "正在生成回答"
    }

internal fun ChatMessage.shouldShowInlineStreamingStatus(): Boolean =
    isStreaming && (hasServerAnswerDelta || content.isBlank())

internal fun ChatMessage.shouldShowAssistantHeader(): Boolean =
    isStreaming || isRuleSummaryMode(
        mode = runTrace?.mode,
        llmStatus = runTrace?.llmStatus,
    )

internal fun ChatMessage.shouldShowAssistantHeaderBadges(): Boolean = isStreaming

internal fun ChatMessage.shouldShowRunTracePanel(): Boolean =
    isError ||
        runTrace?.isExpanded == true ||
        (isStreaming && !hasVisibleAssistantTimeline())

internal fun ChatMessage.hasVisibleAssistantTimeline(): Boolean =
    content.isNotBlank() ||
        parts.any { part ->
            when (part) {
                is ChatMessagePart.Text -> part.markdown.isNotBlank()
                is ChatMessagePart.ResultBlock -> true
            }
        }

internal fun assistantReviewBadgeLabel(
    isStreaming: Boolean,
    hasAuditTrace: Boolean,
    hasToolEvidence: Boolean,
): String = when {
    hasAuditTrace -> "有运行标识"
    hasToolEvidence -> "有工具记录"
    isStreaming -> "生成中"
    else -> "未展开"
}

internal fun isRuleSummaryMode(mode: String?, llmStatus: String?): Boolean =
    mode == "tool_query_rule_summary" || llmStatus in setOf(
        "disabled",
        "not_configured",
        "stream_not_supported",
        "failed_or_empty",
        "stream_failed_or_empty",
    )
