package com.zhihuiji.backend.api.controller.v2.product;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.v2.product.V2ProductSupplierRelationService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/v2/product-supplier-relations")
@RequireStorePermission("archives:view")
public class V2ProductSupplierRelationController {
    private final V2ProductSupplierRelationService v2ProductSupplierRelationService;

    public V2ProductSupplierRelationController(V2ProductSupplierRelationService v2ProductSupplierRelationService) {
        this.v2ProductSupplierRelationService = v2ProductSupplierRelationService;
    }

    @GetMapping
    public ApiResponse<List<V2ProductDtos.ProductSupplierRelationResponse>> list(
        @RequestParam("product_id") Long productId
    ) {
        return ApiResponse.success(v2ProductSupplierRelationService.list(productId));
    }

    @PostMapping
    @RequireStorePermission("archives:write")
    public ApiResponse<V2ProductDtos.ProductSupplierRelationResponse> create(
        @Valid @RequestBody V2ProductDtos.ProductSupplierRelationWriteRequest request
    ) {
        return ApiResponse.success(v2ProductSupplierRelationService.create(request));
    }

    @PutMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<V2ProductDtos.ProductSupplierRelationResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2ProductDtos.ProductSupplierRelationWriteRequest request
    ) {
        return ApiResponse.success(v2ProductSupplierRelationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2ProductSupplierRelationService.delete(id);
        return ApiResponse.success(null);
    }
}
