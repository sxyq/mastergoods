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
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    val suggestions = remember {
        listOf(
            "本月销售趋势前十的商品有哪些？",
            "最近30天未下单的客户有哪些？",
        )
    }

    BottomBarScrollVisibilityEffect(scrollState)
    BottomBarScrollToTopEffect(reselectSignal, scrollState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (showTopBar) Modifier else Modifier)
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (showTopBar) {
            GlassTopBar(
                title = "AI 助手",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("智慧记", style = ZhihuijiTypography.headlineMedium, color = ZhihuijiColors.TextPrimary)
                    Text("AI工作台", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    TopIconAction(icon = Icons.Default.History, onClick = { onNavigateToTasks(0) })
                    TopIconAction(icon = Icons.Default.CenterFocusStrong, onClick = { onNavigateToDrafts() })
                }
            }
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
                    Text("下午好！我是智慧记AI助手", style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                    Text("为你提供经营分析、单据生成与智慧建议", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    Text(
                        "立即发起问答，查看趋势分析、草稿建议与任务进度",
                        style = ZhihuijiTypography.labelMedium,
                        color = ZhihuijiColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            AgentKpiMiniCard(
                title = workbench?.kpis?.getOrNull(0)?.label ?: "今日销售(元)",
                value = workbench?.kpis?.getOrNull(0)?.value ?: "12,840",
                sub = workbench?.kpis?.getOrNull(0)?.trend ?: "较昨日 ↑ 18.6%",
                icon = Icons.Default.AutoGraph,
                iconTint = ZhihuijiColors.Primary,
            )
            AgentKpiMiniCard(
                title = workbench?.kpis?.getOrNull(1)?.label ?: "待收款(元)",
                value = workbench?.kpis?.getOrNull(1)?.value ?: "3,260",
                sub = workbench?.kpis?.getOrNull(1)?.trend ?: "共5笔",
                icon = Icons.Default.Wallet,
                iconTint = ZhihuijiColors.Warning,
            )
            AgentKpiMiniCard(
                title = workbench?.kpis?.getOrNull(2)?.label ?: "待付款(元)",
                value = workbench?.kpis?.getOrNull(2)?.value ?: "6,540",
                sub = workbench?.kpis?.getOrNull(2)?.trend ?: "共4笔",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconTint = ZhihuijiColors.Warning,
            )
            AgentKpiMiniCard(
                title = workbench?.kpis?.getOrNull(3)?.label ?: "低库存预警",
                value = workbench?.kpis?.getOrNull(3)?.value ?: "8",
                sub = workbench?.kpis?.getOrNull(3)?.trend ?: "待补货商品",
                icon = Icons.Default.WarningAmber,
                iconTint = ZhihuijiColors.Warning,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "经营洞察", actionText = "更多", onAction = { onNavigateToChat("帮我分析一下最近7天的销售情况") })
                AgentInsightItem(
                    icon = Icons.Default.AutoGraph,
                    color = ZhihuijiColors.Success,
                    title = workbench?.insights?.getOrNull(0)?.title ?: "销售趋势良好",
                    body = workbench?.insights?.getOrNull(0)?.content ?: "今日销售较昨日增长 18.6%，主要增长来自日用品类商品。",
                )
                AgentInsightItem(
                    icon = Icons.Default.NotificationsActive,
                    color = ZhihuijiColors.Warning,
                    title = workbench?.insights?.getOrNull(1)?.title ?: "回款提醒",
                    body = workbench?.insights?.getOrNull(1)?.content ?: "有 3 笔应收款即将超期，请及时跟进客户。",
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "快捷操作", actionText = "刷新", onAction = { viewModel.loadWorkbench() })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AgentQuickAction(Icons.AutoMirrored.Filled.ReceiptLong, "生成销售单") { onNavigateToDrafts() }
                    AgentQuickAction(Icons.Default.Inventory2, "生成采购单") { onNavigateToDrafts() }
                    AgentQuickAction(Icons.Default.Groups, "应收跟进") { onNavigateToTasks(1) }
                    AgentQuickAction(Icons.Default.WarningAmber, "库存预警") { onNavigateToChat("哪些商品需要紧急补货？") }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "大家都在问", actionText = "换一批", onAction = {})
                suggestions.forEach { question ->
                    QuestionRow(question = question, onClick = { onNavigateToChat(question) })
                }
            }
        }

        Spacer(modifier = Modifier.height(88.dp))
    }
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
