package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.AccountTransferRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2AccountTransferService {
    private final AccountTransferRepository accountTransferRepository;
    private final AccountRepository accountRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2AccountTransferService(AccountTransferRepository accountTransferRepository,
                                    AccountRepository accountRepository,
                                    CurrentOwnerService currentOwnerService) {
        this.accountTransferRepository = accountTransferRepository;
        this.accountRepository = accountRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2FinanceDtos.AccountTransferResponse> list() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return accountTransferRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
            .map(entity -> toResponse(ownerUserId, entity))
            .toList();
    }

    public V2FinanceDtos.AccountTransferResponse get(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AccountTransferEntity entity = accountTransferRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("转账记录不存在"));
        return toResponse(ownerUserId, entity);
    }

    @Transactional
    public V2FinanceDtos.AccountTransferResponse create(V2FinanceDtos.AccountTransferCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new IllegalArgumentException("转出和转入账户不能相同");
        }
        AccountEntity fromAccount = accountRepository.findByIdAndOwnerUserId(request.fromAccountId(), ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("转出账户不存在"));
        AccountEntity toAccount = accountRepository.findByIdAndOwnerUserId(request.toAccountId(), ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("转入账户不存在"));
        Double amount = request.amount();
        Double fee = request.fee() != null ? request.fee() : 0.0;
        if (amount <= 0) {
            throw new IllegalArgumentException("转账金额必须大于0");
        }
        if (fromAccount.getBalance() < amount + fee) {
            throw new IllegalArgumentException("转出账户余额不足");
        }
        String transferNo = generateTransferNo(ownerUserId);
        long now = System.currentTimeMillis();
        AccountTransferEntity entity = new AccountTransferEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setTransferNo(transferNo);
        entity.setFromAccountId(fromAccount.getId());
        entity.setToAccountId(toAccount.getId());
        entity.setAmount(amount);
        entity.setFee(fee);
        entity.setStatus(1);
        entity.setNotes(request.notes());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        fromAccount.setBalance(fromAccount.getBalance() - amount - fee);
        toAccount.setBalance(toAccount.getBalance() + amount);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        try {
            return toResponse(ownerUserId, accountTransferRepository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("生成转账单号失败，请重试", ex);
        }
    }

    private String generateTransferNo(Long ownerUserId) {
        for (int i = 0; i < 5; i++) {
            String transferNo = "TF" + IdGenerator.nextId();
            if (!accountTransferRepository.existsByOwnerUserIdAndTransferNo(ownerUserId, transferNo)) {
                return transferNo;
            }
        }
        throw new IllegalStateException("生成转账单号失败，请重试");
    }

    private V2FinanceDtos.AccountTransferResponse toResponse(Long ownerUserId, AccountTransferEntity entity) {
        String fromName = accountRepository.findByIdAndOwnerUserId(entity.getFromAccountId(), ownerUserId)
            .map(AccountEntity::getName).orElse("未知账户");
        String toName = accountRepository.findByIdAndOwnerUserId(entity.getToAccountId(), ownerUserId)
            .map(AccountEntity::getName).orElse("未知账户");
        return new V2FinanceDtos.AccountTransferResponse(
            entity.getId(), entity.getTransferNo(), entity.getFromAccountId(), fromName,
            entity.getToAccountId(), toName, entity.getAmount(), entity.getFee(),
            entity.getStatus(), entity.getNotes(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
