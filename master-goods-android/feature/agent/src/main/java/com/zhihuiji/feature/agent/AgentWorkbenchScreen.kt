package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.MainBottomBarHeight
import com.zhihuiji.core.designsystem.SurfaceWhite
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryDark

@Composable
fun AgentWorkbenchScreen(
    onNavigateToChat: (initialQuestion: String?) -> Unit,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgentWorkbenchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarSubtitle = when {
        uiState.isLoading -> "正在同步 Agent 入口"
        uiState.error != null -> "远端未同步 · 仅保留对话入口"
        uiState.isRemoteSynced -> "服务端已连接 · 等待提问"
        else -> "等待远端 Agent 状态"
    }

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "AI 助手",
                subtitle = topBarSubtitle,
                actions = {
                    Row {
                        IconButton(onClick = onNavigateToTasks) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "任务与通知",
                                tint = TextSecondary
                            )
                        }
                        IconButton(onClick = { onNavigateToChat(null) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "新建对话",
                                tint = ZhihuijiPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 96.dp,
                end = 16.dp,
                bottom = MainBottomBarHeight + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                AgentEntryHero(
                    greeting = uiState.greeting,
                    isLoading = uiState.isLoading,
                    isRemoteSynced = uiState.isRemoteSynced,
                    error = uiState.error,
                    onStartChat = { onNavigateToChat(null) },
                )
            }
        }
    }
}

@Composable
private fun AgentEntryHero(
    greeting: String,
    isLoading: Boolean,
    isRemoteSynced: Boolean,
    error: String?,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onStartChat,
        shape = RoundedCornerShape(28.dp),
        surfaceColor = ZhihuijiPrimary.copy(alpha = 0.18f),
        contentPadding = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(AgentHeroBrush)
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = SurfaceWhite,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = SurfaceWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (isRemoteSynced) {
                                "主屏保持干净。发送问题后由服务端创建真实 run，返回工具事件、Markdown 回复和结构化结果；无法连接时会明确提示失败。"
                            } else {
                                "主屏保持干净。当前仅保留对话入口；发送问题后会连接服务端，若远端不可用会明确提示失败原因。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SurfaceWhite.copy(alpha = 0.78f),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "开始一次真实 Agent 对话",
                        style = MaterialTheme.typography.labelLarge,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                AgentWorkbenchSyncStatus(
                    isLoading = isLoading,
                    isRemoteSynced = isRemoteSynced,
                    error = error,
                )
            }
        }
    }
}

@Composable
private fun AgentWorkbenchSyncStatus(
    isLoading: Boolean,
    isRemoteSynced: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val text = when {
        isLoading -> "正在同步 Agent 入口"
        error != null -> "远端工作台未同步，仅保留对话入口"
        isRemoteSynced -> "已同步远端 Agent 状态"
        else -> "等待远端 Agent 状态"
    }
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = SurfaceWhite.copy(alpha = if (error == null) 0.76f else 0.88f),
        fontWeight = FontWeight.SemiBold,
    )
}

private val AgentHeroBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF083C4A),
        ZhihuijiPrimaryDark,
        Color(0xFF0EA5A4),
    )
)
