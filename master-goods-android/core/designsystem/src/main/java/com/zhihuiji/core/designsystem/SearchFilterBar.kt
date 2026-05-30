package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索",
    modifier: Modifier = Modifier,
    showFilter: Boolean = true,
    onFilterClick: (() -> Unit)? = null,
    filterIcon: ImageVector? = null,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        cornerRadius = 18.dp,
        blurRadius = 14.dp,
        surfaceAlpha = 0.12f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = ZhihuijiTypography.bodyMedium.copy(color = ZhihuijiColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (query.isBlank()) {
                            Text(text = placeholder, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextTertiary)
                        }
                        innerTextField()
                    },
                )
            }
            if (showFilter && onFilterClick != null && filterIcon != null) {
                IconButton(onClick = onFilterClick, modifier = Modifier.size(40.dp)) {
                    Icon(imageVector = filterIcon, contentDescription = "筛选", tint = ZhihuijiColors.TextSecondary)
                }
            }
        }
    }
}
