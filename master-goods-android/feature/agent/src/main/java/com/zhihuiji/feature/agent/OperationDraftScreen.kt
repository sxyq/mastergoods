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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.agent.AgentDraftDto
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.EmptyState
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.PillTone
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OperationDraftScreen(
    onNavigateBack: () -> Unit,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var instruction by rememberSaveable { mutableStateOf("") }

    // v2: 加载草稿列表
    LaunchedEffect(Unit) { viewModel.loadDrafts() }

    val allDrafts = buildList {
        uiState.draft?.let { currentDraft ->
            add(currentDraft)
        }
        uiState.drafts.forEach { draft ->
            if (draft.id != uiState.draft?.id) {
                add(draft)
            }
        }
    }.sortedByDescending { it.createdAt }
    val saleCount = allDrafts.count { agentDraftCategoryKeyOf(it.draftType) == AGENT_DRAFT_CATEGORY_SALE }
    val purchaseCount = allDrafts.count { agentDraftCategoryKeyOf(it.draftType) == AGENT_DRAFT_CATEGORY_PURCHASE }
    val otherCount = allDrafts.size - saleCount - purchaseCount
    val tabLabels = listOf(
        "全部(${allDrafts.size})",
        "销售($saleCount)",
        "采购($purchaseCount)",
        "其他($otherCount)",
    )
    val filteredDrafts = when (selectedTab) {
        1 -> allDrafts.filter { agentDraftCategoryKeyOf(it.draftType) == AGENT_DRAFT_CATEGORY_SALE }
        2 -> allDrafts.filter { agentDraftCategoryKeyOf(it.draftType) == AGENT_DRAFT_CATEGORY_PURCHASE }
        3 -> allDrafts.filter { agentDraftCategoryKeyOf(it.draftType) == AGENT_DRAFT_CATEGORY_OTHER }
        else -> allDrafts
    }
    val currentDraft = uiState.draft
    val canSubmitCurrentDraft = currentDraft?.status == "draft"
    val primaryActionText = if (canSubmitCurrentDraft) "提交草稿" else "生成草稿"

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
                title = "操作草稿",
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
                    tabs = tabLabels,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "草稿按本地可识别的销售/采购/其他口径分类。当前只基于指令关键词做本地归类，不伪造后端智能识别。",
                            style = ZhihuijiTypography.bodySmall,
                            color = ZhihuijiColors.TextSecondary,
                        )

                        OutlinedTextField(
                            value = instruction,
                            onValueChange = { instruction = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("输入一段指令，例如：帮我生成一张给李想商贸的销售单") },
                            shape = RoundedCornerShape(14.dp),
                        )

                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("草稿列表", style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                    Text("共 ${filteredDrafts.size} 条", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.TextSecondary)
                }

                filteredDrafts.forEach { draft ->
                    DraftCard(
                        draft = draft,
                        showPrimaryButton = draft.id == uiState.draft?.id,
                        onPrimaryAction = { viewModel.submitDraft() },
                    )
                }

                if (filteredDrafts.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Default.Description,
                            title = if (allDrafts.isEmpty()) "还没有真实草稿" else "当前分类下没有草稿",
                            subtitle = if (allDrafts.isEmpty()) {
                                "先输入操作意图生成一条真实草稿。当前页不再展示伪造编号或示例单据，避免把示例内容误看成已生成结果。"
                            } else {
                                "切换分类或继续生成真实草稿。仅在返回真实数据时展示对应条目。"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                        )
                    }
                }

                uiState.submittedDraftResult?.let { message ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("提交结果", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                            Text(message, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            BottomActionBar(
                primaryAction = {
                    PrimaryButton(
                        text = primaryActionText,
                        onClick = {
                            if (canSubmitCurrentDraft) {
                                viewModel.submitDraft()
                            } else {
                                viewModel.generateOperationDraft(instruction)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = if (canSubmitCurrentDraft) true else instruction.isNotBlank(),
                    )
                },
                secondaryActions = listOf(
                    {
                        SecondaryOutlineButton(
                            text = "刷新草稿",
                            onClick = { viewModel.loadDrafts() },
                        )
                    },
                ),
            )
        }
    }
}

@Composable
private fun DraftCard(
    draft: AgentDraftDto,
    showPrimaryButton: Boolean,
    onPrimaryAction: () -> Unit,
) {
    val accent = accentColorForDraftType(draft.draftType)
    val icon = iconForDraftType(draft.draftType)
    val displayTitle = draft.title.ifBlank { titleForDraftType(draft.draftType) }
    val statusLabel = draftStatusLabel(draft.status)
    val statusTone = draftStatusTone(draft.status)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(displayTitle, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        StatusPill(text = statusLabel, tone = statusTone)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DraftMetaRow("草稿类型", titleForDraftType(draft.draftType))
                DraftMetaRow("状态", statusLabel)
                DraftMetaRow("创建时间", TimeFormatter.formatDateTime(draft.createdAt))
                DraftMetaRow("更新时间", TimeFormatter.formatDateTime(draft.updatedAt))
                DraftMetaRow("内容状态", draftContentSummary(draft.contentJson))
                DraftMetaRow("数据来源", "当前展示真实返回的标题、类型与时间；明细字段会在可用后继续补充")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (showPrimaryButton) {
                    PrimaryButton(text = "提交", onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth(), enabled = draft.status == "draft")
                }
            }
        }
    }
}

