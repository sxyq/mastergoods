package com.zhihuiji.backend.api.dto;

public record PayOrderDto(
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
