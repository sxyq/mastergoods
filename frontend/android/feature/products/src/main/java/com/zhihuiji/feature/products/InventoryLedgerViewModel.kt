package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.inventory.InventoryLedgerEntryV2Dto
import com.zhihuiji.data.sync.InventoryV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryLedgerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val productName: String = "",
    val productCode: String = "",
    val entries: List<InventoryLedgerItem> = emptyList(),
) {
    private val summary: InventoryLedgerSummary
        get() = entries.fold(InventoryLedgerSummary(), InventoryLedgerSummary::accumulate)

    val totalIn: Double get() = summary.totalIn
    val totalOut: Double get() = summary.totalOut
    val latestBalance: Double? get() = entries.firstOrNull()?.quantityAfter
}

data class InventoryLedgerItem(
    val id: Long,
    val productName: String,
    val productCode: String,
    val quantityBefore: Double?,
    val quantityChange: Double,
    val quantityAfter: Double?,
    val unitCost: Double?,
    val sourceType: String,
    val sourceNo: String?,
    val notes: String?,
    val createdAt: String,
)

private data class InventoryLedgerSummary(
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
) {
    fun accumulate(item: InventoryLedgerItem): InventoryLedgerSummary {
        return if (item.quantityChange > 0) {
            copy(totalIn = totalIn + item.quantityChange)
        } else if (item.quantityChange < 0) {
            copy(totalOut = totalOut - item.quantityChange)
        } else {
            this
        }
    }
}

@HiltViewModel
class InventoryLedgerViewModel @Inject constructor(
    private val repository: InventoryV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InventoryLedgerUiState())
    val uiState: StateFlow<InventoryLedgerUiState> = _uiState.asStateFlow()

    fun loadLedger(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listInventoryLedger(productId = productId)
                .onSuccess { entries ->
                    val items = entries.map(InventoryLedgerEntryV2Dto::toLedgerItem)
                    val first = items.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            productName = first?.productName.orEmpty(),
                            productCode = first?.productCode.orEmpty(),
                            entries = items,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "库存流水加载失败",
                        )
                    }
                }
        }
    }
}

private fun InventoryLedgerEntryV2Dto.toLedgerItem(): InventoryLedgerItem =
    InventoryLedgerItem(
        id = id,
        productName = productName,
        productCode = productCode,
        quantityBefore = quantityBefore,
        quantityChange = quantityChange,
        quantityAfter = quantityAfter,
        unitCost = unitCost,
        sourceType = sourceType,
        sourceNo = sourceNo,
        notes = notes,
        createdAt = TimeFormatter.formatDateTime(createdAt),
    )
