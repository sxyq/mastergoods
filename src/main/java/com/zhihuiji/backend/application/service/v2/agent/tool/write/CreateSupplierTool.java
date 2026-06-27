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
 * 创建供应商草稿工具。
 *
 * <p>CREATE_ONLY 类型，不直接执行写入，而是构建草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后由 AgentDraftConfirmService 调用 V2SupplierService.create 执行真正创建。
 *
 * <p>草稿 contentJson 字段与 {@code V2PartnerDtos.SupplierWriteRequest}（snake_case）对齐，
 * 确认时可直接反序列化执行。
 */
@Component
public class CreateSupplierTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreateSupplierTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_supplier";
    }

    @Override
    public String displayName() {
        return "创建供应商";
    }

    @Override
    public String description() {
        return "根据用户描述生成供应商草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String name = paramString(params, "name");
        if (name == null) {
            String err = "供应商名称不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
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

        Map<String, Object> draftContent = mapOf(
            "name", name,
            "phone", phone == null ? "" : phone,
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

        String title = "新建供应商：" + name;
        long now = System.currentTimeMillis();
        AgentDraftEntity entity = new AgentDraftEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(ctx.conversationId());
        entity.setDraftType("create_supplier");
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
                "draft_type", "create_supplier",
                "title", title,
                "fields", List.of(
                    mapOf("label", "供应商名称", "value", name),
                    mapOf("label", "手机号", "value", phone == null ? "-" : phone),
                    mapOf("label", "供应商分组", "value", groupId == null ? "-" : String.valueOf(groupId)),
                    mapOf("label", "备注", "value", remark == null ? "-" : remark)
                )
            ))
        );

        String answer = "已生成" + title + "草稿（编号 " + draftId + "），请确认后执行创建。";
        String toolSummary = "生成草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "create_supplier",
            "title", title,
            "status", "active",
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, List.of(block), toolFacts, toolSummary);
    }
}
