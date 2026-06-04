package com.zhihuiji.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.designsystem.BottomActionBar
import com.zhihuiji.core.designsystem.EmptyState
import com.zhihuiji.core.designsystem.FormSection
import com.zhihuiji.core.designsystem.GlassCard
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassTopBar
import com.zhihuiji.core.designsystem.IconBadge
import com.zhihuiji.core.designsystem.PillTone
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.SecondaryOutlineButton
import com.zhihuiji.core.designsystem.StatusPill
import com.zhihuiji.core.designsystem.ZhihuijiColors
import com.zhihuiji.core.designsystem.ZhihuijiTypography

@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    viewModel: ProductEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val draft = uiState.draft
    val existingId = uiState.existingId

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    val stockTone = when {
        draft.stock <= 0.000001 -> PillTone.DANGER
        draft.stock < draft.safeStock -> PillTone.WARNING
        else -> PillTone.SUCCESS
    }

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "商品详情",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
            if (existingId != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(icon = Icons.Default.Inventory2, tint = ZhihuijiColors.Primary, size = 48.dp, cornerRadius = 14.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(draft.name, style = ZhihuijiTypography.titleMedium, color = ZhihuijiColors.TextPrimary)
                                Text(draft.code.ifBlank { "--" }, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.TextSecondary)
                                Text(
                                    text = listOf(draft.categoryName, draft.unitName).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "未配置分类与单位" },
                                    style = ZhihuijiTypography.labelSmall,
                                    color = ZhihuijiColors.TextTertiary,
                                )
                            }
                            StatusPill(text = StatusLabels.stockStatus(draft.stock, draft.safeStock), tone = stockTone)
                        }
                    }

                    FormSection(title = "基础信息") {
                        DetailRow("商品编码", draft.code.ifBlank { "--" })
                        DetailRow("商品名称", draft.name.ifBlank { "--" })
                        DetailRow("商品分类", draft.categoryName.ifBlank { "--" })
                        DetailRow("计量单位", draft.unitName.ifBlank { "--" })
                    }

                    FormSection(title = "库存与价格") {
                        DetailRow("当前库存", MoneyFormatter.formatWithoutSymbol(draft.stock))
                        DetailRow("安全库存", MoneyFormatter.formatWithoutSymbol(draft.safeStock))
                        DetailRow("销售价格", MoneyFormatter.format(draft.salePrice))
                        DetailRow("采购价格", MoneyFormatter.format(draft.purchasePrice))
                    }

                    FormSection(title = "供应与扩域") {
                        DetailRow("价格层级", "${draft.priceLevels.size} 个")
                        DetailRow("供应关系", "${draft.supplierRelations.size} 条")
                        DetailRow("默认状态", if (draft.status == 1) "正常" else "停用")
                    }

                    FormSection(title = "档案扩展") {
                        DetailRow("创建时间", "当前详情未返回")
                        DetailRow("更新时间", "当前详情未返回")
                        DetailRow("扩展资料", if (draft.priceLevels.isNotEmpty() || draft.supplierRelations.isNotEmpty()) "已返回部分扩展信息" else "待补齐")
                    }

                    FormSection(title = "记录概览") {
                        EmptyState(
                            icon = Icons.Default.Inventory2,
                            title = "暂无可展示记录",
                            subtitle = "跟进记录、变更历史等信息会在可用后展示，这里先不生成虚构事件。",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }

                    if (uiState.error != null) {
                        Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                    }
                }
                BottomActionBar(
                    primaryAction = {
                        PrimaryButton(
                            text = "编辑商品",
                            onClick = { onNavigateToEditor(productId) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    secondaryActions = listOf {
                        SecondaryOutlineButton(text = "返回列表", onClick = onNavigateBack)
                    },
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyState(
                        icon = Icons.Default.Inventory2,
                        title = "暂无商品详情",
                        subtitle = "商品数据返回后将在这里展示",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = ZhihuijiTypography.labelSmall, color = ZhihuijiColors.TextSecondary)
        Text(value, style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
    }
}
