package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.SupplierDto
import com.zhihuiji.data.supplier.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierEditorUiState(
    val draft: SupplierDto = SupplierDto(),
    val existingId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SupplierEditorViewModel @Inject constructor(
    private val supplierRepository: SupplierRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierEditorUiState())
    val uiState: StateFlow<SupplierEditorUiState> = _uiState.asStateFlow()

    fun loadSupplier(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            supplierRepository.getSupplier(id).onSuccess { dto ->
                _uiState.value = _uiState.value.copy(existingId = id, draft = dto, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun updateDraft(update: (SupplierDto) -> SupplierDto) {
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
                supplierRepository.updateSupplier(existingId, draft)
            } else {
                supplierRepository.createSupplier(draft)
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
