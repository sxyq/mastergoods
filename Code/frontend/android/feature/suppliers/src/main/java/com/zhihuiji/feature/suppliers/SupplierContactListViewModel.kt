package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierContactItem(
    val id: Long,
    val name: String,
    val phone: String,
    val title: String,
    val isPrimary: Boolean,
)

data class SupplierContactListUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val contacts: List<SupplierContactItem> = emptyList(),
)

@HiltViewModel
class SupplierContactListViewModel @Inject constructor(
    private val repository: SupplierV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierContactListUiState())
    val uiState: StateFlow<SupplierContactListUiState> = _uiState.asStateFlow()

    fun loadContacts(supplierId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listContacts(supplierId)
                .onSuccess { list ->
                    val items = list.map { it.toSupplierContactItem() }
                    _uiState.update { it.copy(isLoading = false, contacts = items) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "联系人加载失败")
                    }
                }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            repository.deleteContact(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isDeleting = false,
                            contacts = state.contacts.filterNot { it.id == id },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isDeleting = false, error = error.message ?: "联系人删除失败")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun PartnerContactV2Dto.toSupplierContactItem(): SupplierContactItem = SupplierContactItem(
    id = id,
    name = name,
    phone = phone.orEmpty(),
    title = title.orEmpty(),
    isPrimary = isPrimary,
)
