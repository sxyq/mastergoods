package com.zhihuiji.backend.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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
