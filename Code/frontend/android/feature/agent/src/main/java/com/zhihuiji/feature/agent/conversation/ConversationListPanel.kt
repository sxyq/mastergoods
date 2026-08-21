package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.model.v2.agent.AgentConversationDto

private val ConversationDrawerShape = RoundedCornerShape(20.dp)

@Composable
fun ConversationListPanel(
    conversations: List<AgentConversationDto>,
    currentConversationId: Long?,
    isLoading: Boolean,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onSwitch: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCreateNew: () -> Unit,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, conversations.size, hasMore, isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisibleIndex ->
                if (
                    hasMore &&
                    !isLoadingMore &&
                    conversations.isNotEmpty() &&
                    lastVisibleIndex >= conversations.lastIndex - 1
                ) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "会话列表",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = onCreateNew,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ZhihuijiPrimary.copy(alpha = 0.10f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建对话",
                    tint = ZhihuijiPrimary,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when {
            isLoading && conversations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = ZhihuijiPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            conversations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无历史会话\n点击右上角新建对话",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = conversations,
                        key = { _, item -> item.id },
                        contentType = { _, _ -> "conversation" },
                    ) { _, conversation ->
                        ConversationRow(
                            conversation = conversation,
                            isActive = conversation.id == currentConversationId,
                            onSwitch = onSwitch,
                            onDelete = onDelete,
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "conversation-pagination") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = ZhihuijiPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: AgentConversationDto,
    isActive: Boolean,
    onSwitch: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isActive) {
        ZhihuijiPrimary.copy(alpha = 0.08f)
    } else {
        GlassSurfaceHigh
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ConversationDrawerShape)
            .background(containerColor)
            .clickable(onClick = { onSwitch(conversation.id) })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) ZhihuijiPrimary.copy(alpha = 0.16f)
                    else TextTertiary.copy(alpha = 0.10f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isActive) ZhihuijiPrimary else TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = conversation.title.ifBlank { "未命名对话" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) ZhihuijiPrimary else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val summary = conversation.latestSummary
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val timeLabel = remember(
                conversation.id,
                conversation.lastMessageAt,
                conversation.updatedAt,
                conversation.createdAt,
            ) {
                TimeFormatter.formatDateTime(
                    conversation.lastMessageAt
                        ?: conversation.updatedAt.takeIf { it > 0 }
                        ?: conversation.createdAt
                )
            }
            if (timeLabel.isNotBlank() && timeLabel != "-") {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }
        IconButton(
            onClick = { onDelete(conversation.id) },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "删除会话",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
