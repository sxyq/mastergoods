package com.zhihuiji.backend.api.dto;

public record SaleOrderItemDto(
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
