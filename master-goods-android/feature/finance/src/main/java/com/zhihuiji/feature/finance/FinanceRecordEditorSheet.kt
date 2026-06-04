package com.zhihuiji.feature.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceRecordEditorSheet(
    onConfirm: (code: String, name: String, type: Int, balance: Double?, notes: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableIntStateOf(1) }
    var balanceText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("新增账户", style = ZhihuijiTypography.headlineMedium)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("账户信息", style = ZhihuijiTypography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "现金", 2 to "银行", 3 to "支付宝", 4 to "微信").forEach { (code, label) ->
                            FilterChip(selected = selectedType == code, onClick = { selectedType = code }, label = { Text(label, style = ZhihuijiTypography.labelSmall) })
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("账户编码") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("账户名称") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("初始余额(选填)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("备注(选填)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        maxLines = 2,
                    )
                }
            }
            PrimaryButton(
                text = "确认新增",
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onConfirm(code, name, selectedType, balanceText.toDoubleOrNull(), notes.ifBlank { null })
                    }
                },
                enabled = code.isNotBlank() && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
