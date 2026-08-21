package com.zhihuiji.backend.application.service.v2.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.product.ProductCategoryEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductCategoryRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2ProductCategoryServiceTest {
    @Mock
    private ProductCategoryRepository productCategoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2ProductCategoryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2ProductCategoryService(productCategoryRepository, productRepository, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createRejectsDuplicateName() {
        when(productCategoryRepository.existsByOwnerUserIdAndName(1L, "饮料")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(new V2ProductDtos.CategoryWriteRequest("饮料", 1, 0)));
    }

    @Test
    void deleteRejectsReferencedCategory() {
        ProductCategoryEntity category = category(10L, "饮料");
        when(productCategoryRepository.findByIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(productRepository.countByOwnerUserIdAndCategoryId(1L, 10L)).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> service.delete(10L));
    }

    @Test
    void createInitializesOwnerAndTimestamps() {
        when(productCategoryRepository.existsByOwnerUserIdAndName(1L, "饮料")).thenReturn(false);
        when(productCategoryRepository.save(any(ProductCategoryEntity.class))).thenAnswer(invocation -> {
            ProductCategoryEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return entity;
        });

        V2ProductDtos.CategoryResponse response = service.create(new V2ProductDtos.CategoryWriteRequest(" 饮料 ", null, null));

        assertEquals(11L, response.id());
        assertEquals("饮料", response.name());
        assertEquals(1, response.status());
        assertEquals(0, response.sortOrder());
    }

    private static ProductCategoryEntity category(Long id, String name) {
        ProductCategoryEntity entity = new ProductCategoryEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(0);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
