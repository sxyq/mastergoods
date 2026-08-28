package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * 生图草稿工具。
 *
 * <p>工具阶段只保存生图参数到 agent_drafts，不访问 Provider。用户确认草稿后，
 * {@code AgentDraftConfirmService} 才调用 AgentImageService 执行正式生图动作。
 */
@Component
public class ImageGenerateTool extends ToolSupport {

    private static final int MAX_PROMPT_LENGTH = 2000;
    private static final int MAX_REFERENCE_ASSETS = 1;

    private final AgentDraftRepository agentDraftRepository;

    public ImageGenerateTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "image_generate";
    }

    @Override
    public String displayName() {
        return "生成图片";
    }

    @Override
    public String description() {
        return "根据提示词和可选的当前账号参考图生成图片草稿；必须先由用户确认，确认前不会调用生图服务或写入正式业务数据";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public String requiredPermission() {
        return "agent:write";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectSchema();
        addStringProperty(schema, "prompt", "生图提示词，1 到 2000 个字符");
        schema.with("properties").with("prompt")
            .put("minLength", 1)
            .put("maxLength", MAX_PROMPT_LENGTH);

        ObjectNode referenceIdSchema = JsonNodeFactory.instance.objectNode();
        referenceIdSchema.put("type", "integer");
        referenceIdSchema.put("minimum", 1);
        addArrayProperty(
            schema,
            "reference_asset_ids",
            "当前账号的参考图片资源 ID，最多 1 个",
            referenceIdSchema,
            null
        );
        schema.with("properties").with("reference_asset_ids").put("maxItems", MAX_REFERENCE_ASSETS);
        addRequired(schema, "prompt");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        String prompt = paramString(params, "prompt");
        if (prompt == null) {
            return failed("提示词不能为空");
        }
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            return failed("提示词超过最大长度");
        }

        List<Long> referenceAssetIds = readReferenceAssetIds(params);
        if (referenceAssetIds == null) {
            return failed("参考图片资源 ID 必须是最多 1 个正整数的数组");
        }

        Map<String, Object> input = mapOf(
            "prompt", prompt,
            "reference_asset_ids", referenceAssetIds
        );
        ToolAudit audit = startAudit(ctx, name(), input);
        String title = "生成图片：" + prompt.substring(0, Math.min(prompt.length(), 240));
        String contentJson;
        try {
            contentJson = ctx.objectMapper().writeValueAsString(mapOf(
                "prompt", prompt,
                "reference_asset_ids", referenceAssetIds
            ));
        } catch (JsonProcessingException ex) {
            String error = "生图草稿序列化失败";
            emitToolFailed(ctx, name(), error);
            return ToolResult.failure(name(), error);
        }

        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ctx.ownerUserId());
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType(name());
        draft.setTitle(title);
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);
        Long draftId = saved.getId();

        emitToolCompleted(ctx, name(), "生成生图草稿 #" + draftId, audit);
        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft_card",
            "生图草稿待确认",
            toJsonNode(ctx, mapOf(
                "draft_id", draftId,
                "draft_type", name(),
                "title", title,
                "fields", List.of(
                    mapOf("label", "提示词", "value", prompt),
                    mapOf("label", "参考图数量", "value", referenceAssetIds.size())
                )
            ))
        );
        JsonNode facts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", name(),
            "title", title,
            "status", "active",
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), facts, "生成生图草稿 " + title + "（#" + draftId + "）");
    }

    private List<Long> readReferenceAssetIds(JsonNode params) {
        JsonNode node = params == null ? null : params.get("reference_asset_ids");
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (!node.isArray() || node.size() > MAX_REFERENCE_ASSETS) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isIntegralNumber() || !item.canConvertToLong() || item.asLong() <= 0L) {
                return null;
            }
            ids.add(item.asLong());
        }
        return ids;
    }

    private ToolResult failed(String message) {
        return ToolResult.failure(name(), message);
    }

}
