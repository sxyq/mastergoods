package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * 搜索栏 + 筛选按钮组合组件
 *
 * @param modifier 修饰符
 * @param searchHint 搜索提示文字
 * @param onSearchClick 搜索点击回调
 * @param onFilterClick 筛选点击回调
 * @param filterCount 筛选条件数量（显示角标）
 */
@Composable
fun SearchFilterBar(
    modifier: Modifier = Modifier,
    searchHint: String = "搜索",
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onAddClick: (() -> Unit)? = null,
    filterCount: Int = 0
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurfaceHigh)
                .then(Modifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSearchClick
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "搜索",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = searchHint,
                style = MaterialTheme.typography.bodyMedium,
                color = TextQuaternary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            LiquidGlassSurface(
                modifier = Modifier.size(44.dp),
                blurRadius = 16.dp,
                shape = RoundedCornerShape(12.dp),
                surfaceColor = GlassSurfaceHigh
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "筛选",
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (filterCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(16.dp)
                        .background(DangerRed, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (filterCount > 99) "99+" else filterCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SurfaceWhite
                    )
                }
            }
        }
        if (onAddClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(ZhihuijiPrimaryBright, ZhihuijiPrimary)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "新增",
                    tint = SurfaceWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
