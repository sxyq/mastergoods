package com.zhihuiji.feature.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.designsystem.*

@Composable
fun AgentWorkbenchScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    reselectSignal: Int = 0,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var queryText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val kpis = uiState.workbench?.kpis.orEmpty()
    val insights = uiState.workbench?.insights.orEmpty()

    BottomBarScrollVisibilityEffect(scrollState)
    BottomBarScrollToTopEffect(reselectSignal, scrollState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (showTopBar) Modifier.glassBackground() else Modifier)
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (showTopBar) {
            GlassTopBar(title = "AI 助手", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavigationClick = onNavigateBack)
        } else {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("智慧记", style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextPrimary)
                    Text("AI工作台", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                }
                Row {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(24.dp), tint = ZhihuijiColors.TextSecondary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(Icons.Default.CenterFocusStrong, null, modifier = Modifier.size(24.dp), tint = ZhihuijiColors.TextSecondary)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(46.dp), tint = ZhihuijiColors.Primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("下午好！我是智慧记AI助手", style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                    Text("为你提供经营分析、单据生成与风险建议", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(title = kpis.getOrNull(0)?.label ?: "今日销售(元)", value = kpis.getOrNull(0)?.value ?: "12.00", trend = kpis.getOrNull(0)?.trend ?: "较昨日 ↑ 18.6%", icon = Icons.Default.Inventory2, tone = KpiTone.PRIMARY, modifier = Modifier.weight(1f))
            KpiCard(title = kpis.getOrNull(1)?.label ?: "待收款(元)", value = kpis.getOrNull(1)?.value ?: "12.00", trend = kpis.getOrNull(1)?.trend ?: "共1笔", icon = Icons.Default.AccountBalanceWallet, tone = KpiTone.WARNING, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(title = kpis.getOrNull(2)?.label ?: "待付款(元)", value = kpis.getOrNull(2)?.value ?: "0.00", trend = kpis.getOrNull(2)?.trend ?: "共0笔", icon = Icons.Default.Payments, tone = KpiTone.SUCCESS, modifier = Modifier.weight(1f))
            KpiCard(title = kpis.getOrNull(3)?.label ?: "低库存预警", value = kpis.getOrNull(3)?.value ?: "0", trend = kpis.getOrNull(3)?.trend ?: "待补货商品", icon = Icons.Default.WarningAmber, tone = KpiTone.WARNING, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("经营洞察", style = ZhihuijiTypography.titleMedium)
                    Text("更多 ›", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.Primary)
                }
                AgentInsightRow(Icons.Default.CheckCircle, insights.getOrNull(0)?.title ?: "销售趋势良好", insights.getOrNull(0)?.content ?: "今日销售较昨日保持增长，主要增长来自日用品类商品。", ZhihuijiColors.Success)
                AgentInsightRow(Icons.Default.NotificationsActive, insights.getOrNull(1)?.title ?: "回款提醒", insights.getOrNull(1)?.content ?: "有客户存在待收款项，请及时跟进。", ZhihuijiColors.Warning)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("快捷操作", style = ZhihuijiTypography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AgentQuickAction(Icons.Default.ReceiptLong, "生成销售单")
                    AgentQuickAction(Icons.Default.Inventory2, "生成采购单")
                    AgentQuickAction(Icons.Default.Groups, "应收跟进")
                    AgentQuickAction(Icons.Default.WarningAmber, "库存预警")
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("大家都在问", style = ZhihuijiTypography.titleMedium)
                    Text("换一批", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.Primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryOutlineButton(text = "今天经营情况", onClick = { viewModel.ask("问问今天的经营情况") }, modifier = Modifier.weight(1f))
                    SecondaryOutlineButton(text = "应收账款分析", onClick = { viewModel.ask("帮我分析应收账款") }, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryOutlineButton(text = "商品卖得好?", onClick = { viewModel.ask("哪些商品卖得好？") }, modifier = Modifier.weight(1f))
                    SecondaryOutlineButton(text = "库存周转如何?", onClick = { viewModel.ask("库存周转情况如何？") }, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = queryText, onValueChange = { queryText = it }, modifier = Modifier.weight(1f), placeholder = { Text("问问今天的经营情况") }, shape = RoundedCornerShape(9.dp), singleLine = true)
                    PrimaryGradientButton(text = "发送", onClick = { if(queryText.isNotBlank()) { viewModel.ask(queryText); queryText = "" } }, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }

        if (uiState.answer != null) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("助手回复", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(uiState.answer!!.answer, style = ZhihuijiTypography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun AgentInsightRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: String, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = color)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
            Text(content, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
        }
    }
}

@Composable
private fun AgentQuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(icon, null, modifier = Modifier.size(28.dp), tint = ZhihuijiColors.Primary)
        Text(label, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
    }
}
