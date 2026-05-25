package com.zhihuiji.feature.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

    Column(modifier = Modifier.fillMaxSize().glassBackground()) {
        GlassTopBar(title = "客户详情", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        val customer = uiState.customer
        if (customer != null) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(customer.name, style = ZhihuijiTypography.headlineMedium)
                        Text("手机: ${customer.phone}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                        if (!customer.address.isNullOrBlank()) Text("地址: ${customer.address}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                        StatusPill(text = StatusLabels.supplierStatus(customer.status), tone = if (customer.status == 1) PillTone.SUCCESS else PillTone.NEUTRAL)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(title = "应收余额", value = MoneyFormatter.formatWithoutSymbol(customer.balance), tone = if (customer.balance > 0) KpiTone.DANGER else KpiTone.SUCCESS, modifier = Modifier.weight(1f))
                    KpiCard(title = "等级", value = StatusLabels.customerLevel(customer.level), tone = KpiTone.PRIMARY, modifier = Modifier.weight(1f))
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
            }
            BottomActionBar(primaryAction = {
                PrimaryGradientButton(text = "编辑", onClick = { onNavigateToEditor(customer.id ?: customerId) }, modifier = Modifier.fillMaxWidth())
            })
        }
    }
}
