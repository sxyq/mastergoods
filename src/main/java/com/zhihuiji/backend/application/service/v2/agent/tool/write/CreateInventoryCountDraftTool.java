package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 库存盘点草稿工具（CREATE_ONLY）。
 *
 * <p>根据实际盘点数量与系统库存计算差异，生成库存调整草稿保存到 agent_drafts 表（status=active），
 * 等待用户确认后执行实际调整。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreateInventoryCountDraftTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CreateInventoryCountDraftTool(AgentDraftRepository agentDraftRepository,
                                         ProductRepository productRepository,
                                         ObjectMapper objectMapper) {
        this.agentDraftRepository = agentDraftRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "create_inventory_count_draft";
    }

    @Override
    public String displayName() {
        return "库存盘点草稿";
    }

    @Override
    public String description() {
        return "根据实际盘点数量与系统库存计算差异并生成库存调整草稿，确认后执行";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode productId = properties.putObject("product_id");
        productId.put("type", "integer");
        productId.put("description", "商品 ID（必填）");
        ObjectNode countedQuantity = properties.putObject("counted_quantity");
        countedQuantity.put("type", "number");
        countedQuantity.put("description", "实际盘点数量（必填）");
        ObjectNode note = properties.putObject("note");
        note.put("type", "string");
        note.put("description", "盘点备注（可选）");
        schema.putArray("required")
            .add("product_id")
            .add("counted_quantity");
        return schema;
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
        Double countedQuantity = paramDouble(params, "counted_quantity", null);
        if (countedQuantity == null) {
            String err = "实际盘点数量不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String note = paramString(params, "note");

        Map<String, Object> input = mapOf(
            "product_id", productId,
            "counted_quantity", countedQuantity,
            "note", note
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        Optional<ProductEntity> productOpt = productRepository.findByIdAndOwnerUserId(productId, ownerUserId);
        if (productOpt.isEmpty()) {
            String err = "未找到商品 " + productId;
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        ProductEntity product = productOpt.get();
        double systemQuantity = safeDouble(product.getStock());
        double difference = countedQuantity - systemQuantity;

        Map<String, Object> content = mapOf(
            "product_id", productId,
            "source_type", "inventory_count",
            "source_no", "count-" + nowBucket(),
            "quantity_change", difference,
            "notes", buildInventoryCountNotes(systemQuantity, countedQuantity, note)
        );
        String contentJson;
        try {
            contentJson = ctx.objectMapper().writeValueAsString(content);
        } catch (JsonProcessingException ex) {
            String err = "草稿序列化失败";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }

        String title = "库存盘点：" + product.getName();
        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ownerUserId);
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType("create_inventory_adjustment");
        draft.setTitle(title);
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);
        Long draftId = saved.getId();

        emitToolCompleted(ctx, name(), "生成盘点草稿 #" + draftId, audit);

        String direction = difference > 0 ? "盘盈" : (difference < 0 ? "盘亏" : "一致");
        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft_card",
            "盘点草稿待确认",
            toJsonNode(ctx, mapOf(
                "draft_id", draftId,
                "draft_type", "create_inventory_adjustment",
                "title", title,
                "fields", List.of(
                    mapOf("label", "商品ID", "value", String.valueOf(productId)),
                    mapOf("label", "商品名称", "value", product.getName()),
                    mapOf("label", "系统库存", "value", formatNumber(systemQuantity)),
                    mapOf("label", "盘点数量", "value", formatNumber(countedQuantity)),
                    mapOf("label", "差异", "value", formatNumber(difference) + "（" + direction + "）"),
                    mapOf("label", "备注", "value", note == null ? "-" : note)
                )
            ))
        );

        String answer = "已生成" + title + "草稿（编号 " + draftId + "），"
            + direction + " " + formatNumber(Math.abs(difference)) + "，请确认后执行调整。";
        String toolSummary = "生成盘点草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "create_inventory_adjustment",
            "title", title,
            "status", "active",
            "product_id", productId,
            "system_quantity", systemQuantity,
            "counted_quantity", countedQuantity,
            "difference", difference,
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, List.of(block), toolFacts, toolSummary);
    }

    private String buildInventoryCountNotes(double systemQuantity, double countedQuantity, String note) {
        String summary = "盘点记录：系统库存 " + formatNumber(systemQuantity)
            + "，盘点数量 " + formatNumber(countedQuantity);
        if (note == null || note.isBlank()) {
            return summary;
        }
        return summary + "；备注：" + note;
    }

    private String nowBucket() {
        return String.valueOf(System.currentTimeMillis());
    }
}
