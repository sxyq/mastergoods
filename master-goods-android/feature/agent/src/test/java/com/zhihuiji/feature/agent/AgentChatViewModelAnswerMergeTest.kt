package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentChatViewModelAnswerMergeTest {

    @Test
    fun authoritativeAnswerFillsEmptyMessage() {
        assertEquals("完整回答", "".withAuthoritativeAnswer("完整回答"))
    }

    @Test
    fun authoritativeAnswerCanAppendMissingTailToStreamedDelta() {
        assertEquals(
            "## 销售分析\n今天销售额为 1280 元。",
            "## 销售分析\n今天".withAuthoritativeAnswer("## 销售分析\n今天销售额为 1280 元。")
        )
    }

    @Test
    fun authoritativeAnswerDoesNotReplaceVisibleStreamWithDifferentText() {
        assertEquals(
            "正在基于真实销售记录分析：",
            "正在基于真实销售记录分析：".withAuthoritativeAnswer("本次查询结果如下：")
        )
    }

    @Test
    fun blankAuthoritativeAnswerKeepsVisibleStream() {
        assertEquals("已有流式内容", "已有流式内容".withAuthoritativeAnswer(" "))
    }

    @Test
    fun streamingTextDeltaAppendsToLastTextPart() {
        val parts = listOf(ChatMessagePart.Text("先分析"))

        val updated = parts.appendStreamingText("库存")

        assertEquals(listOf(ChatMessagePart.Text("先分析库存")), updated)
    }

    @Test
    fun streamingTextDeltaStaysAfterPreviousResultBlock() {
        val block = ResultBlockDto(blockType = "table", title = "销售记录")
        val parts = listOf(
            ChatMessagePart.Text("先看明细"),
            ChatMessagePart.ResultBlock(block),
        )

        val updated = parts.appendStreamingText("再给结论")

        assertEquals(
            listOf(
                ChatMessagePart.Text("先看明细"),
                ChatMessagePart.ResultBlock(block),
                ChatMessagePart.Text("再给结论"),
            ),
            updated
        )
    }

    @Test
    fun authoritativeTextUpdatesOnlyTextPartWithoutMovingResultBlocks() {
        val block = ResultBlockDto(blockType = "line_chart", title = "趋势")
        val parts = listOf(
            ChatMessagePart.Text("销售"),
            ChatMessagePart.ResultBlock(block),
        )

        val updated = parts.withAuthoritativeText("销售趋势")

        assertEquals(
            listOf(
                ChatMessagePart.Text("销售趋势"),
                ChatMessagePart.ResultBlock(block),
            ),
            updated
        )
    }

    @Test
    fun authoritativeTextAppendsAfterResultBlockWhenNoTextArrivedYet() {
        val block = ResultBlockDto(blockType = "table", title = "明细")
        val parts = listOf(ChatMessagePart.ResultBlock(block))

        val updated = parts.withAuthoritativeText("基于上方明细给出结论。")

        assertEquals(
            listOf(
                ChatMessagePart.ResultBlock(block),
                ChatMessagePart.Text("基于上方明细给出结论。"),
            ),
            updated
        )
    }

    @Test
    fun authoritativeTextAppendsConflictingFinalAnswerInsteadOfDroppingIt() {
        val parts = listOf(ChatMessagePart.Text("模型已输出的真实片段"))

        val updated = parts.withAuthoritativeText("最终答案与片段不完全一致")

        assertEquals(
            listOf(
                ChatMessagePart.Text("模型已输出的真实片段"),
                ChatMessagePart.Text("最终答案与片段不完全一致"),
            ),
            updated
        )
    }

    @Test
    fun authoritativeTextDoesNotMoveTimelineTextAroundResultBlock() {
        val block = ResultBlockDto(blockType = "line_chart", title = "销售趋势")
        val parts = listOf(
            ChatMessagePart.Text("先看销售趋势："),
            ChatMessagePart.ResultBlock(block),
            ChatMessagePart.Text("结论是本周持续增长。"),
        )

        val updated = parts.withAuthoritativeText("先看销售趋势：结论是本周持续增长。")

        assertEquals(parts, updated)
    }

    @Test
    fun authoritativeTextAppendsMissingTailToLastTimelineText() {
        val block = ResultBlockDto(blockType = "table", title = "销售明细")
        val parts = listOf(
            ChatMessagePart.Text("先看明细："),
            ChatMessagePart.ResultBlock(block),
            ChatMessagePart.Text("目前最高的是 A 商品"),
        )

        val updated = parts.withAuthoritativeText("先看明细：目前最高的是 A 商品，需要继续关注库存。")

        assertEquals(
            listOf(
                ChatMessagePart.Text("先看明细："),
                ChatMessagePart.ResultBlock(block),
                ChatMessagePart.Text("目前最高的是 A 商品，需要继续关注库存。"),
            ),
            updated
        )
    }
}
