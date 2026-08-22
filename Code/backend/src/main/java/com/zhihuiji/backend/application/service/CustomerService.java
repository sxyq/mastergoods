package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CurrentOwnerService currentOwnerService;

    public CustomerService(CustomerRepository customerRepository, CurrentOwnerService currentOwnerService) {
        this.customerRepository = customerRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional(readOnly = true)
    public List<CustomerEntity> list(String keyword) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedKeyword = normalizeKeyword(keyword);
        return customerRepository.search(ownerUserId, normalizedKeyword, null, null);
    }

    @Transactional(readOnly = true)
    public List<CustomerEntity> list(String keyword, Pageable pageable) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return customerRepository.search(ownerUserId, normalizeKeyword(keyword), null, null, pageable);
    }

    @Transactional(readOnly = true)
    public CustomerEntity get(Long id) {
        return customerRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    @Transactional
    public CustomerEntity create(CustomerEntity customer) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedPhone = customer.getPhone() == null ? "" : customer.getPhone().trim();
        customer.setPhone(normalizedPhone);
        if (customerRepository.findByOwnerUserIdAndPhone(ownerUserId, normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("手机号已存在");
        }
        long now = System.currentTimeMillis();
        customer.setOwnerUserId(ownerUserId);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customer.setSyncStatus(0);
        customer.setSyncVersion(1L);
        return customerRepository.save(customer);
    }

    @Transactional
    public CustomerEntity update(Long id, CustomerEntity payload) {
        CustomerEntity target = get(id);
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedPhone = payload.getPhone() == null ? "" : payload.getPhone().trim();
        if (!normalizedPhone.equals(target.getPhone())) {
            customerRepository.findByOwnerUserIdAndPhone(ownerUserId, normalizedPhone)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("手机号已存在");
                });
        }
        target.setName(payload.getName());
        target.setPhone(normalizedPhone);
        target.setLevel(payload.getLevel());
        target.setAddress(payload.getAddress());
        target.setNotes(payload.getNotes());
        target.setBalance(payload.getBalance());
        target.setStatus(payload.getStatus());
        target.setUpdatedAt(System.currentTimeMillis());
        target.setSyncStatus(0);
        target.setSyncVersion(target.getSyncVersion() + 1);
        return customerRepository.save(target);
    }

    @Transactional
    public void delete(Long id) {
        customerRepository.delete(get(id));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
