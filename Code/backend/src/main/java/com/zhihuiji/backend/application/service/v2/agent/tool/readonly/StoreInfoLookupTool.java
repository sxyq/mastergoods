package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 门店信息查询工具。
 *
 * <p>查询当前 owner 门店基础信息与成员数量。
 */
@Component
public class StoreInfoLookupTool extends ToolSupport {

    private final StoreRepository storeRepository;
    private final StoreMembershipRepository storeMembershipRepository;
    private final ObjectMapper objectMapper;

    public StoreInfoLookupTool(StoreRepository storeRepository,
                               StoreMembershipRepository storeMembershipRepository,
                               ObjectMapper objectMapper) {
        this.storeRepository = storeRepository;
        this.storeMembershipRepository = storeMembershipRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "store_info_lookup";
    }

    @Override
    public String displayName() {
        return "门店信息查询";
    }

    @Override
    public String description() {
        return "查询当前账号门店基础信息与成员数量";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode storeId = properties.putObject("store_id");
        storeId.put("type", "integer");
        storeId.put("description", "门店 ID（可选，不传则查当前 owner 门店）");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long storeId = paramLong(params, "store_id", null);
        Map<String, Object> input = mapOf("store_id", storeId);
        ToolAudit audit = startAudit(ctx, name(), input);

        Optional<StoreEntity> storeOpt = storeId != null
            ? storeRepository.findById(storeId).filter(s -> ownerUserId.equals(s.getOwnerUserId()))
            : storeRepository.findByOwnerUserId(ownerUserId);
        if (storeOpt.isEmpty()) {
            audit.markReturned(0);
            emitToolCompleted(ctx, name(), "未找到门店", audit);
            return ToolResult.empty("未找到门店信息");
        }
        StoreEntity store = storeOpt.get();
        long memberCount = storeMembershipRepository.countByOwnerUserId(ownerUserId);
        audit.markReturned(1);
        emitToolCompleted(ctx, name(), "门店 " + safeText(store.getStoreName(), "-") + " 成员 " + memberCount + " 人", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "门店概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "门店名称", "value", safeText(store.getStoreName(), "-"), "trend_direction", "flat"),
                    mapOf("label", "门店状态", "value", statusLabel(store.getStatus()), "trend_direction", "flat"),
                    mapOf("label", "成员数量", "value", String.valueOf(memberCount), "trend_direction", memberCount == 0 ? "flat" : "up")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "门店信息",
            toJsonNode(ctx, mapOf(
                "headers", List.of("门店名称", "状态", "成员数"),
                "rows", List.of(List.of(
                    safeText(store.getStoreName(), "-"),
                    statusLabel(store.getStatus()),
                    String.valueOf(memberCount)
                )),
                "row_count", 1
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "门店 " + safeText(store.getStoreName(), "-") + "，成员 " + memberCount + " 人";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "store_id", safeLong(store.getId()),
            "store_name", safeText(store.getStoreName(), ""),
            "status", statusLabel(store.getStatus()),
            "member_count", memberCount,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private String statusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "禁用";
            case 1 -> "正常";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
