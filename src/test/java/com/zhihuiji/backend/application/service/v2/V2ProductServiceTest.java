package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductCategoryEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import com.zhihuiji.backend.domain.entity.ProductUnitEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private V2ProductCategoryService categoryService;
    @Mock
    private V2ProductUnitService unitService;
    @Mock
    private V2ProductPriceLevelService priceLevelService;
    @Mock
    private V2ProductSupplierRelationService supplierRelationService;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2ProductService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2ProductService(
            productRepository,
            categoryService,
            unitService,
            priceLevelService,
            supplierRelationService,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void listBatchesSharedLookupsForReturnedProducts() {
        ProductEntity first = product(11L, "P-1", 101L, 201L, "[{\"levelId\":100,\"price\":12.5}]");
        ProductEntity second = product(12L, "P-2", 102L, 202L, "[{\"levelId\":101,\"price\":18.0}]");
        when(productRepository.findAllByOwnerUserIdAndFiltersOrderByUpdatedAtDesc(1L, 1, null, null)).thenReturn(List.of(first, second));
        when(categoryService.getOwnedEntityMap(anyCollection())).thenReturn(Map.of(
            101L, category(101L, "分类A"),
            102L, category(102L, "分类B")
        ));
        when(unitService.getOwnedEntityMap(anyCollection())).thenReturn(Map.of(
            201L, unit(201L, "件"),
            202L, unit(202L, "箱")
        ));
        when(priceLevelService.extractLevelIds(first.getPriceLevelValuesJson())).thenReturn(Set.of(100L));
        when(priceLevelService.extractLevelIds(second.getPriceLevelValuesJson())).thenReturn(Set.of(101L));
        when(priceLevelService.getOwnedEntityMap(anyCollection())).thenReturn(Map.of(
            100L, priceLevel(100L, "L1", "零售", 1),
            101L, priceLevel(101L, "L2", "批发", 2)
        ));
        when(priceLevelService.toValueResponses(eq(first.getPriceLevelValuesJson()), anyMap())).thenAnswer(invocation -> List.of(
            new V2ProductDtos.ProductPriceValueResponse(100L, "L1", "零售", 12.5, 1, 1)
        ));
        when(priceLevelService.toValueResponses(eq(second.getPriceLevelValuesJson()), anyMap())).thenAnswer(invocation -> List.of(
            new V2ProductDtos.ProductPriceValueResponse(101L, "L2", "批发", 18.0, 1, 2)
        ));
        when(supplierRelationService.listByProductIds(anyCollection())).thenReturn(Map.of(
            11L,
            List.of(supplierRelation(1001L, 11L, 301L, "供应商A", true)),
            12L,
            List.of(supplierRelation(1002L, 12L, 302L, "供应商B", true))
        ));

        List<V2ProductDtos.ProductResponse> responses = service.list(" ", 1, null, null);

        assertEquals(2, responses.size());
        assertEquals("P-1", responses.get(0).code());
        assertEquals("P-2", responses.get(1).code());
        assertEquals("分类A", responses.get(0).categoryName());
        assertEquals("件", responses.get(0).unitName());
        assertEquals(1, responses.get(0).priceLevels().size());
        assertEquals("供应商A", responses.get(0).defaultSupplier().supplierName());
        assertEquals(1, responses.get(0).supplierRelations().size());
        assertEquals("供应商B", responses.get(1).defaultSupplier().supplierName());

        ArgumentCaptor<Collection<Long>> categoryIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<Long>> unitIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<Long>> priceLevelIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<Long>> productIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(categoryService).getOwnedEntityMap(categoryIdsCaptor.capture());
        verify(unitService).getOwnedEntityMap(unitIdsCaptor.capture());
        verify(priceLevelService).getOwnedEntityMap(priceLevelIdsCaptor.capture());
        verify(supplierRelationService).listByProductIds(productIdsCaptor.capture());
        assertEquals(Set.of(101L, 102L), Set.copyOf(categoryIdsCaptor.getValue()));
        assertEquals(Set.of(201L, 202L), Set.copyOf(unitIdsCaptor.getValue()));
        assertEquals(Set.of(100L, 101L), Set.copyOf(priceLevelIdsCaptor.getValue()));
        assertEquals(Set.of(11L, 12L), Set.copyOf(productIdsCaptor.getValue()));
        verify(priceLevelService).extractLevelIds(first.getPriceLevelValuesJson());
        verify(priceLevelService).extractLevelIds(second.getPriceLevelValuesJson());
        assertNotNull(responses.get(0).priceLevels().get(0));
    }

    @Test
    void listAppliesRepositoryFiltersBeforeResponseAssembly() {
        ProductEntity second = product(22L, "B-200", 102L, 202L, "[]");
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(1L, "abc", 0, 102L, 202L))
            .thenReturn(List.of(second));
        when(categoryService.getOwnedEntityMap(anyCollection())).thenReturn(Map.of(102L, category(102L, "分类B")));
        when(unitService.getOwnedEntityMap(anyCollection())).thenReturn(Map.of(202L, unit(202L, "箱")));
        when(priceLevelService.getOwnedEntityMap(anyCollection())).thenReturn(Map.of());
        when(supplierRelationService.listByProductIds(anyCollection())).thenReturn(Map.of());

        List<V2ProductDtos.ProductResponse> responses = service.list(" abc ", 0, 102L, 202L);

        assertEquals(1, responses.size());
        assertEquals("B-200", responses.get(0).code());
        verify(productRepository).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(1L, "abc", 0, 102L, 202L);
    }

    private static ProductEntity product(Long id, String code, Long categoryId, Long unitId, String priceLevelsJson) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName("商品" + code);
        entity.setCategoryId(categoryId);
        entity.setCategory("旧分类");
        entity.setUnitId(unitId);
        entity.setUnit("旧单位");
        entity.setSalePrice(20.0);
        entity.setPurchasePrice(12.0);
        entity.setPriceLevelValuesJson(priceLevelsJson);
        entity.setStock(15.0);
        entity.setSafeStock(3.0);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L + id);
        return entity;
    }

    private static ProductCategoryEntity category(Long id, String name) {
        ProductCategoryEntity entity = new ProductCategoryEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(1);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static ProductUnitEntity unit(Long id, String name) {
        ProductUnitEntity entity = new ProductUnitEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(1);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static ProductPriceLevelEntity priceLevel(Long id, String code, String name, Integer sortOrder) {
        ProductPriceLevelEntity entity = new ProductPriceLevelEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static V2ProductDtos.ProductSupplierRelationResponse supplierRelation(
        Long id,
        Long productId,
        Long supplierId,
        String supplierName,
        boolean isDefault
    ) {
        return new V2ProductDtos.ProductSupplierRelationResponse(
            id,
            productId,
            supplierId,
            supplierName,
            "13900000000",
            isDefault,
            1,
            11.0,
            "备注",
            1L,
            1L
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> Collection<T> anyCollection() {
        return (Collection<T>) org.mockito.ArgumentMatchers.anyCollection();
    }
}
