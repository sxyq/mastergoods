package com.zhihuiji.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.designsystem.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncStatus = uiState.syncHealth?.status.orEmpty()
    val isSyncHealthy = syncStatus.equals("UP", ignoreCase = true) || syncStatus.equals("OK", ignoreCase = true)
    var baseUrlDraft by remember(uiState.baseUrl) { mutableStateOf(uiState.baseUrl) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogout()
    }

    Column(modifier = Modifier.fillMaxSize().glassBackground().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        GlassTopBar(title = "设置", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("账号与安全", style = ZhihuijiTypography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("手机号", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                    Text(uiState.userProfile?.phone ?: "-", style = ZhihuijiTypography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("昵称", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                    Text(uiState.userProfile?.nickname ?: "-", style = ZhihuijiTypography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("服务端设置", style = ZhihuijiTypography.titleMedium)
                Text("服务器地址", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                if (uiState.isBaseUrlEditable) {
                    OutlinedTextField(
                        value = baseUrlDraft,
                        onValueChange = { baseUrlDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = ZhihuijiTypography.bodySmall,
                        singleLine = true,
                    )
                    SecondaryOutlineButton(
                        text = "保存地址",
                        onClick = { viewModel.saveBaseUrl(baseUrlDraft) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(uiState.baseUrl, style = ZhihuijiTypography.bodySmall)
                    Text(
                        "正式版已锁定受控服务器地址，不能手动修改。",
                        style = ZhihuijiTypography.bodySmall,
                        color = ZhihuijiColors.TextSecondary,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("客户端ID", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                    Text(uiState.clientId.take(8) + "...", style = ZhihuijiTypography.bodySmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("同步状态", style = ZhihuijiTypography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("健康状态", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                    StatusPill(text = if (syncStatus.isBlank()) "未知" else syncStatus, tone = if (isSyncHealthy) PillTone.SUCCESS else PillTone.DANGER)
                }
                PrimaryGradientButton(text = if(uiState.isSyncing) "同步中..." else "手动同步", onClick = { viewModel.runManualSync() }, enabled = !uiState.isSyncing, modifier = Modifier.fillMaxWidth())
                uiState.error?.let { error ->
                    Text(error.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DangerOutlineButton(text = "退出登录", onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
    }
}
