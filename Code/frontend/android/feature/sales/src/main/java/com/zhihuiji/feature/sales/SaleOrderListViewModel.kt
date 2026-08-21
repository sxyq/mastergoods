package com.zhihuiji.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Filter
import com.zhihuiji.data.order.SaleOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

fun SaleOrderV2Dto.toSaleOrderItem(): SaleOrderItem = SaleOrderItem(
    id = id,
    orderNo = orderNo,
    customerName = customerName ?: "",
    amount = MoneyFormatter.format(totalAmount),
    status = StatusLabels.saleOrderStatus(status),
    paymentStatus = paymentStatusLabel(),
    timeLabel = createdAt.toDisplayTimeLabel()
)

data class SaleOrderListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val orders: List<SaleOrderItem> = emptyList(),
    val keyword: String = "",
    val selectedTab: Int = 0
)

data class SaleOrderItem(
    val id: Long,
    val orderNo: String,
    val customerName: String,
    val amount: String,
    val status: String,
    val paymentStatus: String,
    val timeLabel: String
)

@HiltViewModel
class SaleOrderListViewModel @Inject constructor(
    private val repository: SaleOrderV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleOrderListUiState())
    val uiState: StateFlow<SaleOrderListUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadOrders()
    }

    fun loadOrders() {
        loadJob?.cancel()
        val currentState = _uiState.value
        val filter = SaleOrderV2Filter(
            keyword = currentState.keyword.takeIf { it.isNotBlank() },
            status = currentState.selectedTab.takeIf { it > 0 }?.let {
                when (it) {
                    1 -> 0
                    2 -> 1
                    3 -> 2
                    else -> null
                }
            }
        )
        _uiState.update { it.copy(isLoading = false, error = null) }
        loadJob = viewModelScope.launch {
            launch {
                repository.observeSaleOrders(filter).collect { orders ->
                    _uiState.update { it.copy(orders = orders.map(SaleOrderV2Dto::toSaleOrderItem)) }
                }
            }
            repository.listSaleOrders(filter).onFailure { error ->
                if (_uiState.value.orders.isNotEmpty()) {
                    _uiState.update { it.copy(error = error.message) }
                }
            }
        }
    }

    fun search(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        loadOrders()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        loadOrders()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun SaleOrderV2Dto.paymentStatusLabel(): String = when {
    status == 2 -> "已取消"
    status == 0 -> "草稿"
    totalAmount <= 0.0 -> "待收款"
    paidAmount + 0.0001 >= totalAmount -> "已收款"
    else -> "待收款"
}

private fun Long.toDisplayTimeLabel(): String {
    if (this <= 0L) return "-"
    val zoneId = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
    val today = LocalDate.now(zoneId)
    val date = dateTime.toLocalDate()
    return when (date) {
        today -> "今天 ${TIME_FORMATTER.format(dateTime)}"
        today.minusDays(1) -> "昨天 ${TIME_FORMATTER.format(dateTime)}"
        else -> DATE_FORMATTER.format(dateTime)
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM月dd日")
