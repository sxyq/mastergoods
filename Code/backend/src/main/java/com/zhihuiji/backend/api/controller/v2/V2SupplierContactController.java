package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.v2.V2PartnerContactService;
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
@RequestMapping("/v2/supplier-contacts")
@RequireStorePermission("archives:view")
public class V2SupplierContactController {
    private final V2PartnerContactService v2PartnerContactService;

    public V2SupplierContactController(V2PartnerContactService v2PartnerContactService) {
        this.v2PartnerContactService = v2PartnerContactService;
    }

    @GetMapping
    public ApiResponse<List<V2PartnerDtos.PartnerContactResponse>> list(@RequestParam("supplier_id") Long supplierId) {
        return ApiResponse.success(v2PartnerContactService.list(PartnerTypes.SUPPLIER, supplierId));
    }

    @PostMapping
    @RequireStorePermission("archives:write")
    public ApiResponse<V2PartnerDtos.PartnerContactResponse> create(@Valid @RequestBody V2PartnerDtos.PartnerContactWriteRequest request) {
        return ApiResponse.success(v2PartnerContactService.create(PartnerTypes.SUPPLIER, request));
    }

    @PutMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<V2PartnerDtos.PartnerContactResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2PartnerDtos.PartnerContactWriteRequest request
    ) {
        return ApiResponse.success(v2PartnerContactService.update(PartnerTypes.SUPPLIER, id, request));
    }

    @DeleteMapping("/{id}")
    @RequireStorePermission("archives:write")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2PartnerContactService.delete(PartnerTypes.SUPPLIER, id);
        return ApiResponse.success(null);
    }
}
