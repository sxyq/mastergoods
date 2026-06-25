package com.zhihuiji.feature.products

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.model.v2.product.ProductCategoryV2Dto
import com.zhihuiji.core.model.v2.product.ProductUnitV2Dto
import com.zhihuiji.core.designsystem.DividerLight
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.SurfaceGray
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright

private val productActionPrimaryBrush = Brush.horizontalGradient(
    listOf(ZhihuijiPrimaryBright, ZhihuijiPrimary),
)

@Composable
fun ProductEditScreen(
    productId: Long?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.consumeSaveSuccess()
            onSaveSuccess()
        }
    }

    ProductEditScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCategorySelect = viewModel::selectCategory,
        onUnitSelect = viewModel::selectUnit,
        onSave = { form, continueAdding ->
            val categoryId = form.categoryId ?: return@ProductEditScreenContent
            val unitId = form.unitId ?: return@ProductEditScreenContent
            val salePrice = form.salePrice.toDoubleOrNull() ?: 0.0
            val purchasePrice = form.purchasePrice.toDoubleOrNull() ?: 0.0
            val stock = form.stock.toDoubleOrNull() ?: 0.0
            val safeStock = form.safeStock.toDoubleOrNull() ?: 0.0
            if (productId != null) {
                viewModel.updateProduct(
                    productId = productId,
                    name = form.name,
                    code = form.code,
                    categoryId = categoryId,
                    unitId = unitId,
                    salePrice = salePrice,
                    purchasePrice = purchasePrice,
                    stock = stock,
                    safeStock = safeStock,
                )
            } else {
                viewModel.createProduct(
                    name = form.name,
                    code = form.code,
                    categoryId = categoryId,
                    unitId = unitId,
                    salePrice = salePrice,
                    purchasePrice = purchasePrice,
                    stock = stock,
                    safeStock = safeStock,
                    continueAdding = continueAdding,
                )
            }
        },
        modifier = modifier
    )
}

private data class ProductEditForm(
    val name: String,
    val code: String,
    val categoryId: Long?,
    val unitId: Long?,
    val salePrice: String,
    val purchasePrice: String,
    val stock: String,
    val safeStock: String,
)

