package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.domain.entity.SupplierEntity;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SupplierRepositoryTest {
    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    void keywordAndStatusSearchAppliesStatusToNameAndPhoneBranches() {
        supplierRepository.save(supplier(1L, "禁用匹配供应商", "13900000001", 0));
        supplierRepository.save(supplier(1L, "禁用手机供应商", "匹配-phone-disabled", 0));
        supplierRepository.save(supplier(1L, "启用匹配供应商", "13900000003", 1));
        supplierRepository.save(supplier(1L, "启用手机供应商", "匹配-phone-enabled", 1));
        supplierRepository.save(supplier(2L, "启用匹配供应商", "匹配-phone-other", 1));

        Set<String> names = supplierRepository.search(1L, "匹配", 1, null).stream()
            .map(SupplierEntity::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("启用匹配供应商", "启用手机供应商"), names);
    }

    private static SupplierEntity supplier(Long ownerUserId, String name, String phone, Integer status) {
        SupplierEntity entity = new SupplierEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setName(name);
        entity.setPhone(phone);
        entity.setBalance(0.0);
        entity.setStatus(status);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
