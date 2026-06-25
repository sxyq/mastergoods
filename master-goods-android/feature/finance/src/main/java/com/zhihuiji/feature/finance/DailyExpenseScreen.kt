package com.zhihuiji.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Restaurant
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.BackgroundGradientEnd
import com.zhihuiji.core.designsystem.DividerLight
import com.zhihuiji.core.designsystem.GlassBorder
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceLow
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

private data class ExpenseCategoryOption(
    val label: String,
    val icon: ImageVector,
)

private val expenseCategories = listOf(
    ExpenseCategoryOption("房租", Icons.Outlined.RealEstateAgent),
    ExpenseCategoryOption("水电", Icons.Outlined.ElectricBolt),
    ExpenseCategoryOption("工资", Icons.Outlined.Payments),
    ExpenseCategoryOption("办公", Icons.Outlined.Inventory),
    ExpenseCategoryOption("营销", Icons.Outlined.Campaign),
    ExpenseCategoryOption("物流", Icons.Outlined.LocalShipping),
    ExpenseCategoryOption("餐饮", Icons.Outlined.Restaurant),
    ExpenseCategoryOption("其他", Icons.Outlined.MoreHoriz),
)
private val paymentMethods = listOf(
    StatusLabels.Codes.METHOD_CASH to "现金",
    StatusLabels.Codes.METHOD_WECHAT to "微信",
    StatusLabels.Codes.METHOD_ALIPAY to "支付宝",
    StatusLabels.Codes.METHOD_BANK to "银行卡",
    StatusLabels.Codes.METHOD_OTHER to "其他",
)
private val expenseCategoryRows = expenseCategories.chunked(4)
private val paymentMethodLabels = paymentMethods.toMap()
private val roundedCardShape = RoundedCornerShape(12.dp)
private val bottomScrimBrush = Brush.verticalGradient(
    colors = listOf(
        BackgroundGradientEnd.copy(alpha = 0.82f),
        BackgroundGradientEnd.copy(alpha = 0.96f),
        BackgroundGradientEnd,
    ),
)
private val primaryActionBrush = Brush.horizontalGradient(
    colors = listOf(ZhihuijiPrimaryBright, ZhihuijiPrimary),
)

@Composable
fun DailyExpenseScreen(
    onNavigateBack: () -> Unit,
    onExpenseCreated: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyExpenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.createdRecordId) {
        val recordId = uiState.createdRecordId
        if (recordId != null) {
            viewModel.onCreatedNavigationHandled()
            onExpenseCreated(recordId)
        }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DailyExpenseTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            DailyExpenseBottomBar(
                primaryText = if (uiState.isSaving) "正在记录..." else "记录支出",
                onPrimaryClick = viewModel::submit,
                primaryEnabled = uiState.canSubmit,
            )
        },
    ) { paddingValues ->
        DailyExpenseContent(
            uiState = uiState,
            onAmountChange = viewModel::updateAmount,
            onCategorySelected = viewModel::selectCategory,
            onPartnerNameChange = viewModel::updatePartnerName,
            onMethodSelected = viewModel::selectMethod,
            onNotesChange = viewModel::updateNotes,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun DailyExpenseContent(
    uiState: DailyExpenseUiState,
    onAmountChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPartnerNameChange: (String) -> Unit,
    onMethodSelected: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AmountHeroCard(
            amount = uiState.amount,
            onAmountChange = onAmountChange,
        )

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            surfaceColor = GlassSurfaceHigh,
            shape = roundedCardShape,
            contentPadding = 16.dp,
        ) {
            CategoryIconGrid(
                selectedCategory = uiState.category,
                onSelected = onCategorySelected,
            )
        }

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            surfaceColor = GlassSurfaceHigh,
            shape = roundedCardShape,
            contentPadding = 16.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "付款账户",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                AccountSelectorPreview(
                    selectedMethod = uiState.method,
                    onSelected = onMethodSelected,
                )
                FormInfoRow(
                    label = "发生日期",
                    value = "保存时由后端写入当前时间",
                    icon = Icons.Outlined.CalendarToday,
                )
                GlassTextField(
                    value = uiState.partnerName,
                    onValueChange = onPartnerNameChange,
                    label = "往来对象（可选）",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            tint = TextSecondary,
                        )
                    },
                )

                GlassTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = "备注说明（可选）",
                    placeholder = "添加支出详情描述...",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Notes,
                            contentDescription = null,
                            tint = TextSecondary,
                        )
                    },
                )
            }
        }

        AttachmentCard()

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = "仅提交你录入的真实支出；附件和发生日期不会写入模拟占位数据。",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun DailyExpenseTopBar(
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
                text = "费用支出",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp),
            )
        }
    }
}

@Composable
private fun DailyExpenseBottomBar(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean,
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
                    .background(brush = primaryActionBrush, alpha = if (primaryEnabled) 1f else 0.5f)
                    .clickable(enabled = primaryEnabled, onClick = onPrimaryClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleLarge,
                color = SurfaceWhite,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun AmountHeroCard(
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "支出金额 (¥)",
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
                    lineHeight = 28.sp,
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
                        fontSize = 40.sp,
                        lineHeight = 48.sp,
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
                                    fontSize = 40.sp,
                                    lineHeight = 48.sp,
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
private fun CategoryIconGrid(
    selectedCategory: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "支出类别",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            expenseCategoryRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { option ->
                        CategoryIconTile(
                            option = option,
                            selected = selectedCategory == option.label,
                            onClick = { onSelected(option.label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryIconTile(
    option: ExpenseCategoryOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) Color(0xFFE3F2FD) else Color.Transparent
    val borderColor = if (selected) ZhihuijiPrimary else Color.Transparent
    val contentColor = if (selected) ZhihuijiPrimary else TextSecondary

        Column(
            modifier = modifier
                .height(78.dp)
                .clip(roundedCardShape)
                .background(backgroundColor)
                .border(1.dp, borderColor, roundedCardShape)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = contentColor,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun AccountSelectorPreview(
    selectedMethod: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = paymentMethodLabels[selectedMethod] ?: "其他"
    Box(modifier = Modifier.fillMaxWidth()) {
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
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            )
            Text(
                text = "⌄",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            paymentMethods.forEach { (method, methodLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = methodLabel,
                            color = if (method == selectedMethod) ZhihuijiPrimary else TextPrimary,
                        )
                    },
                    onClick = {
                        onSelected(method)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FormInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(roundedCardShape)
                .background(Color.White.copy(alpha = 0.80f))
                .border(1.dp, DividerLight, roundedCardShape)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun AttachmentCard() {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            surfaceColor = GlassSurfaceHigh,
            shape = roundedCardShape,
            contentPadding = 16.dp,
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "附件与凭证",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Text(
                    text = "最多9张",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(roundedCardShape)
                        .border(1.dp, DividerLight, roundedCardShape)
                        .background(Color.White.copy(alpha = 0.32f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddAPhoto,
                        contentDescription = null,
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "上传照片",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = ZhihuijiPrimary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(roundedCardShape)
                        .background(GlassSurfaceMedium)
                        .border(0.5.dp, GlassBorder, roundedCardShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "待接入",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextSecondary,
                    )
                }
            }
            Text(
                text = "后端资金流水合同暂未提供附件字段，本次不会提交照片占位数据。",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
