package com.zhihuiji.feature.suppliers

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
fun SupplierEditScreen(
    supplierId: Long?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupplierEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(supplierId) {
        if (supplierId != null) {
            viewModel.loadSupplier(supplierId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaveSuccess()
        }
    }

    SupplierEditScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSave = { name, primaryContactName, phone, address, remark ->
            if (supplierId != null) {
                viewModel.updateSupplier(supplierId, name, primaryContactName, phone, address, remark)
            } else {
                viewModel.createSupplier(name, primaryContactName, phone, address, remark)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SupplierEditScreenContent(
    uiState: SupplierEditUiState,
    onNavigateBack: () -> Unit,
    onSave: (String, String?, String, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(uiState.name) }
    var primaryContactName by remember { mutableStateOf(uiState.primaryContactName) }
    var phone by remember { mutableStateOf(uiState.phone) }
    var address by remember { mutableStateOf(uiState.address) }
    var remark by remember { mutableStateOf(uiState.remark) }

    Column(modifier = modifier.fillMaxSize()) {
        GlassTopBar(
            title = if (uiState.isEditMode) "编辑供应商" else "新增供应商",
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
                            value = primaryContactName,
                            onValueChange = { primaryContactName = it },
                            label = "联系人姓名",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    FormSection(title = "联系与结算") {
                        GlassTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "手机号",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        GlassTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = "地址",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    FormSection(title = "备注信息") {
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
                primaryText = "保存供应商",
                onPrimaryClick = {
                    onSave(
                        name,
                        primaryContactName.takeIf { it.isNotBlank() },
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
