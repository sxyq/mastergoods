package com.zhihuiji.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DocumentListCard
import com.zhihuiji.core.designsystem.DocumentListBottomContentPadding
import com.zhihuiji.core.designsystem.DocumentListFabBottomPadding
import com.zhihuiji.core.designsystem.DocumentStatusTone
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun FinanceRecordListScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToDailyExpense: () -> Unit = {},
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val visibleRecords = uiState.records

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
                FinanceRecordStateMessage(
                    title = "资金流水加载失败",
                    message = uiState.error ?: "请稍后重试",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            visibleRecords.isEmpty() -> {
                FinanceRecordStateMessage(
                    title = "暂无资金流水",
                    message = "当前账号没有可展示的真实资金记录",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 12.dp,
                        end = 20.dp,
                        bottom = DocumentListBottomContentPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = visibleRecords,
                        key = { it.id }
                    ) { record ->
                        FinanceRecordListItem(
                            record = record,
                            onClick = { onNavigateToDetail(record.id) }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = DocumentListFabBottomPadding)
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ZhihuijiPrimary)
                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable(onClick = onNavigateToDailyExpense)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "记录支出",
                tint = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun FinanceRecordListItem(
    record: FinanceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = record.type == "收入"
    val isExpense = record.type == "支出"
    val amountColor = if (isIncome) SuccessGreen else if (isExpense) DangerRed else TextPrimary

    DocumentListCard(
        modifier = modifier,
        title = record.recordNo,
        subtitle = record.title.ifBlank { record.category },
        meta = when {
            record.account.isBlank() -> record.date
            record.date.isBlank() -> record.account
            else -> "${record.account} · ${record.date}"
        },
        amount = if (isExpense) "-${record.amount}" else record.amount,
        statusLabel = record.type,
        statusTone = if (isIncome) DocumentStatusTone.SUCCESS else if (isExpense) DocumentStatusTone.DANGER else DocumentStatusTone.NEUTRAL,
        amountColor = amountColor,
        onClick = onClick
    )
}

@Composable
private fun FinanceRecordStateMessage(
    title: String,
    message: String,
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
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
