package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import com.zhihuiji.feature.finance.FinanceRecordListScreen
import com.zhihuiji.feature.payments.PayOrderListScreen
import com.zhihuiji.feature.purchases.PurchaseOrderListScreen
import com.zhihuiji.feature.sales.SaleOrderListScreen
import androidx.compose.ui.unit.dp

@Composable
fun DocumentsScreen(
    initialTab: Int = 0,
    onNavigateToSaleOrderEditor: () -> Unit = {},
    onNavigateToSaleOrderDetail: (Long) -> Unit = {},
    onNavigateToPurchaseOrderEditor: () -> Unit = {},
    onNavigateToPurchaseOrderDetail: (Long) -> Unit = {},
    onNavigateToPayOrderEditor: () -> Unit = {},
    onNavigateToPayOrderDetail: (Long) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val tabs = listOf("销售单", "采购单", "付款单", "资金流水")

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
                Icon(Icons.Default.Search, contentDescription = "搜索", tint = ZhihuijiColors.TextPrimary)
            }
            IconButton(
                onClick = {
                    when (selectedTab) {
                        0 -> onNavigateToSaleOrderEditor()
                        1 -> onNavigateToPurchaseOrderEditor()
                        2 -> onNavigateToPayOrderEditor()
                    }
                },
                enabled = selectedTab != 3,
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增", tint = if (selectedTab == 3) ZhihuijiColors.TextTertiary else ZhihuijiColors.TextPrimary)
            }
        }
        SegmentedTabs(
            tabs = tabs,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SaleOrderListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    onNavigateToEditor = onNavigateToSaleOrderEditor,
                    onNavigateToDetail = onNavigateToSaleOrderDetail,
                )
                1 -> PurchaseOrderListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    onNavigateToEditor = onNavigateToPurchaseOrderEditor,
                    onNavigateToDetail = onNavigateToPurchaseOrderDetail,
                )
                2 -> PayOrderListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    onNavigateToEditor = onNavigateToPayOrderEditor,
                    onNavigateToDetail = onNavigateToPayOrderDetail,
                )
                3 -> FinanceRecordListScreen(onNavigateBack = {}, showTopBar = false)
            }
        }
    }
}
