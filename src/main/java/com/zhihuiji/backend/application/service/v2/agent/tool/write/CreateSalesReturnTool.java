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
 * 销售退货单草稿生成工具（CREATE_ONLY）。
 *
 * <p>不直接创建退货单，而是将用户意图序列化为草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后再调用 V2SalesReturnService.create 完成实际创建。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreateSalesReturnTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreateSalesReturnTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_sales_return";
    }

    @Override
    public String displayName() {
        return "创建销售退货单";
    }

    @Override
    public String description() {
        return "根据销售单生成退货草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long saleOrderId = paramLong(params, "sale_order_id", null);
        String reason = paramString(params, "reason");
        Map<String, Object> input = mapOf(
            "sale_order_id", saleOrderId,
            "reason", reason == null ? "" : reason,
            "items_count", countItems(params)
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (saleOrderId == null) {
            emitToolFailed(ctx, name(), "缺少必填参数 sale_order_id");
            return ToolResult.failure(name(), "缺少必填参数 sale_order_id");
        }

        List<Map<String, Object>> items = extractReturnItems(params);
        Map<String, Object> content = mapOf(
            "original_order_id", saleOrderId,
            "customer_id", null,
            "customer_name", null,
            "items", items,
            "notes", reason
        );
        String contentJson = ctx.objectMapper().valueToTree(content).toString();

        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ownerUserId);
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType("create_sales_return");
        draft.setTitle("新建销售退货单");
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);

        emitToolCompleted(ctx, name(), "已生成销售退货单草稿 ID " + saved.getId(), audit);

        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft",
            "新建销售退货单草稿",
            toJsonNode(ctx, mapOf(
                "draft_id", saved.getId(),
                "draft_type", saved.getDraftType(),
                "title", saved.getTitle(),
                "status", saved.getStatus(),
                "sale_order_id", saleOrderId,
                "items_count", items.size()
            ))
        );
        String answer = "已生成销售退货单草稿（ID: " + saved.getId() + "），请确认后执行。";
        String toolSummary = "生成销售退货单草稿 ID " + saved.getId() + "，销售单 " + saleOrderId + "，退货行 " + items.size();
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", saved.getId(),
            "draft_type", saved.getDraftType(),
            "title", saved.getTitle(),
            "status", saved.getStatus(),
            "sale_order_id", saleOrderId,
            "items_count", items.size(),
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, List.of(block), toolFacts, toolSummary);
    }

    private List<Map<String, Object>> extractReturnItems(JsonNode params) {
        List<Map<String, Object>> items = new ArrayList<>();
        JsonNode itemsNode = params == null ? null : params.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                items.add(mapOf(
                    "product_id", paramLong(item, "product_id", null),
                    "product_name", paramString(item, "product_name"),
                    "quantity", paramDouble(item, "quantity", null),
                    "unit_price", paramDouble(item, "price", null)
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
