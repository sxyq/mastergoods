package com.zhihuiji.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.ReconciliationSummaryReportDto
import com.zhihuiji.core.model.SalesTrendPointReportDto
import com.zhihuiji.data.customer.CustomerV2Repository
import com.zhihuiji.data.product.ProductV2Repository
import com.zhihuiji.data.report.DashboardSnapshot
import com.zhihuiji.data.report.DashboardSnapshotRepository
import com.zhihuiji.data.report.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.HashMap
import javax.inject.Inject

private val CHART_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault())
private val DAY_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("MM月dd日", Locale.getDefault())
private val DAY_CHIP_FORMATTER = DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault())
private val FULL_DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.getDefault())
private val SINGLE_DAY_CHART_SLOTS = listOf(
    0 to "00-06",
    1 to "06-12",
    2 to "12-18",
    3 to "18-24"
)

@Immutable
data class SalesTrendPoint(
    val label: String,
    val value: Double
)

@Immutable
data class LowStockProductItem(
    val id: Long,
    val name: String,
    val stock: Double,
    val safeStock: Double
)

@Immutable
enum class DashboardSalesRange(
    val days: Int,
    val buttonLabel: String,
    val periodLabel: String
) {
    LAST_7(7, "7天", "近7天"),
    LAST_30(30, "30天", "近30天"),
    LAST_90(90, "90天", "近90天"),
    LAST_365(365, "1年", "近1年")
}

@Immutable
sealed interface DashboardSalesScope {
    val periodLabel: String

    @Immutable
    data class Range(val range: DashboardSalesRange) : DashboardSalesScope {
        override val periodLabel: String = range.periodLabel
    }

    @Immutable
    data class SingleDay(val date: LocalDate) : DashboardSalesScope {
        override val periodLabel: String = DAY_PERIOD_FORMATTER.format(date)
    }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val productRepository: ProductV2Repository,
    private val customerRepository: CustomerV2Repository,
    private val reportRepository: ReportRepository,
    private val dashboardSnapshotRepository: DashboardSnapshotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadSequence = 0

    init {
        // Offline-first: subscribe to Room flows for immediately available local data.
        // Low stock products and receivable customer count are read from Room before
        // any network request completes.
        viewModelScope.launch {
            productRepository.observeLowStockProducts(limit = 10).collectLatest { products ->
                val items = products.map { product ->
                    LowStockProductItem(
                        id = product.id,
                        name = product.name,
                        stock = product.stock,
                        safeStock = product.safeStock
                    )
                }
                _uiState.update {
                    it.copy(
                        lowStockCount = items.size,
                        lowStockProducts = items,
                        pendingReminders = buildPendingReminders(items, it.receivableAmount.toDoubleOrNull() ?: 0.0),
                    )
                }
            }
        }
        viewModelScope.launch {
            customerRepository.observeReceivableCustomerCount().collectLatest { count ->
                _uiState.update { it.copy(receivableCustomerCount = count) }
            }
        }
        viewModelScope.launch {
            // Read the last complete local result before scheduling the non-blocking refresh.
            loadCachedDashboard()
            loadDashboard()
        }
    }

