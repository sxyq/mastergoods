package com.zhihuiji.backend.application.service.v2.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.product.ProductPriceLevelEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductPriceLevelRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2ProductPriceLevelServiceTest {
    @Mock
    private ProductPriceLevelRepository productPriceLevelRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2ProductPriceLevelService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2ProductPriceLevelService(
            productPriceLevelRepository,
            productRepository,
            currentOwnerService,
            new ObjectMapper()
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(productPriceLevelRepository.existsByOwnerUserIdAndCode(1L, "WHOLESALE")).thenReturn(true);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.create(new V2ProductDtos.PriceLevelWriteRequest("WHOLESALE", "批发价", 1, 0))
        );
    }

    @Test
    void createInitializesOwnerAndDefaults() {
        when(productPriceLevelRepository.existsByOwnerUserIdAndCode(1L, "WHOLESALE")).thenReturn(false);
        when(productPriceLevelRepository.existsByOwnerUserIdAndName(1L, "批发价")).thenReturn(false);
        when(productPriceLevelRepository.save(any(ProductPriceLevelEntity.class))).thenAnswer(invocation -> {
            ProductPriceLevelEntity entity = invocation.getArgument(0);
            entity.setId(31L);
            return entity;
        });

        V2ProductDtos.PriceLevelResponse response = service.create(
            new V2ProductDtos.PriceLevelWriteRequest(" WHOLESALE ", " 批发价 ", null, null)
        );

        assertEquals(31L, response.id());
        assertEquals("WHOLESALE", response.code());
        assertEquals("批发价", response.name());
        assertEquals(1, response.status());
        assertEquals(0, response.sortOrder());
    }

    @Test
    void deleteRejectsReferencedPriceLevel() {
        ProductPriceLevelEntity entity = priceLevel(31L, "WHOLESALE", "批发价");
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setOwnerUserId(1L);
        product.setPriceLevelValuesJson("[{\"levelId\":31,\"price\":3.8}]");
        when(productPriceLevelRepository.findByIdAndOwnerUserId(31L, 1L)).thenReturn(Optional.of(entity));
        when(productRepository.findAllByOwnerUserId(1L)).thenReturn(List.of(product));

        assertThrows(IllegalArgumentException.class, () -> service.delete(31L));
    }

    @Test
    void encodeValueSnapshotRejectsUnknownLevel() {
        when(productPriceLevelRepository.findAllByOwnerUserIdAndIdIn(1L, List.of(31L))).thenReturn(List.of());

        assertThrows(
            IllegalArgumentException.class,
            () -> service.encodeValueSnapshot(List.of(new V2ProductDtos.ProductPriceValueWriteRequest(31L, 3.8)))
        );
    }

    @Test
    void encodeValueSnapshotPreservesInputOrderAndUsesSingleLookupSet() throws Exception {
        when(productPriceLevelRepository.findAllByOwnerUserIdAndIdIn(eq(1L), anyCollection()))
            .thenReturn(List.of(priceLevel(31L, "WHOLESALE", "批发"), priceLevel(32L, "RETAIL", "零售")));

        String json = service.encodeValueSnapshot(List.of(
            new V2ProductDtos.ProductPriceValueWriteRequest(31L, 3.8),
            new V2ProductDtos.ProductPriceValueWriteRequest(32L, 4.2)
        ));

        List<Map<String, Object>> values = new ObjectMapper().readValue(json, new TypeReference<>() {});
        assertEquals(2, values.size());
        assertEquals(31, ((Number) values.get(0).get("levelId")).intValue());
        assertEquals(32, ((Number) values.get(1).get("levelId")).intValue());

        ArgumentCaptor<java.util.Collection<Long>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        org.mockito.Mockito.verify(productPriceLevelRepository).findAllByOwnerUserIdAndIdIn(eq(1L), captor.capture());
        assertEquals(List.of(31L, 32L), List.copyOf(captor.getValue()));
    }

    private static ProductPriceLevelEntity priceLevel(Long id, String code, String name) {
        ProductPriceLevelEntity entity = new ProductPriceLevelEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(0);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
