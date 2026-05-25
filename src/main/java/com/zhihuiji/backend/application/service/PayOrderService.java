package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayOrderService {
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_CANCELLED = 2;

    private final PayOrderRepository payOrderRepository;
    private final SupplierRepository supplierRepository;

    public PayOrderService(
        PayOrderRepository payOrderRepository,
        SupplierRepository supplierRepository
    ) {
        this.payOrderRepository = payOrderRepository;
        this.supplierRepository = supplierRepository;
    }

    public List<PayOrderEntity> list(
        String keyword,
        Integer status,
        Long createdAfter,
        Long createdBefore
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return payOrderRepository.findAll().stream()
            .filter(item -> status == null || status.equals(item.getStatus()))
            .filter(item -> createdAfter == null || item.getCreatedAt() >= createdAfter)
            .filter(item -> createdBefore == null || item.getCreatedAt() <= createdBefore)
            .filter(item -> normalizedKeyword.isBlank() || matchesKeyword(item, normalizedKeyword))
            .sorted(Comparator.comparingLong(PayOrderEntity::getCreatedAt).reversed())
            .toList();
    }

    public PayOrderEntity getById(Long id) {
        return payOrderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("付款单不存在"));
    }

    @Transactional
    public PayOrderEntity create(CreateCommand command) {
        validateCreateCommand(command);
        long now = System.currentTimeMillis();
        PayOrderEntity entity = new PayOrderEntity();
        entity.setId(nextId());
        entity.setOrderNo(generateOrderNo(now));
        entity.setSupplierId(command.supplierId());
        entity.setSupplierName(resolveSupplierName(command.supplierId(), command.supplierName()));
        entity.setAmount(command.amount());
        entity.setMethod(command.method());
        entity.setReferenceNo(normalizeNullableText(command.referenceNo()));
        entity.setNotes(normalizeNullableText(command.notes()));
        entity.setStatus(command.status() == null ? STATUS_DRAFT : command.status());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        PayOrderEntity saved = payOrderRepository.save(entity);

        if (saved.getSupplierId() != null && saved.getStatus() == STATUS_PAID) {
            SupplierEntity supplier = supplierRepository.findById(saved.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
            supplier.setBalance(Math.max(0.0, safeDouble(supplier.getBalance()) - saved.getAmount()));
            supplier.setUpdatedAt(now);
            supplier.setSyncStatus(0);
            supplier.setSyncVersion(safeLong(supplier.getSyncVersion()) + 1);
            supplierRepository.save(supplier);
        }
        return saved;
    }

    @Transactional
    public PayOrderEntity updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        if (status != STATUS_DRAFT && status != STATUS_PAID && status != STATUS_CANCELLED) {
            throw new IllegalArgumentException("状态不合法");
        }
        PayOrderEntity target = getById(id);
        if (target.getStatus().equals(status)) {
            return target;
        }
        long now = System.currentTimeMillis();
        if (target.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepository.findById(target.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
            if (target.getStatus() == STATUS_PAID && status != STATUS_PAID) {
                supplier.setBalance(Math.max(0.0, safeDouble(supplier.getBalance()) + target.getAmount()));
            } else if (target.getStatus() != STATUS_PAID && status == STATUS_PAID) {
                supplier.setBalance(Math.max(0.0, safeDouble(supplier.getBalance()) - target.getAmount()));
            }
            supplier.setUpdatedAt(now);
            supplier.setSyncStatus(0);
            supplier.setSyncVersion(safeLong(supplier.getSyncVersion()) + 1);
            supplierRepository.save(supplier);
        }
        target.setStatus(status);
        target.setUpdatedAt(now);
        target.setSyncStatus(0);
        target.setSyncVersion(target.getSyncVersion() + 1);
        return payOrderRepository.save(target);
    }

    private void validateCreateCommand(CreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("付款单参数不能为空");
        }
        if (command.amount() == null || command.amount() <= 0.0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }
        if (command.method() == null || command.method() <= 0) {
            throw new IllegalArgumentException("付款方式不合法");
        }
        if ((command.supplierName() == null || command.supplierName().trim().isBlank()) && command.supplierId() == null) {
            throw new IllegalArgumentException("供应商不能为空");
        }
        Integer status = command.status();
        if (status != null && status != STATUS_DRAFT && status != STATUS_PAID && status != STATUS_CANCELLED) {
            throw new IllegalArgumentException("状态不合法");
        }
    }

    private String resolveSupplierName(Long supplierId, String fallbackName) {
        if (supplierId != null) {
            SupplierEntity supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
            return supplier.getName();
        }
        String normalized = fallbackName == null ? "" : fallbackName.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("供应商名称不能为空");
        }
        return normalized;
    }

    private boolean matchesKeyword(PayOrderEntity entity, String normalizedKeyword) {
        return safeString(entity.getOrderNo()).contains(normalizedKeyword)
            || safeString(entity.getSupplierName()).contains(normalizedKeyword)
            || safeString(entity.getReferenceNo()).contains(normalizedKeyword);
    }

    private String generateOrderNo(long timestamp) {
        String millis = String.valueOf(timestamp);
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        return "POUT" + millis + suffix;
    }

    private long nextId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private String normalizeNullableText(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeString(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    public record CreateCommand(
        Long supplierId,
        String supplierName,
        Double amount,
        Integer method,
        String referenceNo,
        String notes,
        Integer status
    ) {}
}
