package com.zhihuiji.backend.api.dto;

import java.util.List;

public record SaleOrderDto(
    Long id,
    String orderNo,
    Long customerId,
    String customerName,
    List<SaleOrderItemDto> items,
    Double subtotalAmount,
    Double discountAmount,
    Double totalAmount,
    Double paidAmount,
    String notes,
    Integer status,
    Long createdAt,
    Long updatedAt
) {}
