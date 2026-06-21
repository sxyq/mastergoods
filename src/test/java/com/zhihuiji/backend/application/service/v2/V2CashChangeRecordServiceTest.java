package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.CashChangeRecordEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.CashChangeRecordRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2CashChangeRecordServiceTest {
    @Mock
    private CashChangeRecordRepository cashChangeRecordRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2CashChangeRecordService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2CashChangeRecordService(cashChangeRecordRepository, accountRepository, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createRejectsWhenReceivedLessThanReceivable() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(new V2FinanceDtos.CashChangeRecordCreateRequest("sale_order", 9L, 100.0, 80.0, null, null, null))
        );

        assertEquals("实收金额不能小于应收金额", error.getMessage());
    }

    @Test
    void createRejectsUnknownAccount() {
        when(accountRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(new V2FinanceDtos.CashChangeRecordCreateRequest("sale_order", 9L, 100.0, 120.0, 5L, null, null))
        );

        assertEquals("账户不存在", error.getMessage());
    }

    @Test
    void createComputesChangeAmountAndUpdatesAccountBalance() {
        AccountEntity account = new AccountEntity();
        account.setId(5L);
        account.setOwnerUserId(1L);
        account.setName("现金");
        account.setBalance(200.0);
        when(accountRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(cashChangeRecordRepository.save(any(CashChangeRecordEntity.class))).thenAnswer(invocation -> {
            CashChangeRecordEntity entity = invocation.getArgument(0);
            entity.setId(18L);
            return entity;
        });

        V2FinanceDtos.CashChangeRecordResponse response = service.create(
            new V2FinanceDtos.CashChangeRecordCreateRequest("sale_order", 9L, 100.0, 120.0, 5L, null, "找零")
        );

        assertEquals(18L, response.id());
        assertEquals(20.0, response.changeAmount());
        assertEquals("现金", response.accountName());
        assertEquals(300.0, account.getBalance());
    }

    @Test
    void deleteRollsBackAccountBalance() {
        CashChangeRecordEntity entity = new CashChangeRecordEntity();
        entity.setId(9L);
        entity.setOwnerUserId(1L);
        entity.setOrderType("sale_order");
        entity.setOrderId(7L);
        entity.setReceivable(88.0);
        entity.setReceived(100.0);
        entity.setChangeAmount(12.0);
        entity.setAccountId(5L);
        when(cashChangeRecordRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.of(entity));

        AccountEntity account = new AccountEntity();
        account.setId(5L);
        account.setOwnerUserId(1L);
        account.setBalance(300.0);
        when(accountRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(account));

        service.delete(9L);

        assertEquals(212.0, account.getBalance());
    }
}
