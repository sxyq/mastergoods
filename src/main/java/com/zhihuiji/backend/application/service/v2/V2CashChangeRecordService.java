package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.CashChangeRecordEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.CashChangeRecordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2CashChangeRecordService {
    private final CashChangeRecordRepository cashChangeRecordRepository;
    private final AccountRepository accountRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2CashChangeRecordService(
        CashChangeRecordRepository cashChangeRecordRepository,
        AccountRepository accountRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.cashChangeRecordRepository = cashChangeRecordRepository;
        this.accountRepository = accountRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2FinanceDtos.CashChangeRecordResponse> list(String orderType, Long orderId, Long accountId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return cashChangeRecordRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
            .filter(entity -> orderType == null || orderType.isBlank() || entity.getOrderType().equalsIgnoreCase(orderType.trim()))
            .filter(entity -> orderId == null || orderId.equals(entity.getOrderId()))
            .filter(entity -> accountId == null || accountId.equals(entity.getAccountId()))
            .map(this::toResponse)
            .toList();
    }

    public V2FinanceDtos.CashChangeRecordResponse get(Long id) {
        return toResponse(getOwnedEntity(id));
    }

    @Transactional
    public V2FinanceDtos.CashChangeRecordResponse create(V2FinanceDtos.CashChangeRecordCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String orderType = normalizeRequired(request.orderType(), "单据类型不能为空");
        double receivable = requireNonNegative(request.receivable(), "应收金额不能为空且不能小于 0");
        double received = requireNonNegative(request.received(), "实收金额不能为空且不能小于 0");
        if (received < receivable) {
            throw new IllegalArgumentException("实收金额不能小于应收金额");
        }
        double changeAmount = received - receivable;
        AccountEntity account = null;
        if (request.accountId() != null) {
            account = accountRepository.findByIdAndOwnerUserId(request.accountId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
        }

        long now = System.currentTimeMillis();
        CashChangeRecordEntity entity = new CashChangeRecordEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderType(orderType);
        entity.setOrderId(request.orderId());
        entity.setReceivable(receivable);
        entity.setReceived(received);
        entity.setChangeAmount(changeAmount);
        entity.setAccountId(account != null ? account.getId() : null);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setNotes(normalizeOptional(request.notes()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        CashChangeRecordEntity saved = cashChangeRecordRepository.save(entity);
        if (account != null) {
            account.setBalance((account.getBalance() != null ? account.getBalance() : 0.0) + receivable);
            account.setUpdatedAt(now);
            accountRepository.save(account);
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        CashChangeRecordEntity entity = getOwnedEntity(id);
        if (entity.getAccountId() != null) {
            AccountEntity account = accountRepository.findByIdAndOwnerUserId(
                entity.getAccountId(),
                currentOwnerService.requireCurrentOwnerUserId()
            ).orElseThrow(() -> new IllegalArgumentException("账户不存在"));
            account.setBalance((account.getBalance() != null ? account.getBalance() : 0.0) - entity.getReceivable());
            account.setUpdatedAt(System.currentTimeMillis());
            accountRepository.save(account);
        }
        cashChangeRecordRepository.delete(entity);
    }

    private CashChangeRecordEntity getOwnedEntity(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return cashChangeRecordRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("找零记录不存在"));
    }

    private V2FinanceDtos.CashChangeRecordResponse toResponse(CashChangeRecordEntity entity) {
        String accountName = null;
        if (entity.getAccountId() != null) {
            accountName = accountRepository.findByIdAndOwnerUserId(entity.getAccountId(), currentOwnerService.requireCurrentOwnerUserId())
                .map(AccountEntity::getName)
                .orElse("未知账户");
        }
        return new V2FinanceDtos.CashChangeRecordResponse(
            entity.getId(),
            entity.getOrderType(),
            entity.getOrderId(),
            entity.getReceivable(),
            entity.getReceived(),
            entity.getChangeAmount(),
            entity.getAccountId(),
            accountName,
            entity.getStatus(),
            entity.getNotes(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private double requireNonNegative(Double value, String message) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("找零状态不合法");
        }
        return status;
    }
}
