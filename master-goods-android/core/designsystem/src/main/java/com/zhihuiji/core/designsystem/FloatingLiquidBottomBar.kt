package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FloatingLiquidBottomBar(
    selectedDestination: String,
    destinations: List<BottomBarDestination>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return

    LiquidSegmentedControl(
        modifier = modifier.fillMaxWidth(),
        items = destinations.map { destination ->
            LiquidSegmentedItem(
                key = destination.route,
                label = destination.label,
                icon = destination.icon,
                selectedIcon = destination.selectedIcon,
            )
        },
        selectedKey = selectedDestination,
        onItemSelected = onNavigate,
        style = LiquidSegmentedStyle.BottomBar,
    )
}
