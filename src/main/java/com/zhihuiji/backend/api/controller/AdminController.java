package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.application.service.AdminService;
import com.zhihuiji.backend.application.service.DemoDataService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequestMapping("/v1/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminService.AdminSummary> summary() {
        return ApiResponse.success(adminService.summary());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminService.UserItem>> users(
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(adminService.listUsers(keyword));
    }

    @PostMapping("/users")
    public ApiResponse<AdminService.UserItem> createUser(@RequestBody AdminService.CreateUserRequest request) {
        return ApiResponse.success(adminService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<AdminService.UserItem> updateUser(
        @PathVariable Long userId,
        @RequestBody AdminService.UpdateUserRequest request
    ) {
        return ApiResponse.success(adminService.updateUser(userId, request));
    }

    @PostMapping("/demo/seed")
    public ApiResponse<DemoDataService.SeedResult> seedDemo(
        @RequestParam(value = "reset", defaultValue = "false") boolean reset
    ) {
        return ApiResponse.success(adminService.seedDemoData(reset));
    }

    @PostMapping("/agent/smoke")
    public ApiResponse<AdminService.AgentSmokeResult> runAgentSmoke() {
        return ApiResponse.success(adminService.runAgentSmoke());
    }
}
