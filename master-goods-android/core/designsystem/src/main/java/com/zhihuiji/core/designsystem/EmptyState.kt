package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = ZhihuijiColors.TextTertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextTertiary)
        }
    }
}

@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = ZhihuijiColors.TextTertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextTertiary)
        }
    }
}
