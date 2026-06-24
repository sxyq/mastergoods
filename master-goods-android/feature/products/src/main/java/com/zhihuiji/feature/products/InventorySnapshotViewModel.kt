package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.inventory.CreateInventorySnapshotV2Request
import com.zhihuiji.core.model.v2.inventory.InventorySnapshotV2Dto
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.data.product.ProductV2Repository
import com.zhihuiji.data.sync.SyncV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventorySnapshotUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
    val snapshotDate: Long = todayStartMillis(),
    val items: List<InventoryCountItem> = emptyList(),
    val countedItems: Int = 0,
    val gainQuantity: Double = 0.0,
    val lossQuantity: Double = 0.0,
) {
    val snapshotDateLabel: String get() = TimeFormatter.formatDate(snapshotDate)
    val totalItems: Int get() = items.size
    val canComplete: Boolean get() = !isLoading && !isSubmitting && items.isNotEmpty()
}

data class InventoryCountItem(
    val productId: Long,
    val productName: String,
    val productCode: String,
    val categoryName: String,
    val systemQuantity: Double,
    val countedQuantity: Double?,
    val unitCost: Double?,
    val totalValue: Double?,
    val snapshotCreatedAt: String?,
) {
    val isCounted: Boolean get() = countedQuantity != null
    val difference: Double? get() = countedQuantity?.let { it - systemQuantity }
    val isBalanced: Boolean get() = difference?.let { abs(it) < 0.0001 } == true
}

@HiltViewModel
class InventorySnapshotViewModel @Inject constructor(
    private val productRepository: ProductV2Repository,
    private val syncRepository: SyncV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InventorySnapshotUiState())
    val uiState: StateFlow<InventorySnapshotUiState> = _uiState.asStateFlow()

    fun loadTodaySnapshots() {
        viewModelScope.launch {
            loadInventory(snapshotDate = todayStartMillis())
        }
    }

    fun completeInventoryCount() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (!currentState.canComplete) return@launch

            val pendingItems = currentState.items.filterNot { it.isCounted }
            if (pendingItems.isEmpty()) {
                _uiState.update {
                    it.copy(statusMessage = "今日盘点快照已全部生成。")
                }
                return@launch
            }

            _uiState.update {
                it.copy(isSubmitting = true, error = null, statusMessage = null)
            }

            var createdCount = 0
            var failedCount = 0
            pendingItems.forEach { item ->
                syncRepository.createInventorySnapshot(
                    CreateInventorySnapshotV2Request(
                        productId = item.productId,
                        snapshotDate = currentState.snapshotDate,
                    )
                )
                    .onSuccess { createdCount++ }
                    .onFailure { failedCount++ }
            }

            val message =
                if (failedCount == 0) {
                    "已生成 $createdCount 条真实库存盘点快照。"
                } else {
                    "已生成 $createdCount 条盘点快照，$failedCount 条提交失败，请稍后重试。"
                }

            loadInventory(
                snapshotDate = currentState.snapshotDate,
                statusMessage = message,
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadInventory(snapshotDate = _uiState.value.snapshotDate)
        }
    }

    private suspend fun loadInventory(
        snapshotDate: Long,
        statusMessage: String? = null,
    ) {
        _uiState.update {
            it.copy(
                isLoading = true,
                isSubmitting = false,
                error = null,
                statusMessage = statusMessage,
                snapshotDate = snapshotDate,
            )
        }

        val products = productRepository.listProducts().getOrElse { error ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSubmitting = false,
                    error = error.message ?: "商品列表加载失败",
                )
            }
            return
        }

        val snapshots = syncRepository.listInventorySnapshots(snapshotDate = snapshotDate).getOrElse { error ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSubmitting = false,
                    error = error.message ?: "库存盘点快照加载失败",
                )
            }
            return
        }

        val snapshotsByProduct = snapshots.associateBy { it.productId }
        val sortedProducts = products.sortedWith(inventoryProductComparator)
        val items = ArrayList<InventoryCountItem>(sortedProducts.size)
        var countedItems = 0
        var gainQuantity = 0.0
        var lossQuantity = 0.0
        for (product in sortedProducts) {
            val item = product.toInventoryCountItem(snapshotsByProduct[product.id])
            items.add(item)
            if (item.isCounted) {
                countedItems++
            }
            item.difference?.let { difference ->
                when {
                    difference > 0 -> gainQuantity += difference
                    difference < 0 -> lossQuantity += difference
                }
            }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isSubmitting = false,
                error = null,
                items = items,
                countedItems = countedItems,
                gainQuantity = gainQuantity,
                lossQuantity = lossQuantity,
            )
        }
    }
}

private fun ProductV2Dto.toInventoryCountItem(snapshot: InventorySnapshotV2Dto?): InventoryCountItem =
    InventoryCountItem(
        productId = id,
        productName = name,
        productCode = code,
        categoryName = categoryName,
        systemQuantity = stock,
        countedQuantity = snapshot?.quantity,
        unitCost = snapshot?.unitCost ?: purchasePrice,
        totalValue = snapshot?.totalValue,
        snapshotCreatedAt = snapshot?.createdAt?.let(TimeFormatter::formatDateTime),
    )

private val inventoryProductComparator =
    compareBy<ProductV2Dto>({ it.categoryName }, { it.name })

private fun todayStartMillis(): Long =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
