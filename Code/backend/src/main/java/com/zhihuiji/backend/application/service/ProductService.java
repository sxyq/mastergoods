package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
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
    private final IdGenerator idGenerator;

    public ProductService(
        ProductRepository productRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository,
        CurrentOwnerService currentOwnerService,
        IdGenerator idGenerator
    ) {
        this.productRepository = productRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.currentOwnerService = currentOwnerService;
        this.idGenerator = idGenerator;
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
        double currentStock = requireFiniteNonNegative(target.getStock(), "商品库存不能为空");
        double requestedStock = requireFiniteNonNegative(payload.getStock(), "商品库存不能为空");
        BigDecimal stockDelta = BigDecimal.valueOf(requestedStock).subtract(BigDecimal.valueOf(currentStock));
        target.setName(payload.getName());
        target.setCategory(payload.getCategory());
        target.setUnit(payload.getUnit());
        target.setSalePrice(payload.getSalePrice());
        target.setPurchasePrice(payload.getPurchasePrice());
        target.setSafeStock(payload.getSafeStock());
        target.setStatus(payload.getStatus());
        target.setUpdatedAt(System.currentTimeMillis());
        target.setSyncStatus(0);
        target.setSyncVersion(target.getSyncVersion() + 1);
        productRepository.save(target);
        if (stockDelta.signum() == 0) {
            return target;
        }
        return adjustStock(id, stockDelta, "商品资料编辑", null);
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
        adjustment.setId(idGenerator.nextId());
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

    private double requireFiniteNonNegative(Double value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("数值必须是有限且非负数字");
        }
        return value;
    }
}
