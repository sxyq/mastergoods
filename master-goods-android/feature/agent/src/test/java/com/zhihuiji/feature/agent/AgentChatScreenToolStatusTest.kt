package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import org.junit.Assert.assertEquals
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
    fun readableToolNameUsesBusinessLabelsForKnownTools() {
        assertEquals("销售趋势", "sales_trend".readableToolName())
        assertEquals("custom tool", "custom_tool".readableToolName())
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
