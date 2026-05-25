package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AgentService {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final int DEFAULT_LIMIT = 6;
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:个|件|箱|只|台|包|套|支|张)?");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:单价|价格|每个|每件|每箱|每台|每套)\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern SUPPLIER_PATTERN = Pattern.compile("供应商\\s*([\\p{L}\\p{N}_\\-]+)");
    private static final Pattern CUSTOMER_PATTERN = Pattern.compile("客户\\s*([\\p{L}\\p{N}_\\-]+)");

    private final ReportService reportService;
    private final SaleOrderService saleOrderService;
    private final PurchaseOrderService purchaseOrderService;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PayOrderRepository payOrderRepository;

    public AgentService(
        ReportService reportService,
        SaleOrderService saleOrderService,
        PurchaseOrderService purchaseOrderService,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        PaymentRepository paymentRepository,
        PayOrderRepository payOrderRepository
    ) {
        this.reportService = reportService;
        this.saleOrderService = saleOrderService;
        this.purchaseOrderService = purchaseOrderService;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.payOrderRepository = payOrderRepository;
    }

    public AgentDto.AgentWorkbenchDto getWorkbench(int windowDays, int limit, int agingDays) {
        int safeWindowDays = normalizePositive(windowDays, 7, 30);
        int safeLimit = normalizePositive(limit, DEFAULT_LIMIT, 20);
        return new AgentDto.AgentWorkbenchDto(
            getReconciliationFollowup(safeLimit, agingDays),
            getReportInsight(safeWindowDays),
            getAlerts(safeLimit, agingDays),
            List.of(
                "今天出库最多的商品是什么",
                "哪些客户欠款最多",
                "近7天库存异常有哪些",
                "本周销售变化如何"
            ),
            List.of(
                "给供应商A入库 20 个传感器 S7，单价 35",
                "给客户B销售 5 个绝缘手套 A12，单价 48"
            )
        );
    }

    public AgentDto.ReconciliationFollowupDto getReconciliationFollowup(int limit, int agingDays) {
        int safeLimit = normalizePositive(limit, DEFAULT_LIMIT, 20);
        int safeAgingDays = normalizePositive(agingDays, 15, 120);
        long now = System.currentTimeMillis();
        long startAt = now - 30L * DAY_MS;
        AgentDto.ReconciliationFollowupDto summary = buildReconciliationFollowup(startAt, now, safeLimit, safeAgingDays);
        return summary;
    }

    public AgentDto.ReportInsightDto getReportInsight(int windowDays) {
        int safeWindowDays = normalizePositive(windowDays, 7, 30);
        long now = System.currentTimeMillis();
        long currentStart = now - safeWindowDays * DAY_MS;
        long previousStart = currentStart - safeWindowDays * DAY_MS;
        var current = reportService.salesSummary(currentStart, now);
        var previous = reportService.salesSummary(previousStart, currentStart);
        var topProducts = reportService.topProducts(currentStart, now, 1);
        var topCustomers = reportService.customerSales(currentStart, now, 1);
        double previousSales = currentSalesBaseline(previous.totalSalesAmount());
        double salesChangeRate = previousSales <= 0.0
            ? (current.totalSalesAmount() > 0.0 ? 100.0 : 0.0)
            : ((current.totalSalesAmount() - previousSales) / previousSales) * 100.0;
        String leadingProduct = topProducts.isEmpty() ? "暂无明显爆品" : topProducts.get(0).productName();
        double leadingProductAmount = topProducts.isEmpty() ? 0.0 : topProducts.get(0).totalAmount();
        String leadingCustomer = topCustomers.isEmpty() ? "暂无明显头部客户" : topCustomers.get(0).customerName();
        double leadingCustomerAmount = topCustomers.isEmpty() ? 0.0 : topCustomers.get(0).totalAmount();
        String direction = salesChangeRate >= 0 ? "上涨" : "下滑";
        String narrative = String.format(
            Locale.ROOT,
            "最近%d天销售额%s %.1f%%，主要由%s贡献，头部客户是%s。",
            safeWindowDays,
            direction,
            Math.abs(salesChangeRate),
            leadingProduct,
            leadingCustomer
        );
        List<String> highlights = new ArrayList<>();
        highlights.add(String.format(Locale.ROOT, "本周期销售额 %.2f", current.totalSalesAmount()));
        highlights.add(String.format(Locale.ROOT, "上一周期销售额 %.2f", previous.totalSalesAmount()));
        highlights.add(String.format(Locale.ROOT, "未收金额 %.2f", current.totalUnpaidAmount()));
        if (!topProducts.isEmpty()) {
            highlights.add(String.format(Locale.ROOT, "主力商品 %s，销售 %.2f", leadingProduct, leadingProductAmount));
        }
        if (!topCustomers.isEmpty()) {
            highlights.add(String.format(Locale.ROOT, "主力客户 %s，贡献 %.2f", leadingCustomer, leadingCustomerAmount));
        }
        List<String> actions = new ArrayList<>();
        if (salesChangeRate < 0) {
            actions.add("回看本周期出库订单和低库存清单，排查是否因为缺货拖慢成交。");
        } else {
            actions.add("围绕当前主力商品补足安全库存，避免高峰期断货。");
        }
        if (current.totalUnpaidAmount() > 0.0) {
            actions.add("同步催收未结清客户，优先处理高金额订单。");
        }
        return new AgentDto.ReportInsightDto(
            safeWindowDays + "天",
            current.totalSalesAmount(),
            previous.totalSalesAmount(),
            round2(salesChangeRate),
            narrative,
            leadingProduct,
            leadingProductAmount,
            leadingCustomer,
            leadingCustomerAmount,
            highlights,
            actions
        );
    }

    public AgentDto.AlertDashboardDto getAlerts(int limit, int agingDays) {
        int safeLimit = normalizePositive(limit, DEFAULT_LIMIT, 20);
        int safeAgingDays = normalizePositive(agingDays, 15, 120);
        long now = System.currentTimeMillis();
        List<AgentDto.AlertDto> alerts = new ArrayList<>();

        reportService.lowStockProducts(safeLimit).forEach(product ->
            alerts.add(new AgentDto.AlertDto(
                "low-stock-" + product.productId(),
                "low_stock",
                product.stock() <= 0 ? "high" : "medium",
                product.productName() + " 库存低于安全线",
                String.format(Locale.ROOT, "当前库存 %.2f，安全库存 %.2f。", product.stock(), product.safeStock()),
                String.format(Locale.ROOT, "建议尽快补货 %.2f。", Math.max(0.0, product.safeStock() - product.stock())),
                product.productName(),
                product.productId(),
                round2(product.stock())
            ))
        );

        saleOrderRepository.findAll().stream()
            .filter(order -> order.getStatus() != null && order.getStatus() != SaleOrderService.STATUS_CANCELLED)
            .filter(order -> unpaidAmount(order) > 0.0)
            .filter(order -> ageDays(order.getCreatedAt(), now) >= safeAgingDays)
            .sorted(Comparator.comparingDouble(this::unpaidAmount).reversed())
            .limit(safeLimit)
            .forEach(order -> alerts.add(new AgentDto.AlertDto(
                "receivable-aging-" + order.getId(),
                "receivable_aging",
                "medium",
                safeString(order.getCustomerName(), "散客") + " 存在异常账龄",
                String.format(Locale.ROOT, "订单 %s 已 %d 天未结清，待收 %.2f。", order.getOrderNo(), ageDays(order.getCreatedAt(), now), unpaidAmount(order)),
                "优先联系客户确认回款时间，必要时拆单催收。",
                safeString(order.getCustomerName(), "散客"),
                order.getCustomerId(),
                round2(unpaidAmount(order))
            )));

        payOrderRepository.findAll().stream()
            .filter(order -> order.getStatus() != null && order.getStatus() == PayOrderService.STATUS_DRAFT)
            .filter(order -> ageDays(order.getCreatedAt(), now) >= safeAgingDays)
            .sorted(Comparator.comparingDouble(order -> -safeDouble(order.getAmount())))
            .limit(Math.max(1, safeLimit / 2))
            .forEach(order -> alerts.add(new AgentDto.AlertDto(
                "payable-aging-" + order.getId(),
                "payable_aging",
                "medium",
                order.getSupplierName() + " 待付款超期",
                String.format(Locale.ROOT, "付款单 %s 已挂起 %d 天，待付 %.2f。", order.getOrderNo(), ageDays(order.getCreatedAt(), now), safeDouble(order.getAmount())),
                "和供应商确认付款节奏，必要时先支付高优先级账单。",
                order.getSupplierName(),
                order.getSupplierId(),
                round2(order.getAmount())
            )));

        Map<Long, Long> refundCountsByOrder = paymentRepository.findAll().stream()
            .filter(this::isRefundPayment)
            .collect(Collectors.groupingBy(PaymentEntity::getOrderId, Collectors.counting()));
        refundCountsByOrder.entrySet().stream()
            .filter(entry -> entry.getValue() >= 2)
            .limit(Math.max(1, safeLimit / 2))
            .forEach(entry -> {
                SaleOrderEntity order = saleOrderRepository.findById(entry.getKey()).orElse(null);
                if (order != null) {
                    alerts.add(new AgentDto.AlertDto(
                        "refund-risk-" + order.getId(),
                        "refund_risk",
                        "high",
                        safeString(order.getCustomerName(), "散客") + " 退款异常",
                        String.format(Locale.ROOT, "订单 %s 累计退款 %d 次。", order.getOrderNo(), entry.getValue()),
                        "排查退款原因和商品质量，必要时锁定相关商品批次。",
                        safeString(order.getCustomerName(), "散客"),
                        order.getCustomerId(),
                        entry.getValue().doubleValue()
                    ));
                }
            });

        List<AgentDto.AlertDto> deduped = alerts.stream()
            .sorted(Comparator.comparing(this::severityRank).reversed())
            .limit(safeLimit * 2L)
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(AgentDto.AlertDto::id, alert -> alert, (left, right) -> left),
                map -> new ArrayList<>(map.values())
            ));
        return new AgentDto.AlertDashboardDto(deduped.stream().limit(safeLimit).toList());
    }

    public AgentDto.AgentAnswerDto answerQuestion(String query) {
        String normalized = normalizeText(query);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        long now = System.currentTimeMillis();
        if (normalized.contains("今天") && (normalized.contains("出库最多") || normalized.contains("卖得最多") || normalized.contains("销量最高"))) {
            long start = startOfDay(now);
            var rows = reportService.topProducts(start, now, 5);
            if (rows.isEmpty()) {
                return emptyAnswer(query, "top_product_today", "今天还没有出库记录。");
            }
            var first = rows.get(0);
            return new AgentDto.AgentAnswerDto(
                query,
                "top_product_today",
                String.format(Locale.ROOT, "今天出库最多的是 %s，销售额 %.2f，数量 %.2f。", first.productName(), first.totalAmount(), first.totalQuantity()),
                rows.stream()
                    .map(row -> String.format(Locale.ROOT, "%s：数量 %.2f，金额 %.2f", row.productName(), row.totalQuantity(), row.totalAmount()))
                    .toList(),
                List.of("商品", "数量", "金额"),
                rows.stream().map(row -> List.of(row.productName(), formatMoney(row.totalQuantity()), formatMoney(row.totalAmount()))).toList(),
                List.of("优先检查该商品安全库存和补货周期。")
            );
        }
        if (normalized.contains("欠款最多") || normalized.contains("应收最多")) {
            var rows = reportService.receivables(5);
            if (rows.isEmpty()) {
                return emptyAnswer(query, "receivables", "当前没有待催收客户。");
            }
            return new AgentDto.AgentAnswerDto(
                query,
                "receivables",
                String.format(Locale.ROOT, "待催收金额最高的是 %s，余额 %.2f。", rows.get(0).customerName(), rows.get(0).balance()),
                rows.stream()
                    .map(row -> String.format(Locale.ROOT, "%s：应收 %.2f", row.customerName(), row.balance()))
                    .toList(),
                List.of("客户", "手机号", "应收余额"),
                rows.stream().map(row -> List.of(row.customerName(), safeString(row.phone(), "-"), formatMoney(row.balance()))).toList(),
                List.of("优先联系前两名客户，确认回款时间。")
            );
        }
        if (normalized.contains("库存异常") || normalized.contains("低库存")) {
            var rows = reportService.lowStockProducts(6);
            if (rows.isEmpty()) {
                return emptyAnswer(query, "inventory_alerts", "近7天没有低库存商品。");
            }
            return new AgentDto.AgentAnswerDto(
                query,
                "inventory_alerts",
                String.format(Locale.ROOT, "当前共有 %d 个低库存商品，最紧急的是 %s。", rows.size(), rows.get(0).productName()),
                rows.stream()
                    .map(row -> String.format(Locale.ROOT, "%s：库存 %.2f / 安全 %.2f", row.productName(), row.stock(), row.safeStock()))
                    .toList(),
                List.of("商品", "库存", "安全库存"),
                rows.stream().map(row -> List.of(row.productName(), formatMoney(row.stock()), formatMoney(row.safeStock()))).toList(),
                List.of("先补货库存最低的商品，再看近7天出库趋势。")
            );
        }
        if (normalized.contains("本周销售") || normalized.contains("最近7天")) {
            var insight = getReportInsight(7);
            return new AgentDto.AgentAnswerDto(
                query,
                "sales_insight",
                insight.narrative(),
                insight.highlights(),
                List.of("指标", "数值"),
                List.of(
                    List.of("本周销售额", formatMoney(insight.currentSales())),
                    List.of("上周销售额", formatMoney(insight.previousSales())),
                    List.of("变化率", String.format(Locale.ROOT, "%.1f%%", insight.salesChangeRate()))
                ),
                insight.suggestedActions()
            );
        }
        if (normalized.contains("待付款供应商") || normalized.contains("应付")) {
            var recon = getReconciliationFollowup(5, 15);
            if (recon.payableSuppliers().isEmpty()) {
                return emptyAnswer(query, "payables", "当前没有待付款供应商。");
            }
            return new AgentDto.AgentAnswerDto(
                query,
                "payables",
                String.format(Locale.ROOT, "待付款最高的是 %s，金额 %.2f。", recon.payableSuppliers().get(0).name(), recon.payableSuppliers().get(0).amount()),
                recon.payableSuppliers().stream()
                    .map(item -> String.format(Locale.ROOT, "%s：应付 %.2f", item.name(), item.amount()))
                    .toList(),
                List.of("供应商", "电话", "应付金额"),
                recon.payableSuppliers().stream()
                    .map(item -> List.of(item.name(), safeString(item.phone(), "-"), formatMoney(item.amount())))
                    .toList(),
                List.of("优先支付金额高且账龄长的供应商。")
            );
        }
        return new AgentDto.AgentAnswerDto(
            query,
            "unsupported",
            "我目前支持经营问答、应收应付、库存异常、本周销售和单据草稿这几类问题。",
            List.of(
                "可以直接问：今天出库最多的商品是什么",
                "可以直接问：哪些客户欠款最多",
                "可以直接问：近7天库存异常有哪些"
            ),
            List.of(),
            List.of(),
            List.of("也可以直接输入一段入库或销售指令，我来帮你生成草稿。")
        );
    }

    public AgentDto.OperationDraftDto draftOperation(String instruction) {
        String normalized = normalizeText(instruction);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("指令不能为空");
        }

        OperationType operationType = resolveOperationType(normalized);
        ProductEntity product = resolveProduct(normalized);
        CustomerEntity customer = operationType == OperationType.SALE ? resolveCustomer(normalized) : null;
        SupplierEntity supplier = operationType == OperationType.PURCHASE ? resolveSupplier(normalized) : null;
        double quantity = resolveQuantity(normalized);
        double unitPrice = resolveUnitPrice(normalized, operationType, product);

        List<String> warnings = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        if (product == null) {
            warnings.add("没有识别到商品，请补充商品名称或编码。");
        }
        if (quantity <= 0.0) {
            warnings.add("没有识别到有效数量。");
        }
        if (operationType == OperationType.PURCHASE && supplier == null) {
            warnings.add("没有识别到供应商。");
        }
        if (operationType == OperationType.SALE && customer == null) {
            warnings.add("没有识别到客户。");
        }
        if (operationType == OperationType.RETURN) {
            warnings.add("退货场景需要关联原销售单，当前只生成建议草稿。");
        }
        if (product != null && operationType == OperationType.SALE && quantity > safeDouble(product.getStock())) {
            warnings.add(String.format(Locale.ROOT, "%s 当前库存 %.2f，无法直接出库 %.2f。", product.getName(), safeDouble(product.getStock()), quantity));
        }
        if (product != null && product.getStock() <= product.getSafeStock()) {
            actions.add(String.format(Locale.ROOT, "%s 已接近安全库存，建议同步补货。", product.getName()));
        }
        if (operationType == OperationType.PURCHASE) {
            actions.add("确认供应商、单价和到货状态后再提交入库单。");
        } else if (operationType == OperationType.SALE) {
            actions.add("确认客户和折扣后再提交销售单。");
        } else {
            actions.add("补充原订单编号后再处理退货。");
        }

        boolean canSubmit = warnings.isEmpty() && operationType != OperationType.RETURN && product != null && quantity > 0.0;
        List<AgentDto.OperationDraftItemDto> items = product == null || quantity <= 0.0
            ? List.of()
            : List.of(new AgentDto.OperationDraftItemDto(
                product.getId(),
                product.getCode(),
                product.getName(),
                quantity,
                unitPrice,
                round2(quantity * unitPrice),
                round2(safeDouble(product.getStock()))
            ));
        String partnerRole = switch (operationType) {
            case PURCHASE -> "supplier";
            case SALE -> "customer";
            case RETURN -> "customer";
        };
        Long partnerId = operationType == OperationType.PURCHASE
            ? (supplier == null ? null : supplier.getId())
            : (customer == null ? null : customer.getId());
        String partnerName = operationType == OperationType.PURCHASE
            ? (supplier == null ? guessSupplierName(normalized) : supplier.getName())
            : (customer == null ? guessCustomerName(normalized) : customer.getName());
        String summary = switch (operationType) {
            case PURCHASE -> String.format(Locale.ROOT, "为 %s 生成入库草稿", safeString(partnerName, "未识别供应商"));
            case SALE -> String.format(Locale.ROOT, "为 %s 生成出库草稿", safeString(partnerName, "未识别客户"));
            case RETURN -> "生成退货建议草稿";
        };
        return new AgentDto.OperationDraftDto(
            operationType.apiValue,
            summary,
            partnerRole,
            partnerId,
            partnerName,
            items,
            instruction,
            canSubmit,
            warnings,
            actions
        );
    }

    public AgentDto.OperationSubmitResultDto submitDraft(AgentDto.OperationDraftDto draft) {
        if (draft == null || draft.items().isEmpty()) {
            throw new IllegalArgumentException("草稿不能为空");
        }
        if (!draft.canSubmit()) {
            throw new IllegalArgumentException("当前草稿还不能提交，请先补齐必要信息");
        }
        AgentDto.OperationDraftItemDto item = draft.items().get(0);
        if (Objects.equals(draft.operationType(), OperationType.PURCHASE.apiValue)) {
            PurchaseOrderService.PurchaseDetail created = purchaseOrderService.create(
                new PurchaseOrderService.CreatePurchaseOrderCommand(
                    draft.partnerName(),
                    List.of(new PurchaseOrderService.PurchaseItemDraft(
                        item.productId(),
                        item.productCode(),
                        item.productName(),
                        item.quantity(),
                        item.unitPrice()
                    )),
                    draft.notes(),
                    PurchaseOrderService.STATUS_RECEIVED
                )
            );
            PurchaseOrderEntity order = created.order();
            return new AgentDto.OperationSubmitResultDto(
                draft.operationType(),
                order.getId(),
                order.getOrderNo(),
                "采购入库单已提交",
                "回到采购页确认到货数量与价格。"
            );
        }
        if (Objects.equals(draft.operationType(), OperationType.SALE.apiValue)) {
            SaleOrderService.OrderDetail created = saleOrderService.create(
                new SaleOrderService.CreateSaleOrderCommand(
                    draft.partnerId(),
                    draft.partnerName(),
                    List.of(new SaleOrderService.SaleItemDraft(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                    )),
                    draft.notes(),
                    0.0
                )
            );
            SaleOrderEntity order = created.order();
            return new AgentDto.OperationSubmitResultDto(
                draft.operationType(),
                order.getId(),
                order.getOrderNo(),
                "销售出库单已提交",
                "回到销售单据页确认收款状态。"
            );
        }
        throw new IllegalArgumentException("当前仅支持提交入库和出库草稿");
    }

    private AgentDto.ReconciliationFollowupDto buildReconciliationFollowup(long startAt, long endAt, int limit, int agingDays) {
        var summary = reportService.reconciliationSummary(startAt, endAt);
        List<AgentDto.FollowupPartyDto> receivableCustomers = reportService.receivables(limit).stream()
            .map(item -> new AgentDto.FollowupPartyDto(
                item.customerId(),
                "customer",
                item.customerName(),
                item.phone(),
                round2(item.balance()),
                "催收"
            ))
            .toList();
        List<AgentDto.FollowupPartyDto> payableSuppliers = supplierRepository.findAll().stream()
            .filter(supplier -> safeDouble(supplier.getBalance()) > 0.0)
            .sorted(Comparator.comparingDouble((SupplierEntity supplier) -> safeDouble(supplier.getBalance())).reversed())
            .limit(limit)
            .map(supplier -> new AgentDto.FollowupPartyDto(
                supplier.getId(),
                "supplier",
                supplier.getName(),
                supplier.getPhone(),
                round2(safeDouble(supplier.getBalance())),
                "付款"
            ))
            .toList();
        List<AgentDto.AgingRiskDto> agingRisks = buildAgingRisks(agingDays, limit);
        return new AgentDto.ReconciliationFollowupDto(
            round2(summary.totalReceivableAmount()),
            round2(summary.totalPayableAmount()),
            round2(summary.totalReceivedAmount()),
            round2(summary.totalPaidAmount()),
            round2(summary.netCashFlow()),
            receivableCustomers,
            payableSuppliers,
            agingRisks
        );
    }

    private List<AgentDto.AgingRiskDto> buildAgingRisks(int agingDays, int limit) {
        long now = System.currentTimeMillis();
        List<AgentDto.AgingRiskDto> rows = new ArrayList<>();
        saleOrderRepository.findAll().stream()
            .filter(order -> order.getStatus() == null || order.getStatus() != SaleOrderService.STATUS_CANCELLED)
            .filter(order -> unpaidAmount(order) > 0.0)
            .filter(order -> ageDays(order.getCreatedAt(), now) >= agingDays)
            .forEach(order -> rows.add(new AgentDto.AgingRiskDto(
                "customer",
                order.getCustomerId(),
                safeString(order.getCustomerName(), "散客"),
                order.getOrderNo(),
                safeLong(order.getCreatedAt()),
                ageDays(order.getCreatedAt(), now),
                round2(unpaidAmount(order)),
                "销售单长时间未回款",
                "联系客户确认回款节点"
            )));
        payOrderRepository.findAll().stream()
            .filter(order -> order.getStatus() != null && order.getStatus() == PayOrderService.STATUS_DRAFT)
            .filter(order -> ageDays(order.getCreatedAt(), now) >= agingDays)
            .forEach(order -> rows.add(new AgentDto.AgingRiskDto(
                "supplier",
                order.getSupplierId(),
                safeString(order.getSupplierName(), "供应商"),
                order.getOrderNo(),
                safeLong(order.getCreatedAt()),
                ageDays(order.getCreatedAt(), now),
                round2(safeDouble(order.getAmount())),
                "付款单长时间未处理",
                "核对对账单并安排付款"
            )));
        return rows.stream()
            .sorted(Comparator.comparingDouble(AgentDto.AgingRiskDto::amount).reversed())
            .limit(limit)
            .toList();
    }

    private ProductEntity resolveProduct(String normalizedInstruction) {
        List<ProductEntity> products = productRepository.findAll();
        List<ProductEntity> matches = products.stream()
            .filter(product -> containsIgnoreCase(normalizedInstruction, product.getCode()) || containsIgnoreCase(normalizedInstruction, product.getName()))
            .sorted(Comparator.comparingInt((ProductEntity product) -> matchScore(normalizedInstruction, product)).reversed())
            .toList();
        if (!matches.isEmpty()) {
            return matches.get(0);
        }
        String productFragment = extractProductFragment(normalizedInstruction);
        if (productFragment.isBlank()) {
            return null;
        }
        return products.stream()
            .filter(product -> containsIgnoreCase(product.getName(), productFragment) || containsIgnoreCase(productFragment, product.getName()))
            .findFirst()
            .orElse(null);
    }

    private CustomerEntity resolveCustomer(String normalizedInstruction) {
        String guessedName = guessCustomerName(normalizedInstruction);
        if (!guessedName.isBlank()) {
            return customerRepository.findAll().stream()
                .filter(customer -> containsIgnoreCase(customer.getName(), guessedName) || containsIgnoreCase(guessedName, customer.getName()))
                .findFirst()
                .orElse(null);
        }
        return customerRepository.findAll().stream()
            .filter(customer -> containsIgnoreCase(normalizedInstruction, customer.getName()))
            .findFirst()
            .orElse(null);
    }

    private SupplierEntity resolveSupplier(String normalizedInstruction) {
        String guessedName = guessSupplierName(normalizedInstruction);
        if (!guessedName.isBlank()) {
            return supplierRepository.findAll().stream()
                .filter(supplier -> containsIgnoreCase(supplier.getName(), guessedName) || containsIgnoreCase(guessedName, supplier.getName()))
                .findFirst()
                .orElse(null);
        }
        return supplierRepository.findAll().stream()
            .filter(supplier -> containsIgnoreCase(normalizedInstruction, supplier.getName()))
            .findFirst()
            .orElse(null);
    }

    private double resolveQuantity(String normalizedInstruction) {
        String fragment = extractProductFragment(normalizedInstruction);
        Matcher matcher = QUANTITY_PATTERN.matcher(fragment);
        if (matcher.find()) {
            return parseDouble(matcher.group(1));
        }
        Matcher fallback = QUANTITY_PATTERN.matcher(normalizedInstruction);
        if (fallback.find()) {
            return parseDouble(fallback.group(1));
        }
        return 0.0;
    }

    private double resolveUnitPrice(String normalizedInstruction, OperationType operationType, ProductEntity product) {
        Matcher matcher = PRICE_PATTERN.matcher(normalizedInstruction);
        if (matcher.find()) {
            return round2(parseDouble(matcher.group(1)));
        }
        if (product == null) {
            return 0.0;
        }
        return round2(operationType == OperationType.PURCHASE ? safeDouble(product.getPurchasePrice()) : safeDouble(product.getSalePrice()));
    }

    private String extractProductFragment(String normalizedInstruction) {
        String instruction = normalizedInstruction.replace('，', ',');
        int operationIndex = Math.max(
            Math.max(instruction.indexOf("入库"), instruction.indexOf("销售")),
            Math.max(instruction.indexOf("出库"), instruction.indexOf("退货"))
        );
        String tail = operationIndex >= 0 && operationIndex + 2 < instruction.length()
            ? instruction.substring(operationIndex + 2)
            : instruction;
        int priceIndex = indexOfAny(tail, List.of("单价", "价格", ","));
        return priceIndex >= 0 ? tail.substring(0, priceIndex).trim() : tail.trim();
    }

    private OperationType resolveOperationType(String normalizedInstruction) {
        if (normalizedInstruction.contains("退货")) {
            return OperationType.RETURN;
        }
        if (normalizedInstruction.contains("入库") || normalizedInstruction.contains("采购") || normalizedInstruction.contains("补货")) {
            return OperationType.PURCHASE;
        }
        if (normalizedInstruction.contains("出库") || normalizedInstruction.contains("销售")) {
            return OperationType.SALE;
        }
        return OperationType.PURCHASE;
    }

    private String guessSupplierName(String normalizedInstruction) {
        Matcher matcher = SUPPLIER_PATTERN.matcher(normalizedInstruction);
        return matcher.find() ? trimPartnerSuffix(matcher.group(1)) : "";
    }

    private String guessCustomerName(String normalizedInstruction) {
        Matcher matcher = CUSTOMER_PATTERN.matcher(normalizedInstruction);
        return matcher.find() ? trimPartnerSuffix(matcher.group(1)) : "";
    }

    private String trimPartnerSuffix(String raw) {
        return raw
            .replace("入库", "")
            .replace("采购", "")
            .replace("销售", "")
            .replace("出库", "")
            .replace("退货", "")
            .trim();
    }

    private AgentDto.AgentAnswerDto emptyAnswer(String query, String intent, String answer) {
        return new AgentDto.AgentAnswerDto(query, intent, answer, List.of(), List.of(), List.of(), List.of());
    }

    private double unpaidAmount(SaleOrderEntity order) {
        return Math.max(0.0, safeDouble(order.getTotalAmount()) - safeDouble(order.getPaidAmount()));
    }

    private long ageDays(Long createdAt, long now) {
        return Math.max(0L, (now - safeLong(createdAt)) / DAY_MS);
    }

    private double currentSalesBaseline(double value) {
        return Math.max(0.0, value);
    }

    private long startOfDay(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    private boolean isRefundPayment(PaymentEntity payment) {
        return (payment.getType() != null && payment.getType() == SaleOrderService.PAYMENT_TYPE_REFUND)
            || safeDouble(payment.getAmount()) < 0.0;
    }

    private int severityRank(AgentDto.AlertDto alert) {
        return switch (alert.severity()) {
            case "high" -> 3;
            case "medium" -> 2;
            default -> 1;
        };
    }

    private int matchScore(String normalizedInstruction, ProductEntity product) {
        int score = 0;
        if (containsIgnoreCase(normalizedInstruction, product.getCode())) {
            score += 5;
        }
        if (containsIgnoreCase(normalizedInstruction, product.getName())) {
            score += 3;
        }
        return score;
    }

    private boolean containsIgnoreCase(String left, String right) {
        if (left == null || right == null || right.isBlank()) {
            return false;
        }
        return left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
    }

    private int indexOfAny(String target, List<String> candidates) {
        int index = -1;
        for (String candidate : candidates) {
            int current = target.indexOf(candidate);
            if (current >= 0 && (index < 0 || current < index)) {
                index = current;
            }
        }
        return index;
    }

    private String normalizeText(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
    }

    private int normalizePositive(int raw, int fallback, int max) {
        if (raw <= 0) {
            return fallback;
        }
        return Math.min(raw, max);
    }

    private String safeString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignore) {
            return 0.0;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private enum OperationType {
        PURCHASE("purchase"),
        SALE("sale"),
        RETURN("return");

        private final String apiValue;

        OperationType(String apiValue) {
            this.apiValue = apiValue;
        }
    }
}
