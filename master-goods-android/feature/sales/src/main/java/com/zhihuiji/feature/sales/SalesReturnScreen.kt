package com.zhihuiji.feature.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassBorderSoft
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.PrimaryButton
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
fun SalesReturnScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SalesReturnViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val salesReturn = uiState.selectedReturn

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SalesReturnTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            salesReturn?.let {
                SalesReturnSubmitBar(
                    salesReturn = it,
                    isSubmitting = uiState.isSubmitting,
                    onSubmit = viewModel::confirmSelectedReturn,
                )
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.returns.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            }

            uiState.error != null && uiState.returns.isEmpty() -> {
                SalesReturnEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "销售退货单加载失败",
                    message = uiState.error ?: "请稍后重试",
                )
            }

            salesReturn == null -> {
                SalesReturnNoDataContent(
                    modifier = Modifier.padding(paddingValues),
                )
            }

            else -> {
                SalesReturnContent(
                    uiState = uiState,
                    salesReturn = salesReturn,
                    onSelectReturn = viewModel::selectReturn,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun SalesReturnNoDataContent(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 16.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ZhihuijiPrimaryLight.copy(alpha = 0.42f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = ZhihuijiPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "退货客户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Text(
                            text = "暂无真实退货客户",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )
                    }
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextTertiary,
                    )
                }
            }
        }
        item {
            SectionTitle(
                title = "退货商品明细",
                action = "添加商品",
            )
        }
        item {
            EmptyLineCard()
        }
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 16.dp,
            ) {
                Text(
                    text = "退货原因",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.54f))
                        .border(0.5.dp, GlassBorderSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "当前账号还没有后端返回的销售退货记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesReturnTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 28.dp,
        shape = RoundedCornerShape(0.dp),
        surfaceColor = GlassSurfaceMedium,
        contentPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary,
                    )
                }
                Text(
                    text = "销售退货单",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = "更多",
                    tint = ZhihuijiPrimary,
                )
            }
        }
    }
}

@Composable
private fun SalesReturnContent(
    uiState: SalesReturnUiState,
    salesReturn: SalesReturnItem,
    onSelectReturn: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 172.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ReturnCustomerCard(salesReturn = salesReturn)
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
                title = "退货商品明细",
                action = "添加商品",
            )
        }

        if (salesReturn.lines.isEmpty()) {
            item {
                EmptyLineCard()
            }
        } else {
            items(
                items = salesReturn.lines,
                key = { it.id },
            ) { item ->
                SalesReturnLineCard(item = item)
            }
        }

        item {
            ReturnExtraInfoCard(salesReturn = salesReturn)
        }

        if (uiState.returns.size > 1) {
            item {
                SectionTitle(title = "其他退货单")
            }
            items(
                items = uiState.returns.filterNot { it.id == salesReturn.id },
                key = { it.id },
            ) { item ->
                SalesReturnCompactCard(
                    salesReturn = item,
                    onClick = { onSelectReturn(item.id) },
                )
            }
        }
    }
}

@Composable
private fun ReturnCustomerCard(
    salesReturn: SalesReturnItem,
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ZhihuijiPrimaryLight.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "退货客户",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = salesReturn.customerName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = salesReturn.returnNo,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(
                text = salesReturn.statusText,
                status = salesReturn.status.toSalesReturnStatusType(),
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        if (action != null) {
            Text(
                text = "⊕ $action",
                style = MaterialTheme.typography.labelLarge,
                color = ZhihuijiPrimary,
            )
        }
    }
}

@Composable
private fun EmptyLineCard(modifier: Modifier = Modifier) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Text(
            text = "后端未返回退货商品明细",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "页面不会用设计稿样例商品补齐数据，请先创建或同步真实退货单。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SalesReturnLineCard(
    item: SalesReturnLineItem,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ProductPreviewBox(productName = item.productName)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "编码: ${item.productCode} · 单价 ¥%.2f".format(item.unitPrice),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "¥%.2f".format(item.unitPrice),
                        style = MaterialTheme.typography.labelLarge,
                        color = DangerRed,
                    )
                    QuantityStepperReadOnly(quantity = item.quantity.formatQuantity())
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
            color = Color.White.copy(alpha = 0.42f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "退货小计",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            Text(
                text = "¥%.2f".format(item.amount),
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun ProductPreviewBox(
    productName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ZhihuijiPrimaryLight.copy(alpha = 0.34f))
            .border(0.5.dp, GlassBorderSoft, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = productName.take(1).ifBlank { "货" },
            style = MaterialTheme.typography.headlineMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun QuantityStepperReadOnly(
    quantity: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GlassSurfaceMedium)
            .border(0.5.dp, GlassBorderSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = quantity,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ReturnExtraInfoCard(
    salesReturn: SalesReturnItem,
    modifier: Modifier = Modifier,
) {
    val reason = salesReturn.notes?.takeIf { it.isNotBlank() } ?: "后端未返回退货原因"
    val notes = salesReturn.notes?.takeIf { it.isNotBlank() } ?: "暂无备注说明"

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
            Text(
                text = "退货原因",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextTertiary,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = Color.White.copy(alpha = 0.42f),
        )

        Text(
            text = "备注说明",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.54f))
                .border(0.5.dp, GlassBorderSoft, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = if (salesReturn.notes.isNullOrBlank()) TextTertiary else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SalesReturnSubmitBar(
    salesReturn: SalesReturnItem,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassSurfaceHigh.copy(alpha = 0.86f))
            .border(0.5.dp, Color.White.copy(alpha = 0.38f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "应退总额 (共${salesReturn.totalQuantity.formatQuantity()}件)",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
                Text(
                    text = "¥%.2f".format(salesReturn.totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DangerRed,
                )
            }
            PrimaryButton(
                modifier = Modifier.width(142.dp),
                text = when {
                    isSubmitting -> "提交中..."
                    salesReturn.canConfirm -> "提交退货单"
                    else -> salesReturn.statusText
                },
                onClick = onSubmit,
                enabled = salesReturn.canConfirm && !isSubmitting,
            )
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SalesReturnCompactCard(
    salesReturn: SalesReturnItem,
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
                    text = salesReturn.returnNo,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${salesReturn.customerName} · ${salesReturn.createdAt}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            StatusPill(
                text = salesReturn.statusText,
                status = salesReturn.status.toSalesReturnStatusType(),
            )
        }
    }
}

@Composable
private fun SalesReturnEmptyState(
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
            )
        }
    }
}

private val SalesReturnItem.totalQuantity: Double
    get() = lines.sumOf { it.quantity }

private fun Int.toSalesReturnStatusType(): StatusType =
    when (this) {
        0 -> StatusType.PENDING
        1 -> StatusType.NORMAL
        2 -> StatusType.COMPLETED
        3 -> StatusType.CANCELLED
        else -> StatusType.NORMAL
    }

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) "%.0f".format(this) else "%.2f".format(this)
