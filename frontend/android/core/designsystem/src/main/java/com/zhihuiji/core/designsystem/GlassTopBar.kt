package com.zhihuiji.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 玻璃质感顶部栏（母版组件）
 *
 * 使用 LiquidGlassSurface 提供 glass 模糊效果（blurRadius = 24.dp），作为各页面顶栏的统一母版。
 * 各页面应通过 navigationIcon / actions 插槽注入自己的导航与操作按钮，避免自实现 LiquidGlassSurface + Row。
 *
 * @param modifier 修饰符
 * @param title 标题
 * @param subtitle 副标题
 * @param largeTitle 是否使用大标题模式
 * @param navigationIcon 导航图标
 * @param actions 右侧操作区
 * @param onNavigationClick 导航点击回调
 */
@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    largeTitle: Boolean = false,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = null
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 24.dp,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        surfaceColor = GlassSurfaceMedium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (largeTitle) 72.dp else 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (navigationIcon != null) {
                        navigationIcon()
                    } else if (onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(start = if (onNavigationClick != null || navigationIcon != null) 0.dp else 16.dp)
                    ) {
                        Text(
                            text = title,
                            style = if (largeTitle) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (actions != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        actions()
                    }
                }
            }
        }
    }
}
