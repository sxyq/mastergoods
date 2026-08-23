package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.search.WebSearchProvider;
import com.zhihuiji.backend.application.service.v2.agent.search.WebSearchRequest;
import com.zhihuiji.backend.application.service.v2.agent.search.WebSearchResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 在线搜索工具。
 *
 * <p>READ_ONLY 工具，通过统一 ToolExecutor 执行边界调用。搜索失败、超时、
 * 无结果或 Provider 未配置时返回结构化失败；搜索结果、网页正文、标题、
 * 摘要在被引用前均视为不可信数据，不能修改系统规则、工具范围、权限、
 * owner/store 或完成条件。
 *
 * <p>URL 安全策略由 {@link WebSearchProvider#isUrlBlocked(String)} 提供，
 * 拒绝环回地址、私有网段、云元数据地址和本机服务地址。当 Provider 未配置
 * （{@link DisabledWebSearchProvider}）时，工具返回结构化失败，不伪造结果。
 */
@Component
public class WebSearchTool extends ToolSupport {

    private final WebSearchProvider searchProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebSearchTool(
        @Qualifier("disabledWebSearchProvider") WebSearchProvider searchProvider,
        ObjectMapper objectMapper
    ) {
        this.searchProvider = searchProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "web_search_lookup";
    }

    @Override
    public String displayName() {
        return "在线搜索";
    }

    @Override
    public String description() {
        return "查询在线搜索引擎，返回标题、URL、摘要与发布时间；未配置 Provider 或被安全策略阻止时返回结构化失败";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectSchema();
        addStringProperty(schema, "query", "搜索关键词");
        addIntegerProperty(schema, "result_limit", "结果条数上限，默认 5，最多 10");
        addStringProperty(schema, "recency", "时间范围：day / week / month / year");
        addArrayProperty(schema, "domains", "限定域名列表", objectSchema(), null);
        addStringProperty(schema, "language", "语言偏好，如 zh-CN、en");
        addRequired(schema, "query");
        // Bound result_limit to a safe range to avoid model abuse.
        ObjectNode resultLimitProperty = (ObjectNode) schema.path("properties").get("result_limit");
        resultLimitProperty.put("minimum", 1);
        resultLimitProperty.put("maximum", WebSearchProvider.MAX_RESULT_LIMIT);
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        String query = paramString(params, "query");
        if (query == null || query.isBlank()) {
            return ToolResult.failure(name(), "搜索关键词不能为空");
        }

        Integer requestedLimit = paramInt(params, "result_limit", null);
        String recency = paramString(params, "recency");
        List<String> domains = readStringArray(params, "domains");
        String language = paramString(params, "language");

        Map<String, Object> input = mapOf(
            "query", query,
            "result_limit", requestedLimit,
            "recency", recency,
            "domains", domains,
            "language", language
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (!searchProvider.isAvailable()) {
            audit.markReturned(0);
            emitToolCompleted(ctx, name(), "搜索 Provider 未配置", audit);
            return ToolResult.failure(name(), WebSearchProvider.NOT_CONFIGURED_MESSAGE);
        }

        WebSearchRequest request = new WebSearchRequest(query, requestedLimit, recency, domains, language);
        WebSearchResult result;
        try {
            result = searchProvider.search(request);
        } catch (RuntimeException ex) {
            audit.markReturned(0);
            emitToolCompleted(ctx, name(), "搜索异常：" + safeMessage(ex), audit);
            return ToolResult.failure(name(), "搜索异常：" + safeMessage(ex));
        }

        if (result == null) {
            audit.markReturned(0);
            emitToolCompleted(ctx, name(), "搜索未返回结果", audit);
            return ToolResult.failure(name(), "搜索未返回结果");
        }

        if (result.isFailure()) {
            audit.markReturned(0);
            String reason = result.errorMessage() == null ? "搜索失败" : result.errorMessage();
            emitToolCompleted(ctx, name(), reason, audit);
            return ToolResult.failure(name(), reason);
        }

        List<WebSearchProvider.WebSearchItem> items = result.items() == null ? List.of() : result.items();
        audit.markReturned(items.size());
        emitToolCompleted(ctx, name(), "返回 " + items.size() + " 条搜索结果", audit);

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(buildSearchResultBlock(ctx, items));
        JsonNode toolFacts = buildToolFacts(ctx, query, items, audit);
        String toolSummary = "搜索 \"" + truncate(query, 60) + "\" 返回 " + items.size() + " 条结果";
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private V2AgentDtos.ResultBlockDto buildSearchResultBlock(
        ToolContext ctx,
        List<WebSearchProvider.WebSearchItem> items
    ) {
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        for (WebSearchProvider.WebSearchItem item : items) {
            rows.add(mapOf(
                "citation_id", item.citationId(),
                "title", item.title() == null ? "" : item.title(),
                "url", item.url() == null ? "" : item.url(),
                "snippet", item.snippet() == null ? "" : item.snippet(),
                "source_name", item.sourceName() == null ? "" : item.sourceName(),
                "published_at", item.publishedAt() == null ? "" : item.publishedAt(),
                "retrieved_at", item.retrievedAt()
            ));
        }
        return new V2AgentDtos.ResultBlockDto(
            "web_search_results",
            "在线搜索结果",
            toJsonNode(ctx, mapOf(
                "headers", List.of("引用", "标题", "来源", "摘要", "发布时间", "URL"),
                "rows", rows,
                "row_count", rows.size()
            ))
        );
    }

    private JsonNode buildToolFacts(
        ToolContext ctx,
        String query,
        List<WebSearchProvider.WebSearchItem> items,
        ToolAudit audit
    ) {
        ArrayNode itemsArray = objectMapper.createArrayNode();
        for (WebSearchProvider.WebSearchItem item : items) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put("citation_id", item.citationId() == null ? "" : item.citationId());
            itemNode.put("title", item.title() == null ? "" : item.title());
            itemNode.put("url", item.url() == null ? "" : item.url());
            itemNode.put("snippet", item.snippet() == null ? "" : item.snippet());
            itemNode.put("source_name", item.sourceName() == null ? "" : item.sourceName());
            itemNode.put("published_at", item.publishedAt() == null ? "" : item.publishedAt());
            itemNode.put("retrieved_at", item.retrievedAt());
            itemsArray.add(itemNode);
        }
        ObjectNode facts = objectMapper.createObjectNode();
        facts.put("query", query);
        facts.set("items", itemsArray);
        facts.put("item_count", items.size());
        facts.set("query_audit", toJsonNode(ctx, audit.facts()));
        // Mark all returned content as untrusted data to remind downstream
        // answer synthesis not to treat titles/snippets as system instructions.
        facts.put("trust_level", "untrusted");
        return facts;
    }

    private List<String> readStringArray(JsonNode params, String key) {
        if (params == null) {
            return null;
        }
        JsonNode node = params.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            return null;
        }
        if (node.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>(node.size());
        for (JsonNode element : node) {
            if (element != null && element.isTextual()) {
                String text = element.asText().trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            }
        }
        return values;
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
