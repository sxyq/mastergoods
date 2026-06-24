package com.zhihuiji.feature.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun CustomerEditScreen(
    customerId: Long?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        if (customerId != null) {
            viewModel.loadCustomer(customerId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaveSuccess()
        }
    }

    CustomerEditScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSave = { name, phone, address, remark ->
            if (customerId != null) {
                viewModel.updateCustomer(customerId, name, phone, address, remark)
            } else {
                viewModel.createCustomer(name, phone, address, remark)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CustomerEditScreenContent(
    uiState: CustomerEditUiState,
    onNavigateBack: () -> Unit,
    onSave: (String, String, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(uiState.isEditMode, uiState.name) { mutableStateOf(uiState.name) }
    var phone by remember(uiState.isEditMode, uiState.phone) { mutableStateOf(uiState.phone) }
    var address by remember(uiState.isEditMode, uiState.address) { mutableStateOf(uiState.address) }
    var remark by remember(uiState.isEditMode, uiState.remark) { mutableStateOf(uiState.remark) }

    Column(modifier = modifier.fillMaxSize()) {
        GlassTopBar(
            title = if (uiState.isEditMode) "编辑客户" else "新增客户",
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZhihuijiPrimary)
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormSection(title = "基本资料") {
                        GlassTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "名称 *",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        GlassTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "手机号",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    FormSection(title = "联系资料") {
                        GlassTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = "地址",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    FormSection(title = "业务备注") {
                        GlassTextField(
                            value = remark,
                            onValueChange = { remark = it },
                            label = "备注",
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(96.dp))
                }
            }

            BottomActionBar(
                primaryText = "保存客户",
                onPrimaryClick = {
                    onSave(
                        name,
                        phone,
                        address.takeIf { it.isNotBlank() },
                        remark.takeIf { it.isNotBlank() }
                    )
                }
            )
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}
