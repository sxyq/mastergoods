package com.zhihuiji.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryLight

@Composable
fun InventorySnapshotScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventorySnapshotViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadTodaySnapshots()
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "库存盘点",
                subtitle = "${uiState.snapshotDateLabel} · 真实库存快照",
                onNavigationClick = onNavigateBack,
            )
        },
        bottomBar = {
            BottomActionBar(
                primaryText = if (uiState.isSubmitting) "提交中..." else "完成盘点",
                onPrimaryClick = viewModel::completeInventoryCount,
                primaryEnabled = uiState.canComplete,
                secondaryText = "保存草稿",
                onSecondaryClick = {},
                secondaryEnabled = false,
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            }

            uiState.error != null && uiState.items.isEmpty() -> {
                InventorySnapshotEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "盘点数据加载失败",
                    message = uiState.error ?: "请稍后重试",
                )
            }

            uiState.items.isEmpty() -> {
                InventorySnapshotEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "暂无商品可盘点",
                    message = "当前账号没有可读取的真实商品档案。",
                )
            }

            else -> {
                InventorySnapshotContent(
                    uiState = uiState,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun InventorySnapshotContent(
    uiState: InventorySnapshotUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            InventoryContextChip(
                text = "默认仓库 · 当前账号真实商品",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InventorySummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "待盘商品",
                    value = uiState.totalItems.toString(),
                    valueColor = TextPrimary,
                    tint = ZhihuijiPrimary,
                )
                InventorySummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "盘盈数量",
                    value = "+${uiState.gainQuantity.formatQuantity()}",
                    valueColor = SuccessGreen,
                    tint = SuccessGreen,
                )
                InventorySummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "盘亏数量",
                    value = uiState.lossQuantity.formatQuantity(),
                    valueColor = DangerRed,
                    tint = DangerRed,
                )
            }
        }

        if (uiState.statusMessage != null || uiState.error != null) {
            item {
                InventoryNoticeCard(
                    message = uiState.statusMessage ?: uiState.error.orEmpty(),
                    isError = uiState.error != null,
                    onRefresh = onRefresh,
                )
            }
        }

        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceMedium,
                contentPadding = 14.dp,
            ) {
                Text(
                    text = "草稿保存需后端盘点草稿接口；当前页面只提交真实库存快照。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "盘点明细",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Text(
                    text = "已盘 ${uiState.countedItems} / ${uiState.totalItems}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
            }
        }

        items(
            items = uiState.items,
            key = { it.productId },
        ) { item ->
            InventoryCountItemCard(item = item)
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun InventoryContextChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        LiquidGlassCard(
            shape = RoundedCornerShape(100.dp),
            surfaceColor = GlassSurfaceHigh,
            contentPadding = 10.dp,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun InventorySummaryCard(
    title: String,
    value: String,
    valueColor: Color,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tint.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (valueColor == TextPrimary) TextSecondary else valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InventoryNoticeCard(
    message: String,
    isError: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 14.dp,
        onClick = if (isError) onRefresh else null,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) DangerRed else SuccessGreen,
        )
    }
}

@Composable
private fun InventoryCountItemCard(
    item: InventoryCountItem,
    modifier: Modifier = Modifier,
) {
    val extraInfoText = remember(item.snapshotCreatedAt, item.totalValue) {
        val createdAtText = item.snapshotCreatedAt?.let { "快照 $it" }
        val totalValueText = item.totalValue?.let { "库存价值 ¥%.2f".format(it) }
        if (createdAtText == null && totalValueText == null) {
            null
        } else {
            buildString {
                createdAtText?.let { append(it) }
                totalValueText?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it)
                }
            }
        }
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductAvatar(name = item.productName)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName.ifBlank { "未命名商品" },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "SKU: ${item.productCode.ifBlank { "-" }}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    InventoryDifferencePill(item = item)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "系统库存",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextTertiary,
                        )
                        Text(
                            text = item.systemQuantity.formatQuantity(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "实际盘点",
                            style = MaterialTheme.typography.labelLarge,
                            color = ZhihuijiPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CountedQuantityBox(item = item)
                    }
                }

                if (extraInfoText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = extraInfoText,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val initial = remember(name) {
        name.take(1).ifBlank { "货" }
    }
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ZhihuijiPrimaryLight.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineMedium,
            color = ZhihuijiPrimary,
        )
    }
}

@Composable
private fun InventoryDifferencePill(
    item: InventoryCountItem,
    modifier: Modifier = Modifier,
) {
    when {
        !item.isCounted -> StatusPill(
            modifier = modifier,
            text = "待盘",
            status = StatusType.PENDING,
        )

        item.isBalanced -> StatusPill(
            modifier = modifier,
            text = "正常",
            status = StatusType.COMPLETED,
        )

        (item.difference ?: 0.0) > 0 -> StatusPill(
            modifier = modifier,
            text = "+${item.difference?.formatQuantity()}",
            status = StatusType.NORMAL,
        )

        else -> StatusPill(
            modifier = modifier,
            text = item.difference?.formatQuantity().orEmpty(),
            status = StatusType.OUT_OF_STOCK,
        )
    }
}

@Composable
private fun CountedQuantityBox(
    item: InventoryCountItem,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        !item.isCounted -> WarningOrange.copy(alpha = 0.48f)
        item.isBalanced -> ZhihuijiPrimary.copy(alpha = 0.38f)
        (item.difference ?: 0.0) > 0 -> SuccessGreen.copy(alpha = 0.55f)
        else -> DangerRed.copy(alpha = 0.55f)
    }
    val textColor = when {
        !item.isCounted -> WarningOrange
        item.isBalanced -> TextPrimary
        (item.difference ?: 0.0) > 0 -> SuccessGreen
        else -> DangerRed
    }

    Box(
        modifier = modifier
            .width(94.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(alpha = 0.52f))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(9.dp))
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.countedQuantity?.formatQuantity() ?: "待生成",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InventorySnapshotEmptyState(
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

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) "%.0f".format(this) else "%.2f".format(this)
