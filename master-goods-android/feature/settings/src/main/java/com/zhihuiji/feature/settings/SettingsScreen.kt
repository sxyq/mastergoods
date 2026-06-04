package com.zhihuiji.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerOutlineButton
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogout()
    }

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            GlassTopBar(
                title = "设置",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SettingsHeroHeader(
                        icon = Icons.Default.AccountCircle,
                        nickname = uiState.userProfile?.nickname ?: "-",
                        phone = uiState.userProfile?.phone ?: "-",
                    )
                    SettingsValueRow(label = "手机号", value = uiState.userProfile?.phone ?: "-")
                    SettingsValueRow(label = "昵称", value = uiState.userProfile?.nickname ?: "-")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SettingsSectionTitle(
                        icon = Icons.Default.Info,
                        title = "关于应用",
                    )
                    Text(
                        text = "智慧记",
                        style = ZhihuijiTypography.headlineLarge,
                        color = ZhihuijiColors.TextPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            DangerOutlineButton(
                text = "退出登录",
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsHeroHeader(
    icon: ImageVector,
    nickname: String,
    phone: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ZhihuijiColors.Primary,
            modifier = Modifier.size(28.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                nickname,
                style = ZhihuijiTypography.displayMedium,
                color = ZhihuijiColors.TextPrimary,
            )
            Text(
                phone,
                style = ZhihuijiTypography.titleLarge,
                color = ZhihuijiColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    icon: ImageVector,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ZhihuijiColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            title,
            style = ZhihuijiTypography.titleMedium,
            color = ZhihuijiColors.TextPrimary,
        )
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = ZhihuijiTypography.labelMedium,
            color = ZhihuijiColors.TextTertiary,
        )
        Text(
            text = value,
            style = ZhihuijiTypography.headlineLarge,
            color = ZhihuijiColors.TextPrimary,
        )
    }
}
