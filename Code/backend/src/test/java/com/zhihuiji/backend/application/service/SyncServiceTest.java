package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncCursorRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SyncServiceTest {
    @Mock
    private SyncCursorRepository syncCursorRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SaleOrderRepository saleOrderRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PayOrderRepository payOrderRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private SyncService syncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        syncService = new SyncService(
            syncCursorRepository,
            customerRepository,
            supplierRepository,
            productRepository,
            saleOrderRepository,
            purchaseOrderRepository,
            payOrderRepository,
            new ObjectMapper(),
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void healthReturnsReadyStatus() {
        SyncService.HealthResult result = syncService.health();

        assertEquals("ok", result.status());
        assertEquals("sync service ready", result.message());
        assertTrue(result.serverTime() > 0L);
    }

    @Test
    void uploadNormalizesClientIdAndPersistsMaximumCursor() {
        when(syncCursorRepository.findByOwnerUserIdAndClientId(1L, "anonymous")).thenReturn(Optional.empty());
        when(syncCursorRepository.save(org.mockito.ArgumentMatchers.any(SyncCursorEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SyncService.UploadResult result = syncService.upload(
            "   ",
            List.of(
                new SyncService.SyncChange("product", "1", "upsert", "{}", 99L),
                new SyncService.SyncChange("customer", "2", "upsert", "{}", 9999999999999L)
            ),
            "1"
        );

        assertEquals(2, result.acceptedCount());
        assertEquals(0, result.failedCount());
        assertEquals("anonymous", captureSavedCursor().getClientId());
        assertEquals(1L, captureSavedCursor().getOwnerUserId());
        assertEquals("9999999999999", result.nextCursor());
        assertEquals("9999999999999", captureSavedCursor().getLastCursor());
    }

    @Test
    void pullCollectsAndSortsAllEntityTypesWithPaging() {
        when(customerRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(customer(1L, 100L)));
        when(supplierRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(supplier(2L, 200L)));
        when(productRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(product(3L, 300L)));
        when(saleOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(saleOrder(4L, 400L)));
        when(purchaseOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(purchaseOrder(5L, 500L)));
        when(payOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(payOrder(6L, 600L)));

        SyncService.PullResult result = syncService.pull("0", 2);

        assertEquals(2, result.changes().size());
        assertTrue(result.hasMore());
        assertEquals("200", result.nextCursor());
        assertEquals("customer", result.changes().get(0).entityType());
        assertEquals("supplier", result.changes().get(1).entityType());
    }

    @Test
    void pullIncludesPayOrderAccountIdInPayload() throws Exception {
        when(customerRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(supplierRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(productRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(saleOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(purchaseOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(payOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(payOrder(6L, 600L)));

        SyncService.PullResult result = syncService.pull("0", 10);

        JsonNode payload = new ObjectMapper().readTree(result.changes().get(0).payload());
        assertEquals(8L, payload.get("account_id").asLong());
    }

    @Test
    void uploadAppliesPayOrderAccountIdFromPayload() {
        PayOrderEntity existing = payOrder(6L, 100L);
        existing.setAccountId(null);
        when(payOrderRepository.findByIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.of(existing));
        when(payOrderRepository.save(any(PayOrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncCursorRepository.findByOwnerUserIdAndClientId(1L, "device-a")).thenReturn(Optional.empty());
        when(syncCursorRepository.save(any(SyncCursorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncService.upload(
            "device-a",
            List.of(new SyncService.SyncChange(
                "pay_order",
                "6",
                "upsert",
                "{\"order_no\":\"PAY-6\",\"supplier_id\":2,\"supplier_name\":\"供应商6\",\"amount\":10.0,\"method\":1,\"account_id\":8,\"status\":1,\"created_at\":100,\"updated_at\":100}",
                100L
            )),
            "0"
        );

        assertEquals(8L, existing.getAccountId());
    }

    @Test
    void pullFallsBackToCreatedAtAndClampsInvalidLimit() {
        CustomerEntity customer = customer(7L, 100L);
        customer.setUpdatedAt(null);
        when(customerRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of(customer));
        when(supplierRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(productRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(saleOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(purchaseOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());
        when(payOrderRepository.findChangedByOwnerUserId(1L, 0L)).thenReturn(List.of());

        SyncService.PullResult result = syncService.pull("bad-cursor", 0);

        assertEquals(1, result.changes().size());
        assertEquals("customer", result.changes().get(0).entityType());
        assertFalse(result.hasMore());
        assertEquals("100", result.nextCursor());
    }

    private SyncCursorEntity captureSavedCursor() {
        ArgumentCaptor<SyncCursorEntity> captor = ArgumentCaptor.forClass(SyncCursorEntity.class);
        org.mockito.Mockito.verify(syncCursorRepository).save(captor.capture());
        return captor.getValue();
    }

    private static CustomerEntity customer(Long id, long updatedAt) {
        CustomerEntity entity = new CustomerEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setName("客户" + id);
        entity.setPhone("1380013800" + id);
        entity.setLevel(1);
        entity.setBalance(10.0);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(updatedAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private static SupplierEntity supplier(Long id, long updatedAt) {
        SupplierEntity entity = new SupplierEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setName("供应商" + id);
        entity.setPhone("1390013900" + id);
        entity.setBalance(20.0);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(updatedAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private static ProductEntity product(Long id, long updatedAt) {
        ProductEntity entity = new ProductEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setCode("P" + id);
        entity.setName("商品" + id);
        entity.setCategory("默认");
        entity.setUnit("件");
        entity.setSalePrice(10.0);
        entity.setPurchasePrice(5.0);
        entity.setStock(10.0);
        entity.setSafeStock(2.0);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(updatedAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private static SaleOrderEntity saleOrder(Long id, long updatedAt) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("SO-" + id);
        entity.setCustomerId(1L);
        entity.setCustomerName("客户" + id);
        entity.setSubtotalAmount(10.0);
        entity.setDiscountAmount(0.0);
        entity.setTotalAmount(10.0);
        entity.setPaidAmount(0.0);
        entity.setNotes("note");
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(updatedAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private static PurchaseOrderEntity purchaseOrder(Long id, long updatedAt) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("PO-" + id);
        entity.setSupplierName("供应商" + id);
        entity.setTotalAmount(10.0);
        entity.setNotes("note");
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(updatedAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private static PayOrderEntity payOrder(Long id, long updatedAt) {
        PayOrderEntity entity = new PayOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("PAY-" + id);
        entity.setSupplierId(2L);
        entity.setSupplierName("供应商" + id);
        entity.setAmount(10.0);
        entity.setMethod(1);
        entity.setAccountId(8L);
        entity.setStatus(1);
        entity.setCreatedAt(updatedAt);
        entity.setUpdatedAt(updatedAt);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        return entity;
    }

    private static void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
