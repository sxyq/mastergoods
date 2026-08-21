package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.product.ProductCategoryEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import com.zhihuiji.backend.domain.entity.SyncChangeLogEntity;
import com.zhihuiji.backend.domain.entity.SyncTombstoneEntity;
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
import com.zhihuiji.backend.infrastructure.repository.product.ProductCategoryRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductPriceLevelRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductSupplierRelationRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductUnitRepository;
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
import com.zhihuiji.backend.infrastructure.repository.SyncChangeLogRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncOperationLogRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncTombstoneRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.data.domain.Pageable;

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
    @Mock private SyncOperationLogRepository syncOperationLogRepository;
    @Mock private SyncTombstoneRepository syncTombstoneRepository;
    @Mock private SyncChangeLogRepository syncChangeLogRepository;
    @Mock private PlatformTransactionManager transactionManager;

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
            currentOwnerService,
            syncOperationLogRepository,
            syncTombstoneRepository,
            syncChangeLogRepository,
            transactionManager
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(currentOwnerService.requireCurrentStoreId()).thenReturn(1L);
        when(currentOwnerService.findCurrentStoreId()).thenReturn(Optional.of(1L));
        when(syncCursorRepository.findByOwnerUserIdAndClientId(1L, "device-a")).thenReturn(Optional.empty());
        when(syncCursorRepository.save(org.mockito.ArgumentMatchers.any(SyncCursorEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(syncOperationLogRepository.reserveOperation(
            anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong()
        )).thenReturn(1);
        stubEmptyPullRepositories();
    }

    @Test
    void pullUsesStableCompositeCursorForSameTimestampPageBoundary() {
        when(productCategoryRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of(
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
        when(productCategoryRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of(
            productCategory(1L, "分类A", 100L)
        ));

        V2SyncService.PullResult result = v2SyncService.pull("device-a", null, 10);

        assertEquals("100|product_category|1", result.nextCursor());
        verify(syncCursorRepository, never()).save(org.mockito.ArgumentMatchers.any(SyncCursorEntity.class));
    }

    @Test
    void acknowledgeCursorPersistsPullCursorAndSubsequentPullStartsFromAckedCursor() {
        when(productCategoryRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of(
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

    @Test
    void uploadSkipsDuplicateOperationId() {
        V2SyncService.SyncChange change = new V2SyncService.SyncChange(
            "op-dup-001", "product_category", "999", "create",
            "{\"name\":\"TestCat\",\"status\":1,\"sort_order\":0}", 100L, null
        );

        // The atomic reservation is the idempotency check.
        when(productCategoryRepository.findByIdAndOwnerUserId(999L, 1L))
            .thenReturn(Optional.empty());

        V2SyncService.UploadResult firstResult = v2SyncService.upload("device-a", List.of(change), "0");

        assertEquals(1, firstResult.acceptedCount());
        assertEquals(0, firstResult.failedCount());
        assertTrue(firstResult.acceptedOperationIds().contains("op-dup-001"));
        verify(syncOperationLogRepository).reserveOperation(
            eq(1L), eq(1L), eq("op-dup-001"), eq("product_category"), eq("999"), eq("create"), anyLong()
        );
        verify(productCategoryRepository, times(1)).save(any(ProductCategoryEntity.class));

        // Second upload: the database reservation reports a conflict → skip (idempotent)
        when(syncOperationLogRepository.reserveOperation(
            anyLong(), anyLong(), eq("op-dup-001"), anyString(), anyString(), anyString(), anyLong()
        )).thenReturn(0);

        V2SyncService.UploadResult secondResult = v2SyncService.upload("device-a", List.of(change), "0");

        assertEquals(1, secondResult.acceptedCount());
        assertEquals(0, secondResult.failedCount());
        assertEquals("duplicate", secondResult.operationResults().get(0).status());
        // productCategoryRepository.save still only called once (from first upload)
        verify(productCategoryRepository, times(1)).save(any(ProductCategoryEntity.class));
    }

    @Test
    void pullUsesPersistentStoreScopedChangeLogAndSequenceCursor() {
        SyncChangeLogEntity row = new SyncChangeLogEntity();
        row.setSequenceNumber(7L);
        row.setOwnerUserId(1L);
        row.setStoreId(1L);
        row.setEntityType("product");
        row.setEntityId("44");
        row.setOperation("upsert");
        row.setOperationId("op-log-44");
        row.setPayload("{\"id\":44,\"name\":\"真实商品\"}");
        row.setChangedAt(200L);
        row.setSyncVersion(3L);
        when(syncChangeLogRepository
            .findByOwnerUserIdAndStoreIdAndChangedAtGreaterThanEqualOrderByChangedAtAscSequenceNumberAsc(
                eq(1L), eq(1L), eq(0L), org.mockito.ArgumentMatchers.any(Pageable.class)))
            .thenReturn(List.of(row));

        V2SyncService.PullResult result = v2SyncService.pull("device-a", "0", 10);

        assertEquals(1, result.changes().size());
        assertEquals("op-log-44", result.changes().get(0).operationId());
        assertEquals("seq:7", result.nextCursor());

        when(syncChangeLogRepository
            .findByOwnerUserIdAndStoreIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                eq(1L), eq(1L), eq(7L), org.mockito.ArgumentMatchers.any(Pageable.class)))
            .thenReturn(List.of());
        V2SyncService.PullResult afterAck = v2SyncService.pull("device-a", "seq:7", 10);
        assertTrue(afterAck.changes().isEmpty());
        assertEquals("seq:7", afterAck.nextCursor());
    }

    @Test
    void pullUsesAcknowledgedLegacyCursorForPersistedLogsAndReturnsMatchingSequence() {
        SyncCursorEntity acknowledgedCursor = new SyncCursorEntity();
        acknowledgedCursor.setOwnerUserId(1L);
        acknowledgedCursor.setClientId("device-a");
        acknowledgedCursor.setLastCursor("200|product|44");
        acknowledgedCursor.setUpdatedAt(200L);
        when(syncCursorRepository.findByOwnerUserIdAndClientId(1L, "device-a"))
            .thenReturn(Optional.of(acknowledgedCursor));

        SyncChangeLogEntity acknowledgedRow = new SyncChangeLogEntity();
        acknowledgedRow.setSequenceNumber(7L);
        acknowledgedRow.setOwnerUserId(1L);
        acknowledgedRow.setStoreId(1L);
        acknowledgedRow.setEntityType("product");
        acknowledgedRow.setEntityId("44");
        acknowledgedRow.setOperation("upsert");
        acknowledgedRow.setChangedAt(200L);

        SyncChangeLogEntity nextRow = new SyncChangeLogEntity();
        nextRow.setSequenceNumber(8L);
        nextRow.setOwnerUserId(1L);
        nextRow.setStoreId(1L);
        nextRow.setEntityType("product");
        nextRow.setEntityId("45");
        nextRow.setOperation("upsert");
        nextRow.setChangedAt(200L);
        when(syncChangeLogRepository
            .findByOwnerUserIdAndStoreIdAndChangedAtGreaterThanEqualOrderByChangedAtAscSequenceNumberAsc(
                eq(1L), eq(1L), eq(200L), any(Pageable.class)))
            .thenReturn(List.of(acknowledgedRow, nextRow));

        V2SyncService.PullResult result = v2SyncService.pull("device-a", null, 10);

        assertEquals("200|product|44", result.effectiveCursor());
        assertEquals(1, result.changes().size());
        assertEquals("45", result.changes().get(0).entityId());
        assertEquals("seq:8", result.nextCursor());
        verify(syncChangeLogRepository)
            .findByOwnerUserIdAndStoreIdAndChangedAtGreaterThanEqualOrderByChangedAtAscSequenceNumberAsc(
                eq(1L), eq(1L), eq(200L), any(Pageable.class));
    }

    @Test
    void uploadDeleteCreatesTombstone() {
        V2SyncService.SyncChange deleteChange = new V2SyncService.SyncChange(
            "op-del-001", "product_category", "888", "delete",
            null, 100L, null
        );

        ProductCategoryEntity existingEntity = new ProductCategoryEntity();
        existingEntity.setId(888L);
        existingEntity.setOwnerUserId(1L);
        when(productCategoryRepository.findByIdAndOwnerUserId(888L, 1L))
            .thenReturn(Optional.of(existingEntity));

        V2SyncService.UploadResult result = v2SyncService.upload("device-a", List.of(deleteChange), "0");

        assertEquals(1, result.acceptedCount());
        assertEquals(0, result.failedCount());
        verify(productCategoryRepository).delete(existingEntity);
        verify(syncTombstoneRepository).save(org.mockito.ArgumentMatchers.argThat(t ->
            t.getOwnerUserId().equals(1L) &&
            t.getEntityType().equals("product_category") &&
            t.getEntityId().equals("888")
        ));
    }

    @Test
    void uploadRejectsStaleBaseVersionWithAnExplicitConflictReason() {
        ProductEntity current = new ProductEntity();
        current.setId(44L);
        current.setOwnerUserId(1L);
        current.setSyncVersion(2L);
        when(productRepository.findByIdForUpdate(1L, 44L)).thenReturn(Optional.of(current));

        V2SyncService.UploadResult result = v2SyncService.upload("device-a", List.of(
            new V2SyncService.SyncChange(
                "op-stale-001", "product", "44", "update",
                "{\"name\":\"过期更新\"}", 100L, 1L
            )
        ), "0");

        assertEquals(0, result.acceptedCount());
        assertEquals(1, result.failedCount());
        assertEquals("op-stale-001", result.failedOperationIds().get(0));
        assertEquals("version_conflict", result.failures().get(0).code());
        verify(productRepository, never()).save(any(ProductEntity.class));
        verify(syncOperationLogRepository, never()).reserveOperation(
            anyLong(), anyLong(), eq("op-stale-001"), anyString(), anyString(), anyString(), anyLong()
        );
    }

    @Test
    void staleBaseVersionReturnsServerSnapshotAndConflictingFields() {
        ProductEntity current = new ProductEntity();
        current.setId(44L);
        current.setOwnerUserId(1L);
        current.setCode("P-44");
        current.setName("服务器商品");
        current.setSyncVersion(2L);
        when(productRepository.findByIdForUpdate(1L, 44L)).thenReturn(Optional.of(current));

        V2SyncService.UploadResult result = v2SyncService.upload("device-a", List.of(
            new V2SyncService.SyncChange(
                "op-stale-details", "product", "44", "update",
                "{\"name\":\"本地商品\",\"sale_price\":99}", 100L, 1L
            )
        ), "0");

        V2SyncService.SyncOperationResult operation = result.operationResults().get(0);
        assertEquals("conflict", operation.status());
        assertEquals(2L, operation.serverVersion());
        assertTrue(operation.conflictFields().contains("name"));
        assertTrue(operation.conflictFields().contains("sale_price"));
        assertTrue(operation.serverPayload().contains("服务器商品"));
    }

    @Test
    void uploadRejectsPaymentDirectSyncUntilItUsesServerCommand() {
        V2SyncService.UploadResult result = v2SyncService.upload("device-a", List.of(
            new V2SyncService.SyncChange(
                "op-payment-001", "payment", "77", "create", "{}", 100L, null
            )
        ), "0");

        assertEquals(0, result.acceptedCount());
        assertEquals(1, result.failedCount());
        assertEquals("server_command_required", result.failures().get(0).code());
        verify(currentOwnerService, never()).requirePermissions("sales:write", "finance:write");
        verify(syncOperationLogRepository, never()).reserveOperation(
            anyLong(), anyLong(), eq("op-payment-001"), anyString(), anyString(), anyString(), anyLong()
        );
    }

    private void stubEmptyPullRepositories() {
        when(productUnitRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(productPriceLevelRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(productSupplierRelationRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(partnerGroupRepository.findChangedByOwnerUserIdAndPartnerType(eq(1L), eq("customer"), anyLong())).thenReturn(List.of());
        when(partnerGroupRepository.findChangedByOwnerUserIdAndPartnerType(eq(1L), eq("supplier"), anyLong())).thenReturn(List.of());
        when(partnerContactRepository.findChangedByOwnerUserIdAndPartnerType(eq(1L), eq("customer"), anyLong())).thenReturn(List.of());
        when(partnerContactRepository.findChangedByOwnerUserIdAndPartnerType(eq(1L), eq("supplier"), anyLong())).thenReturn(List.of());
        when(customerRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(supplierRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(productRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(saleOrderRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(saleOrderItemRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(paymentRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(purchaseOrderRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(purchaseOrderItemRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(payOrderRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(financeRecordRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(accountRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(accountTransferRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(billFundLinkRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(inventoryAdjustmentRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(inventoryLedgerRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(inventorySnapshotRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(inventoryMonthlyStatsRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(salesReturnRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(salesReturnItemRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(purchaseReceiptRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(purchaseReceiptItemRepository.findChangedByOwnerUserId(eq(1L), anyLong())).thenReturn(List.of());
        when(syncTombstoneRepository.findChangedByOwnerUserId(eq(1L), eq(1L), anyLong())).thenReturn(List.of());
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
