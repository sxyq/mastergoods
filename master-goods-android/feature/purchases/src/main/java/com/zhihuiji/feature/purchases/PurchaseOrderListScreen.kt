package com.zhihuiji.feature.purchases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
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
fun PurchaseOrderListScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    scrollToTopSignal: Int = 0,
    onNavigateToEditor: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: PurchaseOrderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    BottomBarScrollVisibilityEffect(listState)
    BottomBarScrollToTopEffect(scrollToTopSignal, listState)

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTopBar) {
                GlassTopBar(title = "采购单", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            }
            SearchFilterBar(query = uiState.filter.keyword ?: "", onQueryChange = { viewModel.loadOrders(uiState.filter.copy(keyword = it.ifBlank { null })) }, placeholder = "搜索单号/供应商/商品", filterIcon = Icons.Default.Tune, onFilterClick = {})
            SegmentedTabs(tabs = listOf("全部", "草稿", "已收货"), selectedIndex = when(uiState.filter.status) { 0 -> 1; 1 -> 2; else -> 0 }, onTabSelected = { viewModel.loadOrders(uiState.filter.copy(status = if(it == 0) null else it - 1)) })
            if (uiState.orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(icon = Icons.Default.ShoppingCart, title = "暂无采购单", modifier = Modifier.fillMaxWidth())
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                ) {
                    items(uiState.orders) { order ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToDetail(order.id) }) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(order.orderNo, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                                    StatusPill(text = StatusLabels.purchaseOrderStatus(order.status), tone = if(order.status == 1) PillTone.SUCCESS else PillTone.WARNING)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("供应商：${order.supplierName ?: ""}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                    Text("金额：${MoneyFormatter.format(order.totalAmount)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                }
                                Text("日期：${TimeFormatter.formatDateTime(order.createdAt)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                            }
                        }
                    }
                }
            }
        }
        if (showTopBar) {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingPrimaryActionButton(
                    text = "采购开单",
                    icon = Icons.Default.Add,
                    onClick = onNavigateToEditor,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}
