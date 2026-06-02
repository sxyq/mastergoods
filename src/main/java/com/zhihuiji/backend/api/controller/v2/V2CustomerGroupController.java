package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.v2.V2PartnerGroupService;
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
@RequestMapping("/v2/customer-groups")
public class V2CustomerGroupController {
    private final V2PartnerGroupService v2PartnerGroupService;

    public V2CustomerGroupController(V2PartnerGroupService v2PartnerGroupService) {
        this.v2PartnerGroupService = v2PartnerGroupService;
    }

    @GetMapping
    public ApiResponse<List<V2PartnerDtos.PartnerGroupResponse>> list() {
        return ApiResponse.success(v2PartnerGroupService.list(PartnerTypes.CUSTOMER));
    }

    @PostMapping
    public ApiResponse<V2PartnerDtos.PartnerGroupResponse> create(@Valid @RequestBody V2PartnerDtos.PartnerGroupWriteRequest request) {
        return ApiResponse.success(v2PartnerGroupService.create(PartnerTypes.CUSTOMER, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<V2PartnerDtos.PartnerGroupResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2PartnerDtos.PartnerGroupWriteRequest request
    ) {
        return ApiResponse.success(v2PartnerGroupService.update(PartnerTypes.CUSTOMER, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2PartnerGroupService.delete(PartnerTypes.CUSTOMER, id);
        return ApiResponse.success(null);
    }
}
