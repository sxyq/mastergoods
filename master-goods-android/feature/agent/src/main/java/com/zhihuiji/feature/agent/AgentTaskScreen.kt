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
import com.zhihuiji.core.designsystem.EmptyState
import com.zhihuiji.core.designsystem.FilterChipRow
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.PillTone
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        // TODO: v2迁移 - task和notification端点暂未提供，列表将为空
        if (selectedTab == 0) {
            viewModel.loadTasks()
        } else {
            viewModel.loadNotifications(unreadOnly = noticeFilter == 1)
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

                AgentTaskNoticeBridgeCard(selectedTab = selectedTab)

                if (selectedTab == 0) {
                    val taskFilterChips = listOf(
                            "全部(${uiState.tasks.size})",
                            "排队(${uiState.tasks.count { it.status == AgentTaskStatus.QUEUED }})",
                            "进行中(${uiState.tasks.count { it.status == AgentTaskStatus.RUNNING }})",
                            "已完成(${uiState.tasks.count { it.status == AgentTaskStatus.COMPLETED }})",
                            "失败(${uiState.tasks.count { it.status == AgentTaskStatus.FAILED }})",
                    )
                    FilterChipRow(
                        chips = taskFilterChips,
                        selectedIndex = taskFilter,
                        onChipSelected = { taskFilter = it },
                    )

                    val visibleTasks = filteredTasks(uiState.tasks, taskFilter)
                    if (visibleTasks.isEmpty()) {
                        AgentListEmptyState(
                            icon = Icons.Default.HourglassTop,
                            title = "任务列表准备中",
                            subtitle = "当前仍缺少 `/v2/agent` 任务列表端点，页面继续保留完整筛选结构，但不伪造任务进度与执行结果。",
                        )
                    } else {
                        visibleTasks.forEach { task ->
                            AgentTaskCard(task = task)
                        }
                    }
                } else {
                    FilterChipRow(
                        chips = listOf("全部(${uiState.notifications.size})", "未读(${uiState.notifications.count { !it.isRead }})"),
                        selectedIndex = noticeFilter,
                        onChipSelected = { noticeFilter = it },
                    )

                    val visibleNotifications = filteredNotifications(uiState.notifications, noticeFilter)
                    if (visibleNotifications.isEmpty()) {
                        AgentListEmptyState(
                            icon = Icons.Default.Sync,
                            title = "通知列表准备中",
                            subtitle = "当前仍缺少 `/v2/agent` 通知列表端点，页面继续保留通知母版，但不伪造送达状态或消息正文。",
                        )
                    } else {
                        visibleNotifications.forEach { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkRead = { viewModel.markNotificationRead(notification.id) },
                            )
                        }
                    }
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
private fun AgentTaskNoticeBridgeCard(selectedTab: Int) {
    val title = if (selectedTab == 0) "任务中心待联调" else "通知中心待联调"
    val body = if (selectedTab == 0) {
        "当前保留了设计稿对应的 Tab、筛选和状态卡布局；只有在服务端返回真实任务后，才展示进度条、耗时和执行结果。"
    } else {
        "当前保留了通知列表母版与已读筛选；只有在服务端返回真实通知后，才展示送达状态、已读状态和通知内容。"
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
            Text(body, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
            StatusPill(text = "待联调", tone = PillTone.INFO)
        }
    }
}

@Composable
private fun AgentListEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        EmptyState(
            icon = icon,
            title = title,
            subtitle = subtitle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
        )
    }
}

@Composable
private fun AgentTaskCard(task: AgentTaskSummaryDto) {
    val (icon, iconColor, statusText, statusTone) = taskVisuals(task.status)
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
                        Text("数据范围：按任务实际输入决定，当前页不预设固定单据范围", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    }
                }
                StatusPill(text = statusText, tone = statusTone)
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
                StatusPill(
                    text = if (notification.isRead) "已读" else "未读",
                    tone = if (notification.isRead) PillTone.SUCCESS else PillTone.INFO,
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

private fun taskVisuals(status: AgentTaskStatus): Quad<ImageVector, androidx.compose.ui.graphics.Color, String, PillTone> = when (status) {
    AgentTaskStatus.QUEUED -> Quad(Icons.Default.HourglassTop, ZhihuijiColors.Warning, StatusLabels.agentTaskStatus(status), PillTone.WARNING)
    AgentTaskStatus.RUNNING -> Quad(Icons.Default.Sync, ZhihuijiColors.Primary, StatusLabels.agentTaskStatus(status), PillTone.INFO)
    AgentTaskStatus.COMPLETED -> Quad(Icons.Default.CheckCircle, ZhihuijiColors.Success, StatusLabels.agentTaskStatus(status), PillTone.SUCCESS)
    AgentTaskStatus.FAILED -> Quad(Icons.Default.ErrorOutline, ZhihuijiColors.Danger, StatusLabels.agentTaskStatus(status), PillTone.DANGER)
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

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
