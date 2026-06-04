package com.zhihuiji.feature.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.*

@Composable
fun CustomerDetailScreen(
    customerId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit = {},
    viewModel: CustomerDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) { viewModel.loadCustomer(customerId) }

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = "客户详情", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            val customer = uiState.customer
            if (customer != null) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(customer.name, style = ZhihuijiTypography.headlineMedium)
                            Text("手机: ${customer.phone}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                            if (!customer.address.isNullOrBlank()) Text("地址: ${customer.address}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                            StatusPill(
                                text = StatusLabels.customerListStatus(customer.status, customer.balance),
                                tone = when {
                                    customer.status == StatusLabels.Codes.CUSTOMER_STATUS_DISABLED -> PillTone.NEUTRAL
                                    customer.balance > 0.0 -> PillTone.DANGER
                                    else -> PillTone.SUCCESS
                                },
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(title = "应收余额", value = MoneyFormatter.formatWithoutSymbol(customer.balance), tone = if (customer.balance > 0) KpiTone.DANGER else KpiTone.SUCCESS, modifier = Modifier.weight(1f))
                        KpiCard(title = "等级", value = StatusLabels.customerLevel(customer.level), tone = KpiTone.PRIMARY, modifier = Modifier.weight(1f))
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("档案信息", style = ZhihuijiTypography.titleMedium)
                            Text("联系人：${customer.primaryContactName ?: customer.name}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                            Text("联系电话：${customer.primaryContactPhone ?: customer.phone}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                            Text(
                                "档案扩展信息会在可用后继续补充，这里先展示当前已返回的基础资料。",
                                style = ZhihuijiTypography.bodySmall,
                                color = ZhihuijiColors.TextSecondary,
                            )
                        }
                    }
                    val notes = customer.notes
                    if (!notes.isNullOrBlank()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("备注", style = ZhihuijiTypography.titleMedium)
                                Text(notes, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                            }
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("跟进记录", style = ZhihuijiTypography.titleMedium)
                            EmptyState(
                                icon = Icons.Default.People,
                                title = "暂无可展示记录",
                                subtitle = "拜访、沟通、回款等记录会在可用后展示，这里先不补造历史事件。",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                BottomActionBar(primaryAction = {
                    PrimaryButton(text = "编辑", onClick = { onNavigateToEditor(customer.id) }, modifier = Modifier.fillMaxWidth())
                })
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Default.People,
                        title = "暂无客户详情",
                        subtitle = "客户数据返回后将在这里展示",
                    )
                }
            }
        }
    }
}
