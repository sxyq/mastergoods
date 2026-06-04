package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.data.customer.CustomerV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerListUiState(
    val customers: List<CustomerV2Dto> = emptyList(),
    val keyword: String = "",
    val statusFilter: Int = 0,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
) {
    val filteredCustomers: List<CustomerV2Dto> get() = customers.filter { customer ->
        when (statusFilter) {
            1 -> customer.status == 1
            2 -> customer.balance > 0
            3 -> customer.status != 1
            else -> true
        }
    }
}

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerV2Repository: CustomerV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()

    init { loadCustomers() }

    fun loadCustomers(keyword: String = _uiState.value.keyword) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, keyword = keyword)
            customerV2Repository.listCustomers(keyword.ifBlank { null })
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(customers = list, isLoading = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
                }
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            customerV2Repository.deleteCustomer(id)
            loadCustomers()
        }
    }

    fun setStatusFilter(filter: Int) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
    }
}
