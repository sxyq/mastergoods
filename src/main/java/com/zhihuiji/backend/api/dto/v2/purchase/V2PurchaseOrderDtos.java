package com.zhihuiji.backend.api.dto.v2.purchase;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class V2PurchaseOrderDtos {
    private V2PurchaseOrderDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PurchaseOrderResponse(
        Long id,
        String orderNo,
        Long supplierId,
        String supplierName,
        List<PurchaseOrderItemResponse> items,
        Double totalAmount,
        Double paidAmount,
        Double receivedAmount,
        String notes,
        Integer status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PurchaseOrderItemResponse(
        Long id,
        Long orderId,
        String productCode,
        String productName,
        Double quantity,
        Double unitCost,
        Double amount,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRequest(
        Long supplierId,
        String supplierName,
        List<CreateItemRequest> items,
        String notes,
        Integer status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateItemRequest(
        Long productId,
        String productCode,
        String productName,
        Double quantity,
        Double unitCost
    ) {}
}
