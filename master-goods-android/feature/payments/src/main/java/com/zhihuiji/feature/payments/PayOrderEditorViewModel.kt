package com.zhihuiji.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.CreatePayOrderRequest
import com.zhihuiji.core.model.SupplierDto
import com.zhihuiji.data.order.PayOrderRepository
import com.zhihuiji.data.supplier.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayOrderEditorUiState(
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val amount: Double = 0.0,
    val method: Int = 1,
    val referenceNo: String = "",
    val notes: String = "",
    val supplierSearchResults: List<SupplierDto> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class PayOrderEditorViewModel @Inject constructor(
    private val payOrderRepository: PayOrderRepository,
    private val supplierRepository: SupplierRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PayOrderEditorUiState())
    val uiState: StateFlow<PayOrderEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            supplierRepository.refreshSuppliers(null, null)
        }
    }

    fun selectSupplier(id: Long, name: String) {
        _uiState.value = _uiState.value.copy(supplierId = id, supplierName = name)
    }

    fun searchSuppliers(keyword: String) {
        viewModelScope.launch {
            supplierRepository.refreshSuppliers(keyword.ifBlank { null }, null)
            val list = supplierRepository.observeSuppliers(keyword, null).first()
            _uiState.value = _uiState.value.copy(supplierSearchResults = list)
        }
    }

    fun updateAmount(amount: Double) { _uiState.value = _uiState.value.copy(amount = amount) }
    fun updateMethod(method: Int) { _uiState.value = _uiState.value.copy(method = method) }
    fun updateReferenceNo(ref: String) { _uiState.value = _uiState.value.copy(referenceNo = ref) }
    fun updateNotes(notes: String) { _uiState.value = _uiState.value.copy(notes = notes) }

    fun submitOrder() {
        if (_uiState.value.supplierName.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("请选择供应商"))
            return
        }
        if (_uiState.value.amount <= 0) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("付款金额必须大于0"))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val request = CreatePayOrderRequest(
                supplierId = _uiState.value.supplierId,
                supplierName = _uiState.value.supplierName,
                amount = _uiState.value.amount,
                method = _uiState.value.method,
                referenceNo = _uiState.value.referenceNo.ifBlank { null },
                notes = _uiState.value.notes.ifBlank { null },
            )
            payOrderRepository.createPayOrder(request).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
