package com.zhihuiji.backend.api.dto;

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
