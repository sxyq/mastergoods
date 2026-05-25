package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.ProductAdjustStockRequest;
import com.zhihuiji.backend.application.service.ProductService;
import com.zhihuiji.backend.domain.entity.ProductEntity;
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
@RequestMapping("/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductEntity>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(productService.list(keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductEntity> get(@PathVariable Long id) {
        return ApiResponse.success(productService.get(id));
    }

    @GetMapping("/by-code")
    public ApiResponse<ProductEntity> getByCode(@RequestParam("code") String code) {
        return ApiResponse.success(productService.findByCode(code));
    }

    @PostMapping
    public ApiResponse<ProductEntity> create(@Valid @RequestBody ProductEntity payload) {
        return ApiResponse.success(productService.create(payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductEntity> update(@PathVariable Long id, @Valid @RequestBody ProductEntity payload) {
        return ApiResponse.success(productService.update(id, payload));
    }

    @PostMapping("/{id}/adjust-stock")
    public ApiResponse<ProductEntity> adjustStock(@PathVariable Long id, @Valid @RequestBody ProductAdjustStockRequest request) {
        return ApiResponse.success(productService.adjustStock(id, request.delta(), request.reason(), request.operator()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success(null);
    }
}
