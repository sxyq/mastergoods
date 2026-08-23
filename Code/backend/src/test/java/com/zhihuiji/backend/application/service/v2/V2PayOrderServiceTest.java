package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

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
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private V2PayOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        service = new V2PayOrderService(
            payOrderService,
            payOrderRepository,
            accountRepository,
            billFundLinkRepository,
            currentOwnerService,
            transactionManager
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
        when(payOrderService.createForOwner(anyLong(), any(), any())).thenReturn(entity);
        when(accountRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(account(5L, 100.0)));
        when(payOrderRepository.save(any(PayOrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(1L, "pay_order", 11L, 2))
            .thenReturn(Optional.of(existingLink));

        service.create(new V2PayOrderDtos.CreateRequest(
            null,
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
    void repeatedCreateWithSameIdempotencyKeyReturnsExistingOrder() {
        PayOrderEntity existing = payOrder(21L, null, PayOrderStatus.DRAFT.code(), 18.0);
        existing.setSupplierId(null);
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(1L, "pay-retry-21"))
            .thenReturn(Optional.of(existing));

        V2PayOrderDtos.PayOrderResponse response = service.create(new V2PayOrderDtos.CreateRequest(
            " pay-retry-21 ", null, "供应商A", 18.0, 1, null, null, null, null
        ));

        assertEquals(21L, response.id());
        verify(payOrderService, never()).create(any());
        verify(payOrderRepository, never()).save(any(PayOrderEntity.class));
    }

    @Test
    void reusedIdempotencyKeyWithDifferentPayloadIsRejected() throws Exception {
        PayOrderEntity existing = payOrder(23L, null, PayOrderStatus.DRAFT.code(), 18.0);
        existing.setIdempotencyPayloadHash(hash("", "供应商A", "18.0", "1", "", "", "", ""));
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(1L, "pay-conflict-23"))
            .thenReturn(Optional.of(existing));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.create(
            new V2PayOrderDtos.CreateRequest(
                "pay-conflict-23", null, "供应商B", 18.0, 1, null, null, null, null
            )));

        assertEquals("相同幂等键不能用于不同付款请求", error.getMessage());
        verify(payOrderService, never()).createForOwner(anyLong(), any(), any());
    }

    @Test
    void idempotencyConflictReturnsOrderCommittedByConcurrentRequest() {
        PayOrderEntity existing = payOrder(22L, null, PayOrderStatus.DRAFT.code(), 18.0);
        existing.setSupplierId(null);
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(1L, "pay-race-22"))
            .thenReturn(Optional.empty(), Optional.of(existing));
        when(payOrderService.createForOwner(anyLong(), any(), any()))
            .thenThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        V2PayOrderDtos.PayOrderResponse response = service.create(new V2PayOrderDtos.CreateRequest(
            "pay-race-22", null, "供应商A", 18.0, 1, null, null, null, null
        ));

        assertEquals(22L, response.id());
        verify(payOrderRepository, times(2)).findByOwnerUserIdAndIdempotencyKey(1L, "pay-race-22");
    }

    @Test
    void nullRequestIsRejectedAsBusinessInputError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.create(null));

        assertEquals("付款单参数不能为空", error.getMessage());
        verify(payOrderService, never()).createForOwner(anyLong(), any(), any());
    }

    @Test
    void publicCreateRejectsNullIdempotencyKey() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWithRequiredIdempotencyKey(request(null))
        );

        assertEquals("幂等键不能为空", error.getMessage());
        verify(payOrderService, never()).createForOwner(anyLong(), any(), any());
    }

    @Test
    void sameKeySamePayloadHashReturnsExistingOrder() throws Exception {
        PayOrderEntity existing = payOrder(31L, null, PayOrderStatus.DRAFT.code(), 18.0);
        existing.setSupplierId(null);
        existing.setIdempotencyPayloadHash(hash("", "供应商A", "18.0", "1", "", "", "", ""));
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(1L, "pay-same-payload-31"))
            .thenReturn(Optional.of(existing));

        V2PayOrderDtos.PayOrderResponse response = service.create(
            new V2PayOrderDtos.CreateRequest("pay-same-payload-31", null, "供应商A", 18.0, 1, null, null, null, null)
        );

        assertEquals(31L, response.id());
        verify(payOrderService, never()).createForOwner(anyLong(), any(), any());
    }

    @Test
    void differentOwnerWithSameKeyCreatesIndependentOrders() {
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L, 2L);
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(1L, "shared-key-33"))
            .thenReturn(Optional.empty());
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(2L, "shared-key-33"))
            .thenReturn(Optional.empty());
        PayOrderEntity owner1Order = payOrder(101L, null, PayOrderStatus.DRAFT.code(), 50.0);
        owner1Order.setOwnerUserId(1L);
        PayOrderEntity owner2Order = payOrder(102L, null, PayOrderStatus.DRAFT.code(), 70.0);
        owner2Order.setOwnerUserId(2L);
        when(payOrderService.createForOwner(eq(1L), any(), eq("shared-key-33")))
            .thenReturn(owner1Order);
        when(payOrderService.createForOwner(eq(2L), any(), eq("shared-key-33")))
            .thenReturn(owner2Order);

        V2PayOrderDtos.PayOrderResponse response1 = service.create(
            new V2PayOrderDtos.CreateRequest("shared-key-33", null, "供应商A", 50.0, 1, null, null, null, null)
        );
        V2PayOrderDtos.PayOrderResponse response2 = service.create(
            new V2PayOrderDtos.CreateRequest("shared-key-33", null, "供应商B", 70.0, 1, null, null, null, null)
        );

        assertEquals(101L, response1.id());
        assertEquals(102L, response2.id());
        verify(payOrderService).createForOwner(eq(1L), any(), eq("shared-key-33"));
        verify(payOrderService).createForOwner(eq(2L), any(), eq("shared-key-33"));
    }

    @Test
    void retryAfterServiceFailureCreatesOrderSuccessfully() {
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(1L, "pay-retry-35"))
            .thenReturn(Optional.empty());
        when(payOrderService.createForOwner(anyLong(), any(), eq("pay-retry-35")))
            .thenThrow(new IllegalArgumentException("temporary failure"))
            .thenReturn(payOrder(35L, null, PayOrderStatus.DRAFT.code(), 20.0));

        assertThrows(IllegalArgumentException.class, () -> service.create(
            new V2PayOrderDtos.CreateRequest("pay-retry-35", null, "供应商A", 20.0, 1, null, null, null, null)
        ));

        V2PayOrderDtos.PayOrderResponse response = service.create(
            new V2PayOrderDtos.CreateRequest("pay-retry-35", null, "供应商A", 20.0, 1, null, null, null, null)
        );

        assertEquals(35L, response.id());
        verify(payOrderService, times(2)).createForOwner(anyLong(), any(), eq("pay-retry-35"));
    }

    @Test
    void illegalIdempotencyKeyWithControlCharacterIsRejected() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWithRequiredIdempotencyKey(request("pay\torder"))
        );

        assertEquals("幂等键格式不合法", error.getMessage());
        verify(payOrderService, never()).createForOwner(anyLong(), any(), any());
    }

    @Test
    void illegalIdempotencyKeyWithSpaceIsRejected() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWithRequiredIdempotencyKey(request("pay order"))
        );

        assertEquals("幂等键格式不合法", error.getMessage());
        verify(payOrderService, never()).createForOwner(anyLong(), any(), any());
    }

    @Test
    void publicCreateRequiresNonBlankSafeIdempotencyKey() {
        IllegalArgumentException blank = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWithRequiredIdempotencyKey(request("   "))
        );
        IllegalArgumentException control = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWithRequiredIdempotencyKey(request("pay\norder"))
        );
        IllegalArgumentException longKey = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWithRequiredIdempotencyKey(request("a".repeat(129)))
        );

        assertEquals("幂等键不能为空", blank.getMessage());
        assertEquals("幂等键格式不合法", control.getMessage());
        assertEquals("幂等键长度不能超过128个字符", longKey.getMessage());
    }

    @Test
    void legacyOrderWithoutPayloadHashRejectsDifferentPayload() {
        PayOrderEntity existing = payOrder(24L, null, PayOrderStatus.DRAFT.code(), 18.0);
        existing.setSupplierId(null);
        when(payOrderRepository.findByOwnerUserIdAndIdempotencyKey(1L, "legacy-24"))
            .thenReturn(Optional.of(existing));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(request("legacy-24", "供应商B", 18.0))
        );

        assertEquals("相同幂等键不能用于不同付款请求", error.getMessage());
        verify(payOrderService, never()).createForOwner(anyLong(), any(), any());
    }

    @Test
    void transitionToPaidDeductsBalanceAndCreatesSingleLink() {
        PayOrderEntity before = payOrder(12L, 8L, PayOrderStatus.DRAFT.code(), 40.0);
        PayOrderEntity after = payOrder(12L, 8L, PayOrderStatus.PAID.code(), 40.0);
        AccountEntity account = account(8L, 120.0);
        when(payOrderService.getById(12L)).thenReturn(before);
        when(payOrderService.updateStatus(12L, PayOrderStatus.PAID.code())).thenReturn(after);
        when(accountRepository.findByIdAndOwnerUserIdForUpdate(8L, 1L)).thenReturn(Optional.of(account));
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
        when(accountRepository.findByIdAndOwnerUserIdForUpdate(9L, 1L)).thenReturn(Optional.of(account));
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
        when(accountRepository.findByIdAndOwnerUserIdForUpdate(6L, 1L)).thenReturn(Optional.of(account));
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
        when(accountRepository.findByIdAndOwnerUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(account));
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
        when(accountRepository.findByIdAndOwnerUserIdForUpdate(4L, 1L)).thenReturn(Optional.of(account(4L, 30.0)));
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

    private static String hash(String... values) throws Exception {
        String canonical = String.join("\u001f", values);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }

    private static V2PayOrderDtos.CreateRequest request(String idempotencyKey) {
        return request(idempotencyKey, "供应商A", 18.0);
    }

    private static V2PayOrderDtos.CreateRequest request(String idempotencyKey, String supplierName, Double amount) {
        return new V2PayOrderDtos.CreateRequest(
            idempotencyKey,
            null,
            supplierName,
            amount,
            1,
            null,
            null,
            null,
            null
        );
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
