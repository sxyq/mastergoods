package com.zhihuiji.feature.payments

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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.AmountTextStyle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun PayOrderDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "付款单详情",
                subtitle = uiState.order?.orderNo ?: "付款金额与关联采购",
                onNavigationClick = onBackClick
            )
        },
        bottomBar = {
            if (uiState.order != null) {
                BottomActionBar(
                    primaryText = "分享",
                    onPrimaryClick = {},
                    secondaryText = "打印单据",
                    onSecondaryClick = {},
                    totalLabel = "付款金额",
                    totalAmount = "¥%.2f".format(uiState.order!!.amount)
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
                    PayOrderDetailContent(order = uiState.order!!)
                }
            }
        }
    }
}

@Composable
private fun PayOrderDetailContent(
    order: com.zhihuiji.core.model.v2.order.PayOrderV2Dto,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PayOrderAmountCard(order = order)
        }
        item {
            PayOrderBasicInfoCard(order = order)
        }
        item {
            PayOrderRemarkCard(notes = order.notes)
        }
    }
}

@Composable
private fun PayOrderAmountCard(
    order: com.zhihuiji.core.model.v2.order.PayOrderV2Dto,
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
        blurRadius = 24.dp,
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = StatusLabels.payOrderStatus(order.status),
                    status = statusType
                )
            }

            Text(
                text = "¥%.2f".format(order.amount),
                style = AmountTextStyle,
                color = ZhihuijiPrimary
            )

            Text(
                text = "${StatusLabels.paymentMethod(order.method)} · ${order.createdAtText()}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PayOrderBasicInfoCard(
    order: com.zhihuiji.core.model.v2.order.PayOrderV2Dto,
    modifier: Modifier = Modifier,
) {
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
            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            InfoRow(label = "供应商名称", value = order.supplierName ?: "-")
            InfoRow(label = "付款方式", value = StatusLabels.paymentMethod(order.method))
            InfoRow(label = "付款时间", value = order.createdAtText())
            InfoRow(label = "关联采购单号", value = order.referenceNo ?: "-")
        }
    }
}

@Composable
private fun PayOrderRemarkCard(
    notes: String?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "备注",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = notes.takeUnless { it.isNullOrBlank() } ?: "暂无备注",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
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
