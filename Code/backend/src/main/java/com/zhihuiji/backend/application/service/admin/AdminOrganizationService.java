package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.common.AdminConflictException;
import com.zhihuiji.backend.api.dto.admin.AdminOrganizationDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.application.service.store.StoreAccessPolicy;
import com.zhihuiji.backend.domain.entity.SessionEntity;
import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.domain.entity.StoreMembershipEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeQuery;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminStoreQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminUserQueryRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only organization queries for API-ADM-03 and API-ADM-04. */
@Service
public class AdminOrganizationService {
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final AdminAuthorizationService authorizationService;
    private final AdminUserQueryRepository userQueryRepository;
    private final AdminStoreQueryRepository storeQueryRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreMembershipRepository membershipRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService auditService;

    @Autowired
    public AdminOrganizationService(
        AdminAuthorizationService authorizationService,
        AdminUserQueryRepository userQueryRepository,
        AdminStoreQueryRepository storeQueryRepository,
        UserRepository userRepository,
        StoreRepository storeRepository,
        StoreMembershipRepository membershipRepository,
        SessionRepository sessionRepository,
        PasswordEncoder passwordEncoder,
        AdminAuditService auditService
    ) {
        this.authorizationService = authorizationService;
        this.userQueryRepository = userQueryRepository;
        this.storeQueryRepository = storeQueryRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.membershipRepository = membershipRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /** Compatibility constructor for read-only unit tests. */
    public AdminOrganizationService(
        AdminAuthorizationService authorizationService,
        AdminUserQueryRepository userQueryRepository,
        AdminStoreQueryRepository storeQueryRepository
    ) {
        this(authorizationService, userQueryRepository, storeQueryRepository, null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminOrganizationDtos.UserSummary> listUsers(
        AdminPrincipal principal,
        String keyword,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal,
            AdminPermission.USER_READ,
            requestedOwnerUserId,
            requestedStoreId
        );
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        Pageable pageable = PaginationUtils.pageable(page, size);
        Page<UserEntity> result = userQueryRepository.findUsers(
            queryScope.allOwners(),
            queryScope.ownerUserIds(),
            queryScope.storeIds(),
            queryScope.allStores(),
            normalizeKeyword(keyword),
            pageable
        );
        List<AdminOrganizationDtos.UserSummary> items = result.getContent().stream()
            .map(this::toUserSummary)
            .toList();
        return pageResponse(result, items, scope);
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminOrganizationDtos.StoreSummary> listStores(
        AdminPrincipal principal,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal,
            AdminPermission.STORE_READ,
            requestedOwnerUserId,
            requestedStoreId
        );
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        Page<AdminStoreQueryRepository.StoreProjection> result = storeQueryRepository.findStores(
            queryScope.allOwners(),
            queryScope.ownerUserIds(),
            queryScope.storeIds(),
            queryScope.allStores(),
            PaginationUtils.pageable(page, size)
        );
        List<AdminOrganizationDtos.StoreSummary> items = result.getContent().stream()
            .map(this::toStoreSummary)
            .toList();
        return pageResponse(result, items, scope);
    }

    @Transactional(readOnly = true)
    public AdminOrganizationDtos.UserSummary getUser(AdminPrincipal principal, Long userId, Long ownerUserId, Long storeId) {
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.USER_READ, ownerUserId, storeId);
        UserEntity user = visibleUser(userId, scope);
        return toUserSummary(user);
    }

    @Transactional
    public AdminOrganizationDtos.UserSummary updateUser(AdminPrincipal principal, Long userId,
                                                         AdminOrganizationDtos.UserPatchRequest request) {
        if (request == null) throw new IllegalArgumentException("user update request is required");
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.USER_MANAGE, request.ownerUserId(), request.storeId());
        requireMutation(request.expectedVersion(), request.idempotencyKey(), request.reason(), request.confirmed());
        String hash = auditService.payloadHash(String.valueOf(request.nickname()) + "|" + request.status() + "|" + request.keepSessions() + "|" + userId);
        if (auditService.findIdempotent(principal, request.idempotencyKey(), hash) != null) {
            return toUserSummary(visibleUser(userId, scope));
        }
        UserEntity user = visibleUser(userId, scope);
        long version = user.getAdminVersion() == null ? 0L : user.getAdminVersion();
        if (!Objects.equals(request.expectedVersion(), version)) throw new AdminConflictException("user version conflict");
        if (request.status() != null && request.status() != 0 && request.status() != 1) throw new IllegalArgumentException("status is invalid");
        if (request.nickname() != null) {
            if (request.nickname().isBlank() || request.nickname().trim().length() > 64) throw new IllegalArgumentException("nickname is invalid");
            user.setNickname(request.nickname().trim());
        }
        int status = request.status() == null ? (user.getStatus() == null ? 1 : user.getStatus()) : request.status();
        user.setStatus(status);
        user.setAdminVersion(version + 1L);
        user.setUpdatedAt(System.currentTimeMillis());
        UserEntity saved = userRepository.saveAndFlush(user);
        if (status == 0 || Boolean.FALSE.equals(request.keepSessions())) invalidateSessions(userId);
        Long owner = resolveOwner(userId, scope, request.ownerUserId(), request.storeId());
        auditService.record(principal, "admin.user.update", "USER", userId.toString(), owner, request.storeId(), "SUCCESS",
            request.reason(), "status=" + status + ",version=" + saved.getAdminVersion(), request.idempotencyKey(), hash);
        return toUserSummary(saved);
    }

