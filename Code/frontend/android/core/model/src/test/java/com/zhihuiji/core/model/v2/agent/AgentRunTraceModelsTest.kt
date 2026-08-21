package com.zhihuiji.core.model.v2.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRunTraceModelsTest {

    @Test
    fun reduceAuditRestoresFlatPlanToolResultAnswerAndTerminal() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-1",
                conversationId = 42L,
                status = "completed",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-plan",
                        seq = 1,
                        eventType = "plan_delta",
                        content = "查询低库存商品",
                        createdAt = 100L,
                    ),
                    AgentTraceEventDto(
                        eventId = "event-tool",
                        seq = 2,
                        eventType = "tool_completed",
                        toolName = "inventory_low_stock_lookup",
                        inputSummary = "查询低库存商品",
                        resultSummary = "命中 2 个商品",
                        returnedCount = 2,
                        createdAt = 200L,
                    ),
                    AgentTraceEventDto(
                        eventId = "event-answer",
                        seq = 3,
                        eventType = "answer_completed",
                        content = "共有 2 个低库存商品。",
                        createdAt = 300L,
                    ),
                    AgentTraceEventDto(
                        eventId = "event-terminal",
                        seq = 4,
                        eventType = "run_completed",
                        content = "共有 2 个低库存商品。",
                        createdAt = 400L,
                    ),
                ),
            ),
        )

        assertTrue(reduced.timeline.any { it is RunTraceItem.PlanSummary })
        val tool = reduced.toolCalls.single()
        assertEquals("inventory_low_stock_lookup", tool.toolName)
        assertEquals("命中 2 个商品", tool.resultSummary)
        assertEquals(2, tool.returnedCount)
        assertTrue(reduced.timeline.any { it is RunTraceItem.Tool })
        assertEquals(AnswerTraceStatus.COMPLETED, reduced.answerStatus)
        assertEquals(RunTerminalStatus.COMPLETED, reduced.terminal?.status)
        assertEquals("run-1", reduced.runId)
    }

    @Test
    fun reduceAuditPreservesHistoricalAnswerDeltaSource() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-retry",
                status = "completed",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-delta",
                        seq = 1,
                        eventType = "answer_delta",
                        content = "最近没有匹配到采购单。",
                        deltaSource = "non_stream_retry",
                        createdAt = 100L,
                    ),
                    AgentTraceEventDto(
                        eventId = "event-answer",
                        seq = 2,
                        eventType = "answer_completed",
                        content = "最近没有匹配到采购单。",
                        createdAt = 200L,
                    ),
                ),
            ),
        )

        assertEquals("non_stream_retry", reduced.answerDeltaSource)
        assertEquals(
            "non_stream_retry",
            reduced.timeline.filterIsInstance<RunTraceItem.Answer>().last().deltaSource,
        )
    }

    @Test
    fun reduceAuditUsesSafeMessageForBlockedTerminal() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-blocked",
                status = "blocked",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-terminal",
                        seq = 1,
                        eventType = "run_completed",
                        safeMessage = "当前请求无法完成，请调整后重试。",
                        createdAt = 500L,
                    ),
                ),
            ),
        )

        assertEquals(RunTerminalStatus.BLOCKED, reduced.terminal?.status)
        assertEquals("当前请求无法完成，请调整后重试。", reduced.terminal?.message)
    }

    @Test
    fun reduceAuditMergesHistoricalToolStartedAndCompletedByToolCallId() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-tool",
                status = "completed",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-tool-start",
                        seq = 1,
                        eventType = "tool_started",
                        toolCallId = "run-tool:lookup:0",
                        toolName = "lookup",
                        inputSummary = "查询摘要",
                        createdAt = 100L,
                    ),
                    AgentTraceEventDto(
                        eventId = "event-tool-complete",
                        seq = 2,
                        eventType = "tool_completed",
                        toolCallId = "run-tool:lookup:0",
                        toolName = "lookup",
                        resultSummary = "命中 1 条",
                        returnedCount = 1,
                        createdAt = 200L,
                    ),
                ),
            ),
        )

        assertEquals(1, reduced.toolCalls.size)
        assertEquals(1, reduced.timeline.filterIsInstance<RunTraceItem.Tool>().size)
        assertEquals("run-tool:lookup:0", reduced.toolCalls.single().toolCallId)
        assertEquals(ToolCallStatus.COMPLETED, reduced.toolCalls.single().status)
    }

    @Test
    fun reduceAuditRestoresHistoricalSafetyBlockCardAndTerminal() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-safety-blocked",
                status = "blocked",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-safety-blocked",
                        seq = 1,
                        eventType = "safety_check_blocked",
                        safeMessage = "该请求无法执行。",
                        createdAt = 100L,
                    ),
                ),
            ),
        )

        val safety = reduced.timeline.filterIsInstance<RunTraceItem.Safety>().single()
        assertEquals(SafetyTraceStatus.BLOCKED, safety.status)
        assertEquals("该请求无法执行。", safety.message)
        assertEquals(RunTerminalStatus.BLOCKED, reduced.terminal?.status)
    }

    @Test
    fun localStopCreatesCancelledTraceWithoutReplacingExistingProcess() {
        val trace = AgentRunTraceReducer.reduce(
            AgentRunTraceReducer.initial("run-local-stop"),
            AgentStreamEvent.PlanDelta(
                runId = "run-local-stop",
                content = "保留已收到的计划",
                timestamp = 100L,
            ),
        )

        val cancelled = AgentRunTraceReducer.cancelled(
            trace = trace,
            message = "已停止本机接收，正在请求服务端取消",
        )

        assertEquals(RunTerminalStatus.CANCELLED, cancelled.terminal?.status)
        assertEquals(AnswerTraceStatus.CANCELLED, cancelled.answerStatus)
        assertTrue(cancelled.timeline.any { it is RunTraceItem.PlanSummary })
        assertEquals("已停止本机接收，正在请求服务端取消", cancelled.terminal?.message)
    }
}
