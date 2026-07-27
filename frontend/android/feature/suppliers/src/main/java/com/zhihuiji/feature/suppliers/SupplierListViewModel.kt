package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

fun SupplierV2Dto.toSupplierItem(): SupplierItem {
    val contact = splitSupplierContact(primaryContactName, phone)
    return SupplierItem(
        id = id,
        name = name,
        groupName = groupName,
        contactName = contact.first,
        contactPhone = contact.second,
        payableAmount = balance,
        payable = MoneyFormatter.format(balance),
        isStopped = status == 0,
        status = StatusLabels.supplierStatus(status)
    )
}

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
    val groupName: String?,
    val contactName: String,
    val contactPhone: String,
    val payableAmount: Double,
    val payable: String,
    val isStopped: Boolean,
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

private fun splitSupplierContact(primaryContactName: String?, phone: String): Pair<String, String> {
    val trimmedName = primaryContactName?.trim().orEmpty()
    val trimmedPhone = phone.trim()
    return when {
        trimmedName.isNotBlank() -> trimmedName to if (trimmedPhone.isNotBlank()) trimmedPhone else "暂无电话"
        trimmedPhone.isBlank() -> "暂无联系人" to "暂无电话"
        trimmedPhone.any { it.isDigit() } -> "暂无联系人" to trimmedPhone
        else -> trimmedPhone to "暂无电话"
    }
}