    fun loadDashboard() {
        val requestSequence = ++loadSequence
        val selectedScope = _uiState.value.selectedSalesScope
        viewModelScope.launch {
            // Offline-first: don't block UI or show sync indicator.
            // Default/cached values remain visible; network results update silently.
            _uiState.update { it.copy(error = null) }

            val dateRange = selectedScope.toDateRange()
            val salesSummaryDeferred = async { reportRepository.salesSummary(dateRange.startMillis, dateRange.endMillis) }
            val salesTrendDeferred = async {
                reportRepository.salesTrend(
                    startAt = dateRange.startMillis,
                    endAt = dateRange.endMillis,
                    bucket = dateRange.salesTrendBucket
                )
            }
            val lowStockDeferred = async { productRepository.listLowStockProducts(size = 10) }
            val reconciliationSummaryDeferred = async {
                reportRepository.reconciliationSummary(dateRange.startMillis, dateRange.endMillis)
            }
            val cashflowSummaryDeferred = async {
                reportRepository.cashflowSummary(dateRange.startMillis, dateRange.endMillis)
            }

            val salesSummaryResult = salesSummaryDeferred.await()
            val salesTrendResult = salesTrendDeferred.await()
            val lowStockResult = lowStockDeferred.await()
            val reconciliationSummaryResult = reconciliationSummaryDeferred.await()
            val cashflowSummaryResult = cashflowSummaryDeferred.await()
            val reconciliationSummary = reconciliationSummaryResult.getOrNull()
            val customerFallbackResult = if (reconciliationSummary?.needsReceivableCustomerFallback() == true) {
                customerRepository.listCustomers()
            } else {
                null
            }

            val nextState = withContext(Dispatchers.Default) {
                val previousState = _uiState.value
                val salesSummary = salesSummaryResult.getOrNull()
                val trendPoints = salesTrendResult.getOrNull()
                val salesAmount = when {
                    salesSummary != null -> salesSummary.totalSalesAmount
                    trendPoints != null -> trendPoints.sumOf { it.totalSalesAmount }
                    else -> previousState.salesAmount.toDoubleOrNull() ?: 0.0
                }
                val salesOrderCount = when {
                    salesSummary != null -> salesSummary.totalOrderCount
                    trendPoints != null -> trendPoints.sumOf { it.totalOrderCount }
                    else -> previousState.salesOrderCount
                }
                val salesTrend = trendPoints?.let {
                    buildSalesTrend(trendPoints = it, dateRange = dateRange)
                } ?: previousState.salesTrend
                val freshLowStock = lowStockResult.getOrNull()?.map { product ->
                    LowStockProductItem(
                        id = product.id,
                        name = product.name,
                        stock = product.stock,
                        safeStock = product.safeStock
                    )
                }
                val lowStockItems = freshLowStock ?: previousState.lowStockProducts
                val lowStockCount = freshLowStock?.size ?: previousState.lowStockCount
                val customers = customerFallbackResult?.getOrNull()
                val receivableAmount = reconciliationSummary?.totalReceivableAmount
                    ?: customers?.sumOf { it.balance }
                    ?: (previousState.receivableAmount.toDoubleOrNull() ?: 0.0)
                val receivableCustomerCount = when {
                    reconciliationSummary == null && customers != null -> customers.count { it.balance > 0.0 }
                    reconciliationSummary == null -> previousState.receivableCustomerCount
                    reconciliationSummary.totalReceivableCustomerCount > 0L ->
                        reconciliationSummary.totalReceivableCustomerCount
                            .coerceAtMost(Int.MAX_VALUE.toLong())
                            .toInt()
                    reconciliationSummary.totalReceivableAmount == 0.0 -> 0
                    customers != null -> customers.count { it.balance > 0.0 }
                    else -> previousState.receivableCustomerCount
                }
                val netCashFlow = cashflowSummaryResult.getOrNull()?.netCashFlow
                    ?: (previousState.netCashFlow.toDoubleOrNull() ?: 0.0)

                val reminders = buildPendingReminders(lowStockItems, receivableAmount)
                val state = DashboardComputedState(
                    salesAmount = "%.2f".format(salesAmount),
                    receivableAmount = "%.2f".format(receivableAmount),
                    lowStockCount = lowStockCount,
                    salesOrderCount = salesOrderCount,
                    netCashFlow = "%.2f".format(netCashFlow),
                    receivableCustomerCount = receivableCustomerCount,
                    salesTrend = salesTrend,
                    lowStockProducts = lowStockItems,
                    pendingReminders = reminders,
                    error = null,
                )
                DashboardLoadResult(
                    state = state,
                    snapshot = trendPoints?.takeIf {
                        salesSummary != null && reconciliationSummary != null &&
                            cashflowSummaryResult.isSuccess &&
                            customerFallbackResult?.isFailure != true
                    }?.let {
                        DashboardSnapshot(
                            salesAmount = salesAmount,
                            salesOrderCount = salesOrderCount,
                            receivableAmount = receivableAmount,
                            receivableCustomerCount = receivableCustomerCount,
                            netCashFlow = netCashFlow,
                            salesTrend = it,
                            updatedAt = System.currentTimeMillis(),
                        )
                    },
                )
            }

            if (requestSequence != loadSequence) return@launch

            _uiState.update {
                it.copy(
                    isLoading = false,
                    salesAmount = nextState.state.salesAmount,
                    receivableAmount = nextState.state.receivableAmount,
                    lowStockCount = nextState.state.lowStockCount,
                    salesOrderCount = nextState.state.salesOrderCount,
                    netCashFlow = nextState.state.netCashFlow,
                    receivableCustomerCount = nextState.state.receivableCustomerCount,
                    salesTrend = nextState.state.salesTrend,
                    lowStockProducts = nextState.state.lowStockProducts,
                    pendingReminders = nextState.state.pendingReminders,
                    error = null
                )
            }
            nextState.snapshot?.let { snapshot ->
                runCatching {
                    dashboardSnapshotRepository.save(selectedScope.toSnapshotScope(), snapshot)
                }
            }
        }
    }