@Composable
private fun ProductEditScreenContent(
    uiState: ProductEditUiState,
    onNavigateBack: () -> Unit,
    onCategorySelect: (ProductCategoryV2Dto) -> Unit,
    onUnitSelect: (ProductUnitV2Dto) -> Unit,
    onSave: (ProductEditForm, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(uiState.name) { mutableStateOf(uiState.name) }
    var code by remember(uiState.code) { mutableStateOf(uiState.code) }
    var salePrice by remember(uiState.salePrice) { mutableStateOf(uiState.salePrice) }
    var purchasePrice by remember(uiState.purchasePrice) { mutableStateOf(uiState.purchasePrice) }
    var stock by remember(uiState.stock) { mutableStateOf(uiState.stock) }
    var safeStock by remember(uiState.safeStock) { mutableStateOf(uiState.safeStock) }

    val form = ProductEditForm(
        name = name,
        code = code,
        categoryId = uiState.categoryId,
        unitId = uiState.unitId,
        salePrice = salePrice,
        purchasePrice = purchasePrice,
        stock = stock,
        safeStock = safeStock,
    )
    val canSave = name.isNotBlank() && uiState.categoryId != null && uiState.unitId != null && !uiState.isLoading

    Column(modifier = modifier.fillMaxSize()) {
        ProductEditTopBar(
            title = if (uiState.isEditMode) "编辑商品" else "添加商品",
            onNavigateBack = onNavigateBack,
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZhihuijiPrimary)
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductImageUploadCard()

                    ProductFormSection(title = "基本信息") {
                        ProductInputField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "商品名称 (必填)",
                            leadingIcon = Icons.Outlined.Badge,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategorySelectField(
                                value = uiState.categoryName,
                                categories = uiState.categories,
                                onSelect = onCategorySelect,
                                modifier = Modifier.weight(1f),
                            )
                            UnitSelectField(
                                value = uiState.unitName,
                                units = uiState.units,
                                onSelect = onUnitSelect,
                                modifier = Modifier.width(116.dp)
                            )
                        }

                        ProductInputField(
                            value = code,
                            onValueChange = { code = it },
                            placeholder = "条码编号",
                            leadingIcon = Icons.Outlined.QrCodeScanner,
                            trailingIcon = Icons.Outlined.QrCodeScanner,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ProductFormSection(title = "价格设置") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProductInputField(
                                value = salePrice,
                                onValueChange = { salePrice = it },
                                label = "零售价 (¥)",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            ProductInputField(
                                value = purchasePrice,
                                onValueChange = { purchasePrice = it },
                                label = "进货价 (¥)",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ProductInputField(
                            value = uiState.wholesalePrice,
                            onValueChange = {},
                            label = "批发价 (¥)",
                            placeholder = "价格等级接入后可用",
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ProductFormSection(title = "库存与供应商") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProductInputField(
                                value = stock,
                                onValueChange = { stock = it },
                                label = "期初库存",
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            ProductInputField(
                                value = safeStock,
                                onValueChange = { safeStock = it },
                                label = "安全库存",
                                placeholder = "5",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = Icons.Filled.Warning,
                                trailingTint = WarningOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ProductInputField(
                            value = uiState.supplierName,
                            onValueChange = {},
                            placeholder = "首选供应商关系待接入",
                            leadingIcon = Icons.Outlined.LocalShipping,
                            trailingIcon = Icons.Outlined.LocalShipping,
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ProductContractNotice(
                        hasCategory = uiState.categoryId != null,
                        hasUnit = uiState.unitId != null,
                    )

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }

                ProductEditBottomActionBar(
                    canSave = canSave,
                    canContinueAdding = !uiState.isEditMode && canSave,
                    onSave = { onSave(form, false) },
                    onSaveAndContinue = { onSave(form, true) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun ProductEditTopBar(
    title: String,
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
                text = title,
                color = TextPrimary,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = "扫码待接入",
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ProductImageUploadCard(modifier: Modifier = Modifier) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 160.dp),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceHigh,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddAPhoto,
                    contentDescription = "点击上传商品图片",
                    tint = TextTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "点击上传商品图片",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "支持 JPG, PNG 格式",
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ProductFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                color = ZhihuijiPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            content()
        }
    }
}

@Composable
private fun CategorySelectField(
    value: String,
    categories: List<ProductCategoryV2Dto>,
    onSelect: (ProductCategoryV2Dto) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        ProductInputField(
            value = value,
            onValueChange = {},
            placeholder = if (categories.isEmpty()) "暂无分类" else "选择分类",
            leadingIcon = Icons.Outlined.Category,
            trailingIcon = Icons.Outlined.ExpandMore,
            enabled = categories.isNotEmpty(),
            readOnly = true,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        expanded = false
                        onSelect(category)
                    },
                )
            }
        }
    }
}

@Composable
private fun UnitSelectField(
    value: String,
    units: List<ProductUnitV2Dto>,
    onSelect: (ProductUnitV2Dto) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        ProductInputField(
            value = value,
            onValueChange = {},
            placeholder = if (units.isEmpty()) "单位" else "单位",
            textAlign = TextAlign.Center,
            trailingIcon = Icons.Outlined.ExpandMore,
            enabled = units.isNotEmpty(),
            readOnly = true,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        expanded = false
                        onSelect(unit)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProductContractNotice(
    hasCategory: Boolean,
    hasUnit: Boolean,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = WarningOrange.copy(alpha = 0.08f),
        contentPadding = 12.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (hasCategory && hasUnit) "真实保存字段已就绪" else "保存前需要选择真实分类和单位",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasCategory && hasUnit) ZhihuijiPrimary else WarningOrange,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "图片上传、扫码、批发价等级和首选供应商关系还没有完整写入流程，本页先禁用展示，避免产生模拟数据。",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun ProductInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    trailingTint: Color = TextTertiary,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textAlign: TextAlign = TextAlign.Start,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (enabled) DividerLight else DividerLight.copy(alpha = 0.7f)
    val textColor = if (enabled) TextPrimary else TextTertiary
    val contentAlpha = if (enabled) 1f else 0.58f
    val fieldHeight = if (label == null) 56.dp else 58.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = textColor,
            textAlign = textAlign,
            fontWeight = FontWeight.Medium
        ),
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .height(fieldHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(SurfaceWhite.copy(alpha = if (enabled) 0.82f else 0.62f))
                    .border(BorderStroke(1.dp, borderColor), shape)
                    .padding(horizontal = 12.dp, vertical = if (label == null) 0.dp else 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = TextTertiary.copy(alpha = contentAlpha),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                if (label == null) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = when (textAlign) {
                            TextAlign.Center -> Alignment.Center
                            TextAlign.Right, TextAlign.End -> Alignment.CenterEnd
                            else -> Alignment.CenterStart
                        }
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextTertiary.copy(alpha = contentAlpha),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    textAlign = textAlign
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            color = TextSecondary.copy(alpha = contentAlpha),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = TextTertiary.copy(alpha = contentAlpha),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        textAlign = textAlign
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = trailingTint.copy(alpha = contentAlpha),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun ProductEditBottomActionBar(
    canSave: Boolean,
    canContinueAdding: Boolean,
    onSave: () -> Unit,
    onSaveAndContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        surfaceColor = GlassSurfaceHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductActionButton(
                text = "保存并继续添加",
                onClick = onSaveAndContinue,
                enabled = canContinueAdding,
                primary = false,
                modifier = Modifier.weight(1f)
            )
            ProductActionButton(
                text = "保存",
                onClick = onSave,
                enabled = canSave,
                primary = true,
                icon = Icons.Filled.Check,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProductActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        label = "${text}_product_action_scale"
    )
    val shape = RoundedCornerShape(12.dp)
    val backgroundModifier = if (primary) {
        Modifier.background(
            brush = productActionPrimaryBrush,
            alpha = if (enabled) 1f else 0.48f,
            shape = shape
        )
    } else {
        Modifier
            .background(SurfaceWhite.copy(alpha = if (enabled) 0.86f else 0.52f), shape)
            .border(BorderStroke(1.dp, DividerLight), shape)
    }

    Row(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .clip(shape)
            .then(backgroundModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = when {
                primary -> SurfaceWhite
                enabled -> TextPrimary
                else -> TextTertiary
            },
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
