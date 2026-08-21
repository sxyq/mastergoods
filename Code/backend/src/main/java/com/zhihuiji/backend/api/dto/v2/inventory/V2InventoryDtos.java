package com.zhihuiji.backend.api.dto.v2.inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class V2InventoryDtos {
    private V2InventoryDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LedgerEntryResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        Double quantityBefore,
        Double quantityChange,
        Double quantityAfter,
        Double unitCost,
        String sourceType,
        Long sourceId,
        String sourceNo,
        String notes,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LedgerEntryCreateRequest(
        @NotNull Long productId,
        @NotBlank String sourceType,
        Long sourceId,
        String sourceNo,
        @NotNull Double quantityChange,
        Double unitCost,
        Long warehouseId,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SnapshotResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        Double quantity,
        Double unitCost,
        Double totalValue,
        Long snapshotDate,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SnapshotCreateRequest(
        @NotNull Long productId,
        @NotNull Long snapshotDate,
        Long warehouseId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MonthlyStatsResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        Integer month,
        Integer year,
        Double quantityIn,
        Double quantityOut,
        Double quantityAdjust,
        Double quantityBegin,
        Double quantityEnd,
        Double totalCostIn,
        Double totalCostOut,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LedgerListResponse(
        List<LedgerEntryResponse> items
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SnapshotListResponse(
        List<SnapshotResponse> items
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MonthlyStatsListResponse(
        List<MonthlyStatsResponse> items
    ) {}
}
