package com.zhihuiji.feature.agent

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun parseMarkdownSupportsTaskListItemsFromAgentChecklists() {
        val blocks = parseMarkdownBlocks("- [x] 已核对应收依据\n- [ ] 联系客户确认回款")
        val items = listItems(blocks.single())

        assertEquals(listOf("已核对应收依据", "联系客户确认回款"), items.map(::listItemText))
        assertEquals(listOf(true, false), items.map(::listItemChecked))
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
    fun inlineMarkdownKeepsLinkDestinationWithBalancedParentheses() {
        val rendered = inlineMarkdownText("来源：[销售报表](https://example.com/report/(daily)) 已核对。")

        assertEquals("来源：销售报表 (https://example.com/report/(daily)) 已核对。", rendered)
    }

    @Test
    fun inlineMarkdownDropsOptionalLinkTitleFromVisibleUrl() {
        val rendered = inlineMarkdownText("参考：[客户明细](https://example.com/customer \"后台明细\")")

        assertEquals("参考：客户明细 (https://example.com/customer)", rendered)
    }

    @Test
    fun inlineMarkdownSupportsAngleWrappedDestinationWithSpacesInTitle() {
        val rendered = inlineMarkdownText("查看：[审计日志](<https://example.com/audit?q=run%201> \"Run audit\")")

        assertEquals("查看：审计日志 (https://example.com/audit?q=run%201)", rendered)
    }

    @Test
    fun inlineMarkdownLeavesUnsupportedSchemeAsPlainText() {
        val rendered = inlineMarkdownAnnotated("查看[危险链接](javascript:alert(1))")

        assertEquals("查看危险链接 (javascript:alert(1))", rendered.text)
        assertTrue(rendered.getLinkAnnotations(0, rendered.length).isEmpty())
    }

    @Test
    fun inlineMarkdownAutolinksBareHttpsUrlFromModelAnswer() {
        val rendered = inlineMarkdownText("来源 https://example.com/report/123 已核对。")

        assertEquals("来源 https://example.com/report/123 已核对。", rendered)
    }

    @Test
    fun inlineMarkdownAutolinksBareWwwUrlAndKeepsTrailingPunctuationOutsideLink() {
        val rendered = inlineMarkdownText("帮助见 www.example.com/help。")

        assertEquals("帮助见 https://www.example.com/help。", rendered)
    }

    @Test
    fun inlineMarkdownBareUrlKeepsChineseRightParenOutsideVisibleUrl() {
        val rendered = inlineMarkdownText("（https://example.com/audit）")

        assertEquals("（https://example.com/audit）", rendered)
    }

    @Test
    fun inlineMarkdownKeepsMixedBoldCodeAndLinkTextReadable() {
        val rendered = inlineMarkdownText("客户 **张三** 的 `receivable` 为 [1280 元](https://example.com/r/1)。")

        assertEquals("客户 张三 的  receivable  为 1280 元 (https://example.com/r/1)。", rendered)
    }

    @Test
    fun inlineMarkdownSupportsUnderscoreEmphasisFromModelReplies() {
        val rendered = inlineMarkdownText("请优先处理 __应收风险__，并标记为 _高优先级_。")

        assertEquals("请优先处理 应收风险，并标记为 高优先级。", rendered)
    }

    @Test
    fun inlineMarkdownKeepsBusinessFieldUnderscoresReadable() {
        val rendered = inlineMarkdownText("依据字段 customer_count 与 top_10_receivable_total 生成。")

        assertEquals("依据字段 customer_count 与 top_10_receivable_total 生成。", rendered)
    }

    @Test
    fun inlineMarkdownKeepsUnclosedFormattingAsPlainTextDuringStreaming() {
        assertEquals("正在分析 **销售额", inlineMarkdownText("正在分析 **销售额"))
        assertEquals("正在分析 __销售额", inlineMarkdownText("正在分析 __销售额"))
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
        return inlineMarkdownAnnotated(text).text
    }

    private fun inlineMarkdownAnnotated(text: String): AnnotatedString {
        val method = markdownFileClass.declaredMethods.first { method ->
            method.name.startsWith("inlineMarkdown") &&
                method.parameterTypes.toList() == listOf(String::class.java, Long::class.javaPrimitiveType)
        }
        method.isAccessible = true
        return method.invoke(null, text, 0L) as AnnotatedString
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

    @Suppress("UNCHECKED_CAST")
    private fun listItems(block: Any): List<Any> {
        val itemsProperty = block.javaClass.getDeclaredMethod("getItems")
        itemsProperty.isAccessible = true
        return itemsProperty.invoke(block) as List<Any>
    }

    private fun listItemText(item: Any): String {
        val textProperty = item.javaClass.getDeclaredMethod("getText")
        textProperty.isAccessible = true
        return textProperty.invoke(item) as String
    }

    private fun listItemChecked(item: Any): Boolean? {
        val checkedProperty = item.javaClass.getDeclaredMethod("getChecked")
        checkedProperty.isAccessible = true
        return checkedProperty.invoke(item) as Boolean?
    }

    private companion object {
        val markdownFileClass: Class<*> = Class.forName(
            "com.zhihuiji.feature.agent.AgentMarkdownTextKt"
        )
    }
}
