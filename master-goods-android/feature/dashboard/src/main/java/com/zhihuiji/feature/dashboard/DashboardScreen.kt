package com.zhihuiji.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.DataTextPrimary
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.MainBottomBarHeight
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val DashboardHorizontalPadding = 16.dp
private val DashboardTopContentPadding = 76.dp
private val DashboardBottomContentExtraPadding = 88.dp
private val DashboardBottomBarTouchClearance = 32.dp
private val DashboardBottomEndCardHeight = 56.dp
private val DashboardSectionGap = 20.dp
private val DashboardInnerSectionGap = 12.dp
private val DashboardCardCorner = RoundedCornerShape(16.dp)
private val DashboardKpiCardHeight = 96.dp
private val DashboardTrendCardHeight = 228.dp
private val DashboardReminderRowHeight = 68.dp
private val DashboardDialogDayFormatter = DateTimeFormatter.ofPattern("MM月dd日", Locale.getDefault())
private val DashboardDialogFullDayFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.getDefault())

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
    bottomBarScrollEvents: Flow<Float> = emptyFlow(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToAgent: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSalesDatePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val navigationBarPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val bottomBarContentInset = MainBottomBarHeight + navigationBarPadding

    LaunchedEffect(bottomBarScrollEvents) {
        bottomBarScrollEvents.collect { scrollDeltaPx ->
            if (scrollDeltaPx != 0f) {
                listState.scrollBy(scrollDeltaPx)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = DashboardHorizontalPadding,
                    end = DashboardHorizontalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(DashboardSectionGap),
            contentPadding = PaddingValues(
                top = DashboardTopContentPadding,
                bottom = bottomBarContentInset +
                    DashboardBottomContentExtraPadding +
                    DashboardBottomBarTouchClearance
            )
        ) {
            item(key = "business_overview") {
                BusinessOverviewSection(
                    uiState = uiState,
                    onSalesRangeSelected = viewModel::selectSalesRange,
                    onSalesDateClick = { showSalesDatePicker = true }
                )
            }

            item(key = "sales_trend") {
                SalesTrendSection(
                    trend = uiState.salesTrend,
                    title = uiState.salesTrendTitle
                )
            }

            item(key = "pending_tasks") {
                PendingTasksSection(
                    uiState = uiState,
                    onSalesClick = onNavigateToSales,
                    onProductsClick = onNavigateToProducts,
                    onCustomersClick = onNavigateToCustomers,
                    onAgentClick = onNavigateToAgent
                )
            }

            if (!uiState.error.isNullOrBlank()) {
                item(key = "dashboard_contract_notice") {
                    DashboardContractNotice(message = uiState.error ?: "")
                }
            }

            item(key = "dashboard_scroll_end") {
                DashboardScrollEndCard()
            }
        }

        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            DashboardTopBar(
                hasPending = uiState.pendingReminders.isNotEmpty(),
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToNotifications = onNavigateToNotifications
            )
        }
    }

    if (showSalesDatePicker) {
        SalesDatePickerDialog(
            selectedDate = uiState.selectedSalesDate,
            onDismiss = { showSalesDatePicker = false },
            onDateSelected = viewModel::selectSingleSalesDate
        )
    }
}

@Composable
private fun DashboardTopBar(
    hasPending: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 20.dp,
        shape = RoundedCornerShape(0.dp),
        surfaceColor = GlassSurfaceMedium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "智慧记",
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = ZhihuijiPrimary,
                maxLines = 1
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    DashboardTopIconButton(
                        icon = Icons.Default.Notifications,
                        contentDescription = "待处理提醒",
                        onClick = onNavigateToNotifications
                    )
                    if (hasPending) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DangerRed)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
                DashboardTopIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "设置",
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun DashboardTopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun BusinessOverviewSection(
    uiState: DashboardUiState,
    onSalesRangeSelected: (DashboardSalesRange) -> Unit,
    onSalesDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardInnerSectionGap)
    ) {
        SalesRangeHeader(
            title = uiState.salesOverviewTitle,
            scopeHint = uiState.salesScopeHint,
            isLoading = uiState.isLoading,
            selectedRange = uiState.selectedSalesRange,
            calendarChipLabel = uiState.calendarChipLabel,
            isDateSelected = uiState.selectedSalesDate != null,
            onRangeSelected = onSalesRangeSelected,
            onDateClick = onSalesDateClick
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardKpiCard(
                modifier = Modifier.weight(1f),
                title = "销售额 (元)",
                value = formatCurrencyText(uiState.salesAmount),
                caption = "${uiState.salesPeriodLabel} ${uiState.salesOrderCount} 张真实订单",
                icon = Icons.Outlined.Payments,
                accent = ZhihuijiPrimary,
                valueColor = DataTextPrimary,
                glow = ZhihuijiPrimary.copy(alpha = 0.07f),
                captionColor = SuccessGreen
            )
            DashboardKpiCard(
                modifier = Modifier.weight(1f),
                title = "待收款",
                value = formatCurrencyText(uiState.receivableAmount),
                caption = "${uiState.receivableCustomerCount} 位客户待跟进",
                icon = Icons.Outlined.AccountBalanceWallet,
                accent = WarningOrange,
                valueColor = WarningOrange,
                glow = WarningOrange.copy(alpha = 0.08f),
                captionDotColor = WarningOrange
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardKpiCard(
                modifier = Modifier.weight(1f),
                title = "低库存预警",
                value = "${uiState.lowStockCount}",
                caption = "需立即补货",
                icon = Icons.Outlined.Inventory,
                accent = DangerRed,
                unit = "件",
                valueColor = DangerRed,
                glow = DangerRed.copy(alpha = 0.08f),
                captionDotColor = DangerRed
            )
            DashboardKpiCard(
                modifier = Modifier.weight(1f),
                title = "净现金流",
                value = signedCurrency(uiState.netCashFlow),
                caption = "${uiState.salesPeriodLabel}资金流水净额",
                icon = Icons.Outlined.AccountBalance,
                accent = if (uiState.netCashFlow.startsWith("-")) DangerRed else SuccessGreen,
                valueColor = if (uiState.netCashFlow.startsWith("-")) DangerRed else SuccessGreen,
                glow = SuccessGreen.copy(alpha = 0.07f)
            )
        }
    }
}

