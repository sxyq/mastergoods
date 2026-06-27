package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 库存调整草稿工具。
 *
 * <p>CREATE_ONLY 类型，不直接执行写入，而是构建草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后由 AgentDraftConfirmService 调用 V2InventoryService.createLedgerEntry 执行真正创建。
 *
 * <p>草稿 contentJson 字段与 {@code V2InventoryDtos.LedgerEntryCreateRequest}（snake_case）对齐，
 * 确认时可直接反序列化执行。工具入参 {@code quantity} 正数表示入库、负数表示出库，
 * 映射为 quantity_change；source_type 固定为 "agent_adjustment"。
 */
@Component
public class CreateInventoryAdjustmentTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreateInventoryAdjustmentTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_inventory_adjustment";
    }

    @Override
    public String displayName() {
        return "库存调整";
    }

    @Override
    public String description() {
        return "根据用户描述生成库存调整草稿，确认后执行";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long productId = paramLong(params, "product_id", null);
        if (productId == null) {
            String err = "商品ID不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        Double quantity = paramDouble(params, "quantity", null);
        if (quantity == null || quantity == 0D) {
            String err = "调整数量不能为空且不能为 0（正数入库/负数出库）";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String productName = paramString(params, "product_name");
        String reason = paramString(params, "reason");

        Map<String, Object> input = mapOf(
            "product_id", productId,
            "product_name", productName,
            "quantity", quantity,
            "reason", reason
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        Map<String, Object> draftContent = mapOf(
            "product_id", productId,
            "source_type", "agent_adjustment",
            "quantity_change", quantity,
            "notes", reason
        );
        String contentJson;
        try {
            contentJson = ctx.objectMapper().writeValueAsString(draftContent);
        } catch (JsonProcessingException ex) {
            String err = "草稿序列化失败";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }

        String title = "库存调整：" + (productName == null ? ("商品#" + productId) : productName);
        long now = System.currentTimeMillis();
        AgentDraftEntity entity = new AgentDraftEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(ctx.conversationId());
        entity.setDraftType("create_inventory_adjustment");
        entity.setTitle(title);
        entity.setContentJson(contentJson);
        entity.setStatus("active");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(entity);
        Long draftId = saved.getId();

        emitToolCompleted(ctx, name(), "生成草稿 #" + draftId, audit);

        String direction = quantity > 0 ? "入库" : "出库";
        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft_card",
            "草稿待确认",
            toJsonNode(ctx, mapOf(
                "draft_id", draftId,
                "draft_type", "create_inventory_adjustment",
                "title", title,
                "fields", List.of(
                    mapOf("label", "商品ID", "value", String.valueOf(productId)),
                    mapOf("label", "商品名称", "value", productName == null ? "-" : productName),
                    mapOf("label", "调整方向", "value", direction),
                    mapOf("label", "调整数量", "value", formatNumber(quantity)),
                    mapOf("label", "原因", "value", reason == null ? "-" : reason)
                )
            ))
        );

        String answer = "已生成" + title + "草稿（编号 " + draftId + "），"
            + direction + " " + formatNumber(quantity) + "，请确认后执行调整。";
        String toolSummary = "生成草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "create_inventory_adjustment",
            "title", title,
            "status", "active",
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, List.of(block), toolFacts, toolSummary);
    }
}
