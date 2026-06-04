package com.zhihuiji.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ZhihuijiColors.Primary,
            disabledContainerColor = ZhihuijiColors.Primary.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ZhihuijiTypography.labelLarge,
            color = Color.White,
        )
    }
}

@Deprecated("Use PrimaryButton instead", ReplaceWith("PrimaryButton(text, onClick, modifier, icon, enabled)"))
@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    PrimaryButton(text, onClick, modifier, icon, enabled)
}

@Composable
fun SecondaryOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, if (enabled) ZhihuijiColors.BorderLight else ZhihuijiColors.BorderLight.copy(alpha = 0.45f)),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = ZhihuijiColors.TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ZhihuijiTypography.labelLarge,
            color = ZhihuijiColors.TextSecondary,
        )
    }
}

@Composable
fun DangerOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, if (enabled) ZhihuijiColors.Danger else ZhihuijiColors.Danger.copy(alpha = 0.45f)),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = if (enabled) ZhihuijiColors.Danger else ZhihuijiColors.Danger.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ZhihuijiTypography.labelLarge,
            color = if (enabled) ZhihuijiColors.Danger else ZhihuijiColors.Danger.copy(alpha = 0.45f),
        )
    }
}
