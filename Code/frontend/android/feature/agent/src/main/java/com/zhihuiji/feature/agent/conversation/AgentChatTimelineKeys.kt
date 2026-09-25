// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.AnswerTraceStatus
import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.DraftTrace
import com.zhihuiji.core.model.v2.agent.PlanStep
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.RunTraceAuditState
import com.zhihuiji.core.model.v2.agent.RunTraceItem
import com.zhihuiji.core.model.v2.agent.RunTerminalStatus
import com.zhihuiji.core.model.v2.agent.SafetyResult
import com.zhihuiji.core.model.v2.agent.TerminalTrace
import com.zhihuiji.core.model.v2.agent.ToolCallRecord

/**
 * 骨架纯派生结构键。
 *
 * 直接持有不可变模型引用（含 DraftTrace.title、ToolCallRecord.queryWindow/totalCount/limit/
 * isTruncated/evidence 等全部字段），用 data equals 比较，避免每次流式重组拼接大量字符串。
 * 不绑定完整 RunTrace 对象本身，也不把 Answer 当作结构变化；trace.timeline 的 ResultBlock
 * 是骨架输入，必须参与本键，否则 trace 结果块变化时会复用旧骨架并显示旧块。
 */
internal class TraceStructureKey(
    internal val messageId: String,
    internal val createdAt: Long,
    internal val isStreaming: Boolean,
    internal val errorMessage: String?,
    internal val planSource: String?,
    internal val planSteps: List<PlanStep>,
    internal val safetyResult: SafetyResult?,
    internal val toolCalls: List<ToolCallRecord>,
    internal val draft: DraftTrace?,
    internal val terminal: TerminalTrace?,
    internal val derivedTerminal: RunTerminalStatus?,
    internal val auditState: RunTraceAuditState?,
    internal val auditWarnings: List<String>,
    internal val structuralTimeline: List<RunTraceItem>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TraceStructureKey) return false
        return messageId == other.messageId &&
            createdAt == other.createdAt &&
            isStreaming == other.isStreaming &&
            errorMessage == other.errorMessage &&
            planSource == other.planSource &&
            planSteps == other.planSteps &&
            safetyResult == other.safetyResult &&
            toolCalls == other.toolCalls &&
            draft == other.draft &&
            terminal == other.terminal &&
            derivedTerminal == other.derivedTerminal &&
            auditState == other.auditState &&
            auditWarnings == other.auditWarnings &&
            structuralTimeline == other.structuralTimeline
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + isStreaming.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (planSource?.hashCode() ?: 0)
        result = 31 * result + planSteps.hashCode()
        result = 31 * result + (safetyResult?.hashCode() ?: 0)
        result = 31 * result + toolCalls.hashCode()
        result = 31 * result + (draft?.hashCode() ?: 0)
        result = 31 * result + (terminal?.hashCode() ?: 0)
        result = 31 * result + (derivedTerminal?.hashCode() ?: 0)
        result = 31 * result + (auditState?.hashCode() ?: 0)
        result = 31 * result + auditWarnings.hashCode()
        result = 31 * result + structuralTimeline.hashCode()
        return result
    }
}

/**
 * Answer 显示键：流式状态与 ResultBlock 变化驱动 attach。
 * ResultBlock 直接持有 DTO 引用，覆盖 renderCacheIdentity 所依赖的全部字段。
 */
internal class AnswerDisplayKey(
    internal val isStreaming: Boolean,
    internal val isError: Boolean,
    internal val errorMessage: String?,
    internal val answerStatus: AnswerTraceStatus,
    internal val answerDeltaSource: String?,
    internal val resultBlocks: List<ResultBlockDto>,
    internal val hasVisibleParts: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnswerDisplayKey) return false
        return isStreaming == other.isStreaming &&
            isError == other.isError &&
            errorMessage == other.errorMessage &&
            answerStatus == other.answerStatus &&
            answerDeltaSource == other.answerDeltaSource &&
            resultBlocks == other.resultBlocks &&
            hasVisibleParts == other.hasVisibleParts
    }

    override fun hashCode(): Int {
        var result = isStreaming.hashCode()
        result = 31 * result + isError.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + answerStatus.hashCode()
        result = 31 * result + (answerDeltaSource?.hashCode() ?: 0)
        result = 31 * result + resultBlocks.hashCode()
        result = 31 * result + hasVisibleParts.hashCode()
        return result
    }
}

/**
 * 骨架输入中可被结构键忽略的只有 Answer：流式文本增量由显示键驱动，不重建 plan/tool。
 * ResultBlock 不能一并排除——buildTraceSkeleton 直接读取 trace.timeline 的结果块，
 * 排除会让 trace 结果块变化而 message visibleParts 未变时继续复用旧骨架。
 */
private fun structuralTimelineOf(timeline: List<RunTraceItem>): List<RunTraceItem> {
    if (timeline.isEmpty()) return timeline
    var hasAnswer = false
    for (item in timeline) {
        if (item is RunTraceItem.Answer) {
            hasAnswer = true
            break
        }
    }
    if (!hasAnswer) return timeline
    return timeline.filterNot { it is RunTraceItem.Answer }
}

internal fun traceStructureKeyOf(message: ChatMessage, trace: RunTrace?): TraceStructureKey {
    val timeline = trace?.timeline.orEmpty()
    return TraceStructureKey(
        messageId = message.id,
        createdAt = message.createdAt,
        isStreaming = message.isStreaming,
        errorMessage = message.errorMessage,
        planSource = trace?.planSource,
        planSteps = trace?.planSteps.orEmpty(),
        safetyResult = trace?.safetyResult,
        // ToolCallRecord 为 data class，equals 覆盖 queryWindow/totalCount/limit/isTruncated/evidence 等全部字段。
        toolCalls = trace?.toolCalls.orEmpty(),
        // DraftTrace 为 data class，equals 覆盖 title/status/timestamp 等。
        draft = trace?.draft,
        terminal = trace?.terminal,
        derivedTerminal = trace?.terminal?.status
            ?: message.assistantTerminalStatus(trace),
        auditState = trace?.auditState,
        auditWarnings = trace?.auditWarnings.orEmpty(),
        structuralTimeline = structuralTimelineOf(timeline),
    )
}

internal fun answerDisplayKeyOf(
    message: ChatMessage,
    trace: RunTrace?,
    visibleParts: List<ChatMessagePart>,
): AnswerDisplayKey {
    var blocks: MutableList<ResultBlockDto>? = null
    for (part in visibleParts) {
        if (part is ChatMessagePart.ResultBlock) {
            val list = blocks ?: mutableListOf<ResultBlockDto>().also { blocks = it }
            list.add(part.block)
        }
    }
    return AnswerDisplayKey(
        isStreaming = message.isStreaming,
        isError = message.isError,
        errorMessage = message.errorMessage,
        answerStatus = message.effectiveAnswerTraceStatus(trace, visibleParts),
        answerDeltaSource = trace?.answerDeltaSource ?: message.answerDeltaSource,
        resultBlocks = blocks ?: emptyList(),
        hasVisibleParts = visibleParts.isNotEmpty(),
    )
}
