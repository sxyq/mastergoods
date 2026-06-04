package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import com.zhihuiji.feature.customers.CustomerListScreen
import com.zhihuiji.feature.products.ProductListScreen
import com.zhihuiji.feature.suppliers.SupplierListScreen

private const val TAB_PRODUCTS = "商品"
private const val TAB_CUSTOMERS = "客户"
private const val TAB_SUPPLIERS = "供应商"

@Composable
fun ArchivesScreen(
    initialTab: Int = 0,
    reselectSignal: Int = 0,
    onNavigateToProductDetail: (Long) -> Unit = {},
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
                1 -> customerScrollToTopSignal++
                2 -> supplierScrollToTopSignal++
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val currentTabTitle = tabs.getOrElse(selectedTab) { TAB_PRODUCTS }
        val currentTabIcon = when (selectedTab) {
            0 -> Icons.Default.Inventory2
            1 -> Icons.Default.Groups
            else -> Icons.Default.LocalShipping
        }
        GlassTopBar(
            title = "智慧记",
            actions = {
                ArchiveTopBarAction(
                    icon = currentTabIcon,
                    contentDescription = currentTabTitle,
                    onClick = {},
                )
                ArchiveTopBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "新增$currentTabTitle",
                    onClick = {
                        when (selectedTab) {
                            0 -> onNavigateToProductEditor(null)
                            1 -> onNavigateToCustomerEditor(null)
                            else -> onNavigateToSupplierEditor(null)
                        }
                    },
                )
            },
        )
        Text(
            text = currentTabTitle,
            style = ZhihuijiTypography.titleMedium,
            color = ZhihuijiColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
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
                    onNavigateToDetail = onNavigateToProductDetail,
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

@Composable
private fun ArchiveTopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ZhihuijiColors.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
