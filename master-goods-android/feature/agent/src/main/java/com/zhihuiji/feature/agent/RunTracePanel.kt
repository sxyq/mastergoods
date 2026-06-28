package com.zhihuiji.feature.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.designsystem.DangerRed
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.SuccessGreen
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.WarningOrange
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 可折叠的过程轨迹面板。
 *
 * 展示 AI 单次运行的服务端事件：安全检查 -> 计划 -> 工具调用 -> 结果。
 */
@Composable
fun RunTracePanel(
    runTrace: RunTrace,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = Color(0xFFFFF7ED).copy(alpha = 0.72f),
        contentPadding = 12.dp
    ) {
        Column {
            // 头部：点击展开/折叠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "运行过程",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // 状态摘要
                    RunTraceStatusSummary(runTrace = runTrace)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 展开内容
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 安全审查
                    runTrace.safetyResult?.let { safety ->
                        SafetyBlock(safety)
                    }

                    if (
                        runTrace.auditId != null ||
                        runTrace.traceId != null ||
                        runTrace.logRef != null ||
                        runTrace.mode != null ||
                        runTrace.llmStatus != null ||
                        runTrace.planSource != null ||
                        runTrace.answerDeltaSource != null
                    ) {
                        TraceAuditBlock(runTrace)
                        ModelStatusBlock(runTrace)
                    }

                    // 计划步骤
                    if (runTrace.planSteps.isNotEmpty()) {
                        PlanStepsBlock(steps = runTrace.planSteps)
                    }

                    // 工具调用
                    if (runTrace.toolCalls.isNotEmpty()) {
                        ToolCallsBlock(toolCalls = runTrace.toolCalls)
                    }
                }
            }
        }
    }
}

@Composable
private fun RunTraceStatusSummary(runTrace: RunTrace) {
    val safetyResult = runTrace.safetyResult
    val toolCalls = runTrace.toolCalls
    val hasSafety = safetyResult != null
    val hasPlan = runTrace.planSteps.isNotEmpty()
    val hasTools = toolCalls.isNotEmpty()
    var failedToolCount = 0
    var allToolsDone = hasTools
    for (toolCall in toolCalls) {
        if (toolCall.status == ToolCallStatus.FAILED) {
            failedToolCount++
        }
        if (toolCall.status != ToolCallStatus.COMPLETED) {
            allToolsDone = false
        }
    }
    val isModelStream = runTrace.answerDeltaSource == "model_stream"
    val isStreamInterrupted = runTrace.isStreamInterrupted()
    val isRuleSummary = runTrace.answerDeltaSource == "rule_summary" ||
        runTrace.mode == "tool_query_rule_summary"

    val (statusText, statusColor) = when {
        hasSafety && !safetyResult!!.passed -> "已拦截" to DangerRed
        hasTools && failedToolCount == toolCalls.size -> "查询失败" to DangerRed
        failedToolCount > 0 -> "部分失败" to WarningOrange
        isStreamInterrupted -> "流式中断" to WarningOrange
        isModelStream -> "模型流" to ZhihuijiPrimary
        isRuleSummary -> "规则摘要" to WarningOrange
        hasTools && allToolsDone -> "查询已完成" to SuccessGreen
        hasTools -> "查询中..." to WarningOrange
        hasPlan -> "思考中..." to TextTertiary
        hasSafety -> "审查通过" to TextTertiary
        else -> "准备中..." to TextTertiary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(statusColor.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
        )
    }
}

@Composable
private fun TraceAuditBlock(runTrace: RunTrace) {
    val auditText = listOfNotNull(
        runTrace.auditId?.let { "运行标识 $it" },
        runTrace.traceId?.let { "轨迹标识 $it" },
        runTrace.logRef?.let { "日志引用 $it" },
    ).joinToString(" · ")
    if (auditText.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ZhihuijiPrimary.copy(alpha = 0.06f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = ZhihuijiPrimary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = auditText,
            style = MaterialTheme.typography.labelSmall,
            color = ZhihuijiPrimary,
        )
    }
}

