package com.zhihuiji.feature.purchases

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.designsystem.BackgroundGradientEnd
import com.zhihuiji.core.designsystem.GlassBorderSoft
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SurfaceGray
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryLight

private val ProductImageGradients = listOf(
    listOf(Color(0xFFEAF4FF), Color(0xFFD8E2FF)),
    listOf(Color(0xFFFFF7ED), Color(0xFFFFE5C2)),
    listOf(Color(0xFFF0FDF8), Color(0xFFCCFBF1)),
)

@Composable
fun PurchaseOrderEditScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.onSaveSuccessHandled()
            onSaveSuccess()
        }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = if (uiState.isLoading) "加载中..." else "采购开单",
                onNavigationClick = onBackClick,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "更多",
                            tint = ZhihuijiPrimary,
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                PurchaseOrderFloatingSummary(
                    totalAmount = uiState.totalAmount,
                    isSaving = uiState.isSaving,
                    onSave = viewModel::saveOrder,
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            } else {
                PurchaseOrderEditContent(
                    uiState = uiState,
                    onSupplierIdChange = viewModel::updateSupplierId,
                    onSupplierNameChange = viewModel::updateSupplierName,
                    onRemarkChange = viewModel::updateRemark,
                    onAddItem = viewModel::addItem,
                    onRemoveItem = viewModel::removeItem,
                    onItemProductChange = viewModel::updateItemProduct,
                    onItemQuantityChange = viewModel::updateItemQuantity,
                    onItemUnitCostChange = viewModel::updateItemUnitCost,
                )
            }
        }
    }
}

@Composable
private fun PurchaseOrderEditContent(
    uiState: PurchaseOrderEditUiState,
    onSupplierIdChange: (String) -> Unit,
    onSupplierNameChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onItemProductChange: (Int, String, String, String?) -> Unit,
    onItemQuantityChange: (Int, String) -> Unit,
    onItemUnitCostChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SupplierSelectionCard(
            supplierId = uiState.supplierId,
            supplierName = uiState.supplierName,
            onSupplierIdChange = onSupplierIdChange,
            onSupplierNameChange = onSupplierNameChange,
        )

        PurchaseItemsSection(
            items = uiState.items,
            onAddItem = onAddItem,
            onRemoveItem = onRemoveItem,
            onItemProductChange = onItemProductChange,
            onItemQuantityChange = onItemQuantityChange,
            onItemUnitCostChange = onItemUnitCostChange,
        )

        RemarkCard(
            remark = uiState.remark,
            onRemarkChange = onRemarkChange,
        )

        if (uiState.error != null) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                surfaceColor = WarningOrange.copy(alpha = 0.10f),
            ) {
                Text(
                    text = uiState.error,
                    color = WarningOrange,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(260.dp))
    }
}

