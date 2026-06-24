package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductPriceLevelRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class V2ProductPriceLevelService {
    private final ProductPriceLevelRepository productPriceLevelRepository;
    private final ProductRepository productRepository;
    private final CurrentOwnerService currentOwnerService;
    private final ObjectMapper objectMapper;

    public V2ProductPriceLevelService(
        ProductPriceLevelRepository productPriceLevelRepository,
        ProductRepository productRepository,
        CurrentOwnerService currentOwnerService,
        ObjectMapper objectMapper
    ) {
        this.productPriceLevelRepository = productPriceLevelRepository;
        this.productRepository = productRepository;
        this.currentOwnerService = currentOwnerService;
        this.objectMapper = objectMapper;
    }

    public List<V2ProductDtos.PriceLevelResponse> list() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<ProductPriceLevelEntity> rows = productPriceLevelRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId);
        List<V2ProductDtos.PriceLevelResponse> responses = new ArrayList<>(rows.size());
        for (ProductPriceLevelEntity row : rows) {
            responses.add(toResponse(row));
        }
        return responses;
    }

    public ProductPriceLevelEntity getOwnedEntity(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return productPriceLevelRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("商品价格层级不存在"));
    }

    public Map<Long, ProductPriceLevelEntity> getOwnedEntityMap(Collection<Long> ids) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProductPriceLevelEntity> levelsById = new LinkedHashMap<>(ids.size());
        for (ProductPriceLevelEntity level : productPriceLevelRepository.findAllByOwnerUserIdAndIdIn(ownerUserId, ids)) {
            levelsById.put(level.getId(), level);
        }
        return levelsById;
    }

    public V2ProductDtos.PriceLevelResponse create(V2ProductDtos.PriceLevelWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String code = normalizeRequired(request.code(), "价格层级编码不能为空");
        String name = normalizeRequired(request.name(), "价格层级名称不能为空");
        if (productPriceLevelRepository.existsByOwnerUserIdAndCode(ownerUserId, code)) {
            throw new IllegalArgumentException("价格层级编码已存在");
        }
        if (productPriceLevelRepository.existsByOwnerUserIdAndName(ownerUserId, name)) {
            throw new IllegalArgumentException("价格层级名称已存在");
        }
        long now = System.currentTimeMillis();
        ProductPriceLevelEntity entity = new ProductPriceLevelEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setCode(code);
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(productPriceLevelRepository.save(entity));
    }

    public V2ProductDtos.PriceLevelResponse update(Long id, V2ProductDtos.PriceLevelWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ProductPriceLevelEntity entity = getOwnedEntity(id);
        String code = normalizeRequired(request.code(), "价格层级编码不能为空");
        String name = normalizeRequired(request.name(), "价格层级名称不能为空");
        if (productPriceLevelRepository.existsByOwnerUserIdAndCodeAndIdNot(ownerUserId, code, id)) {
            throw new IllegalArgumentException("价格层级编码已存在");
        }
        if (productPriceLevelRepository.existsByOwnerUserIdAndNameAndIdNot(ownerUserId, name, id)) {
            throw new IllegalArgumentException("价格层级名称已存在");
        }
        entity.setCode(code);
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setUpdatedAt(System.currentTimeMillis());
        return toResponse(productPriceLevelRepository.save(entity));
    }

    public void delete(Long id) {
        ProductPriceLevelEntity entity = getOwnedEntity(id);
        if (isReferencedByAnyProduct(entity.getId())) {
            throw new IllegalArgumentException("价格层级已被商品引用，无法删除");
        }
        productPriceLevelRepository.delete(entity);
    }

    public String encodeValueSnapshot(List<V2ProductDtos.ProductPriceValueWriteRequest> values) {
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (V2ProductDtos.ProductPriceValueWriteRequest value : values) {
            if (value == null || value.levelId() == null) {
                throw new IllegalArgumentException("商品价格层级不能为空");
            }
            if (!uniqueIds.add(value.levelId())) {
                throw new IllegalArgumentException("商品价格层级不能重复");
            }
            if (value.price() == null || value.price() < 0.0) {
                throw new IllegalArgumentException("价格层级金额不能小于0");
            }
        }
        Map<Long, ProductPriceLevelEntity> definitions = getOwnedEntityMap(uniqueIds);
        if (definitions.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("存在无效的价格层级");
        }
        Map<Long, Double> pricesByLevelId = new LinkedHashMap<>(values.size());
        for (V2ProductDtos.ProductPriceValueWriteRequest value : values) {
            pricesByLevelId.put(value.levelId(), value.price());
        }
        List<StoredProductPriceValue> storedValues = new ArrayList<>(uniqueIds.size());
        for (Long levelId : uniqueIds) {
            storedValues.add(new StoredProductPriceValue(levelId, pricesByLevelId.get(levelId)));
        }
        try {
            return objectMapper.writeValueAsString(storedValues);
        } catch (Exception exception) {
            throw new IllegalStateException("商品价格层级序列化失败", exception);
        }
    }

    public Set<Long> extractLevelIds(String priceLevelValuesJson) {
        if (priceLevelValuesJson == null || priceLevelValuesJson.isBlank()) {
            return Set.of();
        }
        try {
            StoredProductPriceValue[] values = objectMapper.readValue(priceLevelValuesJson, StoredProductPriceValue[].class);
            Set<Long> levelIds = new LinkedHashSet<>(values.length);
            for (StoredProductPriceValue value : values) {
                if (value.levelId() != null) {
                    levelIds.add(value.levelId());
                }
            }
            return levelIds;
        } catch (Exception exception) {
            throw new IllegalStateException("商品价格层级反序列化失败", exception);
        }
    }

    public List<V2ProductDtos.ProductPriceValueResponse> toValueResponses(
        String priceLevelValuesJson,
        Map<Long, ProductPriceLevelEntity> definitionsById
    ) {
        if (priceLevelValuesJson == null || priceLevelValuesJson.isBlank()) {
            return List.of();
        }
        try {
            StoredProductPriceValue[] values = objectMapper.readValue(priceLevelValuesJson, StoredProductPriceValue[].class);
            List<V2ProductDtos.ProductPriceValueResponse> responses = new ArrayList<>(values.length);
            for (StoredProductPriceValue value : values) {
                ProductPriceLevelEntity definition = definitionsById.get(value.levelId());
                if (definition == null) {
                    continue;
                }
                responses.add(new V2ProductDtos.ProductPriceValueResponse(
                    definition.getId(),
                    definition.getCode(),
                    definition.getName(),
                    value.price(),
                    definition.getStatus(),
                    definition.getSortOrder()
                ));
            }
            responses.sort(java.util.Comparator
                .comparing(V2ProductDtos.ProductPriceValueResponse::sortOrder)
                .thenComparing(V2ProductDtos.ProductPriceValueResponse::name));
            return responses;
        } catch (Exception exception) {
            throw new IllegalStateException("商品价格层级反序列化失败", exception);
        }
    }

    private boolean isReferencedByAnyProduct(Long levelId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        for (ProductEntity product : productRepository.findAllByOwnerUserId(ownerUserId)) {
            String priceLevelValuesJson = product.getPriceLevelValuesJson();
            if (priceLevelValuesJson != null && !priceLevelValuesJson.isBlank() && extractLevelIds(priceLevelValuesJson).contains(levelId)) {
                return true;
            }
        }
        return false;
    }

    private V2ProductDtos.PriceLevelResponse toResponse(ProductPriceLevelEntity entity) {
        return new V2ProductDtos.PriceLevelResponse(
            entity.getId(),
            entity.getCode(),
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
            throw new IllegalArgumentException("价格层级状态不合法");
        }
        return status;
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private record StoredProductPriceValue(Long levelId, Double price) {}
}
