package com.zhihuiji.core.designsystem

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

private val SegmentedContainerShape = RoundedCornerShape(28.dp)
private val SegmentedIndicatorShape = RoundedCornerShape(25.dp)
private val SegmentedContainerHeight = 58.dp
private val SegmentedIndicatorHeight = 56.dp
private val SegmentedContentPadding = 3.dp

/**
 * BiliPai-style liquid segmented tabs.
 *
 * The structure mirrors BiliPai's settings segmented control: a continuous rail, a single moving
 * indicator, hidden backdrop capture, and a separate visible label layer with ripple disabled.
 */
@Composable
fun SegmentedTabs(
    modifier: Modifier = Modifier,
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop? = null
) {
    if (tabs.isEmpty()) return

    val density = LocalDensity.current
    val isDarkTheme = isSystemInDarkTheme()
    val itemCount = tabs.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)

    if (backdrop != null) {
        SegmentedTabsDynamic(
            modifier = modifier,
            tabs = tabs,
            safeSelectedIndex = safeSelectedIndex,
            selectedIndex = selectedIndex,
            onTabSelected = onTabSelected,
            externalBackdrop = backdrop,
            density = density,
            isDarkTheme = isDarkTheme,
            itemCount = itemCount,
        )
    } else {
        SegmentedTabsStatic(
            modifier = modifier,
            tabs = tabs,
            safeSelectedIndex = safeSelectedIndex,
            onTabSelected = onTabSelected,
            density = density,
            isDarkTheme = isDarkTheme,
            itemCount = itemCount,
        )
    }
}

@Composable
private fun SegmentedTabsStatic(
    modifier: Modifier,
    tabs: List<String>,
    safeSelectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    density: androidx.compose.ui.unit.Density,
    isDarkTheme: Boolean,
    itemCount: Int,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SegmentedContainerHeight)
    ) {
        val slotWidth = (maxWidth - SegmentedContentPadding * 2) / itemCount
        val indicatorTargetOffset = SegmentedContentPadding + slotWidth * safeSelectedIndex
        val indicatorOffsetPx by animateFloatAsState(
            targetValue = with(density) { indicatorTargetOffset.toPx() },
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = 320f,
                visibilityThreshold = 0.5f
            ),
            label = "liquid_segment_indicator_offset"
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .liquidSegmentedRailChrome(
                    backdrop = null,
                    shape = SegmentedContainerShape,
                    isDarkTheme = isDarkTheme
                )
        )

        Box(
            modifier = Modifier
                .width(slotWidth)
                .height(SegmentedIndicatorHeight)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = indicatorOffsetPx
                }
                .liquidSegmentedIndicatorChrome(
                    backdrop = null,
                    shape = SegmentedIndicatorShape,
                    isDarkTheme = isDarkTheme
                )
        )

        SegmentLabelsLayer(
            modifier = Modifier
                .matchParentSize()
                .padding(SegmentedContentPadding),
            tabs = tabs,
            selectedIndex = safeSelectedIndex,
            interactive = true,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun SegmentedTabsDynamic(
    modifier: Modifier,
    tabs: List<String>,
    safeSelectedIndex: Int,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    externalBackdrop: Backdrop,
    density: androidx.compose.ui.unit.Density,
    isDarkTheme: Boolean,
    itemCount: Int,
) {
    val contentBackdrop = rememberLayerBackdrop()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SegmentedContainerHeight)
    ) {
        val slotWidth = (maxWidth - SegmentedContentPadding * 2) / itemCount
        val indicatorTargetOffset = SegmentedContentPadding + slotWidth * safeSelectedIndex
        val indicatorOffsetPx by animateFloatAsState(
            targetValue = with(density) { indicatorTargetOffset.toPx() },
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = 320f,
                visibilityThreshold = 0.5f
            ),
            label = "liquid_segment_indicator_offset"
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .liquidSegmentedRailChrome(
                    backdrop = externalBackdrop,
                    shape = SegmentedContainerShape,
                    isDarkTheme = isDarkTheme
                )
        )

        SegmentLabelsLayer(
            modifier = Modifier
                .matchParentSize()
                .alpha(0f)
                .layerBackdrop(contentBackdrop)
                .padding(SegmentedContentPadding),
            tabs = tabs,
            selectedIndex = safeSelectedIndex,
            interactive = false,
            onTabSelected = onTabSelected
        )

        Box(
            modifier = Modifier
                .width(slotWidth)
                .height(SegmentedIndicatorHeight)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = indicatorOffsetPx
                }
                .liquidSegmentedIndicatorChrome(
                    backdrop = contentBackdrop,
                    shape = SegmentedIndicatorShape,
                    isDarkTheme = isDarkTheme
                )
        )

        SegmentLabelsLayer(
            modifier = Modifier
                .matchParentSize()
                .padding(SegmentedContentPadding),
            tabs = tabs,
            selectedIndex = safeSelectedIndex,
            interactive = true,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun SegmentLabelsLayer(
    modifier: Modifier,
    tabs: List<String>,
    selectedIndex: Int,
    interactive: Boolean,
    onTabSelected: (Int) -> Unit
) {
    Row(modifier = modifier.fillMaxSize()) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (isSelected) ZhihuijiPrimary else TextSecondary,
                label = "liquid_segment_text_$index"
            )
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(SegmentedIndicatorShape)
                    .then(
                        if (interactive) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onTabSelected(index) }
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun Modifier.liquidSegmentedRailChrome(
    backdrop: Backdrop?,
    shape: RoundedCornerShape,
    isDarkTheme: Boolean
): Modifier {
    val fallback = this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = GlassShadow.copy(alpha = 0.14f),
            spotColor = GlassShadow.copy(alpha = 0.18f)
        )
        .clip(shape)
        .background(
            brush = liquidSegmentedRailBrush(isDarkTheme),
            shape = shape
        )
        .border(
            width = 0.5.dp,
            color = Color.White.copy(alpha = if (isDarkTheme) 0.18f else 0.54f),
            shape = shape
        )

    return if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = GlassShadow.copy(alpha = 0.14f),
                spotColor = GlassShadow.copy(alpha = 0.18f)
            )
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(24.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.86f) },
                shadow = { Shadow.Default.copy(color = GlassShadow.copy(alpha = 0.14f)) },
                innerShadow = {
                    InnerShadow(
                        radius = 8.dp,
                        alpha = 0.18f,
                        color = Color.White.copy(alpha = 0.18f)
                    )
                },
                onDrawSurface = {
                    drawRect(brush = liquidSegmentedRailBrush(isDarkTheme))
                }
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = if (isDarkTheme) 0.18f else 0.54f),
                shape = shape
            )
    } else {
        fallback
    }
}

