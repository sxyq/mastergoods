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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderService {
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_RECEIVED = 1;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ProductRepository productRepository;

    public PurchaseOrderService(
        PurchaseOrderRepository purchaseOrderRepository,
        PurchaseOrderItemRepository purchaseOrderItemRepository,
        ProductRepository productRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public PurchaseDetail create(CreatePurchaseOrderCommand command) {
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("采购明细不能为空");
        }
        long now = System.currentTimeMillis();
        long orderId = IdGenerator.nextId();
        String orderNo = "PO" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        double total = 0.0;
        int orderStatus = command.status() != null && command.status() == STATUS_DRAFT
            ? STATUS_DRAFT
            : STATUS_RECEIVED;

        List<PurchaseOrderItemEntity> itemEntities = new ArrayList<>();
        for (PurchaseItemDraft item : command.items()) {
            ProductEntity product = resolveProduct(item);
            double quantity = item.quantity() == null ? 0.0 : item.quantity();
            double unitCost = item.unitCost() == null ? 0.0 : item.unitCost();
            if (quantity <= 0.0 || unitCost < 0.0) {
                throw new IllegalArgumentException("采购数量或单价不合法");
            }
            double amount = quantity * unitCost;
            total += amount;
            if (orderStatus == STATUS_RECEIVED) {
                product.setStock(product.getStock() + quantity);
                product.setPurchasePrice(unitCost);
                product.setUpdatedAt(now);
                product.setSyncStatus(0);
                product.setSyncVersion(product.getSyncVersion() + 1);
                productRepository.save(product);
            }

            PurchaseOrderItemEntity entity = new PurchaseOrderItemEntity();
            entity.setId(IdGenerator.nextId());
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
        order.setOrderNo(orderNo);
        order.setSupplierName(command.supplierName());
        order.setTotalAmount(total);
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

    public List<PurchaseOrderEntity> list(String keyword, Integer status) {
        return purchaseOrderRepository.search(keyword, status);
    }

    public PurchaseDetail get(Long orderId) {
        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("采购单不存在"));
        return new PurchaseDetail(order, purchaseOrderItemRepository.findByOrderId(orderId));
    }

    public List<PurchaseOrderItemEntity> listItems(Long orderId) {
        return purchaseOrderItemRepository.findByOrderId(orderId);
    }

    private ProductEntity resolveProduct(PurchaseItemDraft item) {
        if (item.productId() != null && item.productId() > 0L) {
            return productRepository.findByIdForUpdate(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productId()));
        }
        if (item.productCode() != null && !item.productCode().isBlank()) {
            return productRepository.findByCodeForUpdate(item.productCode())
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
        String supplierName,
        List<PurchaseItemDraft> items,
        String notes,
        Integer status
    ) {}

    public record PurchaseDetail(
        PurchaseOrderEntity order,
        List<PurchaseOrderItemEntity> items
    ) {}
}
