package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.PayOrderDto;
import com.zhihuiji.backend.application.service.PayOrderService;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/pay-orders")
public class PayOrderController {
    private final PayOrderService payOrderService;

    public PayOrderController(PayOrderService payOrderService) {
        this.payOrderService = payOrderService;
    }

    @GetMapping
    public ApiResponse<List<PayOrderDto>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "created_after", required = false) String createdAfter,
        @RequestParam(value = "created_before", required = false) String createdBefore
    ) {
        return ApiResponse.success(
            payOrderService.list(
                    keyword,
                    status,
                    parseLong(createdAfter),
                    parseLong(createdBefore)
                ).stream()
                .map(this::toDto)
                .toList()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<PayOrderDto> get(@PathVariable Long id) {
        return ApiResponse.success(toDto(payOrderService.getById(id)));
    }

    @PostMapping
    public ApiResponse<PayOrderDto> create(@RequestBody CreateRequest request) {
        PayOrderEntity created = payOrderService.create(
            new PayOrderService.CreateCommand(
                request.supplierId(),
                request.supplierName(),
                request.amount(),
                request.method(),
                request.referenceNo(),
                request.notes(),
                request.status()
            )
        );
        return ApiResponse.success(toDto(created));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<PayOrderDto> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return ApiResponse.success(toDto(payOrderService.updateStatus(id, request.status())));
    }

    private PayOrderDto toDto(PayOrderEntity entity) {
        return new PayOrderDto(
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

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    public record CreateRequest(
        Long supplierId,
        String supplierName,
        Double amount,
        Integer method,
        String referenceNo,
        String notes,
        Integer status
    ) {}

    public record StatusRequest(Integer status) {}
}
