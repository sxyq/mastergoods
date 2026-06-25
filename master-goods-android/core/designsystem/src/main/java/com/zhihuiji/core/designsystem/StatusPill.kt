package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 状态标签类型
 */
enum class StatusType {
    NORMAL,      // 正常 - 绿色
    LOW_STOCK,   // 低库存 - 橙色
    OUT_OF_STOCK,// 缺货 - 红色
    PENDING,     // 待收款 - 蓝色
    COMPLETED,   // 已完成 - 灰色
    ARCHIVED,    // 已归档 - 中性蓝灰
    CANCELLED    // 作废 - 灰色
}

private val statusPillColors: Map<StatusType, Pair<Color, Color>> = mapOf(
    StatusType.NORMAL to (SuccessGreen.copy(alpha = 0.12f) to SuccessGreen),
    StatusType.LOW_STOCK to (WarningOrange.copy(alpha = 0.12f) to WarningOrange),
    StatusType.OUT_OF_STOCK to (DangerRed.copy(alpha = 0.12f) to DangerRed),
    StatusType.PENDING to (ZhihuijiPrimary.copy(alpha = 0.12f) to ZhihuijiPrimary),
    StatusType.COMPLETED to (TextQuaternary.copy(alpha = 0.15f) to TextTertiary),
    StatusType.ARCHIVED to (ZhihuijiPrimary.copy(alpha = 0.08f) to TextSecondary),
    StatusType.CANCELLED to (TextQuaternary.copy(alpha = 0.15f) to TextQuaternary)
)

/**
 * 状态标签组件
 *
 * @param modifier 修饰符
 * @param text 标签文字
 * @param status 状态类型
 */
@Composable
fun StatusPill(
    modifier: Modifier = Modifier,
    text: String,
    status: StatusType = StatusType.NORMAL
) {
    val (backgroundColor, textColor) = statusPillColors.getValue(status)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
