package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.data.customer.CustomerV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayList
import javax.inject.Inject

fun CustomerV2Dto.toCustomerItem(): CustomerItem = CustomerItem(
    id = id,
    name = name,
    phone = phone,
    receivableAmount = balance,
    receivable = MoneyFormatter.format(balance),
    hasDebt = status == 1 && balance > 0.0,
    status = StatusLabels.customerListStatus(status, balance)
)

data class CustomerListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val customers: List<CustomerItem> = emptyList(),
    val keyword: String = "",
    val selectedTabIndex: Int = 0
)

data class CustomerItem(
    val id: Long,
    val name: String,
    val phone: String,
    val receivableAmount: Double,
    val receivable: String,
    val hasDebt: Boolean,
    val status: String
)

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val repository: CustomerV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }
            val status = when (snapshot.selectedTabIndex) {
                1 -> 1 // 正常/欠款
                2 -> 1 // 欠款在UI层过滤
                3 -> 0 // 已停用
                else -> null
            }
            repository.listCustomers(
                keyword = snapshot.keyword.takeIf { it.isNotBlank() },
                status = status
            )
                .onSuccess { customers ->
                    val filtered = ArrayList<CustomerItem>(customers.size)
                    if (snapshot.selectedTabIndex == 2) {
                        for (customer in customers) {
                            val item = customer.toCustomerItem()
                            if (item.hasDebt) {
                                filtered.add(item)
                            }
                        }
                    } else {
                        for (customer in customers) {
                            filtered.add(customer.toCustomerItem())
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customers = filtered
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun search(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        loadCustomers()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        loadCustomers()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
