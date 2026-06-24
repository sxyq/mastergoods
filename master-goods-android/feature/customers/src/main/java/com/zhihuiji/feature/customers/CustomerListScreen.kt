package com.zhihuiji.feature.customers

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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Person
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
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.common.MoneyFormatter

@Composable
fun CustomerListScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (Long) -> Unit = {},
    searchQuery: String = "",
    viewModel: CustomerListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(searchQuery) {
        if (searchQuery != uiState.keyword) {
            viewModel.search(searchQuery)
        }
    }

    CustomerListScreenContent(
        uiState = uiState,
        onNavigateToDetail = onNavigateToDetail,
        onRetry = { viewModel.loadCustomers() },
        modifier = modifier
    )
}

@Composable
private fun CustomerListScreenContent(
    uiState: CustomerListUiState,
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
                CustomerStateMessage(
                    title = "客户加载失败",
                    message = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            uiState.customers.isEmpty() -> {
                CustomerStateMessage(
                    title = "暂无客户",
                    message = if (uiState.keyword.isBlank()) "当前账号没有可展示的真实客户" else "没有匹配的真实客户",
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
                    item(key = "receivable-summary") {
                        CustomerReceivableSummary(customers = uiState.customers)
                    }
                    items(uiState.customers, key = { it.id }) { customer ->
                        CustomerArchiveCard(
                            customer = customer,
                            onClick = { onNavigateToDetail(customer.id) }
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun CustomerReceivableSummary(
    customers: List<CustomerItem>,
    modifier: Modifier = Modifier
) {
    val receivableTotal = remember(customers) {
        customers.sumOf { it.receivableAmount.coerceAtLeast(0.0) }
    }
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(12.dp),
        surfaceColor = GlassSurfaceLow,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "总应收欠款 (元)",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
                )
                Text(
                    text = formatCurrency(receivableTotal),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZhihuijiPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(StatusBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomerArchiveCard(
    customer: CustomerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDebt = customer.hasDebt
    val statusText = when {
        customer.status == "已停用" -> "已停用"
        hasDebt -> "有欠款"
        else -> "已结清"
    }
    val statusColor = when {
        customer.status == "已停用" -> TextTertiary
        hasDebt -> DangerRed
        else -> TextSecondary
    }
    val avatarColor = when {
        customer.status == "已停用" -> SurfaceSoft
        hasDebt -> ZhihuijiPrimary
        else -> StatusBlueLight
    }
    val avatarTextColor = when {
        customer.status == "已停用" -> TextSecondary
        hasDebt -> Color.White
        else -> ZhihuijiPrimary
    }

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
                            .clip(RoundedCornerShape(10.dp))
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = archiveInitial(customer.name, "客"),
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = avatarTextColor
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = customer.name,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = customer.name,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "|",
                                fontSize = 13.sp,
                                color = GlassBorderSoft
                            )
                            Icon(
                                imageVector = Icons.Outlined.Call,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = customer.phone.ifBlank { "暂无电话" },
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                StatusBadge(
                    text = statusText,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "欠款总额",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
                )
                Text(
                    text = customer.receivable,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasDebt) DangerRed else TextSecondary,
                    maxLines = 1
                )
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
private fun CustomerStateMessage(
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

private fun formatCurrency(value: Double): String = MoneyFormatter.format(value)
