package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductCategoryEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import com.zhihuiji.backend.domain.entity.ProductUnitEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
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

    @Transactional(readOnly = true)
    public List<V2ProductDtos.ProductResponse> list(String keyword, Integer status, Long categoryId, Long unitId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedKeyword = normalizeKeyword(keyword);
        List<ProductEntity> products = normalizedKeyword == null
            ? productRepository.findAllByOwnerUserIdAndFiltersOrderByUpdatedAtDesc(ownerUserId, status, categoryId, unitId)
            : productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(
                ownerUserId,
                normalizedKeyword,
                status,
                categoryId,
                unitId
            );
        if (products.isEmpty()) {
            return List.of();
        }
        ProductResponseContext context = ProductResponseContext.from(
            products,
            categoryService,
            unitService,
            priceLevelService,
            supplierRelationService
        );
        return products.stream().map(product -> toResponse(product, context)).toList();
    }

    @Transactional(readOnly = true)
    public List<V2ProductDtos.ProductResponse> lowStock(Integer size) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        int limit = size == null || size <= 0 ? 20 : Math.min(size, 100);
        List<ProductEntity> products = productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, limit));
        if (products.isEmpty()) {
            return List.of();
        }
        ProductResponseContext context = ProductResponseContext.from(
            products,
            categoryService,
            unitService,
            priceLevelService,
            supplierRelationService
        );
        return products.stream().map(product -> toResponse(product, context)).toList();
    }

    @Transactional(readOnly = true)
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
        return toResponse(
            entity,
            ProductResponseContext.single(
                entity,
                categoryService,
                unitService,
                priceLevelService,
                supplierRelationService
            )
        );
    }

    private V2ProductDtos.ProductResponse toResponse(ProductEntity entity, ProductResponseContext context) {
        Map<Long, ProductCategoryEntity> categoriesById = context.categoriesById();
        Map<Long, ProductUnitEntity> unitsById = context.unitsById();
        Map<Long, ProductPriceLevelEntity> priceLevelDefinitionsById = context.priceLevelDefinitionsById();
        List<V2ProductDtos.ProductSupplierRelationResponse> supplierRelations = context.supplierRelationsByProductId()
            .getOrDefault(entity.getId(), List.of());
        ProductCategoryEntity category = entity.getCategoryId() == null ? null : categoriesById.get(entity.getCategoryId());
        ProductUnitEntity unit = entity.getUnitId() == null ? null : unitsById.get(entity.getUnitId());
        V2ProductDtos.ProductSupplierRelationResponse defaultSupplier = null;
        for (V2ProductDtos.ProductSupplierRelationResponse relation : supplierRelations) {
            if (Boolean.TRUE.equals(relation.isDefault())) {
                defaultSupplier = relation;
                break;
            }
        }
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

    private record ProductResponseContext(
        Map<Long, ProductCategoryEntity> categoriesById,
        Map<Long, ProductUnitEntity> unitsById,
        Map<Long, ProductPriceLevelEntity> priceLevelDefinitionsById,
        Map<Long, List<V2ProductDtos.ProductSupplierRelationResponse>> supplierRelationsByProductId
    ) {
        static ProductResponseContext from(
            List<ProductEntity> products,
            V2ProductCategoryService categoryService,
            V2ProductUnitService unitService,
            V2ProductPriceLevelService priceLevelService,
            V2ProductSupplierRelationService supplierRelationService
        ) {
            Set<Long> categoryIds = new LinkedHashSet<>();
            Set<Long> unitIds = new LinkedHashSet<>();
            Set<Long> priceLevelIds = new LinkedHashSet<>();
            Set<Long> productIds = new LinkedHashSet<>();
            for (ProductEntity product : products) {
                if (product.getId() != null && product.getId() > 0L) {
                    productIds.add(product.getId());
                }
                if (product.getCategoryId() != null && product.getCategoryId() > 0L) {
                    categoryIds.add(product.getCategoryId());
                }
                if (product.getUnitId() != null && product.getUnitId() > 0L) {
                    unitIds.add(product.getUnitId());
                }
                priceLevelIds.addAll(priceLevelService.extractLevelIds(product.getPriceLevelValuesJson()));
            }
            Map<Long, ProductCategoryEntity> categoriesById = categoryService.getOwnedEntityMap(categoryIds);
            Map<Long, ProductUnitEntity> unitsById = unitService.getOwnedEntityMap(unitIds);
            Map<Long, ProductPriceLevelEntity> priceLevelDefinitionsById = priceLevelService.getOwnedEntityMap(priceLevelIds);
            Map<Long, List<V2ProductDtos.ProductSupplierRelationResponse>> supplierRelationsByProductId = productIds.isEmpty()
                ? Map.of()
                : supplierRelationService.listByProductIds(productIds);
            return new ProductResponseContext(categoriesById, unitsById, priceLevelDefinitionsById, supplierRelationsByProductId);
        }

        static ProductResponseContext single(
            ProductEntity product,
            V2ProductCategoryService categoryService,
            V2ProductUnitService unitService,
            V2ProductPriceLevelService priceLevelService,
            V2ProductSupplierRelationService supplierRelationService
        ) {
            List<ProductEntity> products = product == null ? List.of() : List.of(product);
            return from(products, categoryService, unitService, priceLevelService, supplierRelationService);
        }
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

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
