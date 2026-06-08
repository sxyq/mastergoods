package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.model.v2.agent.BarChartBlockData
import com.zhihuiji.core.model.v2.agent.DonutChartBlockData
import com.zhihuiji.core.model.v2.agent.DraftCardBlockData
import com.zhihuiji.core.model.v2.agent.EvidenceCardBlockData
import com.zhihuiji.core.model.v2.agent.KpiGridBlockData
import com.zhihuiji.core.model.v2.agent.LineChartBlockData
import com.zhihuiji.core.model.v2.agent.RankListBlockData
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RiskCardBlockData
import com.zhihuiji.core.model.v2.agent.TableBlockData
import com.zhihuiji.core.model.v2.agent.TextBlockData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 富结果块统一分发渲染器。
 *
 * 根据 block_type 分发到对应的渲染组件。
 */
@Composable
fun ResultBlockRenderer(
    block: ResultBlockDto,
    modifier: Modifier = Modifier,
) {
    when (block.blockType) {
        "text", "markdown" -> block.renderParsedBlock<TextBlockData>(modifier) {
            TextResultBlock(data = it, title = block.title, modifier = modifier)
        }

        "kpi_grid" -> block.renderParsedBlock<KpiGridBlockData>(modifier) {
            KpiGridBlock(data = it, title = block.title, modifier = modifier)
        }

        "table" -> block.renderParsedBlock<TableBlockData>(modifier) {
            TableBlock(data = it, title = block.title, modifier = modifier)
        }

        "rank_list" -> block.renderParsedBlock<RankListBlockData>(modifier) {
            RankListBlock(data = it, title = block.title, modifier = modifier)
        }

        "line_chart", "area_chart", "trend_chart" -> block.renderParsedBlock<LineChartBlockData>(modifier) {
            LineChartBlock(data = it, title = block.title, modifier = modifier)
        }

        "bar_chart", "column_chart", "horizontal_bar_chart" -> block.renderParsedBlock<BarChartBlockData>(modifier) {
            BarChartBlock(data = it, title = block.title, modifier = modifier)
        }

        "donut_chart", "pie_chart" -> block.renderParsedBlock<DonutChartBlockData>(modifier) {
            DonutChartBlock(data = it, title = block.title, modifier = modifier)
        }

        "risk_card" -> block.renderParsedBlock<RiskCardBlockData>(modifier) {
            RiskCardBlock(data = it, modifier = modifier)
        }

        "evidence_card" -> block.renderParsedBlock<EvidenceCardBlockData>(modifier) {
            EvidenceCardBlock(data = it, modifier = modifier)
        }

        "draft_card" -> block.renderParsedBlock<DraftCardBlockData>(modifier) {
            DraftCardBlock(data = it, modifier = modifier)
        }

        else -> {
            UnknownBlock(block = block, modifier = modifier)
        }
    }
}

@Composable
private inline fun <reified T> ResultBlockDto.renderParsedBlock(
    modifier: Modifier,
    content: @Composable (T) -> Unit,
) {
    val parsed = remember(blockType, data) { parseData<T>() }
    if (parsed == null) {
        BlockParseFailed(block = this, modifier = modifier)
    } else {
        content(parsed)
    }
}

internal inline fun <reified T> ResultBlockDto.parseData(): T? {
    return try {
        data?.let { ResultBlockJson.decodeFromJsonElement<T>(it) }
    } catch (e: Exception) {
        null
    }
}

private val ResultBlockJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

@Composable
private fun TextResultBlock(
    data: TextBlockData,
    title: String?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = Color.White.copy(alpha = 0.74f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            AgentMarkdownText(markdown = data.text, contentColor = TextPrimary)
        }
    }
}

// ---------- KPI Grid ----------

