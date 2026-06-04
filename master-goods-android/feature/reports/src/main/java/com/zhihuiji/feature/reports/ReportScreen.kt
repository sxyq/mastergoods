package com.zhihuiji.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.designsystem.*
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto

@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    reselectSignal: Int = 0,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()
    var selectedFocus by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadReports()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BottomBarScrollVisibilityEffect(listState)
    BottomBarScrollToTopEffect(reselectSignal, listState)

    val salesAmount = uiState.totalSalesAmount
    val paidAmount = uiState.totalPaidAmount
    val unpaidAmount = uiState.totalUnpaidAmount
    val profitAmount = uiState.estimatedProfit
    val accountBalance = uiState.totalAccountBalance
    val totalCostIn = uiState.totalCostIn
    val totalCostOut = uiState.totalCostOut
    val selectedPeriod = uiState.selectedPeriod
    val error = uiState.error
    val focusChips = remember { listOf("总览", "库存与成本", "回款与账户", "风险提示") }
    val periodTabs = remember { listOf("今日", "近7天", "近30天", "本月") }
    val lowStockBarItems = remember(uiState.lowStockProducts, query) {
        uiState.lowStockProducts
            .filter { reportMatchesQuery(query, it.name, it.code, it.categoryName) }
            .take(5)
            .map { it.name to (it.safeStock - it.stock).coerceAtLeast(0.0) }
    }
    val receivableOrders = remember(uiState.saleOrders, query) {
        uiState.saleOrders
            .asSequence()
            .filter { it.totalAmount - it.paidAmount > 0.009 }
            .filter { reportMatchesQuery(query, it.orderNo, it.customerName) }
            .sortedByDescending { it.totalAmount - it.paidAmount }
            .take(5)
            .toList()
    }
    val riskItems = remember(uiState.saleOrders, uiState.lowStockProducts, totalCostOut, query) {
        buildList {
            add(
                ReportInsight(
                    title = "待收风险",
                    subtitle = "待收 ${MoneyFormatter.format(unpaidAmount)}",
                    meta = "${receivableOrders.size} 个客户需要继续跟进",
                    statusText = if (unpaidAmount > 0.0) "持续跟进" else "稳定",
                    statusTone = if (unpaidAmount > 0.0) PillTone.DANGER else PillTone.SUCCESS,
                    icon = Icons.Default.AccountBalanceWallet,
                    amount = if (unpaidAmount > 0.0) MoneyFormatter.format(unpaidAmount) else null,
                    amountColor = ZhihuijiColors.Danger,
                ),
            )
            add(
                ReportInsight(
                    title = "库存风险",
                    subtitle = "${uiState.lowStockProducts.size} 个商品低于安全库存",
                    meta = "优先处理核心动销商品的补货",
                    statusText = if (uiState.lowStockProducts.isEmpty()) "正常" else "预警",
                    statusTone = if (uiState.lowStockProducts.isEmpty()) PillTone.SUCCESS else PillTone.WARNING,
                    icon = Icons.Default.Inventory2,
                    amount = null,
                    amountColor = ZhihuijiColors.Warning,
                ),
            )
            if (totalCostOut > 0.0) {
                add(
                    ReportInsight(
                        title = "成本支出",
                        subtitle = "本期出库成本 ${MoneyFormatter.format(totalCostOut)}",
                        meta = "当前仍是客户端聚合估算值",
                        statusText = "待验",
                        statusTone = PillTone.INFO,
                        icon = Icons.Default.AccountBalance,
                        amount = MoneyFormatter.format(totalCostOut),
                        amountColor = ZhihuijiColors.InfoBlue,
                    ),
                )
            }
        }.filter { reportMatchesQuery(query, it.title, it.subtitle, it.meta, it.statusText) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item("top_bar") {
            if (showTopBar) {
                GlassTopBar(
                    title = "报表",
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onNavigateBack,
                    actions = {
                        IconButton(onClick = { viewModel.loadReports(selectedPeriod) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = ZhihuijiColors.TextPrimary)
                        }
                    },
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("智慧记", style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextPrimary)
                        Text("报表中心", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                    }
                    Row {
                        IconButton(onClick = { viewModel.loadReports(selectedPeriod) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = ZhihuijiColors.TextSecondary)
                        }
                    }
                }
            }
        }
        item("top_spacing") { Spacer(modifier = Modifier.height(8.dp)) }
        item("overview") {
            ReportOverviewCard(
                selectedPeriodLabel = periodTabs[selectedPeriod],
                isLoading = uiState.isLoading,
                totalCostIn = totalCostIn,
                totalCostOut = totalCostOut,
                unpaidAmount = unpaidAmount,
            )
        }
        item("overview_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
        item("filters") {
            SearchFilterBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "搜索图表、风险或客户",
                filterIcon = Icons.Default.Tune,
                onFilterClick = { selectedFocus = (selectedFocus + 1) % focusChips.size },
                modifier = Modifier.fillMaxWidth(),
            )
            SegmentedTabs(
                tabs = periodTabs,
                selectedIndex = selectedPeriod,
                onTabSelected = { viewModel.setPeriod(it) },
                modifier = Modifier.fillMaxWidth(),
            )
            FilterChipRow(
                chips = focusChips,
                selectedIndex = selectedFocus,
                onChipSelected = { selectedFocus = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "时间标签当前只影响销售单与应收汇总；账户余额、低库存和库存成本仍展示当前快照。",
                style = ZhihuijiTypography.labelSmall,
                color = ZhihuijiColors.TextSecondary,
            )
        }
        item("filters_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
        if (error != null) {
            item("error") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ZhihuijiColors.Danger)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("报表数据加载存在缺口", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                            Text(error.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        }
                        StatusPill(text = "待复查", tone = PillTone.DANGER)
                    }
                }
            }
            item("error_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
        }
        item("kpis_top") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "销售额",
                    value = MoneyFormatter.formatWithoutSymbol(salesAmount),
                    subtitle = "销售单按 ${periodTabs[selectedPeriod]} 过滤",
                    icon = Icons.Default.Sell,
                    tone = KpiTone.PRIMARY,
                    modifier = Modifier.weight(1f),
                )
                KpiCard(
                    title = "利润(估)",
                    value = MoneyFormatter.formatWithoutSymbol(profitAmount),
                    subtitle = "基于当月库存统计估算，未随时间标签重算",
                    icon = Icons.Default.AccountBalance,
                    tone = KpiTone.SUCCESS,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item("kpis_top_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
        item("kpis_bottom") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "应收",
                    value = MoneyFormatter.formatWithoutSymbol(unpaidAmount),
                    subtitle = "销售单按 ${periodTabs[selectedPeriod]} 过滤",
                    icon = Icons.Default.PendingActions,
                    tone = KpiTone.WARNING,
                    modifier = Modifier.weight(1f),
                )
                KpiCard(
                    title = "账户余额",
                    value = MoneyFormatter.formatWithoutSymbol(accountBalance),
                    subtitle = "当前账户快照，不随时间标签切换",
                    icon = Icons.Default.AccountBalanceWallet,
                    tone = KpiTone.SUCCESS,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item("kpis_bottom_spacing") { Spacer(modifier = Modifier.height(12.dp)) }
        if (selectedFocus == 0 || selectedFocus == 2) {
            if (reportMatchesQuery(query, "销售趋势", "走势", "趋势", "待联调")) {
                item("trend_chart") {
                    ChartCard(title = "销售趋势", modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = "趋势数据准备中",
                            subtitle = "当前先展示已接入汇总结果，真实趋势序列与坐标仍待后续联调。",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
                item("trend_chart_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
            }
            if (reportMatchesQuery(query, "回款结构", "余额", "应收")) {
                item("ring_charts") {
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
                        ChartCard(title = "账户余额", modifier = Modifier.weight(1f)) {
                            RingMetricChart(
                                primaryValue = accountBalance,
                                secondaryValue = unpaidAmount,
                                centerText = "余额",
                                primaryLabel = "余额 ${MoneyFormatter.formatWithoutSymbol(accountBalance)}",
                                secondaryLabel = "应收 ${MoneyFormatter.formatWithoutSymbol(unpaidAmount)}",
                            )
                        }
                    }
                }
                item("ring_charts_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
            }
        }
        if (selectedFocus == 0 || selectedFocus == 1) {
            if (reportMatchesQuery(query, "库存缺口", "低库存", "库存")) {
                item("low_stock_chart") {
                    ChartCard(title = "低库存缺口", modifier = Modifier.fillMaxWidth()) {
                        if (lowStockBarItems.isEmpty()) {
                            EmptyState(
                                icon = Icons.Default.Inventory2,
                                title = "当前没有低库存商品",
                                subtitle = "缺口图会在真实低库存商品出现后展示。",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                            )
                        } else {
                            HorizontalBarChart(items = lowStockBarItems)
                        }
                    }
                }
                item("low_stock_chart_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
            }
        }
        if (selectedFocus == 0 || selectedFocus == 2) {
            item("receivables_header") {
                ReportSectionHeader(title = "重点回款客户", actionText = if (receivableOrders.isEmpty()) "暂无欠款" else "TOP ${receivableOrders.size}")
            }
            item("receivables_spacing") { Spacer(modifier = Modifier.height(8.dp)) }
            if (receivableOrders.isEmpty()) {
                item("receivables_empty") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Default.PieChart,
                            title = "当前没有待回款客户",
                            subtitle = "回款结构会随着新订单和收款自动变化",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
            } else {
                items(items = receivableOrders, key = { order -> order.id }) { order ->
                    ReportReceivableItem(order)
                }
            }
            item("receivables_footer_spacing") { Spacer(modifier = Modifier.height(12.dp)) }
        }
        if (selectedFocus == 0 || selectedFocus == 3) {
            item("risk_header") {
                ReportSectionHeader(title = "风险与静态缺口", actionText = if (riskItems.isEmpty()) "无匹配项" else "客户端聚合")
            }
            item("risk_spacing") { Spacer(modifier = Modifier.height(8.dp)) }
            if (riskItems.isEmpty()) {
                item("risk_empty") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Default.Analytics,
                            title = "当前没有匹配的风险项",
                            subtitle = "可以切换关键字或回到总览查看全部洞察",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
            } else {
                items(items = riskItems, key = { item -> item.title }) { item ->
                    BusinessListItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        meta = item.meta,
                        amount = item.amount,
                        amountColor = item.amountColor,
                        statusText = item.statusText,
                        statusTone = item.statusTone,
                        icon = item.icon,
                        iconTint = item.amountColor,
                    )
                }
            }
            item("risk_footer_spacing") { Spacer(modifier = Modifier.height(12.dp)) }
        }
        item("summary_footer") {
            SummaryFooter(
                leftText = "当前报表基于客户端本地聚合与静态占位",
                rightText = if (uiState.isLoading) "刷新中" else "趋势序列待联调",
            )
        }
        item("bottom_spacing") { Spacer(modifier = Modifier.height(88.dp)) }
    }
}

@Composable
private fun ReportOverviewCard(
    selectedPeriodLabel: String,
    isLoading: Boolean,
    totalCostIn: Double,
    totalCostOut: Double,
    unpaidAmount: Double,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("报表总览", style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                    Text("通过关键指标、结构图和风险项快速查看当前已接入的经营变化", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                }
                StatusPill(text = selectedPeriodLabel, tone = PillTone.INFO)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(text = if (isLoading) "刷新中" else "本地聚合", tone = if (isLoading) PillTone.INFO else PillTone.SUCCESS)
                StatusPill(text = if (unpaidAmount > 0.0) "回款跟进" else "回款平稳", tone = if (unpaidAmount > 0.0) PillTone.WARNING else PillTone.SUCCESS)
            }
            FieldRow(label = "入库成本", value = MoneyFormatter.format(totalCostIn), valueColor = ZhihuijiColors.InfoBlue)
            FieldRow(label = "出库成本", value = MoneyFormatter.format(totalCostOut), valueColor = ZhihuijiColors.Warning)
            FieldRow(label = "说明", value = "销售与应收会随时间标签筛选；库存成本、账户余额和占位图表仍代表当前环境内可静态验收部分。", valueColor = ZhihuijiColors.TextSecondary)
        }
    }
}

@Composable
private fun ReportSectionHeader(
    title: String,
    actionText: String,
) {
    SectionHeader(title = title, actionText = actionText)
}

@Composable
private fun ReportReceivableItem(order: SaleOrderV2Dto) {
    val receivableAmount = order.totalAmount - order.paidAmount
    BusinessListItem(
        title = order.customerName?.takeIf { it.isNotBlank() } ?: order.orderNo,
        subtitle = "订单 ${order.orderNo}",
        meta = "总额 ${MoneyFormatter.format(order.totalAmount)}，已收 ${MoneyFormatter.format(order.paidAmount)}",
        amount = MoneyFormatter.format(receivableAmount),
        amountColor = ZhihuijiColors.Danger,
        statusText = if (receivableAmount > order.totalAmount * 0.5) "高优先级" else "待回款",
        statusTone = if (receivableAmount > order.totalAmount * 0.5) PillTone.DANGER else PillTone.WARNING,
        icon = Icons.Default.AccountBalanceWallet,
        iconTint = ZhihuijiColors.Primary,
    )
}

private data class ReportInsight(
    val title: String,
    val subtitle: String,
    val meta: String,
    val statusText: String,
    val statusTone: PillTone,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val amount: String?,
    val amountColor: androidx.compose.ui.graphics.Color,
)

private fun reportMatchesQuery(query: String, vararg values: String?): Boolean {
    if (query.isBlank()) return true
    val keyword = query.trim()
    return values.any { value -> value?.contains(keyword, ignoreCase = true) == true }
}
