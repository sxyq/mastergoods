package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SaleOrderRepositoryTest {
    @Autowired
    private SaleOrderRepository saleOrderRepository;

    @Test
    void salesTrendBucketsAggregatesByOwnerBucketAndExcludesCancelledOrders() {
        saleOrderRepository.save(saleOrder(1L, 1L, 0L, 100.0, OrderStatus.COMPLETED.code()));
        saleOrderRepository.save(saleOrder(2L, 1L, 21_600_000L, 50.0, OrderStatus.COMPLETED.code()));
        saleOrderRepository.save(saleOrder(3L, 1L, 43_200_000L, 80.0, OrderStatus.CANCELLED.code()));
        saleOrderRepository.save(saleOrder(4L, 2L, 0L, 999.0, OrderStatus.COMPLETED.code()));

        List<Object[]> rows = saleOrderRepository.salesTrendBuckets(
            1L,
            0L,
            86_399_999L,
            21_600_000L,
            OrderStatus.CANCELLED.code()
        );

        assertEquals(2, rows.size());
        assertEquals(0L, ((Number) rows.get(0)[0]).longValue());
        assertEquals(100.0, ((Number) rows.get(0)[1]).doubleValue());
        assertEquals(1L, ((Number) rows.get(0)[2]).longValue());
        assertEquals(1L, ((Number) rows.get(1)[0]).longValue());
        assertEquals(50.0, ((Number) rows.get(1)[1]).doubleValue());
        assertEquals(1L, ((Number) rows.get(1)[2]).longValue());
    }

    private static SaleOrderEntity saleOrder(Long id, Long ownerUserId, Long createdAt, Double totalAmount, Integer status) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo("SO-" + id);
        entity.setCustomerName("散客");
        entity.setSubtotalAmount(totalAmount);
        entity.setDiscountAmount(0.0);
        entity.setTotalAmount(totalAmount);
        entity.setPaidAmount(totalAmount);
        entity.setStatus(status);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }
}
