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
        val calls = listOf(
            tool("cashflow_summary", ToolCallStatus.COMPLETED),
            tool("inventory_flow", ToolCallStatus.COMPLETED),
        )

        assertNull(calls.latestVisibleToolCall())
    }

    @Test
    fun readableToolNameUsesBusinessLabelsForKnownTools() {
        assertEquals("销售趋势", "sales_trend".readableToolName())
        assertEquals("custom tool", "custom_tool".readableToolName())
    }

    private fun tool(
        name: String,
        status: ToolCallStatus,
    ): ToolCallRecord = ToolCallRecord(
        toolName = name,
        status = status,
        timestamp = 1L,
    )
}
