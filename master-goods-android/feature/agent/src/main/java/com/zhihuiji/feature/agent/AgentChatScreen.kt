package com.zhihuiji.feature.agent

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.MessageRole
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.ToolCallStatus

private val AgentChatHorizontalPadding = 16.dp
private val AgentChatTopPadding = 16.dp
private val AgentChatBottomInputClearance = 116.dp

@Composable
fun AgentChatScreen(
    initialQuestion: String? = null,
    conversationId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgentChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lastMessageId = uiState.messages.lastOrNull()?.id
    val lastMessageContentLength = uiState.messages.lastOrNull()?.content?.length ?: 0
    val streamingScrollBucket = if (uiState.isStreaming) lastMessageContentLength / 80 else 0

    // 新消息进入时使用动画；流式增量只做轻量贴底，避免每个 token 排队滚动动画。
    LaunchedEffect(uiState.messages.size, lastMessageId) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.isStreaming, streamingScrollBucket) {
        if (uiState.isStreaming && uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    // 如果有初始问题，自动发送
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    LaunchedEffect(initialQuestion) {
        if (!initialQuestion.isNullOrBlank() && uiState.messages.isEmpty()) {
            viewModel.onInputChange(initialQuestion)
            viewModel.sendMessage()
        }
    }

    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "AI 对话",
                subtitle = if (uiState.isStreaming) "等待服务端事件" else "服务端问答与结果块",
                onNavigationClick = onNavigateBack,
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearMessages) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清空对话",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = uiState.inputText,
                isStreaming = uiState.isStreaming,
                canStop = uiState.canStop,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGeneration,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.messages.isEmpty() && !uiState.isLoading -> {
                    EmptyChatState()
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = AgentChatHorizontalPadding,
                            top = AgentChatTopPadding,
                            end = AgentChatHorizontalPadding,
                            bottom = AgentChatBottomInputClearance,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            ChatMessageItem(
                                message = message,
                                onToggleRunTrace = { viewModel.toggleRunTrace(message.id) },
                            )
                        }

                        val showStandaloneTyping = uiState.isStreaming &&
                            uiState.messages.lastOrNull { it.role == MessageRole.ASSISTANT }?.content.isNullOrBlank()
                        if (showStandaloneTyping) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    StreamWaitingIndicator()
                                }
                            }
                        }
                    }
                }
            }

            // 上下文压缩提示（浮动）
            uiState.contextCompacted?.let { compacted ->
                ContextCompactedBanner(
                    state = compacted,
                    onDismiss = viewModel::dismissContextCompacted,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // 草稿确认弹窗
    uiState.showDraftConfirm?.let { draft ->
        DraftConfirmDialog(
            draft = draft,
            onArchive = { viewModel.archiveDraftFromDialog(draft.draftId) },
            onDismiss = viewModel::dismissDraftConfirm,
        )
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onToggleRunTrace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AgentAssistantAvatarBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = if (isUser) {
                Modifier.widthIn(max = 318.dp)
            } else {
                Modifier.weight(1f)
            },
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (message.isError && message.content.isBlank() && message.blocks.isEmpty()) {
                AssistantErrorCard(message = message.errorMessage ?: "出错了")
            } else {
                // 文本气泡
                if (message.content.isNotBlank()) {
                    Text(
                        text = if (isUser) "我" else "智慧记 AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) TextSecondary else AgentAssistantAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            start = if (isUser) 0.dp else 4.dp,
                            end = if (isUser) 4.dp else 0.dp,
                            bottom = 4.dp,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp,
                                )
                            )
                            .background(
                                brush = if (isUser) AgentUserBubbleBrush else AgentAssistantBubbleBrush
                            )
                            .border(
                                width = if (isUser) 0.7.dp else 1.dp,
                                color = if (isUser) {
                                    Color.White.copy(alpha = 0.38f)
                                } else {
                                    AgentAssistantAccent.copy(alpha = 0.30f)
                                },
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp,
                                )
                            )
                            .padding(
                                horizontal = if (isUser) 13.dp else 15.dp,
                                vertical = if (isUser) 11.dp else 14.dp,
                            )
                    ) {
                        if (isUser) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistantMessageHeader(
                                    isStreaming = message.isStreaming,
                                    hasServerAnswerDelta = message.hasServerAnswerDelta,
                                    answerDeltaSource = message.answerDeltaSource,
                                    hasToolEvidence = message.runTrace?.toolCalls?.isNotEmpty() == true,
                                    hasAuditTrace = message.runTrace?.auditId != null || message.runTrace?.traceId != null,
                                    hasCompletedTool = message.runTrace?.toolCalls?.any {
                                        it.status == ToolCallStatus.COMPLETED
                                    } == true,
                                )
                                if (message.isStreaming && message.hasServerAnswerDelta) {
                                    StreamingPlainAnswerText(text = message.content)
                                } else {
                                    AgentMarkdownText(
                                        markdown = message.content,
                                        contentColor = TextPrimary,
                                    )
                                }
                                when {
                                    message.hasServerAnswerDelta -> {
                                        InlineStreamingStatus(message.answerDeltaSource.inlineStreamingLabel())
                                    }
                                    message.isStreaming -> InlineStreamingStatus("正在等待服务端工具或模型事件")
                                }
                            }
                        }
                    }
                }

                // 富结果块（仅助手消息）
                if (!isUser && message.blocks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistantResultBlockStack(blocks = message.blocks)
                }

                if (!isUser && message.isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistantErrorCard(
                        message = message.errorMessage ?: "部分结果接收失败，已保留当前可见内容"
                    )
                }

                // 过程轨迹（仅助手消息）
                val runTrace = message.runTrace
                if (!isUser && runTrace != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RunTracePanel(
                        runTrace = runTrace,
                        isExpanded = runTrace.isExpanded,
                        onToggleExpand = onToggleRunTrace,
                    )
                } else if (!isUser && message.isStreaming && message.content.isBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RealQueryStatusCard()
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(TextTertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "我",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AssistantErrorCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = DangerRed.copy(alpha = 0.08f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = DangerRed,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun AssistantMessageHeader(
    isStreaming: Boolean,
    hasServerAnswerDelta: Boolean,
    answerDeltaSource: String?,
    hasToolEvidence: Boolean,
    hasAuditTrace: Boolean,
    hasCompletedTool: Boolean,
    modifier: Modifier = Modifier,
) {
    val statusLabel = assistantHeaderStatusLabel(
        isStreaming = isStreaming,
        hasServerAnswerDelta = hasServerAnswerDelta,
        answerDeltaSource = answerDeltaSource,
        hasToolEvidence = hasToolEvidence,
        hasAuditTrace = hasAuditTrace,
    )
    val statusColor = when {
        answerDeltaSource == DeltaSourceModelStream -> AgentAssistantAccent
        answerDeltaSource == DeltaSourceRuleSummary -> WarningOrange
        isStreaming -> WarningOrange
        else -> AgentAssistantAccent
    }
    val provenanceLabel = assistantProvenanceLabel(
        hasCompletedTool = hasCompletedTool,
        hasToolEvidence = hasToolEvidence,
        answerDeltaSource = answerDeltaSource,
    )
    val reviewLabel = when {
        hasAuditTrace -> "有运行标识"
        hasToolEvidence -> "有工具记录"
        isStreaming -> "生成中"
        else -> "未展开"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(statusColor.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        AssistantHeaderBadge(
            text = provenanceLabel,
            color = statusColor,
        )
        AssistantHeaderBadge(
            text = reviewLabel,
            color = if (hasAuditTrace) ZhihuijiPrimary else TextTertiary,
        )
    }
}

@Composable
private fun AssistantHeaderBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun StreamingPlainAnswerText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary,
        modifier = modifier,
    )
}

@Composable
private fun AssistantResultBlockStack(
    blocks: List<ResultBlockDto>,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 34.dp,
        shape = RoundedCornerShape(24.dp),
        surfaceColor = Color(0xFFEAF4FF).copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(AgentResultHeaderColors))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.88f))
                )
                Text(
                    text = "结构化结果 · ${blocks.size} 个结果块",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "服务端结果块",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            blocks.forEach { block ->
                ResultBlockRenderer(block = block)
            }
        }
    }
}

