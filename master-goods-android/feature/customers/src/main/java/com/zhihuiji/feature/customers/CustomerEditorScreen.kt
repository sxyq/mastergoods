package com.zhihuiji.feature.customers

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
fun CustomerEditorScreen(
    customerId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: CustomerEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        if (customerId != null && customerId > 0) viewModel.loadCustomer(customerId)
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
                title = if (uiState.existingId != null) "编辑客户" else "新增客户",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = uiState.draft.name, onValueChange = { viewModel.updateDraft { d -> d.copy(name = it) } },
                            label = { Text("名称") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.draft.phone, onValueChange = { viewModel.updateDraft { d -> d.copy(phone = it) } },
                            label = { Text("手机号") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.draft.address ?: "", onValueChange = { viewModel.updateDraft { d -> d.copy(address = it.ifBlank { null }) } },
                            label = { Text("地址") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.draft.notes ?: "", onValueChange = { viewModel.updateDraft { d -> d.copy(notes = it.ifBlank { null }) } },
                            label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 3,
                        )
                    }
                }
                if (uiState.error != null) {
                    Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                }
            }
            BottomActionBar(primaryAction = {
                PrimaryButton(
                    text = if (uiState.isSaving) "保存中..." else "保存",
                    onClick = { viewModel.saveCustomer() }, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth(),
                )
            }, secondaryActions = listOf {
                SecondaryOutlineButton(text = "取消", onClick = onNavigateBack)
            })
        }
    }
}
