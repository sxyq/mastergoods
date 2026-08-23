package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.ImportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.ImportJobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 导入任务查询工具。
 *
 * <p>查询当前账号下的数据导入任务状态，支持按状态过滤。返回任务来源、阶段、重试次数、
 * 失败原因等信息，帮助用户跟踪导入进度与排查问题。
 */
@Component
public class ImportJobLookupTool extends ToolSupport {

    private final ImportJobRepository importJobRepository;

    public ImportJobLookupTool(ImportJobRepository importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    @Override
    public String name() {
        return "import_job_lookup";
    }

    @Override
    public String displayName() {
        return "导入任务查询";
    }

    @Override
    public String description() {
        return "查询数据导入任务状态，包含任务来源、阶段、重试次数与失败原因";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "status", "导入任务状态，可选");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String status = paramString(params, "status");
        Map<String, Object> input = mapOf("status", status == null ? "" : status);
        ToolAudit audit = startAudit(ctx, name(), input);

        List<ImportJobEntity> jobs = StringUtils.hasText(status)
            ? importJobRepository.findAllByOwnerUserIdAndStatusOrderByUpdatedAtDesc(ownerUserId, status, PageRequest.of(0, DEFAULT_TOOL_LIMIT))
            : importJobRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        List<ImportJobEntity> limited = jobs;
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 个导入任务", audit);

        long runningCount = 0L;
        long failedCount = 0L;
        for (ImportJobEntity job : limited) {
            if ("running".equalsIgnoreCase(job.getStatus())) {
                runningCount += 1;
            }
            if ("failed".equalsIgnoreCase(job.getStatus())) {
                failedCount += 1;
            }
        }
        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "导入任务概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "任务总数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "运行中", "value", String.valueOf(runningCount), "trend_direction", runningCount > 0 ? "up" : "flat"),
                    mapOf("label", "失败数", "value", String.valueOf(failedCount), "trend_direction", failedCount > 0 ? "down" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "导入任务列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("ID", "来源", "状态", "阶段", "重试", "失败原因"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "导入任务 " + limited.size() + " 个，运行中 " + runningCount + "，失败 " + failedCount;
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "job_count", limited.size(),
            "running_count", runningCount,
            "failed_count", failedCount,
            "query_audit", audit.facts(),
            "recent_jobs", buildSummaries(limited)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<ImportJobEntity> jobs) {
        List<List<Object>> rows = new ArrayList<>(jobs == null ? 0 : jobs.size());
        if (jobs == null) {
            return rows;
        }
        for (ImportJobEntity item : jobs) {
            rows.add(List.of(
                String.valueOf(item.getId()),
                safeText(item.getSourceType(), "-"),
                safeText(item.getStatus(), "-"),
                safeText(item.getStage(), "-"),
                item.getRetryCount() == null ? "0" : String.valueOf(item.getRetryCount()),
                safeText(item.getFailureMessage(), "-")
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<ImportJobEntity> jobs) {
        List<Map<String, Object>> items = new ArrayList<>(jobs == null ? 0 : jobs.size());
        if (jobs == null) {
            return items;
        }
        for (ImportJobEntity item : jobs) {
            items.add(mapOf(
                "id", item.getId(),
                "source_type", item.getSourceType(),
                "status", item.getStatus(),
                "stage", item.getStage(),
                "retry_count", item.getRetryCount(),
                "created_at", item.getCreatedAt(),
                "started_at", item.getStartedAt(),
                "finished_at", item.getFinishedAt(),
                "failure_message", item.getFailureMessage()
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
