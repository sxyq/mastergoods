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
    fun resultBlockWaitsForFirstStreamingTextBeforeEnteringTimeline() {
        val block = ResultBlockDto(blockType = "line_chart", title = "销售趋势")

        val beforeAnswer = emptyList<ChatMessagePart>().appendResultBlockAfterVisibleText(block)
        val afterFirstDelta = beforeAnswer.appendStreamingText(
            delta = "我查到了销售趋势。",
            pendingBlocks = listOf(block),
        )

        assertEquals(emptyList<ChatMessagePart>(), beforeAnswer)
        assertEquals(
            listOf(
                ChatMessagePart.Text("我查到了销售趋势。"),
                ChatMessagePart.ResultBlock(block),
            ),
            afterFirstDelta
        )
    }

    @Test
    fun duplicateResultBlockBeforeFirstAnswerStaysPendingOnce() {
        val block = ResultBlockDto(blockType = "line_chart", title = "销售趋势")
        val blocks = emptyList<ResultBlockDto>()
            .appendDistinctResultBlock(block)
            .appendDistinctResultBlock(block)

        val beforeAnswer = emptyList<ChatMessagePart>()
            .appendResultBlockAfterVisibleText(block)
            .appendResultBlockAfterVisibleText(block)
        val afterFirstDelta = beforeAnswer.appendStreamingText(
            delta = "我查到了销售趋势。",
            pendingBlocks = blocks,
        )

        assertEquals(listOf(block), blocks)
        assertEquals(emptyList<ChatMessagePart>(), beforeAnswer)
        assertEquals(
            listOf(
                ChatMessagePart.Text("我查到了销售趋势。"),
                ChatMessagePart.ResultBlock(block),
            ),
            afterFirstDelta
        )
    }

    @Test
    fun duplicateResultBlockAfterVisibleAnswerDoesNotRenderTwice() {
        val block = ResultBlockDto(blockType = "table", title = "销售明细")

        val parts = listOf(ChatMessagePart.Text("我查到了明细。"))
            .appendResultBlockAfterVisibleText(block)
            .appendResultBlockAfterVisibleText(block)

        assertEquals(
            listOf(
                ChatMessagePart.Text("我查到了明细。"),
                ChatMessagePart.ResultBlock(block),
            ),
            parts
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
    fun authoritativeTextPrependsBeforeResultBlockWhenNoTextArrivedYet() {
        val block = ResultBlockDto(blockType = "table", title = "明细")
        val parts = listOf(ChatMessagePart.ResultBlock(block))

        val updated = parts.withAuthoritativeText("基于上方明细给出结论。")

        assertEquals(
            listOf(
                ChatMessagePart.Text("基于上方明细给出结论。"),
                ChatMessagePart.ResultBlock(block),
            ),
            updated
        )
    }

    @Test
    fun authoritativeTextKeepsVisibleStreamWhenFinalAnswerConflicts() {
        val parts = listOf(ChatMessagePart.Text("模型已输出的真实片段"))

        val updated = parts.withAuthoritativeText("最终答案与片段不完全一致")

        assertEquals(
            listOf(ChatMessagePart.Text("模型已输出的真实片段")),
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

    @Test
    fun authoritativeTextDoesNotAppendDuplicateFinalAnswerAfterInterleavedResultBlock() {
        val block = ResultBlockDto(blockType = "line_chart", title = "销售趋势")
        val parts = listOf(
            ChatMessagePart.Text("我先查询了近 7 天销售趋势。"),
            ChatMessagePart.ResultBlock(block),
            ChatMessagePart.Text("结论：销售额正在回升。"),
        )

        val updated = parts.withAuthoritativeText("最终汇总：销售额正在回升，请继续关注库存。")

        assertEquals(parts, updated)
    }

    @Test
    fun resultBlockThenCompletedAnswerKeepsAnswerBeforeDataBlock() {
        val block = ResultBlockDto(blockType = "line_chart", title = "销售趋势")
        val afterResultBlock = emptyList<ChatMessagePart>().appendResultBlockAfterVisibleText(block)

        val afterAnswerCompleted = afterResultBlock.withAuthoritativeText(
            content = "我查到了近 7 天销售趋势，整体正在回升。",
            pendingBlocks = listOf(block),
        )
        val afterRunCompleted = afterAnswerCompleted.withAuthoritativeText(
            content = "我查到了近 7 天销售趋势，整体正在回升。",
            pendingBlocks = listOf(block),
        )

        assertEquals(
            listOf(
                ChatMessagePart.Text("我查到了近 7 天销售趋势，整体正在回升。"),
                ChatMessagePart.ResultBlock(block),
            ),
            afterRunCompleted
        )
    }
}
