package com.zhihuiji.feature.purchases

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
import androidx.compose.material.icons.filled.CheckCircle
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
fun PurchaseOrderEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: PurchaseOrderEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProductPicker by remember { mutableStateOf(false) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    var supplierSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            GlassTopBar(title = "采购开单", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("供应商", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextSecondary)
                            Text(uiState.supplierName ?: "未选择", style = ZhihuijiTypography.bodyMedium, color = if (uiState.supplierName != null) ZhihuijiColors.TextPrimary else ZhihuijiColors.TextTertiary)
                        }
                        SecondaryOutlineButton(text = "选择供应商", onClick = { showSupplierPicker = true }, modifier = Modifier.fillMaxWidth())
                    }
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("商品明细", style = ZhihuijiTypography.titleMedium)
                            SecondaryOutlineButton(
                                text = "添加商品",
                                onClick = { showProductPicker = true },
                                modifier = Modifier.width(104.dp),
                                icon = Icons.Default.Add,
                            )
                        }
                        if (uiState.lines.isEmpty()) {
                            Text("暂无商品，点击添加商品选择", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextTertiary)
                        }
                        uiState.lines.forEach { line ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(line.productCode.ifBlank { "商品" }, style = ZhihuijiTypography.titleSmall)
                                    Text(line.productName, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                                }
                                Text("¥${MoneyFormatter.formatWithoutSymbol(line.unitCost)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextPrimary)
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
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.notes, onValueChange = { viewModel.updateNotes(it) }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), maxLines = 2)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("应付合计", style = ZhihuijiTypography.titleMedium)
                            Text(MoneyFormatter.format(uiState.totalAmount), style = ZhihuijiTypography.displayMedium, color = ZhihuijiColors.Primary)
                        }
                    }
                }
                if (uiState.error != null) {
                    Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                }
            }
            BottomActionBar(primaryAction = {
                PrimaryButton(
                    text = if (uiState.isSaving) "提交中..." else "提交采购单",
                    onClick = { viewModel.submitOrder() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CheckCircle,
                )
            }, secondaryActions = listOf {
                SecondaryOutlineButton(
                    text = "草稿功能待实现",
                    onClick = {},
                    enabled = false,
                )
            })
        }
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
                                Text(MoneyFormatter.format(product.purchasePrice), style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.Primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSupplierPicker) {
        ModalBottomSheet(onDismissRequest = { showSupplierPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("选择供应商", style = ZhihuijiTypography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = supplierSearchQuery, onValueChange = { supplierSearchQuery = it; viewModel.searchSuppliers(it) }, label = { Text("搜索供应商") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.supplierSearchResults) { supplier ->
                        GlassCard(onClick = { viewModel.selectSupplier(supplier.id, supplier.name); showSupplierPicker = false }) {
                            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(supplier.name, style = ZhihuijiTypography.bodyMedium)
                                Text(supplier.phone, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
