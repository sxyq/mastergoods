package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.common.ParseUtils;
import com.zhihuiji.backend.api.dto.v2.pay.V2PayOrderDtos;
import com.zhihuiji.backend.application.service.v2.V2PayOrderService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/pay-orders")
@RequireStorePermission("finance:view")
public class V2PayOrderController {
    private final V2PayOrderService v2PayOrderService;

    public V2PayOrderController(V2PayOrderService v2PayOrderService) {
        this.v2PayOrderService = v2PayOrderService;
    }

    @GetMapping
    public ApiResponse<List<V2PayOrderDtos.PayOrderResponse>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "created_after", required = false) String createdAfter,
        @RequestParam(value = "created_before", required = false) String createdBefore,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(
            v2PayOrderService.list(
                keyword,
                status,
                ParseUtils.parseLong(createdAfter),
                ParseUtils.parseLong(createdBefore)
            ),
            page,
            size
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<V2PayOrderDtos.PayOrderResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2PayOrderService.get(id));
    }

    @PostMapping
    @RequireStorePermission("finance:write")
    public ApiResponse<V2PayOrderDtos.PayOrderResponse> create(@Valid @RequestBody V2PayOrderDtos.CreateRequest request) {
        return ApiResponse.success(v2PayOrderService.create(request));
    }

    @PutMapping("/{id}/status")
    @RequireStorePermission("finance:write")
    public ApiResponse<V2PayOrderDtos.PayOrderResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody V2PayOrderDtos.StatusRequest request
    ) {
        return ApiResponse.success(v2PayOrderService.updateStatus(id, request.status()));
    }
}
