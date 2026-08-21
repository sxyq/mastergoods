package com.zhihuiji.backend.api.dto.v2.finance;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class V2FinanceDtos {
    private V2FinanceDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AccountResponse(
        Long id,
        String code,
        String name,
        Integer type,
        Double balance,
        Boolean isDefault,
        Integer status,
        Integer sortOrder,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AccountCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Integer type,
        Double balance,
        Boolean isDefault,
        Integer status,
        Integer sortOrder,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AccountUpdateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Integer type,
        Boolean isDefault,
        Integer status,
        Integer sortOrder,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AccountTransferResponse(
        Long id,
        String transferNo,
        Long fromAccountId,
        String fromAccountName,
        Long toAccountId,
        String toAccountName,
        Double amount,
        Double fee,
        Integer status,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AccountTransferCreateRequest(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull Double amount,
        Double fee,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BillFundLinkResponse(
        Long id,
        String billType,
        Long billId,
        Long accountId,
        String accountName,
        Double amount,
        Integer linkType,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BillFundLinkCreateRequest(
        @NotBlank String billType,
        @NotNull Long billId,
        @NotNull Long accountId,
        @NotNull Double amount,
        Integer linkType,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CashChangeRecordResponse(
        Long id,
        String orderType,
        Long orderId,
        Double receivable,
        Double received,
        Double changeAmount,
        Long accountId,
        String accountName,
        Integer status,
        String notes,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CashChangeRecordCreateRequest(
        @NotBlank String orderType,
        @NotNull Long orderId,
        @NotNull Double receivable,
        @NotNull Double received,
        Long accountId,
        Integer status,
        String notes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AccountListResponse(
        List<AccountResponse> items
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FinanceRecordResponse(
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

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FinanceRecordCreateRequest(
        Integer type,
        String category,
        String partnerName,
        Double amount,
        Integer method,
        String notes
    ) {}
}
