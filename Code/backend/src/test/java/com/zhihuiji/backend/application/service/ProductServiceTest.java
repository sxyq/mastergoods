package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.annotation.Transactional;

class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService(productRepository, inventoryAdjustmentRepository, currentOwnerService, new IdGenerator());
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void listUsesAllProductsWhenKeywordBlankAndSearchWhenPresent() {
        ProductEntity product = product("P1", 10.0);
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(1L, null, null, null, null))
            .thenReturn(List.of(product));
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(1L, "P1", null, null, null))
            .thenReturn(List.of(product));

        assertEquals(1, productService.list(" ").size());
        assertEquals(1, productService.list(" P1 ").size());

        verify(productRepository).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(1L, null, null, null, null);
        verify(productRepository).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(1L, "P1", null, null, null);
    }

    @Test
    void createRejectsDuplicateCodeAndInitializesSyncFields() {
        ProductEntity existing = product("P1", 10.0);
        when(productRepository.findByOwnerUserIdAndCode(1L, "P1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> productService.create(product("P1", 10.0)));

        when(productRepository.findByOwnerUserIdAndCode(1L, "P2")).thenReturn(Optional.empty());
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity created = productService.create(product("P2", 5.0));

        assertEquals(0, created.getSyncStatus());
        assertEquals(1L, created.getSyncVersion());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
    }

    @Test
    void updateCopiesMutableFieldsAndBumpsSyncVersion() {
        ProductEntity target = product("P1", 10.0);
        target.setSyncVersion(3L);
        ProductEntity payload = product("P1", 22.0);
        payload.setName("新商品");
        payload.setCategory("新分类");
        payload.setUnit("箱");
        payload.setStatus(0);
        when(productRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(target));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity updated = productService.update(1L, payload);

        assertEquals("新商品", updated.getName());
        assertEquals("新分类", updated.getCategory());
        assertEquals("箱", updated.getUnit());
        assertEquals(22.0, updated.getSalePrice());
        assertEquals(0, updated.getStatus());
        assertEquals(4L, updated.getSyncVersion());
        assertEquals(0, updated.getSyncStatus());
    }

    @Test
    void updateRecordsInventoryDifferenceInsteadOfWritingStockSilently() {
        ProductEntity target = product("P1", 10.0);
        ProductEntity payload = product("P1", 22.0);
        payload.setStock(12.5);
        target.setSyncVersion(3L);
        when(productRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(target));
        when(productRepository.findByIdForUpdate(1L, 1L)).thenReturn(Optional.of(target));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryAdjustmentRepository.save(any(InventoryAdjustmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity updated = productService.update(1L, payload);

        assertEquals(12.5, updated.getStock());
        ArgumentCaptor<InventoryAdjustmentEntity> captor = ArgumentCaptor.forClass(InventoryAdjustmentEntity.class);
        verify(inventoryAdjustmentRepository).save(captor.capture());
        assertEquals(2.5, captor.getValue().getQuantity());
        assertEquals(1, captor.getValue().getFlowType());
    }

    @Test
    void updateRejectsNonFiniteStock() {
        ProductEntity target = product("P1", 10.0);
        ProductEntity payload = product("P1", 22.0);
        payload.setStock(Double.NaN);
        when(productRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(target));

        assertThrows(IllegalArgumentException.class, () -> productService.update(1L, payload));
    }

    @Test
    void adjustStockRecordsInflowAndOutflowAndRejectsInvalidDelta() {
        ProductEntity target = product("P1", 10.0);
        target.setStock(10.1);
        target.setSyncVersion(1L);
        when(productRepository.findByIdForUpdate(1L, 1L)).thenReturn(Optional.of(target));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryAdjustmentRepository.save(any(InventoryAdjustmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity afterInflow = productService.adjustStock(1L, new BigDecimal("0.2"), "补货", "admin");
        assertEquals(0, BigDecimal.valueOf(afterInflow.getStock()).compareTo(new BigDecimal("10.3")));

        ProductEntity afterOutflow = productService.adjustStock(1L, new BigDecimal("-0.3"), "盘点扣减", "admin");
        assertEquals(0, BigDecimal.valueOf(afterOutflow.getStock()).compareTo(BigDecimal.TEN));

        ArgumentCaptor<InventoryAdjustmentEntity> captor = ArgumentCaptor.forClass(InventoryAdjustmentEntity.class);
        verify(inventoryAdjustmentRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(1, captor.getAllValues().get(0).getFlowType());
        assertEquals(0, captor.getAllValues().get(1).getFlowType());
        assertEquals("admin", captor.getAllValues().get(0).getOperatorName());
        assertEquals(1L, captor.getAllValues().get(0).getOwnerUserId());
        assertEquals(0, BigDecimal.valueOf(captor.getAllValues().get(0).getQuantity()).compareTo(new BigDecimal("0.2")));
        assertEquals(0, BigDecimal.valueOf(captor.getAllValues().get(1).getQuantity()).compareTo(new BigDecimal("0.3")));

        assertThrows(IllegalArgumentException.class, () -> productService.adjustStock(1L, BigDecimal.ZERO, "无效", "admin"));
        assertThrows(IllegalArgumentException.class, () -> productService.adjustStock(1L, new BigDecimal("-100.0"), "超扣", "admin"));
    }

    @Test
    void getAndFindByCodeHandleMissingValues() {
        when(productRepository.findByIdAndOwnerUserId(404L, 1L)).thenReturn(Optional.empty());
        when(productRepository.findByOwnerUserIdAndCode(1L, "P1")).thenReturn(Optional.of(product("P1", 10.0)));

        assertThrows(IllegalArgumentException.class, () -> productService.get(404L));
        assertEquals(null, productService.findByCode(null));
        assertEquals(null, productService.findByCode(" "));
        assertEquals("P1", productService.findByCode(" P1 ").getCode());
    }

    @Test
    void mutableProductAndCustomerOperationsStayTransactional() throws Exception {
        assertTransactional(ProductService.class, "create", ProductEntity.class);
        assertTransactional(ProductService.class, "update", Long.class, ProductEntity.class);
        assertTransactional(ProductService.class, "delete", Long.class);
        assertTransactional(ProductService.class, "adjustStock", Long.class, BigDecimal.class, String.class, String.class);
        assertTransactional(CustomerService.class, "create", com.zhihuiji.backend.domain.entity.CustomerEntity.class);
        assertTransactional(CustomerService.class, "update", Long.class, com.zhihuiji.backend.domain.entity.CustomerEntity.class);
        assertTransactional(CustomerService.class, "delete", Long.class);
    }

    private static ProductEntity product(String code, Double salePrice) {
        ProductEntity entity = new ProductEntity();
        setEntityId(entity, 1L);
        entity.setCode(code);
        entity.setName("商品" + code);
        entity.setCategory("默认");
        entity.setUnit("件");
        entity.setSalePrice(salePrice);
        entity.setPurchasePrice(5.0);
        entity.setStock(10.0);
        entity.setSafeStock(2.0);
        entity.setStatus(1);
        entity.setOwnerUserId(1L);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static void setEntityId(ProductEntity entity, Long id) {
        try {
            java.lang.reflect.Field field = ProductEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static void assertTransactional(Class<?> type, String methodName, Class<?>... parameterTypes)
        throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, () -> type.getSimpleName() + "::" + methodName + " should be transactional");
    }
}
