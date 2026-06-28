package com.zhihuiji.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BackgroundGradientEnd
import com.zhihuiji.core.designsystem.DividerLight
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright

private val roundedCardShape = RoundedCornerShape(12.dp)
private val accountActionPrimaryBrush = Brush.horizontalGradient(
    listOf(ZhihuijiPrimaryBright, ZhihuijiPrimary),
)
private val bottomScrimBrush = Brush.verticalGradient(
    colors = listOf(
        BackgroundGradientEnd.copy(alpha = 0.82f),
        BackgroundGradientEnd.copy(alpha = 0.96f),
        BackgroundGradientEnd,
    ),
)

@Composable
fun AccountEditScreen(
    accountId: Long?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) {
        if (accountId != null) viewModel.loadAccount(accountId)
    }
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.consumeSaveSuccess()
            onSaveSuccess()
        }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AccountEditTopBar(
                title = if (uiState.isEditMode) "编辑账户" else "添加账户",
                onNavigateBack = onNavigateBack,
            )
        },
        bottomBar = {
            AccountEditBottomBar(
                text = if (uiState.isSaving) "保存中..." else "保存",
                onClick = { viewModel.save(accountId) },
                enabled = uiState.canSave,
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ZhihuijiPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountFormSection(title = "基本信息") {
                    GlassTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = "账户名称",
                        placeholder = "如：微信零钱",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                null,
                                tint = TextSecondary,
                            )
                        },
                    )
                    GlassTextField(
                        value = uiState.code,
                        onValueChange = viewModel::updateCode,
                        label = "账户编码",
                        placeholder = "如：WECHAT-001",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Outlined.QrCodeScanner, null, tint = TextSecondary)
                        },
                    )
                    AccountTypeSelectField(
                        value = uiState.typeName,
                        onSelect = viewModel::selectType,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AccountFormSection(title = "余额与状态") {
                    GlassTextField(
                        value = uiState.balance,
                        onValueChange = viewModel::updateBalance,
                        label = if (uiState.isEditMode) "当前余额（不可编辑）" else "初始余额",
                        placeholder = "0.00",
                        singleLine = true,
                        enabled = !uiState.isEditMode,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AccountStatusToggle(
                        status = uiState.status,
                        onToggle = viewModel::toggleStatus,
                    )
                }

                AccountFormSection(title = "备注") {
                    GlassTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateNotes,
                        label = "备注说明（可选）",
                        placeholder = "补充账户描述...",
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.Notes,
                                null,
                                tint = TextSecondary,
                            )
                        },
                    )
                }

                uiState.error?.let { errorText ->
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Text(
                    text = "编辑模式下余额不可直接修改，请通过账户转账调整余额。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun AccountEditTopBar(
    title: String,
    onNavigateBack: () -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        surfaceColor = GlassSurfaceMedium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.align(Alignment.CenterEnd).size(48.dp))
        }
    }
}

@Composable
private fun AccountFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = roundedCardShape,
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                color = ZhihuijiPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun AccountTypeSelectField(
    value: String,
    onSelect: (AccountTypeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(roundedCardShape)
                .background(Color.White.copy(alpha = 0.80f))
                .border(1.dp, DividerLight, roundedCardShape)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Category,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                Text(
                    text = "账户类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = TextSecondary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ACCOUNT_TYPE_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (option.label == value) ZhihuijiPrimary else TextPrimary,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountStatusToggle(
    status: Int,
    onToggle: () -> Unit,
) {
    val active = status == 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "账户状态",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Text(
                text = if (active) "启用" else "停用",
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) ZhihuijiPrimary else MaterialTheme.colorScheme.error,
            )
        }
        TextButton(onClick = onToggle) {
            Text(if (active) "切换为停用" else "切换为启用")
        }
    }
}

@Composable
private fun AccountEditBottomBar(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = bottomScrimBrush)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(roundedCardShape)
                .background(
                    brush = accountActionPrimaryBrush,
                    alpha = if (enabled) 1f else 0.5f,
                )
                .clickable(enabled = enabled, onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = SurfaceWhite,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
