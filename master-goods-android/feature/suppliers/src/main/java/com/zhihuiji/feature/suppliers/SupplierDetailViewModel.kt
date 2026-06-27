package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierDetail(
    val id: Long,
    val name: String,
    val groupName: String?,
    val primaryContactName: String?,
    val primaryContactPhone: String?,
    val phone: String,
    val address: String?,
    val balance: Double,
    val status: Int,
    val remark: String?,
)

data class SupplierDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val supplier: SupplierDetail? = null
)

@HiltViewModel
class SupplierDetailViewModel @Inject constructor(
    private val repository: SupplierV2Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierDetailUiState())
    val uiState: StateFlow<SupplierDetailUiState> = _uiState.asStateFlow()

    fun loadSupplier(supplierId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getSupplier(supplierId)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            supplier = SupplierDetail(
                                id = dto.id,
                                name = dto.name,
                                groupName = dto.groupName,
                                primaryContactName = dto.primaryContactName,
                                primaryContactPhone = dto.primaryContactPhone,
                                phone = dto.phone,
                                address = dto.address,
                                balance = dto.balance,
                                status = dto.status,
                                remark = dto.notes,
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun deleteSupplier(supplierId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deleteSupplier(supplierId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, supplier = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
