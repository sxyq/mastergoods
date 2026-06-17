package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.v2.V2BillFundLinkService;
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
@RequestMapping("/v2/bill-fund-links")
@RequireStorePermission("finance:view")
public class V2BillFundLinkController {
    private final V2BillFundLinkService v2BillFundLinkService;

    public V2BillFundLinkController(V2BillFundLinkService v2BillFundLinkService) {
        this.v2BillFundLinkService = v2BillFundLinkService;
    }

    @GetMapping
    public ApiResponse<List<V2FinanceDtos.BillFundLinkResponse>> list(
        @RequestParam(required = false) String billType,
        @RequestParam(required = false) Long billId,
        @RequestParam(required = false) Long accountId
    ) {
        if (billType != null && billId != null) {
            return ApiResponse.success(v2BillFundLinkService.listByBill(billType, billId));
        }
        if (accountId != null) {
            return ApiResponse.success(v2BillFundLinkService.listByAccount(accountId));
        }
        return ApiResponse.success(List.of());
    }

    @PostMapping
    @RequireStorePermission("finance:write")
    public ApiResponse<V2FinanceDtos.BillFundLinkResponse> create(@Valid @RequestBody V2FinanceDtos.BillFundLinkCreateRequest request) {
        return ApiResponse.success(v2BillFundLinkService.create(request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("finance:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2BillFundLinkService.delete(id);
        return ApiResponse.success(null);
    }
}
