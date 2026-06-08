package com.zhihuiji.feature.agent

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
