package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.BillFundLinkRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2BillFundLinkServiceTest {
    @Mock
    private BillFundLinkRepository billFundLinkRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2BillFundLinkService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2BillFundLinkService(billFundLinkRepository, accountRepository, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createRejectsUnknownAccount() {
        when(accountRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> service.create(new V2FinanceDtos.BillFundLinkCreateRequest("sale_order", 9L, 5L, 19.9, null, "x"))
        );
    }

    @Test
    void createInitializesDefaultLinkType() {
        AccountEntity account = new AccountEntity();
        account.setId(5L);
        account.setOwnerUserId(1L);
        account.setName("现金");
        when(accountRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(billFundLinkRepository.save(any(BillFundLinkEntity.class))).thenAnswer(invocation -> {
            BillFundLinkEntity entity = invocation.getArgument(0);
            entity.setId(18L);
            return entity;
        });

        V2FinanceDtos.BillFundLinkResponse response = service.create(
            new V2FinanceDtos.BillFundLinkCreateRequest("sale_order", 9L, 5L, 19.9, null, "收款")
        );

        assertEquals(18L, response.id());
        assertEquals(1, response.linkType());
        assertEquals("现金", response.accountName());
    }
}
