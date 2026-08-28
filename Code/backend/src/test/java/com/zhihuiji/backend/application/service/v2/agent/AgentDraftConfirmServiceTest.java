package com.zhihuiji.backend.application.service.v2.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.api.dto.v2.inventory.V2InventoryDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.FinanceRecordService;
import com.zhihuiji.backend.application.service.v2.AgentImageService;
import com.zhihuiji.backend.application.service.v2.V2AccountTransferService;
import com.zhihuiji.backend.application.service.v2.V2CustomerService;
import com.zhihuiji.backend.application.service.v2.V2InventoryService;
import com.zhihuiji.backend.application.service.v2.V2PayOrderService;
import com.zhihuiji.backend.application.service.v2.product.V2ProductService;
import com.zhihuiji.backend.application.service.v2.V2PurchaseOrderService;
import com.zhihuiji.backend.application.service.v2.V2PurchaseReceiptService;
import com.zhihuiji.backend.application.service.v2.V2PurchaseReturnService;
import com.zhihuiji.backend.application.service.v2.V2SaleOrderService;
import com.zhihuiji.backend.application.service.v2.V2SalesReturnService;
import com.zhihuiji.backend.application.service.v2.V2SupplierService;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AgentDraftConfirmServiceTest {

    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private CurrentOwnerService currentOwnerService;
    @Mock private V2SaleOrderService v2SaleOrderService;
    @Mock private V2PurchaseOrderService v2PurchaseOrderService;
    @Mock private V2PurchaseReceiptService v2PurchaseReceiptService;
    @Mock private V2SalesReturnService v2SalesReturnService;
    @Mock private V2PurchaseReturnService v2PurchaseReturnService;
    @Mock private V2PayOrderService v2PayOrderService;
    @Mock private V2CustomerService v2CustomerService;
    @Mock private V2SupplierService v2SupplierService;
    @Mock private V2ProductService v2ProductService;
    @Mock private FinanceRecordService financeRecordService;
    @Mock private V2InventoryService v2InventoryService;
    @Mock private V2AccountTransferService v2AccountTransferService;
    @Mock private AgentImageService agentImageService;

    private AgentDraftConfirmService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AgentDraftConfirmService(
            agentDraftRepository,
            currentOwnerService,
            new ObjectMapper(),
            v2SaleOrderService,
            v2PurchaseOrderService,
            v2PurchaseReceiptService,
            v2SalesReturnService,
            v2PurchaseReturnService,
            v2PayOrderService,
            v2CustomerService,
            v2SupplierService,
            v2ProductService,
            financeRecordService,
            v2InventoryService,
            v2AccountTransferService,
            agentImageService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(agentDraftRepository.updateStatusIfCurrent(
            anyLong(), anyLong(), anyString(), anyString(), anyLong()
        )).thenReturn(1);
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void confirmDraftSupportsLegacyInventoryAdjustmentDraftType() {
        AgentDraftEntity draft = activeDraft(
            11L,
            "inventory_adjustment",
            """
                {
                  "product_id": 7,
                  "source_type": "inventory_count",
                  "source_no": "count-123",
                  "quantity_change": -2.5,
                  "notes": "盘点记录：系统库存 10，盘点数量 7.5"
                }
                """
        );
        when(agentDraftRepository.findByIdAndOwnerUserId(11L, 1L)).thenReturn(Optional.of(draft));

        var response = service.confirmDraft(11L);

        verify(v2InventoryService).createLedgerEntry(any(V2InventoryDtos.LedgerEntryCreateRequest.class));
        assertEquals("confirmed", draft.getStatus());
        assertEquals("inventory_adjustment", response.draftType());
        assertNotNull(response.updatedAt());
    }

    @Test
    void confirmDraftAllowsMediaUploadIntentWithoutBusinessWrite() {
        AgentDraftEntity draft = activeDraft(
            12L,
            "media_upload",
            """
                {
                  "file_name": "sku.png",
                  "file_size": 2048,
                  "mime_type": "image/png",
                  "binding_type": "product"
                }
                """
        );
        when(agentDraftRepository.findByIdAndOwnerUserId(12L, 1L)).thenReturn(Optional.of(draft));

        var response = service.confirmDraft(12L);

        verifyNoInteractions(v2InventoryService, v2SaleOrderService, v2PurchaseOrderService, v2AccountTransferService);
        assertEquals("confirmed", draft.getStatus());
        assertEquals("media_upload", response.draftType());
    }

    @Test
    void confirmImageDraftCallsProviderOnlyAfterConfirmationAndReturnsImageResult() {
        AgentDraftEntity draft = activeDraft(
            15L,
            "image_generate",
            "{\"prompt\":\"生成商品主图\",\"reference_asset_ids\":[]}"
        );
        when(agentDraftRepository.findByIdAndOwnerUserId(15L, 1L)).thenReturn(Optional.of(draft));
        when(agentImageService.generate(any())).thenReturn(
            new V2AgentDtos.AgentImageGenerateResponse("data:image/png;base64,ZmFrZQ==", "优化后的提示词")
        );

        var response = service.confirmDraft(15L);

        verify(agentImageService).generate(new V2AgentDtos.AgentImageGenerateRequest("生成商品主图", List.of()));
        assertEquals("confirmed", draft.getStatus());
        assertEquals("data:image/png;base64,ZmFrZQ==", response.imageResult().imageUrl());
        assertEquals("优化后的提示词", response.imageResult().revisedPrompt());
    }

    @Test
    void providerFailureLeavesImageDraftActiveForRetryWithoutSavingConfirmedState() {
        AgentDraftEntity draft = activeDraft(
            16L,
            "image_generate",
            "{\"prompt\":\"生成商品主图\",\"reference_asset_ids\":[]}"
        );
        when(agentDraftRepository.findByIdAndOwnerUserId(16L, 1L)).thenReturn(Optional.of(draft));
        when(agentImageService.generate(any())).thenThrow(
            new com.zhihuiji.backend.api.common.BusinessException("生图服务请求超时")
        );

        var error = org.junit.jupiter.api.Assertions.assertThrows(
            com.zhihuiji.backend.api.common.BusinessException.class,
            () -> service.confirmDraft(16L)
        );

        assertEquals("生图服务请求超时", error.getMessage());
        assertEquals("active", draft.getStatus());
        verify(agentDraftRepository, org.mockito.Mockito.never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void confirmDraftRejectsWhenConditionalClaimWasLost() {
        AgentDraftEntity draft = activeDraft(13L, "media_upload", "{}");
        when(agentDraftRepository.findByIdAndOwnerUserId(13L, 1L)).thenReturn(Optional.of(draft));
        when(agentDraftRepository.updateStatusIfCurrent(eq(13L), eq(1L), eq("active"), eq("confirming"), anyLong()))
            .thenReturn(0);

        assertThrows(com.zhihuiji.backend.api.common.BusinessException.class, () -> service.confirmDraft(13L));
        verifyNoInteractions(v2InventoryService, v2SaleOrderService, v2PurchaseOrderService, v2AccountTransferService);
    }

    @Test
    void cancelDraftRejectsNonActiveDraft() {
        AgentDraftEntity draft = activeDraft(14L, "media_upload", "{}");
        draft.setStatus("confirmed");
        when(agentDraftRepository.findByIdAndOwnerUserId(14L, 1L)).thenReturn(Optional.of(draft));

        assertThrows(com.zhihuiji.backend.api.common.BusinessException.class, () -> service.cancelDraft(14L));
    }

    private AgentDraftEntity activeDraft(Long id, String draftType, String contentJson) {
        AgentDraftEntity entity = new AgentDraftEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setConversationId(101L);
        entity.setDraftType(draftType);
        entity.setTitle("test");
        entity.setContentJson(contentJson);
        entity.setStatus("active");
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private void setId(AgentDraftEntity entity, Long id) {
        try {
            Field field = AgentDraftEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
