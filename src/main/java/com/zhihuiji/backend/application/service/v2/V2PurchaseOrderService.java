package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseOrderDtos;
import com.zhihuiji.backend.application.service.PurchaseOrderService;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class V2PurchaseOrderService {
    private final PurchaseOrderService purchaseOrderService;

    public V2PurchaseOrderService(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    public V2PurchaseOrderDtos.PurchaseOrderResponse create(V2PurchaseOrderDtos.CreateRequest request) {
        List<PurchaseOrderService.PurchaseItemDraft> items = request.items().stream()
            .map(row -> new PurchaseOrderService.PurchaseItemDraft(
                row.productId(),
                row.productCode(),
                row.productName(),
                row.quantity(),
                row.unitCost()
            ))
            .toList();
        return toResponse(purchaseOrderService.create(
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                request.supplierId(),
                request.supplierName(),
                items,
                request.notes(),
                request.status()
            )
        ));
    }

    public List<V2PurchaseOrderDtos.PurchaseOrderResponse> list(String keyword, Integer status) {
        return purchaseOrderService.list(keyword, status).stream()
            .map(order -> toResponse(order, purchaseOrderService.listItems(order.getId())))
            .toList();
    }

    public V2PurchaseOrderDtos.PurchaseOrderResponse get(Long id) {
        return toResponse(purchaseOrderService.get(id));
    }

    private V2PurchaseOrderDtos.PurchaseOrderResponse toResponse(PurchaseOrderService.PurchaseDetail detail) {
        return toResponse(detail.order(), detail.items());
    }

    private V2PurchaseOrderDtos.PurchaseOrderResponse toResponse(PurchaseOrderEntity order, List<PurchaseOrderItemEntity> items) {
        return new V2PurchaseOrderDtos.PurchaseOrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getSupplierId(),
            order.getSupplierName(),
            items.stream().map(this::toItemResponse).toList(),
            order.getTotalAmount(),
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
            item.getProductCode(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitCost(),
            item.getAmount(),
            item.getCreatedAt()
        );
    }
}
