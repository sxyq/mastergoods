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

data class CustomerDetailUiState(
    val customer: CustomerV2Dto? = null,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val customerV2Repository: CustomerV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    fun loadCustomer(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            customerV2Repository.getCustomer(id).onSuccess {
                _uiState.value = _uiState.value.copy(customer = it, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }
}
