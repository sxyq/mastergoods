package com.zhihuiji.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.*
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import kotlin.math.abs

@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToAgent: () -> Unit,
    showTopBar: Boolean = true,
    reselectSignal: Int = 0,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFocus by rememberSaveable { mutableIntStateOf(0) }
    val salesAmount = uiState.totalSalesAmount
    val unpaidAmount = uiState.totalUnpaidAmount
    val accountBalance = uiState.totalAccountBalance
    val lowStockCount = uiState.lowStockCount
    val error = uiState.error
    val reminderItems = remember(uiState.saleOrders, uiState.lowStockProducts, unpaidAmount) {
        listOf(
            DashboardReminder(
                title = "销售单概览",
                subtitle = "${uiState.saleOrders.size} 张销售单已进入当前快照",
                statusText = if (uiState.saleOrders.isEmpty()) "暂无单据" else "本地聚合",
                tone = if (uiState.saleOrders.isEmpty()) PillTone.NEUTRAL else PillTone.INFO,
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconTint = ZhihuijiColors.InfoBlue,
            ),
            DashboardReminder(
                title = "待收款客户",
                subtitle = "待收金额 ${MoneyFormatter.format(unpaidAmount)}",
                statusText = if (unpaidAmount > 0.0) "跟进中" else "已清",
                tone = if (unpaidAmount > 0.0) PillTone.DANGER else PillTone.SUCCESS,
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = ZhihuijiColors.Warning,
            ),
            DashboardReminder(
                title = "低库存商品",
                subtitle = "${uiState.lowStockProducts.size} 个商品低于安全库存",
                statusText = if (uiState.lowStockProducts.isEmpty()) "正常" else "预警",
                tone = if (uiState.lowStockProducts.isEmpty()) PillTone.SUCCESS else PillTone.WARNING,
                icon = Icons.Default.WarningAmber,
                iconTint = ZhihuijiColors.Warning,
            ),
        )
    }
    val receivableOrders = remember(uiState.saleOrders, query) {
        uiState.saleOrders
            .asSequence()
            .filter { it.totalAmount - it.paidAmount > 0.009 }
            .filter { matchesQuery(query, it.orderNo, it.customerName) }
            .sortedByDescending { it.totalAmount - it.paidAmount }
            .take(4)
            .toList()
    }
    val filteredLowStockProducts = remember(uiState.lowStockProducts, query) {
        uiState.lowStockProducts.filter { matchesQuery(query, it.code, it.name, it.categoryName, it.unitName) }
    }
    val focusChips = remember { listOf("总览", "待办提醒", "库存预警", "回款重点") }
    val visibleReminders = remember(reminderItems, query) {
        reminderItems.filter { matchesQuery(query, it.title, it.subtitle, it.statusText) }
    }

    BottomBarScrollVisibilityEffect(listState)
    BottomBarScrollToTopEffect(reselectSignal, listState)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "top_bar") {
            if (showTopBar) {
                GlassTopBar(
                    title = "智慧记",
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = ZhihuijiColors.TextPrimary)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", tint = ZhihuijiColors.TextPrimary)
                        }
                    },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("智慧记", style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextPrimary)
                        Text("当前经营概览", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                    }
                    Row {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = ZhihuijiColors.TextSecondary)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", tint = ZhihuijiColors.TextSecondary)
                        }
                    }
                }
            }
        }
        item(key = "top_spacing") { Spacer(modifier = Modifier.height(10.dp)) }

        item(key = "overview") {
            DashboardOverviewCard(
                salesAmount = salesAmount,
                unpaidAmount = unpaidAmount,
                lowStockCount = lowStockCount,
                accountBalance = accountBalance,
                isLoading = uiState.isLoading,
            )
        }
        item(key = "overview_spacing") { Spacer(modifier = Modifier.height(10.dp)) }

        item(key = "filters") {
            SearchFilterBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "搜索提醒、客户或商品",
                filterIcon = Icons.Default.Tune,
                onFilterClick = { selectedFocus = (selectedFocus + 1) % focusChips.size },
                modifier = Modifier.fillMaxWidth(),
            )
            FilterChipRow(
                chips = focusChips,
                selectedIndex = selectedFocus,
                onChipSelected = { selectedFocus = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "切换当前看板关注视角；当前仅汇总销售单、账户余额和低库存商品三类已接入数据。",
                style = ZhihuijiTypography.labelSmall,
                color = ZhihuijiColors.TextSecondary,
            )
        }
        item(key = "filters_spacing") { Spacer(modifier = Modifier.height(8.dp)) }

        if (error != null) {
            item(key = "error") {
                DashboardMessageCard(
                    icon = Icons.Default.WarningAmber,
                    title = "数据加载存在缺口",
                    message = error.text,
                    tone = PillTone.DANGER,
                )
            }
            item(key = "error_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
        }

        item(key = "kpis_top") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "销售快照",
                    value = MoneyFormatter.format(salesAmount),
                    subtitle = "当前数据快照",
                    icon = Icons.Default.Sell,
                    tone = KpiTone.PRIMARY,
                    modifier = Modifier.weight(1f),
                )
                KpiCard(
                    title = "待收款",
                    value = MoneyFormatter.format(unpaidAmount),
                    subtitle = "优先跟进老客户",
                    icon = Icons.Default.AccountBalanceWallet,
                    tone = KpiTone.WARNING,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "kpis_top_spacing") { Spacer(modifier = Modifier.height(10.dp)) }
        item(key = "kpis_bottom") {
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
                    title = "账户余额",
                    value = MoneyFormatter.format(accountBalance),
                    subtitle = "按账户余额汇总",
                    icon = Icons.Default.AccountBalanceWallet,
                    tone = KpiTone.SUCCESS,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "kpis_bottom_spacing") { Spacer(modifier = Modifier.height(12.dp)) }

        if (selectedFocus == 0 || selectedFocus == 1) {
            item(key = "reminders_header") {
                DashboardSectionHeader(
                    title = "待处理提醒",
                    actionText = "${reminderItems.count { it.tone != PillTone.SUCCESS }} 项需关注",
                )
            }
            item(key = "reminders_spacing") { Spacer(modifier = Modifier.height(8.dp)) }
            if (visibleReminders.isEmpty()) {
                item(key = "reminders_empty") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Default.NotificationsNone,
                            title = "没有匹配的待办",
                            subtitle = "可以换个关键词或直接查看总览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
            } else {
                items(
                    items = visibleReminders,
                    key = { reminder -> reminder.title },
                ) { reminder ->
                    BusinessListItem(
                        title = reminder.title,
                        subtitle = reminder.subtitle,
                        statusText = reminder.statusText,
                        statusTone = reminder.tone,
                        icon = reminder.icon,
                        iconTint = reminder.iconTint,
                    )
                }
            }
            item(key = "reminders_footer_spacing") { Spacer(modifier = Modifier.height(12.dp)) }
        }

        if (selectedFocus == 0 || selectedFocus == 3) {
            item(key = "receivables_header") {
                DashboardSectionHeader(
                    title = "资金与回款",
                    actionText = if (receivableOrders.isEmpty()) "暂无欠款" else "${receivableOrders.size} 个重点客户",
                )
            }
            item(key = "receivables_spacing") { Spacer(modifier = Modifier.height(8.dp)) }
            if (receivableOrders.isEmpty()) {
                item(key = "receivables_empty") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "当前没有待收款订单",
                            subtitle = "收款完成后这里会自动回落为空态",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
            } else {
                items(
                    items = receivableOrders,
                    key = { order -> order.id },
                ) { order ->
                    val receivableAmount = order.totalAmount - order.paidAmount
                    BusinessListItem(
                        title = order.customerName?.takeIf { it.isNotBlank() } ?: order.orderNo,
                        subtitle = "订单 ${order.orderNo}",
                        meta = "已收 ${MoneyFormatter.format(order.paidAmount)} / 应收 ${MoneyFormatter.format(order.totalAmount)}",
                        amount = MoneyFormatter.format(receivableAmount),
                        amountColor = ZhihuijiColors.Danger,
                        statusText = if (receivableAmount > order.totalAmount * 0.5) "优先跟进" else "待回款",
                        statusTone = if (receivableAmount > order.totalAmount * 0.5) PillTone.DANGER else PillTone.WARNING,
                        icon = Icons.Default.Groups2,
                        iconTint = ZhihuijiColors.Primary,
                    )
                }
            }
            item(key = "receivables_footer_spacing") { Spacer(modifier = Modifier.height(12.dp)) }
        }

        item(key = "quick_actions_header") {
            DashboardSectionHeader(title = "快捷开单", actionText = "常用入口")
        }
        item(key = "quick_actions_spacing") { Spacer(modifier = Modifier.height(8.dp)) }
        item(key = "quick_actions_card") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(text = "销售开单", onClick = onNavigateToSales, modifier = Modifier.weight(1f), icon = Icons.Default.Sell)
                        SecondaryOutlineButton(text = "商品档案", onClick = onNavigateToProducts, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryOutlineButton(text = "客户跟进", onClick = onNavigateToCustomers, modifier = Modifier.weight(1f))
                        SecondaryOutlineButton(text = "AI 助手", onClick = onNavigateToAgent, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "quick_actions_footer_spacing") { Spacer(modifier = Modifier.height(12.dp)) }

        if (selectedFocus == 0 || selectedFocus == 2) {
            item(key = "low_stock_header") {
                DashboardSectionHeader(
                    title = "低库存预警",
                    actionText = if (filteredLowStockProducts.isEmpty()) "库存平稳" else "共 ${filteredLowStockProducts.size} 个商品",
                )
            }
            item(key = "low_stock_spacing") { Spacer(modifier = Modifier.height(8.dp)) }
            if (filteredLowStockProducts.isEmpty()) {
                item(key = "low_stock_empty") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Default.Inventory2,
                            title = if (uiState.lowStockProducts.isEmpty()) "暂无低库存预警" else "未找到匹配商品",
                            subtitle = if (uiState.lowStockProducts.isEmpty()) "当前商品库存都高于安全线" else "换个关键词试试，或回到总览查看全部预警",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
            } else {
                items(
                    items = filteredLowStockProducts.take(6),
                    key = { item -> item.id },
                ) { item ->
                    LowStockListItem(item)
                }
            }
            item(key = "low_stock_footer_spacing") { Spacer(modifier = Modifier.height(12.dp)) }
        }

        item(key = "summary_footer") {
            SummaryFooter(
                leftText = "看板数据基于客户端本地汇总快照",
                rightText = if (uiState.isLoading) "刷新中" else "趋势与通知待联调",
            )
        }
        item(key = "bottom_spacing") { Spacer(modifier = Modifier.height(88.dp)) }
    }
}

