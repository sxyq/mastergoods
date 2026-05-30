package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun QuantityStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    minusIcon: ImageVector,
    plusIcon: ImageVector,
    modifier: Modifier = Modifier,
    min: Double = 0.0,
    max: Double = Double.MAX_VALUE,
    step: Double = 1.0,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            onClick = { onValueChange((value - step).coerceIn(min, max)) },
            enabled = value > min,
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(7.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ZhihuijiColors.SurfaceVariant,
                contentColor = ZhihuijiColors.TextSecondary,
            ),
        ) {
            Icon(imageVector = minusIcon, contentDescription = "减少", modifier = Modifier.size(16.dp))
        }
        OutlinedTextField(
            value = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString(),
            onValueChange = { v -> v.toDoubleOrNull()?.let { onValueChange(it.coerceIn(min, max)) } },
            modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
            textStyle = ZhihuijiTypography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = ZhihuijiColors.BorderLight),
            singleLine = true,
        )
        FilledIconButton(
            onClick = { onValueChange((value + step).coerceIn(min, max)) },
            enabled = value < max,
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(7.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ZhihuijiColors.Primary,
                contentColor = ZhihuijiColors.White,
            ),
        ) {
            Icon(imageVector = plusIcon, contentDescription = "增加", modifier = Modifier.size(16.dp))
        }
    }
}
