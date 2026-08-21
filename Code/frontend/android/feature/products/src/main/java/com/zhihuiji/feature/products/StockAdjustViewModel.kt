package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.inventory.CreateInventoryLedgerEntryV2Request
import com.zhihuiji.data.product.ProductV2Repository
import com.zhihuiji.data.sync.InventoryV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockAdjustUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val productName: String? = null,
    val productCode: String = "",
    val categoryName: String = "",
    val unitName: String = "",
    val currentStock: Double = 0.0,
)

@HiltViewModel
class StockAdjustViewModel @Inject constructor(
    private val repository: ProductV2Repository,
    private val inventoryRepository: InventoryV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockAdjustUiState())
    val uiState: StateFlow<StockAdjustUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getProduct(productId)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            productName = dto.name,
                            productCode = dto.code,
                            categoryName = dto.categoryName,
                            unitName = dto.unitName,
                            currentStock = dto.stock
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun adjustStock(productId: Long, quantity: Double, reason: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // 库存调整走 v2/inventory/ledger 台账流水，不再全量回写商品。
            val request = CreateInventoryLedgerEntryV2Request(
                productId = productId,
                sourceType = "manual_adjust",
                sourceNo = null,
                quantityChange = quantity,
                unitCost = null,
                warehouseId = null,
                notes = reason,
            )
            inventoryRepository.createInventoryLedgerEntry(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            currentStock = it.currentStock + quantity,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