@Composable
private fun DashboardOverviewCard(
    salesAmount: Double,
    unpaidAmount: Double,
    lowStockCount: Int,
    accountBalance: Double,
    isLoading: Boolean,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("经营看板", style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                    Text("聚焦销售、库存与回款重点，方便快速扫读当前已接入的经营数据", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                }
                StatusPill(
                    text = if (isLoading) "刷新中" else "本地快照",
                    tone = if (isLoading) PillTone.INFO else PillTone.SUCCESS,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(text = "${lowStockCount} 个库存预警", tone = if (lowStockCount > 0) PillTone.WARNING else PillTone.SUCCESS)
                StatusPill(text = if (unpaidAmount > 0) "待收款跟进" else "回款平稳", tone = if (unpaidAmount > 0) PillTone.DANGER else PillTone.SUCCESS)
            }
            FieldRow(label = "销售额", value = MoneyFormatter.format(salesAmount), valueColor = ZhihuijiColors.Primary)
            FieldRow(label = "账户余额", value = MoneyFormatter.format(accountBalance), valueColor = ZhihuijiColors.Success)
            FieldRow(
                label = "数据说明",
                value = "当前页仅汇总销售单、账户余额与低库存接口；趋势、通知与更多联动能力仍待后续接入。",
                valueColor = ZhihuijiColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    actionText: String,
) {
    SectionHeader(title = title, actionText = actionText)
}

@Composable
private fun DashboardMessageCard(
    icon: ImageVector,
    title: String,
    message: String,
    tone: PillTone,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = toneColor(tone))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                Text(message, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
            }
            StatusPill(text = "需复查", tone = tone)
        }
    }
}

