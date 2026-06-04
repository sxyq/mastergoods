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

data class SupplierDetailUiState(
    val supplier: SupplierV2Dto? = null,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SupplierDetailViewModel @Inject constructor(
    private val supplierV2Repository: SupplierV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierDetailUiState())
    val uiState: StateFlow<SupplierDetailUiState> = _uiState.asStateFlow()

    fun loadSupplier(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            supplierV2Repository.getSupplier(id).onSuccess {
                _uiState.value = _uiState.value.copy(supplier = it, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }
}
