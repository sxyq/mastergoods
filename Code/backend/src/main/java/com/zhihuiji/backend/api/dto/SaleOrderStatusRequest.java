package com.zhihuiji.backend.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleOrderStatusRequest(
    @NotNull @Min(0) @Max(3)
    Integer status
) {}
