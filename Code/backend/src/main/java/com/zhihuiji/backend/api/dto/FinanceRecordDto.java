package com.zhihuiji.backend.api.dto;

public record FinanceRecordDto(
    Long id,
    String recordNo,
    Integer type,
    String category,
    String partnerName,
    Double amount,
    Integer method,
    String notes,
    Long createdAt,
    Long updatedAt
) {}