@Composable
private fun DashboardKpiCard(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accent: Color,
    valueColor: Color,
    glow: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
    captionColor: Color = TextTertiary,
    captionDotColor: Color? = null,
) {
    LiquidGlassCard(
        modifier = modifier.height(DashboardKpiCardHeight),
        shape = DashboardCardCorner,
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp)
                    .size(96.dp)
                    .blur(24.dp)
                    .background(glow, CircleShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.78f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = valueColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (unit != null) {
                        Text(
                            text = unit,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 3.dp, bottom = 1.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (captionDotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(captionDotColor)
                        )
                    }
                    Text(
                        text = caption,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = captionColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesTrendSection(
    trend: List<SalesTrendPoint>,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardInnerSectionGap)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(DashboardTrendCardHeight),
            shape = DashboardCardCorner,
            surfaceColor = GlassSurfaceLow,
            contentPadding = 20.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总计  ${formatCurrency(trend.sumOf { it.value })}",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Text(
                    text = "•••",
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                SalesLineChart(
                    trend = trend,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TrendAxisLabels(
                trend = trend,
                modifier = Modifier.height(22.dp)
            )
        }
    }
}

@Composable
private fun SalesLineChart(
    trend: List<SalesTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (trend.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无趋势数据",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary
            )
        }
        return
    }

    Canvas(
        modifier = modifier.drawWithCache {
            val maxValue = trend.maxOfOrNull { it.value } ?: 0.0
            val hasPositiveValue = maxValue > 0.0
            val singlePointCenter = if (trend.size == 1) {
                Offset(
                    x = size.width / 2f,
                    y = if (hasPositiveValue) size.height * 0.24f else size.height * 0.58f
                )
            } else {
                null
            }
            val baselineY = size.height * 0.72f
            val horizontalStep = if (trend.size > 1) size.width / (trend.size - 1) else size.width
            val topPadding = size.height * 0.12f
            val bottomPadding = size.height * 0.76f
            val quietBaseline = size.height * 0.58f
            val points = if (trend.size > 1) {
                trend.mapIndexed { index, point ->
                    val x = horizontalStep * index
                    val y = if (hasPositiveValue) {
                        val normalized = (point.value / maxValue).toFloat().coerceIn(0f, 1f)
                        bottomPadding - ((bottomPadding - topPadding) * normalized)
                    } else {
                        quietBaseline
                    }
                    Offset(x, y)
                }
            } else {
                emptyList()
            }

            fun Path.drawSmooth(points: List<Offset>) {
                moveTo(points.first().x, points.first().y)
                points.zipWithNext().forEach { (start, end) ->
                    val controlX = (start.x + end.x) / 2f
                    cubicTo(controlX, start.y, controlX, end.y, end.x, end.y)
                }
            }

            val linePath = if (points.isNotEmpty()) {
                Path().apply { drawSmooth(points) }
            } else {
                null
            }
            val fillPath = if (points.isNotEmpty()) {
                Path().apply {
                    drawSmooth(points)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }
            } else {
                null
            }
            val lineStroke = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            val baselineStrokeWidth = 1.dp.toPx()
            val singleHaloRadius = 28.dp.toPx()
            val singleOuterRadius = 8.dp.toPx()
            val singleInnerRadius = 5.dp.toPx()
            val emphasizedPoints = points.mapIndexedNotNull { index, offset ->
                val shouldEmphasize = index == points.lastIndex || index == points.size / 2
                if (shouldEmphasize) {
                    val radius = if (index == points.lastIndex) 4.dp.toPx() else 3.dp.toPx()
                    offset to radius
                } else {
                    null
                }
            }

            onDrawBehind {
                if (singlePointCenter != null) {
                    drawLine(
                        color = ZhihuijiPrimaryBright.copy(alpha = 0.16f),
                        start = Offset(0f, baselineY),
                        end = Offset(size.width, baselineY),
                        strokeWidth = baselineStrokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = ZhihuijiPrimaryBright.copy(alpha = 0.14f),
                        radius = singleHaloRadius,
                        center = singlePointCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = singleOuterRadius,
                        center = singlePointCenter
                    )
                    drawCircle(
                        color = ZhihuijiPrimary,
                        radius = singleInnerRadius,
                        center = singlePointCenter
                    )
                    return@onDrawBehind
                }

                fillPath?.let { path ->
                    drawPath(
                        path = path,
                        color = ZhihuijiPrimaryBright.copy(alpha = 0.10f)
                    )
                }
                linePath?.let { path ->
                    drawPath(
                        path = path,
                        color = ZhihuijiPrimary,
                        style = lineStroke
                    )
                }
                emphasizedPoints.forEach { (offset, radius) ->
                    drawCircle(
                        color = Color.White,
                        radius = radius + 2.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = ZhihuijiPrimary,
                        radius = radius,
                        center = offset
                    )
                }
            }
        }
    ) {}
}

