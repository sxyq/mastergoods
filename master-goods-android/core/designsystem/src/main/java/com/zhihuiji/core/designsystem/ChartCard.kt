package com.zhihuiji.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun LineTrendChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val safeSize = minOf(values.size, labels.size)
    val safeValues = values.take(safeSize)
    val safeLabels = labels.take(safeSize)
    val maxValue = max(safeValues.maxOrNull() ?: 0.0, 1.0)
    Canvas(modifier = modifier.fillMaxWidth().height(112.dp)) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val chartHeight = bottom - top
        val stepX = if (safeValues.size > 1) (right - left) / (safeValues.size - 1) else 0f

        repeat(4) { index ->
            val y = top + chartHeight * index / 3f
            drawLine(
                color = ZhihuijiColors.CardBorder.copy(alpha = 0.72f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val points = safeValues.mapIndexed { index, value ->
            val x = left + stepX * index
            val y = bottom - (value / maxValue).toFloat() * chartHeight
            Offset(x, y)
        }
        if (points.isNotEmpty()) {
            val area = Path().apply {
                moveTo(points.first().x, bottom)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, bottom)
                close()
            }
            drawPath(area, color = ZhihuijiColors.Primary.copy(alpha = 0.10f))
            points.zipWithNext().forEach { (a, b) ->
                drawLine(
                    color = ZhihuijiColors.Primary,
                    start = a,
                    end = b,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            points.forEach { point ->
                drawCircle(color = ZhihuijiColors.White, radius = 5.dp.toPx(), center = point)
                drawCircle(color = ZhihuijiColors.Primary, radius = 3.dp.toPx(), center = point)
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        safeLabels.forEach {
            Text(text = it, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
        }
    }
}

@Composable
fun RingMetricChart(
    primaryValue: Double,
    secondaryValue: Double,
    centerText: String,
    primaryLabel: String,
    secondaryLabel: String,
    modifier: Modifier = Modifier,
) {
    val total = max(primaryValue + secondaryValue, 1.0)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = ZhihuijiColors.CardBorder,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width, size.height),
                )
                drawArc(
                    color = ZhihuijiColors.Primary,
                    startAngle = -90f,
                    sweepAngle = (primaryValue / total * 360).toFloat(),
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width, size.height),
                )
                drawArc(
                    color = ZhihuijiColors.Warning,
                    startAngle = -90f + (primaryValue / total * 360).toFloat() + 8f,
                    sweepAngle = (secondaryValue / total * 360).toFloat().coerceAtLeast(0f),
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width, size.height),
                )
            }
            Text(text = centerText, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            LegendRow(color = ZhihuijiColors.Primary, label = primaryLabel)
            LegendRow(color = ZhihuijiColors.Warning, label = secondaryLabel)
        }
    }
}

@Composable
fun HorizontalBarChart(
    items: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    maxBars: Int = 5,
) {
    val maxValue = max(items.maxOfOrNull { it.second } ?: 0.0, 1.0)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.take(maxBars).forEachIndexed { index, item ->
            val fraction = (item.second / maxValue).toFloat().coerceIn(0.05f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 1}. ${item.first}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                    Text(item.second.formatChartAmount(), style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.Primary)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(ZhihuijiColors.PressedBlue, RoundedCornerShape(100.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(
                                if (index == 0) ZhihuijiColors.Primary else ZhihuijiColors.Primary.copy(alpha = 0.62f),
                                RoundedCornerShape(100.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(9.dp).background(color, RoundedCornerShape(100.dp)))
        Text(label, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
    }
}

private fun Double.formatChartAmount(): String = when {
    this >= 10000 -> "%.1f万".format(this / 10000)
    this >= 1000 -> "%.1fk".format(this / 1000)
    else -> "%.2f".format(this)
}
