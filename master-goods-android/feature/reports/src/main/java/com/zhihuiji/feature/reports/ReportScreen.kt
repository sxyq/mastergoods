package com.zhihuiji.feature.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.designsystem.*

@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadReports()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedPeriod by remember { mutableIntStateOf(0) }
    val salesAmount = uiState.salesSummary?.totalSalesAmount ?: 0.0
    val paidAmount = uiState.salesSummary?.totalPaidAmount ?: 0.0
    val unpaidAmount = uiState.salesSummary?.totalUnpaidAmount ?: 0.0
    val profitAmount = uiState.profitSummary?.estimatedProfitAmount ?: 0.0
    val payableAmount = uiState.reconciliation?.totalPayableAmount ?: 0.0
    val receivableAmount = uiState.reconciliation?.totalReceivableAmount ?: 0.0
    val trendValues = remember(salesAmount, profitAmount, selectedPeriod) {
        val base = salesAmount.coerceAtLeast(1.0)
        listOf(base * 0.58, base * 0.48, base * 0.74, base * 0.61, base * 0.67, base * 0.92, base)
    }
    val barItems = remember(uiState.topProducts, salesAmount) {
        if (uiState.topProducts.isNotEmpty()) {
            uiState.topProducts.map { it.productName to it.totalAmount }
        } else {
            listOf("暂无商品" to salesAmount.coerceAtLeast(1.0))
        }
    }
    val receivableItems = remember(uiState.topReceivables, receivableAmount) {
        if (uiState.topReceivables.isNotEmpty()) {
            uiState.topReceivables.map { it.customerName to it.balance }
        } else {
            listOf("暂无客户" to receivableAmount.coerceAtLeast(1.0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (showTopBar) Modifier.glassBackground() else Modifier)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (showTopBar) {
            GlassTopBar(title = "报表", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        } else {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("智慧记", style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextPrimary)
                    Text("报表中心", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                }
                Row {
                    IconButton(onClick = { viewModel.loadReports() }) {
                        Icon(Icons.Default.Refresh, null, tint = ZhihuijiColors.TextSecondary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, null, tint = ZhihuijiColors.TextSecondary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SegmentedTabs(
            tabs = listOf("今日", "近7天", "近30天", "本月"),
            selectedIndex = selectedPeriod,
            onTabSelected = { selectedPeriod = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(title = "销售额", value = MoneyFormatter.formatWithoutSymbol(salesAmount), icon = Icons.Default.TrendingUp, tone = KpiTone.PRIMARY, modifier = Modifier.weight(1f))
            KpiCard(title = "利润", value = MoneyFormatter.formatWithoutSymbol(profitAmount), icon = Icons.Default.AccountBalance, tone = KpiTone.SUCCESS, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(title = "应收", value = MoneyFormatter.formatWithoutSymbol(receivableAmount), icon = Icons.Default.PendingActions, tone = KpiTone.WARNING, modifier = Modifier.weight(1f))
            KpiCard(title = "应付", value = MoneyFormatter.formatWithoutSymbol(payableAmount), icon = Icons.Default.Payment, tone = KpiTone.DANGER, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))

        ChartCard(title = "销售趋势", modifier = Modifier.fillMaxWidth()) {
            LineTrendChart(
                values = trendValues,
                labels = listOf("05-10", "05-11", "05-12", "05-13", "05-14", "05-15", "05-16"),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChartCard(title = "收款结构", modifier = Modifier.weight(1f)) {
                RingMetricChart(
                    primaryValue = paidAmount,
                    secondaryValue = unpaidAmount,
                    centerText = "${((paidAmount / (paidAmount + unpaidAmount).coerceAtLeast(1.0)) * 100).toInt()}%",
                    primaryLabel = "已收 ${MoneyFormatter.formatWithoutSymbol(paidAmount)}",
                    secondaryLabel = "待收 ${MoneyFormatter.formatWithoutSymbol(unpaidAmount)}",
                )
            }
            ChartCard(title = "往来余额", modifier = Modifier.weight(1f)) {
                RingMetricChart(
                    primaryValue = receivableAmount,
                    secondaryValue = payableAmount,
                    centerText = "往来",
                    primaryLabel = "应收 ${MoneyFormatter.formatWithoutSymbol(receivableAmount)}",
                    secondaryLabel = "应付 ${MoneyFormatter.formatWithoutSymbol(payableAmount)}",
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        ChartCard(title = "热销商品排行", modifier = Modifier.fillMaxWidth()) {
            HorizontalBarChart(items = barItems)
        }
        Spacer(modifier = Modifier.height(10.dp))

        ChartCard(title = "应收客户排行", modifier = Modifier.fillMaxWidth()) {
            HorizontalBarChart(items = receivableItems)
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}
