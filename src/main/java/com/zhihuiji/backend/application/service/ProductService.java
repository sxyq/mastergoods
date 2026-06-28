package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import com.zhihuiji.backend.api.common.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final int FLOW_OUT = 0;
    private static final int FLOW_IN = 1;

    private final ProductRepository productRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final CurrentOwnerService currentOwnerService;

    public ProductService(
        ProductRepository productRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.productRepository = productRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> list(String keyword) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(
            ownerUserId,
            normalizeKeyword(keyword),
            null,
            null,
            null
        );
    }

    @Transactional(readOnly = true)
    public ProductEntity get(Long id) {
        return productRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    }

    @Transactional(readOnly = true)
    public ProductEntity findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return productRepository.findByOwnerUserIdAndCode(currentOwnerService.requireCurrentOwnerUserId(), code.trim())
            .orElse(null);
    }

    @Transactional
    public ProductEntity create(ProductEntity product) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (productRepository.findByOwnerUserIdAndCode(ownerUserId, product.getCode()).isPresent()) {
            throw new IllegalArgumentException("商品编码已存在");
        }
        long now = System.currentTimeMillis();
        product.setOwnerUserId(ownerUserId);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        product.setSyncStatus(0);
        product.setSyncVersion(1L);
        return productRepository.save(product);
    }

    @Transactional
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

    @Transactional
    public void delete(Long id) {
        productRepository.delete(get(id));
    }

    @Transactional
    public ProductEntity adjustStock(Long id, BigDecimal delta, String reason, String operator) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("库存调整值不能为空或0");
        }
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductEntity target = productRepository.findByIdForUpdate(ownerUserId, id)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        long now = System.currentTimeMillis();
        BigDecimal nextStock = BigDecimal.valueOf(target.getStock()).add(delta);
        if (nextStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("库存不足，无法扣减");
        }
        target.setStock(nextStock.doubleValue());
        target.setUpdatedAt(now);
        target.setSyncStatus(0);
        target.setSyncVersion(target.getSyncVersion() == null ? 1L : target.getSyncVersion() + 1);
        ProductEntity saved = productRepository.save(target);

        InventoryAdjustmentEntity adjustment = new InventoryAdjustmentEntity();
        adjustment.setId(IdGenerator.nextId());
        adjustment.setOwnerUserId(ownerUserId);
        adjustment.setProductId(saved.getId());
        adjustment.setProductCode(saved.getCode());
        adjustment.setProductName(saved.getName());
        adjustment.setQuantity(delta.abs().doubleValue());
        adjustment.setFlowType(delta.signum() > 0 ? FLOW_IN : FLOW_OUT);
        adjustment.setReason(reason);
        adjustment.setOperatorName(operator);
        adjustment.setCreatedAt(now);
        inventoryAdjustmentRepository.save(adjustment);
        return saved;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
