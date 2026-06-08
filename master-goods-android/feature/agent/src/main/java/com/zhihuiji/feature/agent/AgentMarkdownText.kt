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

@Composable
fun AgentMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    contentColor: Color = TextPrimary,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEach { block ->
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

@Composable
private fun MarkdownHeading(block: MarkdownBlock.Heading, contentColor: Color) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    val inlineText = remember(block.text, contentColor) {
        inlineMarkdown(block.text, contentColor)
    }
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
    val inlineText = remember(text, contentColor) {
        inlineMarkdown(text, contentColor)
    }
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
            val inlineText = remember(item, contentColor) {
                inlineMarkdown(item, contentColor)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (block.ordered) "${index + 1}." else "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZhihuijiPrimary,
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
            text = block.code,
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
    val inlineText = remember(block.text, quoteColor) {
        inlineMarkdown(block.text, quoteColor)
    }
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
            val inlineText = remember(cell, cellColor) {
                inlineMarkdown(cell, cellColor)
            }
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

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListBlock(val ordered: Boolean, val items: List<String>) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data object Divider : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        if (trimmed.isBlank()) {
            index++
            continue
        }

        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val code = StringBuilder()
            index++
            while (index < lines.size && !lines[index].trim().startsWith("```")) {
                code.appendLine(lines[index])
                index++
            }
            if (index < lines.size) index++
            blocks += MarkdownBlock.CodeBlock(language, code.toString())
            continue
        }

        if (isTableStart(lines, index)) {
            val headers = parseTableRow(lines[index])
            index += 2
            val rows = mutableListOf<List<String>>()
            while (index < lines.size && hasTableDelimiter(lines[index]) && lines[index].trim().isNotBlank()) {
                rows += parseTableRow(lines[index])
                index++
            }
            blocks += MarkdownBlock.Table(headers, rows)
            continue
        }

        val heading = headingMatch(trimmed)
        if (heading != null) {
            blocks += MarkdownBlock.Heading(heading.first, heading.second)
            index++
            continue
        }

        if (trimmed.matches(Regex("^[-*_]{3,}$"))) {
            blocks += MarkdownBlock.Divider
            index++
            continue
        }

        if (trimmed.startsWith(">")) {
            val parts = mutableListOf<String>()
            while (index < lines.size && lines[index].trim().startsWith(">")) {
                parts += lines[index].trim().removePrefix(">").trim()
                index++
            }
            blocks += MarkdownBlock.Quote(parts.joinToString("\n"))
            continue
        }

        val firstListItem = listMatch(trimmed)
        if (firstListItem != null) {
            val ordered = firstListItem.first
            val items = mutableListOf(firstListItem.second)
            index++
            while (index < lines.size) {
                val match = listMatch(lines[index].trim())
                if (match == null || match.first != ordered) break
                items += match.second
                index++
            }
            blocks += MarkdownBlock.ListBlock(ordered, items)
            continue
        }

        val paragraph = mutableListOf(trimmed)
        index++
        while (index < lines.size) {
            val next = lines[index].trim()
            if (next.isBlank() || next.startsWith("```") || headingMatch(next) != null ||
                next.startsWith(">") || listMatch(next) != null || next.matches(Regex("^[-*_]{3,}$")) ||
                isTableStart(lines, index)
            ) {
                break
            }
            paragraph += next
            index++
        }
        blocks += MarkdownBlock.Paragraph(paragraph.joinToString(" "))
    }

    return blocks.ifEmpty { listOf(MarkdownBlock.Paragraph(markdown)) }
}

private fun headingMatch(line: String): Pair<Int, String>? {
    val match = Regex("^(#{1,6})\\s+(.+)$").find(line) ?: return null
    return match.groupValues[1].length to match.groupValues[2].trim()
}

private fun listMatch(line: String): Pair<Boolean, String>? {
    Regex("^[-*+]\\s+(.+)$").find(line)?.let {
        return false to it.groupValues[1].trim()
    }
    Regex("^\\d+[.)]\\s+(.+)$").find(line)?.let {
        return true to it.groupValues[1].trim()
    }
    return null
}

private fun isTableStart(lines: List<String>, index: Int): Boolean {
    if (index + 1 >= lines.size) return false
    val header = lines[index].trim()
    val separator = lines[index + 1].trim()
    if (!hasTableDelimiter(header) || !hasTableDelimiter(separator)) return false

    val headerCells = parseTableRow(header)
    val separatorCells = parseTableRow(separator)
    return headerCells.size >= 2 &&
        separatorCells.size == headerCells.size &&
        separatorCells.all { cell -> cell.trim().matches(Regex(":?-{3,}:?")) }
}

private fun hasTableDelimiter(line: String): Boolean {
    var inCodeSpan = false
    var index = 0
    while (index < line.length) {
        val char = line[index]
        when {
            char == '\\' && index + 1 < line.length -> index++
            char == '`' -> inCodeSpan = !inCodeSpan
            char == '|' && !inCodeSpan -> return true
        }
        index++
    }
    return false
}

private fun parseTableRow(line: String): List<String> {
    val normalized = line.trim().trimUnescapedBoundaryPipes()
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var inCodeSpan = false
    var index = 0

    while (index < normalized.length) {
        val char = normalized[index]
        when {
            char == '\\' && index + 1 < normalized.length && normalized[index + 1] == '|' && !inCodeSpan -> {
                current.append('|')
                index++
            }

            char == '`' -> {
                inCodeSpan = !inCodeSpan
                current.append(char)
            }

            char == '|' && !inCodeSpan -> {
                cells += current.toString().trim()
                current.clear()
            }

            else -> current.append(char)
        }
        index++
    }

    cells += current.toString().trim()
    return cells
}

private fun String.trimUnescapedBoundaryPipes(): String {
    var start = 0
    var end = length
    if (start < end && this[start] == '|') {
        start++
    }
    if (start < end && this[end - 1] == '|' && !isEscaped(end - 1)) {
        end--
    }
    return substring(start, end)
}

private fun String.isEscaped(index: Int): Boolean {
    var slashCount = 0
    var cursor = index - 1
    while (cursor >= 0 && this[cursor] == '\\') {
        slashCount++
        cursor--
    }
    return slashCount % 2 == 1
}

internal fun inlineMarkdown(text: String, contentColor: Color): AnnotatedString =
    buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("[", index) -> {
                    val labelEnd = text.indexOf("](", startIndex = index + 1)
                    val urlEnd = if (labelEnd > index) text.indexOf(")", startIndex = labelEnd + 2) else -1
                    if (labelEnd > index && urlEnd > labelEnd) {
                        val label = text.substring(index + 1, labelEnd).ifBlank {
                            text.substring(labelEnd + 2, urlEnd).trim()
                        }
                        val url = normalizeMarkdownUrl(text.substring(labelEnd + 2, urlEnd))
                        pushLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = ZhihuijiPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = TextDecoration.Underline,
                                    )
                                )
                            )
                        )
                        append(label)
                        if (!label.equals(url, ignoreCase = true)) {
                            append(" ($url)")
                        }
                        pop()
                        index = urlEnd + 1
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

private fun normalizeMarkdownUrl(url: String): String {
    val trimmed = url.trim()
    return when {
        trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("mailto:", ignoreCase = true) ||
            trimmed.startsWith("tel:", ignoreCase = true) -> trimmed
        trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
        else -> trimmed
    }
}
