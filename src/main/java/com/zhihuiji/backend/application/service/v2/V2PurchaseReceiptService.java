package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.PurchaseReceiptStatus;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReceiptDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptItemEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2PurchaseReceiptService {
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReceiptItemRepository purchaseReceiptItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2PurchaseReceiptService(
        PurchaseReceiptRepository purchaseReceiptRepository,
        PurchaseReceiptItemRepository purchaseReceiptItemRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        ProductRepository productRepository,
        SupplierRepository supplierRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.purchaseReceiptRepository = purchaseReceiptRepository;
        this.purchaseReceiptItemRepository = purchaseReceiptItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional
    public V2PurchaseReceiptDtos.PurchaseReceiptResponse create(V2PurchaseReceiptDtos.CreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("收货明细不能为空");
        }

        long now = System.currentTimeMillis();
        long receiptId = IdGenerator.nextId();
        String receiptNo = "RC" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        PurchaseOrderEntity purchaseOrder = null;
        if (request.purchaseOrderId() != null) {
            purchaseOrder = purchaseOrderRepository.findByIdAndOwnerUserId(request.purchaseOrderId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("采购订单不存在"));
        }
        SupplierEntity supplier = resolveSupplier(ownerUserId, request, purchaseOrder);

        double total = 0.0;
        List<PurchaseReceiptItemEntity> itemEntities = new ArrayList<>(request.items().size());
        for (V2PurchaseReceiptDtos.CreateItemRequest item : request.items()) {
            ProductEntity product = resolveProduct(ownerUserId, item);
            double quantity = item.quantity() == null ? 0.0 : item.quantity();
            double unitCost = item.unitCost() == null ? 0.0 : item.unitCost();
            if (quantity <= 0.0 || unitCost < 0.0) {
                throw new IllegalArgumentException("收货数量或单价不合法");
            }
            double amount = quantity * unitCost;
            total += amount;

            PurchaseReceiptItemEntity entity = new PurchaseReceiptItemEntity();
            entity.setId(IdGenerator.nextId());
            entity.setOwnerUserId(ownerUserId);
            entity.setReceiptId(receiptId);
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

        PurchaseReceiptEntity receipt = new PurchaseReceiptEntity();
        receipt.setId(receiptId);
        receipt.setOwnerUserId(ownerUserId);
        receipt.setReceiptNo(receiptNo);
        receipt.setPurchaseOrderId(request.purchaseOrderId());
        receipt.setSupplierId(supplier.getId());
        receipt.setSupplierName(supplier.getName());
        receipt.setTotalAmount(total);
        receipt.setStatus(PurchaseReceiptStatus.DRAFT.code());
        receipt.setNotes(request.notes());
        receipt.setCreatedAt(now);
        receipt.setUpdatedAt(now);
        purchaseReceiptRepository.save(receipt);
        purchaseReceiptItemRepository.saveAll(itemEntities);

        return toResponse(receipt, itemEntities);
    }

    @Transactional(readOnly = true)
    public List<V2PurchaseReceiptDtos.PurchaseReceiptResponse> list(String keyword, Integer status) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedKeyword = normalizeKeyword(keyword);
        List<PurchaseReceiptEntity> receipts = normalizedKeyword == null
            ? listWithoutKeyword(ownerUserId, status)
            : purchaseReceiptRepository.search(ownerUserId, normalizedKeyword, status);
        Map<Long, List<PurchaseReceiptItemEntity>> itemsByReceiptId = loadItemsByReceiptId(ownerUserId, receipts);
        return receipts.stream()
            .map(receipt -> toResponse(receipt, itemsByReceiptId.getOrDefault(receipt.getId(), List.of())))
            .toList();
    }

    @Transactional(readOnly = true)
    public V2PurchaseReceiptDtos.PurchaseReceiptResponse get(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReceiptEntity entity = purchaseReceiptRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("收货单不存在"));
        return toResponse(entity, purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    @Transactional(readOnly = true)
    public List<V2PurchaseReceiptDtos.PurchaseReceiptResponse> listByOrder(Long purchaseOrderId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<PurchaseReceiptEntity> receipts = purchaseReceiptRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(ownerUserId, purchaseOrderId);
        Map<Long, List<PurchaseReceiptItemEntity>> itemsByReceiptId = loadItemsByReceiptId(ownerUserId, receipts);
        return receipts.stream()
            .map(receipt -> toResponse(receipt, itemsByReceiptId.getOrDefault(receipt.getId(), List.of())))
            .toList();
    }

    @Transactional
    public V2PurchaseReceiptDtos.PurchaseReceiptResponse updateDraft(Long id, V2PurchaseReceiptDtos.UpdateDraftRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReceiptEntity entity = purchaseReceiptRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("收货单不存在"));
        if (entity.getStatus() != PurchaseReceiptStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        entity.setNotes(request.notes());
        entity.setUpdatedAt(System.currentTimeMillis());
        purchaseReceiptRepository.save(entity);
        return toResponse(entity, purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    @Transactional
    public V2PurchaseReceiptDtos.PurchaseReceiptResponse confirm(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReceiptEntity entity = purchaseReceiptRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("收货单不存在"));
        if (entity.getStatus() != PurchaseReceiptStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可确认");
        }

        long now = System.currentTimeMillis();
        List<PurchaseReceiptItemEntity> items = purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(ownerUserId, id);
        for (PurchaseReceiptItemEntity item : items) {
            ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.getProductId()));
            product.setStock(product.getStock() + item.getQuantity());
            product.setPurchasePrice(item.getUnitCost());
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

        entity.setStatus(PurchaseReceiptStatus.CONFIRMED.code());
        entity.setUpdatedAt(now);
        purchaseReceiptRepository.save(entity);
        return toResponse(entity, items);
    }

    @Transactional
    public V2PurchaseReceiptDtos.PurchaseReceiptResponse cancel(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseReceiptEntity entity = purchaseReceiptRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("收货单不存在"));
        if (entity.getStatus() == PurchaseReceiptStatus.CANCELLED.code()) {
            return toResponse(entity, purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(ownerUserId, id));
        }
        if (entity.getStatus() == PurchaseReceiptStatus.CONFIRMED.code()) {
            long now = System.currentTimeMillis();
            List<PurchaseReceiptItemEntity> items = purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(ownerUserId, id);
            for (PurchaseReceiptItemEntity item : items) {
                ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.getProductId()));
                product.setStock(product.getStock() - item.getQuantity());
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
        }
        entity.setStatus(PurchaseReceiptStatus.CANCELLED.code());
        entity.setUpdatedAt(System.currentTimeMillis());
        purchaseReceiptRepository.save(entity);
        return toResponse(entity, purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    private ProductEntity resolveProduct(Long ownerUserId, V2PurchaseReceiptDtos.CreateItemRequest item) {
        if (item.productId() != null && item.productId() > 0L) {
            return productRepository.findByIdForUpdate(ownerUserId, item.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productId()));
        }
        if (item.productCode() != null && !item.productCode().isBlank()) {
            return productRepository.findByCodeForUpdate(ownerUserId, item.productCode())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productCode()));
        }
        throw new IllegalArgumentException("收货明细缺少商品标识");
    }

    private SupplierEntity resolveSupplier(Long ownerUserId, V2PurchaseReceiptDtos.CreateRequest request, PurchaseOrderEntity purchaseOrder) {
        Long resolvedSupplierId = request.supplierId();
        if (purchaseOrder != null) {
            if (purchaseOrder.getSupplierId() == null) {
                throw new IllegalArgumentException("采购订单缺少供应商信息");
            }
            if (resolvedSupplierId != null && !resolvedSupplierId.equals(purchaseOrder.getSupplierId())) {
                throw new IllegalArgumentException("收货供应商与采购订单不一致");
            }
            resolvedSupplierId = purchaseOrder.getSupplierId();
        }
        if (resolvedSupplierId == null) {
            throw new IllegalArgumentException("收货单必须关联供应商");
        }
        return supplierRepository.findByIdAndOwnerUserId(resolvedSupplierId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
    }

    private V2PurchaseReceiptDtos.PurchaseReceiptResponse toResponse(PurchaseReceiptEntity entity, List<PurchaseReceiptItemEntity> items) {
        List<V2PurchaseReceiptDtos.PurchaseReceiptItemResponse> itemResponses = new ArrayList<>(items.size());
        for (PurchaseReceiptItemEntity item : items) {
            itemResponses.add(toItemResponse(item));
        }
        return new V2PurchaseReceiptDtos.PurchaseReceiptResponse(
            entity.getId(),
            entity.getReceiptNo(),
            entity.getPurchaseOrderId(),
            entity.getSupplierId(),
            entity.getSupplierName(),
            itemResponses,
            entity.getTotalAmount(),
            entity.getStatus(),
            entity.getNotes(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private List<PurchaseReceiptEntity> listWithoutKeyword(Long ownerUserId, Integer status) {
        if (status == null) {
            return purchaseReceiptRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        }
        return purchaseReceiptRepository.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(ownerUserId, status);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, List<PurchaseReceiptItemEntity>> loadItemsByReceiptId(Long ownerUserId, List<PurchaseReceiptEntity> receipts) {
        if (receipts.isEmpty()) {
            return Map.of();
        }
        List<Long> receiptIds = new ArrayList<>(receipts.size());
        for (PurchaseReceiptEntity receipt : receipts) {
            receiptIds.add(receipt.getId());
        }
        Map<Long, List<PurchaseReceiptItemEntity>> itemsByReceiptId = new LinkedHashMap<>(receiptIds.size());
        for (PurchaseReceiptItemEntity item : purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdInOrderByReceiptIdAscCreatedAtAsc(ownerUserId, receiptIds)) {
            itemsByReceiptId.computeIfAbsent(item.getReceiptId(), ignored -> new ArrayList<>()).add(item);
        }
        return itemsByReceiptId;
    }

    private V2PurchaseReceiptDtos.PurchaseReceiptItemResponse toItemResponse(PurchaseReceiptItemEntity item) {
        return new V2PurchaseReceiptDtos.PurchaseReceiptItemResponse(
            item.getId(),
            item.getReceiptId(),
            item.getProductId(),
            item.getProductCode(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitCost(),
            item.getAmount(),
            item.getCreatedAt()
        );
    }
}
