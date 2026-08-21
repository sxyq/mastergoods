package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.v2.V2CashChangeRecordService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/cash-change-records")
@RequireStorePermission("finance:view")
public class V2CashChangeRecordController {
    private final V2CashChangeRecordService v2CashChangeRecordService;

    public V2CashChangeRecordController(V2CashChangeRecordService v2CashChangeRecordService) {
        this.v2CashChangeRecordService = v2CashChangeRecordService;
    }

    @GetMapping
    public ApiResponse<List<V2FinanceDtos.CashChangeRecordResponse>> list(
        @RequestParam(required = false) String orderType,
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) Long accountId
    ) {
        return ApiResponse.success(v2CashChangeRecordService.list(orderType, orderId, accountId));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2FinanceDtos.CashChangeRecordResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2CashChangeRecordService.get(id));
    }

    @PostMapping
    @RequireStorePermission("finance:write")
    public ApiResponse<V2FinanceDtos.CashChangeRecordResponse> create(
        @Valid @RequestBody V2FinanceDtos.CashChangeRecordCreateRequest request
    ) {
        return ApiResponse.success(v2CashChangeRecordService.create(request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("finance:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2CashChangeRecordService.delete(id);
        return ApiResponse.success(null);
    }
}
