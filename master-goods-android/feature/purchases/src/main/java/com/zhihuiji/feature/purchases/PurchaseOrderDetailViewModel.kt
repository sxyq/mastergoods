package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseOrderDetailUiState(
    val order: PurchaseOrderV2Dto? = null,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class PurchaseOrderDetailViewModel @Inject constructor(
    private val purchaseOrderV2Repository: PurchaseOrderV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseOrderDetailUiState())
    val uiState: StateFlow<PurchaseOrderDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            purchaseOrderV2Repository.getPurchaseOrder(id).onSuccess { order ->
                _uiState.value = _uiState.value.copy(order = order, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    // TODO: cancelOrder - waiting for PurchaseOrderV2Repository.cancelPurchaseOrder()
    // fun cancelOrder(id: Long) { ... }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
