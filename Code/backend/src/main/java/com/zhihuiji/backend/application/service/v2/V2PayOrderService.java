package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.PayOrderStatus;
import com.zhihuiji.backend.api.dto.v2.pay.V2PayOrderDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.PayOrderService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.BillFundLinkRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2PayOrderService {
    private final PayOrderService payOrderService;
    private final PayOrderRepository payOrderRepository;
    private final AccountRepository accountRepository;
    private final BillFundLinkRepository billFundLinkRepository;
    private final CurrentOwnerService currentOwnerService;
    private final TransactionTemplate transactionTemplate;

    public V2PayOrderService(
        PayOrderService payOrderService,
        PayOrderRepository payOrderRepository,
        AccountRepository accountRepository,
        BillFundLinkRepository billFundLinkRepository,
        CurrentOwnerService currentOwnerService,
        PlatformTransactionManager transactionManager
    ) {
        this.payOrderService = payOrderService;
        this.payOrderRepository = payOrderRepository;
        this.accountRepository = accountRepository;
        this.billFundLinkRepository = billFundLinkRepository;
        this.currentOwnerService = currentOwnerService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<V2PayOrderDtos.PayOrderResponse> list(
        String keyword,
        Integer status,
        Long createdAfter,
        Long createdBefore
    ) {
        return list(keyword, status, createdAfter, createdBefore, null);
    }

    @Transactional(readOnly = true)
    public List<V2PayOrderDtos.PayOrderResponse> list(
        String keyword,
        Integer status,
        Long createdAfter,
        Long createdBefore,
        Pageable pageable
    ) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedKeyword = keyword == null || keyword.trim().isBlank() ? null : keyword.trim();
        List<PayOrderEntity> rows = pageable == null
            ? payOrderRepository.search(ownerUserId, normalizedKeyword, status, createdAfter, createdBefore)
            : payOrderRepository.search(ownerUserId, normalizedKeyword, status, createdAfter, createdBefore, pageable);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public V2PayOrderDtos.PayOrderResponse get(Long id) {
        return toResponse(payOrderService.getById(id));
    }

    public V2PayOrderDtos.PayOrderResponse create(V2PayOrderDtos.CreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        String payloadHash = idempotencyKey == null ? null : payloadHash(request);
        if (idempotencyKey != null) {
            PayOrderEntity existing = payOrderRepository
                .findByOwnerUserIdAndIdempotencyKey(ownerUserId, idempotencyKey)
                .orElse(null);
            if (existing != null) {
                assertSamePayload(existing, payloadHash);
                return toResponse(existing);
            }
        }
        try {
            return transactionTemplate.execute(status -> toResponse(createWithinTransaction(ownerUserId, request, idempotencyKey, payloadHash)));
        } catch (DataIntegrityViolationException ex) {
            if (idempotencyKey == null) {
                throw ex;
            }
            return payOrderRepository.findByOwnerUserIdAndIdempotencyKey(ownerUserId, idempotencyKey)
                .map(entity -> {
                    assertSamePayload(entity, payloadHash);
                    return toResponse(entity);
                })
                .orElseThrow(() -> ex);
        }
    }

    private PayOrderEntity createWithinTransaction(
        Long ownerUserId,
        V2PayOrderDtos.CreateRequest request,
        String idempotencyKey,
        String payloadHash
    ) {
        PayOrderEntity entity = payOrderService.createForOwner(
            ownerUserId,
            new PayOrderService.CreateCommand(
                request.supplierId(),
                request.supplierName(),
                request.amount(),
                request.method(),
                request.referenceNo(),
                request.notes(),
                request.status()
            ),
            idempotencyKey
        );
        entity.setIdempotencyPayloadHash(payloadHash);
        if (request.accountId() != null) {
            accountRepository.findByIdAndOwnerUserId(request.accountId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
            entity.setAccountId(request.accountId());
            payOrderRepository.save(entity);
        }
        syncPaidAccountSideEffects(entity, null, ownerUserId);
        return entity;
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("幂等键长度不能超过128个字符");
        }
        return normalized;
    }

    private void assertSamePayload(PayOrderEntity existing, String payloadHash) {
        if (existing.getIdempotencyPayloadHash() != null
            && !existing.getIdempotencyPayloadHash().equals(payloadHash)) {
            throw new IllegalArgumentException("相同幂等键不能用于不同付款请求");
        }
    }

    private String payloadHash(V2PayOrderDtos.CreateRequest request) {
        String canonical = String.join("\u001f",
            value(request.supplierId()),
            value(request.supplierName()),
            value(request.amount()),
            value(request.method()),
            value(request.referenceNo()),
            value(request.notes()),
            value(request.accountId()),
            value(request.status())
        );
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @Transactional
    public V2PayOrderDtos.PayOrderResponse updateStatus(Long id, Integer status) {
        Integer previousStatus = payOrderService.getById(id).getStatus();
        PayOrderEntity entity = payOrderService.updateStatus(id, status);
        syncPaidAccountSideEffects(entity, previousStatus, currentOwnerService.requireCurrentOwnerUserId());
        return toResponse(entity);
    }

    private void syncPaidAccountSideEffects(PayOrderEntity entity, Integer previousStatus, Long ownerUserId) {
        boolean wasPaid = isPaid(previousStatus);
        boolean isPaid = isPaid(entity.getStatus());
        if (!wasPaid && isPaid) {
            if (entity.getAccountId() == null) {
                return;
            }
            ensurePaidLinkAndDeduct(entity, ownerUserId);
        } else if (wasPaid && !isPaid) {
            restoreAccountAndRemoveLink(entity, ownerUserId);
        }
    }

    private void ensurePaidLinkAndDeduct(PayOrderEntity entity, Long ownerUserId) {
        if (findPaidLink(entity, ownerUserId) != null) {
            return;
        }
        AccountEntity account = accountRepository.findByIdAndOwnerUserIdForUpdate(entity.getAccountId(), ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
        if (account.getBalance() < entity.getAmount() - 0.000001) {
            throw new IllegalArgumentException("账户余额不足");
        }
        account.setBalance(account.getBalance() - entity.getAmount());
        account.setUpdatedAt(System.currentTimeMillis());
        accountRepository.save(account);
        billFundLinkRepository.save(buildPaidLink(entity, ownerUserId, account.getId()));
    }

    private void restoreAccountAndRemoveLink(PayOrderEntity entity, Long ownerUserId) {
        BillFundLinkEntity existingLink = findPaidLink(entity, ownerUserId);
        if (existingLink == null) {
            return;
        }
        Long accountId = existingLink.getAccountId() != null ? existingLink.getAccountId() : entity.getAccountId();
        if (accountId != null) {
            AccountEntity account = accountRepository.findByIdAndOwnerUserIdForUpdate(accountId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
            account.setBalance(account.getBalance() + entity.getAmount());
            account.setUpdatedAt(System.currentTimeMillis());
            accountRepository.save(account);
        }
        billFundLinkRepository.delete(existingLink);
    }

    private BillFundLinkEntity findPaidLink(PayOrderEntity entity, Long ownerUserId) {
        return billFundLinkRepository
            .findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(ownerUserId, "pay_order", entity.getId(), 2)
            .orElse(null);
    }

    private BillFundLinkEntity buildPaidLink(PayOrderEntity entity, Long ownerUserId, Long accountId) {
        BillFundLinkEntity link = new BillFundLinkEntity();
        link.setOwnerUserId(ownerUserId);
        link.setBillType("pay_order");
        link.setBillId(entity.getId());
        link.setAccountId(accountId);
        link.setAmount(entity.getAmount());
        link.setLinkType(2);
        link.setCreatedAt(System.currentTimeMillis());
        link.setUpdatedAt(System.currentTimeMillis());
        return link;
    }

    private boolean isPaid(Integer status) {
        return status != null && status == PayOrderStatus.PAID.code();
    }

    private V2PayOrderDtos.PayOrderResponse toResponse(PayOrderEntity entity) {
        return new V2PayOrderDtos.PayOrderResponse(
            entity.getId(),
            entity.getOrderNo(),
            entity.getSupplierId(),
            entity.getSupplierName(),
            entity.getAmount(),
            entity.getMethod(),
            entity.getReferenceNo(),
            entity.getNotes(),
            entity.getAccountId(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
