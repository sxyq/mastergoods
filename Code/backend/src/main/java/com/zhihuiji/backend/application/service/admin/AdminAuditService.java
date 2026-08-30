package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.common.AdminConflictException;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.admin.AdminAuditDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.domain.entity.AdminAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeQuery;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Persistent administrator audit boundary shared by reads and mutations. */
@Service
public class AdminAuditService {
    private static final int MAX_REASON_LENGTH = 512;
    private static final int MAX_SUMMARY_LENGTH = 1000;
    private static final int MAX_FILTER_LENGTH = 64;
    private static final String SECURITY_DENIAL_ACTION = "admin.access.denied";
    private static final String ANONYMOUS_ROLE = "ANONYMOUS";
    private static final String NON_ADMIN_ROLE = "NON_ADMIN";

    private final AdminAuditEventRepository repository;
    private final AdminAuthorizationService authorizationService;

    public AdminAuditService(AdminAuditEventRepository repository, AdminAuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public AdminAuditEventEntity record(
        AdminPrincipal principal,
        String action,
        String resourceType,
        String resourceId,
        Long ownerUserId,
        Long storeId,
        String result,
        String reason,
        String summary,
        String idempotencyKey,
        String payloadHash
    ) {
        if (principal == null) {
            return null;
        }
        if (idempotencyKey != null) {
            AdminAuditEventEntity existing = repository
                .findByAdminUserIdAndIdempotencyKey(principal.userId(), idempotencyKey)
                .orElse(null);
            if (existing != null) {
                if (payloadHash != null && !payloadHash.equals(existing.getIdempotencyPayloadHash())) {
                    throw new AdminConflictException("idempotency key was already used with a different payload");
                }
                return existing;
            }
        }
        long now = System.currentTimeMillis();
        AdminAuditEventEntity entity = new AdminAuditEventEntity();
        entity.setEventId(UUID.randomUUID().toString());
        entity.setAdminUserId(principal.userId());
        entity.setRoleCode(principal.role().name());
        entity.setAction(normalizeRequired(action, "audit action is required", MAX_FILTER_LENGTH));
        entity.setResourceType(normalizeOptional(resourceType, MAX_FILTER_LENGTH));
        entity.setResourceId(normalizeOptional(resourceId, 128));
        entity.setOwnerUserId(ownerUserId);
        entity.setStoreId(storeId);
        entity.setResult(normalizeRequired(result, "audit result is required", 32));
        entity.setReason(normalizeOptional(reason, MAX_REASON_LENGTH));
        entity.setSummary(normalizeOptional(summary, MAX_SUMMARY_LENGTH));
        entity.setIdempotencyKey(normalizeOptional(idempotencyKey, 128));
        entity.setIdempotencyPayloadHash(payloadHash);
        entity.setOccurredAt(now);
        RequestMetadata metadata = requestMetadata();
        entity.setSourceIp(metadata.sourceIp());
        entity.setUserAgentSummary(metadata.userAgent());
        entity.setRequestId(metadata.requestId());
        return repository.save(entity);
    }

    public AdminAuditEventEntity recordRead(AdminPrincipal principal, String action, String resourceType, String resourceId,
                                            Long ownerUserId, Long storeId, String summary) {
        return record(principal, action, resourceType, resourceId, ownerUserId, storeId, "SUCCESS", null, summary, null, null);
    }

    /** Records an administrator-endpoint denial when no valid admin principal exists. */
    @Transactional
    public AdminAuditEventEntity recordSecurityDenial(
        HttpServletRequest request,
        Long actorUserId,
        String roleCode,
        String reason
    ) {
        Long normalizedActorUserId = actorUserId != null && actorUserId > 0 ? actorUserId : null;
        if (normalizedActorUserId == null) {
            // Anonymous events are intentionally stored but excluded from user-facing
            // scope queries: they have no accountable administrator or business scope.
            return repository.save(securityDenial(request, null, ANONYMOUS_ROLE, reason));
        }
        return repository.save(securityDenial(request, normalizedActorUserId, normalizedRole(roleCode), reason));
    }

    private AdminAuditEventEntity securityDenial(
        HttpServletRequest request,
        Long actorUserId,
        String roleCode,
        String reason
    ) {
        AdminAuditEventEntity entity = new AdminAuditEventEntity();
        entity.setEventId(UUID.randomUUID().toString());
        entity.setAdminUserId(actorUserId);
        entity.setRoleCode(roleCode);
        entity.setAction(SECURITY_DENIAL_ACTION);
        entity.setResourceType("ADMIN_ENDPOINT");
        entity.setResourceId(bounded(request == null ? null : request.getRequestURI(), 128));
        entity.setResult("DENIED");
        entity.setReason(bounded(reason, MAX_REASON_LENGTH));
        entity.setSummary(bounded(request == null ? null : request.getMethod(), MAX_SUMMARY_LENGTH));
        entity.setOccurredAt(System.currentTimeMillis());
        RequestMetadata metadata = securityRequestMetadata(request);
        entity.setSourceIp(metadata.sourceIp());
        entity.setUserAgentSummary(metadata.userAgent());
        entity.setRequestId(metadata.requestId());
        return entity;
    }

    public String payloadHash(String payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("audit hash is unavailable", ex);
        }
    }

