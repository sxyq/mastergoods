package com.zhihuiji.backend.api.dto;

import jakarta.validation.constraints.NotNull;

public record ProductAdjustStockRequest(
    @NotNull
    Double delta,
    String reason,
    String operator
) {
    public ProductAdjustStockRequest {
        if (delta != null && delta == 0.0) {
            throw new IllegalArgumentException("delta must not be zero");
        }
    }
}
