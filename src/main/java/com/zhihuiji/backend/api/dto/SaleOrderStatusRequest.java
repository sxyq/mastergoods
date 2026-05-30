package com.zhihuiji.backend.api.dto;

import jakarta.validation.constraints.NotNull;

public record SaleOrderStatusRequest(
    @NotNull
    Integer status
) {}
