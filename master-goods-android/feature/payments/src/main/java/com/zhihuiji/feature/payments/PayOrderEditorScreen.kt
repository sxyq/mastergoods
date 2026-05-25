package com.zhihuiji.feature.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayOrderEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: PayOrderEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSupplierPicker by remember { mutableStateOf(false) }
    var supplierSearchQuery by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    Column(modifier = Modifier.fillMaxSize().glassBackground()) {
        GlassTopBar(title = "新建付款单", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
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
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("本次付款", style = ZhihuijiTypography.titleMedium)
                    Text(MoneyFormatter.format(uiState.amount), style = ZhihuijiTypography.displayLarge, color = ZhihuijiColors.TextPrimary)
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it; it.toDoubleOrNull()?.let { v -> viewModel.updateAmount(v) } },
                        label = { Text("付款金额") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    )
                    Text("付款方式", style = ZhihuijiTypography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "现金", 2 to "微信", 3 to "支付宝", 4 to "银行卡").forEach { (code, label) ->
                            FilterChip(selected = uiState.method == code, onClick = { viewModel.updateMethod(code) }, label = { Text(label, style = ZhihuijiTypography.labelSmall) })
                        }
                    }
                    OutlinedTextField(
                        value = uiState.referenceNo, onValueChange = { viewModel.updateReferenceNo(it) },
                        label = { Text("参考号(选填)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), singleLine = true,
                    )
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    OutlinedTextField(value = uiState.notes, onValueChange = { viewModel.updateNotes(it) }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), maxLines = 2)
                }
            }
            if (uiState.error != null) {
                Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
            }
        }
        BottomActionBar(primaryAction = {
            PrimaryGradientButton(text = if (uiState.isSaving) "提交中..." else "创建付款单", onClick = { viewModel.submitOrder() }, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth())
        }, secondaryActions = listOf {
            SecondaryOutlineButton(text = "取消", onClick = onNavigateBack)
        })
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
                        GlassCard(onClick = { viewModel.selectSupplier(supplier.id ?: 0, supplier.name); showSupplierPicker = false }) {
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
