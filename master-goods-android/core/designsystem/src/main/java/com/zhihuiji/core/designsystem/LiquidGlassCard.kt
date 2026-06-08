package com.zhihuiji.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
/**
 * 液态玻璃卡片组件
 *
 * 基于 LiquidGlassSurface 的卡片组件，支持点击效果。
 *
 * @param modifier 修饰符
 * @param onClick 点击回调
 * @param blurRadius 模糊半径，默认 18dp
 * @param shape 圆角形状，默认 16dp
 * @param enableVibrancy 是否启用色彩增强，默认 true
 * @param contentPadding 内容内边距，默认 16dp
 * @param content 内容
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    blurRadius: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    enableVibrancy: Boolean = true,
    surfaceColor: Color = GlassSurfaceMedium,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    LiquidGlassSurface(
        modifier = modifier
            .clip(shape)
            .then(clickableModifier),
        blurRadius = blurRadius,
        shape = shape,
        enableVibrancy = enableVibrancy,
        surfaceColor = surfaceColor
    ) {
        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}
