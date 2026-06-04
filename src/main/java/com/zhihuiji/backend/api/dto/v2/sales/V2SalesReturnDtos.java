package com.zhihuiji.backend.api.dto.v2.sales;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public final class V2SalesReturnDtos {
    private V2SalesReturnDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SalesReturnResponse(
        Long id,
        String returnNo,
        Long originalOrderId,
        Long customerId,
        String customerName,
        List<SalesReturnItemResponse> items,
        Double totalAmount,
        Double refundAmount,
        Integer status,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SalesReturnItemResponse(
        Long id,
        Long returnId,
        Long productId,
        String productCode,
        String productName,
        Double quantity,
        Double unitPrice,
        Double amount,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRequest(
        Long originalOrderId,
        Long customerId,
        String customerName,
        @NotEmpty List<@Valid CreateItemRequest> items,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateItemRequest(
        Long productId,
        @NotNull @Positive Double quantity,
        @PositiveOrZero Double unitPrice
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
        Double amount,
        Integer method,
        String referenceNo
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StatusRequest(Integer status) {}
}
