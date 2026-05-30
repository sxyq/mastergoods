package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeSelectedIndex = selectedIndex.coerceIn(0, tabs.indices.lastOrNull() ?: 0)
    LiquidSegmentedControl(
        modifier = modifier.fillMaxWidth(),
        items = tabs.mapIndexed { index, tab ->
            LiquidSegmentedItem(
                key = index,
                label = tab,
            )
        },
        selectedKey = safeSelectedIndex,
        onItemSelected = onTabSelected,
        style = LiquidSegmentedStyle.TextOnly,
    )
}
