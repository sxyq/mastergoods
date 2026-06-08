package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentStoredResultBlockParseTest {

    @Test
    fun parseStoredResultBlocksKeepsRawSummaryWhenHistoryPayloadIsBroken() {
        val rawJson = """{"block_type":"table","title":"坏数据","data":"""

        val blocks = parseStoredResultBlocks(rawJson)

        val block = blocks.single()
        assertEquals("parse_error", block.blockType)
        assertEquals("历史结构化结果解析失败", block.title)
        assertTrue(block.data.toString(), block.data.toString().contains("坏数据"))
        assertTrue(block.data.toString(), block.data.toString().contains("raw"))
    }

    @Test
    fun parseStoredResultBlocksTruncatesLargeBrokenHistoryPayload() {
        val rawJson = "[" + "x".repeat(1_200)

        val block = parseStoredResultBlocks(rawJson).single()
        val raw = (block.data as JsonObject)["raw"]!!.jsonPrimitive.content

        assertEquals(600, raw.length)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStoredResultBlocks(rawJson: String): List<ResultBlockDto> {
        val method = viewModelFileClass.getDeclaredMethod("parseStoredResultBlocks", String::class.java)
        method.isAccessible = true
        return method.invoke(null, rawJson) as List<ResultBlockDto>
    }

    private companion object {
        val viewModelFileClass: Class<*> = Class.forName(
            "com.zhihuiji.feature.agent.AgentChatViewModelKt"
        )
    }
}
