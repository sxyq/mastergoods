package com.zhihuiji.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Unified glass-styled input used by form-heavy business screens.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZhihuijiPrimary,
            unfocusedBorderColor = DividerLight,
            disabledBorderColor = DividerLight.copy(alpha = 0.5f),
            focusedContainerColor = GlassSurfaceHigh,
            unfocusedContainerColor = GlassSurfaceHigh,
            disabledContainerColor = GlassSurfaceLow,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextSecondary,
            focusedLabelColor = ZhihuijiPrimary,
            unfocusedLabelColor = TextSecondary,
            focusedPlaceholderColor = TextTertiary,
            unfocusedPlaceholderColor = TextTertiary,
        )
    )
}
