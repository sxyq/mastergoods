package com.zhihuiji.feature.agent

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.designsystem.DangerOutlineButton
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.StatusType
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

private val tabs = listOf("全部", "销售", "采购", "其他")

@Composable
fun DraftListScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DraftListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopBar(
                title = "操作草稿",
                subtitle = "默认仅生成草稿；确认后执行写入，取消后记录已取消状态",
                onNavigationClick = onBackClick
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

                uiState.drafts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无草稿",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }

                else -> {
                    val filteredDrafts = when (uiState.selectedTab) {
                        1 -> uiState.drafts.filter { it.typeLabel == "销售" }
                        2 -> uiState.drafts.filter { it.typeLabel == "采购" }
                        3 -> uiState.drafts.filter { it.typeLabel !in listOf("销售", "采购") }
                        else -> uiState.drafts
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredDrafts,
                            key = { it.id }
                        ) { draft ->
                            DraftCard(
                                draft = draft,
                                onArchive = { viewModel.archiveDraft(draft.id) },
                                onDelete = { viewModel.deleteDraft(draft.id) },
                                onConfirm = { viewModel.requestConfirmDraft(draft.id) },
                                onCancel = { viewModel.requestCancelDraft(draft.id) },
                                isArchiving = uiState.isArchiving,
                            )
                        }
                    }
                }
            }
        }
    }

    // 确认/取消二次确认对话框 —— 必须用户明确点击才会执行真实写入或记录取消状态
    val pendingDraft = uiState.pendingActionDraftId?.let { id ->
        uiState.drafts.firstOrNull { it.id == id }
    }
    when (uiState.pendingActionType) {
        DraftActionType.CONFIRM -> pendingDraft?.let { draft ->
            AlertDialog(
                onDismissRequest = viewModel::dismissPendingAction,
                title = { Text("确认执行草稿？") },
                text = {
                    Text(
                        "确认后将执行写入，无法撤销。\n\n" +
                            "草稿：${draft.title}\n" +
                            "类型：${draft.typeLabel}\n" +
                            "往来方：${draft.partyName}\n" +
                            "金额：${draft.amountText}"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::executeConfirmDraft,
                        enabled = !uiState.isArchiving,
                    ) {
                        Text(if (uiState.isArchiving) "执行中..." else "确认执行")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::dismissPendingAction,
                        enabled = !uiState.isArchiving,
                    ) {
                        Text("再想想")
                    }
                },
            )
        }
        DraftActionType.CANCEL -> pendingDraft?.let { draft ->
            AlertDialog(
                onDismissRequest = viewModel::dismissPendingAction,
                title = { Text("取消该草稿？") },
                text = {
                    Text(
                        "取消后草稿状态将记录为「已取消」，不会执行业务写入。\n\n" +
                            "草稿：${draft.title}\n" +
                            "类型：${draft.typeLabel}"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::executeCancelDraft,
                        enabled = !uiState.isArchiving,
                    ) {
                        Text(if (uiState.isArchiving) "执行中..." else "确认取消")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::dismissPendingAction,
                        enabled = !uiState.isArchiving,
                    ) {
                        Text("再想想")
                    }
                },
            )
        }
        null -> Unit
    }
}

@Composable
private fun DraftCard(
    draft: DraftItem,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isArchiving: Boolean,
    modifier: Modifier = Modifier,
) {
    val isActive = draft.status == "active"
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
                Text(
                    text = draft.typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = ZhihuijiPrimary
                )
                StatusPill(
                    text = draft.statusLabel,
                    status = draft.status.toStatusType()
                )
            }

            Text(
                text = draft.title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DraftMetaColumn(label = "编号", value = draft.businessNo, modifier = Modifier.weight(1f))
                DraftMetaColumn(label = "往来方", value = draft.partyName, modifier = Modifier.weight(1f))
                DraftMetaColumn(label = "金额", value = draft.amountText, modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "创建于 ${TimeFormatter.formatDateTime(draft.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (isActive) {
                // active 状态：显示 确认执行 / 取消 两个主操作
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DangerOutlineButton(
                        modifier = Modifier.weight(1f),
                        text = "取消草稿",
                        onClick = onCancel,
                        enabled = !isArchiving
                    )
                    PrimaryButton(
                        modifier = Modifier.weight(1f),
                        text = if (isArchiving) "执行中..." else "确认执行",
                        onClick = onConfirm,
                        enabled = !isArchiving
                    )
                }
            }

            // 归档/删除作为次要操作（仅清理草稿本身，不触发业务写入）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DangerOutlineButton(
                    modifier = Modifier.weight(1f),
                    text = "删除",
                    onClick = onDelete,
                    enabled = !isArchiving
                )
                PrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = if (isArchiving) "处理中..." else "仅归档",
                    onClick = onArchive,
                    enabled = !isArchiving
                )
            }
        }
    }
}

@Composable
private fun DraftMetaColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun String.toStatusType(): StatusType = when (this) {
    "active" -> StatusType.PENDING
    "pending" -> StatusType.PENDING
    "confirmed" -> StatusType.NORMAL
    "cancelled" -> StatusType.CANCELLED
    "archived" -> StatusType.ARCHIVED
    "deleted" -> StatusType.CANCELLED
    else -> StatusType.PENDING
}
