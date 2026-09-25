// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.DraftTrace
import com.zhihuiji.core.model.v2.agent.MessageRole
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 同一状态、同一时间戳，但 UI 可见字段变化时，结构键/显示键必须更新。
 */
class AgentChatTimelineFieldKeyTest {

    private fun message() = ChatMessage(
        id = "m1",
        conversationId = 1L,
        role = MessageRole.ASSISTANT,
        content = "你好",
        createdAt = 1_000L,
    )

    private fun tool(
        queryWindow: JsonElement? = null,
        totalCount: Int? = null,
        limit: Int? = null,
        isTruncated: Boolean? = null,
        evidence: JsonElement? = null,
        inputSummary: String? = null,
        nextCursor: String? = null,
        progressMessage: String? = null,
        resultSummary: String? = null,
        returnedCount: Int? = null,
    ) = ToolCallRecord(
        toolName = "sales_trend",
        status = ToolCallStatus.COMPLETED,
        timestamp = 2_000L,
        queryWindow = queryWindow,
        totalCount = totalCount,
        limit = limit,
        isTruncated = isTruncated,
        evidence = evidence,
        inputSummary = inputSummary,
        nextCursor = nextCursor,
        progressMessage = progressMessage,
        resultSummary = resultSummary,
        returnedCount = returnedCount,
    )

    private fun keyOf(call: ToolCallRecord): TraceStructureKey =
        traceStructureKeyOf(message(), RunTrace(runId = "r1", toolCalls = listOf(call)))

    @Test
    fun toolQueryWindowChange_updatesStructureKey() {
        assertNotEquals(
            keyOf(tool()),
            keyOf(tool(queryWindow = buildJsonObject { put("from", "2026-01-01") })),
        )
    }

    @Test
    fun toolTotalCountAndLimitChange_updatesStructureKey() {
        val base = tool(totalCount = 10, limit = 50)
        assertNotEquals(keyOf(base), keyOf(base.copy(totalCount = 20)))
        assertNotEquals(keyOf(base), keyOf(base.copy(limit = 100)))
    }

    @Test
    fun toolIsTruncatedAndEvidenceChange_updatesStructureKey() {
        val base = tool(isTruncated = false)
        assertNotEquals(keyOf(base), keyOf(base.copy(isTruncated = true)))
        assertNotEquals(
            keyOf(tool()),
            keyOf(tool(evidence = buildJsonObject { put("note", "命中 2 条") })),
        )
    }

    @Test
    fun toolDisplayAuxFieldsChange_updatesStructureKey() {
        val base = tool()
        assertNotEquals(keyOf(base), keyOf(base.copy(inputSummary = "查销售")))
        assertNotEquals(keyOf(base), keyOf(base.copy(nextCursor = "c-9")))
        assertNotEquals(keyOf(base), keyOf(base.copy(returnedCount = 3)))
        assertNotEquals(keyOf(base), keyOf(base.copy(resultSummary = "完成")))
        assertNotEquals(keyOf(base), keyOf(base.copy(progressMessage = "查询中")))
    }

    @Test
    fun draftTitleChange_updatesStructureKeyWithSameTimestamp() {
        val base = DraftTrace(draftId = 1L, draftType = "sale", title = "旧标题", status = "pending", timestamp = 5L)
        val next = base.copy(title = "新标题")
        assertEquals(base.timestamp, next.timestamp)
        assertEquals(base.status, next.status)
        val k1 = traceStructureKeyOf(message(), RunTrace(runId = "r1", draft = base))
        val k2 = traceStructureKeyOf(message(), RunTrace(runId = "r1", draft = next))
        assertNotEquals(k1, k2)
    }

    @Test
    fun resultBlockPayloadChange_updatesDisplayKey() {
        val blockA = ResultBlockDto(blockType = "table", title = "销售明细")
        val blockB = ResultBlockDto(blockType = "table", title = "销售明细更新")
        val a = answerDisplayKeyOf(message(), null, listOf(ChatMessagePart.ResultBlock(blockA)))
        val b = answerDisplayKeyOf(message(), null, listOf(ChatMessagePart.ResultBlock(blockB)))
        assertNotEquals(a, b)
    }

    @Test
    fun identicalToolRecord_reusesEqualStructureKey() {
        val call = tool(totalCount = 2, limit = 10, isTruncated = true, returnedCount = 2)
        assertEquals(keyOf(call), keyOf(call.copy()))
    }
}
