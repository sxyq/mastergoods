package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.v2.V2ProductCategoryService;
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
@RequestMapping("/v2/product-categories")
public class V2ProductCategoryController {
    private final V2ProductCategoryService v2ProductCategoryService;

    public V2ProductCategoryController(V2ProductCategoryService v2ProductCategoryService) {
        this.v2ProductCategoryService = v2ProductCategoryService;
    }

    @GetMapping
    public ApiResponse<List<V2ProductDtos.CategoryResponse>> list() {
        return ApiResponse.success(v2ProductCategoryService.list());
    }

    @PostMapping
    public ApiResponse<V2ProductDtos.CategoryResponse> create(@Valid @RequestBody V2ProductDtos.CategoryWriteRequest request) {
        return ApiResponse.success(v2ProductCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<V2ProductDtos.CategoryResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2ProductDtos.CategoryWriteRequest request
    ) {
        return ApiResponse.success(v2ProductCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2ProductCategoryService.delete(id);
        return ApiResponse.success(null);
    }
}
