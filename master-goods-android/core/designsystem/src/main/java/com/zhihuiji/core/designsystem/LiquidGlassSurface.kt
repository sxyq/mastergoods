package com.zhihuiji.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    blurRadius: Dp = 20.dp,
    surfaceAlpha: Float = 0.16f,
    lensProgress: Float = 0f,
    highlightAlpha: Float = 0.92f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val lensRefractionHeight = remember(lensProgress) { (12f + 10f * lensProgress).dp }
    val lensRefractionAmount = remember(lensProgress) { (10f + 10f * lensProgress).dp }
    val shadowColor = remember(lensProgress) {
        ZhihuijiColors.Primary.copy(alpha = 0.10f + 0.06f * lensProgress)
    }
    val innerShadow = remember(lensProgress) {
        InnerShadow(radius = 7.dp + (3.dp * lensProgress), alpha = 0.28f + 0.10f * lensProgress)
    }
    val surfaceColor = remember(surfaceAlpha, lensProgress) {
        Color.White.copy(alpha = surfaceAlpha + 0.04f * lensProgress)
    }
    val highlight = remember(highlightAlpha) { Highlight.Default.copy(alpha = highlightAlpha) }
    val shadow = remember(shadowColor) { Shadow.Default.copy(color = shadowColor) }

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(blurRadius.toPx())
                    vibrancy()
                    if (lensProgress > 0.001f) {
                        lens(
                            refractionHeight = lensRefractionHeight.toPx(),
                            refractionAmount = lensRefractionAmount.toPx(),
                            depthEffect = true,
                            chromaticAberration = lensProgress > 0.35f,
                        )
                    }
                },
                highlight = { highlight },
                shadow = { shadow },
                innerShadow = { innerShadow },
                onDrawSurface = {
                    drawRect(surfaceColor)
                },
            )
            .padding(contentPadding),
    ) {
        content()
    }
}
