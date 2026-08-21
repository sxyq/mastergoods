package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.sync.V2ImportJobDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.LegacySQLiteImportService;
import com.zhihuiji.backend.application.service.v2.V2ImportJobService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/import-jobs")
@RequireStorePermission("database:manage")
public class V2ImportJobController {
    private final V2ImportJobService v2ImportJobService;
    private final LegacySQLiteImportService legacySQLiteImportService;
    private final CurrentOwnerService currentOwnerService;

    public V2ImportJobController(
        V2ImportJobService v2ImportJobService,
        LegacySQLiteImportService legacySQLiteImportService,
        CurrentOwnerService currentOwnerService
    ) {
        this.v2ImportJobService = v2ImportJobService;
        this.legacySQLiteImportService = legacySQLiteImportService;
        this.currentOwnerService = currentOwnerService;
    }

    @GetMapping
    public ApiResponse<List<V2ImportJobDtos.ImportJobResponse>> list(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(v2ImportJobService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2ImportJobDtos.ImportJobResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2ImportJobService.get(id));
    }

    @PostMapping
    public ApiResponse<V2ImportJobDtos.ImportJobResponse> create(
        @Valid @RequestBody V2ImportJobDtos.ImportJobCreateRequest request
    ) {
        return ApiResponse.success(v2ImportJobService.create(request));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<V2ImportJobDtos.ImportJobResponse> retry(
        @PathVariable Long id,
        @RequestBody(required = false) V2ImportJobDtos.ImportJobRetryRequest request
    ) {
        return ApiResponse.success(v2ImportJobService.retry(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<V2ImportJobDtos.ImportJobResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(v2ImportJobService.cancel(id));
    }

    @PostMapping("/legacy-sqlite")
    public ApiResponse<LegacySQLiteImportService.ImportResult> importLegacySqlite(
        @Valid @RequestBody V2ImportJobDtos.LegacySQLiteImportRequest request
    ) {
        return ApiResponse.success(
            legacySQLiteImportService.importIntoExistingOwner(
                currentOwnerService.requireCurrentOwnerUserId(),
                new LegacySQLiteImportService.ExistingOwnerImportRequest(
                    request.legacyDbPath(),
                    request.resetOwnedData()
                )
            )
        );
    }
}
