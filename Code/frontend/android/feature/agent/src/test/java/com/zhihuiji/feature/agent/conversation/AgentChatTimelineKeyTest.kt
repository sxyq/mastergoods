package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.AnswerTraceStatus
import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.DraftTrace
import com.zhihuiji.core.model.v2.agent.MessageRole
import com.zhihuiji.core.model.v2.agent.PlanStep
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.RunTraceItem
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentChatTimelineKeyTest {

    private fun baseMessage(
        isStreaming: Boolean = false,
        isError: Boolean = false,
        errorMessage: String? = null,
        answerDeltaSource: String? = null,
    ) = ChatMessage(
        id = "m1",
        conversationId = 1L,
        role = MessageRole.ASSISTANT,
        content = "你好",
        isStreaming = isStreaming,
        isError = isError,
        errorMessage = errorMessage,
        answerDeltaSource = answerDeltaSource,
        createdAt = 1_000L,
    )

    private fun tool(
        name: String = "sales_trend",
        status: ToolCallStatus = ToolCallStatus.RUNNING,
        progress: String? = null,
        summary: String? = null,
        timestamp: Long = 2_000L,
    ) = ToolCallRecord(
        toolName = name,
        status = status,
        progressMessage = progress,
        resultSummary = summary,
        timestamp = timestamp,
    )

    @Test
    fun answerStatus_streamingToCompleted_updatesDisplayKeyNotStructureKey() {
        val tools = listOf(tool())
        val streamingTrace = RunTrace(runId = "r1", answerStatus = AnswerTraceStatus.STREAMING, toolCalls = tools)
        val completedTrace = RunTrace(runId = "r1", answerStatus = AnswerTraceStatus.COMPLETED, toolCalls = tools)

        // 文本增量不改结构键
        val textOnlyA = traceStructureKeyOf(baseMessage(isStreaming = true), streamingTrace)
        val textOnlyB = traceStructureKeyOf(baseMessage(isStreaming = true).copy(content = "更多文本"), streamingTrace)
        assertEquals(textOnlyA, textOnlyB)

        // Answer 状态变化必须更新显示键
        val a1 = answerDisplayKeyOf(baseMessage(isStreaming = true), streamingTrace, emptyList())
        val a2 = answerDisplayKeyOf(
            baseMessage(isStreaming = false),
            completedTrace,
            listOf(ChatMessagePart.Text("完整回答")),
        )
        assertEquals(AnswerTraceStatus.STREAMING, a1.answerStatus)
        assertEquals(AnswerTraceStatus.COMPLETED, a2.answerStatus)
        assertNotEquals(a1, a2)
    }

    @Test
    fun answerStatus_failedCancelledConfirmationPending_driveDisplayKey() {
        val cases = listOf(
            AnswerTraceStatus.FAILED,
            AnswerTraceStatus.CANCELLED,
            AnswerTraceStatus.CONFIRMATION_PENDING,
        )
        for (status in cases) {
            val message = baseMessage(isError = status == AnswerTraceStatus.FAILED, errorMessage = "x")
            val key = answerDisplayKeyOf(
                message,
                RunTrace(runId = "r1", answerStatus = status),
                emptyList(),
            )
            assertEquals(status, key.answerStatus)
        }
    }

    @Test
    fun answerDeltaSourceChange_updatesDisplayKey() {
        val traceA = RunTrace(runId = "r1", answerDeltaSource = "model_stream")
        val traceB = RunTrace(runId = "r1", answerDeltaSource = "non_stream_retry")
        val message = baseMessage(isStreaming = true)
        val a = answerDisplayKeyOf(message, traceA, emptyList())
        val b = answerDisplayKeyOf(message, traceB, emptyList())
        assertEquals("model_stream", a.answerDeltaSource)
        assertEquals("non_stream_retry", b.answerDeltaSource)
        assertNotEquals(a, b)
    }

    @Test
    fun toolStatusChangeDuringStreaming_updatesSkeletonStructureKey() {
        val running = RunTrace(
            runId = "r1",
            toolCalls = listOf(tool(status = ToolCallStatus.RUNNING, progress = "查询中")),
        )
        val completed = RunTrace(
            runId = "r1",
            toolCalls = listOf(tool(status = ToolCallStatus.COMPLETED, summary = "完成 3 条")),
        )
        val message = baseMessage(isStreaming = true)
        assertNotEquals(
            traceStructureKeyOf(message, running),
            traceStructureKeyOf(message, completed),
        )
        val skeleton = buildTraceSkeleton(message, completed)
        val tools = skeleton.filterIsInstance<RunTraceItem.Tool>()
        assertEquals(1, tools.size)
        assertEquals(ToolCallStatus.COMPLETED, tools.single().call.status)
    }

    @Test
    fun newResultBlock_attachesOnceWithoutDuplicates() {
        val block = ResultBlockDto(blockType = "table", title = "销售明细")
        val message = baseMessage(isStreaming = false)
        val trace = RunTrace(runId = "r1", answerStatus = AnswerTraceStatus.COMPLETED)
        val skeleton = buildTraceSkeleton(message, trace)
        val parts = listOf(ChatMessagePart.ResultBlock(block))
        val first = attachStreamingTimelineItems(skeleton, message, trace, parts)
        val second = attachStreamingTimelineItems(first.filterNot { it is RunTraceItem.Answer }, message, trace, parts)
        val blocks = second.filterIsInstance<RunTraceItem.ResultBlock>()
        assertEquals(1, blocks.size)
        assertEquals(block, blocks.single().block)
    }

    @Test
    fun skeletonRebuild_ignoresAnswerOnlyTimelineItemsButKeepsTools() {
        val answerOnly = RunTrace(
            runId = "r1",
            timeline = listOf(
                RunTraceItem.Answer(id = "answer", status = AnswerTraceStatus.STREAMING, timestamp = 1L),
            ),
            toolCalls = listOf(tool()),
        )
        val message = baseMessage(isStreaming = true)
        val key1 = traceStructureKeyOf(message, answerOnly)
        val key2 = traceStructureKeyOf(
            message,
            answerOnly.copy(
                timeline = listOf(
                    RunTraceItem.Answer(id = "answer", status = AnswerTraceStatus.COMPLETED, timestamp = 2L),
                ),
            ),
        )
        assertEquals(key1.structuralTimeline, key2.structuralTimeline)
        assertTrue(key1.toolCalls.isNotEmpty())
    }

    @Test
    fun draftAndSafetyChanges_updateStructureKey() {
        val message = baseMessage(isStreaming = true)
        val base = RunTrace(runId = "r1")
        val withDraft = base.copy(
            draft = DraftTrace(draftId = 9L, draftType = "sale", title = "草稿", status = "pending", timestamp = 3L),
        )
        assertNotEquals(traceStructureKeyOf(message, base), traceStructureKeyOf(message, withDraft))
    }
}
