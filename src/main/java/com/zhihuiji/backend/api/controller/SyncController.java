package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.application.service.SyncService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sync")
public class SyncController {
    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/upload")
    public ApiResponse<SyncService.UploadResult> upload(@RequestBody UploadRequest request) {
        List<SyncService.SyncChange> changes = request.changes().stream()
            .map(c -> new SyncService.SyncChange(c.entityType(), c.entityId(), c.operation(), c.payload(), c.updatedAt()))
            .toList();
        return ApiResponse.success(syncService.upload(request.clientId(), changes, request.lastSyncCursor()));
    }

    @PostMapping("/pull")
    public ApiResponse<SyncService.PullResult> pull(@RequestBody PullRequest request) {
        return ApiResponse.success(syncService.pull(request.sinceCursor(), request.limit()));
    }

    @GetMapping("/health")
    public ApiResponse<SyncService.HealthResult> health() {
        return ApiResponse.success(syncService.health());
    }

    public record UploadRequest(String clientId, List<SyncChangeDto> changes, String lastSyncCursor) {}

    public record SyncChangeDto(String entityType, String entityId, String operation, String payload, Long updatedAt) {}

    public record PullRequest(String sinceCursor, Integer limit) {
        public Integer limit() {
            return limit == null ? 200 : Math.max(1, Math.min(limit, 500));
        }
    }
}
