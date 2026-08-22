package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.PurchaseReturnStatus;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReturnDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReturnEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReturnItemEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReturnRefundEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRefundRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2PurchaseReturnService {
    private static final double EPSILON = 0.000001;

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnItemRepository purchaseReturnItemRepository;
    private final PurchaseReturnRefundRepository purchaseReturnRefundRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentOwnerService currentOwnerService;
    private final IdGenerator idGenerator;

    public V2PurchaseReturnService(
        PurchaseReturnRepository purchaseReturnRepository,
        PurchaseReturnItemRepository purchaseReturnItemRepository,
        PurchaseReturnRefundRepository purchaseReturnRefundRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        PurchaseOrderItemRepository purchaseOrderItemRepository,
        ProductRepository productRepository,
        SupplierRepository supplierRepository,
        CurrentOwnerService currentOwnerService,
        IdGenerator idGenerator
    ) {
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.purchaseReturnItemRepository = purchaseReturnItemRepository;
        this.purchaseReturnRefundRepository = purchaseReturnRefundRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.currentOwnerService = currentOwnerService;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public V2PurchaseReturnDtos.PurchaseReturnResponse create(V2PurchaseReturnDtos.CreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("退货明细不能为空");
        }

        long now = System.currentTimeMillis();
        long returnId = idGenerator.nextId();
        String returnNo = "PR" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        PurchaseOrderEntity purchaseOrder = null;
        Map<String, PurchaseOrderItemEntity> orderItemMap = Map.of();
        Map<String, Double> reservedQuantityMap = Map.of();
        if (request.purchaseOrderId() != null) {
            purchaseOrder = purchaseOrderRepository.findByIdAndOwnerUserId(request.purchaseOrderId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("采购订单不存在"));
            orderItemMap = buildOrderItemMap(purchaseOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, purchaseOrder.getId()));
            reservedQuantityMap = buildReservedQuantityMap(ownerUserId, purchaseOrder.getId());
        }
        SupplierEntity supplier = resolveSupplier(ownerUserId, request, purchaseOrder);

        double total = 0.0;
        List<PurchaseReturnItemEntity> itemEntities = new ArrayList<>(request.items().size());
        for (V2PurchaseReturnDtos.CreateItemRequest item : request.items()) {
            ProductEntity product = resolveProduct(ownerUserId, item);
            double quantity = item.quantity() == null ? 0.0 : item.quantity();
            if (quantity <= 0.0) {
                throw new IllegalArgumentException("退货数量必须大于0");
            }

            PurchaseOrderItemEntity sourceItem = null;
            if (purchaseOrder != null) {
                sourceItem = findSourceItem(orderItemMap, item, product.getId());
                if (sourceItem == null) {
                    throw new IllegalArgumentException("退货商品不在来源采购单中");
                }
                double reservedQuantity = reservedQuantityMap.getOrDefault(productKey(sourceItem.getProductId(), sourceItem.getProductCode()), 0.0);
                double availableQuantity = Math.max(0.0, sourceItem.getQuantity() - reservedQuantity);
                if (quantity > availableQuantity + EPSILON) {
                    throw new IllegalArgumentException("商品 " + product.getName() + " 超出可退数量");
                }
            }

            double unitCost = item.unitCost() != null
                ? item.unitCost()
                : (sourceItem != null ? sourceItem.getUnitCost() : product.getPurchasePrice());
            if (unitCost < 0.0) {
                throw new IllegalArgumentException("退货单价不能为负数");
            }
            double amount = quantity * unitCost;
            total += amount;

            PurchaseReturnItemEntity entity = new PurchaseReturnItemEntity();
            entity.setId(idGenerator.nextId());
            entity.setOwnerUserId(ownerUserId);
            entity.setReturnId(returnId);
            entity.setProductId(product.getId());
            entity.setProductCode(product.getCode());
            entity.setProductName(
                item.productName() == null || item.productName().isBlank()
                    ? product.getName()
                    : item.productName()
            );
            entity.setQuantity(quantity);
            entity.setUnitCost(unitCost);
            entity.setAmount(amount);
            entity.setCreatedAt(now);
            itemEntities.add(entity);
        }

        PurchaseReturnEntity returnEntity = new PurchaseReturnEntity();
        returnEntity.setId(returnId);
        returnEntity.setOwnerUserId(ownerUserId);
        returnEntity.setReturnNo(returnNo);
        returnEntity.setPurchaseOrderId(request.purchaseOrderId());
        returnEntity.setSupplierId(supplier.getId());
        returnEntity.setSupplierName(supplier.getName());
        returnEntity.setTotalAmount(total);
        returnEntity.setRefundAmount(0.0);
        returnEntity.setStatus(PurchaseReturnStatus.DRAFT.code());
        returnEntity.setNotes(request.notes());
        returnEntity.setCreatedAt(now);
        returnEntity.setUpdatedAt(now);
        purchaseReturnRepository.save(returnEntity);
        purchaseReturnItemRepository.saveAll(itemEntities);

        return toResponse(returnEntity, itemEntities, List.of());
    }

    @Transactional(readOnly = true)
    public List<V2PurchaseReturnDtos.PurchaseReturnResponse> list(String keyword, Integer status) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedKeyword = normalizeKeyword(keyword);
        List<PurchaseReturnEntity> returns = normalizedKeyword == null
            ? listWithoutKeyword(ownerUserId, status)
            : purchaseReturnRepository.search(ownerUserId, normalizedKeyword, status);
        return toResponses(ownerUserId, returns);
    }

    @Transactional(readOnly = true)
    public List<V2PurchaseReturnDtos.PurchaseReturnResponse> list(
        String keyword,
        Integer status,
        Pageable pageable
    ) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedKeyword = normalizeKeyword(keyword);
        List<PurchaseReturnEntity> returns = normalizedKeyword == null
            ? listWithoutKeyword(ownerUserId, status, pageable)
            : status == null
                ? purchaseReturnRepository.searchByKeyword(ownerUserId, normalizedKeyword, pageable)
                : purchaseReturnRepository.searchByKeywordAndStatus(ownerUserId, normalizedKeyword, status, pageable);
        return toResponses(ownerUserId, returns);
    }

    @Transactional(readOnly = true)
    public V2PurchaseReturnDtos.PurchaseReturnResponse get(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReturnEntity entity = purchaseReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购退货单不存在"));
        return toResponse(ownerUserId, entity);
    }

    @Transactional(readOnly = true)
    public List<V2PurchaseReturnDtos.PurchaseReturnResponse> listByOrder(Long purchaseOrderId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<PurchaseReturnEntity> returns = purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(ownerUserId, purchaseOrderId);
        return toResponses(ownerUserId, returns);
    }

    @Transactional
    public V2PurchaseReturnDtos.PurchaseReturnResponse updateDraft(Long id, V2PurchaseReturnDtos.UpdateDraftRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReturnEntity entity = purchaseReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购退货单不存在"));
        if (entity.getStatus() != PurchaseReturnStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        entity.setNotes(request.notes());
        entity.setUpdatedAt(System.currentTimeMillis());
        purchaseReturnRepository.save(entity);
        return toResponse(ownerUserId, entity);
    }

    @Transactional
    public V2PurchaseReturnDtos.PurchaseReturnResponse confirm(Long id, V2PurchaseReturnDtos.ConfirmRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReturnEntity entity = purchaseReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购退货单不存在"));
        if (entity.getStatus() != PurchaseReturnStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可确认");
        }

        long now = System.currentTimeMillis();
        List<PurchaseReturnItemEntity> items = purchaseReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id);
        for (PurchaseReturnItemEntity item : items) {
            ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.getProductId()));
            if (product.getStock() < item.getQuantity() - EPSILON) {
                throw new IllegalArgumentException("商品库存不足，无法退回供应商: " + product.getName());
            }
            product.setStock(Math.max(0.0, product.getStock() - item.getQuantity()));
            product.setUpdatedAt(now);
            product.setSyncStatus(0);
            product.setSyncVersion(product.getSyncVersion() + 1);
            productRepository.save(product);
        }

        if (entity.getPurchaseOrderId() != null) {
            PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndOwnerUserId(entity.getPurchaseOrderId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("采购订单不存在"));
            order.setReceivedAmount(Math.max(0.0, order.getReceivedAmount() - entity.getTotalAmount()));
            order.setUpdatedAt(now);
            order.setSyncStatus(0);
            order.setSyncVersion(order.getSyncVersion() + 1);
            purchaseOrderRepository.save(order);
        }

        if (entity.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepository.findByIdAndOwnerUserId(entity.getSupplierId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
            supplier.setBalance(Math.max(0.0, supplier.getBalance() - entity.getTotalAmount()));
            supplier.setUpdatedAt(now);
            supplier.setSyncStatus(0);
            supplier.setSyncVersion(supplier.getSyncVersion() + 1);
            supplierRepository.save(supplier);
        }

        entity.setStatus(PurchaseReturnStatus.CONFIRMED.code());
        if (request.notes() != null) {
            entity.setNotes(request.notes());
        }
        entity.setUpdatedAt(now);
        purchaseReturnRepository.save(entity);
        return toResponse(entity, items, purchaseReturnRefundRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    @Transactional
    public V2PurchaseReturnDtos.PurchaseReturnResponse addRefund(Long id, V2PurchaseReturnDtos.RefundRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReturnEntity entity = purchaseReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购退货单不存在"));
        if (entity.getStatus() == PurchaseReturnStatus.CANCELLED.code()) {
            throw new IllegalArgumentException("已取消退货单不可登记退款");
        }
        double remaining = entity.getTotalAmount() - entity.getRefundAmount();
        if (request.amount() == null || request.amount() <= 0.0 || request.amount() > remaining + EPSILON) {
            throw new IllegalArgumentException("退款金额无效");
        }

        long now = System.currentTimeMillis();
        PurchaseReturnRefundEntity refund = new PurchaseReturnRefundEntity();
        refund.setId(idGenerator.nextId());
        refund.setOwnerUserId(ownerUserId);
        refund.setReturnId(id);
        refund.setAmount(request.amount());
        refund.setMethod(request.method());
        refund.setReferenceNo(request.referenceNo());
        refund.setCreatedAt(now);
        purchaseReturnRefundRepository.save(refund);

        entity.setRefundAmount(entity.getRefundAmount() + request.amount());
        if (Math.abs(entity.getRefundAmount() - entity.getTotalAmount()) < EPSILON || entity.getRefundAmount() > entity.getTotalAmount()) {
            entity.setStatus(PurchaseReturnStatus.COMPLETED.code());
        }
        entity.setUpdatedAt(now);
        purchaseReturnRepository.save(entity);
        return toResponse(ownerUserId, entity);
    }

    @Transactional
    public V2PurchaseReturnDtos.PurchaseReturnResponse cancel(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReturnEntity entity = purchaseReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购退货单不存在"));
        if (entity.getStatus() == PurchaseReturnStatus.CANCELLED.code()) {
            return toResponse(ownerUserId, entity);
        }

        if (entity.getStatus() == PurchaseReturnStatus.CONFIRMED.code() || entity.getStatus() == PurchaseReturnStatus.COMPLETED.code()) {
            long now = System.currentTimeMillis();
            List<PurchaseReturnItemEntity> items = purchaseReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id);
            for (PurchaseReturnItemEntity item : items) {
                ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.getProductId()));
                product.setStock(product.getStock() + item.getQuantity());
                product.setUpdatedAt(now);
                product.setSyncStatus(0);
                product.setSyncVersion(product.getSyncVersion() + 1);
                productRepository.save(product);
            }
            if (entity.getPurchaseOrderId() != null) {
                PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndOwnerUserId(entity.getPurchaseOrderId(), ownerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("采购订单不存在"));
                order.setReceivedAmount(order.getReceivedAmount() + entity.getTotalAmount());
                order.setUpdatedAt(now);
                order.setSyncStatus(0);
                order.setSyncVersion(order.getSyncVersion() + 1);
                purchaseOrderRepository.save(order);
            }
            if (entity.getSupplierId() != null) {
                SupplierEntity supplier = supplierRepository.findByIdAndOwnerUserId(entity.getSupplierId(), ownerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
                supplier.setBalance(supplier.getBalance() + entity.getTotalAmount());
                supplier.setUpdatedAt(now);
                supplier.setSyncStatus(0);
                supplier.setSyncVersion(supplier.getSyncVersion() + 1);
                supplierRepository.save(supplier);
            }
        }
        entity.setStatus(PurchaseReturnStatus.CANCELLED.code());
        entity.setUpdatedAt(System.currentTimeMillis());
        purchaseReturnRepository.save(entity);
        return toResponse(ownerUserId, entity);
    }

    private SupplierEntity resolveSupplier(
        Long ownerUserId,
        V2PurchaseReturnDtos.CreateRequest request,
        PurchaseOrderEntity purchaseOrder
    ) {
        Long resolvedSupplierId = request.supplierId();
        if (purchaseOrder != null) {
            if (purchaseOrder.getSupplierId() == null) {
                throw new IllegalArgumentException("采购订单缺少供应商信息");
            }
            if (resolvedSupplierId != null && !resolvedSupplierId.equals(purchaseOrder.getSupplierId())) {
                throw new IllegalArgumentException("退货供应商与采购订单不一致");
            }
            resolvedSupplierId = purchaseOrder.getSupplierId();
        }
        if (resolvedSupplierId == null) {
            throw new IllegalArgumentException("采购退货单必须关联供应商");
        }
        return supplierRepository.findByIdAndOwnerUserId(resolvedSupplierId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
    }

    private ProductEntity resolveProduct(Long ownerUserId, V2PurchaseReturnDtos.CreateItemRequest item) {
        if (item.productId() != null && item.productId() > 0L) {
            return productRepository.findByIdForUpdate(ownerUserId, item.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productId()));
        }
        if (item.productCode() != null && !item.productCode().isBlank()) {
            return productRepository.findByCodeForUpdate(ownerUserId, item.productCode())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productCode()));
        }
        throw new IllegalArgumentException("退货明细缺少商品标识");
    }

    private PurchaseOrderItemEntity findSourceItem(
        Map<String, PurchaseOrderItemEntity> orderItemMap,
        V2PurchaseReturnDtos.CreateItemRequest item,
        Long productId
    ) {
        PurchaseOrderItemEntity sourceById = productId == null ? null : orderItemMap.get(productKey(productId, null));
        if (sourceById != null) {
            return sourceById;
        }
        if (item.productCode() != null && !item.productCode().isBlank()) {
            return orderItemMap.get(productKey(null, item.productCode()));
        }
        return null;
    }

    private Map<String, PurchaseOrderItemEntity> buildOrderItemMap(List<PurchaseOrderItemEntity> items) {
        Map<String, PurchaseOrderItemEntity> map = new HashMap<>(Math.max(4, items.size() * 2));
        for (PurchaseOrderItemEntity item : items) {
            map.put(productKey(item.getProductId(), null), item);
            if (item.getProductCode() != null && !item.getProductCode().isBlank()) {
                map.put(productKey(null, item.getProductCode()), item);
            }
        }
        return map;
    }

    private Map<String, Double> buildReservedQuantityMap(Long ownerUserId, Long purchaseOrderId) {
        List<PurchaseReturnEntity> returns = purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdAndStatusInOrderByCreatedAtDesc(
            ownerUserId,
            purchaseOrderId,
            List.of(
                PurchaseReturnStatus.DRAFT.code(),
                PurchaseReturnStatus.CONFIRMED.code(),
                PurchaseReturnStatus.COMPLETED.code()
            )
        );
        Map<String, Double> reserved = new HashMap<>();
        if (returns.isEmpty()) {
            return reserved;
        }
        List<Long> returnIds = returns.stream().map(PurchaseReturnEntity::getId).toList();
        List<PurchaseReturnItemEntity> allItems = new ArrayList<>(
            purchaseReturnItemRepository.findAllByOwnerUserIdAndReturnIdIn(ownerUserId, returnIds)
        );
        for (PurchaseReturnItemEntity item : allItems) {
            String key = productKey(item.getProductId(), item.getProductCode());
            reserved.put(key, reserved.getOrDefault(key, 0.0) + safeDouble(item.getQuantity()));
        }
        return reserved;
    }

    private V2PurchaseReturnDtos.PurchaseReturnResponse toResponse(Long ownerUserId, PurchaseReturnEntity entity) {
        return toResponse(
            entity,
            purchaseReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, entity.getId()),
            purchaseReturnRefundRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, entity.getId())
        );
    }

    private List<V2PurchaseReturnDtos.PurchaseReturnResponse> toResponses(Long ownerUserId, List<PurchaseReturnEntity> returns) {
        if (returns.isEmpty()) {
            return List.of();
        }
        List<Long> returnIds = returns.stream().map(PurchaseReturnEntity::getId).toList();
        List<PurchaseReturnItemEntity> allItems = new ArrayList<>(
            purchaseReturnItemRepository.findAllByOwnerUserIdAndReturnIdIn(ownerUserId, returnIds)
        );
        allItems.sort(Comparator.comparing(PurchaseReturnItemEntity::getCreatedAt));
        Map<Long, List<PurchaseReturnItemEntity>> itemsByReturnId = new HashMap<>();
        for (PurchaseReturnItemEntity item : allItems) {
            itemsByReturnId.computeIfAbsent(item.getReturnId(), ignored -> new ArrayList<>()).add(item);
        }

        List<PurchaseReturnRefundEntity> allRefunds = new ArrayList<>(
            purchaseReturnRefundRepository.findAllByOwnerUserIdAndReturnIdIn(ownerUserId, returnIds)
        );
        allRefunds.sort(Comparator.comparing(PurchaseReturnRefundEntity::getCreatedAt));
        Map<Long, List<PurchaseReturnRefundEntity>> refundsByReturnId = new HashMap<>();
        for (PurchaseReturnRefundEntity refund : allRefunds) {
            refundsByReturnId.computeIfAbsent(refund.getReturnId(), ignored -> new ArrayList<>()).add(refund);
        }

        return returns.stream()
            .map(entity -> toResponse(
                entity,
                itemsByReturnId.getOrDefault(entity.getId(), List.of()),
                refundsByReturnId.getOrDefault(entity.getId(), List.of())
            ))
            .toList();
    }

    private V2PurchaseReturnDtos.PurchaseReturnResponse toResponse(
        PurchaseReturnEntity entity,
        List<PurchaseReturnItemEntity> items,
        List<PurchaseReturnRefundEntity> refunds
    ) {
        List<V2PurchaseReturnDtos.PurchaseReturnItemResponse> itemResponses = items.stream().map(this::toItemResponse).toList();
        List<V2PurchaseReturnDtos.PurchaseReturnRefundResponse> refundResponses = refunds.stream().map(this::toRefundResponse).toList();
        return new V2PurchaseReturnDtos.PurchaseReturnResponse(
            entity.getId(),
            entity.getReturnNo(),
            entity.getPurchaseOrderId(),
            entity.getSupplierId(),
            entity.getSupplierName(),
            itemResponses,
            refundResponses,
            entity.getTotalAmount(),
            entity.getRefundAmount(),
            entity.getStatus(),
            entity.getNotes(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private List<PurchaseReturnEntity> listWithoutKeyword(Long ownerUserId, Integer status) {
        if (status == null) {
            return purchaseReturnRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        }
        return purchaseReturnRepository.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(ownerUserId, status);
    }

    private List<PurchaseReturnEntity> listWithoutKeyword(Long ownerUserId, Integer status, Pageable pageable) {
        if (status == null) {
            return purchaseReturnRepository.findByOwnerUserIdOrderByCreatedAtDescIdDesc(ownerUserId, pageable);
        }
        return purchaseReturnRepository.findByOwnerUserIdAndStatusOrderByCreatedAtDescIdDesc(ownerUserId, status, pageable);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private V2PurchaseReturnDtos.PurchaseReturnItemResponse toItemResponse(PurchaseReturnItemEntity item) {
        return new V2PurchaseReturnDtos.PurchaseReturnItemResponse(
            item.getId(),
            item.getReturnId(),
            item.getProductId(),
            item.getProductCode(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitCost(),
            item.getAmount(),
            item.getCreatedAt()
        );
    }

    private V2PurchaseReturnDtos.PurchaseReturnRefundResponse toRefundResponse(PurchaseReturnRefundEntity refund) {
        return new V2PurchaseReturnDtos.PurchaseReturnRefundResponse(
            refund.getId(),
            refund.getReturnId(),
            refund.getAmount(),
            refund.getMethod(),
            refund.getReferenceNo(),
            refund.getCreatedAt()
        );
    }

    private String productKey(Long productId, String productCode) {
        if (productId != null && productId > 0L) {
            return "id:" + productId;
        }
        return "code:" + (productCode == null ? "" : productCode.trim().toLowerCase());
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}
