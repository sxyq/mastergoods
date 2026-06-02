package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentChatScreen(
    onNavigateBack: () -> Unit,
    initialQuestion: String? = null,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf(initialQuestion.orEmpty()) }
    val scrollState = rememberScrollState()
    val suggestions = remember {
        listOf(
            "哪些客户回款风险高？",
            "哪些商品需要补货？",
            "本月利润情况如何？",
        )
    }

    LaunchedEffect(initialQuestion) {
        if (!initialQuestion.isNullOrBlank() && uiState.chatMessages.none { it.role == ChatRole.USER && it.text == initialQuestion }) {
            viewModel.ask(initialQuestion)
            input = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        GlassTopBar(
            title = "AI问答",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onNavigateBack,
            actions = {
                Icon(
                    imageVector = Icons.Default.HeadsetMic,
                    contentDescription = null,
                    tint = ZhihuijiColors.TextSecondary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            },
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.chatMessages.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("向 AI 询问经营问题", style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        Text("你可以直接问销售趋势、回款风险、库存预警，助手会返回结构化分析结果。", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    }
                }
            }

            uiState.chatMessages.forEach { message ->
                if (message.role == ChatRole.USER) {
                    UserBubble(text = message.text, timestampLabel = message.timestampLabel)
                } else {
                    AssistantBubble(message = message)
                }
            }

            if (uiState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    BubbleAvatar(bot = true)
                    GlassCard {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ZhihuijiColors.Primary)
                            Text("正在分析中…", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    text = suggestion,
                    onClick = { viewModel.ask(suggestion) },
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入你的问题…") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )
                PrimaryButton(
                    text = "发送",
                    icon = Icons.AutoMirrored.Filled.Send,
                    onClick = {
                        val text = input.trim()
                        if (text.isNotBlank()) {
                            viewModel.ask(text)
                            input = ""
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun UserBubble(
    text: String,
    timestampLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(ZhihuijiColors.Primary.copy(alpha = 0.92f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(text, style = ZhihuijiTypography.bodyMedium, color = Color.White)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(timestampLabel, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        BubbleAvatar(bot = false)
    }
}

@Composable
private fun AssistantBubble(
    message: AgentChatMessage,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        BubbleAvatar(bot = true)
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(ZhihuijiColors.Primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = ZhihuijiColors.Primary, modifier = Modifier.size(14.dp))
                        }
                        Text("经营分析结果", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                    Text(message.text, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                    message.answer?.takeIf { it.highlights.isNotEmpty() || it.rows.isNotEmpty() || it.suggestedActions.isNotEmpty() }?.let {
                        StructuredAnswerCard(it)
                    }
                }
            }
            if (message.timestampLabel.isNotBlank()) {
                Text(message.timestampLabel, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun StructuredAnswerCard(answer: com.zhihuiji.core.model.AgentAnswerDto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SalesOverviewCard(answer = answer)

        if (answer.rows.isNotEmpty() && answer.columns.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("销售TOP3商品", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                        Text("销量 / 占比", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                    }
                    answer.rows.take(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(ZhihuijiColors.Primary.copy(alpha = 0.10f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${answer.rows.indexOf(row) + 1}", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.Primary, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text(row.getOrNull(0).orEmpty(), style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextPrimary)
                                    Text(
                                        row.drop(1).joinToString("  "),
                                        style = ZhihuijiTypography.labelSmall,
                                        color = ZhihuijiColors.TextSecondary,
                                    )
                                }
                            }
                            Text(
                                row.lastOrNull().orEmpty(),
                                style = ZhihuijiTypography.bodySmall,
                                color = ZhihuijiColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        if (answer.suggestedActions.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("建议行动", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                    answer.suggestedActions.forEachIndexed { index, action ->
                        Text("${index + 1}. $action", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesOverviewCard(
    answer: com.zhihuiji.core.model.AgentAnswerDto,
) {
    val headlineA = answer.highlights.getOrNull(0) ?: "销售金额稳步提升"
    val headlineB = answer.highlights.getOrNull(1) ?: "订单量较前周期增长"
    val chartPoints = listOf(0.42f, 0.36f, 0.58f, 0.46f, 0.63f, 0.56f)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("销售概览", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                Text("近7天", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AnswerMetricCard(
                    title = "销售金额(元)",
                    value = headlineA.filter { it.isDigit() || it == ',' }.ifBlank { "86,340" },
                    trend = "较前7天 ↑ 12.4%",
                    modifier = Modifier.weight(1f),
                )
                AnswerMetricCard(
                    title = "订单数(笔)",
                    value = headlineB.filter { it.isDigit() || it == ',' }.ifBlank { "238" },
                    trend = "较前7天 ↑ 8.2%",
                    modifier = Modifier.weight(1f),
                )
            }

            TrendChartCard(points = chartPoints)
        }
    }
}

@Composable
private fun AnswerMetricCard(
    title: String,
    value: String,
    trend: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
            Text(value, style = ZhihuijiTypography.titleLarge, color = ZhihuijiColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(trend, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.Success)
        }
    }
}

@Composable
private fun TrendChartCard(
    points: List<Float>,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.4f),
            ) {
                if (points.isEmpty()) return@Canvas
                val stepX = size.width / max(points.size - 1, 1)
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = stepX * index
                    val y = size.height * (1f - point.coerceIn(0.1f, 0.9f))
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(
                        color = ZhihuijiColors.Primary,
                        radius = 6f,
                        center = androidx.compose.ui.geometry.Offset(x, y),
                    )
                }
                drawPath(
                    path = path,
                    color = ZhihuijiColors.Primary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("05-10", "05-11", "05-12", "05-13", "05-14", "05-15").forEach {
                    Text(it, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun BubbleAvatar(bot: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (bot) ZhihuijiColors.Primary.copy(alpha = 0.12f) else ZhihuijiColors.Primary.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (bot) Icons.Default.SmartToy else Icons.Default.HeadsetMic,
            contentDescription = null,
            tint = if (bot) ZhihuijiColors.Primary else Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    GlassCard(onClick = onClick) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = ZhihuijiTypography.labelMedium,
            color = ZhihuijiColors.Primary,
        )
    }
}
