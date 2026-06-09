package com.zhihuiji.feature.purchases

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun PurchaseOrderDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "采购单详情",
                subtitle = uiState.order?.orderNo ?: "采购明细与付款进度",
                onNavigationClick = onBackClick,
                actions = {
                    uiState.order?.let { order ->
                        Row {
                            IconButton(onClick = { viewModel.deleteOrder(onDeleteSuccess) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = DangerRed
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            uiState.order?.let { order ->
                BottomActionBar(
                    primaryText = if (order.status == 0) "编辑单据" else "已完成",
                    onPrimaryClick = { onEditClick(order.id) },
                    primaryEnabled = order.status == 0,
                    totalLabel = "采购金额",
                    totalAmount = "¥%.2f".format(order.totalAmount)
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
                    PurchaseOrderDetailContent(order = uiState.order!!)
                }
            }
        }
    }
}

@Composable
private fun PurchaseOrderDetailContent(
    order: com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto,
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
                    ?: "${item.productId}:${item.productName}:${item.quantity}:${item.unitCost}:${item.amount}:${item.createdAt}"
            }
        ) { item ->
            OrderItemCard(item = item)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            TotalAmountRow(totalAmount = order.totalAmount)
        }
    }
}

@Composable
private fun OrderInfoCard(
    order: com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto,
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

            InfoRow(label = "供应商名称", value = order.supplierName ?: "-")
            InfoRow(label = "订单金额", value = "¥%.2f".format(order.totalAmount))
            InfoRow(label = "已付金额", value = "¥%.2f".format(order.paidAmount))
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
    item: com.zhihuiji.core.model.v2.order.PurchaseOrderItemV2Dto,
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
                    text = "单价: ¥%.2f".format(item.unitCost),
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
            text = "¥%.2f".format(totalAmount),
            style = MaterialTheme.typography.titleLarge,
            color = ZhihuijiPrimary
        )
    }
}
