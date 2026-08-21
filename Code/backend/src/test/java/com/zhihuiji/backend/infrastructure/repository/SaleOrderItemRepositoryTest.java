package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

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

    @Test
    void recentStockOutRowsUsesOrderWindowButSortsAndLimitsByItemCreatedAt() {
        ProductEntity product = productRepository.save(product(12.0));
        saleOrderRepository.save(saleOrder(20L, 1L, 1_000L, OrderStatus.COMPLETED.code()));
        saleOrderRepository.save(saleOrder(21L, 1L, 1_100L, OrderStatus.CANCELLED.code()));
        saleOrderRepository.save(saleOrder(22L, 2L, 1_200L, OrderStatus.COMPLETED.code()));
        saleOrderRepository.save(saleOrder(23L, 1L, 3_000L, OrderStatus.COMPLETED.code()));
        saleOrderItemRepository.save(saleOrderItem(20L, 1L, 20L, product.getId(), 1.0, 10.0, 1_500L));
        saleOrderItemRepository.save(saleOrderItem(21L, 1L, 20L, product.getId(), 1.0, 20.0, 1_900L));
        saleOrderItemRepository.save(saleOrderItem(22L, 1L, 21L, product.getId(), 1.0, 30.0, 2_000L));
        saleOrderItemRepository.save(saleOrderItem(23L, 2L, 22L, product.getId(), 1.0, 40.0, 2_100L));
        saleOrderItemRepository.save(saleOrderItem(24L, 1L, 23L, product.getId(), 1.0, 50.0, 3_100L));

        List<Object[]> rows = saleOrderItemRepository.recentStockOutRows(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, 1)
        );

        assertEquals(1, rows.size());
        SaleOrderItemEntity item = (SaleOrderItemEntity) rows.getFirst()[0];
        SaleOrderEntity order = (SaleOrderEntity) rows.getFirst()[1];
        assertEquals(21L, item.getId());
        assertEquals(20L, order.getId());
    }

    @Test
    void recentInventoryFlowRowsSeparateCreatedSalesAndCancelledSales() {
        ProductEntity product = productRepository.save(product(12.0));
        SaleOrderEntity activeEarly = saleOrder(30L, 1L, 1_000L, OrderStatus.COMPLETED.code());
        activeEarly.setUpdatedAt(1_100L);
        SaleOrderEntity activeLate = saleOrder(31L, 1L, 1_800L, OrderStatus.COMPLETED.code());
        activeLate.setUpdatedAt(1_900L);
        SaleOrderEntity cancelled = saleOrder(32L, 1L, 1_200L, OrderStatus.CANCELLED.code());
        cancelled.setUpdatedAt(1_950L);
        SaleOrderEntity cancelledOutsideUpdate = saleOrder(33L, 1L, 1_300L, OrderStatus.CANCELLED.code());
        cancelledOutsideUpdate.setUpdatedAt(3_000L);
        saleOrderRepository.save(activeEarly);
        saleOrderRepository.save(activeLate);
        saleOrderRepository.save(cancelled);
        saleOrderRepository.save(cancelledOutsideUpdate);
        saleOrderItemRepository.save(saleOrderItem(30L, 1L, 30L, product.getId(), 1.0, 10.0, 1_010L));
        saleOrderItemRepository.save(saleOrderItem(31L, 1L, 31L, product.getId(), 1.0, 20.0, 1_810L));
        saleOrderItemRepository.save(saleOrderItem(32L, 1L, 32L, product.getId(), 1.0, 30.0, 1_210L));
        saleOrderItemRepository.save(saleOrderItem(33L, 1L, 33L, product.getId(), 1.0, 40.0, 1_310L));

        List<Object[]> saleRows = saleOrderItemRepository.recentSaleInventoryFlowRows(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, 1)
        );
        List<Object[]> cancelledRows = saleOrderItemRepository.recentCancelledSaleInventoryFlowRows(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, 1)
        );

        assertEquals(1, saleRows.size());
        assertEquals(31L, ((SaleOrderEntity) saleRows.getFirst()[1]).getId());
        assertEquals(1, cancelledRows.size());
        assertEquals(32L, ((SaleOrderEntity) cancelledRows.getFirst()[1]).getId());
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
        return saleOrderItem(id, ownerUserId, orderId, productId, quantity, amount, 1_000L);
    }

    private static SaleOrderItemEntity saleOrderItem(
        Long id,
        Long ownerUserId,
        Long orderId,
        Long productId,
        Double quantity,
        Double amount,
        Long createdAt
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
        entity.setCreatedAt(createdAt);
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
