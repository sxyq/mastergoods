package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductCategoryEntity;
import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.AccountTransferRepository;
import com.zhihuiji.backend.infrastructure.repository.BillFundLinkRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryLedgerRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryMonthlyStatsRepository;
import com.zhihuiji.backend.infrastructure.repository.InventorySnapshotRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerContactRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerGroupRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductCategoryRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductPriceLevelRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductSupplierRelationRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductUnitRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncCursorRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2SyncServiceTest {
    @Mock private SyncCursorRepository syncCursorRepository;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private ProductUnitRepository productUnitRepository;
    @Mock private ProductPriceLevelRepository productPriceLevelRepository;
    @Mock private ProductSupplierRelationRepository productSupplierRelationRepository;
    @Mock private PartnerGroupRepository partnerGroupRepository;
    @Mock private PartnerContactRepository partnerContactRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SaleOrderRepository saleOrderRepository;
    @Mock private SaleOrderItemRepository saleOrderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock private PayOrderRepository payOrderRepository;
    @Mock private FinanceRecordRepository financeRecordRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountTransferRepository accountTransferRepository;
    @Mock private BillFundLinkRepository billFundLinkRepository;
    @Mock private InventoryAdjustmentRepository inventoryAdjustmentRepository;
    @Mock private InventoryLedgerRepository inventoryLedgerRepository;
    @Mock private InventorySnapshotRepository inventorySnapshotRepository;
    @Mock private InventoryMonthlyStatsRepository inventoryMonthlyStatsRepository;
    @Mock private SalesReturnRepository salesReturnRepository;
    @Mock private SalesReturnItemRepository salesReturnItemRepository;
    @Mock private PurchaseReceiptRepository purchaseReceiptRepository;
    @Mock private PurchaseReceiptItemRepository purchaseReceiptItemRepository;
    @Mock private CurrentOwnerService currentOwnerService;

    private V2SyncService v2SyncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        v2SyncService = new V2SyncService(
            syncCursorRepository,
            productCategoryRepository,
            productUnitRepository,
            productPriceLevelRepository,
            productSupplierRelationRepository,
            partnerGroupRepository,
            partnerContactRepository,
            customerRepository,
            supplierRepository,
            productRepository,
            saleOrderRepository,
            saleOrderItemRepository,
            paymentRepository,
            purchaseOrderRepository,
            purchaseOrderItemRepository,
            payOrderRepository,
            financeRecordRepository,
            accountRepository,
            accountTransferRepository,
            billFundLinkRepository,
            inventoryAdjustmentRepository,
            inventoryLedgerRepository,
            inventorySnapshotRepository,
            inventoryMonthlyStatsRepository,
            salesReturnRepository,
            salesReturnItemRepository,
            purchaseReceiptRepository,
            purchaseReceiptItemRepository,
            new ObjectMapper(),
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(syncCursorRepository.findByOwnerUserIdAndClientId(1L, "device-a")).thenReturn(Optional.empty());
        when(syncCursorRepository.save(org.mockito.ArgumentMatchers.any(SyncCursorEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        stubEmptyPullRepositories();
    }

    @Test
    void pullUsesStableCompositeCursorForSameTimestampPageBoundary() {
        when(productCategoryRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(1L)).thenReturn(List.of(
            productCategory(1L, "分类A", 100L),
            productCategory(2L, "分类B", 100L),
            productCategory(3L, "分类C", 100L)
        ));

        V2SyncService.PullResult firstPage = v2SyncService.pull("device-a", "0", 2);
        V2SyncService.PullResult secondPage = v2SyncService.pull("device-a", firstPage.nextCursor(), 2);

        assertEquals(2, firstPage.changes().size());
        assertTrue(firstPage.hasMore());
        assertEquals("100|product_category|2", firstPage.nextCursor());
        assertEquals(1, secondPage.changes().size());
        assertEquals("3", secondPage.changes().get(0).entityId());
    }

    @Test
    void pullDoesNotPersistCursorBeforeClientAck() {
        when(productCategoryRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(1L)).thenReturn(List.of(
            productCategory(1L, "分类A", 100L)
        ));

        V2SyncService.PullResult result = v2SyncService.pull("device-a", null, 10);

        assertEquals("100|product_category|1", result.nextCursor());
        verify(syncCursorRepository, never()).save(org.mockito.ArgumentMatchers.any(SyncCursorEntity.class));
    }

    @Test
    void acknowledgeCursorPersistsPullCursorAndSubsequentPullStartsFromAckedCursor() {
        when(productCategoryRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(1L)).thenReturn(List.of(
            productCategory(1L, "分类A", 100L),
            productCategory(2L, "分类B", 100L),
            productCategory(3L, "分类C", 100L)
        ));

        V2SyncService.PullResult firstPage = v2SyncService.pull("device-a", "0", 1);
        assertEquals("100|product_category|1", firstPage.nextCursor());
        verify(syncCursorRepository, never()).save(org.mockito.ArgumentMatchers.any(SyncCursorEntity.class));

        V2SyncService.CursorStatus acknowledged = v2SyncService.acknowledgeCursor("device-a", firstPage.nextCursor());
        assertEquals("device-a", acknowledged.clientId());
        assertEquals(firstPage.nextCursor(), acknowledged.lastCursor());
        assertNotNull(acknowledged.updatedAt());

        verify(syncCursorRepository).save(org.mockito.ArgumentMatchers.argThat(cursor ->
            cursor != null
                && Long.valueOf(1L).equals(cursor.getOwnerUserId())
                && "device-a".equals(cursor.getClientId())
                && firstPage.nextCursor().equals(cursor.getLastCursor())
        ));

        SyncCursorEntity persistedCursor = new SyncCursorEntity();
        persistedCursor.setOwnerUserId(1L);
        persistedCursor.setClientId("device-a");
        persistedCursor.setLastCursor(firstPage.nextCursor());
        persistedCursor.setUpdatedAt(acknowledged.updatedAt());
        when(syncCursorRepository.findByOwnerUserIdAndClientId(1L, "device-a")).thenReturn(Optional.of(persistedCursor));

        V2SyncService.PullResult secondPage = v2SyncService.pull("device-a", null, 10);

        assertEquals(firstPage.nextCursor(), secondPage.effectiveCursor());
        assertEquals(2, secondPage.changes().size());
        assertEquals("2", secondPage.changes().get(0).entityId());
        assertEquals("3", secondPage.changes().get(1).entityId());
    }

    private void stubEmptyPullRepositories() {
        when(productUnitRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(1L)).thenReturn(List.of());
        when(productPriceLevelRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(1L)).thenReturn(List.of());
        when(productSupplierRelationRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(1L)).thenReturn(List.of());
        when(partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(1L, "customer")).thenReturn(List.of());
        when(partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(1L, "supplier")).thenReturn(List.of());
        when(partnerContactRepository.findAllByOwnerUserIdAndPartnerTypeOrderByUpdatedAtAscIdAsc(1L, "customer")).thenReturn(List.of());
        when(partnerContactRepository.findAllByOwnerUserIdAndPartnerTypeOrderByUpdatedAtAscIdAsc(1L, "supplier")).thenReturn(List.of());
        when(customerRepository.findAllByOwnerUserId(1L)).thenReturn(List.of());
        when(supplierRepository.findAllByOwnerUserId(1L)).thenReturn(List.of());
        when(productRepository.findAllByOwnerUserId(1L)).thenReturn(List.of());
        when(saleOrderRepository.findAllByOwnerUserId(1L)).thenReturn(List.of());
        when(saleOrderItemRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(paymentRepository.findAllByOwnerUserId(1L)).thenReturn(List.of());
        when(purchaseOrderRepository.findAllByOwnerUserId(1L)).thenReturn(List.of());
        when(purchaseOrderItemRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(payOrderRepository.findAllByOwnerUserId(1L)).thenReturn(List.of());
        when(financeRecordRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(1L)).thenReturn(List.of());
        when(accountRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(1L)).thenReturn(List.of());
        when(accountTransferRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(1L)).thenReturn(List.of());
        when(billFundLinkRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(1L)).thenReturn(List.of());
        when(inventoryAdjustmentRepository.findByOwnerUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(inventoryLedgerRepository.findAllByOwnerUserIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of());
        when(inventorySnapshotRepository.findAllByOwnerUserIdOrderBySnapshotDateAscIdAsc(1L)).thenReturn(List.of());
        when(inventoryMonthlyStatsRepository.findAllByOwnerUserIdOrderByYearAscMonthAscIdAsc(1L)).thenReturn(List.of());
        when(salesReturnRepository.findByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(salesReturnItemRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(purchaseReceiptRepository.findByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(purchaseReceiptItemRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
    }

    private ProductCategoryEntity productCategory(Long id, String name, long updatedAt) {
        ProductCategoryEntity entity = new ProductCategoryEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(0);
        entity.setCreatedAt(updatedAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }
}
