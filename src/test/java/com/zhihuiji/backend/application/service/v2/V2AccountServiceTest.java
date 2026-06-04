package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2AccountService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2AccountService(accountRepository, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createInitializesDefaultsWhenOptionalFieldsMissing() {
        when(accountRepository.existsByOwnerUserIdAndCode(1L, "CASH")).thenReturn(false);
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return entity;
        });

        V2FinanceDtos.AccountResponse response = service.create(
            new V2FinanceDtos.AccountCreateRequest(" CASH ", " 现金 ", 1, null, null, null, null, "默认账户")
        );

        assertEquals(11L, response.id());
        assertEquals("CASH", response.code());
        assertEquals("现金", response.name());
        assertEquals(0.0, response.balance());
        assertEquals(false, response.isDefault());
        assertEquals(1, response.status());
        assertEquals(0, response.sortOrder());
    }

    @Test
    void updateDoesNotRequireBalanceAndPreservesExistingBalance() {
        AccountEntity entity = new AccountEntity();
        entity.setId(11L);
        entity.setOwnerUserId(1L);
        entity.setCode("CASH");
        entity.setName("现金");
        entity.setType(1);
        entity.setBalance(88.5);
        entity.setIsDefault(false);
        entity.setStatus(1);
        entity.setSortOrder(0);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(2L);
        when(accountRepository.findByIdAndOwnerUserId(11L, 1L)).thenReturn(Optional.of(entity));
        when(accountRepository.existsByOwnerUserIdAndCodeAndIdNot(1L, "BANK", 11L)).thenReturn(false);
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        V2FinanceDtos.AccountResponse response = service.update(
            11L,
            new V2FinanceDtos.AccountUpdateRequest(" BANK ", " 银行账户 ", 2, true, 1, 3, "已更新")
        );

        assertEquals("BANK", response.code());
        assertEquals("银行账户", response.name());
        assertEquals(2, response.type());
        assertEquals(88.5, response.balance());
        assertEquals(true, response.isDefault());
        assertEquals(3, response.sortOrder());
    }
}
