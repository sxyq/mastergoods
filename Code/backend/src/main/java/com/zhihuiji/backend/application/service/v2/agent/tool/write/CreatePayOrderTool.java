package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentEntityReferenceValidator;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 付款单草稿生成工具（CREATE_ONLY）。
 *
 * <p>不直接创建付款单，而是将用户意图序列化为草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后再调用 V2PayOrderService.create 完成实际创建。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreatePayOrderTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;
    private final AgentEntityReferenceValidator referenceValidator;

    @Autowired
    public CreatePayOrderTool(
        AgentDraftRepository agentDraftRepository,
        AgentEntityReferenceValidator referenceValidator
    ) {
        this.agentDraftRepository = agentDraftRepository;
        this.referenceValidator = referenceValidator;
    }

    /** 保留给不使用 Spring 注入的旧测试和兼容调用方。 */
    public CreatePayOrderTool(AgentDraftRepository agentDraftRepository) {
        this(agentDraftRepository, null);
    }

    @Override
    public String name() {
        return "create_pay_order";
    }

    @Override
    public String displayName() {
        return "创建付款单";
    }

    @Override
    public String description() {
        return "生成供应商付款单草稿，不会实际付款。用户要给供应商付款时，若没有真实供应商 ID，"
            + "先调用 supplier_directory_lookup 查询当前账号的供应商；拿到返回的 supplier_id 和准确 supplier_name 后，"
            + "继续调用本工具生成草稿，不要只查询后结束，也不要编造供应商 ID、名称或金额。"
            + "supplier_id、supplier_name、amount 必填；草稿需等待用户确认后才创建付款单";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "supplier_name", "供应商名称");
        addIntegerProperty(schema, "supplier_id", "供应商 ID");
        schema.with("properties").with("supplier_id").put("minimum", 1);
        schema.with("properties").with("supplier_name").put("minLength", 1);
        addNumberProperty(schema, "amount", "付款金额，必须大于 0", 0.01D);
        addIntegerProperty(schema, "account_id", "付款账户 ID");
        addStringProperty(schema, "remark", "付款备注");
        addRequired(schema, "supplier_id", "supplier_name", "amount");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String supplierName = paramString(params, "supplier_name");
        Long supplierId = paramLong(params, "supplier_id", null);
        Double amount = paramDouble(params, "amount", null);
        Long accountId = paramLong(params, "account_id", null);
        String remark = paramString(params, "remark");
        Map<String, Object> input = mapOf(
            "supplier_name", supplierName == null ? "" : supplierName,
            "supplier_id", supplierId,
            "amount", amount,
            "account_id", accountId,
            "remark", remark == null ? "" : remark
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (supplierName == null) {
            emitToolFailed(ctx, name(), "缺少必填参数 supplier_name");
            return ToolResult.failure(name(), "缺少必填参数 supplier_name");
        }
        if (supplierId == null) {
            emitToolFailed(ctx, name(), "缺少必填参数 supplier_id");
            return ToolResult.failure(name(), "缺少必填参数 supplier_id");
        }
        if (supplierId <= 0L) {
            emitToolFailed(ctx, name(), "supplier_id 必须是正整数");
            return ToolResult.failure(name(), "supplier_id 必须是正整数");
        }
        if (amount == null || !Double.isFinite(amount) || amount <= 0D) {
            emitToolFailed(ctx, name(), "付款金额必须是大于 0 的有限数字");
            return ToolResult.failure(name(), "付款金额必须是大于 0 的有限数字");
        }
        if (referenceValidator != null
            && !referenceValidator.supplierMatches(ownerUserId, supplierId, supplierName)) {
            String error = "供应商不属于当前账号，无法生成付款草稿";
            emitToolFailed(ctx, name(), error);
            return ToolResult.failure(name(), error);
        }

        Map<String, Object> content = mapOf(
            "supplier_id", supplierId,
            "supplier_name", supplierName,
            "amount", amount,
            "method", null,
            "reference_no", null,
            "notes", remark,
            "account_id", accountId,
            "status", null
        );
        String contentJson = ctx.objectMapper().valueToTree(content).toString();

        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ownerUserId);
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType("create_pay_order");
        draft.setTitle("新建付款单：" + supplierName);
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);

        emitToolCompleted(ctx, name(), "已生成付款单草稿 ID " + saved.getId(), audit);

        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft",
            "新建付款单草稿",
            toJsonNode(ctx, mapOf(
                "draft_id", saved.getId(),
                "draft_type", saved.getDraftType(),
                "title", saved.getTitle(),
                "status", saved.getStatus(),
                "supplier_name", supplierName,
                "amount", money(amount)
            ))
        );
        String toolSummary = "生成付款单草稿 ID " + saved.getId() + "，供应商 " + supplierName + "，金额 " + money(amount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", saved.getId(),
            "draft_type", saved.getDraftType(),
            "title", saved.getTitle(),
            "status", saved.getStatus(),
            "supplier_id", supplierId,
            "supplier_name", supplierName,
            "amount", amount,
            "account_id", accountId,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }
}
