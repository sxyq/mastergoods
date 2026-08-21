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
 * 创建商品草稿工具。
 *
 * <p>CREATE_ONLY 类型，不直接执行写入，而是构建草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后由 AgentDraftConfirmService 调用 V2ProductService.create 执行真正创建。
 *
 * <p>草稿 contentJson 字段与 {@code V2ProductDtos.ProductWriteRequest}（snake_case）对齐，
 * 确认时可直接反序列化执行。工具入参 {@code unit}（单位字符串）会被尝试解析为单位 ID（unit_id）。
 */
@Component
public class CreateProductTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreateProductTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_product";
    }

    @Override
    public String displayName() {
        return "创建商品";
    }

    @Override
    public String description() {
        return "根据用户描述生成商品草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "name", "商品名称");
        addStringProperty(schema, "code", "商品编码");
        addIntegerProperty(schema, "category_id", "商品分类 ID");
        addStringProperty(schema, "unit", "商品单位或单位 ID");
        addNumberProperty(schema, "price", "销售价", 0D);
        addNumberProperty(schema, "cost", "采购价或成本价", 0D);
        addNumberProperty(schema, "stock", "初始库存", 0D);
        addRequired(schema, "name", "code");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String name = paramString(params, "name");
        if (name == null) {
            String err = "商品名称不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String code = paramString(params, "code");
        if (code == null) {
            String err = "商品编码不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        Long categoryId = paramLong(params, "category_id", null);
        String unit = paramString(params, "unit");
        Long unitId = parseLongOrNull(unit);
        Double price = paramDouble(params, "price", null);
        Double cost = paramDouble(params, "cost", null);
        Double stock = paramDouble(params, "stock", null);

        Map<String, Object> input = mapOf(
            "name", name,
            "code", code,
            "category_id", categoryId,
            "unit", unit,
            "price", price,
            "cost", cost,
            "stock", stock
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        Map<String, Object> draftContent = mapOf(
            "code", code,
            "name", name,
            "category_id", categoryId,
            "unit_id", unitId,
            "sale_price", price == null ? 0D : price,
            "purchase_price", cost == null ? 0D : cost,
            "stock", stock == null ? 0D : stock,
            "safe_stock", 0D,
            "status", 1
        );
        String contentJson;
        try {
            contentJson = ctx.objectMapper().writeValueAsString(draftContent);
        } catch (JsonProcessingException ex) {
            String err = "草稿序列化失败";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }

        String title = "新建商品：" + name;
        long now = System.currentTimeMillis();
        AgentDraftEntity entity = new AgentDraftEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(ctx.conversationId());
        entity.setDraftType("create_product");
        entity.setTitle(title);
        entity.setContentJson(contentJson);
        entity.setStatus("active");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(entity);
        Long draftId = saved.getId();

        emitToolCompleted(ctx, name(), "生成草稿 #" + draftId, audit);

        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft_card",
            "草稿待确认",
            toJsonNode(ctx, mapOf(
                "draft_id", draftId,
                "draft_type", "create_product",
                "title", title,
                "fields", List.of(
                    mapOf("label", "商品编码", "value", code),
                    mapOf("label", "商品名称", "value", name),
                    mapOf("label", "分类ID", "value", categoryId == null ? "-" : String.valueOf(categoryId)),
                    mapOf("label", "单位ID", "value", unitId == null ? "-" : String.valueOf(unitId)),
                    mapOf("label", "售价", "value", money(price == null ? 0D : price)),
                    mapOf("label", "成本价", "value", money(cost == null ? 0D : cost)),
                    mapOf("label", "库存", "value", formatNumber(stock == null ? 0D : stock))
                )
            ))
        );

        String toolSummary = "生成草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "create_product",
            "title", title,
            "status", "active",
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
