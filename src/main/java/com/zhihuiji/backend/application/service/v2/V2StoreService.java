package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.store.V2StoreDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.store.StoreAccessPolicy;
import com.zhihuiji.backend.domain.entity.SessionEntity;
import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.domain.entity.StoreMembershipEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class V2StoreService {
    private final StoreRepository storeRepository;
    private final StoreMembershipRepository storeMembershipRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentOwnerService currentOwnerService;

    public V2StoreService(
        StoreRepository storeRepository,
        StoreMembershipRepository storeMembershipRepository,
        UserRepository userRepository,
        SessionRepository sessionRepository,
        PasswordEncoder passwordEncoder,
        CurrentOwnerService currentOwnerService
    ) {
        this.storeRepository = storeRepository;
        this.storeMembershipRepository = storeMembershipRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional
    public V2StoreDtos.CurrentStoreResponse getCurrentStore() {
        Long currentUserId = currentOwnerService.requireCurrentUserId();
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        StoreContext context = ensureStoreContext(ownerUserId);
        StoreMembershipEntity membership = resolveCurrentMembership(context, currentUserId);
        UserEntity currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        List<StoreMembershipEntity> memberships = storeMembershipRepository.findByOwnerUserIdOrderByCreatedAtAsc(ownerUserId);
        int enabledCount = 0;
        for (StoreMembershipEntity item : memberships) {
            if (item.getStatus() != null && item.getStatus() == 1) {
                enabledCount++;
            }
        }
        int disabledCount = memberships.size() - enabledCount;
        StoreAccessPolicy.StoreRole role = StoreAccessPolicy.requireRole(membership.getRoleCode());
        return new V2StoreDtos.CurrentStoreResponse(
            context.store().getId(),
            context.store().getStoreName(),
            ownerUserId,
            currentUser.getId(),
            currentUser.getNickname(),
            currentUser.getPhone(),
            role.name(),
            membership.getTitle(),
            normalizeStatus(membership.getStatus()),
            permissionNames(role, membership.getStatus()),
            memberships.size(),
            enabledCount,
            disabledCount
        );
    }

    @Transactional
    public List<V2StoreDtos.MemberResponse> listMembers() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        StoreContext context = ensureStoreContext(ownerUserId);
        List<StoreMembershipEntity> memberships = storeMembershipRepository.findByOwnerUserIdOrderByCreatedAtAsc(ownerUserId);
        List<Long> memberUserIds = new java.util.ArrayList<>(memberships.size());
        for (StoreMembershipEntity membership : memberships) {
            memberUserIds.add(membership.getUserId());
        }
        Map<Long, UserEntity> usersById = new java.util.LinkedHashMap<>(memberUserIds.size());
        for (UserEntity user : userRepository.findAllById(memberUserIds)) {
            usersById.put(user.getId(), user);
        }
        Map<Long, Long> activeSessionsByUserId = activeSessionCountsByUserId(memberUserIds);
        List<V2StoreDtos.MemberResponse> responses = new java.util.ArrayList<>(memberships.size());
        for (StoreMembershipEntity membership : memberships) {
            responses.add(toMemberResponse(
                context.store(),
                membership,
                usersById.get(membership.getUserId()),
                activeSessionsByUserId.getOrDefault(membership.getUserId(), 0L)
            ));
        }
        return responses;
    }

    @Transactional
    public V2StoreDtos.MemberResponse createMember(V2StoreDtos.MemberCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        StoreContext context = ensureStoreContext(ownerUserId);
        ensureCurrentUserCanManage(context);
        String phone = normalizeRequired(request.phone(), "手机号不能为空");
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("手机号已注册");
        }
        StoreAccessPolicy.StoreRole role = normalizeRole(request.role(), false);
        int status = normalizeStatus(request.status());
        long now = System.currentTimeMillis();

        UserEntity user = new UserEntity();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(normalizeRequired(request.password(), "初始密码不能为空")));
        user.setNickname(normalizeRequired(request.nickname(), "店员昵称不能为空"));
        user.setStatus(status);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        UserEntity savedUser = userRepository.save(user);

        StoreMembershipEntity membership = new StoreMembershipEntity();
        membership.setOwnerUserId(ownerUserId);
        membership.setStoreId(context.store().getId());
        membership.setUserId(savedUser.getId());
        membership.setRoleCode(role.name());
        membership.setTitle(normalizeTitle(request.title(), role));
        membership.setStatus(status);
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        StoreMembershipEntity savedMembership = storeMembershipRepository.save(membership);
        return toMemberResponse(context.store(), savedMembership, savedUser);
    }

    @Transactional
    public V2StoreDtos.MemberResponse updateMember(Long userId, V2StoreDtos.MemberUpdateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        StoreContext context = ensureStoreContext(ownerUserId);
        ensureCurrentUserCanManage(context);
        StoreMembershipEntity membership = storeMembershipRepository.findByOwnerUserIdAndUserId(ownerUserId, userId)
            .orElseThrow(() -> new IllegalArgumentException("店员不存在"));
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("账号不存在"));

        boolean ownerMember = ownerUserId.equals(userId);
        StoreAccessPolicy.StoreRole role = normalizeRole(
            StringUtils.hasText(request.role()) ? request.role() : membership.getRoleCode(),
            ownerMember
        );
        int status = request.status() == null ? normalizeStatus(membership.getStatus()) : normalizeStatus(request.status());
        if (ownerMember && status != 1) {
            throw new IllegalArgumentException("店长账号必须保持启用");
        }
        if (StringUtils.hasText(request.nickname())) {
            user.setNickname(request.nickname().trim());
        }
        if (request.status() != null) {
            user.setStatus(status);
        }
        boolean shouldInvalidateSessions = !Boolean.TRUE.equals(request.keepSessions()) && StringUtils.hasText(request.password());
        if (status == 0) {
            shouldInvalidateSessions = true;
        }
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }
        user.setUpdatedAt(System.currentTimeMillis());
        UserEntity savedUser = userRepository.save(user);

        membership.setRoleCode(role.name());
        membership.setTitle(normalizeTitle(request.title(), role));
        membership.setStatus(status);
        membership.setUpdatedAt(System.currentTimeMillis());
        StoreMembershipEntity savedMembership = storeMembershipRepository.save(membership);
        if (shouldInvalidateSessions) {
            invalidateActiveSessions(userId);
        }
        return toMemberResponse(context.store(), savedMembership, savedUser);
    }

    @Transactional
    public StoreContext ensureStoreContext(Long ownerUserId) {
        UserEntity owner = userRepository.findById(ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("店长账号不存在"));
        StoreEntity store = storeRepository.findByOwnerUserId(ownerUserId)
            .orElseGet(() -> createDefaultStore(owner));
        StoreMembershipEntity ownerMembership = storeMembershipRepository.findByOwnerUserIdAndUserId(ownerUserId, ownerUserId)
            .orElseGet(() -> createOwnerMembership(store, owner));
        return new StoreContext(store, ownerMembership);
    }

    private void ensureCurrentUserCanManage(StoreContext context) {
        Long currentUserId = currentOwnerService.requireCurrentUserId();
        StoreMembershipEntity membership = resolveCurrentMembership(context, currentUserId);
        if (!permissionNames(StoreAccessPolicy.requireRole(membership.getRoleCode()), membership.getStatus())
            .contains(StoreAccessPolicy.StorePermission.USERS_MANAGE.code())) {
            throw new IllegalArgumentException("当前角色不能管理员工权限");
        }
    }

    private StoreMembershipEntity resolveCurrentMembership(StoreContext context, Long currentUserId) {
        return storeMembershipRepository.findByOwnerUserIdAndUserId(context.store().getOwnerUserId(), currentUserId)
            .orElseGet(() -> {
                if (!context.store().getOwnerUserId().equals(currentUserId)) {
                    throw new IllegalArgumentException("当前用户未绑定门店成员关系");
                }
                return context.ownerMembership();
            });
    }

    private StoreEntity createDefaultStore(UserEntity owner) {
        long now = System.currentTimeMillis();
        StoreEntity store = new StoreEntity();
        store.setOwnerUserId(owner.getId());
        store.setStoreName(owner.getNickname() + " 的智慧记店铺");
        store.setStatus(1);
        store.setCreatedAt(now);
        store.setUpdatedAt(now);
        return storeRepository.save(store);
    }

    private StoreMembershipEntity createOwnerMembership(StoreEntity store, UserEntity owner) {
        long now = System.currentTimeMillis();
        StoreMembershipEntity membership = new StoreMembershipEntity();
        membership.setOwnerUserId(owner.getId());
        membership.setStoreId(store.getId());
        membership.setUserId(owner.getId());
        membership.setRoleCode(StoreAccessPolicy.StoreRole.OWNER.name());
        membership.setTitle("店长（总）");
        membership.setStatus(1);
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        return storeMembershipRepository.save(membership);
    }

    private V2StoreDtos.MemberResponse toMemberResponse(StoreEntity store, StoreMembershipEntity membership, UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("店员账号不存在");
        }
        return toMemberResponse(store, membership, user, sessionRepository.countByUserIdAndIsActiveTrue(user.getId()));
    }

    private V2StoreDtos.MemberResponse toMemberResponse(
        StoreEntity store,
        StoreMembershipEntity membership,
        UserEntity user,
        Long activeSessions
    ) {
        if (user == null) {
            throw new IllegalArgumentException("店员账号不存在");
        }
        StoreAccessPolicy.StoreRole role = StoreAccessPolicy.requireRole(membership.getRoleCode());
        int status = normalizeStatus(membership.getStatus());
        return new V2StoreDtos.MemberResponse(
            user.getId(),
            user.getPhone(),
            user.getNickname(),
            role.name(),
            membership.getTitle(),
            status,
            permissionNames(role, status),
            user.getCreatedAt(),
            Math.max(user.getUpdatedAt(), membership.getUpdatedAt()),
            activeSessions,
            store.getId(),
            store.getStoreName()
        );
    }

    private void invalidateActiveSessions(Long userId) {
        List<SessionEntity> sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        if (sessions.isEmpty()) {
            return;
        }
        for (SessionEntity session : sessions) {
            session.setIsActive(false);
        }
        sessionRepository.saveAll(sessions);
    }

    private Map<Long, Long> activeSessionCountsByUserId(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> countsByUserId = new java.util.LinkedHashMap<>(userIds.size());
        for (SessionRepository.ActiveSessionCount row : sessionRepository.countActiveSessionsByUserIds(userIds)) {
            countsByUserId.put(row.getUserId(), row.getActiveCount());
        }
        return countsByUserId;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeTitle(String title, StoreAccessPolicy.StoreRole role) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        return role.defaultTitle();
    }

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("店员状态不合法");
        }
        return status;
    }

    private StoreAccessPolicy.StoreRole normalizeRole(String role, boolean ownerMember) {
        StoreAccessPolicy.StoreRole resolved = StoreAccessPolicy.requireRole(role);
        if (ownerMember && resolved != StoreAccessPolicy.StoreRole.OWNER) {
            throw new IllegalArgumentException("店长账号角色不可变更");
        }
        if (!ownerMember && resolved == StoreAccessPolicy.StoreRole.OWNER) {
            throw new IllegalArgumentException("员工账号不能直接设置为店长");
        }
        return resolved;
    }

    private List<String> permissionNames(StoreAccessPolicy.StoreRole role, Integer status) {
        return StoreAccessPolicy.permissionCodes(role.name(), status);
    }

    public record StoreContext(StoreEntity store, StoreMembershipEntity ownerMembership) {}
}
