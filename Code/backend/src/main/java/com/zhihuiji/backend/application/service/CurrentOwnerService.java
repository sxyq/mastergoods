package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.application.service.store.StoreAccessPolicy;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CurrentOwnerService {
    public static final String LEGACY_OWNER_PHONE = "SYSTEM-LEGACY-OWNER";

    private final UserRepository userRepository;
    private final StoreMembershipRepository storeMembershipRepository;

    public CurrentOwnerService(
        UserRepository userRepository,
        StoreMembershipRepository storeMembershipRepository
    ) {
        this.userRepository = userRepository;
        this.storeMembershipRepository = storeMembershipRepository;
    }

    public Long requireCurrentOwnerUserId() {
        return resolveCurrentAccess().ownerUserId();
    }

    public Optional<Long> findCurrentStoreId() {
        Long currentUserId = requireCurrentUserId();
        return storeMembershipRepository.findByUserId(currentUserId)
            .filter(membership -> membership.getStatus() == null || membership.getStatus() == 1)
            .map(value -> value.getStoreId());
    }

    public Long requireCurrentStoreId() {
        return findCurrentStoreId()
            .orElseThrow(() -> new AccessDeniedException("当前账号没有有效门店上下文"));
    }

    public Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("authenticated owner is required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long value) {
            return value;
        }
        if (principal instanceof Integer value) {
            return value.longValue();
        }
        if (principal instanceof String value && !value.isBlank()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // fall through to exception below
            }
        }
        throw new IllegalStateException("unable to resolve current owner");
    }

    public Long requireLegacyOwnerUserId() {
        return userRepository.findByPhone(LEGACY_OWNER_PHONE)
            .map(user -> user.getId())
            .orElseThrow(() -> new IllegalStateException("legacy owner is missing"));
    }

    public void requirePermissions(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return;
        }
        CurrentAccess access = resolveCurrentAccess();
        var permissions = access.role().permissions();
        StringBuilder denied = new StringBuilder();
        for (String permission : permissionCodes) {
            if (!StoreAccessPolicy.containsPermission(permissions, access.status(), permission)) {
                if (denied.length() > 0) {
                    denied.append(", ");
                }
                denied.append(permission);
            }
        }
        if (denied.length() > 0) {
            throw new AccessDeniedException("当前账号缺少权限: " + denied);
        }
    }

    public void requireAnyPermission(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return;
        }
        CurrentAccess access = resolveCurrentAccess();
        var permissions = access.role().permissions();
        for (String permission : permissionCodes) {
            if (StoreAccessPolicy.containsPermission(permissions, access.status(), permission)) {
                return;
            }
        }
        throw new AccessDeniedException("当前账号缺少任一权限: " + String.join(", ", permissionCodes));
    }

    private CurrentAccess resolveCurrentAccess() {
        Long currentUserId = requireCurrentUserId();
        var membership = storeMembershipRepository.findByUserId(currentUserId);
        if (membership.isPresent()) {
            var value = membership.get();
            return new CurrentAccess(
                value.getOwnerUserId(),
                StoreAccessPolicy.requireRole(value.getRoleCode()),
                value.getStatus()
            );
        }
        return new CurrentAccess(currentUserId, StoreAccessPolicy.StoreRole.OWNER, 1);
    }

    private record CurrentAccess(Long ownerUserId, StoreAccessPolicy.StoreRole role, Integer status) {}
}
