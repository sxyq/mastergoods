package com.zhihuiji.backend.api.dto;

public record PurchaseOrderItemDto(
    Long id,
    Long orderId,
    String productCode,
    String productName,
    Double quantity,
    Double unitCost,
    Double amount,
    Long createdAt
) {}
