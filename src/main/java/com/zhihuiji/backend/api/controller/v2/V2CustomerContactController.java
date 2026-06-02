package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.v2.V2PartnerContactService;
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
@RequestMapping("/v2/customer-contacts")
public class V2CustomerContactController {
    private final V2PartnerContactService v2PartnerContactService;

    public V2CustomerContactController(V2PartnerContactService v2PartnerContactService) {
        this.v2PartnerContactService = v2PartnerContactService;
    }

    @GetMapping
    public ApiResponse<List<V2PartnerDtos.PartnerContactResponse>> list(@RequestParam("customer_id") Long customerId) {
        return ApiResponse.success(v2PartnerContactService.list(PartnerTypes.CUSTOMER, customerId));
    }

    @PostMapping
    public ApiResponse<V2PartnerDtos.PartnerContactResponse> create(@Valid @RequestBody V2PartnerDtos.PartnerContactWriteRequest request) {
        return ApiResponse.success(v2PartnerContactService.create(PartnerTypes.CUSTOMER, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<V2PartnerDtos.PartnerContactResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody V2PartnerDtos.PartnerContactWriteRequest request
    ) {
        return ApiResponse.success(v2PartnerContactService.update(PartnerTypes.CUSTOMER, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        v2PartnerContactService.delete(PartnerTypes.CUSTOMER, id);
        return ApiResponse.success(null);
    }
}
