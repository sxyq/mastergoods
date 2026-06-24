package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncCursorRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncService {
    private static final int DEFAULT_PULL_LIMIT = 100;
    private static final int MAX_PULL_LIMIT = 500;

    private final SyncCursorRepository syncCursorRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PayOrderRepository payOrderRepository;
    private final ObjectMapper objectMapper;
    private final CurrentOwnerService currentOwnerService;

    public SyncService(
        SyncCursorRepository syncCursorRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        SaleOrderRepository saleOrderRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        PayOrderRepository payOrderRepository,
        ObjectMapper objectMapper,
        CurrentOwnerService currentOwnerService
    ) {
        this.syncCursorRepository = syncCursorRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.payOrderRepository = payOrderRepository;
        this.objectMapper = objectMapper;
        this.currentOwnerService = currentOwnerService;
    }

    public HealthResult health() {
        return new HealthResult("ok", "sync service ready", System.currentTimeMillis());
    }

    @Transactional
    public UploadResult upload(String clientId, List<SyncChange> changes, String lastSyncCursor) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        String normalizedClientId = normalizeClientId(clientId);
        List<SyncChange> safeChanges = changes == null ? List.of() : changes;
        long nextCursor = Math.max(now, parseCursor(lastSyncCursor));
        int acceptedCount = 0;
        int failedCount = 0;
        for (SyncChange change : safeChanges) {
            try {
                applyUploadedChange(ownerUserId, change);
                acceptedCount++;
                nextCursor = Math.max(nextCursor, safeLong(change.updatedAt()));
            } catch (RuntimeException ignored) {
                failedCount++;
            }
        }

        SyncCursorEntity cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId)
            .orElseGet(SyncCursorEntity::new);
        cursor.setOwnerUserId(ownerUserId);
        cursor.setClientId(normalizedClientId);
        cursor.setLastCursor(String.valueOf(nextCursor));
        cursor.setUpdatedAt(now);
        syncCursorRepository.save(cursor);
        return new UploadResult(
            acceptedCount,
            failedCount,
            failedCount == 0 ? "applied" : "partially applied",
            cursor.getLastCursor()
        );
    }

    @Transactional(readOnly = true)
    public PullResult pull(String sinceCursor, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long since = parseCursor(sinceCursor);
        int safeLimit = normalizeLimit(limit);

        List<SyncChange> changes = new ArrayList<>(safeLimit);
        changes.addAll(collectCustomerChanges(ownerUserId, since));
        changes.addAll(collectSupplierChanges(ownerUserId, since));
        changes.addAll(collectProductChanges(ownerUserId, since));
        changes.addAll(collectSaleOrderChanges(ownerUserId, since));
        changes.addAll(collectPurchaseOrderChanges(ownerUserId, since));
        changes.addAll(collectPayOrderChanges(ownerUserId, since));

        changes.sort(
            Comparator.comparingLong((SyncChange c) -> safeLong(c.updatedAt()))
                .thenComparing(c -> c.entityType() == null ? "" : c.entityType())
                .thenComparing(c -> c.entityId() == null ? "" : c.entityId())
        );

        boolean hasMore = changes.size() > safeLimit;
        List<SyncChange> page = hasMore ? new ArrayList<>(changes.subList(0, safeLimit)) : changes;
        long nextCursor = since;
        if (!page.isEmpty()) {
            nextCursor = safeLong(page.get(page.size() - 1).updatedAt());
        }
        return new PullResult(page, String.valueOf(nextCursor), hasMore);
    }

    private List<SyncChange> collectCustomerChanges(Long ownerUserId, long since) {
        List<CustomerEntity> entities = customerRepository.findChangedByOwnerUserId(ownerUserId, since);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (CustomerEntity entity : entities) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = newPayload(10);
            payload.put("id", entity.getId());
            payload.put("name", entity.getName());
            payload.put("phone", entity.getPhone());
            payload.put("balance", entity.getBalance());
            payload.put("status", entity.getStatus());
            putSyncMetadata(payload, entity.getSyncStatus(), entity.getSyncVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
            rows.add(new SyncChange("customer", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectSupplierChanges(Long ownerUserId, long since) {
        List<SupplierEntity> entities = supplierRepository.findChangedByOwnerUserId(ownerUserId, since);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (SupplierEntity entity : entities) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = newPayload(10);
            payload.put("id", entity.getId());
            payload.put("name", entity.getName());
            payload.put("phone", entity.getPhone());
            payload.put("address", entity.getAddress());
            payload.put("balance", entity.getBalance());
            payload.put("status", entity.getStatus());
            putSyncMetadata(payload, entity.getSyncStatus(), entity.getSyncVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
            rows.add(new SyncChange("supplier", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectProductChanges(Long ownerUserId, long since) {
        List<ProductEntity> entities = productRepository.findChangedByOwnerUserId(ownerUserId, since);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (ProductEntity entity : entities) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = newPayload(14);
            payload.put("id", entity.getId());
            payload.put("code", entity.getCode());
            payload.put("barcode", entity.getCode());
            payload.put("name", entity.getName());
            payload.put("unit", entity.getUnit());
            payload.put("sale_price", entity.getSalePrice());
            payload.put("purchase_price", entity.getPurchasePrice());
            payload.put("stock", entity.getStock());
            payload.put("safe_stock", entity.getSafeStock());
            payload.put("status", entity.getStatus());
            putSyncMetadata(payload, entity.getSyncStatus(), entity.getSyncVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
            rows.add(new SyncChange("product", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectSaleOrderChanges(Long ownerUserId, long since) {
        List<SaleOrderEntity> entities = saleOrderRepository.findChangedByOwnerUserId(ownerUserId, since);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (SaleOrderEntity entity : entities) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = newPayload(12);
            payload.put("id", entity.getId());
            payload.put("order_no", entity.getOrderNo());
            payload.put("customer_id", entity.getCustomerId());
            payload.put("customer_name", entity.getCustomerName());
            payload.put("total_amount", entity.getTotalAmount());
            payload.put("paid_amount", entity.getPaidAmount());
            payload.put("status", entity.getStatus());
            payload.put("notes", entity.getNotes());
            putSyncMetadata(payload, entity.getSyncStatus(), entity.getSyncVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
            rows.add(new SyncChange("sale_order", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectPurchaseOrderChanges(Long ownerUserId, long since) {
        List<PurchaseOrderEntity> entities = purchaseOrderRepository.findChangedByOwnerUserId(ownerUserId, since);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (PurchaseOrderEntity entity : entities) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = newPayload(11);
            payload.put("id", entity.getId());
            payload.put("order_no", entity.getOrderNo());
            payload.put("supplier_name", entity.getSupplierName());
            payload.put("total_amount", entity.getTotalAmount());
            payload.put("status", entity.getStatus());
            payload.put("notes", entity.getNotes());
            putSyncMetadata(payload, entity.getSyncStatus(), entity.getSyncVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
            rows.add(
                new SyncChange(
                    "purchase_order",
                    String.valueOf(entity.getId()),
                    "upsert",
                    writePayload(payload),
                    changedAt
                )
            );
        }
        return rows;
    }

    private List<SyncChange> collectPayOrderChanges(Long ownerUserId, long since) {
        List<PayOrderEntity> entities = payOrderRepository.findChangedByOwnerUserId(ownerUserId, since);
        List<SyncChange> rows = new ArrayList<>(entities.size());
        for (PayOrderEntity entity : entities) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = newPayload(12);
            payload.put("id", entity.getId());
            payload.put("order_no", entity.getOrderNo());
            payload.put("supplier_id", entity.getSupplierId());
            payload.put("supplier_name", entity.getSupplierName());
            payload.put("amount", entity.getAmount());
            payload.put("method", entity.getMethod());
            payload.put("account_id", entity.getAccountId());
            payload.put("status", entity.getStatus());
            payload.put("reference_no", entity.getReferenceNo());
            payload.put("notes", entity.getNotes());
            putSyncMetadata(payload, entity.getSyncStatus(), entity.getSyncVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
            rows.add(new SyncChange("pay_order", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private Map<String, Object> newPayload(int capacity) {
        return new HashMap<>(capacity);
    }

    private void putSyncMetadata(
        Map<String, Object> payload,
        Integer syncStatus,
        Long syncVersion,
        Long createdAt,
        Long updatedAt
    ) {
        payload.put("sync_status", syncStatus);
        payload.put("sync_version", syncVersion);
        payload.put("created_at", createdAt);
        payload.put("updated_at", updatedAt);
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize sync payload", error);
        }
    }

    private String normalizeClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return "anonymous";
        }
        return clientId.trim();
    }

    private long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_PULL_LIMIT;
        }
        return Math.min(limit, MAX_PULL_LIMIT);
    }

    private long resolveChangedAt(Long updatedAt, Long createdAt) {
        long update = safeLong(updatedAt);
        if (update > 0) {
            return update;
        }
        return safeLong(createdAt);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private void applyUploadedChange(Long ownerUserId, SyncChange change) {
        if (change == null || change.entityType() == null || change.entityId() == null || change.operation() == null) {
            throw new IllegalArgumentException("sync change is incomplete");
        }
        switch (change.entityType()) {
            case "customer" -> applyCustomerUpload(ownerUserId, change);
            case "supplier" -> applySupplierUpload(ownerUserId, change);
            case "product" -> applyProductUpload(ownerUserId, change);
            case "sale_order" -> applySaleOrderUpload(ownerUserId, change);
            case "purchase_order" -> applyPurchaseOrderUpload(ownerUserId, change);
            case "pay_order" -> applyPayOrderUpload(ownerUserId, change);
            default -> throw new IllegalArgumentException("unsupported entity type: " + change.entityType());
        }
    }

    private void applyCustomerUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
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
        entity.setAddress(readNullableText(payload, "address", entity.getAddress()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
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
            supplierRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(supplierRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        SupplierEntity entity = supplierRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(SupplierEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setName(readText(payload, "name", entity.getName(), "未命名供应商"));
        entity.setPhone(readText(payload, "phone", entity.getPhone(), "unknown-" + id));
        entity.setAddress(readNullableText(payload, "address", entity.getAddress()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
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
        entity.setUnit(readText(payload, "unit", entity.getUnit(), "件"));
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

    private void applyPurchaseOrderUpload(Long ownerUserId, SyncChange change) {
        Long id = parseEntityId(change.entityId());
        if (isDelete(change.operation())) {
            purchaseOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).ifPresent(purchaseOrderRepository::delete);
            return;
        }
        JsonNode payload = readPayload(change.payload());
        PurchaseOrderEntity entity = purchaseOrderRepository.findByIdAndOwnerUserId(id, ownerUserId).orElseGet(PurchaseOrderEntity::new);
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo(readText(payload, "order_no", entity.getOrderNo(), "PO-" + id));
        entity.setSupplierName(readText(payload, "supplier_name", entity.getSupplierName(), "未命名供应商"));
        entity.setTotalAmount(readDouble(payload, "total_amount", entity.getTotalAmount(), 0.0));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        purchaseOrderRepository.save(entity);
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
        entity.setAccountId(readNullableLong(payload, "account_id", entity.getAccountId()));
        entity.setStatus(readInt(payload, "status", entity.getStatus(), 0));
        entity.setReferenceNo(readNullableText(payload, "reference_no", entity.getReferenceNo()));
        entity.setNotes(readNullableText(payload, "notes", entity.getNotes()));
        entity.setSyncStatus(readInt(payload, "sync_status", entity.getSyncStatus(), 0));
        entity.setSyncVersion(readLong(payload, "sync_version", entity.getSyncVersion(), 0L));
        entity.setCreatedAt(readLong(payload, "created_at", entity.getCreatedAt(), safeLong(change.updatedAt())));
        entity.setUpdatedAt(readLong(payload, "updated_at", entity.getUpdatedAt(), safeLong(change.updatedAt())));
        payOrderRepository.save(entity);
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
        return currentValue != null && !currentValue.isBlank() ? currentValue : defaultValue;
    }

    private String readNullableText(JsonNode payload, String field, String currentValue) {
        JsonNode node = payload.path(field);
        if (node.isMissingNode()) return currentValue;
        return node.isNull() ? null : node.asText();
    }

    private Integer readInt(JsonNode payload, String field, Integer currentValue, int defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asInt(defaultValue);
        }
        return currentValue != null ? currentValue : defaultValue;
    }

    private Long readLong(JsonNode payload, String field, Long currentValue, long defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asLong(defaultValue);
        }
        return currentValue != null ? currentValue : defaultValue;
    }

    private Long readNullableLong(JsonNode payload, String field, Long currentValue) {
        JsonNode node = payload.path(field);
        if (node.isMissingNode()) return currentValue;
        return node.isNull() ? null : node.asLong();
    }

    private Double readDouble(JsonNode payload, String field, Double currentValue, double defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asDouble(defaultValue);
        }
        return currentValue != null ? currentValue : defaultValue;
    }

    public record SyncChange(String entityType, String entityId, String operation, String payload, Long updatedAt) {}

    public record HealthResult(String status, String message, long serverTime) {}

    public record UploadResult(int acceptedCount, int failedCount, String message, String nextCursor) {}

    public record PullResult(List<SyncChange> changes, String nextCursor, boolean hasMore) {}
}
