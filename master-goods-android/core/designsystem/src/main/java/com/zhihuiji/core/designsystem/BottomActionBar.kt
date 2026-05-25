package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomActionBar(
    primaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActions: List<@Composable () -> Unit> = emptyList(),
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = ZhihuijiColors.BorderLight, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            secondaryActions.forEach { action ->
                action()
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                primaryAction()
            }
        }
    }
}
