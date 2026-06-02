package com.zhihuiji.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.OperationDraftDto
import com.zhihuiji.core.model.OperationType
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.SegmentedTabs
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperationDraftScreen(
    onNavigateBack: () -> Unit,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var onlyMine by rememberSaveable { mutableStateOf(true) }
    var instruction by rememberSaveable { mutableStateOf("") }
    val tabLabels = listOf("全部(6)", "销售(3)", "采购(2)", "其他(1)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = onlyMine, onCheckedChange = { onlyMine = it })
                            Text("仅看我创建", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        }
                        Text("创建时间 ↓", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.TextSecondary)
                    }

                    OutlinedTextField(
                        value = instruction,
                        onValueChange = { instruction = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入一段指令，例如：帮我生成一张给李想商贸的销售单") },
                        shape = RoundedCornerShape(14.dp),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryOutlineButton(
                            text = "生成草稿",
                            onClick = { viewModel.generateOperationDraft(instruction) },
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = "提交草稿",
                            onClick = { viewModel.submitDraft() },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.draft?.canSubmit == true,
                        )
                    }
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
                Text("共 3 条待处理", style = ZhihuijiTypography.labelMedium, color = ZhihuijiColors.TextSecondary)
            }

            uiState.draft?.let { draft ->
                DraftCard(
                    title = titleForDraft(draft.operationType),
                    draft = draft,
                    showPrimaryButton = true,
                    onPrimaryAction = { viewModel.submitDraft() },
                )
            }

            DraftPlaceholderCard(
                title = "销售单草稿",
                summary = "根据你的近期对话，推荐补一张给李想商贸的销售单。",
                accent = ZhihuijiColors.Primary,
                meta = "草稿编号  XS-20250516-001",
            )
            DraftPlaceholderCard(
                title = "采购单草稿",
                summary = "根据低库存预警，推荐补货洗衣液与纸巾类商品。",
                accent = ZhihuijiColors.Success,
                meta = "草稿编号  CG-20250516-001",
            )
            DraftPlaceholderCard(
                title = "收款草稿",
                summary = "发现一笔逾期应收款，建议生成收款跟进草稿。",
                accent = ZhihuijiColors.Warning,
                meta = "草稿编号  SK-20250515-001",
            )

            uiState.submittedDraftResult?.let { result ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("提交结果", style = ZhihuijiTypography.titleSmall, color = ZhihuijiColors.TextPrimary)
                        Text(result.message.ifBlank { "草稿已提交，等待下一步处理。" }, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                        Text("下一步：${result.nextAction.ifBlank { "查看对应单据" }}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftCard(
    title: String,
    draft: OperationDraftDto,
    showPrimaryButton: Boolean,
    onPrimaryAction: () -> Unit,
) {
    val totalAmount = draft.items.sumOf { it.amount }
    val accent = accentColorForDraft(draft.operationType)
    val icon = iconForDraft(draft.operationType)
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
                        Text(title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        Text(draft.summary.ifBlank { "待补充摘要" }, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    }
                }
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = ZhihuijiColors.TextSecondary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DraftMetaRow("草稿编号", draftCodeFor(draft))
                DraftMetaRow("往来方", draft.partnerName.ifBlank { "-" })
                DraftMetaRow("角色", draft.partnerRole.ifBlank { "-" })
                DraftMetaRow("商品数", "${draft.items.size}")
                DraftMetaRow("金额(元)", MoneyFormatter.formatWithoutSymbol(totalAmount))
                DraftMetaRow("创建时间", draftCreatedTimeFor(draft))
            }

            if (draft.warnings.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.warnings.forEach { warning ->
                        Text(
                            text = warning,
                            style = ZhihuijiTypography.labelSmall,
                            color = ZhihuijiColors.Warning,
                        )
                    }
                }
            }

            draft.items.take(3).forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productName.ifBlank { item.productCode }, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
                        Text("¥${item.unitPrice} × ${item.quantity}", style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                    }
                    Text(MoneyFormatter.format(item.amount), style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryOutlineButton(text = "编辑", onClick = {}, modifier = Modifier.weight(1f))
                if (showPrimaryButton) {
                    PrimaryButton(text = "提交", onClick = onPrimaryAction, modifier = Modifier.weight(1f), enabled = draft.canSubmit)
                }
            }
        }
    }
}

@Composable
private fun DraftPlaceholderCard(
    title: String,
    summary: String,
    accent: androidx.compose.ui.graphics.Color,
    meta: String,
) {
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
                        Icon(Icons.Default.Description, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(title, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                        Text(meta, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
                    }
                }
                Icon(Icons.Default.Refresh, contentDescription = null, tint = ZhihuijiColors.TextSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DraftMetaRow("草稿编号", meta)
                DraftMetaRow("往来方", placeholderPartnerFor(title))
                DraftMetaRow("商品数", placeholderItemCountFor(title))
                DraftMetaRow("金额(元)", placeholderAmountFor(title))
                DraftMetaRow("创建时间", placeholderTimeFor(title))
            }
            Text(summary, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryOutlineButton(text = "编辑", onClick = {}, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = if (title.contains("收款")) "新建草稿" else "提交",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
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

private fun titleForDraft(type: OperationType): String = when (type) {
    OperationType.SALE -> "销售单草稿"
    OperationType.PURCHASE -> "采购单草稿"
    OperationType.RETURN -> "退货单草稿"
}

private fun accentColorForDraft(type: OperationType): androidx.compose.ui.graphics.Color = when (type) {
    OperationType.SALE -> ZhihuijiColors.Primary
    OperationType.PURCHASE -> ZhihuijiColors.Success
    OperationType.RETURN -> ZhihuijiColors.Warning
}

private fun iconForDraft(type: OperationType) = when (type) {
    OperationType.SALE -> Icons.Default.Description
    OperationType.PURCHASE -> Icons.Default.ShoppingCart
    OperationType.RETURN -> Icons.Default.SyncAlt
}

private fun draftCodeFor(draft: OperationDraftDto): String {
    val prefix = when (draft.operationType) {
        OperationType.SALE -> "XS"
        OperationType.PURCHASE -> "CG"
        OperationType.RETURN -> "TH"
    }
    val partnerPart = (draft.partnerId ?: draft.items.size.toLong()).toString().padStart(3, '0')
    return "$prefix-20250516-$partnerPart"
}

private fun draftCreatedTimeFor(draft: OperationDraftDto): String = when (draft.operationType) {
    OperationType.SALE -> "2025-05-16 10:28"
    OperationType.PURCHASE -> "2025-05-16 09:48"
    OperationType.RETURN -> TimeFormatter.formatDateTime(System.currentTimeMillis())
}

private fun placeholderPartnerFor(title: String): String = when {
    title.contains("销售") -> "李想商贸"
    title.contains("采购") -> "晨光纸品厂"
    else -> "王五超市"
}

private fun placeholderItemCountFor(title: String): String = when {
    title.contains("销售") -> "5"
    title.contains("采购") -> "7"
    else -> "1"
}

private fun placeholderAmountFor(title: String): String = when {
    title.contains("销售") -> "1,258.00"
    title.contains("采购") -> "2,865.50"
    else -> "3,260.00"
}

private fun placeholderTimeFor(title: String): String = when {
    title.contains("销售") -> "2025-05-16 10:28"
    title.contains("采购") -> "2025-05-16 09:48"
    else -> "2025-05-15 16:30"
}
