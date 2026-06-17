package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.application.service.store.StoreAccessPolicy;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
        Long currentUserId = requireCurrentUserId();
        return storeMembershipRepository.findByUserId(currentUserId)
            .map(value -> value.getOwnerUserId())
            .orElse(currentUserId);
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
        Long currentUserId = requireCurrentUserId();
        Long ownerUserId = requireCurrentOwnerUserId();
        StoreMemberAccess access = resolveMemberAccess(currentUserId, ownerUserId);
        List<String> denied = Arrays.stream(permissionCodes)
            .filter(permission -> !StoreAccessPolicy.hasPermission(access.roleCode(), access.status(), permission))
            .toList();
        if (!denied.isEmpty()) {
            throw new AccessDeniedException("当前账号缺少权限: " + String.join(", ", denied));
        }
    }

    public void requireAnyPermission(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return;
        }
        Long currentUserId = requireCurrentUserId();
        Long ownerUserId = requireCurrentOwnerUserId();
        StoreMemberAccess access = resolveMemberAccess(currentUserId, ownerUserId);
        boolean allowed = Arrays.stream(permissionCodes)
            .anyMatch(permission -> StoreAccessPolicy.hasPermission(access.roleCode(), access.status(), permission));
        if (!allowed) {
            throw new AccessDeniedException("当前账号缺少任一权限: " + String.join(", ", permissionCodes));
        }
    }

    private StoreMemberAccess resolveMemberAccess(Long currentUserId, Long ownerUserId) {
        return storeMembershipRepository.findByUserId(currentUserId)
            .map(membership -> new StoreMemberAccess(membership.getRoleCode(), membership.getStatus()))
            .orElseGet(() -> {
                if (currentUserId.equals(ownerUserId)) {
                    return new StoreMemberAccess(StoreAccessPolicy.StoreRole.OWNER.name(), 1);
                }
                throw new AccessDeniedException("当前用户未绑定门店成员关系");
            });
    }

    private record StoreMemberAccess(String roleCode, Integer status) {}
}
