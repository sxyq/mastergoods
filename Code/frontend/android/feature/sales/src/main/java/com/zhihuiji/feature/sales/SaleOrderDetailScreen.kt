package com.zhihuiji.feature.sales

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.model.v2.order.SalePaymentV2Dto
import kotlinx.coroutines.launch

@Composable
fun SaleOrderDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onPaymentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SaleOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var receiptMenuExpanded by remember { mutableStateOf(false) }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "销售单详情",
                subtitle = uiState.order?.orderNo ?: "订单明细与收款",
                onNavigationClick = onBackClick,
                actions = {
                    Box {
                        IconButton(onClick = { receiptMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "小票打印与导出",
                                tint = TextPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = receiptMenuExpanded,
                            onDismissRequest = { receiptMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("打印或另存 PDF") },
                                leadingIcon = {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                },
                                onClick = {
                                    receiptMenuExpanded = false
                                    uiState.order?.let { order ->
                                        coroutineScope.launch {
                                            viewModel.downloadReceiptPdf()
                                                .onSuccess { pdf ->
                                                    runCatching { SaleReceiptExporter.printPdf(context, order, pdf) }
                                                        .onFailure {
                                                            Toast.makeText(context, "打开打印服务失败", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                                .onFailure {
                                                    Toast.makeText(context, it.message ?: "小票 PDF 下载失败", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            uiState.order?.let { order ->
                BottomActionBar(
                    primaryText = "收款",
                    onPrimaryClick = { onPaymentClick(order.id) },
                    secondaryText = if (order.status == 0) "编辑单据" else null,
                    onSecondaryClick = { onEditClick(order.id) },
                    totalLabel = "待收金额",
                    totalAmount = MoneyFormatter.format(order.totalAmount - order.paidAmount)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZhihuijiPrimary)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "出错了",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            IconButton(onClick = { viewModel.loadOrder() }) {
                                Text("重试", color = ZhihuijiPrimary)
                            }
                        }
                    }
                }

                uiState.order != null -> {
                    SaleOrderDetailContent(order = uiState.order!!, payments = uiState.payments)
                }
            }
        }
    }
}

@Composable
private fun SaleOrderDetailContent(
    order: com.zhihuiji.core.model.v2.order.SaleOrderV2Dto,
    payments: List<SalePaymentV2Dto>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OrderInfoCard(order = order)
        }

        item {
            Text(
                text = "商品明细",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(
            items = order.items,
            key = { item ->
                item.id.takeIf { it != 0L }
                    ?: "${item.productId}:${item.productName}:${item.quantity}:${item.unitPrice}:${item.amount}:${item.createdAt}"
            }
        ) { item ->
            OrderItemCard(item = item)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            TotalAmountRow(totalAmount = order.totalAmount)
        }

        if (payments.isNotEmpty()) {
            item {
                Text(
                    text = "收款记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(
                items = payments,
                key = { it.id }
            ) { payment ->
                PaymentRecordCard(payment = payment)
            }
        }
    }
}

@Composable
private fun OrderInfoCard(
    order: com.zhihuiji.core.model.v2.order.SaleOrderV2Dto,
    modifier: Modifier = Modifier,
) {
    val statusType = when (order.status) {
        0 -> StatusType.PENDING
        1 -> StatusType.COMPLETED
        2 -> StatusType.CANCELLED
        else -> StatusType.NORMAL
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNo,
                    style = MaterialTheme.typography.titleMedium
                )
                StatusPill(
                    text = order.statusText(),
                    status = statusType
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            InfoRow(label = "客户名称", value = order.customerName ?: "-")
            InfoRow(label = "订单金额", value = MoneyFormatter.format(order.totalAmount))
            InfoRow(label = "已付金额", value = MoneyFormatter.format(order.paidAmount))
            InfoRow(label = "折扣金额", value = MoneyFormatter.format(order.discountAmount))
            InfoRow(label = "创建时间", value = order.createdAtText())
            val notes = order.notes
            if (!notes.isNullOrBlank()) {
                InfoRow(label = "备注", value = notes)
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun OrderItemCard(
    item: com.zhihuiji.core.model.v2.order.SaleOrderItemV2Dto,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.productName ?: "未知商品",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "数量: %.2f".format(item.quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "单价: ${MoneyFormatter.format(item.unitPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "小计: ${item.subtotalText()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZhihuijiPrimary
                )
            }
        }
    }
}

@Composable
private fun TotalAmountRow(
    totalAmount: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "订单总金额: ",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = MoneyFormatter.format(totalAmount),
            style = MaterialTheme.typography.titleLarge,
            color = ZhihuijiPrimary
        )
    }
}

@Composable
private fun PaymentRecordCard(
    payment: SalePaymentV2Dto,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StatusLabels.paymentType(payment.type),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = MoneyFormatter.format(payment.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = ZhihuijiPrimary
                )
            }
            InfoRow(label = "收款方式", value = StatusLabels.paymentMethod(payment.method))
            val referenceNo = payment.referenceNo
            if (!referenceNo.isNullOrBlank()) {
                InfoRow(label = "凭证号", value = referenceNo)
            }
            InfoRow(label = "收款时间", value = TimeFormatter.formatDateTime(payment.createdAt))
        }
    }
}
