package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.KpiCard
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.MainBottomBarHeight
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryDark
import com.zhihuiji.core.model.v2.agent.KpiCardItem
import com.zhihuiji.core.model.v2.agent.PendingDraftItem
import com.zhihuiji.core.model.v2.agent.RiskAlertItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentWorkbenchScreen(
    onNavigateToChat: (initialQuestion: String?) -> Unit,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgentWorkbenchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarSubtitle = when {
        uiState.isLoading -> "正在同步 Agent 入口"
        uiState.error != null -> "远端未同步 · 仅保留对话入口"
        uiState.isRemoteSynced -> "服务端已连接 · 等待提问"
        else -> "等待远端 Agent 状态"
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "AI 助手",
                subtitle = topBarSubtitle,
                actions = {
                    Row {
                        IconButton(onClick = onNavigateToTasks) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "任务与通知",
                                tint = TextSecondary
                            )
                        }
                        IconButton(onClick = { onNavigateToChat(null) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "新建对话",
                                tint = ZhihuijiPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 96.dp,
                end = 16.dp,
                bottom = MainBottomBarHeight + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                AgentEntryHero(
                    greeting = uiState.greeting,
                    isLoading = uiState.isLoading,
                    isRemoteSynced = uiState.isRemoteSynced,
                    error = uiState.error,
                    onStartChat = { onNavigateToChat(null) },
                )
            }

            // 今日经营简报
            uiState.todaySummary?.takeIf { it.isNotBlank() }?.let { summary ->
                item {
                    TodaySummaryCard(summary = summary)
                }
            }

            // KPI 卡片网格
            if (uiState.kpiCards.isNotEmpty()) {
                item {
                    WorkbenchSectionTitle(text = "今日经营指标")
                }
                val rows = uiState.kpiCards.chunked(2)
                rows.forEach { rowCards ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowCards.forEach { card ->
                                KpiCard(
                                    title = card.label,
                                    value = card.value,
                                    changePercent = card.trendValue,
                                    isPositive = card.trendDirection?.let { it.equals("up", true) || it == "+" } ?: true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // 奇数个时补齐占位，避免单个卡片拉伸
                            if (rowCards.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 快捷问题 chips
            if (uiState.quickQuestions.isNotEmpty()) {
                item {
                    WorkbenchSectionTitle(text = "快捷提问")
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.quickQuestions.forEach { question ->
                            QuickQuestionChip(
                                text = question,
                                onClick = { onNavigateToChat(question) },
                            )
                        }
                    }
                }
            }

            // 待确认草稿
            if (uiState.pendingDrafts.isNotEmpty()) {
                item {
                    WorkbenchSectionTitle(text = "待确认草稿")
                }
                items(uiState.pendingDrafts) { draft ->
                    PendingDraftRow(draft = draft)
                }
            }

            // 风险提醒
            if (uiState.riskAlerts.isNotEmpty()) {
                item {
                    WorkbenchSectionTitle(text = "风险提醒")
                }
                items(uiState.riskAlerts) { alert ->
                    RiskAlertRow(alert = alert)
                }
            }
        }
    }
}

@Composable
private fun WorkbenchSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 2.dp, top = 4.dp),
    )
}

@Composable
private fun TodaySummaryCard(
    summary: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        surfaceColor = SurfaceWhite.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = ZhihuijiPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "今日经营简报",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZhihuijiPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun QuickQuestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ZhihuijiPrimary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = ZhihuijiPrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ZhihuijiPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PendingDraftRow(
    draft: PendingDraftItem,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = SurfaceWhite.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WarningOrange.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Drafts,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = draft.title.ifBlank { "未命名草稿" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DraftMetaTag(text = draftTypeLabel(draft.draftType))
                    val time = TimeFormatter.formatDate(draft.createdAt)
                    if (time.isNotBlank() && time != "-") {
                        DraftMetaTag(text = time)
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftMetaTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TextTertiary.copy(alpha = 0.10f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun draftTypeLabel(type: String): String = when (type.lowercase()) {
    "sale_order" -> "销售单草稿"
    "purchase_order" -> "采购单草稿"
    "pay_order" -> "付款单草稿"
    "finance_record" -> "资金流水草稿"
    "product" -> "商品草稿"
    else -> type.replace('_', ' ')
}

@Composable
private fun RiskAlertRow(
    alert: RiskAlertItem,
    modifier: Modifier = Modifier,
) {
    val tone = when (alert.level.lowercase()) {
        "high" -> DangerRed
        "medium" -> WarningOrange
        else -> ZhihuijiPrimary
    }
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        surfaceColor = tone.copy(alpha = 0.06f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tone.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (alert.description.isNotBlank()) {
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentEntryHero(
    greeting: String,
    isLoading: Boolean,
    isRemoteSynced: Boolean,
    error: String?,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onStartChat,
        shape = RoundedCornerShape(28.dp),
        surfaceColor = ZhihuijiPrimary.copy(alpha = 0.18f),
        contentPadding = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(AgentHeroBrush)
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = SurfaceWhite,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = SurfaceWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (isRemoteSynced) {
                                "主屏保持干净。发送问题后由服务端创建真实 run，返回工具事件、Markdown 回复和结构化结果；无法连接时会明确提示失败。"
                            } else {
                                "主屏保持干净。当前仅保留对话入口；发送问题后会连接服务端，若远端不可用会明确提示失败原因。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SurfaceWhite.copy(alpha = 0.78f),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "开始一次真实 Agent 对话",
                        style = MaterialTheme.typography.labelLarge,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                AgentWorkbenchSyncStatus(
                    isLoading = isLoading,
                    isRemoteSynced = isRemoteSynced,
                    error = error,
                )
            }
        }
    }
}

@Composable
private fun AgentWorkbenchSyncStatus(
    isLoading: Boolean,
    isRemoteSynced: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val text = when {
        isLoading -> "正在同步 Agent 入口"
        error != null -> "远端工作台未同步，仅保留对话入口"
        isRemoteSynced -> "已同步远端 Agent 状态"
        else -> "等待远端 Agent 状态"
    }
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = SurfaceWhite.copy(alpha = if (error == null) 0.76f else 0.88f),
        fontWeight = FontWeight.SemiBold,
    )
}

private val AgentHeroBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF083C4A),
        ZhihuijiPrimaryDark,
        Color(0xFF0EA5A4),
    )
)
