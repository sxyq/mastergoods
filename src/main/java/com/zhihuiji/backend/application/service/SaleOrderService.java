package com.zhihuiji.backend.application.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleOrderService {
    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentOwnerService currentOwnerService;

    public SaleOrderService(
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        PaymentRepository paymentRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional
    public OrderDetail create(CreateSaleOrderCommand command) {
        return createForOwner(currentOwnerService.requireCurrentOwnerUserId(), command);
    }

    @Transactional
    public OrderDetail createForOwner(Long ownerUserId, CreateSaleOrderCommand command) {
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("订单明细不能为空");
        }
        long now = System.currentTimeMillis();
        long orderId = IdGenerator.nextId();
        String orderNo = "SO" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        double subtotal = 0.0;
        List<SaleOrderItemEntity> itemEntities = new ArrayList<>();
        for (SaleItemDraft item : command.items()) {
            ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + item.productId()));
            if (product.getStock() < item.quantity()) {
                throw new IllegalArgumentException("库存不足: " + product.getName());
            }
            product.setStock(product.getStock() - item.quantity());
            product.setSyncStatus(0);
            product.setSyncVersion(product.getSyncVersion() + 1);
            product.setUpdatedAt(now);
            productRepository.save(product);

            SaleOrderItemEntity entity = new SaleOrderItemEntity();
            entity.setId(IdGenerator.nextId());
            entity.setOwnerUserId(ownerUserId);
            entity.setOrderId(orderId);
            entity.setProductId(product.getId());
            entity.setProductCode(product.getCode());
            entity.setProductName(product.getName());
            entity.setCustomerId(command.customerId());
            entity.setCustomerName(command.customerName());
            entity.setQuantity(item.quantity());
            entity.setUnitPrice(item.unitPrice());
            entity.setAmount(item.quantity() * item.unitPrice());
            entity.setCreatedAt(now);
            subtotal += entity.getAmount();
            itemEntities.add(entity);
        }

        double discount = Math.max(command.discountAmount(), 0.0);
        double total = Math.max(0.0, subtotal - discount);
        SaleOrderEntity order = new SaleOrderEntity();
        order.setId(orderId);
        order.setOwnerUserId(ownerUserId);
        order.setOrderNo(orderNo);
        order.setCustomerId(command.customerId());
        order.setCustomerName(command.customerName());
        order.setSubtotalAmount(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setPaidAmount(0.0);
        order.setNotes(command.notes());
        order.setStatus(OrderStatus.DRAFT.code());
        order.setSyncStatus(0);
        order.setSyncVersion(1L);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        saleOrderRepository.save(order);
        saleOrderItemRepository.saveAll(itemEntities);

        if (order.getCustomerId() != null) {
            CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(order.getCustomerId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setBalance(customer.getBalance() + total);
            customer.setUpdatedAt(now);
            customer.setSyncStatus(0);
            customer.setSyncVersion(customer.getSyncVersion() + 1);
            customerRepository.save(customer);
        }
        return toDetail(order);
    }

    public List<SaleOrderEntity> list(String keyword, Integer status) {
        return list(keyword, status, null, null, null, null, null, null);
    }

    public List<SaleOrderEntity> list(
        String keyword,
        Integer status,
        Double minTotalAmount,
        Double maxTotalAmount,
        Long createdAfter,
        Long createdBefore,
        String productKeyword,
        Integer paymentStatus
    ) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return saleOrderRepository.search(
            ownerUserId,
            keyword,
            status,
            minTotalAmount,
            maxTotalAmount,
            createdAfter,
            createdBefore,
            productKeyword,
            paymentStatus
        );
    }

    public OrderDetail get(Long orderId) {
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        return toDetail(order);
    }

    public List<SaleOrderItemEntity> listItems(Long orderId) {
        return saleOrderItemRepository.findByOwnerUserIdAndOrderId(currentOwnerService.requireCurrentOwnerUserId(), orderId);
    }

    public List<PaymentEntity> listPayments(Long orderId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (!saleOrderRepository.existsByIdAndOwnerUserId(orderId, ownerUserId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        return paymentRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId).stream()
            .sorted(Comparator.comparingLong(PaymentEntity::getCreatedAt))
            .toList();
    }

    @Transactional
    public OrderDetail updateDraft(Long orderId, Double discountAmount, String notes) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() == OrderStatus.CANCELLED.code()) {
            throw new IllegalArgumentException("已取消订单不可编辑");
        }
        double subtotal = order.getSubtotalAmount();
        double discount = Math.max(discountAmount == null ? order.getDiscountAmount() : discountAmount, 0.0);
        double total = Math.max(0.0, subtotal - discount);
        double delta = total - order.getTotalAmount();

        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setNotes(notes);
        order.setUpdatedAt(System.currentTimeMillis());
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);

        if (order.getCustomerId() != null && Math.abs(delta) > 0.000001) {
            CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(order.getCustomerId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setBalance(customer.getBalance() + delta);
            customer.setUpdatedAt(System.currentTimeMillis());
            customer.setSyncStatus(0);
            customer.setSyncVersion(customer.getSyncVersion() + 1);
            customerRepository.save(customer);
        }
        return toDetail(order);
    }

    @Transactional
    public PaymentEntity addPayment(Long orderId, Double amount, Integer method, String referenceNo) {
        return addPaymentForOwner(currentOwnerService.requireCurrentOwnerUserId(), orderId, amount, method, referenceNo);
    }

    @Transactional
    public PaymentEntity addPaymentForOwner(Long ownerUserId, Long orderId, Double amount, Integer method, String referenceNo) {
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() == OrderStatus.CANCELLED.code()) {
            throw new IllegalArgumentException("已取消订单不可收款");
        }
        double unpaid = Math.max(0.0, order.getTotalAmount() - order.getPaidAmount());
        if (amount <= 0 || amount - unpaid > 0.000001) {
            throw new IllegalArgumentException("收款金额无效");
        }

        long now = System.currentTimeMillis();
        PaymentEntity payment = new PaymentEntity();
        payment.setId(IdGenerator.nextId());
        payment.setOwnerUserId(ownerUserId);
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setReferenceNo(referenceNo);
        payment.setType(PaymentType.RECEIVE.code());
        payment.setCreatedAt(now);
        payment = paymentRepository.save(payment);

        order.setPaidAmount(order.getPaidAmount() + amount);
        if (Math.abs(order.getPaidAmount() - order.getTotalAmount()) < 0.000001 || order.getPaidAmount() > order.getTotalAmount()) {
            order.setStatus(OrderStatus.COMPLETED.code());
        }
        order.setUpdatedAt(now);
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);

        if (order.getCustomerId() != null) {
            CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(order.getCustomerId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setBalance(Math.max(0.0, customer.getBalance() - amount));
            customer.setUpdatedAt(now);
            customer.setSyncStatus(0);
            customer.setSyncVersion(customer.getSyncVersion() + 1);
            customerRepository.save(customer);
        }
        return payment;
    }

    @Transactional
    public void updateStatus(Long orderId, Integer status) {
        if (status == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus().equals(status)) {
            return;
        }
        if (status == OrderStatus.CANCELLED.code()) {
            cancel(orderId);
            return;
        }
        if (status != OrderStatus.DRAFT.code() && status != OrderStatus.COMPLETED.code()) {
            throw new IllegalArgumentException("不支持的状态值");
        }
        if (order.getStatus() == OrderStatus.CANCELLED.code()) {
            throw new IllegalArgumentException("已取消订单不可变更状态");
        }
        if (status == OrderStatus.COMPLETED.code() && order.getStatus() == OrderStatus.DRAFT.code()) {
            throw new IllegalArgumentException("草稿订单需先确认后再标记为已完成");
        }
        if (status == OrderStatus.DRAFT.code() && order.getStatus() == OrderStatus.COMPLETED.code()) {
            throw new IllegalArgumentException("已完成订单不可回退为草稿");
        }
        if (status == OrderStatus.COMPLETED.code() && order.getPaidAmount() + 0.000001 < order.getTotalAmount()) {
            throw new IllegalArgumentException("未付清订单不可标记为已完成");
        }

        order.setStatus(status);
        order.setUpdatedAt(System.currentTimeMillis());
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);
    }

    public byte[] exportPdf(Long orderId) {
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        return buildSimplePdf(order);
    }

    @Transactional
    public OrderDetail cancel(Long orderId) {
        return cancelForOwner(currentOwnerService.requireCurrentOwnerUserId(), orderId);
    }

    @Transactional
    public OrderDetail cancelForOwner(Long ownerUserId, Long orderId) {
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() == OrderStatus.CANCELLED.code()) {
            return toDetail(order);
        }

        long now = System.currentTimeMillis();
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId);
        for (SaleOrderItemEntity item : items) {
            ProductEntity product = productRepository.findByIdForUpdate(ownerUserId, item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
            product.setStock(product.getStock() + item.getQuantity());
            product.setUpdatedAt(now);
            product.setSyncStatus(0);
            product.setSyncVersion(product.getSyncVersion() + 1);
            productRepository.save(product);
        }

        double unpaid = Math.max(0.0, order.getTotalAmount() - order.getPaidAmount());
        if (order.getCustomerId() != null && unpaid > 0) {
            CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(order.getCustomerId(), ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setBalance(Math.max(0.0, customer.getBalance() - unpaid));
            customer.setUpdatedAt(now);
            customer.setSyncStatus(0);
            customer.setSyncVersion(customer.getSyncVersion() + 1);
            customerRepository.save(customer);
        }

        if (order.getPaidAmount() > 0) {
            PaymentEntity refund = new PaymentEntity();
            refund.setId(IdGenerator.nextId());
            refund.setOwnerUserId(ownerUserId);
            refund.setOrderId(orderId);
            refund.setAmount(order.getPaidAmount());
            refund.setMethod(1);
            refund.setReferenceNo("AUTO-REFUND");
            refund.setType(PaymentType.REFUND.code());
            refund.setCreatedAt(now);
            paymentRepository.save(refund);
        }

        order.setStatus(OrderStatus.CANCELLED.code());
        order.setUpdatedAt(now);
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);
        return toDetail(order);
    }

    private byte[] buildSimplePdf(SaleOrderEntity order) {
        String line1 = "Sale Order: " + escapePdfText(order.getOrderNo());
        String line2 = "Total Amount: " + order.getTotalAmount();
        String line3 = "Paid Amount: " + order.getPaidAmount();
        String line4 = "Status: " + order.getStatus();
        String content = "BT\n/F1 14 Tf\n50 760 Td\n(" + line1 + ") Tj\n0 -24 Td\n("
            + escapePdfText(line2) + ") Tj\n0 -24 Td\n(" + escapePdfText(line3)
            + ") Tj\n0 -24 Td\n(" + escapePdfText(line4) + ") Tj\nET\n";
        byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);

        String[] objects = new String[] {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Count 1 /Kids [3 0 R] >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Length " + contentBytes.length + " >>\nstream\n"
                + content + "endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = out.size();
            String obj = (i + 1) + " 0 obj\n" + objects[i] + "\nendobj\n";
            out.writeBytes(obj.getBytes(StandardCharsets.US_ASCII));
        }
        int startXref = out.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(objects.length + 1).append('\n');
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.length; i++) {
            xref.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        }
        xref.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\n");
        xref.append("startxref\n").append(startXref).append('\n');
        xref.append("%%EOF");
        out.writeBytes(xref.toString().getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }

    private String escapePdfText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)");
    }

    private OrderDetail toDetail(SaleOrderEntity order) {
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOwnerUserIdAndOrderId(order.getOwnerUserId(), order.getId());
        List<PaymentEntity> payments = paymentRepository.findByOwnerUserIdAndOrderId(order.getOwnerUserId(), order.getId());
        return new OrderDetail(order, items, payments);
    }

    public record SaleItemDraft(Long productId, Double quantity, Double unitPrice) {}

    public record CreateSaleOrderCommand(
        Long customerId,
        String customerName,
        List<SaleItemDraft> items,
        String notes,
        Double discountAmount
    ) {}

    public record OrderDetail(
        SaleOrderEntity order,
        List<SaleOrderItemEntity> items,
        List<PaymentEntity> payments
    ) {}

}
