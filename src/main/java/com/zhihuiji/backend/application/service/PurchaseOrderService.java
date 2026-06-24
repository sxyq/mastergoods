package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.PurchaseOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ProductRepository productRepository;
    private final CurrentOwnerService currentOwnerService;

    public PurchaseOrderService(
        PurchaseOrderRepository purchaseOrderRepository,
        PurchaseOrderItemRepository purchaseOrderItemRepository,
        ProductRepository productRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.productRepository = productRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional
    public PurchaseDetail create(CreatePurchaseOrderCommand command) {
        return createForOwner(currentOwnerService.requireCurrentOwnerUserId(), command);
    }

    @Transactional
    public PurchaseDetail createForOwner(Long ownerUserId, CreatePurchaseOrderCommand command) {
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("采购明细不能为空");
        }
        long now = System.currentTimeMillis();
        long orderId = IdGenerator.nextId();
        String orderNo = "PO" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        double total = 0.0;
        int orderStatus = command.status() != null && command.status() == PurchaseOrderStatus.DRAFT.code()
            ? PurchaseOrderStatus.DRAFT.code()
            : PurchaseOrderStatus.RECEIVED.code();

        List<PurchaseOrderItemEntity> itemEntities = new ArrayList<>(command.items().size());
        for (PurchaseItemDraft item : command.items()) {
            ProductEntity product = resolveProduct(ownerUserId, item);
            double quantity = item.quantity() == null ? 0.0 : item.quantity();
            double unitCost = item.unitCost() == null ? 0.0 : item.unitCost();
            if (quantity <= 0.0 || unitCost < 0.0) {
                throw new IllegalArgumentException("采购数量或单价不合法");
            }
            double amount = quantity * unitCost;
            total += amount;
            if (orderStatus == PurchaseOrderStatus.RECEIVED.code()) {
                product.setStock(product.getStock() + quantity);
                product.setPurchasePrice(unitCost);
                product.setUpdatedAt(now);
                product.setSyncStatus(0);
                product.setSyncVersion(product.getSyncVersion() + 1);
                productRepository.save(product);
            }

            PurchaseOrderItemEntity entity = new PurchaseOrderItemEntity();
            entity.setId(IdGenerator.nextId());
            entity.setOwnerUserId(ownerUserId);
            entity.setOrderId(orderId);
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

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setId(orderId);
        order.setOwnerUserId(ownerUserId);
        order.setOrderNo(orderNo);
        order.setSupplierId(command.supplierId());
        order.setSupplierName(command.supplierName());
        order.setTotalAmount(total);
        order.setPaidAmount(0.0);
        order.setReceivedAmount(orderStatus == PurchaseOrderStatus.RECEIVED.code() ? total : 0.0);
        order.setSettlementMethod(command.settlementMethod());
        order.setWarehouseId(command.warehouseId());
        order.setNotes(command.notes());
        order.setStatus(orderStatus);
        order.setSyncStatus(0);
        order.setSyncVersion(1L);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        purchaseOrderRepository.save(order);
        purchaseOrderItemRepository.saveAll(itemEntities);
        return new PurchaseDetail(order, itemEntities);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderEntity> list(String keyword, Integer status) {
        return purchaseOrderRepository.search(
            currentOwnerService.requireCurrentOwnerUserId(),
            normalizeKeyword(keyword),
            status
        );
    }

    @Transactional(readOnly = true)
    public PurchaseDetail get(Long orderId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购单不存在"));
        return new PurchaseDetail(order, purchaseOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderItemEntity> listItems(Long orderId) {
        return purchaseOrderItemRepository.findByOwnerUserIdAndOrderId(currentOwnerService.requireCurrentOwnerUserId(), orderId);
    }

    private ProductEntity resolveProduct(Long ownerUserId, PurchaseItemDraft item) {
        if (item.productId() != null && item.productId() > 0L) {
            return productRepository.findByIdForUpdate(ownerUserId, item.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productId()));
        }
        if (item.productCode() != null && !item.productCode().isBlank()) {
            return productRepository.findByCodeForUpdate(ownerUserId, item.productCode())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productCode()));
        }
        throw new IllegalArgumentException("采购明细缺少商品标识");
    }

    public record PurchaseItemDraft(
        Long productId,
        String productCode,
        String productName,
        Double quantity,
        Double unitCost
    ) {}

    public record CreatePurchaseOrderCommand(
        Long supplierId,
        String supplierName,
        List<PurchaseItemDraft> items,
        Integer settlementMethod,
        Long warehouseId,
        String notes,
        Integer status
    ) {}

    @Transactional
    public PurchaseDetail update(Long orderId, CreatePurchaseOrderCommand command) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购单不存在"));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT.code()) {
            throw new IllegalArgumentException("只有草稿状态的采购单可以编辑");
        }

        long now = System.currentTimeMillis();
        double total = 0.0;

        purchaseOrderItemRepository.deleteAllByOrderId(orderId);

        List<PurchaseOrderItemEntity> itemEntities = new ArrayList<>(command.items().size());
        for (PurchaseItemDraft item : command.items()) {
            ProductEntity product = resolveProduct(ownerUserId, item);
            double quantity = item.quantity() == null ? 0.0 : item.quantity();
            double unitCost = item.unitCost() == null ? 0.0 : item.unitCost();
            if (quantity <= 0.0 || unitCost < 0.0) {
                throw new IllegalArgumentException("采购数量或单价不合法");
            }
            double amount = quantity * unitCost;
            total += amount;

            PurchaseOrderItemEntity entity = new PurchaseOrderItemEntity();
            entity.setId(IdGenerator.nextId());
            entity.setOwnerUserId(ownerUserId);
            entity.setOrderId(orderId);
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

        order.setSupplierId(command.supplierId());
        order.setSupplierName(command.supplierName());
        order.setTotalAmount(total);
        order.setSettlementMethod(command.settlementMethod());
        order.setWarehouseId(command.warehouseId());
        order.setNotes(command.notes());
        order.setUpdatedAt(now);
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        purchaseOrderRepository.save(order);
        purchaseOrderItemRepository.saveAll(itemEntities);
        return new PurchaseDetail(order, itemEntities);
    }

    @Transactional
    public void delete(Long orderId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("采购单不存在"));
        if (order.getStatus() != PurchaseOrderStatus.DRAFT.code()) {
            throw new IllegalArgumentException("只有草稿状态的采购单可以删除");
        }
        purchaseOrderItemRepository.deleteAllByOrderId(orderId);
        purchaseOrderRepository.delete(order);
    }

    public record PurchaseDetail(
        PurchaseOrderEntity order,
        List<PurchaseOrderItemEntity> items
    ) {}

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
