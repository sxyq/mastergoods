package com.zhihuiji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DocumentStatusTone {
    PRIMARY,
    SUCCESS,
    WARNING,
    DANGER,
    NEUTRAL
}

private val DocumentListCardHeight = 116.dp
private val DocumentListCardPadding = 14.dp
private val DocumentListCardDividerTopPadding = 18.dp
private val DocumentListCardStatusEndPadding = 84.dp
private val DocumentStatusPillShape = RoundedCornerShape(100.dp)

@Composable
fun DocumentListCard(
    title: String,
    subtitle: String,
    meta: String,
    amount: String,
    modifier: Modifier = Modifier,
    statusLabel: String? = null,
    statusTone: DocumentStatusTone = DocumentStatusTone.NEUTRAL,
    amountColor: Color = TextPrimary,
    onClick: () -> Unit = {}
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(DocumentListCardHeight),
        onClick = onClick,
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(DocumentListCardPadding)
        ) {
            if (!statusLabel.isNullOrBlank()) {
                DocumentStatusPill(
                    label = statusLabel,
                    tone = statusTone,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = DocumentListCardStatusEndPadding),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = DocumentListCardDividerTopPadding)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GlassBorderSoft)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = meta,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = amount,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DocumentStatusPill(
    label: String,
    tone: DocumentStatusTone,
    modifier: Modifier = Modifier
) {
    val color = when (tone) {
        DocumentStatusTone.PRIMARY -> ZhihuijiPrimary
        DocumentStatusTone.SUCCESS -> SuccessGreen
        DocumentStatusTone.WARNING -> WarningOrange
        DocumentStatusTone.DANGER -> DangerRed
        DocumentStatusTone.NEUTRAL -> TextTertiary
    }
    Box(
        modifier = modifier
            .border(0.5.dp, color.copy(alpha = 0.20f), DocumentStatusPillShape)
            .background(color.copy(alpha = 0.10f), DocumentStatusPillShape)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
    }
}
