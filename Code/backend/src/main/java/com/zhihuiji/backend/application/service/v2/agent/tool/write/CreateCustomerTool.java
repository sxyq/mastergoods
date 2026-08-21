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
 * 创建客户草稿工具。
 *
 * <p>CREATE_ONLY 类型，不直接执行写入，而是构建草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后由 AgentDraftConfirmService 调用 V2CustomerService.create 执行真正创建。
 *
 * <p>草稿 contentJson 字段与 {@code V2PartnerDtos.CustomerWriteRequest}（snake_case）对齐，
 * 确认时可直接反序列化执行。
 */
@Component
public class CreateCustomerTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreateCustomerTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_customer";
    }

    @Override
    public String displayName() {
        return "创建客户";
    }

    @Override
    public String description() {
        return "根据用户提供的客户名称、手机号和可选备注生成客户草稿，确认后创建；客户分组不是必填信息，只有用户明确指定或要求分组时才填写 group_id，不要为了可选分组先查询客户分组";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "name", "客户名称");
        schema.with("properties").with("name").put("minLength", 1);
        addStringProperty(schema, "phone", "客户手机号");
        addIntegerProperty(schema, "group_id", "客户分组 ID，可选；只有用户明确指定分组时填写");
        addStringProperty(schema, "remark", "客户备注");
        addRequired(schema, "name");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String name = paramString(params, "name");
        String phone = paramString(params, "phone");
        Long groupId = paramLong(params, "group_id", null);
        String remark = paramString(params, "remark");
        Map<String, Object> input = mapOf(
            "name", name,
            "phone", phone,
            "group_id", groupId,
            "remark", remark
        );
        ToolAudit audit = startAudit(ctx, name(), input);
        if (name == null) {
            String err = "客户名称不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }

        Map<String, Object> draftContent = mapOf(
            "name", name,
            "phone", phone == null ? "" : phone,
            "level", 1,
            "group_id", groupId,
            "notes", remark,
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

        String title = "新建客户：" + name;
        long now = System.currentTimeMillis();
        AgentDraftEntity entity = new AgentDraftEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(ctx.conversationId());
        entity.setDraftType("create_customer");
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
                "draft_type", "create_customer",
                "title", title,
                "fields", List.of(
                    mapOf("label", "客户名称", "value", name),
                    mapOf("label", "手机号", "value", phone == null ? "-" : phone),
                    mapOf("label", "客户分组", "value", groupId == null ? "-" : String.valueOf(groupId)),
                    mapOf("label", "备注", "value", remark == null ? "-" : remark)
                )
            ))
        );

        String toolSummary = "生成草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "create_customer",
            "title", title,
            "status", "active",
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }
}
