package com.zhihuiji.feature.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
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
import com.zhihuiji.core.designsystem.*

@Composable
fun CustomerListScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    onNavigateToEditor: (Long?) -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: CustomerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().then(if (showTopBar) Modifier.glassBackground() else Modifier)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTopBar) {
                GlassTopBar(title = "客户", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            }
            SearchFilterBar(query = uiState.keyword, onQueryChange = { viewModel.loadCustomers(it) }, placeholder = "搜索客户名称/手机号", filterIcon = Icons.Default.People, onFilterClick = {})
            SegmentedTabs(
                tabs = listOf("全部", "正常", "欠款", "已停用"),
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
            )
            if (uiState.customers.isEmpty()) {
                EmptyState(icon = Icons.Default.People, title = "暂无客户", modifier = Modifier.fillMaxSize().align(Alignment.CenterHorizontally))
            } else {
                val filteredCustomers = uiState.customers.filter { customer ->
                    when (selectedTab) {
                        1 -> customer.status == 1
                        2 -> customer.balance > 0
                        3 -> customer.status != 1
                        else -> true
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                ) {
                    items(filteredCustomers) { customer ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { customer.id?.let { onNavigateToDetail(it) } }) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(customer.name, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                        Text("C${customer.id ?: 0}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                                    }
                                    Text("联系人：${customer.name}  ${customer.phone}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                    Text("应收余额 ${MoneyFormatter.format(customer.balance)}", style = ZhihuijiTypography.labelSmall, color = if (customer.balance > 0) ZhihuijiColors.Danger else ZhihuijiColors.TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusPill(text = StatusLabels.supplierStatus(customer.status), tone = if (customer.status == 1) PillTone.SUCCESS else PillTone.NEUTRAL)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showTopBar) {
            FloatingActionButton(onClick = { onNavigateToEditor(null) }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = ZhihuijiColors.Primary) {
                Icon(Icons.Default.Add, null, tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}
