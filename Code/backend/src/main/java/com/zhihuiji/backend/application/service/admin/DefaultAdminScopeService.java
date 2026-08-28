package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Applies administrator scope on the server before a domain query is built.
 *
 * <p>The service intentionally does not resolve an administrator role. That
 * role must come from a separately trusted identity source. This class only
 * narrows a server-derived principal scope and validates store ownership.</p>
 */
@Service
public class DefaultAdminScopeService implements AdminScopeService {
    private final StoreRepository storeRepository;

    public DefaultAdminScopeService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Override
    public AdminDataScope resolve(AdminPrincipal principal, Long requestedOwnerUserId, Long requestedStoreId) {
        requirePrincipal(principal);
        if (requestedOwnerUserId == null && requestedStoreId == null) {
            return principal.scope();
        }

        if (requestedStoreId != null) {
            StoreEntity store = storeRepository.findById(requestedStoreId).orElseThrow(this::denied);
            Long actualOwnerUserId = store.getOwnerUserId();
            if (actualOwnerUserId == null
                || (requestedOwnerUserId != null && !Objects.equals(requestedOwnerUserId, actualOwnerUserId))
                || !principal.scope().allowsOwner(actualOwnerUserId)
                || !principal.scope().allowsStore(requestedStoreId)) {
                throw denied();
            }
            return narrowedScope(principal.scope(), actualOwnerUserId, requestedStoreId);
        }

        if (!principal.scope().allowsOwner(requestedOwnerUserId)) {
            throw denied();
        }
        return narrowedScope(principal.scope(), requestedOwnerUserId, null);
    }

    private AdminDataScope narrowedScope(AdminDataScope source, Long ownerUserId, Long storeId) {
        return new AdminDataScope(
            false,
            Set.of(ownerUserId),
            storeId == null ? Set.of() : Set.of(storeId),
            source.includeInactive(),
            source.contentMode()
        );
    }

    private void requirePrincipal(AdminPrincipal principal) {
        if (principal == null) {
            throw denied();
        }
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("administrator data scope denied");
    }
}
