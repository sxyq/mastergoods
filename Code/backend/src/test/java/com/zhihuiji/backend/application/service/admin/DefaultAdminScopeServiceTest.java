package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DefaultAdminScopeServiceTest {
    @Mock
    private StoreRepository storeRepository;

    private DefaultAdminScopeService scopeService;

    @BeforeEach
    void setUp() {
        scopeService = new DefaultAdminScopeService(storeRepository);
    }

    @Test
    void noClientFilterKeepsTheServerDerivedScope() {
        AdminDataScope source = AdminDataScope.owners(
            Set.of(101L, 102L),
            false,
            AdminDataScope.ContentMode.REDACTED
        );
        AdminPrincipal principal = observer(source);

        assertSame(source, scopeService.resolve(principal, null, null));
        verify(storeRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void requestedOwnerNarrowsAnAllowedScope() {
        AdminPrincipal principal = observer(AdminDataScope.owners(Set.of(101L, 102L), true, AdminDataScope.ContentMode.REDACTED));

        AdminDataScope narrowed = scopeService.resolve(principal, 102L, null);

        assertEquals(Set.of(102L), narrowed.ownerUserIds());
        assertEquals(Set.of(), narrowed.storeIds());
        assertEquals(false, narrowed.allOwners());
        assertEquals(true, narrowed.includeInactive());
        assertEquals(AdminDataScope.ContentMode.REDACTED, narrowed.contentMode());
    }

    @Test
    void requestedOwnerOutsideScopeIsDeniedWithoutLookingUpData() {
        AdminPrincipal principal = observer(AdminDataScope.owners(Set.of(101L), false, AdminDataScope.ContentMode.METADATA_ONLY));

        assertThrows(AccessDeniedException.class, () -> scopeService.resolve(principal, 999L, null));
        verify(storeRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void requestedStoreMustBelongToRequestedOwnerAndScope() {
        StoreEntity store = store(101L);
        when(storeRepository.findById(501L)).thenReturn(Optional.of(store));
        AdminPrincipal principal = observer(new AdminDataScope(
            false,
            Set.of(101L),
            Set.of(501L),
            false,
            AdminDataScope.ContentMode.REDACTED
        ));

        AdminDataScope narrowed = scopeService.resolve(principal, 101L, 501L);

        assertEquals(Set.of(101L), narrowed.ownerUserIds());
        assertEquals(Set.of(501L), narrowed.storeIds());
        verify(storeRepository).findById(501L);
    }

    @Test
    void requestedStoreCannotCrossOwnerEvenWhenStoreIsInTheRequestedStoreSet() {
        when(storeRepository.findById(501L)).thenReturn(Optional.of(store(202L)));
        AdminPrincipal principal = observer(new AdminDataScope(
            false,
            Set.of(101L, 202L),
            Set.of(501L),
            false,
            AdminDataScope.ContentMode.METADATA_ONLY
        ));

        assertThrows(AccessDeniedException.class, () -> scopeService.resolve(principal, 101L, 501L));
    }

    @Test
    void allOwnerPrincipalCanNarrowToExistingStore() {
        when(storeRepository.findById(501L)).thenReturn(Optional.of(store(202L)));
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );

        AdminDataScope narrowed = scopeService.resolve(principal, null, 501L);

        assertEquals(Set.of(202L), narrowed.ownerUserIds());
        assertEquals(Set.of(501L), narrowed.storeIds());
        assertEquals(AdminDataScope.ContentMode.AUTHORIZED, narrowed.contentMode());
    }

    @Test
    void missingPrincipalIsDenied() {
        assertThrows(AccessDeniedException.class, () -> scopeService.resolve(null, null, null));
    }

    private AdminPrincipal observer(AdminDataScope scope) {
        return AdminPrincipal.forRole(9L, AdminPrincipal.AdminRole.AUDIT_OBSERVER, scope);
    }

    private StoreEntity store(Long ownerUserId) {
        StoreEntity store = new StoreEntity();
        store.setOwnerUserId(ownerUserId);
        return store;
    }
}
