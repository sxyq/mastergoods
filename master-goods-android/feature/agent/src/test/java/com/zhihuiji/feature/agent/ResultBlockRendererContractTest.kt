package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultBlockRendererContractTest {
    @Test
    fun chartContractRejectsMissingLabelsInsteadOfGeneratingMockLabels() {
        val error = validateChartContractForSeries(
            labels = emptyList(),
            series = listOf("销售额" to listOf(1.0))
        )

        assertEquals("图表数据缺少横轴标签，已停止绘制以避免生成模拟标签", error)
    }

    @Test
    fun chartContractRejectsBlankLabelsInsteadOfGeneratingMockLabels() {
        val error = validateChartContractForSeries(
            labels = listOf("06/08", ""),
            series = listOf("销售额" to listOf(1.0, 2.0))
        )

        assertEquals("图表横轴标签存在空值，已停止绘制以避免生成模拟标签", error)
    }

    @Test
    fun chartContractRejectsMissingSeries() {
        val error = validateChartContractForSeries(
            labels = listOf("06/08"),
            series = emptyList()
        )

        assertEquals("图表数据缺少真实序列，无法绘制", error)
    }

    @Test
    fun chartContractRejectsSeriesLengthMismatch() {
        val error = validateChartContractForSeries(
            labels = listOf("06/07", "06/08"),
            series = listOf("销售额" to listOf(1.0))
        )

        assertEquals("图表序列「销售额」的数据量与标签数量不一致，已停止绘制", error)
    }

    @Test
    fun chartContractRejectsInvalidNumbers() {
        val nanError = validateChartContractForSeries(
            labels = listOf("06/07", "06/08"),
            series = listOf("销售额" to listOf(1.0, Double.NaN))
        )
        val infiniteError = validateChartContractForSeries(
            labels = listOf("06/07", "06/08"),
            series = listOf("销售额" to listOf(1.0, Double.POSITIVE_INFINITY))
        )

        assertEquals("图表序列「销售额」包含无效数值，已停止绘制", nanError)
        assertEquals("图表序列「销售额」包含无效数值，已停止绘制", infiniteError)
    }

    @Test
    fun chartContractAllowsValidSeries() {
        val error = validateChartContractForSeries(
            labels = listOf("06/07", "06/08"),
            series = listOf("销售额" to listOf(1.0, 2.0), "回款" to listOf(0.5, 1.5))
        )

        assertNull(error)
    }

    @Test
    fun dataPreviewKeepsUnknownBlockRawSummaryAndTruncatesIt() {
        val block = ResultBlockDto(
            blockType = "future_chart",
            title = "未来图表",
            data = buildJsonObject {
                put("source", "tool:sales_overview_lookup")
                put("payload", "x".repeat(280))
            }
        )

        val preview = block.dataPreview()

        assertTrue(preview!!.startsWith("原始数据: "))
        assertTrue(preview.contains("tool:sales_overview_lookup"))
        assertTrue(preview.length <= "原始数据: ".length + 240)
    }
}
