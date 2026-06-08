package com.zhihuiji.feature.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.order.PurchaseOrderItemV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PurchaseOrderDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val order: PurchaseOrderV2Dto? = null,
)

@HiltViewModel
class PurchaseOrderDetailViewModel @Inject constructor(
    private val repository: PurchaseOrderV2Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle.get<Long>("orderId")) { "orderId is required" }

    private val _uiState = MutableStateFlow(PurchaseOrderDetailUiState())
    val uiState: StateFlow<PurchaseOrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getPurchaseOrder(orderId)
                .onSuccess { order ->
                    _uiState.update { it.copy(isLoading = false, order = order) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun deleteOrder(onDeleteSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.deletePurchaseOrder(orderId)
                .onSuccess {
                    onDeleteSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

fun PurchaseOrderV2Dto.statusText(): String = when (status) {
    0 -> "草稿"
    1 -> "已确认"
    2 -> "已取消"
    else -> "未知"
}

fun PurchaseOrderV2Dto.createdAtText(): String = if (createdAt > 0) {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(createdAt))
} else ""

fun PurchaseOrderItemV2Dto.subtotalText(): String = "¥%.2f".format(amount)
