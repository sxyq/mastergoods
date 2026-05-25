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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleOrderService {
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int PAYMENT_TYPE_RECEIVE = 1;
    public static final int PAYMENT_TYPE_REFUND = 2;

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;

    public SaleOrderService(
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        PaymentRepository paymentRepository
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public OrderDetail create(CreateSaleOrderCommand command) {
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("订单明细不能为空");
        }
        long now = System.currentTimeMillis();
        long orderId = nextId();
        String orderNo = "SO" + now + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        double subtotal = 0.0;
        List<SaleOrderItemEntity> itemEntities = new ArrayList<>();
        for (SaleItemDraft item : command.items()) {
            ProductEntity product = productRepository.findByIdForUpdate(item.productId())
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
            entity.setId(nextId());
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
        order.setOrderNo(orderNo);
        order.setCustomerId(command.customerId());
        order.setCustomerName(command.customerName());
        order.setSubtotalAmount(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setPaidAmount(0.0);
        order.setNotes(command.notes());
        order.setStatus(STATUS_DRAFT);
        order.setSyncStatus(0);
        order.setSyncVersion(1L);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        saleOrderRepository.save(order);
        saleOrderItemRepository.saveAll(itemEntities);

        if (order.getCustomerId() != null) {
            CustomerEntity customer = customerRepository.findById(order.getCustomerId())
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
        return saleOrderRepository.findAll().stream()
            .filter(order -> status == null || order.getStatus().equals(status))
            .filter(order -> keyword == null || keyword.isBlank()
                || order.getOrderNo().toLowerCase().contains(keyword.toLowerCase())
                || (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(keyword.toLowerCase())))
            .filter(order -> minTotalAmount == null || order.getTotalAmount() + 0.000001 >= minTotalAmount)
            .filter(order -> maxTotalAmount == null || order.getTotalAmount() - 0.000001 <= maxTotalAmount)
            .filter(order -> createdAfter == null || order.getCreatedAt() >= createdAfter)
            .filter(order -> createdBefore == null || order.getCreatedAt() <= createdBefore)
            .filter(order -> isPaymentStatusMatched(order, paymentStatus))
            .filter(order -> isProductKeywordMatched(order.getId(), productKeyword))
            .sorted(Comparator.comparingLong(SaleOrderEntity::getCreatedAt).reversed())
            .toList();
    }

    public OrderDetail get(Long orderId) {
        SaleOrderEntity order = saleOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        return toDetail(order);
    }

    public List<SaleOrderItemEntity> listItems(Long orderId) {
        return saleOrderItemRepository.findByOrderId(orderId);
    }

    public List<PaymentEntity> listPayments(Long orderId) {
        if (!saleOrderRepository.existsById(orderId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        return paymentRepository.findByOrderId(orderId).stream()
            .sorted(Comparator.comparingLong(PaymentEntity::getCreatedAt))
            .toList();
    }

    @Transactional
    public OrderDetail updateDraft(Long orderId, Double discountAmount, String notes) {
        SaleOrderEntity order = saleOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() == STATUS_CANCELLED) {
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
            CustomerEntity customer = customerRepository.findById(order.getCustomerId())
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
        SaleOrderEntity order = saleOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() == STATUS_CANCELLED) {
            throw new IllegalArgumentException("已取消订单不可收款");
        }
        double unpaid = Math.max(0.0, order.getTotalAmount() - order.getPaidAmount());
        if (amount <= 0 || amount - unpaid > 0.000001) {
            throw new IllegalArgumentException("收款金额无效");
        }

        long now = System.currentTimeMillis();
        PaymentEntity payment = new PaymentEntity();
        payment.setId(nextId());
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setReferenceNo(referenceNo);
        payment.setType(PAYMENT_TYPE_RECEIVE);
        payment.setCreatedAt(now);
        payment = paymentRepository.save(payment);

        order.setPaidAmount(order.getPaidAmount() + amount);
        if (Math.abs(order.getPaidAmount() - order.getTotalAmount()) < 0.000001 || order.getPaidAmount() > order.getTotalAmount()) {
            order.setStatus(STATUS_COMPLETED);
        }
        order.setUpdatedAt(now);
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);

        if (order.getCustomerId() != null) {
            CustomerEntity customer = customerRepository.findById(order.getCustomerId())
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
        SaleOrderEntity order = saleOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus().equals(status)) {
            return;
        }
        if (status == STATUS_CANCELLED) {
            cancel(orderId);
            return;
        }
        if (status != STATUS_DRAFT && status != STATUS_COMPLETED) {
            throw new IllegalArgumentException("不支持的状态值");
        }
        if (order.getStatus() == STATUS_CANCELLED) {
            throw new IllegalArgumentException("已取消订单不可变更状态");
        }
        if (status == STATUS_COMPLETED && order.getPaidAmount() + 0.000001 < order.getTotalAmount()) {
            throw new IllegalArgumentException("未付清订单不可标记为已完成");
        }

        order.setStatus(status);
        order.setUpdatedAt(System.currentTimeMillis());
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);
    }

    public byte[] exportPdf(Long orderId) {
        SaleOrderEntity order = saleOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        return buildSimplePdf(order);
    }

    @Transactional
    public OrderDetail cancel(Long orderId) {
        SaleOrderEntity order = saleOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() == STATUS_CANCELLED) {
            return toDetail(order);
        }

        long now = System.currentTimeMillis();
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOrderId(orderId);
        for (SaleOrderItemEntity item : items) {
            ProductEntity product = productRepository.findByIdForUpdate(item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
            product.setStock(product.getStock() + item.getQuantity());
            product.setUpdatedAt(now);
            product.setSyncStatus(0);
            product.setSyncVersion(product.getSyncVersion() + 1);
            productRepository.save(product);
        }

        double unpaid = Math.max(0.0, order.getTotalAmount() - order.getPaidAmount());
        if (order.getCustomerId() != null && unpaid > 0) {
            CustomerEntity customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setBalance(Math.max(0.0, customer.getBalance() - unpaid));
            customer.setUpdatedAt(now);
            customer.setSyncStatus(0);
            customer.setSyncVersion(customer.getSyncVersion() + 1);
            customerRepository.save(customer);
        }

        if (order.getPaidAmount() > 0) {
            PaymentEntity refund = new PaymentEntity();
            refund.setId(nextId());
            refund.setOrderId(orderId);
            refund.setAmount(order.getPaidAmount());
            refund.setMethod(1);
            refund.setReferenceNo("AUTO-REFUND");
            refund.setType(PAYMENT_TYPE_REFUND);
            refund.setCreatedAt(now);
            paymentRepository.save(refund);
        }

        order.setStatus(STATUS_CANCELLED);
        order.setUpdatedAt(now);
        order.setSyncStatus(0);
        order.setSyncVersion(order.getSyncVersion() + 1);
        saleOrderRepository.save(order);
        return toDetail(order);
    }

    private boolean isPaymentStatusMatched(SaleOrderEntity order, Integer paymentStatus) {
        if (paymentStatus == null) {
            return true;
        }
        if (paymentStatus == 0) {
            return order.getPaidAmount() + 0.000001 < order.getTotalAmount();
        }
        if (paymentStatus == 1) {
            return order.getPaidAmount() + 0.000001 >= order.getTotalAmount();
        }
        return true;
    }

    private boolean isProductKeywordMatched(Long orderId, String productKeyword) {
        if (productKeyword == null || productKeyword.isBlank()) {
            return true;
        }
        String normalized = productKeyword.toLowerCase(Locale.ROOT);
        return saleOrderItemRepository.findByOrderId(orderId).stream()
            .anyMatch(item ->
                item.getProductCode().toLowerCase(Locale.ROOT).contains(normalized)
                    || item.getProductName().toLowerCase(Locale.ROOT).contains(normalized)
            );
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
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOrderId(order.getId());
        List<PaymentEntity> payments = paymentRepository.findByOrderId(order.getId());
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

    private long nextId() {
        long id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        return id == 0L ? (System.nanoTime() & Long.MAX_VALUE) : id;
    }
}
