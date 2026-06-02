package com.zhihuiji.backend.api.dto.v2.sales;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class V2SaleOrderDtos {
    private V2SaleOrderDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SaleOrderResponse(
        Long id,
        String orderNo,
        Long customerId,
        String customerName,
        List<SaleOrderItemResponse> items,
        Double subtotalAmount,
        Double discountAmount,
        Double totalAmount,
        Double paidAmount,
        String notes,
        Integer status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SaleOrderItemResponse(
        Long id,
        Long orderId,
        Long productId,
        String productCode,
        String productName,
        Long customerId,
        String customerName,
        Double quantity,
        Double unitPrice,
        Double amount,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PaymentResponse(
        Long id,
        Long orderId,
        Double amount,
        Integer method,
        String referenceNo,
        Integer type,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRequest(
        Long customerId,
        String customerName,
        List<CreateItemRequest> items,
        String notes,
        Double discountAmount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateItemRequest(
        Long productId,
        Double quantity,
        Double unitPrice
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateDraftRequest(
        Double discountAmount,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PaymentRequest(
        Double amount,
        Integer method,
        String referenceNo
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StatusRequest(Integer status) {}
}
