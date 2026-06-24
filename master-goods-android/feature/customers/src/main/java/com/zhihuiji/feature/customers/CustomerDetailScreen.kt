package com.zhihuiji.feature.customers

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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.MoneyFormatter
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
fun CustomerDetailScreen(
    customerId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    CustomerDetailScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = { onNavigateToEdit(customerId) },
        onDelete = { viewModel.deleteCustomer(customerId) },
        modifier = modifier
    )
}

@Composable
private fun CustomerDetailScreenContent(
    uiState: CustomerDetailUiState,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer = uiState.customer
    val balanceText = remember(customer?.balance) {
        customer?.let { MoneyFormatter.format(it.balance) }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "客户详情",
                subtitle = customer?.phone ?: "联系资料与应收余额",
                onNavigationClick = onNavigateBack,
                actions = {
                    customer?.let {
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
            customer?.let { currentCustomer ->
                BottomActionBar(
                    primaryText = "编辑资料",
                    onPrimaryClick = onNavigateToEdit,
                    totalLabel = "应收余额",
                    totalAmount = balanceText ?: MoneyFormatter.format(currentCustomer.balance),
                    totalAmountColor = if (currentCustomer.balance > 0.0) DangerRed else ZhihuijiPrimary
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

                customer != null -> {
                    val currentCustomer = customer
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
                                        text = currentCustomer.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary
                                    )
                                    val (statusText, statusType) = when (currentCustomer.status) {
                                        1 -> "正常" to StatusType.NORMAL
                                        0 -> "已停用" to StatusType.CANCELLED
                                        else -> "未知" to StatusType.PENDING
                                    }
                                    StatusPill(
                                        text = statusText,
                                        status = statusType
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "手机号: ${currentCustomer.phone}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        // 联系信息卡片
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "联系信息",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                InfoRow(label = "手机号", value = currentCustomer.phone)
                                InfoRow(label = "地址", value = currentCustomer.address ?: "-")
                            }
                        }

                        // 财务信息卡片
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "财务信息",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                InfoRow(
                                    label = "余额（应收款）",
                                    value = balanceText ?: MoneyFormatter.format(currentCustomer.balance)
                                )
                            }
                        }

                        // 备注卡片
                        if (!currentCustomer.remark.isNullOrBlank()) {
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "备注",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = currentCustomer.remark,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
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
