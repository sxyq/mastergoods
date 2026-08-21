package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.SalesReturnStatus;
import com.zhihuiji.backend.api.dto.v2.sales.V2SalesReturnDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnItemEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2SalesReturnService {
    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentOwnerService currentOwnerService;
    private final IdGenerator idGenerator;

    public V2SalesReturnService(
        SalesReturnRepository salesReturnRepository,
        SalesReturnItemRepository salesReturnItemRepository,
        SaleOrderRepository saleOrderRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        PaymentRepository paymentRepository,
        CurrentOwnerService currentOwnerService,
        IdGenerator idGenerator
    ) {
        this.salesReturnRepository = salesReturnRepository;
        this.salesReturnItemRepository = salesReturnItemRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.currentOwnerService = currentOwnerService;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public V2SalesReturnDtos.SalesReturnResponse create(V2SalesReturnDtos.CreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("退货明细不能为空");
        }

        long now = System.currentTimeMillis();
        long returnId = idGenerator.nextId();
        String returnNo = "SR" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        SaleOrderEntity originalOrder = null;
        if (request.originalOrderId() != null) {
            originalOrder = saleOrderRepository.findByIdAndOwnerUserId(request.originalOrderId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("原销售订单不存在"));
        }
        CustomerEntity customer = resolveCustomer(ownerUserId, request, originalOrder);

        double total = 0.0;
        List<SalesReturnItemEntity> itemEntities = new ArrayList<>(request.items().size());
        for (V2SalesReturnDtos.CreateItemRequest item : request.items()) {
            ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productId()));
            double quantity = item.quantity() == null ? 0.0 : item.quantity();
            double unitPrice = item.unitPrice() == null ? product.getSalePrice() : item.unitPrice();
            if (quantity <= 0.0) {
                throw new IllegalArgumentException("退货数量必须大于0");
            }
            if (unitPrice < 0.0) {
                throw new IllegalArgumentException("退货单价不能为负数");
            }
            double amount = quantity * unitPrice;
            total += amount;

            SalesReturnItemEntity entity = new SalesReturnItemEntity();
            entity.setId(idGenerator.nextId());
            entity.setOwnerUserId(ownerUserId);
            entity.setReturnId(returnId);
            entity.setProductId(product.getId());
            entity.setProductCode(product.getCode());
            entity.setProductName(product.getName());
            entity.setQuantity(quantity);
            entity.setUnitPrice(unitPrice);
            entity.setAmount(amount);
            entity.setCreatedAt(now);
            itemEntities.add(entity);
        }

        SalesReturnEntity returnEntity = new SalesReturnEntity();
        returnEntity.setId(returnId);
        returnEntity.setOwnerUserId(ownerUserId);
        returnEntity.setReturnNo(returnNo);
        returnEntity.setOriginalOrderId(request.originalOrderId());
        returnEntity.setCustomerId(customer.getId());
        returnEntity.setCustomerName(customer.getName());
        returnEntity.setTotalAmount(total);
        returnEntity.setRefundAmount(0.0);
        returnEntity.setStatus(SalesReturnStatus.DRAFT.code());
        returnEntity.setNotes(request.notes());
        returnEntity.setCreatedAt(now);
        returnEntity.setUpdatedAt(now);
        salesReturnRepository.save(returnEntity);
        salesReturnItemRepository.saveAll(itemEntities);

        return toResponse(returnEntity, itemEntities);
    }

    @Transactional(readOnly = true)
    public List<V2SalesReturnDtos.SalesReturnResponse> list(String keyword, Integer status) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedKeyword = normalizeKeyword(keyword);
        List<SalesReturnEntity> returns = normalizedKeyword == null
            ? listWithoutKeyword(ownerUserId, status)
            : salesReturnRepository.search(ownerUserId, normalizedKeyword, status);
        Map<Long, List<SalesReturnItemEntity>> itemsByReturnId = loadItemsByReturnId(ownerUserId, returns);
        return returns.stream()
            .map(entity -> toResponse(entity, itemsByReturnId.getOrDefault(entity.getId(), List.of())))
            .toList();
    }

    @Transactional(readOnly = true)
    public V2SalesReturnDtos.SalesReturnResponse get(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SalesReturnEntity entity = salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("退货单不存在"));
        return toResponse(entity, salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    @Transactional(readOnly = true)
    public List<V2SalesReturnDtos.SalesReturnResponse> listByOrder(Long originalOrderId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<SalesReturnEntity> returns = salesReturnRepository.findByOwnerUserIdAndOriginalOrderIdOrderByCreatedAtDesc(ownerUserId, originalOrderId);
        Map<Long, List<SalesReturnItemEntity>> itemsByReturnId = loadItemsByReturnId(ownerUserId, returns);
        return returns.stream()
            .map(entity -> toResponse(entity, itemsByReturnId.getOrDefault(entity.getId(), List.of())))
            .toList();
    }

    @Transactional
    public V2SalesReturnDtos.SalesReturnResponse updateDraft(Long id, V2SalesReturnDtos.UpdateDraftRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SalesReturnEntity entity = salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("退货单不存在"));
        if (entity.getStatus() != SalesReturnStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        entity.setNotes(request.notes());
        entity.setUpdatedAt(System.currentTimeMillis());
        salesReturnRepository.save(entity);
        return toResponse(entity, salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    @Transactional
    public V2SalesReturnDtos.SalesReturnResponse confirm(Long id, V2SalesReturnDtos.ConfirmRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SalesReturnEntity entity = salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("退货单不存在"));
        if (entity.getStatus() != SalesReturnStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可确认");
        }

        long now = System.currentTimeMillis();
        List<SalesReturnItemEntity> items = salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id);
        for (SalesReturnItemEntity item : items) {
            ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.getProductId()));
            product.setStock(product.getStock() + item.getQuantity());
            product.setUpdatedAt(now);
            product.setSyncStatus(0);
            product.setSyncVersion(product.getSyncVersion() + 1);
            productRepository.save(product);
        }

        if (entity.getCustomerId() != null) {
            CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(entity.getCustomerId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setBalance(Math.max(0.0, customer.getBalance() - entity.getTotalAmount()));
            customer.setUpdatedAt(now);
            customer.setSyncStatus(0);
            customer.setSyncVersion(customer.getSyncVersion() + 1);
            customerRepository.save(customer);
        }

        entity.setStatus(SalesReturnStatus.CONFIRMED.code());
        if (request.notes() != null) {
            entity.setNotes(request.notes());
        }
        entity.setUpdatedAt(now);
        salesReturnRepository.save(entity);
        return toResponse(entity, items);
    }

    @Transactional
    public V2SalesReturnDtos.SalesReturnResponse addRefund(Long id, V2SalesReturnDtos.RefundRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SalesReturnEntity entity = salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("退货单不存在"));
        if (entity.getStatus() == SalesReturnStatus.CANCELLED.code()) {
            throw new IllegalArgumentException("已取消退货单不可退款");
        }
        double remaining = entity.getTotalAmount() - entity.getRefundAmount();
        if (request.amount() == null || request.amount() <= 0 || request.amount() > remaining + 0.000001) {
            throw new IllegalArgumentException("退款金额无效");
        }

        long now = System.currentTimeMillis();
        PaymentEntity refund = new PaymentEntity();
        refund.setId(idGenerator.nextId());
        refund.setOwnerUserId(ownerUserId);
        refund.setOrderId(id);
        refund.setAmount(request.amount());
        refund.setMethod(request.method());
        refund.setReferenceNo(request.referenceNo());
        refund.setType(2);
        refund.setCreatedAt(now);
        paymentRepository.save(refund);

        entity.setRefundAmount(entity.getRefundAmount() + request.amount());
        if (Math.abs(entity.getRefundAmount() - entity.getTotalAmount()) < 0.000001 || entity.getRefundAmount() > entity.getTotalAmount()) {
            entity.setStatus(SalesReturnStatus.COMPLETED.code());
        }
        entity.setUpdatedAt(now);
        salesReturnRepository.save(entity);
        return toResponse(entity, salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    @Transactional
    public V2SalesReturnDtos.SalesReturnResponse cancel(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SalesReturnEntity entity = salesReturnRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("退货单不存在"));
        if (entity.getStatus() == SalesReturnStatus.CANCELLED.code()) {
            return toResponse(entity, salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id));
        }
        if (entity.getStatus() == SalesReturnStatus.CONFIRMED.code() || entity.getStatus() == SalesReturnStatus.COMPLETED.code()) {
            long now = System.currentTimeMillis();
            List<SalesReturnItemEntity> items = salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id);
            for (SalesReturnItemEntity item : items) {
                ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.getProductId()));
                product.setStock(product.getStock() - item.getQuantity());
                product.setUpdatedAt(now);
                product.setSyncStatus(0);
                product.setSyncVersion(product.getSyncVersion() + 1);
                productRepository.save(product);
            }
            if (entity.getCustomerId() != null) {
                CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(entity.getCustomerId(), ownerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
                customer.setBalance(customer.getBalance() + entity.getTotalAmount());
                customer.setUpdatedAt(now);
                customer.setSyncStatus(0);
                customer.setSyncVersion(customer.getSyncVersion() + 1);
                customerRepository.save(customer);
            }
        }
        entity.setStatus(SalesReturnStatus.CANCELLED.code());
        entity.setUpdatedAt(System.currentTimeMillis());
        salesReturnRepository.save(entity);
        return toResponse(entity, salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(ownerUserId, id));
    }

    private CustomerEntity resolveCustomer(Long ownerUserId, V2SalesReturnDtos.CreateRequest request, SaleOrderEntity originalOrder) {
        Long resolvedCustomerId = request.customerId();
        if (originalOrder != null) {
            if (originalOrder.getCustomerId() == null) {
                throw new IllegalArgumentException("原销售订单缺少客户信息");
            }
            if (resolvedCustomerId != null && !resolvedCustomerId.equals(originalOrder.getCustomerId())) {
                throw new IllegalArgumentException("退货客户与原销售订单不一致");
            }
            resolvedCustomerId = originalOrder.getCustomerId();
        }
        if (resolvedCustomerId == null) {
            throw new IllegalArgumentException("退货单必须关联客户");
        }
        return customerRepository.findByIdAndOwnerUserId(resolvedCustomerId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    private V2SalesReturnDtos.SalesReturnResponse toResponse(SalesReturnEntity entity, List<SalesReturnItemEntity> items) {
        List<V2SalesReturnDtos.SalesReturnItemResponse> itemResponses = items.stream().map(this::toItemResponse).toList();
        return new V2SalesReturnDtos.SalesReturnResponse(
            entity.getId(),
            entity.getReturnNo(),
            entity.getOriginalOrderId(),
            entity.getCustomerId(),
            entity.getCustomerName(),
            itemResponses,
            entity.getTotalAmount(),
            entity.getRefundAmount(),
            entity.getStatus(),
            entity.getNotes(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private List<SalesReturnEntity> listWithoutKeyword(Long ownerUserId, Integer status) {
        if (status == null) {
            return salesReturnRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        }
        return salesReturnRepository.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(ownerUserId, status);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, List<SalesReturnItemEntity>> loadItemsByReturnId(Long ownerUserId, List<SalesReturnEntity> returns) {
        if (returns.isEmpty()) {
            return Map.of();
        }
        List<Long> returnIds = returns.stream().map(SalesReturnEntity::getId).toList();
        Map<Long, List<SalesReturnItemEntity>> itemsByReturnId = new LinkedHashMap<>(returnIds.size());
        for (SalesReturnItemEntity item : salesReturnItemRepository.findByOwnerUserIdAndReturnIdInOrderByReturnIdAscCreatedAtAsc(ownerUserId, returnIds)) {
            itemsByReturnId.computeIfAbsent(item.getReturnId(), ignored -> new ArrayList<>()).add(item);
        }
        return itemsByReturnId;
    }

    private V2SalesReturnDtos.SalesReturnItemResponse toItemResponse(SalesReturnItemEntity item) {
        return new V2SalesReturnDtos.SalesReturnItemResponse(
            item.getId(),
            item.getReturnId(),
            item.getProductId(),
            item.getProductCode(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount(),
            item.getCreatedAt()
        );
    }
}
