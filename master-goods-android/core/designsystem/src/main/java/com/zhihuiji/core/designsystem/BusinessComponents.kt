package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
        if (actionText != null) {
            Text(
                text = actionText,
                style = ZhihuijiTypography.labelMedium,
                color = ZhihuijiColors.Primary,
                modifier = Modifier.clickable(enabled = onActionClick != null) { onActionClick?.invoke() },
            )
        }
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun FieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ZhihuijiColors.TextPrimary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
        Text(value, style = ZhihuijiTypography.bodySmall, color = valueColor)
    }
}

@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SectionHeader(title = title, actionText = actionText, onActionClick = onActionClick)
            content()
        }
    }
}

@Composable
fun BusinessListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    amount: String? = null,
    amountColor: Color = ZhihuijiColors.TextPrimary,
    statusText: String? = null,
    statusTone: PillTone = PillTone.NEUTRAL,
    icon: ImageVector? = null,
    iconTint: Color = ZhihuijiColors.Primary,
    onClick: (() -> Unit)? = null,
) {
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconBadge(icon = icon, tint = iconTint, size = 40.dp, cornerRadius = 12.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                if (subtitle != null) {
                    Text(subtitle, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                }
                if (meta != null) {
                    Text(meta, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (statusText != null) {
                    StatusPill(text = statusText, tone = statusTone)
                }
                if (amount != null) {
                    Text(
                        amount,
                        style = ZhihuijiTypography.bodyMedium,
                        color = amountColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (onClick != null) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ZhihuijiColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
    if (onClick == null) {
        GlassCard(modifier = modifier.fillMaxWidth(), content = cardContent)
    } else {
        GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick, content = cardContent)
    }
}

@Composable
fun SummaryFooter(
    leftText: String,
    rightText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(leftText, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
        Text(rightText, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.Primary)
    }
}
