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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.ArrayList
import javax.inject.Inject

fun CustomerV2Dto.toCustomerItem(): CustomerItem = CustomerItem(
    id = id,
    name = name,
    phone = phone,
    groupName = groupName,
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
    val groupName: String?,
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
    private var loadJob: Job? = null

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        loadJob?.cancel()
        val snapshot = _uiState.value
        val status = when (snapshot.selectedTabIndex) {
            1, 2 -> 1
            3 -> 0
            else -> null
        }
        _uiState.update { it.copy(isLoading = false, error = null) }
        loadJob = viewModelScope.launch {
            val keyword = snapshot.keyword.takeIf { it.isNotBlank() }
            launch {
                repository.observeCustomers(keyword = keyword, status = status).collect { updateCustomers(it) }
            }
            repository.listCustomers(keyword = keyword, status = status)
                .onFailure { error ->
                    if (_uiState.value.customers.isNotEmpty()) {
                        _uiState.update { it.copy(error = error.message) }
                    }
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

    private fun updateCustomers(customers: List<CustomerV2Dto>) {
        val filtered = ArrayList<CustomerItem>(customers.size)
        for (customer in customers) {
            val item = customer.toCustomerItem()
            if (_uiState.value.selectedTabIndex != 2 || item.hasDebt) filtered.add(item)
        }
        _uiState.update { it.copy(customers = filtered) }
    }
}
