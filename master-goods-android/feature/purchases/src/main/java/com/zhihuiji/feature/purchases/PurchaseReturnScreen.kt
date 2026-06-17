package com.zhihuiji.feature.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.DangerOutlineButton
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.SurfaceSoft
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

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
                    IconButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "刷新采购退货",
                            tint = ZhihuijiPrimary,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading && uiState.sourceOrders.isEmpty() && uiState.returns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ZhihuijiPrimary)
            }
        } else {
            PurchaseReturnContent(
                uiState = uiState,
                onModeChange = viewModel::changeMode,
                onSelectSourceOrder = viewModel::selectSourceOrder,
                onCreateNoteChange = viewModel::updateCreateNotes,
                onCreateQuantityChange = viewModel::updateCreateQuantity,
                onCreateUnitCostChange = viewModel::updateCreateUnitCost,
                onCreateReturn = viewModel::createReturn,
                onSelectReturn = viewModel::selectReturn,
                onDetailNotesChange = viewModel::updateDetailNotes,
                onRefundAmountChange = viewModel::updateRefundAmount,
                onRefundMethodChange = viewModel::updateRefundMethod,
                onRefundReferenceChange = viewModel::updateRefundReferenceNo,
                onSaveDraft = viewModel::saveDraft,
                onConfirm = viewModel::confirmReturn,
                onAddRefund = viewModel::addRefund,
                onCancel = viewModel::cancelReturn,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PurchaseReturnContent(
    uiState: PurchaseReturnUiState,
    onModeChange: (PurchaseReturnMode) -> Unit,
    onSelectSourceOrder: (Long) -> Unit,
    onCreateNoteChange: (String) -> Unit,
    onCreateQuantityChange: (Int, String) -> Unit,
    onCreateUnitCostChange: (Int, String) -> Unit,
    onCreateReturn: () -> Unit,
    onSelectReturn: (Long) -> Unit,
    onDetailNotesChange: (String) -> Unit,
    onRefundAmountChange: (String) -> Unit,
    onRefundMethodChange: (Int) -> Unit,
    onRefundReferenceChange: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onConfirm: () -> Unit,
    onAddRefund: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            surfaceColor = GlassSurfaceHigh,
            contentPadding = 14.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "采购退货工作台", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(
                        label = "新建退货",
                        selected = uiState.mode == PurchaseReturnMode.CREATE,
                        onClick = { onModeChange(PurchaseReturnMode.CREATE) },
                    )
                    ModeChip(
                        label = "退货管理",
                        selected = uiState.mode == PurchaseReturnMode.MANAGE,
                        onClick = { onModeChange(PurchaseReturnMode.MANAGE) },
                    )
                }
                uiState.error?.takeIf { it.isNotBlank() }?.let {
                    NoticeCard(message = it, isError = true)
                }
                uiState.success?.takeIf { it.isNotBlank() }?.let {
                    NoticeCard(message = it, isError = false)
                }
            }
        }

        if (uiState.mode == PurchaseReturnMode.CREATE) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 14.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "来源采购单", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (uiState.sourceOrders.isEmpty()) {
                        EmptyBlock("暂无真实采购单可用于退货。")
                    } else {
                        uiState.sourceOrders.forEach { order ->
                            OrderChip(
                                label = "${order.orderNo} · ${order.supplierName}",
                                selected = uiState.selectedOrder?.id == order.id,
                                onClick = { onSelectSourceOrder(order.id) },
                            )
                        }
                    }
                }
            }

            uiState.selectedOrder?.let { order ->
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 14.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = order.supplierName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "${order.orderNo} · ${order.createdAtText} · ${order.statusText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        SummaryLine("来源金额", order.totalAmountText)
                        SummaryLine("已付款", order.paidAmountText)
                        SummaryLine("已收货", order.receivedAmountText)
                    }
                }

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 14.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "退货商品", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        order.lines.forEachIndexed { index, line ->
                            val draft = uiState.createItems.getOrNull(index) ?: return@forEachIndexed
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = line.productName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(text = "${line.productCode} · 来源数量 ${line.quantityText}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GlassTextField(
                                        value = draft.quantityInput,
                                        onValueChange = { onCreateQuantityChange(index, it) },
                                        label = "退货数量",
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                    )
                                    GlassTextField(
                                        value = draft.unitCostInput,
                                        onValueChange = { onCreateUnitCostChange(index, it) },
                                        label = "退货单价",
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (index < order.lines.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                        GlassTextField(
                            value = uiState.createNotes,
                            onValueChange = onCreateNoteChange,
                            label = "备注",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PrimaryButton(
                            text = if (uiState.isSaving) "创建中..." else "创建采购退货单",
                            enabled = !uiState.isSaving,
                            onClick = onCreateReturn,
                        )
                    }
                }
            }
        } else {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 14.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "退货单列表", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (uiState.returns.isEmpty()) {
                        EmptyBlock("当前没有真实采购退货单。")
                    } else {
                        uiState.returns.forEach { record ->
                            OrderChip(
                                label = "${record.returnNo} · ${record.supplierName} · ${record.statusText}",
                                selected = uiState.selectedReturn?.id == record.id,
                                onClick = { onSelectReturn(record.id) },
                            )
                        }
                    }
                }
            }

            uiState.selectedReturn?.let { record ->
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 14.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = record.returnNo, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(text = "${record.supplierName} · ${record.createdAtText}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        SummaryLine("状态", record.statusText)
                        SummaryLine("退货金额", record.totalAmountText)
                        SummaryLine("已退款", record.refundAmountText)
                        SummaryLine("待退款", record.remainingRefundText)
                    }
                }

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 14.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "退货商品", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        record.items.forEachIndexed { index, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = item.productName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(text = "${item.productCode} · ${item.quantityText} 件 · 单价 ${item.unitCostText}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(text = "金额 ${item.amountText}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                if (index < record.items.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 14.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "退款记录", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        if (record.refunds.isEmpty()) {
                            Text(text = "暂无退款记录", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        } else {
                            record.refunds.forEach { refund ->
                                Text(
                                    text = "${refund.amountText} · ${refund.methodText} · ${refund.createdAtText}${refund.referenceNo?.let { " · $it" }.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 14.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassTextField(
                            value = uiState.detailNotes,
                            onValueChange = onDetailNotesChange,
                            label = "备注",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        GlassTextField(
                            value = uiState.refundAmount,
                            onValueChange = onRefundAmountChange,
                            label = "退款金额",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethods.forEach { method ->
                                ModeChip(
                                    label = method.second,
                                    selected = uiState.refundMethod == method.first,
                                    onClick = { onRefundMethodChange(method.first) },
                                )
                            }
                        }
                        GlassTextField(
                            value = uiState.refundReferenceNo,
                            onValueChange = onRefundReferenceChange,
                            label = "参考流水号",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryOutlineButton(
                                modifier = Modifier.weight(1f),
                                text = if (uiState.isSaving) "处理中..." else "保存草稿",
                                enabled = !uiState.isSaving && record.status == 0,
                                onClick = onSaveDraft,
                            )
                            PrimaryButton(
                                modifier = Modifier.weight(1f),
                                text = "确认退货",
                                enabled = !uiState.isSaving && record.status == 0,
                                onClick = onConfirm,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton(
                                modifier = Modifier.weight(1f),
                                text = "登记退款",
                                enabled = !uiState.isSaving && record.status != 3 && record.remainingRefund > 0.0,
                                onClick = onAddRefund,
                            )
                            DangerOutlineButton(
                                modifier = Modifier.weight(1f),
                                text = "取消退货",
                                enabled = !uiState.isSaving && record.status != 3,
                                onClick = onCancel,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyBlock(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSoft.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp, vertical = 18.dp),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun NoticeCard(message: String, isError: Boolean) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceColor = if (isError) GlassSurfaceLow else GlassSurfaceHigh,
        contentPadding = 12.dp,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) WarningOrange else ZhihuijiPrimary,
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) ZhihuijiPrimary.copy(alpha = 0.18f) else SurfaceSoft.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) ZhihuijiPrimary else TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun OrderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        surfaceColor = if (selected) GlassSurfaceHigh else GlassSurfaceLow,
        contentPadding = 12.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (selected) ZhihuijiPrimary else TextTertiary),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val PaymentMethods = listOf(
    StatusLabels.Codes.METHOD_BANK to "银行卡",
    StatusLabels.Codes.METHOD_WECHAT to "微信",
    StatusLabels.Codes.METHOD_ALIPAY to "支付宝",
    StatusLabels.Codes.METHOD_CASH to "现金",
)
