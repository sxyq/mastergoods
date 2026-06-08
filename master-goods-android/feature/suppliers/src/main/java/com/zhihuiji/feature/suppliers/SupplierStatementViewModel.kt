package com.zhihuiji.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.PayOrderV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.data.order.PayOrderV2Repository
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import com.zhihuiji.data.supplier.SupplierV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierStatementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val supplier: SupplierStatementSupplier? = null,
    val transactions: List<SupplierStatementTransaction> = emptyList(),
    val purchaseTotal: Double = 0.0,
    val paymentTotal: Double = 0.0,
    val contractWarning: String? = null,
)

data class SupplierStatementSupplier(
    val id: Long,
    val name: String,
    val contactName: String?,
    val phone: String,
    val balance: Double,
    val status: Int,
)

data class SupplierStatementTransaction(
    val id: Long,
    val kind: SupplierStatementTransactionKind,
    val title: String,
    val date: String,
    val amountText: String,
    val statusText: String,
    val timestamp: Long,
)

enum class SupplierStatementTransactionKind {
    PURCHASE,
    PAYMENT,
}

@HiltViewModel
class SupplierStatementViewModel @Inject constructor(
    private val supplierRepository: SupplierV2Repository,
    private val purchaseOrderRepository: PurchaseOrderV2Repository,
    private val payOrderRepository: PayOrderV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierStatementUiState())
    val uiState: StateFlow<SupplierStatementUiState> = _uiState.asStateFlow()

    fun loadStatement(supplierId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, contractWarning = null) }

            val supplierResult = supplierRepository.getSupplier(supplierId)
            val supplier = supplierResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "供应商对账信息加载失败"
                    )
                }
                return@launch
            }

            val purchaseResult = purchaseOrderRepository.listPurchaseOrders()
            val payResult = payOrderRepository.listPayOrders()

            val purchases = purchaseResult.getOrNull()
                ?.filter { it.matchesSupplier(supplier) }
                .orEmpty()
            val payments = payResult.getOrNull()
                ?.filter { it.matchesSupplier(supplier) }
                .orEmpty()

            val warning = listOfNotNull(
                purchaseResult.exceptionOrNull()?.message?.let { "采购单读取失败：$it" },
                payResult.exceptionOrNull()?.message?.let { "付款单读取失败：$it" },
            ).takeIf { it.isNotEmpty() }?.joinToString("\n")

            _uiState.update {
                it.copy(
                    isLoading = false,
                    supplier = supplier.toStatementSupplier(),
                    transactions = (purchases.map { order -> order.toStatementTransaction() } +
                        payments.map { order -> order.toStatementTransaction() })
                        .sortedByDescending { transaction -> transaction.timestamp }
                        .take(20),
                    purchaseTotal = purchases.sumOf { order -> order.totalAmount },
                    paymentTotal = payments.sumOf { order -> order.amount },
                    contractWarning = warning,
                )
            }
        }
    }
}

private fun SupplierV2Dto.toStatementSupplier() = SupplierStatementSupplier(
    id = id,
    name = name,
    contactName = primaryContactName,
    phone = phone,
    balance = balance,
    status = status,
)

private fun PurchaseOrderV2Dto.toStatementTransaction() = SupplierStatementTransaction(
    id = id,
    kind = SupplierStatementTransactionKind.PURCHASE,
    title = "采购单 $orderNo",
    date = TimeFormatter.formatDate(createdAt),
    amountText = "+ ${formatCurrency(totalAmount)}",
    statusText = StatusLabels.purchaseOrderStatus(status),
    timestamp = createdAt,
)

private fun PayOrderV2Dto.toStatementTransaction() = SupplierStatementTransaction(
    id = id,
    kind = SupplierStatementTransactionKind.PAYMENT,
    title = "付款单 $orderNo",
    date = TimeFormatter.formatDate(createdAt),
    amountText = "- ${formatCurrency(amount)}",
    statusText = StatusLabels.payOrderStatus(status),
    timestamp = createdAt,
)

private fun PurchaseOrderV2Dto.matchesSupplier(supplier: SupplierV2Dto): Boolean =
    supplierId == supplier.id || supplierName.matchesSupplierName(supplier.name)

private fun PayOrderV2Dto.matchesSupplier(supplier: SupplierV2Dto): Boolean =
    supplierId == supplier.id || supplierName.matchesSupplierName(supplier.name)

private fun String?.matchesSupplierName(name: String): Boolean =
    !this.isNullOrBlank() && name.isNotBlank() && equals(name, ignoreCase = true)

private fun formatCurrency(value: Double): String = "¥%.2f".format(value)
