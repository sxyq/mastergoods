package com.zhihuiji.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.*

@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToAgent: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val salesAmount = uiState.salesSummary?.totalSalesAmount ?: 0.0
    val unpaidAmount = uiState.salesSummary?.totalUnpaidAmount ?: 0.0
    val profitAmount = uiState.profitSummary?.estimatedProfitAmount ?: 0.0
    val lowStockCount = uiState.lowStockProducts.size
    val trendValues = remember(salesAmount) {
        val base = salesAmount.coerceAtLeast(1.0)
        listOf(base * 0.64, base * 0.51, base * 0.78, base * 0.67, base * 0.74, base * 0.88, base)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (showTopBar) Modifier.glassBackground() else Modifier)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("智慧记", style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextPrimary)
                Text("今日经营概览", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
            }
            Row {
                IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.NotificationsNone, null) }
                IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.CenterFocusStrong, null) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                title = "今日销售",
                value = MoneyFormatter.format(salesAmount),
                trend = "较昨日 ↑ 18.6%",
                icon = Icons.Default.Inventory2,
                tone = KpiTone.PRIMARY,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                title = "待收款",
                value = MoneyFormatter.format(unpaidAmount),
                trend = "较昨日 ↑ 7.2%",
                icon = Icons.Default.AccountBalanceWallet,
                tone = KpiTone.WARNING,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                title = "低库存",
                value = "$lowStockCount",
                subtitle = "需及时补货",
                icon = Icons.Default.WarningAmber,
                tone = KpiTone.WARNING,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                title = "净现金流",
                value = MoneyFormatter.format(profitAmount),
                trend = "较昨日 ↑ 13.4%",
                icon = Icons.Default.PhoneInTalk,
                tone = KpiTone.SUCCESS,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        ChartCard(title = "销售趋势", modifier = Modifier.fillMaxWidth()) {
            LineTrendChart(
                values = trendValues,
                labels = listOf("05-10", "05-11", "05-12", "05-13", "05-14", "05-15", "05-16"),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("待处理提醒", style = ZhihuijiTypography.titleMedium)
                    Text("全部", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.Primary)
                }
                ReminderRow(icon = Icons.Default.ReceiptLong, title = "待审核销售单", subtitle = "${uiState.salesSummary?.totalOrderCount ?: 0}张单据待处理", tone = PillTone.INFO)
                ReminderRow(icon = Icons.Default.AccountBalanceWallet, title = "待收款客户", subtitle = "待收金额 ${MoneyFormatter.format(unpaidAmount)}", tone = PillTone.DANGER)
                ReminderRow(icon = Icons.Default.WarningAmber, title = "低库存商品", subtitle = "${uiState.lowStockProducts.size}个商品低于安全库存", tone = PillTone.WARNING)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("快捷开单", style = ZhihuijiTypography.titleMedium)
                    Text("常用入口", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryGradientButton(text = "销售", onClick = onNavigateToSales, modifier = Modifier.weight(1f))
                    SecondaryOutlineButton(text = "商品", onClick = onNavigateToProducts, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryOutlineButton(text = "客户", onClick = onNavigateToCustomers, modifier = Modifier.weight(1f))
                    SecondaryOutlineButton(text = "助手", onClick = onNavigateToAgent, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.lowStockProducts.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("低库存预警", style = ZhihuijiTypography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.lowStockProducts.take(5).forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.productName, style = ZhihuijiTypography.bodyMedium)
                            StatusPill(text = StatusLabels.stockStatus(item.stock, item.safeStock), tone = PillTone.WARNING)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.topReceivables.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("应收排行", style = ZhihuijiTypography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.topReceivables.take(5).forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.customerName, style = ZhihuijiTypography.bodyMedium)
                            Text(MoneyFormatter.format(item.balance), style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.Danger)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun ReminderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tone: PillTone,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, modifier = Modifier.size(28.dp), tint = when (tone) {
            PillTone.SUCCESS -> ZhihuijiColors.Success
            PillTone.WARNING -> ZhihuijiColors.Warning
            PillTone.DANGER -> ZhihuijiColors.Danger
            PillTone.INFO -> ZhihuijiColors.InfoBlue
            PillTone.NEUTRAL -> ZhihuijiColors.TextTertiary
        })
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
            Text(subtitle, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
        }
        Text("›", style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextTertiary)
    }
}
