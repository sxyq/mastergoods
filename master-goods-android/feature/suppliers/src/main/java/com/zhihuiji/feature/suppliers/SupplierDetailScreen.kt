package com.zhihuiji.feature.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
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
fun SupplierDetailScreen(
    supplierId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit = {},
    viewModel: SupplierDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(supplierId) { viewModel.loadSupplier(supplierId) }

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = "供应商详情", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            val supplier = uiState.supplier
            if (supplier != null) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
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
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("档案信息", style = ZhihuijiTypography.titleMedium)
                            Text("联系人：${supplier.primaryContactName ?: supplier.name}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                            Text("联系电话：${supplier.primaryContactPhone ?: supplier.phone}", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
                            Text(
                                "档案扩展信息会在可用后继续补充，这里先展示当前已返回的基础资料。",
                                style = ZhihuijiTypography.bodySmall,
                                color = ZhihuijiColors.TextSecondary,
                            )
                        }
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
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("历史记录", style = ZhihuijiTypography.titleMedium)
                            EmptyState(
                                icon = Icons.Default.LocalShipping,
                                title = "暂无可展示记录",
                                subtitle = "合作跟进、对账、供货变化等记录会在可用后展示，这里先不补造历史事件。",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                BottomActionBar(primaryAction = {
                    PrimaryButton(text = "编辑", onClick = { onNavigateToEditor(supplier.id) }, modifier = Modifier.fillMaxWidth())
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
                        icon = Icons.Default.LocalShipping,
                        title = "暂无供应商详情",
                        subtitle = "供应商数据返回后将在这里展示",
                    )
                }
            }
        }
    }
}
