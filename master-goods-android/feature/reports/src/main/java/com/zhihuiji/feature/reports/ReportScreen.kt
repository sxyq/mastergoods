package com.zhihuiji.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassBorder
import com.zhihuiji.core.designsystem.GlassBorderSoft
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.MainBottomBarHeight
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.StatusBlueLight
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.SurfaceGray
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import java.util.Locale

private val periodTabs = ReportPeriod.values().map { it.tabLabel }
private val ReportBottomContentExtraPadding = 96.dp

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val navigationBarPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val periodLabel = uiState.selectedPeriodLabel

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = 76.dp,
                bottom = MainBottomBarHeight + navigationBarPadding + ReportBottomContentExtraPadding
            )
        ) {
            item {
                ReportsPageTitle(periodLabel = periodLabel)
            }
            item {
                SegmentedTabs(
                    tabs = periodTabs,
                    selectedIndex = uiState.selectedPeriodIndex.coerceAtMost(periodTabs.lastIndex),
                    onTabSelected = viewModel::setPeriod
                )
            }

            if (uiState.failedReportSections.isNotEmpty()) {
                item {
                    ReportDataStatusStrip(
                        message = uiState.error,
                        failedSections = uiState.failedReportSections,
                        onRetry = { viewModel.loadReports(forcePartnerRefresh = true) }
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    LoadingReportCard()
                }
            } else {
                item {
                    ReportKpiSection(
                        salesAmount = uiState.salesAmount,
                        profitAmount = uiState.profitAmount,
                        profitRate = uiState.profitRate,
                        orderCount = uiState.orderCount,
                    )
                }
                item {
                    SalesCompositionCard(
                        salesAmount = uiState.salesAmount,
                        profitAmount = uiState.profitAmount,
                    )
                }
                item {
                    ProfitDistributionCard(
                        salesAmount = uiState.salesAmount,
                        profitAmount = uiState.profitAmount,
                        selectedPeriodLabel = periodLabel,
                    )
                }
                item {
                    FinanceCompositionCard(
                        salesAmount = uiState.salesAmount,
                        profitAmount = uiState.profitAmount,
                        receivableAmount = uiState.receivableAmount,
                        payableAmount = uiState.payableAmount,
                    )
                }
                item {
                    TopProductsCard(products = uiState.topProducts)
                }
            }
        }

        ReportsTopBar(
            periodLabel = periodLabel,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ReportsTopBar(
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    GlassTopBar(
        modifier = modifier,
        title = "经营报表",
        subtitle = "$periodLabel · 真实经营数据"
    )
}

@Composable
private fun ReportsPageTitle(
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "经营报表",
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "$periodLabel · 已接入模块基于真实接口汇总",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ReportKpiSection(
    salesAmount: String,
    profitAmount: String,
    profitRate: String,
    orderCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SalesTotalHeroCard(
            salesAmount = salesAmount,
            profitRate = profitRate,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactMetricCard(
                modifier = Modifier.weight(1f),
                label = "预估利润",
                value = "¥$profitAmount",
                accent = SuccessGreen,
                trendText = "$profitRate%",
            )
            CompactMetricCard(
                modifier = Modifier.weight(1f),
                label = "成交单量",
                value = "$orderCount",
                accent = ZhihuijiPrimary,
                trendText = "真实订单",
            )
        }
    }
}

@Composable
private fun SalesTotalHeroCard(
    salesAmount: String,
    profitRate: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(ZhihuijiPrimary.copy(alpha = 0.06f))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "销售总额 (元)",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = salesAmount,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZhihuijiPrimary,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "利润率 $profitRate%",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMetricCard(
    label: String,
    value: String,
    accent: Color,
    trendText: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f))
                )
            }
            Text(
                text = value,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = trendText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SalesCompositionCard(
    salesAmount: String,
    profitAmount: String,
    modifier: Modifier = Modifier
) {
    val sales = remember(salesAmount) { salesAmount.toMoneyDouble() }
    val profit = remember(profitAmount) { profitAmount.toMoneyDouble() }
    GlassChartCard(
        modifier = modifier,
        title = "销售趋势",
        icon = Icons.Outlined.Timeline
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryLineChart(
                sales = sales,
                profit = profit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
            )
            CompositionBar(
                label = "销售额",
                amount = sales,
                total = sales.coerceAtLeast(profit),
                color = ZhihuijiPrimary
            )
            CompositionBar(
                label = "预估利润",
                amount = profit,
                total = sales.coerceAtLeast(profit),
                color = SuccessGreen
            )
        }
    }
}

