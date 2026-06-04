package com.zhihuiji.feature.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
fun SaleOrderDetailScreen(
    orderId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit = {},
    viewModel: SaleOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPaymentSheet by remember { mutableStateOf(false) }

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
            GlassTopBar(
                title = "销售单详情",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
            if (order != null) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(order.orderNo, style = ZhihuijiTypography.headlineMedium)
                                StatusPill(text = if (order.paidAmount < order.totalAmount) "待收款" else StatusLabels.saleOrderStatus(order.status), tone = if (order.paidAmount < order.totalAmount) PillTone.INFO else when(order.status) { 1 -> PillTone.SUCCESS; 2 -> PillTone.NEUTRAL; else -> PillTone.WARNING })
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("客户：${order.customerName ?: "散客"}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                Text(
                                    text = if (!order.notes.isNullOrBlank()) "备注已填写" else "暂无业务员字段",
                                    style = ZhihuijiTypography.bodySmall,
                                    color = ZhihuijiColors.TextSecondary,
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("日期：${TimeFormatter.formatDateTime(order.createdAt)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                                Text("来源待联调", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                            }
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            AmountColumn("订单金额", order.totalAmount, ZhihuijiColors.TextPrimary)
                            AmountColumn("已收金额", order.paidAmount, ZhihuijiColors.TextPrimary)
                            AmountColumn("待收金额", order.totalAmount - order.paidAmount, ZhihuijiColors.Danger)
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("商品明细", style = ZhihuijiTypography.titleMedium)
                            order.items.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName ?: "", style = ZhihuijiTypography.bodyMedium)
                                        Text("¥${MoneyFormatter.formatWithoutSymbol(item.unitPrice)} × ${MoneyFormatter.formatWithoutSymbol(item.quantity)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                                    }
                                    Text(MoneyFormatter.formatWithoutSymbol(item.amount), style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                                }
                                HorizontalDivider(color = ZhihuijiColors.BorderLight, thickness = 0.5.dp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("合计金额", style = ZhihuijiTypography.titleMedium)
                                Text(MoneyFormatter.format(order.totalAmount), style = ZhihuijiTypography.headlineLarge, color = ZhihuijiColors.Primary)
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
                            Text("当前仅展示订单时间、金额和收款结果。审批流、打印记录、来源渠道等轨迹字段待联调后补齐。", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        }
                    }
                    if (uiState.payments.isNotEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("收款记录", style = ZhihuijiTypography.titleMedium)
                                uiState.payments.forEach { payment ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(MoneyFormatter.format(payment.amount), style = ZhihuijiTypography.bodyMedium)
                                        Text(StatusLabels.paymentMethod(payment.method), style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                    if (uiState.payments.isEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("收款记录", style = ZhihuijiTypography.titleMedium)
                                EmptyState(
                                    icon = Icons.Default.CheckCircle,
                                    title = "暂无收款记录",
                                    subtitle = "订单完成收款后，记录会展示在这里",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                    if (uiState.error != null) {
                        Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                    }
                }
                BottomActionBar(
                    primaryAction = {
                        if (order.paidAmount < order.totalAmount) {
                            PrimaryButton(
                                text = "收款",
                                onClick = { showPaymentSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.CheckCircle,
                            )
                        } else {
                            PrimaryButton(text = "完成订单", onClick = { viewModel.completeOrder() }, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    secondaryActions = buildList {
                        if (order.status != 2) {
                            add { DangerOutlineButton(text = "作废", onClick = { viewModel.cancelOrder() }) }
                            add { SecondaryOutlineButton(text = "修改", onClick = onNavigateToEdit) }
                        }
                    },
                )
            }
        }
    }

    if (showPaymentSheet) {
        SalePaymentSheet(
            maxAmount = (order?.totalAmount ?: 0.0) - (order?.paidAmount ?: 0.0),
            onConfirm = { amount, method, ref -> viewModel.addPayment(amount, method, ref); showPaymentSheet = false },
            onDismiss = { showPaymentSheet = false },
        )
    }
}

@Composable
private fun AmountColumn(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
        Text(MoneyFormatter.format(value), style = ZhihuijiTypography.titleMedium, color = color)
    }
}
