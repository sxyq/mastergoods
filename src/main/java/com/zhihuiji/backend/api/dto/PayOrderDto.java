package com.zhihuiji.backend.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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
