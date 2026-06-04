package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2AccountService {
    private final AccountRepository accountRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2AccountService(AccountRepository accountRepository, CurrentOwnerService currentOwnerService) {
        this.accountRepository = accountRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2FinanceDtos.AccountResponse> list() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return accountRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId).stream()
            .map(this::toResponse)
            .toList();
    }

    public V2FinanceDtos.AccountResponse get(Long id) {
        return toResponse(getOwnedEntity(id));
    }

    @Transactional
    public V2FinanceDtos.AccountResponse create(V2FinanceDtos.AccountCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String code = normalizeRequired(request.code(), "账户编码不能为空");
        String name = normalizeRequired(request.name(), "账户名称不能为空");
        if (accountRepository.existsByOwnerUserIdAndCode(ownerUserId, code)) {
            throw new IllegalArgumentException("账户编码已存在");
        }
        long now = System.currentTimeMillis();
        AccountEntity entity = new AccountEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setCode(code);
        entity.setName(name);
        entity.setType(request.type());
        entity.setBalance(request.balance() != null ? request.balance() : 0.0);
        entity.setIsDefault(request.isDefault() != null ? request.isDefault() : false);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        entity.setNotes(request.notes());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(accountRepository.save(entity));
    }

    @Transactional
    public V2FinanceDtos.AccountResponse update(Long id, V2FinanceDtos.AccountUpdateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AccountEntity entity = getOwnedEntity(id);
        String code = normalizeRequired(request.code(), "账户编码不能为空");
        String name = normalizeRequired(request.name(), "账户名称不能为空");
        if (accountRepository.existsByOwnerUserIdAndCodeAndIdNot(ownerUserId, code, id)) {
            throw new IllegalArgumentException("账户编码已存在");
        }
        entity.setCode(code);
        entity.setName(name);
        entity.setType(request.type());
        entity.setIsDefault(request.isDefault() != null ? request.isDefault() : false);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        entity.setNotes(request.notes());
        entity.setUpdatedAt(System.currentTimeMillis());
        return toResponse(accountRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        AccountEntity entity = getOwnedEntity(id);
        accountRepository.delete(entity);
    }

    AccountEntity getOwnedEntity(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return accountRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
    }

    private V2FinanceDtos.AccountResponse toResponse(AccountEntity entity) {
        return new V2FinanceDtos.AccountResponse(
            entity.getId(), entity.getCode(), entity.getName(), entity.getType(),
            entity.getBalance(), entity.getIsDefault(), entity.getStatus(),
            entity.getSortOrder(), entity.getNotes(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) return 1;
        if (status != 0 && status != 1) throw new IllegalArgumentException("账户状态不合法");
        return status;
    }
}
