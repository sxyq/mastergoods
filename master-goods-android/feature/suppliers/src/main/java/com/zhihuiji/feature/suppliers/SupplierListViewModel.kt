package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

fun SupplierV2Dto.toSupplierItem(): SupplierItem = SupplierItem(
    id = id,
    name = name,
    contact = primaryContactName?.let { "$it $phone" } ?: phone,
    payable = "¥%.2f".format(balance),
    status = when (status) {
        1 -> "正常"
        0 -> "停用"
        else -> "未知"
    }
)

data class SupplierListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val suppliers: List<SupplierItem> = emptyList(),
    val keyword: String = "",
    val selectedTab: Int = 0
)

data class SupplierItem(
    val id: Long,
    val name: String,
    val contact: String,
    val payable: String,
    val status: String
)

@HiltViewModel
class SupplierListViewModel @Inject constructor(
    private val repository: SupplierV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierListUiState())
    val uiState: StateFlow<SupplierListUiState> = _uiState.asStateFlow()

    init {
        loadSuppliers()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val status = when (_uiState.value.selectedTab) {
                1 -> 1 // 正常
                2 -> 0 // 停用
                else -> null
            }
            repository.listSuppliers(
                keyword = _uiState.value.keyword.takeIf { it.isNotBlank() },
                status = status
            )
                .onSuccess { suppliers ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            suppliers = suppliers.map { dto -> dto.toSupplierItem() }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun search(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        loadSuppliers()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        loadSuppliers()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
