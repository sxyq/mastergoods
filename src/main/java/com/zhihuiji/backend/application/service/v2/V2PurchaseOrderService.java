package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseOrderDtos;
import com.zhihuiji.backend.application.service.PurchaseOrderService;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class V2PurchaseOrderService {
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public V2PurchaseOrderService(
        PurchaseOrderService purchaseOrderService,
        PurchaseOrderItemRepository purchaseOrderItemRepository
    ) {
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    }

    public V2PurchaseOrderDtos.PurchaseOrderResponse create(V2PurchaseOrderDtos.CreateRequest request) {
        List<PurchaseOrderService.PurchaseItemDraft> items = new ArrayList<>(request.items().size());
        for (V2PurchaseOrderDtos.CreateItemRequest row : request.items()) {
            items.add(new PurchaseOrderService.PurchaseItemDraft(
                row.productId(),
                row.productCode(),
                row.productName(),
                row.quantity(),
                row.unitCost()
            ));
        }
        return toResponse(purchaseOrderService.create(
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                request.supplierId(),
                request.supplierName(),
                items,
                request.settlementMethod(),
                request.warehouseId(),
                request.notes(),
                request.status()
            )
        ));
    }

    public List<V2PurchaseOrderDtos.PurchaseOrderResponse> list(String keyword, Integer status) {
        List<PurchaseOrderEntity> orders = purchaseOrderService.list(keyword, status);
        Map<Long, List<PurchaseOrderItemEntity>> itemsByOrderId = findItemsByOrderId(orders);
        List<V2PurchaseOrderDtos.PurchaseOrderResponse> responses = new ArrayList<>(orders.size());
        for (PurchaseOrderEntity order : orders) {
            responses.add(toResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of())));
        }
        return responses;
    }

    public V2PurchaseOrderDtos.PurchaseOrderResponse get(Long id) {
        return toResponse(purchaseOrderService.get(id));
    }

    public V2PurchaseOrderDtos.PurchaseOrderResponse update(Long id, V2PurchaseOrderDtos.CreateRequest request) {
        List<PurchaseOrderService.PurchaseItemDraft> items = new ArrayList<>(request.items().size());
        for (V2PurchaseOrderDtos.CreateItemRequest row : request.items()) {
            items.add(new PurchaseOrderService.PurchaseItemDraft(
                row.productId(),
                row.productCode(),
                row.productName(),
                row.quantity(),
                row.unitCost()
            ));
        }
        return toResponse(purchaseOrderService.update(id,
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                request.supplierId(),
                request.supplierName(),
                items,
                request.settlementMethod(),
                request.warehouseId(),
                request.notes(),
                request.status()
            )
        ));
    }

    public void delete(Long id) {
        purchaseOrderService.delete(id);
    }

    private V2PurchaseOrderDtos.PurchaseOrderResponse toResponse(PurchaseOrderService.PurchaseDetail detail) {
        return toResponse(detail.order(), detail.items());
    }

    private V2PurchaseOrderDtos.PurchaseOrderResponse toResponse(PurchaseOrderEntity order, List<PurchaseOrderItemEntity> items) {
        List<V2PurchaseOrderDtos.PurchaseOrderItemResponse> itemResponses = new ArrayList<>(items.size());
        for (PurchaseOrderItemEntity item : items) {
            itemResponses.add(toItemResponse(item));
        }
        return new V2PurchaseOrderDtos.PurchaseOrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getSupplierId(),
            order.getSupplierName(),
            itemResponses,
            order.getTotalAmount(),
            order.getPaidAmount(),
            order.getReceivedAmount(),
            order.getSettlementMethod(),
            order.getWarehouseId(),
            order.getNotes(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private V2PurchaseOrderDtos.PurchaseOrderItemResponse toItemResponse(PurchaseOrderItemEntity item) {
        return new V2PurchaseOrderDtos.PurchaseOrderItemResponse(
            item.getId(),
            item.getOrderId(),
            item.getProductId(),
            item.getProductCode(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitCost(),
            item.getAmount(),
            item.getCreatedAt()
        );
    }

    private Map<Long, List<PurchaseOrderItemEntity>> findItemsByOrderId(List<PurchaseOrderEntity> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> orderIds = new java.util.LinkedHashSet<>(orders.size());
        Long ownerUserId = null;
        for (PurchaseOrderEntity order : orders) {
            if (ownerUserId == null && order.getOwnerUserId() != null) {
                ownerUserId = order.getOwnerUserId();
            }
            Long orderId = order.getId();
            if (orderId != null && orderId > 0L) {
                orderIds.add(orderId);
            }
        }
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (ownerUserId == null) {
            return Collections.emptyMap();
        }
        List<PurchaseOrderItemEntity> items = purchaseOrderItemRepository.findByOwnerUserIdAndOrderIdIn(ownerUserId, orderIds);
        Map<Long, List<PurchaseOrderItemEntity>> itemsByOrderId = new java.util.LinkedHashMap<>();
        for (PurchaseOrderItemEntity item : items) {
            itemsByOrderId.computeIfAbsent(item.getOrderId(), ignored -> new ArrayList<>()).add(item);
        }
        return itemsByOrderId;
    }
}
