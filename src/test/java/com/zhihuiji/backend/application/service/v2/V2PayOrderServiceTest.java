package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2PayOrderServiceTest {
    @Mock
    private PayOrderService payOrderService;
    @Mock
    private PayOrderRepository payOrderRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BillFundLinkRepository billFundLinkRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2PayOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2PayOrderService(
            payOrderService,
            payOrderRepository,
            accountRepository,
            billFundLinkRepository,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void repeatedPaidStatusDoesNotDeductBalanceOrCreateDuplicateLink() {
        PayOrderEntity entity = payOrder(9L, 7L, PayOrderStatus.PAID.code(), 60.0);
        when(payOrderService.getById(9L)).thenReturn(entity);
        when(payOrderService.updateStatus(9L, PayOrderStatus.PAID.code())).thenReturn(entity);

        service.updateStatus(9L, PayOrderStatus.PAID.code());

        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(billFundLinkRepository, never()).save(any(BillFundLinkEntity.class));
    }

    @Test
    void createWithInitialPaidStatusDoesNotCreateDuplicateLinkWhenExistingMarkerFound() {
        PayOrderEntity entity = payOrder(11L, 5L, PayOrderStatus.PAID.code(), 35.0);
        BillFundLinkEntity existingLink = paidLink(11L, 5L, 35.0);
        when(payOrderService.create(any())).thenReturn(entity);
        when(accountRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(account(5L, 100.0)));
        when(payOrderRepository.save(any(PayOrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(1L, "pay_order", 11L, 2))
            .thenReturn(Optional.of(existingLink));

        service.create(new V2PayOrderDtos.CreateRequest(
            3L,
            null,
            35.0,
            1,
            "REF-1",
            "初始已付款",
            5L,
            PayOrderStatus.PAID.code()
        ));

        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(billFundLinkRepository, never()).save(any(BillFundLinkEntity.class));
    }

    @Test
    void transitionToPaidDeductsBalanceAndCreatesSingleLink() {
        PayOrderEntity before = payOrder(12L, 8L, PayOrderStatus.DRAFT.code(), 40.0);
        PayOrderEntity after = payOrder(12L, 8L, PayOrderStatus.PAID.code(), 40.0);
        AccountEntity account = account(8L, 120.0);
        when(payOrderService.getById(12L)).thenReturn(before);
        when(payOrderService.updateStatus(12L, PayOrderStatus.PAID.code())).thenReturn(after);
        when(accountRepository.findByIdAndOwnerUserId(8L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(1L, "pay_order", 12L, 2))
            .thenReturn(Optional.empty());
        when(billFundLinkRepository.save(any(BillFundLinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(12L, PayOrderStatus.PAID.code());

        assertEquals(80.0, account.getBalance());
        verify(accountRepository, times(1)).save(account);
        verify(billFundLinkRepository, times(1)).save(any(BillFundLinkEntity.class));
    }

    @Test
    void transitionToPaidStillWorksWhenUpdateMutatesSameEntityReference() {
        PayOrderEntity sameEntity = payOrder(13L, 9L, PayOrderStatus.DRAFT.code(), 25.0);
        AccountEntity account = account(9L, 90.0);
        when(payOrderService.getById(13L)).thenReturn(sameEntity);
        doAnswer(invocation -> {
            sameEntity.setStatus(PayOrderStatus.PAID.code());
            return sameEntity;
        }).when(payOrderService).updateStatus(13L, PayOrderStatus.PAID.code());
        when(accountRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(1L, "pay_order", 13L, 2))
            .thenReturn(Optional.empty());
        when(billFundLinkRepository.save(any(BillFundLinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(13L, PayOrderStatus.PAID.code());

        assertEquals(65.0, account.getBalance());
        verify(accountRepository, times(1)).save(account);
        verify(billFundLinkRepository, times(1)).save(any(BillFundLinkEntity.class));
    }

    @Test
    void transitionAwayFromPaidRestoresBalanceAndDeletesLink() {
        PayOrderEntity before = payOrder(15L, 6L, PayOrderStatus.PAID.code(), 55.0);
        PayOrderEntity after = payOrder(15L, 6L, PayOrderStatus.CANCELLED.code(), 55.0);
        AccountEntity account = account(6L, 20.0);
        BillFundLinkEntity existingLink = paidLink(15L, 6L, 55.0);
        when(payOrderService.getById(15L)).thenReturn(before);
        when(payOrderService.updateStatus(15L, PayOrderStatus.CANCELLED.code())).thenReturn(after);
        when(billFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(1L, "pay_order", 15L, 2))
            .thenReturn(Optional.of(existingLink));
        when(accountRepository.findByIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(15L, PayOrderStatus.CANCELLED.code());

        assertEquals(75.0, account.getBalance());
        verify(accountRepository, times(1)).save(account);
        verify(billFundLinkRepository, times(1)).delete(existingLink);
    }

    @Test
    void transitionAwayFromPaidUsesLinkAccountWhenEntityAccountIdIsMissing() {
        PayOrderEntity before = payOrder(16L, null, PayOrderStatus.PAID.code(), 45.0);
        PayOrderEntity after = payOrder(16L, null, PayOrderStatus.CANCELLED.code(), 45.0);
        AccountEntity account = account(10L, 15.0);
        BillFundLinkEntity existingLink = paidLink(16L, 10L, 45.0);
        when(payOrderService.getById(16L)).thenReturn(before);
        when(payOrderService.updateStatus(16L, PayOrderStatus.CANCELLED.code())).thenReturn(after);
        when(billFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(1L, "pay_order", 16L, 2))
            .thenReturn(Optional.of(existingLink));
        when(accountRepository.findByIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(16L, PayOrderStatus.CANCELLED.code());

        assertEquals(60.0, account.getBalance());
        verify(accountRepository, times(1)).save(account);
        verify(billFundLinkRepository, times(1)).delete(existingLink);
    }

    @Test
    void transitionToPaidRejectsInsufficientBalance() {
        PayOrderEntity before = payOrder(18L, 4L, PayOrderStatus.DRAFT.code(), 90.0);
        PayOrderEntity after = payOrder(18L, 4L, PayOrderStatus.PAID.code(), 90.0);
        when(payOrderService.getById(18L)).thenReturn(before);
        when(payOrderService.updateStatus(18L, PayOrderStatus.PAID.code())).thenReturn(after);
        when(accountRepository.findByIdAndOwnerUserId(4L, 1L)).thenReturn(Optional.of(account(4L, 30.0)));
        when(billFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(1L, "pay_order", 18L, 2))
            .thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateStatus(18L, PayOrderStatus.PAID.code())
        );

        assertEquals("账户余额不足", error.getMessage());
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(billFundLinkRepository, never()).save(any(BillFundLinkEntity.class));
    }

    private static PayOrderEntity payOrder(Long id, Long accountId, Integer status, Double amount) {
        PayOrderEntity entity = new PayOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("POUT-" + id);
        entity.setSupplierId(3L);
        entity.setSupplierName("供应商A");
        entity.setAmount(amount);
        entity.setMethod(1);
        entity.setAccountId(accountId);
        entity.setStatus(status);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(2L);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        return entity;
    }

    private static AccountEntity account(Long id, Double balance) {
        AccountEntity entity = new AccountEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode("AC-" + id);
        entity.setName("账户" + id);
        entity.setBalance(balance);
        return entity;
    }

    private static BillFundLinkEntity paidLink(Long billId, Long accountId, Double amount) {
        BillFundLinkEntity entity = new BillFundLinkEntity();
        entity.setId(100L + billId);
        entity.setOwnerUserId(1L);
        entity.setBillType("pay_order");
        entity.setBillId(billId);
        entity.setAccountId(accountId);
        entity.setAmount(amount);
        entity.setLinkType(2);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
