package com.zhihuiji.feature.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
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
fun SupplierListScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    scrollToTopSignal: Int = 0,
    onNavigateToEditor: (Long?) -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: SupplierViewModel = hiltViewModel(),
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
                GlassTopBar(title = "供应商", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            }
            SegmentedTabs(tabs = listOf("全部", "启用", "停用"), selectedIndex = when(uiState.statusFilter) { 1 -> 1; 0 -> 2; else -> 0 }, onTabSelected = { viewModel.changeStatusFilter(when(it) { 1 -> 1; 2 -> 0; else -> null }) })
            SearchFilterBar(query = uiState.keyword, onQueryChange = { viewModel.loadSuppliers(keyword = it) }, placeholder = "搜索供应商名称/手机号", filterIcon = Icons.Default.LocalShipping, onFilterClick = {})
            if (uiState.suppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(icon = Icons.Default.LocalShipping, title = "暂无供应商", modifier = Modifier.fillMaxWidth())
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
                    items(uiState.suppliers) { supplier ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToDetail(supplier.id) }) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(supplier.name, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                        Text("S${supplier.id}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                                    }
                                    Text("联系人：${supplier.primaryContactName ?: supplier.name}  ${supplier.primaryContactPhone ?: supplier.phone}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                    Text("应付余额 ${MoneyFormatter.format(supplier.balance)}", style = ZhihuijiTypography.labelSmall, color = if (supplier.balance > 0) ZhihuijiColors.Danger else ZhihuijiColors.TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusPill(text = StatusLabels.supplierStatus(supplier.status), tone = if (supplier.status == 1) PillTone.SUCCESS else PillTone.NEUTRAL)
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
                    text = "新增供应商",
                    icon = Icons.Default.Add,
                    onClick = { onNavigateToEditor(null) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}
