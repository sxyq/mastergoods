package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.sales.V2SaleOrderDtos;
import com.zhihuiji.backend.application.service.SaleOrderService;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class V2SaleOrderService {
    private final SaleOrderService saleOrderService;

    public V2SaleOrderService(SaleOrderService saleOrderService) {
        this.saleOrderService = saleOrderService;
    }

    public V2SaleOrderDtos.SaleOrderResponse create(V2SaleOrderDtos.CreateRequest request) {
        List<SaleOrderService.SaleItemDraft> items = request.items().stream()
            .map(row -> new SaleOrderService.SaleItemDraft(row.productId(), row.quantity(), row.unitPrice()))
            .toList();
        return toResponse(saleOrderService.create(
            new SaleOrderService.CreateSaleOrderCommand(
                request.customerId(),
                request.customerName(),
                items,
                request.notes(),
                request.discountAmount() == null ? 0.0 : request.discountAmount()
            )
        ));
    }

    public List<V2SaleOrderDtos.SaleOrderResponse> list(
        String keyword,
        Integer status,
        Double minTotalAmount,
        Double maxTotalAmount,
        Long createdAfter,
        Long createdBefore,
        String productKeyword,
        Integer paymentStatus
    ) {
        return saleOrderService.list(
                keyword,
                status,
                minTotalAmount,
                maxTotalAmount,
                createdAfter,
                createdBefore,
                productKeyword,
                paymentStatus
            ).stream()
            .map(order -> toResponse(order, saleOrderService.listItems(order.getId())))
            .toList();
    }

    public V2SaleOrderDtos.SaleOrderResponse get(Long id) {
        return toResponse(saleOrderService.get(id));
    }

    public V2SaleOrderDtos.SaleOrderResponse updateDraft(Long id, V2SaleOrderDtos.UpdateDraftRequest request) {
        return toResponse(saleOrderService.updateDraft(id, request.discountAmount(), request.notes()));
    }

    public V2SaleOrderDtos.PaymentResponse addPayment(Long id, V2SaleOrderDtos.PaymentRequest request) {
        return toPaymentResponse(saleOrderService.addPayment(id, request.amount(), request.method(), request.referenceNo()));
    }

    public List<V2SaleOrderDtos.PaymentResponse> listPayments(Long id) {
        return saleOrderService.listPayments(id).stream()
            .map(this::toPaymentResponse)
            .toList();
    }

    public void updateStatus(Long id, Integer status) {
        saleOrderService.updateStatus(id, status);
    }

    public V2SaleOrderDtos.SaleOrderResponse cancel(Long id) {
        return toResponse(saleOrderService.cancel(id));
    }

    private V2SaleOrderDtos.SaleOrderResponse toResponse(SaleOrderService.OrderDetail detail) {
        return toResponse(detail.order(), detail.items());
    }

    private V2SaleOrderDtos.SaleOrderResponse toResponse(SaleOrderEntity order, List<SaleOrderItemEntity> items) {
        return new V2SaleOrderDtos.SaleOrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getCustomerId(),
            order.getCustomerName(),
            items.stream().map(this::toItemResponse).toList(),
            order.getSubtotalAmount(),
            order.getDiscountAmount(),
            order.getTotalAmount(),
            order.getPaidAmount(),
            order.getNotes(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private V2SaleOrderDtos.SaleOrderItemResponse toItemResponse(SaleOrderItemEntity item) {
        return new V2SaleOrderDtos.SaleOrderItemResponse(
            item.getId(),
            item.getOrderId(),
            item.getProductId(),
            item.getProductCode(),
            item.getProductName(),
            item.getCustomerId(),
            item.getCustomerName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount(),
            item.getCreatedAt()
        );
    }

    private V2SaleOrderDtos.PaymentResponse toPaymentResponse(PaymentEntity payment) {
        return new V2SaleOrderDtos.PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getMethod(),
            payment.getReferenceNo(),
            payment.getType(),
            payment.getCreatedAt()
        );
    }
}
