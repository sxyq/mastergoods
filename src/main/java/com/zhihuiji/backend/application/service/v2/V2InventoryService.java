package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.inventory.V2InventoryDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.InventoryLedgerEntity;
import com.zhihuiji.backend.domain.entity.InventoryMonthlyStatsEntity;
import com.zhihuiji.backend.domain.entity.InventorySnapshotEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryLedgerRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryMonthlyStatsRepository;
import com.zhihuiji.backend.infrastructure.repository.InventorySnapshotRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2InventoryService {
    private final InventoryLedgerRepository ledgerRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final InventoryMonthlyStatsRepository monthlyStatsRepository;
    private final ProductRepository productRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2InventoryService(InventoryLedgerRepository ledgerRepository,
                              InventorySnapshotRepository snapshotRepository,
                              InventoryMonthlyStatsRepository monthlyStatsRepository,
                              ProductRepository productRepository,
                              CurrentOwnerService currentOwnerService) {
        this.ledgerRepository = ledgerRepository;
        this.snapshotRepository = snapshotRepository;
        this.monthlyStatsRepository = monthlyStatsRepository;
        this.productRepository = productRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public Page<V2InventoryDtos.LedgerEntryResponse> listLedger(Long productId, Long startAt, Long endAt, int page, int size) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Pageable pageable = toPageable(page, size);
        Page<InventoryLedgerEntity> entityPage;
        if (productId != null) {
            entityPage = ledgerRepository.findAllByOwnerUserIdAndProductIdOrderByCreatedAtDesc(ownerUserId, productId, pageable);
        } else if (startAt != null && endAt != null) {
            entityPage = ledgerRepository.findAllByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(ownerUserId, startAt, endAt, pageable);
        } else {
            entityPage = ledgerRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, pageable);
        }
        return entityPage.map(this::toLedgerResponse);
    }

    public List<V2InventoryDtos.LedgerEntryResponse> listLedgerBySource(String sourceType, Long sourceId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<InventoryLedgerEntity> rows = ledgerRepository.findAllByOwnerUserIdAndSourceTypeAndSourceIdOrderByCreatedAtDesc(
            ownerUserId,
            sourceType,
            sourceId
        );
        List<V2InventoryDtos.LedgerEntryResponse> responses = new java.util.ArrayList<>(rows.size());
        for (InventoryLedgerEntity row : rows) {
            responses.add(toLedgerResponse(row));
        }
        return responses;
    }

    @Transactional
    public V2InventoryDtos.LedgerEntryResponse createLedgerEntry(V2InventoryDtos.LedgerEntryCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductEntity product = productRepository.findByIdAndOwnerUserId(request.productId(), ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        Double quantityBefore = product.getStock() != null ? product.getStock() : 0.0;
        Double quantityChange = request.quantityChange();
        Double quantityAfter = quantityBefore + quantityChange;
        long now = System.currentTimeMillis();
        InventoryLedgerEntity entity = new InventoryLedgerEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setProductId(product.getId());
        entity.setProductCode(product.getCode());
        entity.setProductName(product.getName());
        entity.setWarehouseId(request.warehouseId());
        entity.setQuantityBefore(quantityBefore);
        entity.setQuantityChange(quantityChange);
        entity.setQuantityAfter(quantityAfter);
        entity.setUnitCost(request.unitCost());
        entity.setSourceType(request.sourceType());
        entity.setSourceId(request.sourceId());
        entity.setSourceNo(request.sourceNo());
        entity.setNotes(request.notes());
        entity.setCreatedAt(now);
        product.setStock(quantityAfter);
        productRepository.save(product);
        return toLedgerResponse(ledgerRepository.save(entity));
    }

    public Page<V2InventoryDtos.SnapshotResponse> listSnapshots(Long snapshotDate, Long startDate, Long endDate, int page, int size) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Pageable pageable = toPageable(page, size);
        Page<InventorySnapshotEntity> entityPage;
        if (snapshotDate != null) {
            entityPage = snapshotRepository.findAllByOwnerUserIdAndSnapshotDateOrderByProductNameAsc(ownerUserId, snapshotDate, pageable);
        } else if (startDate != null && endDate != null) {
            entityPage = snapshotRepository.findAllByOwnerUserIdAndSnapshotDateBetweenOrderBySnapshotDateAscProductNameAsc(ownerUserId, startDate, endDate, pageable);
        } else {
            entityPage = Page.empty(pageable);
        }
        return entityPage.map(this::toSnapshotResponse);
    }

    @Transactional
    public V2InventoryDtos.SnapshotResponse createSnapshot(V2InventoryDtos.SnapshotCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductEntity product = productRepository.findByIdAndOwnerUserId(request.productId(), ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        InventorySnapshotEntity existing = snapshotRepository.findByOwnerUserIdAndProductIdAndSnapshotDate(
            ownerUserId, product.getId(), request.snapshotDate()).orElse(null);
        if (existing != null) {
            existing.setQuantity(product.getStock() != null ? product.getStock() : 0.0);
            existing.setUnitCost(product.getPurchasePrice());
            existing.setTotalValue(existing.getQuantity() * (existing.getUnitCost() != null ? existing.getUnitCost() : 0.0));
            return toSnapshotResponse(snapshotRepository.save(existing));
        }
        long now = System.currentTimeMillis();
        InventorySnapshotEntity entity = new InventorySnapshotEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setProductId(product.getId());
        entity.setProductCode(product.getCode());
        entity.setProductName(product.getName());
        entity.setWarehouseId(request.warehouseId());
        entity.setQuantity(product.getStock() != null ? product.getStock() : 0.0);
        entity.setUnitCost(product.getPurchasePrice());
        entity.setTotalValue(entity.getQuantity() * (entity.getUnitCost() != null ? entity.getUnitCost() : 0.0));
        entity.setSnapshotDate(request.snapshotDate());
        entity.setCreatedAt(now);
        return toSnapshotResponse(snapshotRepository.save(entity));
    }

    public Page<V2InventoryDtos.MonthlyStatsResponse> listMonthlyStats(Integer year, Integer month, int page, int size) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Pageable pageable = toPageable(page, size);
        if (year != null && month != null) {
            return monthlyStatsRepository.findAllByOwnerUserIdAndYearAndMonthOrderByProductNameAsc(ownerUserId, year, month, pageable)
                .map(this::toMonthlyStatsResponse);
        }
        return Page.empty(pageable);
    }

    private Pageable toPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 50 : Math.min(size, 200);
        return PageRequest.of(safePage, safeSize);
    }

    private V2InventoryDtos.LedgerEntryResponse toLedgerResponse(InventoryLedgerEntity entity) {
        return new V2InventoryDtos.LedgerEntryResponse(
            entity.getId(), entity.getProductId(), entity.getProductCode(), entity.getProductName(),
            entity.getWarehouseId(), entity.getQuantityBefore(), entity.getQuantityChange(),
            entity.getQuantityAfter(), entity.getUnitCost(), entity.getSourceType(),
            entity.getSourceId(), entity.getSourceNo(), entity.getNotes(), entity.getCreatedAt()
        );
    }

    private V2InventoryDtos.SnapshotResponse toSnapshotResponse(InventorySnapshotEntity entity) {
        return new V2InventoryDtos.SnapshotResponse(
            entity.getId(), entity.getProductId(), entity.getProductCode(), entity.getProductName(),
            entity.getWarehouseId(), entity.getQuantity(), entity.getUnitCost(),
            entity.getTotalValue(), entity.getSnapshotDate(), entity.getCreatedAt()
        );
    }

    private V2InventoryDtos.MonthlyStatsResponse toMonthlyStatsResponse(InventoryMonthlyStatsEntity entity) {
        return new V2InventoryDtos.MonthlyStatsResponse(
            entity.getId(), entity.getProductId(), entity.getProductCode(), entity.getProductName(),
            entity.getWarehouseId(), entity.getMonth(), entity.getYear(),
            entity.getQuantityIn(), entity.getQuantityOut(), entity.getQuantityAdjust(),
            entity.getQuantityBegin(), entity.getQuantityEnd(),
            entity.getTotalCostIn(), entity.getTotalCostOut(),
            entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
