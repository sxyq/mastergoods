package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.designsystem.EmptyState
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SecondaryOutlineButton

/**
 * 重写清场后的临时首页占位：仅保留助手与设置入口，
 * 新的信息架构在后续阶段接入。
 */
@Composable
fun RewriteHomeScreen(
    onNavigateToAgent: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    EmptyState(
        icon = Icons.Outlined.Construction,
        title = "业务系统重构中",
        subtitle = "商品、单据与报表能力正在重写，当前可使用智能助手与设置。",
        action = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(text = "进入助手", onClick = onNavigateToAgent)
                SecondaryOutlineButton(text = "设置", onClick = onNavigateToSettings)
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
    )
}
