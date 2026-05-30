package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LlmDrivenAgentService {
    private static final Logger log = LoggerFactory.getLogger(LlmDrivenAgentService.class);
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final AgentService agentService;
    private final AgentLlmService agentLlmService;
    private final ReportService reportService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    public LlmDrivenAgentService(
        AgentService agentService,
        AgentLlmService agentLlmService,
        ReportService reportService,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        ObjectMapper objectMapper
    ) {
        this.agentService = agentService;
        this.agentLlmService = agentLlmService;
        this.reportService = reportService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.objectMapper = objectMapper;
    }

    public AgentDto.AgentWorkbenchDto getWorkbench(int windowDays, int limit, int agingDays) {
        AgentDto.AgentWorkbenchDto fallback = agentService.getWorkbench(windowDays, limit, agingDays);
        AgentDto.ReportInsightDto reportInsight = agentService.getReportInsight(windowDays);
        List<String> suggestedQuestions = fallback.suggestedQuestions();
        List<String> suggestedInstructions = fallback.suggestedInstructions();
        if (!agentLlmService.isEnabled()) {
            return new AgentDto.AgentWorkbenchDto(
                fallback.reconciliation(),
                reportInsight,
                fallback.alerts(),
                suggestedQuestions,
                suggestedInstructions,
                buildFallbackOverviewBlocks(fallback.reconciliation(), reportInsight, fallback.alerts()),
                buildFallbackInstantBlocks(
                    buildFallbackProactiveAnswers(suggestedQuestions),
                    buildFallbackProactiveDrafts(suggestedInstructions)
                ),
                buildFallbackProactiveAnswers(suggestedQuestions),
                buildFallbackProactiveDrafts(suggestedInstructions)
            );
        }

        String prompt = """
            Build a warehouse agent workbench recommendation set in Chinese.
            Use only the supplied context.
            Return strict JSON with keys:
            - suggestedQuestions: array of 4 short natural-language questions
            - suggestedInstructions: array of 3 short natural-language operation instructions
            - reportInsightSummary: object with keys narrative, highlights, suggestedActions
            - proactiveAnswer: object with keys intent, query, answer, highlights, columns, rows, suggestedActions
            - proactiveDraft: object with keys operationType, partnerRole, partnerId, partnerName, notes, items
            - overviewBlocks: array of render blocks for the dashboard
            - instantBlocks: array of render blocks for the instant tab
            Each render block must use only these types: hero, metric_grid, bullet_list, table, chart, draft.
            For render block fields use:
            type, title, subtitle, tone, text, bullets, metrics, table, chart, draft.
            Questions must be grounded in the current business state.
            Instructions must stay within purchase or sale draft scenarios.
            Context:
            %s
            """.formatted(toJson(new WorkbenchPromptContext(
            fallback.reconciliation(),
            reportInsight,
            fallback.alerts(),
            loadProducts(20),
            loadCustomers(20),
            loadSuppliers(20)
        )));

        AgentDto.AgentWorkbenchDto workbench = agentLlmService.requestStructuredJson(workbenchSystemPrompt(), prompt)
            .map(node -> {
                List<String> llmQuestions = readStringList(node, "suggestedQuestions", suggestedQuestions, 4);
                List<String> llmInstructions = readStringList(node, "suggestedInstructions", suggestedInstructions, 3);
                AgentDto.AgentAnswerDto proactiveAnswer = readProactiveAnswer(node, llmQuestions);
                AgentDto.ReportInsightDto proactiveInsight = readWorkbenchInsight(node, reportInsight);
                List<AgentDto.AgentAnswerDto> proactiveAnswers = proactiveAnswer == null
                    ? buildFallbackProactiveAnswers(llmQuestions)
                    : List.of(proactiveAnswer);
                List<AgentDto.OperationDraftDto> proactiveDrafts = readProactiveDraft(node, llmInstructions);
                return new AgentDto.AgentWorkbenchDto(
                    fallback.reconciliation(),
                    proactiveInsight,
                    fallback.alerts(),
                    llmQuestions,
                    llmInstructions,
                    readRenderBlocks(
                        node.path("overviewBlocks"),
                        buildFallbackOverviewBlocks(fallback.reconciliation(), proactiveInsight, fallback.alerts())
                    ),
                    readRenderBlocks(
                        node.path("instantBlocks"),
                        buildFallbackInstantBlocks(proactiveAnswers, proactiveDrafts)
                    ),
                    proactiveAnswers,
                    proactiveDrafts
                );
            })
            .orElseGet(() -> new AgentDto.AgentWorkbenchDto(
                fallback.reconciliation(),
                reportInsight,
                fallback.alerts(),
                suggestedQuestions,
                suggestedInstructions,
                buildFallbackOverviewBlocks(fallback.reconciliation(), reportInsight, fallback.alerts()),
                buildFallbackInstantBlocks(
                    buildFallbackProactiveAnswers(suggestedQuestions),
                    buildFallbackProactiveDrafts(suggestedInstructions)
                ),
                buildFallbackProactiveAnswers(suggestedQuestions),
                buildFallbackProactiveDrafts(suggestedInstructions)
            ));
        return workbench;
    }

    public AgentDto.ReconciliationFollowupDto getReconciliationFollowup(int limit, int agingDays) {
        return agentService.getReconciliationFollowup(limit, agingDays);
    }

    public AgentDto.ReportInsightDto getReportInsight(int windowDays) {
        return agentLlmService.enrichReportInsight(agentService.getReportInsight(windowDays));
    }

    public AgentDto.AlertDashboardDto getAlerts(int limit, int agingDays) {
        return agentService.getAlerts(limit, agingDays);
    }

    public AgentDto.AgentAnswerDto answerQuestion(String query) {
        if (!agentLlmService.isEnabled()) {
            return fallbackAnswer(query);
        }

        long now = System.currentTimeMillis();
        long todayStart = startOfDay(now);
        QueryContext context = new QueryContext(
            query,
            reportService.topProducts(todayStart, now, 6),
            reportService.receivables(6),
            reportService.lowStockProducts(6),
            agentService.getReportInsight(7),
            agentService.getReconciliationFollowup(6, 15),
            agentService.getAlerts(6, 15)
        );
        AgentDto.AgentAnswerDto fallback = fallbackAnswer(query);
        String prompt = """
            Answer the warehouse question in Chinese using only the supplied context.
            Return strict JSON with keys:
            - intent: short snake_case label
            - answer: concise answer sentence or paragraph
            - highlights: array of concise bullet strings
            - columns: array of table column names
            - rows: array of string arrays aligned to columns
            - suggestedActions: array of concise follow-up actions
            If the context cannot answer the question, say so directly and leave rows empty.
            Context:
            %s
            """.formatted(toJson(context));

        return agentLlmService.requestStructuredJson(querySystemPrompt(), prompt)
            .map(node -> toAnswerDto(node, query, fallback))
            .filter(this::hasAnswerBody)
            .orElse(fallback);
    }

    public AgentDto.OperationDraftDto draftOperation(String instruction) {
        if (!agentLlmService.isEnabled()) {
            return fallbackDraft(instruction);
        }

        OperationDraftContext context = new OperationDraftContext(
            instruction,
            loadProducts(80),
            loadCustomers(80),
            loadSuppliers(80)
        );
        AgentDto.OperationDraftDto fallback = fallbackDraft(instruction);
        String prompt = """
            Parse the warehouse instruction and return one structured draft.
            Use only product, customer, and supplier choices present in the context.
            Return strict JSON with keys:
            - operationType: one of purchase, sale, return
            - partnerRole: supplier or customer
            - partnerId: number or null
            - partnerName: string
            - notes: string
            - items: array with at most 1 item
            Each item must contain:
            - productId: number or null
            - productCode: string
            - productName: string
            - quantity: number
            - unitPrice: number
            Do not invent products or parties outside the choices.
            Context:
            %s
            """.formatted(toJson(context));

        return agentLlmService.requestStructuredJson(draftSystemPrompt(), prompt)
            .map(node -> toDraftDto(node, instruction, fallback))
            .orElse(fallback);
    }

    public AgentDto.OperationSubmitResultDto submitDraft(AgentDto.OperationDraftDto draft, String idempotencyKey) {
        return agentService.submitDraft(draft, idempotencyKey);
    }

    private AgentDto.AgentAnswerDto toAnswerDto(JsonNode node, String query, AgentDto.AgentAnswerDto fallback) {
        List<String> columns = readStringList(node, "columns", fallback.columns(), 8);
        List<List<String>> rows = readTable(node.path("rows"), columns.size(), fallback.rows(), 8);
        return new AgentDto.AgentAnswerDto(
            query,
            readText(node, "intent", fallback.intent()),
            readText(node, "answer", fallback.answer()),
            readStringList(node, "highlights", fallback.highlights(), 8),
            columns,
            rows,
            readStringList(node, "suggestedActions", fallback.suggestedActions(), 6)
        );
    }

    private boolean hasAnswerBody(AgentDto.AgentAnswerDto answer) {
        return StringUtils.hasText(answer.answer());
    }

    private AgentDto.OperationDraftDto toDraftDto(JsonNode node, String instruction, AgentDto.OperationDraftDto fallback) {
        String operationType = normalizeOperationType(readText(node, "operationType", fallback.operationType()));
        String partnerRole = "purchase".equals(operationType) ? "supplier" : "customer";
        ProductEntity product = resolveProduct(node.path("items"));
        PartyResolution partner = "supplier".equals(partnerRole)
            ? resolveSupplier(node)
            : resolveCustomer(node);
        double quantity = readPositiveDouble(firstItem(node.path("items")), "quantity", 0.0);
        double unitPrice = readPositiveDouble(firstItem(node.path("items")), "unitPrice", defaultUnitPrice(operationType, product));

        List<String> warnings = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        if (product == null) {
            warnings.add("未识别到有效商品，请补充商品编码或名称。");
        }
        if (quantity <= 0.0) {
            warnings.add("未识别到有效数量。");
        }
        if (partner == null || !StringUtils.hasText(partner.name())) {
            warnings.add("未识别到有效往来对象。");
        }
        if ("sale".equals(operationType) && product != null && quantity > safeDouble(product.getStock())) {
            warnings.add(String.format(Locale.ROOT, "%s 当前库存 %.2f，不足以出库 %.2f。", product.getName(), safeDouble(product.getStock()), quantity));
        }
        if ("return".equals(operationType)) {
            warnings.add("退货场景需要关联原单据，当前仅生成建议草稿。");
        }
        if (product != null && safeDouble(product.getStock()) <= safeDouble(product.getSafeStock())) {
            actions.add(String.format(Locale.ROOT, "%s 已接近安全库存，建议同步关注补货。", product.getName()));
        }
        if ("purchase".equals(operationType)) {
            actions.add("确认供应商、到货数量和单价后再提交入库单。");
        } else if ("sale".equals(operationType)) {
            actions.add("确认客户、折扣和收款计划后再提交销售单。");
        } else {
            actions.add("补充原订单号和退货原因后再继续处理。");
        }

        boolean canSubmit = warnings.isEmpty() && product != null && partner != null && quantity > 0.0 && !"return".equals(operationType);
        List<AgentDto.OperationDraftItemDto> items = product == null || quantity <= 0.0
            ? List.of()
            : List.of(new AgentDto.OperationDraftItemDto(
                product.getId(),
                product.getCode(),
                product.getName(),
                round2(quantity),
                round2(unitPrice),
                round2(quantity * unitPrice),
                round2(safeDouble(product.getStock()))
            ));

        String partnerName = partner == null ? fallback.partnerName() : partner.name();
        return new AgentDto.OperationDraftDto(
            operationType,
            buildSummary(operationType, partnerName),
            partnerRole,
            partner == null ? null : partner.id(),
            partnerName,
            items,
            StringUtils.hasText(readText(node, "notes", instruction)) ? readText(node, "notes", instruction) : instruction,
            canSubmit,
            warnings.isEmpty() ? fallback.warnings() : warnings,
            actions
        );
    }

    private String buildSummary(String operationType, String partnerName) {
        String safePartner = StringUtils.hasText(partnerName) ? partnerName : "未识别对象";
        return switch (operationType) {
            case "sale" -> "为 " + safePartner + " 生成销售草稿";
            case "return" -> "生成退货建议草稿";
            default -> "为 " + safePartner + " 生成采购草稿";
        };
    }

    private String normalizeOperationType(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "sale", "return" -> value;
            default -> "purchase";
        };
    }

    private List<ProductOption> loadProducts(int limit) {
        return productRepository.findAll().stream()
            .sorted(Comparator.comparing(ProductEntity::getName))
            .limit(limit)
            .map(product -> new ProductOption(
                product.getId(),
                product.getCode(),
                product.getName(),
                safeDouble(product.getPurchasePrice()),
                safeDouble(product.getSalePrice()),
                safeDouble(product.getStock()),
                safeDouble(product.getSafeStock())
            ))
            .toList();
    }

    private List<PartyOption> loadCustomers(int limit) {
        return customerRepository.findAll().stream()
            .sorted(Comparator.comparing(CustomerEntity::getName))
            .limit(limit)
            .map(customer -> new PartyOption(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                safeDouble(customer.getBalance())
            ))
            .toList();
    }

    private List<PartyOption> loadSuppliers(int limit) {
        return supplierRepository.findAll().stream()
            .sorted(Comparator.comparing(SupplierEntity::getName))
            .limit(limit)
            .map(supplier -> new PartyOption(
                supplier.getId(),
                supplier.getName(),
                supplier.getPhone(),
                safeDouble(supplier.getBalance())
            ))
            .toList();
    }

    private ProductEntity resolveProduct(JsonNode itemsNode) {
        JsonNode itemNode = firstItem(itemsNode);
        Long productId = readLong(itemNode, "productId");
        if (productId != null && productId > 0L) {
            Optional<ProductEntity> byId = productRepository.findById(productId);
            if (byId.isPresent()) {
                return byId.get();
            }
            log.warn("LLM returned productId={} not found in database, falling back to name matching", productId);
        }
        String productCode = readText(itemNode, "productCode", "");
        if (StringUtils.hasText(productCode)) {
            Optional<ProductEntity> byCode = productRepository.findByCode(productCode.trim());
            if (byCode.isPresent()) {
                return byCode.get();
            }
        }
        String productName = readText(itemNode, "productName", "");
        if (!StringUtils.hasText(productName)) {
            return null;
        }
        return productRepository.findAll().stream()
            .filter(product -> textMatches(product.getName(), productName) || textMatches(productName, product.getName()))
            .sorted(Comparator.comparingInt(
                (ProductEntity product) -> productMatchScore(product, productCode, productName)
            ).reversed())
            .findFirst()
            .orElse(null);
    }

    private PartyResolution resolveCustomer(JsonNode node) {
        Long partnerId = readLong(node, "partnerId");
        if (partnerId != null && partnerId > 0L) {
            Optional<CustomerEntity> customer = customerRepository.findById(partnerId);
            if (customer.isPresent()) {
                return new PartyResolution(customer.get().getId(), customer.get().getName());
            }
            log.warn("LLM returned customerId={} not found in database, falling back to name matching", partnerId);
        }
        String partnerName = readText(node, "partnerName", "");
        if (!StringUtils.hasText(partnerName)) {
            return null;
        }
        return customerRepository.findAll().stream()
            .filter(customer -> textMatches(customer.getName(), partnerName) || textMatches(partnerName, customer.getName()))
            .findFirst()
            .map(customer -> new PartyResolution(customer.getId(), customer.getName()))
            .orElse(null);
    }

    private PartyResolution resolveSupplier(JsonNode node) {
        Long partnerId = readLong(node, "partnerId");
        if (partnerId != null && partnerId > 0L) {
            Optional<SupplierEntity> supplier = supplierRepository.findById(partnerId);
            if (supplier.isPresent()) {
                return new PartyResolution(supplier.get().getId(), supplier.get().getName());
            }
            log.warn("LLM returned supplierId={} not found in database, falling back to name matching", partnerId);
        }
        String partnerName = readText(node, "partnerName", "");
        if (!StringUtils.hasText(partnerName)) {
            return null;
        }
        return supplierRepository.findAll().stream()
            .filter(supplier -> textMatches(supplier.getName(), partnerName) || textMatches(partnerName, supplier.getName()))
            .findFirst()
            .map(supplier -> new PartyResolution(supplier.getId(), supplier.getName()))
            .orElse(null);
    }

    private int productMatchScore(ProductEntity product, String productCode, String productName) {
        int score = 0;
        if (textMatches(product.getCode(), productCode)) {
            score += 5;
        }
        if (textMatches(product.getName(), productName)) {
            score += 3;
        }
        return score;
    }

    private JsonNode firstItem(JsonNode itemsNode) {
        return itemsNode != null && itemsNode.isArray() && !itemsNode.isEmpty()
            ? itemsNode.get(0)
            : objectMapper.createObjectNode();
    }

    private List<String> readStringList(JsonNode node, String fieldName, List<String> fallback, int maxItems) {
        JsonNode value = node.path(fieldName);
        if (!value.isArray()) {
            return fallback;
        }
        List<String> items = new ArrayList<>();
        value.forEach(item -> {
            String text = item.asText("").trim();
            if (StringUtils.hasText(text) && items.size() < maxItems) {
                items.add(text);
            }
        });
        return items.isEmpty() ? fallback : items;
    }

    private List<List<String>> readTable(JsonNode node, int columnCount, List<List<String>> fallback, int maxRows) {
        if (!node.isArray()) {
            return fallback;
        }
        List<List<String>> rows = new ArrayList<>();
        node.forEach(rowNode -> {
            if (!rowNode.isArray() || rows.size() >= maxRows) {
                return;
            }
            List<String> row = new ArrayList<>();
            rowNode.forEach(cell -> row.add(cell.isNumber() ? formatNumber(cell.asDouble()) : cell.asText("")));
            if (!row.isEmpty()) {
                if (columnCount > 0 && row.size() > columnCount) {
                    rows.add(row.subList(0, columnCount));
                } else if (columnCount > 0 && row.size() < columnCount) {
                    List<String> padded = new ArrayList<>(row);
                    while (padded.size() < columnCount) {
                        padded.add("");
                    }
                    rows.add(padded);
                } else {
                    rows.add(row);
                }
            }
        });
        return rows.isEmpty() ? fallback : rows;
    }

    private String readText(JsonNode node, String fieldName, String fallback) {
        String value = node.path(fieldName).asText("");
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private Long readLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isIntegralNumber()) {
            return value.asLong();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText().trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private double readPositiveDouble(JsonNode node, String fieldName, double fallback) {
        JsonNode value = node.path(fieldName);
        if (value.isNumber()) {
            return round2(Math.max(0.0, value.asDouble()));
        }
        if (value.isTextual()) {
            try {
                return round2(Math.max(0.0, Double.parseDouble(value.asText().trim())));
            } catch (NumberFormatException ignore) {
                return fallback;
            }
        }
        return fallback;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private List<AgentDto.AgentAnswerDto> buildFallbackProactiveAnswers(List<String> questions) {
        return questions.stream()
            .filter(StringUtils::hasText)
            .limit(1)
            .map(this::fallbackAnswer)
            .toList();
    }

    private List<AgentDto.OperationDraftDto> buildFallbackProactiveDrafts(List<String> instructions) {
        return instructions.stream()
            .filter(StringUtils::hasText)
            .limit(1)
            .map(this::fallbackDraft)
            .toList();
    }

    private List<AgentDto.AgentRenderBlockDto> buildFallbackOverviewBlocks(
        AgentDto.ReconciliationFollowupDto reconciliation,
        AgentDto.ReportInsightDto reportInsight,
        AgentDto.AlertDashboardDto alerts
    ) {
        List<AgentDto.AgentTaskMetricDto> metrics = List.of(
            new AgentDto.AgentTaskMetricDto("待催收", formatMoney(reconciliation.totalReceivable()), "", "warning"),
            new AgentDto.AgentTaskMetricDto("待付款", formatMoney(reconciliation.totalPayable()), "", "success"),
            new AgentDto.AgentTaskMetricDto(
                "高风险异常",
                String.valueOf(alerts.alerts().stream().filter(alert -> "high".equals(alert.severity())).count()),
                "",
                "high"
            )
        );
        AgentDto.AgentTaskTableDto table = new AgentDto.AgentTaskTableDto(
            "待催收客户",
            List.of("客户", "电话", "金额", "动作"),
            reconciliation.receivableCustomers().stream()
                .limit(5)
                .map(item -> List.of(item.name(), safeText(item.phone(), "-"), formatMoney(item.amount()), item.actionLabel()))
                .toList()
        );
        return List.of(
            new AgentDto.AgentRenderBlockDto(
                "hero",
                reportInsight.periodLabel(),
                "经营摘要",
                "primary",
                reportInsight.narrative(),
                reportInsight.highlights(),
                List.of(),
                null,
                null,
                null
            ),
            new AgentDto.AgentRenderBlockDto(
                "metric_grid",
                "经营指标",
                "由 Agent 选择当前最值得关注的指标",
                "primary",
                null,
                List.of(),
                metrics,
                null,
                null,
                null
            ),
            new AgentDto.AgentRenderBlockDto(
                "table",
                "催办对象",
                "优先跟进前五个应收对象",
                "warning",
                null,
                List.of(),
                List.of(),
                table,
                null,
                null
            ),
            new AgentDto.AgentRenderBlockDto(
                "bullet_list",
                "建议动作",
                "按优先级执行",
                "success",
                reportInsight.narrative(),
                reportInsight.suggestedActions(),
                List.of(),
                null,
                null,
                null
            )
        );
    }

    private List<AgentDto.AgentRenderBlockDto> buildFallbackInstantBlocks(
        List<AgentDto.AgentAnswerDto> proactiveAnswers,
        List<AgentDto.OperationDraftDto> proactiveDrafts
    ) {
        List<AgentDto.AgentRenderBlockDto> blocks = new ArrayList<>();
        proactiveAnswers.stream().findFirst().ifPresent(answer ->
            blocks.add(new AgentDto.AgentRenderBlockDto(
                "bullet_list",
                answer.query(),
                answer.intent(),
                "primary",
                answer.answer(),
                answer.highlights().isEmpty() ? answer.suggestedActions() : answer.highlights(),
                List.of(),
                answer.columns().isEmpty() ? null : new AgentDto.AgentTaskTableDto("即时明细", answer.columns(), answer.rows()),
                null,
                null
            ))
        );
        proactiveDrafts.stream().findFirst().ifPresent(draft ->
            blocks.add(new AgentDto.AgentRenderBlockDto(
                "draft",
                draft.summary(),
                draft.operationType(),
                draft.canSubmit() ? "success" : "warning",
                draft.notes(),
                draft.warnings().isEmpty() ? draft.suggestedActions() : draft.warnings(),
                List.of(),
                null,
                null,
                draft
            ))
        );
        return blocks;
    }

    private AgentDto.AgentAnswerDto readProactiveAnswer(JsonNode node, List<String> fallbackQuestions) {
        JsonNode proactiveNode = node.path("proactiveAnswer");
        if (proactiveNode.isMissingNode() || proactiveNode.isEmpty()) {
            return null;
        }
        String fallbackQuery = fallbackQuestions.isEmpty() ? "当前最值得关注的经营问题是什么" : fallbackQuestions.get(0);
        AgentDto.AgentAnswerDto fallback = fallbackAnswer(fallbackQuery);
        AgentDto.AgentAnswerDto answer = toAnswerDto(proactiveNode, readText(proactiveNode, "query", fallbackQuery), fallback);
        return hasAnswerBody(answer) ? answer : null;
    }

    private List<AgentDto.OperationDraftDto> readProactiveDraft(JsonNode node, List<String> fallbackInstructions) {
        JsonNode proactiveNode = node.path("proactiveDraft");
        if (proactiveNode.isMissingNode() || proactiveNode.isEmpty()) {
            return buildFallbackProactiveDrafts(fallbackInstructions);
        }
        String fallbackInstruction = fallbackInstructions.isEmpty()
            ? "根据当前风险生成一条采购或销售草稿"
            : fallbackInstructions.get(0);
        AgentDto.OperationDraftDto fallback = fallbackDraft(fallbackInstruction);
        AgentDto.OperationDraftDto draft = toDraftDto(proactiveNode, fallbackInstruction, fallback);
        return List.of(draft);
    }

    private AgentDto.ReportInsightDto readWorkbenchInsight(JsonNode node, AgentDto.ReportInsightDto fallback) {
        JsonNode insightNode = node.path("reportInsightSummary");
        if (insightNode.isMissingNode() || insightNode.isEmpty()) {
            return fallback;
        }
        return new AgentDto.ReportInsightDto(
            fallback.periodLabel(),
            fallback.currentSales(),
            fallback.previousSales(),
            fallback.salesChangeRate(),
            readText(insightNode, "narrative", fallback.narrative()),
            fallback.leadingProductName(),
            fallback.leadingProductAmount(),
            fallback.leadingCustomerName(),
            fallback.leadingCustomerAmount(),
            readStringList(insightNode, "highlights", fallback.highlights(), 6),
            readStringList(insightNode, "suggestedActions", fallback.suggestedActions(), 6)
        );
    }

    private List<AgentDto.AgentRenderBlockDto> readRenderBlocks(
        JsonNode node,
        List<AgentDto.AgentRenderBlockDto> fallback
    ) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentDto.AgentRenderBlockDto> blocks = new ArrayList<>();
        node.forEach(item -> blocks.add(toRenderBlock(item)));
        return blocks.stream().filter(block -> StringUtils.hasText(block.type())).toList().isEmpty()
            ? fallback
            : blocks.stream().filter(block -> StringUtils.hasText(block.type())).toList();
    }

    private AgentDto.AgentRenderBlockDto toRenderBlock(JsonNode node) {
        AgentDto.AgentTaskTableDto table = null;
        JsonNode tableNode = node.path("table");
        if (!tableNode.isMissingNode() && !tableNode.isEmpty()) {
            table = new AgentDto.AgentTaskTableDto(
                readText(tableNode, "title", ""),
                readStringList(tableNode, "columns", List.of(), 10),
                readTable(tableNode.path("rows"), 0, List.of(), 20)
            );
        }

        AgentDto.AgentTaskChartDto chart = null;
        JsonNode chartNode = node.path("chart");
        if (!chartNode.isMissingNode() && !chartNode.isEmpty()) {
            List<AgentDto.AgentTaskChartSeriesDto> series = new ArrayList<>();
            chartNode.path("series").forEach(seriesNode -> {
                List<Double> values = new ArrayList<>();
                seriesNode.path("values").forEach(valueNode -> values.add(valueNode.asDouble(0.0)));
                series.add(new AgentDto.AgentTaskChartSeriesDto(
                    readText(seriesNode, "name", "series"),
                    values
                ));
            });
            chart = new AgentDto.AgentTaskChartDto(
                readText(chartNode, "title", ""),
                readText(chartNode, "chartType", "bar"),
                readStringList(chartNode, "categories", List.of(), 20),
                series
            );
        }

        return new AgentDto.AgentRenderBlockDto(
            readText(node, "type", ""),
            readText(node, "title", ""),
            readText(node, "subtitle", ""),
            readText(node, "tone", "primary"),
            readText(node, "text", ""),
            readStringList(node, "bullets", List.of(), 12),
            readMetrics(node.path("metrics")),
            table,
            chart,
            null
        );
    }

    private List<AgentDto.AgentTaskMetricDto> readMetrics(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<AgentDto.AgentTaskMetricDto> metrics = new ArrayList<>();
        node.forEach(item -> metrics.add(new AgentDto.AgentTaskMetricDto(
            readText(item, "label", ""),
            readText(item, "value", ""),
            readText(item, "delta", ""),
            readText(item, "emphasis", "primary")
        )));
        return metrics;
    }

    private AgentDto.AgentAnswerDto fallbackAnswer(String query) {
        try {
            return agentService.answerQuestion(query);
        } catch (Exception ex) {
            return new AgentDto.AgentAnswerDto(
                query,
                "general",
                StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "当前无法从业务数据中得到答案。",
                List.of(),
                List.of(),
                List.of(),
                List.of("换一个更具体的问题，例如近 7 天低库存商品有哪些。")
            );
        }
    }

    private AgentDto.OperationDraftDto fallbackDraft(String instruction) {
        try {
            return agentService.draftOperation(instruction);
        } catch (Exception ex) {
            return new AgentDto.OperationDraftDto(
                "purchase",
                "生成采购草稿失败",
                "supplier",
                null,
                null,
                List.of(),
                instruction,
                false,
                List.of(StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "当前无法识别该指令。"),
                List.of("补充商品、数量和往来对象后重试。")
            );
        }
    }

    private double defaultUnitPrice(String operationType, ProductEntity product) {
        if (product == null) {
            return 0.0;
        }
        return "sale".equals(operationType)
            ? safeDouble(product.getSalePrice())
            : safeDouble(product.getPurchasePrice());
    }

    private long startOfDay(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    private boolean textMatches(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String workbenchSystemPrompt() {
        return """
            You are a warehouse operations planning agent.
            Respond in concise Chinese.
            Suggest useful questions and draft instructions grounded only in the provided business context.
            """;
    }

    private String querySystemPrompt() {
        return """
            You are a warehouse business analyst.
            Respond in concise Chinese.
            Use only the provided context.
            Never invent metrics, product names, customer names, supplier names, or actions not supported by context.
            """;
    }

    private String draftSystemPrompt() {
        return """
            You are a warehouse order drafting agent.
            Respond with strict JSON only.
            Choose products and parties only from the provided choices.
            Never invent IDs, codes, or prices outside the context.
            """;
    }

    private record WorkbenchPromptContext(
        AgentDto.ReconciliationFollowupDto reconciliation,
        AgentDto.ReportInsightDto reportInsight,
        AgentDto.AlertDashboardDto alerts,
        List<ProductOption> products,
        List<PartyOption> customers,
        List<PartyOption> suppliers
    ) {}

    private record QueryContext(
        String query,
        List<ReportDto.TopSellingProductReportDto> topProductsToday,
        List<ReportDto.CustomerReceivableReportDto> receivables,
        List<ReportDto.LowStockProductReportDto> lowStockProducts,
        AgentDto.ReportInsightDto reportInsight7d,
        AgentDto.ReconciliationFollowupDto reconciliation,
        AgentDto.AlertDashboardDto alerts
    ) {}

    private record OperationDraftContext(
        String instruction,
        List<ProductOption> products,
        List<PartyOption> customers,
        List<PartyOption> suppliers
    ) {}

    private record ProductOption(
        Long id,
        String code,
        String name,
        double purchasePrice,
        double salePrice,
        double stock,
        double safeStock
    ) {}

    private record PartyOption(
        Long id,
        String name,
        String phone,
        double balance
    ) {}

    private record PartyResolution(Long id, String name) {}
}
