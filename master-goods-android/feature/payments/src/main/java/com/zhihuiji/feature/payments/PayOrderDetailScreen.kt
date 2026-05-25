package com.zhihuiji.feature.payments

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
fun PayOrderDetailScreen(
    orderId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PayOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) { viewModel.loadDetail(orderId) }

    val order = uiState.order
    Column(modifier = Modifier.fillMaxSize().glassBackground()) {
        GlassTopBar(title = "付款单详情", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        if (order != null) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(order.orderNo, style = ZhihuijiTypography.headlineMedium)
                            StatusPill(text = StatusLabels.payOrderStatus(order.status), tone = when (order.status) { 1 -> PillTone.SUCCESS; 2 -> PillTone.DANGER; else -> PillTone.WARNING })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("供应商：${order.supplierName}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                            Text("方式：${StatusLabels.paymentMethod(order.method)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        }
                        Text(TimeFormatter.formatDateTime(order.createdAt), style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                    }
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("付款金额", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                            Text(MoneyFormatter.format(order.amount), style = ZhihuijiTypography.displayMedium, color = ZhihuijiColors.Primary)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("当前状态", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                            Text(StatusLabels.payOrderStatus(order.status), style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        }
                    }
                }
                val refNo = order.referenceNo
                if (!refNo.isNullOrBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("参考号", style = ZhihuijiTypography.titleSmall)
                            Text(refNo, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                        }
                    }
                }
                val notes = order.notes
                if (!notes.isNullOrBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("备注", style = ZhihuijiTypography.titleSmall)
                            Text(notes, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                        }
                    }
                }
                if (uiState.error != null) {
                    Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                }
            }
            BottomActionBar(
                primaryAction = {
                    if (order.status == 0) {
                        PrimaryGradientButton(text = "确认付款", onClick = { viewModel.completeOrder() }, modifier = Modifier.fillMaxWidth())
                    }
                },
                secondaryActions = buildList {
                    if (order.status == 0) {
                        add { DangerOutlineButton(text = "取消", onClick = { viewModel.cancelOrder() }) }
                    }
                },
            )
        }
    }
}
