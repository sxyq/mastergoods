package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.application.service.AdminService;
import com.zhihuiji.backend.application.service.DemoDataService;
import com.zhihuiji.backend.application.service.LegacySQLiteImportService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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
@Profile("local")
@RequestMapping("/v1/admin")
public class AdminController {
    private final AdminService adminService;
    private final LegacySQLiteImportService legacySQLiteImportService;

    public AdminController(
        AdminService adminService,
        LegacySQLiteImportService legacySQLiteImportService
    ) {
        this.adminService = adminService;
        this.legacySQLiteImportService = legacySQLiteImportService;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminService.AdminSummary> summary() {
        return ApiResponse.success(adminService.summary());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminService.UserItem>> users(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(PaginationUtils.slice(adminService.listUsers(keyword), page, size));
    }

    @PostMapping("/users")
    public ApiResponse<AdminService.UserItem> createUser(@Valid @RequestBody AdminService.CreateUserRequest request) {
        return ApiResponse.success(adminService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<AdminService.UserItem> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody AdminService.UpdateUserRequest request
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
    public ResponseEntity<ApiResponse<AdminService.AgentSmokeResult>> runAgentSmoke() {
        AdminService.AgentSmokeResult result = adminService.runAgentSmoke();
        return ResponseEntity
            .status(HttpStatus.GONE)
            .body(ApiResponse.failure(HttpStatus.GONE.value(), result.taskSummary()));
    }

    @PostMapping("/migration/import-legacy")
    public ApiResponse<LegacySQLiteImportService.ImportResult> importLegacy(
        @Valid @RequestBody LegacySQLiteImportService.ImportRequest request
    ) {
        return ApiResponse.success(legacySQLiteImportService.importIntoFirstAccount(request));
    }
}
