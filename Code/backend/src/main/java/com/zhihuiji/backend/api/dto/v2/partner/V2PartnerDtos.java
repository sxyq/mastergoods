package com.zhihuiji.backend.api.dto.v2.partner;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class V2PartnerDtos {
    private V2PartnerDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PartnerGroupResponse(
        Long id,
        String partnerType,
        String name,
        Integer status,
        Integer sortOrder,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PartnerGroupWriteRequest(
        @NotBlank String name,
        Integer status,
        Integer sortOrder
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PartnerContactResponse(
        Long id,
        String partnerType,
        Long partnerId,
        String name,
        String phone,
        String title,
        Boolean isPrimary,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PartnerContactWriteRequest(
        @NotNull Long partnerId,
        @NotBlank String name,
        String phone,
        String title,
        Boolean isPrimary
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CustomerResponse(
        Long id,
        String name,
        String phone,
        Integer level,
        Long groupId,
        String groupName,
        String primaryContactName,
        String primaryContactPhone,
        String address,
        String notes,
        Double balance,
        Integer status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CustomerWriteRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotNull Integer level,
        Long groupId,
        String primaryContactName,
        String primaryContactPhone,
        String address,
        String notes,
        Double balance,
        Integer status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SupplierResponse(
        Long id,
        String name,
        String phone,
        Long groupId,
        String groupName,
        String primaryContactName,
        String primaryContactPhone,
        String address,
        String notes,
        Double balance,
        Integer status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SupplierWriteRequest(
        @NotBlank String name,
        @NotBlank String phone,
        Long groupId,
        String primaryContactName,
        String primaryContactPhone,
        String address,
        String notes,
        Double balance,
        Integer status
    ) {}
}
