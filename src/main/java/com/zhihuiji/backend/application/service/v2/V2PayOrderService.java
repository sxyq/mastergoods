package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.pay.V2PayOrderDtos;
import com.zhihuiji.backend.application.service.PayOrderService;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class V2PayOrderService {
    private final PayOrderService payOrderService;

    public V2PayOrderService(PayOrderService payOrderService) {
        this.payOrderService = payOrderService;
    }

    public List<V2PayOrderDtos.PayOrderResponse> list(
        String keyword,
        Integer status,
        Long createdAfter,
        Long createdBefore
    ) {
        return payOrderService.list(keyword, status, createdAfter, createdBefore).stream()
            .map(this::toResponse)
            .toList();
    }

    public V2PayOrderDtos.PayOrderResponse get(Long id) {
        return toResponse(payOrderService.getById(id));
    }

    public V2PayOrderDtos.PayOrderResponse create(V2PayOrderDtos.CreateRequest request) {
        return toResponse(payOrderService.create(
            new PayOrderService.CreateCommand(
                request.supplierId(),
                request.supplierName(),
                request.amount(),
                request.method(),
                request.referenceNo(),
                request.notes(),
                request.status()
            )
        ));
    }

    public V2PayOrderDtos.PayOrderResponse updateStatus(Long id, Integer status) {
        return toResponse(payOrderService.updateStatus(id, status));
    }

    private V2PayOrderDtos.PayOrderResponse toResponse(PayOrderEntity entity) {
        return new V2PayOrderDtos.PayOrderResponse(
            entity.getId(),
            entity.getOrderNo(),
            entity.getSupplierId(),
            entity.getSupplierName(),
            entity.getAmount(),
            entity.getMethod(),
            entity.getReferenceNo(),
            entity.getNotes(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
