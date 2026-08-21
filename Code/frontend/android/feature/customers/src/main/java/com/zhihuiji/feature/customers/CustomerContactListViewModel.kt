package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto
import com.zhihuiji.data.customer.CustomerV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactItem(
    val id: Long,
    val name: String,
    val phone: String,
    val title: String,
    val isPrimary: Boolean,
)

data class CustomerContactListUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val contacts: List<ContactItem> = emptyList(),
)

@HiltViewModel
class CustomerContactListViewModel @Inject constructor(
    private val repository: CustomerV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerContactListUiState())
    val uiState: StateFlow<CustomerContactListUiState> = _uiState.asStateFlow()

    fun loadContacts(customerId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listContacts(customerId)
                .onSuccess { list ->
                    val items = list.map { it.toContactItem() }
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

private fun PartnerContactV2Dto.toContactItem(): ContactItem = ContactItem(
    id = id,
    name = name,
    phone = phone.orEmpty(),
    title = title.orEmpty(),
    isPrimary = isPrimary,
)
