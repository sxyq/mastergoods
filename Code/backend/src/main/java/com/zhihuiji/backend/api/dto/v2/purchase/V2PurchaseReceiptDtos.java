package com.zhihuiji.backend.api.dto.v2.purchase;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class V2PurchaseReceiptDtos {
    private V2PurchaseReceiptDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PurchaseReceiptResponse(
        Long id,
        String receiptNo,
        Long purchaseOrderId,
        Long supplierId,
        String supplierName,
        List<PurchaseReceiptItemResponse> items,
        Double totalAmount,
        Integer status,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PurchaseReceiptItemResponse(
        Long id,
        Long receiptId,
        Long productId,
        String productCode,
        String productName,
        Double quantity,
        Double unitCost,
        Double amount,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRequest(
        Long purchaseOrderId,
        Long supplierId,
        String supplierName,
        List<CreateItemRequest> items,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateItemRequest(
        Long productId,
        String productCode,
        String productName,
        Double quantity,
        Double unitCost
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateDraftRequest(
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StatusRequest(Integer status) {}
}
