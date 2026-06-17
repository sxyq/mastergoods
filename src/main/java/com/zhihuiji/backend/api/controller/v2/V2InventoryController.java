package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.inventory.V2InventoryDtos;
import com.zhihuiji.backend.application.service.v2.V2InventoryService;
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
@RequestMapping("/v2/inventory")
@RequireStorePermission("inventory:view")
public class V2InventoryController {
    private final V2InventoryService v2InventoryService;

    public V2InventoryController(V2InventoryService v2InventoryService) {
        this.v2InventoryService = v2InventoryService;
    }

    @GetMapping("/ledger")
    public ApiResponse<List<V2InventoryDtos.LedgerEntryResponse>> listLedger(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long startAt,
        @RequestParam(required = false) Long endAt
    ) {
        return ApiResponse.success(v2InventoryService.listLedger(productId, startAt, endAt));
    }

    @GetMapping("/ledger/by-source")
    public ApiResponse<List<V2InventoryDtos.LedgerEntryResponse>> listLedgerBySource(
        @RequestParam("source_type") String sourceType,
        @RequestParam("source_id") Long sourceId
    ) {
        return ApiResponse.success(v2InventoryService.listLedgerBySource(sourceType, sourceId));
    }

    @PostMapping("/ledger")
    @RequireStorePermission("inventory:write")
    public ApiResponse<V2InventoryDtos.LedgerEntryResponse> createLedgerEntry(@Valid @RequestBody V2InventoryDtos.LedgerEntryCreateRequest request) {
        return ApiResponse.success(v2InventoryService.createLedgerEntry(request));
    }

    @GetMapping("/snapshots")
    public ApiResponse<List<V2InventoryDtos.SnapshotResponse>> listSnapshots(
        @RequestParam(required = false) Long snapshotDate,
        @RequestParam(required = false) Long startDate,
        @RequestParam(required = false) Long endDate
    ) {
        return ApiResponse.success(v2InventoryService.listSnapshots(snapshotDate, startDate, endDate));
    }

    @PostMapping("/snapshots")
    @RequireStorePermission("inventory:write")
    public ApiResponse<V2InventoryDtos.SnapshotResponse> createSnapshot(@Valid @RequestBody V2InventoryDtos.SnapshotCreateRequest request) {
        return ApiResponse.success(v2InventoryService.createSnapshot(request));
    }

    @GetMapping("/monthly-stats")
    public ApiResponse<List<V2InventoryDtos.MonthlyStatsResponse>> listMonthlyStats(
        @RequestParam Integer year,
        @RequestParam Integer month
    ) {
        return ApiResponse.success(v2InventoryService.listMonthlyStats(year, month));
    }
}
