package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.AccountTransferRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2AccountTransferServiceTest {
    @Mock
    private AccountTransferRepository accountTransferRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2AccountTransferService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2AccountTransferService(accountTransferRepository, accountRepository, currentOwnerService, new IdGenerator());
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createRejectsSameAccountTransfer() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.create(new V2FinanceDtos.AccountTransferCreateRequest(1L, 1L, 10.0, 0.0, "dup"))
        );
    }

    @Test
    void createRetriesWhenGeneratedTransferNoAlreadyExists() {
        AccountEntity from = account(1L, "现金", 120.0);
        AccountEntity to = account(2L, "银行卡", 30.0);
        when(accountRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdAndOwnerUserId(2L, 1L)).thenReturn(Optional.of(to));
        when(accountTransferRepository.existsByOwnerUserIdAndTransferNo(any(), anyString())).thenReturn(true, false);
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountTransferRepository.save(any(AccountTransferEntity.class))).thenAnswer(invocation -> {
            AccountTransferEntity entity = invocation.getArgument(0);
            entity.setId(21L);
            return entity;
        });

        V2FinanceDtos.AccountTransferResponse response = service.create(
            new V2FinanceDtos.AccountTransferCreateRequest(1L, 2L, 20.0, 2.0, "调拨")
        );

        assertEquals(21L, response.id());
        assertEquals("现金", response.fromAccountName());
        assertEquals("银行卡", response.toAccountName());
        assertEquals(98.0, from.getBalance());
        assertEquals(50.0, to.getBalance());
    }

    private static AccountEntity account(Long id, String name, Double balance) {
        AccountEntity entity = new AccountEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(name);
        entity.setName(name);
        entity.setType(1);
        entity.setBalance(balance);
        entity.setIsDefault(false);
        entity.setStatus(1);
        entity.setSortOrder(0);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
