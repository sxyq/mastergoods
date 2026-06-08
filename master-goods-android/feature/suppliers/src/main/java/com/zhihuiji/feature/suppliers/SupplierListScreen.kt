package com.zhihuiji.feature.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassBorderSoft
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusBlueLight
import com.zhihuiji.core.designsystem.SurfaceSoft
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun SupplierListScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (Long) -> Unit = {},
    searchQuery: String = "",
    viewModel: SupplierListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(searchQuery) {
        if (searchQuery != uiState.keyword) {
            viewModel.search(searchQuery)
        }
    }

    SupplierListScreenContent(
        uiState = uiState,
        onNavigateToDetail = onNavigateToDetail,
        onRetry = { viewModel.loadSuppliers() },
        modifier = modifier
    )
}

@Composable
private fun SupplierListScreenContent(
    uiState: SupplierListUiState,
    onNavigateToDetail: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
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
                SupplierStateMessage(
                    title = "供应商加载失败",
                    message = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            uiState.suppliers.isEmpty() -> {
                SupplierStateMessage(
                    title = "暂无供应商",
                    message = if (uiState.keyword.isBlank()) "当前账号没有可展示的真实供应商" else "没有匹配的真实供应商",
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 0.dp,
                        end = 16.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.suppliers, key = { it.id }) { supplier ->
                        SupplierArchiveCard(
                            supplier = supplier,
                            onClick = { onNavigateToDetail(supplier.id) }
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun SupplierArchiveCard(
    supplier: SupplierItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val payableAmount = parseCurrencyAmount(supplier.payable)
    val isStopped = supplier.status == "停用"
    val (contactName, contactPhone) = splitSupplierContact(supplier.contact)
    val statusColor = when {
        isStopped -> TextTertiary
        payableAmount > 0.0 -> WarningOrange
        else -> ZhihuijiPrimary
    }
    val payableColor = if (payableAmount > 0.0) DangerRed else TextPrimary
    val avatarColor = if (isStopped) SurfaceSoft else StatusBlueLight
    val avatarTextColor = if (isStopped) TextSecondary else ZhihuijiPrimary

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = archiveInitial(supplier.name, "供"),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = avatarTextColor
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = supplier.name,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "联系人与电话",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                StatusBadge(
                    text = supplier.status,
                    color = statusColor
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GlassBorderSoft)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = contactName,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = contactPhone,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "应付款项",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextTertiary
                    )
                    Text(
                        text = supplier.payable,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = payableColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun SupplierStateMessage(
    title: String,
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier,
        surfaceColor = GlassSurfaceLow,
        contentPadding = 16.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = "点击重试",
            modifier = Modifier.padding(top = 10.dp).clickable(onClick = onRetry),
            style = MaterialTheme.typography.labelLarge,
            color = ZhihuijiPrimary
        )
    }
}

private fun archiveInitial(value: String, fallback: String): String =
    value.trim().firstOrNull()?.toString() ?: fallback

private fun parseCurrencyAmount(value: String): Double =
    value.filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull() ?: 0.0

private fun splitSupplierContact(value: String): Pair<String, String> {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return "暂无联系人" to "暂无电话"
    val parts = trimmed.split(Regex("\\s+"), limit = 2)
    return when {
        parts.size == 2 -> parts[0] to parts[1]
        trimmed.any { it.isDigit() } -> "暂无联系人" to trimmed
        else -> trimmed to "暂无电话"
    }
}