@Composable
private fun TrendAxisLabels(
    trend: List<SalesTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (trend.size <= 1) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = trend.firstOrNull()?.label ?: "-",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
        return
    }

    if (trend.size <= 4) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            trend.forEach { point ->
                Text(
                    text = point.label,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = trend.firstOrNull()?.label ?: "-",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1
        )
        Text(
            text = trend.getOrNull(trend.size / 2)?.label ?: "-",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1
        )
        Text(
            text = trend.lastOrNull()?.label ?: "今日",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun PendingTasksSection(
    uiState: DashboardUiState,
    onSalesClick: () -> Unit,
    onProductsClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onAgentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminders = buildDashboardReminderItems(uiState, onProductsClick, onCustomersClick, onAgentClick)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardInnerSectionGap)
    ) {
        Text(
            text = "待处理提醒",
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        if (reminders.isEmpty()) {
            DashboardReminderRow(
                item = DashboardReminderItem(
                    title = "暂无待处理提醒",
                    subtitle = "当前真实数据没有低库存或待收款提醒",
                    count = null,
                    icon = Icons.Outlined.SmartToy,
                    tint = ZhihuijiPrimary,
                    onClick = onAgentClick
                )
            )
        } else {
            reminders.forEach { item ->
                DashboardReminderRow(item = item)
            }
        }
    }
}

@Composable
private fun DashboardReminderRow(
    item: DashboardReminderItem,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(DashboardReminderRowHeight),
        onClick = item.onClick,
        shape = DashboardCardCorner,
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DashboardReminderRowHeight)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(item.tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.count != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DangerRed)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.count.toString(),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Text(
                text = "›",
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun DashboardScrollEndCard(
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(DashboardBottomEndCardHeight),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "已显示全部经营提醒",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun SalesRangeHeader(
    title: String,
    scopeHint: String,
    isLoading: Boolean,
    selectedRange: DashboardSalesRange?,
    calendarChipLabel: String,
    isDateSelected: Boolean,
    onRangeSelected: (DashboardSalesRange) -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                Text(
                    text = "同步中",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZhihuijiPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(ZhihuijiPrimary.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardSalesRange.values().forEach { range ->
                SalesRangeChip(
                    range = range,
                    selected = range == selectedRange,
                    isLoading = isLoading && range == selectedRange,
                    onClick = { onRangeSelected(range) }
                )
            }
            SalesFilterChip(
                label = calendarChipLabel,
                selected = isDateSelected,
                isLoading = isLoading && isDateSelected,
                leadingIcon = Icons.Outlined.CalendarMonth,
                onClick = onDateClick
            )
        }
        Text(
            text = scopeHint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDateSelected) ZhihuijiPrimary else TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SalesRangeChip(
    range: DashboardSalesRange,
    selected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SalesFilterChip(
        label = range.buttonLabel,
        selected = selected,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun SalesFilterChip(
    label: String,
    selected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    LiquidGlassCard(
        modifier = modifier,
        onClick = onClick,
        surfaceColor = if (selected) GlassSurfaceHigh else GlassSurfaceLow,
        shape = RoundedCornerShape(100.dp),
        contentPadding = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (selected) ZhihuijiPrimary.copy(alpha = 0.12f) else Color.Transparent,
                    shape = RoundedCornerShape(100.dp)
                )
                .padding(horizontal = 9.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null && !isLoading) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (selected) ZhihuijiPrimary else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = if (isLoading) "同步" else label,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) ZhihuijiPrimary else TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesDatePickerDialog(
    selectedDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val initialDate = remember(selectedDate, today) {
        when {
            selectedDate == null -> today
            selectedDate.isAfter(today) -> today
            else -> selectedDate
        }
    }
    val todayMillis = remember(today) { today.toDatePickerMillis() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toDatePickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    )
    val pickedDate = datePickerState.selectedDateMillis
        ?.toDatePickerLocalDate()
        ?.let { if (it.isAfter(today)) today else it }
        ?: initialDate

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(pickedDate)
                    onDismiss()
                }
            ) {
                Text(text = "查看这一天")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "选择统计日期",
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp)
                )
            },
            headline = {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "按某一天查看真实销售"
                    )
                    Text(
                        text = "将查看 ${DashboardDialogFullDayFormatter.format(pickedDate)}，未来日期不可选。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SalesDateShortcutChip(
                            label = "今天",
                            date = today,
                            selected = pickedDate == today,
                            onClick = {
                                onDateSelected(today)
                                onDismiss()
                            }
                        )
                        SalesDateShortcutChip(
                            label = "昨天",
                            date = today.minusDays(1),
                            selected = pickedDate == today.minusDays(1),
                            onClick = {
                                onDateSelected(today.minusDays(1))
                                onDismiss()
                            }
                        )
                    }
                    Text(
                        text = "从日历点选日期后，首页销售额、订单数和分时图会一起刷新。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextTertiary
                    )
                }
            },
            showModeToggle = false
        )
    }
}

