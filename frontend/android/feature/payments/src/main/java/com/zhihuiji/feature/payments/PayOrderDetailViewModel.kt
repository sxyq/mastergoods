package com.zhihuiji.feature.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.PayOrderV2Dto
import com.zhihuiji.data.order.PayOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayOrderDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val order: PayOrderV2Dto? = null,
)

@HiltViewModel
class PayOrderDetailViewModel @Inject constructor(
    private val repository: PayOrderV2Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle.get<Long>("orderId")) { "orderId is required" }

    private val _uiState = MutableStateFlow(PayOrderDetailUiState())
    val uiState: StateFlow<PayOrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getPayOrder(orderId)
                .onSuccess { order ->
                    _uiState.update { it.copy(isLoading = false, order = order) }
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

fun PayOrderV2Dto.createdAtText(): String =
    if (createdAt > 0) TimeFormatter.formatDateTime(createdAt) else ""
