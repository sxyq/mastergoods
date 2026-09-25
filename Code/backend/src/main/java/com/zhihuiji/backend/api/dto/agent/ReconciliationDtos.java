// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.api.dto.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class ReconciliationDtos {
    private ReconciliationDtos() {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record ReconciliationFollowupDto(
        double totalReceivable,
        double totalPayable,
        double totalReceived,
        double totalPaid,
        double netCashFlow,
        List<FollowupPartyDto> receivableCustomers,
        List<FollowupPartyDto> payableSuppliers,
        List<AgingRiskDto> agingRisks
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record FollowupPartyDto(
        Long entityId,
        String entityType,
        String name,
        String phone,
        double amount,
        String actionLabel
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgingRiskDto(
        String entityType,
        Long entityId,
        String name,
        String orderNo,
        long createdAt,
        long ageDays,
        double amount,
        String summary,
        String suggestedAction
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record ReportInsightDto(
        String periodLabel,
        double currentSales,
        double previousSales,
        double salesChangeRate,
        String narrative,
        String leadingProductName,
        double leadingProductAmount,
        String leadingCustomerName,
        double leadingCustomerAmount,
        List<String> highlights,
        List<String> suggestedActions
    ) {}
}
