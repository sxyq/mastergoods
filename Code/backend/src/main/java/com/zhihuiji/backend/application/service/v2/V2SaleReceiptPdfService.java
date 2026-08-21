package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Produces the canonical 80 mm sales receipt shared by every client. */
@Service
public class V2SaleReceiptPdfService {
    private static final float RECEIPT_WIDTH = 80f / 25.4f * 72f;
    private static final float SIDE_MARGIN = 10f;
    private static final float VERTICAL_MARGIN = 10f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
        .withZone(ZoneId.systemDefault());

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final StoreRepository storeRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2SaleReceiptPdfService(
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        PaymentRepository paymentRepository,
        StoreRepository storeRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.storeRepository = storeRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional(readOnly = true)
    public byte[] export(Long orderId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId)
            .stream()
            .sorted(Comparator.comparing(SaleOrderItemEntity::getCreatedAt, Comparator.nullsLast(Long::compareTo)))
            .toList();
        List<PaymentEntity> payments = paymentRepository.findByOwnerUserIdAndOrderIdOrderByCreatedAtAsc(ownerUserId, orderId);
        String storeName = storeRepository.findByOwnerUserId(ownerUserId)
            .map(StoreEntity::getStoreName)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .orElse("智慧记");
        return render(storeName, order, items, payments);
    }

    private byte[] render(
        String storeName,
        SaleOrderEntity order,
        List<SaleOrderItemEntity> items,
        List<PaymentEntity> payments
    ) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BaseFont chineseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(chineseFont, 13f, Font.BOLD);
            Font sectionFont = new Font(chineseFont, 9.5f, Font.BOLD);
            Font bodyFont = new Font(chineseFont, 8.5f, Font.NORMAL);
            Font smallFont = new Font(chineseFont, 7.5f, Font.NORMAL);

