package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 筛选芯片数据
 *
 * @param id 唯一标识
 * @param label 显示文字
 * @param selected 是否选中
 */
data class FilterChip(
    val id: String,
    val label: String,
    val selected: Boolean = false
)

/**
 * 横向胶囊筛选组件
 *
 * @param modifier 修饰符
 * @param chips 筛选芯片列表
 * @param onChipClick 芯片点击回调
 * @param onChipRemove 芯片移除回调（可选）
 */
@Composable
fun FilterChipRow(
    modifier: Modifier = Modifier,
    chips: List<FilterChip>,
    onChipClick: (FilterChip) -> Unit = {},
    onChipRemove: ((FilterChip) -> Unit)? = null
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(chips, key = { it.id }) { chip ->
            FilterChipItem(
                chip = chip,
                onClick = { onChipClick(chip) },
                onRemove = if (onChipRemove != null) {
                    { onChipRemove(chip) }
                } else null
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    chip: FilterChip,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val backgroundColor = if (chip.selected) ZhihuijiPrimary.copy(alpha = 0.12f) else SurfaceGray
    val textColor = if (chip.selected) ZhihuijiPrimary else TextSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = chip.label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onRemove != null) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除",
                tint = textColor,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove
                    )
            )
        }
    }
}
