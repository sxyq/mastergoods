package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    public SyncService(
        SyncCursorRepository syncCursorRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        SaleOrderRepository saleOrderRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        PayOrderRepository payOrderRepository,
        ObjectMapper objectMapper
    ) {
        this.syncCursorRepository = syncCursorRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.payOrderRepository = payOrderRepository;
        this.objectMapper = objectMapper;
    }

    public HealthResult health() {
        return new HealthResult("ok", "sync service ready", System.currentTimeMillis());
    }

    @Transactional
    public UploadResult upload(String clientId, List<SyncChange> changes, String lastSyncCursor) {
        long now = System.currentTimeMillis();
        String normalizedClientId = normalizeClientId(clientId);
        List<SyncChange> safeChanges = changes == null ? List.of() : changes;
        long nextCursor = Math.max(now, parseCursor(lastSyncCursor));
        for (SyncChange change : safeChanges) {
            nextCursor = Math.max(nextCursor, safeLong(change.updatedAt()));
        }

        SyncCursorEntity cursor = syncCursorRepository.findById(normalizedClientId).orElseGet(SyncCursorEntity::new);
        cursor.setClientId(normalizedClientId);
        cursor.setLastCursor(String.valueOf(nextCursor));
        cursor.setUpdatedAt(now);
        syncCursorRepository.save(cursor);
        return new UploadResult(safeChanges.size(), 0, "accepted", cursor.getLastCursor());
    }

    public PullResult pull(String sinceCursor, int limit) {
        long since = parseCursor(sinceCursor);
        int safeLimit = normalizeLimit(limit);

        List<SyncChange> changes = new ArrayList<>();
        changes.addAll(collectCustomerChanges(since));
        changes.addAll(collectSupplierChanges(since));
        changes.addAll(collectProductChanges(since));
        changes.addAll(collectSaleOrderChanges(since));
        changes.addAll(collectPurchaseOrderChanges(since));
        changes.addAll(collectPayOrderChanges(since));

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

    private List<SyncChange> collectCustomerChanges(long since) {
        List<SyncChange> rows = new ArrayList<>();
        for (CustomerEntity entity : customerRepository.findAll()) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (changedAt <= since || entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", entity.getId());
            payload.put("name", entity.getName());
            payload.put("phone", entity.getPhone());
            payload.put("balance", entity.getBalance());
            payload.put("status", entity.getStatus());
            payload.put("sync_status", entity.getSyncStatus());
            payload.put("sync_version", entity.getSyncVersion());
            payload.put("created_at", entity.getCreatedAt());
            payload.put("updated_at", entity.getUpdatedAt());
            rows.add(new SyncChange("customer", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectSupplierChanges(long since) {
        List<SyncChange> rows = new ArrayList<>();
        for (SupplierEntity entity : supplierRepository.findAll()) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (changedAt <= since || entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", entity.getId());
            payload.put("name", entity.getName());
            payload.put("phone", entity.getPhone());
            payload.put("address", entity.getAddress());
            payload.put("balance", entity.getBalance());
            payload.put("status", entity.getStatus());
            payload.put("sync_status", entity.getSyncStatus());
            payload.put("sync_version", entity.getSyncVersion());
            payload.put("created_at", entity.getCreatedAt());
            payload.put("updated_at", entity.getUpdatedAt());
            rows.add(new SyncChange("supplier", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectProductChanges(long since) {
        List<SyncChange> rows = new ArrayList<>();
        for (ProductEntity entity : productRepository.findAll()) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (changedAt <= since || entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
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
            payload.put("sync_status", entity.getSyncStatus());
            payload.put("sync_version", entity.getSyncVersion());
            payload.put("created_at", entity.getCreatedAt());
            payload.put("updated_at", entity.getUpdatedAt());
            rows.add(new SyncChange("product", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectSaleOrderChanges(long since) {
        List<SyncChange> rows = new ArrayList<>();
        for (SaleOrderEntity entity : saleOrderRepository.findAll()) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (changedAt <= since || entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", entity.getId());
            payload.put("order_no", entity.getOrderNo());
            payload.put("customer_id", entity.getCustomerId());
            payload.put("customer_name", entity.getCustomerName());
            payload.put("total_amount", entity.getTotalAmount());
            payload.put("paid_amount", entity.getPaidAmount());
            payload.put("status", entity.getStatus());
            payload.put("notes", entity.getNotes());
            payload.put("sync_status", entity.getSyncStatus());
            payload.put("sync_version", entity.getSyncVersion());
            payload.put("created_at", entity.getCreatedAt());
            payload.put("updated_at", entity.getUpdatedAt());
            rows.add(new SyncChange("sale_order", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
    }

    private List<SyncChange> collectPurchaseOrderChanges(long since) {
        List<SyncChange> rows = new ArrayList<>();
        for (PurchaseOrderEntity entity : purchaseOrderRepository.findAll()) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (changedAt <= since || entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", entity.getId());
            payload.put("order_no", entity.getOrderNo());
            payload.put("supplier_name", entity.getSupplierName());
            payload.put("total_amount", entity.getTotalAmount());
            payload.put("status", entity.getStatus());
            payload.put("notes", entity.getNotes());
            payload.put("sync_status", entity.getSyncStatus());
            payload.put("sync_version", entity.getSyncVersion());
            payload.put("created_at", entity.getCreatedAt());
            payload.put("updated_at", entity.getUpdatedAt());
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

    private List<SyncChange> collectPayOrderChanges(long since) {
        List<SyncChange> rows = new ArrayList<>();
        for (PayOrderEntity entity : payOrderRepository.findAll()) {
            long changedAt = resolveChangedAt(entity.getUpdatedAt(), entity.getCreatedAt());
            if (changedAt <= since || entity.getId() == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", entity.getId());
            payload.put("order_no", entity.getOrderNo());
            payload.put("supplier_id", entity.getSupplierId());
            payload.put("supplier_name", entity.getSupplierName());
            payload.put("amount", entity.getAmount());
            payload.put("method", entity.getMethod());
            payload.put("status", entity.getStatus());
            payload.put("reference_no", entity.getReferenceNo());
            payload.put("notes", entity.getNotes());
            payload.put("sync_status", entity.getSyncStatus());
            payload.put("sync_version", entity.getSyncVersion());
            payload.put("created_at", entity.getCreatedAt());
            payload.put("updated_at", entity.getUpdatedAt());
            rows.add(new SyncChange("pay_order", String.valueOf(entity.getId()), "upsert", writePayload(payload), changedAt));
        }
        return rows;
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

    public record SyncChange(String entityType, String entityId, String operation, String payload, Long updatedAt) {}

    public record HealthResult(String status, String message, long serverTime) {}

    public record UploadResult(int acceptedCount, int failedCount, String message, String nextCursor) {}

    public record PullResult(List<SyncChange> changes, String nextCursor, boolean hasMore) {}
}
