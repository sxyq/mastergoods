package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.ParseUtils;
import com.zhihuiji.backend.api.dto.FinanceRecordDto;
import com.zhihuiji.backend.application.service.FinanceRecordService;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/finance-records")
public class FinanceRecordController {
    private final FinanceRecordService financeRecordService;

    public FinanceRecordController(FinanceRecordService financeRecordService) {
        this.financeRecordService = financeRecordService;
    }

    @GetMapping
    public ApiResponse<List<FinanceRecordDto>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "type", required = false) Integer type,
        @RequestParam(value = "created_after", required = false) String createdAfter,
        @RequestParam(value = "created_before", required = false) String createdBefore
    ) {
        List<FinanceRecordDto> payload = financeRecordService
            .list(keyword, type, ParseUtils.parseLong(createdAfter), ParseUtils.parseLong(createdBefore))
            .stream()
            .map(this::toDto)
            .toList();
        return ApiResponse.success(payload);
    }

    @PostMapping
    public ApiResponse<FinanceRecordDto> create(@Valid @RequestBody CreateRequest request) {
        FinanceRecordEntity created = financeRecordService.create(
            new FinanceRecordService.CreateCommand(
                request.type(),
                request.category(),
                request.partnerName(),
                request.amount(),
                request.method(),
                request.notes()
            )
        );
        return ApiResponse.success(toDto(created));
    }

    private FinanceRecordDto toDto(FinanceRecordEntity entity) {
        return new FinanceRecordDto(
            entity.getId(),
            entity.getRecordNo(),
            entity.getType(),
            entity.getCategory(),
            entity.getPartnerName(),
            entity.getAmount(),
            entity.getMethod(),
            entity.getNotes(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public record CreateRequest(
        Integer type,
        String category,
        String partnerName,
        Double amount,
        Integer method,
        String notes
    ) {}
}
