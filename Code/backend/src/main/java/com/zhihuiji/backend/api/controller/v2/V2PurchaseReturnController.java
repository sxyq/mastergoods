package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReturnDtos;
import com.zhihuiji.backend.application.service.v2.V2PurchaseReturnService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/purchase-returns")
@RequireStorePermission("purchase:view")
public class V2PurchaseReturnController {
    private final V2PurchaseReturnService v2PurchaseReturnService;

    public V2PurchaseReturnController(V2PurchaseReturnService v2PurchaseReturnService) {
        this.v2PurchaseReturnService = v2PurchaseReturnService;
    }

    @PostMapping
    @RequireStorePermission("purchase:write")
    public ApiResponse<V2PurchaseReturnDtos.PurchaseReturnResponse> create(
        @Valid @RequestBody V2PurchaseReturnDtos.CreateRequest request
    ) {
        return ApiResponse.success(v2PurchaseReturnService.create(request));
    }

    @GetMapping
    public ApiResponse<List<V2PurchaseReturnDtos.PurchaseReturnResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(v2PurchaseReturnService.list(keyword, status), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2PurchaseReturnDtos.PurchaseReturnResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2PurchaseReturnService.get(id));
    }

    @GetMapping("/by-order/{orderId}")
    public ApiResponse<List<V2PurchaseReturnDtos.PurchaseReturnResponse>> listByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(v2PurchaseReturnService.listByOrder(orderId));
    }

    @PutMapping("/{id}/draft")
    @RequireStorePermission("purchase:write")
    public ApiResponse<V2PurchaseReturnDtos.PurchaseReturnResponse> updateDraft(
        @PathVariable Long id,
        @Valid @RequestBody V2PurchaseReturnDtos.UpdateDraftRequest request
    ) {
        return ApiResponse.success(v2PurchaseReturnService.updateDraft(id, request));
    }

    @PutMapping("/{id}/confirm")
    @RequireStorePermission("purchase:write")
    public ApiResponse<V2PurchaseReturnDtos.PurchaseReturnResponse> confirm(
        @PathVariable Long id,
        @Valid @RequestBody V2PurchaseReturnDtos.ConfirmRequest request
    ) {
        return ApiResponse.success(v2PurchaseReturnService.confirm(id, request));
    }

    @PostMapping("/{id}/refunds")
    @RequireStorePermission("finance:write")
    public ApiResponse<V2PurchaseReturnDtos.PurchaseReturnResponse> addRefund(
        @PathVariable Long id,
        @Valid @RequestBody V2PurchaseReturnDtos.RefundRequest request
    ) {
        return ApiResponse.success(v2PurchaseReturnService.addRefund(id, request));
    }

    @PutMapping("/{id}/cancel")
    @RequireStorePermission("purchase:write")
    public ApiResponse<V2PurchaseReturnDtos.PurchaseReturnResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(v2PurchaseReturnService.cancel(id));
    }
}
