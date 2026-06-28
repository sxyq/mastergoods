package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension

@Composable
fun AgentMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    contentColor: Color = TextPrimary,
    renderIdentity: Any? = markdown,
) {
    val blocks = remember(renderIdentity, markdown) { parseMarkdown(markdown) }
    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEachIndexed { index, block ->
                key(block.stableKey(index)) {
                    when (block) {
                        is MarkdownBlock.Heading -> MarkdownHeading(block, contentColor)
                        is MarkdownBlock.Paragraph -> MarkdownParagraph(block.text, contentColor)
                        is MarkdownBlock.ListBlock -> MarkdownList(block, contentColor)
                        is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(block)
                        is MarkdownBlock.Quote -> MarkdownQuote(block, contentColor)
                        is MarkdownBlock.Divider -> MarkdownDivider()
                        is MarkdownBlock.Table -> MarkdownTable(block)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownHeading(block: MarkdownBlock.Heading, contentColor: Color) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    val inlineText = rememberInlineMarkdown(block.text, contentColor)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (block.level == 1) 24.dp else 18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(ZhihuijiPrimary.copy(alpha = 0.72f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        MarkdownInlineText(
            text = inlineText,
            style = style,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MarkdownParagraph(text: String, contentColor: Color) {
    val inlineText = rememberInlineMarkdown(text, contentColor)
    MarkdownInlineText(
        text = inlineText,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = contentColor
    )
}

@Composable
private fun MarkdownList(block: MarkdownBlock.ListBlock, contentColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        block.items.forEachIndexed { index, item ->
            val inlineText = rememberInlineMarkdown(item.text, contentColor)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when {
                        item.checked == true -> "☑"
                        item.checked == false -> "☐"
                        block.ordered -> "${index + 1}."
                        else -> "•"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.checked == true) TextSecondary else ZhihuijiPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(22.dp)
                )
                MarkdownInlineText(
                    text = inlineText,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownCodeBlock(block: MarkdownBlock.CodeBlock) {
    val clipboardManager = LocalClipboardManager.current
    var copyLabel by remember(block.code) { mutableStateOf("复制") }
    val highlightedCode = remember(block.language, block.code) {
        syntaxHighlightCode(block.code, block.language)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111827).copy(alpha = 0.92f))
            .border(0.7.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = block.language.ifBlank { "代码" },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = copyLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.76f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(block.code))
                        copyLabel = "已复制"
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(
            text = highlightedCode,
            style = MaterialTheme.typography.bodySmall.merge(
                TextStyle(fontFamily = FontFamily.Monospace)
            ),
            color = Color(0xFFE5E7EB),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun MarkdownQuote(block: MarkdownBlock.Quote, contentColor: Color) {
    val quoteColor = contentColor.copy(alpha = 0.86f)
    val inlineText = rememberInlineMarkdown(block.text, quoteColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(ZhihuijiPrimary.copy(alpha = 0.08f))
            .border(0.6.dp, ZhihuijiPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(ZhihuijiPrimary.copy(alpha = 0.55f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        MarkdownInlineText(
            text = inlineText,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            color = quoteColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MarkdownDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(TextTertiary.copy(alpha = 0.16f))
    )
}

@Composable
private fun MarkdownTable(block: MarkdownBlock.Table) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceLow.copy(alpha = 0.82f))
            .border(0.6.dp, ZhihuijiPrimary.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        MarkdownTableRow(
            cells = block.headers,
            isHeader = true
        )
        block.rows.forEach { row ->
            MarkdownTableRow(
                cells = row,
                isHeader = false
            )
        }
    }
}

@Composable
private fun MarkdownTableRow(cells: List<String>, isHeader: Boolean) {
    Row {
        cells.forEach { cell ->
            val cellColor = if (isHeader) TextPrimary else TextSecondary
            val inlineText = rememberInlineMarkdown(cell, cellColor)
            MarkdownInlineText(
                text = inlineText,
                style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                color = cellColor,
                fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .width(124.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHeader) ZhihuijiPrimary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.34f))
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun MarkdownInlineText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    val resolvedStyle = style.copy(
        color = color,
        fontWeight = fontWeight ?: style.fontWeight,
    )
    Text(
        text = text,
        style = resolvedStyle,
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun rememberInlineMarkdown(
    text: String,
    contentColor: Color,
): AnnotatedString = remember(text, contentColor) {
    inlineMarkdown(text, contentColor)
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListBlock(val ordered: Boolean, val items: List<MarkdownListItem>) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data object Divider : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

private data class MarkdownListItem(
    val text: String,
    val checked: Boolean? = null,
)

private fun MarkdownBlock.stableKey(index: Int): String =
    when (this) {
        is MarkdownBlock.Heading -> "heading-$index-$level-${text.hashCode()}"
        is MarkdownBlock.Paragraph -> "paragraph-$index-${text.hashCode()}"
        is MarkdownBlock.ListBlock -> "list-$index-${ordered}-${items.hashCode()}"
        is MarkdownBlock.CodeBlock -> "code-$index-${language.hashCode()}-${code.hashCode()}"
        is MarkdownBlock.Quote -> "quote-$index-${text.hashCode()}"
        MarkdownBlock.Divider -> "divider-$index"
        is MarkdownBlock.Table -> "table-$index-${headers.hashCode()}-${rows.hashCode()}"
    }

// commonmark 核心不解析 GFM 任务列表项，保留此正则用于 ListItem 文本的复选框识别
private val TaskListRegex = Regex("^\\[([ xX])]\\s+(.+)$")
private val MarkdownLinkStyles = TextLinkStyles(
    style = SpanStyle(
        color = ZhihuijiPrimary,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
    )
)
private val CodeKeywordColor = Color(0xFF93C5FD)
private val CodeStringColor = Color(0xFFF9A8D4)
private val CodeCommentColor = Color(0xFF86EFAC)
private val CodeNumberColor = Color(0xFFFCD34D)
private val KotlinLikeKeywords = setOf(
    "abstract", "as", "break", "class", "continue", "data", "do", "else", "false", "for", "fun",
    "if", "in", "interface", "internal", "is", "null", "object", "open", "override", "package",
    "private", "protected", "public", "return", "sealed", "super", "suspend", "this", "throw",
    "true", "try", "typealias", "val", "var", "when", "while"
)
private val JavaScriptKeywords = setOf(
    "async", "await", "break", "case", "catch", "class", "const", "continue", "default", "else",
    "export", "extends", "false", "finally", "for", "from", "function", "if", "import", "let",
    "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof",
    "undefined", "var", "while", "yield"
)
private val JsonKeywords = setOf("true", "false", "null")
private val SqlKeywords = setOf(
    "select", "from", "where", "and", "or", "join", "left", "right", "inner", "outer", "on",
    "group", "by", "order", "limit", "offset", "insert", "into", "values", "update", "set",
    "delete", "create", "table", "alter", "drop", "as", "case", "when", "then", "else", "end"
)
private val ShellKeywords = setOf(
    "if", "then", "else", "fi", "for", "in", "do", "done", "case", "esac", "while", "function",
    "export", "local", "return", "echo", "exit"
)

/**
 * commonmark 解析器单例，启用 GFM 表格扩展。
 *
 * 块级结构由 commonmark AST 解析（正确处理嵌套列表、缩进代码块、围栏代码块、引用等边界情况）；
 * 行内文本通过 [Node.inlineMarkdownSource] 重构为 Markdown 源串，再交由 [inlineMarkdown] 渲染，
 * 以保留已有的行内样式（粗体/斜体/代码/链接）与语法高亮逻辑。
 */
private val commonmarkParser: Parser by lazy {
    Parser.builder()
        .extensions(listOf(TablesExtension.create()))
        .build()
}

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return listOf(MarkdownBlock.Paragraph(""))
    val document = commonmarkParser.parse(markdown)
    val blocks = mutableListOf<MarkdownBlock>()
    var child: Node? = document.firstChild
    while (child != null) {
        mapNodeToBlock(child)?.let { blocks += it }
        child = child.next
    }
    return blocks.ifEmpty { listOf(MarkdownBlock.Paragraph(markdown.trim())) }
}

private fun mapNodeToBlock(node: Node): MarkdownBlock? = when (node) {
    is Heading -> MarkdownBlock.Heading(node.level, node.inlineMarkdownSource())
    is Paragraph -> MarkdownBlock.Paragraph(node.inlineMarkdownSource())
    is BulletList -> MarkdownBlock.ListBlock(ordered = false, items = node.listItems())
    is OrderedList -> MarkdownBlock.ListBlock(ordered = true, items = node.listItems())
    is FencedCodeBlock -> MarkdownBlock.CodeBlock(
        language = node.info.trim(),
        code = node.literal.trimEnd('\n'),
    )
    is IndentedCodeBlock -> MarkdownBlock.CodeBlock(
        language = "",
        code = node.literal.trimEnd('\n'),
    )
    is BlockQuote -> MarkdownBlock.Quote(node.quoteText())
    is ThematicBreak -> MarkdownBlock.Divider
    is TableBlock -> node.toMarkdownTable()
    else -> null
}

/**
 * 将节点的行内子节点重构为 Markdown 源串，供 [inlineMarkdown] 解析。
 *
 * commonmark 把行内格式拆成 AST 节点（StrongEmphasis/Emphasis/Code/Link 等），
 * 这里按 CommonMark 规范重新拼回 marker，使下游正则渲染器可继续工作。
 */
private fun Node.inlineMarkdownSource(): String {
    val sb = StringBuilder()
    var child: Node? = firstChild
    while (child != null) {
        sb.append(child.toMarkdownSource())
        child = child.next
    }
    return sb.toString()
}

private fun Node.toMarkdownSource(): String = when (this) {
    is Text -> literal
    is StrongEmphasis -> "**" + inlineMarkdownSource() + "**"
    is Emphasis -> "*" + inlineMarkdownSource() + "*"
    is Code -> "`" + literal + "`"
    is Link -> {
        val label = inlineMarkdownSource()
        val dest = destination
        if (dest.isNullOrBlank()) label else "[$label]($dest)"
    }
    is SoftLineBreak -> "\n"
    is HardLineBreak -> "\n"
    else -> inlineMarkdownSource()
}

private fun Node.listItems(): List<MarkdownListItem> {
    val items = mutableListOf<MarkdownListItem>()
    var child: Node? = firstChild
    while (child != null) {
        if (child is ListItem) {
            val text = child.inlineMarkdownSource().trim()
            // commonmark 核心不解析 GFM 任务列表，[ ]/[x] 保留为字面文本，这里补识别
            val task = TaskListRegex.find(text)
            if (task != null) {
                items += MarkdownListItem(
                    text = task.groupValues[2].trim(),
                    checked = task.groupValues[1].equals("x", ignoreCase = true),
                )
            } else {
                items += MarkdownListItem(text)
            }
        }
        child = child.next
    }
    return items
}

private fun Node.quoteText(): String {
    val parts = mutableListOf<String>()
    var child: Node? = firstChild
    while (child != null) {
        if (child is Paragraph) {
            parts += child.inlineMarkdownSource()
        } else if (child is BlockQuote) {
            parts += child.quoteText()
        }
        child = child.next
    }
    return parts.joinToString("\n")
}

private fun TableBlock.toMarkdownTable(): MarkdownBlock.Table {
    val headers = mutableListOf<String>()
    val rows = mutableListOf<List<String>>()
    var child: Node? = firstChild
    while (child != null) {
        when (child) {
            is TableHead -> {
                var row: Node? = child.firstChild
                while (row != null) {
                    if (row is TableRow) headers.addAll(row.cells())
                    row = row.next
                }
            }
            is TableBody -> {
                var row: Node? = child.firstChild
                while (row != null) {
                    if (row is TableRow) rows += row.cells()
                    row = row.next
                }
            }
        }
        child = child.next
    }
    return MarkdownBlock.Table(headers, rows)
}

private fun TableRow.cells(): List<String> {
    val cells = mutableListOf<String>()
    var child: Node? = firstChild
    while (child != null) {
        if (child is TableCell) {
            cells += child.inlineMarkdownSource().trim()
        }
        child = child.next
    }
    return cells
}

internal fun syntaxHighlightCode(code: String, language: String): AnnotatedString = buildAnnotatedString {
    if (code.isEmpty()) {
        return@buildAnnotatedString
    }
    val profile = codeLanguageProfile(language)
    var index = 0
    while (index < code.length) {
        val current = code[index]
        val next = code.getOrNull(index + 1)
        val lineCommentPrefix = profile.lineCommentPrefixes.firstOrNull { prefix ->
            code.startsWith(prefix, index)
        }
        when {
            profile.supportsBlockComments && current == '/' && next == '*' -> {
                val end = code.indexOf("*/", startIndex = index + 2)
                val until = if (end >= 0) end + 2 else code.length
                appendStyled(code.substring(index, until), CodeCommentColor)
                index = until
            }

            lineCommentPrefix != null -> {
                val end = code.indexOf('\n', startIndex = index).let { if (it >= 0) it else code.length }
                appendStyled(code.substring(index, end), CodeCommentColor)
                index = end
            }

            current == '"' || current == '\'' || current == '`' -> {
                val end = findStringLiteralEnd(code, index, current)
                appendStyled(code.substring(index, end), CodeStringColor)
                index = end
            }

            current.isDigit() -> {
                val end = findNumberLiteralEnd(code, index)
                appendStyled(code.substring(index, end), CodeNumberColor)
                index = end
            }

            current.isIdentifierStart() -> {
                val end = findIdentifierEnd(code, index)
                val token = code.substring(index, end)
                if (profile.keywords.contains(token.lowercase())) {
                    appendStyled(token, CodeKeywordColor)
                } else {
                    append(token)
                }
                index = end
            }

            else -> {
                append(current)
                index++
            }
        }
    }
}

private fun AnnotatedString.Builder.appendStyled(text: String, color: Color) {
    withStyle(SpanStyle(color = color)) {
        append(text)
    }
}

private fun findStringLiteralEnd(code: String, startIndex: Int, quote: Char): Int {
    var index = startIndex + 1
    while (index < code.length) {
        val current = code[index]
        if (current == '\\') {
            index += 2
            continue
        }
        if (current == quote) {
            return index + 1
        }
        index++
    }
    return code.length
}

private fun findNumberLiteralEnd(code: String, startIndex: Int): Int {
    var index = startIndex + 1
    while (index < code.length) {
        val current = code[index]
        if (current.isDigit() || current == '.' || current == '_' || current == 'x' || current == 'X') {
            index++
            continue
        }
        break
    }
    return index
}

private fun findIdentifierEnd(code: String, startIndex: Int): Int {
    var index = startIndex + 1
    while (index < code.length && code[index].isIdentifierPart()) {
        index++
    }
    return index
}

private fun Char.isIdentifierStart(): Boolean = isLetter() || this == '_' || this == '$'

private fun Char.isIdentifierPart(): Boolean = isLetterOrDigit() || this == '_' || this == '$'

private data class CodeLanguageProfile(
    val keywords: Set<String>,
    val lineCommentPrefixes: List<String>,
    val supportsBlockComments: Boolean,
)

private fun codeLanguageProfile(language: String): CodeLanguageProfile {
    val normalized = language.trim().lowercase()
    return when {
        normalized in setOf("kotlin", "kt", "java", "kts") -> CodeLanguageProfile(
            keywords = KotlinLikeKeywords,
            lineCommentPrefixes = listOf("//"),
            supportsBlockComments = true,
        )
        normalized in setOf("javascript", "js", "typescript", "ts", "tsx", "jsx") -> CodeLanguageProfile(
            keywords = JavaScriptKeywords,
            lineCommentPrefixes = listOf("//"),
            supportsBlockComments = true,
        )
        normalized == "json" -> CodeLanguageProfile(
            keywords = JsonKeywords,
            lineCommentPrefixes = emptyList(),
            supportsBlockComments = false,
        )
        normalized in setOf("sql", "mysql", "sqlite", "postgresql", "postgres") -> CodeLanguageProfile(
            keywords = SqlKeywords,
            lineCommentPrefixes = listOf("--"),
            supportsBlockComments = true,
        )
        normalized in setOf("bash", "sh", "shell", "zsh") -> CodeLanguageProfile(
            keywords = ShellKeywords,
            lineCommentPrefixes = listOf("#"),
            supportsBlockComments = false,
        )
        else -> CodeLanguageProfile(
            keywords = KotlinLikeKeywords,
            lineCommentPrefixes = listOf("//", "#"),
            supportsBlockComments = true,
        )
    }
}

internal fun inlineMarkdown(text: String, contentColor: Color): AnnotatedString =
    if (!hasInlineMarkdownSyntax(text)) {
        AnnotatedString(text)
    } else {
        buildAnnotatedString {
            var index = 0
            while (index < text.length) {
                when {
                    text.startsWith("[", index) -> {
                        val labelEnd = text.indexOf("](", startIndex = index + 1)
                        val urlEnd = if (labelEnd > index) {
                            findMarkdownLinkDestinationEnd(text, startIndex = labelEnd + 2)
                        } else {
                            -1
                        }
                        if (labelEnd > index && urlEnd > labelEnd) {
                            val label = text.substring(index + 1, labelEnd).ifBlank {
                                text.substring(labelEnd + 2, urlEnd).trim()
                            }
                            appendMarkdownLink(
                                label = label,
                                rawDestination = text.substring(labelEnd + 2, urlEnd),
                            )
                            index = urlEnd + 1
                        } else {
                            append(text[index])
                            index++
                        }
                    }

                    startsBareMarkdownUrl(text, index) -> {
                        val match = bareMarkdownUrl(text, index)
                        if (match != null) {
                            appendVisibleUrlLink(match.url)
                            append(match.trailingPunctuation)
                            index = match.endIndex
                        } else {
                            append(text[index])
                            index++
                        }
                    }

                    text.startsWith("**", index) -> {
                        val end = text.indexOf("**", startIndex = index + 2)
                        if (end > index) {
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = contentColor))
                            append(text.substring(index + 2, end))
                            pop()
                            index = end + 2
                        } else {
                            append(text[index])
                            index++
                        }
                    }

                    text.startsWith("__", index) && canOpenUnderscoreEmphasis(text, index, markerLength = 2) -> {
                        val end = findClosingUnderscoreEmphasis(text, startIndex = index + 2, marker = "__")
                        if (end > index) {
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = contentColor))
                            append(text.substring(index + 2, end))
                            pop()
                            index = end + 2
                        } else {
                            append(text[index])
                            index++
                        }
                    }

                    text.startsWith("*", index) -> {
                        val end = text.indexOf("*", startIndex = index + 1)
                        if (end > index) {
                            pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = contentColor))
                            append(text.substring(index + 1, end))
                            pop()
                            index = end + 1
                        } else {
                            append(text[index])
                            index++
                        }
                    }

                    text.startsWith("_", index) && canOpenUnderscoreEmphasis(text, index, markerLength = 1) -> {
                        val end = findClosingUnderscoreEmphasis(text, startIndex = index + 1, marker = "_")
                        if (end > index) {
                            pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = contentColor))
                            append(text.substring(index + 1, end))
                            pop()
                            index = end + 1
                        } else {
                            append(text[index])
                            index++
                        }
                    }

                    text.startsWith("`", index) -> {
                        val end = text.indexOf("`", startIndex = index + 1)
                        if (end > index) {
                            pushStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    color = ZhihuijiPrimary,
                                    background = ZhihuijiPrimary.copy(alpha = 0.10f)
                                )
                            )
                            append(" ${text.substring(index + 1, end)} ")
                            pop()
                            index = end + 1
                        } else {
                            append(text[index])
                            index++
                        }
                    }

                    else -> {
                        append(text[index])
                        index++
                    }
                }
            }
        }
    }
private fun hasInlineMarkdownSyntax(text: String): Boolean =
    text.indexOfAny(charArrayOf('[', '*', '`', '_')) >= 0 ||
        text.contains("https://", ignoreCase = true) ||
        text.contains("http://", ignoreCase = true) ||
        text.contains("www.", ignoreCase = true)

private fun AnnotatedString.Builder.appendVisibleUrlLink(rawUrl: String) {
    val url = normalizeMarkdownUrl(rawUrl) ?: rawUrl.trim()
    pushLink(
        LinkAnnotation.Url(
            url = url,
            styles = MarkdownLinkStyles
        )
    )
    append(url)
    pop()
}

private fun AnnotatedString.Builder.appendMarkdownLink(
    label: String,
    rawDestination: String,
) {
    val destination = markdownLinkDestination(rawDestination)
    val normalizedUrl = normalizeMarkdownUrl(destination)
    val visibleDestination = normalizedUrl ?: destination.trim()
    if (normalizedUrl == null) {
        append(label)
        if (visibleDestination.isNotBlank() && !label.equals(visibleDestination, ignoreCase = true)) {
            append(" ($visibleDestination)")
        }
        return
    }

    pushLink(
        LinkAnnotation.Url(
            url = normalizedUrl,
            styles = MarkdownLinkStyles
        )
    )
    append(label)
    if (!label.equals(normalizedUrl, ignoreCase = true)) {
        append(" ($normalizedUrl)")
    }
    pop()
}

private data class BareMarkdownUrl(
    val url: String,
    val trailingPunctuation: String,
    val endIndex: Int,
)

private fun startsBareMarkdownUrl(text: String, index: Int): Boolean {
    if (index > 0 && !text[index - 1].isWhitespace() && text[index - 1] != '(') return false
    return text.startsWith("https://", index, ignoreCase = true) ||
        text.startsWith("http://", index, ignoreCase = true) ||
        text.startsWith("www.", index, ignoreCase = true)
}

private fun bareMarkdownUrl(text: String, index: Int): BareMarkdownUrl? {
    var end = index
    while (end < text.length && !text[end].isWhitespace()) {
        end++
    }
    if (end <= index) return null

    val rawToken = text.substring(index, end)
    val url = rawToken.trimEnd(*BareUrlTrailingPunctuation)
    val trailing = rawToken.substring(url.length)
    return if (url.isBlank()) {
        null
    } else {
        BareMarkdownUrl(url = url, trailingPunctuation = trailing, endIndex = end)
    }
}

private val BareUrlTrailingPunctuation = charArrayOf(
    '.', ',', ';', ':', '!', '?',
    '。', '，', '；', '：', '！', '？',
    ')', '）',
)

private fun canOpenUnderscoreEmphasis(
    text: String,
    index: Int,
    markerLength: Int,
): Boolean {
    val previous = text.getOrNull(index - 1)
    val next = text.getOrNull(index + markerLength)
    return next != null &&
        !next.isWhitespace() &&
        previous?.isLetterOrDigit() != true &&
        previous != '_'
}

private fun findClosingUnderscoreEmphasis(
    text: String,
    startIndex: Int,
    marker: String,
): Int {
    var index = startIndex
    while (index < text.length) {
        val candidate = text.indexOf(marker, startIndex = index)
        if (candidate < 0) return -1
        val previous = text.getOrNull(candidate - 1)
        val next = text.getOrNull(candidate + marker.length)
        if (previous != null && !previous.isWhitespace() && next?.isLetterOrDigit() != true && next != '_') {
            return candidate
        }
        index = candidate + marker.length
    }
    return -1
}

private fun findMarkdownLinkDestinationEnd(text: String, startIndex: Int): Int {
    var depth = 0
    var inAngleDestination = text.getOrNull(startIndex) == '<'
    var index = startIndex
    while (index < text.length) {
        val char = text[index]
        when {
            char == '\\' && index + 1 < text.length -> index++
            inAngleDestination && char == '>' -> inAngleDestination = false
            inAngleDestination -> Unit
            char == '(' -> depth++
            char == ')' && depth > 0 -> depth--
            char == ')' -> return index
        }
        index++
    }
    return -1
}

private fun markdownLinkDestination(raw: String): String {
    val trimmed = raw.trim()
    val destination = if (trimmed.startsWith("<")) {
        val end = trimmed.indexOf(">")
        if (end > 0) trimmed.substring(1, end) else trimmed
    } else {
        trimmed
            .substringBefore(" \"")
            .substringBefore(" '")
            .substringBefore(" (")
    }
    return destination.trim()
}

private fun normalizeMarkdownUrl(url: String): String? {
    val trimmed = url.trim()
    return when {
        trimmed.isBlank() -> null
        trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("mailto:", ignoreCase = true) ||
            trimmed.startsWith("tel:", ignoreCase = true) -> trimmed
        trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
        else -> null
    }
}
