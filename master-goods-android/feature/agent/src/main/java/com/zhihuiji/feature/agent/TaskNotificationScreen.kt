package com.zhihuiji.feature.agent

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tabs = listOf("任务", "通知")

@Composable
fun TaskNotificationScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    viewModel: TaskNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialTab) {
        viewModel.selectTab(initialTab.coerceIn(tabs.indices))
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "任务与通知",
                subtitle = "查看服务端返回的任务与通知",
                onNavigationClick = onBackClick,
                actions = {
                    if (uiState.selectedTab == 1 && uiState.notifications.any { !it.isRead }) {
                        TextButton(onClick = viewModel::markAllNotificationsRead) {
                            Text("全部已读")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SegmentedTabs(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                tabs = tabs,
                selectedIndex = uiState.selectedTab,
                onTabSelected = viewModel::selectTab
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZhihuijiPrimary)
                    }
                }

                uiState.error != null -> {
                    EmptyState(message = "加载失败：${uiState.error}")
                }

                uiState.selectedTab == 0 && uiState.tasks.isEmpty() -> {
                    EmptyState(message = "暂无任务")
                }

                uiState.selectedTab == 1 && uiState.notifications.isEmpty() -> {
                    EmptyState(message = "暂无通知")
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (uiState.selectedTab) {
                            0 -> {
                                items(
                                    items = uiState.tasks,
                                    key = { "task_${it.id}" }
                                ) { task ->
                                    TaskCard(task = task)
                                }
                            }

                            1 -> {
                                items(
                                    items = uiState.notifications,
                                    key = { "notif_${it.id}" }
                                ) { notification ->
                                    NotificationCard(
                                        notification = notification,
                                        onClick = {
                                            if (!notification.isRead) {
                                                viewModel.markNotificationRead(notification.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskItem,
    modifier: Modifier = Modifier,
) {
    val statusColor = task.status.taskAccentColor()
    val statusType = task.status.taskStatusType()

    LiquidGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Task,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
                StatusPill(
                    text = task.statusLabel,
                    status = statusType
                )
            }

            if (task.progress in 1..99) {
                Column {
                    LinearProgressIndicator(
                        progress = { task.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ZhihuijiPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${task.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "创建于 ${formatTime(task.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

private fun String.taskAccentColor(): androidx.compose.ui.graphics.Color =
    when (lowercase(Locale.ROOT)) {
        "failed", "error" -> DangerRed
        "cancelled", "canceled", "archived" -> TextTertiary
        "completed", "done", "success" -> SuccessGreen
        "running", "processing", "pending" -> WarningOrange
        else -> ZhihuijiPrimary
    }

private fun String.taskStatusType(): StatusType =
    when (lowercase(Locale.ROOT)) {
        "failed", "error" -> StatusType.OUT_OF_STOCK
        "cancelled", "canceled" -> StatusType.CANCELLED
        "archived" -> StatusType.ARCHIVED
        "completed", "done", "success" -> StatusType.COMPLETED
        "running", "processing", "pending" -> StatusType.PENDING
        else -> StatusType.NORMAL
    }

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeIcon = when (notification.type) {
        "order" -> Icons.Default.CheckCircle
        "stock" -> Icons.Default.Notifications
        else -> Icons.Default.Notifications
    }

    val typeColor = when (notification.type) {
        "order" -> SuccessGreen
        "stock" -> WarningOrange
        else -> ZhihuijiPrimary
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ZhihuijiPrimary)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
