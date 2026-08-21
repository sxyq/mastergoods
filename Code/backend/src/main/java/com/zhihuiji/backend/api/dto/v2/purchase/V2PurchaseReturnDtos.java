package com.zhihuiji.backend.api.dto.v2.purchase;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public final class V2PurchaseReturnDtos {
    private V2PurchaseReturnDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PurchaseReturnResponse(
        Long id,
        String returnNo,
        Long purchaseOrderId,
        Long supplierId,
        String supplierName,
        List<PurchaseReturnItemResponse> items,
        List<PurchaseReturnRefundResponse> refunds,
        Double totalAmount,
        Double refundAmount,
        Integer status,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PurchaseReturnItemResponse(
        Long id,
        Long returnId,
        Long productId,
        String productCode,
        String productName,
        Double quantity,
        Double unitCost,
        Double amount,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PurchaseReturnRefundResponse(
        Long id,
        Long returnId,
        Double amount,
        Integer method,
        String referenceNo,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRequest(
        Long purchaseOrderId,
        Long supplierId,
        String supplierName,
        @NotEmpty List<@Valid CreateItemRequest> items,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateItemRequest(
        Long productId,
        String productCode,
        String productName,
        @NotNull @Positive Double quantity,
        @PositiveOrZero Double unitCost
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateDraftRequest(
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConfirmRequest(
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RefundRequest(
        @NotNull @Positive Double amount,
        @NotNull @Positive Integer method,
        String referenceNo
    ) {}
}
