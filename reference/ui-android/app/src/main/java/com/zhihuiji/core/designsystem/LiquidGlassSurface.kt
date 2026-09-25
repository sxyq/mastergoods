package com.zhihuiji.core.designsystem

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * Boundary for the glass scene.
 *
 * The real AndroidLiquidGlass backdrop renderer is intentionally not attached in production yet:
 * device verification on Android 16 crashed during the first Compose measure pass with
 * "LayoutNode should be attached to an owner". Keep the boundary so screens can share one API,
 * but render the stable static glass fallback until the renderer is proven in isolation.
 */
@Composable
fun GlassBackdropLayer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

/**
 * 液态玻璃表面容器
 *
 * 生产路径使用稳定的半透明渐变、柔和阴影与白色描边模拟玻璃质感。
 * 业务页面优先复用本容器，避免各页面自行模拟玻璃效果导致视觉漂移。
 * 若显式传入 `backdrop`，则仅在该局部区域启用真实 backdrop blur。
 *
 * @param modifier 修饰符
 * @param blurRadius 模糊半径
 * @param shape 圆角形状，默认 16dp
 * @param enableVibrancy 是否启用色彩增强
 * @param content 内容
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    enableVibrancy: Boolean = true,
    surfaceColor: Color = GlassSurfaceMedium,
    backdrop: Backdrop? = null,
    content: @Composable () -> Unit
) {
    val staticSurfaceBrush = remember(surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                surfaceColor.copy(alpha = (surfaceColor.alpha + 0.14f).coerceAtMost(0.82f)),
                surfaceColor
            )
        )
    }
    val dynamicSurfaceBrush = remember(surfaceColor) {
        val topAlpha = (surfaceColor.alpha + 0.20f).coerceAtMost(0.96f)
        val bottomAlpha = (surfaceColor.alpha + 0.32f).coerceAtMost(0.99f)
        Brush.verticalGradient(
            colors = listOf(
                surfaceColor.copy(alpha = topAlpha),
                surfaceColor.copy(alpha = bottomAlpha),
            )
        )
    }
    val radialHighlightColors = remember {
        listOf(
            Color.White.copy(alpha = 0.32f),
            Color.Transparent
        )
    }
    Box(
        modifier = if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modifier.dynamicLiquidGlass(
                backdrop = backdrop,
                blurRadius = blurRadius,
                shape = shape,
                enableVibrancy = enableVibrancy,
                surfaceBrush = dynamicSurfaceBrush,
                radialHighlightColors = radialHighlightColors,
            )
        } else {
            modifier.staticLiquidGlass(
                shape = shape,
                surfaceBrush = staticSurfaceBrush,
                radialHighlightColors = radialHighlightColors,
            )
        }
    ) {
        content()
    }
}

private fun Modifier.staticLiquidGlass(
    shape: RoundedCornerShape,
    surfaceBrush: Brush,
    radialHighlightColors: List<Color>,
): Modifier =
    this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = GlassShadow,
            spotColor = GlassShadow
        )
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .glassHighlightOverlay(radialHighlightColors)
        .border(width = 0.5.dp, color = GlassBorder, shape = shape)

private fun Modifier.dynamicLiquidGlass(
    backdrop: Backdrop,
    blurRadius: Dp,
    shape: RoundedCornerShape,
    enableVibrancy: Boolean,
    surfaceBrush: Brush,
    radialHighlightColors: List<Color>,
): Modifier =
    this
        .shadow(
            elevation = 14.dp,
            shape = shape,
            clip = false,
            ambientColor = GlassShadow.copy(alpha = 0.20f),
            spotColor = GlassShadow.copy(alpha = 0.24f)
        )
        .clip(shape)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(blurRadius.toPx())
                if (enableVibrancy) {
                    vibrancy()
                }
            },
            highlight = {
                Highlight.Default.copy(alpha = 0.98f)
            },
            shadow = {
                Shadow.Default.copy(
                    color = GlassShadow.copy(alpha = 0.20f),
                    alpha = 0.9f
                )
            },
            innerShadow = {
                InnerShadow(
                    radius = 12.dp,
                    alpha = 0.20f,
                    color = Color.White.copy(alpha = 0.18f)
                )
            },
            onDrawSurface = {
                drawRect(brush = surfaceBrush)
            }
        )
        .glassHighlightOverlay(radialHighlightColors)
        .border(width = 0.5.dp, color = GlassBorder, shape = shape)

private fun Modifier.glassHighlightOverlay(
    radialHighlightColors: List<Color>,
): Modifier = drawWithCache {
    val highlightCenter = Offset(size.width * 0.18f, size.height * 0.08f)
    val highlightRadius = size.width * 0.72f
    val highlightBrush = Brush.radialGradient(
        colors = radialHighlightColors,
        center = highlightCenter,
        radius = highlightRadius
    )
    onDrawWithContent {
        drawContent()
        drawCircle(
            brush = highlightBrush,
            radius = highlightRadius,
            center = highlightCenter
        )
    }
}
