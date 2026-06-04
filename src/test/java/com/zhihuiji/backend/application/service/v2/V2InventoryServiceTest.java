package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.inventory.V2InventoryDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.InventoryLedgerEntity;
import com.zhihuiji.backend.domain.entity.InventorySnapshotEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryLedgerRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryMonthlyStatsRepository;
import com.zhihuiji.backend.infrastructure.repository.InventorySnapshotRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2InventoryServiceTest {
    @Mock
    private InventoryLedgerRepository ledgerRepository;
    @Mock
    private InventorySnapshotRepository snapshotRepository;
    @Mock
    private InventoryMonthlyStatsRepository monthlyStatsRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2InventoryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2InventoryService(
            ledgerRepository,
            snapshotRepository,
            monthlyStatsRepository,
            productRepository,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createLedgerEntryUpdatesProductStock() {
        ProductEntity product = product(6L, "P001", "矿泉水", 8.0, 1.5);
        when(productRepository.findByIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any(InventoryLedgerEntity.class))).thenAnswer(invocation -> {
            InventoryLedgerEntity entity = invocation.getArgument(0);
            entity.setId(16L);
            return entity;
        });

        V2InventoryDtos.LedgerEntryResponse response = service.createLedgerEntry(
            new V2InventoryDtos.LedgerEntryCreateRequest(6L, "manual_adjust", 1L, "ADJ-1", 5.0, 1.2, null, "盘盈")
        );

        assertEquals(16L, response.id());
        assertEquals(8.0, response.quantityBefore());
        assertEquals(13.0, response.quantityAfter());
        assertEquals(13.0, product.getStock());
    }

    @Test
    void createSnapshotUpdatesExistingRecordInsteadOfCreatingDuplicate() {
        ProductEntity product = product(6L, "P001", "矿泉水", 12.0, 1.5);
        InventorySnapshotEntity existing = new InventorySnapshotEntity();
        existing.setId(7L);
        existing.setOwnerUserId(1L);
        existing.setProductId(6L);
        existing.setProductCode("P001");
        existing.setProductName("矿泉水");
        existing.setQuantity(4.0);
        existing.setUnitCost(1.0);
        existing.setTotalValue(4.0);
        existing.setSnapshotDate(20240601L);
        existing.setCreatedAt(1L);
        when(productRepository.findByIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.of(product));
        when(snapshotRepository.findByOwnerUserIdAndProductIdAndSnapshotDate(1L, 6L, 20240601L))
            .thenReturn(Optional.of(existing));
        when(snapshotRepository.save(any(InventorySnapshotEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        V2InventoryDtos.SnapshotResponse response = service.createSnapshot(
            new V2InventoryDtos.SnapshotCreateRequest(6L, 20240601L, null)
        );

        assertEquals(7L, response.id());
        assertEquals(12.0, response.quantity());
        assertEquals(18.0, response.totalValue());
    }

    @Test
    void listLedgerBySourceReturnsMatchingEntries() {
        InventoryLedgerEntity entry = new InventoryLedgerEntity();
        entry.setId(3L);
        entry.setOwnerUserId(1L);
        entry.setProductId(6L);
        entry.setProductCode("P001");
        entry.setProductName("矿泉水");
        entry.setQuantityBefore(8.0);
        entry.setQuantityChange(2.0);
        entry.setQuantityAfter(10.0);
        entry.setUnitCost(1.5);
        entry.setSourceType("sales_return");
        entry.setSourceId(7L);
        entry.setSourceNo("SR-001");
        entry.setNotes("退货回库");
        entry.setCreatedAt(1L);
        when(ledgerRepository.findAllByOwnerUserIdAndSourceTypeAndSourceId(1L, "sales_return", 7L))
            .thenReturn(java.util.List.of(entry));

        java.util.List<V2InventoryDtos.LedgerEntryResponse> result = service.listLedgerBySource("sales_return", 7L);

        assertEquals(1, result.size());
        assertEquals("sales_return", result.get(0).sourceType());
        assertEquals(7L, result.get(0).sourceId());
        assertEquals("SR-001", result.get(0).sourceNo());
    }

    private static ProductEntity product(Long id, String code, String name, Double stock, Double purchasePrice) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName(name);
        entity.setStock(stock);
        entity.setPurchasePrice(purchasePrice);
        return entity;
    }
}
