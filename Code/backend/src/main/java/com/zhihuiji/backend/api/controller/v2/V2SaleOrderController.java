package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.ParseUtils;
import com.zhihuiji.backend.api.dto.v2.sales.V2SaleOrderDtos;
import com.zhihuiji.backend.application.service.v2.V2SaleReceiptPdfService;
import com.zhihuiji.backend.application.service.v2.V2SaleOrderService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
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
@RequestMapping("/v2/sale-orders")
@RequireStorePermission("sales:view")
public class V2SaleOrderController {
    private final V2SaleOrderService v2SaleOrderService;
    private final V2SaleReceiptPdfService v2SaleReceiptPdfService;

    public V2SaleOrderController(
        V2SaleOrderService v2SaleOrderService,
        V2SaleReceiptPdfService v2SaleReceiptPdfService
    ) {
        this.v2SaleOrderService = v2SaleOrderService;
        this.v2SaleReceiptPdfService = v2SaleReceiptPdfService;
    }

    @PostMapping
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SaleOrderDtos.SaleOrderResponse> create(@Valid @RequestBody V2SaleOrderDtos.CreateRequest request) {
        return ApiResponse.success(v2SaleOrderService.create(request));
    }

    @GetMapping
    public ApiResponse<List<V2SaleOrderDtos.SaleOrderResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "min_total_amount", required = false) String minTotalAmount,
        @RequestParam(value = "max_total_amount", required = false) String maxTotalAmount,
        @RequestParam(value = "created_after", required = false) String createdAfter,
        @RequestParam(value = "created_before", required = false) String createdBefore,
        @RequestParam(value = "product_keyword", required = false) String productKeyword,
        @RequestParam(value = "payment_status", required = false) String paymentStatus,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(
            v2SaleOrderService.list(
                keyword,
                status,
                ParseUtils.parseDouble(minTotalAmount),
                ParseUtils.parseDouble(maxTotalAmount),
                ParseUtils.parseLong(createdAfter),
                ParseUtils.parseLong(createdBefore),
                productKeyword,
                ParseUtils.parseInteger(paymentStatus),
                page,
                size
            )
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<V2SaleOrderDtos.SaleOrderResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2SaleOrderService.get(id));
    }

    @GetMapping(value = "/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receiptPdf(@PathVariable Long id) {
        byte[] bytes = v2SaleReceiptPdfService.export(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(bytes.length)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sale-receipt-" + id + ".pdf\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
            .body(bytes);
    }

    @PutMapping({"/{id}", "/{id}/draft"})
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SaleOrderDtos.SaleOrderResponse> updateDraft(
        @PathVariable Long id,
        @Valid @RequestBody V2SaleOrderDtos.UpdateDraftRequest request
    ) {
        return ApiResponse.success(v2SaleOrderService.updateDraft(id, request));
    }

    @PutMapping("/{id}/confirm")
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SaleOrderDtos.SaleOrderResponse> confirm(
        @PathVariable Long id,
        @Valid @RequestBody V2SaleOrderDtos.ConfirmRequest request
    ) {
        return ApiResponse.success(v2SaleOrderService.confirm(id, request));
    }

    @PostMapping("/{id}/payments")
    @RequireStorePermission({"sales:write", "finance:write"})
    public ApiResponse<V2SaleOrderDtos.PaymentResponse> addPayment(
        @PathVariable Long id,
        @Valid @RequestBody V2SaleOrderDtos.PaymentRequest request
    ) {
        return ApiResponse.success(v2SaleOrderService.addPayment(id, request));
    }

    @GetMapping("/{id}/payments")
    public ApiResponse<List<V2SaleOrderDtos.PaymentResponse>> listPayments(@PathVariable Long id) {
        return ApiResponse.success(v2SaleOrderService.listPayments(id));
    }

    @PutMapping("/{id}/status")
    @RequireStorePermission("sales:write")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody V2SaleOrderDtos.StatusRequest request) {
        v2SaleOrderService.updateStatus(id, request.status());
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/cancel")
    @RequireStorePermission("sales:write")
    public ApiResponse<V2SaleOrderDtos.SaleOrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(v2SaleOrderService.cancel(id));
    }
}
