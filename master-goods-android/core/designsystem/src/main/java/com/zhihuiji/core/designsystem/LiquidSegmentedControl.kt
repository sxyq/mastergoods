package com.zhihuiji.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.minimumInteractiveComponentSize
import kotlin.math.abs
import kotlin.math.roundToInt

data class LiquidSegmentedItem<T>(
    val key: T,
    val label: String,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null,
)

fun StringLiquidSegmentedItem(
    label: String,
    icon: ImageVector? = null,
    selectedIcon: ImageVector? = null,
) = LiquidSegmentedItem(key = label, label = label, icon = icon, selectedIcon = selectedIcon)

enum class LiquidSegmentedStyle {
    TextOnly,
    BottomBar,
}

@Composable
fun <T> LiquidSegmentedControl(
    items: List<LiquidSegmentedItem<T>>,
    selectedKey: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    style: LiquidSegmentedStyle = LiquidSegmentedStyle.TextOnly,
    height: Dp = if (style == LiquidSegmentedStyle.BottomBar) 58.dp else 36.dp,
    cornerRadius: Dp = if (style == LiquidSegmentedStyle.BottomBar) 30.dp else 18.dp,
    indicatorCornerRadius: Dp = if (style == LiquidSegmentedStyle.BottomBar) 24.dp else 12.dp,
    contentPadding: PaddingValues = if (style == LiquidSegmentedStyle.BottomBar) {
        PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    } else {
        PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    },
    surfaceAlpha: Float = if (style == LiquidSegmentedStyle.BottomBar) 0.18f else 0.10f,
    indicatorSurfaceAlpha: Float = if (style == LiquidSegmentedStyle.BottomBar) 0.24f else 0.22f,
) {
    if (items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { it.key == selectedKey }.takeIf { it >= 0 } ?: 0
    val dragState = rememberDampedSegmentedDragState(
        selectedIndex = selectedIndex,
        itemCount = items.size,
    )
    val density = LocalDensity.current

    LaunchedEffect(selectedIndex) {
        dragState.syncSelection(selectedIndex)
    }

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = cornerRadius,
        blurRadius = if (style == LiquidSegmentedStyle.BottomBar) 18.dp else 14.dp,
        surfaceAlpha = surfaceAlpha,
        contentPadding = contentPadding,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val itemWidth = maxWidth / items.size
            val itemWidthPx = with(density) { itemWidth.toPx() }
            val indicatorHorizontalInset = if (style == LiquidSegmentedStyle.BottomBar) 5.dp else 0.dp
            val indicatorWidth = itemWidth - indicatorHorizontalInset * 2
            val indicatorPosition = if (style == LiquidSegmentedStyle.BottomBar) {
                dragState.value
            } else {
                selectedIndex.toFloat()
            }
            val pressProgress = if (style == LiquidSegmentedStyle.BottomBar) {
                dragState.pressProgress.coerceIn(0f, 1f)
            } else {
                0f
            }
            val dragModifier = if (style == LiquidSegmentedStyle.BottomBar) {
                Modifier.draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragState.onDrag(delta, itemWidthPx)
                    },
                    startDragImmediately = false,
                    onDragStarted = {
                        dragState.setPressed(true)
                    },
                    onDragStopped = { velocity ->
                        dragState.onDragEnd(velocity, itemWidthPx) { index ->
                            onItemSelected(items[index].key)
                        }
                    },
                )
            } else {
                Modifier
            }

            LiquidGlassSurface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((itemWidth.toPx() * indicatorPosition) + indicatorHorizontalInset.toPx()).roundToInt(),
                            y = 0,
                        )
                    }
                    .width(indicatorWidth)
                    .fillMaxHeight(),
                cornerRadius = indicatorCornerRadius,
                blurRadius = if (style == LiquidSegmentedStyle.BottomBar) 15.dp else 12.dp,
                surfaceAlpha = indicatorSurfaceAlpha,
                lensProgress = pressProgress,
                highlightAlpha = 0.84f + pressProgress * 0.14f,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(indicatorCornerRadius))
                        .padding(1.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(dragModifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val proximity = (1f - abs(indicatorPosition - index)).coerceIn(0f, 1f)
                    LiquidSegmentedItemCell(
                        modifier = Modifier.weight(1f),
                        item = item,
                        selected = if (style == LiquidSegmentedStyle.BottomBar) {
                            proximity >= 0.5f
                        } else {
                            index == selectedIndex
                        },
                        emphasis = if (style == LiquidSegmentedStyle.BottomBar) proximity else if (index == selectedIndex) 1f else 0f,
                        style = style,
                        onClick = {
                            if (style == LiquidSegmentedStyle.BottomBar) {
                                dragState.onTap(index) { tappedIndex ->
                                    onItemSelected(items[tappedIndex].key)
                                }
                            } else {
                                onItemSelected(item.key)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun StringLiquidSegmentedControl(
    items: List<String>,
    selectedKey: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: LiquidSegmentedStyle = LiquidSegmentedStyle.TextOnly,
) {
    LiquidSegmentedControl(
        items = items.map { StringLiquidSegmentedItem(label = it) },
        selectedKey = selectedKey,
        onItemSelected = onItemSelected,
        modifier = modifier,
        style = style,
    )
}

@Composable
private fun <T> LiquidSegmentedItemCell(
    modifier: Modifier = Modifier,
    item: LiquidSegmentedItem<T>,
    selected: Boolean,
    emphasis: Float,
    style: LiquidSegmentedStyle,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconScale = animateFloatAsState(
        targetValue = 0.94f + 0.06f * emphasis.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220),
        label = "segmentedIconScale",
    )
    val labelAlpha = animateFloatAsState(
        targetValue = 0.74f + 0.26f * emphasis.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220),
        label = "segmentedLabelAlpha",
    )
    val contentColor = lerp(
        ZhihuijiColors.TextSecondary,
        ZhihuijiColors.Primary,
        emphasis.coerceIn(0f, 1f),
    )
    val displayIcon = if (selected) item.selectedIcon ?: item.icon else item.icon ?: item.selectedIcon

    if (style == LiquidSegmentedStyle.BottomBar) {
        Column(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (displayIcon != null) {
                Icon(
                    imageVector = displayIcon,
                    contentDescription = item.label,
                    modifier = Modifier.size(20.dp * iconScale.value),
                    tint = contentColor,
                )
            }
            Text(
                text = item.label,
                modifier = Modifier.padding(top = if (displayIcon != null) 4.dp else 0.dp),
                style = ZhihuijiTypography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor.copy(alpha = labelAlpha.value),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Box(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (displayIcon != null) {
                    Icon(
                        imageVector = displayIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(16.dp * iconScale.value),
                        tint = contentColor.copy(alpha = labelAlpha.value),
                    )
                }
                Text(
                    text = item.label,
                    style = ZhihuijiTypography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor.copy(alpha = labelAlpha.value),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
