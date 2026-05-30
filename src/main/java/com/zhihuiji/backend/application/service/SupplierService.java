package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierEntity> list(String keyword, Integer status) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword && status != null) {
            return supplierRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseAndStatus(keyword.trim(), keyword.trim(), status);
        }
        if (hasKeyword) {
            return supplierRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(keyword.trim(), keyword.trim());
        }
        if (status != null) {
            return supplierRepository.findByStatus(status);
        }
        return supplierRepository.findAll();
    }

    public SupplierEntity get(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
    }

    public SupplierEntity create(SupplierEntity supplier) {
        validatePayload(supplier);
        long now = System.currentTimeMillis();
        supplier.setName(supplier.getName().trim());
        supplier.setPhone(normalizePhone(supplier.getPhone()));
        if (supplierRepository.existsByPhone(supplier.getPhone())) {
            throw new IllegalArgumentException("供应商手机号已存在");
        }
        supplier.setAddress(normalizeNullableText(supplier.getAddress()));
        supplier.setNotes(normalizeNullableText(supplier.getNotes()));
        supplier.setBalance(normalizeAmount(supplier.getBalance()));
        supplier.setStatus(supplier.getStatus() == null ? 1 : supplier.getStatus());
        supplier.setCreatedAt(now);
        supplier.setUpdatedAt(now);
        supplier.setSyncStatus(0);
        supplier.setSyncVersion(1L);
        return supplierRepository.save(supplier);
    }

    public SupplierEntity update(Long id, SupplierEntity payload) {
        SupplierEntity target = get(id);
        validatePayload(payload);
        target.setName(payload.getName().trim());
        target.setPhone(normalizePhone(payload.getPhone()));
        if (supplierRepository.existsByPhoneAndIdNot(target.getPhone(), id)) {
            throw new IllegalArgumentException("供应商手机号已存在");
        }
        target.setAddress(normalizeNullableText(payload.getAddress()));
        target.setNotes(normalizeNullableText(payload.getNotes()));
        target.setBalance(normalizeAmount(payload.getBalance()));
        target.setStatus(payload.getStatus() == null ? target.getStatus() : payload.getStatus());
        target.setUpdatedAt(System.currentTimeMillis());
        target.setSyncStatus(0);
        target.setSyncVersion(target.getSyncVersion() + 1);
        return supplierRepository.save(target);
    }

    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException("供应商不存在");
        }
        supplierRepository.deleteById(id);
    }

    private void validatePayload(SupplierEntity supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("供应商参数不能为空");
        }
        if (supplier.getName() == null || supplier.getName().trim().isBlank()) {
            throw new IllegalArgumentException("供应商名称不能为空");
        }
        if (supplier.getPhone() == null || supplier.getPhone().trim().isBlank()) {
            throw new IllegalArgumentException("供应商手机号不能为空");
        }
        if (supplier.getStatus() != null && supplier.getStatus() != 0 && supplier.getStatus() != 1) {
            throw new IllegalArgumentException("供应商状态不合法");
        }
        if (supplier.getBalance() != null && supplier.getBalance() < 0.0) {
            throw new IllegalArgumentException("供应商余额不能小于0");
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private double normalizeAmount(Double amount) {
        if (amount == null) {
            return 0.0;
        }
        return Math.max(0.0, amount);
    }
}
