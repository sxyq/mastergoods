package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.media.V2MediaDtos;
import com.zhihuiji.backend.application.service.v2.V2MediaService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/media")
@RequireStorePermission("dashboard:view")
public class V2MediaController {
    private final V2MediaService v2MediaService;

    public V2MediaController(V2MediaService v2MediaService) {
        this.v2MediaService = v2MediaService;
    }

    @GetMapping("/assets")
    public ApiResponse<List<V2MediaDtos.MediaAssetResponse>> listAssets() {
        return ApiResponse.success(v2MediaService.listAssets());
    }

    @GetMapping("/assets/{id}")
    public ApiResponse<V2MediaDtos.MediaAssetResponse> getAsset(@PathVariable Long id) {
        return ApiResponse.success(v2MediaService.getAsset(id));
    }

    @PostMapping("/assets")
    @RequireStorePermission(anyOf = {"archives:write", "sales:write", "purchase:write", "finance:write", "inventory:write", "agent:write"})
    public ApiResponse<V2MediaDtos.MediaAssetResponse> createAsset(@Valid @RequestBody V2MediaDtos.MediaAssetCreateRequest request) {
        return ApiResponse.success(v2MediaService.createAsset(request));
    }

    @DeleteMapping("/assets/{id}")
    @RequireStorePermission(anyOf = {"archives:write", "sales:write", "purchase:write", "finance:write", "inventory:write", "agent:write"})
    public ApiResponse<Void> deleteAsset(@PathVariable Long id) {
        v2MediaService.deleteAsset(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/bindings")
    public ApiResponse<List<V2MediaDtos.MediaBindingResponse>> listBindings(
        @RequestParam("target_type") String targetType,
        @RequestParam("target_id") Long targetId
    ) {
        return ApiResponse.success(v2MediaService.listBindings(targetType, targetId));
    }

    @PostMapping("/bindings")
    @RequireStorePermission(anyOf = {"archives:write", "sales:write", "purchase:write", "finance:write", "inventory:write", "agent:write"})
    public ApiResponse<V2MediaDtos.MediaBindingResponse> createBinding(@Valid @RequestBody V2MediaDtos.MediaBindingCreateRequest request) {
        return ApiResponse.success(v2MediaService.createBinding(request));
    }

    @DeleteMapping("/bindings/{id}")
    @RequireStorePermission(anyOf = {"archives:write", "sales:write", "purchase:write", "finance:write", "inventory:write", "agent:write"})
    public ApiResponse<Void> deleteBinding(@PathVariable Long id) {
        v2MediaService.deleteBinding(id);
        return ApiResponse.success(null);
    }
}
