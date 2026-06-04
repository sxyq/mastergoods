package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.partner.CustomerWriteV2Request
import com.zhihuiji.data.customer.CustomerV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerV2Draft(
    val name: String = "",
    val phone: String = "",
    val level: Int = 0,
    val groupId: Long? = null,
    val primaryContactName: String? = null,
    val primaryContactPhone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val status: Int = 1,
) {
    fun toWriteRequest(): CustomerWriteV2Request = CustomerWriteV2Request(
        name = name,
        phone = phone,
        level = level,
        groupId = groupId,
        primaryContactName = primaryContactName,
        primaryContactPhone = primaryContactPhone,
        address = address,
        notes = notes,
        status = status,
    )
}

data class CustomerEditorUiState(
    val draft: CustomerV2Draft = CustomerV2Draft(),
    val existingId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class CustomerEditorViewModel @Inject constructor(
    private val customerV2Repository: CustomerV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerEditorUiState())
    val uiState: StateFlow<CustomerEditorUiState> = _uiState.asStateFlow()

    fun loadCustomer(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            customerV2Repository.getCustomer(id).onSuccess { dto ->
                _uiState.value = _uiState.value.copy(
                    existingId = id,
                    draft = CustomerV2Draft(
                        name = dto.name,
                        phone = dto.phone,
                        level = dto.level,
                        groupId = dto.groupId,
                        primaryContactName = dto.primaryContactName,
                        primaryContactPhone = dto.primaryContactPhone,
                        address = dto.address,
                        notes = dto.notes,
                        status = dto.status,
                    ),
                    isLoading = false,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun updateDraft(update: (CustomerV2Draft) -> CustomerV2Draft) {
        _uiState.value = _uiState.value.copy(draft = update(_uiState.value.draft))
    }

    fun saveCustomer() {
        val draft = _uiState.value.draft
        if (draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("客户名称不能为空"))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val request = draft.toWriteRequest()
            val existingId = _uiState.value.existingId
            val result = if (existingId != null) {
                customerV2Repository.updateCustomer(existingId, request)
            } else {
                customerV2Repository.createCustomer(request)
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
