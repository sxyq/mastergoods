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
import com.zhihuiji.backend.domain.entity.product.ProductCategoryEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.product.ProductPriceLevelEntity;
import com.zhihuiji.backend.domain.entity.product.ProductSupplierRelationEntity;
import com.zhihuiji.backend.domain.entity.product.ProductUnitEntity;
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
import com.zhihuiji.backend.domain.entity.SyncChangeLogEntity;
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
import com.zhihuiji.backend.infrastructure.repository.product.ProductCategoryRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductPriceLevelRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductSupplierRelationRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductUnitRepository;
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
import com.zhihuiji.backend.infrastructure.repository.SyncChangeLogRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncOperationLogRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncTombstoneRepository;
import com.zhihuiji.backend.domain.entity.SyncTombstoneEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
        "product"
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
    private final SyncOperationLogRepository syncOperationLogRepository;
    private final SyncTombstoneRepository syncTombstoneRepository;
    private final SyncChangeLogRepository syncChangeLogRepository;
    private final TransactionTemplate operationTransactionTemplate;

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
        CurrentOwnerService currentOwnerService,
        SyncOperationLogRepository syncOperationLogRepository,
        SyncTombstoneRepository syncTombstoneRepository,
        SyncChangeLogRepository syncChangeLogRepository,
        PlatformTransactionManager transactionManager
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
        this.syncOperationLogRepository = syncOperationLogRepository;
        this.syncTombstoneRepository = syncTombstoneRepository;
        this.syncChangeLogRepository = syncChangeLogRepository;
        this.operationTransactionTemplate = new TransactionTemplate(transactionManager);
        this.operationTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
        cursor.setLastCursor(normalizeCursor(cursorValue));
        cursor.setUpdatedAt(now);
        SyncCursorEntity saved = syncCursorRepository.save(cursor);
        return new CursorStatus(saved.getClientId(), saved.getLastCursor(), saved.getUpdatedAt());
    }

    @Transactional
    public UploadResult upload(String clientId, List<SyncChange> changes, String lastSyncCursor) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Long storeId = currentOwnerService.requireCurrentStoreId();
        long now = System.currentTimeMillis();
        List<SyncChange> safeChanges = changes == null ? List.of() : changes;
        CursorToken nextCursor = parseCursorToken(lastSyncCursor);
        if (nextCursor.updatedAt() < now) {
            nextCursor = new CursorToken(now, nextCursor.entityType(), nextCursor.entityId());
        }
        int acceptedCount = 0;
        int failedCount = 0;
        List<String> acceptedOperationIds = new ArrayList<>();
        List<String> failedOperationIds = new ArrayList<>();
        List<SyncOperationFailure> failures = new ArrayList<>();
        List<SyncOperationResult> operationResults = new ArrayList<>();
        for (SyncChange change : safeChanges) {
            try {
                boolean[] duplicateOperation = {false};
                operationTransactionTemplate.executeWithoutResult(status -> {
                    validateChange(change);
                    requireUploadableEntityType(change.entityType());
                    currentOwnerService.requirePermissions(syncPermissions(change.entityType(), true));
                    validateBaseVersion(ownerUserId, change);
                    if (hasOperationId(change)) {
                        // The composite primary key and ON CONFLICT clause make this
                        // reservation the single atomic idempotency check. Avoid a
                        // read-then-insert pair: it added one database round trip per
                        // operation and still had a race between the two statements.
                        int reserved = syncOperationLogRepository.reserveOperation(
                            ownerUserId,
                            storeId,
                            change.operationId(),
                            change.entityType(),
                            change.entityId(),
                            change.operation(),
                            now
                        );
                        if (reserved == 0) {
                            duplicateOperation[0] = true;
                            return;
                        }
                    }
                    applyUploadedChange(ownerUserId, storeId, change, now);
                });
                if (hasOperationId(change)) {
                    acceptedOperationIds.add(change.operationId());
                }
                acceptedCount++;
                operationResults.add(new SyncOperationResult(
                    operationId(change),
                    duplicateOperation[0] ? "duplicate" : "applied",
                    null,
                    null
                ));
                if (!duplicateOperation[0]) {
                    nextCursor = advanceCursor(nextCursor, change);
                }
            } catch (RuntimeException exception) {
                failedCount++;
                String operationId = change == null ? null : change.operationId();
                if (operationId != null && !operationId.isBlank()) {
                    failedOperationIds.add(change.operationId());
                }
                String failureCode = syncFailureCode(exception);
                failures.add(new SyncOperationFailure(
                    operationId,
                    failureCode,
                    safeFailureMessage(exception)
                ));
                SyncConflictException conflict = exception instanceof SyncConflictException value ? value : null;
                operationResults.add(new SyncOperationResult(
                    operationId,
                    "version_conflict".equals(failureCode) ? "conflict" : "rejected",
                    failureCode,
                    safeFailureMessage(exception),
                    conflict == null ? null : conflict.serverVersion(),
                    conflict == null ? List.of() : conflict.conflictFields(),
                    conflict == null ? null : conflict.serverPayload()
                ));
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
            cursor.getLastCursor(),
            acceptedOperationIds,
            failedOperationIds,
            failures,
            operationResults
        );
    }

    @Transactional(readOnly = true)
    public PullResult pull(String clientId, String sinceCursor, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Long storeId = currentOwnerService.requireCurrentStoreId();
        String normalizedClientId = normalizeClientId(clientId);
        SyncCursorEntity cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId)
            .orElse(null);
        String requestedCursor = sinceCursor == null || sinceCursor.isBlank()
            ? cursor == null ? null : cursor.getLastCursor()
            : sinceCursor;
        CursorToken effectiveCursor = parseCursorToken(requestedCursor);
        int safeLimit = normalizeLimit(limit == null ? DEFAULT_PULL_LIMIT : limit);

        String persistedCursor = normalizeCursor(requestedCursor);
        PersistedChanges persistedChanges = collectPersistedChanges(
            ownerUserId, storeId, persistedCursor, safeLimit
        );
        if (!persistedChanges.changes().isEmpty() || isSequenceCursor(persistedCursor)) {
            return pagePersistedChanges(persistedChanges, persistedCursor, safeLimit);
        }

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

        // Collect tombstones (deleted entities) so clients can learn about deletions
        changes.addAll(collectTombstoneChanges(ownerUserId, effectiveCursor));

        changes.sort(
            Comparator.comparingLong((SyncChange item) -> safeLong(item.updatedAt()))
                .thenComparing(item -> item.entityType() == null ? "" : item.entityType())
                .thenComparing(item -> item.entityId() == null ? "" : item.entityId())
        );
        changes.removeIf(change -> !canPullEntityType(change.entityType()));

        boolean hasMore = changes.size() > safeLimit;
        List<SyncChange> page = hasMore ? new ArrayList<>(changes.subList(0, safeLimit)) : new ArrayList<>(changes);
        CursorToken nextCursor = effectiveCursor;
        if (!page.isEmpty()) {
            nextCursor = cursorFor(page.get(page.size() - 1));
        }
        // Pull tokens are read-time pagination state; durable progress advances only after ack.
        return new PullResult(page, effectiveCursor.encode(), nextCursor.encode(), hasMore);
    }

    private PersistedChanges collectPersistedChanges(
        Long ownerUserId,
        Long storeId,
        String sinceCursor,
        int limit
    ) {
        boolean sequenceCursor = isSequenceCursor(sinceCursor);
        long cursor = sequenceCursor
            ? parseSequenceCursor(sinceCursor)
            : parseCursorToken(sinceCursor).updatedAt();
        var pageable = PageRequest.of(0, Math.min(MAX_PULL_LIMIT, limit + 1));
        List<SyncChangeLogEntity> rows = sequenceCursor
            ? syncChangeLogRepository
                .findByOwnerUserIdAndStoreIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                    ownerUserId, storeId, cursor, pageable)
            : syncChangeLogRepository
                .findByOwnerUserIdAndStoreIdAndChangedAtGreaterThanEqualOrderByChangedAtAscSequenceNumberAsc(
                    ownerUserId, storeId, cursor, pageable);
        CursorToken legacyCursor = parseCursorToken(sinceCursor);
        List<SyncChange> changes = new ArrayList<>(rows.size());
        List<Long> sequences = new ArrayList<>(rows.size());
        for (SyncChangeLogEntity row : rows) {
            if (!canPullEntityType(row.getEntityType())
                || (!sequenceCursor && compareCursor(
                    safeLong(row.getChangedAt()), row.getEntityType(), row.getEntityId(), legacyCursor) <= 0)) {
                continue;
            }
            changes.add(new SyncChange(
                row.getOperationId(),
                row.getEntityType(),
                row.getEntityId(),
                row.getOperation(),
                row.getPayload(),
                row.getChangedAt(),
                row.getSyncVersion()
            ));
            sequences.add(row.getSequenceNumber());
        }
        return new PersistedChanges(changes, sequences);
    }

    private PullResult pagePersistedChanges(PersistedChanges persisted, String sinceCursor, int limit) {
        List<SyncChange> changes = persisted.changes();
        boolean hasMore = changes.size() > limit;
        List<SyncChange> page = hasMore
            ? new ArrayList<>(changes.subList(0, limit))
            : new ArrayList<>(changes);
        String effectiveCursor = sinceCursor == null || sinceCursor.isBlank() ? "seq:0" : sinceCursor;
        String nextCursor = effectiveCursor;
        if (!page.isEmpty()) {
            // Keep the public SyncChange timestamp unchanged for existing clients.
            nextCursor = "seq:" + persisted.sequences().get(page.size() - 1);
        }
        return new PullResult(page, effectiveCursor, nextCursor, hasMore);
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

    private List<SyncChange> collectTombstoneChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<SyncTombstoneEntity> tombstones = syncTombstoneRepository.findChangedByOwnerUserId(
            ownerUserId, currentStoreId(ownerUserId), sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(tombstones.size());
        for (SyncTombstoneEntity tombstone : tombstones) {
            long changedAt = safeLong(tombstone.getDeletedAt());
            if (compareCursor(changedAt, tombstone.getEntityType(), tombstone.getEntityId(), since) <= 0) continue;
            rows.add(new SyncChange(tombstone.getEntityType(), tombstone.getEntityId(), "delete", null, changedAt));
        }
        return rows;
    }

    private void validateBaseVersion(Long ownerUserId, SyncChange change) {
        if (change.baseVersion() == null) {
            return;
        }
        Long currentVersion = switch (change.entityType()) {
            case "customer" -> customerRepository.findByIdAndOwnerUserIdForUpdate(parseEntityId(change.entityId()), ownerUserId)
                .map(CustomerEntity::getSyncVersion).orElse(null);
            case "supplier" -> supplierRepository.findByIdAndOwnerUserIdForUpdate(parseEntityId(change.entityId()), ownerUserId)
                .map(SupplierEntity::getSyncVersion).orElse(null);
            case "product" -> productRepository.findByIdForUpdate(ownerUserId, parseEntityId(change.entityId()))
                .map(ProductEntity::getSyncVersion).orElse(null);
            case "sale_order" -> saleOrderRepository.findByIdAndOwnerUserIdForUpdate(parseEntityId(change.entityId()), ownerUserId)
                .map(SaleOrderEntity::getSyncVersion).orElse(null);
            case "purchase_order" -> purchaseOrderRepository.findByIdAndOwnerUserIdForUpdate(parseEntityId(change.entityId()), ownerUserId)
                .map(PurchaseOrderEntity::getSyncVersion).orElse(null);
            case "pay_order" -> payOrderRepository.findByIdAndOwnerUserIdForUpdate(parseEntityId(change.entityId()), ownerUserId)
                .map(PayOrderEntity::getSyncVersion).orElse(null);
            case "finance_record" -> financeRecordRepository.findByIdAndOwnerUserIdForUpdate(parseEntityId(change.entityId()), ownerUserId)
                .map(FinanceRecordEntity::getSyncVersion).orElse(null);
            default -> null;
        };
        long expectedVersion = change.baseVersion();
        if (currentVersion == null && expectedVersion != 0L) {
            throw new SyncConflictException(
                "sync version conflict: entity does not exist",
                null,
                List.of(),
                null
            );
        }
        if (currentVersion != null && currentVersion != expectedVersion) {
            ConflictSnapshot snapshot = conflictSnapshot(ownerUserId, change);
            throw new SyncConflictException(
                "sync version conflict: expected " + expectedVersion + ", current " + currentVersion,
                currentVersion,
                snapshot.conflictFields(),
                snapshot.serverPayload()
            );
        }
    }

    private ConflictSnapshot conflictSnapshot(Long ownerUserId, SyncChange change) {
        JsonNode incoming = readPayload(change.payload());
        JsonNode server = switch (change.entityType()) {
            case "product" -> productRepository.findByIdForUpdate(ownerUserId, parseEntityId(change.entityId()))
                .map(this::productConflictPayload)
                .orElse(null);
            case "customer" -> customerRepository.findByIdAndOwnerUserIdForUpdate(
                    parseEntityId(change.entityId()), ownerUserId)
                .map(this::customerConflictPayload)
                .orElse(null);
            case "supplier" -> supplierRepository.findByIdAndOwnerUserIdForUpdate(
                    parseEntityId(change.entityId()), ownerUserId)
                .map(this::supplierConflictPayload)
                .orElse(null);
            default -> null;
        };
        if (server == null) {
            return new ConflictSnapshot(List.of(), null);
        }
        List<String> fields = new ArrayList<>();
        incoming.fieldNames().forEachRemaining(field -> {
            if (Set.of("id", "created_at", "updated_at", "sync_version", "sync_status").contains(field)) {
                return;
            }
            if (!server.path(field).equals(incoming.path(field))) {
                fields.add(field);
            }
        });
        return new ConflictSnapshot(fields, server.toString());
    }

    private JsonNode productConflictPayload(ProductEntity entity) {
        return readPayload(payload(
            "id", entity.getId(), "code", entity.getCode(), "name", entity.getName(),
            "category", entity.getCategory(), "category_id", entity.getCategoryId(),
            "unit", entity.getUnit(), "unit_id", entity.getUnitId(),
            "sale_price", entity.getSalePrice(), "purchase_price", entity.getPurchasePrice(),
            "stock", entity.getStock(), "safe_stock", entity.getSafeStock(),
            "status", entity.getStatus(), "sync_status", entity.getSyncStatus(),
            "sync_version", entity.getSyncVersion(), "created_at", entity.getCreatedAt(),
            "updated_at", entity.getUpdatedAt()
        ));
    }

    private JsonNode customerConflictPayload(CustomerEntity entity) {
        return readPayload(payload(
            "id", entity.getId(), "name", entity.getName(), "phone", entity.getPhone(),
            "level", entity.getLevel(), "group_id", entity.getGroupId(), "address", entity.getAddress(),
            "notes", entity.getNotes(), "primary_contact_name", entity.getContactName(),
            "primary_contact_phone", entity.getContactPhone(), "balance", entity.getBalance(),
            "status", entity.getStatus(), "sync_status", entity.getSyncStatus(),
            "sync_version", entity.getSyncVersion(), "created_at", entity.getCreatedAt(),
            "updated_at", entity.getUpdatedAt()
        ));
    }

    private JsonNode supplierConflictPayload(SupplierEntity entity) {
        return readPayload(payload(
            "id", entity.getId(), "name", entity.getName(), "phone", entity.getPhone(),
            "group_id", entity.getGroupId(), "address", entity.getAddress(), "notes", entity.getNotes(),
            "primary_contact_name", entity.getContactName(), "primary_contact_phone", entity.getContactPhone(),
            "balance", entity.getBalance(), "status", entity.getStatus(), "sync_status", entity.getSyncStatus(),
            "sync_version", entity.getSyncVersion(), "created_at", entity.getCreatedAt(),
            "updated_at", entity.getUpdatedAt()
        ));
    }

    private boolean canPullEntityType(String entityType) {
        try {
            currentOwnerService.requirePermissions(syncPermissions(entityType, false));
            return true;
        } catch (AccessDeniedException denied) {
            return false;
        }
    }

    private void requireUploadableEntityType(String entityType) {
        if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("unsupported entity type: " + entityType);
        }
        if (!UPLOADABLE_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalStateException("server command required for entity type: " + entityType);
        }
    }

    private String[] syncPermissions(String entityType, boolean write) {
        String archives = write ? "archives:write" : "archives:view";
        String sales = write ? "sales:write" : "sales:view";
        String purchase = write ? "purchase:write" : "purchase:view";
        String inventory = write ? "inventory:write" : "inventory:view";
        String finance = write ? "finance:write" : "finance:view";
        return switch (entityType == null ? "" : entityType) {
            case "product_category", "product_unit", "product_price_level", "product_supplier_relation",
                "customer_group", "supplier_group", "customer_contact", "supplier_contact",
                "customer", "supplier", "product" -> new String[]{archives};
            case "sale_order", "sale_order_item", "sales_return", "sales_return_item" -> new String[]{sales};
            case "payment" -> new String[]{sales, finance};
            case "purchase_order", "purchase_order_item", "purchase_receipt", "purchase_receipt_item" ->
                new String[]{purchase, inventory};
            case "pay_order", "finance_record", "account", "account_transfer", "bill_fund_link" ->
                new String[]{finance};
            case "inventory_adjustment", "inventory_ledger", "inventory_snapshot", "inventory_monthly_stats" ->
                new String[]{inventory};
            default -> new String[0];
        };
    }

    private long nextSyncVersion(Long currentVersion, Long baseVersion) {
        long current = currentVersion == null ? 0L : Math.max(0L, currentVersion);
        long base = baseVersion == null ? current : Math.max(0L, baseVersion);
        return Math.max(current, base) + 1L;
    }

    private void validateChange(SyncChange change) {
        if (change == null
            || change.entityType() == null || change.entityType().isBlank()
            || change.entityId() == null || change.entityId().isBlank()
            || change.operation() == null || change.operation().isBlank()) {
            throw new IllegalArgumentException("sync change is incomplete");
        }
    }

    private boolean hasOperationId(SyncChange change) {
        return change.operationId() != null && !change.operationId().isBlank();
    }

    private String operationId(SyncChange change) {
        return change == null ? null : change.operationId();
    }

    private String syncFailureCode(RuntimeException exception) {
        if (exception instanceof AccessDeniedException) {
            return "permission_denied";
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.startsWith("sync version conflict:")) {
            return "version_conflict";
        }
        if (message.startsWith("unsupported entity type:")) {
            return "unsupported_entity_type";
        }
        if (message.startsWith("server command required for entity type:")) {
            return "server_command_required";
        }
        if (message.contains("required")) {
            return "validation_failed";
        }
        return "sync_apply_failed";
    }

    private String safeFailureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "sync operation failed" : message;
    }

    private void applyUploadedChange(Long ownerUserId, Long storeId, SyncChange change, long changedAt) {
        validateBaseVersion(ownerUserId, change);
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
        // Create tombstone for delete operations so other clients can learn about deletions
        if (isDelete(change.operation())) {
            SyncTombstoneEntity tombstone = new SyncTombstoneEntity();
            tombstone.setOwnerUserId(ownerUserId);
            tombstone.setStoreId(storeId);
            tombstone.setEntityType(change.entityType());
            tombstone.setEntityId(change.entityId());
            tombstone.setDeletedAt(System.currentTimeMillis());
            syncTombstoneRepository.save(tombstone);
        }
        JsonNode uploadedPayload = readPayload(change.payload());
        SyncChangeLogEntity log = new SyncChangeLogEntity();
        log.setOwnerUserId(ownerUserId);
        log.setStoreId(storeId);
        log.setEntityType(change.entityType());
        log.setEntityId(change.entityId());
        log.setOperation(change.operation());
        log.setPayload(change.payload());
        log.setSyncVersion(uploadedPayload.path("sync_version").isNumber()
            ? uploadedPayload.path("sync_version").asLong()
            : null);
        log.setOperationId(change.operationId());
        log.setChangedAt(changedAt);
        syncChangeLogRepository.save(log);
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
        entity.setProductId(requireOwnedProduct(ownerUserId, readRequiredLong(payload, "product_id", "商品不能为空")));
        entity.setSupplierId(requireOwnedSupplier(ownerUserId, readRequiredLong(payload, "supplier_id", "供应商不能为空")));
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
        entity.setPartnerId(requireOwnedPartner(ownerUserId, partnerType,
            readRequiredLong(payload, "partner_id", "往来单位不能为空")));
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
        entity.setGroupId(requireOwnedGroup(ownerUserId, "customer",
            readNullableLong(payload, "group_id", entity.getGroupId())));
        entity.setAddress(readNullableText(payload, "address", entity.getAddress()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setContactName(readNullableText(
            payload,
            "primary_contact_name",
            readNullableText(payload, "contact_name", entity.getContactName())
        ));
        entity.setContactPhone(readNullableText(
            payload,
            "primary_contact_phone",
            readNullableText(payload, "contact_phone", entity.getContactPhone())
        ));
        entity.setBalance(readDouble(payload, "balance", entity.getBalance(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(nextSyncVersion(entity.getSyncVersion(), change.baseVersion()));
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
        entity.setGroupId(requireOwnedGroup(ownerUserId, "supplier",
            readNullableLong(payload, "group_id", entity.getGroupId())));
        entity.setAddress(readNullableText(payload, "address", entity.getAddress()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setContactName(readNullableText(
            payload,
            "primary_contact_name",
            readNullableText(payload, "contact_name", entity.getContactName())
        ));
        entity.setContactPhone(readNullableText(
            payload,
            "primary_contact_phone",
            readNullableText(payload, "contact_phone", entity.getContactPhone())
        ));
        entity.setBalance(readDouble(payload, "balance", entity.getBalance(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(nextSyncVersion(entity.getSyncVersion(), change.baseVersion()));
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
        entity.setCategoryId(requireOwnedCategory(ownerUserId,
            readNullableLong(payload, "category_id", entity.getCategoryId())));
        entity.setUnit(readText(payload, "unit", entity.getUnit(), "件"));
        entity.setUnitId(requireOwnedUnit(ownerUserId,
            readNullableLong(payload, "unit_id", entity.getUnitId())));
        entity.setPriceLevelValuesJson(readNullableText(payload, "price_level_values_json", entity.getPriceLevelValuesJson()));
        entity.setSalePrice(readDouble(payload, "sale_price", entity.getSalePrice(), 0.0));
        entity.setPurchasePrice(readDouble(payload, "purchase_price", entity.getPurchasePrice(), 0.0));
        entity.setStock(readDouble(payload, "stock", entity.getStock(), 0.0));
        entity.setSafeStock(readDouble(payload, "safe_stock", entity.getSafeStock(), 0.0));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 1));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(nextSyncVersion(entity.getSyncVersion(), change.baseVersion()));
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
        entity.setCustomerId(requireOwnedCustomer(ownerUserId,
            readNullableLong(payload, "customer_id", entity.getCustomerId())));
        entity.setCustomerName(readNullableText(payload, "customer_name", entity.getCustomerName()));
        entity.setSubtotalAmount(readDouble(payload, "subtotal_amount", entity.getSubtotalAmount(), 0.0));
        entity.setDiscountAmount(readDouble(payload, "discount_amount", entity.getDiscountAmount(), 0.0));
        entity.setTotalAmount(readDouble(payload, "total_amount", entity.getTotalAmount(), 0.0));
        entity.setPaidAmount(readDouble(payload, "paid_amount", entity.getPaidAmount(), 0.0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(nextSyncVersion(entity.getSyncVersion(), change.baseVersion()));
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
        entity.setOrderId(requireOwnedSaleOrder(ownerUserId,
            readRequiredLong(payload, "order_id", "销售单不能为空")));
        entity.setProductId(requireOwnedProduct(ownerUserId,
            readRequiredLong(payload, "product_id", "商品不能为空")));
        entity.setProductCode(readText(payload, "product_code", entity.getProductCode(), ""));
        entity.setProductName(readText(payload, "product_name", entity.getProductName(), "未命名商品"));
        entity.setCustomerId(requireOwnedCustomer(ownerUserId,
            readNullableLong(payload, "customer_id", entity.getCustomerId())));
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
        entity.setOrderId(requireOwnedSaleOrder(ownerUserId,
            readRequiredLong(payload, "order_id", "关联单据不能为空")));
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
        entity.setSupplierId(requireOwnedSupplier(ownerUserId,
            readNullableLong(payload, "supplier_id", entity.getSupplierId())));
        entity.setSupplierName(readText(payload, "supplier_name", entity.getSupplierName(), "未命名供应商"));
        entity.setTotalAmount(readDouble(payload, "total_amount", entity.getTotalAmount(), 0.0));
        entity.setPaidAmount(readDouble(payload, "paid_amount", entity.getPaidAmount(), 0.0));
        entity.setReceivedAmount(readDouble(payload, "received_amount", entity.getReceivedAmount(), 0.0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(nextSyncVersion(entity.getSyncVersion(), change.baseVersion()));
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
        entity.setOrderId(requireOwnedPurchaseOrder(ownerUserId,
            readRequiredLong(payload, "order_id", "采购单不能为空")));
        entity.setProductId(requireOwnedProduct(ownerUserId,
            readRequiredLong(payload, "product_id", "商品不能为空")));
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
        entity.setSupplierId(requireOwnedSupplier(ownerUserId,
            readNullableLong(payload, "supplier_id", entity.getSupplierId())));
        entity.setSupplierName(readText(payload, "supplier_name", entity.getSupplierName(), "未命名供应商"));
        entity.setAmount(readDouble(payload, "amount", entity.getAmount(), 0.0));
        entity.setMethod(readInt(payload, "method", entity.getMethod(), 0));
        entity.setReferenceNo(readNullableText(payload, "reference_no", entity.getReferenceNo()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setAccountId(requireOwnedAccount(ownerUserId,
            readNullableLong(payload, "account_id", entity.getAccountId())));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(nextSyncVersion(entity.getSyncVersion(), change.baseVersion()));
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
        entity.setSyncVersion(nextSyncVersion(entity.getSyncVersion(), change.baseVersion()));
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
        entity.setFromAccountId(requireOwnedAccount(ownerUserId,
            readRequiredLong(payload, "from_account_id", "转出账户不能为空")));
        entity.setToAccountId(requireOwnedAccount(ownerUserId,
            readRequiredLong(payload, "to_account_id", "转入账户不能为空")));
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
        entity.setAccountId(requireOwnedAccount(ownerUserId,
            readRequiredLong(payload, "account_id", "账户不能为空")));
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
        entity.setProductId(requireOwnedProduct(ownerUserId,
            readRequiredLong(payload, "product_id", "商品不能为空")));
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
        entity.setOriginalOrderId(requireOwnedSaleOrder(ownerUserId,
            readNullableLong(payload, "original_order_id", entity.getOriginalOrderId())));
        entity.setCustomerId(requireOwnedCustomer(ownerUserId,
            readNullableLong(payload, "customer_id", entity.getCustomerId())));
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
        entity.setReturnId(requireOwnedSalesReturn(ownerUserId,
            readRequiredLong(payload, "return_id", "退货单不能为空")));
        entity.setProductId(requireOwnedProduct(ownerUserId,
            readRequiredLong(payload, "product_id", "商品不能为空")));
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
        entity.setPurchaseOrderId(requireOwnedPurchaseOrder(ownerUserId,
            readNullableLong(payload, "purchase_order_id", entity.getPurchaseOrderId())));
        entity.setSupplierId(requireOwnedSupplier(ownerUserId,
            readNullableLong(payload, "supplier_id", entity.getSupplierId())));
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
        entity.setReceiptId(requireOwnedPurchaseReceipt(ownerUserId,
            readRequiredLong(payload, "receipt_id", "收货单不能为空")));
        entity.setProductId(requireOwnedProduct(ownerUserId,
            readRequiredLong(payload, "product_id", "商品不能为空")));
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

    private String normalizeCursor(String cursor) {
        if (isSequenceCursor(cursor)) {
            return "seq:" + parseSequenceCursor(cursor);
        }
        return parseCursorToken(cursor).encode();
    }

    private boolean isSequenceCursor(String cursor) {
        return cursor != null && cursor.startsWith("seq:");
    }

    private long parseSequenceCursor(String cursor) {
        if (!isSequenceCursor(cursor)) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(cursor.substring("seq:".length())));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private Long currentStoreId(Long ownerUserId) {
        return currentOwnerService.findCurrentStoreId().orElseThrow(
            () -> new AccessDeniedException("当前账号没有有效门店上下文"));
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
            if (!node.isNumber()) {
                throw new IllegalArgumentException("同步字段不是数字: " + field);
            }
            double value = node.asDouble();
            if (!Double.isFinite(value) || (isNonNegativeNumericField(field) && value < 0.0)) {
                throw new IllegalArgumentException("同步字段数值不合法: " + field);
            }
            return value;
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
            if (!node.isNumber()) {
                throw new IllegalArgumentException("同步字段不是数字: " + field);
            }
            double value = node.asDouble();
            if (!Double.isFinite(value) || (isNonNegativeNumericField(field) && value < 0.0)) {
                throw new IllegalArgumentException("同步字段数值不合法: " + field);
            }
            return value;
        }
        return currentValue;
    }

    private boolean isNonNegativeNumericField(String field) {
        return Set.of(
            "balance", "sale_price", "purchase_price", "stock", "safe_stock",
            "subtotal_amount", "discount_amount", "total_amount", "paid_amount",
            "received_amount", "quantity", "unit_price", "unit_cost", "amount",
            "fee", "refund_amount", "last_purchase_price"
        ).contains(field);
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

    private Long requireOwnedProduct(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        productRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("商品不属于当前账号"));
        return id;
    }

    private Long requireOwnedCustomer(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        customerRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("客户不属于当前账号"));
        return id;
    }

    private Long requireOwnedSupplier(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        supplierRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("供应商不属于当前账号"));
        return id;
    }

    private Long requireOwnedCategory(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        productCategoryRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("商品分类不属于当前账号"));
        return id;
    }

    private Long requireOwnedUnit(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        productUnitRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("商品单位不属于当前账号"));
        return id;
    }

    private Long requireOwnedGroup(Long ownerUserId, String partnerType, Long id) {
        if (id == null) {
            return null;
        }
        partnerGroupRepository.findByIdAndOwnerUserIdAndPartnerType(id, ownerUserId, partnerType)
            .orElseThrow(() -> new AccessDeniedException("往来单位分组不属于当前账号"));
        return id;
    }

    private Long requireOwnedPartner(Long ownerUserId, String partnerType, Long id) {
        return "customer".equals(partnerType)
            ? requireOwnedCustomer(ownerUserId, id)
            : requireOwnedSupplier(ownerUserId, id);
    }

    private Long requireOwnedSaleOrder(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        saleOrderRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("销售单不属于当前账号"));
        return id;
    }

    private Long requireOwnedPurchaseOrder(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        purchaseOrderRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("采购单不属于当前账号"));
        return id;
    }

    private Long requireOwnedAccount(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        accountRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("账户不属于当前账号"));
        return id;
    }

    private Long requireOwnedSalesReturn(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("销售退货单不属于当前账号"));
        return id;
    }

    private Long requireOwnedPurchaseReceipt(Long ownerUserId, Long id) {
        if (id == null) {
            return null;
        }
        purchaseReceiptRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new AccessDeniedException("采购收货单不属于当前账号"));
        return id;
    }

    public record HealthResult(
        String status,
        String message,
        Boolean ownerScoped,
        Long serverTime,
        List<String> supportedEntityTypes,
        List<String> uploadableEntityTypes
    ) {}

    public record SyncChange(
        String operationId,
        String entityType,
        String entityId,
        String operation,
        String payload,
        Long updatedAt,
        Long baseVersion
    ) {
        public SyncChange(String entityType, String entityId, String operation, String payload, Long updatedAt) {
            this(null, entityType, entityId, operation, payload, updatedAt, null);
        }
    }

    public record UploadResult(
        Integer acceptedCount,
        Integer failedCount,
        String status,
        String nextCursor,
        List<String> acceptedOperationIds,
        List<String> failedOperationIds,
        List<SyncOperationFailure> failures,
        List<SyncOperationResult> operationResults
    ) {
        public UploadResult(
            Integer acceptedCount,
            Integer failedCount,
            String status,
            String nextCursor,
            List<String> acceptedOperationIds,
            List<String> failedOperationIds,
            List<SyncOperationFailure> failures
        ) {
            this(
                acceptedCount,
                failedCount,
                status,
                nextCursor,
                acceptedOperationIds,
                failedOperationIds,
                failures,
                List.of()
            );
        }
    }

    public record SyncOperationFailure(String operationId, String code, String message) {}

    public record SyncOperationResult(
        String operationId,
        String status,
        String code,
        String message,
        Long serverVersion,
        List<String> conflictFields,
        String serverPayload
    ) {
        public SyncOperationResult(String operationId, String status, String code, String message) {
            this(operationId, status, code, message, null, List.of(), null);
        }
    }

    public record PullResult(List<SyncChange> changes, String effectiveCursor, String nextCursor, Boolean hasMore) {}

    public record CursorStatus(String clientId, String lastCursor, Long updatedAt) {}

    private record PersistedChanges(List<SyncChange> changes, List<Long> sequences) {}

    private record ConflictSnapshot(List<String> conflictFields, String serverPayload) {}

    private static final class SyncConflictException extends RuntimeException {
        private final Long serverVersion;
        private final List<String> conflictFields;
        private final String serverPayload;

        private SyncConflictException(
            String message,
            Long serverVersion,
            List<String> conflictFields,
            String serverPayload
        ) {
            super(message);
            this.serverVersion = serverVersion;
            this.conflictFields = conflictFields == null ? List.of() : List.copyOf(conflictFields);
            this.serverPayload = serverPayload;
        }

        private Long serverVersion() { return serverVersion; }
        private List<String> conflictFields() { return conflictFields; }
        private String serverPayload() { return serverPayload; }
    }

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
