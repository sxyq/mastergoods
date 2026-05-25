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
    onConfirm: (type: Int, category: String, amount: Double, method: Int?, notes: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember { mutableIntStateOf(1) }
    var category by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("新增流水", style = ZhihuijiTypography.headlineMedium)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("流水信息", style = ZhihuijiTypography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "收入", 2 to "支出").forEach { (code, label) ->
                            FilterChip(selected = selectedType == code, onClick = { selectedType = code }, label = { Text(label, style = ZhihuijiTypography.labelSmall) })
                        }
                    }
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("分类") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("金额") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("结算方式", style = ZhihuijiTypography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "现金", 2 to "微信", 3 to "支付宝", 4 to "银行卡").forEach { (code, label) ->
                            FilterChip(selected = selectedMethod == code, onClick = { selectedMethod = code }, label = { Text(label, style = ZhihuijiTypography.labelSmall) })
                        }
                    }
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
            PrimaryGradientButton(
                text = "确认新增",
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: return@PrimaryGradientButton
                    if (amt > 0 && category.isNotBlank()) {
                        onConfirm(selectedType, category, amt, selectedMethod, notes.ifBlank { null })
                    }
                },
                enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true && category.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
