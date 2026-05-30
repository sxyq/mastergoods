package com.zhihuiji.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Stable
class DampedSegmentedDragState internal constructor(
    initialIndex: Int,
    private val itemCount: Int,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private const val VELOCITY_PREDICTION_FACTOR = 0.16f
    }
    private val indicator = Animatable(initialIndex.toFloat())
    private val pressPulse = Animatable(0f)
    private var indicatorJob: Job? = null
    private var pulseJob: Job? = null

    var selectedIndex by mutableIntStateOf(initialIndex)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var dragVelocityItemsPerSecond by mutableFloatStateOf(0f)
        private set

    val value: Float get() = indicator.value
    val pressProgress: Float get() = pressPulse.value

    fun syncSelection(index: Int) {
        if (index == selectedIndex && !isDragging) return
        selectedIndex = index.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        if (!isDragging) {
            indicatorJob?.cancel()
            indicatorJob = coroutineScope.launch {
                indicator.animateTo(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
        }
    }

    fun setPressed(pressed: Boolean) {
        pulseJob?.cancel()
        pulseJob = coroutineScope.launch {
            pressPulse.animateTo(
                targetValue = if (pressed || isDragging) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    fun onTap(targetIndex: Int, onSelected: (Int) -> Unit) {
        val bounded = targetIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        selectedIndex = bounded
        indicatorJob?.cancel()
        indicatorJob = coroutineScope.launch {
            pressPulse.snapTo(0f)
            pressPulse.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            indicator.animateTo(
                targetValue = bounded.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            pressPulse.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = Spring.StiffnessLow,
                ),
            )
            onSelected(bounded)
        }
    }

    fun onDrag(deltaX: Float, itemWidthPx: Float) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        isDragging = true
        val current = indicator.value
        val overscroll = current < 0f || current > itemCount - 1f
        val resistance = if (overscroll) 0.28f else 0.96f
        val deltaItems = (deltaX / itemWidthPx) * resistance
        dragVelocityItemsPerSecond = deltaItems
        indicatorJob?.cancel()
        indicatorJob = coroutineScope.launch {
            indicator.snapTo(
                (current + deltaItems).coerceIn(-0.35f, (itemCount - 1).toFloat() + 0.35f),
            )
            pressPulse.snapTo(1f)
        }
    }

    fun onDragEnd(velocityPxPerSecond: Float, itemWidthPx: Float, onSelected: (Int) -> Unit) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        isDragging = false
        val velocityItems = velocityPxPerSecond / itemWidthPx
        dragVelocityItemsPerSecond = velocityItems
        val current = indicator.value
        var target = (current + velocityItems * VELOCITY_PREDICTION_FACTOR).roundToInt()
        val base = current.roundToInt()
        val maxStep = 1
        if (abs(target - base) > maxStep) {
            target = base + (target - base).sign.toInt() * maxStep
        }
        val bounded = target.coerceIn(0, itemCount - 1)
        selectedIndex = bounded
        indicatorJob?.cancel()
        indicatorJob = coroutineScope.launch {
            indicator.animateTo(
                targetValue = bounded.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.74f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            pressPulse.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        onSelected(bounded)
    }
}

@Composable
fun rememberDampedSegmentedDragState(
    selectedIndex: Int,
    itemCount: Int,
): DampedSegmentedDragState {
    val coroutineScope = rememberCoroutineScope()
    return remember(itemCount, coroutineScope) {
        DampedSegmentedDragState(
            initialIndex = selectedIndex,
            itemCount = itemCount,
            coroutineScope = coroutineScope,
        )
    }
}
