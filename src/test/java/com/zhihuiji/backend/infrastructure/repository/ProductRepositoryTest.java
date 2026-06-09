package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.domain.entity.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    void productAggregatesAreOwnerScopedAndCountLowStockWithoutLoadingRows() {
        productRepository.save(product(1L, "P-1", 5.0, 10.0));
        productRepository.save(product(1L, "P-2", 12.0, 10.0));
        productRepository.save(product(1L, "P-3", 3.0, 3.0));
        productRepository.save(product(2L, "P-4", 100.0, 200.0));

        assertEquals(3L, productRepository.countByOwnerUserId(1L));
        assertEquals(20.0, productRepository.sumStockByOwnerUserId(1L));
        assertEquals(2L, productRepository.countLowStockByOwnerUserId(1L));
    }

    private static ProductEntity product(Long ownerUserId, String code, Double stock, Double safeStock) {
        ProductEntity entity = new ProductEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setCode(code);
        entity.setName("商品" + code);
        entity.setCategory("默认");
        entity.setUnit("件");
        entity.setSalePrice(10.0);
        entity.setPurchasePrice(5.0);
        entity.setStock(stock);
        entity.setSafeStock(safeStock);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
