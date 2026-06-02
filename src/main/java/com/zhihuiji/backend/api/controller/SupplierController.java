package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.application.service.SupplierService;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
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
@RequestMapping("/v1/suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ApiResponse<List<SupplierEntity>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(supplierService.list(keyword, status), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierEntity> get(@PathVariable Long id) {
        return ApiResponse.success(supplierService.get(id));
    }

    @PostMapping
    public ApiResponse<SupplierEntity> create(@Valid @RequestBody SupplierEntity payload) {
        return ApiResponse.success(supplierService.create(payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierEntity> update(@PathVariable Long id, @Valid @RequestBody SupplierEntity payload) {
        return ApiResponse.success(supplierService.update(id, payload));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.success(null);
    }
}
