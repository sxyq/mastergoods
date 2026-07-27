package com.zhihuiji.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val SecondaryButtonShape = RoundedCornerShape(12.dp)

/**
 * 次要描边按钮
 *
 * 参考 BiliPai 的弹性动画设计。
 *
 * @param modifier 修饰符
 * @param text 按钮文字
 * @param onClick 点击回调
 * @param enabled 是否可用
 */
@Composable
fun SecondaryOutlineButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(scale)
            .clip(SecondaryButtonShape)
            .background(GlassSurfaceHigh)
            .border(
                border = BorderStroke(1.dp, DividerLight),
                shape = SecondaryButtonShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = ZhihuijiPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
