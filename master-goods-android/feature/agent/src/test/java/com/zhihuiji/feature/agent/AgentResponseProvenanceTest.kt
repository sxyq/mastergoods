package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.MessageRole
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RunTrace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentResponseProvenanceTest {

    @Test
    fun completedAnswerWithoutServerDeltaIsNotLabeledAsStreaming() {
        val status = assistantHeaderStatusLabel(
            isStreaming = false,
            hasServerAnswerDelta = false,
            answerDeltaSource = null,
            hasToolEvidence = true,
            hasAuditTrace = true,
        )
        val provenance = assistantProvenanceLabel(
            hasCompletedTool = true,
            hasToolEvidence = true,
            answerDeltaSource = null,
        )

        assertEquals("服务端回复结果", status)
        assertEquals("工具完成", provenance)
    }

    @Test
    fun modelStreamDeltaIsTheOnlyModelStreamingLabel() {
        assertEquals("模型正在流式生成", DeltaSourceModelStream.headerStatusLabel())
        assertEquals("模型流式回复", DeltaSourceModelStream.headerStatusLabel(isStreaming = false))
        assertEquals("模型实时输出中", DeltaSourceModelStream.inlineStreamingLabel())
        assertEquals(
            "模型流",
            assistantProvenanceLabel(
                hasCompletedTool = false,
                hasToolEvidence = false,
                answerDeltaSource = DeltaSourceModelStream,
            )
        )
    }

    @Test
    fun ruleSummaryIsLabeledAsServerSummaryNotModelStream() {
        assertEquals("数据查询 / 规则摘要模式", DeltaSourceRuleSummary.headerStatusLabel())
        assertEquals("正在展示服务端规则摘要", DeltaSourceRuleSummary.inlineStreamingLabel())
        assertEquals(
            "服务端摘要",
            assistantProvenanceLabel(
                hasCompletedTool = false,
                hasToolEvidence = false,
                answerDeltaSource = DeltaSourceRuleSummary,
            )
        )
    }

    @Test
    fun inlineStreamingStatusOnlyShowsWhileAssistantIsStreaming() {
        val completedModelAnswer = ChatMessage(
            id = "assistant-1",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            content = "已经完成回答",
            isStreaming = false,
            hasServerAnswerDelta = true,
            answerDeltaSource = DeltaSourceModelStream,
        )
        val streamingModelAnswer = completedModelAnswer.copy(isStreaming = true)
        val waitingForServerEvent = completedModelAnswer.copy(
            content = "",
            isStreaming = true,
            hasServerAnswerDelta = false,
            answerDeltaSource = null,
        )

        assertFalse(completedModelAnswer.shouldShowInlineStreamingStatus())
        assertTrue(streamingModelAnswer.shouldShowInlineStreamingStatus())
        assertTrue(waitingForServerEvent.shouldShowInlineStreamingStatus())
    }

    @Test
    fun assistantHeaderBadgesAreHiddenAfterCompletion() {
        val completedAnswer = ChatMessage(
            id = "assistant-2",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            content = "已经完成回答",
            isStreaming = false,
            hasServerAnswerDelta = true,
            answerDeltaSource = DeltaSourceModelStream,
        )
        val streamingAnswer = completedAnswer.copy(isStreaming = true)

        assertFalse(completedAnswer.shouldShowAssistantHeaderBadges())
        assertTrue(streamingAnswer.shouldShowAssistantHeaderBadges())
        assertEquals(
            "有运行标识",
            assistantReviewBadgeLabel(
                isStreaming = true,
                hasAuditTrace = true,
                hasToolEvidence = true,
            )
        )
    }

    @Test
    fun completedSuccessAnswerHidesHeaderButRuleSummaryKeepsIt() {
        val completedSuccess = ChatMessage(
            id = "assistant-3",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            content = "已经完成回答",
            isStreaming = false,
            hasServerAnswerDelta = true,
            answerDeltaSource = DeltaSourceModelStream,
            runTrace = RunTrace(
                runId = "run-1",
                mode = "tool_query_llm_streamed",
                llmStatus = "streaming",
            ),
        )
        val completedRuleSummary = completedSuccess.copy(
            hasServerAnswerDelta = false,
            answerDeltaSource = null,
            runTrace = RunTrace(
                runId = "run-2",
                mode = "tool_query_rule_summary",
                llmStatus = "disabled",
            ),
        )

        assertFalse(completedSuccess.shouldShowAssistantHeader())
        assertTrue(completedRuleSummary.shouldShowAssistantHeader())
        assertEquals(
            "数据查询 / 规则摘要模式",
            assistantHeaderStatusLabel(
                isStreaming = false,
                hasServerAnswerDelta = false,
                answerDeltaSource = null,
                hasToolEvidence = true,
                hasAuditTrace = true,
                mode = "tool_query_rule_summary",
                llmStatus = "disabled",
            )
        )
    }

    @Test
    fun runTracePanelHidesForCompletedSuccessUnlessAttentionIsNeeded() {
        val completedSuccess = ChatMessage(
            id = "assistant-4",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            content = "已经完成回答",
            isStreaming = false,
            hasServerAnswerDelta = true,
            answerDeltaSource = DeltaSourceModelStream,
            runTrace = RunTrace(
                runId = "run-1",
                mode = "tool_query_llm_streamed",
                llmStatus = "streaming",
                isExpanded = false,
            ),
        )
        val streamingBeforeVisibleTimeline = completedSuccess.copy(
            content = "",
            isStreaming = true,
        )
        val error = completedSuccess.copy(isError = true)
        val expanded = completedSuccess.copy(runTrace = completedSuccess.runTrace?.copy(isExpanded = true))
        val ruleSummary = completedSuccess.copy(
            hasServerAnswerDelta = false,
            answerDeltaSource = null,
            runTrace = completedSuccess.runTrace?.copy(
                mode = "tool_query_rule_summary",
                llmStatus = "disabled",
            ),
        )

        assertFalse(completedSuccess.shouldShowRunTracePanel())
        assertTrue(streamingBeforeVisibleTimeline.shouldShowRunTracePanel())
        assertTrue(error.shouldShowRunTracePanel())
        assertTrue(expanded.shouldShowRunTracePanel())
        assertFalse(ruleSummary.shouldShowRunTracePanel())
    }

    @Test
    fun streamingRunTracePanelCollapsesAfterVisibleTimelineArrives() {
        val waitingForFirstVisibleEvent = ChatMessage(
            id = "assistant-5",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
            runTrace = RunTrace(runId = "run-1"),
        )
        val resultBlockVisible = waitingForFirstVisibleEvent.copy(
            parts = listOf(ChatMessagePart.ResultBlock(ResultBlockDto(blockType = "kpi_grid", title = "指标")))
        )
        val textVisible = waitingForFirstVisibleEvent.copy(
            parts = listOf(ChatMessagePart.Text("已经开始回答"))
        )

        assertTrue(waitingForFirstVisibleEvent.shouldShowRunTracePanel())
        assertFalse(resultBlockVisible.shouldShowRunTracePanel())
        assertFalse(textVisible.shouldShowRunTracePanel())
    }
}
