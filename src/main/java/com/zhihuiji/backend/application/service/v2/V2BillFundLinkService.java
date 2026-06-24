package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.BillFundLinkRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2BillFundLinkService {
    private final BillFundLinkRepository billFundLinkRepository;
    private final AccountRepository accountRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2BillFundLinkService(BillFundLinkRepository billFundLinkRepository,
                                 AccountRepository accountRepository,
                                 CurrentOwnerService currentOwnerService) {
        this.billFundLinkRepository = billFundLinkRepository;
        this.accountRepository = accountRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2FinanceDtos.BillFundLinkResponse> listByBill(String billType, Long billId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<BillFundLinkEntity> rows = billFundLinkRepository.findAllByOwnerUserIdAndBillTypeAndBillIdOrderByCreatedAtDesc(ownerUserId, billType, billId);
        return toResponses(ownerUserId, rows);
    }

    public List<V2FinanceDtos.BillFundLinkResponse> listByAccount(Long accountId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<BillFundLinkEntity> rows = billFundLinkRepository.findAllByOwnerUserIdAndAccountIdOrderByCreatedAtDesc(ownerUserId, accountId);
        return toResponses(ownerUserId, rows);
    }

    @Transactional
    public V2FinanceDtos.BillFundLinkResponse create(V2FinanceDtos.BillFundLinkCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        accountRepository.findByIdAndOwnerUserId(request.accountId(), ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
        long now = System.currentTimeMillis();
        BillFundLinkEntity entity = new BillFundLinkEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setBillType(request.billType());
        entity.setBillId(request.billId());
        entity.setAccountId(request.accountId());
        entity.setAmount(request.amount());
        entity.setLinkType(request.linkType() != null ? request.linkType() : 1);
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
        List<V2FinanceDtos.BillFundLinkResponse> responses = new ArrayList<>(rows.size());
        for (BillFundLinkEntity row : rows) {
            responses.add(toResponse(row, accountNames.get(row.getAccountId())));
        }
        return responses;
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
}
