package com.zhihuiji.feature.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BackgroundGradientEnd
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryLight

@Composable
fun PurchaseReceiptScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseReceiptViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val receipt = uiState.selectedReceipt

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "采购入库",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = TextPrimary,
                        )
                    }
                },
            )
        },
        bottomBar = {
            receipt?.let {
                PurchaseReceiptBottomBar(
                    primaryText = if (uiState.isSubmitting) "确认中..." else if (it.canConfirm) "确认入库" else it.statusText,
                    primaryEnabled = it.canConfirm && !uiState.isSubmitting,
                    onPrimaryClick = viewModel::confirmSelectedReceipt,
                )
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.receipts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            }

            uiState.error != null && uiState.receipts.isEmpty() -> {
                PurchaseReceiptUnavailableContent(
                    title = "采购入库单加载失败",
                    message = uiState.error ?: "请稍后重试",
                    modifier = Modifier.padding(paddingValues),
                )
            }

            receipt == null -> {
                PurchaseReceiptUnavailableContent(
                    title = "暂无采购入库单",
                    message = "当前账号还没有后端返回的采购入库记录。",
                    modifier = Modifier.padding(paddingValues),
                )
            }

            else -> {
                PurchaseReceiptContent(
                    uiState = uiState,
                    receipt = receipt,
                    onSelectReceipt = viewModel::selectReceipt,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun PurchaseReceiptContent(
    uiState: PurchaseReceiptUiState,
    receipt: PurchaseReceiptItem,
    onSelectReceipt: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val otherReceipts = remember(uiState.receipts, receipt.id) {
        uiState.receipts.filterNot { it.id == receipt.id }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PurchaseReceiptHeaderCard(receipt = receipt)
        }

        item {
            WarehouseCard()
        }

        if (uiState.statusMessage != null || uiState.error != null) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceMedium,
                    contentPadding = 14.dp,
                ) {
                    Text(
                        text = uiState.statusMessage ?: uiState.error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.error == null) SuccessGreen else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = "入库明细 (${receipt.items.size})",
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (receipt.items.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 16.dp,
                ) {
                    Text(
                        text = "后端未返回入库商品明细",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "不会用设计稿样例商品填充，请检查真实采购入库单数据。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        items(
            items = receipt.items,
            key = { it.id },
        ) { item ->
            PurchaseReceiptLineCard(item = item)
        }

        item {
            RemarkCard(notes = receipt.notes)
        }

        if (uiState.receipts.size > 1) {
            item {
                Text(
                    text = "其他入库单",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(
                items = otherReceipts,
                key = { it.id },
            ) { item ->
                PurchaseReceiptCompactCard(
                    receipt = item,
                    onClick = { onSelectReceipt(item.id) },
                )
            }
        }
    }
}

@Composable
private fun PurchaseReceiptHeaderCard(
    receipt: PurchaseReceiptItem,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 96.dp))
                    .background(ZhihuijiPrimaryLight.copy(alpha = 0.38f)),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (receipt.purchaseOrderId == null) "入库单号" else "采购单号",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = receipt.purchaseOrderLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (receipt.purchaseOrderId != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "入库单 ${receipt.receiptNo}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    StatusPill(
                        text = receipt.statusText,
                        status = receipt.status.toStatusType(),
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = Color.White.copy(alpha = 0.50f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    HeaderInfo(
                        modifier = Modifier.weight(1f),
                        icon = "⌂",
                        label = "供应商",
                        value = receipt.supplierName,
                    )
                    HeaderInfo(
                        modifier = Modifier.weight(1f),
                        icon = "◷",
                        label = "预计日期",
                        value = receipt.createdAt,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderInfo(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "$icon $label",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WarehouseCard(
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(ZhihuijiPrimaryLight.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "仓",
                        style = MaterialTheme.typography.titleMedium,
                        color = ZhihuijiPrimary,
                    )
                }
                Column {
                    Text(
                        text = "入库仓库",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "主仓库",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                text = "更改 ›",
                style = MaterialTheme.typography.bodyMedium,
                color = ZhihuijiPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "当前后端合同未返回仓库字段，确认后按默认仓库更新库存。",
            style = MaterialTheme.typography.labelLarge,
            color = TextTertiary,
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "▣",
            style = MaterialTheme.typography.titleMedium,
            color = ZhihuijiPrimary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PurchaseReceiptLineCard(
    item: PurchaseReceiptLineItem,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE0E2E7))
                    .border(1.dp, Color.White.copy(alpha = 0.62f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "img",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${item.productCode}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextTertiary,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "规格: 后端未返回",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = "应收",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                        )
                        Text(
                            text = item.quantity.formatQuantity(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "实收",
                            style = MaterialTheme.typography.labelLarge,
                            color = ZhihuijiPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        ReadOnlyReceivedStepper(value = item.quantity.formatQuantity())
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyReceivedStepper(
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceWhite.copy(alpha = 0.78f))
            .border(0.5.dp, Color(0xFFC1C6D6).copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperControlText(text = "−")
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
            )
        }
        StepperControlText(text = "+")
    }
}

@Composable
private fun StepperControlText(text: String) {
    Box(
        modifier = Modifier.size(width = 32.dp, height = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun RemarkCard(
    notes: String?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Text(
            text = "入库备注 (选填)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceWhite.copy(alpha = 0.74f))
                .border(0.5.dp, Color(0xFFC1C6D6).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Text(
                text = notes?.takeIf { it.isNotBlank() } ?: "后端未返回备注",
                style = MaterialTheme.typography.bodyMedium,
                color = if (notes.isNullOrBlank()) TextTertiary else TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PurchaseReceiptCompactCard(
    receipt: PurchaseReceiptItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        surfaceColor = GlassSurfaceMedium,
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receipt.purchaseOrderLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${receipt.supplierName} · ${receipt.createdAt}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            StatusPill(
                text = receipt.statusText,
                status = receipt.status.toStatusType(),
            )
        }
    }
}

@Composable
private fun PurchaseReceiptBottomBar(
    primaryText: String,
    primaryEnabled: Boolean,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundGradientEnd.copy(alpha = 0.02f),
                        BackgroundGradientEnd.copy(alpha = 0.92f),
                        BackgroundGradientEnd,
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlineActionButton(
                text = "部分收货",
                enabled = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            FilledActionButton(
                text = primaryText,
                enabled = primaryEnabled,
                onClick = onPrimaryClick,
                modifier = Modifier.weight(2f),
            )
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun OutlineActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite.copy(alpha = if (enabled) 0.92f else 0.62f))
            .border(0.5.dp, Color(0xFFC1C6D6).copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary.copy(alpha = if (enabled) 1f else 0.45f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FilledActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(48.dp)
            .scale(if (isPressed && enabled) 0.97f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(ZhihuijiPrimary, Color(0xFF005BC0)),
                ),
            )
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = SurfaceWhite,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PurchaseReceiptUnavailableContent(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 16.dp,
            ) {
                Text(
                    text = "采购单号",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "暂无真实采购入库单",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.50f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        item {
            WarehouseCard()
        }
        item {
            SectionTitle(title = "入库明细 (0)")
        }
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 16.dp,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "不会用设计稿样例商品替代真实后端数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        item {
            RemarkCard(notes = null)
        }
    }
}

private val PurchaseReceiptItem.purchaseOrderLabel: String
    get() = purchaseOrderId?.let { "采购单 #$it" } ?: receiptNo

private fun Int.toStatusType(): StatusType =
    when (this) {
        0 -> StatusType.PENDING
        1 -> StatusType.NORMAL
        2 -> StatusType.CANCELLED
        else -> StatusType.COMPLETED
    }

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) "%.0f".format(this) else "%.2f".format(this)