    private suspend fun loadCachedDashboard() {
        val selectedScope = _uiState.value.selectedSalesScope
        val snapshot = runCatching {
            dashboardSnapshotRepository.load(selectedScope.toSnapshotScope())
        }.getOrNull() ?: return
        _uiState.update { current ->
            if (current.selectedSalesScope != selectedScope) return@update current
            val lowStockItems = current.lowStockProducts
            current.copy(
                salesAmount = "%.2f".format(snapshot.salesAmount),
                receivableAmount = "%.2f".format(snapshot.receivableAmount),
                salesOrderCount = snapshot.salesOrderCount,
                netCashFlow = "%.2f".format(snapshot.netCashFlow),
                receivableCustomerCount = snapshot.receivableCustomerCount,
                salesTrend = buildSalesTrend(snapshot.salesTrend, selectedScope.toDateRange()),
                pendingReminders = buildPendingReminders(lowStockItems, snapshot.receivableAmount),
                error = null,
            )
        }
    }

    private fun buildSalesTrend(
        trendPoints: List<SalesTrendPointReportDto>,
        dateRange: DashboardDateRange
    ): List<SalesTrendPoint> {
        if (dateRange.days == 1) {
            return buildSingleDaySalesTrend(trendPoints)
        }

        val amountByDate = aggregateSalesTrendByDate(trendPoints)

        return (0 until dateRange.days).map { offset ->
            val date = dateRange.startDate.plusDays(offset.toLong())
            SalesTrendPoint(
                label = CHART_DATE_FORMATTER.format(date),
                value = amountByDate[date] ?: 0.0
            )
        }
    }

    private fun buildSingleDaySalesTrend(trendPoints: List<SalesTrendPointReportDto>): List<SalesTrendPoint> {
        val amountBySlot = aggregateSalesTrendBySlot(trendPoints)

        return SINGLE_DAY_CHART_SLOTS.map { (slot, label) ->
            SalesTrendPoint(
                label = label,
                value = amountBySlot[slot] ?: 0.0
            )
        }
    }

    private fun buildPendingReminders(
        lowStockItems: List<LowStockProductItem>,
        receivableAmount: Double
    ): List<String> {
        val reminders = mutableListOf<String>()
        if (lowStockItems.isNotEmpty()) {
            reminders.add("${lowStockItems.size} 个商品库存低于安全线")
        }
        if (receivableAmount > 0) {
            reminders.add("待收款金额 ¥%.2f".format(receivableAmount))
        }
        return reminders
    }

    fun refresh() {
        loadDashboard()
    }

    fun selectSalesRange(range: DashboardSalesRange) {
        val nextScope = DashboardSalesScope.Range(range)
        if (_uiState.value.selectedSalesScope == nextScope) return
        _uiState.update { it.copy(selectedSalesScope = nextScope) }
        loadDashboard()
    }

