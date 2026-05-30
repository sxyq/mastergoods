package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        blurRadius = 16.dp,
        surfaceAlpha = 0.12f,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = ZhihuijiTypography.titleLarge,
                    color = ZhihuijiColors.TextPrimary,
                )
            },
            modifier = Modifier.height(56.dp),
            navigationIcon = {
                if (navigationIcon != null && onNavigationClick != null) {
                    IconButton(onClick = onNavigationClick) {
                        Icon(
                            imageVector = navigationIcon,
                            contentDescription = "返回",
                            tint = ZhihuijiColors.TextPrimary,
                        )
                    }
                }
            },
            actions = {
                actions()
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        )
    }
}
