package com.zhihuiji.feature.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DocumentListCard
import com.zhihuiji.core.designsystem.DocumentListBottomContentPadding
import com.zhihuiji.core.designsystem.DocumentStatusTone
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun PayOrderListScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: PayOrderListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
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
                PayOrderStateMessage(
                    title = "付款单加载失败",
                    message = uiState.error ?: "请稍后重试",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            uiState.orders.isEmpty() -> {
                PayOrderStateMessage(
                    title = "暂无付款订单",
                    message = "当前账号没有可展示的真实付款单",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 12.dp,
                        end = 20.dp,
                        bottom = DocumentListBottomContentPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.orders,
                        key = { it.id }
                    ) { order ->
                        PayOrderListItem(
                            order = order,
                            onClick = { onNavigateToDetail(order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayOrderListItem(
    order: PayOrderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DocumentListCard(
        modifier = modifier,
        title = order.orderNo,
        subtitle = order.payee.ifBlank { "未命名收款方" },
        meta = listOf(order.method, order.referenceNo ?: "未关联", order.date)
            .filter { it.isNotBlank() }
            .joinToString(" · "),
        amount = order.amount,
        statusLabel = order.status,
        statusTone = when (order.status) {
            "待付款" -> DocumentStatusTone.WARNING
            "已付款" -> DocumentStatusTone.SUCCESS
            "已取消" -> DocumentStatusTone.DANGER
            else -> DocumentStatusTone.NEUTRAL
        },
        amountColor = if (order.status == "已付款") ZhihuijiPrimary else Color(0xFF181C20),
        onClick = onClick
    )
}

@Composable
private fun PayOrderStateMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier,
        surfaceColor = GlassSurfaceLow,
        contentPadding = 16.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF181C20)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
