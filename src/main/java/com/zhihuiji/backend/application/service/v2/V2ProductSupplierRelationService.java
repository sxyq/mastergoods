package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.ProductSupplierRelationEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductSupplierRelationRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2ProductSupplierRelationService {
    private final ProductSupplierRelationRepository productSupplierRelationRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2ProductSupplierRelationService(
        ProductSupplierRelationRepository productSupplierRelationRepository,
        ProductRepository productRepository,
        SupplierRepository supplierRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.productSupplierRelationRepository = productSupplierRelationRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2ProductDtos.ProductSupplierRelationResponse> list(Long productId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        requireOwnedProduct(ownerUserId, productId);
        return buildResponses(
            productSupplierRelationRepository.findAllByOwnerUserIdAndProductIdOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(
                ownerUserId,
                productId
            )
        );
    }

    public Map<Long, List<V2ProductDtos.ProductSupplierRelationResponse>> listByProductIds(Collection<Long> productIds) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductSupplierRelationEntity> relations = productSupplierRelationRepository
            .findAllByOwnerUserIdAndProductIdInOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(ownerUserId, productIds);
        Map<Long, SupplierEntity> suppliersById = loadSuppliers(relations);
        return relations.stream()
            .collect(Collectors.groupingBy(
                ProductSupplierRelationEntity::getProductId,
                java.util.LinkedHashMap::new,
                Collectors.mapping(relation -> toResponse(relation, suppliersById.get(relation.getSupplierId())), Collectors.toList()))
            );
    }

    @Transactional
    public V2ProductDtos.ProductSupplierRelationResponse create(V2ProductDtos.ProductSupplierRelationWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductEntity product = requireOwnedProduct(ownerUserId, request.productId());
        SupplierEntity supplier = requireOwnedSupplier(ownerUserId, request.supplierId());
        if (productSupplierRelationRepository.existsByOwnerUserIdAndProductIdAndSupplierId(
            ownerUserId,
            product.getId(),
            supplier.getId()
        )) {
            throw new IllegalArgumentException("商品供应商关系已存在");
        }
        ProductSupplierRelationEntity entity = new ProductSupplierRelationEntity();
        fillEntity(ownerUserId, entity, request, product.getId(), supplier.getId(), true);
        ProductSupplierRelationEntity saved = productSupplierRelationRepository.save(entity);
        return toResponse(saved, supplier);
    }

    @Transactional
    public V2ProductDtos.ProductSupplierRelationResponse update(Long id, V2ProductDtos.ProductSupplierRelationWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductSupplierRelationEntity entity = getOwnedEntity(id);
        ProductEntity product = requireOwnedProduct(ownerUserId, request.productId());
        SupplierEntity supplier = requireOwnedSupplier(ownerUserId, request.supplierId());
        if (productSupplierRelationRepository.existsByOwnerUserIdAndProductIdAndSupplierIdAndIdNot(
            ownerUserId,
            product.getId(),
            supplier.getId(),
            id
        )) {
            throw new IllegalArgumentException("商品供应商关系已存在");
        }
        fillEntity(ownerUserId, entity, request, product.getId(), supplier.getId(), false);
        ProductSupplierRelationEntity saved = productSupplierRelationRepository.save(entity);
        return toResponse(saved, supplier);
    }

    @Transactional
    public void replaceForProduct(Long productId, List<V2ProductDtos.ProductSupplierRelationWriteRequest> requests) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        requireOwnedProduct(ownerUserId, productId);
        List<V2ProductDtos.ProductSupplierRelationWriteRequest> normalizedRequests = requests == null ? List.of() : requests;
        validateRequests(ownerUserId, productId, normalizedRequests, null);
        productSupplierRelationRepository.deleteAllByOwnerUserIdAndProductId(ownerUserId, productId);
        long now = System.currentTimeMillis();
        for (V2ProductDtos.ProductSupplierRelationWriteRequest request : normalizedRequests) {
            ProductSupplierRelationEntity entity = new ProductSupplierRelationEntity();
            entity.setOwnerUserId(ownerUserId);
            entity.setProductId(productId);
            entity.setSupplierId(request.supplierId());
            entity.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
            entity.setPurchasePriority(normalizePurchasePriority(request.purchasePriority()));
            entity.setLastPurchasePrice(normalizeNullablePrice(request.lastPurchasePrice()));
            entity.setNotes(normalizeNullable(request.notes()));
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            productSupplierRelationRepository.save(entity);
        }
    }

    public void delete(Long id) {
        productSupplierRelationRepository.delete(getOwnedEntity(id));
    }

    public ProductSupplierRelationEntity getOwnedEntity(Long id) {
        return productSupplierRelationRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("商品供应商关系不存在"));
    }

    private void fillEntity(
        Long ownerUserId,
        ProductSupplierRelationEntity entity,
        V2ProductDtos.ProductSupplierRelationWriteRequest request,
        Long productId,
        Long supplierId,
        boolean isCreate
    ) {
        validateRequests(ownerUserId, productId, List.of(request), isCreate ? null : entity.getId());
        entity.setOwnerUserId(ownerUserId);
        entity.setProductId(productId);
        entity.setSupplierId(supplierId);
        entity.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        entity.setPurchasePriority(normalizePurchasePriority(request.purchasePriority()));
        entity.setLastPurchasePrice(normalizeNullablePrice(request.lastPurchasePrice()));
        entity.setNotes(normalizeNullable(request.notes()));
        long now = System.currentTimeMillis();
        if (isCreate) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        if (Boolean.TRUE.equals(entity.getIsDefault())) {
            clearDefaultSupplier(ownerUserId, productId, entity.getId());
        }
    }

    private void clearDefaultSupplier(Long ownerUserId, Long productId, Long keepId) {
        productSupplierRelationRepository
            .findAllByOwnerUserIdAndProductIdOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(ownerUserId, productId)
            .stream()
            .filter(relation -> !relation.getId().equals(keepId))
            .filter(relation -> Boolean.TRUE.equals(relation.getIsDefault()))
            .forEach(relation -> {
                relation.setIsDefault(false);
                relation.setUpdatedAt(System.currentTimeMillis());
                productSupplierRelationRepository.save(relation);
            });
    }

    private void validateRequests(
        Long ownerUserId,
        Long productId,
        List<V2ProductDtos.ProductSupplierRelationWriteRequest> requests,
        Long currentRelationId
    ) {
        Set<Long> supplierIds = new LinkedHashSet<>();
        long defaultCount = 0L;
        for (V2ProductDtos.ProductSupplierRelationWriteRequest request : requests) {
            if (request == null || request.productId() == null || request.supplierId() == null) {
                throw new IllegalArgumentException("商品供应商关系参数不完整");
            }
            if (!productId.equals(request.productId())) {
                throw new IllegalArgumentException("商品供应商关系必须归属于同一个商品");
            }
            if (!supplierIds.add(request.supplierId())) {
                throw new IllegalArgumentException("同一商品不能重复关联同一个供应商");
            }
            if (Boolean.TRUE.equals(request.isDefault())) {
                defaultCount++;
            }
            normalizePurchasePriority(request.purchasePriority());
            normalizeNullablePrice(request.lastPurchasePrice());
            requireOwnedSupplier(ownerUserId, request.supplierId());
        }
        if (defaultCount > 1) {
            throw new IllegalArgumentException("同一个商品只能设置一个默认供应商");
        }
        if (requests.size() == 1 && currentRelationId != null) {
            V2ProductDtos.ProductSupplierRelationWriteRequest request = requests.get(0);
            if (productSupplierRelationRepository.existsByOwnerUserIdAndProductIdAndSupplierIdAndIdNot(
                ownerUserId,
                productId,
                request.supplierId(),
                currentRelationId
            )) {
                throw new IllegalArgumentException("商品供应商关系已存在");
            }
        }
    }

    private ProductEntity requireOwnedProduct(Long ownerUserId, Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("商品不能为空");
        }
        return productRepository.findByIdAndOwnerUserId(productId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    }

    private SupplierEntity requireOwnedSupplier(Long ownerUserId, Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("供应商不能为空");
        }
        return supplierRepository.findByIdAndOwnerUserId(supplierId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
    }

    private List<V2ProductDtos.ProductSupplierRelationResponse> buildResponses(List<ProductSupplierRelationEntity> relations) {
        Map<Long, SupplierEntity> suppliersById = loadSuppliers(relations);
        return relations.stream()
            .map(relation -> toResponse(relation, suppliersById.get(relation.getSupplierId())))
            .toList();
    }

    private Map<Long, SupplierEntity> loadSuppliers(List<ProductSupplierRelationEntity> relations) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Set<Long> supplierIds = relations.stream()
            .map(ProductSupplierRelationEntity::getSupplierId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (supplierIds.isEmpty()) {
            return Map.of();
        }
        return supplierRepository.findAllByOwnerUserIdAndIdIn(ownerUserId, supplierIds).stream()
            .collect(Collectors.toMap(SupplierEntity::getId, value -> value));
    }

    private V2ProductDtos.ProductSupplierRelationResponse toResponse(
        ProductSupplierRelationEntity entity,
        SupplierEntity supplier
    ) {
        return new V2ProductDtos.ProductSupplierRelationResponse(
            entity.getId(),
            entity.getProductId(),
            entity.getSupplierId(),
            supplier == null ? null : supplier.getName(),
            supplier == null ? null : supplier.getPhone(),
            entity.getIsDefault(),
            entity.getPurchasePriority(),
            entity.getLastPurchasePrice(),
            entity.getNotes(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private Integer normalizePurchasePriority(Integer purchasePriority) {
        if (purchasePriority == null) {
            return 0;
        }
        if (purchasePriority < 0) {
            throw new IllegalArgumentException("采购优先级不能小于0");
        }
        return purchasePriority;
    }

    private Double normalizeNullablePrice(Double price) {
        if (price == null) {
            return null;
        }
        if (price < 0.0) {
            throw new IllegalArgumentException("最近采购价不能小于0");
        }
        return price;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
