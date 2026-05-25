package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerEntity> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAll();
        }
        return customerRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(keyword, keyword);
    }

    public CustomerEntity get(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    public CustomerEntity create(CustomerEntity customer) {
        if (customerRepository.findByPhone(customer.getPhone()).isPresent()) {
            throw new IllegalArgumentException("手机号已存在");
        }
        long now = System.currentTimeMillis();
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customer.setSyncStatus(0);
        customer.setSyncVersion(1L);
        return customerRepository.save(customer);
    }

    public CustomerEntity update(Long id, CustomerEntity payload) {
        CustomerEntity target = get(id);
        target.setName(payload.getName());
        target.setPhone(payload.getPhone());
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

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}

