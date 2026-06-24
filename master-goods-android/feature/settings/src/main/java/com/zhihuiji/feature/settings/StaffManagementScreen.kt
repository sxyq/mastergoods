package com.zhihuiji.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BackgroundGradientEnd
import com.zhihuiji.core.designsystem.DangerOutlineButton
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.SurfaceSoft
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.model.StoreStaffMember

@Composable
fun StaffManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: StaffManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            StaffManagementTopBar(
                onNavigateBack = onNavigateBack,
                hasNotice = !uiState.error.isNullOrBlank() || !uiState.success.isNullOrBlank(),
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
                StaffHeroCard(uiState = uiState)
            }

            item {
                StaffSearchCard(
                    keyword = uiState.searchKeyword,
                    isLoading = uiState.isLoading,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onRefresh = viewModel::refreshUsers,
                )
            }

            item {
                CreateStaffCard(
                    uiState = uiState,
                    onPhoneChange = viewModel::updateCreatePhone,
                    onNicknameChange = viewModel::updateCreateNickname,
                    onPasswordChange = viewModel::updateCreatePassword,
                    onRoleChange = viewModel::updateCreateRole,
                    onTitleChange = viewModel::updateCreateTitle,
                    onStatusChange = viewModel::updateCreateStatus,
                    onSubmit = viewModel::createUser,
                )
            }

            if (!uiState.error.isNullOrBlank()) {
                item {
                    InlineNoticeCard(
                        message = uiState.error.orEmpty(),
                        isError = true,
                        onClick = viewModel::clearMessage,
                    )
                }
            }

            if (!uiState.success.isNullOrBlank()) {
                item {
                    InlineNoticeCard(
                        message = uiState.success.orEmpty(),
                        isError = false,
                        onClick = viewModel::clearMessage,
                    )
                }
            }

            if (uiState.isLoading && uiState.staffMembers.isEmpty()) {
                item {
                    LoadingCard(message = "正在读取真实店员列表...")
                }
            } else if (uiState.staffMembers.isEmpty()) {
                item {
                    EmptyStaffCard()
                }
            } else {
                items(uiState.staffMembers, key = { it.id }) { member ->
                    StaffMemberCard(
                        member = member,
                        isSaving = uiState.isSaving,
                        onSave = viewModel::saveUser,
                        onToggleStatus = viewModel::toggleUserStatus,
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffManagementTopBar(
    onNavigateBack: () -> Unit,
    hasNotice: Boolean,
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回设置",
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Text(
                text = "店员与权限",
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
                    .padding(end = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(22.dp),
                )
                if (hasNotice) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BackgroundGradientEnd),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffHeroCard(
    uiState: StaffManagementUiState,
    modifier: Modifier = Modifier,
) {
    val summary = remember(uiState.staffMembers) {
        var enabledCount = 0
        var activeSessions = 0L
        for (member in uiState.staffMembers) {
            if (member.status == 1) {
                enabledCount += 1
            }
            activeSessions += member.activeSessions
        }
        StaffSummary(
            enabledCount = enabledCount,
            disabledCount = uiState.staffMembers.size - enabledCount,
            activeSessions = activeSessions,
        )
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceSoft.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = uiState.currentStore?.storeName ?: "真实店员管理",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = uiState.backendModeNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip(label = "真实账号", value = uiState.staffMembers.size.toString())
                SummaryChip(label = "启用", value = summary.enabledCount.toString())
                SummaryChip(label = "停用", value = summary.disabledCount.toString())
                SummaryChip(label = "活跃会话", value = summary.activeSessions.toString())
                uiState.currentStore?.let {
                    SummaryChip(label = "当前角色", value = roleLabel(it.role))
                    SummaryChip(label = "我的权限", value = it.permissions.size.toString())
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 10.dp,
    ) {
        Column(
            modifier = Modifier.width(92.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun StaffSearchCard(
    keyword: String,
    isLoading: Boolean,
    onKeywordChange: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "筛选真实店员", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            GlassTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                label = "手机号 / 昵称 / 岗位 / 角色",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryOutlineButton(
                text = if (isLoading) "刷新中..." else "刷新店员列表",
                enabled = !isLoading,
                onClick = onRefresh,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateStaffCard(
    uiState: StaffManagementUiState,
    onPhoneChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onStatusChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAddAlt1,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(text = "新增真实店员", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            }

            GlassTextField(
                value = uiState.createPhone,
                onValueChange = onPhoneChange,
                label = "手机号",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.createNickname,
                onValueChange = onNicknameChange,
                label = "昵称",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.createPassword,
                onValueChange = onPasswordChange,
                label = "初始密码",
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StoreRoleOptions.forEach { role ->
                    FilterStatusChip(
                        label = roleLabel(role),
                        selected = uiState.createRole == role,
                        onClick = { onRoleChange(role) },
                    )
                }
            }
            GlassTextField(
                value = uiState.createTitle,
                onValueChange = onTitleChange,
                label = "岗位",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterStatusChip(
                    label = "默认启用",
                    selected = uiState.createStatus == 1,
                    onClick = { onStatusChange(1) },
                )
                FilterStatusChip(
                    label = "创建即停用",
                    selected = uiState.createStatus == 0,
                    onClick = { onStatusChange(0) },
                )
            }
            PrimaryButton(
                text = if (uiState.isSaving) "提交中..." else "创建真实店员",
                enabled = !uiState.isSaving,
                onClick = onSubmit,
            )
        }
    }
}

@Composable
private fun FilterStatusChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) ZhihuijiPrimary.copy(alpha = 0.16f) else SurfaceSoft.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) ZhihuijiPrimary else TextSecondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffMemberCard(
    member: StoreStaffMember,
    isSaving: Boolean,
    onSave: (StoreStaffMember, String, String, String, String, Boolean) -> Unit,
    onToggleStatus: (StoreStaffMember, String?, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nickname by remember(member.id, member.nickname) { mutableStateOf(member.nickname) }
    var password by remember(member.id, member.updatedAt) { mutableStateOf("") }
    var role by remember(member.id, member.role) { mutableStateOf(member.role) }
    var title by remember(member.id, member.title) { mutableStateOf(member.title) }
    var keepSessions by remember(member.id) { mutableStateOf(true) }
    val ownerLocked = member.role == "OWNER"
    val updatedAtText = remember(member.updatedAt) {
        formatTimestamp(member.updatedAt)
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = nickname.ifBlank { member.nickname },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${member.phone} · ID ${member.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                StatusPill(
                    text = if (member.status == 1) "启用" else "停用",
                    status = if (member.status == 1) StatusType.NORMAL else StatusType.CANCELLED,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryTag(text = roleLabel(member.role))
                SummaryTag(text = title.ifBlank { member.title })
                SummaryTag(text = "权限 ${member.permissions.size}")
                SummaryTag(text = "活跃会话 ${member.activeSessions}")
                SummaryTag(text = "更新 $updatedAtText")
            }

            if (member.permissions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val visiblePermissionCount = minOf(6, member.permissions.size)
                    for (index in 0 until visiblePermissionCount) {
                        SummaryTag(text = member.permissions[index])
                    }
                    if (member.permissions.size > 6) {
                        SummaryTag(text = "+${member.permissions.size - 6}")
                    }
                }
            }

            GlassTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = "昵称",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = password,
                onValueChange = { password = it },
                label = "重置密码（留空则不改）",
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (ownerLocked) {
                    FilterStatusChip(
                        label = roleLabel("OWNER"),
                        selected = true,
                        onClick = {},
                    )
                } else {
                    StoreRoleOptions.forEach { option ->
                        FilterStatusChip(
                            label = roleLabel(option),
                            selected = role == option,
                            onClick = {
                                role = option
                                if (title.isBlank() || title == defaultTitleForRole(member.role)) {
                                    title = defaultTitleForRole(option)
                                }
                            },
                            modifier = Modifier,
                        )
                    }
                }
            }
            GlassTextField(
                value = title,
                onValueChange = { title = it },
                label = "岗位",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = keepSessions,
                    onCheckedChange = { keepSessions = it },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "重置密码后保留现有会话",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryOutlineButton(
                    modifier = Modifier.weight(1f),
                    text = if (isSaving) "保存中..." else "保存资料",
                    enabled = !isSaving,
                    onClick = { onSave(member, nickname, password, role, title, keepSessions) },
                )
                if (member.status == 1) {
                    DangerOutlineButton(
                        modifier = Modifier.weight(1f),
                        text = if (ownerLocked) "店长固定启用" else "停用店员",
                        enabled = !isSaving && !ownerLocked,
                        onClick = { onToggleStatus(member, nickname, role, title) },
                    )
                } else {
                    PrimaryButton(
                        modifier = Modifier.weight(1f),
                        text = "重新启用",
                        enabled = !isSaving,
                        onClick = { onToggleStatus(member, nickname, role, title) },
                    )
                }
            }
        }
    }
}

private val StoreRoleOptions = listOf("MANAGER", "SALES", "PURCHASING", "WAREHOUSE", "FINANCE", "ASSISTANT")

private data class StaffSummary(
    val enabledCount: Int,
    val disabledCount: Int,
    val activeSessions: Long,
)

private fun roleLabel(role: String): String =
    when (role) {
        "OWNER" -> "店长（总）"
        "MANAGER" -> "店长助理"
        "SALES" -> "销售员工"
        "PURCHASING" -> "采购员工"
        "WAREHOUSE" -> "仓库员工"
        "FINANCE" -> "财务员工"
        "ASSISTANT" -> "AI/只读助理"
        else -> role
    }

@Composable
private fun SummaryTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SurfaceSoft.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
    }
}

@Composable
private fun InlineNoticeCard(
    message: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        surfaceColor = if (isError) GlassSurfaceLow else GlassSurfaceHigh,
        contentPadding = 14.dp,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) BackgroundGradientEnd else ZhihuijiPrimary,
        )
    }
}

@Composable
private fun LoadingCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = ZhihuijiPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun EmptyStaffCard(
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = ZhihuijiPrimary,
                modifier = Modifier.size(24.dp),
            )
            Text(text = "当前没有真实店员账号", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                text = "可以先用上面的表单创建账号；当前能力对齐 `/v1/admin/users`，不生成本地演示成员。",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "--"
    val date = java.util.Date(timestamp)
    return java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(date)
}