@Composable
private fun KpiGridBlock(
    data: KpiGridBlockData,
    title: String?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        blurRadius = 30.dp,
        surfaceColor = Color(0xFFEAF4FF).copy(alpha = 0.78f),
    ) {
        Column {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (data.kpis.isEmpty()) {
                EmptyStructuredMessage("本轮查询没有返回可展示的指标数据")
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    data.kpis.forEach { kpi ->
                        KpiMiniCard(kpi = kpi)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMiniCard(kpi: KpiGridBlockData.KpiItem) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.56f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = kpi.value,
            style = MaterialTheme.typography.titleMedium,
            color = ZhihuijiPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = kpi.label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        val trendValue = kpi.trendValue
        if (!trendValue.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trendValue,
                style = MaterialTheme.typography.labelSmall,
                color = when (kpi.trendDirection) {
                    "up" -> SuccessGreen
                    "down" -> DangerRed
                    else -> TextTertiary
                },
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------- Table ----------

@Composable
private fun TableBlock(
    data: TableBlockData,
    title: String?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        blurRadius = 30.dp,
        surfaceColor = Color(0xFFFFF8E7).copy(alpha = 0.80f),
    ) {
        Column {
            if (!title.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val contractError = validateTableContract(data.headers, data.rows)
            if (contractError != null) {
                ChartContractErrorMessage(contractError)
            } else {
                Column(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    // Header
                    Row {
                        data.headers.forEach { header ->
                            val inlineText = remember(header) {
                                inlineMarkdown(header, TextSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = inlineText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    // Rows
                    data.rows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier.background(
                                if (index % 2 == 0) Color.Transparent
                                else ZhihuijiPrimary.copy(alpha = 0.04f)
                            )
                        ) {
                            row.forEach { cell ->
                                val inlineText = remember(cell) {
                                    inlineMarkdown(cell, TextPrimary)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = inlineText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            data.rowCount?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "当前展示 ${data.rows.size} 行，服务端返回 $it 行",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }
    }
}

// ---------- Rank List ----------

@Composable
private fun RankListBlock(
    data: RankListBlockData,
    title: String?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        blurRadius = 30.dp,
        surfaceColor = Color(0xFFF3EEFF).copy(alpha = 0.78f),
    ) {
        Column {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (data.items.isEmpty()) {
                EmptyStructuredMessage("本轮查询没有返回可展示的排行数据")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.items.forEach { item ->
                        RankItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankItemRow(item: RankListBlockData.RankItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (item.rank) {
                            1 -> WarningOrange.copy(alpha = 0.15f)
                            2 -> ZhihuijiPrimary.copy(alpha = 0.1f)
                            3 -> SuccessGreen.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.rank}",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (item.rank) {
                        1 -> WarningOrange
                        2 -> ZhihuijiPrimary
                        3 -> SuccessGreen
                        else -> TextTertiary
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
            )
        }
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------- Charts ----------

@Composable
private fun LineChartBlock(
    data: LineChartBlockData,
    title: String?,
    modifier: Modifier = Modifier,
) {
    val renderSeries = data.series.mapIndexed { index, series ->
        ChartSeriesUi(
            name = series.name,
            values = series.data,
            color = chartColor(series.color, index),
        )
    }
    val labels = data.labels
    val contractError = validateChartContract(labels = labels, series = renderSeries)

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        blurRadius = 32.dp,
        surfaceColor = Color(0xFFEFF6FF).copy(alpha = 0.82f),
    ) {
        Column {
            ChartHeader(title = title ?: data.title ?: "折线趋势", icon = Icons.AutoMirrored.Filled.ShowChart)
            Spacer(modifier = Modifier.height(12.dp))
            if (contractError != null) {
                ChartContractErrorMessage(contractError)
            } else if (!hasChartValues(renderSeries)) {
                EmptyChartMessage("本轮查询没有返回可绘制的趋势数据")
            } else {
                LineChartCanvas(labels = labels, series = renderSeries)
                Spacer(modifier = Modifier.height(8.dp))
                ChartLabelStrip(labels = labels)
                Spacer(modifier = Modifier.height(10.dp))
                ChartLegend(items = renderSeries.map { it.name to it.color })
            }
        }
    }
}

@Composable
private fun BarChartBlock(
    data: BarChartBlockData,
    title: String?,
    modifier: Modifier = Modifier,
) {
    val renderSeries = data.series.mapIndexed { index, series ->
        ChartSeriesUi(
            name = series.name,
            values = series.data,
            color = chartColor(series.color, index),
        )
    }
    val labels = data.labels
    val contractError = validateChartContract(labels = labels, series = renderSeries)

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        blurRadius = 32.dp,
        surfaceColor = Color(0xFFFFF4EA).copy(alpha = 0.82f),
    ) {
        Column {
            ChartHeader(title = title ?: data.title ?: "柱状统计", icon = Icons.Default.BarChart)
            Spacer(modifier = Modifier.height(12.dp))
            if (contractError != null) {
                ChartContractErrorMessage(contractError)
            } else if (!hasChartValues(renderSeries)) {
                EmptyChartMessage("本轮查询没有返回可绘制的柱状数据")
            } else {
                BarChartCanvas(labels = labels, series = renderSeries)
                Spacer(modifier = Modifier.height(8.dp))
                ChartLabelStrip(labels = labels)
                Spacer(modifier = Modifier.height(10.dp))
                ChartLegend(items = renderSeries.map { it.name to it.color })
            }
        }
    }
}

@Composable
private fun DonutChartBlock(
    data: DonutChartBlockData,
    title: String?,
    modifier: Modifier = Modifier,
) {
    val segmentResult = donutChartSegments(data.segments)
    val segments = segmentResult.segments

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        blurRadius = 32.dp,
        surfaceColor = Color(0xFFF0FDF8).copy(alpha = 0.82f),
    ) {
        Column {
            ChartHeader(title = title ?: data.title ?: "占比分布", icon = Icons.Default.DonutLarge)
            Spacer(modifier = Modifier.height(12.dp))
            if (segments.isEmpty()) {
                EmptyChartMessage("本轮查询没有返回可绘制的分组数据")
            } else {
                val total = segments.sumOf { it.value }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DonutChartCanvas(
                        segments = segments,
                        modifier = Modifier.size(148.dp)
                    )
                    DonutLegend(
                        segments = segments,
                        total = total,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (segmentResult.ignoredCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ChartContractNotice("已忽略 ${segmentResult.ignoredCount} 个无效或非正数占比分段")
                }
            }
        }
    }
}

@Composable
private fun LineChartCanvas(
    labels: List<String>,
    series: List<ChartSeriesUi>,
    modifier: Modifier = Modifier,
) {
    val chartValues = series.flatMap { it.values.take(labels.size) }
        .filter { it.isUsableChartValue() }
    val minValue = minOf(0.0, chartValues.minOrNull() ?: 0.0)
    val maxValue = maxOf(0.0, chartValues.maxOrNull() ?: 1.0)
    val range = (maxValue - minValue).takeIf { it > 0.000001 } ?: 1.0

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val left = 10.dp.toPx()
        val right = 10.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 16.dp.toPx()
        val graphWidth = (size.width - left - right).coerceAtLeast(1f)
        val graphHeight = (bottom - top).coerceAtLeast(1f)

        repeat(4) { index ->
            val y = top + graphHeight * index / 3f
            drawLine(
                color = TextTertiary.copy(alpha = if (index == 3) 0.18f else 0.10f),
                start = Offset(left, y),
                end = Offset(left + graphWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val zeroY = top + ((maxValue - 0.0) / range).toFloat().coerceIn(0f, 1f) * graphHeight
        drawLine(
            color = ZhihuijiPrimary.copy(alpha = 0.12f),
            start = Offset(left, zeroY),
            end = Offset(left + graphWidth, zeroY),
            strokeWidth = 1.2.dp.toPx(),
        )

        series.forEachIndexed { seriesIndex, item ->
            val points = item.values.take(labels.size).mapIndexedNotNull { index, value ->
                if (!value.isUsableChartValue()) {
                    null
                } else {
                    val x = left + if (labels.size <= 1) {
                        graphWidth / 2f
                    } else {
                        graphWidth * index / (labels.size - 1).coerceAtLeast(1)
                    }
                    val ratio = ((maxValue - value) / range).toFloat().coerceIn(0f, 1f)
                    Offset(x, top + ratio * graphHeight)
                }
            }

            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                if (seriesIndex == 0) {
                    val areaPath = Path().apply {
                        moveTo(points.first().x, bottom)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, bottom)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(item.color.copy(alpha = 0.22f), Color.Transparent),
                            startY = top,
                            endY = bottom,
                        )
                    )
                }
                drawPath(
                    path = path,
                    color = item.color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            points.forEach { point ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = 5.dp.toPx(),
                    center = point,
                )
                drawCircle(
                    color = item.color,
                    radius = 3.2.dp.toPx(),
                    center = point,
                )
            }
        }
    }
}

@Composable
private fun BarChartCanvas(
    labels: List<String>,
    series: List<ChartSeriesUi>,
    modifier: Modifier = Modifier,
) {
    val chartValues = series.flatMap { it.values.take(labels.size) }
        .filter { it.isUsableChartValue() }
    val chartScale = barChartScale(chartValues)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val left = 10.dp.toPx()
        val right = 10.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 16.dp.toPx()
        val graphWidth = (size.width - left - right).coerceAtLeast(1f)
        val graphHeight = (bottom - top).coerceAtLeast(1f)
        val groupCount = labels.size.coerceAtLeast(1)

        repeat(4) { index ->
            val y = top + graphHeight * index / 3f
            drawLine(
                color = TextTertiary.copy(alpha = if (index == 3) 0.18f else 0.10f),
                start = Offset(left, y),
                end = Offset(left + graphWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val groupWidth = graphWidth / groupCount
        val barGap = 3.dp.toPx()
        val maxBarWidth = 22.dp.toPx()
        val candidateBarWidth = groupWidth / (series.size.coerceAtLeast(1) + 1.2f)
        val barWidth = candidateBarWidth.coerceAtLeast(4.dp.toPx()).coerceAtMost(maxBarWidth)
        val groupBarsWidth = barWidth * series.size + barGap * (series.size - 1).coerceAtLeast(0)
        val zeroY = top + ((chartScale.maxValue - 0.0) / chartScale.range).toFloat().coerceIn(0f, 1f) * graphHeight
        drawLine(
            color = TextTertiary.copy(alpha = 0.22f),
            start = Offset(left, zeroY),
            end = Offset(left + graphWidth, zeroY),
            strokeWidth = 1.2.dp.toPx(),
        )

        repeat(groupCount) { labelIndex ->
            val groupStart = left + groupWidth * labelIndex + (groupWidth - groupBarsWidth) / 2f
            series.forEachIndexed { seriesIndex, item ->
                val value = item.values[labelIndex]
                val valueY = top + ((chartScale.maxValue - value) / chartScale.range).toFloat().coerceIn(0f, 1f) * graphHeight
                val rawBarHeight = kotlin.math.abs(valueY - zeroY)
                val barHeight = rawBarHeight.coerceAtLeast(if (value != 0.0) 3.dp.toPx() else 0f)
                val x = groupStart + seriesIndex * (barWidth + barGap)
                val y = if (value >= 0.0) {
                    zeroY - barHeight
                } else {
                    zeroY
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(item.color.copy(alpha = 0.88f), item.color.copy(alpha = 0.48f)),
                        startY = y,
                        endY = y + barHeight,
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                )
            }
        }
    }
}

internal data class BarChartScale(
    val minValue: Double,
    val maxValue: Double,
    val range: Double,
)

internal fun barChartScale(values: List<Double>): BarChartScale {
    val usableValues = values.filter { it.isUsableChartValue() }
    val minValue = minOf(0.0, usableValues.minOrNull() ?: 0.0)
    val maxValue = maxOf(0.0, usableValues.maxOrNull() ?: 1.0)
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
    return BarChartScale(
        minValue = minValue,
        maxValue = maxValue,
        range = range,
    )
}

internal data class DonutSegmentFilterResult(
    val segments: List<ChartSegmentUi>,
    val ignoredCount: Int,
)

internal fun donutChartSegments(rawSegments: List<DonutChartBlockData.Segment>): DonutSegmentFilterResult {
    var ignoredCount = 0
    val segments = rawSegments.mapIndexedNotNull { index, segment ->
        if (!segment.value.isUsableChartValue() || segment.value <= 0.0) {
            ignoredCount++
            null
        } else {
            ChartSegmentUi(
                name = segment.name,
                value = segment.value,
                color = chartColor(segment.color, index),
            )
        }
    }
    return DonutSegmentFilterResult(
        segments = segments,
        ignoredCount = ignoredCount,
    )
}

@Composable
private fun DonutChartCanvas(
    segments: List<ChartSegmentUi>,
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.value }.takeIf { it > 0.0 } ?: 1.0
    Canvas(modifier = modifier) {
        val strokeWidth = 22.dp.toPx()
        val diameter = (size.minDimension - strokeWidth).coerceAtLeast(1f)
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f

        drawCircle(
            color = TextTertiary.copy(alpha = 0.10f),
            radius = diameter / 2f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = strokeWidth)
        )

        segments.forEach { segment ->
            val sweep = (segment.value / total * 360.0).toFloat().coerceAtLeast(1.2f)
            drawArc(
                color = segment.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
            startAngle += sweep
        }

        drawCircle(
            color = Color.White.copy(alpha = 0.70f),
            radius = (diameter - strokeWidth) / 2.4f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}

@Composable
private fun ChartLabelStrip(labels: List<String>) {
    if (labels.isEmpty()) {
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.64f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ChartLegend(items: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (name, color) ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(color.copy(alpha = 0.10f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DonutLegend(
    segments: List<ChartSegmentUi>,
    total: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        segments.take(6).forEach { segment ->
            val percent = segment.value / total * 100.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(segment.color)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = segment.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${formatChartValue(segment.value)} · ${formatChartValue(percent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ChartContractErrorMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(WarningOrange.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = WarningOrange,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ChartContractNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningOrange.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = WarningOrange,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyChartMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.50f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyStructuredMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.50f))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChartHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ZhihuijiPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class ChartSeriesUi(
    val name: String,
    val values: List<Double>,
    val color: Color,
)

internal data class ChartSegmentUi(
    val name: String,
    val value: Double,
    val color: Color,
)

private val ChartPalette = listOf(
    ZhihuijiPrimary,
    SuccessGreen,
    WarningOrange,
    DangerRed,
    Color(0xFF7C3AED),
    Color(0xFF0891B2),
)

private fun chartColor(rawColor: String?, index: Int): Color {
    if (!rawColor.isNullOrBlank()) {
        val normalized = rawColor.trim().removePrefix("#")
        val argb = when (normalized.length) {
            6 -> "FF$normalized"
            8 -> normalized
            else -> null
        }
        if (argb != null) {
            runCatching { Color(argb.toLong(16)) }.getOrNull()?.let { return it }
        }
    }
    return ChartPalette[index % ChartPalette.size]
}

private fun hasChartValues(series: List<ChartSeriesUi>): Boolean =
    series.any { item -> item.values.any { it.isUsableChartValue() } }

internal fun validateChartContractForSeries(
    labels: List<String>,
    series: List<Pair<String, List<Double>>>,
): String? = validateChartContract(
    labels = labels,
    series = series.mapIndexed { index, item ->
        ChartSeriesUi(
            name = item.first,
            values = item.second,
            color = chartColor(rawColor = null, index = index),
        )
    }
)

private fun validateChartContract(labels: List<String>, series: List<ChartSeriesUi>): String? {
    if (labels.isEmpty()) {
        return "图表数据缺少横轴标签，已停止绘制以避免生成模拟标签"
    }
    if (labels.any { it.isBlank() }) {
        return "图表横轴标签存在空值，已停止绘制以避免生成模拟标签"
    }
    if (series.isEmpty()) {
        return "图表数据缺少真实序列，无法绘制"
    }
    val mismatchedSeries = series.firstOrNull { it.values.size != labels.size }
    if (mismatchedSeries != null) {
        return "图表序列「${mismatchedSeries.name}」的数据量与标签数量不一致，已停止绘制"
    }
    val invalidSeries = series.firstOrNull { item -> item.values.any { !it.isUsableChartValue() } }
    if (invalidSeries != null) {
        return "图表序列「${invalidSeries.name}」包含无效数值，已停止绘制"
    }
    return null
}

internal fun validateTableContract(headers: List<String>, rows: List<List<String>>): String? {
    if (headers.isEmpty()) {
        return "表格数据缺少真实列名，无法渲染"
    }
    if (headers.any { it.isBlank() }) {
        return "表格列名存在空值，已停止渲染以避免误读"
    }
    if (rows.isEmpty()) {
        return "本轮查询没有返回可展示的表格行数据"
    }
    val mismatchedRowIndex = rows.indexOfFirst { row -> row.size != headers.size }
    if (mismatchedRowIndex >= 0) {
        return "表格第 ${mismatchedRowIndex + 1} 行的数据量与列名数量不一致，已停止渲染"
    }
    return null
}

private fun Double.isUsableChartValue(): Boolean = !isNaN() && !isInfinite()

private fun formatChartValue(value: Double): String =
    if (kotlin.math.abs(value - kotlin.math.round(value)) < 0.000001) {
        value.toLong().toString()
    } else {
        "%.2f".format(value)
    }

// ---------- Risk Card ----------

@Composable
private fun RiskCardBlock(data: RiskCardBlockData, modifier: Modifier = Modifier) {
    val (bgColor, iconColor) = when (data.level) {
        "high" -> DangerRed.copy(alpha = 0.08f) to DangerRed
        "medium" -> WarningOrange.copy(alpha = 0.08f) to WarningOrange
        else -> SuccessGreen.copy(alpha = 0.08f) to SuccessGreen
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = bgColor,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = iconColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = data.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            data.affectedItems?.let { items ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "影响: ${items.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
            data.suggestedAction?.let { action ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "建议: $action",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZhihuijiPrimary,
                )
            }
        }
    }
}

// ---------- Evidence Card ----------

@Composable
private fun EvidenceCardBlock(data: EvidenceCardBlockData, modifier: Modifier = Modifier) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = Color(0xFFF5F8FF).copy(alpha = 0.70f),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = data.title ?: "依据",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (data.items.isEmpty()) {
                EmptyStructuredMessage("本轮查询没有返回可展示的依据明细")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    data.items.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                                Text(
                                    text = item.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (!item.source.isNullOrBlank()) {
                                Text(
                                    text = "来源: ${item.source}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                )
                            }
                            item.auditSummary()?.let { audit ->
                                Text(
                                    text = audit,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isTruncated == true) WarningOrange else TextTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun EvidenceCardBlockData.EvidenceItem.auditSummary(): String? {
    val parts = listOfNotNull(
        toolCallId?.takeIf { it.isNotBlank() }?.let { "调用 ${it.compactMiddle()}" },
        queryWindow?.evidenceQueryWindowSummary()?.let { "范围 $it" },
        isTruncated?.takeIf { it }?.let { "结果已截断" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun JsonElement.evidenceQueryWindowSummary(): String? {
    val obj = this as? JsonObject ?: return compactJsonText()
    val parts = listOfNotNull(
        obj.stringValue("owner_scope")?.let { if (it == "current_owner") "当前账号" else it },
        obj.intValue("window_days")?.let { "近 ${it} 天" },
        obj.intValue("limit")?.let { "上限 $it 条" },
        obj.intValue("rank_limit")?.let { "排行 $it 条" },
        obj.intValue("low_stock_limit")?.let { "低库存 $it 条" },
        obj.booleanValue("is_truncated")?.takeIf { it }?.let { "已截断" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: compactJsonText()
}

private fun JsonObject.stringValue(key: String): String? =
    this[key]?.jsonPrimitiveOrNull()?.contentOrNull

private fun JsonObject.intValue(key: String): Int? =
    this[key]?.jsonPrimitiveOrNull()?.intOrNull

private fun JsonObject.booleanValue(key: String): Boolean? =
    this[key]?.jsonPrimitiveOrNull()?.booleanOrNull

private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? =
    runCatching { jsonPrimitive }.getOrNull()

// ---------- Draft Card ----------

@Composable
private fun DraftCardBlock(data: DraftCardBlockData, modifier: Modifier = Modifier) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = Color(0xFFFFF7ED).copy(alpha = 0.72f),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = data.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "这是 AI 草稿，尚未执行业务写入。",
                style = MaterialTheme.typography.labelSmall,
                color = WarningOrange,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.itemCount?.let {
                    Text(
                        text = "$it 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                data.totalAmount?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = ZhihuijiPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            data.warnings?.let { warnings ->
                if (warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    warnings.forEach { warning ->
                        Text(
                            text = "⚠ $warning",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningOrange,
                        )
                    }
                }
            }
        }
    }
}

// ---------- Unknown Block ----------

@Composable
private fun BlockParseFailed(
    block: ResultBlockDto,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        surfaceColor = WarningOrange.copy(alpha = 0.08f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = block.title ?: "结构化结果解析失败",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarningOrange,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "后端返回了 ${block.blockType} 数据块，但当前 Android 端无法解析其字段。",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                block.dataPreview()?.let { preview ->
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnknownBlock(block: ResultBlockDto, modifier: Modifier = Modifier) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        surfaceColor = TextTertiary.copy(alpha = 0.08f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = block.title ?: "暂不支持的结构化结果",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "后端返回了 ${block.blockType} 数据块，当前 Android 端暂未提供专用渲染器。",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            block.dataPreview()?.let { preview ->
                Text(
                    text = preview,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun ResultBlockDto.dataPreview(): String? {
    val raw = data?.toString()?.takeIf { it.isNotBlank() } ?: return null
    return "原始数据: " + raw.take(240)
}

private fun JsonElement.compactJsonText(maxLength: Int = 90): String? =
    toString().takeIf { it.isNotBlank() }?.let { raw ->
        if (raw.length <= maxLength) raw else raw.take(maxLength) + "..."
    }

private fun String.compactMiddle(maxLength: Int = 28): String =
    if (length <= maxLength) this else take(14) + "..." + takeLast(8)
