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

    public List<V2InventoryDtos.LedgerEntryResponse> listLedger(Long productId, Long startAt, Long endAt) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (productId != null) {
            return ledgerRepository.findAllByOwnerUserIdAndProductIdOrderByCreatedAtDesc(ownerUserId, productId).stream()
                .map(this::toLedgerResponse).toList();
        }
        if (startAt != null && endAt != null) {
            return ledgerRepository.findAllByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(ownerUserId, startAt, endAt).stream()
                .map(this::toLedgerResponse).toList();
        }
        return ledgerRepository.findAllByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(ownerUserId, 0L, System.currentTimeMillis()).stream()
            .map(this::toLedgerResponse).toList();
    }

    public List<V2InventoryDtos.LedgerEntryResponse> listLedgerBySource(String sourceType, Long sourceId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return ledgerRepository.findAllByOwnerUserIdAndSourceTypeAndSourceId(ownerUserId, sourceType, sourceId).stream()
            .map(this::toLedgerResponse).toList();
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

    public List<V2InventoryDtos.SnapshotResponse> listSnapshots(Long snapshotDate, Long startDate, Long endDate) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (snapshotDate != null) {
            return snapshotRepository.findAllByOwnerUserIdAndSnapshotDateOrderByProductNameAsc(ownerUserId, snapshotDate).stream()
                .map(this::toSnapshotResponse).toList();
        }
        if (startDate != null && endDate != null) {
            return snapshotRepository.findAllByOwnerUserIdAndSnapshotDateBetweenOrderBySnapshotDateAscProductNameAsc(ownerUserId, startDate, endDate).stream()
                .map(this::toSnapshotResponse).toList();
        }
        return List.of();
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

    public List<V2InventoryDtos.MonthlyStatsResponse> listMonthlyStats(Integer year, Integer month) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (year != null && month != null) {
            return monthlyStatsRepository.findAllByOwnerUserIdAndYearAndMonthOrderByProductNameAsc(ownerUserId, year, month).stream()
                .map(this::toMonthlyStatsResponse).toList();
        }
        return List.of();
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
