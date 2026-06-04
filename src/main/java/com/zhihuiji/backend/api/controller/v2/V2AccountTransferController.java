package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.v2.V2AccountTransferService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/account-transfers")
public class V2AccountTransferController {
    private final V2AccountTransferService v2AccountTransferService;

    public V2AccountTransferController(V2AccountTransferService v2AccountTransferService) {
        this.v2AccountTransferService = v2AccountTransferService;
    }

    @GetMapping
    public ApiResponse<List<V2FinanceDtos.AccountTransferResponse>> list() {
        return ApiResponse.success(v2AccountTransferService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<V2FinanceDtos.AccountTransferResponse> get(@PathVariable Long id) {
        return ApiResponse.success(v2AccountTransferService.get(id));
    }

    @PostMapping
    public ApiResponse<V2FinanceDtos.AccountTransferResponse> create(@Valid @RequestBody V2FinanceDtos.AccountTransferCreateRequest request) {
        return ApiResponse.success(v2AccountTransferService.create(request));
    }
}
