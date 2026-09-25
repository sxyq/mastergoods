// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.api.dto.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class OperationDraftDtos {
    private OperationDraftDtos() {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record OperationDraftDto(
        String operationType,
        String summary,
        String partnerRole,
        Long partnerId,
        String partnerName,
        List<OperationDraftItemDto> items,
        String notes,
        boolean canSubmit,
        List<String> warnings,
        List<String> suggestedActions
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record OperationDraftItemDto(
        Long productId,
        String productCode,
        String productName,
        double quantity,
        double unitPrice,
        double amount,
        double currentStock
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record OperationSubmitResultDto(
        String operationType,
        Long orderId,
        String orderNo,
        String message,
        String nextAction
    ) {}
}
