package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class InventoryAdjustmentRepositoryTest {
    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Test
    void findByOwnerAndCreatedAtRangeOrdersByCreatedAtDescWithLimit() {
        inventoryAdjustmentRepository.save(adjustment(1L, 1L, 1_000L));
        inventoryAdjustmentRepository.save(adjustment(2L, 1L, 1_900L));
        inventoryAdjustmentRepository.save(adjustment(3L, 1L, 2_500L));
        inventoryAdjustmentRepository.save(adjustment(4L, 2L, 1_950L));

        List<InventoryAdjustmentEntity> rows =
            inventoryAdjustmentRepository.findByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                1L,
                0L,
                2_000L,
                PageRequest.of(0, 1)
            );

        assertEquals(1, rows.size());
        assertEquals(2L, rows.getFirst().getId());
    }

    private static InventoryAdjustmentEntity adjustment(Long id, Long ownerUserId, Long createdAt) {
        InventoryAdjustmentEntity entity = new InventoryAdjustmentEntity();
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setProductId(10L + id);
        entity.setProductCode("P-" + id);
        entity.setProductName("商品" + id);
        entity.setQuantity(1.0);
        entity.setFlowType(1);
        entity.setReason("盘点");
        entity.setOperatorName("老板");
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
