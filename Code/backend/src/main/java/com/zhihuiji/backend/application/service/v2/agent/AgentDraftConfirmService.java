package com.zhihuiji.backend.application.service.v2.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.api.dto.v2.inventory.V2InventoryDtos;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.api.dto.v2.pay.V2PayOrderDtos;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseOrderDtos;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReceiptDtos;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReturnDtos;
import com.zhihuiji.backend.api.dto.v2.sales.V2SaleOrderDtos;
import com.zhihuiji.backend.api.dto.v2.sales.V2SalesReturnDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.FinanceRecordService;
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
import com.zhihuiji.backend.application.service.v2.AgentImageService;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 草稿确认服务。
 *
 * <p>负责读取 agent_drafts 表中的 active 草稿，根据 draftType 路由到对应业务 Service.create
 * 执行真正写入，确认成功后更新草稿 status=confirmed；取消则置为 cancelled。
 *
 * <p>路由逻辑通过 switch(draftType) 分发，每个分支将 contentJson（JSON 字符串）反序列化为
 * 对应 CreateRequest DTO 后调用业务 Service。写入工具生成草稿时已将 contentJson 字段与目标
 * CreateRequest（snake_case，FinanceRecordService.CreateCommand 为 camelCase）对齐，可直接反序列化。
 *
 * <p>错误处理：草稿不存在、状态非 active 抛 BusinessException；反序列化或业务创建失败时
 * 通过 try/catch 捕获并抛 BusinessException 携带错误信息，草稿保持 active 供用户重试或取消。
 */
