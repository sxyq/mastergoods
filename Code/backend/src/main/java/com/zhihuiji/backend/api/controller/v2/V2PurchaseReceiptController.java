package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReceiptDtos;
import com.zhihuiji.backend.application.service.v2.V2PurchaseReceiptService;
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
@RequestMapping("/v2/purchase-receipts")
@RequireStorePermission("inventory:view")
public class V2PurchaseReceiptController {
    private final V2PurchaseReceiptService v2PurchaseReceiptService;

    public V2PurchaseReceiptController(V2PurchaseReceiptService v2PurchaseReceiptService) {
        this.v2PurchaseReceiptService = v2PurchaseReceiptService;
    }

    @PostMapping
    @RequireStorePermission("inventory:write")
    public ApiResponse<V2PurchaseReceiptDtos.PurchaseReceiptResponse> create(
        @Valid @RequestBody V2PurchaseReceiptDtos.CreateRequest request
    ) {
        return ApiResponse.success(v2PurchaseReceiptService.create(request));
    }

    @GetMapping
    public ApiResponse<List<V2PurchaseReceiptDtos.PurchaseReceiptResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(v2PurchaseReceiptService.list(keyword, status, PaginationUtils.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2PurchaseReceiptDtos.PurchaseReceiptResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2PurchaseReceiptService.get(id));
    }

    @GetMapping("/by-order/{orderId}")
    public ApiResponse<List<V2PurchaseReceiptDtos.PurchaseReceiptResponse>> listByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(v2PurchaseReceiptService.listByOrder(orderId));
    }

    @PutMapping("/{id}/draft")
    @RequireStorePermission("inventory:write")
    public ApiResponse<V2PurchaseReceiptDtos.PurchaseReceiptResponse> updateDraft(
        @PathVariable Long id,
        @Valid @RequestBody V2PurchaseReceiptDtos.UpdateDraftRequest request
    ) {
        return ApiResponse.success(v2PurchaseReceiptService.updateDraft(id, request));
    }

    @PutMapping("/{id}/confirm")
    @RequireStorePermission("inventory:write")
    public ApiResponse<V2PurchaseReceiptDtos.PurchaseReceiptResponse> confirm(@PathVariable Long id) {
        return ApiResponse.success(v2PurchaseReceiptService.confirm(id));
    }

    @PutMapping("/{id}/cancel")
    @RequireStorePermission("inventory:write")
    public ApiResponse<V2PurchaseReceiptDtos.PurchaseReceiptResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(v2PurchaseReceiptService.cancel(id));
    }
}