@Composable
private fun LowStockListItem(item: ProductV2Dto) {
    val shortage = (item.safeStock - item.stock).coerceAtLeast(0.0)
    BusinessListItem(
        title = item.name,
        subtitle = item.code,
        meta = "库存 ${MoneyFormatter.formatWithoutSymbol(item.stock)} / 安全库存 ${MoneyFormatter.formatWithoutSymbol(item.safeStock)}",
        amount = "缺口 ${MoneyFormatter.formatWithoutSymbol(shortage)}",
        amountColor = ZhihuijiColors.Warning,
        statusText = StatusLabels.stockStatus(item.stock, item.safeStock),
        statusTone = when {
            item.stock <= 0.000001 -> PillTone.DANGER
            item.stock < item.safeStock -> PillTone.WARNING
            else -> PillTone.SUCCESS
        },
        icon = Icons.Default.Inventory2,
        iconTint = ZhihuijiColors.Warning,
    )
}

private data class DashboardReminder(
    val title: String,
    val subtitle: String,
    val statusText: String,
    val tone: PillTone,
    val icon: ImageVector,
    val iconTint: Color,
)

private fun matchesQuery(query: String, vararg values: String?): Boolean {
    if (query.isBlank()) return true
    val keyword = query.trim()
    return values.any { value -> value?.contains(keyword, ignoreCase = true) == true }
}

private fun toneColor(tone: PillTone): Color = when (tone) {
    PillTone.SUCCESS -> ZhihuijiColors.Success
    PillTone.WARNING -> ZhihuijiColors.Warning
    PillTone.DANGER -> ZhihuijiColors.Danger
    PillTone.INFO -> ZhihuijiColors.InfoBlue
    PillTone.NEUTRAL -> ZhihuijiColors.TextTertiary
}
