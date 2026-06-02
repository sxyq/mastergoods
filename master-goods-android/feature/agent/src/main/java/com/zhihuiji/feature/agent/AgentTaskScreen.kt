package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.AgentNotificationDto
import com.zhihuiji.core.model.AgentTaskStatus
import com.zhihuiji.core.model.AgentTaskSummaryDto
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AgentTaskScreen(
    onNavigateBack: () -> Unit,
    initialTab: Int = 0,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedTab by rememberSaveable { mutableStateOf(initialTab.coerceIn(0, 1)) }
    var taskFilter by rememberSaveable { mutableStateOf(0) }
    var noticeFilter by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(selectedTab, taskFilter, noticeFilter) {
        if (selectedTab == 0) {
            viewModel.loadTasks()
        } else {
            viewModel.loadNotifications(unreadOnly = noticeFilter == 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        GlassTopBar(
            title = "任务与通知",
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
            SegmentedTabs(
                tabs = listOf("任务", "通知"),
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
            )

            if (selectedTab == 0) {
                SegmentedTabs(
                    tabs = listOf(
                        "全部(${uiState.tasks.size})",
                        "排队(${uiState.tasks.count { it.status == AgentTaskStatus.QUEUED }})",
                        "进行中(${uiState.tasks.count { it.status == AgentTaskStatus.RUNNING }})",
                        "已完成(${uiState.tasks.count { it.status == AgentTaskStatus.COMPLETED }})",
                        "失败(${uiState.tasks.count { it.status == AgentTaskStatus.FAILED }})",
                    ),
                    selectedIndex = taskFilter,
                    onTabSelected = { taskFilter = it },
                )

                filteredTasks(uiState.tasks, taskFilter).forEach { task ->
                    AgentTaskCard(task = task)
                }
            } else {
                SegmentedTabs(
                    tabs = listOf("全部(${uiState.notifications.size})", "未读(${uiState.notifications.count { !it.isRead }})"),
                    selectedIndex = noticeFilter,
                    onTabSelected = { noticeFilter = it },
                )

                filteredNotifications(uiState.notifications, noticeFilter).forEach { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkRead = { viewModel.markNotificationRead(notification.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    AgentTaskScreen(
        onNavigateBack = onNavigateBack,
        initialTab = 1,
        viewModel = viewModel,
    )
}

@Composable
private fun AgentTaskCard(task: AgentTaskSummaryDto) {
    val (icon, iconColor, statusText, statusColor) = taskVisuals(task.status)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(iconColor.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(task.title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        Text("数据范围：销售单、收款单、库存单", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    }
                }
                StatusChip(text = statusText, color = statusColor)
            }

            LinearProgressIndicator(
                progress = { (task.progress.coerceIn(0, 100) / 100f) },
                modifier = Modifier.fillMaxWidth(),
                color = ZhihuijiColors.Primary,
                trackColor = ZhihuijiColors.BorderLight,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("创建时间：${TimeFormatter.formatTime(task.createdAt)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                Text("进度 ${task.progress}%", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("任务类型：${taskTypeLabel(task.taskType)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                Text(taskDurationLabel(task), style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
            }
            Text(
                when (task.status) {
                    AgentTaskStatus.QUEUED -> "等待空闲工作线程开始处理"
                    AgentTaskStatus.RUNNING -> "正在整理数据并生成阶段性结果"
                    AgentTaskStatus.COMPLETED -> "任务已完成，可进入详情查看最终结果"
                    AgentTaskStatus.FAILED -> "执行失败，请检查输入或重试"
                },
                style = ZhihuijiTypography.labelSmall,
                color = ZhihuijiColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AgentNotificationDto,
    onMarkRead: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(notificationAccent(notification).copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(notificationIcon(notification), contentDescription = null, tint = notificationAccent(notification), modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notification.title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(notification.content, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    }
                }
                StatusChip(
                    text = if (notification.isRead) "已读" else "未读",
                    color = if (notification.isRead) ZhihuijiColors.Success else ZhihuijiColors.Primary,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("时间：${TimeFormatter.formatTime(notification.createdAt)}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                if (!notification.isRead) {
                    SecondaryOutlineButton(text = "标为已读", onClick = onMarkRead)
                }
            }
            Text(
                notificationDeliveryLabel(notification),
                style = ZhihuijiTypography.labelSmall,
                color = if (notification.isDelivered) ZhihuijiColors.Success else ZhihuijiColors.Warning,
            )
        }
    }
}

private fun filteredTasks(
    tasks: List<AgentTaskSummaryDto>,
    filter: Int,
): List<AgentTaskSummaryDto> = when (filter) {
    1 -> tasks.filter { it.status == AgentTaskStatus.QUEUED }
    2 -> tasks.filter { it.status == AgentTaskStatus.RUNNING }
    3 -> tasks.filter { it.status == AgentTaskStatus.COMPLETED }
    4 -> tasks.filter { it.status == AgentTaskStatus.FAILED }
    else -> tasks
}

private fun filteredNotifications(
    notifications: List<AgentNotificationDto>,
    filter: Int,
): List<AgentNotificationDto> = when (filter) {
    1 -> notifications.filter { !it.isRead }
    else -> notifications
}

private fun taskVisuals(status: AgentTaskStatus): Quad<ImageVector, androidx.compose.ui.graphics.Color, String, androidx.compose.ui.graphics.Color> = when (status) {
    AgentTaskStatus.QUEUED -> Quad(Icons.Default.HourglassTop, ZhihuijiColors.Warning, StatusLabels.agentTaskStatus(status), ZhihuijiColors.Warning)
    AgentTaskStatus.RUNNING -> Quad(Icons.Default.Sync, ZhihuijiColors.Primary, StatusLabels.agentTaskStatus(status), ZhihuijiColors.Primary)
    AgentTaskStatus.COMPLETED -> Quad(Icons.Default.CheckCircle, ZhihuijiColors.Success, StatusLabels.agentTaskStatus(status), ZhihuijiColors.Success)
    AgentTaskStatus.FAILED -> Quad(Icons.Default.ErrorOutline, ZhihuijiColors.Danger, StatusLabels.agentTaskStatus(status), ZhihuijiColors.Danger)
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

@Composable
private fun StatusChip(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = ZhihuijiTypography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun notificationAccent(notification: AgentNotificationDto): Color = when {
    notification.title.contains("失败") -> ZhihuijiColors.Danger
    notification.title.contains("完成") || notification.title.contains("成功") -> ZhihuijiColors.Success
    notification.title.contains("同步") || notification.title.contains("生成") -> ZhihuijiColors.Primary
    else -> ZhihuijiColors.Warning
}

private fun notificationIcon(notification: AgentNotificationDto): ImageVector = when {
    notification.title.contains("失败") -> Icons.Default.ErrorOutline
    notification.title.contains("完成") || notification.title.contains("成功") -> Icons.Default.CheckCircle
    notification.title.contains("同步") || notification.title.contains("生成") -> Icons.Default.Sync
    else -> Icons.Default.HourglassTop
}

private fun taskTypeLabel(taskType: String): String = when {
    taskType.contains("sales", ignoreCase = true) -> "销售分析"
    taskType.contains("purchase", ignoreCase = true) -> "采购建议"
    taskType.contains("sync", ignoreCase = true) -> "数据同步"
    taskType.contains("backup", ignoreCase = true) -> "账套备份"
    else -> "智能任务"
}

private fun taskDurationLabel(task: AgentTaskSummaryDto): String = when {
    task.status == AgentTaskStatus.COMPLETED && task.completedAt != null -> {
        val completedAt = task.completedAt ?: task.createdAt
        "耗时 ${((completedAt - task.createdAt) / 1000L).coerceAtLeast(1)}秒"
    }
    task.status == AgentTaskStatus.RUNNING -> "持续处理中"
    task.status == AgentTaskStatus.FAILED -> "等待重试"
    else -> "等待执行"
}

private fun notificationDeliveryLabel(notification: AgentNotificationDto): String = when {
    notification.isDelivered && notification.isRead -> "已送达并已阅读"
    notification.isDelivered -> "已送达到设备，等待处理"
    else -> "通知发送中，请稍后查看"
}
