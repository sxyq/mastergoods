package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.ProductSupplierRelationEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductSupplierRelationRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2ProductSupplierRelationServiceTest {
    @Mock
    private ProductSupplierRelationRepository productSupplierRelationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2ProductSupplierRelationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2ProductSupplierRelationService(
            productSupplierRelationRepository,
            productRepository,
            supplierRepository,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createRejectsDuplicateSupplierRelation() {
        when(productRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(product(1L)));
        when(supplierRepository.findByIdAndOwnerUserId(41L, 1L)).thenReturn(Optional.of(supplier(41L, "供应商A")));
        when(productSupplierRelationRepository.existsByOwnerUserIdAndProductIdAndSupplierId(1L, 1L, 41L)).thenReturn(true);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.create(new V2ProductDtos.ProductSupplierRelationWriteRequest(1L, 41L, true, 0, 1.2, "长期合作"))
        );
    }

    @Test
    void replaceForProductRejectsMultipleDefaultSuppliers() {
        when(productRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(product(1L)));
        when(supplierRepository.findAllByOwnerUserIdAndIdIn(eq(1L), any()))
            .thenReturn(List.of(supplier(41L, "供应商A"), supplier(42L, "供应商B")));

        assertThrows(
            IllegalArgumentException.class,
            () -> service.replaceForProduct(
                1L,
                List.of(
                    new V2ProductDtos.ProductSupplierRelationWriteRequest(1L, 41L, true, 0, 1.2, null),
                    new V2ProductDtos.ProductSupplierRelationWriteRequest(1L, 42L, true, 1, 1.3, null)
                )
            )
        );

        ArgumentCaptor<java.util.Collection<Long>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(supplierRepository).findAllByOwnerUserIdAndIdIn(eq(1L), captor.capture());
        assertEquals(List.of(41L, 42L), List.copyOf(captor.getValue()));
    }

    @Test
    void createDefaultRelationClearsExistingDefault() {
        ProductSupplierRelationEntity existingDefault = relation(11L, 1L, 41L, true);
        when(productRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(product(1L)));
        when(supplierRepository.findByIdAndOwnerUserId(42L, 1L)).thenReturn(Optional.of(supplier(42L, "供应商B")));
        when(productSupplierRelationRepository.existsByOwnerUserIdAndProductIdAndSupplierId(1L, 1L, 42L)).thenReturn(false);
        when(productSupplierRelationRepository.findAllByOwnerUserIdAndProductIdOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(1L, 1L))
            .thenReturn(List.of(existingDefault));
        when(productSupplierRelationRepository.save(any(ProductSupplierRelationEntity.class))).thenAnswer(invocation -> {
            ProductSupplierRelationEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(12L);
            }
            return entity;
        });

        V2ProductDtos.ProductSupplierRelationResponse response = service.create(
            new V2ProductDtos.ProductSupplierRelationWriteRequest(1L, 42L, true, 0, 1.2, "长期合作")
        );

        assertEquals(12L, response.id());
        assertEquals(42L, response.supplierId());
        assertEquals("供应商B", response.supplierName());
        ArgumentCaptor<ProductSupplierRelationEntity> captor = ArgumentCaptor.forClass(ProductSupplierRelationEntity.class);
        verify(productSupplierRelationRepository, times(2)).save(captor.capture());
        List<ProductSupplierRelationEntity> savedEntities = captor.getAllValues();
        assertEquals(false, savedEntities.get(0).getIsDefault());
        assertEquals(true, savedEntities.get(1).getIsDefault());
    }

    private static ProductEntity product(Long id) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode("P001");
        entity.setName("矿泉水");
        entity.setStatus(1);
        entity.setSalePrice(2.5);
        entity.setPurchasePrice(1.5);
        entity.setStock(10.0);
        entity.setSafeStock(2.0);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static SupplierEntity supplier(Long id, String name) {
        SupplierEntity entity = new SupplierEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setPhone("13600000000");
        entity.setStatus(1);
        entity.setBalance(0.0);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static ProductSupplierRelationEntity relation(Long id, Long productId, Long supplierId, boolean isDefault) {
        ProductSupplierRelationEntity entity = new ProductSupplierRelationEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setProductId(productId);
        entity.setSupplierId(supplierId);
        entity.setIsDefault(isDefault);
        entity.setPurchasePriority(0);
        entity.setLastPurchasePrice(1.1);
        entity.setNotes("历史默认");
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
