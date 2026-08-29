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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Scheduled;
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
            Instant.now(), AdminScopeDtos.Scope.from(scope), scope.allOwners() ? "COMPLETE" : "PARTIAL");
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
        long now = System.currentTimeMillis();
        AdminExportJobEntity job = new AdminExportJobEntity();
        job.setExportId(UUID.randomUUID().toString());
        job.setAdminUserId(principal.userId());
        job.setExportType(request.exportType().trim());
        job.setFieldsJson(writeFields(fields));
        job.setScopeOwnerUserId(request.ownerUserId());
        job.setScopeStoreId(request.storeId());
        job.setScopeOwnerUserIdsJson(writeIds(scope.ownerUserIds()));
        job.setScopeStoreIdsJson(writeIds(scope.storeIds()));
        job.setScopeAllOwners(query.allOwners());
        job.setScopeAllStores(query.allStores());
        job.setRequestedFromAt(request.from() == null ? null : request.from().toEpochMilli());
        job.setRequestedToAt(request.to() == null ? null : request.to().toEpochMilli());
        job.setStatus("PENDING");
        job.setContentCsv(null);
        job.setIdempotencyKey(request.idempotencyKey().trim());
        job.setIdempotencyPayloadHash(hash);
        job.setCreatedAt(now);
        job.setExpiresAt(now + TTL_MILLIS);
        job.setCompletedAt(null);
        job.setDownloadCount(0);
        AdminExportJobEntity saved = jobRepository.saveAndFlush(job);
        auditService.record(principal, "admin.export.create", "EXPORT", saved.getExportId(), request.ownerUserId(), request.storeId(),
            "SUCCESS", request.reason(), "status=PENDING", request.idempotencyKey(), hash);
        return toDto(saved);
    }

    /** Processes one claimed job using only the authorization snapshot captured at creation. */
    @Transactional
    public void processClaimed(String exportId) {
        AdminExportJobEntity job = jobRepository.findByExportId(exportId).orElse(null);
        if (job == null || !"RUNNING".equalsIgnoreCase(job.getStatus())) return;
        long now = System.currentTimeMillis();
        try {
            if (job.getExpiresAt() == null || job.getExpiresAt() <= now) {
                job.setStatus("EXPIRED");
                job.setContentCsv(null);
                job.setCompletedAt(now);
                jobRepository.save(job);
                return;
            }
            AdminScopeQuery snapshot = snapshot(job);
            Page<AdminAuditEventEntity> source = auditRepository.findVisible(
                job.getAdminUserId(), snapshot.allOwners(), snapshot.ownerUserIds(), snapshot.allStores(), snapshot.storeIds(),
                null, null, null, job.getRequestedFromAt(), job.getRequestedToAt(), PageRequest.of(0, MAX_EXPORT_ROWS)
            );
            job.setContentCsv(csv(parseFields(job.getFieldsJson()), source.getContent()));
            job.setStatus("READY");
            job.setCompletedAt(now);
            job.setErrorSummary(null);
            jobRepository.save(job);
        } catch (RuntimeException ex) {
            job.setStatus("FAILED");
            job.setCompletedAt(now);
            job.setContentCsv(null);
            job.setErrorSummary(safeError(ex));
            jobRepository.save(job);
        }
    }

    @Transactional
    public AdminExportDtos.Job get(AdminPrincipal principal, String exportId) {
        AdminExportJobEntity job = visibleJob(principal, exportId);
        markExpired(job);
        return toDto(job);
    }

    @Transactional
    public byte[] download(AdminPrincipal principal, String exportId) {
        AdminExportJobEntity job = visibleJob(principal, exportId);
        long now = System.currentTimeMillis();
        if (job.getExpiresAt() == null || job.getExpiresAt() <= now) {
            markExpired(job);
            auditService.record(principal, "admin.export.download", "EXPORT", exportId, job.getScopeOwnerUserId(), job.getScopeStoreId(),
                "EXPIRED", null, "export expired", null, null);
            throw new IllegalArgumentException("export has expired");
        }
        if (!"READY".equalsIgnoreCase(job.getStatus())) {
            auditService.record(principal, "admin.export.download", "EXPORT", exportId, job.getScopeOwnerUserId(), job.getScopeStoreId(),
                "REJECTED", null, "export status=" + job.getStatus(), null, null);
            throw new IllegalArgumentException("export is not ready");
        }
        int previousDownloadCount = job.getDownloadCount() == null ? 0 : job.getDownloadCount();
        if (previousDownloadCount == 0) job.setDownloadedAt(now);
        job.setDownloadCount(previousDownloadCount + 1);
        jobRepository.save(job);
        auditService.recordRead(principal, "admin.export.download", "EXPORT", exportId, job.getScopeOwnerUserId(), job.getScopeStoreId(),
            previousDownloadCount == 0 ? "downloaded" : "redownloaded");
        return (job.getContentCsv() == null ? "" : job.getContentCsv()).getBytes(StandardCharsets.UTF_8);
    }

    /** Removes expired export payloads while retaining the status and audit trail. */
    @Scheduled(
        fixedDelayString = "${admin.export.cleanup.fixed-delay-ms:3600000}",
        initialDelayString = "${admin.export.cleanup.initial-delay-ms:3600000}"
    )
    @Transactional
    public int cleanupExpired() {
        return jobRepository.expireAndClearExpired(System.currentTimeMillis());
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
        boolean downloadable = "READY".equalsIgnoreCase(job.getStatus())
            && job.getExpiresAt() != null && job.getExpiresAt() > System.currentTimeMillis();
        return new AdminExportDtos.Job(job.getExportId(), job.getExportType(), parseFields(job.getFieldsJson()), job.getStatus(),
            instant(job.getCreatedAt()), instant(job.getExpiresAt()), instant(job.getCompletedAt()),
            downloadable ? "/v2/admin/exports/" + job.getExportId() + "/download" : null, true, job.getErrorSummary(),
            job.getDownloadCount() == null ? 0 : job.getDownloadCount());
    }

    private void markExpired(AdminExportJobEntity job) {
        if (job.getExpiresAt() != null && job.getExpiresAt() <= System.currentTimeMillis()
            && !"EXPIRED".equalsIgnoreCase(job.getStatus())) {
            job.setStatus("EXPIRED");
            job.setContentCsv(null);
            jobRepository.save(job);
        }
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
    private String writeIds(Set<Long> ids) { try { return objectMapper.writeValueAsString(ids == null ? List.of() : ids); } catch (Exception ex) { throw new IllegalStateException("export scope serialization failed", ex); } }
    private List<String> parseFields(String value) { try { return objectMapper.readValue(value == null ? "[]" : value, new TypeReference<List<String>>() {}); } catch (Exception ignored) { return List.of(); } }
    private AdminScopeQuery snapshot(AdminExportJobEntity job) {
        Boolean allOwners = job.getScopeAllOwners();
        Boolean allStores = job.getScopeAllStores();
        if (allOwners == null || allStores == null || job.getScopeOwnerUserIdsJson() == null || job.getScopeStoreIdsJson() == null) {
            throw new IllegalStateException("export authorization snapshot is unavailable");
        }
        Set<Long> ownerIds = parseIds(job.getScopeOwnerUserIdsJson());
        Set<Long> storeIds = parseIds(job.getScopeStoreIdsJson());
        if (!allOwners && ownerIds.isEmpty() && job.getScopeOwnerUserId() == null && job.getScopeStoreId() == null) {
            throw new IllegalStateException("export authorization snapshot is empty");
        }
        if (!allOwners && ownerIds.isEmpty() && job.getScopeOwnerUserId() != null) ownerIds = Set.of(job.getScopeOwnerUserId());
        if (!allStores && storeIds.isEmpty() && job.getScopeStoreId() != null) storeIds = Set.of(job.getScopeStoreId());
        return new AdminScopeQuery(allOwners, ownerIds, storeIds, allStores);
    }

    private Set<Long> parseIds(String value) {
        try {
            List<Long> values = objectMapper.readValue(value, new TypeReference<List<Long>>() {});
            return new LinkedHashSet<>(values == null ? List.of() : values.stream().filter(id -> id != null && id > 0).toList());
        } catch (Exception ex) {
            throw new IllegalStateException("export authorization snapshot is invalid");
        }
    }

    private String safeError(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return "export processing failed";
        return message.length() > 512 ? message.substring(0, 512) : message;
    }
    private Instant instant(Long value) { return value == null ? null : Instant.ofEpochMilli(value); }
}
