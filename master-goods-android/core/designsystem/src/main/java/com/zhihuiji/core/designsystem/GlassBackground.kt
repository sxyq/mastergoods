package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind

data class GlassOrb(
    val color: Color,
    val alpha: Float,
    val radiusFraction: Float,
    val centerOffset: Offset,
)

val DefaultGlassOrbs = listOf(
    GlassOrb(Color(0xFFBEE7FF), 0.22f, 0.38f, Offset(0.18f, 0.10f)),
    GlassOrb(Color(0xFF8ACBFF), 0.14f, 0.28f, Offset(0.84f, 0.16f)),
    GlassOrb(Color(0xFFD9F2FF), 0.18f, 0.46f, Offset(0.78f, 0.82f)),
)

fun Modifier.glassBackground(
    orbs: List<GlassOrb> = DefaultGlassOrbs,
): Modifier = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            ZhihuijiColors.BackgroundGradientStart,
            ZhihuijiColors.BackgroundGradientMid,
            ZhihuijiColors.BackgroundGradientEnd,
        )
    )
).drawBehind {
    val maxRadius = size.minDimension
    orbs.forEach { orb ->
        drawCircle(
            color = orb.color.copy(alpha = orb.alpha),
            radius = maxRadius * orb.radiusFraction,
            center = Offset(size.width * orb.centerOffset.x, size.height * orb.centerOffset.y),
        )
    }
}
