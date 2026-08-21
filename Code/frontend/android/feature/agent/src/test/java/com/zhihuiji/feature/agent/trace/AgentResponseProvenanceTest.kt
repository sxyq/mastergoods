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
    fun visibleAssistantPartsKeepOnlyAnswerTextAndRealResultBlocks() {
        val block = ResultBlockDto(blockType = "table", title = "真实销售明细")
        val parts = listOf(
            ChatMessagePart.Text(" "),
            ChatMessagePart.PendingResultBlock(block),
            ChatMessagePart.Text("正式回答"),
            ChatMessagePart.ResultBlock(block),
        )

        assertEquals(
            listOf(ChatMessagePart.Text("正式回答"), ChatMessagePart.ResultBlock(block)),
            parts.visibleAssistantParts(),
        )
    }

    @Test
    fun pendingResultOnlyDoesNotCreateVisibleAssistantContent() {
        val block = ResultBlockDto(blockType = "line_chart", title = "销售趋势")
        val message = ChatMessage(
            id = "assistant-pending",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            parts = listOf(ChatMessagePart.PendingResultBlock(block)),
            isStreaming = true,
        )

        assertTrue(message.displayParts().visibleAssistantParts().isEmpty())
        assertFalse(message.hasVisibleAssistantTimeline())
    }

    @Test
    fun storedStructuredOnlyAnswerStillRendersItsRealBlock() {
        val block = ResultBlockDto(blockType = "kpi_grid", title = "真实指标")
        val message = ChatMessage(
            id = "assistant-block-only",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            blocks = listOf(block),
        )

        assertEquals(
            listOf(ChatMessagePart.ResultBlock(block)),
            message.displayParts().visibleAssistantParts(),
        )
        assertTrue(message.hasVisibleAssistantTimeline())
    }

    @Test
    fun interruptedModeRemainsMachineReadableOnly() {
        assertTrue(isStreamInterruptedMode("tool_query_llm_stream_interrupted", "streaming"))
        assertTrue(isStreamInterruptedMode("tool_query_llm_stream", "stream_interrupted"))
        assertFalse(isStreamInterruptedMode("tool_query_llm_stream", "streaming"))
    }

    @Test
    fun answerTimelineCountsTextAndResultButNotPendingProcessData() {
        val block = ResultBlockDto(blockType = "table", title = "客户")
        val textMessage = ChatMessage(
            id = "assistant-text",
            conversationId = 1L,
            role = MessageRole.ASSISTANT,
            parts = listOf(ChatMessagePart.Text("正式回答")),
        )
        val resultMessage = textMessage.copy(
            parts = listOf(ChatMessagePart.ResultBlock(block)),
        )
        val pendingMessage = textMessage.copy(
            content = "",
            parts = listOf(ChatMessagePart.PendingResultBlock(block)),
        )

        assertTrue(textMessage.hasVisibleAssistantTimeline())
        assertTrue(resultMessage.hasVisibleAssistantTimeline())
        assertFalse(pendingMessage.hasVisibleAssistantTimeline())
    }

}