    fun selectSingleSalesDate(date: LocalDate) {
        val nextScope = DashboardSalesScope.SingleDay(date.coerceNotFuture())
        if (_uiState.value.selectedSalesScope == nextScope) return
        _uiState.update { it.copy(selectedSalesScope = nextScope) }
        loadDashboard()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun DashboardSalesScope.toDateRange(): DashboardDateRange {
        return when (this) {
            is DashboardSalesScope.Range -> {
                val today = LocalDate.now()
                val startDate = today.minusDays((range.days - 1).toLong())
                DashboardDateRange(
                    startDate = startDate,
                    startMillis = startDate.startOfDayMillis(),
                    endMillis = today.endOfDayMillis(),
                    days = range.days
                )
            }

            is DashboardSalesScope.SingleDay -> {
                val selectedDate = date.coerceNotFuture()
                DashboardDateRange(
                    startDate = selectedDate,
                    startMillis = selectedDate.startOfDayMillis(),
                    endMillis = selectedDate.endOfDayMillis(),
                    days = 1
                )
            }
        }
    }

    private fun DashboardSalesScope.toSnapshotScope(): String = when (this) {
        is DashboardSalesScope.Range -> "range:${range.days}"
        is DashboardSalesScope.SingleDay -> "day:$date"
    }

}

@Immutable
data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSalesScope: DashboardSalesScope = DashboardSalesScope.Range(DashboardSalesRange.LAST_365),
    val salesAmount: String = "0.00",
    val receivableAmount: String = "0.00",
    val lowStockCount: Int = 0,
    val salesOrderCount: Int = 0,
    val netCashFlow: String = "0.00",
    val receivableCustomerCount: Int = 0,
    val salesTrend: List<SalesTrendPoint> = emptyList(),
    val lowStockProducts: List<LowStockProductItem> = emptyList(),
    val pendingReminders: List<String> = emptyList()
) {
    val selectedSalesRange: DashboardSalesRange?
        get() = (selectedSalesScope as? DashboardSalesScope.Range)?.range

    val selectedSalesDate: LocalDate?
        get() = (selectedSalesScope as? DashboardSalesScope.SingleDay)?.date

    val salesPeriodLabel: String
        get() = selectedSalesScope.periodLabel

    val salesOverviewTitle: String
        get() = "${salesPeriodLabel}经营概览"

    val salesTrendTitle: String
        get() = when (selectedSalesScope) {
            is DashboardSalesScope.Range -> "${salesPeriodLabel}销售趋势"
            is DashboardSalesScope.SingleDay -> "${salesPeriodLabel}销售分时"
        }

    val calendarChipLabel: String
        get() = selectedSalesDate?.let { "${DAY_CHIP_FORMATTER.format(it)} 单日" } ?: "查单日"

    val salesScopeHint: String
        get() = when (val scope = selectedSalesScope) {
            is DashboardSalesScope.Range -> "区间查看：销售额、订单数和趋势按${scope.periodLabel}同步统计"
            is DashboardSalesScope.SingleDay -> "单日查看：${FULL_DAY_FORMATTER.format(scope.date)}，图表按 6 小时时段统计"
        }
}

private data class DashboardDateRange(
    val startDate: LocalDate,
    val startMillis: Long,
    val endMillis: Long,
    val days: Int
) {
    val salesTrendBucket: String
    get() = if (days == 1) "hour6" else "day"
}

private data class DashboardComputedState(
    val salesAmount: String,
    val receivableAmount: String,
    val lowStockCount: Int,
    val salesOrderCount: Int,
    val netCashFlow: String,
    val receivableCustomerCount: Int,
    val salesTrend: List<SalesTrendPoint>,
    val lowStockProducts: List<LowStockProductItem>,
    val pendingReminders: List<String>,
    val error: String?,
)

private data class DashboardLoadResult(
    val state: DashboardComputedState,
    val snapshot: DashboardSnapshot?,
)

private fun ReconciliationSummaryReportDto?.needsReceivableCustomerFallback(): Boolean =
    this == null || (totalReceivableAmount > 0.0 && totalReceivableCustomerCount == 0L)

private fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun LocalDate.endOfDayMillis(): Long =
    plusDays(1).startOfDayMillis() - 1L

private fun LocalDate.coerceNotFuture(): LocalDate {
    val today = LocalDate.now()
    return if (isAfter(today)) today else this
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

private fun Long.toLocalHour(): Int =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .hour

/**
 * Aggregates the report stream without allocating one temporary list per date.
 * Dashboard ranges can contain hundreds of buckets, so a single-pass map keeps
 * refresh work O(n) with less garbage than groupBy(...).mapValues(...).
 */
internal fun aggregateSalesTrendByDate(
    trendPoints: List<SalesTrendPointReportDto>,
): Map<LocalDate, Double> {
    val amounts = HashMap<LocalDate, Double>(trendPoints.size)
    trendPoints.forEach { point ->
        val date = point.startAt.toLocalDate()
        amounts[date] = (amounts[date] ?: 0.0) + point.totalSalesAmount
    }
    return amounts
}

/** Single-pass 6-hour aggregation for the one-day dashboard view. */
internal fun aggregateSalesTrendBySlot(
    trendPoints: List<SalesTrendPointReportDto>,
): Map<Int, Double> {
    val amounts = HashMap<Int, Double>(SINGLE_DAY_CHART_SLOTS.size)
    trendPoints.forEach { point ->
        val slot = (point.startAt.toLocalHour() / 6).coerceIn(0, 3)
        amounts[slot] = (amounts[slot] ?: 0.0) + point.totalSalesAmount
    }
    return amounts
}
