package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentOwnerService {
    public static final String LEGACY_OWNER_PHONE = "SYSTEM-LEGACY-OWNER";

    private final UserRepository userRepository;

    public CurrentOwnerService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireCurrentOwnerUserId() {
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
}
