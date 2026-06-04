package com.zhihuiji.feature.purchases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.*

@Composable
fun PurchaseOrderDetailScreen(
    orderId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PurchaseOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) { viewModel.loadDetail(orderId) }

    val order = uiState.order
    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            GlassTopBar(title = "采购单详情", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            if (order != null) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(order.orderNo, style = ZhihuijiTypography.headlineMedium)
                                StatusPill(text = StatusLabels.purchaseOrderStatus(order.status), tone = if (order.status == 1) PillTone.SUCCESS else PillTone.WARNING)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("供应商：${order.supplierName ?: ""}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                Text("来源待联调", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                            }
                            Text("日期：${TimeFormatter.formatDateTime(order.createdAt)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PurchaseAmountColumn("商品合计", order.totalAmount)
                            PurchaseAmountTextColumn("金额说明", "当前仅返回合计金额")
                            PurchaseAmountTextColumn("优惠信息", "待联调")
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("商品明细", style = ZhihuijiTypography.titleMedium)
                            order.items.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName ?: "", style = ZhihuijiTypography.bodyMedium)
                                        Text("¥${MoneyFormatter.formatWithoutSymbol(item.unitCost)} × ${MoneyFormatter.formatWithoutSymbol(item.quantity)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                                    }
                                    Text(MoneyFormatter.formatWithoutSymbol(item.amount), style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                                }
                            }
                        }
                    }
                    val notes = order.notes
                    if (!notes.isNullOrBlank()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("备注", style = ZhihuijiTypography.titleMedium)
                                Text(notes, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                            }
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("处理轨迹", style = ZhihuijiTypography.titleMedium)
                            Text("当前仅展示采购单主数据与商品明细。入库流转、来源渠道和审批轨迹待联调后补齐。", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        }
                    }
                    if (uiState.error != null) {
                        Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                    }
                }
                BottomActionBar(primaryAction = {
                    PrimaryButton(text = "返回", onClick = onNavigateBack, modifier = Modifier.fillMaxWidth())
                })
            }
        }
    }
}

@Composable
private fun PurchaseAmountColumn(label: String, value: Double) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
        Text(MoneyFormatter.format(value), style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
    }
}

@Composable
private fun PurchaseAmountTextColumn(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
        Text(value, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
    }
}
