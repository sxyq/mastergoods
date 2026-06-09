package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    ): ToolCallRecord = ToolCallRecord(
        toolName = name,
        status = status,
        completedAt = completedAt,
        timestamp = 1L,
    )
}
