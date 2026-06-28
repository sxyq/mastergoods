package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.PartnerContactWriteV2Request
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierContactEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val phone: String = "",
    val title: String = "",
    val isPrimary: Boolean = false,
) {
    val canSave: Boolean
        get() = !isSaving && name.isNotBlank()
}

@HiltViewModel
class SupplierContactEditViewModel @Inject constructor(
    private val repository: SupplierV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierContactEditUiState())
    val uiState: StateFlow<SupplierContactEditUiState> = _uiState.asStateFlow()

    // 联系人无单独详情端点，通过供应商联系人列表按 id 查找
    fun loadContact(supplierId: Long, contactId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listContacts(supplierId)
                .onSuccess { list ->
                    val dto = list.firstOrNull { it.id == contactId }
                    if (dto != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEditMode = true,
                                name = dto.name,
                                phone = dto.phone.orEmpty(),
                                title = dto.title.orEmpty(),
                                isPrimary = dto.isPrimary,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = "联系人不存在")
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "联系人加载失败")
                    }
                }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun updatePhone(value: String) {
        _uiState.update { it.copy(phone = value, error = null) }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, error = null) }
    }

    fun togglePrimary() {
        _uiState.update { it.copy(isPrimary = !it.isPrimary) }
    }

    fun save(supplierId: Long, contactId: Long?) {
        val current = _uiState.value
        if (!current.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = PartnerContactWriteV2Request(
                partnerId = supplierId,
                name = current.name.trim(),
                phone = current.phone.trim().takeIf { it.isNotEmpty() },
                title = current.title.trim().takeIf { it.isNotEmpty() },
                isPrimary = current.isPrimary,
            )
            if (contactId != null) {
                repository.updateContact(contactId, request)
                    .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isSaving = false, error = error.message ?: "联系人更新失败")
                        }
                    }
            } else {
                repository.createContact(request)
                    .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isSaving = false, error = error.message ?: "联系人创建失败")
                        }
                    }
            }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