@Composable
private fun InlineStreamingStatus(
    text: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "agent_inline_streaming")
    val cursorAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "agent_inline_streaming_alpha",
    )

    Row(
        modifier = modifier.alpha(cursorAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ZhihuijiPrimary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun RealQueryStatusCard(
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier,
        blurRadius = 24.dp,
        shape = RoundedCornerShape(18.dp),
        surfaceColor = AgentAssistantAccent.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                color = ZhihuijiPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = "正在等待服务端查询结果",
                    style = MaterialTheme.typography.labelLarge,
                    color = ZhihuijiPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "服务端会按当前账号权限选择可用工具；失败时会明确提示",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StreamWaitingIndicator(
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier,
        surfaceColor = Color.White.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        contentPadding = 12.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = ZhihuijiPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "等待服务端流式片段",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isStreaming: Boolean,
    canStop: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        blurRadius = 40.dp,
        shape = RoundedCornerShape(30.dp),
        surfaceColor = Color.White.copy(alpha = 0.84f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                label = null,
                placeholder = "输入经营问题，服务端会选择可用工具...",
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                enabled = !isStreaming,
            )

            if (canStop) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AgentStopButtonBrush)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "停止接收",
                        tint = Color.White,
                    )
                }
            } else {
                val canSend = inputText.isNotBlank() && !isStreaming
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (canSend) AgentSendButtonBrush else AgentDisabledSendBrush)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AgentAssistantAvatarBrush),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "从一个问题开始",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "发送问题后，服务端会基于当前账号权限选择可用工具，并返回 Markdown、表格或统计图。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmptyStatePill("服务端查询", ZhihuijiPrimary)
                EmptyStatePill("模型流", AgentAssistantAccent)
                EmptyStatePill("图表结果", WarningOrange)
            }
        }
    }
}

