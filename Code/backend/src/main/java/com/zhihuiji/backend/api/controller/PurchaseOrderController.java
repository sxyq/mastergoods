package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.PurchaseOrderDto;
import com.zhihuiji.backend.api.dto.PurchaseOrderItemDto;
import com.zhihuiji.backend.application.service.PurchaseOrderService;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/v1/purchase-orders")
@RequireStorePermission("purchase:view")
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @RequireStorePermission("purchase:write")
    public ApiResponse<PurchaseOrderDto> create(@Valid @RequestBody CreateRequest request) {
        List<PurchaseOrderService.PurchaseItemDraft> items = request.items().stream()
            .map(row -> new PurchaseOrderService.PurchaseItemDraft(
                row.productId(), row.productCode(), row.productName(),
                row.quantity(), row.unitCost()))
            .toList();
        return ApiResponse.success(toDto(purchaseOrderService.create(
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                request.supplierId(),
                request.supplierName(),
                items,
                null,
                null,
                request.notes(),
                request.status()
            )
        )));
    }

    @GetMapping
    public ApiResponse<List<PurchaseOrderDto>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtils.pageable(page, size);
        List<PurchaseOrderEntity> rows = purchaseOrderService.list(keyword, status, pageable);
        return ApiResponse.success(rows.stream()
            .map(order -> toDto(order, purchaseOrderService.listItems(order.getId())))
            .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseOrderDto> get(@PathVariable Long id) {
        return ApiResponse.success(toDto(purchaseOrderService.get(id)));
    }

    private PurchaseOrderDto toDto(PurchaseOrderService.PurchaseDetail detail) {
        return toDto(detail.order(), detail.items());
    }

    private PurchaseOrderDto toDto(PurchaseOrderEntity order, List<PurchaseOrderItemEntity> items) {
        List<PurchaseOrderItemDto> itemDtos = items.stream()
            .map(item -> new PurchaseOrderItemDto(
                item.getId(),
                item.getOrderId(),
                item.getProductCode(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getAmount(),
                item.getCreatedAt()))
            .toList();
        return new PurchaseOrderDto(
            order.getId(),
            order.getOrderNo(),
            order.getSupplierId(),
            order.getSupplierName(),
            itemDtos,
            order.getTotalAmount(),
            order.getNotes(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    public record CreateRequest(
        Long supplierId,
        String supplierName,
        List<ItemRequest> items,
        String notes,
        Integer status
    ) {}

    public record ItemRequest(
        Long productId,
        String productCode,
        String productName,
        Double quantity,
        Double unitCost
    ) {}
}
