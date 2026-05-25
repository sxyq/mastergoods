package com.zhihuiji.feature.suppliers

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
fun SupplierDetailScreen(
    supplierId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit = {},
    viewModel: SupplierDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(supplierId) { viewModel.loadSupplier(supplierId) }

    Column(modifier = Modifier.fillMaxSize().glassBackground()) {
        GlassTopBar(title = "供应商详情", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        val supplier = uiState.supplier
        if (supplier != null) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(supplier.name, style = ZhihuijiTypography.headlineMedium)
                        Text("手机: ${supplier.phone}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                        if (!supplier.address.isNullOrBlank()) Text("地址: ${supplier.address}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                        StatusPill(text = StatusLabels.supplierStatus(supplier.status), tone = if (supplier.status == 1) PillTone.SUCCESS else PillTone.NEUTRAL)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(title = "应付余额", value = MoneyFormatter.formatWithoutSymbol(supplier.balance), tone = if (supplier.balance > 0) KpiTone.DANGER else KpiTone.SUCCESS, modifier = Modifier.weight(1f))
                    KpiCard(title = "状态", value = StatusLabels.supplierStatus(supplier.status), tone = if (supplier.status == 1) KpiTone.SUCCESS else KpiTone.WARNING, modifier = Modifier.weight(1f))
                }
                val notes = supplier.notes
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
                PrimaryGradientButton(text = "编辑", onClick = { onNavigateToEditor(supplier.id ?: supplierId) }, modifier = Modifier.fillMaxWidth())
            })
        }
    }
}
