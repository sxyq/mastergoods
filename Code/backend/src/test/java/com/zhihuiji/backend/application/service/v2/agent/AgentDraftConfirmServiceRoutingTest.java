package com.zhihuiji.backend.application.service.v2.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.api.dto.v2.inventory.V2InventoryDtos;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.FinanceRecordService;
import com.zhihuiji.backend.application.service.v2.AgentImageService;
import com.zhihuiji.backend.application.service.v2.V2AccountTransferService;
import com.zhihuiji.backend.application.service.v2.V2CustomerService;
import com.zhihuiji.backend.application.service.v2.V2InventoryService;
import com.zhihuiji.backend.application.service.v2.V2PayOrderService;
import com.zhihuiji.backend.application.service.v2.V2PurchaseOrderService;
import com.zhihuiji.backend.application.service.v2.V2PurchaseReceiptService;
import com.zhihuiji.backend.application.service.v2.V2PurchaseReturnService;
import com.zhihuiji.backend.application.service.v2.V2SaleOrderService;
import com.zhihuiji.backend.application.service.v2.V2SalesReturnService;
import com.zhihuiji.backend.application.service.v2.V2SupplierService;
import com.zhihuiji.backend.application.service.v2.product.V2ProductService;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AgentDraftConfirmServiceRoutingTest {

    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private CurrentOwnerService currentOwnerService;
    @Mock private V2SaleOrderService saleOrderService;
    @Mock private V2PurchaseOrderService purchaseOrderService;
    @Mock private V2PurchaseReceiptService purchaseReceiptService;
    @Mock private V2SalesReturnService salesReturnService;
    @Mock private V2PurchaseReturnService purchaseReturnService;
    @Mock private V2PayOrderService payOrderService;
    @Mock private V2CustomerService customerService;
    @Mock private V2SupplierService supplierService;
    @Mock private V2ProductService productService;
    @Mock private FinanceRecordService financeRecordService;
    @Mock private V2InventoryService inventoryService;
    @Mock private V2AccountTransferService accountTransferService;
    @Mock private AgentImageService imageService;

    private AgentDraftConfirmService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AgentDraftConfirmService(
            agentDraftRepository, currentOwnerService, new ObjectMapper(), saleOrderService,
            purchaseOrderService, purchaseReceiptService, salesReturnService, purchaseReturnService,
            payOrderService, customerService, supplierService, productService, financeRecordService,
            inventoryService, accountTransferService, imageService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(agentDraftRepository.updateStatusIfCurrent(
            anyLong(), anyLong(), anyString(), anyString(), anyLong()
        )).thenReturn(1);
        when(agentDraftRepository.save(any(AgentDraftEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createCustomerRoutesAndConfirmsOnlyCustomerDraft() {
        AgentDraftEntity draft = activeDraft(101L, "create_customer",
            "{\"name\":\"客户A\",\"phone\":\"13800000001\",\"level\":1}");
        when(agentDraftRepository.findByIdAndOwnerUserId(101L, 1L)).thenReturn(Optional.of(draft));

        service.confirmDraft(101L);

        verify(customerService).create(any(V2PartnerDtos.CustomerWriteRequest.class));
        assertConfirmed(draft);
        verifyNoOtherInteractions(customerService);
    }

    @Test
    void createSupplierRoutesAndConfirmsOnlySupplierDraft() {
        AgentDraftEntity draft = activeDraft(102L, "create_supplier",
            "{\"name\":\"供应商A\",\"phone\":\"13900000001\"}");
        when(agentDraftRepository.findByIdAndOwnerUserId(102L, 1L)).thenReturn(Optional.of(draft));

        service.confirmDraft(102L);

        verify(supplierService).create(any(V2PartnerDtos.SupplierWriteRequest.class));
        assertConfirmed(draft);
        verifyNoOtherInteractions(supplierService);
    }

    @Test
    void createProductRoutesAndConfirmsOnlyProductDraft() {
        AgentDraftEntity draft = activeDraft(103L, "create_product",
            "{\"code\":\"P-001\",\"name\":\"商品A\",\"category_id\":1,\"unit_id\":1,"
                + "\"sale_price\":10.0,\"purchase_price\":5.0,\"stock\":0.0,"
                + "\"safe_stock\":0.0,\"status\":1}");
        when(agentDraftRepository.findByIdAndOwnerUserId(103L, 1L)).thenReturn(Optional.of(draft));

        service.confirmDraft(103L);

        verify(productService).create(any(V2ProductDtos.ProductWriteRequest.class));
        assertConfirmed(draft);
        verifyNoOtherInteractions(productService);
    }

    @Test
    void createFinanceRecordRoutesAndConfirmsOnlyFinanceService() {
        AgentDraftEntity draft = activeDraft(104L, "create_finance_record",
            "{\"type\":1,\"category\":\"销售收入\",\"amount\":100.0}");
        when(agentDraftRepository.findByIdAndOwnerUserId(104L, 1L)).thenReturn(Optional.of(draft));

        service.confirmDraft(104L);

        verify(financeRecordService).create(any(FinanceRecordService.CreateCommand.class));
        assertConfirmed(draft);
        verifyNoOtherInteractions(financeRecordService);
    }

    @Test
    void createAccountTransferRoutesAndConfirmsOnlyTransferService() {
        AgentDraftEntity draft = activeDraft(105L, "create_account_transfer",
            "{\"from_account_id\":1,\"to_account_id\":2,\"amount\":20.0}");
        when(agentDraftRepository.findByIdAndOwnerUserId(105L, 1L)).thenReturn(Optional.of(draft));

        service.confirmDraft(105L);

        verify(accountTransferService).create(any(V2FinanceDtos.AccountTransferCreateRequest.class));
        assertConfirmed(draft);
        verifyNoOtherInteractions(accountTransferService);
    }

    @Test
    void inventoryAdjustmentAliasesRouteToLedgerCreation() {
        for (String draftType : new String[] {"inventory_adjustment", "create_inventory_adjustment"}) {
            AgentDraftEntity draft = activeDraft(106L, draftType,
                "{\"product_id\":1,\"quantity_change\":2.0,\"source_type\":\"manual\"}");
            when(agentDraftRepository.findByIdAndOwnerUserId(106L, 1L)).thenReturn(Optional.of(draft));

            service.confirmDraft(106L);

            assertEquals("confirmed", draft.getStatus());
            assertEquals(1L, draft.getConfirmedBy());
            assertNotNull(draft.getConfirmedAt());
        }
        verify(inventoryService, times(2)).createLedgerEntry(any(V2InventoryDtos.LedgerEntryCreateRequest.class));
        verify(agentDraftRepository, times(2)).updateStatusIfCurrent(
            eq(106L), eq(1L), eq("active"), eq("confirming"), anyLong()
        );
        verify(agentDraftRepository, times(2)).save(any(AgentDraftEntity.class));
        verifyNoInteractions(saleOrderService, purchaseOrderService, purchaseReceiptService,
            salesReturnService, purchaseReturnService, payOrderService,
            customerService, supplierService, productService, financeRecordService,
            accountTransferService, imageService);
    }

    @Test
    void activeDraftMustBeClaimedBeforeBusinessCreation() {
        AgentDraftEntity draft = activeDraft(107L, "create_customer",
            "{\"name\":\"客户B\",\"phone\":\"13800000002\",\"level\":1}");
        when(agentDraftRepository.findByIdAndOwnerUserId(107L, 1L)).thenReturn(Optional.of(draft));
        when(agentDraftRepository.updateStatusIfCurrent(eq(107L), eq(1L), eq("active"),
            eq("confirming"), anyLong())).thenReturn(0);

        org.junit.jupiter.api.Assertions.assertThrows(
            com.zhihuiji.backend.api.common.BusinessException.class,
            () -> service.confirmDraft(107L)
        );

        assertEquals("active", draft.getStatus());
        verifyNoInteractions(customerService, saleOrderService, purchaseOrderService,
            purchaseReceiptService, salesReturnService, purchaseReturnService, payOrderService,
            supplierService, productService, financeRecordService, inventoryService,
            accountTransferService, imageService);
    }

    private void assertConfirmed(AgentDraftEntity draft) {
        assertEquals("confirmed", draft.getStatus());
        assertEquals(1L, draft.getConfirmedBy());
        assertNotNull(draft.getConfirmedAt());
        verify(agentDraftRepository).updateStatusIfCurrent(
            eq(draft.getId()), eq(1L), eq("active"), eq("confirming"), anyLong()
        );
        verify(agentDraftRepository).save(draft);
    }

    private void verifyNoOtherInteractions(Object targetService) {
        Object[] services = {
            saleOrderService, purchaseOrderService, purchaseReceiptService, salesReturnService,
            purchaseReturnService, payOrderService, customerService, supplierService,
            productService, financeRecordService, inventoryService, accountTransferService,
            imageService
        };
        verifyNoInteractions(java.util.Arrays.stream(services)
            .filter(service -> service != targetService)
            .toArray());
    }

    private AgentDraftEntity activeDraft(Long id, String draftType, String contentJson) {
        AgentDraftEntity entity = new AgentDraftEntity();
        try {
            Field field = AgentDraftEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
        entity.setOwnerUserId(1L);
        entity.setConversationId(1L);
        entity.setDraftType(draftType);
        entity.setTitle("routing test");
        entity.setContentJson(contentJson);
        entity.setStatus("active");
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}
