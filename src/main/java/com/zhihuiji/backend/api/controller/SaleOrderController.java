package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.ParseUtils;
import com.zhihuiji.backend.api.dto.SaleOrderDto;
import com.zhihuiji.backend.api.dto.SaleOrderItemDto;
import com.zhihuiji.backend.api.dto.SaleOrderStatusRequest;
import com.zhihuiji.backend.application.service.SaleOrderService;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sale-orders")
public class SaleOrderController {
    private final SaleOrderService saleOrderService;

    public SaleOrderController(SaleOrderService saleOrderService) {
        this.saleOrderService = saleOrderService;
    }

    @PostMapping
    public ApiResponse<SaleOrderDto> create(@Valid @RequestBody CreateRequest request) {
        List<SaleOrderService.SaleItemDraft> items = request.items().stream()
            .map(row -> new SaleOrderService.SaleItemDraft(row.productId(), row.quantity(), row.unitPrice()))
            .toList();
        return ApiResponse.success(toDto(saleOrderService.create(
            new SaleOrderService.CreateSaleOrderCommand(
                request.customerId(),
                request.customerName(),
                items,
                request.notes(),
                request.discountAmount() == null ? 0.0 : request.discountAmount()
            )
        )));
    }

    @GetMapping
    public ApiResponse<List<SaleOrderDto>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "min_total_amount", required = false) String minTotalAmount,
        @RequestParam(value = "max_total_amount", required = false) String maxTotalAmount,
        @RequestParam(value = "created_after", required = false) String createdAfter,
        @RequestParam(value = "created_before", required = false) String createdBefore,
        @RequestParam(value = "product_keyword", required = false) String productKeyword,
        @RequestParam(value = "payment_status", required = false) String paymentStatus
    ) {
        List<SaleOrderEntity> orders = saleOrderService.list(
            keyword,
            status,
            ParseUtils.parseDouble(minTotalAmount),
            ParseUtils.parseDouble(maxTotalAmount),
            ParseUtils.parseLong(createdAfter),
            ParseUtils.parseLong(createdBefore),
            productKeyword,
            ParseUtils.parseInteger(paymentStatus)
        );
        List<SaleOrderDto> payload = orders.stream()
            .map(order -> toDto(order, saleOrderService.listItems(order.getId())))
            .toList();
        return ApiResponse.success(payload);
    }

    @GetMapping("/{id}")
    public ApiResponse<SaleOrderDto> get(@PathVariable Long id) {
        return ApiResponse.success(toDto(saleOrderService.get(id)));
    }

    @PutMapping({"/{id}", "/{id}/draft"})
    public ApiResponse<SaleOrderDto> updateDraft(@PathVariable Long id, @Valid @RequestBody UpdateDraftRequest request) {
        return ApiResponse.success(toDto(saleOrderService.updateDraft(id, request.discountAmount(), request.notes())));
    }

    @PostMapping("/{id}/payments")
    public ApiResponse<PaymentEntity> addPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success(saleOrderService.addPayment(id, request.amount(), request.method(), request.referenceNo()));
    }

    @GetMapping("/{id}/payments")
    public ApiResponse<List<PaymentEntity>> listPayments(@PathVariable Long id) {
        return ApiResponse.success(saleOrderService.listPayments(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody SaleOrderStatusRequest request) {
        saleOrderService.updateStatus(id, request.status());
        return ApiResponse.success(null);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] bytes = saleOrderService.exportPdf(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sale-order-" + id + ".pdf\"")
            .body(bytes);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<SaleOrderDto> cancel(@PathVariable Long id) {
        return ApiResponse.success(toDto(saleOrderService.cancel(id)));
    }

    private SaleOrderDto toDto(SaleOrderService.OrderDetail detail) {
        return toDto(detail.order(), detail.items());
    }

    private SaleOrderDto toDto(SaleOrderEntity order, List<SaleOrderItemEntity> items) {
        List<SaleOrderItemDto> itemDtos = items.stream()
            .map(item -> new SaleOrderItemDto(
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
            ))
            .toList();
        return new SaleOrderDto(
            order.getId(),
            order.getOrderNo(),
            order.getCustomerId(),
            order.getCustomerName(),
            itemDtos,
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

    public record CreateRequest(
        Long customerId,
        String customerName,
        List<ItemRequest> items,
        String notes,
        Double discountAmount
    ) {}

    public record ItemRequest(Long productId, Double quantity, Double unitPrice) {}

    public record UpdateDraftRequest(Double discountAmount, String notes) {}

    public record PaymentRequest(Double amount, Integer method, String referenceNo) {}
}
