package com.zhihuiji.feature.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.*

@Composable
fun CustomerListScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    scrollToTopSignal: Int = 0,
    onNavigateToEditor: (Long?) -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: CustomerViewModel = hiltViewModel(),
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
                GlassTopBar(title = "客户", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            }
            SearchFilterBar(query = uiState.keyword, onQueryChange = { viewModel.loadCustomers(it) }, placeholder = "搜索客户名称/手机号", filterIcon = Icons.Default.People, onFilterClick = {})
            SegmentedTabs(
                tabs = listOf("全部", "正常", "欠款", "已停用"),
                selectedIndex = uiState.statusFilter,
                onTabSelected = { viewModel.setStatusFilter(it) },
            )
            if (uiState.filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(icon = Icons.Default.People, title = "暂无客户", modifier = Modifier.fillMaxWidth())
                }
            } else {
                val filteredCustomers = uiState.filteredCustomers
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                ) {
                    items(filteredCustomers) { customer ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToDetail(customer.id) }) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(customer.name, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                        Text("C${customer.id}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                                    }
                                    Text("联系人：${customer.primaryContactName ?: customer.name}  ${customer.primaryContactPhone ?: customer.phone}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                    Text("应收余额 ${MoneyFormatter.format(customer.balance)}", style = ZhihuijiTypography.labelSmall, color = if (customer.balance > 0) ZhihuijiColors.Danger else ZhihuijiColors.TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusPill(
                                        text = StatusLabels.customerListStatus(customer.status, customer.balance),
                                        tone = customerStatusTone(customer.status, customer.balance),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showTopBar) {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingPrimaryActionButton(
                    text = "新增客户",
                    icon = Icons.Default.Add,
                    onClick = { onNavigateToEditor(null) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}

private fun customerStatusTone(status: Int, balance: Double): PillTone = when {
    status == StatusLabels.Codes.CUSTOMER_STATUS_DISABLED -> PillTone.NEUTRAL
    balance > 0.0 -> PillTone.DANGER
    status == StatusLabels.Codes.CUSTOMER_STATUS_ACTIVE -> PillTone.SUCCESS
    else -> PillTone.NEUTRAL
}
