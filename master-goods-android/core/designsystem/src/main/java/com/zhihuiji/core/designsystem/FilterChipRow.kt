package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun FilterChipRow(
    chips: List<String>,
    selectedIndex: Int,
    onChipSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEachIndexed { index, chip ->
            val selected = index == selectedIndex
            val bgColor = if (selected) ZhihuijiColors.Primary.copy(alpha = 0.1f) else ZhihuijiColors.SurfaceVariant
            val textColor = if (selected) ZhihuijiColors.Primary else ZhihuijiColors.TextSecondary
            Text(
                text = chip,
                style = ZhihuijiTypography.labelSmall,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onChipSelected(index) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}