@Composable
private fun SupplierSelectionCard(
    supplierId: String,
    supplierName: String,
    onSupplierIdChange: (String) -> Unit,
    onSupplierNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 24.dp,
        shape = RoundedCornerShape(22.dp),
        surfaceColor = GlassSurfaceHigh,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZhihuijiPrimaryLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "供应商",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = supplierName.ifBlank { "请选择或录入供应商" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextTertiary,
                )
            }

            GlassTextField(
                value = supplierName,
                onValueChange = onSupplierNameChange,
                label = "供应商名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            GlassTextField(
                value = supplierId,
                onValueChange = onSupplierIdChange,
                label = "供应商ID",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DisabledSelectField(
                    label = "结算方式",
                    value = "待接入真实字段",
                    modifier = Modifier.weight(1f),
                )
                DisabledSelectField(
                    label = "入库仓库",
                    value = "待接入真实字段",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DisabledSelectField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.34f))
                .border(0.6.dp, Color.White.copy(alpha = 0.52f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PurchaseItemsSection(
    items: List<PurchaseEditItem>,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onItemProductChange: (Int, String, String, String?) -> Unit,
    onItemQuantityChange: (Int, String) -> Unit,
    onItemUnitCostChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "采购明细",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "共 ${items.size} 件商品",
                style = MaterialTheme.typography.labelMedium,
                color = ZhihuijiPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        items.forEachIndexed { index, item ->
            PurchaseItemCard(
                index = index,
                item = item,
                onRemove = { onRemoveItem(index) },
                onProductIdChange = { productId ->
                    onItemProductChange(index, productId, item.productName, null)
                },
                onProductNameChange = { productName ->
                    onItemProductChange(index, item.productId?.toString() ?: "", productName, null)
                },
                onProductCodeChange = { productCode ->
                    onItemProductChange(index, item.productId?.toString() ?: "", item.productName, productCode)
                },
                onQuantityChange = { onItemQuantityChange(index, it) },
                onUnitCostChange = { onItemUnitCostChange(index, it) },
            )
        }

        DashedAddProductButton(onClick = onAddItem)

        if (items.isEmpty()) {
            Text(
                text = "当前还没有商品明细。先添加一行，再录入真实商品信息。",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PurchaseItemCard(
    index: Int,
    item: PurchaseEditItem,
    onRemove: () -> Unit,
    onProductIdChange: (String) -> Unit,
    onProductNameChange: (String) -> Unit,
    onProductCodeChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitCostChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quantityInt = item.quantity.toDoubleOrNull()?.toInt()?.coerceAtLeast(0) ?: 0
    val productIdentifier = item.productCode.ifBlank { item.productId?.toString() ?: "未填写真实编码" }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 22.dp,
        shape = RoundedCornerShape(22.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProductImagePlaceholder(index = index)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName.ifBlank { "商品 ${index + 1}" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "条码/编码: $productIdentifier",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "单价 ",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                            )
                            Text(
                                text = "¥${item.unitCost.ifBlank { "0.00" }}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ZhihuijiPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        PurchaseQuantityPill(
                            value = quantityInt,
                            onValueChange = { onQuantityChange(it.toString()) },
                        )
                    }
                }
            }

            ProductRealFieldEditor(
                item = item,
                onProductIdChange = onProductIdChange,
                onProductNameChange = onProductNameChange,
                onProductCodeChange = onProductCodeChange,
                onUnitCostChange = onUnitCostChange,
            )
        }
    }
}

@Composable
private fun ProductRealFieldEditor(
    item: PurchaseEditItem,
    onProductIdChange: (String) -> Unit,
    onProductNameChange: (String) -> Unit,
    onProductCodeChange: (String) -> Unit,
    onUnitCostChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.24f))
            .border(0.6.dp, Color.White.copy(alpha = 0.46f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "真实保存字段",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontWeight = FontWeight.SemiBold,
        )
        GlassTextField(
            value = item.productName,
            onValueChange = onProductNameChange,
            label = "商品名称",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassTextField(
                value = item.productId?.toString() ?: "",
                onValueChange = onProductIdChange,
                label = "商品ID",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            GlassTextField(
                value = item.productCode,
                onValueChange = onProductCodeChange,
                label = "商品编码",
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        GlassTextField(
            value = item.unitCost,
            onValueChange = onUnitCostChange,
            label = "单价",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
    }
}

@Composable
private fun ProductImagePlaceholder(
    index: Int,
    modifier: Modifier = Modifier,
) {
    val brush = remember(index % 3) {
        Brush.linearGradient(colors = ProductImageGradients[index % 3])
    }
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .border(0.8.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "货",
            style = MaterialTheme.typography.headlineLarge,
            color = ZhihuijiPrimary.copy(alpha = 0.76f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PurchaseQuantityPill(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(SurfaceGray.copy(alpha = 0.86f))
            .padding(horizontal = 3.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityButton(
            icon = Icons.Default.Remove,
            enabled = value > 0,
            onClick = { onValueChange((value - 1).coerceAtLeast(0)) },
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(38.dp),
        )
        QuantityButton(
            icon = Icons.Default.Add,
            enabled = true,
            filled = true,
            onClick = { onValueChange(value + 1) },
        )
    }
}

@Composable
private fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    filled: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (filled) ZhihuijiPrimary else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                filled -> SurfaceWhite
                enabled -> TextPrimary
                else -> TextTertiary.copy(alpha = 0.45f)
            },
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun DashedAddProductButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = 1.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()), 0f),
            )
            drawRoundRect(
                color = ZhihuijiPrimary.copy(alpha = 0.46f),
                topLeft = Offset(stroke.width / 2, stroke.width / 2),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                style = stroke,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = null,
                tint = ZhihuijiPrimary,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = "继续添加商品",
                style = MaterialTheme.typography.bodyMedium,
                color = ZhihuijiPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RemarkCard(
    remark: String,
    onRemarkChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 24.dp,
        shape = RoundedCornerShape(22.dp),
        surfaceColor = GlassSurfaceHigh,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "备注信息",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            GlassTextField(
                value = remark,
                onValueChange = onRemarkChange,
                placeholder = "请输入订单备注，如送货时间要求...",
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )
        }
    }
}

@Composable
private fun PurchaseOrderFloatingSummary(
    totalAmount: Double,
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalAmountText = remember(totalAmount) {
        MoneyFormatter.formatWithoutSymbol(totalAmount)
    }
    val bottomScrim = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                BackgroundGradientEnd.copy(alpha = 0.94f),
                BackgroundGradientEnd,
            ),
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = bottomScrim)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            blurRadius = 32.dp,
            shape = RoundedCornerShape(28.dp),
            surfaceColor = GlassSurfaceHigh,
            contentPadding = 18.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "应付合计",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "¥",
                                style = MaterialTheme.typography.titleMedium,
                                color = ZhihuijiPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = totalAmountText,
                                style = MaterialTheme.typography.headlineLarge,
                                color = ZhihuijiPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "整单折扣",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        Text(
                            text = "未接入",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ZhihuijiPrimary.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlassBorderSoft.copy(alpha = 0.42f))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DisabledDraftButton(
                        text = "保存草稿",
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = if (isSaving) "保存中..." else "保存采购单",
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = ZhihuijiPrimaryBright,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "当前版本仅保存真实采购单，不伪造草稿/提交双状态",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun DisabledDraftButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.30f))
            .border(1.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
