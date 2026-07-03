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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
                subtitle = "仅归档 AI 草稿，不执行业务写入",
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
                                isArchiving = uiState.isArchiving
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftCard(
    draft: DraftItem,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    isArchiving: Boolean,
    modifier: Modifier = Modifier,
) {
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
                    text = if (isArchiving) "归档中..." else "仅归档",
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
    "pending" -> StatusType.PENDING
    "confirmed" -> StatusType.NORMAL
    "archived" -> StatusType.ARCHIVED
    "deleted" -> StatusType.CANCELLED
    else -> StatusType.PENDING
}
