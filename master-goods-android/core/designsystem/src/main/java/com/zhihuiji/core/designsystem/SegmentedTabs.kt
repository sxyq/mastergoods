package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val bgColor = if (selected) ZhihuijiColors.PressedBlue else ZhihuijiColors.White.copy(alpha = 0.58f)
            val textColor = if (selected) ZhihuijiColors.Primary else ZhihuijiColors.TextSecondary
            Text(
                text = tab,
                style = ZhihuijiTypography.labelMedium,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            )
        }
    }
}
