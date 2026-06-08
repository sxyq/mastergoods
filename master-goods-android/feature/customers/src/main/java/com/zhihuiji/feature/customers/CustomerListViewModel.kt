package com.zhihuiji.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.data.customer.CustomerV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

fun CustomerV2Dto.toCustomerItem(): CustomerItem = CustomerItem(
    id = id,
    name = name,
    phone = phone,
    receivable = "¥%.2f".format(balance),
    status = when (status) {
        1 -> if (balance > 0) "欠款" else "正常"
        0 -> "已停用"
        else -> "未知"
    }
)

data class CustomerListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val customers: List<CustomerItem> = emptyList(),
    val keyword: String = "",
    val selectedTabIndex: Int = 0
)

data class CustomerItem(
    val id: Long,
    val name: String,
    val phone: String,
    val receivable: String,
    val status: String
)

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val repository: CustomerV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val status = when (_uiState.value.selectedTabIndex) {
                1 -> 1 // 正常/欠款
                2 -> 1 // 欠款在UI层过滤
                3 -> 0 // 已停用
                else -> null
            }
            repository.listCustomers(
                keyword = _uiState.value.keyword.takeIf { it.isNotBlank() },
                status = status
            )
                .onSuccess { customers ->
                    val items = customers.map { it.toCustomerItem() }
                    val filtered = when (_uiState.value.selectedTabIndex) {
                        2 -> items.filter { it.status == "欠款" }
                        else -> items
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customers = filtered
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
        loadCustomers()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        loadCustomers()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
