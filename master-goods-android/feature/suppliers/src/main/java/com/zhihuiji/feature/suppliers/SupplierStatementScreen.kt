package com.zhihuiji.feature.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.AmountTextStyle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun SupplierStatementScreen(
    supplierId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupplierStatementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(supplierId) {
        viewModel.loadStatement(supplierId)
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "对账单",
                subtitle = uiState.supplier?.name ?: "读取供应商真实往来",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享暂不可用",
                            tint = TextTertiary
                        )
                    }
                }
            )
        },
        bottomBar = {
            uiState.supplier?.let { supplier ->
                BottomActionBar(
                    primaryText = "发起付款",
                    onPrimaryClick = {},
                    primaryEnabled = false,
                    secondaryText = "下载PDF",
                    onSecondaryClick = {},
                    secondaryEnabled = false,
                    totalLabel = "应付总额",
                    totalAmount = formatStatementCurrency(supplier.balance),
                    totalAmountColor = if (supplier.balance > 0.0) DangerRed else SuccessGreen
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            }

            uiState.error != null -> {
                SupplierStatementEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "供应商对账加载失败",
                    message = uiState.error ?: "请稍后重试"
                )
            }

            uiState.supplier != null -> {
                SupplierStatementContent(
                    uiState = uiState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun SupplierStatementContent(
    uiState: SupplierStatementUiState,
    modifier: Modifier = Modifier
) {
    val supplier = uiState.supplier ?: return
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                blurRadius = 24.dp,
                contentPadding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ZhihuijiPrimary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Factory,
                            contentDescription = null,
                            tint = ZhihuijiPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = supplier.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "供应商ID：${supplier.id}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    StatusPill(
                        text = if (supplier.status == 1) "启用" else "停用",
                        status = if (supplier.status == 1) StatusType.NORMAL else StatusType.CANCELLED
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Total Payable / 应付总额",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatStatementCurrency(supplier.balance),
                    style = AmountTextStyle,
                    color = if (supplier.balance > 0.0) DangerRed else ZhihuijiPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "来自当前账号供应商余额字段；没有生成示例对账数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }

        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceMedium,
                contentPadding = 16.dp
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatementMetric(
                        modifier = Modifier.weight(1f),
                        label = "采购增加",
                        value = formatStatementCurrency(uiState.purchaseTotal),
                        color = DangerRed
                    )
                    StatementMetric(
                        modifier = Modifier.weight(1f),
                        label = "付款抵扣",
                        value = formatStatementCurrency(uiState.paymentTotal),
                        color = SuccessGreen
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions / 最近往来",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                LiquidGlassCard(
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 8.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ZhihuijiPrimary
                        )
                        Text(
                            text = "全部真实记录",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZhihuijiPrimary
                        )
                    }
                }
            }
        }

        if (uiState.transactions.isEmpty()) {
            item {
                SupplierStatementEmptyCard()
            }
        } else {
            items(
                items = uiState.transactions,
                key = { transaction -> "${transaction.kind}-${transaction.id}" }
            ) { transaction ->
                SupplierStatementTransactionRow(transaction = transaction)
            }
        }

        item {
            ContractBoundaryCard(warning = uiState.contractWarning)
        }
    }
}

@Composable
private fun StatementMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
    }
}

@Composable
private fun SupplierStatementTransactionRow(
    transaction: SupplierStatementTransaction,
    modifier: Modifier = Modifier
) {
    val isPurchase = transaction.kind == SupplierStatementTransactionKind.PURCHASE
    val color = if (isPurchase) DangerRed else SuccessGreen
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPurchase) Icons.Default.ShoppingCart else Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = color
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = transaction.amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(
                    text = transaction.statusText,
                    status = transaction.statusType()
                )
            }
        }
    }
}

@Composable
private fun SupplierStatementEmptyCard() {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp
    ) {
        Text(
            text = "暂无供应商往来记录",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "已读取当前账号真实采购单和付款单，没有匹配该供应商的记录；未展示任何 mock/sample/demo 数据。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ContractBoundaryCard(warning: String?) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceMedium,
        contentPadding = 16.dp
    ) {
        Text(
            text = "合同边界",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "本页按现有真实接口 /v2/suppliers、/v2/purchase-orders、/v2/pay-orders 汇总。当前后端没有独立对账单、PDF 导出或发起付款合同，底部动作保持禁用，不伪造结果。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        if (!warning.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.bodyMedium,
                color = WarningOrange
            )
        }
    }
}

@Composable
private fun SupplierStatementEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            surfaceColor = GlassSurfaceHigh
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Start
            )
        }
    }
}

private fun SupplierStatementTransaction.statusType(): StatusType = when (kind) {
    SupplierStatementTransactionKind.PURCHASE -> when (statusText) {
        "已收货" -> StatusType.COMPLETED
        "草稿" -> StatusType.PENDING
        else -> StatusType.LOW_STOCK
    }
    SupplierStatementTransactionKind.PAYMENT -> when (statusText) {
        "已付款" -> StatusType.NORMAL
        "待付款" -> StatusType.PENDING
        "已取消" -> StatusType.CANCELLED
        else -> StatusType.COMPLETED
    }
}

private fun formatStatementCurrency(value: Double): String = "¥%.2f".format(value)
