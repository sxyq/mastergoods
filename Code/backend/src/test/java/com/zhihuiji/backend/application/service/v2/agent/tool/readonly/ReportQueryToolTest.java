package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.ReportService;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReportQueryToolTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReportService reportService = Mockito.mock(ReportService.class);
    private final ReportQueryTool tool = new ReportQueryTool(reportService);

    @Test
    void schemaRequiresARealReportTypeAndRejectsMissingInput() {
        JsonNode schema = tool.parameterSchema();

        assertEquals("object", schema.path("type").asText());
        assertFalse(schema.path("additionalProperties").asBoolean(true));
        assertTrue(schema.path("required").toString().contains("report_type"));
        assertTrue(schema.path("properties").path("report_type").path("enum").toString().contains("sales_summary"));

        var result = tool.execute(
            new ToolContext(1L, null, 1L, "run-report-schema", null, objectMapper),
            objectMapper.createObjectNode()
        );

        assertFalse(result.success());
        assertEquals("report_type 参数为空", result.errorMessage());
        assertTrue(result.blocks().isEmpty());
    }

    @Test
    void includesTheActualQueryWindowAndAllSummaryFacts() {
        Mockito.when(reportService.salesSummary(1751299200000L, 1753891200000L))
            .thenReturn(new com.zhihuiji.backend.api.dto.report.ReportDto.SalesSummaryReportDto(
                1751299200000L,
                1753891200000L,
                1200.5,
                800.0,
                20.0,
                400.5,
                3
            ));

        var params = objectMapper.createObjectNode()
            .put("report_type", "sales_summary")
            .put("period", "2025-07");
        var result = tool.execute(
            new ToolContext(7L, 7L, 1L, "run-report-facts", null, objectMapper),
            params
        );

        assertTrue(result.success());
        assertEquals("sales_summary", result.toolFacts().path("report_type").asText());
        assertEquals("2025-07", result.toolFacts().path("period").asText());
        assertEquals("2025-07-01", result.toolFacts().path("period_start").asText());
        assertEquals("2025-07-31", result.toolFacts().path("period_end").asText());
        assertEquals("¥1200.50", result.toolFacts().path("total_sales").asText());
        assertEquals("¥800.00", result.toolFacts().path("total_paid").asText());
        assertEquals("¥20.00", result.toolFacts().path("total_refund").asText());
        assertEquals("¥400.50", result.toolFacts().path("total_unpaid").asText());
        assertEquals(3, result.toolFacts().path("order_count").asInt());
        assertTrue(result.toolFacts().has("query_audit"));
    }
}
