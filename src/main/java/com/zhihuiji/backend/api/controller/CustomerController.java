package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.application.service.CustomerService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<List<CustomerEntity>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(customerService.list(keyword), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerEntity> get(@PathVariable Long id) {
        return ApiResponse.success(customerService.get(id));
    }

    @PostMapping
    public ApiResponse<CustomerEntity> create(@Valid @RequestBody CustomerEntity payload) {
        return ApiResponse.success(customerService.create(payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerEntity> update(@PathVariable Long id, @Valid @RequestBody CustomerEntity payload) {
        return ApiResponse.success(customerService.update(id, payload));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success(null);
    }
}
