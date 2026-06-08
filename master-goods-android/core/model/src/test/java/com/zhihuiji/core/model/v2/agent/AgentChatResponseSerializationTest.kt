package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentChatResponseSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun decodesNonStreamingAgentRunContract() {
        val response = json.decodeFromString(
            AgentChatResponse.serializer(),
            """
            {
              "run_id": "run-contract-1",
              "conversation_id": 101,
              "answer": "已基于真实客户应收数据完成查询。",
              "blocks": [
                {
                  "block_type": "kpi_grid",
                  "title": "应收概览",
                  "data": {
                    "kpis": [
                      {"label": "待收款", "value": "180.00", "unit": "元"}
                    ]
                  }
                }
              ],
              "draft_id": null,
              "safety_passed": true,
              "mode": "tool_query_rule_summary",
              "llm_status": "disabled",
              "plan_source": "keyword",
              "plan_summary": "customer_receivable_lookup(limit=10)",
              "tool_calls": [
                {
                  "tool_call_id": "run-contract-1:customer_receivable_lookup:0",
                  "tool_name": "customer_receivable_lookup",
                  "status": "completed",
                  "input_summary": "查询待收款客户",
                  "query_window": {"owner_user_id": 1, "limit": 10},
                  "returned_count": 2,
                  "total_count": 2,
                  "limit": 10,
                  "is_truncated": false,
                  "duration_ms": 12,
                  "result_summary": "命中 2 位待收款客户"
                }
              ],
              "evidence_refs": [
                {
                  "evidence_id": "ev-1",
                  "tool_call_id": "run-contract-1:customer_receivable_lookup:0",
                  "tool_name": "customer_receivable_lookup",
                  "label": "客户A",
                  "value": "100.00",
                  "query_window": {"limit": 10},
                  "is_truncated": false
                }
              ],
              "result_blocks": [
                {
                  "block_type": "evidence_card",
                  "title": "查询依据",
                  "data": {
                    "items": [
                      {"label": "客户A", "value": "100.00", "source": "customer_receivable_lookup"}
                    ]
                  }
                }
              ],
              "performance_summary": {
                "started_at": 1710000000000,
                "completed_at": 1710000000100,
                "duration_ms": 100,
                "tool_duration_ms": 12,
                "model_duration_ms": 0
              },
              "audit_id": "run-contract-1:audit",
              "trace_id": "run-contract-1:trace",
              "observability": {
                "request_id": "run-contract-1",
                "correlation_id": "run-contract-1",
                "trace_id": "run-contract-1:trace",
                "audit_id": "run-contract-1:audit",
                "log_ref": "agent-run:run-contract-1"
              },
              "unknown_future_field": "ignored"
            }
            """.trimIndent()
        )

        assertEquals("run-contract-1", response.runId)
        assertEquals(101L, response.conversationId)
        assertEquals("keyword", response.planSource)
        assertEquals("customer_receivable_lookup(limit=10)", response.planSummary)
        assertEquals(1, response.blocks.size)
        assertEquals("kpi_grid", response.blocks.first().blockType)
        assertEquals(1, response.resultBlocks.size)
        assertEquals("evidence_card", response.resultBlocks.first().blockType)

        val toolCall = response.toolCalls.single()
        assertEquals("run-contract-1:customer_receivable_lookup:0", toolCall.toolCallId)
        assertEquals("customer_receivable_lookup", toolCall.toolName)
        assertEquals("completed", toolCall.status)
        assertEquals(2, toolCall.returnedCount)
        assertEquals(2, toolCall.totalCount)
        assertEquals(10, toolCall.limit)
        assertEquals(false, toolCall.isTruncated)
        assertEquals(12L, toolCall.durationMs)
        assertNotNull(toolCall.queryWindow)

        val evidenceRef = response.evidenceRefs.single()
        assertEquals(toolCall.toolCallId, evidenceRef.toolCallId)
        assertEquals("客户A", evidenceRef.label)
        assertEquals("100.00", evidenceRef.value)
        assertEquals(false, evidenceRef.isTruncated)
        assertNotNull(evidenceRef.queryWindow)

        val performance = response.performanceSummary
        assertNotNull(performance)
        assertEquals(100L, performance?.durationMs)
        assertEquals(12L, performance?.toolDurationMs)
        assertEquals(0L, performance?.modelDurationMs)

        assertEquals("run-contract-1:audit", response.auditId)
        assertEquals("run-contract-1:trace", response.traceId)
        val observability = response.observability
        assertNotNull(observability)
        assertEquals("run-contract-1", observability?.requestId)
        assertEquals("run-contract-1", observability?.correlationId)
        assertEquals(response.auditId, observability?.auditId)
        assertEquals(response.traceId, observability?.traceId)
        assertEquals("agent-run:run-contract-1", observability?.logRef)
    }

    @Test
    fun decodesLegacyNonStreamingResponseWithoutAgentAuditFields() {
        val response = json.decodeFromString(
            AgentChatResponse.serializer(),
            """
            {
              "run_id": "run-legacy",
              "conversation_id": 102,
              "answer": "兼容旧响应",
              "blocks": [],
              "safety_passed": true
            }
            """.trimIndent()
        )

        assertEquals("run-legacy", response.runId)
        assertEquals("兼容旧响应", response.answer)
        assertTrue(response.toolCalls.isEmpty())
        assertTrue(response.evidenceRefs.isEmpty())
        assertTrue(response.resultBlocks.isEmpty())
        assertEquals(null, response.performanceSummary)
        assertEquals(null, response.auditId)
        assertEquals(null, response.traceId)
        assertEquals(null, response.observability)
    }
}
