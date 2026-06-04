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
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import com.zhihuiji.feature.finance.FinanceRecordListScreen
import com.zhihuiji.feature.payments.PayOrderListScreen
import com.zhihuiji.feature.purchases.PurchaseOrderListScreen
import com.zhihuiji.feature.sales.SaleOrderListScreen
import androidx.compose.material3.Text

private const val TAB_SALES = "销售单"
private const val TAB_PURCHASES = "采购单"
private const val TAB_PAYMENTS = "付款单"
private const val TAB_FINANCE = "资金流水"

@Composable
fun DocumentsScreen(
    initialTab: Int = 0,
    reselectSignal: Int = 0,
    onNavigateToSaleOrderEditor: () -> Unit = {},
    onNavigateToSaleOrderDetail: (Long) -> Unit = {},
    onNavigateToPurchaseOrderEditor: () -> Unit = {},
    onNavigateToPurchaseOrderDetail: (Long) -> Unit = {},
    onNavigateToPayOrderEditor: () -> Unit = {},
    onNavigateToPayOrderDetail: (Long) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var saleScrollToTopSignal by rememberSaveable { mutableIntStateOf(0) }
    var purchaseScrollToTopSignal by rememberSaveable { mutableIntStateOf(0) }
    var payScrollToTopSignal by rememberSaveable { mutableIntStateOf(0) }
    var financeScrollToTopSignal by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(TAB_SALES, TAB_PURCHASES, TAB_PAYMENTS, TAB_FINANCE)

    LaunchedEffect(reselectSignal) {
        if (reselectSignal > 0) {
            when (selectedTab) {
                0 -> saleScrollToTopSignal++
                1 -> purchaseScrollToTopSignal++
                2 -> payScrollToTopSignal++
                3 -> financeScrollToTopSignal++
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val currentTabTitle = tabs.getOrElse(selectedTab) { TAB_SALES }
        val currentTabIcon = when (selectedTab) {
            0 -> Icons.Default.Sell
            1 -> Icons.Default.ShoppingCart
            2 -> Icons.Default.Wallet
            else -> Icons.Default.ReceiptLong
        }
        GlassTopBar(
            title = "智慧记",
            actions = {
                DocumentTopBarAction(
                    icon = currentTabIcon,
                    contentDescription = currentTabTitle,
                    onClick = {},
                )
                DocumentTopBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "新增$currentTabTitle",
                    onClick = {
                        when (selectedTab) {
                            0 -> onNavigateToSaleOrderEditor()
                            1 -> onNavigateToPurchaseOrderEditor()
                            2 -> onNavigateToPayOrderEditor()
                        }
                    },
                    enabled = selectedTab != 3,
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
                0 -> key(0) { SaleOrderListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    scrollToTopSignal = saleScrollToTopSignal,
                    onNavigateToEditor = onNavigateToSaleOrderEditor,
                    onNavigateToDetail = onNavigateToSaleOrderDetail,
                ) }
                1 -> key(1) { PurchaseOrderListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    scrollToTopSignal = purchaseScrollToTopSignal,
                    onNavigateToEditor = onNavigateToPurchaseOrderEditor,
                    onNavigateToDetail = onNavigateToPurchaseOrderDetail,
                ) }
                2 -> key(2) { PayOrderListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    scrollToTopSignal = payScrollToTopSignal,
                    onNavigateToEditor = onNavigateToPayOrderEditor,
                    onNavigateToDetail = onNavigateToPayOrderDetail,
                ) }
                3 -> key(3) { FinanceRecordListScreen(
                    onNavigateBack = {},
                    showTopBar = false,
                    scrollToTopSignal = financeScrollToTopSignal,
                ) }
            }
        }
    }
}

@Composable
private fun DocumentTopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) ZhihuijiColors.TextPrimary else ZhihuijiColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}
