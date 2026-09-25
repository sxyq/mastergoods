package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 底部固定操作区
 *
 * @param modifier 修饰符
 * @param primaryText 主按钮文字
 * @param onPrimaryClick 主按钮点击回调
 * @param secondaryText 次按钮文字
 * @param onSecondaryClick 次按钮点击回调
 * @param totalAmount 合计金额（可选）
 * @param totalLabel 金额标签
 * @param dangerMode 是否使用危险模式（主按钮变红）
 */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean = true,
    secondaryText: String? = null,
    onSecondaryClick: () -> Unit = {},
    secondaryEnabled: Boolean = true,
    totalAmount: String? = null,
    totalLabel: String = "合计",
    totalAmountColor: Color? = null,
    dangerMode: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = bottomActionBarBackgroundBrush)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            blurRadius = 24.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            surfaceColor = GlassSurfaceHigh,
            contentPadding = 16.dp
        ) {
            if (totalAmount != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = totalLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = totalAmount,
                        style = AmountTextStyle,
                        color = totalAmountColor ?: if (dangerMode) DangerRed else ZhihuijiPrimary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (secondaryText != null) {
                    if (dangerMode) {
                        DangerOutlineButton(
                            modifier = Modifier.weight(1f),
                            text = secondaryText,
                            onClick = onSecondaryClick,
                            enabled = secondaryEnabled
                        )
                    } else {
                        SecondaryOutlineButton(
                            modifier = Modifier.weight(1f),
                            text = secondaryText,
                            onClick = onSecondaryClick,
                            enabled = secondaryEnabled
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (dangerMode) {
                    DangerOutlineButton(
                        modifier = Modifier.weight(1f),
                        text = primaryText,
                        onClick = onPrimaryClick,
                        enabled = primaryEnabled
                    )
                } else {
                    PrimaryButton(
                        modifier = Modifier.weight(1f),
                        text = primaryText,
                        onClick = onPrimaryClick,
                        enabled = primaryEnabled
                    )
                }
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

private val bottomActionBarBackgroundBrush by lazy {
    Brush.verticalGradient(
        colors = listOf(
            BackgroundGradientEnd.copy(alpha = 0.82f),
            BackgroundGradientEnd.copy(alpha = 0.96f),
            BackgroundGradientEnd
        )
    )
}
