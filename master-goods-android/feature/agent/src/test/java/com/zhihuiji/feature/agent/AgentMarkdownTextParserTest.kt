package com.zhihuiji.feature.agent

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentMarkdownTextParserTest {
    @Test
    fun parseTableRowKeepsEscapedPipeInsideCell() {
        val row = parseTableRow("| 商品 | 规格 A\\|B | 金额 |")

        assertEquals(listOf("商品", "规格 A|B", "金额"), row)
    }

    @Test
    fun parseTableRowKeepsPipeInsideInlineCode() {
        val row = parseTableRow("| 表达式 | `a|b` | 说明 |")

        assertEquals(listOf("表达式", "`a|b`", "说明"), row)
    }

    @Test
    fun parseMarkdownKeepsCodeBlockTrailingWhitespace() {
        val blocks = parseMarkdownBlocks("```sql\nselect 1;  \n\n```")
        val code = codeBlockText(blocks.single())

        assertEquals("select 1;  \n\n", code)
    }

    @Test
    fun parseMarkdownSupportsDeepHeadingLevels() {
        val blocks = parseMarkdownBlocks("#### 四级标题\n###### 六级标题")

        assertEquals(listOf(4, 6), blocks.map(::headingLevel))
        assertEquals(listOf("四级标题", "六级标题"), blocks.map(::headingText))
    }

    @Test
    fun parseMarkdownSeparatesCommonAiAnswerBlocks() {
        val markdown = """
            ## 销售建议
            - 检查库存
            - 联系客户
            1. 先确认应收
            2. 再安排回款
            > 仅基于当前账号真实数据
            ---
            最后给出结论。
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)

        assertEquals(
            listOf("Heading", "ListBlock", "ListBlock", "Quote", "Divider", "Paragraph"),
            blocks.map { it.javaClass.simpleName }
        )
    }

    @Test
    fun parseMarkdownKeepsPlainStreamingTextAsSingleParagraph() {
        val blocks = parseMarkdownBlocks("正在基于真实销售记录分析，本周销售额持续回升。")

        assertEquals(listOf("Paragraph"), blocks.map { it.javaClass.simpleName })
        assertEquals("正在基于真实销售记录分析，本周销售额持续回升。", paragraphText(blocks.single()))
    }

    @Test
    fun parseMarkdownFastPathStillAllowsInlineMarkdownInsideParagraph() {
        val blocks = parseMarkdownBlocks("客户 **张三** 的 `receivable` 为 [1280 元](https://example.com/r/1)。")

        assertEquals(listOf("Paragraph"), blocks.map { it.javaClass.simpleName })
        assertEquals("客户 **张三** 的 `receivable` 为 [1280 元](https://example.com/r/1)。", paragraphText(blocks.single()))
        assertEquals(
            "客户 张三 的  receivable  为 1280 元 (https://example.com/r/1)。",
            inlineMarkdownText(paragraphText(blocks.single()))
        )
    }

    @Test
    fun inlineMarkdownKeepsPlainStreamingTextWithoutInlineScanChanges() {
        val rendered = inlineMarkdownText("正在基于真实销售记录分析，本周销售额持续回升。")

        assertEquals("正在基于真实销售记录分析，本周销售额持续回升。", rendered)
    }

    @Test
    fun inlineMarkdownShowsLinkLabelAndVisibleUrl() {
        val rendered = inlineMarkdownText("查看[官方文档](https://example.com/docs)后继续。")

        assertEquals("查看官方文档 (https://example.com/docs)后继续。", rendered)
    }

    @Test
    fun inlineMarkdownNormalizesWwwLinkAndKeepsVisibleUrl() {
        val rendered = inlineMarkdownText("入口：[帮助中心](www.example.com/help)")

        assertEquals("入口：帮助中心 (https://www.example.com/help)", rendered)
    }

    @Test
    fun inlineMarkdownKeepsMixedBoldCodeAndLinkTextReadable() {
        val rendered = inlineMarkdownText("客户 **张三** 的 `receivable` 为 [1280 元](https://example.com/r/1)。")

        assertEquals("客户 张三 的  receivable  为 1280 元 (https://example.com/r/1)。", rendered)
    }

    @Test
    fun inlineMarkdownKeepsUnclosedFormattingAsPlainTextDuringStreaming() {
        assertEquals("正在分析 **销售额", inlineMarkdownText("正在分析 **销售额"))
        assertEquals("字段 `receivable", inlineMarkdownText("字段 `receivable"))
    }

    @Test
    fun inlineMarkdownKeepsBrokenLinkSyntaxAsPlainText() {
        val rendered = inlineMarkdownText("不要丢失[缺少右括号](https://example.com")

        assertEquals("不要丢失[缺少右括号](https://example.com", rendered)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTableRow(line: String): List<String> {
        val method = markdownFileClass.getDeclaredMethod("parseTableRow", String::class.java)
        method.isAccessible = true
        return method.invoke(null, line) as List<String>
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMarkdownBlocks(markdown: String): List<Any> {
        val method = markdownFileClass.getDeclaredMethod("parseMarkdown", String::class.java)
        method.isAccessible = true
        return method.invoke(null, markdown) as List<Any>
    }

    private fun inlineMarkdownText(text: String): String {
        val method = markdownFileClass.declaredMethods.first { method ->
            method.name.startsWith("inlineMarkdown") &&
                method.parameterTypes.toList() == listOf(String::class.java, Long::class.javaPrimitiveType)
        }
        method.isAccessible = true
        return (method.invoke(null, text, 0L) as AnnotatedString).text
    }

    private fun codeBlockText(block: Any): String {
        val codeProperty = block.javaClass.getDeclaredMethod("getCode")
        codeProperty.isAccessible = true
        return codeProperty.invoke(block) as String
    }

    private fun headingLevel(block: Any): Int {
        val levelProperty = block.javaClass.getDeclaredMethod("getLevel")
        levelProperty.isAccessible = true
        return levelProperty.invoke(block) as Int
    }

    private fun headingText(block: Any): String {
        val textProperty = block.javaClass.getDeclaredMethod("getText")
        textProperty.isAccessible = true
        return textProperty.invoke(block) as String
    }

    private fun paragraphText(block: Any): String {
        val textProperty = block.javaClass.getDeclaredMethod("getText")
        textProperty.isAccessible = true
        return textProperty.invoke(block) as String
    }

    private companion object {
        val markdownFileClass: Class<*> = Class.forName(
            "com.zhihuiji.feature.agent.AgentMarkdownTextKt"
        )
    }
}
