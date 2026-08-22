package com.zhihuiji.backend.application.service.v2.agent.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentToolPlan;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ResponsePayload;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AnswerSynthesizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LongCatAnthropicClient longCatAnthropicClient;
    private SseStreamEmitter sseStreamEmitter;
    private AnswerSynthesizer answerSynthesizer;

    @BeforeEach
    void setUp() {
        longCatAnthropicClient = mock(LongCatAnthropicClient.class);
        sseStreamEmitter = mock(SseStreamEmitter.class);
        answerSynthesizer = new AnswerSynthesizer(
            longCatAnthropicClient,
            sseStreamEmitter,
            mock(RunAuditService.class),
            mock(AgentMessageRepository.class),
            objectMapper,
            mock(ToolPlanner.class)
        );
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
    }

    @Test
    void acceptsModelTextThatConfirmsARealVisualization() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("已生成销售趋势图，请查看图表。"));

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        facts.put("visualization_enabled", true);
        ToolExecutionResult result = new ToolExecutionResult(
            "result_visualization",
            "已启用 chart 自适应结果展示",
            facts,
            false
        );
        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "line_chart",
            "销售趋势",
            objectMapper.createObjectNode()
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "最近一周销售给我画一张趋势图",
            new ResponsePayload(List.of(block), List.of(result)),
            List.of(),
            null
        );

        assertEquals("已生成销售趋势图，请查看图表。", answer.answer());
    }

    @Test
    void acceptsModelTextThatConfirmsARealMediaDraft() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("已生成上传意图草稿，请确认后继续。"));

        ObjectNode facts = objectMapper.createObjectNode();
        facts.put("draft_id", 44L);
        facts.put("draft_type", "media_upload");
        facts.putObject("query_audit");
        ToolExecutionResult result = new ToolExecutionResult(
            "media_upload_tool",
            "生成媒体上传草稿（#44）",
            facts,
            false
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "先生成上传意图草稿",
            new ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("已生成上传意图草稿，请确认后继续。", answer.answer());
    }

    @Test
    void returnsUnavailableWhenModelAnswersRemainUngrounded() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("查询完成，共有 999 条记录。"))
            .thenReturn(Optional.of("已查询，结果是 888 条。"));

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        facts.put("receipt_count", 1);
        ToolExecutionResult result = new ToolExecutionResult(
            "purchase_receipt_lookup",
            "最近采购入库单 1 条",
            facts,
            false
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "最近入了哪些采购货？入库明细和状态给我看看。",
            new ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("", answer.answer());
        assertEquals("llm_answer_unavailable", answer.mode());
        assertEquals("model_empty_or_ungrounded", answer.llmStatus());
        verify(longCatAnthropicClient, times(2)).createJsonMessage(anyString(), anyString());
    }

    @Test
    void ignoresPlannerTerminalAnswerAndRequiresARealProviderFinalAnswer() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("真实模型回答：当前有 3 个商品。"));

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        facts.put("product_count", 3);
        ToolExecutionResult result = new ToolExecutionResult(
            "product_catalog_lookup",
            "商品总数 3 个",
            facts,
            false
        );
        AgentToolPlan plan = new AgentToolPlan(
            List.of("product_catalog_lookup"),
            "工具结果已足够",
            "native_tool_use",
            java.util.Map.of(),
            null,
            List.of(),
            "服务端拼装的终答，不应直接展示"
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "查看商品数量",
            new ResponsePayload(List.of(), List.of(result), List.of(), plan),
            List.of(),
            null
        );

        assertEquals("真实模型回答：当前有 3 个商品。", answer.answer());
        assertFalse(answer.answer().contains("服务端拼装的终答"));
        verify(longCatAnthropicClient).createJsonMessage(anyString(), anyString());
    }

    @Test
    void acceptsAReportAnswerThatRepeatsTheActualQueryWindow() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("2025年7月销售额为 ¥0.00，估算利润为 ¥0.00。"));

        ObjectNode salesFacts = objectMapper.createObjectNode()
            .put("report_type", "sales_summary")
            .put("period", "2025-07")
            .put("period_start", "2025-07-01")
            .put("period_end", "2025-07-31")
            .put("total_sales", "¥0.00")
            .put("total_paid", "¥0.00")
            .put("total_refund", "¥0.00")
            .put("total_unpaid", "¥0.00")
            .put("order_count", 0);
        salesFacts.putObject("query_audit");
        ObjectNode profitFacts = objectMapper.createObjectNode()
            .put("report_type", "profit_summary")
            .put("period", "2025-07")
            .put("period_start", "2025-07-01")
            .put("period_end", "2025-07-31")
            .put("estimated_cost", "¥0.00")
            .put("estimated_profit", "¥0.00")
            .put("profit_rate", 0.0);
        profitFacts.putObject("query_audit");

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "帮我看看这个月销售怎么样，给我一个经营汇总。",
            new ResponsePayload(
                List.of(),
                List.of(
                    new ToolExecutionResult("report_query", "销售汇总 销售额 ¥0.00", salesFacts, false),
                    new ToolExecutionResult("report_query", "利润汇总 利润 ¥0.00", profitFacts, false)
                )
            ),
            List.of(),
            null
        );

        assertEquals("2025年7月销售额为 ¥0.00，估算利润为 ¥0.00。", answer.answer());
        assertEquals("tool_query_llm", answer.mode());
        assertEquals("completed", answer.llmStatus());
    }

    @Test
    void acceptsReadOnlyAnswerThatMentionsInboundAndReturnsAsQueryResults() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("已完成采购链路查询，入库 1 条、退货 0 条，详细记录如下。"));

        ObjectNode facts = objectMapper.createObjectNode()
            .put("receipt_count", 1)
            .put("return_count", 0);
        facts.putObject("query_audit");
        ToolExecutionResult result = new ToolExecutionResult(
            "purchase_tracking_lookup",
            "采购链路查询完成",
            facts,
            false
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "把采购单、入库和退货的关联过程串起来看看",
            new ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("已完成采购链路查询，入库 1 条、退货 0 条，详细记录如下。", answer.answer());
        assertEquals("completed", answer.llmStatus());
    }

    @Test
    void acceptsInboundStatusDescriptionWithoutTreatingItAsAWrite() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("采购的 ¥25.00 已全部入库，到货率为 100%。"));

        ObjectNode facts = objectMapper.createObjectNode()
            .put("total_amount", "¥25.00")
            .put("received_amount", "¥25.00")
            .put("receipt_count", 1)
            .put("return_count", 0)
            .put("arrival_rate", "100%");
        facts.putObject("query_audit");
        ToolExecutionResult result = new ToolExecutionResult(
            "purchase_tracking_lookup",
            "采购链路查询完成",
            facts,
            false
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "把采购单、入库和退货的关联过程串起来看看",
            new ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("采购的 ¥25.00 已全部入库，到货率为 100%。", answer.answer());
        assertEquals("completed", answer.llmStatus());
    }

    @Test
    void rejectsActualWriteClaimWhenNoWriteToolSucceeded() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("已完成入库，采购单已更新。"));

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        ToolExecutionResult result = new ToolExecutionResult(
            "purchase_tracking_lookup",
            "采购链路查询完成",
            facts,
            false
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "把采购单、入库和退货的关联过程串起来看看",
            new ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("", answer.answer());
        assertEquals("llm_answer_unavailable", answer.mode());
        verify(longCatAnthropicClient, times(2)).createJsonMessage(anyString(), anyString());
    }

    @Test
    void returnsUnavailableWithoutTemplateWhenEveryModelAnswerAttemptFails() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.empty());

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        facts.put("product_count", 3);
        ToolExecutionResult result = new ToolExecutionResult(
            "product_catalog_lookup",
            "商品总数 3 个，返回 3 个",
            facts,
            false
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "帮我看看现在有多少商品",
            new ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("", answer.answer());
        assertEquals("llm_answer_unavailable", answer.mode());
        assertEquals("model_empty_or_ungrounded", answer.llmStatus());
    }

    @Test
    void returnsGroundedSummaryWhenProviderReturnsNoFormalAnswerForRealFacts() {
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.empty());
        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit").put("returned_count", 2);
        ToolExecutionResult result = new ToolExecutionResult(
            "customer_receivable_lookup", "应收客户 2 个", facts, false
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "哪些客户还欠我钱？",
            new ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertFalse(answer.answer().isBlank());
        assertEquals("tool_query_grounded_fallback", answer.mode());
        assertEquals("facts_fallback", answer.llmStatus());
        assertFalse(answer.answer().contains("999"));
    }

    @Test
    void streamUsesStrictRealModelRetryAfterUngroundedFirstAnswer() {
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(Optional.of("最近有 999 个采购单。"));
        when(longCatAnthropicClient.createJsonMessage(contains("上一次正式回答为空或未通过格式/事实校验"), anyString()))
            .thenReturn(Optional.of("最近没有匹配到采购单。"));

        ObjectNode facts = objectMapper.createObjectNode();
        facts.putObject("query_audit");
        ToolExecutionResult result = new ToolExecutionResult(
            "purchase_tracking_lookup",
            "未匹配到采购单",
            facts,
            false
        );

        SseEmitter emitter = mock(SseEmitter.class);
        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswerForStream(
            "最近采购、入库和退货之间的情况帮我串起来看看。",
            new ResponsePayload(List.of(), List.of(result)),
            emitter,
            "run-retry",
            () -> {},
            List.of(),
            null
        );

        assertEquals("最近没有匹配到采购单。", answer.answer());
        assertEquals("tool_query_llm", answer.mode());
        assertEquals("non_stream_retry", answer.llmStatus());
        verify(longCatAnthropicClient).createJsonMessage(
            contains("当前问题是查询类问题时，不要声称已创建"),
            anyString()
        );
        verify(sseStreamEmitter, atLeastOnce()).emitAnswerDeltaUnchecked(
            eq(emitter),
            eq("run-retry"),
            anyString(),
            eq("non_stream_retry")
        );
        verify(sseStreamEmitter, never()).emitAnswerDeltaUnchecked(
            eq(emitter),
            eq("run-retry"),
            anyString(),
            eq("model_stream")
        );
    }

    @Test
    void keepsGeneratedToolCallIdWhenModelToolCallIdIsMissing() throws Exception {
        RunAuditService runAuditService = mock(RunAuditService.class);
        SseStreamEmitter emitter = new SseStreamEmitter(objectMapper, runAuditService);
        SseStreamEmitter.ToolAudit audit = emitter.startToolAudit(
            null,
            "run-tool-id",
            "product_catalog_lookup",
            Map.of(),
            3,
            null
        );

        emitter.emitToolCompleted(null, "run-tool-id", "product_catalog_lookup", "查询完成", audit);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runAuditService, times(2)).queueRunAuditEvent(
            eq("run-tool-id"),
            payloadCaptor.capture(),
            anyString()
        );
        assertEquals(
            RunAuditService.toolCallId("run-tool-id", 3, "product_catalog_lookup"),
            payloadCaptor.getAllValues().get(1).get("tool_call_id")
        );
    }
}
