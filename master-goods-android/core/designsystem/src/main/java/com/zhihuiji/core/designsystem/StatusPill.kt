package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class PillTone {
    SUCCESS, WARNING, DANGER, INFO, NEUTRAL
}

@Composable
fun StatusPill(
    text: String,
    tone: PillTone,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor) = when (tone) {
        PillTone.SUCCESS -> ZhihuijiColors.Success.copy(alpha = 0.14f) to ZhihuijiColors.Success
        PillTone.WARNING -> ZhihuijiColors.Warning.copy(alpha = 0.14f) to ZhihuijiColors.Warning
        PillTone.DANGER -> ZhihuijiColors.Danger.copy(alpha = 0.14f) to ZhihuijiColors.Danger
        PillTone.INFO -> ZhihuijiColors.InfoBlue.copy(alpha = 0.14f) to ZhihuijiColors.InfoBlue
        PillTone.NEUTRAL -> ZhihuijiColors.SurfaceVariant to ZhihuijiColors.TextSecondary
    }
    Text(
        text = text,
        style = ZhihuijiTypography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
