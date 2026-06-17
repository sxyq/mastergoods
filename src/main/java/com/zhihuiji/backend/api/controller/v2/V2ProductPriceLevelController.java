package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.v2.V2ProductPriceLevelService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/product-price-levels")
@RequireStorePermission("archives:view")
public class V2ProductPriceLevelController {
    private final V2ProductPriceLevelService v2ProductPriceLevelService;

    public V2ProductPriceLevelController(V2ProductPriceLevelService v2ProductPriceLevelService) {
        this.v2ProductPriceLevelService = v2ProductPriceLevelService;
    }

    @GetMapping
    public ApiResponse<List<V2ProductDtos.PriceLevelResponse>> list() {
        return ApiResponse.success(v2ProductPriceLevelService.list());
    }

    @PostMapping
    @RequireStorePermission("archives:write")
    public ApiResponse<V2ProductDtos.PriceLevelResponse> create(@Valid @RequestBody V2ProductDtos.PriceLevelWriteRequest request) {
        return ApiResponse.success(v2ProductPriceLevelService.create(request));
    }

    @PutMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<V2ProductDtos.PriceLevelResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2ProductDtos.PriceLevelWriteRequest request
    ) {
        return ApiResponse.success(v2ProductPriceLevelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2ProductPriceLevelService.delete(id);
        return ApiResponse.success(null);
    }
}
