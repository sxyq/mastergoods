package com.zhihuiji.feature.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
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
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright
import java.util.Locale
import kotlin.math.abs

@Composable
fun InventoryLedgerScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryLedgerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        viewModel.loadLedger(productId)
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "库存流水",
                onNavigationClick = onNavigateBack,
                actions = {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "筛选库存流水",
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            }

            uiState.error != null -> {
                LedgerEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "库存流水加载失败",
                    message = uiState.error ?: "请稍后重试",
                )
            }

            uiState.entries.isEmpty() -> {
                LedgerEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "暂无库存流水",
                    message = "当前商品还没有真实库存变动记录，未展示任何本地生成流水。",
                )
            }

            else -> {
                InventoryLedgerContent(
                    productId = productId,
                    uiState = uiState,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun InventoryLedgerContent(
    productId: Long,
    uiState: InventoryLedgerUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            InventoryProductSummaryCard(
                productId = productId,
                uiState = uiState,
            )
        }

        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                blurRadius = 24.dp,
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 18.dp,
            ) {
                Text(
                    text = "库存变动记录",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "按真实业务发生时间倒序排列",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        itemsIndexed(
            items = uiState.entries,
            key = { _, item -> item.id },
        ) { index, item ->
            InventoryTimelineItemCard(
                item = item,
                isLast = index == uiState.entries.lastIndex,
            )
        }
    }
}

@Composable
private fun InventoryProductSummaryCard(
    productId: Long,
    uiState: InventoryLedgerUiState,
    modifier: Modifier = Modifier,
) {
    val stockStatus = uiState.latestBalance.inventoryStatus()

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 26.dp,
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 18.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.productName.ifBlank { "商品 #$productId" },
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.productCode.ifBlank { "编号未返回" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(
                text = stockStatus.label,
                status = stockStatus.statusType,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "当前结存",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = uiState.latestBalance?.formatQuantity() ?: "未记录",
                style = MaterialTheme.typography.displayMedium,
                color = ZhihuijiPrimary,
                fontWeight = FontWeight.Bold,
            )
            if (uiState.latestBalance != null) {
                Text(
                    text = "库存单位以商品档案为准",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun InventoryTimelineItemCard(
    item: InventoryLedgerItem,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val isIncrease = item.quantityChange >= 0
    val tone = remember(item.quantityChange) {
        item.timelineTone()
    }
    val auxiliary = remember(item.quantityBefore, item.unitCost, item.notes) {
        item.auxiliaryText()
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 22.dp,
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            TimelineMarker(
                icon = tone.icon,
                color = tone.color,
                isLast = isLast,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.sourceType.sourceLabel(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.createdAt,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (isIncrease) "+" else ""}${item.quantityChange.formatQuantity()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isIncrease) SuccessGreen else DangerRed,
                        )
                        Text(
                            text = "结存: ${item.quantityAfter?.formatQuantity() ?: "-"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.sourceNo?.takeIf { it.isNotBlank() }?.let { "单号: $it" } ?: "单号未返回",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (auxiliary != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = auxiliary,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineMarker(
    icon: ImageVector,
    color: Color,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(16.dp),
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(2.dp)
                    .height(52.dp)
                    .background(ZhihuijiPrimaryBright.copy(alpha = 0.18f), RoundedCornerShape(100.dp)),
            )
        }
    }
}

@Composable
private fun LedgerEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            blurRadius = 24.dp,
            surfaceColor = GlassSurfaceHigh,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private data class InventoryStatus(
    val label: String,
    val statusType: StatusType,
)

private data class TimelineTone(
    val icon: ImageVector,
    val color: Color,
)

private fun Double?.inventoryStatus(): InventoryStatus =
    when {
        this == null -> InventoryStatus("结存未记录", StatusType.COMPLETED)
        this > 0.0 -> InventoryStatus("库存充足", StatusType.PENDING)
        this == 0.0 -> InventoryStatus("库存为零", StatusType.LOW_STOCK)
        else -> InventoryStatus("库存异常", StatusType.OUT_OF_STOCK)
    }

private fun InventoryLedgerItem.timelineTone(): TimelineTone =
    when {
        quantityChange > 0 -> TimelineTone(Icons.Filled.Add, ZhihuijiPrimary)
        quantityChange < 0 -> TimelineTone(Icons.Filled.Remove, WarningOrange)
        else -> TimelineTone(Icons.Filled.Inventory, TextTertiary)
    }

private fun InventoryLedgerItem.auxiliaryText(): String? {
    val noteText = notes?.takeIf { it.isNotBlank() }
    if (quantityBefore == null && unitCost == null && noteText == null) {
        return null
    }
    return buildString {
        var needsSeparator = false

        fun appendSeparatorIfNeeded() {
            if (needsSeparator) {
                append(" · ")
            }
            needsSeparator = true
        }

        quantityBefore?.let {
            appendSeparatorIfNeeded()
            append("变动前: ")
            append(it.formatQuantity())
        }
        unitCost?.let {
            appendSeparatorIfNeeded()
            append("单位成本: ")
            append(MoneyFormatter.format(it))
        }
        noteText?.let {
            appendSeparatorIfNeeded()
            append(it)
        }
    }
}

private fun Double.formatQuantity(): String {
    val rounded = kotlin.math.round(this)
    return if (abs(this - rounded) < 0.0001) {
        String.format(Locale.US, "%,.0f", this)
    } else {
        String.format(Locale.US, "%,.2f", this)
    }
}

private val ledgerSourceLabels = mapOf(
    "sale_order" to "销售出库",
    "sales_return" to "销售退货入库",
    "purchase_order" to "采购单",
    "purchase_receipt" to "采购入库",
    "stock_adjust" to "库存调整",
    "inventory_snapshot" to "库存盘点",
)

private fun String.sourceLabel(): String =
    ledgerSourceLabels[this] ?: ifBlank { "库存变动" }
