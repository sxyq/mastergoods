package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentEntityReferenceValidator;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 销售单草稿生成工具（CREATE_ONLY）。
 *
 * <p>不直接创建销售单，而是将用户意图序列化为草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后再调用 V2SaleOrderService.create 完成实际创建。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreateSaleOrderTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;
    private final AgentEntityReferenceValidator referenceValidator;

    @Autowired
    public CreateSaleOrderTool(
        AgentDraftRepository agentDraftRepository,
        AgentEntityReferenceValidator referenceValidator
    ) {
        this.agentDraftRepository = agentDraftRepository;
        this.referenceValidator = referenceValidator;
    }

    public CreateSaleOrderTool(AgentDraftRepository agentDraftRepository) {
        this(agentDraftRepository, null);
    }

    @Override
    public String name() {
        return "create_sale_order";
    }

    @Override
    public String displayName() {
        return "创建销售单";
    }

    @Override
    public String description() {
        return "根据用户描述生成销售单草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public java.util.List<String> dependsOn() {
        // 客户与销售商品均需通过当前 owner/store 校验的真实查询结果。
        return java.util.List.of("customer_directory_lookup", "product_catalog_lookup");
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "customer_name", "客户名称");
        addIntegerProperty(schema, "customer_id", "客户 ID");
        schema.with("properties").with("customer_id").put("minimum", 1);
        addStringProperty(schema, "remark", "销售单备注");
        addArrayProperty(schema, "items", "销售商品明细", saleOrderItemSchema(), 1);
        addRequired(schema, "customer_id", "customer_name", "items");
        return schema;
    }

    private ObjectNode saleOrderItemSchema() {
        var item = objectSchema();
        addIntegerProperty(item, "product_id", "商品 ID");
        item.with("properties").with("product_id").put("minimum", 1);
        addStringProperty(item, "product_name", "商品名称");
        addNumberProperty(item, "quantity", "销售数量，必须大于 0", 0.000001D);
        addNumberProperty(item, "price", "销售单价，必须大于 0", 0.000001D);
        addRequired(item, "product_id", "product_name", "quantity", "price");
        return item;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String customerName = paramString(params, "customer_name");
        Long customerId = positiveIntegralLong(params, "customer_id");
        String remark = paramString(params, "remark");
        Map<String, Object> input = mapOf(
            "customer_name", customerName == null ? "" : customerName,
            "customer_id", customerId,
            "remark", remark == null ? "" : remark,
            "items_count", countItems(params)
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (customerName == null) {
            emitToolFailed(ctx, name(), "缺少必填参数 customer_name");
            return ToolResult.failure(name(), "缺少必填参数 customer_name");
        }
        String inputError = validateSaleOrderInput(params);
        if (inputError != null) {
            emitToolFailed(ctx, name(), inputError);
            return ToolResult.failure(name(), inputError);
        }

        List<Map<String, Object>> items = extractSaleOrderItems(params);
        if (referenceValidator != null
            && (!referenceValidator.customerMatches(ownerUserId, customerId, customerName)
                || !hasOwnedProducts(ctx, params))) {
            String error = "客户或商品不属于当前账号，无法生成销售草稿";
            emitToolFailed(ctx, name(), error);
            return ToolResult.failure(name(), error);
        }
        Map<String, Object> content = mapOf(
            "customer_id", customerId,
            "customer_name", customerName,
            "items", items,
            "notes", remark,
            "discount_amount", null
        );
        String contentJson = ctx.objectMapper().valueToTree(content).toString();

        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ownerUserId);
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType("create_sale_order");
        draft.setTitle("新建销售单：" + customerName);
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);

        emitToolCompleted(ctx, name(), "已生成销售单草稿 ID " + saved.getId(), audit);

        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft",
            "新建销售单草稿",
            toJsonNode(ctx, mapOf(
                "draft_id", saved.getId(),
                "draft_type", saved.getDraftType(),
                "title", saved.getTitle(),
                "status", saved.getStatus(),
                "customer_name", customerName,
                "items_count", items.size()
            ))
        );
        String toolSummary = "生成销售单草稿 ID " + saved.getId() + "，客户 " + customerName + "，商品行 " + items.size();
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", saved.getId(),
            "draft_type", saved.getDraftType(),
            "title", saved.getTitle(),
            "status", saved.getStatus(),
            "customer_name", customerName,
            "items_count", items.size(),
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }

    private List<Map<String, Object>> extractSaleOrderItems(JsonNode params) {
        List<Map<String, Object>> items = new ArrayList<>();
        JsonNode itemsNode = params == null ? null : params.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                items.add(mapOf(
                    "product_id", positiveIntegralLong(item, "product_id"),
                    "product_name", paramString(item, "product_name"),
                    "quantity", positiveFiniteDouble(item, "quantity"),
                    "unit_price", positiveFiniteDouble(item, "price")
                ));
            }
        }
        return items;
    }

    private int countItems(JsonNode params) {
        JsonNode itemsNode = params == null ? null : params.get("items");
        return itemsNode != null && itemsNode.isArray() ? itemsNode.size() : 0;
    }

    private boolean hasOwnedProducts(ToolContext ctx, JsonNode params) {
        JsonNode items = params == null ? null : params.get("items");
        if (items == null || !items.isArray() || items.isEmpty()) {
            return false;
        }
        for (JsonNode item : items) {
            if (!referenceValidator.productBelongsToOwner(
                ctx.ownerUserId(), positiveIntegralLong(item, "product_id"))) {
                return false;
            }
        }
        return true;
    }

    private String validateSaleOrderInput(JsonNode params) {
        JsonNode customerIdNode = params == null ? null : params.get("customer_id");
        if (customerIdNode == null || customerIdNode.isNull()) {
            return "缺少必填参数 customer_id";
        }
        if (positiveIntegralLong(params, "customer_id") == null) {
            return "customer_id 必须是正整数";
        }

        JsonNode items = params.get("items");
        if (items == null || !items.isArray() || items.isEmpty()) {
            return "至少需要一条商品明细";
        }
        for (JsonNode item : items) {
            if (item == null || !item.isObject()) {
                return "商品明细格式无效";
            }
            if (positiveIntegralLong(item, "product_id") == null) {
                return "商品明细 product_id 必须是正整数";
            }
            if (paramString(item, "product_name") == null) {
                return "商品名称不能为空";
            }
            if (positiveFiniteDouble(item, "quantity") == null) {
                return "商品数量必须是大于 0 的有限数字";
            }
            if (positiveFiniteDouble(item, "price") == null) {
                return "商品单价必须是大于 0 的有限数字";
            }
        }
        return null;
    }

    private Long positiveIntegralLong(JsonNode params, String key) {
        JsonNode node = params == null ? null : params.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            long value = node.asLong();
            return value > 0L ? value : null;
        }
        if (!node.isTextual()) {
            return null;
        }
        String text = node.asText().trim();
        if (!text.matches("\\+?[0-9]+")) {
            return null;
        }
        try {
            long value = Long.parseLong(text);
            return value > 0L ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double positiveFiniteDouble(JsonNode params, String key) {
        JsonNode node = params == null ? null : params.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        double value;
        try {
            if (node.isNumber() || node.isTextual()) {
                value = node.isNumber() ? node.asDouble() : Double.parseDouble(node.asText().trim());
            } else {
                return null;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return Double.isFinite(value) && value > 0D ? value : null;
    }
}
