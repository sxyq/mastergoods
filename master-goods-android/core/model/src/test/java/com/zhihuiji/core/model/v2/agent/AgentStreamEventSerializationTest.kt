package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentStreamEventSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "event_type"
    }

    @Test
    fun decodesBackendRunStartedEventWithEventTypeDiscriminator() {
        val event = json.decodeFromString(
            AgentStreamEvent.serializer(),
            """
            {
              "event_type": "run_started",
              "run_id": "run-1",
              "conversation_id": 42,
              "audit_id": "run-1:audit",
              "trace_id": "run-1:trace",
              "observability": {
                "request_id": "run-1",
                "correlation_id": "run-1",
                "trace_id": "run-1:trace",
                "audit_id": "run-1:audit",
                "log_ref": "agent-run:run-1"
              },
              "timestamp": 1000
            }
            """.trimIndent()
        )

        assertTrue(event is AgentStreamEvent.RunStarted)
        val started = event as AgentStreamEvent.RunStarted
        assertEquals("run-1", started.runId)
        assertEquals(42L, started.conversationId)
        assertEquals("run-1:audit", started.auditId)
        assertEquals("run-1:trace", started.traceId)
        assertEquals("agent-run:run-1", started.observability?.logRef)
    }

    @Test
    fun decodesBackendToolCompletedEventWithSnakeCaseFields() {
        val event = json.decodeFromString(
            AgentStreamEvent.serializer(),
            """
            {
              "event_type": "tool_completed",
              "event_id": "run-1:4",
              "seq": 4,
              "run_id": "run-1",
              "conversation_id": 42,
              "tool_call_id": "run-1:inventory_low_stock_lookup",
              "tool_name": "inventory_low_stock_lookup",
              "result_summary": "命中 3 个低库存商品",
              "input_summary": "查询当前账号低库存商品，参数 {limit=3}",
              "query_window": {"owner_scope": "current_owner", "limit": 3},
              "started_at": 900,
              "completed_at": 1001,
              "duration_ms": 18,
              "returned_count": 3,
              "total_count": 9,
              "limit": 3,
              "is_truncated": true,
              "evidence": {"source": "tool:inventory_low_stock_lookup", "scope": "current_owner", "returned_count": 3, "is_truncated": true},
              "next_cursor": "offset:3:limit:3",
              "audit_id": "run-1:audit",
              "trace_id": "run-1:trace",
              "timestamp": 1001
            }
            """.trimIndent()
        )

        assertTrue(event is AgentStreamEvent.ToolCompleted)
        val completed = event as AgentStreamEvent.ToolCompleted
        assertEquals("run-1:4", completed.eventId)
        assertEquals(4, completed.seq)
        assertEquals(42L, completed.conversationId)
        assertEquals("run-1:inventory_low_stock_lookup", completed.toolCallId)
        assertEquals("inventory_low_stock_lookup", completed.toolName)
        assertEquals("命中 3 个低库存商品", completed.resultSummary)
        assertEquals("查询当前账号低库存商品，参数 {limit=3}", completed.inputSummary)
        assertEquals(900L, completed.startedAt)
        assertEquals(1001L, completed.completedAt)
        assertEquals(18L, completed.durationMs)
        assertEquals(3, completed.returnedCount)
        assertEquals(9, completed.totalCount)
        assertEquals(3, completed.limit)
        assertEquals(true, completed.isTruncated)
        assertEquals("offset:3:limit:3", completed.nextCursor)
        assertEquals("run-1:audit", completed.auditId)
        assertEquals("run-1:trace", completed.traceId)
    }

    @Test
    fun decodesAnswerDeltaSourceAndObservability() {
        val event = json.decodeFromString(
            AgentStreamEvent.serializer(),
            """
            {
              "event_type": "answer_delta",
              "event_id": "run-1:6",
              "seq": 6,
              "run_id": "run-1",
              "conversation_id": 42,
              "delta": "正在基于真实查询结果生成回答",
              "delta_source": "model_stream",
              "audit_id": "run-1:audit",
              "trace_id": "run-1:trace",
              "observability": {
                "request_id": "run-1",
                "correlation_id": "run-1",
                "trace_id": "run-1:trace",
                "audit_id": "run-1:audit",
                "log_ref": "agent-run:run-1"
              },
              "timestamp": 1002
            }
            """.trimIndent()
        )

        assertTrue(event is AgentStreamEvent.AnswerDelta)
        val delta = event as AgentStreamEvent.AnswerDelta
        assertEquals("run-1:6", delta.eventId)
        assertEquals(6, delta.seq)
        assertEquals(42L, delta.conversationId)
        assertEquals("正在基于真实查询结果生成回答", delta.delta)
        assertEquals("model_stream", delta.deltaSource)
        assertEquals("run-1:audit", delta.auditId)
        assertEquals("run-1:trace", delta.traceId)
        assertEquals("agent-run:run-1", delta.observability?.logRef)
    }

    @Test
    fun decodesPlanDeltaSourceForFallbackPlanner() {
        val event = json.decodeFromString(
            AgentStreamEvent.serializer(),
            """
            {
              "event_type": "plan_delta",
              "run_id": "run-1",
              "plan_source": "keyword_fallback",
              "content": "根据问题关键词兜底选择只读查询工具：customer_receivable_lookup",
              "timestamp": 1001
            }
            """.trimIndent()
        )

        assertTrue(event is AgentStreamEvent.PlanDelta)
        val plan = event as AgentStreamEvent.PlanDelta
        assertEquals("run-1", plan.runId)
        assertEquals("keyword_fallback", plan.planSource)
        assertTrue(plan.content.contains("兜底"))
    }

    @Test
    fun decodesBackendAnswerCompletedEvent() {
        val event = json.decodeFromString(
            AgentStreamEvent.serializer(),
            """
            {
              "event_type": "answer_completed",
              "run_id": "run-1",
              "answer": "已基于真实业务数据完成查询。",
              "audit_id": "run-1:audit",
              "trace_id": "run-1:trace",
              "observability": {
                "request_id": "run-1",
                "correlation_id": "run-1",
                "trace_id": "run-1:trace",
                "audit_id": "run-1:audit",
                "log_ref": "agent-run:run-1"
              },
              "timestamp": 1002
            }
            """.trimIndent()
        )

        assertTrue(event is AgentStreamEvent.AnswerCompleted)
        val completed = event as AgentStreamEvent.AnswerCompleted
        assertEquals("run-1", completed.runId)
        assertEquals("已基于真实业务数据完成查询。", completed.answer)
        assertEquals("run-1:audit", completed.auditId)
        assertEquals("run-1:trace", completed.traceId)
        assertEquals("agent-run:run-1", completed.observability?.logRef)
    }

    @Test
    fun decodesBackendRunCompletedAuditFields() {
        val event = json.decodeFromString(
            AgentStreamEvent.serializer(),
            """
            {
              "event_type": "run_completed",
              "run_id": "run-1",
              "final_answer": "已完成",
              "mode": "tool_query_rule_summary",
              "llm_status": "disabled",
              "plan_source": "keyword_fallback",
              "audit_id": "run-1:audit",
              "trace_id": "run-1:trace",
              "observability": {
                "request_id": "run-1",
                "correlation_id": "run-1",
                "trace_id": "run-1:trace",
                "audit_id": "run-1:audit",
                "log_ref": "agent-run:run-1"
              },
              "timestamp": 1003
            }
            """.trimIndent()
        )

        assertTrue(event is AgentStreamEvent.RunCompleted)
        val completed = event as AgentStreamEvent.RunCompleted
        assertEquals("run-1", completed.runId)
        assertEquals("keyword_fallback", completed.planSource)
        assertEquals("run-1:audit", completed.auditId)
        assertEquals("run-1:trace", completed.traceId)
        assertEquals("agent-run:run-1", completed.observability?.logRef)
    }

    @Test
    fun decodesBackendResultBlockEvent() {
        val event = json.decodeFromString(
            AgentStreamEvent.serializer(),
            """
            {
              "event_type": "result_block",
              "run_id": "run-1",
              "block": {
                "block_type": "table",
                "title": "低库存商品列表",
                "data": {
                  "headers": ["商品", "库存"],
                  "rows": [["A", "2"]],
                  "row_count": 1
                }
              },
              "timestamp": 1002
            }
            """.trimIndent()
        )

        assertTrue(event is AgentStreamEvent.ResultBlockEvent)
        val blockEvent = event as AgentStreamEvent.ResultBlockEvent
        assertEquals("table", blockEvent.block.blockType)
        assertEquals("低库存商品列表", blockEvent.block.title)
    }
}
