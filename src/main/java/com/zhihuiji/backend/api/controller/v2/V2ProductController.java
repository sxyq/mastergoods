package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.v2.V2ProductService;
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
@RequestMapping("/v2/products")
public class V2ProductController {
    private final V2ProductService v2ProductService;

    public V2ProductController(V2ProductService v2ProductService) {
        this.v2ProductService = v2ProductService;
    }

    @GetMapping
    public ApiResponse<List<V2ProductDtos.ProductResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "category_id", required = false) Long categoryId,
        @RequestParam(value = "unit_id", required = false) Long unitId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(
            v2ProductService.list(keyword, status, categoryId, unitId),
            page,
            size
        ));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<V2ProductDtos.ProductResponse>> lowStock(
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(v2ProductService.lowStock(size));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2ProductDtos.ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2ProductService.get(id));
    }

    @PostMapping
    public ApiResponse<V2ProductDtos.ProductResponse> create(@Valid @RequestBody V2ProductDtos.ProductWriteRequest request) {
        return ApiResponse.success(v2ProductService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<V2ProductDtos.ProductResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2ProductDtos.ProductWriteRequest request
    ) {
        return ApiResponse.success(v2ProductService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2ProductService.delete(id);
        return ApiResponse.success(null);
    }
}
