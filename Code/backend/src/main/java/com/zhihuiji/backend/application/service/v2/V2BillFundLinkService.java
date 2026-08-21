package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.BillFundLinkRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2BillFundLinkService {
    private final BillFundLinkRepository billFundLinkRepository;
    private final AccountRepository accountRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PayOrderRepository payOrderRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2BillFundLinkService(BillFundLinkRepository billFundLinkRepository,
                                 AccountRepository accountRepository,
                                 SaleOrderRepository saleOrderRepository,
                                 PurchaseOrderRepository purchaseOrderRepository,
                                 PayOrderRepository payOrderRepository,
                                 FinanceRecordRepository financeRecordRepository,
                                 CurrentOwnerService currentOwnerService) {
        this.billFundLinkRepository = billFundLinkRepository;
        this.accountRepository = accountRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.payOrderRepository = payOrderRepository;
        this.financeRecordRepository = financeRecordRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2FinanceDtos.BillFundLinkResponse> listByBill(String billType, Long billId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedBillType = normalizeBillType(billType);
        requirePositiveId(billId, "关联单据不能为空");
        List<BillFundLinkEntity> rows = billFundLinkRepository.findAllByOwnerUserIdAndBillTypeAndBillIdOrderByCreatedAtDesc(ownerUserId, normalizedBillType, billId);
        return toResponses(ownerUserId, rows);
    }

    public List<V2FinanceDtos.BillFundLinkResponse> listByAccount(Long accountId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        requirePositiveId(accountId, "账户不能为空");
        List<BillFundLinkEntity> rows = billFundLinkRepository.findAllByOwnerUserIdAndAccountIdOrderByCreatedAtDesc(ownerUserId, accountId);
        return toResponses(ownerUserId, rows);
    }

    @Transactional
    public V2FinanceDtos.BillFundLinkResponse create(V2FinanceDtos.BillFundLinkCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("资金关联不能为空");
        }
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String billType = normalizeBillType(request.billType());
        requirePositiveId(request.billId(), "关联单据不能为空");
        requirePositiveId(request.accountId(), "账户不能为空");
        double amount = requirePositiveAmount(request.amount());
        int linkType = request.linkType() == null ? 1 : request.linkType();
        if (linkType != 1 && linkType != 2) {
            throw new IllegalArgumentException("资金关联类型不合法");
        }
        if (!billExists(ownerUserId, billType, request.billId())) {
            throw new IllegalArgumentException("关联单据不存在");
        }
        accountRepository.findByIdAndOwnerUserId(request.accountId(), ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
        long now = System.currentTimeMillis();
        BillFundLinkEntity entity = new BillFundLinkEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setBillType(billType);
        entity.setBillId(request.billId());
        entity.setAccountId(request.accountId());
        entity.setAmount(amount);
        entity.setLinkType(linkType);
        entity.setNotes(request.notes());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(billFundLinkRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        BillFundLinkEntity entity = billFundLinkRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("单据资金关联不存在"));
        billFundLinkRepository.delete(entity);
    }

    private List<V2FinanceDtos.BillFundLinkResponse> toResponses(Long ownerUserId, List<BillFundLinkEntity> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> accountIds = new java.util.LinkedHashSet<>(rows.size());
        for (BillFundLinkEntity row : rows) {
            accountIds.add(row.getAccountId());
        }
        Map<Long, String> accountNames = new HashMap<>(accountIds.size());
        for (AccountEntity account : accountRepository.findAllByOwnerUserIdAndIdIn(ownerUserId, accountIds)) {
            accountNames.put(account.getId(), account.getName());
        }
        return rows.stream()
            .map(row -> toResponse(row, accountNames.get(row.getAccountId())))
            .toList();
    }

    private V2FinanceDtos.BillFundLinkResponse toResponse(BillFundLinkEntity entity) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String accountName = accountRepository.findByIdAndOwnerUserId(entity.getAccountId(), ownerUserId)
            .map(AccountEntity::getName).orElse("未知账户");
        return toResponse(entity, accountName);
    }

    private V2FinanceDtos.BillFundLinkResponse toResponse(BillFundLinkEntity entity, String accountName) {
        return new V2FinanceDtos.BillFundLinkResponse(
            entity.getId(), entity.getBillType(), entity.getBillId(),
            entity.getAccountId(), accountName != null ? accountName : "未知账户", entity.getAmount(),
            entity.getLinkType(), entity.getNotes(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private String normalizeBillType(String billType) {
        if (billType == null || billType.isBlank()) {
            throw new IllegalArgumentException("关联单据类型不能为空");
        }
        String normalized = billType.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("sale_order", "purchase_order", "pay_order", "finance_record").contains(normalized)) {
            throw new IllegalArgumentException("关联单据类型不支持");
        }
        return normalized;
    }

    private void requirePositiveId(Long id, String message) {
        if (id == null || id <= 0L) {
            throw new IllegalArgumentException(message);
        }
    }

    private double requirePositiveAmount(Double amount) {
        if (amount == null || !Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("关联金额必须是有限且大于0的数字");
        }
        return amount;
    }

    private boolean billExists(Long ownerUserId, String billType, Long billId) {
        return switch (billType) {
            case "sale_order" -> saleOrderRepository.findByIdAndOwnerUserId(billId, ownerUserId).isPresent();
            case "purchase_order" -> purchaseOrderRepository.findByIdAndOwnerUserId(billId, ownerUserId).isPresent();
            case "pay_order" -> payOrderRepository.findByIdAndOwnerUserId(billId, ownerUserId).isPresent();
            case "finance_record" -> financeRecordRepository.findByIdAndOwnerUserId(billId, ownerUserId).isPresent();
            default -> false;
        };
    }
}
