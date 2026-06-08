package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.CustomerWriteV2Request
import com.zhihuiji.data.customer.CustomerV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val remark: String = "",
)

@HiltViewModel
class CustomerEditViewModel @Inject constructor(
    private val repository: CustomerV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerEditUiState())
    val uiState: StateFlow<CustomerEditUiState> = _uiState.asStateFlow()

    fun loadCustomer(customerId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getCustomer(customerId)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            name = dto.name,
                            phone = dto.phone,
                            address = dto.address ?: "",
                            remark = dto.notes ?: "",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun createCustomer(
        name: String,
        phone: String,
        address: String?,
        remark: String?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = CustomerWriteV2Request(
                name = name,
                phone = phone,
                level = 0,
                address = address,
                notes = remark,
                status = 1,
            )
            repository.createCustomer(request)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String,
        address: String?,
        remark: String?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = CustomerWriteV2Request(
                name = name,
                phone = phone,
                level = 0,
                address = address,
                notes = remark,
                status = null,
            )
            repository.updateCustomer(customerId, request)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
