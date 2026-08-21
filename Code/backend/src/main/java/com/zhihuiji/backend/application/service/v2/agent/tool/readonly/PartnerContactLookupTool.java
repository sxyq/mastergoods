package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PartnerContactEntity;
import com.zhihuiji.backend.infrastructure.repository.PartnerContactRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 联系人查询工具，查询当前账号客户/供应商联系人、联系方式与主要联系人。
 */
@Component
public class PartnerContactLookupTool extends ToolSupport {

    private final PartnerContactRepository partnerContactRepository;

    public PartnerContactLookupTool(PartnerContactRepository partnerContactRepository) {
        this.partnerContactRepository = partnerContactRepository;
    }

    @Override
    public String name() {
        return "partner_contact_lookup";
    }

    @Override
    public String displayName() {
        return "联系人查询";
    }

    @Override
    public String description() {
        return "查询当前账号客户/供应商联系人、联系方式与主要联系人";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "partner_type", "伙伴类型，可选");
        addIntegerProperty(schema, "partner_id", "伙伴 ID，可选");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String partnerType = paramString(params, "partner_type");
        Long partnerId = paramLong(params, "partner_id", null);
        Map<String, Object> input = mapOf(
            "partner_type", partnerType == null ? "" : partnerType,
            "partner_id", partnerId,
            "limit", DEFAULT_TOOL_LIMIT
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<PartnerContactEntity> contacts;
        if (partnerId == null || !StringUtils.hasText(partnerType)) {
            contacts = List.of();
        } else {
            contacts = partnerContactRepository
                .findAllByOwnerUserIdAndPartnerTypeAndPartnerIdOrderByIsPrimaryDescCreatedAtAsc(
                    ownerUserId, partnerType, partnerId);
        }
        List<PartnerContactEntity> recent = limit(contacts, DEFAULT_TOOL_LIMIT);
        audit.markLimitedResult(recent.size(), DEFAULT_TOOL_LIMIT);

        long primaryCount = 0L;
        for (PartnerContactEntity contact : recent) {
            if (Boolean.TRUE.equals(contact.getIsPrimary())) {
                primaryCount += 1L;
            }
        }
        emitToolCompleted(ctx, name(), "命中 " + recent.size() + " 个联系人", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "联系人概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "联系人数", "value", String.valueOf(recent.size()), "trend_direction", recent.isEmpty() ? "flat" : "up"),
                    mapOf("label", "主要联系人", "value", String.valueOf(primaryCount), "trend_direction", primaryCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "联系人列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("姓名", "电话", "是否主要联系人"),
                "rows", buildContactRows(recent),
                "row_count", recent.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "联系人 " + recent.size() + " 个，主要联系人 " + primaryCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "contact_count", recent.size(),
            "primary_count", primaryCount,
            "query_audit", audit.facts(),
            "contacts", buildContactSummaries(recent)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildContactRows(List<PartnerContactEntity> contacts) {
        List<List<Object>> rows = new ArrayList<>(contacts == null ? 0 : contacts.size());
        if (contacts == null) {
            return rows;
        }
        for (PartnerContactEntity contact : contacts) {
            rows.add(List.of(
                safeText(contact.getName(), "-"),
                safeText(contact.getPhone(), "-"),
                Boolean.TRUE.equals(contact.getIsPrimary()) ? "是" : "否"
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildContactSummaries(List<PartnerContactEntity> contacts) {
        List<Map<String, Object>> items = new ArrayList<>(contacts == null ? 0 : contacts.size());
        if (contacts == null) {
            return items;
        }
        for (PartnerContactEntity contact : contacts) {
            items.add(mapOf(
                "id", safeLong(contact.getId()),
                "name", safeText(contact.getName(), "-"),
                "phone", safeText(contact.getPhone(), "-"),
                "title", safeText(contact.getTitle(), "-"),
                "is_primary", Boolean.TRUE.equals(contact.getIsPrimary())
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
