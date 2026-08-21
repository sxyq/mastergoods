package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus

internal const val DeltaSourceModelStream = "model_stream"

internal fun ChatMessage.hasVisibleAssistantTimeline(): Boolean =
    displayParts().visibleAssistantParts().isNotEmpty()

internal fun isStreamInterruptedMode(mode: String?, llmStatus: String?): Boolean =
    mode == "tool_query_llm_stream_interrupted" || llmStatus == "stream_interrupted"

/** User-facing summaries intentionally expose executed tools, not model reasoning or safety details. */
internal fun List<ToolCallRecord>.assistantToolActivitySummary(): String? {
    if (isEmpty()) return null
    val activeCount = count { it.status == ToolCallStatus.PENDING || it.status == ToolCallStatus.RUNNING }
    val failedCount = count { it.status == ToolCallStatus.FAILED }
    return when {
        activeCount > 0 -> "正在查询 $size 个数据源"
        failedCount == 0 -> "已完成 $size 个数据查询"
        failedCount == size -> "查询未完成"
        else -> "已完成 ${size - failedCount}/$size 个数据查询"
    }
}

internal fun ToolCallRecord.userFacingToolLabel(): String = when (toolName) {
    "plan" -> "确定查询范围"
    "sales_overview_lookup", "sales_trend_lookup", "sale_order_lookup", "sales_full_chain_lookup" -> "查询销售数据"
    "inventory_low_stock_lookup", "inventory_panorama_lookup", "inventory_snapshot_lookup", "inventory_ledger_lookup" -> "查询库存数据"
    "customer_profile_lookup", "customer_receivable_lookup" -> "查询客户数据"
    "supplier_payable_lookup", "supplier_statement_lookup", "product_supplier_relation_lookup" -> "查询供应商数据"
    "cashflow_summary_lookup", "finance_record_lookup", "account_balance_lookup", "payment_lookup" -> "查询资金数据"
    "report_query" -> "汇总经营数据"
    "result_visualization" -> "整理图表数据"
    else -> "查询业务数据"
}

internal fun ToolCallRecord.userFacingToolOutcome(): String = when (status) {
    ToolCallStatus.PENDING, ToolCallStatus.RUNNING -> "正在查询"
    ToolCallStatus.COMPLETED -> resultSummary?.takeIf { it.isNotBlank() } ?: "查询完成"
    ToolCallStatus.FAILED -> "本次查询未完成"
}
