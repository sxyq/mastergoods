package com.zhihuiji.feature.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun SaleOrderEditScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SaleOrderEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.onSaveSuccessHandled()
            onSaveSuccess()
        }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = if (uiState.isLoading) "加载中..." else "销售开单",
                subtitle = "客户、商品明细与应收金额",
                onNavigationClick = onBackClick
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                BottomActionBar(
                    primaryText = if (uiState.isSaving) "保存中..." else "保存销售单",
                    onPrimaryClick = viewModel::saveOrder,
                    primaryEnabled = !uiState.isSaving,
                    totalLabel = "应收合计",
                    totalAmount = MoneyFormatter.format(calculateSaleOrderTotal(uiState))
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZhihuijiPrimary)
                    }
                }

                else -> {
                    SaleOrderEditContent(
                        uiState = uiState,
                        onCustomerIdChange = viewModel::updateCustomerId,
                        onCustomerNameChange = viewModel::updateCustomerName,
                        onRemarkChange = viewModel::updateRemark,
                        onDiscountChange = viewModel::updateDiscountAmount,
                        onAddItem = viewModel::addItem,
                        onRemoveItem = viewModel::removeItem,
                        onItemProductChange = viewModel::updateItemProduct,
                        onItemQuantityChange = viewModel::updateItemQuantity,
                        onItemUnitPriceChange = viewModel::updateItemUnitPrice,
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleOrderEditContent(
    uiState: SaleOrderEditUiState,
    onCustomerIdChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onItemProductChange: (Int, String, String) -> Unit,
    onItemQuantityChange: (Int, String) -> Unit,
    onItemUnitPriceChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassTextField(
                    value = uiState.customerId,
                    onValueChange = onCustomerIdChange,
                    label = "客户ID",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                GlassTextField(
                    value = uiState.customerName,
                    onValueChange = onCustomerNameChange,
                    label = "客户名称",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                GlassTextField(
                    value = uiState.discountAmount,
                    onValueChange = onDiscountChange,
                    label = "折扣金额",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                GlassTextField(
                    value = uiState.remark,
                    onValueChange = onRemarkChange,
                    label = "备注",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "商品明细",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = "添加商品")
            }
        }

        uiState.items.forEachIndexed { index, item ->
            EditItemCard(
                index = index,
                item = item,
                onRemove = { onRemoveItem(index) },
                onProductIdChange = { productId ->
                    onItemProductChange(index, productId, item.productName)
                },
                onProductNameChange = { productName ->
                    onItemProductChange(index, item.productId?.toString() ?: "", productName)
                },
                onQuantityChange = { onItemQuantityChange(index, it) },
                onUnitPriceChange = { onItemUnitPriceChange(index, it) },
            )
        }

        if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "点击 + 添加商品",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        val totalAmount = calculateSaleOrderTotal(uiState)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "订单总金额: ",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = MoneyFormatter.format(totalAmount),
                style = MaterialTheme.typography.titleLarge,
                color = ZhihuijiPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(112.dp))
    }
}

private fun calculateSaleOrderTotal(uiState: SaleOrderEditUiState): Double {
    return uiState.items.sumOf {
        val qty = it.quantity.toDoubleOrNull() ?: 0.0
        val price = it.unitPrice.toDoubleOrNull() ?: 0.0
        qty * price
    } - (uiState.discountAmount.toDoubleOrNull() ?: 0.0)
}

@Composable
private fun EditItemCard(
    index: Int,
    item: EditItem,
    onRemove: () -> Unit,
    onProductIdChange: (String) -> Unit,
    onProductNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "商品 ${index + 1}",
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            GlassTextField(
                value = item.productId?.toString() ?: "",
                onValueChange = onProductIdChange,
                label = "商品ID",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            GlassTextField(
                value = item.productName,
                onValueChange = onProductNameChange,
                label = "商品名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassTextField(
                    value = item.quantity,
                    onValueChange = onQuantityChange,
                    label = "数量",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                GlassTextField(
                    value = item.unitPrice,
                    onValueChange = onUnitPriceChange,
                    label = "单价",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }
    }
}
