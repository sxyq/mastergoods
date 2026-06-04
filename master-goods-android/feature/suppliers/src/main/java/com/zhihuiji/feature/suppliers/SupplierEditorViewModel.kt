package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.core.model.v2.partner.SupplierWriteV2Request
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierV2Draft(
    val name: String = "",
    val phone: String = "",
    val groupId: Long? = null,
    val primaryContactName: String? = null,
    val primaryContactPhone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val status: Int = 1,
)

fun SupplierV2Dto.toDraft() = SupplierV2Draft(
    name = name,
    phone = phone,
    groupId = groupId,
    primaryContactName = primaryContactName,
    primaryContactPhone = primaryContactPhone,
    address = address,
    notes = notes,
    status = status,
)

fun SupplierV2Draft.toWriteRequest() = SupplierWriteV2Request(
    name = name,
    phone = phone,
    groupId = groupId,
    primaryContactName = primaryContactName,
    primaryContactPhone = primaryContactPhone,
    address = address,
    notes = notes,
    status = status,
)

data class SupplierEditorUiState(
    val draft: SupplierV2Draft = SupplierV2Draft(),
    val existingId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SupplierEditorViewModel @Inject constructor(
    private val supplierV2Repository: SupplierV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierEditorUiState())
    val uiState: StateFlow<SupplierEditorUiState> = _uiState.asStateFlow()

    fun loadSupplier(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            supplierV2Repository.getSupplier(id).onSuccess { dto ->
                _uiState.value = _uiState.value.copy(existingId = id, draft = dto.toDraft(), isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun updateDraft(update: (SupplierV2Draft) -> SupplierV2Draft) {
        _uiState.value = _uiState.value.copy(draft = update(_uiState.value.draft))
    }

    fun saveSupplier() {
        val draft = _uiState.value.draft
        if (draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("供应商名称不能为空"))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val existingId = _uiState.value.existingId
            val result = if (existingId != null) {
                supplierV2Repository.updateSupplier(existingId, draft.toWriteRequest())
            } else {
                supplierV2Repository.createSupplier(draft.toWriteRequest())
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
