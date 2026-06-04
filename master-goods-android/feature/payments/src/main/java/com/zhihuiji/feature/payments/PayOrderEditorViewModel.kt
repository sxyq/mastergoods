package com.zhihuiji.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.order.CreatePayOrderV2Request
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.data.order.PayOrderV2Repository
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayOrderEditorUiState(
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val amount: Double = 0.0,
    val method: Int = 1,
    val referenceNo: String = "",
    val notes: String = "",
    val accountId: Long? = null,
    val supplierSearchResults: List<SupplierV2Dto> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class PayOrderEditorViewModel @Inject constructor(
    private val payOrderV2Repository: PayOrderV2Repository,
    private val supplierV2Repository: SupplierV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PayOrderEditorUiState())
    val uiState: StateFlow<PayOrderEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            supplierV2Repository.listSuppliers()
        }
    }

    fun selectSupplier(id: Long, name: String) {
        _uiState.value = _uiState.value.copy(supplierId = id, supplierName = name)
    }

    fun searchSuppliers(keyword: String) {
        viewModelScope.launch {
            supplierV2Repository.listSuppliers(keyword = keyword.ifBlank { null })
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(supplierSearchResults = list)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
                }
        }
    }

    fun updateAmount(amount: Double) { _uiState.value = _uiState.value.copy(amount = amount) }
    fun updateMethod(method: Int) { _uiState.value = _uiState.value.copy(method = method) }
    fun updateReferenceNo(ref: String) { _uiState.value = _uiState.value.copy(referenceNo = ref) }
    fun updateNotes(notes: String) { _uiState.value = _uiState.value.copy(notes = notes) }
    fun updateAccountId(accountId: Long?) { _uiState.value = _uiState.value.copy(accountId = accountId) }

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
            val request = CreatePayOrderV2Request(
                supplierId = _uiState.value.supplierId,
                supplierName = _uiState.value.supplierName,
                amount = _uiState.value.amount,
                method = _uiState.value.method,
                referenceNo = _uiState.value.referenceNo.ifBlank { null },
                notes = _uiState.value.notes.ifBlank { null },
                accountId = _uiState.value.accountId,
            )
            payOrderV2Repository.createPayOrder(request).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
