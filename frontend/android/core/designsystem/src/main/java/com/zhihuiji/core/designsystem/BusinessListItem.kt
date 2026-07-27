package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 通用业务列表项
 *
 * @param modifier 修饰符
 * @param title 标题
 * @param subtitle 副标题
 * @param trailing 尾部内容
 * @param trailingColor 尾部内容颜色
 * @param statusPill 状态标签
 * @param onClick 点击回调
 * @param content 自定义内容
 */
@Composable
fun BusinessListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    trailingColor: Color = ZhihuijiPrimary,
    statusPill: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        blurRadius = 20.dp,
        surfaceColor = GlassSurfaceMedium,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trailing,
                        style = AmountSmallTextStyle,
                        color = trailingColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (statusPill != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    statusPill()
                }
            }
            if (content != null) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}