    public AdminAuditEventEntity findIdempotent(AdminPrincipal principal, String idempotencyKey, String payloadHash) {
        if (principal == null || idempotencyKey == null || idempotencyKey.isBlank()) return null;
        AdminAuditEventEntity existing = repository.findByAdminUserIdAndIdempotencyKey(principal.userId(), idempotencyKey.trim()).orElse(null);
        if (existing == null) return null;
        if (payloadHash != null && !payloadHash.equals(existing.getIdempotencyPayloadHash())) {
            throw new AdminConflictException("idempotency key was already used with a different payload");
        }
        return existing;
    }

    @Transactional
    public AdminPageDtos.PageResponse<AdminAuditDtos.Event> list(
        AdminPrincipal principal,
        String eventId,
        String action,
        String resourceType,
        String result,
        Instant from,
        Instant to,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal, AdminPermission.AUDIT_READ, requestedOwnerUserId, requestedStoreId
        );
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        String normalizedEventId = normalizeOptional(eventId, 128);
        String normalizedAction = normalizeQueryFilter(action, MAX_FILTER_LENGTH);
        String normalizedResourceType = normalizeQueryFilter(resourceType, MAX_FILTER_LENGTH);
        String normalizedResult = normalizeQueryFilter(result, 32);
        Page<AdminAuditEventEntity> resultPage = normalizedEventId == null
            ? repository.findVisible(
                principal.userId(), query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds(),
                normalizedAction, normalizedResourceType, normalizedResult,
                from == null ? null : from.toEpochMilli(), to == null ? null : to.toEpochMilli(),
                PaginationUtils.pageable(page, size))
            : repository.findVisibleByEventId(
                normalizedEventId, principal.userId(), query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds(),
                PaginationUtils.pageable(page, size));
        var items = resultPage.getContent().stream().map(this::toDto).toList();
        recordRead(principal, "admin.audit.read", "AUDIT", normalizedEventId, requestedOwnerUserId, requestedStoreId,
            "page=" + resultPage.getNumber() + ",size=" + resultPage.getSize());
        return new AdminPageDtos.PageResponse<>(items, resultPage.getNumber(), resultPage.getSize(),
            resultPage.getTotalElements(), resultPage.hasNext(), Instant.now(), AdminScopeDtos.Scope.from(scope),
            scope.allOwners() ? "COMPLETE" : "PARTIAL");
    }

    /** Compatibility overload for callers that do not provide an owner/store filter. */
    @Transactional
    public AdminPageDtos.PageResponse<AdminAuditDtos.Event> list(
        AdminPrincipal principal,
        String action,
        String resourceType,
        String result,
        Instant from,
        Instant to,
        Integer page,
        Integer size
    ) {
        return list(principal, null, action, resourceType, result, from, to, null, null, page, size);
    }

    /** Compatibility overload for callers that provide an owner/store filter without eventId. */
    @Transactional
    public AdminPageDtos.PageResponse<AdminAuditDtos.Event> list(
        AdminPrincipal principal,
        String action,
        String resourceType,
        String result,
        Instant from,
        Instant to,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        return list(principal, null, action, resourceType, result, from, to,
            requestedOwnerUserId, requestedStoreId, page, size);
    }

    /** Compatibility overload for callers that need exact event tracing without an owner/store filter. */
    @Transactional
    public AdminPageDtos.PageResponse<AdminAuditDtos.Event> listByEventId(
        AdminPrincipal principal,
        String eventId,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        return list(principal, eventId, null, null, null, null, null,
            requestedOwnerUserId, requestedStoreId, page, size);
    }

    public AdminAuditDtos.Event toDto(AdminAuditEventEntity entity) {
        return new AdminAuditDtos.Event(
            entity.getEventId(), entity.getAction(), id(entity.getAdminUserId()), entity.getResourceType(), entity.getResourceId(),
            entity.getResult(), entity.getReason(), instant(entity.getOccurredAt()), entity.getRoleCode(), id(entity.getOwnerUserId()),
            id(entity.getStoreId()), entity.getSourceIp(), entity.getUserAgentSummary(), entity.getRequestId(), entity.getSummary()
        );
    }

    private RequestMetadata requestMetadata() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new RequestMetadata(null, null, null);
        }
        HttpServletRequest request = attributes.getRequest();
        String requestId = request.getHeader("X-Request-ID");
        return new RequestMetadata(
            normalizeOptional(request.getRemoteAddr(), 64),
            normalizeOptional(request.getHeader("User-Agent"), 256),
            normalizeOptional(requestId, 128)
        );
    }

    private RequestMetadata securityRequestMetadata(HttpServletRequest request) {
        if (request == null) return new RequestMetadata(null, null, null);
        return new RequestMetadata(
            bounded(request.getRemoteAddr(), 64),
            bounded(request.getHeader("User-Agent"), 256),
            bounded(request.getHeader("X-Request-ID"), 128)
        );
    }

    private String normalizedRole(String roleCode) {
        String normalized = bounded(roleCode, 32);
        return normalized == null ? NON_ADMIN_ROLE : normalized;
    }

    private String bounded(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String normalizeRequired(String value, String message, int maxLength) {
        String normalized = normalizeOptional(value, maxLength);
        if (normalized == null) throw new IllegalArgumentException(message);
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        if (normalized.length() > maxLength) throw new IllegalArgumentException("audit field is too long");
        return normalized;
    }

    private String normalizeQueryFilter(String value, int maxLength) {
        String normalized = normalizeOptional(value, maxLength);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String id(Long value) { return value == null ? null : value.toString(); }
    private Instant instant(Long value) { return value == null ? null : Instant.ofEpochMilli(value); }

    private record RequestMetadata(String sourceIp, String userAgent, String requestId) {}
}
