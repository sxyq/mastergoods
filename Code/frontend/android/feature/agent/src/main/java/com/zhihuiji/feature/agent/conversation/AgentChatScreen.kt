package com.zhihuiji.feature.agent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassBorderSoft
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.AnswerTraceStatus
import com.zhihuiji.core.model.v2.agent.DraftTrace
import com.zhihuiji.core.model.v2.agent.MessageRole
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RunTerminalStatus
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.RunTraceAuditState
import com.zhihuiji.core.model.v2.agent.RunTraceItem
import com.zhihuiji.core.model.v2.agent.SafetyTraceStatus
import com.zhihuiji.core.model.v2.agent.TerminalTrace
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private val AgentChatHorizontalPadding = 16.dp
private val AgentChatTopPadding = 24.dp
private val AgentChatBottomInputClearance = 144.dp
private const val AgentChatAutoFollowBottomThresholdItems = 1
private enum class ImageGenerationMode {
    TEXT_TO_IMAGE,
    IMAGE_TO_IMAGE,
}

private data class ChatTailState(
    val lastMessage: ChatMessage?,
)

private data class MessagePrependAnchor(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val messageCount: Int,
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
    onNavigateToDraftList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgentChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = uiState.messages
    val listState = rememberLazyListState()
    var pendingMessagePrependAnchor by remember(uiState.conversationId) {
        mutableStateOf<MessagePrependAnchor?>(null)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val density = LocalDensity.current
    val imeBottomInset = with(density) {
        WindowInsets.ime.getBottom(this).toDp()
    }
    // GlassScaffold draws its bottom bar as an overlay. Reserve both the bar
    // and the IME area so the empty state and message list never sit beneath it.
    val chatContentBottomClearance = AgentChatBottomInputClearance + imeBottomInset
    val agentTopBarClearance = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    } + 56.dp
    val chatTailState = remember(messages) {
        ChatTailState(
            lastMessage = messages.lastOrNull(),
        )
    }
    val lastMessage = chatTailState.lastMessage
    val tailScrollBucket = if (lastMessage?.role == MessageRole.ASSISTANT) {
        lastMessage.streamingAutoFollowBucket()
    } else {
        0
    }
    // 新消息进入时使用动画；流式增量只做轻量贴底，避免每个 token 排队滚动动画。
    LaunchedEffect(lastMessage?.id) {
        if (messages.isNotEmpty()) {
            listState.scrollToConversationEnd(messages.size, animated = true)
        }
    }

    LaunchedEffect(
        uiState.conversationId,
        uiState.hasMoreMessages,
        uiState.isStreaming,
        messages.size,
    ) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                if (
                    firstVisibleItemIndex <= 1 &&
                    uiState.hasMoreMessages &&
                    !uiState.isLoadingMoreMessages &&
                    !uiState.isStreaming &&
                    pendingMessagePrependAnchor == null
                ) {
                    pendingMessagePrependAnchor = MessagePrependAnchor(
                        firstVisibleItemIndex = firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                        messageCount = messages.size,
                    )
                    viewModel.loadMoreMessages()
                }
            }
    }

    LaunchedEffect(uiState.isLoadingMoreMessages, messages.size) {
        val anchor = pendingMessagePrependAnchor ?: return@LaunchedEffect
        if (uiState.isLoadingMoreMessages) return@LaunchedEffect

        val insertedMessageCount = messages.size - anchor.messageCount
        if (insertedMessageCount > 0 && messages.isNotEmpty()) {
            val restoredIndex = (anchor.firstVisibleItemIndex + insertedMessageCount)
                .coerceIn(0, messages.lastIndex)
            listState.scrollToItem(
                index = restoredIndex,
                scrollOffset = anchor.firstVisibleItemScrollOffset,
            )
        }
        pendingMessagePrependAnchor = null
    }

    LaunchedEffect(uiState.isStreaming, lastMessage?.id, tailScrollBucket) {
        if (
            lastMessage?.role == MessageRole.ASSISTANT &&
            listState.shouldAutoFollowStreamingContent(messages.size)
        ) {
            listState.scrollToConversationEnd(messages.size)
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
    val onSwitchConversation = remember(viewModel) {
        { conversationId: Long -> viewModel.switchConversation(conversationId) }
    }
    val onDeleteConversation = remember(viewModel) {
        { conversationId: Long -> viewModel.deleteConversation(conversationId) }
    }
    val onCreateConversation = remember(viewModel) {
        {
            viewModel.closeDrawer()
            viewModel.clearMessages()
        }
    }
    val drawerConversations = rememberUpdatedState(uiState.conversations)
    val drawerConversationId = rememberUpdatedState(uiState.conversationId)
    val drawerLoading = rememberUpdatedState(uiState.isLoadingConversations)
    val conversationDrawerContent: @Composable () -> Unit = remember(viewModel) {
        {
            ModalDrawerSheet {
                ConversationListPanel(
                    conversations = drawerConversations.value,
                    currentConversationId = drawerConversationId.value,
                    isLoading = drawerLoading.value,
                    isLoadingMore = uiState.isLoadingMoreConversations,
                    hasMore = uiState.hasMoreConversations,
                    onSwitch = onSwitchConversation,
                    onDelete = onDeleteConversation,
                    onCreateNew = onCreateConversation,
                    onLoadMore = viewModel::loadMoreConversations,
                )
            }
        }
    }
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
        drawerContent = conversationDrawerContent,
    ) {
        GlassScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {},
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
                    onGenerateImage = { prompt, useReferenceImages ->
                        viewModel.generateImage(prompt, useReferenceImages)
                    },
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = agentTopBarClearance,
                            bottom = chatContentBottomClearance,
                        )
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
                                    bottom = 24.dp,
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
                                        onEditUserMessage = {
                                            editUserMessageState = EditUserMessageState(
                                                messageId = message.id,
                                                originalText = message.content,
                                            )
                                        },
                                        onRegenerate = { viewModel.regenerateMessage(message.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Keep the conversation header in the content layer. The outer
                // scaffold's top-bar slot collapses to zero during Android 16 IME
                // resize, while this window-anchored layer remains measurable.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(agentTopBarClearance)
                ) {
                    GlassTopBar(
                        modifier = Modifier.fillMaxSize(),
                        title = "AI 对话",
                        onNavigationClick = onNavigateBack,
                        actions = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onNavigateToDraftList) {
                                    Icon(
                                        imageVector = Icons.Default.Drafts,
                                        contentDescription = "草稿列表",
                                        tint = TextSecondary,
                                    )
                                }
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
                        }
                    )
                }

                // 上下文压缩提示（浮动）
                uiState.contextCompacted?.let { compacted ->
                    ContextCompactedBanner(
                        state = compacted,
                        onDismiss = viewModel::dismissContextCompacted,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = agentTopBarClearance)
                    )
                }

                // 重连中提示（浮动）
                if (retryMessage != null) {
                    RetryBanner(
                        message = retryMessage,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = agentTopBarClearance)
                    )
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
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

    // 创建类工具的待确认状态使用覆盖式弹窗表达二次授权。
    // 确认/拒绝只调用草稿确认接口，关闭弹窗不触发正式写入。
    val draftConfirmState = uiState.showDraftConfirm
    if (draftConfirmState != null &&
        draftConfirmState.confirmPhase != DraftConfirmPhase.CONFIRMED &&
        draftConfirmState.confirmPhase != DraftConfirmPhase.REJECTED
    ) {
        DraftConfirmDialog(
            draft = draftConfirmState,
            onConfirm = { viewModel.confirmDraftFromDialog(draftConfirmState.draftId) },
            onCancel = { viewModel.cancelDraftFromDialog(draftConfirmState.draftId) },
            onDismiss = { viewModel.dismissDraftConfirm() },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onEditUserMessage: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = if (isUser) {
                Modifier.widthIn(max = 318.dp)
            } else {
                Modifier.fillMaxWidth()
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
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                            Row(
                                modifier = Modifier.padding(top = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = TimeFormatter.formatTime(message.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "已发送",
                                    tint = Color(0xFF4A8BFF),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                if (!isUser) {
                    AssistantResponseSurface(
                        message = message,
                        parts = displayParts,
                        onRegenerate = onRegenerate,
                    )
                }

                if (!isUser && message.isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistantErrorCard(
                        message = message.errorMessage ?: "部分结果接收失败，已保留当前可见内容"
                    )
                }

            }
        }
    }
}

@Composable
private fun AssistantResponseSurface(
    message: ChatMessage,
    parts: List<ChatMessagePart>,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trace = message.runTrace
    val visibleParts = remember(message.id, parts) { parts.visibleAssistantParts() }
    val timeline = assistantVisibleTimeline(message, trace, visibleParts)
    if (!message.isStreaming && visibleParts.isEmpty() && timeline.isEmpty()) return

    val thinkingItems = timeline.filterIsInstance<RunTraceItem.PlanSummary>()
    val executionItems = timeline.filter {
        it is RunTraceItem.Safety || it is RunTraceItem.Tool
    }
    val resultItems = timeline.filterIsInstance<RunTraceItem.ResultBlock>()
    val renderParts = orderedAssistantParts(
        visibleParts = visibleParts,
        traceBlocks = resultItems.map { it.block },
    )
    val draftItems = timeline.filterIsInstance<RunTraceItem.Draft>()
    val auditItems = timeline.filterIsInstance<RunTraceItem.AuditLossy>()
    val terminal = message.assistantTerminalStatus(trace)
    var thinkingExpanded by rememberSaveable(message.id) {
        mutableStateOf(message.isStreaming)
    }
    var executionExpanded by rememberSaveable(message.id) {
        mutableStateOf(true)
    }

    // 生成完成后强制回到收起态；完成后用户点击思考过程行即可再次展开。
    LaunchedEffect(message.id, message.isStreaming) {
        thinkingExpanded = message.isStreaming
    }

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        blurRadius = 30.dp,
        shape = RoundedCornerShape(20.dp),
        surfaceColor = Color.White.copy(alpha = 0.84f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AssistantMessageHeader(
                message = message,
                trace = trace,
                terminal = terminal,
            )

            if (thinkingItems.isNotEmpty() || message.isStreaming) {
                AssistantThinkingSection(
                    items = thinkingItems,
                    expanded = thinkingExpanded,
                    onToggle = { thinkingExpanded = !thinkingExpanded },
                    durationLabel = trace?.visibleDurationLabel(message.isStreaming),
                )
            }

            if (executionItems.isNotEmpty() || message.isStreaming) {
                AssistantExecutionSection(
                    items = executionItems,
                    expanded = executionExpanded,
                    onToggle = { executionExpanded = !executionExpanded },
                    isStreaming = message.isStreaming,
                )
            }

            AssistantAnswerBody(
                messageId = message.id,
                parts = renderParts,
                isStreaming = message.isStreaming,
                showWaitingIndicator = !timeline.hasVisibleProcess(),
            )
            draftItems.forEach { item ->
                key(item.id) { AssistantDraftTraceCard(item.draft) }
            }
            auditItems.forEach { item ->
                key(item.id) { AssistantAuditStatusCard(item) }
            }
            if (terminal != null && terminal != RunTerminalStatus.COMPLETED) {
                AssistantTerminalStatusCard(
                    terminal = trace?.terminal ?: TerminalTrace(
                        status = terminal,
                        message = message.errorMessage,
                        timestamp = message.createdAt,
                    ),
                )
            }

            if (!message.isStreaming && !message.isError && (message.content.isNotBlank() || resultItems.isNotEmpty())) {
                AssistantMessageActionBar(
                    message = message,
                    onRegenerate = onRegenerate,
                    onShowSource = { executionExpanded = true },
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageHeader(
    message: ChatMessage,
    trace: RunTrace?,
    terminal: RunTerminalStatus?,
) {
    val (statusLabel, statusColor) = when {
        message.isStreaming -> "处理中" to ZhihuijiPrimary
        message.isError -> "失败" to DangerRed
        terminal == RunTerminalStatus.COMPLETED -> "已完成" to AgentAssistantAccent
        terminal != null -> "已${terminal.terminalStatusLabel()}" to WarningOrange
        else -> "已完成" to AgentAssistantAccent
    }
    val toolCount = trace?.toolCalls.orEmpty().size
    val dataCount = trace?.toolCalls.orEmpty().sumOf { it.returnedCount ?: 0 }
    val meta = when {
        dataCount > 0 -> "本次查询 · $dataCount 个数据点"
        toolCount > 0 -> "本次查询 · $toolCount 个步骤"
        message.isStreaming -> "正在处理当前问题"
        else -> "本次回答"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFE4EEFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "智慧记助手",
                tint = ZhihuijiPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "智慧记助手",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZhihuijiPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(ZhihuijiPrimary.copy(alpha = 0.10f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(statusColor.copy(alpha = 0.10f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = TimeFormatter.formatTime(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun AssistantThinkingSection(
    items: List<RunTraceItem.PlanSummary>,
    expanded: Boolean,
    onToggle: () -> Unit,
    durationLabel: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.52f))
            .border(1.dp, GlassBorderSoft, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF7867D9),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "思考过程",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            durationLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起思考过程" else "展开思考过程",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded && items.isNotEmpty()) {
            HorizontalDivider(color = GlassBorderSoft)
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items.forEach { item ->
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantExecutionSection(
    items: List<RunTraceItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    isStreaming: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.44f))
            .border(1.dp, GlassBorderSoft, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "执行过程 · ${items.size} 个步骤",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起执行过程" else "展开执行过程",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            HorizontalDivider(color = GlassBorderSoft)
            if (items.isEmpty() && isStreaming) {
                AssistantProcessPending(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = GlassBorderSoft)
                        AssistantTimelineEntry(
                            item = item,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantMessageActionBar(
    message: ChatMessage,
    onRegenerate: () -> Unit,
    onShowSource: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by rememberSaveable(message.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorderSoft, RoundedCornerShape(12.dp))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistantActionButton(
            icon = Icons.Default.ContentCopy,
            label = if (copied) "已复制" else "复制",
            onClick = {
                clipboardManager.setText(AnnotatedString(message.content))
                copied = true
            },
            modifier = Modifier.weight(1f),
        )
        HorizontalDivider(
            modifier = Modifier.height(22.dp).width(1.dp),
            color = GlassBorderSoft,
        )
        AssistantActionButton(
            icon = Icons.Default.Refresh,
            label = "重新生成",
            onClick = onRegenerate,
            modifier = Modifier.weight(1f),
        )
        HorizontalDivider(
            modifier = Modifier.height(22.dp).width(1.dp),
            color = GlassBorderSoft,
        )
        AssistantActionButton(
            icon = Icons.Default.Storage,
            label = "查看数据来源",
            onClick = onShowSource,
            enabled = message.runTrace?.toolCalls?.isNotEmpty() == true,
            modifier = Modifier.weight(1.25f),
        )
    }
}

@Composable
private fun AssistantActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AssistantTimelineEntry(
    item: RunTraceItem,
    modifier: Modifier = Modifier,
) {
    val title: String
    val detail: String?
    val accent: Color
    when (item) {
        is RunTraceItem.PlanSummary -> {
            title = "计划摘要"
            detail = item.content.takeIf { it.isNotBlank() }
            accent = ZhihuijiPrimary
        }

        is RunTraceItem.Safety -> {
            title = "安全状态"
            val result = item.result
            detail = when {
                result?.passed == true || item.status == SafetyTraceStatus.PASSED -> "安全检查已通过"
                result?.passed == false || item.status == SafetyTraceStatus.BLOCKED ->
                    listOfNotNull(
                        "当前请求未通过安全检查",
                        result?.reason,
                        result?.suggestedAction,
                    ).joinToString("：")
                else -> null
            }
            accent = if (result?.passed == false || item.status == SafetyTraceStatus.BLOCKED) {
                WarningOrange
            } else {
                ZhihuijiPrimary
            }
        }

        is RunTraceItem.Tool -> {
            AssistantToolTraceCard(call = item.call, modifier = modifier)
            return
        }

        is RunTraceItem.Answer -> {
            title = "回答正文"
            detail = item.status.answerStatusLabel()
            accent = AgentAssistantAccent
        }

        is RunTraceItem.Terminal -> {
            AssistantTerminalStatusCard(item.terminal, modifier)
            return
        }

        is RunTraceItem.AuditLossy -> {
            AssistantAuditStatusCard(item, modifier)
            return
        }

        is RunTraceItem.ResultBlock,
        is RunTraceItem.Draft,
        -> return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            detail?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AssistantToolTraceCard(
    call: ToolCallRecord,
    modifier: Modifier = Modifier,
) {
    val statusText = when (call.status) {
        ToolCallStatus.PENDING -> "等待查询"
        ToolCallStatus.RUNNING -> call.progressMessage?.takeIf { it.isNotBlank() } ?: "正在查询"
        ToolCallStatus.COMPLETED -> call.resultSummary?.takeIf { it.isNotBlank() } ?: "查询完成"
        ToolCallStatus.FAILED -> call.resultSummary?.takeIf { it.isNotBlank() } ?: "查询未完成"
    }
    val detail = listOfNotNull(
        call.queryWindow?.safeQueryWindowSummary()?.let { "范围 $it" },
        call.returnedCount?.let { returned ->
            call.totalCount?.let { total -> "返回 $returned/$total 条" } ?: "返回 $returned 条"
        },
        call.limit?.let { "上限 $it 条" },
        call.isTruncated?.takeIf { it }?.let { "结果已截断" },
        call.evidence?.safeEvidenceSummary()?.let { "证据 $it" },
    ).joinToString(" · ")
    val accent = when (call.status) {
        ToolCallStatus.PENDING, ToolCallStatus.RUNNING -> ZhihuijiPrimary
        ToolCallStatus.COMPLETED -> AgentAssistantAccent
        ToolCallStatus.FAILED -> WarningOrange
    }
    val statusIcon = when (call.status) {
        ToolCallStatus.PENDING, ToolCallStatus.RUNNING -> Icons.Default.Refresh
        ToolCallStatus.COMPLETED -> Icons.Default.CheckCircle
        ToolCallStatus.FAILED -> Icons.Default.Close
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = call.userFacingToolLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            detail.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (call.isTruncated == true) WarningOrange else TextTertiary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AssistantAnswerBody(
    messageId: String,
    parts: List<ChatMessagePart>,
    isStreaming: Boolean,
    showWaitingIndicator: Boolean,
) {
    if (parts.isEmpty()) {
        if (isStreaming && showWaitingIndicator) {
            AssistantProcessPending()
        }
        return
    }
    parts.forEachIndexed { index, part ->
        key(part.stableKey(messageId, index)) {
            when (part) {
                is ChatMessagePart.Text -> {
                    AssistantTextPart(
                        markdown = part.markdown,
                        renderIdentity = part.renderIdentity(messageId, index),
                    )
                }

                is ChatMessagePart.ResultBlock -> {
                    AssistantResultBlockPart(
                        block = part.block,
                        renderIdentity = part.renderIdentity(messageId, index),
                    )
                }

                is ChatMessagePart.PendingResultBlock -> Unit
            }
        }
    }
}

private fun List<RunTraceItem>.hasVisibleProcess(): Boolean =
    any { item -> item !is RunTraceItem.Answer }

@Composable
private fun AssistantDraftTraceCard(
    draft: DraftTrace,
    modifier: Modifier = Modifier,
) {
    val status = draft.status?.takeIf { it.isNotBlank() }?.let { "状态：$it" } ?: "等待确认"
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(WarningOrange),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "草稿与确认",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$status · ${draft.title}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AssistantTerminalStatusCard(
    terminal: TerminalTrace,
    modifier: Modifier = Modifier,
) {
    val accent = when (terminal.status) {
        RunTerminalStatus.COMPLETED, RunTerminalStatus.CONFIRMATION_PENDING -> AgentAssistantAccent
        RunTerminalStatus.BLOCKED, RunTerminalStatus.CANCELLED, RunTerminalStatus.EXHAUSTED -> WarningOrange
        RunTerminalStatus.FAILED, RunTerminalStatus.INTERRUPTED -> DangerRed
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "运行${terminal.status.terminalStatusLabel()}",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            terminal.message?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AssistantAuditStatusCard(
    item: RunTraceItem.AuditLossy,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(WarningOrange),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "过程记录不完整",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodySmall,
                color = WarningOrange,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AssistantCollapsedRunSummary(
    message: ChatMessage,
    trace: RunTrace?,
    timeline: List<RunTraceItem>,
) {
    val terminal = message.assistantTerminalStatus(trace)
    val toolCount = timeline.count { it is RunTraceItem.Tool }
    val auditWarning = timeline.filterIsInstance<RunTraceItem.AuditLossy>().firstOrNull()
    val summary = buildList {
        terminal?.let { add("运行${it.terminalStatusLabel()}") }
        if (toolCount > 0) add("已记录 $toolCount 个查询步骤")
        auditWarning?.let { add("审计记录不完整") }
    }.ifEmpty { listOf("回答已生成") }
    Text(
        text = summary.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = if (auditWarning != null) WarningOrange else TextSecondary,
    )
}

private fun assistantVisibleTimeline(
    message: ChatMessage,
    trace: RunTrace?,
    visibleParts: List<ChatMessagePart>,
): List<RunTraceItem> {
    if (trace == null && !message.isStreaming && visibleParts.isEmpty()) return emptyList()

    val items = trace?.timeline.orEmpty().toMutableList()
    trace?.planSteps.orEmpty().forEachIndexed { index, step ->
        if (items.none { item ->
                item is RunTraceItem.PlanSummary &&
                    item.content == step.content &&
                    item.timestamp == step.timestamp
            }
        ) {
            items += RunTraceItem.PlanSummary(
                id = "plan:fallback:${step.timestamp}:$index",
                content = step.content,
                planSource = trace?.planSource,
                timestamp = step.timestamp,
            )
        }
    }

    trace?.safetyResult?.let { result ->
        val existing = items.filterIsInstance<RunTraceItem.Safety>().firstOrNull()
        val safety = RunTraceItem.Safety(
            id = existing?.id ?: "safety",
            status = if (result.passed) SafetyTraceStatus.PASSED else SafetyTraceStatus.BLOCKED,
            result = result,
            message = result.reason,
            timestamp = existing?.timestamp ?: message.createdAt,
        )
        val index = items.indexOfFirst { it is RunTraceItem.Safety }
        if (index >= 0) items[index] = safety else items += safety
    }

    trace?.toolCalls?.takeIf { it.isNotEmpty() }?.let { calls ->
        items.removeAll { it is RunTraceItem.Tool }
        calls.forEachIndexed { index, call ->
            items += RunTraceItem.Tool(
                call = call,
                id = "tool:${calls.toolDisplayStableKey(call, index)}",
            )
        }
    }

    val shouldShowAnswer = message.isStreaming || visibleParts.isNotEmpty() ||
        trace?.answerStatus?.let { it != AnswerTraceStatus.NOT_STARTED } == true ||
        trace?.toolCalls?.isNotEmpty() == true
    if (shouldShowAnswer) {
        val existing = items.filterIsInstance<RunTraceItem.Answer>().firstOrNull()
        items.removeAll { it is RunTraceItem.Answer }
        items += RunTraceItem.Answer(
            id = existing?.id ?: "answer",
            status = message.effectiveAnswerTraceStatus(trace, visibleParts),
            deltaSource = trace?.answerDeltaSource,
            timestamp = existing?.timestamp ?: message.createdAt,
        )
    }

    val existingBlocks = items.filterIsInstance<RunTraceItem.ResultBlock>().map { it.block }
    visibleParts.filterIsInstance<ChatMessagePart.ResultBlock>()
        .map { it.block }
        .filterNot { block -> existingBlocks.any { it == block } }
        .forEachIndexed { index, block ->
            items += RunTraceItem.ResultBlock(
                block = block,
                id = "result:${message.id}:$index:${block.renderCacheIdentity()}",
                timestamp = message.createdAt,
            )
        }

    trace?.draft?.let { draft ->
        items.removeAll { it is RunTraceItem.Draft }
        items += RunTraceItem.Draft(
            draft = draft,
            id = "draft:${draft.draftId}",
            timestamp = draft.timestamp,
        )
    }

    if (!message.isStreaming) {
        message.assistantTerminalStatus(trace)?.let { status ->
            val existing = items.filterIsInstance<RunTraceItem.Terminal>().firstOrNull()
            items.removeAll { it is RunTraceItem.Terminal }
            val terminal = trace?.terminal ?: TerminalTrace(
                status = status,
                message = message.errorMessage,
                timestamp = existing?.timestamp ?: message.createdAt,
            )
            items += RunTraceItem.Terminal(
                terminal = terminal,
                id = existing?.id ?: "terminal",
                timestamp = terminal.timestamp,
            )
        }
    }

    if (trace?.auditState == RunTraceAuditState.MISSING || trace?.auditState == RunTraceAuditState.LOSSY) {
        val existing = items.filterIsInstance<RunTraceItem.AuditLossy>().firstOrNull()
        items.removeAll { it is RunTraceItem.AuditLossy }
        items += RunTraceItem.AuditLossy(
            message = trace.auditWarnings.firstOrNull()
                ?: existing?.message
                ?: "运行轨迹暂不可用",
            droppedCount = existing?.droppedCount,
            failedCount = existing?.failedCount,
            id = existing?.id ?: "audit-lossy",
            timestamp = existing?.timestamp ?: message.createdAt,
        )
    }

    return items
        .filterNot { item -> item is RunTraceItem.Safety && item.result == null && item.status == SafetyTraceStatus.CHECKING }
        .sortedWith(compareBy({ it.phaseRank() }, { it.seq ?: Int.MAX_VALUE }, { it.timestamp }, { it.id }))
}

private fun RunTraceItem.phaseRank(): Int = when (this) {
    is RunTraceItem.PlanSummary -> 0
    is RunTraceItem.Safety -> 1
    is RunTraceItem.Tool -> 2
    is RunTraceItem.Answer -> 3
    is RunTraceItem.ResultBlock -> 4
    is RunTraceItem.Draft -> 5
    is RunTraceItem.Terminal -> 6
    is RunTraceItem.AuditLossy -> 7
}

private fun List<ToolCallRecord>.toolDisplayStableKey(
    call: ToolCallRecord,
    index: Int,
): String {
    val occurrence = take(index + 1).count { it.toolName == call.toolName } - 1
    return call.toolCallId?.takeIf { it.isNotBlank() }?.let { "id:$it" }
        ?: call.eventId?.takeIf { it.isNotBlank() }?.let { "event:$it" }
        ?: call.seq?.let { "seq:$it" }
        ?: call.startedAt?.let { "start:${call.toolName}:$it" }
        ?: "name:${call.toolName}:$occurrence"
}

private fun ChatMessage.isAssistantRunTerminal(trace: RunTrace?): Boolean =
    !isStreaming && (trace != null || isError)

private fun ChatMessage.assistantTerminalStatus(trace: RunTrace?): RunTerminalStatus? {
    trace?.terminal?.status?.let { return it }
    trace?.timeline?.filterIsInstance<RunTraceItem.Terminal>()?.lastOrNull()?.terminal?.status?.let { return it }
    trace?.answerStatus?.toTerminalStatusOrNull()?.let { return it }
    if (isError) {
        return if (errorMessage?.contains("STREAM_PARSE_ERROR", ignoreCase = true) == true ||
            isStreamInterruptedMode(trace?.mode, trace?.llmStatus)
        ) {
            RunTerminalStatus.INTERRUPTED
        } else {
            RunTerminalStatus.FAILED
        }
    }
    if (!isStreaming && trace != null) {
        if (trace.toolCalls.any { it.resultSummary?.contains("取消") == true }) {
            return RunTerminalStatus.CANCELLED
        }
        if (trace.toolCalls.any { it.resultSummary?.contains("中断") == true }) {
            return RunTerminalStatus.INTERRUPTED
        }
        return RunTerminalStatus.COMPLETED
    }
    return null
}

private fun ChatMessage.effectiveAnswerTraceStatus(
    trace: RunTrace?,
    visibleParts: List<ChatMessagePart>,
): AnswerTraceStatus = when {
    isStreaming -> AnswerTraceStatus.STREAMING
    isError && errorMessage?.contains("STREAM_PARSE_ERROR", ignoreCase = true) == true ->
        AnswerTraceStatus.INTERRUPTED
    isError -> AnswerTraceStatus.FAILED
    trace?.answerStatus != null && trace.answerStatus != AnswerTraceStatus.NOT_STARTED -> trace.answerStatus
    visibleParts.isNotEmpty() -> AnswerTraceStatus.COMPLETED
    else -> AnswerTraceStatus.NOT_STARTED
}

private fun AnswerTraceStatus.toTerminalStatusOrNull(): RunTerminalStatus? = when (this) {
    AnswerTraceStatus.COMPLETED -> RunTerminalStatus.COMPLETED
    AnswerTraceStatus.BLOCKED -> RunTerminalStatus.BLOCKED
    AnswerTraceStatus.CANCELLED -> RunTerminalStatus.CANCELLED
    AnswerTraceStatus.FAILED -> RunTerminalStatus.FAILED
    AnswerTraceStatus.INTERRUPTED -> RunTerminalStatus.INTERRUPTED
    AnswerTraceStatus.NOT_STARTED, AnswerTraceStatus.STREAMING -> null
    AnswerTraceStatus.CONFIRMATION_PENDING -> RunTerminalStatus.CONFIRMATION_PENDING
}

private fun AnswerTraceStatus.answerStatusLabel(): String = when (this) {
    AnswerTraceStatus.NOT_STARTED -> "等待回答"
    AnswerTraceStatus.STREAMING -> "正在生成回答"
    AnswerTraceStatus.CONFIRMATION_PENDING -> "等待确认草稿"
    AnswerTraceStatus.COMPLETED -> "回答已生成"
    AnswerTraceStatus.BLOCKED -> "回答被安全策略阻止"
    AnswerTraceStatus.CANCELLED -> "回答已取消"
    AnswerTraceStatus.FAILED -> "回答生成失败"
    AnswerTraceStatus.INTERRUPTED -> "连接中断，回答可能不完整"
}

private fun RunTerminalStatus.terminalStatusLabel(): String = when (this) {
    RunTerminalStatus.COMPLETED -> "完成"
    RunTerminalStatus.CONFIRMATION_PENDING -> "待确认"
    RunTerminalStatus.BLOCKED -> "受阻"
    RunTerminalStatus.CANCELLED -> "取消"
    RunTerminalStatus.FAILED -> "失败"
    RunTerminalStatus.EXHAUSTED -> "耗尽"
    RunTerminalStatus.INTERRUPTED -> "中断"
}

private fun assistantSurfaceSummary(
    message: ChatMessage,
    trace: RunTrace?,
    timeline: List<RunTraceItem>,
): String = when {
    message.isStreaming -> timeline.filterIsInstance<RunTraceItem.Tool>()
        .firstOrNull { it.call.status == ToolCallStatus.RUNNING }
        ?.call
        ?.let { "正在${it.userFacingToolLabel()}" }
        ?: "正在生成回答"
    message.assistantTerminalStatus(trace) != null -> "运行${message.assistantTerminalStatus(trace)!!.terminalStatusLabel()}"
    timeline.any { it is RunTraceItem.Tool } -> "已记录查询过程"
    else -> "回答已生成"
}

private fun RunTrace.visibleDurationLabel(isStreaming: Boolean): String? {
    val firstTimestamp = timeline.minOfOrNull { it.timestamp } ?: return null
    val lastTimestamp = timeline.maxOfOrNull { it.timestamp } ?: return null
    val endTimestamp = if (isStreaming) {
        System.currentTimeMillis()
    } else {
        lastTimestamp
    }
    val elapsedMs = (endTimestamp - firstTimestamp).coerceAtLeast(0L)
    val tenths = (elapsedMs / 100L).coerceAtMost(999L)
    return "${tenths / 10}.${tenths % 10} 秒"
}

private fun JsonElement.safeQueryWindowSummary(): String? {
    val obj = this as? JsonObject ?: return null
    val parts = listOfNotNull(
        obj.stringValue("owner_scope")?.takeIf { it == "current_owner" }?.let { "当前账号" },
        obj.stringValue("period")?.compactEvidenceText(24)?.let { "周期 $it" },
        obj.stringValue("date_from")?.let { "从 $it" },
        obj.stringValue("start_date")?.let { "从 $it" },
        obj.stringValue("date_to")?.let { "到 $it" },
        obj.stringValue("end_date")?.let { "到 $it" },
        obj.intValue("days")?.let { "近 $it 天" },
        obj.intValue("limit")?.let { "上限 $it 条" },
        obj.intValue("offset")?.let { "偏移 $it" },
        obj.stringValue("status")?.compactEvidenceText(20)?.let { "状态 $it" },
        obj.stringValue("order_status")?.compactEvidenceText(20)?.let { "订单 $it" },
        obj.stringValue("payment_status")?.compactEvidenceText(20)?.let { "付款 $it" },
        obj.stringValue("stock_status")?.compactEvidenceText(20)?.let { "库存 $it" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun JsonElement.safeEvidenceSummary(): String? {
    val obj = this as? JsonObject ?: return null
    val parts = listOfNotNull(
        obj.stringValue("source")?.removePrefix("tool:")?.compactEvidenceText(28)?.let { "来源 $it" },
        obj.stringValue("scope")?.takeIf { it == "current_owner" }?.let { "当前账号" },
        obj.stringValue("summary")?.compactEvidenceText(48),
        obj.intValue("returned_count")?.let { "返回 $it 条" },
        obj.intValue("total_count")?.let { "共 $it 条" },
        obj.booleanValue("is_truncated")?.takeIf { it }?.let { "已截断" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
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

internal fun ChatMessage.displayParts(): List<ChatMessagePart> =
    parts.ifEmpty {
        ArrayList<ChatMessagePart>(1 + blocks.size).apply {
            if (content.isNotBlank()) {
                add(ChatMessagePart.Text(content))
            }
            for (block in blocks) {
                add(ChatMessagePart.ResultBlock(block))
            }
        }
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

private suspend fun LazyListState.scrollToConversationEnd(
    messageCount: Int,
    animated: Boolean = false,
) {
    if (messageCount <= 0) return
    if (animated) {
        animateScrollToItem(messageCount - 1)
    } else {
        scrollToItem(messageCount - 1)
    }
    // scrollToItem aligns the item's top. A long assistant answer needs one
    // more pass to reveal its actual bottom above the floating input bar.
    withFrameNanos { }
    scrollBy(100_000f)
}

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

internal fun List<ChatMessagePart>.visibleAssistantParts(): List<ChatMessagePart> =
    filter { part ->
        when (part) {
            is ChatMessagePart.Text -> part.markdown.isNotBlank()
            is ChatMessagePart.ResultBlock -> true
            is ChatMessagePart.PendingResultBlock -> false
        }
    }

internal fun orderedAssistantParts(
    visibleParts: List<ChatMessagePart>,
    traceBlocks: List<ResultBlockDto>,
): List<ChatMessagePart> {
    val visibleBlocks = visibleParts
        .filterIsInstance<ChatMessagePart.ResultBlock>()
        .map { it.block }
    val missingTraceBlocks = traceBlocks.filterNot { block -> block in visibleBlocks }
    return visibleParts + missingTraceBlocks.map(ChatMessagePart::ResultBlock)
}

private fun ChatMessagePart.stableKey(messageId: String, index: Int): String =
    when (this) {
        is ChatMessagePart.Text -> "text-$messageId-$index"
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
    AgentMarkdownText(
        markdown = markdown,
        contentColor = TextPrimary,
        renderIdentity = renderIdentity,
        modifier = modifier,
    )
}

@Composable
private fun AssistantResultBlockPart(
    block: ResultBlockDto,
    renderIdentity: Any,
    modifier: Modifier = Modifier,
) {
    ResultBlockRenderer(
        block = block,
        renderIdentity = renderIdentity,
        modifier = modifier,
    )
}

@Composable
private fun AssistantProcessPending(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                .background(TextTertiary),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "正在等待回答…",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
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
    onGenerateImage: (String, Boolean) -> Unit,
    onDismissGeneratedImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGenerateDialog by remember { mutableStateOf(false) }
    var generatePrompt by remember { mutableStateOf("") }
    var generationMode by remember {
        mutableStateOf(
            if (imageAttachments.isNotEmpty()) {
                ImageGenerationMode.IMAGE_TO_IMAGE
            } else {
                ImageGenerationMode.TEXT_TO_IMAGE
            }
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onUploadImage(uri)
        }
    }
    val canSend = (inputText.isNotBlank() || imageAttachments.isNotEmpty()) && !isStreaming

    LaunchedEffect(generatedImageUrl) {
        if (generatedImageUrl != null) {
            showGenerateDialog = false
        }
    }

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
                        generationMode = if (imageAttachments.isNotEmpty()) {
                            ImageGenerationMode.IMAGE_TO_IMAGE
                        } else {
                            ImageGenerationMode.TEXT_TO_IMAGE
                        }
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
                        .heightIn(min = 56.dp, max = 96.dp),
                    label = null,
                    placeholder = if (imageAttachments.isEmpty()) {
                        "输入问题"
                    } else {
                        "可结合图片提问，或直接发送分析图片"
                    },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) onSend()
                        },
                    ),
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
        ImageGenerationDialog(
            onDismissRequest = { showGenerateDialog = false },
            mode = generationMode,
            onModeChange = { generationMode = it },
            prompt = generatePrompt,
            onPromptChange = { generatePrompt = it },
            imageAttachments = imageAttachments,
            attachmentAuthToken = attachmentAuthToken,
            isUploadingImage = isUploadingImage,
            isGeneratingImage = isGeneratingImage,
            onPickReferenceImage = {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveImage = onRemoveImage,
            onGenerate = {
                onGenerateImage(
                    generatePrompt,
                    generationMode == ImageGenerationMode.IMAGE_TO_IMAGE,
                )
            },
        )
    }

    if (generatedImageUrl != null) {
        GeneratedImageDialog(
            imageUrl = generatedImageUrl,
            prompt = generatedImagePrompt,
            onDismiss = onDismissGeneratedImage,
        )
    }
}

@Composable
private fun ImageGenerationDialog(
    mode: ImageGenerationMode,
    onModeChange: (ImageGenerationMode) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    imageAttachments: List<AgentImageAttachmentUi>,
    attachmentAuthToken: String?,
    isUploadingImage: Boolean,
    isGeneratingImage: Boolean,
    onPickReferenceImage: () -> Unit,
    onRemoveImage: (Long) -> Unit,
    onGenerate: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val isImageToImage = mode == ImageGenerationMode.IMAGE_TO_IMAGE
    val canGenerate = prompt.isNotBlank() &&
        !isUploadingImage &&
        !isGeneratingImage &&
        (!isImageToImage || imageAttachments.isNotEmpty())

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                blurRadius = 30.dp,
                shape = RoundedCornerShape(28.dp),
                surfaceColor = Color.White.copy(alpha = 0.96f),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI 生图",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "文字创作，或基于参考图进行重绘",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭生图面板",
                                tint = TextSecondary,
                            )
                        }
                    }

                    SegmentedTabs(
                        tabs = listOf("文生图", "图生图"),
                        selectedIndex = mode.ordinal,
                        onTabSelected = { index ->
                            onModeChange(
                                if (index == ImageGenerationMode.IMAGE_TO_IMAGE.ordinal) {
                                    ImageGenerationMode.IMAGE_TO_IMAGE
                                } else {
                                    ImageGenerationMode.TEXT_TO_IMAGE
                                }
                            )
                        },
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSurfaceLow)
                            .border(1.dp, GlassBorderSoft, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = if (isImageToImage) "图生图" else "文生图",
                                style = MaterialTheme.typography.titleSmall,
                                color = ZhihuijiPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (isImageToImage) {
                                    "上传一张参考图，再描述想要的风格、构图或修改内容。"
                                } else {
                                    "描述主体、风格和画面氛围，生成一张全新图片。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }

                    if (isImageToImage) {
                        if (imageAttachments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(132.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurfaceLow)
                                    .border(1.dp, GlassBorderSoft, RoundedCornerShape(16.dp))
                                    .clickable(enabled = !isUploadingImage, onClick = onPickReferenceImage),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddAPhoto,
                                        contentDescription = null,
                                        tint = ZhihuijiPrimary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Text(
                                        text = if (isUploadingImage) "正在上传参考图..." else "选择参考图",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ZhihuijiPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurfaceLow)
                                    .border(1.dp, GlassBorderSoft, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val reference = imageAttachments.first()
                                AgentInputImageThumbnail(
                                    url = reference.url,
                                    authToken = attachmentAuthToken,
                                    onRemove = { onRemoveImage(reference.assetId) },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "参考图已就绪",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "生成时将使用这张图片",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                    TextButton(
                                        onClick = onPickReferenceImage,
                                        enabled = !isUploadingImage && !isGeneratingImage,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AddAPhoto,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("更换参考图")
                                    }
                                }
                            }
                        }
                    }

                    GlassTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        label = "生成描述",
                        placeholder = if (isImageToImage) {
                            "例如：保留商品主体，改成白底电商海报"
                        } else {
                            "例如：一张清爽的夏季促销商品海报"
                        },
                        singleLine = false,
                        minLines = 4,
                        maxLines = 6,
                        enabled = !isGeneratingImage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    )

                    if (isGeneratingImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                color = ZhihuijiPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "正在生成图片...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismissRequest, enabled = !isGeneratingImage) {
                            Text("取消")
                        }
                        Button(onClick = onGenerate, enabled = canGenerate) {
                            Text(if (isGeneratingImage) "生成中" else "开始生成")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedImageDialog(
    imageUrl: String,
    prompt: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                blurRadius = 30.dp,
                shape = RoundedCornerShape(28.dp),
                surfaceColor = Color.White.copy(alpha = 0.96f),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "生成结果",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "图片已生成",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭生成结果",
                                tint = TextSecondary,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(GlassSurfaceLow)
                            .border(1.dp, GlassBorderSoft, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .build(),
                            contentDescription = "生成图片结果",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    prompt?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = onDismiss) {
                            Text("完成")
                        }
                    }
                }
            }
        }
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
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
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
                text = "想查什么，直接问",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
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
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isBusy = draft.confirmPhase == DraftConfirmPhase.CONFIRMING
    val errorMessage = draft.errorMessage
    AlertDialog(
        onDismissRequest = {
            // 拒绝、关闭弹窗、返回页面不触发正式写入
            if (!isBusy) onDismiss()
        },
        title = {
            Text(
                text = "操作确认",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "是否允许我${draftTypeLabelForChat(draft.draftType)}？",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZhihuijiPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                draft.status?.takeIf { it.isNotBlank() }?.let { status ->
                    Text(
                        text = "草稿状态：${draftStatusLabelForChat(status)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                Text(
                    text = "允许一次将写入当前门店数据，请确认。",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerRed,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBusy,
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("确认中")
                } else {
                    Text("允许一次")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isBusy,
            ) {
                Text("拒绝", color = TextSecondary)
            }
        },
    )
}

private fun draftStatusLabelForChat(status: String): String = when (status.lowercase()) {
    "active" -> "待确认"
    "pending" -> "待确认"
    "confirmed" -> "已确认"
    "cancelled" -> "已取消"
    "archived" -> "已归档"
    else -> status
}

private fun draftTypeLabelForChat(type: String): String = when (type.lowercase()) {
    "sale_order" -> "创建销售单"
    "purchase_order" -> "创建采购单"
    "pay_order" -> "创建付款单"
    "finance_record" -> "记录资金流水"
    "product" -> "新增商品"
    "customer" -> "新增客户"
    "supplier" -> "新增供应商"
    "sales_return" -> "创建销售退货单"
    "purchase_return" -> "创建采购退货单"
    else -> "执行这项操作"
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
        Color(0xFFDCEAFF),
        Color(0xFFD1E2FF),
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
