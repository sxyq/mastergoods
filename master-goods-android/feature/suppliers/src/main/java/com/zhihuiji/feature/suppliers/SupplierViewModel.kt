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

data class SupplierListUiState(
    val suppliers: List<SupplierDto> = emptyList(),
    val keyword: String = "",
    val statusFilter: Int? = null,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val supplierRepository: SupplierRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierListUiState())
    val uiState: StateFlow<SupplierListUiState> = _uiState.asStateFlow()

    init { loadSuppliers() }

    fun loadSuppliers(keyword: String = _uiState.value.keyword, status: Int? = _uiState.value.statusFilter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, keyword = keyword, statusFilter = status)
            supplierRepository.refreshSuppliers(keyword.ifBlank { null }, status)
            supplierRepository.observeSuppliers(keyword, status).collect { list ->
                _uiState.value = _uiState.value.copy(suppliers = list, isLoading = false)
            }
        }
    }

    fun changeStatusFilter(status: Int?) { loadSuppliers(status = status) }
    fun deleteSupplier(id: Long) { viewModelScope.launch { supplierRepository.deleteSupplier(id); loadSuppliers() } }
}
