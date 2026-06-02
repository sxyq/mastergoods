package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductCategoryEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import com.zhihuiji.backend.domain.entity.ProductUnitEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2ProductService {
    private final ProductRepository productRepository;
    private final V2ProductCategoryService categoryService;
    private final V2ProductUnitService unitService;
    private final V2ProductPriceLevelService priceLevelService;
    private final V2ProductSupplierRelationService supplierRelationService;
    private final CurrentOwnerService currentOwnerService;

    public V2ProductService(
        ProductRepository productRepository,
        V2ProductCategoryService categoryService,
        V2ProductUnitService unitService,
        V2ProductPriceLevelService priceLevelService,
        V2ProductSupplierRelationService supplierRelationService,
        CurrentOwnerService currentOwnerService
    ) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.unitService = unitService;
        this.priceLevelService = priceLevelService;
        this.supplierRelationService = supplierRelationService;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2ProductDtos.ProductResponse> list(String keyword, Integer status, Long categoryId, Long unitId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<ProductEntity> products = (keyword == null || keyword.isBlank())
            ? productRepository.findAllByOwnerUserId(ownerUserId)
            : productRepository.findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndCodeContainingIgnoreCase(
                ownerUserId,
                keyword.trim(),
                ownerUserId,
                keyword.trim()
            );
        return products.stream()
            .filter(product -> status == null || status.equals(product.getStatus()))
            .filter(product -> categoryId == null || categoryId.equals(product.getCategoryId()))
            .filter(product -> unitId == null || unitId.equals(product.getUnitId()))
            .sorted(Comparator.comparing(ProductEntity::getUpdatedAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    public List<V2ProductDtos.ProductResponse> lowStock(Integer size) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        int limit = size == null || size <= 0 ? 20 : Math.min(size, 100);
        return productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, limit)).stream()
            .map(this::toResponse)
            .toList();
    }

    public V2ProductDtos.ProductResponse get(Long id) {
        return toResponse(getOwnedEntity(id));
    }

    @Transactional
    public V2ProductDtos.ProductResponse create(V2ProductDtos.ProductWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String code = normalizeRequired(request.code(), "商品编码不能为空");
        if (productRepository.findByOwnerUserIdAndCode(ownerUserId, code).isPresent()) {
            throw new IllegalArgumentException("商品编码已存在");
        }
        ProductCategoryEntity category = categoryService.getOwnedEntity(request.categoryId());
        ProductUnitEntity unit = unitService.getOwnedEntity(request.unitId());
        long now = System.currentTimeMillis();
        ProductEntity entity = new ProductEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setCode(code);
        entity.setName(normalizeRequired(request.name(), "商品名称不能为空"));
        entity.setCategoryId(category.getId());
        entity.setCategory(category.getName());
        entity.setUnitId(unit.getId());
        entity.setUnit(unit.getName());
        entity.setSalePrice(normalizeNonNegative(request.salePrice(), "商品售价不能为空"));
        entity.setPurchasePrice(normalizeNonNegative(request.purchasePrice(), "商品进价不能为空"));
        entity.setPriceLevelValuesJson(priceLevelService.encodeValueSnapshot(request.priceLevels()));
        entity.setStock(normalizeNonNegative(request.stock(), "商品库存不能为空"));
        entity.setSafeStock(normalizeNonNegative(request.safeStock(), "安全库存不能为空"));
        entity.setStatus(normalizeStatus(request.status(), "商品状态不合法"));
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        ProductEntity saved = productRepository.save(entity);
        if (request.supplierRelations() != null) {
            supplierRelationService.replaceForProduct(saved.getId(), request.supplierRelations());
        }
        return toResponse(saved);
    }

    @Transactional
    public V2ProductDtos.ProductResponse update(Long id, V2ProductDtos.ProductWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductEntity entity = getOwnedEntity(id);
        String code = normalizeRequired(request.code(), "商品编码不能为空");
        productRepository.findByOwnerUserIdAndCode(ownerUserId, code)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("商品编码已存在");
            });
        ProductCategoryEntity category = categoryService.getOwnedEntity(request.categoryId());
        ProductUnitEntity unit = unitService.getOwnedEntity(request.unitId());
        entity.setCode(code);
        entity.setName(normalizeRequired(request.name(), "商品名称不能为空"));
        entity.setCategoryId(category.getId());
        entity.setCategory(category.getName());
        entity.setUnitId(unit.getId());
        entity.setUnit(unit.getName());
        entity.setSalePrice(normalizeNonNegative(request.salePrice(), "商品售价不能为空"));
        entity.setPurchasePrice(normalizeNonNegative(request.purchasePrice(), "商品进价不能为空"));
        if (request.priceLevels() != null) {
            entity.setPriceLevelValuesJson(priceLevelService.encodeValueSnapshot(request.priceLevels()));
        }
        entity.setStock(normalizeNonNegative(request.stock(), "商品库存不能为空"));
        entity.setSafeStock(normalizeNonNegative(request.safeStock(), "安全库存不能为空"));
        entity.setStatus(normalizeStatus(request.status(), "商品状态不合法"));
        entity.setUpdatedAt(System.currentTimeMillis());
        entity.setSyncStatus(0);
        entity.setSyncVersion(entity.getSyncVersion() + 1);
        ProductEntity saved = productRepository.save(entity);
        if (request.supplierRelations() != null) {
            supplierRelationService.replaceForProduct(saved.getId(), request.supplierRelations());
        }
        return toResponse(saved);
    }

    public void delete(Long id) {
        productRepository.delete(getOwnedEntity(id));
    }

    private ProductEntity getOwnedEntity(Long id) {
        return productRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    }

    private V2ProductDtos.ProductResponse toResponse(ProductEntity entity) {
        Map<Long, ProductCategoryEntity> categoriesById = categoryService.getOwnedEntityMap(entity.getCategoryId() == null ? Set.of() : Set.of(entity.getCategoryId()));
        Map<Long, ProductUnitEntity> unitsById = unitService.getOwnedEntityMap(entity.getUnitId() == null ? Set.of() : Set.of(entity.getUnitId()));
        Set<Long> priceLevelIds = new LinkedHashSet<>(priceLevelService.extractLevelIds(entity.getPriceLevelValuesJson()));
        Map<Long, ProductPriceLevelEntity> priceLevelDefinitionsById = priceLevelService.getOwnedEntityMap(priceLevelIds);
        List<V2ProductDtos.ProductSupplierRelationResponse> supplierRelations = supplierRelationService.list(entity.getId());
        ProductCategoryEntity category = entity.getCategoryId() == null ? null : categoriesById.get(entity.getCategoryId());
        ProductUnitEntity unit = entity.getUnitId() == null ? null : unitsById.get(entity.getUnitId());
        V2ProductDtos.ProductSupplierRelationResponse defaultSupplier = supplierRelations.stream()
            .filter(relation -> Boolean.TRUE.equals(relation.isDefault()))
            .findFirst()
            .orElse(null);
        return new V2ProductDtos.ProductResponse(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getCategoryId(),
            category == null ? entity.getCategory() : category.getName(),
            entity.getUnitId(),
            unit == null ? entity.getUnit() : unit.getName(),
            entity.getSalePrice(),
            entity.getPurchasePrice(),
            priceLevelService.toValueResponses(entity.getPriceLevelValuesJson(), priceLevelDefinitionsById),
            defaultSupplier,
            supplierRelations,
            entity.getStock(),
            entity.getSafeStock(),
            entity.getStatus(),
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

    private Double normalizeNonNegative(Double value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value < 0.0) {
            throw new IllegalArgumentException("数值不能小于0");
        }
        return value;
    }

    private Integer normalizeStatus(Integer status, String message) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException(message);
        }
        return status;
    }
}
