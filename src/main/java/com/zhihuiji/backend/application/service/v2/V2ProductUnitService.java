package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductUnitEntity;
import java.util.Collection;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductUnitRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class V2ProductUnitService {
    private final ProductUnitRepository productUnitRepository;
    private final ProductRepository productRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2ProductUnitService(
        ProductUnitRepository productUnitRepository,
        ProductRepository productRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.productUnitRepository = productUnitRepository;
        this.productRepository = productRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2ProductDtos.UnitResponse> list() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return productUnitRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId).stream()
            .map(this::toResponse)
            .toList();
    }

    public ProductUnitEntity getOwnedEntity(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return productUnitRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("商品单位不存在"));
    }

    public Map<Long, ProductUnitEntity> getOwnedEntityMap(Collection<Long> ids) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return productUnitRepository.findAllByOwnerUserIdAndIdIn(ownerUserId, ids).stream()
            .collect(Collectors.toMap(ProductUnitEntity::getId, value -> value));
    }

    public V2ProductDtos.UnitResponse create(V2ProductDtos.UnitWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String name = normalizeRequired(request.name(), "商品单位名称不能为空");
        if (productUnitRepository.existsByOwnerUserIdAndName(ownerUserId, name)) {
            throw new IllegalArgumentException("商品单位名称已存在");
        }
        long now = System.currentTimeMillis();
        ProductUnitEntity entity = new ProductUnitEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(productUnitRepository.save(entity));
    }

    public V2ProductDtos.UnitResponse update(Long id, V2ProductDtos.UnitWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductUnitEntity entity = getOwnedEntity(id);
        String name = normalizeRequired(request.name(), "商品单位名称不能为空");
        if (productUnitRepository.existsByOwnerUserIdAndNameAndIdNot(ownerUserId, name, id)) {
            throw new IllegalArgumentException("商品单位名称已存在");
        }
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setUpdatedAt(System.currentTimeMillis());
        return toResponse(productUnitRepository.save(entity));
    }

    public void delete(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductUnitEntity entity = getOwnedEntity(id);
        if (productRepository.countByOwnerUserIdAndUnitId(ownerUserId, id) > 0) {
            throw new IllegalArgumentException("商品单位已被商品引用，无法删除");
        }
        productUnitRepository.delete(entity);
    }

    private V2ProductDtos.UnitResponse toResponse(ProductUnitEntity entity) {
        return new V2ProductDtos.UnitResponse(
            entity.getId(),
            entity.getName(),
            entity.getStatus(),
            entity.getSortOrder(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("商品单位状态不合法");
        }
        return status;
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}
