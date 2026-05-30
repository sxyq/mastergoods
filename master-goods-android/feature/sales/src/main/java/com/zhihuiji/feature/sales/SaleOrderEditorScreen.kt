package com.zhihuiji.feature.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleOrderEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: SaleOrderEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProductPicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    var customerSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    Column(modifier = Modifier.fillMaxSize().glassBackground()) {
        GlassTopBar(title = "销售开单", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("客户", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextSecondary)
                        Text(uiState.customerName ?: "请选择客户", style = ZhihuijiTypography.bodyMedium, color = if (uiState.customerName != null) ZhihuijiColors.TextPrimary else ZhihuijiColors.TextTertiary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("结算方式：月结30天", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                        SecondaryOutlineButton(text = "选择", onClick = { showCustomerPicker = true }, modifier = Modifier.width(88.dp))
                    }
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("商品明细", style = ZhihuijiTypography.titleMedium)
                        SecondaryOutlineButton(text = "扫码添加", onClick = { showProductPicker = true }, modifier = Modifier.width(104.dp))
                    }
                    if (uiState.lines.isEmpty()) {
                        Text("暂无商品，点击添加商品选择", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextTertiary)
                    }
                    uiState.lines.forEach { line ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(line.productCode.ifBlank { "商品" }, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                    Text(line.productName, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                                }
                                Text("¥${MoneyFormatter.formatWithoutSymbol(line.unitPrice)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextPrimary)
                                QuantityStepper(
                                    value = line.quantity,
                                    onValueChange = { viewModel.changeQuantity(line.lineId, it.coerceAtLeast(1.0)) },
                                    minusIcon = Icons.Default.Remove,
                                    plusIcon = Icons.Default.Add,
                                    modifier = Modifier.width(112.dp),
                                    min = 1.0,
                                )
                                Text(MoneyFormatter.formatWithoutSymbol(line.amount), style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextPrimary, modifier = Modifier.width(52.dp))
                                IconButton(onClick = { viewModel.removeItem(line.lineId) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = ZhihuijiColors.TextTertiary, modifier = Modifier.size(16.dp)) }
                            }
                            HorizontalDivider(color = ZhihuijiColors.BorderLight, thickness = 0.5.dp)
                        }
                    }
                    SecondaryOutlineButton(text = "添加商品", onClick = { showProductPicker = true }, modifier = Modifier.fillMaxWidth())
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("商品合计", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        Text(MoneyFormatter.formatWithoutSymbol(uiState.lines.sumOf { it.amount }), style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("整单折扣", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        OutlinedTextField(
                            value = if (uiState.discountAmount == 0.0) "" else uiState.discountAmount.toString(),
                            onValueChange = { viewModel.updateDiscount(it.toDoubleOrNull() ?: 0.0) },
                            placeholder = { Text("优惠金额") },
                            modifier = Modifier.width(132.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = ZhihuijiTypography.bodySmall,
                        )
                    }
                    OutlinedTextField(value = uiState.notes, onValueChange = { viewModel.updateNotes(it) }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), maxLines = 2)
                    HorizontalDivider(color = ZhihuijiColors.BorderLight, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("合计", style = ZhihuijiTypography.titleMedium)
                        Text(MoneyFormatter.format(uiState.totalAmount), style = ZhihuijiTypography.displayMedium, color = ZhihuijiColors.Primary)
                    }
                }
            }
            if (uiState.error != null) {
                Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
            }
        }
        BottomActionBar(primaryAction = {
            PrimaryGradientButton(text = if (uiState.isSaving) "提交中..." else "提交订单", onClick = { viewModel.submitOrder() }, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth())
        }, secondaryActions = listOf {
            SecondaryOutlineButton(text = "保存草稿", onClick = onNavigateBack)
        })
    }

    if (showProductPicker) {
        ModalBottomSheet(onDismissRequest = { showProductPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("选择商品", style = ZhihuijiTypography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = productSearchQuery, onValueChange = { productSearchQuery = it; viewModel.searchProducts(it) }, label = { Text("搜索商品") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.productSearchResults) { product ->
                        GlassCard(onClick = { viewModel.addItem(product); showProductPicker = false }) {
                            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(product.name, style = ZhihuijiTypography.bodyMedium); Text(product.code, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary) }
                                Text(MoneyFormatter.format(product.salePrice), style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.Primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomerPicker) {
        ModalBottomSheet(onDismissRequest = { showCustomerPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("选择客户", style = ZhihuijiTypography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = customerSearchQuery, onValueChange = { customerSearchQuery = it; viewModel.searchCustomers(it) }, label = { Text("搜索客户") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.customerSearchResults) { customer ->
                        GlassCard(onClick = { viewModel.selectCustomer(customer.id ?: 0, customer.name); showCustomerPicker = false }) {
                            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(customer.name, style = ZhihuijiTypography.bodyMedium)
                                Text(customer.phone, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