@Composable
private fun SalesDateShortcutChip(
    label: String,
    date: LocalDate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier,
        onClick = onClick,
        surfaceColor = if (selected) GlassSurfaceHigh else GlassSurfaceLow,
        shape = RoundedCornerShape(100.dp),
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = if (selected) ZhihuijiPrimary.copy(alpha = 0.12f) else Color.Transparent,
                    shape = RoundedCornerShape(100.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) ZhihuijiPrimary else TextSecondary
            )
            Text(
                text = DashboardDialogDayFormatter.format(date),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (selected) ZhihuijiPrimary else TextTertiary
            )
        }
    }
}

@Composable
private fun DashboardContractNotice(
    message: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceMedium,
        contentPadding = 14.dp
    ) {
        Text(
            text = "部分首页数据刷新失败",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = WarningOrange
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = TextSecondary
        )
    }
}

private data class DashboardReminderItem(
    val title: String,
    val subtitle: String,
    val count: Int?,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

private fun buildDashboardReminderItems(
    uiState: DashboardUiState,
    onProductsClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onAgentClick: () -> Unit,
): List<DashboardReminderItem> = buildList {
    if (uiState.lowStockCount > 0) {
        add(
            DashboardReminderItem(
                title = "低库存商品",
                subtitle = uiState.lowStockProducts.firstOrNull()?.let { "${it.name} 等商品需补货" }
                    ?: "${uiState.lowStockCount} 个商品库存低于安全线",
                count = uiState.lowStockCount,
                icon = Icons.Outlined.Inventory,
                tint = DangerRed,
                onClick = onProductsClick
            )
        )
    }
    if (uiState.receivableCustomerCount > 0) {
        add(
            DashboardReminderItem(
                title = "待收款客户",
                subtitle = "${uiState.receivableCustomerCount} 位客户账款需要跟进",
                count = uiState.receivableCustomerCount,
                icon = Icons.Outlined.People,
                tint = ZhihuijiPrimary,
                onClick = onCustomersClick
            )
        )
    }
    if (isEmpty() && uiState.pendingReminders.isNotEmpty()) {
        add(
            DashboardReminderItem(
                title = "AI 经营提醒",
                subtitle = uiState.pendingReminders.first(),
                count = uiState.pendingReminders.size,
                icon = Icons.Outlined.SmartToy,
                tint = WarningOrange,
                onClick = onAgentClick
            )
        )
    }
}

private fun signedCurrency(value: String): String {
    val normalized = value.toDoubleOrNull() ?: return "¥$value"
    val prefix = when {
        normalized > 0.0 -> "+¥"
        normalized < 0.0 -> "-¥"
        else -> "¥"
    }
    return prefix + formatNumber(abs(normalized))
}

private fun formatCurrency(value: Double): String = "¥${formatNumber(value)}"

private fun formatCurrencyText(value: String): String {
    val normalized = value.toDoubleOrNull() ?: return "¥$value"
    return formatCurrency(normalized)
}

private fun formatNumber(value: Double): String = DecimalFormat("#,##0").format(value)

private fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toDatePickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
