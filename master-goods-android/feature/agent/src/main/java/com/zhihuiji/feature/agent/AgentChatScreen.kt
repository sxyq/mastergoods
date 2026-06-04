package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
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
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentChatScreen(
    onNavigateBack: () -> Unit,
    initialQuestion: String? = null,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // TODO: v2迁移 - 问答流程已改为 createConversation → createMessage(role=user) → poll messages
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

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            GlassTopBar(
                title = "AI问答",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.chatMessages.isEmpty()) {
                    WelcomeHeroCard()
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
}

@Composable
private fun WelcomeHeroCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BubbleAvatar(bot = true)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text("你好，我是智慧记 AI 助手", style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                Text("可以先从销售趋势、回款风险、库存预警这些经营问题开始提问。", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                Text("回答会基于当前已接入的数据能力生成；没有接通的能力会明确说明，不伪造成已完成分析。", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.Primary)
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
        if (answer.highlights.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("重点摘要", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                    answer.highlights.forEachIndexed { index, highlight ->
                        Text(
                            text = "${index + 1}. $highlight",
                            style = ZhihuijiTypography.bodySmall,
                            color = ZhihuijiColors.TextSecondary,
                        )
                    }
                }
            }
        }

        if (answer.rows.isNotEmpty() && answer.columns.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("结构化结果", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                        Text("按返回字段展示", style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                    }
                    answer.rows.take(3).forEachIndexed { rowIndex, row ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "结果 ${rowIndex + 1}",
                                    style = ZhihuijiTypography.labelMedium,
                                    color = ZhihuijiColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                answer.columns.forEachIndexed { columnIndex, column ->
                                    AnswerFieldRow(
                                        label = column,
                                        value = row.getOrNull(columnIndex).orEmpty().ifBlank { "-" },
                                    )
                                }
                            }
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
private fun AnswerFieldRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
        Text(value, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextPrimary, fontWeight = FontWeight.Medium)
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
