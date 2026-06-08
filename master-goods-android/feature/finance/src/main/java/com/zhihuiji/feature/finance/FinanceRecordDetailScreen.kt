package com.zhihuiji.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.AmountTextStyle
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

@Composable
fun FinanceRecordDetailScreen(
    recordId: Long,
    onNavigateBack: () -> Unit,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val record = uiState.records.firstOrNull { it.id == recordId }

    GlassScaffold(
        topBar = {
            GlassTopBar(
                title = "资金流水详情",
                subtitle = record?.recordNo ?: "读取当前账号真实流水",
                onNavigationClick = onNavigateBack
            )
        },
        bottomBar = {
            BottomActionBar(
                primaryText = "返回列表",
                onPrimaryClick = onNavigateBack,
                totalLabel = "流水金额",
                totalAmount = record?.amount,
                totalAmountColor = record?.amountColor()
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && record == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZhihuijiPrimary)
                }
            }

            uiState.error != null && record == null -> {
                DetailEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "资金流水加载失败",
                    message = uiState.error ?: "请稍后重试"
                )
            }

            record == null -> {
                DetailEmptyState(
                    modifier = Modifier.padding(paddingValues),
                    title = "未找到这条资金流水",
                    message = "当前账号下没有匹配的真实流水记录，未展示任何模拟数据。"
                )
            }

            else -> {
                FinanceRecordDetailContent(
                    record = record,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun FinanceRecordDetailContent(
    record: FinanceItem,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                blurRadius = 24.dp,
                surfaceColor = GlassSurfaceHigh,
                contentPadding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.type,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = record.amount,
                            style = AmountTextStyle,
                            color = record.amountColor()
                        )
                    }
                    StatusPill(
                        text = record.type,
                        status = if (record.type == "收入") StatusType.NORMAL else StatusType.OUT_OF_STOCK
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "编号 ${record.recordNo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }

        item {
            DetailSection(title = "基础信息") {
                DetailRow(label = "分类", value = record.category)
                DetailRow(label = "账户/方式", value = record.account)
                DetailRow(label = "往来对象", value = record.partnerName?.ifBlank { null } ?: "未记录")
                DetailRow(label = "发生日期", value = record.date)
                DetailRow(label = "更新日期", value = record.updatedDate)
            }
        }

        item {
            DetailSection(title = "备注") {
                Text(
                    text = record.notes?.takeIf { it.isNotBlank() } ?: "暂无备注",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (record.notes.isNullOrBlank()) TextTertiary else TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceColor = GlassSurfaceHigh,
        contentPadding = 16.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            surfaceColor = GlassSurfaceHigh
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun FinanceItem.amountColor() =
    if (type == "收入") SuccessGreen else DangerRed
