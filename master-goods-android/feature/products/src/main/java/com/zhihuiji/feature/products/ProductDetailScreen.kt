package com.zhihuiji.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToStockAdjust: (Long) -> Unit,
    onNavigateToInventoryLedger: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    ProductDetailScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = { onNavigateToEdit(productId) },
        onNavigateToStockAdjust = { onNavigateToStockAdjust(productId) },
        onNavigateToInventoryLedger = { onNavigateToInventoryLedger(productId) },
        onDelete = { viewModel.deleteProduct(productId) },
        modifier = modifier
    )
}

@Composable
private fun ProductDetailScreenContent(
    uiState: ProductDetailUiState,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToStockAdjust: () -> Unit,
    onNavigateToInventoryLedger: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "商品详情",
                subtitle = uiState.product?.code ?: "价格、库存与供应信息",
                onNavigationClick = onNavigateBack,
                actions = {
                    uiState.product?.let {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = DangerRed
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            uiState.product?.let { product ->
                BottomActionBar(
                    primaryText = "调整库存",
                    onPrimaryClick = onNavigateToStockAdjust,
                    secondaryText = "编辑资料",
                    onSecondaryClick = onNavigateToEdit,
                    totalLabel = "当前库存",
                    totalAmount = "%.2f ${product.unit}".format(product.stock)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZhihuijiPrimary)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.product != null -> {
                    val product = uiState.product!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 基本信息卡片
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary
                                    )
                                    val statusType = when {
                                        product.stock <= 0 -> StatusType.OUT_OF_STOCK
                                        product.stock < product.safeStock -> StatusType.LOW_STOCK
                                        else -> StatusType.NORMAL
                                    }
                                    StatusPill(
                                        text = when (statusType) {
                                            StatusType.OUT_OF_STOCK -> "缺货"
                                            StatusType.LOW_STOCK -> "低库存"
                                            else -> "正常"
                                        },
                                        status = statusType
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "编码: ${product.code}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        // 价格信息卡片
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "价格信息",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                InfoRow(label = "销售价", value = "¥%.2f".format(product.salePrice))
                                InfoRow(label = "进货价", value = "¥%.2f".format(product.purchasePrice))
                            }
                        }

                        // 库存信息卡片
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "库存信息",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                InfoRow(label = "当前库存", value = "%.2f ${product.unit}".format(product.stock))
                                InfoRow(label = "安全库存", value = "%.2f ${product.unit}".format(product.safeStock))
                            }
                        }

                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onNavigateToInventoryLedger
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "商品库存流水",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "查看真实入库、出库、调整与盘点记录",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "查看库存流水",
                                    tint = ZhihuijiPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(128.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