@Composable
private fun EmptyStatePill(
    text: String,
    accent: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ContextCompactedBanner(
    state: ContextCompactedState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(WarningOrange.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Compress,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "上下文已压缩（${state.compactedCount} 条），${state.summary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningOrange,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = WarningOrange,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DraftConfirmDialog(
    draft: DraftConfirmState,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("仅归档草稿") },
        text = {
            Column {
                Text("AI 已生成一份 ${draft.draftType} 草稿：")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ZhihuijiPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "当前版本还未接入业务执行接口。这里仅归档草稿，不会修改商品、单据或资金数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onArchive) {
                Text("仅归档", color = ZhihuijiPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private val AgentAssistantAccent = Color(0xFF0EA5A4)

private val AgentAssistantAvatarBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0EA5A4),
        Color(0xFF14B8A6),
        Color(0xFF38BDF8),
    )
)

private val AgentAssistantBubbleBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFFFF).copy(alpha = 0.98f),
        Color(0xFFF1FFFD).copy(alpha = 0.97f),
        Color(0xFFEAF8FF).copy(alpha = 0.96f),
    )
)

private val AgentUserBubbleBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0C4D96),
        ZhihuijiPrimary,
        Color(0xFF1A73E8),
    )
)

private val AgentSendButtonBrush = Brush.linearGradient(
    colors = listOf(
        ZhihuijiPrimary,
        Color(0xFF1A73E8),
    )
)

private val AgentStopButtonBrush = Brush.linearGradient(
    colors = listOf(
        DangerRed,
        Color(0xFFFF6B5E),
    )
)

private val AgentDisabledSendBrush = Brush.linearGradient(
    colors = listOf(
        TextTertiary.copy(alpha = 0.32f),
        TextTertiary.copy(alpha = 0.22f),
    )
)

private val AgentResultHeaderColors = listOf(
    Color(0xFF0C4D96),
    Color(0xFF0EA5A4),
    Color(0xFF38BDF8),
)
