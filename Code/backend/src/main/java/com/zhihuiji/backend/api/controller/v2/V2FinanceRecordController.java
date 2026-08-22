package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.ParseUtils;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.FinanceRecordService;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/finance-records")
@RequireStorePermission("finance:view")
public class V2FinanceRecordController {
    private final FinanceRecordService financeRecordService;

    public V2FinanceRecordController(FinanceRecordService financeRecordService) {
        this.financeRecordService = financeRecordService;
    }

    @GetMapping
    public ApiResponse<List<V2FinanceDtos.FinanceRecordResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "type", required = false) Integer type,
        @RequestParam(value = "created_after", required = false) String createdAfter,
        @RequestParam(value = "created_before", required = false) String createdBefore,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        List<FinanceRecordEntity> rows = financeRecordService.list(
            keyword,
            type,
            ParseUtils.parseLong(createdAfter),
            ParseUtils.parseLong(createdBefore),
            PaginationUtils.pageable(page, size)
        );
        List<V2FinanceDtos.FinanceRecordResponse> payload = rows.stream()
            .map(this::toResponse)
            .toList();
        return ApiResponse.success(payload);
    }

    @PostMapping
    @RequireStorePermission("finance:write")
    public ApiResponse<V2FinanceDtos.FinanceRecordResponse> create(
        @Valid @RequestBody V2FinanceDtos.FinanceRecordCreateRequest request
    ) {
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
        return ApiResponse.success(toResponse(created));
    }

    private V2FinanceDtos.FinanceRecordResponse toResponse(FinanceRecordEntity entity) {
        return new V2FinanceDtos.FinanceRecordResponse(
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
}
