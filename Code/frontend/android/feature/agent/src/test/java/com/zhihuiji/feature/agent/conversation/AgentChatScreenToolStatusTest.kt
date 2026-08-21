package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentChatScreenToolStatusTest {

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

        assertEquals(
            calls,
            calls.closeOpenToolCalls(
                resultSummary = "运行已结束，未收到工具完成事件",
                completedAt = 10_000L,
            ),
        )
    }

    @Test
    fun runTraceAnswerDeltaSourceOnlyCopiesWhenSourceChanges() {
        val trace = RunTrace(runId = "run-1", answerDeltaSource = DeltaSourceModelStream)

        assertTrue(trace.withAnswerDeltaSourceIfChanged(DeltaSourceModelStream) === trace)
        assertTrue(trace.withAnswerDeltaSourceIfChanged(null) === trace)
        assertEquals("other", trace.withAnswerDeltaSourceIfChanged("other").answerDeltaSource)
    }

    @Test
    fun onlyModelDeltasAreVisibleAssistantText() {
        assertTrue(null.isVisibleAnswerDeltaSource())
        assertTrue(DeltaSourceModelStream.isVisibleAnswerDeltaSource())
        assertFalse("non_stream_retry".isVisibleAnswerDeltaSource())
        assertFalse("rule_summary".isVisibleAnswerDeltaSource())
        assertFalse("server_notice".isVisibleAnswerDeltaSource())
    }

    @Test
    fun directReplyBoxRendersTextAndRealResultsTogether() {
        val block = ResultBlockDto(blockType = "line_chart", title = "真实销售趋势")
        val parts = listOf(
            ChatMessagePart.Text("共有 3 个商品。"),
            ChatMessagePart.ResultBlock(block),
        )

        assertEquals(parts, parts.visibleAssistantParts())
    }

    @Test
    fun orderedAssistantPartsKeepsTextAndResultBlockOrder() {
        val firstBlock = ResultBlockDto(blockType = "table", title = "销售明细")
        val secondBlock = ResultBlockDto(blockType = "line_chart", title = "销售趋势")
        val parts = listOf(
            ChatMessagePart.Text("先看明细。"),
            ChatMessagePart.ResultBlock(firstBlock),
            ChatMessagePart.Text("再看趋势。"),
        )

        assertEquals(
            parts + ChatMessagePart.ResultBlock(secondBlock),
            orderedAssistantParts(
                visibleParts = parts,
                traceBlocks = listOf(firstBlock, secondBlock),
            ),
        )
    }

    @Test
    fun processPlaceholderNeverEntersDirectReplyBox() {
        val block = ResultBlockDto(blockType = "table", title = "销售明细")
        val parts = listOf(ChatMessagePart.PendingResultBlock(block))

        assertTrue(parts.visibleAssistantParts().isEmpty())
    }

    @Test
    fun toolActivityIsHiddenWhenNoToolWasCalled() {
        assertEquals(null, emptyList<ToolCallRecord>().assistantToolActivitySummary())
    }

    @Test
    fun toolActivityOnlyReportsExecutionStateAndSafeLabels() {
        val calls = listOf(
            tool("inventory_low_stock_lookup", ToolCallStatus.COMPLETED, resultSummary = "命中 0 个商品"),
            tool("result_visualization", ToolCallStatus.RUNNING),
        )

        assertEquals("正在查询 2 个数据源", calls.assistantToolActivitySummary())
        assertEquals("查询库存数据", calls.first().userFacingToolLabel())
        assertEquals("命中 0 个商品", calls.first().userFacingToolOutcome())
        assertEquals("整理图表数据", calls.last().userFacingToolLabel())
        assertEquals("正在查询", calls.last().userFacingToolOutcome())
    }

    @Test
    fun streamAutoFollowDoesNotTriggerForEmptyTimeline() {
        assertFalse(
            shouldAutoFollowStream(
                messageCount = 0,
                lastVisibleItemIndex = null,
                visibleItemCount = 0,
            ),
        )
    }

    @Test
    fun streamAutoFollowAllowsInitialLayoutToReachLatestMessage() {
        assertTrue(
            shouldAutoFollowStream(
                messageCount = 2,
                lastVisibleItemIndex = null,
                visibleItemCount = 0,
            ),
        )
    }

    @Test
    fun streamAutoFollowKeepsUserReadingOlderMessages() {
        assertFalse(
            shouldAutoFollowStream(
                messageCount = 12,
                lastVisibleItemIndex = 6,
                visibleItemCount = 5,
            ),
        )
    }

    @Test
    fun streamAutoFollowContinuesWhenUserIsNearBottom() {
        assertTrue(
            shouldAutoFollowStream(
                messageCount = 12,
                lastVisibleItemIndex = 10,
                visibleItemCount = 5,
            ),
        )
    }

    private fun tool(
        name: String,
        status: ToolCallStatus,
        completedAt: Long? = null,
        resultSummary: String? = null,
        inputSummary: String? = null,
    ): ToolCallRecord = ToolCallRecord(
        toolName = name,
        status = status,
        completedAt = completedAt,
        resultSummary = resultSummary,
        inputSummary = inputSummary,
        timestamp = 1L,
    )
}
