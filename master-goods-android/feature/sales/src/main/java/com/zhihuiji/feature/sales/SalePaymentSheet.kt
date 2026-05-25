package com.zhihuiji.feature.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalePaymentSheet(
    maxAmount: Double,
    onConfirm: (amount: Double, method: Int, referenceNo: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf(maxAmount.toString()) }
    var selectedMethod by remember { mutableIntStateOf(1) }
    var referenceNo by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("收款", style = ZhihuijiTypography.headlineMedium)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("单据信息", style = ZhihuijiTypography.titleMedium)
                        Text("待收金额", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                    }
                    Text(MoneyFormatter.format(maxAmount), style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.Danger)
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("本次收款", style = ZhihuijiTypography.titleMedium)
                    Text(MoneyFormatter.format(maxAmount), style = ZhihuijiTypography.displayLarge, color = ZhihuijiColors.TextPrimary)
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                        label = { Text("收款金额") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            )
            Text("收款方式", style = ZhihuijiTypography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "现金", 2 to "微信", 3 to "支付宝", 4 to "银行卡").forEach { (code, label) ->
                    FilterChip(selected = selectedMethod == code, onClick = { selectedMethod = code }, label = { Text(label, style = ZhihuijiTypography.labelSmall) })
                }
            }
            OutlinedTextField(
                value = referenceNo, onValueChange = { referenceNo = it },
                        label = { Text("参考号(选填)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), singleLine = true,
            )
                }
            }
            PrimaryGradientButton(
                text = "确认收款",
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: return@PrimaryGradientButton
                    if (amt > 0) onConfirm(amt, selectedMethod, referenceNo.ifBlank { null })
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
