package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.sync.V2SyncDtos;
import com.zhihuiji.backend.application.service.v2.V2SyncService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/sync")
@RequireStorePermission("database:manage")
public class V2SyncController {
    private final V2SyncService v2SyncService;

    public V2SyncController(V2SyncService v2SyncService) {
        this.v2SyncService = v2SyncService;
    }

    @GetMapping("/health")
    public ApiResponse<V2SyncDtos.SyncHealthResponse> health() {
        V2SyncService.HealthResult result = v2SyncService.health();
        return ApiResponse.success(new V2SyncDtos.SyncHealthResponse(
            result.status(),
            result.message(),
            result.ownerScoped(),
            result.serverTime(),
            result.supportedEntityTypes(),
            result.uploadableEntityTypes()
        ));
    }

    @GetMapping("/cursor/{clientId}")
    public ApiResponse<V2SyncDtos.SyncCursorResponse> cursor(@PathVariable String clientId) {
        V2SyncService.CursorStatus result = v2SyncService.cursorStatus(clientId);
        return ApiResponse.success(new V2SyncDtos.SyncCursorResponse(
            result.clientId(),
            result.lastCursor(),
            result.updatedAt()
        ));
    }

    @PostMapping("/cursor/ack")
    public ApiResponse<V2SyncDtos.SyncCursorResponse> acknowledgeCursor(
        @Valid @RequestBody V2SyncDtos.SyncCursorAckRequest request
    ) {
        V2SyncService.CursorStatus result = v2SyncService.acknowledgeCursor(request.clientId(), request.cursor());
        return ApiResponse.success(new V2SyncDtos.SyncCursorResponse(
            result.clientId(),
            result.lastCursor(),
            result.updatedAt()
        ));
    }

    @PostMapping("/upload")
    public ApiResponse<V2SyncDtos.SyncUploadResponse> upload(@Valid @RequestBody V2SyncDtos.SyncUploadRequest request) {
        List<V2SyncService.SyncChange> changes = new java.util.ArrayList<>(request.changes().size());
        for (V2SyncDtos.SyncChangeDto item : request.changes()) {
            changes.add(new V2SyncService.SyncChange(
                item.entityType(),
                item.entityId(),
                item.operation(),
                item.payload(),
                item.updatedAt()
            ));
        }
        V2SyncService.UploadResult result = v2SyncService.upload(request.clientId(), changes, request.lastSyncCursor());
        return ApiResponse.success(new V2SyncDtos.SyncUploadResponse(
            result.acceptedCount(),
            result.failedCount(),
            result.status(),
            result.nextCursor()
        ));
    }

    @PostMapping("/pull")
    public ApiResponse<V2SyncDtos.SyncPullResponse> pull(@Valid @RequestBody V2SyncDtos.SyncPullRequest request) {
        V2SyncService.PullResult result = v2SyncService.pull(request.clientId(), request.sinceCursor(), request.limit());
        List<V2SyncDtos.SyncChangeDto> changes = new java.util.ArrayList<>(result.changes().size());
        for (V2SyncService.SyncChange item : result.changes()) {
            changes.add(new V2SyncDtos.SyncChangeDto(
                item.entityType(),
                item.entityId(),
                item.operation(),
                item.payload(),
                item.updatedAt()
            ));
        }
        return ApiResponse.success(new V2SyncDtos.SyncPullResponse(
            changes,
            result.effectiveCursor(),
            result.nextCursor(),
            result.hasMore()
        ));
    }
}
