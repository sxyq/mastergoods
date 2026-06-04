package com.zhihuiji.feature.products

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
fun StockAdjustSheet(
    productName: String,
    currentStock: Double,
    onConfirm: (delta: Double, reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var deltaText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var adjustType by remember { mutableIntStateOf(0) }
    val tabs = listOf("入库", "出库", "盘盈", "盘亏")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("库存调整 - $productName", style = ZhihuijiTypography.headlineMedium)
            Text("当前库存: $currentStock", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
            SegmentedTabs(tabs = tabs, selectedIndex = adjustType, onTabSelected = { adjustType = it })
            OutlinedTextField(
                value = deltaText, onValueChange = { deltaText = it },
                label = { Text("调整数量") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            )
            OutlinedTextField(
                value = reason, onValueChange = { reason = it },
                label = { Text("调整原因") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
            )
            PrimaryButton(
                text = "确认调整",
                onClick = {
                    val qty = deltaText.toDoubleOrNull() ?: return@PrimaryButton
                    val signedDelta = when (adjustType) {
                        0 -> qty
                        1 -> -qty
                        2 -> qty
                        3 -> -qty
                        else -> qty
                    }
                    onConfirm(signedDelta, reason)
                },
                enabled = deltaText.toDoubleOrNull() != null && deltaText.toDouble() > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
