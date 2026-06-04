package com.zhihuiji.feature.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
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
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val filteredAccounts = remember(uiState.accounts, query, selectedTab) {
        if (selectedTab == 2) {
            emptyList()
        } else {
            uiState.accounts.filter { account ->
                query.isBlank() || account.name.contains(query, ignoreCase = true) || account.code.contains(query, ignoreCase = true)
            }
        }
    }
    val filteredTransfers = remember(uiState.transfers, query, selectedTab) {
        if (selectedTab == 1) {
            emptyList()
        } else {
            uiState.transfers.filter { transfer ->
                query.isBlank() ||
                    transfer.transferNo.contains(query, ignoreCase = true) ||
                    transfer.fromAccountName.contains(query, ignoreCase = true) ||
                    transfer.toAccountName.contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            showEditorSheet = false
            viewModel.clearCreateSuccess()
        }
    }

    BottomBarScrollVisibilityEffect(listState)
    BottomBarScrollToTopEffect(scrollToTopSignal, listState)

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showTopBar) {
                    GlassTopBar(title = "财务", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
                }
                SearchFilterBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "搜索账户/转账单号",
                    filterIcon = Icons.Default.Tune,
                    onFilterClick = {
                        selectedTab = when (selectedTab) {
                            0 -> 1
                            1 -> 2
                            else -> 0
                        }
                    },
                )
                SegmentedTabs(
                    tabs = listOf("全部", "账户", "转账"),
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(title = "账户余额", value = MoneyFormatter.formatWithoutSymbol(uiState.totalBalance), tone = KpiTone.PRIMARY, modifier = Modifier.weight(1f))
                    KpiCard(title = "账户数", value = "${uiState.accounts.size}", tone = KpiTone.SUCCESS, modifier = Modifier.weight(1f))
                }
                if (filteredAccounts.isEmpty() && filteredTransfers.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.AccountBalance,
                        title = if (query.isBlank()) "暂无财务数据" else "没有匹配的财务数据",
                        modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                    ) {
                        if (filteredAccounts.isNotEmpty()) {
                            item {
                                Text("账户", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                            }
                            items(filteredAccounts) { account ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(account.name, style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                            Text(account.code, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                        }
                                        Text(MoneyFormatter.format(account.balance), style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.Primary)
                                    }
                                }
                            }
                        }
                        if (filteredTransfers.isNotEmpty()) {
                            item {
                                Text("转账记录", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                            }
                            items(filteredTransfers) { transfer ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp), tint = ZhihuijiColors.TextSecondary)
                                                Text("${transfer.fromAccountName} → ${transfer.toAccountName}", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                            }
                                            Text(transfer.transferNo, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                            Text(TimeFormatter.formatDateTime(transfer.createdAt), style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                                        }
                                        Text(MoneyFormatter.format(transfer.amount), style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            FloatingPrimaryActionButton(
                text = "新增账户",
                icon = Icons.Default.Add,
                onClick = { showEditorSheet = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    if (showEditorSheet) {
        FinanceRecordEditorSheet(
            onConfirm = { code, name, type, balance, notes ->
                viewModel.createAccount(code, name, type, balance, notes)
            },
            onDismiss = { showEditorSheet = false },
        )
    }
}
