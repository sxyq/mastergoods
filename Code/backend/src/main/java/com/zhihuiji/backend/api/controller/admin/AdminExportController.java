package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminExportDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.application.service.admin.AdminExportService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-ADM-14 export task and short-lived download endpoints. */
@RestController
@RequestMapping("/v2/admin/exports")
public class AdminExportController {
    private final AdminExportService service;
    private final AdminPrincipalResolver principalResolver;

    public AdminExportController(AdminExportService service, AdminPrincipalResolver principalResolver) {
        this.service = service;
        this.principalResolver = principalResolver;
    }

    @GetMapping
    public ApiResponse<AdminPageDtos.PageResponse<AdminExportDtos.Job>> list(
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(service.list(principalResolver.requireCurrent(), page, size));
    }

    @PostMapping
    public ApiResponse<AdminExportDtos.Job> create(@Valid @RequestBody AdminExportDtos.CreateRequest request) {
        return ApiResponse.success(service.create(principalResolver.requireCurrent(), request));
    }

    @GetMapping("/{exportId}")
    public ApiResponse<AdminExportDtos.Job> get(@PathVariable String exportId) {
        return ApiResponse.success(service.get(principalResolver.requireCurrent(), exportId));
    }

    @GetMapping("/{exportId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String exportId) {
        byte[] body = service.download(principalResolver.requireCurrent(), exportId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("admin-export-" + exportId + ".csv", StandardCharsets.UTF_8).build().toString())
            .body(body);
    }
}
