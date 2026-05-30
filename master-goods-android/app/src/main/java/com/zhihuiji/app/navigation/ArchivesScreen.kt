package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import com.zhihuiji.feature.customers.CustomerListScreen
import com.zhihuiji.feature.products.ProductListScreen
import com.zhihuiji.feature.suppliers.SupplierListScreen
import androidx.compose.ui.unit.dp

private const val TAB_PRODUCTS = "商品"
private const val TAB_CUSTOMERS = "客户"
private const val TAB_SUPPLIERS = "供应商"

@Composable
fun ArchivesScreen(
    initialTab: Int = 0,
    reselectSignal: Int = 0,
    onNavigateToProductEditor: (Long?) -> Unit = {},
    onNavigateToCustomerEditor: (Long?) -> Unit = {},
    onNavigateToCustomerDetail: (Long) -> Unit = {},
    onNavigateToSupplierEditor: (Long?) -> Unit = {},
    onNavigateToSupplierDetail: (Long) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var productScrollToTopSignal by rememberSaveable { mutableIntStateOf(0) }
    var customerScrollToTopSignal by rememberSaveable { mutableIntStateOf(0) }
    var supplierScrollToTopSignal by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(TAB_PRODUCTS, TAB_CUSTOMERS, TAB_SUPPLIERS)

    LaunchedEffect(reselectSignal) {
        if (reselectSignal > 0) {
            when (selectedTab) {
                0 -> productScrollToTopSignal++
                1, 2 -> {
                    selectedTab = 0
                    productScrollToTopSignal++
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("智慧记", style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextPrimary)
            Text(
                tabs[selectedTab],
                style = ZhihuijiTypography.titleMedium,
                color = ZhihuijiColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.FilterList, contentDescription = "筛选", tint = ZhihuijiColors.TextPrimary)
            }
            IconButton(
                onClick = {
                    when (selectedTab) {
                        0 -> onNavigateToProductEditor(null)
                        1 -> onNavigateToCustomerEditor(null)
                        2 -> onNavigateToSupplierEditor(null)
                    }
                },
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "新增", tint = ZhihuijiColors.TextPrimary)
            }
        }
        SegmentedTabs(
            tabs = tabs,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> key(0) { ProductListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    scrollToTopSignal = productScrollToTopSignal,
                    onNavigateToEditor = onNavigateToProductEditor,
                ) }
                1 -> key(1) { CustomerListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    scrollToTopSignal = customerScrollToTopSignal,
                    onNavigateToEditor = onNavigateToCustomerEditor,
                    onNavigateToDetail = onNavigateToCustomerDetail,
                ) }
                2 -> key(2) { SupplierListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    scrollToTopSignal = supplierScrollToTopSignal,
                    onNavigateToEditor = onNavigateToSupplierEditor,
                    onNavigateToDetail = onNavigateToSupplierDetail,
                ) }
            }
        }
    }
}
