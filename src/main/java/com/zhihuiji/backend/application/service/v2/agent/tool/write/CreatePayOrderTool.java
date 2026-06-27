package com.zhihuiji.backend.application.service.v2.agent.tool.write;

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
 * 付款单草稿生成工具（CREATE_ONLY）。
 *
 * <p>不直接创建付款单，而是将用户意图序列化为草稿 JSON 保存到 agent_drafts 表（status=active），
 * 等待用户确认后再调用 V2PayOrderService.create 完成实际创建。属于三阶段安全确认的第一阶段。
 */
@Component
public class CreatePayOrderTool extends ToolSupport {

    private final AgentDraftRepository agentDraftRepository;

    public CreatePayOrderTool(AgentDraftRepository agentDraftRepository) {
        this.agentDraftRepository = agentDraftRepository;
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
        return "根据用户描述生成付款单草稿，确认后创建";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
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
        if (amount == null) {
            emitToolFailed(ctx, name(), "缺少必填参数 amount");
            return ToolResult.failure(name(), "缺少必填参数 amount");
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
        String answer = "已生成付款单草稿（ID: " + saved.getId() + "），请确认后执行。";
        String toolSummary = "生成付款单草稿 ID " + saved.getId() + "，供应商 " + supplierName + "，金额 " + money(amount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", saved.getId(),
            "draft_type", saved.getDraftType(),
            "title", saved.getTitle(),
            "status", saved.getStatus(),
            "supplier_name", supplierName,
            "amount", amount,
            "account_id", accountId,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, List.of(block), toolFacts, toolSummary);
    }
}
