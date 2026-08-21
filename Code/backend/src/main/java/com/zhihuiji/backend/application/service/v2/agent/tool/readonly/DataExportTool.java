package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 数据导出辅助工具。
 *
 * <p>返回可导出的数据范围说明、数据量预估、列字段清单与导出指令，
 * 不实际生成文件，由前端按指令调用现有导出端点。
 */
@Component
public class DataExportTool extends ToolSupport {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final SaleOrderRepository saleOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final ObjectMapper objectMapper;

    public DataExportTool(SaleOrderRepository saleOrderRepository,
                          PurchaseOrderRepository purchaseOrderRepository,
                          ProductRepository productRepository,
                          CustomerRepository customerRepository,
                          SupplierRepository supplierRepository,
                          FinanceRecordRepository financeRecordRepository,
                          ObjectMapper objectMapper) {
        this.saleOrderRepository = saleOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.financeRecordRepository = financeRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "data_export_tool";
    }

    @Override
    public String displayName() {
        return "数据导出辅助";
    }

    @Override
    public String description() {
        return "返回可导出数据范围、数据量预估、字段清单与导出指令";
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
        ObjectNode dataType = properties.putObject("data_type");
        dataType.put("type", "string");
        dataType.put("description", "导出数据类型：sales/purchase/inventory/customer/supplier/finance（必填）");
        ObjectNode format = properties.putObject("format");
        format.put("type", "string");
        format.put("description", "导出格式：csv/json，默认 csv");
        ObjectNode days = properties.putObject("days");
        days.put("type", "integer");
        days.put("description", "导出时间窗口天数，默认 30");
        schema.putArray("required")
            .add("data_type");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String dataType = paramString(params, "data_type");
        String format = paramString(params, "format");
        int days = Math.max(1, paramInt(params, "days", 30));
        String fmt = format == null ? "csv" : format.toLowerCase();
        if (dataType == null) {
            String err = "缺少必填参数 data_type";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String type = dataType.toLowerCase();
        Map<String, Object> input = mapOf(
            "data_type", type,
            "format", fmt,
            "days", days
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        long now = System.currentTimeMillis();
        long startAt = now - (long) days * DAY_MILLIS;
        long estimatedCount = estimateCount(ownerUserId, type, startAt, now);
        List<String> columns = columnsFor(type);
        String instruction = "请前往「" + labelFor(type) + "」模块，选择时间范围近"
            + days + "天，格式 " + fmt + "，点击导出下载。";
        audit.markReturned(1);
        emitToolCompleted(ctx, name(), "预估导出 " + estimatedCount + " 条 " + type + " 数据", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "导出概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "数据类型", "value", labelFor(type), "trend_direction", "flat"),
                    mapOf("label", "导出格式", "value", fmt.toUpperCase(), "trend_direction", "flat"),
                    mapOf("label", "预估条数", "value", String.valueOf(estimatedCount), "trend_direction", estimatedCount > 0 ? "up" : "flat"),
                    mapOf("label", "时间范围", "value", "近" + days + "天", "trend_direction", "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto columnBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "导出字段清单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("字段"),
                "rows", buildColumnRows(columns),
                "row_count", columns.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, columnBlock);
        int columnCount = columns.size();
        String toolSummary = "导出 " + type + " 预估 " + estimatedCount + " 条，格式 " + fmt
            + "，字段 " + columnCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "data_type", type,
            "format", fmt,
            "days", days,
            "estimated_count", estimatedCount,
            "column_count", columnCount,
            "columns", columns,
            "instruction", instruction,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildColumnRows(List<String> columns) {
        List<List<Object>> rows = new ArrayList<>(columns.size());
        for (String column : columns) {
            rows.add(List.of(column));
        }
        return rows;
    }

    private long estimateCount(Long ownerUserId, String type, long startAt, long endAt) {
        return switch (type) {
            case "sales" -> safeLong(saleOrderRepository.countNonCancelledBetween(ownerUserId, startAt, endAt));
            case "purchase" -> purchaseOrderRepository.findByOwnerUserIdAndCreatedAtBetween(ownerUserId, startAt, endAt).size();
            case "inventory" -> productRepository.countByOwnerUserId(ownerUserId);
            case "customer" -> customerRepository.countByOwnerUserId(ownerUserId);
            case "supplier" -> supplierRepository.countByOwnerUserId(ownerUserId);
            case "finance" -> financeRecordRepository.search(ownerUserId, null, null, startAt, endAt).size();
            default -> 0L;
        };
    }

    private List<String> columnsFor(String type) {
        return switch (type) {
            case "sales" -> List.of("单号", "客户", "总额", "已收", "状态", "创建时间");
            case "purchase" -> List.of("单号", "供应商", "总额", "已付", "状态", "创建时间");
            case "inventory" -> List.of("商品编码", "商品名称", "分类", "库存", "安全库存", "销售价");
            case "customer" -> List.of("客户名称", "电话", "余额", "状态");
            case "supplier" -> List.of("供应商名称", "电话", "余额", "状态");
            case "finance" -> List.of("单号", "类型", "金额", "分类", "创建时间");
            default -> List.of();
        };
    }

    private String labelFor(String type) {
        return switch (type) {
            case "sales" -> "销售单";
            case "purchase" -> "采购单";
            case "inventory" -> "库存";
            case "customer" -> "客户";
            case "supplier" -> "供应商";
            case "finance" -> "收支记录";
            default -> "数据";
        };
    }
}
