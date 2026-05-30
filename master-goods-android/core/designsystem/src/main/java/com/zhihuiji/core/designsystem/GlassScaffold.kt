package com.zhihuiji.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomBarDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
fun GlassScaffold(
    title: String = "",
    selectedDestination: String,
    destinations: List<BottomBarDestination>,
    onNavigate: (String) -> Unit,
    showBottomBar: Boolean = true,
    isBottomBarVisible: Boolean = true,
    setBottomBarVisible: (Boolean) -> Unit = {},
    topBarActions: @Composable (() -> Unit) = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    CompositionLocalProvider(
        LocalBottomBarVisible provides isBottomBarVisible,
        LocalSetBottomBarVisible provides setBottomBarVisible,
    ) {
        Scaffold(
            containerColor = ZhihuijiColors.BackgroundGradientEnd,
            bottomBar = {
                if (showBottomBar) {
                    AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                    ) {
                        FloatingLiquidBottomBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            selectedDestination = selectedDestination,
                            destinations = destinations,
                            onNavigate = onNavigate,
                        )
                    }
                }
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackground()
                    .padding(paddingValues)
            ) {
                content(paddingValues)
            }
        }
    }
}