@Composable
private fun ModelStatusBlock(runTrace: RunTrace) {
    val text = listOfNotNull(
        runTrace.mode?.let { "模式: ${it.agentModeLabel()}" },
        runTrace.llmStatus?.let { "模型: ${it.llmStatusLabel()}" },
        runTrace.planSource?.let { "规划: ${it.planSourceLabel()}" },
        runTrace.answerDeltaSource?.let { "输出: ${it.answerDeltaSourceLabel()}" },
    ).joinToString(" · ")
    val isRuleSummary = isRuleSummaryMode(mode = runTrace.mode, llmStatus = runTrace.llmStatus) ||
        runTrace.answerDeltaSource == "rule_summary"
    val color = if (isRuleSummary || runTrace.isStreamInterrupted()) WarningOrange else ZhihuijiPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.06f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

@Composable
private fun SafetyBlock(safety: com.zhihuiji.core.model.v2.agent.SafetyResult) {
    val (icon, color, text) = if (safety.passed) {
        Triple(Icons.Default.CheckCircle, SuccessGreen, "安全审查通过")
    } else {
        Triple(Icons.Default.Error, DangerRed, safety.reason ?: "安全审查未通过")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.06f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = if (!safety.passed) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private val agentModeLabels = mapOf(
    "tool_query_llm_synthesized" to "工具查询 + 模型综合",
    "tool_query_rule_summary" to "工具查询 + 规则摘要",
    "tool_query_failed" to "工具查询失败",
    "unsupported_intent" to "未接入查询",
    "blocked" to "安全拦截",
)
private fun String.agentModeLabel(): String = agentModeLabels[this] ?: this

private val llmStatusLabels = mapOf(
    "available" to "可用",
    "streaming" to "流式生成中",
    "stream_interrupted" to "流式中断",
    "stream_failed_or_empty" to "流式失败或空响应",
    "disabled" to "已关闭",
    "not_configured" to "未配置",
    "stream_not_supported" to "当前模型接口不支持流式",
    "failed_or_empty" to "返回为空",
    "not_requested" to "未调用",
)
private fun String.llmStatusLabel(): String = llmStatusLabels[this] ?: this

private val planSourceLabels = mapOf(
    "llm" to "模型规划",
    "keyword" to "关键词规划",
    "keyword_fallback" to "关键词兜底规划",
    "safety" to "安全策略",
    "tool" to "工具结果",
)
private fun String.planSourceLabel(): String = planSourceLabels[this] ?: this

private val answerDeltaSourceLabels = mapOf(
    "model_stream" to "模型实时流",
    "rule_summary" to "服务端规则摘要",
)
private fun String.answerDeltaSourceLabel(): String = answerDeltaSourceLabels[this] ?: this

internal fun RunTrace.isStreamInterrupted(): Boolean =
    mode == "tool_query_llm_stream_interrupted" || llmStatus == "stream_interrupted"

@Composable
private fun PlanStepsBlock(steps: List<com.zhihuiji.core.model.v2.agent.PlanStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ZhihuijiPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZhihuijiPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = step.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ToolCallsBlock(toolCalls: List<com.zhihuiji.core.model.v2.agent.ToolCallRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        toolCalls.forEach { call ->
            ToolCallItem(call = call)
        }
    }
}

@Composable
private fun ToolCallItem(call: com.zhihuiji.core.model.v2.agent.ToolCallRecord) {
    val (statusIcon, statusColor) = when (call.status) {
        ToolCallStatus.PENDING -> Icons.Default.Settings to TextTertiary
        ToolCallStatus.RUNNING -> Icons.Default.Settings to WarningOrange
        ToolCallStatus.COMPLETED -> Icons.Default.CheckCircle to SuccessGreen
        ToolCallStatus.FAILED -> Icons.Default.Error to DangerRed
    }
    val queryWindowSummary = remember(call.queryWindow) { call.queryWindow?.toQueryWindowSummary() }
    val evidenceSummary = remember(call.evidence) { call.evidence?.toEvidenceSummary() }
    val auditSummary = remember(
        call.seq,
        call.conversationId,
        call.eventId,
        call.auditId,
        call.traceId,
        call.startedAt,
        call.completedAt,
        call.durationMs,
        call.returnedCount,
        call.totalCount,
        call.limit,
        call.isTruncated,
        call.nextCursor,
    ) {
        call.auditSummary()
    }
    val hasDetails = call.inputSummary != null ||
        queryWindowSummary != null ||
        call.toolCallId != null ||
        call.resultSummary != null ||
        evidenceSummary != null ||
        auditSummary != null
    // 工具调用详情二级折叠：仅在存在详情时可展开
    var expanded by remember(call.toolCallId ?: call.toolName) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = call.toolName,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (call.status) {
                    ToolCallStatus.PENDING -> "等待中"
                    ToolCallStatus.RUNNING -> "查询中..."
                    ToolCallStatus.COMPLETED -> "工具已返回"
                    ToolCallStatus.FAILED -> "失败"
                },
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (hasDetails) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "折叠" else "展开",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                call.inputSummary?.let { input ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "输入: $input",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                queryWindowSummary?.let { scope ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "范围: $scope",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                call.toolCallId?.let { toolCallId ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "调用: $toolCallId",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                call.resultSummary?.let { result ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (call.status == ToolCallStatus.FAILED) "错误: $result" else "结果: $result",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (call.status == ToolCallStatus.FAILED) DangerRed else TextSecondary,
                    )
                }
                evidenceSummary?.let { evidence ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "依据: $evidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                auditSummary?.let { audit ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = audit,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (call.isTruncated == true) WarningOrange else TextTertiary,
                    )
                }
            }
        }
    }
}

private fun com.zhihuiji.core.model.v2.agent.ToolCallRecord.auditSummary(): String? {
    val parts = listOfNotNull(
        seq?.let { "事件 #$it" },
        conversationId?.let { "会话 $it" },
        eventId?.let { "事件 ${it.compactEvidenceText()}" },
        auditId?.let { "运行 ${it.compactEvidenceText()}" },
        traceId?.let { "轨迹 ${it.compactEvidenceText()}" },
        startedAt?.let { "开始 ${it.formatClockTime()}" },
        completedAt?.let { "完成 ${it.formatClockTime()}" },
        durationMs?.let { "耗时 ${it}ms" },
        returnedCount?.let { returned ->
            totalCount?.let { total -> "返回 $returned/$total 条" } ?: "返回 $returned 条"
        },
        limit?.let { "上限 $it" },
        isTruncated?.takeIf { it }?.let { "结果已截断" },
        nextCursor?.takeIf { it.isNotBlank() }?.let { "可继续加载" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun JsonElement.toQueryWindowSummary(): String? {
    val obj = (this as? JsonObject) ?: return compactJsonText()
    val parts = listOfNotNull(
        obj.stringValue("owner_scope")?.let { if (it == "current_owner") "当前账号" else it },
        obj.intValue("window_days")?.let { "近 ${it} 天" },
        obj.intValue("limit")?.let { "上限 $it 条" },
        obj.intValue("rank_limit")?.let { "排行 $it 条" },
        obj.intValue("low_stock_limit")?.let { "低库存 $it 条" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: compactJsonText()
}

private fun JsonElement.toEvidenceSummary(): String? {
    val obj = (this as? JsonObject) ?: return compactJsonText()
    val parts = listOfNotNull(
        obj.stringValue("source")?.let { it.removePrefix("tool:") },
        obj.stringValue("scope")?.let { if (it == "current_owner") "当前账号" else it },
        obj.intValue("returned_count")?.let { "返回 $it 条" },
        obj.intValue("total_count")?.let { "共 $it 条" },
        obj.booleanValue("is_truncated")?.takeIf { it }?.let { "已截断" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: compactJsonText()
}

private fun JsonElement.compactJsonText(maxLength: Int = 90): String? =
    toString().takeIf { it.isNotBlank() }?.let { raw ->
        if (raw.length <= maxLength) raw else raw.take(maxLength) + "..."
    }

private fun Long.formatClockTime(): String =
    SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(this))
