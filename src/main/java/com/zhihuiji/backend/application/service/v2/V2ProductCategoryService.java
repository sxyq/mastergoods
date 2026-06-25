package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductCategoryEntity;
import java.util.Collection;
import com.zhihuiji.backend.infrastructure.repository.ProductCategoryRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class V2ProductCategoryService {
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2ProductCategoryService(
        ProductCategoryRepository productCategoryRepository,
        ProductRepository productRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.productCategoryRepository = productCategoryRepository;
        this.productRepository = productRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2ProductDtos.CategoryResponse> list() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<ProductCategoryEntity> rows = productCategoryRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId);
        return rows.stream().map(this::toResponse).toList();
    }

    public ProductCategoryEntity getOwnedEntity(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return productCategoryRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("商品分类不存在"));
    }

    public Map<Long, ProductCategoryEntity> getOwnedEntityMap(Collection<Long> ids) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProductCategoryEntity> categoriesById = new LinkedHashMap<>(ids.size());
        for (ProductCategoryEntity category : productCategoryRepository.findAllByOwnerUserIdAndIdIn(ownerUserId, ids)) {
            categoriesById.put(category.getId(), category);
        }
        return categoriesById;
    }

    public V2ProductDtos.CategoryResponse create(V2ProductDtos.CategoryWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String name = normalizeRequired(request.name(), "商品分类名称不能为空");
        if (productCategoryRepository.existsByOwnerUserIdAndName(ownerUserId, name)) {
            throw new IllegalArgumentException("商品分类名称已存在");
        }
        long now = System.currentTimeMillis();
        ProductCategoryEntity entity = new ProductCategoryEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(productCategoryRepository.save(entity));
    }

    public V2ProductDtos.CategoryResponse update(Long id, V2ProductDtos.CategoryWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductCategoryEntity entity = getOwnedEntity(id);
        String name = normalizeRequired(request.name(), "商品分类名称不能为空");
        if (productCategoryRepository.existsByOwnerUserIdAndNameAndIdNot(ownerUserId, name, id)) {
            throw new IllegalArgumentException("商品分类名称已存在");
        }
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setUpdatedAt(System.currentTimeMillis());
        return toResponse(productCategoryRepository.save(entity));
    }

    public void delete(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductCategoryEntity entity = getOwnedEntity(id);
        if (productRepository.countByOwnerUserIdAndCategoryId(ownerUserId, id) > 0) {
            throw new IllegalArgumentException("商品分类已被商品引用，无法删除");
        }
        productCategoryRepository.delete(entity);
    }

    private V2ProductDtos.CategoryResponse toResponse(ProductCategoryEntity entity) {
        return new V2ProductDtos.CategoryResponse(
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
            throw new IllegalArgumentException("商品分类状态不合法");
        }
        return status;
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}
