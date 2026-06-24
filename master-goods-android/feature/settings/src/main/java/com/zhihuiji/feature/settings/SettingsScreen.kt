package com.zhihuiji.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.StatusBlueLight
import com.zhihuiji.core.designsystem.SurfaceSoft
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    canManageUsers: Boolean = true,
    canManageDatabase: Boolean = true,
    onNavigateBack: () -> Unit = {},
    onNavigateToStaffManagement: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accountTrailing = remember(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) "真实账号" else "未登录"
    }
    val securityTrailing = remember(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) "会话有效" else "待登录"
    }
    val staffTrailing = remember(uiState.isLoggedIn, canManageUsers) {
        when {
            !uiState.isLoggedIn -> "待登录"
            canManageUsers -> "真实接口"
            else -> "无权限"
        }
    }
    val syncTrailing = remember(uiState.syncBadge, uiState.isSyncing, canManageDatabase) {
        if (canManageDatabase) {
            uiState.syncBadge.ifBlank { if (uiState.isSyncing) "同步中" else "实时同步" }
        } else {
            "无权限"
        }
    }
    val importTrailing = remember(uiState.importStatus, canManageDatabase) {
        if (canManageDatabase) uiState.importStatus.shortSettingStatus() else "无权限"
    }
    val serverTrailing = remember(uiState.serverUrl) {
        if (uiState.serverUrl.isBlank()) "未读取" else "当前"
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsTopBar(
                onNavigateBack = onNavigateBack,
                hasNotification = !uiState.error.isNullOrBlank(),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        ) {
            item {
                AccountHeaderCard(uiState = uiState)
            }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "个人资料",
                        trailing = accountTrailing,
                        onClick = viewModel::loadAccount,
                    )
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = "账号安全",
                        trailing = securityTrailing,
                        onClick = viewModel::loadAccount,
                    )
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Default.SupervisorAccount,
                        title = "店员与权限",
                        trailing = staffTrailing,
                        enabled = uiState.isLoggedIn && canManageUsers,
                        onClick = onNavigateToStaffManagement,
                    )
                }
            }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.CloudSync,
                        title = "同步设置",
                        trailing = syncTrailing,
                        isLoading = uiState.isSyncing,
                        enabled = uiState.isLoggedIn && canManageDatabase,
                        onClick = viewModel::runSync,
                    )
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Default.ImportExport,
                        title = "数据导入导出",
                        trailing = importTrailing,
                        enabled = uiState.isLoggedIn && canManageDatabase,
                        onClick = viewModel::refreshSyncStatus,
                    )
                }
            }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.Computer,
                        title = "服务器地址",
                        trailing = serverTrailing,
                        onClick = viewModel::refreshSyncStatus,
                    )
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "关于我们",
                        trailing = "v1.0.0",
                    )
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                item {
                    ErrorNotice(message = uiState.error.orEmpty(), onClick = viewModel::clearError)
                }
            }

            item {
                LogoutButton(
                    enabled = !uiState.isLoading,
                    onClick = viewModel::logout,
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onNavigateBack: () -> Unit,
    hasNotification: Boolean,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 20.dp,
        shape = RoundedCornerShape(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "返回主界面",
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Text(
                text = "智慧记",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "状态提醒",
                    tint = ZhihuijiPrimary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp),
                )
                if (hasNotification) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DangerRed),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeaderCard(
    uiState: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    val avatar = remember(uiState.userName) { avatarText(uiState.userName) }
    val subtitle = remember(uiState.isLoggedIn, uiState.clientId, uiState.accountSubtitle) {
        uiState.storefrontSubtitle
    }
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(StatusBlueLight.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = avatar,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZhihuijiPrimary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = uiState.userName.ifBlank { "未登录" },
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailing: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else null,
        shape = RoundedCornerShape(0.dp),
        surfaceColor = Color.Transparent,
        contentPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(StatusBlueLight.copy(alpha = if (enabled) 0.52f else 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) ZhihuijiPrimary else TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) TextPrimary else TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ZhihuijiPrimary,
                    strokeWidth = 2.dp,
                )
            } else if (!trailing.isNullOrBlank()) {
                Text(
                    text = trailing,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary.copy(alpha = if (enabled) 1f else 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(SurfaceSoft.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary.copy(alpha = if (enabled && onClick != null) 1f else 0.34f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        thickness = 0.5.dp,
        color = SurfaceSoft.copy(alpha = 0.8f),
    )
}

@Composable
private fun ErrorNotice(
    message: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 14.dp,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = DangerRed,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LogoutButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        onClick = if (enabled) onClick else null,
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (enabled) "退出登录" else "处理中",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = DangerRed,
            )
        }
    }
}

private fun avatarText(name: String): String =
    name.trim().take(1).ifBlank { "未" }

private val SettingsUiState.storefrontSubtitle: String
    get() = when {
        !isLoggedIn -> "登录后显示真实门店资料"
        clientId.isNotBlank() && clientId != "未生成" -> "客户端 ${clientId.take(8)}"
        else -> accountSubtitle
    }

private fun String.shortSettingStatus(): String = when {
    contains("暂无真实导入任务") -> "待接入"
    contains("失败") || contains("异常") -> "异常"
    startsWith("最近任务") -> "已记录"
    isBlank() -> "待查询"
    else -> "真实接口"
}
