package com.zhihuiji.feature.agent

import androidx.compose.ui.graphics.Color
import com.zhihuiji.core.model.v2.agent.DonutChartBlockData
import com.zhihuiji.core.model.v2.agent.EvidenceCardBlockData
import com.zhihuiji.core.model.v2.agent.LineChartBlockData
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun structuredTableCellsCanReuseInlineMarkdownForLinksAndCode() {
        val rendered = inlineMarkdown("查看[客户档案](www.example.com/customer) 与 `VIP` 标记", Color.Black)

        assertEquals("查看客户档案 (https://www.example.com/customer) 与  VIP  标记", rendered.text)
    }

    @Test
    fun markdownResultBlockAcceptsMarkdownFieldAliasFromAgent() {
        val block = ResultBlockDto(
            blockType = "markdown",
            title = "AI 分析",
            data = buildJsonObject {
                put("markdown", "## 销售结论\n- 今日销售额 1280 元")
            }
        )

        assertEquals("## 销售结论\n- 今日销售额 1280 元", block.parseTextBlockMarkdown())
    }

    @Test
    fun textResultBlockRejectsEmptyTextAndMarkdownInsteadOfRenderingBlankCard() {
        val block = ResultBlockDto(
            blockType = "markdown",
            title = "空结果",
            data = buildJsonObject {
                put("text", "")
                put("markdown", "")
            }
        )

        assertNull(block.parseTextBlockMarkdown())
    }

    @Test
    fun evidenceCardItemKeepsAuditSummaryForFieldLevelEvidence() {
        val item = EvidenceCardBlockData.EvidenceItem(
            label = "欠款客户数 (customer_count)",
            value = "2个",
            source = "tool:customer_receivable_lookup",
            toolCallId = "run-contract-1:customer_receivable_lookup:0",
            queryWindow = buildJsonObject {
                put("owner_scope", "current_owner")
                put("limit", 10)
            },
            isTruncated = true,
        )

        val summary = item.auditSummary()

        assertTrue(summary!!.contains("调用 run-contract"))
        assertTrue(summary.contains("当前账号"))
        assertTrue(summary.contains("上限 10 条"))
        assertTrue(summary.contains("结果已截断"))
    }

    @Test
    fun evidenceCardLongTextsUseCompactReadableLabelsForNarrowScreens() {
        val item = EvidenceCardBlockData.EvidenceItem(
            label = "超长客户名称与字段 customer_count 不应挤压金额",
            value = "12345678901234567890123456789012345678901234567890 元",
            source = "tool:customer_receivable_lookup_with_extremely_long_backend_source_name",
            toolCallId = "run-contract-very-long-id:customer_receivable_lookup:0",
            queryWindow = buildJsonObject {
                put("owner_scope", "current_owner")
                put("limit", 10)
            },
            isTruncated = true,
        )

        assertEquals("12345678901234...567890 元", item.displayValue())
        assertEquals(
            "来源: customer_recei...rce_name",
            item.displaySource()
        )
        assertTrue(item.auditSummary()!!.contains("调用 run-contract-v...lookup:0"))
    }

    @Test
    fun evidenceCardAuditSummaryCompactsLongFreeformOwnerScope() {
        val item = EvidenceCardBlockData.EvidenceItem(
            label = "范围",
            value = "当前查询",
            queryWindow = buildJsonObject {
                put("owner_scope", "tenant_owner_scope_with_unusually_long_debug_suffix_1234567890")
                put("limit", 10)
            },
        )

        val summary = item.auditSummary()!!

        assertTrue(summary.contains("tenant_owner_s...34567890"))
        assertTrue(summary.contains("上限 10 条"))
        assertFalse(summary.contains("tenant_owner_scope_with_unusually_long_debug_suffix_1234567890"))
    }

    @Test
    fun tableContractRejectsRowColumnMismatchInsteadOfRenderingMisalignedData() {
        val shortRow = validateTableContract(
            headers = listOf("客户", "金额"),
            rows = listOf(listOf("张三"))
        )
        val longRow = validateTableContract(
            headers = listOf("客户", "金额"),
            rows = listOf(listOf("张三", "100", "多余字段"))
        )

        assertEquals("表格第 1 行的数据量与列名数量不一致，已停止渲染", shortRow)
        assertEquals("表格第 1 行的数据量与列名数量不一致，已停止渲染", longRow)
    }

    @Test
    fun tableContractAllowsAlignedRealRows() {
        val error = validateTableContract(
            headers = listOf("客户", "金额"),
            rows = listOf(listOf("张三", "100"))
        )

        assertNull(error)
    }

    @Test
    fun barChartScaleKeepsNegativeValuesInsteadOfDroppingThem() {
        val scale = barChartScale(listOf(-8.0, 12.0, 0.0))

        assertEquals(-8.0, scale.minValue, 0.000001)
        assertEquals(12.0, scale.maxValue, 0.000001)
        assertEquals(20.0, scale.range, 0.000001)
    }

    @Test
    fun barChartScaleSupportsAllNegativeSeriesAgainstZeroBaseline() {
        val scale = barChartScale(listOf(-8.0, -2.0))

        assertEquals(-8.0, scale.minValue, 0.000001)
        assertEquals(0.0, scale.maxValue, 0.000001)
        assertEquals(8.0, scale.range, 0.000001)
    }

    @Test
    fun donutChartSegmentsKeepValidPositiveValuesAndCountIgnoredSegments() {
        val result = donutChartSegments(
            listOf(
                donutSegment("有效A", 12.0),
                donutSegment("零值", 0.0),
                donutSegment("负值", -3.0),
                donutSegment("无效", Double.NaN),
                donutSegment("有效B", 8.0),
                donutSegment("无限", Double.POSITIVE_INFINITY),
            )
        )

        assertEquals(listOf("有效A", "有效B"), result.segments.map { it.name })
        assertEquals(listOf(12.0, 8.0), result.segments.map { it.value })
        assertEquals(4, result.ignoredCount)
    }

    @Test
    fun donutChartSegmentsReportAllInvalidSegmentsWithoutCreatingMockData() {
        val result = donutChartSegments(
            listOf(
                donutSegment("零值", 0.0),
                donutSegment("负值", -1.0),
                donutSegment("无效", Double.NaN),
            )
        )

        assertTrue(result.segments.isEmpty())
        assertEquals(3, result.ignoredCount)
    }

    @Test
    fun knownChartBlockMissingRequiredFieldsFailsParsingInsteadOfCreatingEmptyChart() {
        val block = ResultBlockDto(
            blockType = "line_chart",
            title = "缺字段趋势图",
            data = buildJsonObject {
                put("title", "销售趋势")
                put("labels", "06/09")
            }
        )

        assertNull(block.parseData<LineChartBlockData>())
        assertTrue(block.dataPreview()!!.contains("\"labels\":\"06/09\""))
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

    private fun donutSegment(name: String, value: Double): DonutChartBlockData.Segment =
        DonutChartBlockData.Segment(
            name = name,
            value = value,
        )
}
