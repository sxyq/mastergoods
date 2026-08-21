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
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 创建资金流水草稿工具。
 *
 * <p>CREATE_ONLY 类型，不直接执行写入，而是构建草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后由 AgentDraftConfirmService 调用 FinanceRecordService.create 执行真正创建。
 *
 * <p>草稿 contentJson 字段与 {@code FinanceRecordService.CreateCommand}（camelCase，无 @JsonNaming）对齐，
 * 确认时可直接反序列化执行。工具入参 {@code type}（income/expense）会被转换为整型
 * （1=收入，2=支出，对应 FinanceRecordService.TYPE_INCOME/TYPE_EXPENSE）。
 * 注意：FinanceRecordService.CreateCommand 不支持账户字段，入参 account_id 仅用于草稿展示，不进入 contentJson。
 */
@Component
public class CreateFinanceRecordTool extends ToolSupport {

    static final int TYPE_INCOME = 1;
    static final int TYPE_EXPENSE = 2;

    private final AgentDraftRepository agentDraftRepository;

    public CreateFinanceRecordTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
    }

    @Override
    public String name() {
        return "create_finance_record";
    }

    @Override
    public String displayName() {
        return "创建资金流水";
    }

    @Override
    public String description() {
        return "根据用户描述生成收入或支出资金流水草稿，确认后创建；type 和 amount 是必填，category、remark 可直接使用用户原话，account_id 仅用于展示且可选，不需要先查询账户";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringEnumProperty(schema, "type", "资金流水类型", "income", "expense");
        addNumberProperty(schema, "amount", "金额，必须大于 0", 0.01D);
        addStringProperty(schema, "category", "资金流水分类");
        addIntegerProperty(schema, "account_id", "账户 ID");
        addStringProperty(schema, "remark", "资金流水备注");
        addRequired(schema, "type", "amount");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String typeRaw = paramString(params, "type");
        Integer type = parseType(typeRaw);
        if (type == null) {
            String err = "资金流水类型不能为空，需为 income 或 expense";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        Double amount = paramDouble(params, "amount", null);
        if (amount == null || amount <= 0D) {
            String err = "资金流水金额必须大于 0";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String category = paramString(params, "category");
        Long accountId = paramLong(params, "account_id", null);
        String remark = paramString(params, "remark");

        Map<String, Object> input = mapOf(
            "type", typeRaw,
            "amount", amount,
            "category", category,
            "account_id", accountId,
            "remark", remark
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        // contentJson 与 FinanceRecordService.CreateCommand（camelCase）对齐
        Map<String, Object> draftContent = mapOf(
            "type", type,
            "category", category == null ? "" : category,
            "amount", amount,
            "notes", remark,
            "partnerName", null,
            "method", null
        );
        String contentJson;
        try {
            contentJson = ctx.objectMapper().writeValueAsString(draftContent);
        } catch (JsonProcessingException ex) {
            String err = "草稿序列化失败";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }

        String title = "新建资金流水";
        long now = System.currentTimeMillis();
        AgentDraftEntity entity = new AgentDraftEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(ctx.conversationId());
        entity.setDraftType("create_finance_record");
        entity.setTitle(title);
        entity.setContentJson(contentJson);
        entity.setStatus("active");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(entity);
        Long draftId = saved.getId();

        emitToolCompleted(ctx, name(), "生成草稿 #" + draftId, audit);

        String typeLabel = type == TYPE_INCOME ? "收入" : "支出";
        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft_card",
            "草稿待确认",
            toJsonNode(ctx, mapOf(
                "draft_id", draftId,
                "draft_type", "create_finance_record",
                "title", title,
                "fields", List.of(
                    mapOf("label", "类型", "value", typeLabel),
                    mapOf("label", "金额", "value", money(amount)),
                    mapOf("label", "分类", "value", category == null ? "-" : category),
                    mapOf("label", "账户ID", "value", accountId == null ? "-" : String.valueOf(accountId)),
                    mapOf("label", "备注", "value", remark == null ? "-" : remark)
                )
            ))
        );

        String toolSummary = "生成草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "create_finance_record",
            "title", title,
            "status", "active",
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }

    private Integer parseType(String typeRaw) {
        if (typeRaw == null || typeRaw.isBlank()) {
            return null;
        }
        String lower = typeRaw.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "income", "收入", "1" -> TYPE_INCOME;
            case "expense", "支出", "2" -> TYPE_EXPENSE;
            default -> null;
        };
    }
}