@Composable
private fun ProfitDistributionCard(
    salesAmount: String,
    profitAmount: String,
    selectedPeriodLabel: String,
    modifier: Modifier = Modifier
) {
    val sales = remember(salesAmount) { salesAmount.toMoneyDouble() }
    val profit = remember(profitAmount) { profitAmount.toMoneyDouble() }
    val items = remember(sales, profit) {
        val cost = (sales - profit).coerceAtLeast(0.0)
        listOf(
            FinanceSlice("销售额", sales, ZhihuijiPrimary),
            FinanceSlice("预估成本", cost, StatusBlueLight),
            FinanceSlice("预估利润", profit, SuccessGreen)
        )
    }
    GlassChartCard(
        modifier = modifier,
        title = "利润分布（$selectedPeriodLabel）",
        icon = Icons.Outlined.Leaderboard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { item ->
                CompositionBar(
                    label = item.label,
                    amount = item.value,
                    total = sales.coerceAtLeast(1.0),
                    color = item.color
                )
            }
        }
    }
}

@Composable
private fun FinanceCompositionCard(
    salesAmount: String,
    profitAmount: String,
    receivableAmount: String,
    payableAmount: String,
    modifier: Modifier = Modifier
) {
    val items = remember(salesAmount, profitAmount, receivableAmount, payableAmount) {
        listOf(
            FinanceSlice("销售额", salesAmount.toMoneyDouble(), ZhihuijiPrimary),
            FinanceSlice("预估利润", profitAmount.toMoneyDouble(), SuccessGreen),
            FinanceSlice("应收", receivableAmount.toMoneyDouble(), WarningOrange),
            FinanceSlice("应付", payableAmount.toMoneyDouble(), DangerRed)
        )
    }
    GlassChartCard(
        modifier = modifier,
        title = "往来余额构成",
        icon = Icons.Outlined.PieChart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DonutSummaryChart(
                items = items,
                modifier = Modifier.size(116.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    FinanceLegendItem(item = item)
                }
            }
        }
    }
}

@Composable
private fun GlassChartCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun SummaryLineChart(
    sales: Double,
    profit: Double,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(sales, profit) {
        listOf(sales, profit).maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    }
    Canvas(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val start = Offset(0f, height * 0.78f)
            val mid = Offset(width * 0.46f, height * (0.78f - 0.56f * (profit / maxValue).toFloat()))
            val end = Offset(width, height * (0.78f - 0.62f * (sales / maxValue).toFloat()))
            val curvePath = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(width * 0.24f, height * 0.48f, mid.x, mid.y)
                quadraticTo(width * 0.72f, height * 0.08f, end.x, end.y)
            }
            val baselineStrokeWidth = 1.dp.toPx()
            val lineStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            val outerRadius = 5.dp.toPx()
            val innerRadius = 3.dp.toPx()

            onDrawBehind {
                drawLine(
                    color = GlassBorderSoft,
                    start = Offset(0f, height * 0.78f),
                    end = Offset(width, height * 0.78f),
                    strokeWidth = baselineStrokeWidth
                )
                drawPath(
                    path = curvePath,
                    brush = Brush.linearGradient(listOf(ZhihuijiPrimary, StatusBlueLight)),
                    style = lineStroke
                )
                listOf(start, mid, end).forEach { point ->
                    drawCircle(color = Color.White, radius = outerRadius, center = point)
                    drawCircle(color = ZhihuijiPrimary, radius = innerRadius, center = point)
                }
            }
        }
    ) {}
}

