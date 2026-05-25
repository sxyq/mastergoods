package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class KpiTone { PRIMARY, SUCCESS, WARNING, DANGER }

@Composable
fun KpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trend: String? = null,
    icon: ImageVector? = null,
    tone: KpiTone = KpiTone.PRIMARY,
) {
    val valueColor = when (tone) {
        KpiTone.PRIMARY -> ZhihuijiColors.Primary
        KpiTone.SUCCESS -> ZhihuijiColors.Success
        KpiTone.WARNING -> ZhihuijiColors.Warning
        KpiTone.DANGER -> ZhihuijiColors.Danger
    }
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = value, style = ZhihuijiTypography.headlineLarge, color = valueColor)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                }
                if (trend != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val trendColor = if (trend.contains("+") || trend.contains("↑")) ZhihuijiColors.Success else ZhihuijiColors.Danger
                    Text(text = trend, style = ZhihuijiTypography.labelSmall, color = trendColor)
                }
            }
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(valueColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = valueColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}
