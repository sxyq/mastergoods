package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * KPI 指标卡片
 *
 * 统一使用 liquid glass 玻璃容器，内部保留轻渐变强化数据卡层级。
 *
 * @param modifier 修饰符
 * @param title 指标名称
 * @param value 指标数值
 * @param changePercent 变化百分比
 * @param isPositive 是否为正向变化
 * @param backgroundColors 背景渐变颜色
 */
@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    changePercent: String? = null,
    isPositive: Boolean = true,
    backgroundColors: List<Color> = listOf(
        ZhihuijiPrimaryLight.copy(alpha = 0.6f),
        SurfaceWhite.copy(alpha = 0.8f)
    )
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceMedium,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = rememberKpiGradient(backgroundColors),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = AmountTextStyle,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (changePercent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isPositive) SuccessGreen else DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = changePercent,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isPositive) SuccessGreen else DangerRed
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberKpiGradient(colors: List<Color>): Brush {
    return remember(colors) { Brush.linearGradient(colors = colors) }
}
