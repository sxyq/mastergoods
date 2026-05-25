package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.CustomerDto
import com.zhihuiji.data.customer.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerListUiState(
    val customers: List<CustomerDto> = emptyList(),
    val keyword: String = "",
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()

    init { loadCustomers() }

    fun loadCustomers(keyword: String = _uiState.value.keyword) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, keyword = keyword)
            customerRepository.refreshCustomers(keyword.ifBlank { null })
            customerRepository.observeCustomers(keyword).collect { list ->
                _uiState.value = _uiState.value.copy(customers = list, isLoading = false)
            }
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch { customerRepository.deleteCustomer(id); loadCustomers() }
    }
}