            Document document = new Document(
                new Rectangle(RECEIPT_WIDTH, calculatePageHeight(order, items, payments)),
                SIDE_MARGIN,
                SIDE_MARGIN,
                VERTICAL_MARGIN,
                VERTICAL_MARGIN
            );
            PdfWriter.getInstance(document, output);
            document.open();
            addLine(document, storeName, titleFont, Element.ALIGN_CENTER, 1f);
            addLine(document, "销售单", sectionFont, Element.ALIGN_CENTER, 4f);
            addDivider(document, smallFont);
            addLine(document, "客户：" + fallback(order.getCustomerName(), "散客"), bodyFont, Element.ALIGN_LEFT, 0f);
            addLine(document, "单号：" + fallback(order.getOrderNo(), "-"), bodyFont, Element.ALIGN_LEFT, 0f);
            addLine(document, "日期：" + formatTimestamp(order.getCreatedAt()), bodyFont, Element.ALIGN_LEFT, 0f);
            addLine(document, "状态：" + statusLabel(order.getStatus()), bodyFont, Element.ALIGN_LEFT, 4f);
            addDivider(document, smallFont);
            addLine(document, "商品明细", sectionFont, Element.ALIGN_LEFT, 2f);
            for (SaleOrderItemEntity item : items) {
                addLine(document, fallback(item.getProductName(), fallback(item.getProductCode(), "未命名商品")), sectionFont, Element.ALIGN_LEFT, 0f);
                addLine(
                    document,
                    formatQuantity(item.getQuantity()) + " × " + formatMoney(item.getUnitPrice()) + "    " + formatMoney(item.getAmount()),
                    smallFont,
                    Element.ALIGN_RIGHT,
                    2f
                );
            }
            addDivider(document, smallFont);
            addLine(document, "商品合计：" + formatMoney(order.getSubtotalAmount()), bodyFont, Element.ALIGN_RIGHT, 0f);
            if (amount(order.getDiscountAmount()) > 0) {
                addLine(document, "整单优惠：-" + formatMoney(order.getDiscountAmount()), bodyFont, Element.ALIGN_RIGHT, 0f);
            }
            addLine(document, "应收金额：" + formatMoney(order.getTotalAmount()), sectionFont, Element.ALIGN_RIGHT, 0f);
            addLine(document, "已收金额：" + formatMoney(order.getPaidAmount()), bodyFont, Element.ALIGN_RIGHT, 0f);
            addLine(
                document,
                "本单欠款：" + formatMoney(Math.max(amount(order.getTotalAmount()) - amount(order.getPaidAmount()), 0)),
                sectionFont,
                Element.ALIGN_RIGHT,
                3f
            );
            if (!payments.isEmpty()) {
                addLine(document, "收款记录", sectionFont, Element.ALIGN_LEFT, 1f);
                for (PaymentEntity payment : payments) {
                    addLine(
                        document,
                        formatTimestamp(payment.getCreatedAt()) + "  " + paymentLabel(payment) + "（" + paymentMethodLabel(payment.getMethod()) + "）" + formatMoney(payment.getAmount()),
                        smallFont,
                        Element.ALIGN_LEFT,
                        0f
                    );
                }
            }
            if (StringUtils.hasText(order.getNotes())) {
                addLine(document, "备注：" + order.getNotes().trim(), bodyFont, Element.ALIGN_LEFT, 2f);
            }
            addDivider(document, smallFont);
            addLine(document, "打印时间：" + formatTimestamp(System.currentTimeMillis()), smallFont, Element.ALIGN_LEFT, 0f);
            addLine(document, "谢谢惠顾", smallFont, Element.ALIGN_CENTER, 0f);
            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException error) {
            throw new IllegalStateException("小票 PDF 生成失败", error);
        }
    }

    private float calculatePageHeight(
        SaleOrderEntity order,
        List<SaleOrderItemEntity> items,
        List<PaymentEntity> payments
    ) {
        float contentWidth = RECEIPT_WIDTH - SIDE_MARGIN * 2;
        float height = VERTICAL_MARGIN * 2 + 98f;
        height += wrappedHeight("客户：" + fallback(order.getCustomerName(), "散客"), 8.5f, contentWidth);
        height += wrappedHeight("单号：" + fallback(order.getOrderNo(), "-"), 8.5f, contentWidth);
        height += wrappedHeight("日期：" + formatTimestamp(order.getCreatedAt()), 8.5f, contentWidth);
        height += wrappedHeight("状态：" + statusLabel(order.getStatus()), 8.5f, contentWidth);
        for (SaleOrderItemEntity item : items) {
            height += wrappedHeight(fallback(item.getProductName(), fallback(item.getProductCode(), "未命名商品")), 9.5f, contentWidth);
            height += 13f;
        }
        height += 76f;
        if (!payments.isEmpty()) {
            height += 14f;
            for (PaymentEntity payment : payments) {
                height += wrappedHeight(
                    formatTimestamp(payment.getCreatedAt())
                        + "  "
                        + paymentLabel(payment)
                        + "（"
                        + paymentMethodLabel(payment.getMethod())
                        + "）"
                        + formatMoney(payment.getAmount()),
                    7.5f,
                    contentWidth
                );
            }
        }
        if (StringUtils.hasText(order.getNotes())) {
            height += wrappedHeight("备注：" + order.getNotes().trim(), 8.5f, contentWidth) + 2f;
        }
        // Keep ordinary receipts on one variable-height page. The extra space covers
        // Paragraph leading/spacing, which is not reflected by the rough text estimate.
        return Math.max(250f, height + 44f);
    }

    private float wrappedHeight(String text, float fontSize, float maxWidth) {
        if (!StringUtils.hasText(text)) {
            return fontSize + 4f;
        }
        float estimatedCharacterWidth = fontSize;
        int charsPerLine = Math.max(1, (int) (maxWidth / estimatedCharacterWidth));
        int lines = (int) Math.ceil((double) text.length() / charsPerLine);
        return Math.max(1, lines) * (fontSize + 4f);
    }

    private void addLine(Document document, String text, Font font, int alignment, float spacingAfter) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(alignment);
        paragraph.setLeading(font.getSize() + 4f);
        paragraph.setSpacingAfter(spacingAfter);
        document.add(paragraph);
    }

    private void addDivider(Document document, Font font) throws DocumentException {
        addLine(document, "--------------------------------", font, Element.ALIGN_LEFT, 2f);
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String formatTimestamp(Long timestamp) {
        return timestamp == null ? "-" : DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    private String formatMoney(Double value) {
        return String.format(Locale.CHINA, "¥%.2f", amount(value));
    }

    private String formatMoney(double value) {
        return String.format(Locale.CHINA, "¥%.2f", value);
    }

    private String formatQuantity(Double value) {
        double quantity = amount(value);
        return Math.rint(quantity) == quantity
            ? String.format(Locale.ROOT, "%.0f", quantity)
            : String.format(Locale.ROOT, "%.2f", quantity);
    }

    private double amount(Double value) {
        return value == null ? 0D : value;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (OrderStatus.fromCode(status)) {
            case DRAFT -> "草稿";
            case CONFIRMED -> "已确认";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已作废";
            case null -> "未知";
        };
    }

    private String paymentLabel(PaymentEntity payment) {
        return payment.getType() != null && payment.getType() == PaymentType.REFUND.code() ? "退款" : "收款";
    }

    private String paymentMethodLabel(Integer method) {
        if (method == null) {
            return "其他";
        }
        return switch (method) {
            case 1 -> "现金";
            case 2 -> "微信";
            case 3 -> "支付宝";
            case 4 -> "银行卡";
            default -> "其他";
        };
    }
}
