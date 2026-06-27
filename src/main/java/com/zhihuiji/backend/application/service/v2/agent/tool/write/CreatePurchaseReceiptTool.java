package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 采购入库单草稿生成工具（CREATE_ONLY）。
 *
 * <p>不直接创建入库单，而是将用户意图序列化为草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后再调用 V2PurchaseReceiptService.create 完成实际创建。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreatePurchaseReceiptTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreatePurchaseReceiptTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_purchase_receipt";
    }

    @Override
    public String displayName() {
        return "创建采购入库单";
    }

    @Override
    public String description() {
        return "根据采购单生成入库草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long purchaseOrderId = paramLong(params, "purchase_order_id", null);
        Map<String, Object> input = mapOf(
            "purchase_order_id", purchaseOrderId,
            "items_count", countItems(params)
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (purchaseOrderId == null) {
            emitToolFailed(ctx, name(), "缺少必填参数 purchase_order_id");
            return ToolResult.failure(name(), "缺少必填参数 purchase_order_id");
        }

        List<Map<String, Object>> items = extractPurchaseItems(params);
        Map<String, Object> content = mapOf(
            "purchase_order_id", purchaseOrderId,
            "supplier_id", null,
            "supplier_name", null,
            "items", items,
            "notes", null
        );
        String contentJson = ctx.objectMapper().valueToTree(content).toString();

        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ownerUserId);
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType("create_purchase_receipt");
        draft.setTitle("新建采购入库单");
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);

        emitToolCompleted(ctx, name(), "已生成采购入库单草稿 ID " + saved.getId(), audit);

        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft",
            "新建采购入库单草稿",
            toJsonNode(ctx, mapOf(
                "draft_id", saved.getId(),
                "draft_type", saved.getDraftType(),
                "title", saved.getTitle(),
                "status", saved.getStatus(),
                "purchase_order_id", purchaseOrderId,
                "items_count", items.size()
            ))
        );
        String answer = "已生成采购入库单草稿（ID: " + saved.getId() + "），请确认后执行。";
        String toolSummary = "生成采购入库单草稿 ID " + saved.getId() + "，采购单 " + purchaseOrderId + "，入库行 " + items.size();
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", saved.getId(),
            "draft_type", saved.getDraftType(),
            "title", saved.getTitle(),
            "status", saved.getStatus(),
            "purchase_order_id", purchaseOrderId,
            "items_count", items.size(),
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, List.of(block), toolFacts, toolSummary);
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
}
