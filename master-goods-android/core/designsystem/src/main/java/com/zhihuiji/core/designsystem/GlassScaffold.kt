package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
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
    topBarActions: @Composable (() -> Unit) = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = ZhihuijiColors.BackgroundGradientEnd,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = ZhihuijiColors.White.copy(alpha = 0.94f),
                    tonalElevation = 4.dp,
                ) {
                    destinations.forEach { dest ->
                        val selected = dest.route == selectedDestination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onNavigate(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.icon,
                                    contentDescription = dest.label,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = dest.label,
                                    style = ZhihuijiTypography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ZhihuijiColors.Primary,
                                selectedTextColor = ZhihuijiColors.Primary,
                                unselectedIconColor = ZhihuijiColors.TextTertiary,
                                unselectedTextColor = ZhihuijiColors.TextTertiary,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
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
            content(PaddingValues())
        }
    }
}