private fun Modifier.liquidSegmentedIndicatorChrome(
    backdrop: Backdrop?,
    shape: RoundedCornerShape,
    isDarkTheme: Boolean
): Modifier {
    val staticChrome = this
        .clip(shape)
        .background(liquidSegmentedIndicatorBrush(isDarkTheme), shape)
        .drawIndicatorSheen(isDarkTheme)
        .shadow(
            elevation = 2.dp,
            shape = shape,
            clip = false,
            ambientColor = GlassShadow.copy(alpha = if (isDarkTheme) 0.28f else 0.16f),
            spotColor = GlassShadow.copy(alpha = if (isDarkTheme) 0.28f else 0.16f)
        )

    val chrome = if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(22.dp.toPx())
                },
                highlight = {
                    Highlight.Default.copy(alpha = 0.98f)
                },
                shadow = {
                    Shadow(
                        color = GlassShadow.copy(alpha = if (isDarkTheme) 0.28f else 0.16f),
                        alpha = 0.42f
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 9.dp,
                        alpha = 0.50f,
                        color = Color.White.copy(alpha = if (isDarkTheme) 0.20f else 0.38f)
                    )
                },
                onDrawSurface = {
                    drawRect(brush = liquidSegmentedIndicatorBrush(isDarkTheme))
                    drawRect(
                        color = Color.White.copy(alpha = if (isDarkTheme) 0.06f else 0.18f)
                    )
                }
            )
    } else {
        staticChrome
    }
    return chrome.border(
        width = 0.5.dp,
        color = Color.White.copy(alpha = if (isDarkTheme) 0.18f else 0.42f),
        shape = shape
    )
}

private fun Modifier.drawIndicatorSheen(isDarkTheme: Boolean): Modifier =
    this.then(
        Modifier.background(
            color = Color.White.copy(alpha = if (isDarkTheme) 0.06f else 0.18f)
        )
    )

private fun liquidSegmentedRailBrush(isDarkTheme: Boolean): Brush =
    if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E2634).copy(alpha = 0.58f),
                Color(0xFF111827).copy(alpha = 0.52f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEAF2FF).copy(alpha = 0.58f),
                Color(0xFFF7FAFF).copy(alpha = 0.42f)
            )
        )
    }

private fun liquidSegmentedIndicatorBrush(isDarkTheme: Boolean): Brush =
    Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDarkTheme) 0.14f else 0.78f),
            Color(0xFFE8ECF4).copy(alpha = if (isDarkTheme) 0.18f else 0.56f),
            Color(0xFFC9CED8).copy(alpha = if (isDarkTheme) 0.20f else 0.34f)
        )
    )
