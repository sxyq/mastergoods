package com.zhihuiji.core.model.v2.agent

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 结构化结果块 DTO，用于流式事件中的 result_block 事件。
 * 后端必须按此结构输出，Android 端按 block_type 分发渲染。
 */
@Serializable
@Immutable
data class ResultBlockDto(
    @SerialName("block_type") val blockType: String,
    val title: String? = null,
    val data: JsonElement? = null,
)

/** 纯文本块（兜底） */
@Serializable
@Immutable
data class TextBlockData(
    val text: String? = null,
    val markdown: String? = null,
)

/** KPI 网格块：一组关键指标卡片 */
@Serializable
@Immutable
data class KpiGridBlockData(
    val kpis: List<KpiItem>,
) {
    @Serializable
    @Immutable
    data class KpiItem(
        val label: String,
        val value: String,
        @SerialName("trend_direction") val trendDirection: String? = null, // up / down / flat
        @SerialName("trend_value") val trendValue: String? = null,
        val unit: String? = null,
    )
}

/** 表格块 */
@Serializable
@Immutable
data class TableBlockData(
    val headers: List<String>,
    val rows: List<List<String>>,
    @SerialName("row_count") val rowCount: Int? = null,
)

/** 排行列表块 */
@Serializable
@Immutable
data class RankListBlockData(
    val items: List<RankItem>,
) {
    @Serializable
    @Immutable
    data class RankItem(
        val rank: Int,
        val name: String,
        val value: String,
        @SerialName("change_direction") val changeDirection: String? = null,
    )
}

/** 折线图块 */
@Serializable
@Immutable
data class LineChartBlockData(
    val title: String? = null,
    val labels: List<String>,
    val series: List<ChartSeries>,
) {
    @Serializable
    @Immutable
    data class ChartSeries(
        val name: String,
        val data: List<Double>,
        val color: String? = null,
    )
}

/** 柱状图块 */
@Serializable
@Immutable
data class BarChartBlockData(
    val title: String? = null,
    val labels: List<String>,
    val series: List<ChartSeries>,
) {
    @Serializable
    @Immutable
    data class ChartSeries(
        val name: String,
        val data: List<Double>,
        val color: String? = null,
    )
}

/** 环形/饼图块 */
@Serializable
@Immutable
data class DonutChartBlockData(
    val title: String? = null,
    val segments: List<Segment>,
) {
    @Serializable
    @Immutable
    data class Segment(
        val name: String,
        val value: Double,
        val color: String? = null,
    )
}

/** 风险卡片块 */
@Serializable
@Immutable
data class RiskCardBlockData(
    val level: String, // high / medium / low
    val title: String,
    val description: String,
    @SerialName("affected_items") val affectedItems: List<String>? = null,
    @SerialName("suggested_action") val suggestedAction: String? = null,
)

/** 证据/依据卡片块 */
@Serializable
@Immutable
data class EvidenceCardBlockData(
    val title: String? = null,
    val items: List<EvidenceItem>,
) {
    @Serializable
    @Immutable
    data class EvidenceItem(
        val label: String,
        val value: String,
        val source: String? = null,
        @SerialName("tool_call_id") val toolCallId: String? = null,
        @SerialName("query_window") val queryWindow: JsonElement? = null,
        @SerialName("is_truncated") val isTruncated: Boolean? = null,
    )
}

/** 草稿卡片块：展示 AI 生成的业务草稿，供用户确认 */
@Serializable
@Immutable
data class DraftCardBlockData(
    @SerialName("draft_id") val draftId: Long,
    @SerialName("draft_type") val draftType: String,
    val title: String,
    val summary: String,
    @SerialName("item_count") val itemCount: Int? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    @SerialName("partner_name") val partnerName: String? = null,
    val warnings: List<String>? = null,
)
