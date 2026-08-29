package com.zhihuiji.backend.api.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthenticationRequiredException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AdminAuditService adminAuditService;
    private final AdminPrincipalResolver adminPrincipalResolver;

    public GlobalExceptionHandler() {
        this.adminAuditService = null;
        this.adminPrincipalResolver = null;
    }

    @Autowired(required = false)
    public GlobalExceptionHandler(AdminAuditService adminAuditService, AdminPrincipalResolver adminPrincipalResolver) {
        this.adminAuditService = adminAuditService;
        this.adminPrincipalResolver = adminPrincipalResolver;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream().findFirst()
            .map(e -> e.getField() + " " + e.getDefaultMessage()).orElse("Invalid request");
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiResponse.CODE_BAD_REQUEST, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiResponse.CODE_BAD_REQUEST, "Invalid request"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiResponse.CODE_BAD_REQUEST, "Invalid request body"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiResponse.CODE_BAD_REQUEST, "Invalid request parameter"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiResponse.CODE_BAD_REQUEST, "Missing request parameter"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("Authorization".equalsIgnoreCase(ex.getHeaderName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(ApiResponse.CODE_UNAUTHORIZED, "missing bearer token"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiResponse.CODE_BAD_REQUEST, "Missing request header"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiResponse.failure(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Uploaded file is too large"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(ApiResponse.CODE_BAD_REQUEST, "Invalid multipart request"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.failure(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AdminConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminConflict(AdminConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.failure(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.failure(ApiResponse.CODE_UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceState(IllegalStateException ex) {
        log.warn("Backend service state rejected request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.failure(ApiResponse.CODE_SERVICE_UNAVAILABLE, "Service is not configured"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        recordAdminDenial("permission_denied", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.failure(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    @ExceptionHandler(AdminAuthenticationRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminAuthenticationRequired(AdminAuthenticationRequiredException ex) {
        recordAdminDenial("authentication_required", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.failure(ApiResponse.CODE_UNAUTHORIZED, "administrator authentication required"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.failure(ApiResponse.CODE_METHOD_NOT_ALLOWED, "Method not allowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.failure(ApiResponse.CODE_NOT_FOUND, "Resource not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled backend exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure(ApiResponse.CODE_INTERNAL_ERROR, "Internal server error"));
    }

    private void recordAdminDenial(String reason, String summary) {
        if (adminAuditService == null || adminPrincipalResolver == null) return;
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
            || !attributes.getRequest().getRequestURI().startsWith("/v2/admin")) return;
        try {
            AdminPrincipal principal = adminPrincipalResolver.requireCurrent();
            adminAuditService.record(principal, "admin.access.denied", "ADMIN", null, null, null,
                "DENIED", reason, summary, null, null);
        } catch (RuntimeException ignored) {
            log.warn("Unable to persist administrator denial audit");
        }
    }
}
