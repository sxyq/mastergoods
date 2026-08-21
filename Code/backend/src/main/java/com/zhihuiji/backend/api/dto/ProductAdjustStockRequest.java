package com.zhihuiji.backend.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductAdjustStockRequest(
    @NotNull
    BigDecimal delta,
    String reason,
    String operator
) {
    public ProductAdjustStockRequest {
        if (delta != null && delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("delta must not be zero");
        }
    }
}
