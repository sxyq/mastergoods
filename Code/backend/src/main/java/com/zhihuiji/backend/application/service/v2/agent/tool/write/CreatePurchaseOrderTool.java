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
 * 采购单草稿生成工具（CREATE_ONLY）。
 *
 * <p>不直接创建采购单，而是将用户意图序列化为草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后再调用 V2PurchaseOrderService.create 完成实际创建。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreatePurchaseOrderTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;
    private final AgentEntityReferenceValidator referenceValidator;

    @Autowired
    public CreatePurchaseOrderTool(
        AgentDraftRepository agentDraftRepository,
        AgentEntityReferenceValidator referenceValidator
    ) {
        this.agentDraftRepository = agentDraftRepository;
        this.referenceValidator = referenceValidator;
    }

    public CreatePurchaseOrderTool(AgentDraftRepository agentDraftRepository) {
        this(agentDraftRepository, null);
    }

    @Override
    public String name() {
        return "create_purchase_order";
    }

    @Override
    public String displayName() {
        return "创建采购单";
    }

    @Override
    public String description() {
        return "根据用户描述生成采购单草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "supplier_name", "供应商名称");
        addIntegerProperty(schema, "supplier_id", "供应商 ID");
        schema.with("properties").with("supplier_id").put("minimum", 1);
        addStringProperty(schema, "remark", "采购单备注");
        addArrayProperty(schema, "items", "采购商品明细", purchaseItemSchema(), 1);
        addRequired(schema, "supplier_id", "supplier_name", "items");
        return schema;
    }

    private ObjectNode purchaseItemSchema() {
        var item = objectSchema();
        addIntegerProperty(item, "product_id", "商品 ID");
        item.with("properties").with("product_id").put("minimum", 1);
        addStringProperty(item, "product_code", "商品编码");
        addStringProperty(item, "product_name", "商品名称");
        addNumberProperty(item, "quantity", "采购数量，必须大于 0", 0.000001D);
        addNumberProperty(item, "price", "采购单价，不能为负数", 0D);
        addRequired(item, "product_id", "product_name", "quantity", "price");
        return item;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String supplierName = paramString(params, "supplier_name");
        Long supplierId = paramLong(params, "supplier_id", null);
        String remark = paramString(params, "remark");
        Map<String, Object> input = mapOf(
            "supplier_name", supplierName == null ? "" : supplierName,
            "supplier_id", supplierId,
            "remark", remark == null ? "" : remark,
            "items_count", countItems(params)
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (supplierName == null) {
            emitToolFailed(ctx, name(), "缺少必填参数 supplier_name");
            return ToolResult.failure(name(), "缺少必填参数 supplier_name");
        }

        List<Map<String, Object>> items = extractPurchaseItems(params);
        if (referenceValidator != null
            && (!referenceValidator.supplierMatches(ownerUserId, supplierId, supplierName)
                || !hasOwnedProducts(ctx, params))) {
            String error = "供应商或商品不属于当前账号，无法生成采购草稿";
            emitToolFailed(ctx, name(), error);
            return ToolResult.failure(name(), error);
        }
        Map<String, Object> content = mapOf(
            "supplier_id", supplierId,
            "supplier_name", supplierName,
            "items", items,
            "settlement_method", null,
            "warehouse_id", null,
            "notes", remark,
            "status", null
        );
        String contentJson = ctx.objectMapper().valueToTree(content).toString();

        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ownerUserId);
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType("create_purchase_order");
        draft.setTitle("新建采购单：" + supplierName);
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);

        emitToolCompleted(ctx, name(), "已生成采购单草稿 ID " + saved.getId(), audit);

        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft",
            "新建采购单草稿",
            toJsonNode(ctx, mapOf(
                "draft_id", saved.getId(),
                "draft_type", saved.getDraftType(),
                "title", saved.getTitle(),
                "status", saved.getStatus(),
                "supplier_name", supplierName,
                "items_count", items.size()
            ))
        );
        String toolSummary = "生成采购单草稿 ID " + saved.getId() + "，供应商 " + supplierName + "，商品行 " + items.size();
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", saved.getId(),
            "draft_type", saved.getDraftType(),
            "title", saved.getTitle(),
            "status", saved.getStatus(),
            "supplier_name", supplierName,
            "items_count", items.size(),
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }

    private List<Map<String, Object>> extractPurchaseItems(JsonNode params) {
        List<Map<String, Object>> items = new ArrayList<>();
        JsonNode itemsNode = params == null ? null : params.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                items.add(mapOf(
                    "product_id", paramLong(item, "product_id", null),
                    "product_code", paramString(item, "product_code"),
                    "product_name", paramString(item, "product_name"),
                    "quantity", paramDouble(item, "quantity", null),
                    "unit_cost", paramDouble(item, "price", null)
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
                ctx.ownerUserId(), paramPositiveLong(item, "product_id"))) {
                return false;
            }
        }
        return true;
    }
}
