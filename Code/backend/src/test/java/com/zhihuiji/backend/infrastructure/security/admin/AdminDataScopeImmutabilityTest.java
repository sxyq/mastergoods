package com.zhihuiji.backend.infrastructure.security.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminDataScopeImmutabilityTest {
    @Test
    void constructorCopiesOwnerAndStoreSets() {
        Set<Long> owners = new HashSet<>(Set.of(101L));
        Set<Long> stores = new HashSet<>(Set.of(501L));

        AdminDataScope scope = new AdminDataScope(
            false,
            owners,
            stores,
            true,
            AdminDataScope.ContentMode.REDACTED
        );

        owners.add(202L);
        stores.clear();

        assertEquals(Set.of(101L), scope.ownerUserIds());
        assertEquals(Set.of(501L), scope.storeIds());
        assertTrue(scope.includeInactive());
        assertEquals(AdminDataScope.ContentMode.REDACTED, scope.contentMode());
    }

    @Test
    void exposedScopeSetsCannotBeMutated() {
        AdminDataScope scope = new AdminDataScope(Set.of(101L), Set.of(501L));

        assertThrows(UnsupportedOperationException.class, () -> scope.ownerUserIds().add(202L));
        assertThrows(UnsupportedOperationException.class, () -> scope.storeIds().clear());
    }

    @Test
    void nullInputsProduceAnEmptyMetadataOnlyScope() {
        AdminDataScope scope = new AdminDataScope(false, null, null, false, null);

        assertEquals(Set.of(), scope.ownerUserIds());
        assertEquals(Set.of(), scope.storeIds());
        assertEquals(AdminDataScope.ContentMode.METADATA_ONLY, scope.contentMode());
        assertFalse(scope.allowsOwner(null));
        assertFalse(scope.allowsStore(null));
        assertFalse(scope.allowsOwner(101L));
        assertFalse(scope.allowsStore(501L));
    }

    @Test
    void allOwnersScopeAllowsResourceIdsWithoutClientSuppliedSets() {
        AdminDataScope scope = AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED);

        assertTrue(scope.ownerUserIds().isEmpty());
        assertTrue(scope.storeIds().isEmpty());
        assertTrue(scope.allowsOwner(101L));
        assertTrue(scope.allowsStore(501L));
    }
}