@Composable
private fun DraftMetaRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
        Text(value, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}

private fun titleForDraftType(draftType: String): String = when {
    agentDraftCategoryKeyOf(draftType) == AGENT_DRAFT_CATEGORY_SALE -> "销售单草稿"
    agentDraftCategoryKeyOf(draftType) == AGENT_DRAFT_CATEGORY_PURCHASE -> "采购单草稿"
    draftType.contains("return", ignoreCase = true) -> "退货单草稿"
    draftType.equals("operation", ignoreCase = true) -> "操作草稿"
    else -> "草稿"
}

private fun accentColorForDraftType(draftType: String): androidx.compose.ui.graphics.Color = when {
    agentDraftCategoryKeyOf(draftType) == AGENT_DRAFT_CATEGORY_SALE -> ZhihuijiColors.Primary
    agentDraftCategoryKeyOf(draftType) == AGENT_DRAFT_CATEGORY_PURCHASE -> ZhihuijiColors.Success
    draftType.contains("return", ignoreCase = true) -> ZhihuijiColors.Warning
    else -> ZhihuijiColors.Primary
}

private fun iconForDraftType(draftType: String) = when {
    agentDraftCategoryKeyOf(draftType) == AGENT_DRAFT_CATEGORY_SALE -> Icons.Default.Description
    agentDraftCategoryKeyOf(draftType) == AGENT_DRAFT_CATEGORY_PURCHASE -> Icons.Default.ShoppingCart
    draftType.contains("return", ignoreCase = true) -> Icons.Default.SyncAlt
    else -> Icons.Default.Description
}

private fun draftStatusLabel(status: String): String = when (status) {
    "draft" -> "草稿"
    "submitted" -> "已提交"
    "approved" -> "已审核"
    "rejected" -> "已驳回"
    else -> status
}

private fun draftStatusTone(status: String): PillTone = when (status) {
    "draft" -> PillTone.INFO
    "submitted" -> PillTone.WARNING
    "approved" -> PillTone.SUCCESS
    "rejected" -> PillTone.DANGER
    else -> PillTone.NEUTRAL
}

private fun draftContentSummary(contentJson: String): String = when {
    contentJson.isBlank() -> "暂无内容"
    contentJson == "{}" -> "暂未生成可解析明细"
    else -> "已返回内容，后续可继续补充字段级展示"
}
