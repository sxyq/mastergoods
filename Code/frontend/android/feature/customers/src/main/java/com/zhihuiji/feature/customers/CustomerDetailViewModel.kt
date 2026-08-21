package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.data.customer.CustomerV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDetail(
    val id: Long,
    val name: String,
    val phone: String,
    val groupName: String?,
    val primaryContactName: String?,
    val primaryContactPhone: String?,
    val address: String?,
    val balance: Double,
    val status: Int,
    val remark: String?,
)

data class CustomerDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val customer: CustomerDetail? = null,
    val isDeleted: Boolean = false,
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val repository: CustomerV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    fun loadCustomer(customerId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getCustomer(customerId)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customer = dto.toCustomerDetail(),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deleteCustomer(customerId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isDeleted = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}

private fun CustomerV2Dto.toCustomerDetail(): CustomerDetail = CustomerDetail(
    id = id,
    name = name,
    phone = phone,
    groupName = groupName,
    primaryContactName = primaryContactName,
    primaryContactPhone = primaryContactPhone,
    address = address,
    balance = balance,
    status = status,
    remark = notes,
)
