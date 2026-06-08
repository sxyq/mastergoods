package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 玻璃质感主壳容器
 *
 * 性能优化：
 * 1. 背景使用静态渐变 Brush，不使用实时 blur
 * 2. 使用 Box + Column 减少不必要的嵌套层级
 * 3. 避免在 Scaffold 中使用动态颜色计算
 *
 * @param modifier 修饰符
 * @param topBar 顶部栏
 * @param bottomBar 底部栏
 * @param floatingActionButton 浮动操作按钮
 * @param content 内容区域
 */
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val backgroundBrush = rememberStaticGradientBrush()

    GlassBackdropLayer(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        AuroraAmbientLights()
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                topBar()
                Box(modifier = Modifier.weight(1f)) {
                    content(PaddingValues(horizontal = 0.dp, vertical = 0.dp))
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                bottomBar()
            }
        }
        floatingActionButton()
    }
}

private val staticGradientBrush by lazy {
    Brush.linearGradient(
        colors = listOf(
            BackgroundGradientStart,
            BackgroundGradientMid,
            BackgroundGradientEnd
        )
    )
}

@Composable
private fun rememberStaticGradientBrush(): Brush = staticGradientBrush

@Composable
private fun AuroraAmbientLights() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AuroraBlue.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.06f, size.height * 0.02f),
                radius = size.width * 0.62f
            ),
            radius = size.width * 0.62f,
            center = Offset(size.width * 0.06f, size.height * 0.02f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AuroraCyan.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * 0.28f, size.height * 0.24f),
                radius = size.width * 0.48f
            ),
            radius = size.width * 0.48f,
            center = Offset(size.width * 0.28f, size.height * 0.24f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AuroraIndigo.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 1.08f, size.height * 0.72f),
                radius = size.width * 0.82f
            ),
            radius = size.width * 0.82f,
            center = Offset(size.width * 1.08f, size.height * 0.72f)
        )
    }
}
