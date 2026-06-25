package com.zhihuiji.feature.sales

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

private val paymentMethods = listOf(
    0 to "现金",
    1 to "微信",
    2 to "支付宝",
    3 to "银行转账",
)

@Composable
fun PaymentScreen(
    onBackClick: () -> Unit,
    onPaySuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.paySuccess) {
        if (uiState.paySuccess) {
            viewModel.onPaySuccessHandled()
            onPaySuccess()
        }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "销售收款",
                subtitle = uiState.order?.orderNo ?: "确认金额、方式与流水备注",
                onNavigationClick = onBackClick
            )
        },
        bottomBar = {
            uiState.order?.let { order ->
                val pendingAmount = order.totalAmount - order.paidAmount
                BottomActionBar(
                    primaryText = if (uiState.isPaying) "正在收款..." else "确认收款",
                    onPrimaryClick = viewModel::pay,
                    primaryEnabled = !uiState.isPaying,
                    totalLabel = "待收金额",
                    totalAmount = MoneyFormatter.format(pendingAmount)
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

                uiState.error != null && uiState.order == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "出错了",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            IconButton(onClick = { viewModel.loadOrder() }) {
                                Text("重试", color = ZhihuijiPrimary)
                            }
                        }
                    }
                }

                uiState.order != null -> {
                    PaymentContent(
                        uiState = uiState,
                        onAmountChange = viewModel::updateAmount,
                        onPaymentMethodChange = viewModel::updatePaymentMethod,
                        onRemarkChange = viewModel::updateRemark,
                        onPay = viewModel::pay,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentContent(
    uiState: PaymentUiState,
    onAmountChange: (String) -> Unit,
    onPaymentMethodChange: (Int) -> Unit,
    onRemarkChange: (String) -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = uiState.order!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OrderInfoCard(order = order)

        HorizontalDivider()

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassTextField(
                    value = uiState.amount,
                    onValueChange = onAmountChange,
                    label = "收款金额",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = { Text("¥") }
                )

                Text(
                    text = "收款方式",
                    style = MaterialTheme.typography.titleMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paymentMethods.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (method, label) ->
                                PaymentMethodChip(
                                    label = label,
                                    selected = uiState.paymentMethod == method,
                                    onClick = { onPaymentMethodChange(method) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                GlassTextField(
                    value = uiState.remark,
                    onValueChange = onRemarkChange,
                    label = "备注 / 流水号",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier,
        onClick = onClick,
        surfaceColor = if (selected) GlassSurfaceHigh else GlassSurfaceLow,
        blurRadius = if (selected) 22.dp else 16.dp,
        contentPadding = 12.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) ZhihuijiPrimary else TextPrimary
        )
    }
}

@Composable
private fun OrderInfoCard(
    order: com.zhihuiji.core.model.v2.order.SaleOrderV2Dto,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "订单信息",
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            InfoRow(label = "订单号", value = order.orderNo)
            InfoRow(label = "客户名称", value = order.customerName ?: "-")
            InfoRow(label = "订单金额", value = MoneyFormatter.format(order.totalAmount))
            InfoRow(label = "已付金额", value = MoneyFormatter.format(order.paidAmount))
            InfoRow(
                label = "待付金额",
                value = MoneyFormatter.format(order.totalAmount - order.paidAmount)
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
