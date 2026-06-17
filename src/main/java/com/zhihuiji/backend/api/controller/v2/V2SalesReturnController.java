package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.sales.V2SalesReturnDtos;
import com.zhihuiji.backend.application.service.v2.V2SalesReturnService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/sales-returns")
@RequireStorePermission("sales:view")
public class V2SalesReturnController {
    private final V2SalesReturnService v2SalesReturnService;

    public V2SalesReturnController(V2SalesReturnService v2SalesReturnService) {
        this.v2SalesReturnService = v2SalesReturnService;
    }

    @PostMapping
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SalesReturnDtos.SalesReturnResponse> create(
        @Valid @RequestBody V2SalesReturnDtos.CreateRequest request
    ) {
        return ApiResponse.success(v2SalesReturnService.create(request));
    }

    @GetMapping
    public ApiResponse<List<V2SalesReturnDtos.SalesReturnResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(v2SalesReturnService.list(keyword, status), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2SalesReturnDtos.SalesReturnResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2SalesReturnService.get(id));
    }

    @GetMapping("/by-order/{orderId}")
    public ApiResponse<List<V2SalesReturnDtos.SalesReturnResponse>> listByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(v2SalesReturnService.listByOrder(orderId));
    }

    @PutMapping("/{id}/draft")
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SalesReturnDtos.SalesReturnResponse> updateDraft(
        @PathVariable Long id,
        @Valid @RequestBody V2SalesReturnDtos.UpdateDraftRequest request
    ) {
        return ApiResponse.success(v2SalesReturnService.updateDraft(id, request));
    }

    @PutMapping("/{id}/confirm")
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SalesReturnDtos.SalesReturnResponse> confirm(
        @PathVariable Long id,
        @Valid @RequestBody V2SalesReturnDtos.ConfirmRequest request
    ) {
        return ApiResponse.success(v2SalesReturnService.confirm(id, request));
    }

    @PostMapping("/{id}/refunds")
    @RequireStorePermission("finance:write")
    public ApiResponse<V2SalesReturnDtos.SalesReturnResponse> addRefund(
        @PathVariable Long id,
        @Valid @RequestBody V2SalesReturnDtos.RefundRequest request
    ) {
        return ApiResponse.success(v2SalesReturnService.addRefund(id, request));
    }

    @PutMapping("/{id}/cancel")
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SalesReturnDtos.SalesReturnResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(v2SalesReturnService.cancel(id));
    }
}
