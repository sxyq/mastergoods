package com.zhihuiji.backend.api.dto.v2.pay;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public final class V2PayOrderDtos {
    private V2PayOrderDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PayOrderResponse(
        Long id,
        String orderNo,
        Long supplierId,
        String supplierName,
        Double amount,
        Integer method,
        String referenceNo,
        String notes,
        Integer status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRequest(
        Long supplierId,
        String supplierName,
        Double amount,
        Integer method,
        String referenceNo,
        String notes,
        Integer status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StatusRequest(Integer status) {}
}
