package com.zhihuiji.feature.agent

import org.junit.Assert.assertEquals
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
        assertEquals("服务端返回规则摘要", DeltaSourceRuleSummary.headerStatusLabel())
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
}
