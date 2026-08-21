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
 * 创建账户转账草稿工具。
 *
 * <p>CREATE_ONLY 类型，不直接执行写入，而是构建草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后由 AgentDraftConfirmService 调用 V2AccountTransferService.create 执行真正创建。
 *
 * <p>草稿 contentJson 字段与 {@code V2FinanceDtos.AccountTransferCreateRequest}（snake_case）对齐，
 * 确认时可直接反序列化执行。
 */
@Component
public class CreateAccountTransferTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreateAccountTransferTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_account_transfer";
    }

    @Override
    public String displayName() {
        return "创建账户转账";
    }

    @Override
    public String description() {
        return "根据用户描述生成账户转账草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addIntegerProperty(schema, "from_account_id", "转出账户 ID");
        addIntegerProperty(schema, "to_account_id", "转入账户 ID");
        addNumberProperty(schema, "amount", "转账金额，必须大于 0", 0.01D);
        addStringProperty(schema, "remark", "转账备注");
        addRequired(schema, "from_account_id", "to_account_id", "amount");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long fromAccountId = paramLong(params, "from_account_id", null);
        if (fromAccountId == null) {
            String err = "转出账户ID不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        Long toAccountId = paramLong(params, "to_account_id", null);
        if (toAccountId == null) {
            String err = "转入账户ID不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        if (fromAccountId.equals(toAccountId)) {
            String err = "转出和转入账户不能相同";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        Double amount = paramDouble(params, "amount", null);
        if (amount == null || amount <= 0D) {
            String err = "转账金额必须大于 0";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String remark = paramString(params, "remark");

        Map<String, Object> input = mapOf(
            "from_account_id", fromAccountId,
            "to_account_id", toAccountId,
            "amount", amount,
            "remark", remark
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        Map<String, Object> draftContent = mapOf(
            "from_account_id", fromAccountId,
            "to_account_id", toAccountId,
            "amount", amount,
            "notes", remark
        );
        String contentJson;
        try {
            contentJson = ctx.objectMapper().writeValueAsString(draftContent);
        } catch (JsonProcessingException ex) {
            String err = "草稿序列化失败";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }

        String title = "新建账户转账";
        long now = System.currentTimeMillis();
        AgentDraftEntity entity = new AgentDraftEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(ctx.conversationId());
        entity.setDraftType("create_account_transfer");
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
                "draft_type", "create_account_transfer",
                "title", title,
                "fields", List.of(
                    mapOf("label", "转出账户ID", "value", String.valueOf(fromAccountId)),
                    mapOf("label", "转入账户ID", "value", String.valueOf(toAccountId)),
                    mapOf("label", "转账金额", "value", money(amount)),
                    mapOf("label", "备注", "value", remark == null ? "-" : remark)
                )
            ))
        );

        String toolSummary = "生成草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "create_account_transfer",
            "title", title,
            "status", "active",
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }
}
