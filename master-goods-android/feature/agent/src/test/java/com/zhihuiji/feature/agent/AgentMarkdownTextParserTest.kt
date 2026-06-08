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

    private companion object {
        val markdownFileClass: Class<*> = Class.forName(
            "com.zhihuiji.feature.agent.AgentMarkdownTextKt"
        )
    }
}
