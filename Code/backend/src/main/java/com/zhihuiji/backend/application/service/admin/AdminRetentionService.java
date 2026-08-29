package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.common.AdminConflictException;
import com.zhihuiji.backend.api.dto.admin.AdminRetentionDtos;
import com.zhihuiji.backend.domain.entity.AdminRetentionPolicyEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminRetentionPolicyRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Versioned platform retention policy with explicit confirmation. */
@Service
public class AdminRetentionService {
    private static final long POLICY_ID = 1L;
    private final AdminAuthorizationService authorizationService;
    private final AdminRetentionPolicyRepository repository;
    private final AdminAuditService auditService;

    public AdminRetentionService(AdminAuthorizationService authorizationService,
                                 AdminRetentionPolicyRepository repository,
                                 AdminAuditService auditService) {
        this.authorizationService = authorizationService;
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AdminRetentionDtos.Policy get(AdminPrincipal principal) {
        authorizationService.requirePermission(principal, AdminPermission.SYSTEM_READ);
        AdminRetentionPolicyEntity policy = repository.findById(POLICY_ID).orElse(null);
        if (policy == null) {
            var response = new AdminRetentionDtos.Policy(0L, 365, 30, 30, 365, "REDACTED", null, null);
            auditService.recordRead(principal, "admin.retention.read", "RETENTION", "1", null, null, "defaults");
            return response;
        }
        var response = toDto(policy);
        auditService.recordRead(principal, "admin.retention.read", "RETENTION", "1", null, null, "policy");
        return response;
    }

    @Transactional
    public AdminRetentionDtos.Policy update(AdminPrincipal principal, AdminRetentionDtos.UpdateRequest request) {
        authorizationService.requirePermission(principal, AdminPermission.SYSTEM_RETENTION_MANAGE);
        validate(request);
        String normalizedContentMode = normalizeContentMode(request.contentMode());
        String payload = request.auditDays() + "|" + request.messageDays() + "|" + request.toolResultDays() + "|"
            + request.metricsDays() + "|" + normalizedContentMode;
        String hash = auditService.payloadHash(payload);
        String idempotencyKey = request.idempotencyKey().trim();
        AdminRetentionPolicyEntity existingByKey = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existingByKey != null) {
            if (!hash.equals(existingByKey.getIdempotencyPayloadHash())) {
                throw new AdminConflictException("idempotency key was already used with a different payload");
            }
            return toDto(existingByKey);
        }
        AdminRetentionPolicyEntity policy = repository.findById(POLICY_ID).orElse(null);
        long current = policy == null || policy.getVersion() == null ? 0L : policy.getVersion();
        if (request.expectedVersion() != current) throw new AdminConflictException("retention policy version conflict");
        long now = System.currentTimeMillis();
        if (policy == null) {
            policy = new AdminRetentionPolicyEntity();
            policy.setId(POLICY_ID);
        }
        policy.setAuditDays(request.auditDays());
        policy.setMessageDays(request.messageDays());
        policy.setToolResultDays(request.toolResultDays());
        policy.setMetricsDays(request.metricsDays());
        policy.setContentMode(normalizedContentMode);
        policy.setVersion(current + 1L);
        policy.setEffectiveAt(now);
        policy.setUpdatedBy(principal.userId());
        policy.setIdempotencyKey(idempotencyKey);
        policy.setIdempotencyPayloadHash(hash);
        policy.setReason(request.reason().trim());
        AdminRetentionPolicyEntity saved = repository.saveAndFlush(policy);
        auditService.record(principal, "admin.retention.update", "RETENTION", POLICY_ID + "", null, null,
            "SUCCESS", request.reason(), "version=" + saved.getVersion(), request.idempotencyKey(), hash);
        return toDto(saved);
    }

    private AdminRetentionDtos.Policy toDto(AdminRetentionPolicyEntity policy) {
        return new AdminRetentionDtos.Policy(policy.getVersion() == null ? 0L : policy.getVersion(), policy.getAuditDays(),
            policy.getMessageDays(), policy.getToolResultDays(), policy.getMetricsDays(), policy.getContentMode(),
            policy.getEffectiveAt() == null ? null : Instant.ofEpochMilli(policy.getEffectiveAt()),
            policy.getUpdatedBy() == null ? null : policy.getUpdatedBy().toString());
    }

    private void validate(AdminRetentionDtos.UpdateRequest request) {
        if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0
            || request.idempotencyKey() == null || request.idempotencyKey().isBlank() || request.idempotencyKey().trim().length() > 128
            || request.reason() == null || request.reason().isBlank() || request.reason().trim().length() > 512
            || !Boolean.TRUE.equals(request.confirmed())) {
            throw new IllegalArgumentException("retention update requires confirmation, version, idempotencyKey and reason");
        }
        validateDays(request.auditDays(), "auditDays", 3650);
        validateDays(request.messageDays(), "messageDays", 3650);
        validateDays(request.toolResultDays(), "toolResultDays", 3650);
        validateDays(request.metricsDays(), "metricsDays", 3650);
        normalizeContentMode(request.contentMode());
    }

    private void validateDays(Integer value, String field, int max) {
        if (value == null || value < 1 || value > max) throw new IllegalArgumentException(field + " is invalid");
    }

    private String normalizeContentMode(String value) {
        String normalized = value == null || value.isBlank() ? "REDACTED" : value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.equals("REDACTED") && !normalized.equals("METADATA_ONLY")) throw new IllegalArgumentException("contentMode is invalid");
        return normalized;
    }
}
