package com.zhihuiji.feature.agent

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus

private val AgentChatHorizontalPadding = 16.dp
private val AgentChatTopPadding = 16.dp
private val AgentChatBottomInputClearance = 116.dp
private const val CompletedToolPillVisibleMs = 1_200L
private const val AgentChatAutoFollowBottomThresholdItems = 1
private val EmptyChatPills = listOf("真实查询", "流式回答", "图表结果")

private data class ChatTailState(
    val lastMessage: ChatMessage?,
    val lastAssistantMessage: ChatMessage?,
    val activeStreamingMessage: ChatMessage?,
)

private data class EditUserMessageState(
    val messageId: String,
    val originalText: String,
)

@Composable
fun AgentChatScreen(
    initialQuestion: String? = null,
    conversationId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgentChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = uiState.messages
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val chatTailState = remember(messages) {
        var activeStreamingMessage: ChatMessage? = null
        var lastAssistantMessage: ChatMessage? = null
        for (index in messages.lastIndex downTo 0) {
            val message = messages[index]
            if (lastAssistantMessage == null && message.role == MessageRole.ASSISTANT) {
                lastAssistantMessage = message
            }
            if (activeStreamingMessage == null && message.role == MessageRole.ASSISTANT && message.isStreaming) {
                activeStreamingMessage = message
            }
            if (lastAssistantMessage != null && activeStreamingMessage != null) {
                break
            }
        }
        ChatTailState(
            lastMessage = messages.lastOrNull(),
            lastAssistantMessage = lastAssistantMessage,
            activeStreamingMessage = activeStreamingMessage,
        )
    }
    val lastMessage = chatTailState.lastMessage
    val lastAssistantMessage = chatTailState.lastAssistantMessage
    val activeStreamingMessage = chatTailState.activeStreamingMessage
    val activeStreamingMessageId = activeStreamingMessage?.id
    val streamingScrollBucket = if (uiState.isStreaming && activeStreamingMessage != null) {
        activeStreamingMessage.streamingAutoFollowBucket()
    } else {
        0
    }
    // 新消息进入时使用动画；流式增量只做轻量贴底，避免每个 token 排队滚动动画。
    LaunchedEffect(messages.size, lastMessage?.id) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(uiState.isStreaming, activeStreamingMessageId, streamingScrollBucket) {
        if (
            uiState.isStreaming &&
            activeStreamingMessageId != null &&
            lastMessage?.id == activeStreamingMessageId &&
            listState.shouldAutoFollowStreamingContent(messages.size)
        ) {
            listState.scrollToItem(messages.lastIndex)
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

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var editUserMessageState by remember { mutableStateOf<EditUserMessageState?>(null) }
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen && !drawerState.isOpen) {
            drawerState.open()
        } else if (!uiState.isDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }
    // 重连中提示
    val retryMessage = uiState.retryMessage

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ConversationListPanel(
                    conversations = uiState.conversations,
                    currentConversationId = uiState.conversationId,
                    isLoading = uiState.isLoadingConversations,
                    onSwitch = viewModel::switchConversation,
                    onDelete = viewModel::deleteConversation,
                    onCreateNew = {
                        viewModel.closeDrawer()
                        viewModel.clearMessages()
                    },
                )
            }
        },
    ) {
        GlassScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                GlassTopBar(
                    title = "AI 对话",
                    subtitle = if (uiState.isStreaming) "正在分析真实业务数据" else "真实问答与结果块",
                    onNavigationClick = onNavigateBack,
                    actions = {
                        IconButton(onClick = viewModel::openDrawer) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "会话列表",
                                tint = TextSecondary,
                            )
                        }
                        if (messages.isNotEmpty()) {
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
                    imageAttachments = uiState.imageAttachments,
                    attachmentAuthToken = uiState.attachmentAuthToken,
                    isUploadingImage = uiState.isUploadingImage,
                    isGeneratingImage = uiState.isGeneratingImage,
                    generatedImageUrl = uiState.generatedImageUrl,
                    generatedImagePrompt = uiState.generatedImagePrompt,
                    onInputChange = viewModel::onInputChange,
                    onUploadImage = { uri -> viewModel.uploadImage(uri, context) },
                    onRemoveImage = viewModel::removeImageAttachment,
                    onGenerateImage = viewModel::generateImage,
                    onDismissGeneratedImage = viewModel::dismissGeneratedImage,
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
                        EmptyChatState(
                            onPillClick = { pillText ->
                                viewModel.onInputChange(pillText)
                                viewModel.sendMessage()
                            },
                        )
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
                                items = messages,
                                key = { it.id },
                                contentType = { message -> "message-${message.role.name.lowercase()}" },
                            ) { message ->
                                ChatMessageItem(
                                    message = message,
                                    allowActiveAnimations = message.id == activeStreamingMessageId,
                                    onToggleRunTrace = { viewModel.toggleRunTrace(message.id) },
                                    onRegenerate = { viewModel.regenerateMessage(message.id) },
                                    onEditUserMessage = {
                                        editUserMessageState = EditUserMessageState(
                                            messageId = message.id,
                                            originalText = message.content,
                                        )
                                    },
                                    onFollowUp = { text ->
                                        viewModel.onInputChange(text)
                                        viewModel.sendMessage()
                                    },
                                )
                            }

                            val showStandaloneTyping = lastAssistantMessage
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

                // 重连中提示（浮动）
                if (retryMessage != null) {
                    RetryBanner(
                        message = retryMessage,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // 草稿确认弹窗
    uiState.showDraftConfirm?.let { draft ->
        DraftConfirmDialog(
            draft = draft,
            onConfirm = { viewModel.confirmDraftFromDialog(draft.draftId) },
            onCancelDraft = { viewModel.cancelDraftFromDialog(draft.draftId) },
            onDismiss = viewModel::dismissDraftConfirm,
        )
    }

    editUserMessageState?.let { editState ->
        EditUserMessageDialog(
            initialText = editState.originalText,
            onDismiss = { editUserMessageState = null },
            onConfirm = { updatedText ->
                viewModel.editAndResend(editState.messageId, updatedText)
                editUserMessageState = null
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    allowActiveAnimations: Boolean,
    onToggleRunTrace: () -> Unit,
    onRegenerate: () -> Unit,
    onEditUserMessage: () -> Unit,
    onFollowUp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == MessageRole.USER
    val runTrace = message.runTrace
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember(message.id) { mutableStateOf(false) }

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
                    val bubbleShape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    )
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {},
                                onLongClick = if (isUser) onEditUserMessage else null,
                            )
                            .clip(bubbleShape)
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
                                shape = bubbleShape
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
                        runTrace = runTrace,
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

                // AI 消息操作栏（复制 / 重新生成 / 分享），仅非流式且非错误时展示
                if (!isUser && !message.isStreaming && !message.isError && message.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AssistantMessageActionBar(
                        copied = copied,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            copied = true
                        },
                        onRegenerate = onRegenerate,
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message.content)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享回答"))
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FollowUpChips(onFollowUp = onFollowUp)
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
private fun EditUserMessageDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "编辑后重发",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "将删除这条消息及其后的上下文，并用编辑后的内容重新提问。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                GlassTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "请输入新的问题",
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("重发")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private fun ChatMessage.displayParts(): List<ChatMessagePart> =
    parts.ifEmpty {
        ArrayList<ChatMessagePart>(1 + blocks.size).apply {
            if (content.isNotBlank()) {
                add(ChatMessagePart.Text(content))
                for (index in blocks.indices) {
                    add(ChatMessagePart.ResultBlock(blocks[index]))
                }
            }
        }
    }

@Composable
private fun AssistantMessageActionBar(
    copied: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistantActionButton(
            icon = Icons.Default.ContentCopy,
            label = if (copied) "已复制" else "复制",
            onClick = onCopy,
        )
        AssistantActionButton(
            icon = Icons.Default.Refresh,
            label = "重新生成",
            onClick = onRegenerate,
        )
        AssistantActionButton(
            icon = Icons.Default.Share,
            label = "分享",
            onClick = onShare,
        )
    }
}

@Composable
private fun AssistantActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val FollowUpChipTexts = listOf("详细说说", "还有其他方法吗", "导出表格")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FollowUpChips(
    onFollowUp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FollowUpChipTexts.forEach { text ->
            FollowUpChip(text = text, onClick = { onFollowUp(text) })
        }
    }
}

@Composable
private fun FollowUpChip(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = AgentAssistantAccent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AgentAssistantAccent.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun RetryBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(WarningOrange.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = WarningOrange,
                strokeWidth = 1.8.dp,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = WarningOrange,
                fontWeight = FontWeight.SemiBold,
            )
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
        answerDeltaSource == DeltaSourceRuleSummary || isStreaming -> WarningOrange
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
    runTrace: RunTrace?,
    allowActiveAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    val toolCalls = runTrace?.toolCalls.orEmpty()
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
                var hasCompletedTool = false
                for (index in toolCalls.indices) {
                    if (toolCalls[index].status == ToolCallStatus.COMPLETED) {
                        hasCompletedTool = true
                        break
                    }
                }
                AssistantMessageHeader(
                    isStreaming = message.isStreaming,
                    hasServerAnswerDelta = message.hasServerAnswerDelta,
                    answerDeltaSource = message.answerDeltaSource,
                    mode = runTrace?.mode,
                    llmStatus = runTrace?.llmStatus,
                    hasToolEvidence = toolCalls.isNotEmpty(),
                    hasAuditTrace = runTrace?.auditId != null || runTrace?.traceId != null,
                    hasCompletedTool = hasCompletedTool,
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
    // 逐字渐显：首次进入组合时由 false→true 触发淡入；renderIdentity 变化时重新触发
    var visible by remember(renderIdentity) { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistantTextSourceLabel()
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)),
        ) {
            AgentMarkdownText(
                markdown = markdown,
                contentColor = TextPrimary,
                renderIdentity = renderIdentity,
            )
        }
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

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val visibleTool = remember(toolCalls, nowMs) { toolCalls.latestVisibleToolCall(nowMs) }
    if (visibleTool == null) return

    if (visibleTool.status == ToolCallStatus.RUNNING || visibleTool.status == ToolCallStatus.PENDING) {
        Spacer(modifier = Modifier.height(8.dp))
        InlineToolActivityPill(
            toolCall = visibleTool,
            modifier = modifier,
        )
        return
    }

    val isVisible = visibleTool.isRecentlyFinished(nowMs)
    LaunchedEffect(
        visibleTool.toolName,
        visibleTool.status,
        visibleTool.completedAt,
        visibleTool.timestamp,
    ) {
        while (visibleTool.isRecentlyFinished(nowMs)) {
            delay(120)
            nowMs = System.currentTimeMillis()
        }
    }

    if (!isVisible) return
    Spacer(modifier = Modifier.height(8.dp))
    InlineToolActivityPill(
        toolCall = visibleTool,
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
        ToolCallStatus.RUNNING -> {
            val inputLabel = inputSummary.shortToolActivityLabel()
            inputLabel ?: resultSummary.shortToolActivityLabel() ?: "正在查询真实业务数据"
        }
    }

private fun String?.shortToolActivityLabel(): String? =
    this?.trim()?.take(34)?.ifBlank { null }

internal fun List<ToolCallRecord>.latestVisibleToolCall(nowMs: Long = System.currentTimeMillis()): ToolCallRecord? {
    var latestFinishedCall: ToolCallRecord? = null
    for (index in lastIndex downTo 0) {
        val call = this[index]
        when (call.status) {
            ToolCallStatus.RUNNING,
            ToolCallStatus.PENDING -> return call

            ToolCallStatus.FAILED,
            ToolCallStatus.COMPLETED -> if (latestFinishedCall == null) {
                latestFinishedCall = call
            }
        }
    }
    return latestFinishedCall?.takeIf { it.isRecentlyFinished(nowMs) }
}

internal fun ToolCallRecord.isRecentlyFinished(nowMs: Long): Boolean {
    val completedAt = completedAt ?: timestamp
    return nowMs - completedAt in 0..CompletedToolPillVisibleMs
}

private val readableToolNames = mapOf(
    "cashflow_summary" to "现金流",
    "sales_trend" to "销售趋势",
    "stock_out_records" to "缺货记录",
    "inventory_flow" to "库存流水",
    "profit_summary" to "利润分析",
    "finance_records" to "资金明细",
    "inventory_low_stock_lookup" to "低库存查询",
    "product_catalog_lookup" to "商品查询",
    "customer_receivable_lookup" to "客户应收查询",
    "supplier_payable_lookup" to "供应商应付查询",
    "sales_overview_lookup" to "经营概览查询",
    "sale_order_lookup" to "销售单查询",
    "purchase_order_lookup" to "采购单查询",
    "pay_order_lookup" to "付款单查询",
    "finance_record_lookup" to "资金流水查询",
)

internal fun String.readableToolName(): String = readableToolNames[this] ?: replace('_', ' ')

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
@OptIn(ExperimentalLayoutApi::class)
private fun ChatInputBar(
    inputText: String,
    isStreaming: Boolean,
    canStop: Boolean,
    imageAttachments: List<AgentImageAttachmentUi>,
    attachmentAuthToken: String?,
    isUploadingImage: Boolean,
    isGeneratingImage: Boolean,
    generatedImageUrl: String?,
    generatedImagePrompt: String?,
    onInputChange: (String) -> Unit,
    onUploadImage: (android.net.Uri) -> Unit,
    onRemoveImage: (Long) -> Unit,
    onGenerateImage: (String) -> Unit,
    onDismissGeneratedImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showGenerateDialog by remember { mutableStateOf(false) }
    var generatePrompt by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onUploadImage(uri)
        }
    }
    val canSend = (inputText.isNotBlank() || imageAttachments.isNotEmpty()) && !isStreaming

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        blurRadius = 40.dp,
        shape = RoundedCornerShape(30.dp),
        surfaceColor = Color.White.copy(alpha = 0.84f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (imageAttachments.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    imageAttachments.forEach { attachment ->
                        AgentInputImageThumbnail(
                            url = attachment.url,
                            authToken = attachmentAuthToken,
                            onRemove = { onRemoveImage(attachment.assetId) },
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = {
                        launcher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isStreaming && !isUploadingImage,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f))
                ) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(
                            color = ZhihuijiPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AddAPhoto,
                            contentDescription = "上传图片",
                            tint = ZhihuijiPrimary,
                        )
                    }
                }

                TextButton(
                    onClick = {
                        generatePrompt = inputText.ifBlank { generatePrompt }
                        showGenerateDialog = true
                    },
                    enabled = !isStreaming && !isGeneratingImage,
                ) {
                    Text(
                        text = if (isGeneratingImage) "生图中" else "生图",
                        color = ZhihuijiPrimary,
                    )
                }

                GlassTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState()),
                    label = null,
                    placeholder = if (imageAttachments.isEmpty()) {
                        "输入经营问题，AI 会查询真实业务数据..."
                    } else {
                        "可结合图片提问，或直接发送分析图片"
                    },
                    singleLine = false,
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

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = {
                Text(if (imageAttachments.isEmpty()) "文本生图" else "以图生图")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (imageAttachments.isEmpty()) {
                            "直接输入提示词生成图片。"
                        } else {
                            "当前会使用已上传图片作为参考图。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    GlassTextField(
                        value = generatePrompt,
                        onValueChange = { generatePrompt = it },
                        placeholder = "输入生图提示词",
                        singleLine = false,
                        enabled = !isGeneratingImage,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onGenerateImage(generatePrompt)
                        showGenerateDialog = false
                    },
                    enabled = generatePrompt.isNotBlank() && !isGeneratingImage,
                ) {
                    Text("开始生成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (generatedImageUrl != null) {
        AlertDialog(
            onDismissRequest = onDismissGeneratedImage,
            title = { Text("生成结果") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    generatedImagePrompt?.takeIf { it.isNotBlank() }?.let { prompt ->
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(generatedImageUrl)
                            .build(),
                        contentDescription = "生成图片结果",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissGeneratedImage) {
                    Text("关闭")
                }
            },
        )
    }
}

@Composable
private fun AgentInputImageThumbnail(
    url: String,
    authToken: String?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request = remember(url, authToken) {
        ImageRequest.Builder(context)
            .data(url)
            .apply {
                if (!authToken.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $authToken")
                }
            }
            .build()
    }
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.65f)),
    ) {
        AsyncImage(
            model = request,
            contentDescription = "Agent 输入图片",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xCC000000))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除图片",
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun EmptyChatState(
    onPillClick: (String) -> Unit,
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
                    EmptyStatePill(
                        text = label,
                        accent = accent,
                        onClick = { onPillClick(label) },
                    )
                }
            }
        }
    }
}

internal fun emptyChatHelperText(): String =
    "发送问题后，AI 会按当前账号权限查询真实业务数据，并返回 Markdown、表格或统计图。"

internal fun emptyChatPills(): List<String> =
    EmptyChatPills

@Composable
private fun EmptyStatePill(
    text: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
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
    onConfirm: () -> Unit,
    onCancelDraft: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认草稿") },
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
                    text = "确认后会调用后端草稿确认接口执行真实创建；取消则会将该草稿标记为 cancelled。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认执行", color = ZhihuijiPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelDraft) {
                Text("取消草稿")
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
