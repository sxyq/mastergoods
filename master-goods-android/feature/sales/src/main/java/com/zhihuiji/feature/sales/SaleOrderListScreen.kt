package com.zhihuiji.feature.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.*

@Composable
fun SaleOrderListScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    scrollToTopSignal: Int = 0,
    onNavigateToEditor: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: SaleOrderListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val statusTabs = listOf("全部", "待审核", "待发货", "待收款", "已完成", "已作废")

    BottomBarScrollVisibilityEffect(listState)
    BottomBarScrollToTopEffect(scrollToTopSignal, listState)

    Box(modifier = Modifier.fillMaxSize().then(if (showTopBar) Modifier.glassBackground() else Modifier)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTopBar) {
                GlassTopBar(title = "销售单", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            }
            SearchFilterBar(query = uiState.filter.keyword ?: "", onQueryChange = { viewModel.updateFilter(keyword = it.ifBlank { null }) }, placeholder = "搜索单号/客户/商品", filterIcon = Icons.Default.Tune, onFilterClick = {})
            SegmentedTabs(tabs = statusTabs, selectedIndex = when(uiState.filter.status) { 0 -> 1; 1 -> 4; 2 -> 5; else -> 0 }, onTabSelected = { viewModel.updateFilter(status = when (it) { 1 -> 0; 4 -> 1; 5 -> 2; else -> null }) })
            if (uiState.orders.isEmpty()) {
                EmptyState(icon = Icons.Default.Receipt, title = "暂无销售单", modifier = Modifier.fillMaxSize().align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                ) {
                    items(uiState.orders) { order ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToDetail(order.id) }) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(order.orderNo, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                                    StatusPill(text = StatusLabels.saleOrderStatus(order.status), tone = when(order.status) { 1 -> PillTone.SUCCESS; 2 -> PillTone.NEUTRAL; else -> PillTone.INFO })
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("客户：${order.customerName ?: "散客"}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                    Text("金额：${MoneyFormatter.format(order.totalAmount)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                }
                                Text("日期：${TimeFormatter.formatDateTime(order.createdAt)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("共 ${uiState.orders.size} 条", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                            Text(
                                "合计金额：${MoneyFormatter.format(uiState.orders.sumOf { it.totalAmount })}",
                                style = ZhihuijiTypography.labelSmall,
                                color = ZhihuijiColors.Primary,
                            )
                        }
                    }
                }
            }
        }
        if (showTopBar) {
            FloatingActionButton(onClick = onNavigateToEditor, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = ZhihuijiColors.Primary) {
                Icon(Icons.Default.Add, null, tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}
