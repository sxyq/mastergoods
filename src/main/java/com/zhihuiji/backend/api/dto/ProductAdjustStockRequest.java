package com.zhihuiji.backend.api.dto;

import jakarta.validation.constraints.NotNull;

public record ProductAdjustStockRequest(
    @NotNull
    Double delta,
    String reason,
    String operator
) {}
