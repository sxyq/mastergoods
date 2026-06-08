package com.zhihuiji.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.feature.finance.FinanceRecordListScreen
import com.zhihuiji.feature.payments.PayOrderListScreen
import com.zhihuiji.feature.purchases.PurchaseOrderListScreen
import com.zhihuiji.feature.sales.SaleOrderListScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentsScreen(
    initialTab: Int = 0,
    onNavigateToSaleOrderDetail: (Long) -> Unit,
    onNavigateToSaleOrderCreate: () -> Unit,
    onNavigateToSalesReturns: () -> Unit,
    onNavigateToPurchaseOrderDetail: (Long) -> Unit,
    onNavigateToPurchaseOrderCreate: () -> Unit,
    onNavigateToPurchaseReceipts: () -> Unit,
    onNavigateToPurchaseReturns: () -> Unit,
    onNavigateToPayOrderDetail: (Long) -> Unit,
    onNavigateToFinanceRecordDetail: (Long) -> Unit,
    onNavigateToDailyExpense: () -> Unit,
    onNavigateToInventorySnapshot: () -> Unit,
) {
    val tabs = listOf("销售单", "采购单", "付款单", "资金流水")
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(tabs.indices),
        pageCount = { tabs.size },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        DocumentsTopAppBar()
        DocumentsTabBar(
            tabs = tabs,
            pagerState = pagerState,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                DocumentsPageContent(
                    page = page,
                    onNavigateToSaleOrderDetail = onNavigateToSaleOrderDetail,
                    onNavigateToSaleOrderCreate = onNavigateToSaleOrderCreate,
                    onNavigateToPurchaseOrderDetail = onNavigateToPurchaseOrderDetail,
                    onNavigateToPurchaseOrderCreate = onNavigateToPurchaseOrderCreate,
                    onNavigateToPayOrderDetail = onNavigateToPayOrderDetail,
                    onNavigateToFinanceRecordDetail = onNavigateToFinanceRecordDetail,
                    onNavigateToDailyExpense = onNavigateToDailyExpense,
                )
            }
        }
    }

}

@Composable
private fun DocumentsPageContent(
    page: Int,
    onNavigateToSaleOrderDetail: (Long) -> Unit,
    onNavigateToSaleOrderCreate: () -> Unit,
    onNavigateToPurchaseOrderDetail: (Long) -> Unit,
    onNavigateToPurchaseOrderCreate: () -> Unit,
    onNavigateToPayOrderDetail: (Long) -> Unit,
    onNavigateToFinanceRecordDetail: (Long) -> Unit,
    onNavigateToDailyExpense: () -> Unit,
) {
    when (page) {
        0 -> SaleOrderListScreen(
            onNavigateToDetail = onNavigateToSaleOrderDetail,
            onNavigateToCreate = onNavigateToSaleOrderCreate,
        )
        1 -> PurchaseOrderListScreen(
            onNavigateToDetail = onNavigateToPurchaseOrderDetail,
            onNavigateToCreate = onNavigateToPurchaseOrderCreate,
        )
        2 -> PayOrderListScreen(
            onNavigateToDetail = onNavigateToPayOrderDetail,
        )
        3 -> FinanceRecordListScreen(
            onNavigateToDetail = onNavigateToFinanceRecordDetail,
            onNavigateToDailyExpense = onNavigateToDailyExpense,
        )
    }
}

@Composable
private fun DocumentsTopAppBar(
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        surfaceColor = GlassSurfaceMedium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DocumentsTopIconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "单据中心",
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            DocumentsTopIconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索单据",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun DocumentsTopIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DocumentsTabBar(
    tabs: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        surfaceColor = GlassSurfaceMedium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Text(
                            text = tab,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) ZhihuijiPrimary else TextSecondary,
                        )
                        if (selected && index == 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 1.dp)
                                    .size(6.dp)
                                    .background(ZhihuijiPrimary, RoundedCornerShape(100.dp)),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(
                                color = if (selected) ZhihuijiPrimary else Color.Transparent,
                                shape = RoundedCornerShape(100.dp),
                            )
                    )
                }
            }
        }
    }
}
