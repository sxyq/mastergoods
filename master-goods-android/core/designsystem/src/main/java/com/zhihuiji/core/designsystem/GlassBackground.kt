package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

fun Modifier.glassBackground(): Modifier = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            ZhihuijiColors.BackgroundGradientStart,
            ZhihuijiColors.BackgroundGradientMid,
            ZhihuijiColors.BackgroundGradientEnd,
        )
    )
)
