package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BottomBarScrollToTopEffect
import com.zhihuiji.core.designsystem.BottomBarScrollVisibilityEffect
import com.zhihuiji.core.designsystem.EmptyState
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.PillTone
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentWorkbenchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String?) -> Unit = {},
    onNavigateToDrafts: () -> Unit = {},
    onNavigateToTasks: (Int) -> Unit = {},
    showTopBar: Boolean = true,
    reselectSignal: Int = 0,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val workbench = uiState.workbench
    val scrollState = rememberScrollState()
    val quickQuestions = remember {
        listOf(
            "最近有哪些客户需要我优先跟进？",
            "帮我整理今天待处理的经营风险。",
        )
    }

    BottomBarScrollVisibilityEffect(scrollState)
    BottomBarScrollToTopEffect(reselectSignal, scrollState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
            if (showTopBar) {
                GlassTopBar(
                    title = "AI 助手",
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onNavigateBack,
                )
            } else {
                GlassTopBar(
                    title = "AI 助手",
                    actions = {
                        TopIconAction(icon = Icons.Default.History, onClick = { onNavigateToTasks(0) })
                        TopIconAction(icon = Icons.Default.CenterFocusStrong, onClick = { onNavigateToDrafts() })
                    },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToChat(null) }) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(ZhihuijiColors.Primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(30.dp), tint = ZhihuijiColors.Primary)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(agentGreetingText(), style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        Text("为你提供经营分析、单据生成与智慧建议", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        Text(
                            if (workbench == null) "当前可先使用问答与草稿能力，工作台总览会在这里逐步展示" else "立即发起问答，查看趋势分析、草稿建议与任务进度",
                            style = ZhihuijiTypography.labelMedium,
                            color = ZhihuijiColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (workbench == null) {
                AgentUnavailableCard(
                    title = "工作台总览暂未显示",
                    body = "当前可先使用问答与草稿能力。经营总览、洞察和任务摘要会在可用后显示，这里先不展示未确认的经营数字。",
                    actionText = "先去问答",
                    onAction = { onNavigateToChat("帮我总结最近7天的经营风险") },
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    workbench.kpis.take(4).forEachIndexed { index, kpi ->
                        AgentKpiMiniCard(
                            title = kpi.label,
                            value = kpi.value,
                            sub = kpi.trend ?: "",
                            icon = when (index) {
                                0 -> Icons.Default.AutoGraph
                                1 -> Icons.Default.Wallet
                                2 -> Icons.AutoMirrored.Filled.ReceiptLong
                                else -> Icons.Default.WarningAmber
                            },
                            iconTint = when (index) {
                                0 -> ZhihuijiColors.Primary
                                1 -> ZhihuijiColors.Warning
                                2 -> ZhihuijiColors.Warning
                                else -> ZhihuijiColors.Warning
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "经营洞察", actionText = "更多", onAction = { onNavigateToChat("帮我分析一下最近7天的销售情况") })
                    if (workbench?.insights?.isNotEmpty() == true) {
                        workbench.insights.take(2).forEachIndexed { index, insight ->
                            AgentInsightItem(
                                icon = if (index == 0) Icons.Default.AutoGraph else Icons.Default.NotificationsActive,
                                color = if (index == 0) ZhihuijiColors.Success else ZhihuijiColors.Warning,
                                title = insight.title,
                                body = insight.content,
                            )
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Default.NotificationsActive,
                            title = "经营洞察暂未显示",
                            subtitle = "可以先通过 AI 问答获取趋势分析；这里会在可用后展示卡片化洞察。",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "快捷操作", actionText = "刷新", onAction = { viewModel.loadWorkbench() })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AgentQuickAction(Icons.AutoMirrored.Filled.ReceiptLong, "生成销售单") { onNavigateToDrafts() }
                        AgentQuickAction(Icons.Default.Inventory2, "生成采购单") { onNavigateToDrafts() }
                        AgentQuickAction(Icons.Default.Groups, "任务中心") { onNavigateToTasks(0) }
                        AgentQuickAction(Icons.Default.WarningAmber, "库存预警") { onNavigateToChat("哪些商品需要紧急补货？") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "本地快捷提问", actionText = "换一批", onAction = {})
                    quickQuestions.forEach { question ->
                        QuestionRow(question = question, onClick = { onNavigateToChat(question) })
                    }
                }
            }

        Spacer(modifier = Modifier.height(88.dp))
    }
}

private fun agentGreetingText(calendar: Calendar = Calendar.getInstance()): String {
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..5 -> "凌晨好"
        in 6..11 -> "早上好"
        in 12..17 -> "下午好"
        else -> "晚上好"
    }
    return "$greeting！我是智慧记AI助手"
}

@Composable
private fun TopIconAction(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(Color.White.copy(alpha = 0.52f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ZhihuijiColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AgentKpiMiniCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    iconTint: Color,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.TextSecondary)
                Text(value, style = ZhihuijiTypography.headlineSmall, color = ZhihuijiColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    sub,
                    style = ZhihuijiTypography.bodySmall,
                    color = if (sub.contains("待") || sub.contains("共")) ZhihuijiColors.TextSecondary else ZhihuijiColors.Success,
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AgentUnavailableCard(
    title: String,
    body: String,
    actionText: String,
    onAction: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                StatusPill(text = "稍后可用", tone = PillTone.INFO)
            }
            Text(body, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
            SecondaryOutlineButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
        Text(
            actionText,
            style = ZhihuijiTypography.labelMedium,
            color = ZhihuijiColors.Primary,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

@Composable
private fun AgentInsightItem(
    icon: ImageVector,
    color: Color,
    title: String,
    body: String,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(body, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun AgentQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (label.contains("库存")) ZhihuijiColors.Warning.copy(alpha = 0.12f) else ZhihuijiColors.Primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = if (label.contains("库存")) ZhihuijiColors.Warning else ZhihuijiColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(label, style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.TextSecondary)
    }
}

@Composable
private fun QuestionRow(
    question: String,
    onClick: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(question, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                Text("点击后自动带入 AI 问答页", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Default.Refresh, contentDescription = null, tint = ZhihuijiColors.Primary, modifier = Modifier.size(16.dp))
        }
    }
}
