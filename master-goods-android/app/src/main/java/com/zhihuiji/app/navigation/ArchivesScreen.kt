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

@Composable
fun ArchivesScreen(
    initialTab: Int = 0,
    onNavigateToProductEditor: (Long?) -> Unit = {},
    onNavigateToCustomerEditor: (Long?) -> Unit = {},
    onNavigateToCustomerDetail: (Long) -> Unit = {},
    onNavigateToSupplierEditor: (Long?) -> Unit = {},
    onNavigateToSupplierDetail: (Long) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val tabs = listOf("商品", "客户", "供应商")

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
                0 -> ProductListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    onNavigateToEditor = onNavigateToProductEditor,
                )
                1 -> CustomerListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    onNavigateToEditor = onNavigateToCustomerEditor,
                    onNavigateToDetail = onNavigateToCustomerDetail,
                )
                2 -> SupplierListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    onNavigateToEditor = onNavigateToSupplierEditor,
                    onNavigateToDetail = onNavigateToSupplierDetail,
                )
            }
        }
    }
}
