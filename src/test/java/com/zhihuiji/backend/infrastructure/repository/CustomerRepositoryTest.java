package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.domain.entity.CustomerEntity;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CustomerRepositoryTest {
    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void searchAppliesKeywordStatusAndGroupFilters() {
        customerRepository.save(customer(1L, "禁用匹配客户", "13800000001", 0, 3L));
        customerRepository.save(customer(1L, "启用匹配客户", "匹配-phone-enabled", 1, 3L));
        customerRepository.save(customer(1L, "启用其他客户", "13800000003", 1, 4L));
        customerRepository.save(customer(2L, "启用匹配客户", "匹配-phone-other", 1, 3L));

        Set<String> names = customerRepository.search(1L, "匹配", 1, 3L).stream()
            .map(CustomerEntity::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("启用匹配客户"), names);
    }

    private static CustomerEntity customer(Long ownerUserId, String name, String phone, Integer status, Long groupId) {
        CustomerEntity entity = new CustomerEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setName(name);
        entity.setPhone(phone);
        entity.setLevel(1);
        entity.setGroupId(groupId);
        entity.setBalance(0.0);
        entity.setStatus(status);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
