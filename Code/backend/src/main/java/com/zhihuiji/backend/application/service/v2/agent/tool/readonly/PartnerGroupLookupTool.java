package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerGroupRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 合作伙伴分组查询工具，查询当前账号客户/供应商分组、分组列表与成员统计。
 */
@Component
public class PartnerGroupLookupTool extends ToolSupport {

    private static final String PARTNER_TYPE_CUSTOMER = "customer";
    private static final String PARTNER_TYPE_SUPPLIER = "supplier";

    private final PartnerGroupRepository partnerGroupRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;

    public PartnerGroupLookupTool(PartnerGroupRepository partnerGroupRepository,
                                  CustomerRepository customerRepository,
                                  SupplierRepository supplierRepository) {
        this.partnerGroupRepository = partnerGroupRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public String name() {
        return "partner_group_lookup";
    }

    @Override
    public String displayName() {
        return "合作伙伴分组查询";
    }

    @Override
    public String description() {
        return "查询当前账号客户/供应商分组、分组列表与成员统计";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "partner_type", "伙伴类型，可选");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String partnerType = paramString(params, "partner_type");
        Map<String, Object> input = mapOf(
            "partner_type", partnerType == null ? "" : partnerType,
            "limit", DEFAULT_TOOL_LIMIT
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<PartnerGroupEntity> groups = loadGroups(ownerUserId, partnerType);
        List<PartnerGroupEntity> recent = limit(groups, DEFAULT_TOOL_LIMIT);
        audit.markLimitedResult(recent.size(), DEFAULT_TOOL_LIMIT);

        long totalMemberCount = 0L;
        for (PartnerGroupEntity group : recent) {
            totalMemberCount += countMembers(ownerUserId, group.getPartnerType(), group.getId());
        }
        emitToolCompleted(ctx, name(), "命中 " + recent.size() + " 个分组", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "分组概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "分组数", "value", String.valueOf(recent.size()), "trend_direction", recent.isEmpty() ? "flat" : "up"),
                    mapOf("label", "关联成员数", "value", String.valueOf(totalMemberCount), "trend_direction", totalMemberCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "合作伙伴分组",
            toJsonNode(ctx, mapOf(
                "headers", List.of("名称", "类型", "成员数"),
                "rows", buildGroupRows(ownerUserId, recent),
                "row_count", recent.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "分组 " + recent.size() + " 个，关联成员 " + totalMemberCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "group_count", recent.size(),
            "total_member_count", totalMemberCount,
            "query_audit", audit.facts(),
            "groups", buildGroupSummaries(ownerUserId, recent)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<PartnerGroupEntity> loadGroups(Long ownerUserId, String partnerType) {
        if (StringUtils.hasText(partnerType)) {
            return partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(ownerUserId, partnerType);
        }
        List<PartnerGroupEntity> all = new ArrayList<>();
        all.addAll(partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(ownerUserId, PARTNER_TYPE_CUSTOMER));
        all.addAll(partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(ownerUserId, PARTNER_TYPE_SUPPLIER));
        return all;
    }

    private long countMembers(Long ownerUserId, String partnerType, Long groupId) {
        if (groupId == null) {
            return 0L;
        }
        if (PARTNER_TYPE_SUPPLIER.equalsIgnoreCase(partnerType)) {
            return safeLong(supplierRepository.countByOwnerUserIdAndGroupId(ownerUserId, groupId));
        }
        return safeLong(customerRepository.countByOwnerUserIdAndGroupId(ownerUserId, groupId));
    }

    private String partnerTypeLabel(String partnerType) {
        if (PARTNER_TYPE_SUPPLIER.equalsIgnoreCase(partnerType)) {
            return "供应商";
        }
        if (PARTNER_TYPE_CUSTOMER.equalsIgnoreCase(partnerType)) {
            return "客户";
        }
        return safeText(partnerType, "-");
    }

    private List<List<Object>> buildGroupRows(Long ownerUserId, List<PartnerGroupEntity> groups) {
        List<List<Object>> rows = new ArrayList<>(groups == null ? 0 : groups.size());
        if (groups == null) {
            return rows;
        }
        for (PartnerGroupEntity group : groups) {
            rows.add(List.of(
                safeText(group.getName(), "-"),
                partnerTypeLabel(group.getPartnerType()),
                String.valueOf(countMembers(ownerUserId, group.getPartnerType(), group.getId()))
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildGroupSummaries(Long ownerUserId, List<PartnerGroupEntity> groups) {
        List<Map<String, Object>> items = new ArrayList<>(groups == null ? 0 : groups.size());
        if (groups == null) {
            return items;
        }
        for (PartnerGroupEntity group : groups) {
            items.add(mapOf(
                "id", safeLong(group.getId()),
                "name", safeText(group.getName(), "-"),
                "partner_type", partnerTypeLabel(group.getPartnerType()),
                "member_count", countMembers(ownerUserId, group.getPartnerType(), group.getId())
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
