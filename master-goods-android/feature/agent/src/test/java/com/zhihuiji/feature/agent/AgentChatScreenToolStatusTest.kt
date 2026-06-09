package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AgentChatScreenToolStatusTest {

    @Test
    fun latestVisibleToolPrefersRunningToolOverCompletedEvidence() {
        val calls = listOf(
            tool("cashflow_summary", ToolCallStatus.COMPLETED),
            tool("sales_trend", ToolCallStatus.RUNNING),
        )

        assertEquals("sales_trend", calls.latestVisibleToolCall()?.toolName)
    }

    @Test
    fun latestVisibleToolPrefersRunningToolEvenWhenCompletedEventArrivesLater() {
        val now = 10_000L
        val calls = listOf(
            tool("sales_trend", ToolCallStatus.RUNNING),
            tool("inventory_flow", ToolCallStatus.COMPLETED, completedAt = 9_900L),
        )

        assertEquals("sales_trend", calls.latestVisibleToolCall(now)?.toolName)
    }

    @Test
    fun latestVisibleToolUsesNewestActiveToolWhenMultipleAreRunning() {
        val calls = listOf(
            tool("sales_trend", ToolCallStatus.RUNNING),
            tool("inventory_flow", ToolCallStatus.PENDING),
        )

        assertEquals("inventory_flow", calls.latestVisibleToolCall()?.toolName)
    }

    @Test
    fun latestVisibleToolDoesNotKeepCompletedToolAsPersistentPill() {
        val now = 10_000L
        val calls = listOf(
            tool("cashflow_summary", ToolCallStatus.COMPLETED, completedAt = 1_000L),
            tool("inventory_flow", ToolCallStatus.COMPLETED, completedAt = 2_000L),
        )

        assertNull(calls.latestVisibleToolCall(now))
    }

    @Test
    fun latestVisibleToolShowsRecentlyCompletedToolBriefly() {
        val now = 10_000L
        val calls = listOf(
            tool("cashflow_summary", ToolCallStatus.COMPLETED, completedAt = 7_000L),
            tool("inventory_flow", ToolCallStatus.COMPLETED, completedAt = 9_200L),
        )

        assertEquals("inventory_flow", calls.latestVisibleToolCall(now)?.toolName)
    }

    @Test
    fun latestVisibleToolDoesNotKeepFailedToolAsPersistentPill() {
        val now = 10_000L
        val calls = listOf(
            tool("sales_trend", ToolCallStatus.FAILED, completedAt = 1_000L),
        )

        assertNull(calls.latestVisibleToolCall(now))
    }

    @Test
    fun latestVisibleToolShowsRecentlyFailedToolBriefly() {
        val now = 10_000L
        val calls = listOf(
            tool("sales_trend", ToolCallStatus.FAILED, completedAt = 9_300L),
        )

        assertEquals("sales_trend", calls.latestVisibleToolCall(now)?.toolName)
    }

    @Test
    fun closeOpenToolCallsStopsRunningPillsAfterRunTerminalEvent() {
        val calls = listOf(
            tool("sales_trend", ToolCallStatus.RUNNING),
            tool("inventory_flow", ToolCallStatus.PENDING),
        )

        val closed = calls.closeOpenToolCalls(
            resultSummary = "运行已结束，未收到工具完成事件",
            completedAt = 10_000L,
        )

        assertEquals(ToolCallStatus.FAILED, closed[0].status)
        assertEquals(ToolCallStatus.FAILED, closed[1].status)
        assertEquals("运行已结束，未收到工具完成事件", closed[0].resultSummary)
        assertEquals("运行已结束，未收到工具完成事件", closed[1].resultSummary)
        assertNull(closed.latestVisibleToolCall(16_001L))
    }

    @Test
    fun closeOpenToolCallsKeepsExistingSummaryButRemovesActiveStatus() {
        val calls = listOf(
            tool(
                name = "sales_trend",
                status = ToolCallStatus.RUNNING,
                resultSummary = "已查询 3 条销售记录",
            ),
        )

        val closed = calls.closeOpenToolCalls(
            resultSummary = "生成已取消，工具查询已停止",
            completedAt = 10_000L,
        )

        assertEquals(ToolCallStatus.FAILED, closed.single().status)
        assertEquals("已查询 3 条销售记录", closed.single().resultSummary)
    }

    @Test
    fun closeOpenToolCallsKeepsRealCompletedAndFailedToolEvents() {
        val calls = listOf(
            tool("cashflow_summary", ToolCallStatus.COMPLETED, completedAt = 9_000L),
            tool("sales_trend", ToolCallStatus.FAILED, completedAt = 9_100L),
        )

        val closed = calls.closeOpenToolCalls(
            resultSummary = "运行已结束，未收到工具完成事件",
            completedAt = 10_000L,
        )

        assertEquals(calls, closed)
    }

    @Test
    fun runTraceAnswerDeltaSourceOnlyCopiesWhenSourceChanges() {
        val trace = RunTrace(runId = "run-1", answerDeltaSource = "model_stream")

        assertSame(trace, trace.withAnswerDeltaSourceIfChanged("model_stream"))
        assertSame(trace, trace.withAnswerDeltaSourceIfChanged(null))
        assertEquals(
            "server_notice",
            trace.withAnswerDeltaSourceIfChanged("server_notice").answerDeltaSource
        )
    }

    @Test
    fun readableToolNameUsesBusinessLabelsForKnownTools() {
        assertEquals("销售趋势", "sales_trend".readableToolName())
        assertEquals("custom tool", "custom_tool".readableToolName())
    }

    @Test
    fun resultBlockTimingLabelSeparatesStreamingAndCompletedMessages() {
        assertEquals("实时结果", resultBlockTimingLabel(isStreaming = true))
        assertEquals("查询结果", resultBlockTimingLabel(isStreaming = false))
    }

    @Test
    fun resultBlockSourceLabelDistinguishesStructuredEvidenceAndMarkdown() {
        assertEquals(
            "实时结果 · 结构化查询",
            resultBlockSourceLabel(blockType = "line_chart", isStreaming = true)
        )
        assertEquals(
            "查询结果 · 工具证据",
            resultBlockSourceLabel(blockType = "evidence_card", isStreaming = false)
        )
        assertEquals(
            "查询结果 · Markdown 结果块",
            resultBlockSourceLabel(blockType = "markdown", isStreaming = false)
        )
    }

    @Test
    fun assistantTextSourceLabelMarksModelSummarySeparatelyFromResultBlocks() {
        assertEquals("AI 总结", assistantTextSourceLabel())
        assertFalse(resultBlockSourceLabel(blockType = "markdown", isStreaming = false).contains("AI 总结"))
    }

    @Test
    fun pendingResultNoticeDoesNotClaimLoadingAfterStreamEnds() {
        assertEquals("已取得真实结果，正在组织回答", pendingResultBlockNoticeText(isStreaming = true))
        assertEquals("查询结果已返回，未收到可读回答", pendingResultBlockNoticeText(isStreaming = false))
    }

    @Test
    fun emptyChatCopyUsesUserFacingAgentLanguage() {
        assertEquals(
            "发送问题后，AI 会按当前账号权限查询真实业务数据，并返回 Markdown、表格或统计图。",
            emptyChatHelperText()
        )
        assertEquals(listOf("真实查询", "流式回答", "图表结果"), emptyChatPills())
        assertFalse(emptyChatHelperText().contains("服务端"))
        assertFalse(emptyChatPills().any { it.contains("服务端") || it.contains("模型流") })
    }

    private fun tool(
        name: String,
        status: ToolCallStatus,
        completedAt: Long? = null,
        resultSummary: String? = null,
    ): ToolCallRecord = ToolCallRecord(
        toolName = name,
        status = status,
        completedAt = completedAt,
        resultSummary = resultSummary,
        timestamp = 1L,
    )
}
