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
 * 销售单草稿生成工具（CREATE_ONLY）。
 *
 * <p>不直接创建销售单，而是将用户意图序列化为草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后再调用 V2SaleOrderService.create 完成实际创建。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreateSaleOrderTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreateSaleOrderTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
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
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String customerName = paramString(params, "customer_name");
        Long customerId = paramLong(params, "customer_id", null);
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

        List<Map<String, Object>> items = extractSaleOrderItems(params);
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
        String answer = "已生成销售单草稿（ID: " + saved.getId() + "），请确认后执行。";
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
        return ToolResult.success(answer, List.of(block), toolFacts, toolSummary);
    }

    private List<Map<String, Object>> extractSaleOrderItems(JsonNode params) {
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
