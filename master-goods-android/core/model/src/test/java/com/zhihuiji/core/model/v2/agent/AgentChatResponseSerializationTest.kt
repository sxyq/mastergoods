package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentChatResponseSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun decodesCleanWorkbenchStatusContract() {
        val response = json.decodeFromString(
            AgentWorkbenchV2Dto.serializer(),
            """
            {
              "greeting": "你好，我是智慧记 AI 助手",
              "kpi_cards": [],
              "quick_questions": [],
              "recent_conversations": [],
              "pending_drafts": [],
              "risk_alerts": [],
              "today_summary": null,
              "status": "clean_entry_ready",
              "data_policy": "AI 首页不预取或展示报表型经营数据；发送问题后才创建真实 owner-scoped run。",
              "capabilities": [
                {
                  "id": "real_data_chat",
                  "title": "真实数据问答",
                  "description": "按用户问题创建服务端 run。"
                }
              ],
              "warnings": ["当前入口不返回默认 KPI、风险、今日摘要或报表图表。"]
            }
            """.trimIndent()
        )

        assertEquals("clean_entry_ready", response.status)
        assertTrue(response.kpiCards.isEmpty())
        assertTrue(response.riskAlerts.isEmpty())
        assertEquals(null, response.todaySummary)
        assertTrue(response.dataPolicy?.contains("不预取") == true)
        assertEquals("real_data_chat", response.capabilities.single().id)
        assertTrue(response.warnings.single().contains("不返回默认 KPI"))
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
                      {
                        "label": "欠款客户数 (customer_count)",
                        "value": "2个",
                        "source": "tool:customer_receivable_lookup",
                        "tool_call_id": "run-contract-1:customer_receivable_lookup:0",
                        "query_window": {"owner_scope": "current_owner", "limit": 10},
                        "is_truncated": false
                      }
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
        val evidenceBlock = Json.decodeFromJsonElement<EvidenceCardBlockData>(response.resultBlocks.first().data!!)
        val evidenceItem = evidenceBlock.items.single()
        assertEquals("欠款客户数 (customer_count)", evidenceItem.label)
        assertEquals("2个", evidenceItem.value)
        assertEquals("tool:customer_receivable_lookup", evidenceItem.source)
        assertEquals("run-contract-1:customer_receivable_lookup:0", evidenceItem.toolCallId)
        assertEquals(false, evidenceItem.isTruncated)
        assertNotNull(evidenceItem.queryWindow)

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
