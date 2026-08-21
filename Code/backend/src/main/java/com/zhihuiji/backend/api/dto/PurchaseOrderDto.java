package com.zhihuiji.backend.api.dto;

import java.util.List;

public record PurchaseOrderDto(
    Long id,
    String orderNo,
    Long supplierId,
    String supplierName,
    List<PurchaseOrderItemDto> items,
    Double totalAmount,
    String notes,
    Integer status,
    Long createdAt,
    Long updatedAt
) {}
