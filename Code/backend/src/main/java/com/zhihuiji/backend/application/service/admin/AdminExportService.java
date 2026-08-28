package com.zhihuiji.backend.application.service.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.common.AdminConflictException;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.admin.AdminExportDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.domain.entity.AdminAuditEventEntity;
import com.zhihuiji.backend.domain.entity.AdminExportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminExportJobRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeQuery;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bounded, already-redacted CSV exports with short-lived download state. */
@Service
public class AdminExportService {
    private static final int MAX_EXPORT_ROWS = 10_000;
    private static final long TTL_MILLIS = 24L * 60 * 60 * 1000;
    private static final Set<String> AUDIT_FIELDS = Set.of(
        "eventId", "action", "resourceType", "resourceId", "result", "occurredAt", "actorAdminUserId", "ownerUserId", "storeId"
    );

    private final AdminAuthorizationService authorizationService;
    private final AdminExportJobRepository jobRepository;
    private final AdminAuditEventRepository auditRepository;
    private final AdminAuditService auditService;
    private final ObjectMapper objectMapper;

    public AdminExportService(AdminAuthorizationService authorizationService,
                              AdminExportJobRepository jobRepository,
                              AdminAuditEventRepository auditRepository,
                              AdminAuditService auditService,
                              ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.jobRepository = jobRepository;
        this.auditRepository = auditRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminExportDtos.Job> list(AdminPrincipal principal, Integer page, Integer size) {
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.EXPORT, null, null);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        Page<AdminExportJobEntity> jobs = jobRepository.findVisible(principal.userId(), query.allOwners(), query.ownerUserIds(),
            query.allStores(), query.storeIds(), PaginationUtils.pageable(page, size));
        var items = jobs.getContent().stream().map(this::toDto).toList();
        return new AdminPageDtos.PageResponse<>(items, jobs.getNumber(), jobs.getSize(), jobs.getTotalElements(), jobs.hasNext(),
            Instant.now(), AdminScopeDtos.Scope.from(scope), scope.allOwners() || scope.storeIds().isEmpty() ? "COMPLETE" : "PARTIAL");
    }

