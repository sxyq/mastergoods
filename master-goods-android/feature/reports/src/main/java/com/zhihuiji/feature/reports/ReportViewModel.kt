package com.zhihuiji.feature.reports

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.TopSellingProductReportDto
import com.zhihuiji.data.report.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@Immutable
enum class ReportPeriod(
    val tabLabel: String,
    val periodLabel: String,
) {
    TODAY("今日", "今日"),
    THIS_WEEK("本周", "本周"),
    THIS_MONTH("本月", "本月")
}

private val reportPeriods = enumValues<ReportPeriod>()

fun TopSellingProductReportDto.toTopProductItem(): TopProductItem = TopProductItem(
    id = productId,
    name = productName,
    salesAmount = "¥%.0f".format(totalAmount),
    salesCount = totalQuantity.toInt()
)

@Immutable
data class TopProductItem(
    val id: Long,
    val name: String,
    val salesAmount: String,
    val salesCount: Int
)

@Immutable
data class ReportUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val failedReportSections: List<String> = emptyList(),
    val selectedPeriodIndex: Int = 0,
    val salesAmount: String = "0.00",
    val profitAmount: String = "0.00",
    val profitRate: String = "0.00",
    val orderCount: Int = 0,
    val receivableAmount: String = "0.00",
    val payableAmount: String = "0.00",
    val topProducts: List<TopProductItem> = emptyList()
) {
    val selectedPeriod: ReportPeriod
        get() = reportPeriods[selectedPeriodIndex.coerceIn(0, reportPeriods.lastIndex)]

    val selectedPeriodLabel: String
        get() = selectedPeriod.periodLabel
}

private data class PartnerBalanceSummary(
    val receivableAmount: Double,
    val payableAmount: Double,
)

private data class ReportDateRange(
    val startAt: Long,
    val endAt: Long,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var loadSequence = 0
    private var cachedPartnerBalances: PartnerBalanceSummary? = null

    init {
        loadReports()
    }

    fun loadReports(forcePartnerRefresh: Boolean = false) {
        val requestSequence = ++loadSequence
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, failedReportSections = emptyList()) }

            val dateRange = getPeriodMillis(_uiState.value.selectedPeriod)

            val salesSummaryDeferred = async { reportRepository.salesSummary(dateRange.startAt, dateRange.endAt) }
            val profitSummaryDeferred = async { reportRepository.profitSummary(dateRange.startAt, dateRange.endAt) }
            val topProductsDeferred = async { reportRepository.topProducts(dateRange.startAt, dateRange.endAt, limit = 5) }
            val partnerBalancesDeferred = if (forcePartnerRefresh || cachedPartnerBalances == null) {
                async { fetchPartnerBalances(dateRange) }
            } else {
                null
            }

            val salesSummary = salesSummaryDeferred.await()
            val profitSummary = profitSummaryDeferred.await()
            val topProducts = topProductsDeferred.await()
            val partnerBalancesResult = partnerBalancesDeferred?.await()

            if (requestSequence != loadSequence) return@launch

            val salesAmount = salesSummary.getOrNull()?.totalSalesAmount ?: 0.0
            val orderCount = salesSummary.getOrNull()?.totalOrderCount ?: 0
            val profitAmount = profitSummary.getOrNull()?.estimatedProfitAmount ?: 0.0
            val profitRate = profitSummary.getOrNull()?.estimatedProfitRate ?: 0.0
            partnerBalancesResult?.getOrNull()?.let { cachedPartnerBalances = it }

            val partnerBalances = cachedPartnerBalances ?: PartnerBalanceSummary(
                receivableAmount = 0.0,
                payableAmount = 0.0,
            )

            val topProductItems = topProducts.getOrNull()?.let { result ->
                buildList(result.size) {
                    for (item in result) {
                        add(item.toTopProductItem())
                    }
                }
            } ?: emptyList()

            val partnerBalancesFailed = partnerBalancesResult?.isFailure == true
            val failedSections = ArrayList<String>(4).apply {
                if (salesSummary.isFailure) add("销售汇总")
                if (profitSummary.isFailure) add("利润预估")
                if (topProducts.isFailure) add("商品排行")
                if (partnerBalancesFailed) add("往来余额")
            }
            val errorMsg =
                salesSummary.messageOrNull()
                    ?: profitSummary.messageOrNull()
                    ?: topProducts.messageOrNull()
                    ?: partnerBalancesResult?.messageOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    salesAmount = "%.2f".format(salesAmount),
                    profitAmount = "%.2f".format(profitAmount),
                    profitRate = "%.2f".format(profitRate),
                    orderCount = orderCount,
                    receivableAmount = "%.2f".format(partnerBalances.receivableAmount),
                    payableAmount = "%.2f".format(partnerBalances.payableAmount),
                    topProducts = topProductItems,
                    error = if (failedSections.isNotEmpty()) errorMsg else null,
                    failedReportSections = failedSections
                )
            }
        }
    }

    fun setPeriod(index: Int) {
        val nextIndex = index.coerceIn(0, reportPeriods.lastIndex)
        if (nextIndex == _uiState.value.selectedPeriodIndex) return
        _uiState.update { it.copy(selectedPeriodIndex = nextIndex) }
        loadReports()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, failedReportSections = emptyList()) }
    }

    private suspend fun fetchPartnerBalances(dateRange: ReportDateRange): Result<PartnerBalanceSummary> =
        reportRepository.reconciliationSummary(dateRange.startAt, dateRange.endAt).map { summary ->
            PartnerBalanceSummary(
                receivableAmount = summary.totalReceivableAmount,
                payableAmount = summary.totalPayableAmount,
            )
        }

    private fun getPeriodMillis(period: ReportPeriod): ReportDateRange {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val startDate = when (period) {
            ReportPeriod.TODAY -> today
            ReportPeriod.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ReportPeriod.THIS_MONTH -> today.withDayOfMonth(1)
        }
        return ReportDateRange(
            startAt = startDate.startOfDayMillis(),
            endAt = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }
}

private fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun <T> Result<T>.messageOrNull(): String? =
    exceptionOrNull()?.message
