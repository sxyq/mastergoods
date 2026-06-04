package com.zhihuiji.feature.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.designsystem.*

@Composable
fun ProductEditorScreen(
    productId: Long?,
    onNavigateBack: () -> Unit,
    onStockAdjust: () -> Unit = {},
    viewModel: ProductEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) {
        if (productId != null && productId > 0) viewModel.loadProduct(productId)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = if (uiState.existingId != null) "编辑商品" else "新增商品",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("基本信息", style = ZhihuijiTypography.titleMedium)
                        OutlinedTextField(
                            value = uiState.draft.code, onValueChange = { v -> viewModel.updateDraft { it.copy(code = v) } },
                            label = { Text("编码") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.draft.name, onValueChange = { viewModel.updateDraft { d -> d.copy(name = it) } },
                            label = { Text("名称") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.draft.categoryName, onValueChange = { viewModel.updateDraft { d -> d.copy(categoryName = it) } },
                            label = { Text("分类") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.draft.unitName, onValueChange = { viewModel.updateDraft { d -> d.copy(unitName = it) } },
                            label = { Text("单位") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        )
                    }
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("价格设置", style = ZhihuijiTypography.titleMedium)
                        OutlinedTextField(
                            value = if (uiState.draft.salePrice == 0.0) "" else uiState.draft.salePrice.toString(),
                            onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDraft { d -> d.copy(salePrice = it) } } },
                            label = { Text("售价") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                        )
                        OutlinedTextField(
                            value = if (uiState.draft.purchasePrice == 0.0) "" else uiState.draft.purchasePrice.toString(),
                            onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDraft { d -> d.copy(purchasePrice = it) } } },
                            label = { Text("进价") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                        )
                    }
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("库存设置", style = ZhihuijiTypography.titleMedium)
                        OutlinedTextField(
                            value = if (uiState.draft.safeStock == 0.0) "" else uiState.draft.safeStock.toString(),
                            onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDraft { d -> d.copy(safeStock = it) } } },
                            label = { Text("安全库存") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                        )
                        if (uiState.existingId != null) {
                            PrimaryButton(text = "库存调整", onClick = onStockAdjust, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                if (uiState.error != null) {
                    Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                }
            }
            BottomActionBar(primaryAction = {
                PrimaryButton(
                    text = if (uiState.isSaving) "保存中..." else "保存",
                    onClick = { viewModel.saveProduct() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }, secondaryActions = listOf {
                SecondaryOutlineButton(text = "取消", onClick = onNavigateBack)
            })
        }
    }
}
