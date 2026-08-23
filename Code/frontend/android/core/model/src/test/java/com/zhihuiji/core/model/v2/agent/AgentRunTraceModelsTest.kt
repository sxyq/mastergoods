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

    @Test
    fun reduceDeduplicatesTerminalEventsPerRun() {
        val initial = AgentRunTraceReducer.initial("run-dedup")
        val first = AgentRunTraceReducer.reduce(
            initial,
            AgentStreamEvent.RunCompleted(
                runId = "run-dedup",
                terminalStatus = "COMPLETED",
                finalAnswer = "第一次完成",
                timestamp = 100L,
            ),
        )
        // 第二次终态事件应被忽略
        val second = AgentRunTraceReducer.reduce(
            first,
            AgentStreamEvent.RunFailed(
                runId = "run-dedup",
                terminalStatus = "FAILED",
                safeMessage = "后续失败",
                timestamp = 200L,
            ),
        )

        assertEquals(RunTerminalStatus.COMPLETED, second.terminal?.status)
        assertEquals("第一次完成", second.terminal?.message)
    }

    @Test
    fun reduceHandlesConfirmationPendingTerminalStatus() {
        val trace = AgentRunTraceReducer.reduce(
            AgentRunTraceReducer.initial("run-confirm"),
            AgentStreamEvent.RunCompleted(
                runId = "run-confirm",
                terminalStatus = "CONFIRMATION_PENDING",
                finalAnswer = "已生成草稿",
                safeMessage = "已生成草稿，请确认",
                timestamp = 100L,
            ),
        )

        assertEquals(RunTerminalStatus.CONFIRMATION_PENDING, trace.terminal?.status)
        assertEquals(AnswerTraceStatus.CONFIRMATION_PENDING, trace.answerStatus)
        assertEquals("已生成草稿，请确认", trace.terminal?.message)
    }

    @Test
    fun reduceHandlesExhaustedTerminalEvent() {
        val trace = AgentRunTraceReducer.reduce(
            AgentRunTraceReducer.initial("run-exhausted"),
            AgentStreamEvent.RunExhausted(
                runId = "run-exhausted",
                terminalStatus = "EXHAUSTED",
                safeMessage = "工具预算耗尽",
                timestamp = 100L,
            ),
        )

        assertEquals(RunTerminalStatus.EXHAUSTED, trace.terminal?.status)
        assertEquals(AnswerTraceStatus.FAILED, trace.answerStatus)
    }

    @Test
    fun reduceHandlesBlockedTerminalEvent() {
        val trace = AgentRunTraceReducer.reduce(
            AgentRunTraceReducer.initial("run-blocked-terminal"),
            AgentStreamEvent.RunBlocked(
                runId = "run-blocked-terminal",
                terminalStatus = "BLOCKED",
                safeMessage = "安全策略阻止",
                timestamp = 100L,
            ),
        )

        assertEquals(RunTerminalStatus.BLOCKED, trace.terminal?.status)
        assertEquals(AnswerTraceStatus.BLOCKED, trace.answerStatus)
    }

    @Test
    fun reduceHandlesFailedTerminalEvent() {
        val trace = AgentRunTraceReducer.reduce(
            AgentRunTraceReducer.initial("run-failed"),
            AgentStreamEvent.RunFailed(
                runId = "run-failed",
                terminalStatus = "FAILED",
                safeMessage = "系统错误",
                errorCode = "SYSTEM_ERROR",
                timestamp = 100L,
            ),
        )

        assertEquals(RunTerminalStatus.FAILED, trace.terminal?.status)
        assertEquals(AnswerTraceStatus.FAILED, trace.answerStatus)
    }

    @Test
    fun reduceHandlesRunCancelledWithTerminalStatus() {
        val trace = AgentRunTraceReducer.reduce(
            AgentRunTraceReducer.initial("run-cancelled-ts"),
            AgentStreamEvent.RunCancelled(
                runId = "run-cancelled-ts",
                terminalStatus = "CANCELLED",
                reason = "用户取消",
                timestamp = 100L,
            ),
        )

        assertEquals(RunTerminalStatus.CANCELLED, trace.terminal?.status)
        assertEquals("用户取消", trace.terminal?.message)
    }

    @Test
    fun reduceAuditRestoresExhaustedTerminalFromAuditStatus() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-audit-exhausted",
                status = "exhausted",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-terminal",
                        seq = 1,
                        eventType = "run_exhausted",
                        safeMessage = "预算耗尽",
                        createdAt = 500L,
                    ),
                ),
            ),
        )

        assertEquals(RunTerminalStatus.EXHAUSTED, reduced.terminal?.status)
        assertEquals("预算耗尽", reduced.terminal?.message)
    }

    @Test
    fun reduceAuditRestoresConfirmationPendingFromAuditStatus() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-audit-confirm",
                status = "confirmation_pending",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-terminal",
                        seq = 1,
                        eventType = "run_completed",
                        content = "草稿已生成",
                        createdAt = 500L,
                    ),
                ),
            ),
        )

        assertEquals(RunTerminalStatus.CONFIRMATION_PENDING, reduced.terminal?.status)
    }

    @Test
    fun reduceAuditRestoresDraftStatusFromEventDto() {
        val reduced = AgentRunTraceReducer.reduceAudit(
            AgentRunTraceDto(
                runId = "run-draft-status",
                status = "completed",
                events = listOf(
                    AgentTraceEventDto(
                        eventId = "event-draft",
                        seq = 1,
                        eventType = "draft_created",
                        draftId = 100L,
                        draftType = "sale_order",
                        title = "销售单草稿",
                        status = "active",
                        createdAt = 100L,
                    ),
                ),
            ),
        )

        val draft = reduced.draft
        assertEquals(100L, draft?.draftId)
        assertEquals("active", draft?.status)
    }
}
