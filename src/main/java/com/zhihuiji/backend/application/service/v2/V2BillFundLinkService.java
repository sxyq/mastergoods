package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.BillFundLinkRepository;
import java.util.List;
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
        return billFundLinkRepository.findAllByOwnerUserIdAndBillTypeAndBillIdOrderByCreatedAtDesc(ownerUserId, billType, billId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<V2FinanceDtos.BillFundLinkResponse> listByAccount(Long accountId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return billFundLinkRepository.findAllByOwnerUserIdAndAccountIdOrderByCreatedAtDesc(ownerUserId, accountId).stream()
            .map(this::toResponse)
            .toList();
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

    private V2FinanceDtos.BillFundLinkResponse toResponse(BillFundLinkEntity entity) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String accountName = accountRepository.findByIdAndOwnerUserId(entity.getAccountId(), ownerUserId)
            .map(AccountEntity::getName).orElse("未知账户");
        return new V2FinanceDtos.BillFundLinkResponse(
            entity.getId(), entity.getBillType(), entity.getBillId(),
            entity.getAccountId(), accountName, entity.getAmount(),
            entity.getLinkType(), entity.getNotes(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
