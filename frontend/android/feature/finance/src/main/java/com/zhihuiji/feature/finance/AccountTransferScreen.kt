package com.zhihuiji.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright
import com.zhihuiji.core.designsystem.roundedCardShape

private val transferActionBrush = Brush.horizontalGradient(
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
fun AccountTransferScreen(
    onNavigateBack: () -> Unit,
    onTransferSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountTransferViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.consumeSaveSuccess()
            onTransferSuccess()
        }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AccountTransferTopBar(onNavigateBack = onNavigateBack) },
        bottomBar = {
            AccountTransferBottomBar(
                text = if (uiState.isSaving) "提交中..." else "确认转账",
                onClick = viewModel::submit,
                enabled = uiState.canSubmit,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TransferAmountHeroCard(
                amount = uiState.amount,
                onAmountChange = viewModel::updateAmount,
            )

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                shape = roundedCardShape,
                contentPadding = 16.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "转账账户",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    AccountDropdownField(
                        label = "转出账户",
                        value = uiState.fromAccountLabel,
                        options = uiState.accounts,
                        onSelect = viewModel::selectFromAccount,
                    )
                    AccountDropdownField(
                        label = "转入账户",
                        value = uiState.toAccountLabel,
                        options = uiState.accounts,
                        onSelect = viewModel::selectToAccount,
                    )
                    GlassTextField(
                        value = uiState.fee,
                        onValueChange = viewModel::updateFee,
                        label = "手续费（可选）",
                        placeholder = "0.00",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GlassTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateNotes,
                        label = "备注说明（可选）",
                        placeholder = "添加转账说明...",
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
            }

            if (uiState.accounts.isEmpty() && !uiState.isLoading) {
                Text(
                    text = "暂无可用账户，请先在账户管理中添加启用的账户。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
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
                text = "转出与转入账户不能相同；提交后余额将由后端同步调整。",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun AccountTransferTopBar(onNavigateBack: () -> Unit) {
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
                text = "账户转账",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.align(Alignment.CenterEnd).size(48.dp))
        }
    }
}

@Composable
private fun TransferAmountHeroCard(
    amount: String,
    onAmountChange: (String) -> Unit,
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        shape = roundedCardShape,
        contentPadding = 16.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "转账金额 (¥)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "¥",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(end = 4.dp),
                )
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.width(220.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (amount.isBlank()) {
                                Text(
                                    text = "0.00",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountDropdownField(
    label: String,
    value: String,
    options: List<AccountSelectOption>,
    onSelect: (AccountSelectOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val empty = options.isEmpty()
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(roundedCardShape)
                    .background(Color.White.copy(alpha = if (empty) 0.52f else 0.80f))
                    .border(1.dp, DividerLight, roundedCardShape)
                    .clickable(enabled = !empty) { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = value.ifBlank { if (empty) "暂无可用账户" else "请选择账户" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isBlank()) TextTertiary else TextPrimary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
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
private fun AccountTransferBottomBar(
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
                    brush = transferActionBrush,
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
