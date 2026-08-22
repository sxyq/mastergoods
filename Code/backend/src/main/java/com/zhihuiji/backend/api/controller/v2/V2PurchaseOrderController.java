package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseOrderDtos;
import com.zhihuiji.backend.application.service.v2.V2PurchaseOrderService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/purchase-orders")
@RequireStorePermission("purchase:view")
public class V2PurchaseOrderController {
    private final V2PurchaseOrderService v2PurchaseOrderService;

    public V2PurchaseOrderController(V2PurchaseOrderService v2PurchaseOrderService) {
        this.v2PurchaseOrderService = v2PurchaseOrderService;
    }

    @PostMapping
    @RequireStorePermission("purchase:write")
    public ApiResponse<V2PurchaseOrderDtos.PurchaseOrderResponse> create(
        @Valid @RequestBody V2PurchaseOrderDtos.CreateRequest request
    ) {
        return ApiResponse.success(v2PurchaseOrderService.create(request));
    }

    @GetMapping
    public ApiResponse<List<V2PurchaseOrderDtos.PurchaseOrderResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(v2PurchaseOrderService.list(keyword, status, PaginationUtils.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2PurchaseOrderDtos.PurchaseOrderResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2PurchaseOrderService.get(id));
    }

    @PutMapping("/{id}")
    @RequireStorePermission("purchase:write")
    public ApiResponse<V2PurchaseOrderDtos.PurchaseOrderResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2PurchaseOrderDtos.CreateRequest request
    ) {
        return ApiResponse.success(v2PurchaseOrderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("purchase:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2PurchaseOrderService.delete(id);
        return ApiResponse.success(null);
    }
}
