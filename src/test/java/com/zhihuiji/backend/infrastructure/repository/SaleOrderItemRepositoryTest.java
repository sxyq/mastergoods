package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SaleOrderItemRepositoryTest {
    @Autowired
    private SaleOrderRepository saleOrderRepository;
    @Autowired
    private SaleOrderItemRepository saleOrderItemRepository;
    @Autowired
    private ProductRepository productRepository;

    @Test
    void profitSummaryAggregatesSalesAndTreatsMissingProductCostAsZero() {
        ProductEntity product = productRepository.save(product(12.0));
        saleOrderRepository.save(saleOrder(10L, 1L, 1_000L, OrderStatus.COMPLETED.code()));
        saleOrderRepository.save(saleOrder(11L, 1L, 1_500L, OrderStatus.COMPLETED.code()));
        saleOrderRepository.save(saleOrder(12L, 1L, 1_600L, OrderStatus.CANCELLED.code()));
        saleOrderRepository.save(saleOrder(13L, 2L, 1_000L, OrderStatus.COMPLETED.code()));
        saleOrderItemRepository.save(saleOrderItem(1L, 1L, 10L, product.getId(), 2.0, 100.0));
        saleOrderItemRepository.save(saleOrderItem(2L, 1L, 11L, 999L, 1.0, 40.0));
        saleOrderItemRepository.save(saleOrderItem(3L, 1L, 12L, product.getId(), 1.0, 80.0));
        saleOrderItemRepository.save(saleOrderItem(4L, 2L, 13L, product.getId(), 1.0, 999.0));

        Object[] row = normalizeAggregateRow(saleOrderItemRepository.profitSummary(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code()
        ));

        assertEquals(140.0, ((Number) row[0]).doubleValue());
        assertEquals(24.0, ((Number) row[1]).doubleValue());
    }

    private static Object[] normalizeAggregateRow(Object[] raw) {
        if (raw.length == 1 && raw[0] instanceof Object[] nested) {
            return nested;
        }
        return raw;
    }

    private static SaleOrderEntity saleOrder(Long id, Long ownerUserId, Long createdAt, Integer status) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo("SO-" + id);
        entity.setCustomerName("散客");
        entity.setSubtotalAmount(100.0);
        entity.setDiscountAmount(0.0);
        entity.setTotalAmount(100.0);
        entity.setPaidAmount(100.0);
        entity.setStatus(status);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static SaleOrderItemEntity saleOrderItem(
        Long id,
        Long ownerUserId,
        Long orderId,
        Long productId,
        Double quantity,
        Double amount
    ) {
        SaleOrderItemEntity entity = new SaleOrderItemEntity();
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderId(orderId);
        entity.setProductId(productId);
        entity.setProductCode("P-" + productId);
        entity.setProductName("商品" + productId);
        entity.setQuantity(quantity);
        entity.setUnitPrice(amount / quantity);
        entity.setAmount(amount);
        entity.setCreatedAt(1_000L);
        return entity;
    }

    private static ProductEntity product(Double purchasePrice) {
        ProductEntity entity = new ProductEntity();
        entity.setOwnerUserId(1L);
        entity.setCode("P-COST");
        entity.setName("商品");
        entity.setCategory("默认");
        entity.setUnit("件");
        entity.setSalePrice(50.0);
        entity.setPurchasePrice(purchasePrice);
        entity.setStock(1.0);
        entity.setSafeStock(1.0);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
