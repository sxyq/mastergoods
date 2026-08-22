package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.PayOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayOrderService {
    private final PayOrderRepository payOrderRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentOwnerService currentOwnerService;
    private final IdGenerator idGenerator;

    public PayOrderService(
        PayOrderRepository payOrderRepository,
        SupplierRepository supplierRepository,
        CurrentOwnerService currentOwnerService,
        IdGenerator idGenerator
    ) {
        this.payOrderRepository = payOrderRepository;
        this.supplierRepository = supplierRepository;
        this.currentOwnerService = currentOwnerService;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public List<PayOrderEntity> list(
        String keyword,
        Integer status,
        Long createdAfter,
        Long createdBefore
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return payOrderRepository.search(
            currentOwnerService.requireCurrentOwnerUserId(),
            normalizedKeyword,
            status,
            createdAfter,
            createdBefore
        );
    }

    @Transactional(readOnly = true)
    public PayOrderEntity getById(Long id) {
        return payOrderRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("付款单不存在"));
    }

    @Transactional
    public PayOrderEntity create(CreateCommand command) {
        return createForOwner(currentOwnerService.requireCurrentOwnerUserId(), command);
    }

    @Transactional
    public PayOrderEntity createForOwner(Long ownerUserId, CreateCommand command) {
        return createForOwner(ownerUserId, command, null);
    }

    @Transactional
    public PayOrderEntity createForOwner(Long ownerUserId, CreateCommand command, String idempotencyKey) {
        validateCreateCommand(command);
        long now = System.currentTimeMillis();
        PayOrderEntity entity = new PayOrderEntity();
        entity.setId(idGenerator.nextId());
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo(generateOrderNo());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setSupplierId(command.supplierId());
        entity.setSupplierName(resolveSupplierName(ownerUserId, command.supplierId(), command.supplierName()));
        entity.setAmount(command.amount());
        entity.setMethod(command.method());
        entity.setReferenceNo(normalizeNullableText(command.referenceNo()));
        entity.setNotes(normalizeNullableText(command.notes()));
        entity.setStatus(command.status() == null ? PayOrderStatus.DRAFT.code() : command.status());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        PayOrderEntity saved = payOrderRepository.save(entity);

        if (saved.getSupplierId() != null && saved.getStatus() == PayOrderStatus.PAID.code()) {
            SupplierEntity supplier = supplierRepository.findByIdAndOwnerUserIdForUpdate(saved.getSupplierId(), ownerUserId)
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
        if (!PayOrderStatus.isValid(status)) {
            throw new IllegalArgumentException("状态不合法");
        }
        PayOrderEntity target = getById(id);
        if (target.getStatus().equals(status)) {
            return target;
        }
        long now = System.currentTimeMillis();
        if (target.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepository.findByIdAndOwnerUserIdForUpdate(target.getSupplierId(), currentOwnerService.requireCurrentOwnerUserId())
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
            if (target.getStatus() == PayOrderStatus.PAID.code() && status != PayOrderStatus.PAID.code()) {
                supplier.setBalance(Math.max(0.0, safeDouble(supplier.getBalance()) + target.getAmount()));
            } else if (target.getStatus() != PayOrderStatus.PAID.code() && status == PayOrderStatus.PAID.code()) {
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
        if (command.amount() == null || !Double.isFinite(command.amount()) || command.amount() <= 0.0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }
        if (command.method() == null || command.method() <= 0) {
            throw new IllegalArgumentException("付款方式不合法");
        }
        if ((command.supplierName() == null || command.supplierName().trim().isBlank()) && command.supplierId() == null) {
            throw new IllegalArgumentException("供应商不能为空");
        }
        Integer status = command.status();
        if (status != null && !PayOrderStatus.isValid(status)) {
            throw new IllegalArgumentException("状态不合法");
        }
    }

    private String resolveSupplierName(Long ownerUserId, Long supplierId, String fallbackName) {
        if (supplierId != null) {
            SupplierEntity supplier = supplierRepository.findByIdAndOwnerUserId(supplierId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
            return supplier.getName();
        }
        String normalized = fallbackName == null ? "" : fallbackName.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("供应商名称不能为空");
        }
        return normalized;
    }

    private String generateOrderNo() {
        return "POUT" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableText(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