    @Transactional
    public AdminExportDtos.Job create(AdminPrincipal principal, AdminExportDtos.CreateRequest request) {
        if (request == null) throw new IllegalArgumentException("export request is required");
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.EXPORT, request.ownerUserId(), request.storeId());
        validate(request);
        if (!"audit_events".equals(request.exportType().trim())) throw new IllegalArgumentException("exportType is not supported");
        List<String> fields = normalizeFields(request.fields());
        if (request.from() != null && request.to() != null && !request.from().isBefore(request.to())) {
            throw new IllegalArgumentException("from must be before to");
        }
        String payload = request.exportType().trim() + "|" + fields + "|" + request.from() + "|" + request.to() + "|"
            + request.ownerUserId() + "|" + request.storeId();
        String hash = auditService.payloadHash(payload);
        AdminExportJobEntity existing = jobRepository.findByAdminUserIdAndIdempotencyKey(principal.userId(), request.idempotencyKey().trim()).orElse(null);
        if (existing != null) {
            if (!hash.equals(existing.getIdempotencyPayloadHash())) throw new AdminConflictException("idempotency key was already used with a different payload");
            return toDto(existing);
        }
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        Page<AdminAuditEventEntity> source = auditRepository.findVisible(principal.userId(), query.allOwners(), query.ownerUserIds(),
            query.allStores(), query.storeIds(), null, null, null, request.from() == null ? null : request.from().toEpochMilli(),
            request.to() == null ? null : request.to().toEpochMilli(), PageRequest.of(0, MAX_EXPORT_ROWS));
        long now = System.currentTimeMillis();
        AdminExportJobEntity job = new AdminExportJobEntity();
        job.setExportId(UUID.randomUUID().toString());
        job.setAdminUserId(principal.userId());
        job.setExportType(request.exportType().trim());
        job.setFieldsJson(writeFields(fields));
        job.setScopeOwnerUserId(request.ownerUserId());
        job.setScopeStoreId(request.storeId());
        job.setStatus("READY");
        job.setContentCsv(csv(fields, source.getContent()));
        job.setIdempotencyKey(request.idempotencyKey().trim());
        job.setIdempotencyPayloadHash(hash);
        job.setCreatedAt(now);
        job.setExpiresAt(now + TTL_MILLIS);
        job.setCompletedAt(now);
        job.setDownloadCount(0);
        AdminExportJobEntity saved = jobRepository.saveAndFlush(job);
        auditService.record(principal, "admin.export.create", "EXPORT", saved.getExportId(), request.ownerUserId(), request.storeId(),
            "SUCCESS", request.reason(), "rows=" + source.getContent().size(), request.idempotencyKey(), hash);
        return toDto(saved);
    }

    @Transactional
    public AdminExportDtos.Job get(AdminPrincipal principal, String exportId) {
        AdminExportJobEntity job = visibleJob(principal, exportId);
        return toDto(job);
    }

    @Transactional
    public byte[] download(AdminPrincipal principal, String exportId) {
        AdminExportJobEntity job = visibleJob(principal, exportId);
        long now = System.currentTimeMillis();
        if (job.getExpiresAt() == null || job.getExpiresAt() <= now) {
            job.setStatus("EXPIRED");
            jobRepository.save(job);
            auditService.record(principal, "admin.export.download", "EXPORT", exportId, job.getScopeOwnerUserId(), job.getScopeStoreId(),
                "EXPIRED", null, "export expired", null, null);
            throw new IllegalArgumentException("export has expired");
        }
        if (!"READY".equalsIgnoreCase(job.getStatus())) throw new IllegalArgumentException("export is not ready");
        job.setDownloadedAt(now);
        job.setDownloadCount((job.getDownloadCount() == null ? 0 : job.getDownloadCount()) + 1);
        jobRepository.save(job);
        auditService.recordRead(principal, "admin.export.download", "EXPORT", exportId, job.getScopeOwnerUserId(), job.getScopeStoreId(), "downloaded");
        return (job.getContentCsv() == null ? "" : job.getContentCsv()).getBytes(StandardCharsets.UTF_8);
    }

    private AdminExportJobEntity visibleJob(AdminPrincipal principal, String exportId) {
        if (exportId == null || exportId.isBlank() || exportId.length() > 128) throw new IllegalArgumentException("exportId is invalid");
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.EXPORT, null, null);
        AdminExportJobEntity job = jobRepository.findByExportId(exportId.trim()).orElseThrow(() -> new AccessDeniedException("export resource not visible"));
        Long owner = job.getScopeOwnerUserId();
        Long store = job.getScopeStoreId();
        if (!scope.allOwners() && owner != null && !scope.ownerUserIds().contains(owner)) throw new AccessDeniedException("export resource not visible");
        if (!scope.allOwners() && store != null && !scope.storeIds().contains(store)) throw new AccessDeniedException("export resource not visible");
        if (!scope.allOwners() && owner == null && store == null && !principal.userId().equals(job.getAdminUserId())) throw new AccessDeniedException("export resource not visible");
        return job;
    }

    private AdminExportDtos.Job toDto(AdminExportJobEntity job) {
        return new AdminExportDtos.Job(job.getExportId(), job.getExportType(), parseFields(job.getFieldsJson()), job.getStatus(),
            instant(job.getCreatedAt()), instant(job.getExpiresAt()), instant(job.getCompletedAt()),
            "/v2/admin/exports/" + job.getExportId() + "/download", true, job.getErrorSummary(),
            job.getDownloadCount() == null ? 0 : job.getDownloadCount());
    }

    private List<String> normalizeFields(List<String> values) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("fields are required");
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || !AUDIT_FIELDS.contains(value.trim())) throw new IllegalArgumentException("export field is not allowed");
            fields.add(value.trim());
        }
        return List.copyOf(fields);
    }

    private String csv(List<String> fields, List<AdminAuditEventEntity> rows) {
        StringBuilder out = new StringBuilder(String.join(",", fields)).append('\n');
        for (AdminAuditEventEntity row : rows) {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) out.append(',');
                out.append(escape(csvValue(row, fields.get(i))));
            }
            out.append('\n');
        }
        return out.toString();
    }

    private String csvValue(AdminAuditEventEntity row, String field) {
        return switch (field) {
            case "eventId" -> row.getEventId();
            case "action" -> row.getAction();
            case "resourceType" -> row.getResourceType();
            case "resourceId" -> row.getResourceId();
            case "result" -> row.getResult();
            case "occurredAt" -> instant(row.getOccurredAt()) == null ? null : instant(row.getOccurredAt()).toString();
            case "actorAdminUserId" -> row.getAdminUserId() == null ? null : row.getAdminUserId().toString();
            case "ownerUserId" -> row.getOwnerUserId() == null ? null : row.getOwnerUserId().toString();
            case "storeId" -> row.getStoreId() == null ? null : row.getStoreId().toString();
            default -> null;
        };
    }

    private String escape(String value) {
        if (value == null) return "";
        String normalized = value.replace("\r", " ").replace("\n", " ");
        return normalized.contains(",") || normalized.contains("\"") ? "\"" + normalized.replace("\"", "\"\"") + "\"" : normalized;
    }

    private void validate(AdminExportDtos.CreateRequest request) {
        if (request.exportType() == null || request.exportType().isBlank() || request.exportType().length() > 64
            || request.reason() == null || request.reason().isBlank() || request.reason().trim().length() > 512
            || request.idempotencyKey() == null || request.idempotencyKey().isBlank() || request.idempotencyKey().trim().length() > 128) {
            throw new IllegalArgumentException("export requires type, fields, reason and idempotencyKey");
        }
    }

    private String writeFields(List<String> fields) { try { return objectMapper.writeValueAsString(fields); } catch (Exception ex) { throw new IllegalStateException("export fields serialization failed", ex); } }
    private List<String> parseFields(String value) { try { return objectMapper.readValue(value == null ? "[]" : value, new TypeReference<List<String>>() {}); } catch (Exception ignored) { return List.of(); } }
    private Instant instant(Long value) { return value == null ? null : Instant.ofEpochMilli(value); }
}
