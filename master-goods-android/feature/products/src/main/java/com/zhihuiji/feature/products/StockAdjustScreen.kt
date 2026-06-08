package com.zhihuiji.feature.products

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.DividerLight
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.SurfaceGray
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright

@Composable
fun StockAdjustScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    onAdjustSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StockAdjustViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onAdjustSuccess()
        }
    }

    StockAdjustScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAdjust = { quantity, reason ->
            viewModel.adjustStock(productId, quantity, reason)
        },
        modifier = modifier
    )
}

private enum class AdjustmentType(
    val label: String,
    val englishLabel: String,
    val submitSign: Double,
) {
    LOSS("盘亏", "Loss", -1.0),
    GAIN("盘盈", "Gain", 1.0),
    USE("领用", "Use", -1.0),
}

@Composable
private fun StockAdjustScreenContent(
    uiState: StockAdjustUiState,
    onNavigateBack: () -> Unit,
    onAdjust: (Double, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(AdjustmentType.LOSS) }
    var quantity by remember { mutableIntStateOf(1) }
    var remarks by remember { mutableStateOf("") }
    val businessDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val canSubmit = !uiState.isLoading && uiState.productName != null && quantity > 0
    val signedQuantity = quantity * selectedType.submitSign
    val projectedStock = uiState.currentStock + signedQuantity

    Column(modifier = modifier.fillMaxSize()) {
        StockAdjustTopBar(onNavigateBack = onNavigateBack)

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "库存调整单",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "记录盘点盈亏或领用消耗",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                AdjustmentTypeSelector(
                    selectedType = selectedType,
                    onSelected = { selectedType = it }
                )

                StockAdjustMetaCard(
                    businessDate = businessDate,
                    operatorLabel = "当前登录账号"
                )

                StockAdjustDetailCard(
                    uiState = uiState,
                    selectedType = selectedType,
                    quantity = quantity,
                    projectedStock = projectedStock,
                    onDecrease = { quantity = (quantity - 1).coerceAtLeast(1) },
                    onIncrease = { quantity = (quantity + 1).coerceAtMost(9999) }
                )

                StockAdjustRemarksCard(
                    value = remarks,
                    onValueChange = { remarks = it }
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(104.dp))
            }

            StockAdjustSubmitBar(
                enabled = canSubmit,
                isLoading = uiState.isLoading,
                onSubmit = {
                    val reason = buildAdjustmentReason(selectedType, remarks)
                    onAdjust(signedQuantity, reason)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun StockAdjustTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        surfaceColor = GlassSurfaceMedium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "智慧记",
                color = TextPrimary,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun AdjustmentTypeSelector(
    selectedType: AdjustmentType,
    onSelected: (AdjustmentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 6.dp
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AdjustmentType.entries.forEach { type ->
                val selected = selectedType == type
                val shape = RoundedCornerShape(9.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(shape)
                        .background(if (selected) ZhihuijiPrimary else Color.Transparent, shape)
                        .clickable { onSelected(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${type.label} (${type.englishLabel})",
                        color = if (selected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StockAdjustMetaCard(
    businessDate: String,
    operatorLabel: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StockAdjustInfoRow(
                icon = Icons.Outlined.CalendarToday,
                label = "业务日期",
                value = businessDate
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerLight.copy(alpha = 0.55f))
            )
            StockAdjustInfoRow(
                icon = Icons.Outlined.Person,
                label = "经办人",
                value = operatorLabel
            )
        }
    }
}

@Composable
private fun StockAdjustDetailCard(
    uiState: StockAdjustUiState,
    selectedType: AdjustmentType,
    quantity: Int,
    projectedStock: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "调整明细",
                color = TextPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "共 1 项",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(SurfaceGray.copy(alpha = 0.72f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }

        if (uiState.isLoading && uiState.productName == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZhihuijiPrimary, modifier = Modifier.size(28.dp))
            }
        } else {
            ProductAdjustRow(
                uiState = uiState,
                selectedType = selectedType,
                quantity = quantity,
                projectedStock = projectedStock,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        AddProductPlaceholder(modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun ProductAdjustRow(
    uiState: StockAdjustUiState,
    selectedType: AdjustmentType,
    quantity: Int,
    projectedStock: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory,
                contentDescription = null,
                tint = ZhihuijiPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = uiState.productName ?: "商品加载中",
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "SKU: ${uiState.productCode.ifBlank { "未设置编码" }}",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "当前账面库存: ${formatStock(uiState.currentStock, uiState.unitName)}",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "调整后预计: ${formatStock(projectedStock, uiState.unitName)}",
                color = when (selectedType) {
                    AdjustmentType.GAIN -> SuccessGreen
                    AdjustmentType.LOSS, AdjustmentType.USE -> DangerRed
                },
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        QuantityStepper(
            quantity = quantity,
            selectedType = selectedType,
            onDecrease = onDecrease,
            onIncrease = onIncrease
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    selectedType: AdjustmentType,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quantityColor = when (selectedType) {
        AdjustmentType.GAIN -> SuccessGreen
        AdjustmentType.LOSS, AdjustmentType.USE -> DangerRed
    }

    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(SurfaceWhite.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, DividerLight), RoundedCornerShape(9.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            icon = Icons.Default.Remove,
            contentDescription = "减少数量",
            onClick = onDecrease
        )
        Box(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quantity.toString(),
                color = quantityColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        StepperButton(
            icon = Icons.Default.Add,
            contentDescription = "增加数量",
            onClick = onIncrease
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(34.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AddProductPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .drawBehind {
                drawRoundRect(
                    color = DividerLight,
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(10.dp.toPx(), 7.dp.toPx()),
                            phase = 0f
                        )
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AddCircle,
                contentDescription = null,
                tint = ZhihuijiPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "添加调整商品",
                color = ZhihuijiPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun StockAdjustRemarksCard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp
    ) {
        Text(
            text = "备注说明",
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold
        )
        RemarksInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = "请输入调整原因，例如：破损、过期或内部试用...",
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .padding(top = 12.dp)
        )
    }
}

@Composable
private fun RemarksInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = TextPrimary,
            lineHeight = 20.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(SurfaceWhite.copy(alpha = 0.74f))
                    .border(BorderStroke(1.dp, DividerLight), shape)
                    .padding(12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextTertiary.copy(alpha = 0.74f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun StockAdjustSubmitBar(
    enabled: Boolean,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        surfaceColor = GlassSurfaceMedium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(
                        if (enabled) ZhihuijiPrimaryBright else TextTertiary.copy(alpha = 0.42f),
                        shape
                    )
                    .then(if (enabled) Modifier.clickable(onClick = onSubmit) else Modifier),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "提交调整单",
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StockAdjustInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun buildAdjustmentReason(type: AdjustmentType, remarks: String): String =
    listOf(type.label, remarks.trim())
        .filter { it.isNotBlank() }
        .joinToString("：")

private fun formatStock(value: Double, unitName: String): String {
    val normalized = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.2f".format(value).trimEnd('0').trimEnd('.')
    }
    return if (unitName.isBlank()) normalized else "$normalized $unitName"
}
