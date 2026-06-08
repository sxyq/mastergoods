package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.SupplierWriteV2Request
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val primaryContactName: String = "",
    val phone: String = "",
    val address: String = "",
    val remark: String = "",
)

@HiltViewModel
class SupplierEditViewModel @Inject constructor(
    private val repository: SupplierV2Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierEditUiState())
    val uiState: StateFlow<SupplierEditUiState> = _uiState.asStateFlow()

    fun loadSupplier(supplierId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getSupplier(supplierId)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            name = dto.name,
                            primaryContactName = dto.primaryContactName ?: "",
                            phone = dto.phone,
                            address = dto.address ?: "",
                            remark = dto.notes ?: "",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun createSupplier(
        name: String,
        primaryContactName: String?,
        phone: String,
        address: String?,
        remark: String?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = SupplierWriteV2Request(
                name = name,
                phone = phone,
                primaryContactName = primaryContactName,
                address = address,
                notes = remark,
                status = 1,
            )
            repository.createSupplier(request)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateSupplier(
        supplierId: Long,
        name: String,
        primaryContactName: String?,
        phone: String,
        address: String?,
        remark: String?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = SupplierWriteV2Request(
                name = name,
                phone = phone,
                primaryContactName = primaryContactName,
                address = address,
                notes = remark,
                status = null,
            )
            repository.updateSupplier(supplierId, request)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
