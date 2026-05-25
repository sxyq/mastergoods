package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final int FLOW_OUT = 0;
    private static final int FLOW_IN = 1;

    private final ProductRepository productRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    public ProductService(
        ProductRepository productRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
    }

    public List<ProductEntity> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll();
        }
        return productRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword);
    }

    public ProductEntity get(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    }

    public ProductEntity findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return productRepository.findByCode(code.trim()).orElse(null);
    }

    public ProductEntity create(ProductEntity product) {
        if (productRepository.findByCode(product.getCode()).isPresent()) {
            throw new IllegalArgumentException("商品编码已存在");
        }
        long now = System.currentTimeMillis();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        product.setSyncStatus(0);
        product.setSyncVersion(1L);
        return productRepository.save(product);
    }

    public ProductEntity update(Long id, ProductEntity payload) {
        ProductEntity target = get(id);
        target.setName(payload.getName());
        target.setCategory(payload.getCategory());
        target.setUnit(payload.getUnit());
        target.setSalePrice(payload.getSalePrice());
        target.setPurchasePrice(payload.getPurchasePrice());
        target.setStock(payload.getStock());
        target.setSafeStock(payload.getSafeStock());
        target.setStatus(payload.getStatus());
        target.setUpdatedAt(System.currentTimeMillis());
        target.setSyncStatus(0);
        target.setSyncVersion(target.getSyncVersion() + 1);
        return productRepository.save(target);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductEntity adjustStock(Long id, Double delta, String reason, String operator) {
        if (delta == null || Math.abs(delta) < 0.000001) {
            throw new IllegalArgumentException("库存调整值不能为空或0");
        }
        ProductEntity target = productRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        long now = System.currentTimeMillis();
        double nextStock = target.getStock() + delta;
        if (nextStock < 0) {
            throw new IllegalArgumentException("库存不足，无法扣减");
        }
        target.setStock(nextStock);
        target.setUpdatedAt(now);
        target.setSyncStatus(0);
        target.setSyncVersion(target.getSyncVersion() == null ? 1L : target.getSyncVersion() + 1);
        ProductEntity saved = productRepository.save(target);

        InventoryAdjustmentEntity adjustment = new InventoryAdjustmentEntity();
        adjustment.setId(nextId());
        adjustment.setProductId(saved.getId());
        adjustment.setProductCode(saved.getCode());
        adjustment.setProductName(saved.getName());
        adjustment.setQuantity(Math.abs(delta));
        adjustment.setFlowType(delta > 0 ? FLOW_IN : FLOW_OUT);
        adjustment.setReason(reason);
        adjustment.setOperatorName(operator);
        adjustment.setCreatedAt(now);
        inventoryAdjustmentRepository.save(adjustment);
        return saved;
    }

    private long nextId() {
        long id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        return id == 0L ? (System.nanoTime() & Long.MAX_VALUE) : id;
    }
}
