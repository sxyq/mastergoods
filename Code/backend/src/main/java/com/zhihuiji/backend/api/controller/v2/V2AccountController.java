package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.v2.V2AccountService;
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
@RequestMapping("/v2/accounts")
@RequireStorePermission("finance:view")
public class V2AccountController {
    private final V2AccountService v2AccountService;

    public V2AccountController(V2AccountService v2AccountService) {
        this.v2AccountService = v2AccountService;
    }

    @GetMapping
    public ApiResponse<List<V2FinanceDtos.AccountResponse>> list() {
        return ApiResponse.success(v2AccountService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<V2FinanceDtos.AccountResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2AccountService.get(id));
    }

    @PostMapping
    @RequireStorePermission("finance:write")
    public ApiResponse<V2FinanceDtos.AccountResponse> create(@Valid @RequestBody V2FinanceDtos.AccountCreateRequest request) {
        return ApiResponse.success(v2AccountService.create(request));
    }

    @PutMapping("/{id}")
    @RequireStorePermission("finance:write")
    public ApiResponse<V2FinanceDtos.AccountResponse> update(@PathVariable Long id, @Valid @RequestBody V2FinanceDtos.AccountUpdateRequest request) {
        return ApiResponse.success(v2AccountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("finance:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2AccountService.delete(id);
        return ApiResponse.success(null);
    }
}
