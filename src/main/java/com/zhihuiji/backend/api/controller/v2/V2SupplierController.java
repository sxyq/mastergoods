package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.v2.V2SupplierService;
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
@RequestMapping("/v2/suppliers")
public class V2SupplierController {
    private final V2SupplierService v2SupplierService;

    public V2SupplierController(V2SupplierService v2SupplierService) {
        this.v2SupplierService = v2SupplierService;
    }

    @GetMapping
    public ApiResponse<List<V2PartnerDtos.SupplierResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "group_id", required = false) Long groupId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(
            v2SupplierService.list(keyword, status, groupId),
            page,
            size
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2PartnerDtos.SupplierResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2SupplierService.get(id));
    }

    @PostMapping
    public ApiResponse<V2PartnerDtos.SupplierResponse> create(@Valid @RequestBody V2PartnerDtos.SupplierWriteRequest request) {
        return ApiResponse.success(v2SupplierService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<V2PartnerDtos.SupplierResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2PartnerDtos.SupplierWriteRequest request
    ) {
        return ApiResponse.success(v2SupplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2SupplierService.delete(id);
        return ApiResponse.success(null);
    }
}
