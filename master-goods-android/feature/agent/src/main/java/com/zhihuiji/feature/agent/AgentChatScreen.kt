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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.MessageRole
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus

private val AgentChatHorizontalPadding = 16.dp
private val AgentChatTopPadding = 16.dp
private val AgentChatBottomInputClearance = 116.dp
private const val CompletedToolPillVisibleMs = 1_200L
private const val AgentChatAutoFollowBottomThresholdItems = 1

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
    val activeStreamingMessage = uiState.messages.lastOrNull { message ->
        message.role == MessageRole.ASSISTANT && message.isStreaming
    }
    val activeStreamingMessageId = activeStreamingMessage?.id
    val streamingScrollBucket = if (uiState.isStreaming && activeStreamingMessage != null) {
        activeStreamingMessage.streamingAutoFollowBucket()
    } else {
        0
    }
    // 新消息进入时使用动画；流式增量只做轻量贴底，避免每个 token 排队滚动动画。
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.id) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.isStreaming, activeStreamingMessageId, streamingScrollBucket) {
        if (
            uiState.isStreaming &&
            activeStreamingMessageId != null &&
            uiState.messages.lastOrNull()?.id == activeStreamingMessageId &&
            listState.shouldAutoFollowStreamingContent(uiState.messages.size)
        ) {
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(conversationId, initialQuestion) {
        viewModel.startConversation(
            conversationId = conversationId,
            initialQuestion = initialQuestion,
        )
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
                subtitle = if (uiState.isStreaming) "正在分析真实业务数据" else "真实问答与结果块",
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
                            key = { it.id },
                            contentType = { message -> "message-${message.role.name.lowercase()}" },
                        ) { message ->
                            ChatMessageItem(
                                message = message,
                                allowActiveAnimations = message.id == activeStreamingMessageId,
                                onToggleRunTrace = { viewModel.toggleRunTrace(message.id) },
                            )
                        }

                        val showStandaloneTyping = uiState.messages
                            .lastOrNull { it.role == MessageRole.ASSISTANT }
                            .shouldShowStandaloneTypingIndicator(uiState.isStreaming)
                        if (showStandaloneTyping) {
                            item(
                                key = "standalone-typing-indicator",
                                contentType = "typing-indicator",
                            ) {
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
    allowActiveAnimations: Boolean,
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
            val displayParts = remember(message.id, message.parts, message.content, message.blocks) {
                message.displayParts()
            }
            if (message.isError && displayParts.isEmpty()) {
                AssistantErrorCard(message = message.errorMessage ?: "出错了")
            } else {
                // 文本气泡
                if (isUser && message.content.isNotBlank()) {
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
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                }

                if (!isUser) {
                    Text(
                        text = "智慧记 AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgentAssistantAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                    AssistantMessageTimeline(
                        message = message,
                        parts = displayParts,
                        allowActiveAnimations = allowActiveAnimations,
                    )
                }

                if (!isUser && message.isStreaming && allowActiveAnimations) {
                    StreamingToolActivityPill(toolCalls = message.runTrace?.toolCalls.orEmpty())
                }

                if (!isUser && message.isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistantErrorCard(
                        message = message.errorMessage ?: "部分结果接收失败，已保留当前可见内容"
                    )
                }

                // 过程轨迹（仅助手消息）
                val runTrace = message.runTrace
                if (!isUser && runTrace != null && message.shouldShowRunTracePanel()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RunTracePanel(
                        runTrace = runTrace,
                        isExpanded = runTrace.isExpanded,
                        onToggleExpand = onToggleRunTrace,
                    )
                } else if (!isUser && message.shouldShowRealQueryStatusCard()) {
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

private fun ChatMessage.displayParts(): List<ChatMessagePart> =
    parts.ifEmpty {
        buildList {
            if (content.isNotBlank()) {
                add(ChatMessagePart.Text(content))
                blocks.forEach { block ->
                    add(ChatMessagePart.ResultBlock(block))
                }
            }
        }
    }

private fun ChatMessage.streamingAutoFollowBucket(): Int =
    (content.length / 80) + parts.size

private fun LazyListState.shouldAutoFollowStreamingContent(messageCount: Int): Boolean =
    shouldAutoFollowStream(
        messageCount = messageCount,
        lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index,
        visibleItemCount = layoutInfo.visibleItemsInfo.size,
    )

internal fun shouldAutoFollowStream(
    messageCount: Int,
    lastVisibleItemIndex: Int?,
    visibleItemCount: Int,
): Boolean {
    if (messageCount <= 0) return false
    if (lastVisibleItemIndex == null) return visibleItemCount == 0
    val lastMessageIndex = messageCount - 1
    return lastMessageIndex - lastVisibleItemIndex <= AgentChatAutoFollowBottomThresholdItems
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
    mode: String?,
    llmStatus: String?,
    hasToolEvidence: Boolean,
    hasAuditTrace: Boolean,
    hasCompletedTool: Boolean,
    showBadges: Boolean,
    modifier: Modifier = Modifier,
) {
    val statusLabel = assistantHeaderStatusLabel(
        isStreaming = isStreaming,
        hasServerAnswerDelta = hasServerAnswerDelta,
        answerDeltaSource = answerDeltaSource,
        hasToolEvidence = hasToolEvidence,
        hasAuditTrace = hasAuditTrace,
        mode = mode,
        llmStatus = llmStatus,
    )
    val statusColor = when {
        isStreamInterruptedMode(mode = mode, llmStatus = llmStatus) -> WarningOrange
        answerDeltaSource == DeltaSourceModelStream -> AgentAssistantAccent
        answerDeltaSource == DeltaSourceRuleSummary -> WarningOrange
        isStreaming -> WarningOrange
        else -> AgentAssistantAccent
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
        if (showBadges) {
            val provenanceLabel = assistantProvenanceLabel(
                hasCompletedTool = hasCompletedTool,
                hasToolEvidence = hasToolEvidence,
                answerDeltaSource = answerDeltaSource,
            )
            val reviewLabel = assistantReviewBadgeLabel(
                isStreaming = isStreaming,
                hasAuditTrace = hasAuditTrace,
                hasToolEvidence = hasToolEvidence,
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
private fun AssistantMessageTimeline(
    message: ChatMessage,
    parts: List<ChatMessagePart>,
    allowActiveAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 30.dp,
        shape = RoundedCornerShape(20.dp),
        surfaceColor = Color.White.copy(alpha = 0.82f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (message.shouldShowAssistantHeader()) {
                AssistantMessageHeader(
                    isStreaming = message.isStreaming,
                    hasServerAnswerDelta = message.hasServerAnswerDelta,
                    answerDeltaSource = message.answerDeltaSource,
                    mode = message.runTrace?.mode,
                    llmStatus = message.runTrace?.llmStatus,
                    hasToolEvidence = message.runTrace?.toolCalls?.isNotEmpty() == true,
                    hasAuditTrace = message.runTrace?.auditId != null || message.runTrace?.traceId != null,
                    hasCompletedTool = message.runTrace?.toolCalls?.any {
                        it.status == ToolCallStatus.COMPLETED
                    } == true,
                    showBadges = message.shouldShowAssistantHeaderBadges(),
                )
            }
            if (parts.isEmpty()) {
                InlineStreamingStatus(
                    text = if (message.isStreaming) {
                        "正在分析问题并等待真实结果"
                    } else {
                        "暂无可展示回答"
                    },
                    animate = message.isStreaming && allowActiveAnimations,
                )
            } else {
                parts.forEachIndexed { index, part ->
                    key(part.stableKey(message.id, index)) {
                        when (part) {
                            is ChatMessagePart.Text -> {
                                AssistantTextPart(
                                    markdown = part.markdown,
                                    renderIdentity = part.renderIdentity(message.id, index),
                                )
                            }
                            is ChatMessagePart.ResultBlock -> {
                                AssistantResultBlockPart(
                                    block = part.block,
                                    isStreaming = message.isStreaming,
                                    renderIdentity = part.renderIdentity(message.id, index),
                                )
                            }
                            is ChatMessagePart.PendingResultBlock -> {
                                AssistantPendingResultBlockPart(
                                    block = part.block,
                                    isStreaming = message.isStreaming,
                                )
                            }
                        }
                    }
                }
                if (message.shouldShowInlineStreamingStatus()) {
                    InlineStreamingStatus(
                        text = message.answerDeltaSource?.inlineStreamingLabel()
                            ?: "正在分析问题并等待真实结果",
                        animate = message.isStreaming && allowActiveAnimations,
                    )
                }
            }
        }
    }
}

private fun ChatMessagePart.stableKey(messageId: String, index: Int): String =
    when (this) {
        is ChatMessagePart.Text -> "text-$messageId-$index-${markdown.hashCode()}"
        is ChatMessagePart.ResultBlock ->
            "result-$messageId-$index-${block.renderCacheIdentity()}"
        is ChatMessagePart.PendingResultBlock ->
            "pending-$messageId-$index-${block.renderCacheIdentity()}"
    }

private fun ChatMessagePart.renderIdentity(messageId: String, index: Int): String =
    stableKey(messageId, index)

@Composable
private fun AssistantTextPart(
    markdown: String,
    renderIdentity: Any,
    modifier: Modifier = Modifier,
) {
    if (markdown.isBlank()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistantTextSourceLabel()
        AgentMarkdownText(
            markdown = markdown,
            contentColor = TextPrimary,
            renderIdentity = renderIdentity,
        )
    }
}

@Composable
private fun AssistantResultBlockPart(
    block: ResultBlockDto,
    isStreaming: Boolean,
    renderIdentity: Any,
    modifier: Modifier = Modifier,
) {
    TimelineResultBlock(
        block = block,
        isStreaming = isStreaming,
        renderIdentity = renderIdentity,
        modifier = modifier,
    )
}

@Composable
private fun AssistantPendingResultBlockPart(
    block: ResultBlockDto,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    PendingResultBlockNotice(
        block = block,
        isStreaming = isStreaming,
        modifier = modifier,
    )
}

@Composable
private fun AssistantTextSourceLabel(modifier: Modifier = Modifier) {
    Text(
        text = assistantTextSourceLabel(),
        style = MaterialTheme.typography.labelSmall,
        color = AgentAssistantAccent,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AgentAssistantAccent.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

internal fun assistantTextSourceLabel(): String = "AI 总结"

@Composable
private fun PendingResultBlockNotice(
    block: ResultBlockDto,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    val notice = pendingResultBlockNoticeText(isStreaming)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZhihuijiPrimary.copy(alpha = 0.08f))
            .border(0.7.dp, ZhihuijiPrimary.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (isStreaming) {
            CircularProgressIndicator(
                color = ZhihuijiPrimary,
                strokeWidth = 1.8.dp,
                modifier = Modifier.size(15.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(ZhihuijiPrimary)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = block.title?.takeIf { it.isNotBlank() } ?: block.blockType.readableResultBlockName(),
                style = MaterialTheme.typography.labelMedium,
                color = ZhihuijiPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notice,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun pendingResultBlockNoticeText(isStreaming: Boolean): String =
    if (isStreaming) {
        "已取得真实结果，正在组织回答"
    } else {
        "查询结果已返回，未收到可读回答"
    }

@Composable
private fun TimelineResultBlock(
    block: ResultBlockDto,
    isStreaming: Boolean,
    renderIdentity: Any,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(AgentResultHeaderColors))
                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                text = block.title?.takeIf { it.isNotBlank() } ?: block.blockType.readableResultBlockName(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = resultBlockSourceLabel(block.blockType, isStreaming),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
        ResultBlockRenderer(
            block = block,
            renderIdentity = renderIdentity,
        )
    }
}

internal fun resultBlockTimingLabel(isStreaming: Boolean): String =
    if (isStreaming) "实时结果" else "查询结果"

internal fun resultBlockSourceLabel(blockType: String, isStreaming: Boolean): String {
    val source = when (blockType) {
        "evidence_card" -> "工具证据"
        "text", "markdown" -> "Markdown 结果块"
        "draft_card" -> "草稿结果"
        else -> "结构化查询"
    }
    return "${resultBlockTimingLabel(isStreaming)} · $source"
}

private fun String.readableResultBlockName(): String =
    when (this) {
        "kpi_grid" -> "指标结果"
        "table" -> "表格结果"
        "rank_list" -> "排行结果"
        "line_chart", "area_chart", "trend_chart" -> "趋势图"
        "bar_chart", "column_chart", "horizontal_bar_chart" -> "柱状图"
        "donut_chart", "pie_chart" -> "占比图"
        else -> replace('_', ' ')
    }

@Composable
private fun StreamingToolActivityPill(
    toolCalls: List<ToolCallRecord>,
    modifier: Modifier = Modifier,
) {
    if (toolCalls.isEmpty()) return

    val activeTool = remember(toolCalls) { toolCalls.latestActiveToolCall() }
    if (activeTool != null) {
        Spacer(modifier = Modifier.height(8.dp))
        InlineToolActivityPill(
            toolCall = activeTool,
            modifier = modifier,
        )
        return
    }

    val latestFinishedTool = remember(toolCalls) { toolCalls.latestFinishedToolCallCandidate() } ?: return
    var nowMs by remember(
        latestFinishedTool.toolName,
        latestFinishedTool.status,
        latestFinishedTool.completedAt,
        latestFinishedTool.timestamp,
    ) {
        mutableStateOf(System.currentTimeMillis())
    }
    val isVisible = latestFinishedTool.isRecentlyFinished(nowMs)
    LaunchedEffect(
        latestFinishedTool.toolName,
        latestFinishedTool.status,
        latestFinishedTool.completedAt,
        latestFinishedTool.timestamp,
    ) {
        while (latestFinishedTool.isRecentlyFinished(nowMs)) {
            delay(120)
            nowMs = System.currentTimeMillis()
        }
    }

    if (!isVisible) return
    Spacer(modifier = Modifier.height(8.dp))
    InlineToolActivityPill(
        toolCall = latestFinishedTool,
        modifier = modifier,
    )
}

@Composable
private fun InlineToolActivityPill(
    toolCall: ToolCallRecord,
    modifier: Modifier = Modifier,
) {
    val isRunning = toolCall.status == ToolCallStatus.RUNNING || toolCall.status == ToolCallStatus.PENDING
    val tone = when (toolCall.status) {
        ToolCallStatus.COMPLETED -> ZhihuijiPrimary
        ToolCallStatus.FAILED -> DangerRed
        else -> WarningOrange
    }
    val label = toolCall.activityLabel()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.10f))
            .border(0.6.dp, tone.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (isRunning) {
            CircularProgressIndicator(
                color = tone,
                strokeWidth = 1.6.dp,
                modifier = Modifier.size(13.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(tone)
            )
        }
        Text(
            text = toolCall.toolName.readableToolName(),
            style = MaterialTheme.typography.labelSmall,
            color = tone,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun ToolCallRecord.activityLabel(): String =
    when (status) {
        ToolCallStatus.COMPLETED -> resultSummary.shortToolActivityLabel() ?: "工具查询完成"
        ToolCallStatus.FAILED -> resultSummary.shortToolActivityLabel() ?: "工具查询失败"
        ToolCallStatus.PENDING,
        ToolCallStatus.RUNNING -> inputSummary.shortToolActivityLabel()
            ?: resultSummary.shortToolActivityLabel()
            ?: "正在查询真实业务数据"
    }

private fun String?.shortToolActivityLabel(): String? =
    this?.trim()?.take(34)?.ifBlank { null }

internal fun List<ToolCallRecord>.latestVisibleToolCall(nowMs: Long = System.currentTimeMillis()): ToolCallRecord? {
    val activeCall = latestActiveToolCall()
    if (activeCall != null) {
        return activeCall
    }
    return latestFinishedToolCallCandidate()?.takeIf { it.isRecentlyFinished(nowMs) }
}

internal fun List<ToolCallRecord>.latestActiveToolCall(): ToolCallRecord? =
    asReversed().firstOrNull { call ->
        call.status == ToolCallStatus.RUNNING || call.status == ToolCallStatus.PENDING
    }

internal fun List<ToolCallRecord>.latestFinishedToolCallCandidate(): ToolCallRecord? =
    asReversed().firstOrNull { call ->
        call.status == ToolCallStatus.FAILED || call.status == ToolCallStatus.COMPLETED
    }

internal fun ToolCallRecord.isRecentlyFinished(nowMs: Long): Boolean {
    val completedAt = completedAt ?: timestamp
    return nowMs - completedAt in 0..CompletedToolPillVisibleMs
}

internal fun String.readableToolName(): String =
    when (this) {
        "cashflow_summary" -> "现金流"
        "sales_trend" -> "销售趋势"
        "stock_out_records" -> "缺货记录"
        "inventory_flow" -> "库存流水"
        "profit_summary" -> "利润分析"
        "finance_records" -> "资金明细"
        "inventory_low_stock_lookup" -> "低库存查询"
        "product_catalog_lookup" -> "商品查询"
        "customer_receivable_lookup" -> "客户应收查询"
        "supplier_payable_lookup" -> "供应商应付查询"
        "sales_overview_lookup" -> "经营概览查询"
        "sale_order_lookup" -> "销售单查询"
        "purchase_order_lookup" -> "采购单查询"
        "pay_order_lookup" -> "付款单查询"
        "finance_record_lookup" -> "资金流水查询"
        else -> replace('_', ' ')
    }

@Composable
private fun InlineStreamingStatus(
    text: String,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cursorAlpha = if (animate) {
        val transition = rememberInfiniteTransition(label = "agent_inline_streaming")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "agent_inline_streaming_alpha",
        ).value
    } else {
        1f
    }

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
                    text = "正在查询真实业务数据",
                    style = MaterialTheme.typography.labelLarge,
                    color = ZhihuijiPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "会按当前账号权限选择可用工具；失败时会明确提示",
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
                text = "正在生成回答",
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
                placeholder = "输入经营问题，AI 会查询真实业务数据...",
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
                text = emptyChatHelperText(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                emptyChatPills().forEachIndexed { index, label ->
                    val accent = when (index) {
                        0 -> ZhihuijiPrimary
                        1 -> AgentAssistantAccent
                        else -> WarningOrange
                    }
                    EmptyStatePill(label, accent)
                }
            }
        }
    }
}

internal fun emptyChatHelperText(): String =
    "发送问题后，AI 会按当前账号权限查询真实业务数据，并返回 Markdown、表格或统计图。"

internal fun emptyChatPills(): List<String> =
    listOf("真实查询", "流式回答", "图表结果")

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
