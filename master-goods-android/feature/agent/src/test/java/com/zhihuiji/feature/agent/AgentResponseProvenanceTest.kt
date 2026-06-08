package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.MessageRole
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
}
