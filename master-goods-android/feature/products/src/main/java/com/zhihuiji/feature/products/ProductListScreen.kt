package com.zhihuiji.feature.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
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
fun ProductListScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    scrollToTopSignal: Int = 0,
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToEditor: (Long?) -> Unit = {},
    viewModel: ProductListViewModel = hiltViewModel(),
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
                GlassTopBar(
                    title = "商品", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack,
                )
            }
            SearchFilterBar(
                query = uiState.keyword, onQueryChange = { viewModel.loadProducts(it) },
                placeholder = "搜索商品名称/编码", filterIcon = androidx.compose.material.icons.Icons.Default.Inventory2,
                onFilterClick = {},
            )
            SegmentedTabs(
                tabs = listOf("全部", "低库存", "正常", "缺货"),
                selectedIndex = uiState.stockFilter,
                onTabSelected = { viewModel.setStockFilter(it) },
            )
            if (uiState.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(icon = Icons.Default.Inventory2, title = "暂无商品", modifier = Modifier.fillMaxWidth())
                }
            } else {
                val filteredProducts = uiState.filteredProducts
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("共 ${filteredProducts.size} 个商品", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                            Text("默认排序", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                        }
                    }
                    items(filteredProducts) { product ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToDetail(product.id) }) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(ZhihuijiColors.PressedBlue, RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Inventory2, null, tint = ZhihuijiColors.Primary, modifier = Modifier.size(24.dp))
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(product.code, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                    Text(product.name, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("库存 ${MoneyFormatter.formatWithoutSymbol(product.stock)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                                        Text("安全库存 ${MoneyFormatter.formatWithoutSymbol(product.safeStock)}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(MoneyFormatter.format(product.salePrice), style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                    Spacer(Modifier.height(8.dp))
                                    val stockTone = when {
                                        product.stock <= 0.000001 -> PillTone.DANGER
                                        product.stock < product.safeStock -> PillTone.WARNING
                                        else -> PillTone.SUCCESS
                                    }
                                    StatusPill(text = StatusLabels.stockStatus(product.stock, product.safeStock), tone = stockTone)
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
                    text = "新增商品",
                    icon = Icons.Default.Add,
                    onClick = { onNavigateToEditor(null) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}
