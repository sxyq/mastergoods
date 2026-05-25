package com.zhihuiji.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ZhihuijiColors.CardBackground,
        ),
        border = BorderStroke(0.6.dp, ZhihuijiColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        content = {
            content()
        },
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ZhihuijiColors.CardBackground,
        ),
        border = BorderStroke(0.6.dp, ZhihuijiColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        onClick = onClick,
        content = {
            content()
        },
    )
}
