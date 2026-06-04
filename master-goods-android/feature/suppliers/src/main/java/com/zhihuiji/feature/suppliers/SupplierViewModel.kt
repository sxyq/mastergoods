package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierListUiState(
    val suppliers: List<SupplierV2Dto> = emptyList(),
    val keyword: String = "",
    val statusFilter: Int? = null,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val supplierV2Repository: SupplierV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierListUiState())
    val uiState: StateFlow<SupplierListUiState> = _uiState.asStateFlow()

    init { loadSuppliers() }

    fun loadSuppliers(keyword: String = _uiState.value.keyword, status: Int? = _uiState.value.statusFilter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, keyword = keyword, statusFilter = status)
            supplierV2Repository.listSuppliers(keyword.ifBlank { null }, status).onSuccess { list ->
                _uiState.value = _uiState.value.copy(suppliers = list, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun changeStatusFilter(status: Int?) { loadSuppliers(status = status) }
    fun deleteSupplier(id: Long) { viewModelScope.launch { supplierV2Repository.deleteSupplier(id); loadSuppliers() } }
}