@Service
public class AgentDraftConfirmService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CONFIRMING = "confirming";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final int PENDING_PAGE_SIZE = 100;

    private final AgentDraftRepository agentDraftRepository;
    private final CurrentOwnerService currentOwnerService;
    private final ObjectMapper objectMapper;

    private final V2SaleOrderService v2SaleOrderService;
    private final V2PurchaseOrderService v2PurchaseOrderService;
    private final V2PurchaseReceiptService v2PurchaseReceiptService;
    private final V2SalesReturnService v2SalesReturnService;
    private final V2PurchaseReturnService v2PurchaseReturnService;
    private final V2PayOrderService v2PayOrderService;
    private final V2CustomerService v2CustomerService;
    private final V2SupplierService v2SupplierService;
    private final V2ProductService v2ProductService;
    private final FinanceRecordService financeRecordService;
    private final V2InventoryService v2InventoryService;
    private final V2AccountTransferService v2AccountTransferService;
    private final AgentImageService agentImageService;
    private final AgentDraftConfirmationStateService confirmationStateService;

    @org.springframework.beans.factory.annotation.Autowired
    public AgentDraftConfirmService(
        AgentDraftRepository agentDraftRepository,
        CurrentOwnerService currentOwnerService,
        ObjectMapper objectMapper,
        V2SaleOrderService v2SaleOrderService,
        V2PurchaseOrderService v2PurchaseOrderService,
        V2PurchaseReceiptService v2PurchaseReceiptService,
        V2SalesReturnService v2SalesReturnService,
        V2PurchaseReturnService v2PurchaseReturnService,
        V2PayOrderService v2PayOrderService,
        V2CustomerService v2CustomerService,
        V2SupplierService v2SupplierService,
        V2ProductService v2ProductService,
        FinanceRecordService financeRecordService,
        V2InventoryService v2InventoryService,
        V2AccountTransferService v2AccountTransferService,
        AgentImageService agentImageService,
        AgentDraftConfirmationStateService confirmationStateService
    ) {
        this.agentDraftRepository = agentDraftRepository;
        this.currentOwnerService = currentOwnerService;
        this.objectMapper = objectMapper;
        this.v2SaleOrderService = v2SaleOrderService;
        this.v2PurchaseOrderService = v2PurchaseOrderService;
        this.v2PurchaseReceiptService = v2PurchaseReceiptService;
        this.v2SalesReturnService = v2SalesReturnService;
        this.v2PurchaseReturnService = v2PurchaseReturnService;
        this.v2PayOrderService = v2PayOrderService;
        this.v2CustomerService = v2CustomerService;
        this.v2SupplierService = v2SupplierService;
        this.v2ProductService = v2ProductService;
        this.financeRecordService = financeRecordService;
        this.v2InventoryService = v2InventoryService;
        this.v2AccountTransferService = v2AccountTransferService;
        this.agentImageService = agentImageService;
        this.confirmationStateService = confirmationStateService;
    }

    /** Compatibility constructor for isolated tests and legacy callers. */
    public AgentDraftConfirmService(
        AgentDraftRepository agentDraftRepository,
        CurrentOwnerService currentOwnerService,
        ObjectMapper objectMapper,
        V2SaleOrderService v2SaleOrderService,
        V2PurchaseOrderService v2PurchaseOrderService,
        V2PurchaseReceiptService v2PurchaseReceiptService,
        V2SalesReturnService v2SalesReturnService,
        V2PurchaseReturnService v2PurchaseReturnService,
        V2PayOrderService v2PayOrderService,
        V2CustomerService v2CustomerService,
        V2SupplierService v2SupplierService,
        V2ProductService v2ProductService,
        FinanceRecordService financeRecordService,
        V2InventoryService v2InventoryService,
        V2AccountTransferService v2AccountTransferService,
        AgentImageService agentImageService
    ) {
        this(agentDraftRepository, currentOwnerService, objectMapper, v2SaleOrderService, v2PurchaseOrderService,
            v2PurchaseReceiptService, v2SalesReturnService, v2PurchaseReturnService, v2PayOrderService,
            v2CustomerService, v2SupplierService, v2ProductService, financeRecordService, v2InventoryService,
            v2AccountTransferService, agentImageService, null);
    }

    /**
     * 列出当前 owner 下所有 status=active 的待确认草稿。
     *
     * @return 草稿响应列表
     */
    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentDraftResponse> listPendingDrafts() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return agentDraftRepository
            .findAllByOwnerUserIdAndStatusIgnoreCaseOrderByUpdatedAtDescIdDesc(
                ownerUserId, STATUS_ACTIVE, Pageable.ofSize(PENDING_PAGE_SIZE))
            .stream()
            .map(this::toDraftResponse)
            .toList();
    }

    /**
     * 确认草稿：读取草稿 → 按 draftType 路由到对应业务 Service.create → 更新 status=confirmed。
     *
     * <p>反序列化或业务创建失败时抛 BusinessException，草稿保持 active 状态供重试。
     *
     * @param draftId 草稿 ID
     * @return 确认后的草稿响应
     */
    @Transactional
    public V2AgentDtos.AgentDraftResponse confirmDraft(Long draftId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentDraftEntity entity = agentDraftRepository.findByIdAndOwnerUserId(draftId, ownerUserId)
            .orElseThrow(() -> new BusinessException("草稿不存在"));
        if (STATUS_CONFIRMED.equalsIgnoreCase(entity.getStatus())) {
            return toDraftResponse(entity);
        }
        if (!STATUS_ACTIVE.equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("草稿状态不是 active，无法确认：" + entity.getStatus());
        }
        if (agentDraftRepository.updateStatusIfCurrent(
            draftId,
            ownerUserId,
            STATUS_ACTIVE,
            STATUS_CONFIRMING,
            System.currentTimeMillis()
        ) != 1) {
            throw new BusinessException("草稿已被其他请求确认或状态已变化");
        }
        Object created;
        try {
            created = dispatchCreate(entity);
        } catch (BusinessException ex) {
            recordFailure(entity, ownerUserId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            String message = "草稿确认失败（" + entity.getDraftType() + "）：" + cause.getMessage();
            recordFailure(entity, ownerUserId, message);
            throw new BusinessException(message);
        }
        V2AgentDtos.AgentImageGenerateResponse imageResult = created instanceof V2AgentDtos.AgentImageGenerateResponse response
            ? response : null;
        if (imageResult != null) {
            entity.setContentJson(AgentDraftImageResultCodec.withImageResult(
                objectMapper, entity.getContentJson(), imageResult
            ));
        }
        entity.setConfirmedBy(ownerUserId);
        entity.setConfirmedAt(System.currentTimeMillis());
        entity.setBusinessReference(businessReference(entity.getDraftType(), created));
        entity.setFailureReason(null);
        entity.setStatus(STATUS_CONFIRMED);
        entity.setUpdatedAt(System.currentTimeMillis());
        return toDraftResponse(agentDraftRepository.save(entity));
    }

    /**
     * 取消草稿：将 status 置为 cancelled。
     *
     * @param draftId 草稿 ID
     * @return 取消后的草稿响应
     */
    @Transactional
    public V2AgentDtos.AgentDraftResponse cancelDraft(Long draftId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentDraftEntity entity = agentDraftRepository.findByIdAndOwnerUserId(draftId, ownerUserId)
            .orElseThrow(() -> new BusinessException("草稿不存在"));
        if (!STATUS_ACTIVE.equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("草稿状态不是 active，无法取消：" + entity.getStatus());
        }
        if (agentDraftRepository.updateStatusIfCurrent(
            draftId,
            ownerUserId,
            STATUS_ACTIVE,
            STATUS_CANCELLED,
            System.currentTimeMillis()
        ) != 1) {
            throw new BusinessException("草稿已被其他请求确认或状态已变化");
        }
        entity.setStatus(STATUS_CANCELLED);
        entity.setUpdatedAt(System.currentTimeMillis());
        return toDraftResponse(agentDraftRepository.save(entity));
    }

    /**
     * 按 draftType 路由到对应业务 Service.create，contentJson 反序列化为对应 CreateRequest。
     *
     * @param entity 草稿实体
     * @throws Exception 反序列化或业务创建异常
     */
    private Object dispatchCreate(AgentDraftEntity entity) throws Exception {
        String contentJson = entity.getContentJson();
        String draftType = entity.getDraftType();
        return switch (draftType) {
            case "create_sale_order" -> v2SaleOrderService.create(objectMapper.readValue(contentJson, V2SaleOrderDtos.CreateRequest.class));
            case "create_purchase_order" -> v2PurchaseOrderService.create(objectMapper.readValue(contentJson, V2PurchaseOrderDtos.CreateRequest.class));
            case "create_purchase_receipt" -> v2PurchaseReceiptService.create(objectMapper.readValue(contentJson, V2PurchaseReceiptDtos.CreateRequest.class));
            case "create_sales_return" -> v2SalesReturnService.create(objectMapper.readValue(contentJson, V2SalesReturnDtos.CreateRequest.class));
            case "create_purchase_return" -> v2PurchaseReturnService.create(objectMapper.readValue(contentJson, V2PurchaseReturnDtos.CreateRequest.class));
            case "create_pay_order" -> v2PayOrderService.createWithRequiredIdempotencyKey(
                objectMapper.readValue(contentJson, V2PayOrderDtos.CreateRequest.class));
            case "create_customer" -> v2CustomerService.create(objectMapper.readValue(contentJson, V2PartnerDtos.CustomerWriteRequest.class));
            case "create_supplier" -> v2SupplierService.create(objectMapper.readValue(contentJson, V2PartnerDtos.SupplierWriteRequest.class));
            case "create_product" -> v2ProductService.create(objectMapper.readValue(contentJson, V2ProductDtos.ProductWriteRequest.class));
            case "create_finance_record" -> financeRecordService.create(objectMapper.readValue(contentJson, FinanceRecordService.CreateCommand.class));
            case "create_inventory_adjustment", "inventory_adjustment" -> v2InventoryService.createLedgerEntry(
                objectMapper.readValue(contentJson, V2InventoryDtos.LedgerEntryCreateRequest.class));
            case "create_account_transfer" -> v2AccountTransferService.create(
                objectMapper.readValue(contentJson, V2FinanceDtos.AccountTransferCreateRequest.class));
            case "media_upload" -> null;
            case "image_generate" -> agentImageService.generate(AgentDraftImageResultCodec.readRequest(objectMapper, contentJson));
            default -> throw new BusinessException("不支持的草稿类型：" + draftType);
        };
    }

    private void recordFailure(AgentDraftEntity entity, Long ownerUserId, String reason) {
        String safeReason = safeFailure(reason);
        entity.setStatus(STATUS_ACTIVE);
        entity.setFailureReason(safeReason);
        entity.setUpdatedAt(System.currentTimeMillis());
        if (confirmationStateService != null) {
            try {
                confirmationStateService.recordFailure(entity.getId(), ownerUserId, safeReason);
            } catch (RuntimeException ignored) {
                // Preserve the original confirmation failure when the evidence sink is unavailable.
            }
        }
    }

    private String businessReference(String draftType, Object created) {
        if (created == null) return null;
        try {
            JsonNode id = objectMapper.valueToTree(created).path("id");
            if (id.isMissingNode() || id.isNull()) return null;
            String value = id.isTextual() ? id.asText() : id.toString();
            if (value.isBlank()) return null;
            return draftType + ":" + (value.length() <= 120 ? value : value.substring(0, 120));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String safeFailure(String reason) {
        if (reason == null || reason.isBlank()) return "草稿确认失败";
        String normalized = reason.replaceAll("[\\r\\n\\t]+", " ").trim();
        normalized = normalized.replaceAll(
            "(?i)(api[_-]?key|token|secret|password|authorization|bearer)(\\s*[:=]\\s*)[^\\s,;]+",
            "$1$2***"
        );
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }

    private V2AgentDtos.AgentDraftResponse toDraftResponse(AgentDraftEntity entity) {
        return new V2AgentDtos.AgentDraftResponse(
            entity.getId(),
            entity.getConversationId(),
            entity.getDraftType(),
            entity.getTitle(),
            entity.getContentJson(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            AgentDraftImageResultCodec.readPersistedResult(objectMapper, entity)
        );
    }
}
