package com.zhihuiji.backend.api.dto.v2.product;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class V2ProductDtos {
    private V2ProductDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductResponse(
        Long id,
        String code,
        String name,
        Long categoryId,
        String categoryName,
        Long unitId,
        String unitName,
        Double salePrice,
        Double purchasePrice,
        List<ProductPriceValueResponse> priceLevels,
        ProductSupplierRelationResponse defaultSupplier,
        List<ProductSupplierRelationResponse> supplierRelations,
        Double stock,
        Double safeStock,
        Integer status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductWriteRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        @NotNull Long categoryId,
        @NotNull Long unitId,
        @NotNull Double salePrice,
        @NotNull Double purchasePrice,
        List<ProductPriceValueWriteRequest> priceLevels,
        List<ProductSupplierRelationWriteRequest> supplierRelations,
        @NotNull Double stock,
        @NotNull Double safeStock,
        @NotNull Integer status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CategoryResponse(
        Long id,
        String name,
        Integer status,
        Integer sortOrder,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CategoryWriteRequest(
        @NotBlank String name,
        Integer status,
        Integer sortOrder
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UnitResponse(
        Long id,
        String name,
        Integer status,
        Integer sortOrder,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UnitWriteRequest(
        @NotBlank String name,
        Integer status,
        Integer sortOrder
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PriceLevelResponse(
        Long id,
        String code,
        String name,
        Integer status,
        Integer sortOrder,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PriceLevelWriteRequest(
        @NotBlank String code,
        @NotBlank String name,
        Integer status,
        Integer sortOrder
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductPriceValueResponse(
        Long levelId,
        String code,
        String name,
        Double price,
        Integer status,
        Integer sortOrder
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductPriceValueWriteRequest(
        @NotNull Long levelId,
        @NotNull Double price
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductSupplierRelationResponse(
        Long id,
        Long productId,
        Long supplierId,
        String supplierName,
        String supplierPhone,
        Boolean isDefault,
        Integer purchasePriority,
        Double lastPurchasePrice,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductSupplierRelationWriteRequest(
        @NotNull Long productId,
        @NotNull Long supplierId,
        Boolean isDefault,
        Integer purchasePriority,
        Double lastPurchasePrice,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductListResponse(
        List<ProductResponse> items
    ) {}
}
