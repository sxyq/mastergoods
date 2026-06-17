package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.v2.V2CustomerService;
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
@RequestMapping("/v2/customers")
@RequireStorePermission("archives:view")
public class V2CustomerController {
    private final V2CustomerService v2CustomerService;

    public V2CustomerController(V2CustomerService v2CustomerService) {
        this.v2CustomerService = v2CustomerService;
    }

    @GetMapping
    public ApiResponse<List<V2PartnerDtos.CustomerResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "group_id", required = false) Long groupId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(
            v2CustomerService.list(keyword, status, groupId),
            page,
            size
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2PartnerDtos.CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2CustomerService.get(id));
    }

    @PostMapping
    @RequireStorePermission("archives:write")
    public ApiResponse<V2PartnerDtos.CustomerResponse> create(@Valid @RequestBody V2PartnerDtos.CustomerWriteRequest request) {
        return ApiResponse.success(v2CustomerService.create(request));
    }

    @PutMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<V2PartnerDtos.CustomerResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2PartnerDtos.CustomerWriteRequest request
    ) {
        return ApiResponse.success(v2CustomerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2CustomerService.delete(id);
        return ApiResponse.success(null);
    }
}
