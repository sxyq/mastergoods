package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.InventoryLedgerEntity;
import com.zhihuiji.backend.domain.entity.InventoryMonthlyStatsEntity;
import com.zhihuiji.backend.domain.entity.InventorySnapshotEntity;
import com.zhihuiji.backend.domain.entity.PartnerContactEntity;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.ProductCategoryEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import com.zhihuiji.backend.domain.entity.ProductSupplierRelationEntity;
import com.zhihuiji.backend.domain.entity.ProductUnitEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptItemEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnItemEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.AccountTransferRepository;
import com.zhihuiji.backend.infrastructure.repository.BillFundLinkRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryLedgerRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryMonthlyStatsRepository;
import com.zhihuiji.backend.infrastructure.repository.InventorySnapshotRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerContactRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerGroupRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductCategoryRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductPriceLevelRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductSupplierRelationRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductUnitRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncCursorRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2SyncService {
    private static final int DEFAULT_PULL_LIMIT = 100;
    private static final int MAX_PULL_LIMIT = 500;
    private static final List<String> SUPPORTED_ENTITY_TYPES = List.of(
        "product_category",
        "product_unit",
        "product_price_level",
        "product_supplier_relation",
        "customer_group",
        "supplier_group",
        "customer_contact",
        "supplier_contact",
        "customer",
        "supplier",
        "product",
        "sale_order",
        "sale_order_item",
        "payment",
        "purchase_order",
        "purchase_order_item",
        "pay_order",
        "finance_record",
        "account",
        "account_transfer",
        "bill_fund_link",
        "inventory_adjustment",
        "inventory_ledger",
        "inventory_snapshot",
        "inventory_monthly_stats",
        "sales_return",
        "sales_return_item",
        "purchase_receipt",
        "purchase_receipt_item"
    );
    private static final List<String> UPLOADABLE_ENTITY_TYPES = List.of(
        "product_category",
        "product_unit",
        "product_price_level",
        "product_supplier_relation",
        "customer_group",
        "supplier_group",
        "customer_contact",
        "supplier_contact",
        "customer",
        "supplier",
        "product",
        "sale_order",
        "sale_order_item",
        "payment",
        "purchase_order",
        "purchase_order_item",
        "pay_order",
        "finance_record",
        "account",
        "account_transfer",
        "bill_fund_link",
        "inventory_adjustment",
        "sales_return",
        "sales_return_item",
        "purchase_receipt",
        "purchase_receipt_item"
    );

    private final SyncCursorRepository syncCursorRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductUnitRepository productUnitRepository;
    private final ProductPriceLevelRepository productPriceLevelRepository;
    private final ProductSupplierRelationRepository productSupplierRelationRepository;
    private final PartnerGroupRepository partnerGroupRepository;
    private final PartnerContactRepository partnerContactRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PayOrderRepository payOrderRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final AccountRepository accountRepository;
    private final AccountTransferRepository accountTransferRepository;
    private final BillFundLinkRepository billFundLinkRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final InventoryMonthlyStatsRepository inventoryMonthlyStatsRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReceiptItemRepository purchaseReceiptItemRepository;
    private final ObjectMapper objectMapper;
    private final CurrentOwnerService currentOwnerService;

    public V2SyncService(
        SyncCursorRepository syncCursorRepository,
        ProductCategoryRepository productCategoryRepository,
        ProductUnitRepository productUnitRepository,
        ProductPriceLevelRepository productPriceLevelRepository,
        ProductSupplierRelationRepository productSupplierRelationRepository,
        PartnerGroupRepository partnerGroupRepository,
        PartnerContactRepository partnerContactRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        PaymentRepository paymentRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        PurchaseOrderItemRepository purchaseOrderItemRepository,
        PayOrderRepository payOrderRepository,
        FinanceRecordRepository financeRecordRepository,
        AccountRepository accountRepository,
        AccountTransferRepository accountTransferRepository,
        BillFundLinkRepository billFundLinkRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository,
        InventoryLedgerRepository inventoryLedgerRepository,
        InventorySnapshotRepository inventorySnapshotRepository,
        InventoryMonthlyStatsRepository inventoryMonthlyStatsRepository,
        SalesReturnRepository salesReturnRepository,
        SalesReturnItemRepository salesReturnItemRepository,
        PurchaseReceiptRepository purchaseReceiptRepository,
        PurchaseReceiptItemRepository purchaseReceiptItemRepository,
        ObjectMapper objectMapper,
        CurrentOwnerService currentOwnerService
    ) {
        this.syncCursorRepository = syncCursorRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productUnitRepository = productUnitRepository;
        this.productPriceLevelRepository = productPriceLevelRepository;
        this.productSupplierRelationRepository = productSupplierRelationRepository;
        this.partnerGroupRepository = partnerGroupRepository;
        this.partnerContactRepository = partnerContactRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.payOrderRepository = payOrderRepository;
        this.financeRecordRepository = financeRecordRepository;
        this.accountRepository = accountRepository;
        this.accountTransferRepository = accountTransferRepository;
        this.billFundLinkRepository = billFundLinkRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.inventoryMonthlyStatsRepository = inventoryMonthlyStatsRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.salesReturnItemRepository = salesReturnItemRepository;
        this.purchaseReceiptRepository = purchaseReceiptRepository;
        this.purchaseReceiptItemRepository = purchaseReceiptItemRepository;
        this.objectMapper = objectMapper;
        this.currentOwnerService = currentOwnerService;
    }

    public HealthResult health() {
        return new HealthResult(
            "ok",
            "owner scoped sync ready",
            true,
            System.currentTimeMillis(),
            SUPPORTED_ENTITY_TYPES,
            UPLOADABLE_ENTITY_TYPES
        );
    }

    @Transactional(readOnly = true)
    public CursorStatus cursorStatus(String clientId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedClientId = normalizeClientId(clientId);
        Optional<SyncCursorEntity> cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId);
        return new CursorStatus(
            normalizedClientId,
            cursor.map(SyncCursorEntity::getLastCursor).orElse(CursorToken.initial().encode()),
            cursor.map(SyncCursorEntity::getUpdatedAt).orElse(null)
        );
    }

    @Transactional
    public CursorStatus acknowledgeCursor(String clientId, String cursorValue) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        SyncCursorEntity cursor = loadOrCreateCursor(ownerUserId, clientId);
        cursor.setLastCursor(parseCursorToken(cursorValue).encode());
        cursor.setUpdatedAt(now);
        SyncCursorEntity saved = syncCursorRepository.save(cursor);
        return new CursorStatus(saved.getClientId(), saved.getLastCursor(), saved.getUpdatedAt());
    }

    @Transactional
    public UploadResult upload(String clientId, List<SyncChange> changes, String lastSyncCursor) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        List<SyncChange> safeChanges = changes == null ? List.of() : changes;
        CursorToken nextCursor = parseCursorToken(lastSyncCursor);
        if (nextCursor.updatedAt() < now) {
            nextCursor = new CursorToken(now, nextCursor.entityType(), nextCursor.entityId());
        }
        int acceptedCount = 0;
        int failedCount = 0;
        for (SyncChange change : safeChanges) {
            try {
                applyUploadedChange(ownerUserId, change);
                acceptedCount++;
                nextCursor = advanceCursor(nextCursor, change);
            } catch (RuntimeException ignored) {
                failedCount++;
            }
        }
        SyncCursorEntity cursor = loadOrCreateCursor(ownerUserId, clientId);
        cursor.setLastCursor(nextCursor.encode());
        cursor.setUpdatedAt(now);
        syncCursorRepository.save(cursor);
        return new UploadResult(
            acceptedCount,
            failedCount,
            failedCount == 0 ? "applied" : "partially_applied",
            cursor.getLastCursor()
        );
    }

    @Transactional(readOnly = true)
    public PullResult pull(String clientId, String sinceCursor, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedClientId = normalizeClientId(clientId);
        SyncCursorEntity cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId)
            .orElse(null);
        CursorToken effectiveCursor = sinceCursor == null || sinceCursor.isBlank()
            ? parseCursorToken(cursor == null ? null : cursor.getLastCursor())
            : parseCursorToken(sinceCursor);
        int safeLimit = normalizeLimit(limit == null ? DEFAULT_PULL_LIMIT : limit);

        List<SyncChange> changes = new ArrayList<>(64);
        changes.addAll(collectProductCategoryChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectProductUnitChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectProductPriceLevelChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectProductSupplierRelationChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectPartnerGroupChanges(ownerUserId, "customer", effectiveCursor));
        changes.addAll(collectPartnerGroupChanges(ownerUserId, "supplier", effectiveCursor));
        changes.addAll(collectPartnerContactChanges(ownerUserId, "customer", effectiveCursor));
        changes.addAll(collectPartnerContactChanges(ownerUserId, "supplier", effectiveCursor));
        changes.addAll(collectCustomerChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectSupplierChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectProductChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectSaleOrderChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectSaleOrderItemChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectPaymentChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectPurchaseOrderChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectPurchaseOrderItemChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectPayOrderChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectFinanceRecordChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectAccountChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectAccountTransferChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectBillFundLinkChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectInventoryAdjustmentChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectInventoryLedgerChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectInventorySnapshotChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectInventoryMonthlyStatsChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectSalesReturnChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectSalesReturnItemChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectPurchaseReceiptChanges(ownerUserId, effectiveCursor));
        changes.addAll(collectPurchaseReceiptItemChanges(ownerUserId, effectiveCursor));

        changes.sort(
            Comparator.comparingLong((SyncChange item) -> safeLong(item.updatedAt()))
                .thenComparing(item -> item.entityType() == null ? "" : item.entityType())
                .thenComparing(item -> item.entityId() == null ? "" : item.entityId())
        );

        boolean hasMore = changes.size() > safeLimit;
        List<SyncChange> page = hasMore ? new ArrayList<>(changes.subList(0, safeLimit)) : new ArrayList<>(changes);
        CursorToken nextCursor = effectiveCursor;
        if (!page.isEmpty()) {
            nextCursor = cursorFor(page.get(page.size() - 1));
        }
        // Pull tokens are read-time pagination state; durable progress advances only after ack.
        return new PullResult(page, effectiveCursor.encode(), nextCursor.encode(), hasMore);
    }

    private SyncCursorEntity loadOrCreateCursor(Long ownerUserId, String clientId) {
        String normalizedClientId = normalizeClientId(clientId);
        SyncCursorEntity cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId)
            .orElseGet(SyncCursorEntity::new);
        cursor.setOwnerUserId(ownerUserId);
        cursor.setClientId(normalizedClientId);
        if (cursor.getLastCursor() == null) {
            cursor.setLastCursor(CursorToken.initial().encode());
        }
        if (cursor.getUpdatedAt() == null) {
            cursor.setUpdatedAt(System.currentTimeMillis());
        }
        return cursor;
    }

    private List<SyncChange> collectProductCategoryChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<ProductCategoryEntity> entities = productCategoryRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((ProductCategoryEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("product_category", entity.getId(), changedAt, since)) c.accept(change("product_category", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "name", entity.getName(),
                "status", entity.getStatus(),
                "sort_order", entity.getSortOrder(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectProductUnitChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<ProductUnitEntity> entities = productUnitRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((ProductUnitEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("product_unit", entity.getId(), changedAt, since)) c.accept(change("product_unit", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "name", entity.getName(),
                "status", entity.getStatus(),
                "sort_order", entity.getSortOrder(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectProductPriceLevelChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<ProductPriceLevelEntity> entities = productPriceLevelRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((ProductPriceLevelEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("product_price_level", entity.getId(), changedAt, since)) c.accept(change("product_price_level", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "code", entity.getCode(),
                "name", entity.getName(),
                "status", entity.getStatus(),
                "sort_order", entity.getSortOrder(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectProductSupplierRelationChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<ProductSupplierRelationEntity> entities = productSupplierRelationRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((ProductSupplierRelationEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("product_supplier_relation", entity.getId(), changedAt, since)) c.accept(change("product_supplier_relation", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "product_id", entity.getProductId(),
                "supplier_id", entity.getSupplierId(),
                "is_default", entity.getIsDefault(),
                "purchase_priority", entity.getPurchasePriority(),
                "last_purchase_price", entity.getLastPurchasePrice(),
                "notes", entity.getNotes(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectPartnerGroupChanges(Long ownerUserId, String partnerType, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        String entityType = "customer".equals(partnerType) ? "customer_group" : "supplier_group";
        List<PartnerGroupEntity> entities = partnerGroupRepository.findChangedByOwnerUserIdAndPartnerType(ownerUserId, partnerType, sinceTimestamp);
        return entities.stream()
            .mapMulti((PartnerGroupEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip(entityType, entity.getId(), changedAt, since)) c.accept(change(entityType, entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "partner_type", entity.getPartnerType(),
                "name", entity.getName(),
                "status", entity.getStatus(),
                "sort_order", entity.getSortOrder(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectPartnerContactChanges(Long ownerUserId, String partnerType, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        String entityType = "customer".equals(partnerType) ? "customer_contact" : "supplier_contact";
        List<PartnerContactEntity> entities = partnerContactRepository.findChangedByOwnerUserIdAndPartnerType(ownerUserId, partnerType, sinceTimestamp);
        return entities.stream()
            .mapMulti((PartnerContactEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip(entityType, entity.getId(), changedAt, since)) c.accept(change(entityType, entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "partner_type", entity.getPartnerType(),
                "partner_id", entity.getPartnerId(),
                "name", entity.getName(),
                "phone", entity.getPhone(),
                "title", entity.getTitle(),
                "is_primary", entity.getIsPrimary(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectCustomerChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<CustomerEntity> entities = customerRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((CustomerEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("customer", entity.getId(), changedAt, since)) c.accept(change("customer", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "name", entity.getName(),
                "phone", entity.getPhone(),
                "level", entity.getLevel(),
                "group_id", entity.getGroupId(),
                "address", entity.getAddress(),
                "notes", entity.getNotes(),
                "contact_name", entity.getContactName(),
                "contact_phone", entity.getContactPhone(),
                "balance", entity.getBalance(),
                "status", entity.getStatus(),
                "sync_status", entity.getSyncStatus(),
                "sync_version", entity.getSyncVersion(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectSupplierChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<SupplierEntity> entities = supplierRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((SupplierEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("supplier", entity.getId(), changedAt, since)) c.accept(change("supplier", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "name", entity.getName(),
                "phone", entity.getPhone(),
                "group_id", entity.getGroupId(),
                "address", entity.getAddress(),
                "notes", entity.getNotes(),
                "contact_name", entity.getContactName(),
                "contact_phone", entity.getContactPhone(),
                "balance", entity.getBalance(),
                "status", entity.getStatus(),
                "sync_status", entity.getSyncStatus(),
                "sync_version", entity.getSyncVersion(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectProductChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<ProductEntity> entities = productRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((ProductEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("product", entity.getId(), changedAt, since)) c.accept(change("product", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "code", entity.getCode(),
                "name", entity.getName(),
                "category", entity.getCategory(),
                "category_id", entity.getCategoryId(),
                "unit", entity.getUnit(),
                "unit_id", entity.getUnitId(),
                "price_level_values_json", entity.getPriceLevelValuesJson(),
                "sale_price", entity.getSalePrice(),
                "purchase_price", entity.getPurchasePrice(),
                "stock", entity.getStock(),
                "safe_stock", entity.getSafeStock(),
                "status", entity.getStatus(),
                "sync_status", entity.getSyncStatus(),
                "sync_version", entity.getSyncVersion(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectSaleOrderChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<SaleOrderEntity> entities = saleOrderRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((SaleOrderEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("sale_order", entity.getId(), changedAt, since)) c.accept(change("sale_order", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "order_no", entity.getOrderNo(),
                "customer_id", entity.getCustomerId(),
                "customer_name", entity.getCustomerName(),
                "subtotal_amount", entity.getSubtotalAmount(),
                "discount_amount", entity.getDiscountAmount(),
                "total_amount", entity.getTotalAmount(),
                "paid_amount", entity.getPaidAmount(),
                "notes", entity.getNotes(),
                "status", entity.getStatus(),
                "sync_status", entity.getSyncStatus(),
                "sync_version", entity.getSyncVersion(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectSaleOrderItemChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<SaleOrderItemEntity> entities = saleOrderItemRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (SaleOrderItemEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("sale_order_item", entity.getId(), changedAt, since)) continue;
            rows.add(change("sale_order_item", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "order_id", entity.getOrderId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "customer_id", entity.getCustomerId(),
                "customer_name", entity.getCustomerName(),
                "quantity", entity.getQuantity(),
                "unit_price", entity.getUnitPrice(),
                "amount", entity.getAmount(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private List<SyncChange> collectPaymentChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<PaymentEntity> entities = paymentRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (PaymentEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("payment", entity.getId(), changedAt, since)) continue;
            rows.add(change("payment", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "order_id", entity.getOrderId(),
                "amount", entity.getAmount(),
                "method", entity.getMethod(),
                "reference_no", entity.getReferenceNo(),
                "type", entity.getType(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private List<SyncChange> collectPurchaseOrderChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<PurchaseOrderEntity> entities = purchaseOrderRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((PurchaseOrderEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("purchase_order", entity.getId(), changedAt, since)) c.accept(change("purchase_order", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "order_no", entity.getOrderNo(),
                "supplier_id", entity.getSupplierId(),
                "supplier_name", entity.getSupplierName(),
                "total_amount", entity.getTotalAmount(),
                "paid_amount", entity.getPaidAmount(),
                "received_amount", entity.getReceivedAmount(),
                "notes", entity.getNotes(),
                "status", entity.getStatus(),
                "sync_status", entity.getSyncStatus(),
                "sync_version", entity.getSyncVersion(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectPurchaseOrderItemChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<PurchaseOrderItemEntity> entities = purchaseOrderItemRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (PurchaseOrderItemEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("purchase_order_item", entity.getId(), changedAt, since)) continue;
            rows.add(change("purchase_order_item", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "order_id", entity.getOrderId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "quantity", entity.getQuantity(),
                "unit_cost", entity.getUnitCost(),
                "amount", entity.getAmount(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private List<SyncChange> collectPayOrderChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<PayOrderEntity> entities = payOrderRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((PayOrderEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("pay_order", entity.getId(), changedAt, since)) c.accept(change("pay_order", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "order_no", entity.getOrderNo(),
                "supplier_id", entity.getSupplierId(),
                "supplier_name", entity.getSupplierName(),
                "amount", entity.getAmount(),
                "method", entity.getMethod(),
                "reference_no", entity.getReferenceNo(),
                "notes", entity.getNotes(),
                "account_id", entity.getAccountId(),
                "status", entity.getStatus(),
                "sync_status", entity.getSyncStatus(),
                "sync_version", entity.getSyncVersion(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectFinanceRecordChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<FinanceRecordEntity> entities = financeRecordRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((FinanceRecordEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("finance_record", entity.getId(), changedAt, since)) c.accept(change("finance_record", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "record_no", entity.getRecordNo(),
                "type", entity.getType(),
                "category", entity.getCategory(),
                "partner_name", entity.getPartnerName(),
                "amount", entity.getAmount(),
                "method", entity.getMethod(),
                "notes", entity.getNotes(),
                "sync_status", entity.getSyncStatus(),
                "sync_version", entity.getSyncVersion(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectAccountChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<AccountEntity> entities = accountRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((AccountEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("account", entity.getId(), changedAt, since)) c.accept(change("account", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "code", entity.getCode(),
                "name", entity.getName(),
                "type", entity.getType(),
                "balance", entity.getBalance(),
                "is_default", entity.getIsDefault(),
                "status", entity.getStatus(),
                "sort_order", entity.getSortOrder(),
                "notes", entity.getNotes(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectAccountTransferChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<AccountTransferEntity> entities = accountTransferRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((AccountTransferEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("account_transfer", entity.getId(), changedAt, since)) c.accept(change("account_transfer", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "transfer_no", entity.getTransferNo(),
                "from_account_id", entity.getFromAccountId(),
                "to_account_id", entity.getToAccountId(),
                "amount", entity.getAmount(),
                "fee", entity.getFee(),
                "status", entity.getStatus(),
                "notes", entity.getNotes(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectBillFundLinkChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<BillFundLinkEntity> entities = billFundLinkRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((BillFundLinkEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("bill_fund_link", entity.getId(), changedAt, since)) c.accept(change("bill_fund_link", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "bill_type", entity.getBillType(),
                "bill_id", entity.getBillId(),
                "account_id", entity.getAccountId(),
                "amount", entity.getAmount(),
                "link_type", entity.getLinkType(),
                "notes", entity.getNotes(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectInventoryAdjustmentChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<InventoryAdjustmentEntity> entities = inventoryAdjustmentRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (InventoryAdjustmentEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("inventory_adjustment", entity.getId(), changedAt, since)) continue;
            rows.add(change("inventory_adjustment", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "quantity", entity.getQuantity(),
                "flow_type", entity.getFlowType(),
                "reason", entity.getReason(),
                "operator_name", entity.getOperatorName(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private List<SyncChange> collectInventoryLedgerChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<InventoryLedgerEntity> entities = inventoryLedgerRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (InventoryLedgerEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("inventory_ledger", entity.getId(), changedAt, since)) continue;
            rows.add(change("inventory_ledger", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "warehouse_id", entity.getWarehouseId(),
                "quantity_before", entity.getQuantityBefore(),
                "quantity_change", entity.getQuantityChange(),
                "quantity_after", entity.getQuantityAfter(),
                "unit_cost", entity.getUnitCost(),
                "source_type", entity.getSourceType(),
                "source_id", entity.getSourceId(),
                "source_no", entity.getSourceNo(),
                "notes", entity.getNotes(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private List<SyncChange> collectInventorySnapshotChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<InventorySnapshotEntity> entities = inventorySnapshotRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (InventorySnapshotEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("inventory_snapshot", entity.getId(), changedAt, since)) continue;
            rows.add(change("inventory_snapshot", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "warehouse_id", entity.getWarehouseId(),
                "quantity", entity.getQuantity(),
                "unit_cost", entity.getUnitCost(),
                "total_value", entity.getTotalValue(),
                "snapshot_date", entity.getSnapshotDate(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private List<SyncChange> collectInventoryMonthlyStatsChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<InventoryMonthlyStatsEntity> entities = inventoryMonthlyStatsRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((InventoryMonthlyStatsEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("inventory_monthly_stats", entity.getId(), changedAt, since)) c.accept(change("inventory_monthly_stats", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "warehouse_id", entity.getWarehouseId(),
                "month", entity.getMonth(),
                "year", entity.getYear(),
                "quantity_in", entity.getQuantityIn(),
                "quantity_out", entity.getQuantityOut(),
                "quantity_adjust", entity.getQuantityAdjust(),
                "quantity_begin", entity.getQuantityBegin(),
                "quantity_end", entity.getQuantityEnd(),
                "total_cost_in", entity.getTotalCostIn(),
                "total_cost_out", entity.getTotalCostOut(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectSalesReturnChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<SalesReturnEntity> entities = salesReturnRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((SalesReturnEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("sales_return", entity.getId(), changedAt, since)) c.accept(change("sales_return", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "return_no", entity.getReturnNo(),
                "original_order_id", entity.getOriginalOrderId(),
                "customer_id", entity.getCustomerId(),
                "customer_name", entity.getCustomerName(),
                "total_amount", entity.getTotalAmount(),
                "refund_amount", entity.getRefundAmount(),
                "status", entity.getStatus(),
                "notes", entity.getNotes(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectSalesReturnItemChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<SalesReturnItemEntity> entities = salesReturnItemRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (SalesReturnItemEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("sales_return_item", entity.getId(), changedAt, since)) continue;
            rows.add(change("sales_return_item", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "return_id", entity.getReturnId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "quantity", entity.getQuantity(),
                "unit_price", entity.getUnitPrice(),
                "amount", entity.getAmount(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private List<SyncChange> collectPurchaseReceiptChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<PurchaseReceiptEntity> entities = purchaseReceiptRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        return entities.stream()
            .mapMulti((PurchaseReceiptEntity entity, Consumer<SyncChange> c) -> {
                long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
                if (!shouldSkip("purchase_receipt", entity.getId(), changedAt, since)) c.accept(change("purchase_receipt", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "receipt_no", entity.getReceiptNo(),
                "purchase_order_id", entity.getPurchaseOrderId(),
                "supplier_id", entity.getSupplierId(),
                "supplier_name", entity.getSupplierName(),
                "total_amount", entity.getTotalAmount(),
                "status", entity.getStatus(),
                "notes", entity.getNotes(),
                "created_at", entity.getCreatedAt(),
                "updated_at", entity.getUpdatedAt()
            )));
            })
            .toList();
    }

    private List<SyncChange> collectPurchaseReceiptItemChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<PurchaseReceiptItemEntity> entities = purchaseReceiptItemRepository.findChangedByOwnerUserId(ownerUserId, sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (PurchaseReceiptItemEntity entity : entities) {
            long changedAt = safeLong(entity.getCreatedAt());
            if (shouldSkip("purchase_receipt_item", entity.getId(), changedAt, since)) continue;
            rows.add(change("purchase_receipt_item", entity.getId(), changedAt, payload(
                "id", entity.getId(),
                "receipt_id", entity.getReceiptId(),
                "product_id", entity.getProductId(),
                "product_code", entity.getProductCode(),
                "product_name", entity.getProductName(),
                "quantity", entity.getQuantity(),
                "unit_cost", entity.getUnitCost(),
                "amount", entity.getAmount(),
                "created_at", entity.getCreatedAt()
            )));
        }
        return rows;
    }

    private void applyUploadedChange(Long ownerUserId, SyncChange change) {
        if (change == null || change.entityType() == null || change.entityId() == null || change.operation() == null) {
            throw new IllegalArgumentException("sync change is incomplete");
        }
        switch (change.entityType()) {
            case "product_category" -> applyProductCategoryUpload(ownerUserId, change);
            case "product_unit" -> applyProductUnitUpload(ownerUserId, change);
            case "product_price_level" -> applyProductPriceLevelUpload(ownerUserId, change);
            case "product_supplier_relation" -> applyProductSupplierRelationUpload(ownerUserId, change);
            case "customer_group" -> applyPartnerGroupUpload(ownerUserId, "customer", change);
            case "supplier_group" -> applyPartnerGroupUpload(ownerUserId, "supplier", change);
            case "customer_contact" -> applyPartnerContactUpload(ownerUserId, "customer", change);
            case "supplier_contact" -> applyPartnerContactUpload(ownerUserId, "supplier", change);
            case "customer" -> applyCustomerUpload(ownerUserId, change);
            case "supplier" -> applySupplierUpload(ownerUserId, change);
            case "product" -> applyProductUpload(ownerUserId, change);
            case "sale_order" -> applySaleOrderUpload(ownerUserId, change);
            case "sale_order_item" -> applySaleOrderItemUpload(ownerUserId, change);
            case "payment" -> applyPaymentUpload(ownerUserId, change);
            case "purchase_order" -> applyPurchaseOrderUpload(ownerUserId, change);
            case "purchase_order_item" -> applyPurchaseOrderItemUpload(ownerUserId, change);
            case "pay_order" -> applyPayOrderUpload(ownerUserId, change);
            case "finance_record" -> applyFinanceRecordUpload(ownerUserId, change);
            case "account" -> applyAccountUpload(ownerUserId, change);
            case "account_transfer" -> applyAccountTransferUpload(ownerUserId, change);
            case "bill_fund_link" -> applyBillFundLinkUpload(ownerUserId, change);
            case "inventory_adjustment" -> applyInventoryAdjustmentUpload(ownerUserId, change);
            case "sales_return" -> applySalesReturnUpload(ownerUserId, change);
            case "sales_return_item" -> applySalesReturnItemUpload(ownerUserId, change);
            case "purchase_receipt" -> applyPurchaseReceiptUpload(ownerUserId, change);
            case "purchase_receipt_item" -> applyPurchaseReceiptItemUpload(ownerUserId, change);
            default -> throw new IllegalArgumentException("unsupported entity type: " + change.entityType());
        }
    }

    private void applyProductCategoryUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            productCategoryRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(productCategoryRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        ProductCategoryEntity entity = productCategoryRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(ProductCategoryEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setName(readText(payload, "name", entity.getName(), "默认分类"));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSortOrder(readInt(payload, "sort_order", entity.getSortOrder(), 0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        productCategoryRepository.save(entity);
    }

    private void applyProductUnitUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            productUnitRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(productUnitRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        ProductUnitEntity entity = productUnitRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(ProductUnitEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setName(readText(payload, "name", entity.getName(), "件"));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSortOrder(readInt(payload, "sort_order", entity.getSortOrder(), 0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        productUnitRepository.save(entity);
    }

    private void applyProductPriceLevelUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            productPriceLevelRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(productPriceLevelRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        ProductPriceLevelEntity entity = productPriceLevelRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(ProductPriceLevelEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setCode(readText(payload, "code", entity.getCode(), "DEFAULT"));
        entity.setName(readText(payload, "name", entity.getName(), "默认价格"));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSortOrder(readInt(payload, "sort_order", entity.getSortOrder(), 0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        productPriceLevelRepository.save(entity);
    }

    private void applyProductSupplierRelationUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            productSupplierRelationRepository.deleteByIdAndOwnerUserId(id, ownerUserId);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        ProductSupplierRelationEntity entity = productSupplierRelationRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(ProductSupplierRelationEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setProductId(readRequiredLong(payload, "product_id", "商品不能为空"));
        entity.setSupplierId(readRequiredLong(payload, "supplier_id", "供应商不能为空"));
        entity.setIsDefault(readBoolean(payload, "is_default", entity.getIsDefault(), false));
        entity.setPurchasePriority(readInt(payload, "purchase_priority", entity.getPurchasePriority(), 0));
        entity.setLastPurchasePrice(readNullableDouble(payload, "last_purchase_price", entity.getLastPurchasePrice()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        productSupplierRelationRepository.save(entity);
    }

    private void applyPartnerGroupUpload(Long ownerUserId, String partnerType, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            partnerGroupRepository.findByIdAndOwnerUserIdAndPartnerType(id, ownerUserId, partnerType).ifPresent(partnerGroupRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PartnerGroupEntity entity = partnerGroupRepository.findByIdAndOwnerUserIdAndPartnerType(id, ownerUserId, partnerType).orElseGet(PartnerGroupEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setPartnerType(partnerType);
        entity.setName(readText(payload, "name", entity.getName(), "默认分组"));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSortOrder(readInt(payload, "sort_order", entity.getSortOrder(), 0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        partnerGroupRepository.save(entity);
    }

    private void applyPartnerContactUpload(Long ownerUserId, String partnerType, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            partnerContactRepository.findByIdAndOwnerUserIdAndPartnerType(id, ownerUserId, partnerType).ifPresent(partnerContactRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PartnerContactEntity entity = partnerContactRepository.findByIdAndOwnerUserIdAndPartnerType(id, ownerUserId, partnerType).orElseGet(PartnerContactEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setPartnerType(partnerType);
        entity.setPartnerId(readRequiredLong(payload, "partner_id", "往来单位不能为空"));
        entity.setName(readText(payload, "name", entity.getName(), "默认联系人"));
        entity.setPhone(readNullableText(payload, "phone", entity.getPhone()));
        entity.setTitle(readNullableText(payload, "title", entity.getTitle()));
        entity.setIsPrimary(readBoolean(payload, "is_primary", entity.getIsPrimary(), false));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        partnerContactRepository.save(entity);
    }

    private void applyCustomerUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            partnerContactRepository.deleteByOwnerUserIdAndPartnerTypeAndPartnerId(ownerUserId, "customer", id);
            customerRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(customerRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        CustomerEntity entity = customerRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(CustomerEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setName(readText(payload, "name", entity.getName(), "未命名客户"));
        entity.setPhone(readText(payload, "phone", entity.getPhone(), "unknown-" + id));
        entity.setLevel(readInt(payload, "level", entity.getLevel(), 0));
        entity.setGroupId(readNullableLong(payload, "group_id", entity.getGroupId()));
        entity.setAddress(readNullableText(payload, "address", entity.getAddress()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setContactName(readNullableText(payload, "contact_name", entity.getContactName()));
        entity.setContactPhone(readNullableText(payload, "contact_phone", entity.getContactPhone()));
        entity.setBalance(readDouble(payload, "balance", entity.getBalance(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        customerRepository.save(entity);
    }

    private void applySupplierUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            partnerContactRepository.deleteByOwnerUserIdAndPartnerTypeAndPartnerId(ownerUserId, "supplier", id);
            supplierRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(supplierRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        SupplierEntity entity = supplierRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(SupplierEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setName(readText(payload, "name", entity.getName(), "未命名供应商"));
        entity.setPhone(readText(payload, "phone", entity.getPhone(), "unknown-" + id));
        entity.setGroupId(readNullableLong(payload, "group_id", entity.getGroupId()));
        entity.setAddress(readNullableText(payload, "address", entity.getAddress()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setContactName(readNullableText(payload, "contact_name", entity.getContactName()));
        entity.setContactPhone(readNullableText(payload, "contact_phone", entity.getContactPhone()));
        entity.setBalance(readDouble(payload, "balance", entity.getBalance(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        supplierRepository.save(entity);
    }

    private void applyProductUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            productSupplierRelationRepository.deleteAllByOwnerUserIdAndProductId(ownerUserId, id);
            productRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(productRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        ProductEntity entity = productRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(ProductEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setCode(readText(payload, "code", entity.getCode(), "P-" + id));
        entity.setName(readText(payload, "name", entity.getName(), "未命名商品"));
        entity.setCategory(readText(payload, "category", entity.getCategory(), "默认分类"));
        entity.setCategoryId(readNullableLong(payload, "category_id", entity.getCategoryId()));
        entity.setUnit(readText(payload, "unit", entity.getUnit(), "件"));
        entity.setUnitId(readNullableLong(payload, "unit_id", entity.getUnitId()));
        entity.setPriceLevelValuesJson(readNullableText(payload, "price_level_values_json", entity.getPriceLevelValuesJson()));
        entity.setSalePrice(readDouble(payload, "sale_price", entity.getSalePrice(), 0.0));
        entity.setPurchasePrice(readDouble(payload, "purchase_price", entity.getPurchasePrice(), 0.0));
        entity.setStock(readDouble(payload, "stock", entity.getStock(), 0.0));
        entity.setSafeStock(readDouble(payload, "safe_stock", entity.getSafeStock(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        productRepository.save(entity);
    }

    private void applySaleOrderUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            saleOrderItemRepository.deleteByOwnerUserIdAndOrderId(ownerUserId, id);
            paymentRepository.deleteByOwnerUserIdAndOrderId(ownerUserId, id);
            saleOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(saleOrderRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        SaleOrderEntity entity = saleOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(SaleOrderEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo(readText(payload, "order_no", entity.getOrderNo(), "SO-" + id));
        entity.setCustomerId(readNullableLong(payload, "customer_id", entity.getCustomerId()));
        entity.setCustomerName(readNullableText(payload, "customer_name", entity.getCustomerName()));
        entity.setSubtotalAmount(readDouble(payload, "subtotal_amount", entity.getSubtotalAmount(), 0.0));
        entity.setDiscountAmount(readDouble(payload, "discount_amount", entity.getDiscountAmount(), 0.0));
        entity.setTotalAmount(readDouble(payload, "total_amount", entity.getTotalAmount(), 0.0));
        entity.setPaidAmount(readDouble(payload, "paid_amount", entity.getPaidAmount(), 0.0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        saleOrderRepository.save(entity);
    }

    private void applySaleOrderItemUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            saleOrderItemRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(saleOrderItemRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        SaleOrderItemEntity entity = saleOrderItemRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(SaleOrderItemEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderId(readRequiredLong(payload, "order_id", "销售单不能为空"));
        entity.setProductId(readRequiredLong(payload, "product_id", "商品不能为空"));
        entity.setProductCode(readText(payload, "product_code", entity.getProductCode(), ""));
        entity.setProductName(readText(payload, "product_name", entity.getProductName(), "未命名商品"));
        entity.setCustomerId(readNullableLong(payload, "customer_id", entity.getCustomerId()));
        entity.setCustomerName(readNullableText(payload, "customer_name", entity.getCustomerName()));
        entity.setQuantity(readDouble(payload, "quantity", entity.getQuantity(), 0.0));
        entity.setUnitPrice(readDouble(payload, "unit_price", entity.getUnitPrice(), 0.0));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        saleOrderItemRepository.save(entity);
    }

    private void applyPaymentUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            paymentRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(paymentRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PaymentEntity entity = paymentRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(PaymentEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderId(readRequiredLong(payload, "order_id", "关联单据不能为空"));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setMethod(readInt(payload, "method", entity.getMethod(), 0));
        entity.setReferenceNo(readNullableText(payload, "reference_no", entity.getReferenceNo()));
        entity.setType(readInt(payload, "type", entity.getType(), 0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        paymentRepository.save(entity);
    }

    private void applyPurchaseOrderUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            purchaseOrderItemRepository.deleteByOwnerUserIdAndOrderId(ownerUserId, id);
            purchaseOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(purchaseOrderRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PurchaseOrderEntity entity = purchaseOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(PurchaseOrderEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo(readText(payload, "order_no", entity.getOrderNo(), "PO-" + id));
        entity.setSupplierId(readNullableLong(payload, "supplier_id", entity.getSupplierId()));
        entity.setSupplierName(readText(payload, "supplier_name", entity.getSupplierName(), "未命名供应商"));
        entity.setTotalAmount(readDouble(payload, "total_amount", entity.getTotalAmount(), 0.0));
        entity.setPaidAmount(readDouble(payload, "paid_amount", entity.getPaidAmount(), 0.0));
        entity.setReceivedAmount(readDouble(payload, "received_amount", entity.getReceivedAmount(), 0.0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        purchaseOrderRepository.save(entity);
    }

    private void applyPurchaseOrderItemUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            purchaseOrderItemRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(purchaseOrderItemRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PurchaseOrderItemEntity entity = purchaseOrderItemRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(PurchaseOrderItemEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderId(readRequiredLong(payload, "order_id", "采购单不能为空"));
        entity.setProductId(readRequiredLong(payload, "product_id", "商品不能为空"));
        entity.setProductCode(readText(payload, "product_code", entity.getProductCode(), ""));
        entity.setProductName(readText(payload, "product_name", entity.getProductName(), "未命名商品"));
        entity.setQuantity(readDouble(payload, "quantity", entity.getQuantity(), 0.0));
        entity.setUnitCost(readDouble(payload, "unit_cost", entity.getUnitCost(), 0.0));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        purchaseOrderItemRepository.save(entity);
    }

    private void applyPayOrderUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            payOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(payOrderRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PayOrderEntity entity = payOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(PayOrderEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo(readText(payload, "order_no", entity.getOrderNo(), "PAY-" + id));
        entity.setSupplierId(readNullableLong(payload, "supplier_id", entity.getSupplierId()));
        entity.setSupplierName(readText(payload, "supplier_name", entity.getSupplierName(), "未命名供应商"));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setMethod(readInt(payload, "method", entity.getMethod(), 0));
        entity.setReferenceNo(readNullableText(payload, "reference_no", entity.getReferenceNo()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setAccountId(readNullableLong(payload, "account_id", entity.getAccountId()));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        payOrderRepository.save(entity);
    }

    private void applyFinanceRecordUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            financeRecordRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(financeRecordRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        FinanceRecordEntity entity = financeRecordRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(FinanceRecordEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setRecordNo(readText(payload, "record_no", entity.getRecordNo(), "FR-" + id));
        entity.setType(readInt(payload, "type", entity.getType(), 0));
        entity.setCategory(readText(payload, "category", entity.getCategory(), "未分类"));
        entity.setPartnerName(readNullableText(payload, "partner_name", entity.getPartnerName()));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setMethod(readInt(payload, "method", entity.getMethod(), 0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        financeRecordRepository.save(entity);
    }

    private void applyAccountUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            accountRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(accountRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        AccountEntity entity = accountRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(AccountEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setCode(readText(payload, "code", entity.getCode(), "ACC-" + id));
        entity.setName(readText(payload, "name", entity.getName(), "默认账户"));
        entity.setType(readInt(payload, "type", entity.getType(), 1));
        entity.setBalance(readDouble(payload, "balance", entity.getBalance(), 0.0));
        entity.setIsDefault(readBoolean(payload, "is_default", entity.getIsDefault(), false));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSortOrder(readInt(payload, "sort_order", entity.getSortOrder(), 0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        accountRepository.save(entity);
    }

    private void applyAccountTransferUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            accountTransferRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(accountTransferRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        AccountTransferEntity entity = accountTransferRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(AccountTransferEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setTransferNo(readText(payload, "transfer_no", entity.getTransferNo(), "AT-" + id));
        entity.setFromAccountId(readRequiredLong(payload, "from_account_id", "转出账户不能为空"));
        entity.setToAccountId(readRequiredLong(payload, "to_account_id", "转入账户不能为空"));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setFee(readDouble(payload, "fee", entity.getFee(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        accountTransferRepository.save(entity);
    }

    private void applyBillFundLinkUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            billFundLinkRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(billFundLinkRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        BillFundLinkEntity entity = billFundLinkRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(BillFundLinkEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setBillType(readText(payload, "bill_type", entity.getBillType(), "unknown"));
        entity.setBillId(readRequiredLong(payload, "bill_id", "关联单据不能为空"));
        entity.setAccountId(readRequiredLong(payload, "account_id", "账户不能为空"));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setLinkType(readInt(payload, "link_type", entity.getLinkType(), 1));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        billFundLinkRepository.save(entity);
    }

    private void applyInventoryAdjustmentUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            inventoryAdjustmentRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(inventoryAdjustmentRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        InventoryAdjustmentEntity entity = inventoryAdjustmentRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(InventoryAdjustmentEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setProductId(readRequiredLong(payload, "product_id", "商品不能为空"));
        entity.setProductCode(readText(payload, "product_code", entity.getProductCode(), ""));
        entity.setProductName(readText(payload, "product_name", entity.getProductName(), "未命名商品"));
        entity.setQuantity(readDouble(payload, "quantity", entity.getQuantity(), 0.0));
        entity.setFlowType(readInt(payload, "flow_type", entity.getFlowType(), 0));
        entity.setReason(readNullableText(payload, "reason", entity.getReason()));
        entity.setOperatorName(readNullableText(payload, "operator_name", entity.getOperatorName()));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        inventoryAdjustmentRepository.save(entity);
    }

    private void applySalesReturnUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            salesReturnItemRepository.deleteByOwnerUserIdAndReturnId(ownerUserId, id);
            salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(salesReturnRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        SalesReturnEntity entity = salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(SalesReturnEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setReturnNo(readText(payload, "return_no", entity.getReturnNo(), "SR-" + id));
        entity.setOriginalOrderId(readNullableLong(payload, "original_order_id", entity.getOriginalOrderId()));
        entity.setCustomerId(readNullableLong(payload, "customer_id", entity.getCustomerId()));
        entity.setCustomerName(readNullableText(payload, "customer_name", entity.getCustomerName()));
        entity.setTotalAmount(readDouble(payload, "total_amount", entity.getTotalAmount(), 0.0));
        entity.setRefundAmount(readDouble(payload, "refund_amount", entity.getRefundAmount(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        salesReturnRepository.save(entity);
    }

    private void applySalesReturnItemUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            salesReturnItemRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(salesReturnItemRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        SalesReturnItemEntity entity = salesReturnItemRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(SalesReturnItemEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setReturnId(readRequiredLong(payload, "return_id", "退货单不能为空"));
        entity.setProductId(readRequiredLong(payload, "product_id", "商品不能为空"));
        entity.setProductCode(readNullableText(payload, "product_code", entity.getProductCode()));
        entity.setProductName(readNullableText(payload, "product_name", entity.getProductName()));
        entity.setQuantity(readDouble(payload, "quantity", entity.getQuantity(), 0.0));
        entity.setUnitPrice(readDouble(payload, "unit_price", entity.getUnitPrice(), 0.0));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        salesReturnItemRepository.save(entity);
    }

    private void applyPurchaseReceiptUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            purchaseReceiptItemRepository.deleteByOwnerUserIdAndReceiptId(ownerUserId, id);
            purchaseReceiptRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(purchaseReceiptRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PurchaseReceiptEntity entity = purchaseReceiptRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(PurchaseReceiptEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setReceiptNo(readText(payload, "receipt_no", entity.getReceiptNo(), "PR-" + id));
        entity.setPurchaseOrderId(readNullableLong(payload, "purchase_order_id", entity.getPurchaseOrderId()));
        entity.setSupplierId(readNullableLong(payload, "supplier_id", entity.getSupplierId()));
        entity.setSupplierName(readNullableText(payload, "supplier_name", entity.getSupplierName()));
        entity.setTotalAmount(readDouble(payload, "total_amount", entity.getTotalAmount(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        purchaseReceiptRepository.save(entity);
    }

    private void applyPurchaseReceiptItemUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            purchaseReceiptItemRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(purchaseReceiptItemRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PurchaseReceiptItemEntity entity = purchaseReceiptItemRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(PurchaseReceiptItemEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setReceiptId(readRequiredLong(payload, "receipt_id", "收货单不能为空"));
        entity.setProductId(readRequiredLong(payload, "product_id", "商品不能为空"));
        entity.setProductCode(readNullableText(payload, "product_code", entity.getProductCode()));
        entity.setProductName(readNullableText(payload, "product_name", entity.getProductName()));
        entity.setQuantity(readDouble(payload, "quantity", entity.getQuantity(), 0.0));
        entity.setUnitCost(readDouble(payload, "unit_cost", entity.getUnitCost(), 0.0));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        purchaseReceiptItemRepository.save(entity);
    }

    private SyncChange change(String entityType, Long entityId, long updatedAt, String payload) {
        return new SyncChange(entityType, String.valueOf(entityId), "upsert", payload, updatedAt);
    }

    private boolean shouldSkip(String entityType, Long id, long changedAt, CursorToken since) {
        if (id == null) {
            return true;
        }
        return compareCursor(changedAt, entityType, String.valueOf(id), since) <= 0;
    }

    private String payload(Object... entries) {
        Map<String, Object> map = new HashMap<>(Math.max(4, entries.length / 2));
        for (int index = 0; index < entries.length; index += 2) {
            map.put((String) entries[index], entries[index + 1]);
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize sync payload", error);
        }
    }

    private JsonNode readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("invalid sync payload", error);
        }
    }

    private String normalizeClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return "anonymous";
        }
        return clientId.trim();
    }

    private CursorToken parseCursorToken(String cursor) {
        return CursorToken.parse(cursor);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_PULL_LIMIT;
        }
        return Math.min(limit, MAX_PULL_LIMIT);
    }

    private long resolveChangedAt(Long updatedAt, Long createdAt) {
        long updated = safeLong(updatedAt);
        return updated > 0 ? updated : safeLong(createdAt);
    }

    private int compareCursor(long updatedAt, String entityType, String entityId, CursorToken cursor) {
        int timestampOrder = Long.compare(updatedAt, cursor.updatedAt());
        if (timestampOrder != 0) {
            return timestampOrder;
        }
        int entityTypeOrder = safeText(entityType).compareTo(safeText(cursor.entityType()));
        if (entityTypeOrder != 0) {
            return entityTypeOrder;
        }
        return safeText(entityId).compareTo(safeText(cursor.entityId()));
    }

    private CursorToken cursorFor(SyncChange change) {
        return new CursorToken(
            safeLong(change.updatedAt()),
            safeText(change.entityType()),
            safeText(change.entityId())
        );
    }

    private CursorToken advanceCursor(CursorToken current, SyncChange change) {
        CursorToken candidate = cursorFor(change);
        return compareCursor(candidate.updatedAt(), candidate.entityType(), candidate.entityId(), current) > 0
            ? candidate
            : current;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private Long parseEntityId(String entityId) {
        try {
            return Long.parseLong(entityId);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid entity id: " + entityId, error);
        }
    }

    private boolean isDelete(String operation) {
        return "delete".equalsIgnoreCase(operation);
    }

    private String readText(JsonNode payload, String field, String currentValue, String defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            String value = node.asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        return defaultValue;
    }

    private String readNullableText(JsonNode payload, String field, String currentValue) {
        JsonNode node = payload.path(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isMissingNode()) {
            String value = node.asText();
            return value.isBlank() ? null : value;
        }
        return currentValue;
    }

    private Integer readInt(JsonNode payload, String field, Integer currentValue, Integer defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asInt();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Boolean readBoolean(JsonNode payload, String field, Boolean currentValue, Boolean defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asBoolean();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Double readDouble(JsonNode payload, String field, Double currentValue, Double defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asDouble();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Double readNullableDouble(JsonNode payload, String field, Double currentValue) {
        JsonNode node = payload.path(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isMissingNode()) {
            return node.asDouble();
        }
        return currentValue;
    }

    private Long readLong(JsonNode payload, String field, Long currentValue, Long defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asLong();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Long readNullableLong(JsonNode payload, String field, Long currentValue) {
        JsonNode node = payload.path(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isMissingNode()) {
            if (node.isTextual() && node.asText().isBlank()) {
                return null;
            }
            return node.asLong();
        }
        return currentValue;
    }

    private Long readRequiredLong(JsonNode payload, String field, String message) {
        Long value = readNullableLong(payload, field, null);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public record HealthResult(
        String status,
        String message,
        Boolean ownerScoped,
        Long serverTime,
        List<String> supportedEntityTypes,
        List<String> uploadableEntityTypes
    ) {}

    public record SyncChange(String entityType, String entityId, String operation, String payload, Long updatedAt) {}

    public record UploadResult(Integer acceptedCount, Integer failedCount, String status, String nextCursor) {}

    public record PullResult(List<SyncChange> changes, String effectiveCursor, String nextCursor, Boolean hasMore) {}

    public record CursorStatus(String clientId, String lastCursor, Long updatedAt) {}

    record CursorToken(long updatedAt, String entityType, String entityId) {
        static CursorToken initial() {
            return new CursorToken(0L, "", "");
        }

        static CursorToken parse(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return initial();
            }
            int firstSeparator = cursor.indexOf('|');
            if (firstSeparator < 0) {
                return new CursorToken(parseLong(cursor), "", "");
            }
            int secondSeparator = cursor.indexOf('|', firstSeparator + 1);
            if (secondSeparator < 0) {
                return new CursorToken(
                    parseLong(cursor.substring(0, firstSeparator)),
                    cursor.substring(firstSeparator + 1),
                    ""
                );
            }
            return new CursorToken(
                parseLong(cursor.substring(0, firstSeparator)),
                cursor.substring(firstSeparator + 1, secondSeparator),
                cursor.substring(secondSeparator + 1)
            );
        }

        String encode() {
            return updatedAt + "|" + (entityType == null ? "" : entityType) + "|" + (entityId == null ? "" : entityId);
        }

        private static long parseLong(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
    }
}