    @Transactional(readOnly = true)
    public AdminOrganizationDtos.StoreSummary getStore(AdminPrincipal principal, Long storeId, Long ownerUserId) {
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.STORE_READ, ownerUserId, storeId);
        StoreEntity store = visibleStore(storeId, scope);
        return new AdminOrganizationDtos.StoreSummary(id(store.getId()), id(store.getOwnerUserId()), store.getStoreName(),
            status(store.getStatus()), memberCount(store.getId(), store.getOwnerUserId()), instant(store.getCreatedAt()), instant(store.getUpdatedAt()),
            store.getAdminVersion() == null ? 0L : store.getAdminVersion());
    }

    @Transactional
    public AdminOrganizationDtos.StoreSummary updateStore(AdminPrincipal principal, Long storeId,
                                                           AdminOrganizationDtos.StorePatchRequest request) {
        if (request == null) throw new IllegalArgumentException("store update request is required");
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.STORE_MANAGE, request.ownerUserId(), storeId);
        requireMutation(request.expectedVersion(), request.idempotencyKey(), request.reason(), request.confirmed());
        String hash = auditService.payloadHash(String.valueOf(request.name()) + "|" + request.status() + "|" + storeId);
        if (auditService.findIdempotent(principal, request.idempotencyKey(), hash) != null) return getStore(principal, storeId, request.ownerUserId());
        StoreEntity store = visibleStore(storeId, scope);
        long version = store.getAdminVersion() == null ? 0L : store.getAdminVersion();
        if (!Objects.equals(request.expectedVersion(), version)) throw new AdminConflictException("store version conflict");
        if (request.name() != null) {
            if (request.name().isBlank() || request.name().trim().length() > 128) throw new IllegalArgumentException("store name is invalid");
            store.setStoreName(request.name().trim());
        }
        if (request.status() != null && request.status() != 0 && request.status() != 1) throw new IllegalArgumentException("status is invalid");
        if (request.status() != null) store.setStatus(request.status());
        store.setAdminVersion(version + 1L);
        store.setUpdatedAt(System.currentTimeMillis());
        StoreEntity saved = storeRepository.saveAndFlush(store);
        auditService.record(principal, "admin.store.update", "STORE", storeId.toString(), saved.getOwnerUserId(), storeId, "SUCCESS",
            request.reason(), "status=" + saved.getStatus() + ",version=" + saved.getAdminVersion(), request.idempotencyKey(), hash);
        return new AdminOrganizationDtos.StoreSummary(id(saved.getId()), id(saved.getOwnerUserId()), saved.getStoreName(), status(saved.getStatus()),
            memberCount(saved.getId(), saved.getOwnerUserId()), instant(saved.getCreatedAt()), instant(saved.getUpdatedAt()), saved.getAdminVersion());
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminOrganizationDtos.MemberSummary> listMembers(AdminPrincipal principal, Long storeId,
                                                                                         Long ownerUserId, Integer page, Integer size) {
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.STORE_READ, ownerUserId, storeId);
        StoreEntity store = visibleStore(storeId, scope);
        Pageable pageable = PaginationUtils.pageable(page, size);
        Page<StoreMembershipEntity> result = membershipRepository.findByOwnerUserIdAndStoreIdOrderByCreatedAtAsc(
            store.getOwnerUserId(), storeId, pageable);
        List<StoreMembershipEntity> memberships = result.getContent();
        List<Long> ids = memberships.stream().map(StoreMembershipEntity::getUserId).toList();
        Map<Long, UserEntity> users = userRepository.findAllById(ids).stream().collect(java.util.stream.Collectors.toMap(UserEntity::getId, u -> u));
        List<AdminOrganizationDtos.MemberSummary> items = memberships.stream().map(m -> member(m, users.get(m.getUserId()))).toList();
        return pageResponse(result, items, scope);
    }

    @Transactional
    public AdminOrganizationDtos.MemberSummary updateMember(AdminPrincipal principal, Long storeId, Long userId,
                                                             AdminOrganizationDtos.MemberPatchRequest request) {
        if (request == null) throw new IllegalArgumentException("member update request is required");
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.STORE_MANAGE, null, storeId);
        requireMutation(request.expectedVersion(), request.idempotencyKey(), request.reason(), request.confirmed());
        StoreEntity store = visibleStore(storeId, scope);
        StoreMembershipEntity membership = membershipRepository.findByOwnerUserIdAndStoreIdAndUserId(store.getOwnerUserId(), storeId, userId)
            .orElseThrow(() -> new AccessDeniedException("member resource not visible"));
        String hash = auditService.payloadHash(String.valueOf(request.nickname()) + "|" + request.role() + "|" + request.title() + "|" + request.status() + "|" + userId + "|" + storeId);
        if (auditService.findIdempotent(principal, request.idempotencyKey(), hash) != null) {
            return member(membership, userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not found")));
        }
        long version = membership.getAdminVersion() == null ? 0L : membership.getAdminVersion();
        if (!Objects.equals(request.expectedVersion(), version)) throw new AdminConflictException("member version conflict");
        boolean ownerMember = store.getOwnerUserId().equals(userId);
        StoreAccessPolicy.StoreRole role = StoreAccessPolicy.requireRole(request.role() == null ? membership.getRoleCode() : request.role());
        if (ownerMember && role != StoreAccessPolicy.StoreRole.OWNER) throw new IllegalArgumentException("store owner role cannot change");
        if (!ownerMember && role == StoreAccessPolicy.StoreRole.OWNER) throw new IllegalArgumentException("member cannot become owner");
        int status = request.status() == null ? (membership.getStatus() == null ? 1 : membership.getStatus()) : request.status();
        if (status != 0 && status != 1) throw new IllegalArgumentException("status is invalid");
        if (ownerMember && status != 1) throw new IllegalArgumentException("store owner must remain active");
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (request.nickname() != null) { if (request.nickname().isBlank() || request.nickname().trim().length() > 64) throw new IllegalArgumentException("nickname is invalid"); user.setNickname(request.nickname().trim()); }
        user.setStatus(status); user.setUpdatedAt(System.currentTimeMillis()); userRepository.save(user);
        membership.setRoleCode(role.name());
        if (request.title() != null) membership.setTitle(request.title().isBlank() ? role.defaultTitle() : request.title().trim());
        membership.setStatus(status); membership.setAdminVersion(version + 1L); membership.setUpdatedAt(System.currentTimeMillis());
        StoreMembershipEntity saved = membershipRepository.saveAndFlush(membership);
        if (status == 0 || Boolean.FALSE.equals(request.keepSessions())) invalidateSessions(userId);
        auditService.record(principal, "admin.store.member.update", "STORE_MEMBER", userId.toString(), store.getOwnerUserId(), storeId, "SUCCESS",
            request.reason(), "role=" + role.name() + ",status=" + status + ",version=" + saved.getAdminVersion(), request.idempotencyKey(), hash);
        return member(saved, user);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("keyword is too long");
        }
        return normalized;
    }

    private AdminOrganizationDtos.UserSummary toUserSummary(UserEntity user) {
        return new AdminOrganizationDtos.UserSummary(
            id(user.getId()),
            maskPhone(user.getPhone()),
            user.getNickname(),
            status(user.getStatus()),
            instant(user.getCreatedAt()),
            instant(user.getUpdatedAt()),
            user.getAdminVersion() == null ? 0L : user.getAdminVersion()
        );
    }

    private AdminOrganizationDtos.StoreSummary toStoreSummary(AdminStoreQueryRepository.StoreProjection store) {
        return new AdminOrganizationDtos.StoreSummary(
            id(store.getStoreId()),
            id(store.getOwnerUserId()),
            store.getName(),
            status(store.getStatus()),
            Math.toIntExact(store.getMemberCount() == null ? 0L : store.getMemberCount()),
            instant(store.getCreatedAt()),
            instant(store.getUpdatedAt()),
            store.getAdminVersion() == null ? 0L : store.getAdminVersion()
        );
    }

    private <T> AdminPageDtos.PageResponse<T> pageResponse(
        Page<?> page,
        List<T> items,
        AdminDataScope scope
    ) {
        return new AdminPageDtos.PageResponse<>(
            items,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            Instant.now(),
            AdminScopeDtos.Scope.from(scope),
            scope.allOwners() ? "COMPLETE" : "PARTIAL"
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String value = phone.trim();
        if (value.length() <= 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }

    private String status(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "ACTIVE" : "DISABLED";
    }

    private String id(Long value) {
        return value == null ? null : value.toString();
    }

    private UserEntity visibleUser(Long userId, AdminDataScope scope) {
        if (userRepository == null || membershipRepository == null || userId == null || userId <= 0) throw new IllegalArgumentException("userId is invalid");
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AccessDeniedException("user resource not visible"));
        if (scope.allOwners() || scope.ownerUserIds().contains(userId)) return user;
        StoreMembershipEntity membership = membershipRepository.findByUserId(userId).orElseThrow(() -> new AccessDeniedException("user resource not visible"));
        if (scope.ownerUserIds().contains(membership.getOwnerUserId()) && (scope.storeIds().isEmpty() || scope.storeIds().contains(membership.getStoreId()))) return user;
        throw new AccessDeniedException("user resource not visible");
    }

    private StoreEntity visibleStore(Long storeId, AdminDataScope scope) {
        if (storeRepository == null || storeId == null || storeId <= 0) throw new IllegalArgumentException("storeId is invalid");
        StoreEntity store = storeRepository.findById(storeId).orElseThrow(() -> new AccessDeniedException("store resource not visible"));
        if (scope.allOwners() || (scope.ownerUserIds().contains(store.getOwnerUserId()) && (scope.storeIds().isEmpty() || scope.storeIds().contains(storeId)))) return store;
        throw new AccessDeniedException("store resource not visible");
    }

    private int memberCount(Long storeId, Long ownerUserId) {
        return membershipRepository == null ? 0 : Math.toIntExact(membershipRepository.countByOwnerUserIdAndStoreId(ownerUserId, storeId));
    }

    private AdminOrganizationDtos.MemberSummary member(StoreMembershipEntity membership, UserEntity user) {
        if (user == null) throw new IllegalArgumentException("member user not found");
        return new AdminOrganizationDtos.MemberSummary(id(user.getId()), id(membership.getStoreId()), user.getNickname(), maskPhone(user.getPhone()),
            membership.getRoleCode(), membership.getTitle(), status(membership.getStatus()), instant(membership.getCreatedAt()),
            instant(Math.max(user.getUpdatedAt() == null ? 0L : user.getUpdatedAt(), membership.getUpdatedAt() == null ? 0L : membership.getUpdatedAt())),
            membership.getAdminVersion() == null ? 0L : membership.getAdminVersion());
    }

    private Long resolveOwner(Long userId, AdminDataScope scope, Long requestedOwner, Long requestedStore) {
        if (requestedOwner != null) return requestedOwner;
        if (requestedStore != null && storeRepository != null) return storeRepository.findById(requestedStore).map(StoreEntity::getOwnerUserId).orElse(null);
        if (scope.ownerUserIds().contains(userId)) return userId;
        return membershipRepository == null ? null : membershipRepository.findByUserId(userId).map(StoreMembershipEntity::getOwnerUserId).orElse(null);
    }

    private void requireMutation(Long expectedVersion, String idempotencyKey, String reason, Boolean confirmed) {
        if (expectedVersion == null || expectedVersion < 0 || idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.trim().length() > 128
            || reason == null || reason.isBlank() || reason.trim().length() > 512 || !Boolean.TRUE.equals(confirmed)) {
            throw new IllegalArgumentException("mutation requires confirmation, version, idempotencyKey and reason");
        }
        if (userRepository == null || auditService == null) throw new IllegalStateException("organization mutation is unavailable");
    }

    private void invalidateSessions(Long userId) {
        if (sessionRepository == null) return;
        List<SessionEntity> sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        for (SessionEntity session : sessions) session.setIsActive(false);
        if (!sessions.isEmpty()) sessionRepository.saveAll(sessions);
    }

    private Instant instant(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }
}
