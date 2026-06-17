package com.zhihuiji.feature.purchases

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.GlassBorder
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryLight

private const val PurchaseReturnUnavailableMessage =
    "采购退货后端合同已经补齐；当前移动端页面仍以来源预览为主，完整的新建、确认、退款和取消交互待继续接入。"

@Composable
fun PurchaseReturnScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseReturnViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "采购退货",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多操作未接入",
                            tint = TextTertiary,
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomActionBar(
                primaryText = "提交退货申请",
                onPrimaryClick = {},
                primaryEnabled = false,
                totalLabel = "预计退款总额",
                totalAmount = "待计算",
                totalAmountColor = ZhihuijiPrimary,
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.sourceOrders.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            }

            uiState.error != null && uiState.sourceOrders.isEmpty() -> {
                PurchaseReturnUnavailableContent(
                    title = "采购退货来源加载失败",
                    message = uiState.error ?: "请稍后重试",
                    modifier = Modifier.padding(paddingValues),
                )
            }

            uiState.selectedOrder == null -> {
                PurchaseReturnUnavailableContent(
                    title = "暂无可预览的采购订单",
                    message = "当前账号没有后端返回的真实采购单，暂不能生成采购退货来源预览。",
                    modifier = Modifier.padding(paddingValues),
                )
            }

            else -> {
                PurchaseReturnContent(
                    uiState = uiState,
                    sourceOrder = uiState.selectedOrder,
                    onSelectSourceOrder = viewModel::selectSourceOrder,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun PurchaseReturnContent(
    uiState: PurchaseReturnUiState,
    sourceOrder: PurchaseReturnSourceOrder?,
    onSelectSourceOrder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PurchaseReturnSupplierCard(sourceOrder = sourceOrder)
        }

        if (uiState.error != null) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceMedium,
                    contentPadding = 14.dp,
                ) {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item {
            PurchaseReturnReasonCard()
        }

        item {
            PurchaseReturnItemsCard(sourceOrder = sourceOrder)
        }

        item {
            PurchaseReturnRefundCard(sourceOrder = sourceOrder)
        }

        if (uiState.sourceOrders.size > 1) {
            item {
                Text(
                    text = "其他采购来源单",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(
                items = uiState.sourceOrders.filterNot { it.id == sourceOrder?.id },
                key = { it.id },
            ) { order ->
                PurchaseReturnSourceOrderCompactCard(
                    order = order,
                    onClick = { onSelectSourceOrder(order.id) },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }
}

@Composable
private fun PurchaseReturnSupplierCard(
    sourceOrder: PurchaseReturnSourceOrder?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PurchaseReturnGlyph(text = "供")
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sourceOrder?.supplierName ?: "未选择真实供应商",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sourceOrder?.let {
                            "来源单: ${it.orderNo} | ${it.createdAtText} | ${it.statusText}"
                        } ?: "联系人、电话与采购退货来源单待后端返回",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextTertiary,
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.50f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "当前应付欠款",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = sourceOrder?.payableBalanceText ?: "待后端返回",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (sourceOrder == null) TextTertiary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            ContractHint()
        }
    }
}

@Composable
private fun PurchaseReturnReasonCard(
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        SectionTitle(
            title = "退货原因",
            trailing = "*必填",
            trailingColor = WarningOrange,
        )

        Spacer(modifier = Modifier.height(12.dp))

        DisabledInputRow(
            text = "请选择退货原因",
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            },
        )

        Spacer(modifier = Modifier.height(10.dp))

        DisabledTextArea(text = "补充说明（选填）")
    }
}

@Composable
private fun PurchaseReturnItemsCard(
    sourceOrder: PurchaseReturnSourceOrder?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        SectionTitle(
            title = "退货商品",
            trailing = "+ 添加商品",
            trailingColor = ZhihuijiPrimary.copy(alpha = 0.45f),
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (sourceOrder == null || sourceOrder.lines.isEmpty()) {
            PurchaseReturnEmptyItemsPanel()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sourceOrder.lines.forEachIndexed { index, line ->
                    PurchaseReturnSourceLineCard(
                        line = line,
                        showDivider = index < sourceOrder.lines.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseReturnRefundCard(
    sourceOrder: PurchaseReturnSourceOrder?,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp,
    ) {
        Text(
            text = "退款信息",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "退款方式",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DisabledChoicePill(
                    text = "冲减欠款",
                    selected = true,
                )
                DisabledChoicePill(
                    text = "原路退回",
                    selected = false,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.50f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "预计退款总额",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "待计算",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZhihuijiPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sourceOrder?.let { "来源商品合计 ${it.totalQuantityText} 件" }
                        ?: "共 0 件真实退货商品",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: String,
    trailingColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelLarge,
            color = trailingColor,
        )
    }
}

@Composable
private fun PurchaseReturnSourceLineCard(
    line: PurchaseReturnSourceLine,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurfaceMedium)
                    .border(1.dp, Color.White.copy(alpha = 0.64f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = line.productName.firstOrNull()?.toString() ?: "货",
                    style = MaterialTheme.typography.titleLarge,
                    color = ZhihuijiPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.productName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "编码: ${line.productCode}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = line.unitCostText,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "来源采购数量: ${line.quantityText}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(GlassSurfaceMedium)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    ReadOnlyReturnQuantityStepper(value = line.quantityText)
                }

                Text(
                    text = "来源金额 ${line.amountText}；当前移动端仍按来源采购数量展示，后续补齐已退数量与可退数量联动。",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = Color.White.copy(alpha = 0.50f),
            )
        }
    }
}

@Composable
private fun ReadOnlyReturnQuantityStepper(
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "−", style = MaterialTheme.typography.labelLarge, color = TextTertiary)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(42.dp),
        )
        Text(text = "+", style = MaterialTheme.typography.labelLarge, color = TextTertiary)
    }
}

@Composable
private fun PurchaseReturnSourceOrderCompactCard(
    order: PurchaseReturnSourceOrder,
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
                    text = order.orderNo,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${order.supplierName} · ${order.lines.size} 个商品",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = order.totalAmountText,
                style = MaterialTheme.typography.titleMedium,
                color = ZhihuijiPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PurchaseReturnUnavailableContent(
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
                .padding(horizontal = 20.dp),
            surfaceColor = GlassSurfaceHigh,
            contentPadding = 18.dp,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PurchaseReturnEmptyItemsPanel(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceMedium)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PurchaseReturnGlyph(text = "货", size = 52.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "暂无真实可退商品",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "不会用设计稿样例商品填充。接入采购退货合同后，这里再展示商品图、可退数量、单价与数量选择器。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PurchaseReturnGlyph(
    text: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(ZhihuijiPrimaryLight.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = ZhihuijiPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ContractHint(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningOrange.copy(alpha = 0.08f))
            .border(1.dp, WarningOrange.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = PurchaseReturnUnavailableMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun DisabledInputRow(
    text: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingIcon()
        }
    }
}

@Composable
private fun DisabledTextArea(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
    }
}

@Composable
private fun DisabledChoicePill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ZhihuijiPrimaryLight.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.48f))
            .border(
                width = 1.dp,
                color = if (selected) ZhihuijiPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.62f),
                shape = RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) ZhihuijiPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
