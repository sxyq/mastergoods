package com.zhihuiji.feature.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.*

@Composable
fun FinanceRecordListScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    scrollToTopSignal: Int = 0,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditorSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            showEditorSheet = false
            viewModel.clearCreateSuccess()
        }
    }

    BottomBarScrollVisibilityEffect(listState)
    BottomBarScrollToTopEffect(scrollToTopSignal, listState)

    Box(modifier = Modifier.fillMaxSize().then(if (showTopBar) Modifier.glassBackground() else Modifier)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTopBar) {
                GlassTopBar(title = "资金流水", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
            }
            SegmentedTabs(tabs = listOf("全部", "收入", "支出"), selectedIndex = when(uiState.filter.type) { 1 -> 1; 2 -> 2; else -> 0 }, onTabSelected = { viewModel.changeType(when(it) { 1 -> 1; 2 -> 2; else -> null }) })
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(title = "收入", value = MoneyFormatter.formatWithoutSymbol(uiState.totalIncome), tone = KpiTone.SUCCESS, modifier = Modifier.weight(1f))
                KpiCard(title = "支出", value = MoneyFormatter.formatWithoutSymbol(uiState.totalExpense), tone = KpiTone.DANGER, modifier = Modifier.weight(1f))
            }
            if (uiState.records.isEmpty()) {
                EmptyState(icon = Icons.Default.AccountBalance, title = "暂无流水", modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                ) {
                    items(uiState.records) { record ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(record.category, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                    Text(record.partnerName ?: "无往来单位", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                    Text(TimeFormatter.formatDateTime(record.createdAt), style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(MoneyFormatter.formatSigned(if (record.type == 1) record.amount else -record.amount), style = ZhihuijiTypography.titleSmall, color = if (record.type == 1) ZhihuijiColors.Success else ZhihuijiColors.Danger)
                                    StatusPill(text = StatusLabels.financeType(record.type), tone = if (record.type == 1) PillTone.SUCCESS else PillTone.DANGER)
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showEditorSheet = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = ZhihuijiColors.Primary) {
            Icon(Icons.Default.Add, null, tint = Color.White)
        }
    }

    if (showEditorSheet) {
        FinanceRecordEditorSheet(
            onConfirm = { type, category, amount, method, notes ->
                viewModel.createRecord(type, category, amount, method, notes)
            },
            onDismiss = { showEditorSheet = false },
        )
    }
}