@Composable
private fun CompositionBar(
    label: String,
    amount: Double,
    total: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (total <= 0.0) 0f else (amount / total).toFloat().coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary
            )
            Text(
                text = amount.formatMoney(),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(SurfaceGray.copy(alpha = 0.72f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun DonutSummaryChart(
    items: List<FinanceSlice>,
    modifier: Modifier = Modifier
) {
    val positiveItems = remember(items) { items.filter { it.value > 0.0 } }
    val total = remember(positiveItems) { positiveItems.sumOf { it.value } }
    Canvas(
        modifier = modifier.drawWithCache {
            val strokeWidth = 18.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val arcSweeps = positiveItems.map { item ->
                item to ((item.value / total) * 360f).toFloat()
            }

            onDrawBehind {
                if (total <= 0.0) {
                    drawCircle(
                        color = SurfaceGray,
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    return@onDrawBehind
                }
                var startAngle = -90f
                arcSweeps.forEach { (item, sweep) ->
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
        }
    ) {}
}

@Composable
private fun FinanceLegendItem(
    item: FinanceSlice,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(item.color)
            )
            Text(
                text = item.label,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary
            )
        }
        Text(
            text = item.value.formatMoney(),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun TopProductsCard(
    products: List<TopProductItem>,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Leaderboard,
                        contentDescription = null,
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Top 5 畅销商品",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ZhihuijiPrimary.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "完整榜单 ›",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZhihuijiPrimary
                    )
                }
            }

            if (products.isEmpty()) {
                Text(
                text = "当前周期暂无真实销售排行，未生成默认榜单",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                val maxAmount = products.maxOf { it.salesAmount.toMoneyDouble() }.takeIf { it > 0.0 } ?: 1.0
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    products.take(5).forEachIndexed { index, product ->
                        TopProductRankRow(
                            rank = index + 1,
                            product = product,
                            maxAmount = maxAmount
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopProductRankRow(
    rank: Int,
    product: TopProductItem,
    maxAmount: Double,
    modifier: Modifier = Modifier
) {
    val amount = product.salesAmount.toMoneyDouble()
    val fraction = (amount / maxAmount).toFloat().coerceIn(0.08f, 1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankBadge(rank = rank)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "销量 ${product.salesCount} 件",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = product.salesAmount,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(SurfaceGray.copy(alpha = 0.72f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(ZhihuijiPrimary.copy(alpha = 1f - (rank - 1) * 0.12f))
            )
        }
    }
}

@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val selected = rank == 1
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) WarningOrange.copy(alpha = 0.14f) else GlassSurfaceHigh)
            .border(0.5.dp, if (selected) WarningOrange.copy(alpha = 0.18f) else GlassBorder, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) WarningOrange else TextSecondary
        )
    }
}

@Composable
private fun LoadingReportCard(modifier: Modifier = Modifier) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = GlassSurfaceLow
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ZhihuijiPrimary)
        }
    }
}

@Composable
private fun ReportDataStatusStrip(
    message: String?,
    failedSections: List<String>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val failedSectionText = failedSections.takeIf { it.isNotEmpty() }?.joinToString("、")
    val statusTitle = failedSectionText?.let { "已显示可用数据，$it 暂未同步" } ?: "已显示可用数据"
    val statusMessage = message
        ?.takeIf { it.isNotBlank() }
        ?.let { "部分数据源同步失败，可先查看已更新内容，或点击刷新重试。" }
        ?: "部分数据源暂未同步，可先查看已更新内容，或点击刷新重试。"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(StatusBlueLight.copy(alpha = 0.08f))
            .border(0.5.dp, StatusBlueLight.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(StatusBlueLight)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = statusTitle,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "刷新",
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.62f))
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = ZhihuijiPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Immutable
private data class FinanceSlice(
    val label: String,
    val value: Double,
    val color: Color
)

private fun String.toMoneyDouble(): Double =
    filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull() ?: 0.0

private fun Double.formatMoney(): String =
    String.format(Locale.CHINA, "¥%,.2f", this)
