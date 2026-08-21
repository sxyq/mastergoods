package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.dto.v2.sales.V2SaleOrderDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.SaleOrderService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2SaleOrderService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private final SaleOrderService saleOrderService;
    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CurrentOwnerService currentOwnerService;
    private final IdGenerator idGenerator;

    public V2SaleOrderService(
        SaleOrderService saleOrderService,
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        CurrentOwnerService currentOwnerService,
        IdGenerator idGenerator
    ) {
        this.saleOrderService = saleOrderService;
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.currentOwnerService = currentOwnerService;
        this.idGenerator = idGenerator;
    }

    public V2SaleOrderDtos.SaleOrderResponse create(V2SaleOrderDtos.CreateRequest request) {
        List<SaleOrderService.SaleItemDraft> items = new ArrayList<>(request.items().size());
        for (V2SaleOrderDtos.CreateItemRequest row : request.items()) {
            items.add(new SaleOrderService.SaleItemDraft(row.productId(), row.quantity(), row.unitPrice()));
        }
        return toResponse(saleOrderService.create(
            new SaleOrderService.CreateSaleOrderCommand(
                request.customerId(),
                request.customerName(),
                items,
                request.notes(),
                request.discountAmount() == null ? 0.0 : request.discountAmount()
            )
        ));
    }

    @Transactional(readOnly = true)
    public List<V2SaleOrderDtos.SaleOrderResponse> list(
        String keyword,
        Integer status,
        Double minTotalAmount,
        Double maxTotalAmount,
        Long createdAfter,
        Long createdBefore,
        String productKeyword,
        Integer paymentStatus
    ) {
        return list(
            keyword,
            status,
            minTotalAmount,
            maxTotalAmount,
            createdAfter,
            createdBefore,
            productKeyword,
            paymentStatus,
            null,
            null
        );
    }

    public List<V2SaleOrderDtos.SaleOrderResponse> list(
        String keyword,
        Integer status,
        Double minTotalAmount,
        Double maxTotalAmount,
        Long createdAfter,
        Long createdBefore,
        String productKeyword,
        Integer paymentStatus,
        Integer page,
        Integer size
    ) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<SaleOrderEntity> orders = saleOrderRepository.search(
            ownerUserId,
            keyword,
            status,
            minTotalAmount,
            maxTotalAmount,
            createdAfter,
            createdBefore,
            productKeyword,
            paymentStatus,
            PageRequest.of(normalizePage(page), normalizeSize(size))
        );
        Map<Long, List<SaleOrderItemEntity>> itemsByOrderId = findItemsByOrderId(ownerUserId, orders);
        return orders.stream()
            .map(order -> toResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
            .toList();
    }

    @Transactional(readOnly = true)
    public V2SaleOrderDtos.SaleOrderResponse get(Long id) {
        return toResponse(saleOrderService.get(id));
    }

    @Transactional
    public V2SaleOrderDtos.SaleOrderResponse updateDraft(Long id, V2SaleOrderDtos.UpdateDraftRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != OrderStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }

        long now = System.currentTimeMillis();

        if (request.items() != null && !request.items().isEmpty()) {
            List<SaleOrderItemEntity> oldItems = saleOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, id);
            for (SaleOrderItemEntity oldItem : oldItems) {
                ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, oldItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
                product.setStock(product.getStock() + oldItem.getQuantity());
                product.setUpdatedAt(now);
                product.setSyncStatus(0);
                product.setSyncVersion(product.getSyncVersion() + 1);
                productRepository.save(product);
            }
            saleOrderItemRepository.deleteAll(oldItems);

            double subtotal = 0.0;
            List<SaleOrderItemEntity> newItems = new ArrayList<>(request.items().size());
            for (V2SaleOrderDtos.CreateItemRequest itemReq : request.items()) {
                ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + itemReq.productId()));
                if (product.getStock() < itemReq.quantity()) {
                    throw new IllegalArgumentException("库存不足: " + product.getName());
                }
                product.setStock(product.getStock() - itemReq.quantity());
                product.setUpdatedAt(now);
                product.setSyncStatus(0);
                product.setSyncVersion(product.getSyncVersion() + 1);
                productRepository.save(product);

                SaleOrderItemEntity item = new SaleOrderItemEntity();
                item.setId(idGenerator.nextId());
                item.setOwnerUserId(ownerUserId);
                item.setOrderId(id);
                item.setProductId(product.getId());
                item.setProductCode(product.getCode());
                item.setProductName(product.getName());
                item.setCustomerId(order.getCustomerId());
                item.setCustomerName(order.getCustomerName());
                item.setQuantity(itemReq.quantity());
                item.setUnitPrice(itemReq.unitPrice());
                item.setAmount(itemReq.quantity() * itemReq.unitPrice());
                item.setCreatedAt(now);
                subtotal += item.getAmount();
                newItems.add(item);
            }
            saleOrderItemRepository.saveAll(newItems);
            order.setSubtotalAmount(subtotal);
        }

        double discount = Math.max(request.discountAmount() == null ? order.getDiscountAmount() : request.discountAmount(), 0.0);
        double oldTotal = order.getTotalAmount();
        double newTotal = Math.max(0.0, order.getSubtotalAmount() - discount);
        double delta = newTotal - oldTotal;

        order.setDiscountAmount(discount);
        order.setTotalAmount(newTotal);
        if (request.notes() != null) {
            order.setNotes(request.notes());
        }
        order.setUpdatedAt(now);
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);

        if (order.getCustomerId() != null && Math.abs(delta) > 0.000001) {
            CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(order.getCustomerId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setBalance(customer.getBalance() + delta);
            customer.setUpdatedAt(now);
            customer.setSyncStatus(0);
            customer.setSyncVersion(customer.getSyncVersion() + 1);
            customerRepository.save(customer);
        }

        return toResponse(order, saleOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, id));
    }

    @Transactional
    public V2SaleOrderDtos.SaleOrderResponse confirm(Long id, V2SaleOrderDtos.ConfirmRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != OrderStatus.DRAFT.code()) {
            throw new IllegalArgumentException("仅草稿状态可确认");
        }
        order.setStatus(OrderStatus.CONFIRMED.code());
        if (request.notes() != null) {
            order.setNotes(request.notes());
        }
        order.setUpdatedAt(System.currentTimeMillis());
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);
        return toResponse(order, saleOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, id));
    }

    public V2SaleOrderDtos.PaymentResponse addPayment(Long id, V2SaleOrderDtos.PaymentRequest request) {
        return toPaymentResponse(saleOrderService.addPayment(id, request.amount(), request.method(), request.referenceNo()));
    }

    @Transactional(readOnly = true)
    public List<V2SaleOrderDtos.PaymentResponse> listPayments(Long id) {
        List<PaymentEntity> payments = saleOrderService.listPayments(id);
        return payments.stream().map(this::toPaymentResponse).toList();
    }

    public void updateStatus(Long id, Integer status) {
        saleOrderService.updateStatus(id, status);
    }

    public V2SaleOrderDtos.SaleOrderResponse cancel(Long id) {
        return toResponse(saleOrderService.cancel(id));
    }

    private V2SaleOrderDtos.SaleOrderResponse toResponse(SaleOrderService.OrderDetail detail) {
        return toResponse(detail.order(), detail.items());
    }

    private V2SaleOrderDtos.SaleOrderResponse toResponse(SaleOrderEntity order, List<SaleOrderItemEntity> items) {
        List<V2SaleOrderDtos.SaleOrderItemResponse> itemResponses = items.stream().map(this::toItemResponse).toList();
        return new V2SaleOrderDtos.SaleOrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getCustomerId(),
            order.getCustomerName(),
            itemResponses,
            order.getSubtotalAmount(),
            order.getDiscountAmount(),
            order.getTotalAmount(),
            order.getPaidAmount(),
            order.getNotes(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private V2SaleOrderDtos.SaleOrderItemResponse toItemResponse(SaleOrderItemEntity item) {
        return new V2SaleOrderDtos.SaleOrderItemResponse(
            item.getId(),
            item.getOrderId(),
            item.getProductId(),
            item.getProductCode(),
            item.getProductName(),
            item.getCustomerId(),
            item.getCustomerName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount(),
            item.getCreatedAt()
        );
    }

    private V2SaleOrderDtos.PaymentResponse toPaymentResponse(PaymentEntity payment) {
        return new V2SaleOrderDtos.PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getMethod(),
            payment.getReferenceNo(),
            payment.getType(),
            payment.getCreatedAt()
        );
    }

    private Map<Long, List<SaleOrderItemEntity>> findItemsByOrderId(Long ownerUserId, List<SaleOrderEntity> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> orderIds = new java.util.HashSet<>(orders.size());
        for (SaleOrderEntity order : orders) {
            Long orderId = order.getId();
            if (orderId != null && orderId > 0L) {
                orderIds.add(orderId);
            }
        }
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOwnerUserIdAndOrderIdIn(ownerUserId, orderIds);
        Map<Long, List<SaleOrderItemEntity>> itemsByOrderId = new java.util.LinkedHashMap<>();
        for (SaleOrderItemEntity item : items) {
            itemsByOrderId.computeIfAbsent(item.getOrderId(), ignored -> new ArrayList<>()).add(item);
        }
        return itemsByOrderId;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
