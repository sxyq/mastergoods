package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.store.V2StoreDtos;
import com.zhihuiji.backend.application.service.v2.V2StoreService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/stores/current")
public class V2StoreController {
    private final V2StoreService v2StoreService;

    public V2StoreController(V2StoreService v2StoreService) {
        this.v2StoreService = v2StoreService;
    }

    @GetMapping
    public ApiResponse<V2StoreDtos.CurrentStoreResponse> current() {
        return ApiResponse.success(v2StoreService.getCurrentStore());
    }

    @GetMapping("/members")
    @RequireStorePermission("users:manage")
    public ApiResponse<List<V2StoreDtos.MemberResponse>> members() {
        return ApiResponse.success(v2StoreService.listMembers());
    }

    @PostMapping("/members")
    @RequireStorePermission("users:manage")
    public ApiResponse<V2StoreDtos.MemberResponse> createMember(
        @Valid @RequestBody V2StoreDtos.MemberCreateRequest request
    ) {
        return ApiResponse.success(v2StoreService.createMember(request));
    }

    @PutMapping("/members/{userId}")
    @RequireStorePermission("users:manage")
    public ApiResponse<V2StoreDtos.MemberResponse> updateMember(
        @PathVariable Long userId,
        @Valid @RequestBody V2StoreDtos.MemberUpdateRequest request
    ) {
        return ApiResponse.success(v2StoreService.updateMember(userId, request));
    }
}
