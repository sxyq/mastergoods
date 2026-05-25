package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentLlmService {
    private static final Logger log = LoggerFactory.getLogger(AgentLlmService.class);

    private final LongCatAnthropicClient longCatClient;
    private final ObjectMapper objectMapper;

    public AgentLlmService(LongCatAnthropicClient longCatClient, ObjectMapper objectMapper) {
        this.longCatClient = longCatClient;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return longCatClient.isConfigured();
    }

    public Optional<JsonNode> requestStructuredJson(String systemPrompt, String userPrompt) {
        return longCatClient.createJsonMessage(systemPrompt, userPrompt)
            .map(this::parseJson)
            .filter(node -> !node.isMissingNode() && !node.isEmpty());
    }

    public AgentDto.AgentAnswerDto enrichAnswer(AgentDto.AgentAnswerDto fallback) {
        String prompt = """
            Rewrite this warehouse answer in Chinese.
            Keep facts exactly consistent with the context.
            Return strict JSON with keys: answer, highlights, suggestedActions.
            highlights and suggestedActions must be string arrays.
            Context:
            %s
            """.formatted(toJson(fallback));
        return requestStructuredJson(answerSystemPrompt(), prompt)
            .map(node -> new AgentDto.AgentAnswerDto(
                fallback.query(),
                fallback.intent(),
                readText(node, "answer", fallback.answer()),
                fallback.highlights().isEmpty() ? readList(node, "highlights", fallback.highlights()) : readList(node, "highlights", fallback.highlights()),
                fallback.columns(),
                fallback.rows(),
                readList(node, "suggestedActions", fallback.suggestedActions())
            ))
            .orElse(fallback);
    }

    public AgentDto.ReportInsightDto enrichReportInsight(AgentDto.ReportInsightDto fallback) {
        String prompt = """
            Rewrite this report insight in Chinese for a warehouse operator.
            Keep all numbers and named entities consistent with the context.
            Return strict JSON with keys: narrative, highlights, suggestedActions.
            Context:
            %s
            """.formatted(toJson(fallback));
        return requestStructuredJson(insightSystemPrompt(), prompt)
            .map(node -> new AgentDto.ReportInsightDto(
                fallback.periodLabel(),
                fallback.currentSales(),
                fallback.previousSales(),
                fallback.salesChangeRate(),
                readText(node, "narrative", fallback.narrative()),
                fallback.leadingProductName(),
                fallback.leadingProductAmount(),
                fallback.leadingCustomerName(),
                fallback.leadingCustomerAmount(),
                readList(node, "highlights", fallback.highlights()),
                readList(node, "suggestedActions", fallback.suggestedActions())
            ))
            .orElse(fallback);
    }

    public AgentDto.OperationDraftDto enrichDraft(String instruction, AgentDto.OperationDraftDto fallback) {
        String prompt = """
            Refine this draft response in Chinese for a warehouse operator.
            Do not change operationType, partner identity, items, quantities, unit prices, stock, or canSubmit.
            Return strict JSON with keys: summary, warnings, suggestedActions.
            Instruction:
            %s
            Context:
            %s
            """.formatted(instruction, toJson(fallback));
        return requestStructuredJson(draftSystemPrompt(), prompt)
            .map(node -> new AgentDto.OperationDraftDto(
                fallback.operationType(),
                readText(node, "summary", fallback.summary()),
                fallback.partnerRole(),
                fallback.partnerId(),
                fallback.partnerName(),
                fallback.items(),
                fallback.notes(),
                fallback.canSubmit(),
                readList(node, "warnings", fallback.warnings()),
                readList(node, "suggestedActions", fallback.suggestedActions())
            ))
            .orElse(fallback);
    }

    private JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(cleanJsonPayload(raw));
        } catch (Exception ex) {
            log.warn("Agent LLM JSON parse failed: {}", ex.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String cleanJsonPayload(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            if (firstNewline >= 0) {
                value = value.substring(firstNewline + 1);
            }
            if (value.endsWith("```")) {
                value = value.substring(0, value.length() - 3);
            }
        }
        return value.trim();
    }

    private String readText(JsonNode node, String fieldName, String fallback) {
        String value = node.path(fieldName).asText("");
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private List<String> readList(JsonNode node, String fieldName, List<String> fallback) {
        JsonNode listNode = node.path(fieldName);
        if (!listNode.isArray()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        listNode.forEach(item -> {
            String text = item.asText("").trim();
            if (StringUtils.hasText(text)) {
                values.add(text);
            }
        });
        return values.isEmpty() ? fallback : values;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Agent LLM context serialization failed: {}", ex.getMessage());
            return "{}";
        }
    }

    private String answerSystemPrompt() {
        return """
            You are a warehouse operations copilot.
            Respond in concise Chinese.
            Never invent metrics, product names, customer names, supplier names, or actions that are not supported by the context.
            """;
    }

    private String insightSystemPrompt() {
        return """
            You are a warehouse reporting analyst.
            Respond in concise Chinese for operations staff.
            Emphasize trends, risks, and next actions without changing facts.
            """;
    }

    private String draftSystemPrompt() {
        return """
            You are a warehouse order drafting assistant.
            Respond in concise Chinese.
            Preserve all structured facts and only improve summary, warnings, and suggested actions.
            """;
    }
}
