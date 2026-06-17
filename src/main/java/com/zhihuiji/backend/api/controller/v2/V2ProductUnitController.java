package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.v2.V2ProductUnitService;
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
@RequestMapping("/v2/product-units")
@RequireStorePermission("archives:view")
public class V2ProductUnitController {
    private final V2ProductUnitService v2ProductUnitService;

    public V2ProductUnitController(V2ProductUnitService v2ProductUnitService) {
        this.v2ProductUnitService = v2ProductUnitService;
    }

    @GetMapping
    public ApiResponse<List<V2ProductDtos.UnitResponse>> list() {
        return ApiResponse.success(v2ProductUnitService.list());
    }

    @PostMapping
    @RequireStorePermission("archives:write")
    public ApiResponse<V2ProductDtos.UnitResponse> create(@Valid @RequestBody V2ProductDtos.UnitWriteRequest request) {
        return ApiResponse.success(v2ProductUnitService.create(request));
    }

    @PutMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<V2ProductDtos.UnitResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2ProductDtos.UnitWriteRequest request
    ) {
        return ApiResponse.success(v2ProductUnitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2ProductUnitService.delete(id);
        return ApiResponse.success(null);
    }
}
