package com.zhihuiji.backend.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SupplierDto(
    Long id,
    String name,
    String phone,
    String address,
    String notes,
    Double balance,
    Integer status,
    Long createdAt,
    Long updatedAt
) {}
